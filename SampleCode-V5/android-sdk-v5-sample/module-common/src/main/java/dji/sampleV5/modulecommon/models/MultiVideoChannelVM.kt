package dji.sampleV5.modulecommon.models

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

class MultiVideoChannelVM : DJIViewModel() {
    val videoStreamSources = MutableLiveData<MutableList<StreamSource>>()
    val availableVideoStreamSources = MutableLiveData<MutableList<StreamSource>>()
    val handler = Handler(Looper.getMainLooper())
    val DELAY_TIME = 4000L
    var primaryChannel = MediaDataCenter.getInstance().videoStreamManager.getAvailableVideoChannel(VideoChannelType.PRIMARY_STREAM_CHANNEL)
    var secondaryChannel = MediaDataCenter.getInstance().videoStreamManager.getAvailableVideoChannel(VideoChannelType.SECONDARY_STREAM_CHANNEL)
    val primaryChannelStateListener = object : VideoChannelStateChangeListener {
        override fun onUpdate(from: VideoChannelState?, to: VideoChannelState?) {
            if (VideoChannelState.ON == to) {
                primaryChannel?.let { it ->
                    it.streamSource?.let {
                        removeStreamSource(it)
                    }
                }
            }
            if (VideoChannelState.CLOSE == to) {
                primaryChannel?.let { it ->
                    it.streamSource?.let {
                        addStreamSource(it)
                    }
                }
            }
        }
    }
    val secondaryChannelStateListener = object : VideoChannelStateChangeListener {
        override fun onUpdate(from: VideoChannelState?, to: VideoChannelState?) {
            if (VideoChannelState.ON == to) {
                secondaryChannel?.let { it ->
                    it.streamSource?.let {
                        removeStreamSource(it)
                    }
                }
            }
            if (VideoChannelState.CLOSE == to) {
                secondaryChannel?.let { it ->
                    it.streamSource?.let {
                        addStreamSource(it)
                    }
                }
            }
        }
    }
    private val streamSourcesListener = StreamSourceListener {
        it?.let {
            availableVideoStreamSources.value = it.toMutableList()
            videoStreamSources.value = it.toMutableList()
            handler.postDelayed({
                for (i in 0 until (getAllVideoChannels()?.size ?: 0)) {
                    if (getAllVideoChannels()?.get(i)?.videoChannelStatus == VideoChannelState.ON) {
                        /**
                         * 更换相机时重新组织videoStreamSources，去除掉channel已经开启的部分
                         */
                        val iterator = videoStreamSources.value!!.iterator()
                        while (iterator.hasNext()) {
                            val streamSource = iterator.next()
                            if (streamSource.streamId == getAllVideoChannels()?.get(i)?.streamSource?.streamId) {
                                iterator.remove()
                            }
                        }
                    }
                }
                videoStreamSources.postValue(videoStreamSources.value)
            }, DELAY_TIME)
            //更换相机时触发自动分配StreamSource，需要给一定delayTime

            availableVideoStreamSources.postValue(availableVideoStreamSources.value)
        }
    }

    fun initMultiVideoChannels() {
        MediaDataCenter.getInstance().videoStreamManager.addStreamSourcesListener(streamSourcesListener)
    }

    fun removeStreamSourceListener(){
        MediaDataCenter.getInstance().videoStreamManager.removeStreamSourcesListener(streamSourcesListener)
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

    fun removeStreamSource(source:StreamSource){
        val iterator = videoStreamSources.value!!.iterator()
        while (iterator.hasNext()) {
            val streamSource = iterator.next()
            if (streamSource.streamId == source.streamId) {
                iterator.remove()
            }
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
                if (it){
                    reset()
                }
            }
        }
    }

    fun removeConnectionListener(){
        FlightControllerKey.KeyConnection.create().cancelListen(this)
    }

    private fun reset(){
        primaryChannel = MediaDataCenter.getInstance().videoStreamManager.getAvailableVideoChannel(VideoChannelType.PRIMARY_STREAM_CHANNEL)
        secondaryChannel = MediaDataCenter.getInstance().videoStreamManager.getAvailableVideoChannel(VideoChannelType.SECONDARY_STREAM_CHANNEL)
        removeChannelStateListener()
        initChannelStateListener()
        removeStreamSourceListener()
        initMultiVideoChannels()
        videoStreamSources.postValue(videoStreamSources.value)
    }
}