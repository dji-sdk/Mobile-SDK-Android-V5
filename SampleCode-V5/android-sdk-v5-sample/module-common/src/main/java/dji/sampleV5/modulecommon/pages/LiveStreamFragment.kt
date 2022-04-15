package dji.sampleV5.modulecommon.pages

import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import dji.sampleV5.modulecommon.R
import dji.sampleV5.modulecommon.models.LiveStreamVM
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.video.channel.VideoChannelType
import dji.v5.common.video.decoder.DecoderOutputMode
import dji.v5.common.video.decoder.DecoderState
import dji.v5.common.video.decoder.VideoDecoder
import dji.v5.common.video.interfaces.IVideoDecoder
import dji.v5.manager.datacenter.livestream.LiveStreamType
import dji.v5.manager.datacenter.livestream.LiveVideoBitrateMode
import dji.v5.manager.datacenter.livestream.StreamQuality
import dji.v5.utils.common.StringUtils
import kotlinx.android.synthetic.main.frag_live_stream_page.*

/**
 * ClassName : LiveStreamFragment
 * Description : 直播功能
 * Author : daniel.chen
 * CreateDate : 2022/3/23 10:58 上午
 * Copyright : ©2022 DJI All Rights Reserved.
 */
class LiveStreamFragment:DJIFragment(), View.OnClickListener,SurfaceHolder.Callback{
    private val liveStreamVM:LiveStreamVM by activityViewModels()
    private var videoDecoder: IVideoDecoder? = null
    private lateinit var surfaceView: SurfaceView
    private lateinit var dialog: AlertDialog
    private lateinit var configDialog: AlertDialog
    private var checkedItem: Int = -1
    private var liveStreamType:LiveStreamType = LiveStreamType.UNKNOWN
    private var liveStreamChannelType:VideoChannelType = VideoChannelType.PRIMARY_STREAM_CHANNEL
    private var liveStreamBitrateMode:LiveVideoBitrateMode = LiveVideoBitrateMode.AUTO
    private var liveStreamQuality:StreamQuality = StreamQuality.UNKNOWN
    private val msg = "input is null"

    var fps:Int=-1
    var vbps:Int=-1
    var isStreaming:Boolean=false
    var resolution_w:Int=-1
    var resolution_h:Int=-1
    var packet_loss:Int =-1
    var packet_cache_len:Int =-1
    var rtt:Int =-1
    var error:IDJIError?=null

