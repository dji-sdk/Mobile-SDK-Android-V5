package dji.sampleV5.aircraft.models

import android.annotation.SuppressLint
import dji.sampleV5.aircraft.data.source.VersionInfo
import dji.sampleV5.aircraft.data.source.VersionInfoRepository
import dji.v5.utils.inner.SDKConfig
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject

/**
 * 该VM负责了磁盘缓存逻辑
 */
class VersionInfoVm : DJIViewModel() {

    private val repository = VersionInfoRepository()

    private var latestVersionInfo: VersionInfo? = null

    /**
     * Pair中的Bool表示最新版本是否大于当前版本
     */
    private val latestVersionInfoSubject: BehaviorSubject<Pair<VersionInfo, Boolean>> =
        BehaviorSubject.create()

    /**
     * 已经截取版本号中的字母，仅保留数字和分隔符[.]
     */
    private val currentBuildVersion: String

    init {
        val version = cutoffLetterUntilDigit(SDKConfig.getInstance().registrationSDKVersion)
        val lastIndexOf = version.indexOf('-')
        currentBuildVersion = if (lastIndexOf > 0) {
            version.subSequence(0, lastIndexOf).toString()
        } else {
            version
        }
    }

    fun getCurrentVersionInfo(): Single<VersionInfo> {
        return repository.fetchCurrentVersionInfo(currentBuildVersion)
    }

    @SuppressLint("CheckResult")
    fun refreshLatestVersionInfo() {
        latestVersionInfo?.let {
            notifyLatestVersion(it)
            return
        }
        repository.fetchLatestVersionInfo(currentBuildVersion)
            .subscribe({
                notifyLatestVersion(it)
            }, {})
    }

    fun listenLatestVersionInfo(): Observable<Pair<VersionInfo, Boolean>> {
        return latestVersionInfoSubject.hide()
    }

    private fun notifyLatestVersion(versionInfo: VersionInfo) {
        val currentVersion = repository.getCurrentVersionExTest().ifEmpty { currentBuildVersion }
        if (currentVersion == versionInfo.versionName) {
            // 最新版本和当前版本一致则只显示当前版本
            // 不一致时不论当前版本大小都展示出来提醒用户 - 楠哥
            return
        }
        latestVersionInfoSubject.onNext(Pair(versionInfo, compareVersion(versionInfo.versionName, currentVersion) > 0))
    }

    private fun compareVersion(version1: String, version2: String): Int {
        val arr1 = version1.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val arr2 = version2.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val maxLen = arr1.size.coerceAtLeast(arr2.size)
        for (i in 0 until maxLen) {
            try {
                val num1 = if (i < arr1.size) arr1[i].toInt() else 0
                val num2 = if (i < arr2.size) arr2[i].toInt() else 0
                if (num1 == num2) {
                    continue
                }
                return num1 - num2
            } catch (_: NumberFormatException) {
                // do nothing
            }
        }
        return 1
    }

    /**
     * 移除字符串尾部字母，直到遇到数字或[.]
     */
    private fun cutoffLetterUntilDigit(text: String): String {
        val subIndex = text.toCharArray().reversed().run {
            forEachIndexed { index, c ->
                if (c.isDigit() || c == '.') {
                    return@run index
                }
            }
            return@run 0
        }
        return text.substring(0, (text.length - subIndex))
    }
}