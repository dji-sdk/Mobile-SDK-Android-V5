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
import dji.sample.patrol.data.DestinationPoint
import dji.sample.patrol.data.ResultCallback
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError

/**
 * AR 投射 SDK 集成边界（design D3/D9）：AR 点 Config/Info 上传与查询均可 JVM fake。
 */
interface DestinationArPort {
    /** 上传 AR 点配置，成功后回调 `configId`。 */
    fun uploadPointConfig(callback: ResultCallback<String>)

    /** 用 `configId` 上传 AR 点信息（坐标真实经纬高 HAE）。 */
    fun uploadPointInfo(
        configId: String,
        point: DestinationPoint,
        text: String,
        callback: ResultCallback<Unit>,
    )

    /** 查询既有 AR 点配置（断连/重启后按属性匹配复用 `configId`）。 */
    fun queryPointConfig(callback: ResultCallback<List<ArPointConfigInfo>>)
}

/** AR 点配置的工程内投影（`name` 用于按属性匹配复用）。 */
data class ArPointConfigInfo(val id: String, val name: String, val projectionType: String?)

/** 生产实现：`ARManager.getInstance()`（`dji.open.pilot.client.manager.ui.ARManager`）；`dji.*` import 仅落本文件。 */
object PilotDestinationArPort : DestinationArPort {

    private const val TAG = "PatrolDestAr"
    private const val CONFIG_NAME = "patrol-destination"

    /** 预上传后的 SVG 图标文件名（空表示仅文字）。 */
    @Volatile
    var iconFileName: String? = null

    private val manager get() = ARManager.getInstance()

    override fun uploadPointConfig(callback: ResultCallback<String>) {
        val useIcon = !iconFileName.isNullOrBlank()
        val textConfig = ARTextConfig.Builder.builder()
            .withTextSize(40f)
            .withFontGravity(ARFontGravity.TOP)
            .build()
        val builder = ARPointConfig.Builder.builder()
            .withName(CONFIG_NAME)
            .withUseText(true)
            .withTextConfig(textConfig)
        if (useIcon) {
            builder
                .withArProjectionType(ARIconProjectionType.ORTHOGRAPHIC)
                .withIconOrthographicConfig(
                    ARIconOrthographicConfig.Builder.builder()
                        .withSize(80f)
                        .withIconFileName(iconFileName)
                        .build(),
                )
        } else {
            builder.withArProjectionType(ARIconProjectionType.NONE)
        }
        manager.uploadARPointConfig(builder.build(), object : CommonCallbacks.CompletionCallbackWithParam<String> {
            override fun onSuccess(data: String?) {
                Log.i(TAG, "uploadARPointConfig onSuccess configId=$data")
                callback.onSuccess(data ?: "")
            }

            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "uploadARPointConfig onFailure ${error.errorCode()} ${error.description()}")
                callback.onFailure(IllegalStateException("${error.errorCode()}: ${error.description()}"))
            }
        })
    }

    override fun uploadPointInfo(
        configId: String,
        point: DestinationPoint,
        text: String,
        callback: ResultCallback<Unit>,
    ) {
        val info = ARPointInfo.Builder.builder()
            .withConfigId(configId)
            .withVisible(true)
            .withIconVisible(true)
            .withTextVisible(true)
            .withShowEdgeGuide(true)
            .withText(text)
            .withLocation(OpenLocation(point.latitude, point.longitude, point.altitude))
            .build()
        manager.uploadARPointInfo(info, object : CommonCallbacks.CompletionCallbackWithParam<String> {
            override fun onSuccess(data: String?) {
                Log.i(TAG, "uploadARPointInfo onSuccess id=$data")
                callback.onSuccess(Unit)
            }

            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "uploadARPointInfo onFailure ${error.errorCode()} ${error.description()}")
                callback.onFailure(IllegalStateException("${error.errorCode()}: ${error.description()}"))
            }
        })
    }

    override fun queryPointConfig(callback: ResultCallback<List<ArPointConfigInfo>>) {
        manager.queryARPointConfig(object : CommonCallbacks.CompletionCallbackWithParam<List<ARPointConfig>> {
            override fun onSuccess(data: List<ARPointConfig>?) {
                val configs = data.orEmpty().map {
                    ArPointConfigInfo(it.id, it.name, it.arProjectionType?.name)
                }
                Log.i(TAG, "queryARPointConfig onSuccess size=${configs.size}")
                callback.onSuccess(configs)
            }

            override fun onFailure(error: IDJIError) {
                Log.e(TAG, "queryARPointConfig onFailure ${error.errorCode()} ${error.description()}")
                callback.onFailure(IllegalStateException("${error.errorCode()}: ${error.description()}"))
            }
        })
    }
}