    var curWidth: Int = -1
    var curHeight: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_live_stream_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
        initListener()
        initLiveStreamInfo()
    }

    private fun initView(view: View) {
        surfaceView = view.findViewById(R.id.live_stream_surface_view)
        surfaceView.holder.addCallback(this)
    }

    private fun initListener() {
        btn_set_live_stream_config.setOnClickListener(this)
        btn_get_live_stream_config.setOnClickListener(this)
        btn_start_live_stream.setOnClickListener(this)
        btn_stop_live_stream.setOnClickListener(this)
        btn_set_live_stream_channel_type.setOnClickListener(this)
        btn_get_live_stream_channel_type.setOnClickListener(this)
        btn_set_live_stream_quality.setOnClickListener(this)
        btn_get_live_stream_quality.setOnClickListener(this)
        btn_set_live_stream_bit_rate_mode.setOnClickListener(this)
        btn_get_live_stream_bit_rate_mode.setOnClickListener(this)
        btn_set_live_stream_bit_rate.setOnClickListener(this)
        btn_get_live_stream_bit_rate.setOnClickListener(this)
        btn_enable_audio.setOnClickListener(this)
        btn_disable_audio.setOnClickListener(this)
    }

    private fun initLiveStreamInfo() {
        liveStreamVM.refreshLiveStreamError()
        liveStreamVM.refreshLiveStreamStatus()
        liveStreamVM.curLiveStreanmStatus.observe(viewLifecycleOwner) {
            it?.let {
                fps = it.fps
                vbps = it.vbps
                isStreaming = it.isStreaming
                resolution_w = it.resolution?.width!!
                resolution_h = it.resolution?.height!!
                packet_loss = it.packetLoss
                packet_cache_len = it.packetCacheLen
                rtt = it.rtt
                updateLiveStreamInfo()
            }
        }

        liveStreamVM.curLiveStreamError.observe(viewLifecycleOwner){
            it?.let {
                error = it
                updateLiveStreamInfo()
            }
        }
    }

    private fun updateLiveStreamInfo(){
        val liveStreamInfo = "\nfps: ${fps}fps \n" +
                "vbps: ${vbps}Kbps \n" +
                "isStreaming: $isStreaming \n" +
                "resolution_w: $resolution_w \n" +
                "resolution_h: $resolution_h \n" +
                "packet_loss: ${packet_loss}% \n" +
                "packet_cache_len: $packet_cache_len \n" +
                "rtt: ${rtt}ms \n"+
                "error: $error"
        tv_live_stream_info.text = liveStreamInfo
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_set_live_stream_config -> {
                showSetLiveStreamConfigDialog()
            }

            R.id.btn_get_live_stream_config -> {
                val streamConfig = liveStreamVM.getLiveStreamConfig()?.let { liveStreamVM.getLiveStreamConfig().toString() }?:let { "null" }
                ToastUtils.showToast(streamConfig)
            }

            R.id.btn_start_live_stream -> {
                liveStreamVM.startStream(object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        ToastUtils.showToast(StringUtils.getResStr(R.string.msg_start_live_stream_success))
                    }

                    override fun onFailure(error: IDJIError) {
                        ToastUtils.showToast(
                            StringUtils.getResStr(
                                R.string.msg_start_live_stream_failed,
                                error.description()
                            )
                        )
                    }
                })
            }

            R.id.btn_stop_live_stream -> {
                liveStreamVM.stopStream(object:CommonCallbacks.CompletionCallback{
                    override fun onSuccess() {
                        ToastUtils.showToast(StringUtils.getResStr(R.string.msg_stop_live_stream_success))
                    }

                    override fun onFailure(error: IDJIError) {
                        ToastUtils.showToast(
                            StringUtils.getResStr(
                                R.string.msg_stop_live_stream_failed,
                                error.description()
                            )
                        )
                    }
                })
            }

            R.id.btn_set_live_stream_channel_type -> {
                showSetLiveStreamChannelTypeDialog()
            }

            R.id.btn_get_live_stream_channel_type -> {
                ToastUtils.showToast(liveStreamVM.getVideoChannel().name)
            }


            R.id.btn_set_live_stream_quality -> {
                showSetLiveStreamQualityDialog()
            }

            R.id.btn_get_live_stream_quality -> {
                ToastUtils.showToast(liveStreamVM.getLiveStreamQuality().name)
            }

            R.id.btn_set_live_stream_bit_rate_mode -> {
                showSetLiveStreamBitRateModeDialog()
            }

            R.id.btn_get_live_stream_bit_rate_mode -> {
                ToastUtils.showToast(liveStreamVM.getLiveVideoBitRateMode().name)
            }

            R.id.btn_set_live_stream_bit_rate -> {
                showSetLiveStreamBitrateDialog()
            }

            R.id.btn_get_live_stream_bit_rate -> {
                ToastUtils.showToast(liveStreamVM.getLiveVideoBitRate().toString())
            }

            R.id.btn_enable_audio -> {
                liveStreamVM.setLiveAudioEnabled(true)
            }

            R.id.btn_disable_audio -> {
                liveStreamVM.setLiveAudioEnabled(false)
            }
        }
    }

    private fun showSetLiveStreamRtmpConfigDialog(){
        val factory = LayoutInflater.from(this@LiveStreamFragment.requireContext())
        val rtmpConfigView = factory.inflate(R.layout.dialog_livestream_rtmp_config_view, null)
        val etRtmpUrl = rtmpConfigView.findViewById<EditText>(R.id.et_livestream_rtmp_config)
        etRtmpUrl.setText(
            liveStreamVM.getRtmpUrl().toCharArray(),
            0,
            liveStreamVM.getRtmpUrl().length
        )
        configDialog = this@LiveStreamFragment.requireContext()?.let {
            AlertDialog.Builder(it, R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
                .setIcon(android.R.drawable.ic_menu_camera)
                .setTitle(R.string.ad_set_live_stream_rtmp_config)
                .setCancelable(false)
                .setView(rtmpConfigView)
                .setPositiveButton(R.string.ad_confirm) { configDialog, _ ->
                    kotlin.run {
                        val inputValue = etRtmpUrl.text.toString()
                        if (TextUtils.isEmpty(inputValue)) {
                            ToastUtils.showToast(msg)
                        } else {
                            liveStreamVM.setRTMPConfig(inputValue)
                        }
                        configDialog.dismiss()
                    }
                }
                .setNegativeButton(R.string.ad_cancel) { configDialog, _ ->
                    kotlin.run {
                        configDialog.dismiss()
                    }
                }
                .create()
        }
        configDialog.show()
    }

    private fun showSetLiveStreamRtspConfigDialog(){
        val factory = LayoutInflater.from(this@LiveStreamFragment.requireContext())
        val rtspConfigView = factory.inflate(R.layout.dialog_livestream_rtsp_config_view, null)
        val etRtspUsername = rtspConfigView.findViewById<EditText>(R.id.et_livestream_rtsp_username)
        val etRtspPassword = rtspConfigView.findViewById<EditText>(R.id.et_livestream_rtsp_password)
        val etRtspPort = rtspConfigView.findViewById<EditText>(R.id.et_livestream_rtsp_port)
        val rtspConfig = liveStreamVM.getRtspSettings()
        if (!TextUtils.isEmpty(rtspConfig) && rtspConfig.length > 0) {
            val configs = rtspConfig.trim().split("^_^")
            etRtspUsername.setText(
                configs[0].toCharArray(),
                0,
                configs[0].length
            )
            etRtspPassword.setText(
                configs[1].toCharArray(),
                0,
                configs[1].length
            )
            etRtspPort.setText(
                configs[2].toCharArray(),
                0,
                configs[2].length
            )
        }

        configDialog = this@LiveStreamFragment.requireContext()?.let {
            AlertDialog.Builder(it, R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
                .setIcon(android.R.drawable.ic_menu_camera)
                .setTitle(R.string.ad_set_live_stream_rtsp_config)
                .setCancelable(false)
                .setView(rtspConfigView)
                .setPositiveButton(R.string.ad_confirm) { configDialog, _ ->
                    kotlin.run {
                        val inputUserName = etRtspUsername.text.toString()
                        val inputPassword = etRtspPassword.text.toString();
                        val inputPort = etRtspPort.text.toString();
                        if (TextUtils.isEmpty(inputUserName) || TextUtils.isEmpty(inputPassword) || TextUtils.isEmpty(
                                inputPort
                            )
                        ) {
                            ToastUtils.showToast(msg)
                        } else {
                            try {
                                liveStreamVM.setRTSPConfig(
                                    inputUserName,
                                    inputPassword,
                                    inputPort.toInt()
                                )
                            } catch (e: NumberFormatException) {
                                ToastUtils.showToast("RTSP port must be int value")
                            }
                        }
                        configDialog.dismiss()
                    }
                }
                .setNegativeButton(R.string.ad_cancel) { configDialog, _ ->
                    kotlin.run {
                        configDialog.dismiss()
                    }
                }
                .create()
        }
        configDialog.show()
    }

    private fun showSetLiveStreamGb28181ConfigDialog(){
        val factory = LayoutInflater.from(this@LiveStreamFragment.requireContext())
        val gbConfigView = factory.inflate(R.layout.dialog_livestream_gb28181_config_view, null)
        val etGbServerIp = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_server_ip)
        val etGbServerPort = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_server_port)
        val etGbServerId = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_server_id)
        val etGbAgentId = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_agent_id)
        val etGbChannel = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_channel)
        val etGbLocalPort = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_local_port)
        val etGbPassword = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_password)

        val gbConfig = liveStreamVM.getGb28181Settings()
        if (!TextUtils.isEmpty(gbConfig) && gbConfig.length > 0) {
            val configs = gbConfig.trim().split("^_^")
            etGbServerIp.setText(
                configs[0].toCharArray(),
                0,
                configs[0].length
            )
            etGbServerPort.setText(
                configs[1].toCharArray(),
                0,
                configs[1].length
            )
            etGbServerId.setText(
                configs[2].toCharArray(),
                0,
                configs[2].length
            )
            etGbAgentId.setText(
                configs[3].toCharArray(),
                0,
                configs[3].length
            )
            etGbChannel.setText(
                configs[4].toCharArray(),
                0,
                configs[4].length
            )
            etGbLocalPort.setText(
                configs[5].toCharArray(),
                0,
                configs[5].length
            )
            etGbPassword.setText(
                configs[6].toCharArray(),
                0,
                configs[6].length
            )
        }

        configDialog = this@LiveStreamFragment.requireContext()?.let {
            AlertDialog.Builder(it, R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
                .setIcon(android.R.drawable.ic_menu_camera)
                .setTitle(R.string.ad_set_live_stream_gb28181_config)
                .setCancelable(false)
                .setView(gbConfigView)
                .setPositiveButton(R.string.ad_confirm) { configDialog, _ ->
                    kotlin.run {
                        val serverIp = etGbServerIp.text.toString()
                        val serverPort = etGbServerPort.text.toString()
                        val serverId = etGbServerId.text.toString()
                        val agentId = etGbAgentId.text.toString()
                        val channel = etGbChannel.text.toString()
                        val localPort = etGbLocalPort.text.toString()
                        val password = etGbPassword.text.toString()
                        if (TextUtils.isEmpty(serverIp) || TextUtils.isEmpty(serverPort) || TextUtils.isEmpty(
                                serverId
                            ) || TextUtils.isEmpty(agentId) || TextUtils.isEmpty(channel) || TextUtils.isEmpty(
                                localPort
                            ) || TextUtils.isEmpty(password)
                        ) {
                            ToastUtils.showToast(msg)
                        } else {
                            try {
                                liveStreamVM.setGB28181(
                                    serverIp,
                                    serverPort.toInt(),
                                    serverId,
                                    agentId,
                                    channel,
                                    localPort.toInt(),
                                    password
                                )
                            } catch (e: NumberFormatException) {
                                ToastUtils.showToast("RTSP port must be int value")
                            }
                        }
                        configDialog.dismiss()
                    }
                }
                .setNegativeButton(R.string.ad_cancel) { configDialog, _ ->
                    kotlin.run {
                        configDialog.dismiss()
                    }
                }
                .create()
        }
        configDialog.show()
    }

    private fun showSetLiveStreamAgoraConfigDialog(){
        val factory = LayoutInflater.from(this@LiveStreamFragment.requireContext())
        val agoraConfigView = factory.inflate(R.layout.dialog_livestream_agora_config_view, null)

        val etAgoraChannelId = agoraConfigView.findViewById<EditText>(R.id.et_livestream_agora_channel_id)
        val etAgoraToken = agoraConfigView.findViewById<EditText>(R.id.et_livestream_agora_token)
        val etAgoraUid = agoraConfigView.findViewById<EditText>(R.id.et_livestream_agora_uid)

        val agoraConfig = liveStreamVM.getAgoraSettings()
        if (!TextUtils.isEmpty(agoraConfig) && agoraConfig.length > 0) {
            val configs = agoraConfig.trim().split("^_^")
            etAgoraChannelId.setText(configs[0].toCharArray(), 0, configs[0].length)
            etAgoraToken.setText(configs[1].toCharArray(), 0, configs[1].length)
            etAgoraUid.setText(configs[2].toCharArray(), 0, configs[2].length)
        }

        configDialog = this@LiveStreamFragment.requireContext()?.let {
            AlertDialog.Builder(it, R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
                .setIcon(android.R.drawable.ic_menu_camera)
                .setTitle(R.string.ad_set_live_stream_agora_config)
                .setCancelable(false)
                .setView(agoraConfigView)
                .setPositiveButton(R.string.ad_confirm) { configDialog, _ ->
                    kotlin.run {
                        val channelId = etAgoraChannelId.text.toString()
                        val token = etAgoraToken.text.toString()
                        val uid = etAgoraUid.text.toString()
                        if (TextUtils.isEmpty(channelId) || TextUtils.isEmpty(token) || TextUtils.isEmpty(
                                uid
                            )
                        ) {
                            ToastUtils.showToast(msg)
                        } else {
                            liveStreamVM.setAgoraConfig(channelId, token, uid)
                        }
                        configDialog.dismiss()
                    }
                }
                .setNegativeButton(R.string.ad_cancel) { configDialog, _ ->
                    kotlin.run {
                        configDialog.dismiss()
                    }
                }
                .create()
        }
        configDialog.show()
    }

    private fun showSetLiveStreamBitrateDialog(){
        val editText = EditText(this@LiveStreamFragment.requireContext())
        dialog = this@LiveStreamFragment.requireContext()?.let {
            AlertDialog.Builder(it, R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
                .setIcon(android.R.drawable.ic_menu_camera)
                .setTitle(R.string.ad_set_live_stream_bit_rate)
                .setCancelable(false)
                .setView(editText)
                .setPositiveButton(R.string.ad_confirm) { dialog, _ ->
                    kotlin.run {
                        val inputValue = editText.text.toString().toInt()
                        liveStreamVM.setLiveVideoBitRate(inputValue)
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(R.string.ad_cancel) { dialog, _ ->
                    kotlin.run {
                        dialog.dismiss()
                    }
                }
                .create()
        }
        dialog.show()
    }

    private fun showSetLiveStreamQualityDialog(){
        val liveStreamQualities = liveStreamVM.getLiveStreamQualities()
        liveStreamQualities?.let {
            val items = arrayOfNulls<String>(liveStreamQualities.size)
            for (i in liveStreamQualities.indices) {
                items[i] = liveStreamQualities[i].name
            }
            if (!items.isNullOrEmpty()) {
                dialog = this@LiveStreamFragment.requireContext()?.let {
                    AlertDialog.Builder(it, R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
                        .setIcon(android.R.drawable.ic_menu_camera)
                        .setTitle(R.string.ad_select_live_stream_quality)
                        .setCancelable(false)
                        .setSingleChoiceItems(items, checkedItem) { _, i ->
                            checkedItem = i
                            ToastUtils.showToast(
                                it,
                                "选择所使用的bitRateMode： " + (items[i] ?: "选择所使用的bitRateMode为null"),
                            )
                        }
                        .setPositiveButton(R.string.ad_confirm) { dialog, _ ->
                            kotlin.run {
                                liveStreamQuality = liveStreamQualities[checkedItem]
                                liveStreamVM.setLiveStreamQuality(liveStreamQuality)
                                dialog.dismiss()
                            }
                        }
                        .setNegativeButton(R.string.ad_cancel) { dialog, _ ->
                            kotlin.run {
                                dialog.dismiss()
                            }
                        }
                        .create()
                }
            }
            dialog.show()
        }
    }

    private fun showSetLiveStreamBitRateModeDialog() {
        val liveStreamBitrateModes = liveStreamVM.getLiveStreamBitRateModes()
        liveStreamBitrateModes?.let {
            val items = arrayOfNulls<String>(liveStreamBitrateModes.size)
            for (i in liveStreamBitrateModes.indices) {
                items[i] = liveStreamBitrateModes[i].name
            }
            if (!items.isNullOrEmpty()) {
                dialog = this@LiveStreamFragment.requireContext()?.let {
                    AlertDialog.Builder(it, R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
                        .setIcon(android.R.drawable.ic_menu_camera)
                        .setTitle(R.string.ad_select_live_stream_bit_rate_mode)
                        .setCancelable(false)
                        .setSingleChoiceItems(items, checkedItem) { _, i ->
                            checkedItem = i
                            ToastUtils.showToast(
                                it,
                                "选择所使用的bitRateMode： " + (items[i] ?: "选择所使用的bitRateMode为null"),
                            )
                        }
                        .setPositiveButton("确认") { dialog, _ ->
                            kotlin.run {
                                liveStreamBitrateMode = liveStreamBitrateModes[checkedItem]
                                liveStreamVM.setLiveVideoBitRateMode(liveStreamBitrateMode)
                                dialog.dismiss()
                            }
                        }
                        .setNegativeButton("取消") { dialog, _ ->
                            kotlin.run {
                                dialog.dismiss()
                            }
                        }
                        .create()
                }
            }
            dialog.show()
        }
    }

    private fun showSetLiveStreamChannelTypeDialog() {
        val liveStreamChannelTypes = liveStreamVM.getLiveStreamChannelTypes()
        liveStreamChannelTypes?.let {
            val items = arrayOfNulls<String>(liveStreamChannelTypes.size)
            for (i in liveStreamChannelTypes.indices) {
                items[i] = liveStreamChannelTypes[i].name
            }
            if (!items.isNullOrEmpty()) {
                dialog = this@LiveStreamFragment.requireContext()?.let {
                    AlertDialog.Builder(it, R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
                        .setIcon(android.R.drawable.ic_dialog_email)
                        .setTitle(R.string.ad_select_live_stream_channel_type)
                        .setCancelable(false)
                        .setSingleChoiceItems(items, checkedItem) { _, i ->
                            checkedItem = i
                            ToastUtils.showToast(
                                it,
                                "选择使用所使用的channel： " + (items[i] ?: "选择使用所使用的channel为null"),
                            )
                        }
                        .setPositiveButton("确认") { dialog, _ ->
                            kotlin.run {
                                liveStreamChannelType = liveStreamChannelTypes[checkedItem]

                                videoDecoder?.let {
                                    videoDecoder?.onPause()
                                    videoDecoder?.destory()
                                    videoDecoder = null
                                }

                                if (videoDecoder == null) {
                                    videoDecoder = VideoDecoder(
                                        this@LiveStreamFragment.context,
                                        liveStreamChannelType,
                                        DecoderOutputMode.SURFACE_MODE,
                                        surfaceView.holder,
                                        curWidth,
                                        curHeight,
                                        true
                                    )
                                } else if (videoDecoder?.decoderStatus == DecoderState.PAUSED) {
                                    videoDecoder?.onResume()
                                }

                                liveStreamVM.setVideoChannel(liveStreamChannelType)
                                dialog.dismiss()
                            }
                        }
                        .setNegativeButton("取消") { dialog, _ ->
                            kotlin.run {
                                dialog.dismiss()
                            }
                        }
                        .create()
                }
            }
            dialog.show()
        }
    }

    private fun showSetLiveStreamConfigDialog() {
        val liveStreamTypes = liveStreamVM.getLiveStreamTypes()
        liveStreamTypes?.let {
            val items = arrayOfNulls<String>(liveStreamTypes.size)
            for (i in liveStreamTypes.indices) {
                items[i] = liveStreamTypes[i].name
            }
            if (!items.isNullOrEmpty()) {
                dialog = this@LiveStreamFragment.requireContext()?.let {
                    AlertDialog.Builder(it, R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
                        .setIcon(android.R.drawable.ic_input_get)
                        .setTitle(R.string.ad_select_live_stream_type)
                        .setCancelable(false)
                        .setSingleChoiceItems(items, checkedItem) { _, i ->
                            checkedItem = i
                            ToastUtils.showToast(
                                it,
                                "选择的直播类型为： " + (items[i] ?: "直播类型为null"),
                            )
                        }
                        .setPositiveButton("确认") { dialog, _ ->
                            kotlin.run {
                                liveStreamType = liveStreamTypes[checkedItem]
                                setLiveStreamConfig(liveStreamType)
                                dialog.dismiss()
                            }
                        }
                        .setNegativeButton("取消") { dialog, _ ->
                            kotlin.run {
                                dialog.dismiss()
                            }
                        }
                        .create()
                }
            }
            dialog.show()
        }
    }

    private fun setLiveStreamConfig(liveStreamtype: LiveStreamType) {
        liveStreamtype?.let {
            when (liveStreamtype) {
                LiveStreamType.RTMP -> {
                    showSetLiveStreamRtmpConfigDialog()
                }
                LiveStreamType.RTSP -> {
                    showSetLiveStreamRtspConfigDialog()
                }
                LiveStreamType.GB28181 -> {
                    showSetLiveStreamGb28181ConfigDialog()
                }
                LiveStreamType.AGORA -> {
                    showSetLiveStreamAgoraConfigDialog()
                }
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        videoDecoder?.let {
            if (videoDecoder?.decoderStatus == DecoderState.PAUSED) {
                videoDecoder?.onResume()
            }
        } ?: let {
            videoDecoder = VideoDecoder(
                this@LiveStreamFragment.context,
                VideoChannelType.PRIMARY_STREAM_CHANNEL,
                DecoderOutputMode.SURFACE_MODE,
                surfaceView.holder,
                surfaceView.width,
                surfaceView.height,
                true
            )

            curWidth = surfaceView.width
            curHeight = surfaceView.height
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        if (videoDecoder == null) {
            videoDecoder = VideoDecoder(
                this@LiveStreamFragment.context,
                VideoChannelType.PRIMARY_STREAM_CHANNEL,
                DecoderOutputMode.SURFACE_MODE,
                surfaceView.holder,
                width,
                height,
                true
            )
        } else if (videoDecoder?.decoderStatus == DecoderState.PAUSED) {
            videoDecoder?.onResume()
        }
        curWidth = width
        curHeight = height
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        videoDecoder?.let {
            videoDecoder?.onPause()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        videoDecoder?.let {
            it.destory()
        }
    }
}