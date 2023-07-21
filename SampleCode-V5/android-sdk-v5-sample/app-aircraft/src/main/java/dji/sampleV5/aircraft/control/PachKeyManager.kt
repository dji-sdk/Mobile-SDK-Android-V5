package dji.sampleV5.aircraft.control

import android.util.Log
import dji.sampleV5.aircraft.telemetry.Coordinate
import dji.sampleV5.aircraft.telemetry.TuskAircraftState
import dji.sampleV5.aircraft.telemetry.TuskAircraftStatus
import dji.sampleV5.aircraft.telemetry.TuskControllerStatus
import dji.sampleV5.aircraft.telemetry.TuskServiceWebsocket
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

class PachKeyManager {
    // Initialize necessary classes
    private val telemService = TuskServiceWebsocket()
    private var controller = VirtualStickControl()
    private var pidController = PidController(0.4f, 0.05f, 0.9f)

    var stateData = TuskAircraftState( 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0, windDirection = null, false)
    var statusData = TuskAircraftStatus( connected = false, battery = 0, gps = 0, signalQuality =  0,
            goHomeState = null, flightMode = null, motorsOn = false, homeLocationLat = null,
            homeLocationLong = null, gimbalAngle = 0.0)
    var controllerStatus = TuskControllerStatus( battery = 0, pauseButton = false, goHomeButton = false,
            leftStickX = 0,leftStickY=0,rightStickX=0,rightStickY=0, fiveDUp = false, fiveDDown = false,
            fiveDRight = false, fiveDLeft = false, fiveDPress = false)

    var backyardCoordinatesSingleAlt = listOf(
            Coordinate(40.010457220936324, -105.24444971137794, 200.0),
            Coordinate(40.011165499597105, -105.24412041426442, 200.0),
            Coordinate(40.01110330957, -105.24382269358645, 200.0),
            Coordinate(40.01045031086439, -105.24401215219972, 200.0)
    )

    var backyardCoordinatesIncreasingAlt = listOf(
            Coordinate(40.010457220936324, -105.24444971137794, 200.0),
            Coordinate(40.011165499597105, -105.24412041426442, 250.0),
            Coordinate(40.01110330957, -105.24382269358645, 300.0),
            Coordinate(40.01045031086439, -105.24401215219972, 350.0)
    )

    var backyardSingleCoordinate = listOf(Coordinate(40.010457220936324, -105.24444971137794, 30.0))
    // Create variables here
    private var keyDisposables: CompositeDisposable? = null


    private val fiveDKey = KeyTools.createKey(RemoteControllerKey.KeyFiveDimensionPressedStatus)
    var fiveDUp = false
    var fiveDDown = false
    var fiveDRight = false
    var fiveDLeft = false
    var fiveDPress = false

