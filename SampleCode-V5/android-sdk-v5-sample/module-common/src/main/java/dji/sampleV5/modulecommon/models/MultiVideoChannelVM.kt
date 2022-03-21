package dji.sampleV5.modulecommon.models

import androidx.lifecycle.MutableLiveData
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.video.interfaces.IVideoFrame
import dji.v5.common.video.interfaces.StreamDataListener
import dji.v5.common.video.stream.StreamSource
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.datacenter.video.StreamSourceListener
import dji.v5.utils.common.LogUtils

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
}