package dji.sampleV5.aircraft.data.source

import android.annotation.SuppressLint
import androidx.annotation.WorkerThread
import com.google.gson.annotations.SerializedName
import dji.v5.common.error.IDJIError
import dji.v5.network.DJIHttpCallback
import dji.v5.network.DJIHttpRequest
import dji.v5.network.DJIHttpResponse
import dji.v5.network.DJINetworkManager
import dji.v5.utils.common.ContextUtil
import dji.v5.utils.common.FileUtils
import dji.v5.utils.common.JsonUtil
import dji.v5.utils.common.LogUtils
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.io.File
import java.util.*

/**
 * 该Repository负责简单的HTTP数据获取能力，以及数据存储和获取能力
 */
@SuppressLint("CheckResult")
class VersionInfoRepository {


    /**
     * 用于观察网路模块（DJINetworkManager）是否初始化完成，并不关心网络实际是否有效
     */
    private val networkInitSubject: BehaviorSubject<Boolean> = BehaviorSubject.create()

    private var hostConfig: HostConfig? = null

    private var currentVersionInfo: VersionInfo? = null

    init {
        Completable.create { emitter ->
            DJINetworkManager.getInstance().addNetworkStatusListener {
                if (it) {
                    emitter.onComplete()
                }
            }
        }.mergeWith(Completable.fromAction {
            hostConfig = getHostConfig()
        }.observeOn(Schedulers.io())).subscribe {
            // 通知网络初始化完成
            networkInitSubject.onNext(true)
        }
    }

    /**
     * 获取当前版本信息
     */
    fun fetchCurrentVersionInfo(versionName: String, language: String = getLanguageHeaderValue()): Single<VersionInfo> {
        val versionNameEx = getCurrentVersionExTest().ifEmpty { versionName }
        LogUtils.i(LOG_TAG, "getCurrentVersionInfo, $versionNameEx")

        val fileName = "version_info_${versionNameEx}_${language}.json"
        return Single.create<VersionInfo> {
            if (currentVersionInfo == null) {
                val currentVersionInfo = loadVersionInfoFromLocal(fileName)
                this.currentVersionInfo = currentVersionInfo
                it.onSuccess(currentVersionInfo)
                return@create
            }
            it.onError(VersionInfoCacheFileNotFound())
        }.onErrorResumeNext { fetchVersionInfoAndSave(versionNameEx, fileName, false, language) }
            .subscribeOn(Schedulers.io())
    }

    /**
     * 强制拉去最新版本信息
     */
    fun fetchLatestVersionInfo(versionName: String, language: String = getLanguageHeaderValue()): Observable<VersionInfo> {
        return Observable.create { emitter ->
            val fileName = "${FILE_NAME_LATEST_VERSION_INFO_CACHE}_${language}.json"
            // 1. 从本地文件加载并通知给客户端，用于尽快刷新UI
            Schedulers.io().scheduleDirect {
                try {
                    val versionInfo = loadVersionInfoFromLocal(fileName)
                    emitter.onNext(versionInfo)
                } catch (throwable: Throwable) {
                    //忽略
                    LogUtils.i(LOG_TAG, "error, $throwable")
                }
            }
            // 2. 从云端获取最新数据，需要则通知给客户端
            val fetchD = fetchVersionInfoAndSave(versionName, fileName, true, language)
                .observeOn(Schedulers.io())
                .subscribe({
                    emitter.onNext(it)
                    emitter.onComplete()
                }, {
                    if (emitter.isDisposed) {
                        return@subscribe
                    }
                    emitter.onError(it)
                })
            // 3. 请求被取消时取消云端请求
            emitter.setCancellable {
                fetchD.dispose()
            }
        }
    }

    private fun fetchVersionInfoAndSave(
        versionName: String,
        fileNameForCache: String,
        isLatest: Boolean,
        language: String,
    ): Single<VersionInfo> {
        return fetchVersionInfoFromRemote(versionName, isLatest, language)
            .delaySubscription(networkInitSubject.filter {
                it
            }.firstOrError())
            .observeOn(Schedulers.io())
            .doOnSuccess {
                saveVersionInfoToLocal(fileNameForCache, it)
                currentVersionInfo = it
            }
            .onErrorResumeNext { Single.error(FetchVersionInfoError()) }
    }

