package dji.sampleV5.modulecommon.pages

import android.graphics.Bitmap
import android.os.Bundle
import android.transition.Fade
import android.transition.TransitionInflater
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import dji.sampleV5.modulecommon.R
import dji.sampleV5.modulecommon.data.DJIToastResult
import dji.sampleV5.modulecommon.data.MEDIA_FILE_DETAILS_STR
import dji.sampleV5.modulecommon.models.MediaDetailsVM
import dji.sampleV5.modulecommon.models.MediaVM
import dji.sampleV5.modulecommon.util.Util
import dji.sdk.keyvalue.value.camera.MediaFileType
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.datacenter.media.MediaFile
import dji.v5.manager.datacenter.media.MediaFileDownloadListener
import dji.v5.utils.common.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.frag_mediafile_details.*
import kotlinx.android.synthetic.main.layout_media_play_download_progress.*
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import dji.sampleV5.modulecommon.util.ToastUtils

/**
 * @author feel.feng
 * @time 2022/04/25 9:18 下午
 * @description:
 */
class MediaFileDetailsFragment : DJIFragment(), View.OnClickListener {
    private val mediaDetailsVM: MediaDetailsVM by activityViewModels()
    var mediaFile: MediaFile? = null
    var mediaFileDir = "/mediafile"
    lateinit var image: ImageView

    @Nullable
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.frag_mediafile_details, container, false)
    }

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initTransition()
        initView(view)

    }

    private fun initView(view: View) {
        image = view.findViewById(R.id.image) as ImageView
        mediaFile = arguments?.getSerializable(MEDIA_FILE_DETAILS_STR) as MediaFile
        image.setImageBitmap(mediaFile?.thumbNail)

        image.setOnClickListener(this)
        preview_file.setOnClickListener(this)
        download_file.setOnClickListener(this)
        cancel_download.setOnClickListener(this)
        pull_xmp_file_data.setOnClickListener(this)
        pull_xmp_custom_info.setOnClickListener(this)
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
        Navigation.findNavController(image).navigate(
            R.id.video_play_page,
            bundleOf(
                MEDIA_FILE_DETAILS_STR to mediaFile
            ),
        )
    }

    private fun fetchPreview() {
        mediaFile?.pullPreviewFromCamera(object :
            CommonCallbacks.CompletionCallbackWithParam<Bitmap> {
            override fun onSuccess(t: Bitmap?) {

                AndroidSchedulers.mainThread().scheduleDirect {
                    //  image.setImageBitmap(t)
                    Glide.with(ContextUtil.getContext()).load(t).into(image)
                }
            }

            override fun onFailure(error: IDJIError) {
                LogUtils.e("MediaFile", "fetch preview failed$error")
            }

        })
    }

    private fun downloadFile() {
        val dirs: File = File(DiskUtil.getExternalCacheDirPath(ContextUtil.getContext(), mediaFileDir))
        if (!dirs.exists()) {
            dirs.mkdirs()
        }

        val filepath = DiskUtil.getExternalCacheDirPath(ContextUtil.getContext(), mediaFileDir + "/" + mediaFile?.fileName)
        val file: File = File(filepath)
        var offset: Long = 0L
        if (file.exists()) {
            offset = file.length();
        }
        val outputStream = FileOutputStream(file, true)
        val bos = BufferedOutputStream(outputStream)
        var beginTime = System.currentTimeMillis()
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

                var spendTime = (System.currentTimeMillis() - beginTime)
                var speedBytePerMill: Float? = mediaFile?.fileSize?.div(spendTime.toFloat())
                var divs = 1000.div(1024 * 1024.toFloat());
                var speedKbPerSecond: Float? = speedBytePerMill?.times(divs)

                if (mediaFile!!.fileSize <= offset) {
                    ToastUtils.showToast(getString(R.string.already_download))
                } else {
                    ToastUtils.showToast(
                        getString(R.string.msg_download_compelete_tips) + "${speedKbPerSecond}Mbps"
                                + getString(R.string.msg_download_save_tips) + "${filepath}"
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
        progressContainer.visibility = View.VISIBLE
    }

    fun updateProgress(offset: Long, currentsize: Long, total: Long) {
        val fullSize = offset + total;
        val downloadedSize = offset + currentsize
        progressBar.max = fullSize.toInt()
        progressBar.progress = downloadedSize.toInt()
        val data: Double = StringUtils.formatDouble((downloadedSize.toDouble() / fullSize.toDouble()))
        val result: String = StringUtils.formatDouble(data * 100, "#0").toString() + "%"
        progressInfo.text = result
    }

    fun hideProgress() {
        progressContainer.visibility = View.GONE
    }

}