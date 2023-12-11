package dji.v5.ux.core.widget.gpssignal

import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.key.RtkMobileStationKey
import dji.sdk.keyvalue.value.flightcontroller.GPSSignalLevel
import dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource
import dji.sdk.keyvalue.value.rtkbasestation.RTKServiceState
import dji.sdk.keyvalue.value.rtkbasestation.RTKStationConnetState
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.et.create
import dji.v5.et.listen
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.rtk.RTKSystemState
import dji.v5.manager.aircraft.rtk.RTKSystemStateListener
import dji.v5.manager.aircraft.rtk.network.INetworkServiceInfoListener
import dji.v5.manager.aircraft.rtk.station.RTKStationConnectStatusListener
import dji.v5.manager.interfaces.IRTKCenter
import dji.v5.utils.common.LogUtils
import dji.v5.ux.accessory.RTKStartServiceHelper
import dji.v5.ux.accessory.RTKStartServiceHelper.isNetworkRTK
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.Flowable

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2022/9/8
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */

class GpsSignalWidgetModel(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore,
    val rtkCenter: IRTKCenter,
) : WidgetModel(djiSdkModel, keyedStore) {
    private var rtkSystemState = RTKSystemState()
    private var isRtkModuleAvailable = false
    private var rtkServiceState = RTKServiceState.UNKNOWN
    private var currentGpsSignalLevel: GPSSignalLevel = GPSSignalLevel.UNKNOWN

    private var baseStationConnectStatus = RTKStationConnetState.UNKNOWN

    // 飞机是否正在使用精度保持功能
    private var isUsingRtkKeeping = false

    // GNSS 卫星数量
    private val gpsSatelliteCountProcessor = DataProcessor.create(0)

    //RTK卫星数
    private val rtkSatelliteCountProcessor = DataProcessor.create(0)

    //GPS信号强度
    private val gpsSignalLevelProcessor = DataProcessor.create(SignalLevel.LEVEL_1)

    //RTK信息封装类
    private val rtkOverviewProcessor = DataProcessor.create(RtkOverview())
    private val rtkSystemStateListener = RTKSystemStateListener {
        rtkSystemState = it
        updateRtkOverview()
        rtkSatelliteCountProcessor.onNext(getRtkSatelliteCount(it))
        updateRTKListener(it.rtkReferenceStationSource)
    }
    private val connectStatusListener = RTKStationConnectStatusListener {
        baseStationConnectStatus = it
        updateRtkOverview()
    }

    private val networkServiceInfoListener: INetworkServiceInfoListener = object :
        INetworkServiceInfoListener {
        override fun onServiceStateUpdate(state: RTKServiceState?) {
            state?.let {
                if (rtkServiceState != state) {
                    rtkServiceState = state
                    updateRtkOverview()
                }
            }

        }

        override fun onErrorCodeUpdate(error: IDJIError?) {
            LogUtils.e(tag, "networkServiceInfoListener onErrorCodeUpdate:$error")
        }
    }


    val gpsSatelliteCount: Flowable<Int>
        get() = gpsSatelliteCountProcessor.toFlowable()

    val rtkSatelliteCount: Flowable<Int>
        get() = rtkSatelliteCountProcessor.toFlowable()

    val rtkOverview: Flowable<RtkOverview>
        get() = rtkOverviewProcessor.toFlowable()

    val gpsSignalLevel: Flowable<SignalLevel>
        get() = gpsSignalLevelProcessor.toFlowable()


    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyGPSSatelliteCount), gpsSatelliteCountProcessor)
        rtkCenter.addRTKSystemStateListener(rtkSystemStateListener)
        rtkCenter.rtkStationManager.addRTKStationConnectStatusListener(connectStatusListener)
        rtkCenter.qxrtkManager.addNetworkRTKServiceInfoListener(networkServiceInfoListener)
        rtkCenter.customRTKManager.addNetworkRTKServiceInfoListener(networkServiceInfoListener)
        rtkCenter.cmccrtkManager.addNetworkRTKServiceInfoListener(networkServiceInfoListener)
        FlightControllerKey.KeyGPSSignalLevel.create().listen(this) {
            it?.let {
                currentGpsSignalLevel = it
                updateSignalLevel(it)
            }
        }
        FlightControllerKey.KeyConnection.create().listen(this) {
            //飞机断连则重置数据
            if (it == false) {
                resetData()
                updateRtkOverview()
                rtkSatelliteCountProcessor.onNext(0)
                gpsSatelliteCountProcessor.onNext(0)
            }

        }
        RTKStartServiceHelper.rtkModuleAvailable.subscribe {
            isRtkModuleAvailable = it
            updateRtkOverview()
        }
        // 飞机是否正在使用精度保持功能
        RtkMobileStationKey.KeyRTKkeepStatus.create().listen(this) {
            if (it == null) {
                return@listen
            }
            isUsingRtkKeeping = it
            updateRtkOverview()
        }
    }

    override fun inCleanup() {
        rtkCenter.removeRTKSystemStateListener(rtkSystemStateListener)
        rtkCenter.qxrtkManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
        rtkCenter.customRTKManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
        rtkCenter.cmccrtkManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
        rtkCenter.rtkStationManager.removeRTKStationConnectStatusListener(connectStatusListener)
        KeyManager.getInstance().cancelListen(this)

    }

    fun setRTKEnable(boolean: Boolean) {
        rtkCenter.setAircraftRTKModuleEnabled(boolean, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                //发现RTK精度保持功能关闭，则一起开启
                if (!rtkSystemState.rtkMaintainAccuracyEnabled && boolean) {
                    rtkCenter.setRTKMaintainAccuracyEnabled(true, null)
                }
                updateSignalLevel(currentGpsSignalLevel)
            }

            override fun onFailure(djiError: IDJIError) {
                //do nothing
            }
        })

    }

    private fun updateRTKListener(rtkSource: RTKReferenceStationSource) {
        when (rtkSource) {
            RTKReferenceStationSource.QX_NETWORK_SERVICE -> {
                rtkCenter.customRTKManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
                rtkCenter.cmccrtkManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
                rtkCenter.rtkStationManager.removeRTKStationConnectStatusListener(connectStatusListener)
                rtkCenter.qxrtkManager.addNetworkRTKServiceInfoListener(networkServiceInfoListener)
            }
            RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE -> {
                rtkCenter.qxrtkManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
                rtkCenter.cmccrtkManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
                rtkCenter.rtkStationManager.removeRTKStationConnectStatusListener(connectStatusListener)
                rtkCenter.customRTKManager.addNetworkRTKServiceInfoListener(networkServiceInfoListener)

            }
            RTKReferenceStationSource.NTRIP_NETWORK_SERVICE -> {
                rtkCenter.qxrtkManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
                rtkCenter.customRTKManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
                rtkCenter.rtkStationManager.removeRTKStationConnectStatusListener(connectStatusListener)
                rtkCenter.cmccrtkManager.addNetworkRTKServiceInfoListener(networkServiceInfoListener)
            }
            RTKReferenceStationSource.BASE_STATION -> {
                rtkCenter.qxrtkManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
                rtkCenter.customRTKManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
                rtkCenter.cmccrtkManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
                rtkCenter.rtkStationManager.addRTKStationConnectStatusListener(connectStatusListener)

            }
            else -> {
                //do nothing
            }
        }

    }

    /**
     * 更新RTK概览
     */
    private fun updateRtkOverview() {
        var currentRtkState = RtkState.NOT_CONNECT
        rtkSystemState.run {
            if (isRTKEnabled) {
                when {
                    rtkReferenceStationSource == RTKReferenceStationSource.NONE -> {
                        //无
                        currentRtkState = RtkState.NOT_CONNECT
                    }
                    rtkReferenceStationSource == RTKReferenceStationSource.BASE_STATION -> {
                        //base rtk
                        currentRtkState = updateBaseRtkState()
                    }
                    isNetworkRTK(rtkReferenceStationSource) -> {
                        //网络RTK
                        currentRtkState = updateRtcmStatus()
                    }
                }
            } else {
                currentRtkState = RtkState.NOT_OPEN
            }

            val rtkOverview = RtkOverview(
                connected = isRtkModuleAvailable,
                enabled = isRTKEnabled,
                rtkHealthy = rtkHealthy,
                rtkState = currentRtkState,
                rtkSource = rtkReferenceStationSource,
                rtkKeepingStatus = isUsingRtkKeeping
            )
            rtkOverviewProcessor.onNext(rtkOverview)
            LogUtils.d(tag, "rtkOverview=$rtkOverview")
        }

    }

    private fun resetData() {
        baseStationConnectStatus = RTKStationConnetState.UNKNOWN
        rtkSystemState = RTKSystemState()
        isRtkModuleAvailable = false
        rtkServiceState = RTKServiceState.UNKNOWN

    }

    private fun updateBaseRtkState(): RtkState {
        return if (baseStationConnectStatus == RTKStationConnetState.CONNECTED) {
            if (rtkSystemState.rtkHealthy) {
                RtkState.CONNECTED
            } else {
                RtkState.CONVERGING
            }
        } else {
            RtkState.NOT_CONNECT
        }
    }

    /**
     * 更新GPS信号强度LiveData
     */
    private fun updateSignalLevel(gpsSignalLevel: GPSSignalLevel) {
        gpsSignalLevelProcessor.onNext(
            when (gpsSignalLevel) {
                GPSSignalLevel.LEVEL_10,
                GPSSignalLevel.LEVEL_5,
                GPSSignalLevel.LEVEL_4,
                ->
                    SignalLevel.LEVEL_3
                GPSSignalLevel.LEVEL_3 ->
                    SignalLevel.LEVEL_2
                else ->
                    SignalLevel.LEVEL_1
            }
        )
    }


    private fun updateRtcmStatus(): RtkState {
        return when (rtkServiceState) {
            RTKServiceState.RTCM_CONNECTED,
            RTKServiceState.RTCM_NORMAL,
            RTKServiceState.RTCM_USER_HAS_ACTIVATE,
            RTKServiceState.RTCM_USER_ACCOUNT_EXPIRES_SOON,
            RTKServiceState.RTCM_USE_DEFAULT_MOUNT_POINT,
            RTKServiceState.TRANSMITTING,
            -> {
                if (rtkSystemState.rtkHealthy) {
                    RtkState.CONNECTED
                } else {
                    RtkState.CONVERGING
                }
            }
            RTKServiceState.RTCM_AUTH_FAILED,
            RTKServiceState.RTCM_USER_NOT_BOUNDED,
            RTKServiceState.RTCM_USER_NOT_ACTIVATED,
            RTKServiceState.SERVICE_SUSPENSION,
            RTKServiceState.ACCOUNT_EXPIRED,
            RTKServiceState.RTCM_ILLEGAL_UTC_TIME,
            RTKServiceState.NETWORK_NOT_REACHABLE,
            RTKServiceState.RTCM_SET_COORDINATE_FAILURE,
            RTKServiceState.LOGIN_FAILURE,
            RTKServiceState.ACCOUNT_ERROR,
            RTKServiceState.CONNECTING,
            RTKServiceState.INVALID_REQUEST,
            RTKServiceState.SERVER_NOT_REACHABLE,
            RTKServiceState.UNKNOWN,
            -> {
                RtkState.ERROR
            }
            else -> {
                RtkState.NOT_CONNECT
            }
        }
    }

    /**
     * 计算RTK的星数。只能只取天线1（主天线）的卫星数作为标准，因为飞控大多数情况也只使用主天线的卫星数来计算。
     */
    private fun getRtkSatelliteCount(rtkSystemState: RTKSystemState): Int {
        return if (!rtkSystemState.isRTKEnabled) {
            0
        } else {
            rtkSystemState.run {
                satelliteInfo?.run {
                    mobileStationReceiver1Info.map {
                        it.count
                    }.sum()
                } ?: 0
            }

        }
    }


    /**
     * RTK概览状态
     */
    data class RtkOverview(
        val connected: Boolean = false,
        val enabled: Boolean = false,
        val rtkHealthy: Boolean = false,
        val rtkState: RtkState = RtkState.NOT_OPEN,
        val rtkSource: RTKReferenceStationSource = RTKReferenceStationSource.NONE,
        val rtkKeepingStatus: Boolean = false,
    ) {
        override fun toString(): String {
            return "RtkOverview(connected=$connected, enabled=$enabled, rtkHealthy=$rtkHealthy, rtkState=$rtkState, rtkSource=$rtkSource, rtkKeepingStatus=$rtkKeepingStatus)"
        }
    }


    /**
     * RTK统一状态（包含D-RTK和网络RTK的状态）
     */
    enum class RtkState {
        /**
         * 未开启
         */
        NOT_OPEN,

        /**
         * 未连接
         */
        NOT_CONNECT,

        /**
         * 收敛中
         */
        CONVERGING,

        /**
         * 已连接（已使用）
         */
        CONNECTED,

        /**
         * 异常
         */
        ERROR

    }

    /**
     * 信号强度
     */
    enum class SignalLevel {
        /**
         * 信号很差，红色，飞行器无法返航
         */
        LEVEL_1,

        /**
         * 信号较差，黄色，飞行器大概知道home点，可返航
         */
        LEVEL_2,

        /**
         * 信号良好，白色，飞行能正常返航、悬停
         */
        LEVEL_3
    }

}