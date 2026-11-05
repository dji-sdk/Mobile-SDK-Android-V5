package com.dji.sample.gasdetection.ui

/**
 * 插件入口页的就绪状态。
 *
 * 设计意图：把「SDK 注册 / Pilot 连接 / 飞机连接」三个独立输入收敛成一个用户能直接理解的结论，
 * 避免入口页把三个状态分散展示后让用户自行判断。纯 Kotlin 类型，不依赖 Android 或 SDK，
 * 便于在 UI 之外的层复用与验证。
 *
 * 不变量：优先级固定为 SDK 注册 → Pilot 连接 → 飞机连接，任一前置未就绪时不显示「就绪」。
 */
enum class EntryStatus {
    /** SDK 尚未注册，后续能力都不可用。 */
    SDK_UNREGISTERED,

    /** SDK 已注册，但 Pilot 宿主未连接。 */
    PILOT_DISCONNECTED,

    /** Pilot 已连接，但没有飞机连接。 */
    AIRCRAFT_DISCONNECTED,

    /** 三者均就绪，可以开始业务操作。 */
    ALL_READY;

    /** 是否处于可开始业务操作的就绪态。 */
    val isReady: Boolean
        get() = this == ALL_READY

    companion object {
        /**
         * 按固定优先级推导入口页状态。
         *
         * 输入均为 `false`（包括状态流尚未产生首个值）时按未就绪处理，不提前显示就绪。
         */
        fun resolve(
            sdkRegistered: Boolean,
            pilotConnected: Boolean,
            aircraftConnected: Boolean,
        ): EntryStatus = when {
            !sdkRegistered -> SDK_UNREGISTERED
            !pilotConnected -> PILOT_DISCONNECTED
            !aircraftConnected -> AIRCRAFT_DISCONNECTED
            else -> ALL_READY
        }
    }
}

/**
 * 入口页单次渲染所需的不可变状态。
 *
 * 设计意图：一次 `combine` 生成完整快照后统一渲染，避免多个 collector 分别更新徽章与文案时
 * 出现「徽章已变、结论未变」的中间帧。
 *
 * @property sdkRegistered SDK 是否已注册，决定 SDK 徽章文案与样式。
 * @property pilotConnected Pilot 是否已连接，决定 Pilot 徽章文案与样式。
 * @property status 由三个输入推导出的统一状态，决定内容区文案与颜色。
 */
data class EntryUiState(
    val sdkRegistered: Boolean,
    val pilotConnected: Boolean,
    val status: EntryStatus,
) {
    companion object {
        /** 由三个原始输入生成入口页快照。 */
        fun of(
            sdkRegistered: Boolean,
            pilotConnected: Boolean,
            aircraftConnected: Boolean,
        ): EntryUiState = EntryUiState(
            sdkRegistered = sdkRegistered,
            pilotConnected = pilotConnected,
            status = EntryStatus.resolve(sdkRegistered, pilotConnected, aircraftConnected),
        )
    }
}
