package dji.sample.flighttask.pilot

import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.key.RemoteControllerKey
import dji.v5.manager.KeyManager
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 远程遥控器 SN 监听，独立成类以隔离加固 SDK 类型。
 *
 * 对外发布版以加固 AAR 形式依赖 MSDK，加固类要等 `attachBaseContext` 里的 secneo Helper
 * 解壳后才可用。若 Application 类自身出现 SDK 类型（如 `DJIKey`），ART 可能在实例化阶段
 * 就解析它，早于解壳，直接抛 `NoClassDefFoundError`。因此这里把所有 SDK 类型收敛在本类内，
 * 宿主只需使用基本类型回调。
 */
internal class RemoteControllerSerialNumberMonitor {

    private val serialNumberKey by lazy {
        KeyTools.createKey(RemoteControllerKey.KeySerialNumber)
    }

    private val waiting = AtomicBoolean(false)

    /** 开始等待非空 RC SN；重复调用会重置上一次监听，回调只会触发一次。 */
    fun await(onAvailable: (String) -> Unit) {
        cancel()
        if (!waiting.compareAndSet(false, true)) return
        KeyManager.getInstance().listen(serialNumberKey, this, true) { _, value ->
            val serialNumber = value?.trim().orEmpty()
            if (serialNumber.isBlank()) return@listen
            if (!waiting.compareAndSet(true, false)) return@listen
            cancel()
            onAvailable(serialNumber)
        }
    }

    /** 取消等待；断开连接或重复 [await] 时调用，避免旧监听继续回调。 */
    fun cancel() {
        waiting.set(false)
        KeyManager.getInstance().cancelListen(serialNumberKey, this)
    }
}