    init {
        telemService.connectWebSocket()
        initializeFlightParameters()
        keyDisposables = CompositeDisposable()
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
                if (fiveDPress){
                    followWaypoints(backyardSingleCoordinate)
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

    fun sendControllerStatus(status: TuskControllerStatus) = runBlocking {
        launch {
            telemService.postControllerStatus(status)
        }
    }

    // Holder function that registers all necessary keys and handles their operation during changes
    private fun registerKeys(){
        // Setup PachKeyManager and define the keys that we want to listen to
        Log.v("PachKeyManager", "Registering Keys")
        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D)
        ) {
            stateData = stateData.copy(
                latitude = it.latitude,
                longitude = it.longitude,
                altitude = it.altitude
            )
            sendState(stateData)
            Log.v("PachTelemetry", "KeyAircraftLocation $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyAircraftAttitude)
        ) {
            stateData = stateData.copy(
                roll = it.roll,
                pitch = it.pitch,
                yaw = it.yaw)
            sendState(stateData)
            Log.d("PachTelemetry", "KeyAircraftAttitude $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyAircraftVelocity)
        ) {
            stateData = stateData.copy(
                velocityX = it.x,
                velocityY = it.y,
                velocityZ = it.z)
            sendState(stateData)
            Log.d("PachTelemetry", "AircraftVelocity $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyWindSpeed)
        ) {
            stateData = stateData.copy(windSpeed = it)
            sendState(stateData)
            Log.d("PachTelemetry", "WindSpeed $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyWindDirection)
        ) {
            stateData = stateData.copy(windDirection = it.toString())
            sendState(stateData)
            Log.d("PachTelemetry", "WindDirection $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyIsFlying)
        ) {
            stateData = stateData.copy(isFlying = it)
            sendState(stateData)
            Log.d("PachTelemetry", "IsFlying $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyConnection)
        ) {
            statusData = statusData.copy(connected = it)
            sendStatus(statusData)
            Log.d("PachTelemetry", "Connection $it")
        }

        registerKey(
            KeyTools.createKey(BatteryKey.KeyChargeRemainingInPercent)
        ) {
            statusData = statusData.copy(battery = it)
            sendStatus(statusData)
            Log.d("PachTelemetry", "Battery Level $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyGPSSignalLevel)
        ) {
            statusData = statusData.copy(gps = it.value())
            sendStatus(statusData)
            Log.d("PachTelemetry", "GPSSignalLevel $it")
        }

        registerKey(
            KeyTools.createKey(AirLinkKey.KeySignalQuality)
        ) {
            statusData = statusData.copy(signalQuality = it)
            sendStatus(statusData)
            Log.d("PachTelemetry", "SignalQuality $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyGoHomeState)
        ) {
            statusData = statusData.copy(goHomeState = it.toString())
            sendStatus(statusData)
            Log.d("PachTelemetry", "GoHomeState $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyFlightModeString)
        ) {
            statusData = statusData.copy(flightMode = it)
            sendStatus(statusData)
            Log.d("PachTelemetry", "FlightMode $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyAreMotorsOn)
        ) {
            statusData = statusData.copy(motorsOn = it)
            sendStatus(statusData)
            Log.d("PachTelemetry", "MotorsOn $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyHomeLocation)
        ) {
            statusData =
                statusData.copy(
                    homeLocationLat = it.latitude,
                    homeLocationLong = it.longitude)
            sendStatus(statusData)
            Log.d("PachTelemetry", "HomeLocation $it")
        }

        registerKey(
            KeyTools.createKey(GimbalKey.KeyGimbalAttitude)
        ) {
            statusData = statusData.copy(gimbalAngle = it.pitch)
            sendStatus(statusData)
            Log.d("PachTelemetry", "GimbalPitch $it")
        }

        // TuskControllerKeys Setup
        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyBatteryInfo)
        ) {
            controllerStatus = controllerStatus.copy(battery = it.batteryPercent)
            sendControllerStatus(controllerStatus)
            Log.d("PachTelemetry", "ControllerBattery $it")
        }

        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyPauseButtonDown)
        ){
            controllerStatus = controllerStatus.copy(pauseButton = it)
            sendControllerStatus(controllerStatus)
            Log.d("PachTelemetry", "PauseButton $it")
        }

        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyGoHomeButtonDown)
        ){
            controllerStatus = controllerStatus.copy(goHomeButton = it)
            sendControllerStatus(controllerStatus)
            Log.d("PachTelemetry", "GoHomeButton $it")
        }

        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyStickLeftHorizontal)
        ){
            controllerStatus = controllerStatus.copy(leftStickX = it)
            sendControllerStatus(controllerStatus)
            Log.d("PachTelemetry", "StickLeftHorizontal $it")
        }

        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyStickLeftVertical)
        ){
            controllerStatus = controllerStatus.copy(leftStickY = it)
            sendControllerStatus(controllerStatus)
            Log.d("PachTelemetry", "StickLeftVertical $it")
        }

        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyStickRightHorizontal)
        ){
            controllerStatus = controllerStatus.copy(rightStickX = it)
            sendControllerStatus(controllerStatus)
            Log.d("PachTelemetry", "StickRightHorizontal $it")
        }

