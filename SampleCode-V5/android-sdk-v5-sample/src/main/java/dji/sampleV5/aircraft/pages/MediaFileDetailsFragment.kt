package dji.sampleV5.aircraft.pages

import android.graphics.Bitmap
import android.os.Bundle
import android.transition.Fade
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.data.MEDIA_FILE_DETAILS_STR
import dji.sampleV5.aircraft.databinding.FragMediafileDetailsBinding
import dji.v5.ux.util.ToastUtils
import dji.sdk.keyvalue.value.camera.MediaFileType
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.datacenter.media.MediaFile
import dji.v5.manager.datacenter.media.MediaFileDownloadListener
import dji.v5.manager.intelligent.media.ThermalPhotoInfo
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
        binding?.parseThermal?.setOnClickListener(this)
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

            R.id.parse_thermal -> {
                parseThermalInfo()
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

    private fun parseThermalInfo() {
        val filepath = DiskUtil.getExternalCacheDirPath(ContextUtil.getContext(), mediaFileDir + "/" + mediaFile?.fileName)
        if (!File(filepath).exists()) {
            ToastUtils.showToast("Please download the origin file first")
            return
        }
        MediaDataCenter.getInstance().mediaManager.parseThermalTemperature(filepath,
            object : CommonCallbacks.CompletionCallbackWithParam<ThermalPhotoInfo> {
                override fun onSuccess(info: ThermalPhotoInfo) {
                    val centerTemperature = info.getTemperatureByOriginXY(info.originWidth / 2, info.originHeight / 2)
                    // 自校验1: 统计坏值(NaN/Inf/超合理范围)并定位最高温像素
                    var invalidCount = 0
                    var maxX = 0
                    var maxY = 0
                    var max = -Float.MAX_VALUE
                    for (y in 0 until info.thermalHeight) {
                        val row = info.temperatures[y]
                        for (x in 0 until info.thermalWidth) {
                            val t = row[x]
                            if (t.isNaN() || t.isInfinite() || t < -40f || t > 1000f) {
                                invalidCount++
                            } else if (t > max) {
                                max = t
                                maxX = x
                                maxY = y
                            }
                        }
                    }
                    // 自校验2: 温度阵与照片灰度的相关性(同方向应强正相关, 证明温度与画面像素级对应)
                    val correlation = correlateWithImage(filepath, info)
                    val valid = invalidCount == 0 && correlation > 0.8f
                    val verdict = if (valid) "VALID (数据有效)" else "SUSPICIOUS (数据可疑, 见下方指标)"
                    val msg = """
                        temperatures: $verdict

                        thermal: ${info.thermalWidth} x ${info.thermalHeight}
                        origin: ${info.originWidth} x ${info.originHeight}
                        maxTemperature: ${info.maxTemperature} ℃
                        minTemperature: ${info.minTemperature} ℃
                        centerTemperature: $centerTemperature ℃
                        emissivity: ${info.emissivity}
                        ambientTemperature: ${info.ambientTemperature} ℃
                        captureTime: ${info.captureTime}
                        --- validation ---
                        hottestAt(thermal): ($maxX, $maxY)
                        invalidValues: $invalidCount / ${info.thermalWidth * info.thermalHeight}
                        imageCorrelation: $correlation
                    """.trimIndent()
                    // 回调在工作线程，弹窗需切主线程
                    activity?.runOnUiThread {
                        context?.let {
                            AlertDialog.Builder(it, androidx.appcompat.R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
                                .setTitle("Thermal Parse Result")
                                .setMessage(msg)
                                .setPositiveButton(R.string.ad_confirm) { dialog, _ -> dialog.dismiss() }
                                .create().show()
                        }
                    }
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast(error.toString())
                }
            })
    }

    /**
     * 温度阵与照片灰度的 Pearson 相关性。红外图(如 WhiteHot)越热越亮，
     * 相关性高(如 >0.8)即证明温度数据与画面像素级对应、方向一致。
     */
    private fun correlateWithImage(filepath: String, info: ThermalPhotoInfo): Float {
        return try {
            val src = android.graphics.BitmapFactory.decodeFile(filepath) ?: return Float.NaN
            val bmp = Bitmap.createScaledBitmap(src, info.thermalWidth, info.thermalHeight, true)
            if (bmp != src) {
                src.recycle()
            }
            val w = info.thermalWidth
            val h = info.thermalHeight
            val n = w * h
            val pixels = IntArray(n)
            bmp.getPixels(pixels, 0, w, 0, 0, w, h)
            bmp.recycle()
            var sumG = 0.0
            var sumT = 0.0
            for (y in 0 until h) {
                val row = info.temperatures[y]
                for (x in 0 until w) {
                    val p = pixels[y * w + x]
                    sumG += ((p shr 16 and 0xFF) + (p shr 8 and 0xFF) + (p and 0xFF)) / 3.0
                    sumT += row[x]
                }
            }
            val meanG = sumG / n
            val meanT = sumT / n
            var cov = 0.0
            var varG = 0.0
            var varT = 0.0
            for (y in 0 until h) {
                val row = info.temperatures[y]
                for (x in 0 until w) {
                    val p = pixels[y * w + x]
                    val g = ((p shr 16 and 0xFF) + (p shr 8 and 0xFF) + (p and 0xFF)) / 3.0 - meanG
                    val t = row[x] - meanT
                    cov += g * t
                    varG += g * g
                    varT += t * t
                }
            }
            if (varG <= 0 || varT <= 0) Float.NaN else (cov / Math.sqrt(varG * varT)).toFloat()
        } catch (e: Exception) {
            LogUtils.e("MediaFile", "correlateWithImage error: " + e.message)
            Float.NaN
        }
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