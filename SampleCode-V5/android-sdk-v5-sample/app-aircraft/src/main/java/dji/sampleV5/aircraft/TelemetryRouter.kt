package dji.sampleV5.aircraft

import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.flightcontroller.GPSSignalLevel
import android.util.Log
import dji.sdk.keyvalue.key.BatteryKey
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

class TelemetryRouter {
    private var gpsFlowable: Flowable<GPSSignalLevel>? = null
    private var batteryFlowable: Flowable<Int>? = null

    init {
        KeyManager.getInstance().listen(
            KeyTools.createKey(FlightControllerKey.KeyConnection),
            this
        ) { oldValue, newValue ->
            run {
                Log.v("DJIMainActivity", "Connection: $newValue")

                // Use RxUtil.addListener() to get continuous GPS signal level
                gpsFlowable = RxUtil.addListener(
                    KeyTools.createKey(FlightControllerKey.KeyGPSSignalLevel),
                    object : Consumer<GPSSignalLevel> {
                        override fun accept(signalLevel: GPSSignalLevel) {
                            Log.v("DJIMainActivity", "GPS Signal Level: $signalLevel")
                        }
                    })

                // Use RxUtil.addListener() to get continuous battery level
                batteryFlowable = RxUtil.addListener(
                    KeyTools.createKey(BatteryKey.KeyChargeRemainingInPercent),
                    object : Consumer<Int> {
                        override fun accept(batteryLevel: Int) {
                            Log.v("DJIMainActivity", "Battery Level: $batteryLevel")
                        }
                 })
            }
        }
    }
}