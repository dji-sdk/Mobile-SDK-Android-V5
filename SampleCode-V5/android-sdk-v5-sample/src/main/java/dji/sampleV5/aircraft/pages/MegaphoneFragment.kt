package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.SeekBar
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.databinding.FragMegaphonePageBinding
import dji.sampleV5.aircraft.models.MegaphoneVM
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.utils.ProductUtil
import dji.sdk.keyvalue.value.flightcontroller.FlashLedColor
import dji.sdk.keyvalue.value.flightcontroller.FlashLedPattern
import dji.sdk.keyvalue.value.flightcontroller.UserRequestFlashLedMsg
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.megaphone.*
import dji.v5.utils.common.LogUtils
import dji.v5.ux.util.ToastUtils
import dji.v5.utils.common.LogPath


/**
 * Description : Megaphone fragment
 * Author : daniel.chen
 * CreateDate : 2022/1/17 2:47 下午
 * Copyright : ©2021 DJI All Rights Reserved.
 */
class MegaphoneFragment : DJIFragment() {
    private val megaphoneVM: MegaphoneVM by activityViewModels()
    private var binding: FragMegaphonePageBinding? = null

    private var curPlayMode: PlayMode = PlayMode.UNKNOWN
    private var curMegaphoneIndex:MegaphoneIndex = MegaphoneIndex.UNKNOWN
    private var isPlaying: Boolean = false
    private var isFirstSwitch: Boolean = true
    private var isFlashLedActive: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragMegaphonePageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /**
         * 初始化Listener，同时拿一次当前喊话器的状态
         */
        initBtnListener()
        initWarmingLightBtn()

        megaphoneVM.curRealTimeSentBytes.observe(viewLifecycleOwner) {
            updateUploadStatus()
        }
        megaphoneVM.curRealTimeTotalBytes.observe(viewLifecycleOwner) {
            updateUploadStatus()
        }
        megaphoneVM.curRealTimeUploadedState.observe(viewLifecycleOwner) {
            updateUploadStatus()
            updateUploadState()
        }

        megaphoneVM.isLeftPayloadConnect.observe(viewLifecycleOwner) {
            updateMegaphoneConnectState()
            if (curMegaphoneIndex == MegaphoneIndex.PORTSIDE){
                updateStatus()
            }
        }

        megaphoneVM.isRightPayloadConnect.observe(viewLifecycleOwner) {
            updateMegaphoneConnectState()
            if (curMegaphoneIndex == MegaphoneIndex.STARBOARD){
                updateStatus()
            }
        }

        megaphoneVM.isUpPayloadConnect.observe(viewLifecycleOwner) {
            updateMegaphoneConnectState()
            if (curMegaphoneIndex == MegaphoneIndex.UPSIDE){
                updateStatus()
            }
        }

        megaphoneVM.isOSDKPayloadConnect.observe(viewLifecycleOwner) {
            updateMegaphoneConnectState()
            if (curMegaphoneIndex == MegaphoneIndex.OSDK){
                updateStatus()
            }
        }

        megaphoneVM.isPort1Connect.observe(viewLifecycleOwner) {
            updateMegaphoneConnectState()
            if (curMegaphoneIndex == MegaphoneIndex.PORT_1){
                updateStatus()
            }
        }

        megaphoneVM.isPort1Connect.observe(viewLifecycleOwner) {
            updateMegaphoneConnectState()
            if (curMegaphoneIndex == MegaphoneIndex.PORT_2){
                updateStatus()
            }
        }

        megaphoneVM.isPort1Connect.observe(viewLifecycleOwner) {
            updateMegaphoneConnectState()
            if (curMegaphoneIndex == MegaphoneIndex.PORT_3){
                updateStatus()
            }
        }

        megaphoneVM.isPort1Connect.observe(viewLifecycleOwner) {
            updateMegaphoneConnectState()
            if (curMegaphoneIndex == MegaphoneIndex.PORT_4){
                updateStatus()
            }
        }

