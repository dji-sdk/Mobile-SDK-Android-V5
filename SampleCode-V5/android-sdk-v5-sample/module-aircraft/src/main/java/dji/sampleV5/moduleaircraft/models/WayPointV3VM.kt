package dji.sampleV5.moduleaircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.moduleaircraft.data.FlightControlState
import dji.sampleV5.modulecommon.models.DJIViewModel
import dji.sampleV5.moduleaircraft.data.MissionUploadStateInfo
import dji.sdk.keyvalue.key.*
import dji.sdk.keyvalue.value.common.Attitude
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.waypoint3.WaylineExecutingInfoListener
import dji.v5.manager.aircraft.waypoint3.WaypointMissionManager
import dji.v5.manager.aircraft.waypoint3.WaypointMissionExecuteStateListener

/**
 * @author feel.feng
 * @time 2022/02/27 10:10 上午
 * @description:
 */
class WayPointV3VM : DJIViewModel() {
    val RadToDeg = 57.295779513082321
    val missionUploadState = MutableLiveData<MissionUploadStateInfo>()

    val flightControlState = MutableLiveData<FlightControlState>()
    var compassHeadKey : DJIKey<Double> = FlightControllerKey.KeyCompassHeading.create()
    var altitudeKey :DJIKey<Double> = FlightControllerKey.KeyAltitude.create()


    fun pushKMZFileToAircraft(missionId: String, missionPath: String) {
        WaypointMissionManager.getInstance().pushKMZFileToAircraft(missionId, missionPath, object :
            CommonCallbacks.CompletionCallbackWithProgress<Double> {
            override fun onProgressUpdate(progress: Double) {
                missionUploadState.value = MissionUploadStateInfo(updateProgress = progress)
                refreshMissionState()
            }

            override fun onSuccess() {
                missionUploadState.value = MissionUploadStateInfo(tips = "Mission Upload Success")
                refreshMissionState()
            }

            override fun onFailure(error: IDJIError) {
                missionUploadState.value = MissionUploadStateInfo(error = error)
                refreshMissionState()
            }

        })
    }

    private fun refreshMissionState() {
        missionUploadState.postValue(missionUploadState.value)
    }

    fun startMission(missionId: String, callback: CommonCallbacks.CompletionCallback) {
        WaypointMissionManager.getInstance().startMission(missionId, callback)
    }

    fun pauseMission(callback: CommonCallbacks.CompletionCallback) {
        WaypointMissionManager.getInstance().pauseMission(callback)
    }

    fun resumeMission(callback: CommonCallbacks.CompletionCallback) {
        WaypointMissionManager.getInstance().resumeMission(callback)
    }

    fun stopMission(missionID: String, callback: CommonCallbacks.CompletionCallback) {
        WaypointMissionManager.getInstance().stopMission(missionID, callback)
    }

    fun addMissionStateListener(listener: WaypointMissionExecuteStateListener) {
        WaypointMissionManager.getInstance().addWaypointMissionExecuteStateListener(listener)
    }

    fun removeMissionStateListener(listener: WaypointMissionExecuteStateListener) {
        WaypointMissionManager.getInstance().removeWaypointMissionExecuteStateListener(listener)
    }

    fun removeAllMissionStateListener() {
        WaypointMissionManager.getInstance().clearAllWaypointMissionExecuteStateListener()
    }

    fun addWaylineExecutingInfoListener(listener: WaylineExecutingInfoListener) {
        WaypointMissionManager.getInstance().addWaylineExecutingInfoListener(listener)
    }

    fun removeWaylineExecutingInfoListener(listener: WaylineExecutingInfoListener) {
        WaypointMissionManager.getInstance().removeWaylineExecutingInfoListener(listener)
    }

    fun clearAllWaylineExecutingInfoListener() {
        WaypointMissionManager.getInstance().clearAllWaylineExecutingInfoListener()
    }

    fun listenFlightControlState() {
        val homelocation : LocationCoordinate2D = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyHomeLocation))
        KeyManager.getInstance().listen(
            KeyTools.createKey(FlightControllerKey.KeyAircraftLocation), this
        ) { _, newValue ->
            newValue?.let {
                val height = getHeight()
                val distance = calculateDistance(homelocation.latitude , homelocation.longitude , it.latitude , it.longitude)
                val heading = getHeading()
                flightControlState.value = FlightControlState(it.longitude, it.latitude , distance = distance , height = height , head = heading)
                refreshFlightControlState()
            }
        }
    }



    fun cancelListenFlightControlState() {
        KeyManager.getInstance().cancelListen(KeyTools.createKey(FlightControllerKey.KeyAircraftLocation), this)
    }

    private fun refreshFlightControlState() {
        flightControlState.postValue(flightControlState.value)
    }

    private fun convertToAngle( mCoordinate:Double):Double = mCoordinate* RadToDeg

    fun calculateDistance(
        latA: Double,
        lngA: Double,
        latB: Double,
        lngB: Double
    ): Double {
        val earthR = 6371000.0
        val x =
            Math.cos(latA * Math.PI / 180) * Math.cos(
                latB * Math.PI / 180
            ) * Math.cos((lngA - lngB) * Math.PI / 180)
        val y =
            Math.sin(latA * Math.PI / 180) * Math.sin(
                latB * Math.PI / 180
            )
        var s = x + y
        if (s > 1) {
            s = 1.0
        }
        if (s < -1) {
            s = -1.0
        }
        val alpha = Math.acos(s)
        return alpha * earthR
    }

    private fun   getHeading() = (compassHeadKey.get()?:0.0).toFloat()

    private fun  getHeight():Double  = (altitudeKey.get()?:0.0)


}