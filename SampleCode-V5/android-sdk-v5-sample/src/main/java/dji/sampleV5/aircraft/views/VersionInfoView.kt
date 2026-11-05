package dji.sampleV5.aircraft.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.data.source.VersionInfo
import dji.sampleV5.aircraft.databinding.LayoutVersionInfoBinding
import dji.sampleV5.aircraft.models.VersionInfoVm
import dji.sampleV5.aircraft.util.DialogUtil
import dji.v5.utils.common.DjiSharedPreferencesManager
import dji.v5.utils.common.StringUtils
import dji.v5.ux.util.Helper
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VersionInfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleRes) {

    private var binding: LayoutVersionInfoBinding = LayoutVersionInfoBinding.inflate(LayoutInflater.from(context),this)
    private val versionInfoVm: VersionInfoVm by (context as ComponentActivity).viewModels()

    private val disposables = CompositeDisposable()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        versionInfoVm.getCurrentVersionInfo().observeOn(AndroidSchedulers.mainThread()).subscribe({
            refreshCurrentVersionInfo(it)
        }, {
            // 忽略
        }).addTo(disposables)

        versionInfoVm.listenLatestVersionInfo().observeOn(AndroidSchedulers.mainThread()).subscribe({
            refreshLatestVersionInfo(it.first, it.second)
        }, {
        }).addTo(disposables)

        versionInfoVm.refreshLatestVersionInfo()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposables.clear()
        disposables.dispose()
    }

    private fun refreshCurrentVersionInfo(currentVersionInfo: VersionInfo) {
        binding.scrollNews.visibility = View.VISIBLE
        showVersionInfoTo(binding.itemCurrentVersionInfo, currentVersionInfo, R.string.release_note_tile_current)
    }

    /**
     * @param latest 最新版本信息
     * @param latestGreaterThan 最新版本是否大于当前版本，true:大于
     */
    private fun refreshLatestVersionInfo(latest: VersionInfo, latestGreaterThan: Boolean) {
        binding.scrollNews.visibility = View.VISIBLE
        // 有新版本时
        showVersionInfoTo(
            binding.itemLatestVersionInfo,
            latest,
            R.string.release_note_tile_latest,
            latestGreaterThan && !isClickedLatestVersionInfo(latest)
        )

        binding.itemLatestVersionInfo.setOnClickListener {
            DjiSharedPreferencesManager.putString(context, SP_KEY_CLICKED_LATEST_VERSION_INFO_STRING, latest.versionName)
            showVersionInfoTo(binding.itemLatestVersionInfo, latest, R.string.release_note_tile_latest, !isClickedLatestVersionInfo(latest))

            DialogUtil.showTwoButtonDialog(context, StringUtils.getResStr(R.string.news_dialog_content_tips),
                StringUtils.getResStr(R.string.news_dialog_btn_left), {
                    Helper.startBrowser(context, StringUtils.getResStr(R.string.release_node_url))
                }, StringUtils.getResStr(R.string.news_dialog_btn_right), {
                })
        }
    }

    private fun showVersionInfoTo(itemNewsLayout: ItemNewsLayout, versionInfo: VersionInfo, titleResId: Int, isShowAlert: Boolean = false) {
        itemNewsLayout.visibility = View.VISIBLE
        itemNewsLayout.apply {
            setTitle(context.getString(titleResId, versionInfo.versionName))
            setDate(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(versionInfo.releaseTimeStamp * 1000)))
            setDescription(versionInfo.releaseNode)
            showAlert(isShowAlert)
        }
    }

    private fun isClickedLatestVersionInfo(latest: VersionInfo): Boolean {
        val clickedVersionName = DjiSharedPreferencesManager.getString(context, SP_KEY_CLICKED_LATEST_VERSION_INFO_STRING, "")
        // 空字符串、或者版本信息不匹配时表示没有点击过
        return clickedVersionName.equals(latest.versionName)
    }

    companion object {
        private const val SP_KEY_CLICKED_LATEST_VERSION_INFO_STRING = "sp_key_clicked_latest_version_info_string"
    }

}