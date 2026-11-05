package com.dji.sample.gasdetection.gas.catalog

import android.content.res.AssetManager

/**
 * 从 assets 读取气体配置的加载器。
 *
 * 设计意图：把 Android 资源访问隔离在加载器里，解析与业务逻辑保持纯 JVM 可测；
 * 读取失败或解析失败都直接抛出，由调用方决定降级文案，不返回部分可用的配置。
 */
class GasConfigLoader(
    private val assets: AssetManager,
    private val assetName: String = DEFAULT_ASSET,
) {
    /** 读取并解析配置；文件不存在或格式非法时抛出异常。 */
    fun load(): GasConfig {
        val raw = assets.open(assetName).use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        }
        return GasConfigParser.parse(raw)
    }

    companion object {
        /** 随工程打包的默认配置文件。 */
        const val DEFAULT_ASSET = "gas-inspection-config.json"
    }
}
