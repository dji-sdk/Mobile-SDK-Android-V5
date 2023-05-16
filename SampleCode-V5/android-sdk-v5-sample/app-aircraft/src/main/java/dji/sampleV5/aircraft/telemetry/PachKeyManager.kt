package dji.sampleV5.aircraft.telemetry

import android.util.Log
import dji.sdk.keyvalue.key.*
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotation
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotationMode
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.utils.RxUtil
import dji.v5.manager.KeyManager
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer

class PachKeyManager {
    // Create variables here
    private var keyDisposables: CompositeDisposable? = null
    private var batteryDataProcessor = DataProcessor.create(0)
    private var gpsDataProcessor = DataProcessor.create(0)
    private var attitudeDataProcessor = DataProcessor.create(0)
    init {

        // Init flowables that are required
        // Attitude
        // Battery
        // GPS Status

//        KeyManager.getInstance().listen(
//            KeyTools.createKey(FlightControllerKey.KeyConnection),
//            this
//        ) { oldValue, newValue ->
//            run {
//                Log.v("DJIMainActivity", "Connection: $newValue")
//                if (newValue == true) {
//                    // Pulling aircraft flight status
//                    val aircraftStatus = KeyTools.createKey(FlightControllerKey.KeyIsFlying)
//
//
//                    /// Get Thermal Camera Max Temperature
//                    // Set camera to thermal mode first by calling KeyCameraVideoStreamSource --> INFRARED_CAMERA
//                    val cameraSourceKey = KeyTools.createKey(CameraKey.KeyCameraVideoStreamSource)
//                    KeyManager.getInstance().setValue(cameraSourceKey,
//                        CameraVideoStreamSourceType.INFRARED_CAMERA,
//                        object : CommonCallbacks.CompletionCallback {
//                            override fun onSuccess() {
//                                Log.v("DJIMainActivity", "Camera Source Set to Thermal")
//                                val thermalMeasureMode =
//                                    KeyTools.createKey(CameraKey.KeyThermalTemperatureMeasureMode)
//                                object : CommonCallbacks.CompletionCallbackWithParam<Boolean> {
//                                    override fun onSuccess(value: Boolean?) {
//                                        Log.v(
//                                            "DJIMainActivity",
//                                            "Thermal Measurement Mode: ,$value"
//                                        )
//                                    }
//
//                                    override fun onFailure(error: IDJIError) {
//                                        Log.e(
//                                            "DJIMainActivity",
//                                            "Thermal Measurement Mode: ,$error"
//                                        )
//                                    }
//                                }
//
////                                    KeyTools.createKey(CameraKey.KeyThermalTemperatureMeasureMode)
//                                KeyManager.getInstance().setValue(thermalMeasureMode,
//                                    ThermalTemperatureMeasureMode.REGION,
//                                    object : CommonCallbacks.CompletionCallback {
//                                        override fun onSuccess() {
//                                            Log.v(
//                                                "DJIMainActivity",
//                                                "Thermal Measure Mode Set to REGION"
//                                            )
//                                        }
//
//                                        override fun onFailure(error: IDJIError) {
//                                            Log.v(
//                                                "DJIMainActivity",
//                                                "Thermal Measure Mode Error: ,$error"
//                                            )
//                                        }
//                                    }
//                                )
//                                // Set CameraLensType to CAMERA_LENS_THERMAL
//                                val thermalLensTypeKey = KeyTools.createCameraKey(
//                                    CameraKey.KeyThermalRegionMetersureTemperature,
//                                    ComponentIndexType.FPV,
//                                    CameraLensType.CAMERA_LENS_THERMAL
//                                )
////                    CameraLensType.CAMERA_LENS_THERMAL
//                                KeyManager.getInstance().getValue(thermalLensTypeKey,
//                                    object :
//                                        CommonCallbacks.CompletionCallbackWithParam<ThermalAreaMetersureTemperature> {
//                                        // Call KeyThermalTemperatureMeasureMode to set ThermalTemperatureMeasureMode to REGION
//                                        override fun onSuccess(t: ThermalAreaMetersureTemperature?) {
//                                            Log.v(
//                                                "DJIMainActivity",
//                                                "Thermal Area Measure Mode Set to REGION"
//                                            )
//                                        }
//
//                                        override fun onFailure(error: IDJIError) {
//                                            Log.v(
//                                                "DJIMainActivity",
//                                                "Thermal Area Measure Mode Error: ,$error"
//                                            )
//                                        }
//                                    }
//                                )
//                            }
//
//                            override fun onFailure(error: IDJIError) {
//                                Log.v("DJIMainActivity", "Camera Source Error: ,$error")
//                            }
//                        }
//                    )
//
//                    val gimbalAngleKey = KeyTools.createKey(GimbalKey.KeyGimbalAttitude)
//                    var gimbalAngle = 0.0
//                    KeyManager.getInstance().listen(gimbalAngleKey, this) { _, newValue ->
//                        run {
//                            if (newValue != null) {
//                                gimbalAngle = newValue.getPitch().toDouble()
//                                Log.v("DJIMainActivity", "Gimbal Angle: $gimbalAngle")
//                                if (gimbalAngle < -45.0) {
//                                    rotateGimbal(0.0) // Rotates gimbal to 0deg
//                                }
//                            }
//                            Log.v("DJIMainActivity", "Gimbal Angle: $newValue")
//                        }
//                    }
//                    rotateGimbal(-45.0) // Rotates gimbal to -45deg
//                }
//            }
//
//        }
        keyDisposables = CompositeDisposable()
//
//        keyDisposables?.add(
//            RxUtil.addListener(KeyTools.createKey(FlightControllerKey.KeyAircraftAttitude), this)
//                .subscribe({ value ->
//                    Log.v("PachKeyManager", "Aircraft Attitude: $value")
//                }, { error ->
//                    Log.e("PachKeyManager", "Aircraft Attitude Error: $error")
//                })
////                .doOnNext(batteryDataProcessor)
//        )
//        keyDisposables?.add(
//            RxUtil.addListener(KeyTools.createKey(FlightControllerKey.KeyAircraftLocation), this)
//                .subscribe({ value ->
//                    Log.v("PachKeyManager", "Aircraft Location: $value")
//                }, { error ->
//                    Log.e("PachKeyManager", "Aircraft Location Error: $error")
//                })
//        )



    }

