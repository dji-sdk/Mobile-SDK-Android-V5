package dji.sampleV5.aircraft.telemetry

import dji.sdk.keyvalue.value.common.Attitude

class TuskService {
    // Initialize retrofit to connect to JS server
    // Use hardcoded static IP as host, we'll create a LAN network using hotspot
    // Assume the endpoint we want to have

}

data class TuskTelemetry(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val battery: Int,
    val gps: Int,
    val roll: Double,
    val pitch: Double,
    val yaw: Double
)