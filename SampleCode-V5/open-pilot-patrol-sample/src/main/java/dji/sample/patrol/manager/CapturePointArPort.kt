package dji.sample.patrol.manager

import android.util.Log
import dji.open.pilot.base.manager.OpenLocation
import dji.open.pilot.base.manager.ar.ARFontGravity
import dji.open.pilot.base.manager.ar.ARIconOrthographicConfig
import dji.open.pilot.base.manager.ar.ARIconProjectionType
import dji.open.pilot.base.manager.ar.ARPointConfig
import dji.open.pilot.base.manager.ar.ARPointInfo
import dji.open.pilot.base.manager.ar.ARTextConfig
import dji.open.pilot.client.manager.ui.ARManager
import dji.sample.patrol.data.GeoPoint3D
import dji.sample.patrol.data.ResultCallback
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError

/**
 * 拍照点 AR 投射 SDK 边界（design D2/D3/D6）。
 *
 * 所有方法只使用工程自有类型（[GeoPoint3D]、[ResultCallback]），使编排可通过 JVM fake 单测；
 * `dji.*` 类型只在 [PilotCapturePointArPort] 内出现。
 */
interface CapturePointArPort {

    /** 把本地 SVG 推到 Pilot，成功后回调远端文件名。 */
    fun pushIconFile(localPath: String, callback: ResultCallback<String>)

    /** 查询既有 AR 点配置（断连/重启后按 `name` 复用）。 */
    fun queryPointConfigs(callback: ResultCallback<List<ArPointConfigInfo>>)

    /**
     * 上传 AR 点配置，成功后回调 `configId`。
     *
     * @param configName 配置名（同时也是 query 复用的匹配键）
     * @param iconFileName 已推送到 Pilot 的 SVG 文件名
     */
    fun uploadPointConfig(
        configName: String,
        iconFileName: String,
        textSize: Float,
        iconSize: Float,
        textBackgroundColor: Int,
        callback: ResultCallback<String>,
    )

    /** 用 `configId` 上传一个点位，成功后回调点位 id（用于后续删除）。 */
    fun uploadPointInfo(
        configId: String,
        point: GeoPoint3D,
        text: String,
        textColor: Int,
        callback: ResultCallback<String>,
    )

    /** 按点位 id 删除已上传的 AR 点位（清理本次任务标识）。 */
    fun deletePointInfo(infoId: String, callback: ResultCallback<Unit>)
}

/**
 * 生产实现：`ARManager.getInstance()`（`dji.open.pilot.client.manager.ui.ARManager`）。
 *
 * 上传成功时缓存 `ARPointInfo` 对象：删除接口按 `id` 定位服务端对象，保留原始对象可避免重新
 * Builder 出「服务端从未见过的新 id」（`AR_POINT_INFO_NOT_FOUND`）。
 */
object PilotCapturePointArPort : CapturePointArPort {

    private const val TAG = "PatrolCapAr"

    private val manager get() = ARManager.getInstance()
    private val uploadedInfos = HashMap<String, ARPointInfo>()

    override fun pushIconFile(localPath: String, callback: ResultCallback<String>) =
        RemoteFilePusher.push(localPath, callback)

    override fun queryPointConfigs(callback: ResultCallback<List<ArPointConfigInfo>>) {
        manager.queryARPointConfig(object : CommonCallbacks.CompletionCallbackWithParam<List<ARPointConfig>> {
            override fun onSuccess(data: List<ARPointConfig>?) {
                val configs = data.orEmpty().map { ArPointConfigInfo(it.id, it.name, it.arProjectionType?.name) }
                Log.i(TAG, "queryARPointConfig onSuccess size=${configs.size}")
                callback.onSuccess(configs)
            }

            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "queryARPointConfig onFailure ${error.errorCode()} ${error.description()}")
                callback.onFailure(IllegalStateException("${error.errorCode()}: ${error.description()}"))
            }
        })
    }

    override fun uploadPointConfig(
        configName: String,
        iconFileName: String,
        textSize: Float,
        iconSize: Float,
        textBackgroundColor: Int,
        callback: ResultCallback<String>,
    ) {
        val textConfig = ARTextConfig.Builder.builder()
            .withTextSize(textSize)
            .withFontGravity(ARFontGravity.TOP)
            .withShadow(true)
            .withBackgroundColor(textBackgroundColor)
            .build()
        val config = ARPointConfig.Builder.builder()
            .withName(configName)
            .withUseText(true)
            .withTextConfig(textConfig)
            .withArProjectionType(ARIconProjectionType.ORTHOGRAPHIC)
            .withIconOrthographicConfig(
                ARIconOrthographicConfig.Builder.builder()
                    .withSize(iconSize)
                    .withIconFileName(iconFileName)
                    .build(),
            )
            .build()
        manager.uploadARPointConfig(config, object : CommonCallbacks.CompletionCallbackWithParam<String> {
            override fun onSuccess(data: String?) {
                Log.i(TAG, "uploadARPointConfig onSuccess name=$configName configId=$data")
                callback.onSuccess(data.orEmpty())
            }

            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "uploadARPointConfig onFailure name=$configName ${error.errorCode()} ${error.description()}")
                callback.onFailure(IllegalStateException("${error.errorCode()}: ${error.description()}"))
            }
        })
    }

    override fun uploadPointInfo(
        configId: String,
        point: GeoPoint3D,
        text: String,
        textColor: Int,
        callback: ResultCallback<String>,
    ) {
        val info = ARPointInfo.Builder.builder()
            .withConfigId(configId)
            .withVisible(true)
            .withIconVisible(true)
            .withTextVisible(true)
            .withShowEdgeGuide(true)
            .withText(text)
            .withTextColor(textColor)
            .withLocation(OpenLocation(point.latitude, point.longitude, point.altitude))
            .build()
        manager.uploadARPointInfo(info, object : CommonCallbacks.CompletionCallbackWithParam<String> {
            override fun onSuccess(data: String?) {
                val infoId = data?.takeIf { it.isNotBlank() } ?: info.id
                uploadedInfos[infoId] = info
                Log.i(TAG, "uploadARPointInfo onSuccess id=$infoId text=$text")
                callback.onSuccess(infoId)
            }

            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "uploadARPointInfo onFailure text=$text ${error.errorCode()} ${error.description()}")
                callback.onFailure(IllegalStateException("${error.errorCode()}: ${error.description()}"))
            }
        })
    }

    override fun deletePointInfo(infoId: String, callback: ResultCallback<Unit>) {
        val info = uploadedInfos.remove(infoId)
        if (info == null) {
            Log.w(TAG, "deleteARPointInfo skipped: unknown info id=$infoId")
            callback.onFailure(IllegalStateException("unknown AR point info id: $infoId"))
            return
        }
        manager.deleteARPointInfo(info, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.i(TAG, "deleteARPointInfo onSuccess id=$infoId")
                callback.onSuccess(Unit)
            }

            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "deleteARPointInfo onFailure id=$infoId ${error.errorCode()} ${error.description()}")
                callback.onFailure(IllegalStateException("${error.errorCode()}: ${error.description()}"))
            }
        })
    }

    /** 测试辅助：清空本地对象缓存。 */
    fun clearCacheForTest() = uploadedInfos.clear()
}
