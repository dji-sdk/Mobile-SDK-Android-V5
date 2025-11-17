package dji.sampleV5.aircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.utils.CallbackUtils
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.interfaces.ILiveStreamManager
import dji.v5.manager.datacenter.livestream.*
import dji.v5.manager.datacenter.livestream.settings.AgoraSettings
import dji.v5.manager.datacenter.livestream.settings.GB28181Settings
import dji.v5.manager.datacenter.livestream.settings.RtmpSettings
import dji.v5.manager.datacenter.livestream.settings.RtspSettings
import dji.v5.manager.interfaces.ICameraStreamManager
import dji.v5.utils.common.ContextUtil
import dji.v5.utils.common.DjiSharedPreferencesManager

/**
 * ClassName : LiveStreamVM
 * Description : 直播VM
 * Author : daniel.chen
 * CreateDate : 2022/3/23 11:04 上午
 * Copyright : ©2022 DJI All Rights Reserved.
 */
class LiveStreamVM : DJIViewModel() {
    private val availableCameraUpdatedListener: ICameraStreamManager.AvailableCameraUpdatedListener
    private val liveStreamStatusListener: LiveStreamStatusListener
    private val RTMP_KEY = "livestream-rtmp"
    private val RTSP_KEY = "livestream-rtsp"
    private val GB28181_KEY = "livestream-gb28181"
    private val AGORA_KEY = "livestream-agora"
    val liveStreamStatus = MutableLiveData<LiveStreamStatus?>()
    val liveStreamError = MutableLiveData<IDJIError?>()
    val availableCameraList = MutableLiveData<List<ComponentIndexType>>()
    val streamManager: ILiveStreamManager = MediaDataCenter.getInstance().liveStreamManager
    val cameraManager: ICameraStreamManager = MediaDataCenter.getInstance().cameraStreamManager

    init {
        liveStreamStatusListener = object : LiveStreamStatusListener {
            override fun onLiveStreamStatusUpdate(status: LiveStreamStatus?) {
                status?.let {
                    liveStreamStatus.postValue(it)
                }
            }

            override fun onError(error: IDJIError?) {
                error?.let {
                    liveStreamError.postValue(it)
                }
            }
        }

        availableCameraUpdatedListener = object :ICameraStreamManager.AvailableCameraUpdatedListener {
            override fun onAvailableCameraUpdated(list: MutableList<ComponentIndexType>) {
                availableCameraList.postValue(list)
            }

            override fun onCameraStreamEnableUpdate(cameraStreamEnableMap: MutableMap<ComponentIndexType, Boolean>) {

            }
        }

        addListener()
    }

    override fun onCleared() {
        super.onCleared()
        removeListener()
    }

    private fun reset() {
        liveStreamError.postValue(null)
        liveStreamStatus.postValue(null)
    }

