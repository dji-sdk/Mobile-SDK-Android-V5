package dji.sampleV5.moduleaircraft.pages

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.models.MegaphoneVM
import dji.sdk.keyvalue.key.PayloadKey
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.payload.MegaphoneSystemState
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.megaphone.MegaphoneStatus
import dji.v5.manager.aircraft.megaphone.PlayMode
import dji.v5.manager.aircraft.megaphone.WorkMode
import dji.v5.utils.common.LogUtils
import kotlinx.android.synthetic.main.frag_megaphone_page.*

/**
 * Description : Megaphone fragment
 * Author : daniel.chen
 * CreateDate : 2022/1/17 2:47 下午
 * Copyright : ©2021 DJI All Rights Reserved.
 */
class MegaphoneFragment : DJIFragment() {
    private val megaphoneVM: MegaphoneVM by activityViewModels()


    private var curPlayMode: PlayMode = PlayMode.UNKNOWN
    private lateinit var curWorkMode: WorkMode
    private var isPlaying: Boolean = false
    private val TAG: String = "MegaphoneFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_megaphone_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /**
         * 初始化监听，同时拿一次当前喊话器的状态
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
        }

        megaphoneVM.isPayloadConnect.observe(viewLifecycleOwner) {
            updateMegaphoneConnectState()
        }

        megaphoneVM.getPlayMode(object : CommonCallbacks.CompletionCallbackWithParam<PlayMode> {
            override fun onSuccess(t: PlayMode?) {
                curPlayMode = t!!
                if (curPlayMode == PlayMode.SINGLE) {
                    btn_play_mode.setImageResource(R.drawable.ic_action_playback_repeat_1)
                } else {
                    btn_play_mode.setImageResource(R.drawable.ic_action_playback_repeat)
                }
            }

            override fun onFailure(error: IDJIError) {
                LogUtils.e(TAG, "get play mode failed ${error}")
            }
        })

        megaphoneVM.getStatus(object :
            CommonCallbacks.CompletionCallbackWithParam<MegaphoneStatus> {
            override fun onSuccess(t: MegaphoneStatus?) {
                tv_megaphone_state.text = t!!.name
            }

            override fun onFailure(error: IDJIError) {
                LogUtils.e(TAG, "get system status onFailure,$error")
            }
        })

        megaphoneVM.getVolume(object : CommonCallbacks.CompletionCallbackWithParam<Int> {
            override fun onSuccess(t: Int?) {
                sb_volume_control.progress = t!!
            }

            override fun onFailure(error: IDJIError) {
                LogUtils.e(TAG, "get volume onFailure,$error")
            }
        })

        KeyManager.getInstance()
            .listen(
                KeyTools.createKey(PayloadKey.KeyMegaphonePlayState, ComponentIndexType.UP),
                this
            ) { _, newValue ->
                newValue?.let {
                    curPlayMode = PlayMode.find(newValue.playMode.value())
                    if (curPlayMode == PlayMode.SINGLE) {
                        btn_play_mode.setImageResource(R.drawable.ic_action_playback_repeat_1)
                    } else {
                        btn_play_mode.setImageResource(R.drawable.ic_action_playback_repeat)
                    }

                    tv_megaphone_state.text = newValue.state.toString()

                    sb_volume_control.progress = newValue.volume
                    val textSource =
                        getString(R.string.cur_volume_value) + "${newValue.volume}"
                    tv_cur_volume_value.text = textSource

                    if (newValue.state == MegaphoneSystemState.PLAYING) {
                        btn_play_control.setImageResource(R.drawable.ic_media_stop)
                        isPlaying = true
                    } else {
                        btn_play_control.setImageResource(R.drawable.ic_media_play)
                        isPlaying = false
                    }
                }
            }
    }

    private fun initBtnListener() {
        btn_play_mode.setOnClickListener {
            setPlayMode()
        }

        btn_play_control.setOnClickListener {
            setPlayControl()
        }

        sb_volume_control.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                LogUtils.e(TAG, "volume change")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                LogUtils.e(TAG, "volume touch")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                megaphoneVM.setVolume(seekBar!!.progress,
                    object : CommonCallbacks.CompletionCallback {
                        override fun onSuccess() {
                            ToastUtils.showToast(context, "set volume success")
                        }

                        override fun onFailure(error: IDJIError) {
                            ToastUtils.showToast(context, "set volume failed: $error")
                        }
                    })
            }
        })
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
                    btn_play_mode.setImageResource(R.drawable.ic_action_playback_repeat_1)
                } else {
                    btn_play_mode.setImageResource(R.drawable.ic_action_playback_repeat)
                }
                ToastUtils.showToast(context, "set playmode to $curPlayMode success")
            }

            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast(context, "set playmode to $tempPlayMode failed: $error")
            }

        })
    }

    private fun setPlayControl() {
        if (!isPlaying) {
            megaphoneVM.startPlay(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    isPlaying = true
                    btn_play_control.setImageResource(R.drawable.ic_media_stop)
                    ToastUtils.showToast(context, "start play success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast(context, "start play failed: $error")
                }
            })
        } else {
            megaphoneVM.stopPlay(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    isPlaying = false
                    btn_play_control.setImageResource(R.drawable.ic_media_play)
                    ToastUtils.showToast(context, "stop play success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast(context, "stop play failed: $error")
                }
            })
        }
    }

    private fun changeFragment() {
        //fragment之间的切换
        tts_frg_btn.setOnClickListener {
            megaphoneVM.setWorkMode(WorkMode.TTS, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast(context, "set work mode to ${WorkMode.TTS} success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast(
                        context,
                        "set work mode to ${WorkMode.TTS} failed: $error"
                    )
                }
            })

            val transaction: FragmentTransaction = requireFragmentManager().beginTransaction()
            val ttsFragment = TTSFragment()
            transaction.replace(R.id.fragment_container, ttsFragment)
            transaction.commit()
        }

        record_frg_btn.setOnClickListener {
            megaphoneVM.setWorkMode(WorkMode.VOICE, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast(context, "set work mode to ${WorkMode.VOICE} success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast(
                        context,
                        "set work mode to ${WorkMode.VOICE} failed: $error"
                    )
                }
            })

            val transaction: FragmentTransaction = requireFragmentManager().beginTransaction()
            val recordFragment = RecordFragment()
            transaction.replace(R.id.fragment_container, recordFragment)
            transaction.commit()
        }

        file_list_frg_btn.setOnClickListener {
            megaphoneVM.setWorkMode(WorkMode.VOICE, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast(context, "set work mode to ${WorkMode.VOICE} success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast(
                        context,
                        "set work mode to ${WorkMode.VOICE} failed: $error"
                    )
                }
            })

            val transaction: FragmentTransaction = requireFragmentManager().beginTransaction()
            val localFileFragment = LocalFileFragment()
            transaction.replace(R.id.fragment_container, localFileFragment!!)
            transaction.commit()
        }
    }

    private fun updateUploadStatus() {
        var builder = StringBuilder()
        builder.append("Cur sent bytes: ").append(megaphoneVM.curRealTimeSentBytes.value)
        builder.append("\n")
        builder.append("Total bytes: ").append(megaphoneVM.curRealTimeTotalBytes.value)
        builder.append("\n")
        builder.append("Cur state: ").append(megaphoneVM.curRealTimeUploadedState.value)
        builder.append("\n")
        mainHandler.post {
            et_upload_status.text = builder.toString()
        }
    }

    private fun updateMegaphoneConnectState() {
        mainHandler.post {
            if (megaphoneVM.isPayloadConnect.value == false) {
                val textSource =
                    getString(R.string.title_megaphone) + "<font color=\'#ff0000\'><small>disconnect</small></font>"
                tv_megaphone_title.text = Html.fromHtml(textSource)
            } else {
                val textSource =
                    getString(R.string.title_megaphone) + "<font color=\'#00ff00\'><small>connect</small></font>"
                tv_megaphone_title.text = Html.fromHtml(textSource)
            }
        }
    }
}