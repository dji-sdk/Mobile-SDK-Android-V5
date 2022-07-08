package dji.sampleV5.modulecommon.models

import androidx.lifecycle.MutableLiveData
import dji.v5.common.video.interfaces.IVideoChannel
import dji.v5.common.video.stream.StreamSource
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.datacenter.video.StreamSourceListener

class MultiVideoChannelVM : DJIViewModel() {
    val videoStreamSources = MutableLiveData<List<StreamSource>>()
    private val streamSourcesListener = StreamSourceListener {
        it?.let {
            videoStreamSources.value = it
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
}