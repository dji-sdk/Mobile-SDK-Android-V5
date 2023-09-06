package dji.sampleV5.aircraft.video
import androidx.fragment.app.activityViewModels
import dji.sampleV5.modulecommon.models.LiveStreamVM
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.v5.common.callback.CommonCallbacks
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.common.error.IDJIError
import dji.v5.manager.datacenter.livestream.LiveStreamStatus
import dji.v5.manager.datacenter.livestream.LiveStreamStatusListener
import dji.v5.utils.common.ToastUtils

class StreamManager:DJIFragment() {
    private val liveStreamVM: LiveStreamVM by activityViewModels()
    private val manager = MediaDataCenter.getInstance().liveStreamManager
    private val listener = this.manager.addLiveStreamStatusListener(streamer)
    val RtspUsername = "admin"
    val RtspPassword = "admin"
    val RtspPort = 8554
    var isStreaming = this.manager.isStreaming
    var streamSettings = this.manager.liveStreamSettings


    object streamer : LiveStreamStatusListener {
        override fun onLiveStreamStatusUpdate(status: LiveStreamStatus?) {
            TODO("Not yet implemented")
        }

        override fun onError(error: IDJIError?) {
            TODO("Not yet implemented")
        }

    }


    fun startStream() {
        //If the stream is not already started, start it
        if (!isStreaming) {
            // Setup settings
            try {
                liveStreamVM.setRTSPConfig(
                    RtspUsername,
                    RtspPassword,
                    RtspPort
                )
            } catch (e: NumberFormatException) {
                ToastUtils.showToast("RTSP port must be int value")
            }
            manager.startStream(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("Stream started on " +
                            streamSettings.rtspSettings.userName + "  " +
                            streamSettings.rtspSettings.password + "  " +
                            streamSettings.rtspSettings.port)
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("Stream start failed: $error")
                }
            })
        }
    }

}