    // Create functions to act on received actions or key sets

    // Listen for pause button

    // Start mission

    // Stop flying but don't crash or land

    // land now

    fun <T> registerKey(
        djiKey: DJIKey<T>,
        consumer: Consumer<T>,
    ): CompositeDisposable? {
        keyDisposables?.add(
            RxUtil.addListener(djiKey, this)
                .subscribe(consumer) { error ->
                    Log.e("PachKeyManager", "Error: $error")
                }

        )
        return keyDisposables
    }

    // Make a function to rotate the gimbal by a certain angle
    fun rotateGimbal(angle: Double) {
        // Gimbal can be rotated by a range of [-90, 35]
        // The provided angle sets the pitch of the gimbal to the given angle
        val gimbalKey = KeyTools.createKey(GimbalKey.KeyRotateByAngle)
        KeyManager.getInstance().performAction(gimbalKey,
            GimbalAngleRotation(
                GimbalAngleRotationMode.ABSOLUTE_ANGLE,
                angle, // Pitch
                0.0,   // Roll
                0.0,   // Yaw
                false, // Pitch ignored
                false, // Roll ignored
                false, // Yaw ignored
                2.0,   // Rotation time
                false, // Joint reference
                0      // Timeout
            ),
            object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
                override fun onSuccess(t: EmptyMsg?) {
                    Log.v("PachKeyManager", "Gimbal Rotated")
                }

                override fun onFailure(error: IDJIError) {
                    Log.e("PachKeyManager", "Gimbal Rotation Error: ,$error")
                }
            }
        )
    }

}

// Router
// dataProcessor
// new PachKeyManager().registerKey(BATTERY_KEY, dataProcessor)
// dataProcessor.subscribe()