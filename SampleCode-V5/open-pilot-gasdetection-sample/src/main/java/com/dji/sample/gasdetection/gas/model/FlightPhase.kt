package com.dji.sample.gasdetection.gas.model

/**
 * 采样时的作业阶段。
 *
 * 设计意图：测量记录需要能回溯「这条样本是在自动飞向、搜索、跟随还是手动飞行时产生的」，
 * 但阶段由插件按本地事实补全，不是载荷能力。取值严格采用规划方案 §4.2 定义的四个阶段，
 * 不额外扩展枚举，避免与后端契约分叉。
 */
enum class FlightPhase(val code: Int) {
    /** 自动飞向目标点。 */
    FLY_TO(1),

    /** 自动搜索目标。 */
    AI_SEARCH(2),

    /** 虚拟摇杆跟随。 */
    VS_FOLLOW(3),

    /** 手动飞行（无任务上下文时也归入该阶段）。 */
    MANUAL(4),
    ;

    companion object {
        /** 按帧内阶段码查找；未知码返回 `null`。 */
        fun fromCode(code: Int): FlightPhase? = entries.firstOrNull { it.code == code }
    }
}
