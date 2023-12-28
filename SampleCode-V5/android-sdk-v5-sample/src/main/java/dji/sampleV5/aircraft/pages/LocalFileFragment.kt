package dji.sampleV5.aircraft.pages

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.models.MegaphoneVM
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.megaphone.FileInfo
import dji.v5.manager.aircraft.megaphone.UploadType
import dji.v5.utils.common.ContextUtil
import dji.v5.utils.common.FileUtils
import kotlinx.android.synthetic.main.frag_local_file.*
import java.io.File

/**
 * Description : 本地文件Fragment
 * Author : daniel.chen
 * CreateDate : 2022/1/18 7:41 下午
 * Copyright : ©2021 DJI All Rights Reserved.
 */
class LocalFileFragment: DJIFragment(){
    private val megaphoneVM: MegaphoneVM by activityViewModels()
    private val REQUEST_CODE = 100
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_local_file,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBtnListener()
    }

    fun initBtnListener(){
        btn_choose_local_file.setOnClickListener {
            pickFile()
        }

        btn_start_upload.setOnClickListener {
            if (et_local_file_path!!.text != null) {
                val filePath:String = et_local_file_path!!.text.toString()
                val file = File(filePath)
                if(file.exists()){
                    val fileInfo = FileInfo(
                        UploadType.VOICE_FILE,
                        file,
                        null
                    )
                    megaphoneVM.pushFileToMegaphone(
                        fileInfo,
                        object : CommonCallbacks.CompletionCallbackWithProgress<Int> {
                            override fun onProgressUpdate(progress: Int) {
                                tv_local_file_upload_status.text = progress.toString()
                            }

                            override fun onSuccess() {
                                tv_local_file_upload_status.text = "upload success"
                            }

                            override fun onFailure(error: IDJIError) {
                                tv_local_file_upload_status.text = "upload failed"
                            }
                        })
                }
            }
        }

        btn_start_upload_last_opus_file.setOnClickListener {
            val fileName = "AudioTest.opus"
            val file = File(
                FileUtils.getMainDir(
                    ContextUtil.getContext().getExternalFilesDir(""),
                    "RecordFile"
                ), fileName
            )
            if(file.exists()){
                val fileInfo = FileInfo(
                    UploadType.VOICE_FILE,
                    file,
                    null
                )
                megaphoneVM.pushFileToMegaphone(
                    fileInfo,
                    object : CommonCallbacks.CompletionCallbackWithProgress<Int> {
                        override fun onProgressUpdate(progress: Int) {
                            tv_local_file_upload_status.text = progress.toString()
                        }

                        override fun onSuccess() {
                            tv_local_file_upload_status.text = "upload success"
                        }

                        override fun onFailure(error: IDJIError) {
                            tv_local_file_upload_status.text = "upload failed"
                        }
                    })
            }
        }

        btn_cancel_upload.setOnClickListener {
            megaphoneVM.stopPushingFile(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    tv_local_file_upload_status.text = "upload cancel success"
                }

                override fun onFailure(error: IDJIError) {
                    tv_local_file_upload_status.text = "upload cancel failed"
                }
            })
        }
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        this.startActivityForResult(intent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
        data?.apply {
            getData()?.let {
                et_local_file_path.setText(getPath(context, it))
            }
        }
    }
    fun getPath(context: Context?, uri: Uri?): String {
        if (DocumentsContract.isDocumentUri(context, uri) && isExternalStorageDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).toTypedArray()
            if (split.size != 2 ) {
                return ""
            }
            val type = split[0]
            if ("primary".equals(type, ignoreCase = true)) {
                return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            }
        }
        return ""
    }

    fun isExternalStorageDocument(uri: Uri?): Boolean {
        return "com.android.externalstorage.documents" == uri?.authority
    }
}