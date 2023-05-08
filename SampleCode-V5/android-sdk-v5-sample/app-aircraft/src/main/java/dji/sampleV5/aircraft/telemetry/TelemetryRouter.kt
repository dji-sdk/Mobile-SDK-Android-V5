package dji.sampleV5.aircraft.telemetry

import dji.sdk.keyvalue.value.flightcontroller.GPSSignalLevel
import android.util.Log
import dji.sdk.keyvalue.key.*
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType
import dji.sdk.keyvalue.value.camera.ThermalAreaMetersureTemperature
import dji.sdk.keyvalue.value.camera.ThermalTemperatureMeasureMode
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotation
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotationMode
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import io.reactivex.rxjava3.core.Flowable;




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


import dji.v5.common.utils.RxUtil
import dji.v5.manager.KeyManager
import io.reactivex.rxjava3.functions.Consumer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TelemetryRouter {
    val keyManager = KeyManager.getInstance()
    private val telemService = TuskService()

    init {
        // Setup Retrofit
        val mockTelem = TuskTelemetry(0.0, 0.0, 0.0, 0, 0, 0.0, 0.0, 0.0)
        send_telemetry(mockTelem)


        // Setup PachKeyManager
    }

fun send_telemetry(telemetry: TuskTelemetry) = runBlocking{
    launch {
        telemService.postTelemetry(telemetry)
    }
}



}
// PachKeyManager
// We'll setup all the listeners and convert into Rx.Flowable

// TelemetryService
// Setup Retrofit to push telemetry data to our server

// TelemetryRouter
// Create instance of TelemetryService and DJIKeyManagerListener.
// Subscribe to Flowable from DJIKeyManagerListener and push to TelemetryService.