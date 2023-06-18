package dji.sampleV5.aircraft.telemetry

import android.util.Log
import dji.sampleV5.aircraft.control.PachKeyManager
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
    val keyManager = PachKeyManager()

        init {
        Log.d("TelemetryRouter", "TelemetryRouter Initializing")
//        registerKeys()

    }



}
// PachKeyManager
// We'll setup all the listeners and convert into Rx.Flowable

// TelemetryService
// Setup Retrofit to push telemetry data to our server

// TelemetryRouter
// Create instance of TelemetryService and DJIKeyManagerListener.
// Subscribe to Flowable from DJIKeyManagerListener and push to TelemetryService.