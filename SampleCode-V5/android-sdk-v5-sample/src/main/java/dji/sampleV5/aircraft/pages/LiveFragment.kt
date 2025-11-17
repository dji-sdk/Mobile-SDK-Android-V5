package dji.sampleV5.aircraft.pages

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.models.LiveStreamVM
import dji.sampleV5.aircraft.util.ToastUtils
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.datacenter.livestream.LiveStreamStatus
import dji.v5.manager.datacenter.livestream.LiveVideoBitrateMode
import dji.v5.manager.datacenter.livestream.StreamQuality
import dji.v5.manager.datacenter.livestream.VideoResolution
import dji.v5.manager.interfaces.ICameraStreamManager
import dji.v5.utils.common.NumberUtils
import dji.v5.utils.common.StringUtils

class LiveFragment : DJIFragment() {
    private val cameraStreamManager = MediaDataCenter.getInstance().cameraStreamManager

    private val liveStreamVM: LiveStreamVM by viewModels()

    private val emptyInputMessage = "input is empty"

    private lateinit var cameraIndex: ComponentIndexType
    private lateinit var rgProtocol: RadioGroup
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnShow: Button
    private lateinit var btnHide: Button
    private lateinit var rgCamera: RadioGroup
    private lateinit var rgQuality: RadioGroup
    private lateinit var rgBitRate: RadioGroup
    private lateinit var rgCameraStreamScaleType: RadioGroup
    private lateinit var rgLiveStreamScaleType: RadioGroup
    private lateinit var sbBitRate: SeekBar
    private lateinit var tvBitRate: TextView
    private lateinit var tvLiveInfo: TextView
    private lateinit var tvLiveError: TextView
    private lateinit var svCameraStream: SurfaceView
    private var isLocalLiveShow: Boolean = true

    private var cameraStreamSurface: Surface? = null
    private var cameraStreamWidth = -1
    private var cameraStreamHeight = -1
    private var cameraStreamScaleType: ICameraStreamManager.ScaleType = ICameraStreamManager.ScaleType.CENTER_INSIDE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_live, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rgProtocol = view.findViewById(R.id.rg_protocol)
        btnStart = view.findViewById(R.id.btn_start)
        btnStop = view.findViewById(R.id.btn_stop)
        btnShow = view.findViewById(R.id.btn_show_view)
        btnHide = view.findViewById(R.id.btn_hide_view)
        rgCamera = view.findViewById(R.id.rg_camera)
        rgQuality = view.findViewById(R.id.rg_quality)
        rgBitRate = view.findViewById(R.id.rg_bit_rate)
        rgCameraStreamScaleType = view.findViewById(R.id.rg_camera_scale_type)
        rgLiveStreamScaleType = view.findViewById(R.id.rg_live_scale_type)
        sbBitRate = view.findViewById(R.id.sb_bit_rate)
        tvBitRate = view.findViewById(R.id.tv_bit_rate)
        tvLiveInfo = view.findViewById(R.id.tv_live_info)
        tvLiveError = view.findViewById(R.id.tv_live_error)
        svCameraStream = view.findViewById(R.id.sv_camera_stream)

