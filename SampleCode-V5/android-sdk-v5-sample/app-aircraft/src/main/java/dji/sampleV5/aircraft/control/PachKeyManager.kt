package dji.sampleV5.aircraft.control

import android.util.Log
import dji.sampleV5.aircraft.telemetry.TuskAircraftState
import dji.sampleV5.aircraft.telemetry.TuskAircraftStatus
import dji.sampleV5.aircraft.telemetry.TuskControllerStatus
import dji.sampleV5.aircraft.telemetry.TuskServiceRetrofit
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
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PachKeyManager {
    // Initialize necessary classes
    private val telemService = TuskServiceRetrofit()
    private var controller = VirtualStickControl()
    var stateData = TuskAircraftState( 0.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0.0, 0, windDirection = null, false)
    var statusData = TuskAircraftStatus( connected = false, battery = 0, gps = 0, signalQuality =  0,
        goHomeState = null, flightMode = null, motorsOn = false, homeLocationLat = null,
        homeLocationLong = null, gimbalAngle = 0.0)
    var controllerStatus = TuskControllerStatus( battery = 0, pauseButton = false, goHomeButton = false,
        leftStickX = 0,leftStickY=0,rightStickX=0,rightStickY=0, fiveDUp = false, fiveDDown = false,
        fiveDRight = false, fiveDLeft = false, fiveDPress = false)

    // Create variables here
    private var keyDisposables: CompositeDisposable? = null


    private val fiveDKey = KeyTools.createKey(RemoteControllerKey.KeyFiveDimensionPressedStatus)
    var fiveDUp = false
    var fiveDDown = false
    var fiveDRight = false
    var fiveDLeft = false
    var fiveDPress = false

    init {
        initializeFlightParameters()
        registerKeys()

        KeyManager.getInstance().listen(fiveDKey, this) { _, newValue ->
            run {
                Log.v("PachKeyManager", "FiveD: $newValue")
                if (newValue != null) {
                    fiveDUp = newValue.upwards
                    fiveDDown = newValue.downwards
                    fiveDRight = newValue.rightwards
                    fiveDLeft = newValue.leftwards
                    fiveDPress = newValue.middlePressed
                }

                // If motors are on, start mission
                if (fiveDUp) {
                    controller.startTakeOff()

                }

                if (fiveDDown){
                    controller.startLanding()
                    controller.endVirtualStick()
                }
            }
        }
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
        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D)
        ) {
            stateData = stateData.copy(
                latitude = it.latitude,
                longitude = it.longitude,
                altitude = it.altitude
            )
//                sendState(stateData)
            Log.d("PachKeyManager", "KeyAircraftLocation $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyAircraftAttitude)
        ) {
            stateData = stateData.copy(roll = it.roll, pitch = it.pitch, yaw = it.yaw)
//                sendState(stateData)
            Log.d("PachKeyManager", "KeyAircraftAttitude $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyAircraftVelocity)
        ) {
            stateData = stateData.copy(velocityX = it.x, velocityY = it.y, velocityZ = it.z)
//                sendState(stateData)
            Log.d("PachKeyManager", "AircraftVelocity $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyWindSpeed)
        ) {
            stateData = stateData.copy(windSpeed = it)
//                sendState(stateData)
            Log.d("PachKeyManager", "WindSpeed $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyWindDirection)
        ) {
            stateData = stateData.copy(windDirection = it.toString())
//                sendState(stateData)
            Log.d("PachKeyManager", "WindDirection $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyIsFlying)
        ) {
            stateData = stateData.copy(isFlying = it)
//                sendState(stateData)
            Log.d("PachKeyManager", "IsFlying $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyConnection)
        ) {
            statusData = statusData.copy(connected = it)
//                sendStatus(statusData)
            Log.d("PachKeyManager", "Connection $it")
        }

        registerKey(
            KeyTools.createKey(BatteryKey.KeyChargeRemainingInPercent)
        ) {
            statusData = statusData.copy(battery = it)
//                sendStatus(statusData)
            Log.d("PachKeyManager", "Battery Level $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyGPSSignalLevel)
        ) {
            statusData = statusData.copy(gps = it.value())
//                sendStatus(statusData)
            Log.d("PachKeyManager", "GPSSignalLevel $it")
        }

        registerKey(
            KeyTools.createKey(AirLinkKey.KeySignalQuality)
        ) {
            statusData = statusData.copy(signalQuality = it)
//                sendStatus(statusData)
            Log.d("PachKeyManager", "SignalQuality $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyGoHomeState)
        ) {
            statusData = statusData.copy(goHomeState = it.toString())
//                sendStatus(statusData)
            Log.d("PachKeyManager", "GoHomeState $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyFlightModeString)
        ) {
            statusData = statusData.copy(flightMode = it)
//                sendStatus(statusData)
            Log.d("PachKeyManager", "FlightMode $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyAreMotorsOn)
        ) {
            statusData = statusData.copy(motorsOn = it)
//                sendStatus(statusData)
            Log.d("PachKeyManager", "MotorsOn $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyHomeLocation)
        ) {
            statusData =
                statusData.copy(homeLocationLat = it.latitude, homeLocationLong = it.longitude)
//                sendStatus(statusData)
            Log.d("PachKeyManager", "HomeLocation $it")
        }

        registerKey(
            KeyTools.createKey(GimbalKey.KeyGimbalAttitude)
        ) {
            statusData = statusData.copy(gimbalAngle = it.pitch)
//                sendStatus(statusData)
            Log.d("PachKeyManager", "GimbalPitch $it")
        }

        // TuskControllerKeys Setup
        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyBatteryInfo)
        ) {
            controllerStatus = controllerStatus.copy(battery = it.batteryPercent)
            Log.d("PachKeyManager", "ControllerBattery $it")
        }

        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyPauseButtonDown)
        ){
            controllerStatus = controllerStatus.copy(pauseButton = it)
            Log.d("PachKeyManager", "PauseButton $it")
        }

        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyGoHomeButtonDown)
        ){
            controllerStatus = controllerStatus.copy(goHomeButton = it)
            Log.d("PachKeyManager", "GoHomeButton $it")
        }

        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyStickLeftHorizontal)
        ){
            controllerStatus = controllerStatus.copy(leftStickX = it)
            Log.d("PachKeyManager", "StickLeftHorizontal $it")
        }

        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyStickLeftVertical)
        ){
            controllerStatus = controllerStatus.copy(leftStickY = it)
            Log.d("PachKeyManager", "StickLeftVertical $it")
        }

        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyStickRightHorizontal)
        ){
            controllerStatus = controllerStatus.copy(rightStickX = it)
            Log.d("PachKeyManager", "StickRightHorizontal $it")
        }

        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyStickLeftVertical)
        ){
            controllerStatus = controllerStatus.copy(rightStickY = it)
            Log.d("PachKeyManager", "StickRightVertical $it")
        }

        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyFiveDimensionPressedStatus)
        ){
            controllerStatus = controllerStatus.copy(
                fiveDUp = it.upwards,
                fiveDDown = it.downwards,
                fiveDLeft = it.leftwards,
                fiveDRight = it.rightwards,
                fiveDPress = it.middlePressed)
            Log.d("PachKeyManager", "FiveDButton $it")
        }
        // Continue to do this for the other required keys...
    }

    private fun initializeFlightParameters() {
        // Function initializes any static parameters prior to flight
        // Set battery warning threshold to 30%
        val batteryWarningValue = 30
        val batteryThresh = KeyTools.createKey(FlightControllerKey.KeyLowBatteryWarningThreshold)
        KeyManager.getInstance().setValue(batteryThresh, batteryWarningValue, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                Log.v("PachKeyManager", "Battery threshold set to $batteryWarningValue%")
            }

            override fun onFailure(error: IDJIError) {
                Log.e("PachKeyManager", "Error: $error")
            }
        })
    }

    // Create functions to act on received actions or key sets

    // Listen for pause button

    // Start mission

    // Stop flying but don't crash or land

    // land now

    private fun <T> registerKey(
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

    fun getThermalVideo(){
        // When function is called, the thermal video will be set as primary stream
    // Get Thermal Camera Max Temperature
        // Set camera to thermal mode first by calling KeyCameraVideoStreamSource --> INFRARED_CAMERA
        val cameraSourceKey = KeyTools.createKey(CameraKey.KeyCameraVideoStreamSource)
        KeyManager.getInstance().setValue(cameraSourceKey,
            CameraVideoStreamSourceType.INFRARED_CAMERA,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.v("PachKeyManager", "Camera Source Set to Thermal")
                    val thermalMeasureMode =
                        KeyTools.createKey(CameraKey.KeyThermalTemperatureMeasureMode)
                    object : CommonCallbacks.CompletionCallbackWithParam<Boolean> {
                        override fun onSuccess(value: Boolean?) {
                            Log.v(
                                "PachKeyManager",
                                "Thermal Measurement Mode: ,$value"
                            )
                        }
                        override fun onFailure(error: IDJIError) {
                            Log.e(
                                "PachKeyManager",
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
                                    "PachKeyManager",
                                    "Thermal Measure Mode Set to REGION"
                                )
                            }

                            override fun onFailure(error: IDJIError) {
                                Log.v(
                                    "PachKeyManager",
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
                                    "PachKeyManager",
                                    "Thermal Area Measure Mode Set to REGION"
                                )
                            }

                            override fun onFailure(error: IDJIError) {
                                Log.v(
                                    "PachKeyManager",
                                    "Thermal Area Measure Mode Error: ,$error"
                                )
                            }
                        }
                    )
                }

                override fun onFailure(error: IDJIError) {
                    Log.v("PachKeyManager", "Camera Source Set to Thermal")
                }
            }
        )
    }

    fun getWideVideo(){
        // When function is called, the rgb video will be set as primary stream
        val cameraSourceKey = KeyTools.createKey(CameraKey.KeyCameraVideoStreamSource)
        KeyManager.getInstance().setValue(cameraSourceKey,
            CameraVideoStreamSourceType.WIDE_CAMERA,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.v("PachKeyManager", "Camera Source Set to Wide")
                    }
                override fun onFailure(error: IDJIError) {
                    Log.v("PachKeyManager", "Camera Failed to Set to Wide $error")
                }
            })
    }

    fun getZoomVideo(){
        // When function is called, the rgb video will be set as primary stream
        val cameraSourceKey = KeyTools.createKey(CameraKey.KeyCameraVideoStreamSource)
        KeyManager.getInstance().setValue(cameraSourceKey,
            CameraVideoStreamSourceType.ZOOM_CAMERA,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.v("PachKeyManager", "Camera Source Set to Zoom")
                }
                override fun onFailure(error: IDJIError) {
                    Log.v("PachKeyManager", "Camera Failed to Set to Zoom $error")
                }
            })
    }

    fun go2Altitude(alt: Double){
    // When called, this function will make the aircraft go to a certain altitude
    }

    fun go2Location(lat: Double, lon: Double, alt: Double){
        // When called, this function will make the aircraft go to a certain location
        // Edge Cases:
            // What if drone is already flying?
            // What if drone loses connection or GPS signal?
            // What if remote controller is disconnected?
            // What if drone is already at the location?
            // What if the operator takes control of the aircraft?
    }

    fun followWaypoints(wpList: Triple<Double, Double, Double>){
        // When called, this function will make the aircraft follow a list of waypoints

    }
}

// Router
// dataProcessor
// new PachKeyManager().registerKey(BATTERY_KEY, dataProcessor)
// dataProcessor.subscribe()