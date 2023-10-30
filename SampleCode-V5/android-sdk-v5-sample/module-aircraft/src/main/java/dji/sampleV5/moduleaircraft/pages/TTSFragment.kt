package dji.sampleV5.moduleaircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.models.MegaphoneVM
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.megaphone.FileInfo
import dji.v5.manager.aircraft.megaphone.PlayMode
import dji.v5.manager.aircraft.megaphone.UploadType
import dji.v5.utils.common.LogUtils
import kotlinx.android.synthetic.main.frag_tts.*

/**
 * Description : TTS fragment
 * Author : daniel.chen
 * CreateDate : 2022/1/17 2:41 下午
 * Copyright : ©2021 DJI All Rights Reserved.
 */
class TTSFragment:DJIFragment(){
    private val megaphoneVM: MegaphoneVM by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_tts,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBtnListener()
    }

    private fun initBtnListener() {
        btn_tts.setOnClickListener {
            et_tts.text.let {
                var fileInfo = FileInfo(
                    UploadType.TTS_DATA,
                    null,
                    et_tts.text.toString().toByteArray(Charsets.UTF_8)
                )
                megaphoneVM.pushFileToMegaphone(fileInfo,
                    object : CommonCallbacks.CompletionCallbackWithProgress<Int> {
                        override fun onProgressUpdate(progress: Int) {
                            tv_tts_upload_status.text = "upload Progress：$progress"
                        }

                        override fun onSuccess() {
                            tv_tts_upload_status.text = "upload success!"
                            megaphoneVM.startPlay(object:CommonCallbacks.CompletionCallback{
                                override fun onSuccess() {
                                    LogUtils.i("TTS","Start Play Success")
                                }

                                override fun onFailure(error: IDJIError) {
                                    LogUtils.i("TTS","Start Play Failed")
                                }
                            })
                        }

                        override fun onFailure(error: IDJIError) {
                            tv_tts_upload_status.text = "upload failed: $error"
                        }
                    })
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        megaphoneVM.getPlayMode(object :CommonCallbacks.CompletionCallbackWithParam<PlayMode>{
            override fun onSuccess(t: PlayMode?) {
               if (t != PlayMode.UNKNOWN) {
                   megaphoneVM.stopPlay(null)
               }
            }

            override fun onFailure(error: IDJIError) {
               LogUtils.e(logTag , "Get Play Mode Failed!")
            }

        })

    }
}