    fun startStream(callback: CommonCallbacks.CompletionCallback?) {
        streamManager.startStream(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                CallbackUtils.onSuccess(callback)
                reset()
            }

            override fun onFailure(error: IDJIError) {
                CallbackUtils.onFailure(callback, error)
            }

        })
    }

    fun stopStream(callback: CommonCallbacks.CompletionCallback?) {
        streamManager.stopStream(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                CallbackUtils.onSuccess(callback)
                reset()
            }

            override fun onFailure(error: IDJIError) {
                CallbackUtils.onFailure(callback, error)
            }

        })
    }

    fun isStreaming(): Boolean {
        return streamManager.isStreaming;
    }

    fun setCameraIndex(cameraIndex: ComponentIndexType) {
        streamManager.cameraIndex = cameraIndex
    }

    fun setLiveStreamConfig(liveStreamSettings: LiveStreamSettings) {
        streamManager.liveStreamSettings = liveStreamSettings;
    }

    fun setLiveStreamQuality(liveStreamQuality: StreamQuality) {
        streamManager.liveStreamQuality = liveStreamQuality
    }

    fun setLiveStreamScaleType(scaleType: ICameraStreamManager.ScaleType) {
        streamManager.liveStreamScaleType = scaleType
    }

    fun setLiveVideoBitRateMode(bitRateMode: LiveVideoBitrateMode) {
        streamManager.liveVideoBitrateMode = bitRateMode
    }

    fun setLiveVideoBitRate(bitrate: Int) {
        streamManager.liveVideoBitrate = bitrate
    }

    fun setRTMPConfig(rtmpUrl: String) {
        val liveStreamConfig = LiveStreamSettings.Builder()
            .setLiveStreamType(LiveStreamType.RTMP)
            .setRtmpSettings(
                RtmpSettings.Builder()
                    .setUrl(rtmpUrl)
                    .build()
            )
            .build()
        DjiSharedPreferencesManager.putString(ContextUtil.getContext(), RTMP_KEY, rtmpUrl)
        setLiveStreamConfig(liveStreamConfig)
    }

    fun getRtmpUrl(): String {
        return DjiSharedPreferencesManager.getString(ContextUtil.getContext(), RTMP_KEY, "")
    }

    fun setRTSPConfig(userName: String, password: String, port: Int) {
        val rtspConfig = RtspSettings.Builder()
            .setUserName(userName)
            .setPassWord(password)
            .setPort(port)
            .build()
        val liveStreamConfig = LiveStreamSettings.Builder()
            .setLiveStreamType(LiveStreamType.RTSP)
            .setRtspSettings(rtspConfig)
            .build()
        val rtspSettings = userName + "^_^" + password + "^_^" + port.toString()
        DjiSharedPreferencesManager.putString(ContextUtil.getContext(), RTSP_KEY, rtspSettings)
        setLiveStreamConfig(liveStreamConfig)
    }

    fun getRtspSettings(): String {
        return DjiSharedPreferencesManager.getString(ContextUtil.getContext(), RTSP_KEY, "")
    }

    fun setGB28181(
        serverIP: String,
        serverPort: Int,
        serverID: String,
        agentID: String,
        channel: String,
        localPort: Int,
        password: String
    ) {
        val gb28181Config = GB28181Settings.Builder()
            .setServerIP(serverIP)
            .setServerPort(serverPort)
            .setServerID(serverID)
            .setAgentID(agentID)
            .setChannel(channel)
            .setLocalPort(localPort)
            .setPassword(password)
            .build()
        val liveStreamConfig = LiveStreamSettings.Builder()
            .setLiveStreamType(LiveStreamType.GB28181)
            .setGB28181Settings(gb28181Config)
            .build()
        val gb28181Settings =
            serverIP + "^_^" + serverPort.toString() + "^_^" + serverID + "^_^" + agentID + "^_^" + channel + "^_^" + localPort.toString() + "^_^" + password
        DjiSharedPreferencesManager.putString(
            ContextUtil.getContext(),
            GB28181_KEY,
            gb28181Settings
        )
        setLiveStreamConfig(liveStreamConfig)
    }

    fun getGb28181Settings(): String {
        return DjiSharedPreferencesManager.getString(ContextUtil.getContext(), GB28181_KEY, "")
    }

    fun setAgoraConfig(channelId: String, token: String, uid: String) {
        val agoraConfig = AgoraSettings.Builder()
            .setChannelId(channelId)
            .setToken(token)
            .setUid(uid)
            .setEnableSafety(false)
            .build()
        val liveStreamConfig = LiveStreamSettings.Builder()
            .setLiveStreamType(LiveStreamType.AGORA)
            .setAgoraSettings(agoraConfig)
            .build()
        val agoraSettings = channelId + "^_^" + token + "^_^" + uid
        DjiSharedPreferencesManager.putString(
            ContextUtil.getContext(),
            AGORA_KEY,
            agoraSettings
        )
        setLiveStreamConfig(liveStreamConfig)
    }

    fun getAgoraSettings(): String {
        return DjiSharedPreferencesManager.getString(ContextUtil.getContext(), AGORA_KEY, "")
    }

    fun addListener() {
        streamManager.addLiveStreamStatusListener(liveStreamStatusListener)
        cameraManager.addAvailableCameraUpdatedListener(availableCameraUpdatedListener)
    }

    fun removeListener() {
        streamManager.removeLiveStreamStatusListener(liveStreamStatusListener)
        cameraManager.removeAvailableCameraUpdatedListener(availableCameraUpdatedListener)
    }

    fun getAircraftStreamFrameInfo(cameraIndex: ComponentIndexType) = cameraManager.getAircraftStreamFrameInfo(cameraIndex)
}