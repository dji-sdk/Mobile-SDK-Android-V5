package com.dji.sample.gasdetection.gas.task

import com.dji.sample.gasdetection.gas.autoflight.AutoFlightSafetySnapshot
import com.dji.sample.gasdetection.gas.autoflight.FlightAuthority
import com.dji.sample.gasdetection.gas.payload.IngestState
import com.dji.sample.gasdetection.gas.remote.AutoFlightCapability

/**
 * 执行前预检编排。
 *
 * 设计意图：每项检查只回答「通过/不通过 + 原因」，不在这里做文案或阶段迁移；自动作业的
 * 飞行前置条件由 [AutoFlightSafetySnapshot] 提供，缺失或未知一律按不通过处理（控制权未知除外）。
 * 未收到能力回调时按未知处理，避免把「还没连上飞机」当成「支持自动作业」。
 */
class PreflightEvaluator(
    private val ingestState: () -> IngestState,
    private val channelConfigReady: () -> Boolean,
    private val capability: () -> AutoFlightCapability,
    private val aircraftConnected: () -> Boolean,
    private val safety: () -> AutoFlightSafetySnapshot = { AutoFlightSafetySnapshot() },
) {

    fun evaluate(): PreflightResult {
        val payloadOnline = ingestState().onlinePayloads > 0
        val connected = aircraftConnected() || safety().aircraftConnected
        // 飞机未连接时 SDK 可能返回空能力列表，此时结论应为「未知」而不是「不支持」。
        val capabilityValue = if (connected) capability() else AutoFlightCapability.Unknown
        val snapshot = safety()
        val authorityOccupied = snapshot.authority in OCCUPYING_AUTHORITIES
        val batteryOk = !snapshot.lowBattery && !snapshot.criticalLowBattery
        val targetReady = snapshot.target?.isComplete == true

        val items = listOf(
            PreflightItem(
                id = PreflightId.PAYLOAD_ONLINE,
                passed = payloadOnline,
                failure = if (payloadOnline) null else PreflightFailure.PAYLOAD_OFFLINE,
            ),
            PreflightItem(
                id = PreflightId.CHANNEL_CONFIG,
                passed = channelConfigReady(),
                failure = if (channelConfigReady()) null else PreflightFailure.CONFIG_INVALID,
            ),
            when (capabilityValue) {
                AutoFlightCapability.Supported -> PreflightItem(
                    id = PreflightId.AUTO_FLIGHT_CAPABILITY,
                    passed = true,
                )

                AutoFlightCapability.Unsupported -> PreflightItem(
                    id = PreflightId.AUTO_FLIGHT_CAPABILITY,
                    passed = false,
                    failure = PreflightFailure.AUTO_FLIGHT_UNSUPPORTED,
                )

                AutoFlightCapability.Unknown -> PreflightItem(
                    id = PreflightId.AUTO_FLIGHT_CAPABILITY,
                    passed = false,
                    failure = PreflightFailure.AUTO_FLIGHT_UNKNOWN,
                )
            },
            PreflightItem(
                id = PreflightId.AIRCRAFT_CONNECTED,
                passed = connected,
                failure = if (connected) null else PreflightFailure.AIRCRAFT_DISCONNECTED,
            ),
            PreflightItem(
                id = PreflightId.AIRCRAFT_FLYING,
                passed = snapshot.flying,
                failure = if (snapshot.flying) null else PreflightFailure.AIRCRAFT_NOT_FLYING,
            ),
            PreflightItem(
                id = PreflightId.GPS_READY,
                passed = snapshot.gpsReady,
                failure = if (snapshot.gpsReady) null else PreflightFailure.GPS_WEAK,
            ),
            PreflightItem(
                id = PreflightId.BATTERY_OK,
                passed = batteryOk,
                failure = if (batteryOk) null else PreflightFailure.BATTERY_LOW,
            ),
            PreflightItem(
                id = PreflightId.MISSION_IDLE,
                passed = snapshot.missionIdle,
                failure = if (snapshot.missionIdle) null else PreflightFailure.MISSION_RUNNING,
            ),
            PreflightItem(
                id = PreflightId.CONTROL_AUTHORITY,
                passed = !authorityOccupied,
                failure = if (authorityOccupied) PreflightFailure.AUTHORITY_OCCUPIED else null,
            ),
            PreflightItem(
                id = PreflightId.TARGET_READY,
                passed = targetReady,
                failure = if (targetReady) null else PreflightFailure.TARGET_MISSING,
            ),
        )
        return PreflightResult(items)
    }

    private companion object {
        /** 这些控制权归属表示他方正在控制飞机，插件不得启动自动作业。 */
        val OCCUPYING_AUTHORITIES = setOf(
            FlightAuthority.OSDK,
            FlightAuthority.AIRPORT,
            FlightAuthority.AUTO_TEST,
        )
    }
}