        registerKey(
            KeyTools.createKey(RemoteControllerKey.KeyStickLeftVertical)
        ){
            controllerStatus = controllerStatus.copy(rightStickY = it)
            sendControllerStatus(controllerStatus)
            Log.d("PachTelemetry", "StickRightVertical $it")
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
            sendControllerStatus(controllerStatus)
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

    private fun safetyChecks(): Boolean {
        // Function checks all safety information before returning a boolean for proceeding.
        // Checks:
        // 1. Is the aircraft flying?

        Log.v("SafetyChecks", "In Safety Check Function")

        if (!stateData.isFlying!!){
            Log.v("SafetyChecks", "Aircraft is not flying")
            return false
        }
        if (controllerStatus.goHomeButton!!){
            Log.v("SafetyChecks", "Go Home Button is pressed")
            return false
        }
        if (controllerStatus.pauseButton!!){
            Log.v("SafetyChecks", "Pause Button is pressed")
            return false
        }
        if (statusData.gps!! <3){
            Log.v("SafetyChecks", "GPS Signal is weak")
            return false
        }
        // Add check to see if it's in GOHOME State
        return true
    }
    fun go2Altitude(alt: Double){
        // When called, this function will make the aircraft go to a certain altitude
    }

    suspend fun go2Location(lat: Double, lon: Double, alt: Double){
        // When called, this function will make the aircraft go to a certain location
        // Edge Cases:
        // What if drone is already flying?
        // What if drone loses connection or GPS signal?
        // What if remote controller is disconnected?
        // What if drone is already at the location?
        // What if the operator takes control of the aircraft?


        // compute yaw angle based on current location and target location
        val yawAngle = computeYawAngle(lat, lon)
        Log.v("PachKeyManager", "Yaw Offset: $yawAngle")
//        // command yaw and altitude angle to drone
//        while (((abs(stateData.yaw?.minus(yawAngle)!!) > pidController.yawTolerance) &&
//                    (abs(stateData.altitude?.minus(alt)!!) > pidController.altTolerance))) {
//            Log.v("PachKeyManager", "Commanded Yaw: $yawAngle")
//            Log.v("PachKeyManager", "Commanded Altitude: $alt")
//            if (safetyChecks()) {
//                controller.sendYawAlt(yawAngle, alt)
//
//            } else{
//                Log.v("PachKeyManager", "Safety Check Failed")
//                break
//            }
//            delay(500L)
//        }

        while (abs(stateData.yaw?.minus(yawAngle)!!) > pidController.yawTolerance){
            Log.v("PachKeyManager", "Commanded Yaw: $yawAngle")

            if (safetyChecks()) {
                controller.sendYaw(yawAngle)

            } else{
                Log.v("PachKeyManager", "Safety Check Failed")
                break
            }
            delay(500L)
        }

        while (abs(stateData.altitude?.minus(alt)!!) > pidController.altTolerance){
            Log.v("PachKeyManager", "Commanded Altitude: $alt")
            if (safetyChecks()) {
                controller.setAlt(alt)
            } else{
                Log.v("PachKeyManager", "Safety Check Failed")
                break
            }
            delay(500L)
        }

        // compute distance to target location using lat and lon
        var distance = computeDistance(lat, lon)
        pidController.setSetpoint(distance)
        while (distance > pidController.posTolerance) {
            //What if we overshoot the target location? Will the aircraft back up or turn around?
            Log.v("PachKeyManager", "Distance: $distance")
            val xvel = pidController.getControl(stateData.latitude!!)
            xvel.coerceIn(-pidController.maxVelocity, pidController.maxVelocity)

            distance = computeDistance(lat, lon)

            // command drone x velocity to move to target location
            if (safetyChecks()) {
                controller.sendForwardVel(xvel)
            } else{
                Log.v("PachKeyManager", "Safety Check Failed")
                break
            }
            delay(500L)
        }
    }

    // compute distance to target location using lat and lon
    fun computeDistance(lat: Double, lon: Double)
            : Double {
        val latdiff = lat - stateData.latitude!!
        val londiff = lon - stateData.longitude!!
        return sqrt(latdiff.pow(2) + londiff.pow(2))
    }

    private fun computeYawAngle(lat: Double, lon: Double)
            : Double {
        val latdiff = lat - stateData.latitude!!
        val londiff = lon - stateData.longitude!!
        return atan2(londiff, latdiff)*(180/PI)
    }

    fun followWaypoints(wpList: List<Coordinate>)= runBlocking{
        // When called, this function will make the aircraft follow a list of waypoints
        // Figure out if the latest state is given

        // If drone is not flying, then takeoff
        if (stateData.isFlying!=true){
            controller.startTakeOff()
        }

        // Check to see that virtual stick is enabled
        if (!controller.virtualStickState.isVirtualStickEnable){
            controller.enableVirtualStick()
        }
        if (!controller.virtualStickState.isVirtualStickAdvancedModeEnabled){
            controller.enableVirtualStickAdvancedMode()
        }

        for (wp in wpList){
            if (safetyChecks()) {
                go2Location(wp.lat, wp.lon, wp.alt)
            } else{
                Log.v("SafetyChecks", "Safety Check Failed")
                break
            }
        }

//        controller.endVirtualStick()
    }
}

// Router
// dataProcessor
// new PachKeyManager().registerKey(BATTERY_KEY, dataProcessor)
// dataProcessor.subscribe()