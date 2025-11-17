package dji.sampleV5.aircraft.pages

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.*
import android.view.*
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.data.DEFAULT_STR
import dji.sampleV5.aircraft.databinding.FragAppSilentlyUpgradePageBinding
import dji.sampleV5.aircraft.databinding.VideoChannelPageBinding
import dji.sampleV5.aircraft.models.MultiVideoChannelVM
import dji.sampleV5.aircraft.models.VideoChannelVM
import dji.sampleV5.aircraft.models.VideoChannelVMFactory
import dji.sampleV5.aircraft.util.ToastUtils
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.video.channel.VideoChannelState
import dji.v5.common.video.channel.VideoChannelType
import dji.v5.common.video.decoder.*
import dji.v5.common.video.interfaces.*
import dji.v5.common.video.stream.StreamSource
import dji.v5.utils.common.*
import java.io.*

@Deprecated(message = "Replace With CameraStreamListFragment")
class VideoChannelFragment : DJIFragment(), View.OnClickListener, SurfaceHolder.Callback,
    YuvDataListener {
    private val TAG = LogUtils.getTag("VideoChannelFragment")
    private val PATH: String = "/DJI_ScreenShot"
    private val multiVideoChannelVM: MultiVideoChannelVM by activityViewModels()
    private var binding: VideoChannelPageBinding? = null
    private lateinit var channelVM: VideoChannelVM
    private lateinit var surfaceView: SurfaceView
    private lateinit var dialog: AlertDialog
    private var streamSources: List<StreamSource>? = null
    private var videoDecoder: IVideoDecoder? = null

    private var videoWidth: Int = -1
    private var videoHeight: Int = -1
    private var widthChanged = false
    private var heightChanged = false
    private var fps: Int = -1

    private var checkedItem: Int = -1
    private var count: Int = 0
    private var stringBuilder: StringBuilder? = StringBuilder()
    private val DISPLAY = 100
    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg?.what) {
                DISPLAY -> {
                    display(msg?.obj.toString())
                }
            }
        }
    }

    //组帧后数据Listener
    private val streamDataListener =
        StreamDataListener {
            /**
             * 码流数据回调方法
             *
             * @param VideoFrame 码流帧数据
             */
            it.let {
                if (fps != it.fps) {
                    fps = it.fps
                    mainHandler.post {
                        channelVM.videoChannelInfo.value?.fps = fps
                        channelVM.refreshVideoChannelInfo()
                    }
                }
                if (videoWidth != it.width) {
                    videoWidth = it.width
                    widthChanged = true
                }
                if (videoHeight != it.height) {
                    videoHeight = it.height
                    heightChanged = true
                }
                if (widthChanged || heightChanged) {
                    widthChanged = false
                    heightChanged = false
                    mainHandler.post {
                        channelVM.videoChannelInfo.value?.resolution =
                            "${videoWidth}*${videoHeight}"
                        channelVM.refreshVideoChannelInfo()
                    }
                }
            }
        }

    private val decoderStateChangeListener =
        DecoderStateChangeListener { _, newState ->
            mainHandler.post {
                channelVM.videoChannelInfo.value?.decoderState = newState
                channelVM.refreshVideoChannelInfo()
            }

        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = VideoChannelPageBinding.inflate(inflater, container, false)
        binding?.root?.let {
            surfaceView = it.findViewById(R.id.surface_view)
            surfaceView.holder.addCallback(this)
            it.findViewById<Button>(R.id.startChannel).setOnClickListener(this)
            it.findViewById<Button>(R.id.closeChannel).setOnClickListener(this)
            it.findViewById<Button>(R.id.yuvScreenShot).setOnClickListener(this)
            it.findViewById<Button>(R.id.startSocket).setOnClickListener(this)
            it.findViewById<Button>(R.id.closeSocket).setOnClickListener(this)
            it.findViewById<Button>(R.id.startBroadcast).setOnClickListener(this)
            it.findViewById<Button>(R.id.stopBroadcast).setOnClickListener(this)
        }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val videoChannelType: VideoChannelType = when (tag) {
            VideoChannelType.SECONDARY_STREAM_CHANNEL.name -> VideoChannelType.SECONDARY_STREAM_CHANNEL
            VideoChannelType.EXTENDED_STREAM_CHANNEL.name -> VideoChannelType.EXTENDED_STREAM_CHANNEL
            else -> VideoChannelType.PRIMARY_STREAM_CHANNEL
        }
        val factory = VideoChannelVMFactory(videoChannelType)
        channelVM = ViewModelProvider(this, factory).get(VideoChannelVM::class.java)
        init()
    }

    private fun init() {
        multiVideoChannelVM.videoStreamSources.observe(viewLifecycleOwner) {
            streamSources = it
        }
        initVideoChannelInfo()
    }

    private fun initVideoChannelInfo() {
        streamSources = multiVideoChannelVM.videoStreamSources.value
        channelVM.videoChannelInfo.observe(viewLifecycleOwner) {
            it?.let {
                val videoStreamInfo =
                    "\n StreamSource: [${it.streamSource?.physicalDeviceCategory} : ${it.streamSource?.physicalDeviceType?.deviceType} : ${it.streamSource?.physicalDevicePosition}] \n " +
                            "ChannelType: [${it.videoChannelType.name}] State: [${it.videoChannelState}] \n " +
                            "DecoderState: [${it.decoderState}] Resolution: [${it.resolution}] \n " +
                            "FPS: [${it.fps}] Format: [${it.format}] BitRate: [${it.bitRate} Kb/s] \n " +
                            "Socket: [${it.socket}]"
                binding?.videoStreamInfo?.text = videoStreamInfo
            }
        }
        channelVM.videoChannel?.addStreamDataListener(streamDataListener) ?: showDisconnectToast()
        this@VideoChannelFragment.setFragmentResultListener("ResetAllVideoChannel") { requestKey, _ ->
            if ("ResetAllVideoChannel" == requestKey) {
                mainHandler.post {
                    channelVM.videoChannelInfo.value?.streamSource = null
                    channelVM.videoChannelInfo.value?.videoChannelState = VideoChannelState.CLOSE
                    channelVM.refreshVideoChannelInfo()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        channelVM.videoChannel?.removeStreamDataListener(streamDataListener)
        if (videoDecoder != null) {
            videoDecoder?.destroy()
            videoDecoder = null
        }
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.startChannel -> {
                channelVM.videoChannel?.let {
                    if (channelVM.videoChannel!!.videoChannelStatus == VideoChannelState.CLOSE) {
                        showStartChannelDialog()
                    }
                } ?: showDisconnectToast()
            }

            R.id.closeChannel -> {
                channelVM.videoChannel?.let {
                    channelVM.videoChannel!!.closeChannel(object :
                        CommonCallbacks.CompletionCallback {
                        /**
                         * Invoked when the asynchronous operation completes successfully. Override to
                         * handheld in your own code.
                         */
                        override fun onSuccess() {
                            mainHandler.post {
                                channelVM.videoChannelInfo.value?.videoChannelState =
                                    VideoChannelState.CLOSE
                                channelVM.videoChannelInfo.value?.socket = DEFAULT_STR
                                channelVM.refreshVideoChannelInfo()
                            }
                            ToastUtils.showToast("Close Channel Success")

                        }

                        /**
                         * Invoked when the asynchronous operation completes. If the operation  completes
                         * successfully, `error` will be `null`. Override to handheld in your own code.
                         *
                         * @param error The DJI error result
                         */
                        override fun onFailure(error: IDJIError) {
                            ToastUtils.showToast(
                                "Close Channel Failed: (errorCode ${error.errorCode()}, description: ${error.description()})"
                            )

                        }
                    })
                } ?: showDisconnectToast()
            }

            R.id.yuvScreenShot -> {
                channelVM.videoChannel?.let {
                    handlerYUV()
                } ?: showDisconnectToast()
            }
        }
    }

    private fun showStartChannelDialog() {
        val length = streamSources?.size
        if (length == 0) {
            return
        }
        val items = length?.let { arrayOfNulls<String>(it) }
        streamSources?.let {
            for (i in 0 until length!!) {
                items?.set(i, streamSources!![i].toString())
            }
            if (!items.isNullOrEmpty()) {
                dialog = this@VideoChannelFragment.requireContext().let { context ->
                    AlertDialog.Builder(context, androidx.appcompat.R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
                        .setIcon(android.R.drawable.ic_menu_camera)
                        .setTitle(R.string.select_stream_source)
                        .setCancelable(false)
                        .setSingleChoiceItems(
                            items, checkedItem
                        ) { _, i ->
                            checkedItem = i
                            ToastUtils.showToast(
                                "选择的码流源为： " + (items[i] ?: "空"),
                            )
                        }.setPositiveButton(R.string.confirm) { dialog, _ ->
                            run {
                                val streamSource = streamSources?.getOrNull(checkedItem)
                                streamSource?.let { startChannel(it) }
                                dialog.dismiss()
                            }
                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ ->
                            run {
                                dialog.dismiss()
                            }
                        }
                        .create()
                }
                dialog.show()
            }
        }
    }


    private fun startChannel(streamSource: StreamSource) {
        channelVM.videoChannel?.let {
            channelVM.videoChannel!!.startChannel(
                streamSource,
                object : CommonCallbacks.CompletionCallback {
                    /**
                     * Invoked when the asynchronous operation completes successfully. Override to
                     * handheld in your own code.
                     */
                    override fun onSuccess() {
                        mainHandler.post {
                            channelVM.videoChannelInfo.value?.streamSource =
                                streamSource
                            channelVM.refreshVideoChannelInfo()
                        }

                        ToastUtils.showToast("Start Channel Success")

                    }

                    /**
                     * Invoked when the asynchronous operation completes. If the operation  completes
                     * successfully, `error` will be `null`. Override to handheld in your own code.
                     *
                     * @param error The DJI error result
                     */
                    override fun onFailure(error: IDJIError) {
                        ToastUtils.showToast(
                            "Start Channel Failed: (errorCode ${error.errorCode()}, description: ${error.description()})"
                        )
                    }
                })
        } ?: showDisconnectToast()
    }

    /**
     * This is called immediately after the surface is first created.
     * Implementations of this should start up whatever rendering code
     * they desire.  Note that only one thread can ever draw into
     * a [Surface], so you should not draw into the Surface here
     * if your normal rendering will be in another thread.
     *
     * @param holder The SurfaceHolder whose surface is being created.
     */
    override fun surfaceCreated(holder: SurfaceHolder) {
        // do nothing
    }

    /**
     * This is called immediately after any structural changes (format or
     * size) have been made to the surface.  You should at this point update
     * the imagery in the surface.  This method is always called at least
     * once, after [.surfaceCreated].
     *
     * @param holder The SurfaceHolder whose surface has changed.
     * @param format The new PixelFormat of the surface.
     * @param width The new width of the surface.
     * @param height The new height of the surface.
     */
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (videoDecoder == null) {
            channelVM.videoChannel?.let {
                videoDecoder = VideoDecoder(
                    this@VideoChannelFragment.context,
                    channelVM.videoChannel!!.videoChannelType,
                    DecoderOutputMode.SURFACE_MODE,
                    surfaceView.holder,
                    width,
                    height
                )
                videoDecoder?.addDecoderStateChangeListener(decoderStateChangeListener)
                decoderStateChangeListener.onUpdate(videoDecoder?.decoderStatus,videoDecoder?.decoderStatus)
            }
        }
    }

    /**
     * This is called immediately before a surface is being destroyed. After
     * returning from this call, you should no longer try to access this
     * surface.  If you have a rendering thread that directly accesses
     * the surface, you must ensure that thread is no longer touching the
     * Surface before returning from this function.
     *
     * @param holder The SurfaceHolder whose surface is being destroyed.
     */
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        videoDecoder?.destroy()
    }

    override fun onReceive(mediaFormat: MediaFormat?, data: ByteArray?, width: Int, height: Int) {
        if (++count == 30) {
            count = 0
            data?.let {
                AsyncTask.execute(object : Runnable {
                    override fun run() {
                        var path: String =
                            DiskUtil.getExternalCacheDirPath(ContextUtil.getContext(), PATH)
                        val dir = File(path)
                        if (!dir.exists() || !dir.isDirectory) {
                            dir.mkdirs()
                        }
                        path = path + "/YUV_" + System.currentTimeMillis() + ".yuv"
                        var fos: FileOutputStream? = null
                        try {
                            val file = File(path)
                            if (file.exists()) {
                                file.delete()
                            }
                            fos = FileOutputStream(file)
                            fos.write(it, 0, it.size)
                        } catch (e: Exception) {
                            LogUtils.e(TAG, e.message)
                        } finally {
                            fos?.let {
                                fos.flush()
                                fos.close()
                            }
                        }
                        saveYuvData(mediaFormat, data, width, height)
                    }
                })
            }
        }
    }

    private fun saveYuvData(mediaFormat: MediaFormat?, data: ByteArray?, width: Int, height: Int) {
        data?.let {
            mediaFormat?.let {
                when (it.getInteger(MediaFormat.KEY_COLOR_FORMAT)) {
                    0, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar -> {
                        newSaveYuvDataToJPEG(data, width, height)
                    }

                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar -> {
                        newSaveYuvDataToJPEG420P(data, width, height)
                    }
                }
            }
        }
    }

    private fun handlerYUV() {
        if (binding?.horizontalScrollView?.yuvScreenShot?.isSelected == false) {
            binding?.horizontalScrollView?.yuvScreenShot?.setText(R.string.btn_resume_video)
            binding?.horizontalScrollView?.yuvScreenShot?.isSelected = true
            binding?.yuvScreenSavePath?.text = ""
            binding?.yuvScreenSavePath?.visibility = View.VISIBLE
            videoDecoder?.let {
                videoDecoder!!.onPause()
                videoDecoder!!.destroy()
                videoDecoder = null
            }
            videoDecoder = VideoDecoder(
                this@VideoChannelFragment.context,
                channelVM.videoChannel!!.videoChannelType
            )
            videoDecoder?.addDecoderStateChangeListener(decoderStateChangeListener)
            decoderStateChangeListener.onUpdate(videoDecoder?.decoderStatus,videoDecoder?.decoderStatus)
            videoDecoder?.addYuvDataListener(this)
            videoDecoder?.onResume()
        } else {
            stringBuilder?.let {
                stringBuilder!!.clear()
            }

            binding?.horizontalScrollView?.yuvScreenShot?.setText(R.string.btn_yuv_screen_shot)
            binding?.horizontalScrollView?.yuvScreenShot?.isSelected = false
            binding?.yuvScreenSavePath?.setText("")
            binding?.yuvScreenSavePath?.visibility = View.INVISIBLE
            videoDecoder?.let {
                videoDecoder!!.onPause()
                videoDecoder!!.destroy()
                videoDecoder = null
            }
            videoDecoder = VideoDecoder(
                this@VideoChannelFragment.context,
                channelVM.videoChannel!!.videoChannelType,
                DecoderOutputMode.SURFACE_MODE,
                surfaceView.holder,
                surfaceView.width,
                surfaceView.height
            )
            videoDecoder?.addDecoderStateChangeListener(decoderStateChangeListener)
            decoderStateChangeListener.onUpdate(videoDecoder?.decoderStatus,videoDecoder?.decoderStatus)
            videoDecoder?.removeYuvDataListener(this)
            videoDecoder?.onResume()

        }
    }

    private fun newSaveYuvDataToJPEG420P(yuvFrame: ByteArray, width: Int, height: Int) {
        if (yuvFrame.size < width * height) {
            return
        }
        val length = width * height
        val u = ByteArray(width * height / 4)
        val v = ByteArray(width * height / 4)
        for (i in u.indices) {
            u[i] = yuvFrame[length + i]
            v[i] = yuvFrame[length + u.size + i]
        }
        for (i in u.indices) {
            yuvFrame[length + 2 * i] = v[i]
            yuvFrame[length + 2 * i + 1] = u[i]
        }
        screenShot(
            yuvFrame,
            DiskUtil.getExternalCacheDirPath(ContextUtil.getContext(), PATH),
            width,
            height
        )
    }

    private fun newSaveYuvDataToJPEG(yuvFrame: ByteArray, width: Int, height: Int) {
        if (yuvFrame.size < width * height) {
            return
        }
        val length = width * height
        val u = ByteArray(width * height / 4)
        val v = ByteArray(width * height / 4)
        for (i in u.indices) {
            v[i] = yuvFrame[length + 2 * i]
            u[i] = yuvFrame[length + 2 * i + 1]
        }
        for (i in u.indices) {
            yuvFrame[length + 2 * i] = u[i]
            yuvFrame[length + 2 * i + 1] = v[i]
        }
        screenShot(
            yuvFrame,
            DiskUtil.getExternalCacheDirPath(ContextUtil.getContext(), PATH),
            width,
            height
        )
    }

    private fun screenShot(buf: ByteArray, shotDir: String, width: Int, height: Int) {
        val dir = File(shotDir)
        if (!dir.exists() || !dir.isDirectory) {
            dir.mkdirs()
        }
        val yuvImage = YuvImage(
            buf,
            ImageFormat.NV21,
            width,
            height,
            null
        )
        val outputFile: OutputStream
        val path = dir.toString() + "/ScreenShot_" + System.currentTimeMillis() + ".jpeg"
        outputFile = try {
            FileOutputStream(File(path))
        } catch (e: FileNotFoundException) {
            LogUtils.e(LogPath.SAMPLE, "screenShot: new bitmap output file error: $e")
            return
        }
        yuvImage.compressToJpeg(
            Rect(
                0,
                0,
                width,
                height
            ), 100, outputFile
        )
        try {
            outputFile.close()
        } catch (e: IOException) {
            LogUtils.e(LogPath.SAMPLE, "test screenShot: compress yuv image error: ${e.message}")
        }
        Message.obtain(mHandler, DISPLAY, path).sendToTarget()
    }

    private fun display(path: String) {
        stringBuilder?.let {
            it.insert(0, path)
            it.insert(0, "\n")
            binding?.yuvScreenSavePath?.text = it
        }
    }

    private fun showDisconnectToast() {
        ToastUtils.showShortToast("video stream disconnect")
    }
}