package dji.sampleV5.aircraft.models

import android.R
import android.content.Context
import android.widget.ArrayAdapter
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dji.sampleV5.aircraft.data.FlightControlState
import dji.sampleV5.aircraft.data.MissionUploadStateInfo
import dji.sdk.keyvalue.key.DJIKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.sdk.keyvalue.value.common.Velocity3D
import dji.sdk.wpmz.value.mission.WaylineExitOnRCLostAction
import dji.sdk.wpmz.value.mission.WaylineFinishedAction
import dji.sdk.keyvalue.value.flightcontroller.FlightMode
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.utils.RxUtil
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.waypoint3.WaylineExecutingInfoListener
import dji.v5.manager.aircraft.waypoint3.WaypointActionListener
import dji.v5.manager.aircraft.waypoint3.WaypointMissionExecuteStateListener
import dji.v5.manager.aircraft.waypoint3.WaypointMissionManager
import dji.v5.manager.aircraft.waypoint3.model.BreakPointInfo
import dji.v5.manager.areacode.AreaCode
import dji.v5.manager.areacode.AreaCodeManager
import dji.v5.utils.common.ContextUtil
import dji.v5.utils.common.DjiSharedPreferencesManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable


/**
 * @author feel.feng
 * @time 2022/02/27 10:10 上午
 * @description:
 */
class WayPointV3VM : DJIViewModel() {
    val RadToDeg = 57.295779513082321
    val missionUploadState = MutableLiveData<MissionUploadStateInfo>()

    val flightControlState = MutableLiveData<FlightControlState>()
    var compassHeadKey: DJIKey<Double> = FlightControllerKey.KeyCompassHeading.create()
    var altitudeKey: DJIKey<Double> = FlightControllerKey.KeyAltitude.create()
    var flightSpeed: DJIKey<Velocity3D> = FlightControllerKey.KeyAircraftVelocity.create()
    var flightMode : DJIKey<FlightMode> = FlightControllerKey.KeyFlightMode.create()