        initRGCamera()
        initRGQuality()
        initRGBitRate()
        initCameraStreamScaleType()
        initLiveStreamScaleType()
        initLiveButton()
        initCameraStream()
        initLiveData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopLive()
    }

    @SuppressLint("SetTextI18n")
    private fun initLiveData() {
        liveStreamVM.liveStreamStatus.observe(viewLifecycleOwner) { status ->
            var liveStreamStatus = status
            if (liveStreamStatus == null) {
                liveStreamStatus = LiveStreamStatus(0, 0, 0, 0, 0, false, VideoResolution(0, 0))
            }
            val liveWidth = liveStreamStatus.resolution?.width ?: 0
            val liveHeight = liveStreamStatus.resolution?.height ?: 0
            val sourceWidth = liveStreamVM.getAircraftStreamFrameInfo(cameraIndex)?.width ?: 0
            val sourceHeight = liveStreamVM.getAircraftStreamFrameInfo(cameraIndex)?.height ?: 0
            val sourceFps = liveStreamVM.getAircraftStreamFrameInfo(cameraIndex)?.frameRate ?: 0
            val liveGCD = NumberUtils.gcd(liveWidth, liveHeight)
            val sourceGCD = NumberUtils.gcd(sourceWidth, sourceHeight)

            val statusStr = StringBuilder().append(liveStreamStatus)
                .append("source width = $sourceWidth\n")
                .append("source height = $sourceHeight\n")
                .append("source fps = $sourceFps\n")

            if (liveGCD != 0) {
                statusStr.append("live ratio = ${liveWidth / liveGCD}/${liveHeight / liveGCD}\n")
            } else {
                statusStr.append("live ratio = NA\n")
            }

            if (sourceGCD != 0) {
                statusStr.append("source ratio = ${sourceWidth / sourceGCD}/${sourceHeight / sourceGCD}\n")
            } else {
                statusStr.append("source ratio = NA\n")
            }
            tvLiveInfo.text = statusStr.toString()

            rgProtocol.isEnabled = !liveStreamStatus.isStreaming
            for (i in 0 until rgProtocol.childCount) {
                rgProtocol.getChildAt(i).isEnabled = rgProtocol.isEnabled
            }
            btnStart.isEnabled = !liveStreamVM.isStreaming()
            btnStop.isEnabled = liveStreamVM.isStreaming()
        }

        liveStreamVM.liveStreamError.observe(viewLifecycleOwner) { error ->
            if (error == null) {
                tvLiveError.text = ""
                tvLiveError.visibility = View.GONE
            } else {
                tvLiveError.text = "error : $error"
                tvLiveError.visibility = View.VISIBLE
            }
        }

        liveStreamVM.availableCameraList.observe(viewLifecycleOwner) { cameraIndexList ->
            var firstAvailableView: View? = null
            var isNeedChangeCamera = false
            for (i in 0 until rgCamera.childCount) {
                val view = rgCamera.getChildAt(i)
                val index = ComponentIndexType.find((view.tag as String).toInt())
                if (cameraIndexList.contains(index)) {
                    view.visibility = View.VISIBLE
                    if (firstAvailableView == null) {
                        firstAvailableView = view
                    }
                } else {
                    view.visibility = View.GONE
                    if (rgCamera.checkedRadioButtonId == view.id) {
                        isNeedChangeCamera = true
                    }
                }
            }
            if (isNeedChangeCamera && firstAvailableView != null) {
                rgCamera.check(firstAvailableView.id)
            }
            if (cameraIndexList.isEmpty()) {
                stopLive()
            }
        }
    }

    private fun initRGCamera() {
        rgCamera.setOnCheckedChangeListener { group: RadioGroup, checkedId: Int ->
            val view = group.findViewById<View>(checkedId)
            cameraIndex = ComponentIndexType.find((view.tag as String).toInt())
            cameraStreamSurface = svCameraStream.holder.surface
            if (cameraStreamSurface != null && svCameraStream.width != 0) {
                putCameraStreamSurface()
            }
            liveStreamVM.setCameraIndex(cameraIndex)
        }
        rgCamera.check(R.id.rb_camera_left)
    }

    private fun putCameraStreamSurface() {
        if (!isLocalLiveShow) {
            return
        }
        if (cameraIndex == ComponentIndexType.UNKNOWN) {
            return
        }
        cameraStreamSurface?.let {
            cameraStreamManager.putCameraStreamSurface(
                cameraIndex,
                it,
                cameraStreamWidth,
                cameraStreamHeight,
                cameraStreamScaleType
            )
        }
    }

    private fun removeCameraStreamSurface() {
        cameraStreamSurface?.let {
            cameraStreamManager.removeCameraStreamSurface(it)
        }
    }


    private fun initRGQuality() {
        rgQuality.setOnCheckedChangeListener { group: RadioGroup, checkedId: Int ->
            val view = group.findViewById<View>(checkedId)
            liveStreamVM.setLiveStreamQuality(StreamQuality.find((view.tag as String).toInt()))
        }
        rgQuality.check(R.id.rb_quality_hd)
    }

    private fun initLiveStreamScaleType() {
        rgLiveStreamScaleType.setOnCheckedChangeListener { group: RadioGroup, checkedId: Int ->
            val view = group.findViewById<View>(checkedId)
            liveStreamVM.setLiveStreamScaleType(ICameraStreamManager.ScaleType.find((view.tag as String).toInt()))
        }
        rgLiveStreamScaleType.check(R.id.rb_live_scale_type_center_crop)
    }

    private fun initCameraStreamScaleType() {
        rgCameraStreamScaleType.setOnCheckedChangeListener { group: RadioGroup, checkedId: Int ->
            val view = group.findViewById<View>(checkedId)
            cameraStreamScaleType = ICameraStreamManager.ScaleType.find((view.tag as String).toInt())
            putCameraStreamSurface()
        }
        rgCameraStreamScaleType.check(R.id.rb_camera_scale_type_center_inside)
    }

    @SuppressLint("SetTextI18n")
    private fun initRGBitRate() {
        rgBitRate.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            if (checkedId == R.id.rb_bit_rate_auto) {
                sbBitRate.visibility = View.GONE
                tvBitRate.visibility = View.GONE
                liveStreamVM.setLiveVideoBitRateMode(LiveVideoBitrateMode.AUTO)
            } else if (checkedId == R.id.rb_bit_rate_manual) {
                sbBitRate.visibility = View.VISIBLE
                tvBitRate.visibility = View.VISIBLE
                liveStreamVM.setLiveVideoBitRateMode(LiveVideoBitrateMode.MANUAL)
                sbBitRate.progress = 20
            }
        }
        sbBitRate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                tvBitRate.text = (bitRate / 8 / 1024).toString() + " vbbs"
                if (!fromUser) {
                    liveStreamVM.setLiveVideoBitRate(bitRate)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                liveStreamVM.setLiveVideoBitRate(bitRate)
            }

            private val bitRate: Int
                get() = (8 * 1024 * 2048 * (0.1 + 0.9 * sbBitRate.progress / sbBitRate.max)).toInt()
        })
        rgBitRate.check(R.id.rb_bit_rate_auto)
    }

    private fun initLiveButton() {
        btnStart.setOnClickListener { _ ->
            val protocolCheckId = rgProtocol.checkedRadioButtonId
            if (protocolCheckId == R.id.rb_rtmp) {
                showSetLiveStreamRtmpConfigDialog()
            } else if (protocolCheckId == R.id.rb_rtsp) {
                showSetLiveStreamRtspConfigDialog()
            } else if (protocolCheckId == R.id.rb_gb28181) {
                showSetLiveStreamGb28181ConfigDialog()
            } else if (protocolCheckId == R.id.rb_agora) {
                showSetLiveStreamAgoraConfigDialog()
            }
        }
        btnStop.setOnClickListener {
            stopLive()
        }
        btnShow.setOnClickListener {
            svCameraStream.visibility = View.VISIBLE
            isLocalLiveShow = true
            putCameraStreamSurface()
        }
        btnHide.setOnClickListener {
            svCameraStream.visibility = View.GONE
            isLocalLiveShow = false
            removeCameraStreamSurface()
        }
    }

    private fun initCameraStream() {
        svCameraStream.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {}
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                cameraStreamWidth = width
                cameraStreamHeight = height
                cameraStreamSurface = holder.surface
                putCameraStreamSurface()
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraStreamManager.removeCameraStreamSurface(holder.surface)
            }
        })
    }

    private fun startLive() {
        if (!liveStreamVM.isStreaming()) {
            liveStreamVM.startStream(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showShortToast(StringUtils.getResStr(R.string.msg_start_live_stream_success))
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showLongToast(
                        StringUtils.getResStr(R.string.msg_start_live_stream_failed, error.description())
                    )
                }
            });
        }
    }

    private fun stopLive() {
        liveStreamVM.stopStream(null)
    }

    private fun showSetLiveStreamRtmpConfigDialog() {
        val factory = LayoutInflater.from(requireContext())
        val rtmpConfigView = factory.inflate(R.layout.dialog_livestream_rtmp_config_view, null)
        val etRtmpUrl = rtmpConfigView.findViewById<EditText>(R.id.et_livestream_rtmp_config)
        etRtmpUrl.setText(
            liveStreamVM.getRtmpUrl().toCharArray(),
            0,
            liveStreamVM.getRtmpUrl().length
        )
        val configDialog = requireContext().let {
            AlertDialog.Builder(it, androidx.appcompat.R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
                .setIcon(android.R.drawable.ic_menu_camera)
                .setTitle(R.string.ad_set_live_stream_rtmp_config)
                .setCancelable(false)
                .setView(rtmpConfigView)
                .setPositiveButton(R.string.ad_confirm) { configDialog, _ ->
                    kotlin.run {
                        val inputValue = etRtmpUrl.text.toString()
                        if (TextUtils.isEmpty(inputValue)) {
                            ToastUtils.showToast(emptyInputMessage)
                        } else {
                            liveStreamVM.setRTMPConfig(inputValue)
                            startLive()
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

    private fun showSetLiveStreamRtspConfigDialog() {
        val factory = LayoutInflater.from(requireContext())
        val rtspConfigView = factory.inflate(R.layout.dialog_livestream_rtsp_config_view, null)
        val etRtspUsername = rtspConfigView.findViewById<EditText>(R.id.et_livestream_rtsp_username)
        val etRtspPassword = rtspConfigView.findViewById<EditText>(R.id.et_livestream_rtsp_password)
        val etRtspPort = rtspConfigView.findViewById<EditText>(R.id.et_livestream_rtsp_port)
        val rtspConfig = liveStreamVM.getRtspSettings()
        if (!TextUtils.isEmpty(rtspConfig) && rtspConfig.isNotEmpty()) {
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

        val configDialog = requireContext().let {
            AlertDialog.Builder(it, androidx.appcompat.R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
                .setIcon(android.R.drawable.ic_menu_camera)
                .setTitle(R.string.ad_set_live_stream_rtsp_config)
                .setCancelable(false)
                .setView(rtspConfigView)
                .setPositiveButton(R.string.ad_confirm) { configDialog, _ ->
                    kotlin.run {
                        val inputUserName = etRtspUsername.text.toString()
                        val inputPassword = etRtspPassword.text.toString()
                        val inputPort = etRtspPort.text.toString()
                        if (TextUtils.isEmpty(inputUserName) || TextUtils.isEmpty(inputPassword) || TextUtils.isEmpty(
                                inputPort
                            )
                        ) {
                            ToastUtils.showToast(emptyInputMessage)
                        } else {
                            try {
                                liveStreamVM.setRTSPConfig(
                                    inputUserName,
                                    inputPassword,
                                    inputPort.toInt()
                                )
                                startLive()
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

    private fun showSetLiveStreamGb28181ConfigDialog() {
        val factory = LayoutInflater.from(requireContext())
        val gbConfigView = factory.inflate(R.layout.dialog_livestream_gb28181_config_view, null)
        val etGbServerIp = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_server_ip)
        val etGbServerPort = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_server_port)
        val etGbServerId = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_server_id)
        val etGbAgentId = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_agent_id)
        val etGbChannel = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_channel)
        val etGbLocalPort = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_local_port)
        val etGbPassword = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_password)

        val gbConfig = liveStreamVM.getGb28181Settings()
        if (!TextUtils.isEmpty(gbConfig) && gbConfig.isNotEmpty()) {
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

        val configDialog = requireContext().let {
            AlertDialog.Builder(it, androidx.appcompat.R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
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
                            ToastUtils.showToast(emptyInputMessage)
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
                                startLive()
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

    private fun showSetLiveStreamAgoraConfigDialog() {
        val factory = LayoutInflater.from(requireContext())
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

        val configDialog = requireContext().let {
            AlertDialog.Builder(it, androidx.appcompat.R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
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
                            ToastUtils.showToast(emptyInputMessage)
                        } else {
                            liveStreamVM.setAgoraConfig(channelId, token, uid)
                            startLive()
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

}