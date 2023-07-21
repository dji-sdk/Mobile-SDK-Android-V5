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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class PachKeyManager() {
    // Initialize necessary classes
    private val telemService = TuskServiceWebsocket()
    private var controller = VirtualStickControl()
    private var pidController = PidController(0.4f, 0.05f, 0.9f)
    val mainScope = CoroutineScope(Dispatchers.Main)

    var stateData = TuskAircraftState( 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0, windDirection = null, false)
    var statusData = TuskAircraftStatus( connected = false, battery = 0, gps = 0, signalQuality =  0,
            goHomeState = null, flightMode = null, motorsOn = false, homeLocationLat = null,
            homeLocationLong = null, gimbalAngle = 0.0, goHomeStatus = null)
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

    var backyardSingleCoordinate = listOf(Coordinate(40.010819889488076, -105.244268000203, 30.0))
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

    }

    fun runTesting() {
        KeyManager.getInstance().listen(fiveDKey, this) { _, newValue ->
            mainScope.launch {
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

                if (fiveDDown) {
                    controller.startLanding()
                    controller.endVirtualStick()
                }
                if (fiveDPress) {
                    Log.v("PachKeyManager", "FiveD Pressed")
                    followWaypoints(backyardSingleCoordinate)

                }
            }

        }
        registerKeys()

    }

    private fun sendState(telemetry: TuskAircraftState) {
        telemService.postState(telemetry)
    }

    private fun sendStatus(status: TuskAircraftStatus) {
        telemService.postStatus(status)
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
        registerKey(
                KeyTools.createKey(FlightControllerKey.KeyGoHomeState)
        ){
            statusData = statusData.copy(goHomeStatus = it.toString())
            sendStatus(statusData)
            Log.d("PachTelemetry", "GoHomeStatus $it")
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

        if (!stateData.isFlying!!){
            Log.v("PachKeyManager", "Aircraft is not flying")
            return false
        }
        if (controllerStatus.goHomeButton!!){
            Log.v("PachKeyManager", "Go Home Button is pressed")
            return false
        }
        if (controllerStatus.pauseButton!!){
            Log.v("SafetyChecks", "Pause Button is pressed")
            return false
        }
        if (statusData.gps!! <3){
            Log.v("PachKeyManager", "GPS Signal is weak")
            return false
        }
        // Add check to see if it's in IDLE State
        if (statusData.goHomeStatus != "IDLE"){
            Log.v("PachKeyManager", "Aircraft is IDLE")
            return false
        }

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
            delay(100L)
        }

        while (abs(stateData.altitude?.minus(alt)!!) > pidController.altTolerance){
            Log.v("PachKeyManager", "Commanded Altitude: $alt")
            if (safetyChecks()) {
                controller.setAlt(alt)
            } else{
                Log.v("PachKeyManager", "Safety Check Failed")
                break
            }
            delay(100L)
        }

        // compute distance to target location using lat and lon
        var distance = computeLatLonDistance(lat, lon)
        pidController.setSetpoint(distance)
        while (distance > pidController.posTolerance) { // add check for velocity tolerance: if velocity too high, keep going
            // ((distance > pidController.posTolerance) and (stateData.velocityX!! > pidController.velTolerance))
            //What if we overshoot the target location? Will the aircraft back up or turn around?
            Log.v("PachKeyManager", "Distance: $distance")
            var xvel = pidController.getControl(stateData.latitude!!)
            xvel = xvel.coerceIn(-pidController.maxVelocity, pidController.maxVelocity)
            Log.v("PachKeyManager", "Commanded X Velocity: $xvel")
            distance = computeLatLonDistance(lat, lon)

            // command drone x velocity to move to target location
            if (safetyChecks()) {
                controller.sendForwardVel(xvel)
            } else{
                Log.v("PachKeyManager", "Safety Check Failed")
                break
            }
            delay(100L)
        }
    }

    // compute distance to target location using lat and lon
    fun computeDistance(lat: Double, lon: Double)
            : Double {
        val latdiff = lat - stateData.latitude!!
        val londiff = lon - stateData.longitude!!
        return sqrt(latdiff.pow(2) + londiff.pow(2))
    }

    fun computeLatLonDistance(lat1 : Double, lon1: Double): Double {  // generally used geo measurement function
        val lat2 = stateData.latitude!!
        val lon2 = stateData.longitude!!
        val R = 6378.137 // Radius of earth in KM
        val dLat = lat2 * Math.PI / 180.0 - lat1 * Math.PI / 180.0;
        val dLon = lon2 * Math.PI / 180.0 - lon1 * Math.PI / 180.0
        val a = sin(dLat/2) * sin(dLat/2) +
                cos(lat1 * Math.PI / 180) * cos(lat2 * Math.PI / 180) *
                Math.sin(dLon/2) * Math.sin(dLon/2)
        val c = 2.0 * atan2(Math.sqrt(a), sqrt(1-a))
        val d = R * c
        return d * 1000.0 // meters
    }

    private fun computeYawAngle(lat: Double, lon: Double)
            : Double {
        val yDiff = lat - stateData.latitude!!
        val xDiff = lon - stateData.longitude!!
        val res = atan2(yDiff, xDiff) *(180 / PI)
        if ((yDiff<0.0) && (xDiff<0.0)) {
            return -(270.0+res)
        }else{
            return 90-res
        }
    }

    suspend fun followWaypoints(wpList: List<Coordinate>){
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