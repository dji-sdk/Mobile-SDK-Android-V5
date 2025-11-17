package dji.sampleV5.aircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.aircraft.data.DJIToastResult
import dji.sampleV5.aircraft.util.Util
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.flysafe.FlySafeDatabaseComponent
import dji.v5.manager.aircraft.flysafe.FlySafeDatabaseListener
import dji.v5.manager.aircraft.flysafe.FlySafeDatabaseState
import dji.v5.manager.aircraft.flysafe.FlySafeDatabaseUpgradeMode
import dji.v5.manager.aircraft.flysafe.FlySafeNotificationListener
import dji.v5.manager.aircraft.flysafe.FlyZoneManager
import dji.v5.manager.aircraft.flysafe.info.FlySafeDatabaseInfo
import dji.v5.manager.aircraft.flysafe.info.FlySafeReturnToHomeInformation
import dji.v5.manager.aircraft.flysafe.info.FlySafeSeriousWarningInformation
import dji.v5.manager.aircraft.flysafe.info.FlySafeTipInformation
import dji.v5.manager.aircraft.flysafe.info.FlySafeWarningInformation
import dji.v5.manager.aircraft.flysafe.info.FlyZoneInformation
import dji.v5.manager.aircraft.flysafe.info.FlyZoneLicenseInfo
import dji.v5.utils.common.ContextUtil
import dji.v5.utils.common.FileUtils
import dji.v5.utils.common.LogUtils
import java.io.File
import java.text.SimpleDateFormat
import kotlin.math.roundToInt

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/8/12
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class FlySafeVM : DJIViewModel() {

    val flySafeWarningInformation = MutableLiveData<FlySafeWarningInformation>()
    val flySafeSeriousWarningInformation = MutableLiveData<FlySafeSeriousWarningInformation>()
    val flySafeReturnToHomeInformation = MutableLiveData<FlySafeReturnToHomeInformation>()
    val flySafeTipInformation = MutableLiveData<FlySafeTipInformation>()
    val flyZoneInformation = MutableLiveData<MutableList<FlyZoneInformation>>()
    val serverFlyZoneLicenseInfo = MutableLiveData<MutableList<FlyZoneLicenseInfo>>()
    val aircraftFlyZoneLicenseInfo = MutableLiveData<MutableList<FlyZoneLicenseInfo>>()

    val importAndSyncState = MutableLiveData<ImportAndSyncState>()
    val dataBaseInfo = MutableLiveData<DataBaseInfo>()
    val dataUpgradeState = MutableLiveData<FlySafeDatabaseState>()

    private val availableTestFlySafeDynamicDatabaseName = "de.geojson"
    private val unAvailableTestFlySafeDynamicDatabaseName = "France.json"

    private val flySafeNotificationListener = object : FlySafeNotificationListener {

        override fun onWarningNotificationUpdate(info: FlySafeWarningInformation) {
            flySafeWarningInformation.postValue(info)
        }

        override fun onSeriousWarningNotificationUpdate(info: FlySafeSeriousWarningInformation) {
            flySafeSeriousWarningInformation.postValue(info)
        }

        override fun onReturnToHomeNotificationUpdate(info: FlySafeReturnToHomeInformation) {
            flySafeReturnToHomeInformation.postValue(info)
        }

        override fun onTipNotificationUpdate(info: FlySafeTipInformation) {
            flySafeTipInformation.postValue(info)
        }

        override fun onSurroundingFlyZonesUpdate(infos: MutableList<FlyZoneInformation>) {
            flyZoneInformation.postValue(infos)
        }
    }

    fun initListener() {
        FlyZoneManager.getInstance().addFlySafeNotificationListener(flySafeNotificationListener)
        addFlySafeDatabaseListener()

    }

    override fun onCleared() {
        KeyManager.getInstance().cancelListen(this)
        FlyZoneManager.getInstance().removeFlySafeNotificationListener(flySafeNotificationListener)
        removeFlySafeDatabaseListener()
    }

    fun getAircraftLocation(): LocationCoordinate3D = FlightControllerKey.KeyAircraftLocation3D.create().get(LocationCoordinate3D(0.0, 0.0,0.0))

    fun getFlyZonesInSurroundingArea(location: LocationCoordinate2D) {
        FlyZoneManager.getInstance().getFlyZonesInSurroundingArea(location, object :
            CommonCallbacks.CompletionCallbackWithParam<MutableList<FlyZoneInformation>> {

            override fun onSuccess(infos: MutableList<FlyZoneInformation>?) {
                toastResult?.postValue(DJIToastResult.success())
                flyZoneInformation.postValue(infos ?: arrayListOf())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun downloadFlyZoneLicensesFromServer() {
        FlyZoneManager.getInstance().downloadFlyZoneLicensesFromServer(object :
            CommonCallbacks.CompletionCallbackWithParam<MutableList<FlyZoneLicenseInfo>> {

            override fun onSuccess(infos: MutableList<FlyZoneLicenseInfo>?) {
                toastResult?.postValue(DJIToastResult.success())
                serverFlyZoneLicenseInfo.postValue(infos ?: arrayListOf())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun pushFlyZoneLicensesToAircraft() {
        FlyZoneManager.getInstance().pushFlyZoneLicensesToAircraft(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                pullFlyZoneLicensesFromAircraft()
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun pullFlyZoneLicensesFromAircraft() {
        FlyZoneManager.getInstance().pullFlyZoneLicensesFromAircraft(object :
            CommonCallbacks.CompletionCallbackWithParam<MutableList<FlyZoneLicenseInfo>> {

            override fun onSuccess(infos: MutableList<FlyZoneLicenseInfo>?) {
                toastResult?.postValue(DJIToastResult.success())

                aircraftFlyZoneLicenseInfo.postValue(infos ?: arrayListOf())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun deleteFlyZoneLicensesFromAircraft() {
        FlyZoneManager.getInstance().deleteFlyZoneLicensesFromAircraft(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun setFlyZoneLicensesEnabled(info: FlyZoneLicenseInfo, isEnable: Boolean) {
        FlyZoneManager.getInstance().setFlyZoneLicensesEnabled(info, isEnable, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                pullFlyZoneLicensesFromAircraft()
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun unlockAuthorizationFlyZone(flyZoneID: Int) {
        FlyZoneManager.getInstance().unlockAuthorizationFlyZone(flyZoneID, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun unlockAllEnhancedWarningFlyZone() {
        FlyZoneManager.getInstance().unlockAllEnhancedWarningFlyZone(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun pushFlySafeDynamicDatabaseToAircraftAndApp(fileName: String) {
        FlyZoneManager.getInstance().importFlySafeDynamicDatabaseToMSDK(fileName, object :
            CommonCallbacks.CompletionCallbackWithProgress<Double> {
            override fun onProgressUpdate(progress: Double?) {
                importAndSyncState.postValue(ImportAndSyncState(progress!!.toFloat().roundToInt()))
            }

            override fun onSuccess() {
                importAndSyncState.postValue(ImportAndSyncState(100))
            }

            override fun onFailure(error: IDJIError) {
                importAndSyncState.postValue(ImportAndSyncState(-1, error))
            }

        })
    }

    fun syncFlySafeMSDKDatabaseToAircraft() {
        FlyZoneManager.getInstance().pushFlySafeDynamicDatabaseToAircraft(object :
            CommonCallbacks.CompletionCallbackWithProgress<Double> {
            override fun onProgressUpdate(progress: Double?) {
                importAndSyncState.postValue(ImportAndSyncState(Math.round(progress!!.toFloat())))
            }

            override fun onSuccess() {
                importAndSyncState.postValue(ImportAndSyncState(100))
            }

            override fun onFailure(error: IDJIError) {
                importAndSyncState.postValue(ImportAndSyncState(-1, error))
            }

        })
    }

    fun setFlySafeDynamicDatabaseUpgradeMode(flySafeDynamicDatabaseUpgradeMode: FlySafeDatabaseUpgradeMode) {
        FlyZoneManager.getInstance().setFlySafeDynamicDatabaseUpgradeMode(flySafeDynamicDatabaseUpgradeMode, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                importAndSyncState.postValue(ImportAndSyncState(100))
            }

            override fun onFailure(error: IDJIError) {
                importAndSyncState.postValue(ImportAndSyncState(-1, error))
            }

        })
    }

    fun addFlySafeDatabaseListener() {
        FlyZoneManager.getInstance().addFlySafeDatabaseListener(object : FlySafeDatabaseListener {
            override fun onFlySafeDatabaseInfoUpdate(flySafeDatabaseInfo: FlySafeDatabaseInfo) {
                LogUtils.i("testFly", "dataName :" + flySafeDatabaseInfo.databaseName + " compnent :" + flySafeDatabaseInfo.component)
                dataBaseInfo.postValue(
                    DataBaseInfo(
                        flySafeDatabaseInfo.databaseName,
                        formatCEDBTime(flySafeDatabaseInfo.databaseTimeStamp * 1000),
                        Util.byte2AdaptiveUnitStrDefault(flySafeDatabaseInfo.databaseSize),
                        flySafeDatabaseInfo.component, flySafeDatabaseInfo.flySafeDatabaseUpgradeMode
                    )
                )
            }

            override fun onFlySafeDatabaseStateUpdate(flySafeDatabaseState: FlySafeDatabaseState) {
                dataUpgradeState.postValue(flySafeDatabaseState)
            }

        })
    }

    fun pushAvailableTestFlySafeDynamicDatabaseToApp() {
        val file = File(ContextUtil.getContext().getExternalFilesDir("/"), availableTestFlySafeDynamicDatabaseName)
        FileUtils.copyAssetsFileIfNeed(ContextUtil.getContext(), "flysafe/$availableTestFlySafeDynamicDatabaseName", file)
        pushFlySafeDynamicDatabaseToAircraftAndApp(file.path)
    }

    fun pushUnAvailableTestFlySafeDynamicDatabaseToApp() {
        val file = File(ContextUtil.getContext().getExternalFilesDir("/"), unAvailableTestFlySafeDynamicDatabaseName)
        FileUtils.copyAssetsFileIfNeed(ContextUtil.getContext(), "flysafe/$unAvailableTestFlySafeDynamicDatabaseName", file)
        pushFlySafeDynamicDatabaseToAircraftAndApp(file.path)
    }

    private fun formatCEDBTime(timestamp: Long): String {
        val format = SimpleDateFormat("yyyy/MM/dd")
        return format.format(timestamp)
    }

    private fun removeFlySafeDatabaseListener() {
        FlyZoneManager.getInstance().clearAllFlySafeDatabaseListener()
    }

    data class DataBaseInfo(
        var dataBaseName: String,
        var dataBaseTime: String,
        var dataBaseSize: String,
        var component: FlySafeDatabaseComponent,
        var upgradeMode: FlySafeDatabaseUpgradeMode
    )

    data class ImportAndSyncState(
        var importAndSyncProgress: Int,
        var error: IDJIError? = null,
    )
}