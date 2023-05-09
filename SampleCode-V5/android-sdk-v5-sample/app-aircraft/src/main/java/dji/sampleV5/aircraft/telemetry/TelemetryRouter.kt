package dji.sampleV5.aircraft.telemetry

import android.util.Log
import dji.sdk.keyvalue.key.*
import dji.sdk.keyvalue.value.common.*


//                override fun onFailure(error: DJIError) {
//                     implementation of onFailure method
//                }
//        val GPSLevel = KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyGPSSignalLevel), this, new CommonCallbacks.KeyListener<Boolean>(){});


//    val ADS_enable = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyAdsbEnable))
//    val minTemp = ThermalAreaMetersureTemperature().getMinAreaTemperature()
//    val maxTemp = ThermalAreaMetersureTemperature().getMaxAreaTemperature()
//    val uasLoc = FlightControllerKey.KeyAircraftLocation3D
//    val flightStatus = FlightControllerKey.KeyIsFlying
//    val takeoff = FlightControllerKey.KeyStartTakeoff
//    val rthLocation = FlightControllerKey.KeyHomeLocation
//    val startGoHome = FlightControllerKey.KeyStartGoHome


import dji.v5.manager.KeyManager
import io.reactivex.rxjava3.functions.Consumer
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TelemetryRouter {
    val keyManager = KeyManager.getInstance()
    private val telemService = TuskService()
    val pachKeyManager = PachKeyManager()
    var stateData = TuskAircraftState( 0.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0.0, 0, windDirection = null, false)
    var statusData = TuskAircraftStatus( connected = false, battery = 0, gps = 0, signalQuality =  0,
        goHomeState = null, flightMode = null, motorsOn = false, homeLocationLat = null, homeLocationLong = null, gimbalAngle = 0.0)
    var controlStatus = TuskControllerStatus( battery = 0, pauseButton = false, homeButton = false,
        0,0,0,0)

    init {
        Log.d("TelemetryRouter", "TelemetryRouter Initializing")
        registerKeys()

    }

    private fun sendState(telemetry: TuskAircraftState) = runBlocking {
        launch {
            telemService.postState(telemetry)
        }
    }

    private fun sendStatus(status: TuskAircraftStatus) = runBlocking {
        launch {
            telemService.postStatus(status)
        }
    }

    fun getActions() = runBlocking {
        launch {
            telemService.getActions()
        }
    }

    // Holder function that registers all necessary keys and handles their operation during changes
    private fun registerKeys(){
        // Setup PachKeyManager and define the keys that we want to listen to
        pachKeyManager.registerKey(
            KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D),
            Consumer {
                stateData = stateData.copy(latitude = it.latitude, longitude = it.longitude, altitude = it.altitude)
                sendState(stateData)
                Log.d("PachKeyManager", "KeyAircraftLocation $it") }
        )

        pachKeyManager.registerKey(
            KeyTools.createKey(FlightControllerKey.KeyAircraftAttitude),
            Consumer {
                stateData = stateData.copy(roll = it.roll, pitch = it.pitch, yaw = it.yaw)
                sendState(stateData)
                Log.d("PachKeyManager", "KeyAircraftAttitude $it") }
        )

        pachKeyManager.registerKey(
            KeyTools.createKey(FlightControllerKey.KeyAircraftVelocity),
            Consumer {
                stateData = stateData.copy(velocityX = it.x, velocityY = it.y, velocityZ = it.z)
                sendState(stateData)
                Log.d("PachKeyManager", "AircraftVelocity $it") }
        )

        pachKeyManager.registerKey(
            KeyTools.createKey(FlightControllerKey.KeyWindSpeed),
            Consumer {
                stateData = stateData.copy(windSpeed = it)
                sendState(stateData)
                Log.d("PachKeyManager", "WindSpeed $it") }
        )

        pachKeyManager.registerKey(
            KeyTools.createKey(FlightControllerKey.KeyWindDirection),
            Consumer {
                stateData = stateData.copy(windDirection = it.toString())
                sendState(stateData)
                Log.d("PachKeyManager", "WindDirection $it") }
        )

        pachKeyManager.registerKey(
            KeyTools.createKey(FlightControllerKey.KeyIsFlying),
            Consumer {
                stateData = stateData.copy(isFlying = it)
                sendState(stateData)
                Log.d("PachKeyManager", "IsFlying $it") }
        )

        pachKeyManager.registerKey(
            KeyTools.createKey(FlightControllerKey.KeyGPSSignalLevel),
            Consumer {
                statusData = statusData.copy(gps = it.value())
                sendStatus(statusData)
                Log.d("PachKeyManager", "GPSSignalLevel $it") }
        )

        pachKeyManager.registerKey(
            KeyTools.createKey(BatteryKey.KeyChargeRemainingInPercent),
            Consumer {
                statusData = statusData.copy(battery = it)
                sendStatus(statusData)
                Log.d("PachKeyManager", "Battery Level $it") }
        )

        // Continue to do this for the other required keys...
    }

}
// PachKeyManager
// We'll setup all the listeners and convert into Rx.Flowable

// TelemetryService
// Setup Retrofit to push telemetry data to our server

// TelemetryRouter
// Create instance of TelemetryService and DJIKeyManagerListener.
// Subscribe to Flowable from DJIKeyManagerListener and push to TelemetryService.