        megaphoneVM.megaphonePlayState.observe(viewLifecycleOwner) {
            updateMegaphonePlayState()
        }

        updateMegaphoneIndex()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        KeyManager.getInstance().cancelListen(this)
    }

    private fun updateMegaphoneIndex() {
        megaphoneVM.getMegaphoneIndex(object :
            CommonCallbacks.CompletionCallbackWithParam<MegaphoneIndex> {
            override fun onSuccess(t: MegaphoneIndex?) {
                t?.let {
                    curMegaphoneIndex = t
                    updateStatus()
                }
                binding?.tvCurMegaphoneIndex?.text = t?.let { t.name } ?: let { "N/A" }
            }

            override fun onFailure(error: IDJIError) {
                LogUtils.e(LogPath.SAMPLE, "get megaphone index onFailure,$error")
                updateStatus()
            }
        })
    }

    private fun updateStatus() {
        megaphoneVM.getStatus(object :
            CommonCallbacks.CompletionCallbackWithParam<MegaphoneStatus> {
            override fun onSuccess(t: MegaphoneStatus?) {
                binding?.tvMegaphoneState?.text = t!!.name
            }

            override fun onFailure(error: IDJIError) {
                binding?.tvMegaphoneState?.text = MegaphoneStatus.UNKNOWN.name
                LogUtils.e(LogPath.SAMPLE, "get system status onFailure,$error")
            }
        })

        megaphoneVM.getVolume(object : CommonCallbacks.CompletionCallbackWithParam<Int> {
            override fun onSuccess(t: Int?) {
                binding?.sbVolumeControl?.progress = t!!
            }

            override fun onFailure(error: IDJIError) {
                LogUtils.e(LogPath.SAMPLE, "get volume onFailure,$error")
            }
        })

        megaphoneVM.getPlayMode(object : CommonCallbacks.CompletionCallbackWithParam<PlayMode> {
            override fun onSuccess(t: PlayMode?) {
                curPlayMode = t!!
                if (curPlayMode == PlayMode.SINGLE) {
                    binding?.btnPlayMode?.setImageResource(R.drawable.ic_action_playback_repeat_1)
                } else {
                    binding?.btnPlayMode?.setImageResource(R.drawable.ic_action_playback_repeat)
                }
            }

            override fun onFailure(error: IDJIError) {
                LogUtils.e(LogPath.SAMPLE, "get play mode failed ${error}")
            }
        })
    }

    private fun initBtnListener() {
        binding?.btnPlayMode?.setOnClickListener {
            setPlayMode()
        }

        binding?.btnPlayControl?.setOnClickListener {
            megaphoneVM.getWorkMode(object : CommonCallbacks.CompletionCallbackWithParam<WorkMode> {
                override fun onSuccess(mode: WorkMode?) {
                    if (mode == WorkMode.REAL_TIME) {
                        ToastUtils.showToast("Can not play in realtime mode")
                    } else {
                        setPlayControl()
                    }
                }

                override fun onFailure(error: IDJIError) {
                    LogUtils.e(LogPath.SAMPLE, "getWorkMode failed")
                }

            })
        }

        binding?.sbVolumeControl?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                LogUtils.e(LogPath.SAMPLE, "volume change")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                LogUtils.e(LogPath.SAMPLE, "volume touch")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                megaphoneVM.setVolume(seekBar!!.progress,
                    object : CommonCallbacks.CompletionCallback {
                        override fun onSuccess() {
                            ToastUtils.showToast("set volume success")
                        }

                        override fun onFailure(error: IDJIError) {
                            ToastUtils.showToast("set volume failed: $error")
                        }
                    })
            }
        })

        binding?.spMegaphoneSwitch?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (isFirstSwitch) {
                    isFirstSwitch = false
                    return
                }
                val selectedItem = parent?.getItemAtPosition(position).toString()
                megaphoneVM.setMegaphoneIndex(MegaphoneIndex.valueOf(selectedItem), object :
                    CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        ToastUtils.showToast("set megaphone index success")
                        updateMegaphoneIndex()
                    }

                    override fun onFailure(error: IDJIError) {
                        updateStatus()
                        ToastUtils.showToast("set megaphone index failed: $error")
                    }
                })
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                //空方法，不实现
            }
        }
        changeFragment()
    }

    private fun setPlayMode() {
        val tempPlayMode: PlayMode
        if (curPlayMode == PlayMode.SINGLE) {
            tempPlayMode = PlayMode.LOOP
        } else {
            tempPlayMode = PlayMode.SINGLE
        }
        megaphoneVM.setPlayMode(tempPlayMode, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                curPlayMode = tempPlayMode
                if (curPlayMode == PlayMode.SINGLE) {
                    binding?.btnPlayMode?.setImageResource(R.drawable.ic_action_playback_repeat_1)
                } else {
                    binding?.btnPlayMode?.setImageResource(R.drawable.ic_action_playback_repeat)
                }
                ToastUtils.showToast("set playmode to $curPlayMode success")
            }

            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast("set playmode to $tempPlayMode failed: $error")
            }

        })
    }

    private fun setPlayControl() {
        if (!isPlaying) {
            megaphoneVM.startPlay(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    isPlaying = true
                    binding?.btnPlayControl?.setImageResource(R.drawable.ic_media_stop)
                    ToastUtils.showToast("start play success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("start play failed: $error")
                }
            })
        } else {
            megaphoneVM.stopPlay(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    isPlaying = false
                    binding?.btnPlayControl?.setImageResource(R.drawable.ic_media_play)
                    ToastUtils.showToast("stop play success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("stop play failed: $error")
                }
            })
        }
    }

    private fun changeFragment() {
        //fragment之间的切换
        binding?.ttsFrgBtn?.setOnClickListener {
            megaphoneVM.setWorkMode(WorkMode.TTS, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("set work mode to ${WorkMode.TTS} success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast(
                        "set work mode to ${WorkMode.TTS} failed: $error"
                    )
                }
            })

            val transaction: FragmentTransaction = requireFragmentManager().beginTransaction()
            val ttsFragment = TTSFragment()
            transaction.replace(R.id.fragment_container, ttsFragment)
            transaction.commit()
            megaphoneVM.removeRealTimeListener()
        }

        binding?.recordFrgBtn?.setOnClickListener {
            megaphoneVM.setWorkMode(WorkMode.VOICE, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("set work mode to ${WorkMode.VOICE} success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast(
                        "set work mode to ${WorkMode.VOICE} failed: $error"
                    )
                }
            })

            val transaction: FragmentTransaction = requireFragmentManager().beginTransaction()
            val recordFragment = RecordFragment()
            transaction.replace(R.id.fragment_container, recordFragment)
            transaction.commit()
            megaphoneVM.addListener()
        }

        binding?.realtimeFragBtn?.setOnClickListener {
            megaphoneVM.setWorkMode(WorkMode.REAL_TIME, object :
                CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("set work mode to ${WorkMode.REAL_TIME} success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast(
                        "set work mode to ${WorkMode.REAL_TIME} failed: $error"
                    )
                }
            })

            val transaction: FragmentTransaction = requireFragmentManager().beginTransaction()
            val recordFragment = RealTimeFragment()
            transaction.replace(R.id.fragment_container, recordFragment)
            transaction.commit()
            megaphoneVM.addListener()
        }

        binding?.fileListFrgBtn?.setOnClickListener {
            megaphoneVM.setWorkMode(WorkMode.VOICE, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("set work mode to ${WorkMode.VOICE} success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast(
                        "set work mode to ${WorkMode.VOICE} failed: $error"
                    )
                }
            })

            val transaction: FragmentTransaction = requireFragmentManager().beginTransaction()
            val localFileFragment = LocalFileFragment()
            transaction.replace(R.id.fragment_container, localFileFragment!!)
            transaction.commit()
            megaphoneVM.removeRealTimeListener()
        }
    }

    private fun initWarmingLightBtn() {
        binding?.btnPlayWarminglightControl?.setOnClickListener {
            sendFlashLedMsg(!isFlashLedActive)
        }
    }

    private fun sendFlashLedMsg(enable: Boolean) {
        val msg = if (enable) {
            // -----------------------------------------------------------------------
            // 开启警示灯配置说明
            // -----------------------------------------------------------------------
            // enabled = 1：启用爆闪灯控制
            //
            // pattern_name / pattern_uuid：
            //   使用固定预设名称 "preset_intermittent_pulse00000000000000000000000"
            //   （末尾补 0 至协议要求的固定长度48位），飞控通过该 uuid 识别预设方案，
            //   此处 name 与 uuid 保持一致即可。
            //
            // patterns（灯序列）设计逻辑：
            //   FlashLedPattern(color, period, brightness)
            //     - color     : 灯光颜色枚举（RED/BLUE/OFF）
            //     - period    : 持续时长，单位为协议最小时间片（20ms的倍数）
            //     - brightness: 亮度百分比 0~100
            //
            //   整体效果：【红色快闪 × 7次】→【长灭间隔】→【蓝色快闪 × 7次】→【长灭间隔】循环
            //
            //   红色段（7次脉冲）：
            //     RED  period=1  亮度100% → 点亮一个最小时间片
            //     OFF  period=2  亮度0%   → 熄灭两个时间片（形成短暂间隙）
            //     … 重复 7 次，形成快速闪烁视觉效果
            //     末尾 OFF period=20 → 熄灭较长时间，作为红蓝切换间的视觉分隔
            //
            //   蓝色段（7次脉冲）：结构与红色段完全对称
            //     末尾 OFF period=20 → 同样保留长灭间隔，确保一轮循环结束后有明显停顿
            // -----------------------------------------------------------------------
            val patterns = listOf(
                // ── 红色快闪段（7次）──
                FlashLedPattern(FlashLedColor.RED,  1,  100), // 红灯亮
                FlashLedPattern(FlashLedColor.OFF,  2,  0),   // 短暂熄灭
                FlashLedPattern(FlashLedColor.RED,  1,  100),
                FlashLedPattern(FlashLedColor.OFF,  2,  0),
                FlashLedPattern(FlashLedColor.RED,  1,  100),
                FlashLedPattern(FlashLedColor.OFF,  2,  0),
                FlashLedPattern(FlashLedColor.RED,  1,  100),
                FlashLedPattern(FlashLedColor.OFF,  2,  0),
                FlashLedPattern(FlashLedColor.RED,  1,  100),
                FlashLedPattern(FlashLedColor.OFF,  2,  0),
                FlashLedPattern(FlashLedColor.RED,  1,  100),
                FlashLedPattern(FlashLedColor.OFF,  2,  0),
                FlashLedPattern(FlashLedColor.RED,  1,  100),
                FlashLedPattern(FlashLedColor.OFF,  20, 0),   // 红蓝切换间隔，长灭
                // ── 蓝色快闪段（7次）──
                FlashLedPattern(FlashLedColor.BLUE, 1,  100), // 蓝灯亮
                FlashLedPattern(FlashLedColor.OFF,  2,  0),   // 短暂熄灭
                FlashLedPattern(FlashLedColor.BLUE, 1,  100),
                FlashLedPattern(FlashLedColor.OFF,  2,  0),
                FlashLedPattern(FlashLedColor.BLUE, 1,  100),
                FlashLedPattern(FlashLedColor.OFF,  2,  0),
                FlashLedPattern(FlashLedColor.BLUE, 1,  100),
                FlashLedPattern(FlashLedColor.OFF,  2,  0),
                FlashLedPattern(FlashLedColor.BLUE, 1,  100),
                FlashLedPattern(FlashLedColor.OFF,  2,  0),
                FlashLedPattern(FlashLedColor.BLUE, 1,  100),
                FlashLedPattern(FlashLedColor.OFF,  2,  0),
                FlashLedPattern(FlashLedColor.BLUE, 1,  100),
                FlashLedPattern(FlashLedColor.OFF,  20, 0)    // 本轮结束间隔，长灭后重新循环
            )
            UserRequestFlashLedMsg(
                1,
                "preset_intermittent_pulse00000000000000000000000",
                "preset_intermittent_pulse00000000000000000000000",
                patterns
            )
        } else {
            // -----------------------------------------------------------------------
            // 关闭警示灯配置说明
            // -----------------------------------------------------------------------
            // enabled = 0：关闭爆闪灯控制
            // pattern_name / pattern_uuid：全填 "0" 至48位，表示无预设方案
            // patterns：空列表，飞控收到后立即停止当前闪灯序列
            // -----------------------------------------------------------------------
            UserRequestFlashLedMsg(
                0,
                "000000000000000000000000000000000000000000000000",
                "000000000000000000000000000000000000000000000000",
                emptyList()
            )
        }

        KeyManager.getInstance().setValue(
            KeyTools.createKey(FlightControllerKey.KeyUserRequestFlashLed),
            msg,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    isFlashLedActive = enable
                    mainHandler.post {
                        if (isFlashLedActive) {
                            binding?.btnPlayWarminglightControl?.setImageResource(R.drawable.ic_media_stop)
                        } else {
                            binding?.btnPlayWarminglightControl?.setImageResource(R.drawable.ic_media_play)
                        }
                    }
                    ToastUtils.showToast("flash led ${if (enable) "start" else "stop"} success")
                    LogUtils.i(LogPath.SAMPLE, "sendFlashLedMsg enable=$enable success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("flash led ${if (enable) "start" else "stop"} failed: $error")
                    LogUtils.e(LogPath.SAMPLE, "sendFlashLedMsg enable=$enable failed: $error")
                }
            }
        )
    }

    private fun updateUploadStatus() {
        var builder = StringBuilder()
        builder.append("Cur sent bytes: ").append(megaphoneVM.curRealTimeSentBytes.value)
        builder.append("\n")
        builder.append("Total bytes: ").append(megaphoneVM.curRealTimeTotalBytes.value)
        builder.append("\n")
        builder.append("Cur upload state: ").append(megaphoneVM.curRealTimeUploadedState.value)
        builder.append("\n")
        mainHandler.post {
            binding?.etUploadStatus?.text = builder.toString()
        }
    }

    private fun updateUploadState() {
        if (UploadState.UPLOAD_SUCCESS == (megaphoneVM.curRealTimeUploadedState.value) && megaphoneVM.isQuickPlay) {
            MegaphoneManager.getInstance()
                .startPlay(object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        LogUtils.i(LogPath.SAMPLE, "Start Play Success")
                    }

                    override fun onFailure(error: IDJIError) {
                        LogUtils.i(LogPath.SAMPLE, "Start Play Failed")
                    }
                })
        }