    @WorkerThread
    @Synchronized
    private fun loadVersionInfoFromLocal(fileName: String): VersionInfo {
        // 获取文件
        val versionInfoFile = ContextUtil.getContext().getFileStreamPath(fileName)

        // 判断本地是否有该文件，加载本地文件
        if (!versionInfoFile.exists()) {
            // 不存在，返回空列表
            throw VersionInfoCacheFileNotFound()
        }

        // 存在，则加载本地文件
        return JsonUtil.toBean(versionInfoFile, VersionInfo::class.java) ?: throw VersionInfoCacheFileNotFound()
    }

    @WorkerThread
    @Synchronized
    private fun saveVersionInfoToLocal(fileName: String, versionInfoList: VersionInfo) {
        // 获取文件
        val versionInfoFile = ContextUtil.getContext().getFileStreamPath(fileName)

        // 重新创建文件
        FileUtils.delFile(versionInfoFile, false)
        FileUtils.createFile(versionInfoFile)

        // 将版本信息写入文件
        versionInfoFile.writer().use {
            it.write(JsonUtil.toJson(versionInfoList) ?: "")
        }
    }

    /**
     * 从云端拉取版本信息
     * @param versionName 当前版本信息
     * @param isLatest true：拉取当前大版本（major version）的最新版本信息；false：拉取versionName所指定版本信息（可能拉取到空数据）
     */
    private fun fetchVersionInfoFromRemote(versionName: String, isLatest: Boolean, l: String): Single<VersionInfo> {
        return Single.create {
            DJINetworkManager.getInstance().enqueue(buildHttpRequestExTest(versionName, isLatest, l), object : DJIHttpCallback<DJIHttpResponse> {
                override fun onFailure(error: IDJIError) {
                    LogUtils.i(LOG_TAG, "error, ${error.errorCode()} ${error.errorType()} ${error.description()}")
                    it.onError(FetchVersionInfoError())
                }

                override fun onResponse(response: DJIHttpResponse) {
                    LogUtils.i(LOG_TAG, "success, ${response.code()}")

                    if (!response.isSuccessful) {
                        it.onError(FetchVersionInfoError())
                        return
                    }
                    val httpResult = HttpResult.fromJson(response.body())
                    if (httpResult == null) {
                        it.onError(FetchVersionInfoError())
                        return
                    }
                    LogUtils.i(LOG_TAG, "success, $httpResult")
                    it.onSuccess(httpResult.data)
                }

                override fun onLoading(current: Long, total: Long) {
//                    super.onLoading(current, total)
                }
            })
        }
    }

    /**
     * 根据入参构建不同的请求体
     */
    @WorkerThread
    private fun buildHttpRequest(host: String, versionName: String, isLatest: Boolean, language: String): DJIHttpRequest {
        val versionSplits = versionName.split(".").toMutableList()

        // 检查VersionName是否是三段式版本号，不是则补齐
        val diff = 3 - versionSplits.size
        for (i in 0 until diff) {
            versionSplits.add("0")
        }

        val httpRequest = DJIHttpRequest.Builder.newBuilder()
            .requestType(DJIHttpRequest.RequestType.GET)
            .url(
                if (isLatest) {
                    "$host$URL_PATH_TCH_LATEPATHST_VERSION_INFO"
                } else {
                    "$host$URL_PATH_FETCH_CURRENT_VERSION_INFO"
                }
            )
            .params(
                if (isLatest) {
                    mapOf(
                        "sdk_name" to "mobile sdk",
                        "platform" to "android",
                        "major_version" to versionSplits[0],
                    )
                } else {
                    mapOf(
                        "sdk_name" to "mobile sdk",
                        "platform" to "android",
                        "major_version" to versionSplits[0],
                        "minor_version" to versionSplits[1],
                        "patch_version" to versionSplits[2],
                    )
                }
            )
            .headers(mapOf("Language" to language, "content-type" to "application/json"))
            .build()
        LogUtils.i(LOG_TAG, "request, $httpRequest")
        return httpRequest
    }

