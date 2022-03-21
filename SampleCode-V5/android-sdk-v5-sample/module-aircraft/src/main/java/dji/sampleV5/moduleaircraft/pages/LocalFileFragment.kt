package dji.sampleV5.moduleaircraft.pages

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
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
class LocalFileFragment:DJIFragment(){
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
            if(et_local_file_path!!.text!=null){
                val filePath:String = et_local_file_path!!.text.toString()
                val fileInfo = FileInfo(
                    UploadType.VOICE_FILE,
                    File(filePath),
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
        if (requestCode == REQUEST_CODE && resultCode == -1 && data != null) {
            val uri = data.data
            et_local_file_path.setText(uri?.let { getPath(ContextUtil.getContext(), it) })
        }
    }

    fun getPath(context: Context, uri: Uri): String? {
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val path = getExternalStorageDocument(uri)
                path?.let {
                    it
                }
            } else if (isDownloadsDocument(uri)) {
                return getDownLoadDocument(context, uri)
            } else if (isMediaDocument(uri)) {
                return getMediaDocument(context, uri)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            return getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    fun getExternalStorageDocument(uri: Uri):String{
        val docId = DocumentsContract.getDocumentId(uri)
        val split = docId.split(":".toRegex()).toTypedArray()
        val type = split[0]
        if ("primary".equals(type, ignoreCase = true)) {
            return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
        }
        return ""
    }

    fun getDownLoadDocument(context: Context, uri: Uri): String? {
        val id = DocumentsContract.getDocumentId(uri)
        val contentUri = ContentUris.withAppendedId(
            Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id)
        )
        return getDataColumn(
            context,
            contentUri,
            null,
            null
        )
    }

    fun getMediaDocument(context: Context, uri: Uri): String? {
        val docId = DocumentsContract.getDocumentId(uri)
        val split = docId.split(":".toRegex()).toTypedArray()
        val type = split[0]
        var contentUri: Uri? = null
        if ("image" == type) {
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else if ("video" == type) {
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else if ("audio" == type) {
            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val selection = "_id=?"
        val selectionArgs = arrayOf(
            split[1]
        )
        return getDataColumn(
            context,
            contentUri,
            selection,
            selectionArgs
        )
    }

    fun getDataColumn(
        context: Context, uri: Uri?, selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(
            column
        )
        try {
            cursor = context.contentResolver.query(
                uri!!, projection, selection, selectionArgs,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val column_index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(column_index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }


    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }


    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }
}