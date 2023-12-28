package dji.sampleV5.aircraft.models

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.v5.common.video.channel.VideoChannelState
import dji.v5.common.video.channel.VideoChannelType
import dji.v5.common.video.interfaces.IVideoChannel
import dji.v5.common.video.interfaces.VideoChannelStateChangeListener
import dji.v5.common.video.stream.StreamSource
import dji.v5.et.cancelListen
import dji.v5.et.create
import dji.v5.et.listen
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.datacenter.video.StreamSourceListener
import java.util.concurrent.CopyOnWriteArrayList

class MultiVideoChannelVM : DJIViewModel() {
    val videoStreamSources = MutableLiveData<CopyOnWriteArrayList<StreamSource>>()
    val availableVideoStreamSources = MutableLiveData<CopyOnWriteArrayList<StreamSource>>()
    val handler = Handler(Looper.getMainLooper())
    val DELAY_TIME = 4000L
    var primaryChannel = MediaDataCenter.getInstance().videoStreamManager.getAvailableVideoChannel(
        VideoChannelType.PRIMARY_STREAM_CHANNEL
    )
    var secondaryChannel =
        MediaDataCenter.getInstance().videoStreamManager.getAvailableVideoChannel(
            VideoChannelType.SECONDARY_STREAM_CHANNEL
        )
    var curPrimarySource: StreamSource? = null
    var curSecondarySource: StreamSource? = null
    val primaryChannelStateListener = object : VideoChannelStateChangeListener {
        override fun onUpdate(from: VideoChannelState?, to: VideoChannelState?) {
            if (VideoChannelState.ON == to) {
                primaryChannel?.let { it ->
                    it.streamSource?.let {
                        removeStreamSource(it)
                        curPrimarySource = it
                    }
                }
            }
            if (VideoChannelState.CLOSE == to) {
                primaryChannel?.let { _ ->
                    curPrimarySource?.let { it1 -> addStreamSource(it1) }
                }
            }
            updateVideoChannel()
        }
    }
    val secondaryChannelStateListener = object : VideoChannelStateChangeListener {
        override fun onUpdate(from: VideoChannelState?, to: VideoChannelState?) {
            if (VideoChannelState.ON == to) {
                secondaryChannel?.let { it ->
                    it.streamSource?.let {
                        removeStreamSource(it)
                        curSecondarySource = it
                    }
                }
            }
            if (VideoChannelState.CLOSE == to) {
                //在这里close的时候streamSource会被清空，在vm中可以先暂存
                secondaryChannel?.let { _ ->
                    curSecondarySource?.let { it1 -> addStreamSource(it1) }
                }
            }
            updateVideoChannel()
        }
    }

    private val streamSourcesListener = StreamSourceListener {
        it?.let {
            availableVideoStreamSources.value = CopyOnWriteArrayList(it.toMutableList())
            updateVideoChannel()
        }
    }

    fun initMultiVideoChannels() {
        MediaDataCenter.getInstance().videoStreamManager.addStreamSourcesListener(
            streamSourcesListener
        )
    }

    fun removeStreamSourceListener() {
        MediaDataCenter.getInstance().videoStreamManager.removeStreamSourcesListener(
            streamSourcesListener
        )
    }

    fun resetMultiVideoChannels() {
        if (MediaDataCenter.getInstance().videoStreamManager != null) {
            MediaDataCenter.getInstance().videoStreamManager.resetAllVideoChannels()
        }
    }

    fun getAllVideoChannels(): List<IVideoChannel>? {
        return MediaDataCenter.getInstance().videoStreamManager?.let {
            MediaDataCenter.getInstance().videoStreamManager.availableVideoChannels
        } ?: let {
            null
        }
    }

    /**
     * Listen channel的开关状态，及时更新可用streamSources，已经被开启的channel所使用的source需要从videoStreamSources移除，被关闭的需要添加
     */
    fun initChannelStateListener() {
        primaryChannel?.addVideoChannelStateChangeListener(primaryChannelStateListener)
        secondaryChannel?.addVideoChannelStateChangeListener(secondaryChannelStateListener)
    }

    fun removeChannelStateListener() {
        primaryChannel?.removeVideoChannelStateChangeListener(primaryChannelStateListener)
        secondaryChannel?.removeVideoChannelStateChangeListener(secondaryChannelStateListener)
    }

    fun removeStreamSource(source: StreamSource) {
        val targetSource = videoStreamSources.value!!.find { it.streamId == source.streamId }
        targetSource?.let {
            videoStreamSources.value!!.remove(it)
        }
        videoStreamSources.postValue(videoStreamSources.value)
    }

    fun addStreamSource(source: StreamSource) {
        for (i in 0 until videoStreamSources.value!!.size) {
            if (videoStreamSources.value!![i].streamId == source.streamId) {
                return
            }
        }
        videoStreamSources.value!!.add(source)
        videoStreamSources.postValue(videoStreamSources.value)
    }

    fun addConnectionListener() {
        FlightControllerKey.KeyConnection.create().listen(this) {
            it?.let {
                if (it) {
                    reset()
                }
            }
        }
    }

    fun removeConnectionListener() {
        FlightControllerKey.KeyConnection.create().cancelListen(this)
    }

    private fun reset() {
        primaryChannel = MediaDataCenter.getInstance().videoStreamManager.getAvailableVideoChannel(
            VideoChannelType.PRIMARY_STREAM_CHANNEL
        )
        secondaryChannel =
            MediaDataCenter.getInstance().videoStreamManager.getAvailableVideoChannel(
                VideoChannelType.SECONDARY_STREAM_CHANNEL
            )
        removeChannelStateListener()
        initChannelStateListener()
        removeStreamSourceListener()
        initMultiVideoChannels()
        videoStreamSources.postValue(videoStreamSources.value)
    }

    private fun updateVideoChannel() {
        handler.post {
            availableVideoStreamSources.value?.let { availableVideoStreamSourceList ->
                videoStreamSources.value = CopyOnWriteArrayList(availableVideoStreamSourceList)
                val allChannels = getAllVideoChannels()
                allChannels?.let {
                    it.forEach {
                        if (it.videoChannelStatus == VideoChannelState.ON) {
                            /**
                             * 更换相机时重新组织videoStreamSources，去除掉channel已经开启的部分
                             */
                            videoStreamSources.value!!.forEach { v ->
                                run {
                                    if (v.streamId == it?.streamSource?.streamId) {
                                        videoStreamSources.value!!.remove(v)
                                    }
                                }
                            }
                        }
                    }
                }
                videoStreamSources.postValue(videoStreamSources.value)
            }
        }
        //更换相机时触发自动分配StreamSource，需要给一定delayTime
    }
}