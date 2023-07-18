package dji.sampleV5.modulecommon.pages

import android.os.Bundle
import android.view.*
import android.widget.SeekBar
import androidx.fragment.app.activityViewModels
import dji.sampleV5.modulecommon.R
import dji.sampleV5.modulecommon.data.MEDIA_FILE_DETAILS_STR
import dji.sampleV5.modulecommon.models.VideoPlayVM
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.utils.CameraUtil
import dji.sdk.keyvalue.value.camera.CameraType
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.video.channel.VideoChannelType
import dji.v5.common.video.decoder.*
import dji.v5.common.video.interfaces.*
import dji.v5.manager.KeyManager
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.datacenter.media.MediaFile
import dji.v5.manager.datacenter.media.VideoPlayState
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.ToastUtils
import kotlinx.android.synthetic.main.video_play_page.*


class VideoPlayFragment : DJIFragment(), SurfaceHolder.Callback, View.OnClickListener {

    val videoPlayVM : VideoPlayVM by activityViewModels()
    private lateinit var surfaceView: SurfaceView
    private var videoDecoder: IVideoDecoder? = null
    var mediaFile: MediaFile? = null
    private val TAG = LogUtils.getTag(this)
    private var enterPlaybackSuccess = false
    var videoPlayState:VideoPlayState = VideoPlayState.IDLE


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.video_play_page, container, false)
        surfaceView = view.findViewById(R.id.surfaceView)
        surfaceView.holder.addCallback(this)
        mediaFile = arguments?.getSerializable(MEDIA_FILE_DETAILS_STR) as MediaFile
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        enterPlayback()
    }

    private fun enterPlayback() {
        MediaDataCenter.getInstance().mediaManager.enable(object :CommonCallbacks.CompletionCallback{
            override fun onSuccess() {
                enterPlaybackSuccess = true
                LogUtils.e(TAG , "enter success");
            }

            override fun onFailure(error: IDJIError) {
                enterPlaybackSuccess = false
                LogUtils.e(TAG , "enter failed" + error.description());
            }

        })
    }


    private fun initView() {
        surfaceView.setOnClickListener(this)
        operateSeekBar()
        videoPlayVM.addVideoPlayStateListener()
        stop.setOnClickListener(){
            videoPlayVM.stop()
        }
        videoPlayVM.videoPlayStatus.observe(viewLifecycleOwner) {
            videoPlayState = it.state
            when(it.state) {
                VideoPlayState.PLAYING -> {
                    val currentDouble: Double = it.getPlayingPosition()
                    var currentIntPosition = currentDouble.toInt()
                    val videoTime = mediaFile!!.duration.toDouble()
                    val totalVideoTime = videoTime.toInt()
                    if (currentIntPosition <= 0) {   //对当前位置progress数据进行判断是否正常
                        currentIntPosition = 0
                    } else if (currentIntPosition >= videoTime) {
                        currentIntPosition = totalVideoTime
                    }
                    seekBar?.progress = currentIntPosition
                    playingtime?.setText(videoPlayVM.showTime(currentIntPosition))
                    operate?.visibility = View.GONE
                }
                VideoPlayState.PAUSED -> {
                    operate?.visibility = View.VISIBLE
                    operate?.setImageResource(R.drawable.video_pause)
                }

                else -> {
                    operate?.visibility = View.VISIBLE
                    operate?.setImageResource(R.drawable.icon_play)
                }
            }

        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        if (videoDecoder != null) {
            videoDecoder?.destroy()
            videoDecoder = null
        }
        videoPlayVM.stop()
        videoPlayVM.removeAllListener()
        enterPlaybackSuccess = false
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (videoDecoder == null) {
            videoDecoder = createVideoDecoder()
        } else if (videoDecoder?.decoderStatus == DecoderState.PAUSED) {
            videoDecoder?.onResume()
        }
        videoDecoder?.mediaFile = mediaFile
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (videoDecoder == null) {
            videoDecoder = createVideoDecoder()

        } else if (videoDecoder?.decoderStatus == DecoderState.PAUSED) {
            videoDecoder?.onResume()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        videoDecoder?.onPause()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.surfaceView -> {
                when(videoPlayState) {
                    VideoPlayState.PLAYING -> videoPlayVM.pause()
                    VideoPlayState.PAUSED -> videoPlayVM.resume()
                    else -> {
                        play()
                    }
                }

            }
        }
    }

    private fun createVideoDecoder():IVideoDecoder{
        return  VideoDecoder(
            this@VideoPlayFragment.context,
            VideoChannelType.EXTENDED_STREAM_CHANNEL,
            DecoderOutputMode.SURFACE_MODE,
            surfaceView.holder
        )
    }
    private fun play(){
        if (!enterPlaybackSuccess) {
            ToastUtils.showToast("Please retry")
            return
        }
        MediaDataCenter.getInstance().mediaManager.playVideo(mediaFile , object :CommonCallbacks.CompletionCallbackWithParam<IVideoFrame>{
            override fun onSuccess(data: IVideoFrame?) {
                videoDecoder?.queueInFrame(data!!)

            }

            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast("play failed:" + error.description())
            }
        })
    }

    private fun operateSeekBar() {
        val timed = mediaFile!!.duration.toDouble()
        val time = timed.toInt() / 1000
        duration?.setText(videoPlayVM.showTime(time))
        val doubleDuration = mediaFile!!.duration.toDouble()
        val intDuration = doubleDuration.toInt() / 1000
        seekBar.max = intDuration
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                seek_tip?.setText("Progress：$progress")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                seek_tip?.setVisibility(View.VISIBLE)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                seek_tip?.setVisibility(View.INVISIBLE)
                val douProgress = seekBar.progress.toDouble()
                val intProgress = douProgress.toInt()
                videoPlayVM.seek(intProgress)
            }
        })
    }


}