//        megaphoneVM.curRealTimeUploadedState.value = UploadState.UNKNOWN
    }

    private fun updateMegaphoneConnectState() {
        mainHandler.post {
            var connectionBuilder = StringBuilder()
            if (megaphoneVM.isLeftPayloadConnect.value == false) {
                val textSource = "<font color=\'#ff0000\'><small>PORTSIDE</small></font>"
                connectionBuilder.append(textSource)
            } else {
                val textSource = "<font color=\'#00ff00\'><small>PORTSIDE</small></font>"
                connectionBuilder.append(textSource)
            }

            if (megaphoneVM.isRightPayloadConnect.value == false) {
                val textSource = "<font color=\'#ff0000\'><small>STARBOARD</small></font>"
                connectionBuilder.append("  " + textSource)
            } else {
                val textSource = "<font color=\'#00ff00\'><small>STARBOARD</small></font>"
                connectionBuilder.append("  " + textSource)
            }

            if (megaphoneVM.isUpPayloadConnect.value == false) {
                val textSource = "<font color=\'#ff0000\'><small>UPSIDE</small></font>"
                connectionBuilder.append("  " + textSource)
            } else {
                val textSource = "<font color=\'#00ff00\'><small>UPSIDE</small></font>"
                connectionBuilder.append("  " + textSource)
            }

            if (megaphoneVM.isOSDKPayloadConnect.value == false) {
                val textSource = "<font color=\'#ff0000\'><small>OSDK</small></font>"
                connectionBuilder.append("  " + textSource)
            } else {
                val textSource = "<font color=\'#00ff00\'><small>OSDK</small></font>"
                connectionBuilder.append("  " + textSource)
            }

            if (megaphoneVM.isPort1Connect.value == false) {
                val textSource = "<font color=\'#ff0000\'><small>PORT_1</small></font>"
                connectionBuilder.append("  " + textSource)
            } else {
                val textSource = "<font color=\'#00ff00\'><small>PORT_1</small></font>"
                connectionBuilder.append("  " + textSource)
            }

            if (megaphoneVM.isPort2Connect.value == false) {
                val textSource = "<font color=\'#ff0000\'><small>PORT_2</small></font>"
                connectionBuilder.append("  " + textSource)
            } else {
                val textSource = "<font color=\'#00ff00\'><small>PORT_2</small></font>"
                connectionBuilder.append("  " + textSource)
            }

            if (megaphoneVM.isPort3Connect.value == false) {
                val textSource = "<font color=\'#ff0000\'><small>PORT_3</small></font>"
                connectionBuilder.append("  " + textSource)
            } else {
                val textSource = "<font color=\'#00ff00\'><small>PORT_3</small></font>"
                connectionBuilder.append("  " + textSource)
            }

            if (megaphoneVM.isPort4Connect.value == false) {
                val textSource = "<font color=\'#ff0000\'><small>PORT_4</small></font>"
                connectionBuilder.append("  " + textSource)
            } else {
                val textSource = "<font color=\'#00ff00\'><small>PORT_4</small></font>"
                connectionBuilder.append("  " + textSource)
            }
            binding?.tvMegaphoneConnectStatus?.text = Html.fromHtml(connectionBuilder.toString())
        }
    }

    private fun updateMegaphonePlayState() {
        mainHandler.post {
            curPlayMode = megaphoneVM.megaphonePlayState.value?.let { it.playMode }
                ?: let { PlayMode.UNKNOWN }
            if (curPlayMode == PlayMode.SINGLE) {
                binding?.btnPlayMode?.setImageResource(R.drawable.ic_action_playback_repeat_1)
            } else {
                binding?.btnPlayMode?.setImageResource(R.drawable.ic_action_playback_repeat)
            }
            binding?.tvMegaphoneState?.text = megaphoneVM.megaphonePlayState.value?.let { it.status.toString() } ?: let { "N/A" }
            binding?.sbVolumeControl?.progress = megaphoneVM.megaphonePlayState.value?.let { it.volume } ?: let { 0 }
            val textSource =
                getString(R.string.cur_volume_value) + "${megaphoneVM.megaphonePlayState.value?.let { it.volume } ?: let { 0 }}"
            binding?.tvCurVolumeValue?.text = textSource

            val megaphoneStatus = megaphoneVM.megaphonePlayState.value?.let { it.status } ?: let { MegaphoneStatus.UNKNOWN }
            if (megaphoneStatus == MegaphoneStatus.PLAYING) {
                binding?.btnPlayControl?.setImageResource(R.drawable.ic_media_stop)
                isPlaying = true
            } else {
                binding?.btnPlayControl?.setImageResource(R.drawable.ic_media_play)
                isPlaying = false
            }

            // 支持的机型且喊话器处于 IDLE 状态时，显示警示灯控制布局
            val isM5 = ProductUtil.isM5Product()
            val isIdle = megaphoneStatus == MegaphoneStatus.IDLE
            binding?.rlWarminglightControl?.visibility =
                if (isM5 && isIdle) View.VISIBLE else View.GONE
        }
    }
}