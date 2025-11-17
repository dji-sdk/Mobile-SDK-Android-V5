package dji.sampleV5.aircraft.pages

import android.graphics.Bitmap
import android.os.Bundle
import android.transition.Fade
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.data.MEDIA_FILE_DETAILS_STR
import dji.sampleV5.aircraft.databinding.FragMediafileDetailsBinding
import dji.sampleV5.aircraft.util.ToastUtils
import dji.sdk.keyvalue.value.camera.MediaFileType
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.datacenter.media.MediaFile
import dji.v5.manager.datacenter.media.MediaFileDownloadListener
import dji.v5.utils.common.ContextUtil
import dji.v5.utils.common.DiskUtil
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.StringUtils
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * @author feel.feng
 * @time 2022/04/25 9:18 下午
 * @description:
 */
class MediaFileDetailsFragment : DJIFragment(), View.OnClickListener {

    private var binding: FragMediafileDetailsBinding? = null

    var mediaFile: MediaFile? = null
    var mediaFileDir = "/mediafile"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragMediafileDetailsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initTransition()
        initView(view)
    }

    private fun initView(view: View) {
        mediaFile = arguments?.getSerializable(MEDIA_FILE_DETAILS_STR) as MediaFile
        binding?.image?.setImageBitmap(mediaFile?.thumbNail)
        binding?.image?.setOnClickListener(this)
        binding?.previewFile?.setOnClickListener(this)
        binding?.downloadFile?.setOnClickListener(this)
        binding?.cancelDownload?.setOnClickListener(this)
        binding?.pullXmpFileData?.setOnClickListener(this)
        binding?.pullXmpCustomInfo?.setOnClickListener(this)
    }


    private fun initTransition() {
        sharedElementEnterTransition = DetailsTransition()
        enterTransition = Fade()
        sharedElementReturnTransition = DetailsTransition()
        exitTransition = Fade()

    }

    override fun onDestroy() {
        super.onDestroy()
        mediaFile?.release()
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.preview_file -> {
                fetchPreview()
            }

            R.id.download_file -> {
                downloadFile()
            }

            R.id.cancel_download -> {
                cancleDownload()
            }

            R.id.image -> {
                if (mediaFile?.fileType == MediaFileType.MP4 || mediaFile?.fileType == MediaFileType.MOV) {
                    enterVideoPage()
                }
            }

            R.id.pull_xmp_file_data -> {
                pullXMPFileDataFromCamera()
            }

            R.id.pull_xmp_custom_info -> {
                pullXMPCustomInfoFromCamera()
            }
        }
    }

    private fun enterVideoPage() {
        binding?.image?.let {
            Navigation.findNavController(it).navigate(
                R.id.video_play_page,
                bundleOf(
                    MEDIA_FILE_DETAILS_STR to mediaFile
                ),
            )
        }
    }

    private fun fetchPreview() {
        mediaFile?.pullPreviewFromCamera(object :
            CommonCallbacks.CompletionCallbackWithParam<Bitmap> {
            override fun onSuccess(t: Bitmap?) {

                AndroidSchedulers.mainThread().scheduleDirect {
                    binding?.image?.let {
                        Glide.with(ContextUtil.getContext()).load(t).into(it)
                    }
                }
            }

            override fun onFailure(error: IDJIError) {
                LogUtils.e("MediaFile", "fetch preview failed$error")
            }

        })
    }

    private fun downloadFile() {
        val dirs = File(DiskUtil.getExternalCacheDirPath(ContextUtil.getContext(), mediaFileDir))
        if (!dirs.exists()) {
            dirs.mkdirs()
        }

        val filepath = DiskUtil.getExternalCacheDirPath(ContextUtil.getContext(), mediaFileDir + "/" + mediaFile?.fileName)
        val file = File(filepath)
        var offset = 0L
        if (file.exists()) {
            offset = file.length();
        }
        val outputStream = FileOutputStream(file, true)
        val bos = BufferedOutputStream(outputStream)
        val beginTime = System.currentTimeMillis()
        mediaFile?.pullOriginalMediaFileFromCamera(offset, object : MediaFileDownloadListener {
            override fun onStart() {
                showProgress()
            }

            override fun onProgress(total: Long, current: Long) {
                updateProgress(offset, current, total)
            }

            override fun onRealtimeDataUpdate(data: ByteArray, position: Long) {
                try {
                    bos.write(data)
                    bos.flush()
                } catch (e: IOException) {
                    LogUtils.e("MediaFile", "write error" + e.message)
                }
            }

            override fun onFinish() {

                val spendTime = (System.currentTimeMillis() - beginTime)
                val speedBytePerMill: Float? = mediaFile?.fileSize?.div(spendTime.toFloat())
                val divs = 1000.div(1024 * 1024.toFloat());
                val speedKbPerSecond: Float? = speedBytePerMill?.times(divs)

                if (mediaFile!!.fileSize <= offset) {
                    ToastUtils.showToast(getString(R.string.already_download) + getString(R.string.msg_download_save_tips) + ":" + filepath)
                } else {
                    ToastUtils.showToast(
                        getString(R.string.msg_download_compelete_tips) + "${speedKbPerSecond}Mbps"
                                + getString(R.string.msg_download_save_tips) + ":" + filepath
                    )
                }
                hideProgress()
                try {
                    outputStream.close()
                    bos.close()
                } catch (error: IOException) {
                    LogUtils.e("MediaFile", "close error$error")
                }
            }

            override fun onFailure(error: IDJIError?) {
                LogUtils.e("MediaFile", "download error$error")
            }

        })
    }

    private fun cancleDownload() {
        mediaFile?.stopPullOriginalMediaFileFromCamera(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                hideProgress()
            }

            override fun onFailure(error: IDJIError) {
                hideProgress()

            }
        })
    }

    private fun pullXMPFileDataFromCamera() {
        mediaFile?.pullXMPFileDataFromCamera(object :
            CommonCallbacks.CompletionCallbackWithParam<String> {
            override fun onSuccess(s: String) {
                ToastUtils.showToast(s)
            }

            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast(error.toString())
            }
        })
    }

    private fun pullXMPCustomInfoFromCamera() {
        mediaFile?.pullXMPCustomInfoFromCamera(object :
            CommonCallbacks.CompletionCallbackWithParam<String> {
            override fun onSuccess(s: String) {
                ToastUtils.showToast(s)
            }

            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast(error.toString())
            }
        })
    }

    fun showProgress() {
        binding?.layoutMediaPlayDownloadProgress?.progressContainer?.visibility = View.VISIBLE
    }

    fun updateProgress(offset: Long, currentsize: Long, total: Long) {
        val fullSize = offset + total;
        val downloadedSize = offset + currentsize
        binding?.layoutMediaPlayDownloadProgress?.progressBar?.max = fullSize.toInt()
        binding?.layoutMediaPlayDownloadProgress?.progressBar?.progress = downloadedSize.toInt()
        val data: Double = StringUtils.formatDouble((downloadedSize.toDouble() / fullSize.toDouble()))
        val result: String = StringUtils.formatDouble(data * 100, "#0").toString() + "%"
        binding?.layoutMediaPlayDownloadProgress?.progressInfo?.text = result
    }

    fun hideProgress() {
        binding?.layoutMediaPlayDownloadProgress?.progressContainer?.visibility = View.GONE
    }

}