    private fun getHostConfig(): HostConfig? {
        val hostConfigFile = getHostConfigFile() ?: return null
        JsonUtil.toBean(hostConfigFile, HostConfig::class.java)?.let {
            // 做一个简单的URL校验
            if (it.host.endsWith('/') && it.host.startsWith("https://")) {
                return it
            }
        }
        return null
    }

    private fun getHostConfigFile(): File? {
        val dir = ContextUtil.getContext().getExternalFilesDir("")
        val configFile = File(dir, FILE_NAME_HOST_VERSION_CONFIG)
        if (configFile.exists()) {
            return configFile
        }
        return null
    }

    private fun buildHttpRequestExTest(versionName: String, isLatest: Boolean, language: String): DJIHttpRequest {
        hostConfig?.let {
            return buildHttpRequest(it.host, it.versionName.ifEmpty { versionName }, isLatest, language)
        }
        return buildHttpRequest(URL_HOST, versionName, isLatest, language)
    }

    /**
     * 目前MSDK只有中英文，经测试发现
     *  - string.xml 只有在系统设置为简体中文（国家无所谓）时才会选择中文的string.xml
     * 所以该处添加两项判断
     */
    private fun getLanguageHeaderValue(): String {
        //"ZH" means Chinese
        val currentLocale = Locale.getDefault()
        val country: String = currentLocale.country.lowercase(Locale.ROOT)
        LogUtils.i(LOG_TAG,currentLocale.language,currentLocale.script)
        return if ("zh".equals(currentLocale.language, true) && "cn".equals(country)) {
            "cn"
        } else {
            "en"
        }
    }

    fun getCurrentVersionExTest(): String {
        return hostConfig?.versionName ?: ""
    }

    class VersionInfoCacheFileNotFound : Exception()
    class FetchVersionInfoError : Exception()

    companion object {
        /**
         * 服务器host
         */
        private const val URL_HOST = "https://dev.dji.com/"

        /**
         * 获取最新版本信息
         */
        private const val URL_PATH_TCH_LATEPATHST_VERSION_INFO = "api/v1/release/latest"

        /**
         * 获取指定版本信息
         */
        private const val URL_PATH_FETCH_CURRENT_VERSION_INFO = "api/v1/release"

        /**
         * 用于存储最新版本信息的文件
         */
        private const val FILE_NAME_LATEST_VERSION_INFO_CACHE = "version_info_latest"

        /**
         * 提供给测试使用的配置文件路径
         */
        @SuppressLint("SdCardPath")
        private const val FILE_NAME_HOST_VERSION_CONFIG = "config_host_version.json"
        private const val LOG_TAG = "VersionInfoRepository"
    }
}

private data class HostConfig(
    @SerializedName("host")
    val host: String = "",
    @SerializedName("version")
    val versionName: String = "",
)

data class HttpResult(val code: Int, val message: String, val data: VersionInfo) {

    companion object {
        fun fromJson(json: String): HttpResult? {
            return JsonUtil.toBean(json, HttpResult::class.java)
        }
    }

    override fun toString(): String {
        return "HttpResult(code=$code, message='$message', data='$data')"
    }
}

data class VersionInfo(
    val versionCode: Int = 0,
    @SerializedName("MajorVersion")
    val versionMajor: String = "",
    @SerializedName("MinorVersion")
    val versionMinor: String = "",
    @SerializedName("PatchVersion")
    val versionPatch: String = "",

    /**
     * UTC时间戳（单位s）
     */
    @SerializedName("ReleaseDate")
    val releaseTimeStamp: Long = 0,
    @SerializedName("SupportedProducts")
    val supportProducts: String = "",
    @SerializedName("Highlights")
    val releaseNode: String = "",
) {
    val versionName: String
        get() {
            return "$versionMajor.$versionMinor.$versionPatch"
        }
}