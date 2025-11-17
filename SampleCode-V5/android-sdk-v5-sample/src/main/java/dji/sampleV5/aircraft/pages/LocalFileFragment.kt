package dji.sampleV5.aircraft.pages

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import dji.sampleV5.aircraft.databinding.FragLocalFileBinding
import dji.sampleV5.aircraft.models.MegaphoneVM
import dji.sampleV5.aircraft.util.ToastUtils
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.recorder.PCMTools
import dji.v5.manager.aircraft.megaphone.FileInfo
import dji.v5.manager.aircraft.megaphone.UploadType
import dji.v5.utils.common.ContextUtil
import dji.v5.utils.common.DJIExecutor
import dji.v5.utils.common.DeviceInfoUtil.getPackageName
import dji.v5.utils.common.DocumentUtil
import dji.v5.utils.common.FileUtils
import java.io.File

/**
 * Description : 本地文件Fragment
 * Author : daniel.chen
 * CreateDate : 2022/1/18 7:41 下午
 * Copyright : ©2021 DJI All Rights Reserved.
 */
class LocalFileFragment : DJIFragment() {
    private val megaphoneVM: MegaphoneVM by activityViewModels()
    private val REQUEST_CODE = 100
    private var binding: FragLocalFileBinding? = null
    private val OPEN_DOCUMENT_TREE = 1
    private val OPEN_MANAGE_EXTERNAL_STORAGE = 2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragLocalFileBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBtnListener()
    }

    private fun initBtnListener() {
        binding?.btnChooseLocalFile?.setOnClickListener {
            showFileChooser()
        }

        binding?.btnConvertToOpus?.setOnClickListener {
            if (binding?.etLocalFilePath?.text != null) {
                DJIExecutor.getExecutor().execute {
                    val filePath: String = binding?.etLocalFilePath?.text.toString()
                    val pcmFilePath = PCMTools.convertToPcmFileSync(filePath)
                    if (TextUtils.isEmpty(pcmFilePath)) {
                        ToastUtils.showToast("error Path :$filePath")
                        return@execute
                    }
                    val opusPath = PCMTools.convertToOpusFileSync(pcmFilePath)
                    activity?.runOnUiThread {
                        ToastUtils.showToast("Opus Path :$opusPath")
                        binding?.etLocalFilePath?.setText(opusPath)
                    }
                }
            }
        }

        binding?.btnStartUpload?.setOnClickListener {
            if (binding?.etLocalFilePath?.text != null) {
                val filePath: String = binding?.etLocalFilePath?.text.toString()
                val file = File(filePath)
                if (file.exists()) {
                    val fileInfo = FileInfo(
                        UploadType.VOICE_FILE,
                        file,
                        null
                    )
                    megaphoneVM.pushFileToMegaphone(
                        fileInfo,
                        object : CommonCallbacks.CompletionCallbackWithProgress<Int> {
                            override fun onProgressUpdate(progress: Int) {
                                binding?.tvLocalFileUploadStatus?.text = progress.toString()
                            }

                            override fun onSuccess() {
                                binding?.tvLocalFileUploadStatus?.text = "upload success"
                            }

                            override fun onFailure(error: IDJIError) {
                                binding?.tvLocalFileUploadStatus?.text = "upload failed"
                            }
                        })
                }
            }
        }

        binding?.btnStartUploadLastOpusFile?.setOnClickListener {
            val fileName = "AudioTest.opus"
            val file = File(
                FileUtils.getMainDir(
                    ContextUtil.getContext().getExternalFilesDir(""),
                    "RecordFile"
                ), fileName
            )
            if (file.exists()) {
                val fileInfo = FileInfo(
                    UploadType.VOICE_FILE,
                    file,
                    null
                )
                megaphoneVM.pushFileToMegaphone(
                    fileInfo,
                    object : CommonCallbacks.CompletionCallbackWithProgress<Int> {
                        override fun onProgressUpdate(progress: Int) {
                            binding?.tvLocalFileUploadStatus?.text = progress.toString()
                        }

                        override fun onSuccess() {
                            binding?.tvLocalFileUploadStatus?.text = "upload success"
                        }

                        override fun onFailure(error: IDJIError) {
                            binding?.tvLocalFileUploadStatus?.text = "upload failed"
                        }
                    })
            }
        }

        binding?.btnCancelUpload?.setOnClickListener {
            megaphoneVM.stopPushingFile(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    binding?.tvLocalFileUploadStatus?.text = "upload cancel success"
                }

                override fun onFailure(error: IDJIError) {
                    binding?.tvLocalFileUploadStatus?.text = "upload cancel failed"
                }
            })
        }
    }

    var lancher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result?.apply {
            data?.apply {
                data?.let {
                    binding?.etLocalFilePath?.setText(getPath(context, it))
                }
            }
        }
    }

    private fun showFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        lancher.launch(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == OPEN_DOCUMENT_TREE) {
            grantUriPermission(data)
        }


        if (requestCode == OPEN_MANAGE_EXTERNAL_STORAGE
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            showFileChooser()
        }
    }

    @SuppressLint("WrongConstant")
    private fun grantUriPermission(data: Intent?) {
        val uri = data!!.data
        requireActivity().grantUriPermission(
            getPackageName(), uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        val takeFlags = data.flags and (Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_READ_URI_PERMISSION)
        requireActivity().contentResolver.takePersistableUriPermission(uri!!, takeFlags)
    }

    private fun getPath(context: Context?, uri: Uri?): String {
        if (DocumentsContract.isDocumentUri(context, uri) && isExternalStorageDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).toTypedArray()
            if (split.size != 2) {
                return ""
            }
            val type = split[0]
            if ("primary".equals(type, ignoreCase = true)) {
                return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            }
        }
        return ""
    }

    private fun isExternalStorageDocument(uri: Uri?): Boolean {
        return "com.android.externalstorage.documents" == uri?.authority
    }
}