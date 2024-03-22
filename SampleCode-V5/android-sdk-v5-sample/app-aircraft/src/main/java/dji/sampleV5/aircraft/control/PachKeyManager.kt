package dji.sampleV5.aircraft.control

import android.util.Log
import dji.sampleV5.aircraft.telemetry.Coordinate
import dji.sampleV5.aircraft.telemetry.Event
import dji.sampleV5.aircraft.telemetry.StreamInfo
import dji.sampleV5.aircraft.telemetry.TuskAircraftState
import dji.sampleV5.aircraft.telemetry.TuskAircraftStatus
import dji.sampleV5.aircraft.telemetry.TuskControllerStatus
import dji.sampleV5.aircraft.telemetry.TuskServiceWebsocket
import dji.sampleV5.aircraft.video.StreamManager
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
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class PachKeyManager() {
    // Initialize necessary classes
    private val telemService = TuskServiceWebsocket()
    private var controller = VirtualStickControl()
    private var pidController = PidController(0.4f, 0.05f, 0.9f)
    val mainScope = CoroutineScope(Dispatchers.Main)
    val streamer = StreamManager()
    var stateData = TuskAircraftState( 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0, 0, windDirection = null, false)
    var statusData = TuskAircraftStatus( connected = false, battery = 0, gpsSignal = 0, gps = 0,
        signalQuality =  0,  goHomeState = null, flightMode = null, motorsOn = false, homeLocationLat = null,
            homeLocationLong = null, gimbalAngle = 0.0, goHomeStatus = null, takeoffAltitude = null)
    var controllerStatus = TuskControllerStatus( battery = 0, pauseButton = false, goHomeButton = false,
            leftStickX = 0,leftStickY=0,rightStickX=0,rightStickY=0, fiveDUp = false, fiveDDown = false,
            fiveDRight = false, fiveDLeft = false, fiveDPress = false)
    val R = 6378.137 // Radius of earth in KM

    var backyardCoordinatesSingleAlt = listOf(
            Coordinate(40.010457220936324, -105.24444971137794, 200.0),
            Coordinate(40.011165499597105, -105.24412041426442, 200.0),
            Coordinate(40.01110330957, -105.24382269358645, 200.0),
            Coordinate(40.01045031086439, -105.24401215219972, 200.0)
    )

    var backyardCoordinatesIncreasingAlt = listOf(
            Coordinate(40.01079, -105.24426, 30.0),
            Coordinate(40.01114, -105.24407, 40.0),
            Coordinate(40.01103, -105.24356, 45.0),
            Coordinate(40.01061, -105.24414, 50.0)
    )

    var backyardCoordinatesComplexChangingAlt = listOf(
        Coordinate(40.01079, -105.24426, 30.0),
        Coordinate(40.01114, -105.24407, 40.0),
        Coordinate(40.01103, -105.24356, 45.0),
        Coordinate(40.01061, -105.24414, 40.0),
        Coordinate(40.01114, -105.24407, 40.0),
        Coordinate(40.01173, -105.24352, 45.0),
        Coordinate(40.01174, -105.24273, 50.0),
        Coordinate(40.01046, -105.24427, 15.0)
    )

    var backyardSingleCoordinate = listOf(Coordinate(40.010819889488076, -105.244268000203, 30.0))

    var HIPPOWaypoints = listOf<Coordinate>()
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
//        streamer.startStream()
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
//                    streamer.startStream()
                    sendStreamURL(streamer.getStreamURL())
//                    streamer.initChannelStateListener()
//                    controller.startLanding()
//                    controller.endVirtualStick()
                }
                if (fiveDPress) {
                    Log.v("PachKeyManager", "FiveD Pressed")
//                    followWaypoints(backyardCoordinatesComplexChangingAlt)
//                    Log.v("PachKeyManager", "Following Waypoint List: $HIPPOWaypoints")
//                    HIPPOWaypoints = telemService.waypointList
//                    followWaypoints(HIPPOWaypoints)
                    flyHippo()
//                    flyOrbitPath(
//                        Coordinate(stateData.latitude!!, stateData.longitude!!,stateData.altitude!!),
//                        10.0)
//                    diveAndYaw(60.0, 20.0)
//                    controller.endVirtualStick()
                    Log.v("PachKeyManager", "Finished fiveDPress Execution")
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

    private fun sendAutonomyStatus(status: String) {
        telemService.postAutonomyStatus(Event(status))
        Log.v("PachKeyManager", "Autonomy Status: $status")
    }

    private fun sendStreamURL(url: String) {
        telemService.postStreamURL(StreamInfo(url))
        Log.v("PachKeyManager", "Stream URL: $url")
    }

    private fun sendControllerStatus(status: TuskControllerStatus) {
        telemService.postControllerStatus(status)
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
            KeyTools.createKey(FlightControllerKey.KeyGPSSatelliteCount)
        )    {
                statusData = statusData.copy(gps = it)
                sendStatus(statusData)
                Log.d("PachTelemetry", "GPSSatelliteCount $it")
        }

        registerKey(
            KeyTools.createKey(FlightControllerKey.KeyGPSSignalLevel)
        ) {
            statusData = statusData.copy(gpsSignal = it.value())
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

        registerKey(
                KeyTools.createKey(FlightControllerKey.KeyTakeoffLocationAltitude)
        ){
            statusData = statusData.copy(takeoffAltitude = it)
            sendStatus(statusData)
            Log.d("PachTelemetry", "TakeoffAltitude $it")
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

    private fun <T : Any> registerKey(
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
    private fun decisionChecks(): Boolean {
        // Function checks to see if any key decision points have been reached
        if (telemService.isAlertAction) {
            Log.v("PachKeyManager", "Alert Action")
            return false
        }
        if (telemService.isGatherAction) {
            Log.v("PachKeyManager", "Gather Action")
            return false
        }

        return true
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
            Log.v("PachKeyManager", "Pause Button is pressed")
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
    suspend fun goToAltitude(alt: Double){
        // When called, this function will make the aircraft go to a certain altitude
        while (stateData.altitude!! != alt) {
            Log.v("PachKeyManager", "Altitude Set: ${alt}. Current Altitude: ${stateData.altitude}")
            // command drone x velocity to move to target location
            if (safetyChecks()) {
                controller.sendVirtualStickVelocityBody(0.0, 0.0, stateData.yaw!!, alt)
            } else {
                Log.v("PachKeyManager", "Safety Check Failed")
                break
            }

            delay(100L)
        }
    }

    private fun adjustAltForTerrain(alt: Double): Double {
        // Function expects the input to be in MSL altitude.
        // Using the takeoff altitude, we then adjust to account for relative altitude in meters
        return alt - statusData.takeoffAltitude!!
    }

    suspend fun goToYawAngle(angle: Double){
        // When called aircraft will rotate to a given yaw angle
        while (stateData.yaw!! != angle) {
            Log.v("PachKeyManager", "Yaw Set: ${stateData.yaw}")
            // command drone x velocity to move to target location
            if (safetyChecks()) {
                controller.sendVirtualStickVelocityBody(0.0, 0.0, angle, stateData.altitude!!)
            } else {
                Log.v("PachKeyManager", "Safety Check Failed")
                break
            }
            delay(100L)
        }
    }

    suspend fun go2LocationForward(lat: Double, lon: Double, alt: Double){
        // When called, this function will make the aircraft go to a certain location
        // Setting the yaw to a negative number will cause the yaw to be dynamically set. Helpful
        // cases when the aircraft needs to adjust its heading to reach a location
        // Edge Cases:
        // What if drone is already flying?
        // What if drone loses connection or GPS signal?
        // What if remote controller is disconnected?
        // What if drone is already at the location?
        // What if the operator takes control of the aircraft?

        // compute distance to target location using lat and lon
        var distance = computeLatLonDistance(lat, lon)
        var yawAngle = computeYawAngle(lat, lon)
        var xVel = pidController.getControl(distance)
        while (distance > pidController.posTolerance) {
            // ((distance > pidController.posTolerance) and (stateData.velocityX!! > pidController.velTolerance))
            //What if we overshoot the target location? Will the aircraft back up or turn around?
            Log.v("PachControlAction", "Distance: $distance")
            xVel = pidController.getControl(distance)
            val clippedXvel = xVel.coerceIn(-pidController.maxVelocity, pidController.maxVelocity)
            Log.v("PachControlAction", "Commanded X Velocity: $xVel, Clipped Velocity  $clippedXvel")
            distance = computeLatLonDistance(lat, lon)

            // Update Yaw
            yawAngle = computeYawAngle(lat, lon)

            Log.v("PachControlAction", "Commanded Yaw: $yawAngle | Commanded Altitude: $alt | xvel: $xVel | clippedXvel: $clippedXvel")

            // command drone x velocity to move to target location
            if (decisionChecks()) {
                if (safetyChecks()) {
                    controller.sendVirtualStickVelocityBody(clippedXvel, 0.0, yawAngle, alt)
                } else {
                    Log.v("PachKeyManager", "Safety Check Failed")
                    break
                }
            } else {
                Log.v("PachKeyManager", "Decision Check Failed")
                break
            }
            delay(100L)
        }
    }

    suspend fun goToLocationFixedYaw(lat: Double, lon: Double, alt: Double, yaw: Double){
        // Function goes to a coordinate location assuming a fixed yaw angle
        // compute distance to target location using lat and lon
        // TODO: There seems to be a control issue with this implementation.
        //  Aircraft doesn't seem to reach the waypoint in the expected manner. Potential coordinate frame issue.
        //  Can try checking basic flight control
        var distance = computeLatLonDistance(lat, lon)
        while (distance > pidController.posTolerance) {
            // ((distance > pidController.posTolerance) and (stateData.velocityX!! > pidController.velTolerance))
            //What if we overshoot the target location? Will the aircraft back up or turn around?
            Log.v("PachControlAction", "Distance: $distance")
            val yError = computeLatDistance(lat)
            val xError = computeLonDistance(lon)
            Log.v("PachControlAction", "Y Error: $yError | X Error: $xError | Distance: $distance")
            val xVel = pidController.getControl(xError)
            val yVel = pidController.getControl(yError)
            val clippedXvel = xVel.coerceIn(-pidController.maxVelocity, pidController.maxVelocity)
            val clippedYvel = yVel.coerceIn(-pidController.maxVelocity, pidController.maxVelocity)

            Log.v("PachControlAction", "Commanded Yaw: $yaw | Commanded Altitude: $alt | xvel: $xVel | yvel: $yVel")
            // command drone x & y velocity to move to target location with a defined yaw
            if (!telemService.isAlertAction) {
                if (safetyChecks()) {
                    controller.sendVirtualStickVelocityGround(clippedYvel, clippedXvel, yaw, alt)
                } else {
                    Log.v("PachKeyManager", "Safety Check Failed")
                    break
                }
            } else {
                Log.v("PachKeyManager", "Alerted Operator")
                break
            }
            delay(50L)
            distance = computeLatLonDistance(lat, lon)
        }
    }

    suspend fun flyHippo() {
        // Function will fly using the HIPPO decision making framework.
        // This will fly to a given waypoint and continue along to the following waypoint unless
        // a specific decision making flag has been raised.
        // When called, this function will make the aircraft go to a certain location
        // Edge Cases:
        // What if drone is already flying?
        // What if drone loses connection or GPS signal?
        // What if remote controller is disconnected?
        // What if drone is already at the location?
        // What if the operator takes control of the aircraft?

        // If drone is not flying, then takeoff
        if (stateData.isFlying!=true){
            controller.startTakeOff()
        }

        // compute distance to target location using lat and lon
        var waypoint = getNewDirection()
        var waypointID = telemService.nextWaypointID
        sendAutonomyStatus("waypoint-reached")
        val orbitRadius = 10.0
        // Check to see that advanced virtual stick is enabled
        controller.ensureAdvancedVirtualStickMode()

        while (safetyChecks()) {
            // Handle logic for action execution
            if (decisionChecks()) {
                Log.v("PachKeyManagerHIPPO", "Going to Waypoint: $waypoint")
                go2LocationForward(
                    waypoint.lat,
                    waypoint.lon,
                    waypoint.alt)
            } else if (telemService.isAlertAction) {
                // Alert action stops the aircraft's movement
                telemService.isAlertAction = false
                Log.v("PachKeyManagerHIPPO", "Alert Action")
                sendAutonomyStatus("AlertedOperator")
                break
            } else if (telemService.isGatherAction) {
                // Send Gather Confirmation
                Log.v("PachKeyManagerHIPPO", "Gather Action")
                sendAutonomyStatus("GatheringInfo")
                diveAndYaw(waypoint.alt-10, 30.0)
//                flyOrbitPath(
//                    Coordinate(stateData.latitude!!, stateData.longitude!!,stateData.altitude!!),
//                    orbitRadius)
                Log.v("PackKeyManagerHIPPO", "Gathering Complete")
            }
             else {
                Log.v("PachKeyManagerHIPPO", "Unknown Decision Check Failed")
                break
            }

            // Handle logic for updating waypoint
            if (telemService.isGatherAction){
                telemService.isGatherAction = false
                Log.v("PachKeyManagerHIPPO", "Continuing to Waypoint")
            }
            else if (telemService.isAlertAction){
                telemService.isAlertAction = false
                Log.v("PachKeyManagerHIPPO", "Alerted Operator")
                break
            }
            else {
                if (telemService.plannerAction == "stay" && telemService.isStayAction){
                    Log.v("PachKeyManagerHIPPO", "Staying at Waypoint: $waypoint")
//                    sendAutonomyStatus("waypoint-reached")
                    delay(telemService.dwellTime.toLong())
                    telemService.isStayAction = false // reset flag to false so stay action is not taken
                }
                else if (waypointID != telemService.nextWaypointID) {
                    sendAutonomyStatus("waypoint-reached")
                    waypoint = getNewDirection()
                    waypointID = telemService.nextWaypointID
                    Log.v("PachKeyManagerHIPPO", "Waypoint Updated: ID$waypointID with action ${telemService.plannerAction}")
//                    delay(100L)
                } else {
                    delay(100L)
                    Log.v("PachKeyManagerHIPPO", "Waypoint Not Updated")
                }
            }
        }
        controller.endVirtualStick()
    }

    private fun getNewDirection(): Coordinate {
        // Function will update flight parameters based on the external information
        // Copy next waypoint variable to new variable
        val newAlt : Double = adjustAltForTerrain(telemService.nextWaypoint.alt)
        val waypoint = Coordinate(
            telemService.nextWaypoint.lat,
            telemService.nextWaypoint.lon,
            newAlt
        )
        if (telemService.plannerAction == "stay"){
            telemService.isStayAction = true
        }
        pidController.maxVelocity = telemService.maxVelocity
        return waypoint
    }

    private suspend fun followWaypoints(wpList: List<Coordinate>){
        // When called, this function will make the aircraft follow a list of waypoints
        // Figure out if the latest state is given

        // If drone is not flying, then takeoff
        if (stateData.isFlying!=true){
            controller.startTakeOff()
        }

        // Check to see that advanced virtual stick is enabled
        controller.ensureAdvancedVirtualStickMode()

        for (wp in wpList){
            if (safetyChecks()) {
                go2LocationForward(wp.lat, wp.lon, wp.alt)

            } else{
                Log.v("SafetyChecks", "Safety Check Failed")
                break
            }
        }

        controller.endVirtualStick()
    }

    suspend fun flyOrbitPath(center:Coordinate, radius:Double=10.0) {
        // When called, this function will make the aircraft fly in a circle around a point

        // Check to see that advanced virtual stick is enabled
        controller.ensureAdvancedVirtualStickMode()

        // Create a list of points that are evenly spaced around the circle
        val numPoints = 5
        val circlePoints = Array(numPoints) { Coordinate(0.0, 0.0, 0.0) }
        for (i in 1..numPoints) {
            val numDegrees = 360.0/numPoints*i
            // Compute new lat/lon coordinates with 111,111m per degree assumption
            val dLat = radius * cos(Math.toRadians((numDegrees)))/(111111) + center.lat
            val dLon = radius * sin(Math.toRadians(numDegrees))/(111111* cos(Math.toRadians(center.lat)))+ center.lon
            //TODO: Configure altitude to account for multiple possible terrains.
            val z = center.alt
            circlePoints[i - 1] = Coordinate(dLat,dLon, z)
        }
        Log.v("PachKeyManager", "Circle Points: $circlePoints")
        // Fly the orbit
        for (i in 1..numPoints) {
//            val angle =
            val yawAngle = 360.0/numPoints*i - 180.0
//            val yawAngle = if (angle<180) {
//                angle+180
//            } else{
//                angle-180
//            }
            val wp = circlePoints[i - 1]
            if (!telemService.isAlertAction) {
                Log.v("PachKeyManager", "NextWaypoint: $wp")
                goToLocationFixedYaw(wp.lat, wp.lon, wp.alt, yawAngle)
            } else {
                Log.v("PachKeyManager", "Alerted Operator")
                break
            }
        }
    }

    suspend fun diveAndYaw(alt: Double, yawDiff: Double) {
        // Function will make the aircraft descend to a certain altitude and change yaw angles
        // Check to see that advanced virtual stick is enabled
        controller.ensureAdvancedVirtualStickMode()
        Log.v("PachKeyManager", "Diving to $alt")
        goToAltitude(alt)
        // Controller yaw angle has a range of [-180, 180] with 0 at North. Positive is CW
        val yawL = if (stateData.yaw!! - yawDiff<-180.0) {
            stateData.yaw!! - yawDiff + 360.0
        } else {
            stateData.yaw!! - yawDiff
        }
        val yawR = if (stateData.yaw!! + yawDiff > 180.0) {
            stateData.yaw!! + yawDiff - 360.0
        } else {
            stateData.yaw!! + yawDiff
        }
        Log.v("PachKeyManager", "Yawing Left to $yawL")
        goToYawAngle(yawL)
        delay(500L)
        Log.v("PachKeyManager", "Yawing Right to $yawR")
        goToYawAngle(yawR)
        delay(500L)
    }

    // compute distance to target location using lat and lon
    private fun computeLatLonDistance(lat1 : Double, lon1: Double): Double {
        // generally used geo measurement function
        val lat2 = stateData.latitude!!
        val lon2 = stateData.longitude!!
        val dLat = lat2 * Math.PI / 180.0 - lat1 * Math.PI / 180.0;
        val dLon = lon2 * Math.PI / 180.0 - lon1 * Math.PI / 180.0
        val a = sin(dLat/2) * sin(dLat/2) +
                cos(lat1 * Math.PI / 180) * cos(lat2 * Math.PI / 180) *
                Math.sin(dLon/2) * Math.sin(dLon/2)
        val d = 2.0 * atan2(Math.sqrt(a), sqrt(1-a)) * R * 1000.0
        return d // meters
    }

    private fun computeLatDistance(lat1 : Double) : Double{
        // Computes distance in meters between current latitude and target latitude
        val lat2 = stateData.latitude!!
        val dLat = lat2 * Math.PI / 180.0 - lat1 * Math.PI / 180.0;
        val a = sin(dLat/2) * sin(dLat/2)
        val d =  2.0 * atan2(Math.sqrt(a), sqrt(1-a))* R * 1000.0
        return if (lat2<lat1) {
            -d
        } else {
            d
        }
    }

    private fun computeLonDistance(lon1: Double) : Double{
        // Computes distance in meters between current longitude and target longitude.
        // Assumes a constant latitude taken as the current aircraft position
        // Use East as positive direction
        val lat2 = stateData.latitude!!
        val lon2 = stateData.longitude!!
        val dLon = lon2 * Math.PI / 180.0 - lon1 * Math.PI / 180.0
        val a = cos(lat2 * Math.PI / 180) * cos(lat2 * Math.PI / 180) *
                Math.sin(dLon/2) * Math.sin(dLon/2)
        val d = 2.0 * atan2(Math.sqrt(a), sqrt(1-a)) * R * 1000.0
        return if (lon2>lon1) {
            -d
        } else{
            d
        }
    }

    private fun computeYawAngle(lat: Double, lon: Double)
            : Double {
        // Computes yaw angle to target location to keep aircraft facing forward
        val yDiff = lat - stateData.latitude!!
        val xDiff = lon - stateData.longitude!!
        val res = atan2(yDiff, xDiff) *(180 / PI)
        if ((yDiff<0.0) && (xDiff<0.0)) {
            return -(270.0+res)
        }else{
            return 90-res
        }
    }
}

// Router
// dataProcessor
// new PachKeyManager().registerKey(BATTERY_KEY, dataProcessor)
// dataProcessor.subscribe()