package dji.sampleV5.aircraft.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dji.v5.common.video.channel.VideoChannelType

class VideoChannelVMFactory (channelType: VideoChannelType): ViewModelProvider.Factory {
    private var videoChannelType: VideoChannelType = channelType

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(p0: Class<T>): T {
        return VideoChannelVM(videoChannelType) as T
    }
}