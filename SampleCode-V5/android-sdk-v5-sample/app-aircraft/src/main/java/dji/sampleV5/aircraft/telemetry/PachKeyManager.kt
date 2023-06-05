package dji.sampleV5.aircraft.telemetry

import android.util.Log
import dji.sampleV5.aircraft.control.VirtualStickControl
import dji.sdk.keyvalue.key.*
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType
import dji.sdk.keyvalue.value.camera.ThermalAreaMetersureTemperature
import dji.sdk.keyvalue.value.camera.ThermalTemperatureMeasureMode
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
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
    private var virtualStick = VirtualStickControl()

    private val motorKey = KeyTools.createKey(FlightControllerKey.KeyAreMotorsOn)
    var motorOn = false

    private val fiveDKey = KeyTools.createKey(RemoteControllerKey.KeyFiveDimensionPressedStatus)
    var fiveDUp = false
    var fiveDDown = false
    var fiveDRight = false
    var fiveDLeft = false
    var fiveDPress = false

    private val compassHeadingKey = KeyTools.createKey(FlightControllerKey.KeyCompassHeading)
    var compassHeading = 0.0f

    private val flightStatus = KeyTools.createKey(FlightControllerKey.KeyIsFlying)
    var isFlying = false
    init {
        initializeFlightParameters()
        KeyManager.getInstance().listen(motorKey, this) { _, newValue ->
            run {
                Log.v("DJIMainActivity", "Motors: $newValue")
                if (newValue != null) {
                    motorOn = newValue
                }
            }
        }

        KeyManager.getInstance().listen(fiveDKey, this) { _, newValue ->
            run {
                Log.v("DJIMainActivity", "FiveD: $newValue")
                if (newValue != null) {
                    fiveDUp = newValue.upwards
                    fiveDDown = newValue.downwards
                    fiveDRight = newValue.rightwards
                    fiveDLeft = newValue.leftwards
                    fiveDPress = newValue.middlePressed
                }
                // If motors are on, start mission
                if (fiveDUp) {
                    virtualStick.startTakeOff()

                }

                if (fiveDDown && isFlying){
                    virtualStick.startLanding()
                    virtualStick.endVirtualStick()
                }

                if (fiveDLeft && isFlying){
                    if (compassHeading != 0.0f){
                        virtualStick.changeLeftPosition(300, 0)
                    }
//                    virtualStick.change_left_position(200,0)
                }
            }
        }

        KeyManager.getInstance().listen(flightStatus, this) { _, newValue ->
            run {
                Log.v("DJIMainActivity", "Flying: $newValue")
                if (newValue != null) {
                    isFlying = newValue
                }
            }
        }

        KeyManager.getInstance().listen(compassHeadingKey, this) { _, newValue ->
            run {
                Log.v("DJIMainActivity", "Compass Heading: $newValue")
                if (newValue != null) {
                    compassHeading = newValue.toFloat()
                }
            }
        }

        KeyManager.getInstance().listen(
            KeyTools.createKey(FlightControllerKey.KeyConnection),
            this
        ) { _, connect_status ->
            run {
                Log.v("DJIMainActivity", "Connection: $connect_status")

                    // Pulling aircraft flight status
//                    val aircraftStatus = KeyTools.createKey(FlightControllerKey.KeyIsFlying)
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
            }

        }
        keyDisposables = CompositeDisposable()

    }

    private fun initializeFlightParameters() {
        // Function initializes any static parameters prior to flight
        // Set battery warning threshold to 30%
        val batteryWarningValue = 30
        val batteryThresh = KeyTools.createKey(FlightControllerKey.KeyLowBatteryWarningThreshold)
        KeyManager.getInstance().setValue(batteryThresh, batteryWarningValue, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.v("DJIMainActivity", "Battery threshold set to $batteryWarningValue%")
            }

            override fun onFailure(error: IDJIError) {
                Log.e("DJIMainActivity", "Error: $error")
            }
        })
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

    fun get_thermal_video(){
        // When function is called, the thermal video will be set as primary stream
    // Get Thermal Camera Max Temperature
        // Set camera to thermal mode first by calling KeyCameraVideoStreamSource --> INFRARED_CAMERA
        val cameraSourceKey = KeyTools.createKey(CameraKey.KeyCameraVideoStreamSource)
        KeyManager.getInstance().setValue(cameraSourceKey,
            CameraVideoStreamSourceType.INFRARED_CAMERA,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.v("DJIMainActivity", "Camera Source Set to Thermal")
                    val thermalMeasureMode =
                        KeyTools.createKey(CameraKey.KeyThermalTemperatureMeasureMode)
                    object : CommonCallbacks.CompletionCallbackWithParam<Boolean> {
                        override fun onSuccess(value: Boolean?) {
                            Log.v(
                                "DJIMainActivity",
                                "Thermal Measurement Mode: ,$value"
                            )
                        }
                        override fun onFailure(error: IDJIError) {
                            Log.e(
                                "DJIMainActivity",
                                "Thermal Measurement Mode: ,$error"
                            )
                        }
                    }

    //                                    KeyTools.createKey(CameraKey.KeyThermalTemperatureMeasureMode)
                    KeyManager.getInstance().setValue(thermalMeasureMode,
                        ThermalTemperatureMeasureMode.REGION,
                        object : CommonCallbacks.CompletionCallback {
                            override fun onSuccess() {
                                Log.v(
                                    "DJIMainActivity",
                                    "Thermal Measure Mode Set to REGION"
                                )
                            }

                            override fun onFailure(error: IDJIError) {
                                Log.v(
                                    "DJIMainActivity",
                                    "Thermal Measure Mode Error: ,$error"
                                )
                            }
                        }
                    )
                    // Set CameraLensType to CAMERA_LENS_THERMAL
                    val thermalLensTypeKey = KeyTools.createCameraKey(
                        CameraKey.KeyThermalRegionMetersureTemperature,
                        ComponentIndexType.FPV,
                        CameraLensType.CAMERA_LENS_THERMAL
                    )
    //                    CameraLensType.CAMERA_LENS_THERMAL
                    KeyManager.getInstance().getValue(thermalLensTypeKey,
                        object :
                            CommonCallbacks.CompletionCallbackWithParam<ThermalAreaMetersureTemperature> {
                            // Call KeyThermalTemperatureMeasureMode to set ThermalTemperatureMeasureMode to REGION
                            override fun onSuccess(t: ThermalAreaMetersureTemperature?) {
                                Log.v(
                                    "DJIMainActivity",
                                    "Thermal Area Measure Mode Set to REGION"
                                )
                            }

                            override fun onFailure(error: IDJIError) {
                                Log.v(
                                    "DJIMainActivity",
                                    "Thermal Area Measure Mode Error: ,$error"
                                )
                            }
                        }
                    )
                }

                override fun onFailure(error: IDJIError) {
                    Log.v("DJIMainActivity", "Camera Source Set to Thermal")
                }
            }
        )
    }

    fun get_rgb_video(){
        // When function is called, the rgb video will be set as primary stream
    }

    fun go_to_altitude(alt: Double){
        // When called, this function will make the aircraft go to a certain altitude
    }

    fun go_to_location(lat: Double, lon: Double, alt: Double){
        // When called, this function will make the aircraft go to a certain location
        // Edge Cases:
            // What if drone is already flying?
            // What if drone loses connection or GPS signal?
            // What if remote controller is disconnected?
            // What if drone is already at the location?
            // What if the operator takes control of the aircraft?
    }

}

// Router
// dataProcessor
// new PachKeyManager().registerKey(BATTERY_KEY, dataProcessor)
// dataProcessor.subscribe()