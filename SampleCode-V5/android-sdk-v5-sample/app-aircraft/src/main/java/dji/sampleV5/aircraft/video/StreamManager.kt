package dji.sampleV5.aircraft.video
import android.util.Log
import androidx.fragment.app.activityViewModels
import dji.sampleV5.modulecommon.models.LiveStreamVM
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.v5.common.callback.CommonCallbacks
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.common.error.IDJIError
import dji.v5.common.video.channel.VideoChannelState
import dji.v5.common.video.channel.VideoChannelType
import dji.v5.common.video.interfaces.IVideoChannel
import dji.v5.manager.datacenter.livestream.LiveStreamSettings
import dji.v5.manager.datacenter.livestream.LiveStreamStatus
import dji.v5.manager.datacenter.livestream.LiveStreamStatusListener
import dji.v5.manager.datacenter.livestream.LiveStreamType
import dji.v5.manager.datacenter.livestream.settings.RtspSettings
import dji.v5.utils.common.ToastUtils
import java.net.Inet4Address
import java.net.NetworkInterface

class StreamManager() {
    private val manager = MediaDataCenter.getInstance().liveStreamManager
    private val listener = this.manager.addLiveStreamStatusListener(streamer)
    private var isStreaming: Boolean = this.manager.isStreaming
    val userName = "rinao"
    val password = "unicorn"
    val port = 8554
    var streamSettings = this.manager.liveStreamSettings


    object streamer : LiveStreamStatusListener {
        override fun onLiveStreamStatusUpdate(status: LiveStreamStatus?) {
            Log.v("StreamManager", "Stream status: ${status?.isStreaming}")

        }

        override fun onError(error: IDJIError?) {
            Log.e("StreamManager", "Stream error: ${error.toString()}")
        }

    }

    fun isStreaming(): Boolean {
        return this.isStreaming
    }

    fun startStream() {
        Log.v("StreamManager", "Start stream")
        //If the stream is not already started, start it
        if (!isStreaming) {
            // Setup settings
            try {
                val rtspConfig = RtspSettings.Builder()
                    .setUserName(userName)
                    .setPassWord(password)
                    .setPort(port)
                    .build()
                val liveStreamConfig = LiveStreamSettings.Builder()
                    .setLiveStreamType(LiveStreamType.RTSP)
                    .setRtspSettings(rtspConfig)
                    .build()
                manager.liveStreamSettings = liveStreamConfig
            } catch (e: NumberFormatException) {
                ToastUtils.showToast("RTSP port must be int value")
            }
            manager.videoChannelType = VideoChannelType.SECONDARY_STREAM_CHANNEL
            manager.startStream(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.v("StreamManager", "Stream started on " +
                            manager.liveStreamSettings?.rtspSettings?.userName + "  " +
                            manager.liveStreamSettings?.rtspSettings?.password + "  " +
                            manager.liveStreamSettings?.rtspSettings?.port)
                    isStreaming = true;
                    ToastUtils.showToast("Livestream Started")
                }

                override fun onFailure(error: IDJIError) {
                    Log.e("StreamManager", "Stream failed to start: ${error.toString()}")
                }
            })
        }
        else{
            manager.stopStream(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.v("StreamManager", "Stream stopped")
                    ToastUtils.showToast("Livestream Stopped")
                    isStreaming = false;
                }

                override fun onFailure(error: IDJIError) {
                    Log.e("StreamManager", "Stream failed to stop: ${error.toString()}")
                }
            })
        }
    }

    fun initChannelStateListener(){
        val primaryChannel: IVideoChannel? = MediaDataCenter.getInstance().videoStreamManager.getAvailableVideoChannel(VideoChannelType.PRIMARY_STREAM_CHANNEL)
        if (primaryChannel != null) {
            val primaryChannelStateListener: (VideoChannelState, VideoChannelState) -> Unit = { from, to ->
                val primaryStreamSource = primaryChannel.streamSource
                Log.v("StreamManager", "Primary channel state changed from $from to $to")
                if (VideoChannelState.ON == to && primaryStreamSource != null) {
                    startStream()

               }
            }

            primaryChannel.addVideoChannelStateChangeListener(primaryChannelStateListener)
        }

    }

    private fun getLocalIPAddress(): String? {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val networkInterface = en.nextElement()
                val enu = networkInterface.inetAddresses
                while (enu.hasMoreElements()) {
                    val inetAddress = enu.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.getHostAddress()
                    }
                }
            }
        } catch (ex: Exception) {
            Log.v("StreamManager", "Failed to get IP address: $ex")
            ex.printStackTrace()
        }

        return null
    }
    fun getStreamURL(): String {
        val ip = getLocalIPAddress()
        val port = manager.liveStreamSettings?.rtspSettings?.port
        val username = manager.liveStreamSettings?.rtspSettings?.userName
        val password = manager.liveStreamSettings?.rtspSettings?.password
        Log.v("StreamManager", "Stream URL: rtsp://$username:$password@$ip:$port:streaming/live/1")
        return "rtsp://$username:$password@$ip:$port/streaming/live/1"
    }

}