    fun pushKMZFileToAircraft(missionPath: String) {
        WaypointMissionManager.getInstance().pushKMZFileToAircraft(missionPath, object :
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

    fun startMission(
        missionId: String,
        waylineIDs: List<Int>,
        callback: CommonCallbacks.CompletionCallback
    ) {
        WaypointMissionManager.getInstance().startMission(missionId, waylineIDs, callback)
    }

    fun startMission(
        missionId: String,
        breakPointInfo: BreakPointInfo,
        callback: CommonCallbacks.CompletionCallback
    ) {
        WaypointMissionManager.getInstance().startMission(missionId, breakPointInfo, callback)
    }

    fun pauseMission(callback: CommonCallbacks.CompletionCallback) {
        WaypointMissionManager.getInstance().pauseMission(callback)
    }

    fun resumeMission(callback: CommonCallbacks.CompletionCallback) {
        WaypointMissionManager.getInstance().resumeMission(callback)
    }

    fun resumeMission(
        breakPointInfo: BreakPointInfo,
        callback: CommonCallbacks.CompletionCallback
    ) {
        WaypointMissionManager.getInstance().resumeMission(breakPointInfo, callback)
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

    fun addWaypointActionListener(listener: WaypointActionListener) {
        WaypointMissionManager.getInstance().addWaypointActionListener(listener)
    }

    fun clearAllWaypointActionListener() {
        WaypointMissionManager.getInstance().clearAllWaypointActionListener()
    }

    fun listenFlightControlState(): Disposable {
        return Flowable.combineLatest(
            RxUtil
                .addListener(KeyTools.createKey(FlightControllerKey.KeyHomeLocation), this)
                .observeOn(AndroidSchedulers.mainThread()),
            RxUtil
                .addListener(KeyTools.createKey(FlightControllerKey.KeyAircraftLocation), this)
                .observeOn(AndroidSchedulers.mainThread())
        ) { homelocation: LocationCoordinate2D?, aircraftLocation: LocationCoordinate2D? ->
            if (homelocation == null || aircraftLocation == null) {
                return@combineLatest
            }
            val height = getHeight()
            val distance = calculateDistance(
                homelocation.latitude,
                homelocation.longitude,
                aircraftLocation.latitude,
                aircraftLocation.longitude
            )
            val heading = getHeading()
            val speed3D = getSpeed()
            val x = speed3D.getX()
            val y = speed3D.getY()
            val speed = Math.sqrt((x * x + y * y))
            flightControlState.value = FlightControlState(
                aircraftLocation.longitude,
                aircraftLocation.latitude,
                distance = distance,
                height = height,
                head = heading,
                speed = speed,
                homeLocation = homelocation
            )
            refreshFlightControlState()
        }.subscribe()
    }

    fun isLocationValid(latitude: Double, longitude: Double): Boolean {
        return latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180 && latitude != 0.0 && longitude != 0.0
    }

    fun cancelListenFlightControlState() {
        KeyManager.getInstance().cancelListen(this)
    }

    fun getAvailableWaylineIDs(missionPath: String): List<Int> {
        return WaypointMissionManager.getInstance().getAvailableWaylineIDs(missionPath)
    }

    private fun refreshFlightControlState() {
        flightControlState.postValue(flightControlState.value)
    }


    fun calculateDistance(
        latA: Double,
        lngA: Double,
        latB: Double,
        lngB: Double,
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

    private fun getHeading() = (compassHeadKey.get(0.0)).toFloat()

    private fun getHeight(): Double = (altitudeKey.get(0.0))

    private fun getSpeed(): Velocity3D = flightSpeed.get(Velocity3D(0.0, 0.0, 0.0))

    fun getFlightMode() :FlightMode = flightMode.get(FlightMode.UNKNOWN)

    fun isInMainlandChina(): Boolean {
        return AreaCodeManager.getInstance().areaCode.areaCodeEnum == AreaCode.CHINA
    }

    fun isMacau(): Boolean {
        return AreaCodeManager.getInstance().areaCode.areaCodeEnum == AreaCode.MACAU
    }

    fun isHongKong(): Boolean {
        return AreaCodeManager.getInstance().areaCode.areaCodeEnum == AreaCode.HONG_KONG

    }

    fun getMapType(context: Context?): Int {
        return DjiSharedPreferencesManager.getInt(context, "map_selection", 0)
    }

    fun saveMapType(context: Context?, pos: Int) {
        DjiSharedPreferencesManager.putInt(context, "map_selection", pos)
    }

    fun getMapSpinnerAdapter(): ArrayAdapter<String> {
        return if (isGoogleMapsSupported()) {
            ArrayAdapter<String>(
                ContextUtil.getContext(),
                R.layout.simple_spinner_dropdown_item,
                ContextUtil.getContext().resources.getStringArray(
                    dji.sampleV5.aircraft.R.array.maps_array_all
                )
            )
        } else {
            ArrayAdapter<String>(
                ContextUtil.getContext(),
                R.layout.simple_spinner_dropdown_item,
                ContextUtil.getContext().resources.getStringArray(
                    dji.sampleV5.aircraft.R.array.maps_array
                )
            )
        }
    }

    fun isGoogleMapsSupported(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode =
            googleApiAvailability.isGooglePlayServicesAvailable(ContextUtil.getContext())
        return resultCode == ConnectionResult.SUCCESS
    }

}
data class MissionGlobalModel( var globalSpeed:Double = 5.0,
                               var finishAction: WaylineFinishedAction = WaylineFinishedAction.GO_HOME,
                                var lostAction: WaylineExitOnRCLostAction = WaylineExitOnRCLostAction.GO_BACK) {
    companion object {
        fun transStringToFinishAction(textStr: String): WaylineFinishedAction {
            WaylineFinishedAction.values().forEach {
                if (it.name == textStr) {
                    return it
                }
            }
            return WaylineFinishedAction.UNKNOWN
        }

        fun transStringToLostAction(textStr: String): WaylineExitOnRCLostAction {
            WaylineExitOnRCLostAction.values().forEach {
                if (it.name == textStr) {
                    return it
                }
            }
            return WaylineExitOnRCLostAction.UNKNOWN
        }

        fun transFinishActionToIndex(finishAction: WaylineFinishedAction): Int {
            return finishAction.value()
        }

    }

}