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

import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.megaphone.*
import dji.v5.utils.common.LogUtils
import dji.sampleV5.aircraft.util.ToastUtils
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
        }
    }
}