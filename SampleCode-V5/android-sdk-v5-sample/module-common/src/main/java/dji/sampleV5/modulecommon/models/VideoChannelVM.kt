package dji.sampleV5.modulecommon.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.modulecommon.data.VideoChannelInfo
import dji.v5.common.video.interfaces.IVideoChannel
import dji.v5.common.video.channel.VideoChannelState
import dji.v5.common.video.interfaces.VideoChannelStateChangeListener
import dji.v5.common.video.channel.VideoChannelType
import dji.v5.manager.datacenter.MediaDataCenter

class VideoChannelVM(channelType: VideoChannelType) : DJIViewModel() {
    val videoChannelInfo = MutableLiveData<VideoChannelInfo>()
    var videoChannel: IVideoChannel? = null
    var videoChannelStateListener: VideoChannelStateChangeListener? = null


    init {
        MediaDataCenter.getInstance().videoStreamManager.getAvailableVideoChannel(channelType)
            ?.let {
                videoChannel = it
                if (videoChannelInfo.value == null) {
                    videoChannelInfo.value = VideoChannelInfo(videoChannel!!.videoChannelStatus)
                }
                videoChannelInfo.value?.streamSource = videoChannel!!.streamSource
                videoChannelInfo.value?.videoChannelType = videoChannel!!.videoChannelType
                videoChannelInfo.value?.format = videoChannel!!.streamFormat.name
                initListeners()
                videoChannelStateListener = VideoChannelStateChangeListener { _, to ->
                    /**
                     * 码流通道切换事件回调方法
                     *
                     * @param from 码流通道前一个状态
                     * @param to 码流通道当前状态
                     */
                    if (videoChannelInfo.value == null) {
                        videoChannelInfo.value = VideoChannelInfo(to)
                    } else {
                        videoChannelInfo.value?.videoChannelState = to
                    }
                    if (to == VideoChannelState.ON || to == VideoChannelState.SOCKET_ON) {
                        videoChannelInfo.value?.streamSource = videoChannel!!.streamSource
                    } else {
                        videoChannelInfo.value?.streamSource = null
                    }
                    refreshVideoChannelInfo()
                }
            }
    }

    override fun onCleared() {
        removeListeners()
    }

    private fun initListeners() {
        videoChannel?.let {
            it.addVideoChannelStateChangeListener(videoChannelStateListener)
        }
    }

    private fun removeListeners() {
        videoChannel?.let {
            it.removeVideoChannelStateChangeListener(videoChannelStateListener)
        }
    }

    fun refreshVideoChannelInfo() {
        videoChannelInfo.postValue(videoChannelInfo.value)
    }
}