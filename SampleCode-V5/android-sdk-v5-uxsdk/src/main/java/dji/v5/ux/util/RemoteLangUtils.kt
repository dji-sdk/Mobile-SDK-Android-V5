package dji.v5.ux.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import dji.open.pilot.base.manager.RemoteLanguage
import dji.open.pilot.client.manager.RemoteServerConfigManager
import dji.v5.utils.common.LogUtils
import dji.v5.ux.util.RemoteLangUtils.getString
import dji.v5.ux.util.RemoteLangUtils.init
import java.util.Locale

/**
 * Remote 多语言工具类（合并自 PilotLanguageManager）。
 *
 * 职责：
 * 1. 持有 Application Context 和当前语言对应的 Resources
 * 2. 通过 [init] 完成初始化并自动注册到 [RemoteServerConfigManager]，
 *    服务端语言变更时自动更新本地语言资源，无需外部手动调用
 * 3. 提供 [getString] 获取当前语言对应的文案；语言未知时回落到系统默认资源（values/strings.xml）
 *
 * 文案资源完全由插件 APP 自身的 strings.xml 提供，与 open-pilot 仓库无关。
 */
object RemoteLangUtils {

    private const val TAG = "RemoteLangUtils"

    private var appContext: Context? = null
    private var currentLanguage: RemoteLanguage? = null
    private var langResources: Resources? = null
    private var languageListenerId = -1

    /**
     * 初始化工具类，保存 Application Context 并注册语言变更订阅。
     * 必须在插件 APP 启动时调用一次（Application.onCreate 或 ClientService 初始化时）。
     * 重复调用安全，会先取消旧订阅再重新注册。
     *
     * @param context 应用级 Context（建议传 getApplicationContext()）
     */
    @JvmStatic
    fun init(context: Context) {
        appContext = context.applicationContext
        if (languageListenerId != -1) {
            RemoteServerConfigManager.getInstance().removeLanguageObserver(languageListenerId)
        }
        onLanguageChanged(RemoteServerConfigManager.getInstance().currentLanguage)
        languageListenerId = RemoteServerConfigManager.getInstance()
            .addLanguageObserver { language ->
                onLanguageChanged(
                    language
                )
            }
        LogUtils.i(TAG, "RemoteLangUtils initialized")
    }

    /**
     * 获取当前持有的语言枚举。
     */
    @JvmStatic
    fun getCurrentLanguage(): RemoteLanguage? = currentLanguage

    /**
     * 根据资源 ID 获取当前服务端语言对应的文案。
     * 若语言未知（langResources 为 null），回落到系统默认资源（values/strings.xml），
     * 符合 Android 标准资源匹配行为。
     *
     * @param resId 客户端 R.string.xxx 资源 ID
     * @return 对应语言的文案字符串；资源 ID 不存在时返回空字符串
     */
    @JvmStatic
    fun getString(resId: Int): String {
        val res = langResources ?: appContext?.resources ?: return ""
        return try {
            val result = res.getString(resId)
            LogUtils.d(TAG, "getString: $resId success:  $result")
            result
        } catch (e: Resources.NotFoundException) {
            LogUtils.e(TAG, "getString: resId not found: $resId, ${e.message}")
            ""
        }
    }

    /**
     * 收到 RemoteServerConfigManager 的语言变更回调，更新本地语言资源。
     */
    private fun onLanguageChanged(language: RemoteLanguage?) {
        LogUtils.i(TAG, "onLanguageChanged: ${language?.tag ?: "null(fallback to default resources)"}")
        if (language!=null && currentLanguage != language) {
            currentLanguage = language
            langResources = buildLangResources(language.locale)
        }
    }

    /**
     * 根据 Locale 构建对应语言的 Resources 对象。
     * 使用 createConfigurationContext 隔离，不影响全局 Locale。
     */
    private fun buildLangResources(locale: Locale): Resources? {
        val context = appContext ?: run {
            LogUtils.e(TAG, "buildLangResources: appContext is null, call init() first")
            return null
        }
        return try {
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            context.createConfigurationContext(config).resources
        } catch (e: Exception) {
            LogUtils.e(TAG, "buildLangResources failed for locale=$locale: ${e.message}")
            context.resources // 降级为系统默认资源
        }
    }
}
