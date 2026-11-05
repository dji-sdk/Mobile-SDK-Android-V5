package dji.sample.patrol.manager

import android.util.Log
import dji.sample.patrol.data.DestinationFailureCategory
import dji.sample.patrol.data.DestinationPoint
import dji.sample.patrol.data.ResultCallback

/**
 * 可选 AR 投射管理器（design D3）。
 *
 * 设计意图：默认关闭；开启时「先 Config 后 Info」把目的地点位投射到图传画面，失败降级为仅地图
 * （**不阻塞** FlyTo/航线）；`configId` 断连/重启后按属性匹配复用，避免 `AR_POINT_CONFIG_EXIST`。
 * 通过 [arPort] 与 SDK 解耦，可 JVM fake 单测。
 */
object DestinationArManager {

    private const val TAG = "PatrolDestArMgr"
    private const val CONFIG_NAME = "patrol-destination"

    /** 可注入的 AR 端口（生产为 [PilotDestinationArPort]）。 */
    @Volatile
    var arPort: DestinationArPort? = null

    /** AR 开关（默认关闭）。 */
    @Volatile
    var enabled: Boolean = false

    /** AR 失败回调（`ar-error`，非阻塞）。 */
    @Volatile
    var onError: ((DestinationFailureCategory, String) -> Unit)? = null

    @Volatile
    var configId: String? = null
        private set

    /** 开启/关闭 AR 投射。 */
    fun applyEnabled(value: Boolean) {
        enabled = value
        Log.i(TAG, "AR enabled=$value")
    }

    /** 断连/离开清理：清空缓存 `configId`（不删除 Pilot 端既有配置）。 */
    fun reset() {
        configId = null
    }

    /** 投射目的地点位（默认关闭时直接返回）。 */
    fun project(point: DestinationPoint, text: String) {
        if (!enabled) return
        val port = arPort ?: return
        val existing = configId
        if (existing != null) {
            uploadInfo(port, existing, point, text)
            return
        }
        // 先按属性匹配复用既有配置，匹配不到才重新上传（避免 AR_POINT_CONFIG_EXIST）。
        port.queryPointConfig(object : ResultCallback<List<ArPointConfigInfo>> {
            override fun onSuccess(data: List<ArPointConfigInfo>) {
                val match = data.firstOrNull { it.name == CONFIG_NAME }
                if (match != null) {
                    configId = match.id
                    Log.i(TAG, "reuse AR configId=${match.id}")
                    uploadInfo(port, match.id, point, text)
                } else {
                    uploadConfig(port, point, text)
                }
            }

            override fun onFailure(error: Throwable) {
                // 查询失败不阻断：直接尝试上传配置（同名覆盖/存在由 SDK 语义处理）。
                uploadConfig(port, point, text)
            }
        })
    }

    private fun uploadConfig(port: DestinationArPort, point: DestinationPoint, text: String) {
        port.uploadPointConfig(object : ResultCallback<String> {
            override fun onSuccess(data: String) {
                if (data.isBlank()) {
                    report("empty configId from uploadARPointConfig")
                    return
                }
                configId = data
                uploadInfo(port, data, point, text)
            }

            override fun onFailure(error: Throwable) {
                report(error.message ?: "uploadARPointConfig failed")
            }
        })
    }

    private fun uploadInfo(port: DestinationArPort, configId: String, point: DestinationPoint, text: String) {
        port.uploadPointInfo(configId, point, text, object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) {
                Log.i(TAG, "AR point projected configId=$configId")
            }

            override fun onFailure(error: Throwable) {
                report(error.message ?: "uploadARPointInfo failed")
            }
        })
    }

    private fun report(message: String) {
        Log.w(TAG, "ar-error: $message")
        onError?.invoke(DestinationFailureCategory.AR_ERROR, message)
    }

    /** 测试辅助：重置。 */
    fun resetForTest() {
        arPort = null
        enabled = false
        onError = null
        configId = null
    }
}
