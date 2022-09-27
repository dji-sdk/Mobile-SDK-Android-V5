package dji.v5.ux.accessory


import dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource
import dji.sdk.keyvalue.value.rtkbasestation.RTKServiceState
import dji.sdk.keyvalue.value.rtkbasestation.RTKStationConnetState
import dji.sdk.keyvalue.value.rtkmobilestation.RTKLocation
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.rtk.*
import dji.v5.manager.aircraft.rtk.network.INetworkServiceInfoListener
import dji.v5.manager.aircraft.rtk.station.RTKStationConnectStatusListener
import dji.v5.manager.interfaces.IRTKCenter
import dji.v5.utils.common.LogUtils
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.GlobalPreferenceKeys
import dji.v5.ux.core.communication.GlobalPreferencesInterface
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.communication.UXKeys
import dji.v5.ux.core.util.DataProcessor
import dji.v5.ux.core.util.UnitConversionUtil
import io.reactivex.rxjava3.core.Flowable

/**
 * Description :Widget Model for the [RTKSatelliteStatusWidget] used to define
 * the underlying logic and communication
 *
 * @author: Byte.Cai
 *  date : 2022/5/23
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */

class RTKSatelliteStatusWidgetModel(
    djiSdkModel: DJISDKModel,
    uxKeyManager: ObservableInMemoryKeyedStore,
    private val preferencesManager: GlobalPreferencesInterface?,
    private val rtkCenter: IRTKCenter,
) :
    WidgetModel(djiSdkModel, uxKeyManager) {

    private val TAG = LogUtils.getTag(this)
    private val rtkLocationInfoProcessor: DataProcessor<RTKLocationInfo> = DataProcessor.create(RTKLocationInfo())
    private val rtkSystemStateProcessor: DataProcessor<RTKSystemState> = DataProcessor.create(RTKSystemState())
    private val rtkStationConnectStateProcessor: DataProcessor<RTKStationConnetState> =
        DataProcessor.create(RTKStationConnetState.UNKNOWN)
    private val rtkNetworkServiceInfoProcessor: DataProcessor<RTKServiceState> =
        DataProcessor.create(RTKServiceState.UNKNOWN)

    private val unitTypeProcessor: DataProcessor<UnitConversionUtil.UnitType> =
        DataProcessor.create(UnitConversionUtil.UnitType.METRIC)

    //rtk连接数据的封装Processor，用于widget显示rtk是否连接和数据是否正在使用
    private val rtkBaseStationStateProcessor: DataProcessor<RTKBaseStationState> =
        DataProcessor.create(RTKBaseStationState.DISCONNECTED)
    private val rtkNetworkServiceStateProcessor: DataProcessor<RTKNetworkServiceState> = DataProcessor.create(
        RTKNetworkServiceState(
            RTKServiceState.UNKNOWN,
            isRTKBeingUsed = false,
            isNetworkServiceOpen = false,
            rtkSignal = RTKReferenceStationSource.UNKNOWN
        )
    )

    //标准差
    private val standardDeviationProcessor: DataProcessor<StandardDeviation> = DataProcessor.create(
        StandardDeviation(
            0f,
            0f,
            0f,
            UnitConversionUtil.UnitType.METRIC
        )
    )

    private val rtkLocationInfoListener = RTKLocationInfoListener {
        rtkLocationInfoProcessor.onNext(it)
        updateStandardDeviation(it.rtkLocation)

    }
    private val rtkSystemStateListener = RTKSystemStateListener {
        rtkSystemStateProcessor.onNext(it)
        //RTKSystemState涉及RTK服务类型的改变，所以有关于RTK服务类型的都需要更新
        updateRTKConnectionState()
        updateRTKListener(it.rtkReferenceStationSource)

    }
    private val stationConnectStatusListener = RTKStationConnectStatusListener {
        LogUtils.i(TAG, it)
        rtkStationConnectStateProcessor.onNext(it)
        updateRTKConnectionState()
    }

    private var mRTKServiceState = RTKServiceState.UNKNOWN

    private val networkServiceInfoListener: INetworkServiceInfoListener = object :
        INetworkServiceInfoListener {
        override fun onServiceStateUpdate(state: RTKServiceState?) {
            state?.let {
                if (mRTKServiceState != state) {
                    LogUtils.i(TAG, "onServiceStateUpdate RTKServiceState=$state")
                    mRTKServiceState = state
                    rtkNetworkServiceInfoProcessor.onNext(it)
                    updateRTKConnectionState()
                }
            }
        }

        override fun onErrorCodeUpdate(error: IDJIError?) {
            error?.let {
                LogUtils.e(TAG, error.toString())
            }
        }
    }


    @get:JvmName("getRTKLocationInfo")
    val rtkLocationInfo: Flowable<RTKLocationInfo>
        get() = rtkLocationInfoProcessor.toFlowable()


    @get:JvmName("getRTKSystemState")
    val rtkSystemState: Flowable<RTKSystemState>
        get() = rtkSystemStateProcessor.toFlowable()


    /**
     * Get the standard deviation of the location accuracy.
     */
    val standardDeviation: Flowable<StandardDeviation>
        get() = standardDeviationProcessor.toFlowable()

    /**
     * Get the state of the RTK base station.
     */
    @get:JvmName("getRTKBaseStationState")
    val rtkBaseStationState: Flowable<RTKBaseStationState>
        get() = rtkBaseStationStateProcessor.toFlowable()

    /**
     * Get the state of the network service.
     */
    @get:JvmName("getRTKNetworkServiceState")
    val rtkNetworkServiceState: Flowable<RTKNetworkServiceState>
        get() = rtkNetworkServiceStateProcessor.toFlowable()


    //region Constructor
    init {
        if (preferencesManager != null) {
            unitTypeProcessor.onNext(preferencesManager.unitType)
        }
    }
    //endregion


    override fun inSetup() {
        rtkCenter.addRTKLocationInfoListener(rtkLocationInfoListener)
        rtkCenter.addRTKSystemStateListener(rtkSystemStateListener)
        rtkCenter.qxrtkManager.addNetworkRTKServiceInfoListener(networkServiceInfoListener)
        rtkCenter.customRTKManager.addNetworkRTKServiceInfoListener(networkServiceInfoListener)
        rtkCenter.cmccrtkManager.addNetworkRTKServiceInfoListener(networkServiceInfoListener)
        rtkCenter.rtkStationManager.addRTKStationConnectStatusListener(stationConnectStatusListener)
        //测试发现productConnection有时候返回true比较慢，所以在其值返回也是要刷新依赖其者的状态
        productConnection.observeOn(SchedulerProvider.ui()).subscribe {
            updateRTKConnectionState()
        }

        val unitKey = UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE)
        bindDataProcessor(unitKey, unitTypeProcessor)
        updateRTKConnectionState()
    }

    override fun inCleanup() {
        rtkCenter.removeRTKLocationInfoListener(rtkLocationInfoListener)
        rtkCenter.removeRTKSystemStateListener(rtkSystemStateListener)
        rtkCenter.rtkStationManager.removeRTKStationConnectStatusListener(stationConnectStatusListener)
        rtkCenter.qxrtkManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
        rtkCenter.customRTKManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
        rtkCenter.cmccrtkManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
    }

    private fun updateRTKListener(rtkSource: RTKReferenceStationSource) {
        when (rtkSource) {
            RTKReferenceStationSource.QX_NETWORK_SERVICE -> {
                rtkCenter.customRTKManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
                rtkCenter.cmccrtkManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
                rtkCenter.rtkStationManager.removeRTKStationConnectStatusListener(stationConnectStatusListener)
                rtkCenter.qxrtkManager.addNetworkRTKServiceInfoListener(networkServiceInfoListener)
            }
            RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE -> {
                rtkCenter.qxrtkManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
                rtkCenter.cmccrtkManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
                rtkCenter.rtkStationManager.removeRTKStationConnectStatusListener(stationConnectStatusListener)
                rtkCenter.customRTKManager.addNetworkRTKServiceInfoListener(networkServiceInfoListener)

            }
            RTKReferenceStationSource.NTRIP_NETWORK_SERVICE -> {
                rtkCenter.qxrtkManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
                rtkCenter.customRTKManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
                rtkCenter.rtkStationManager.removeRTKStationConnectStatusListener(stationConnectStatusListener)
                rtkCenter.cmccrtkManager.addNetworkRTKServiceInfoListener(networkServiceInfoListener)

            }
            RTKReferenceStationSource.BASE_STATION -> {
                rtkCenter.qxrtkManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
                rtkCenter.customRTKManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
                rtkCenter.cmccrtkManager.removeNetworkRTKServiceInfoListener(networkServiceInfoListener)
                rtkCenter.rtkStationManager.addRTKStationConnectStatusListener(stationConnectStatusListener)

            }
            else -> {
                //do nothing
            }
        }

    }


    private fun updateStandardDeviation(rtkLocation: RTKLocation?) {
        var stdLatitude = 0f
        var stdLongitude = 0f
        var stdAltitude = 0f
        rtkLocation?.let {
            if (unitTypeProcessor.value == UnitConversionUtil.UnitType.IMPERIAL) {
                stdLatitude = UnitConversionUtil.convertMetersToFeet(it.stdLatitude.toFloat())
                stdLongitude = UnitConversionUtil.convertMetersToFeet(it.stdLongitude.toFloat())
                stdAltitude = UnitConversionUtil.convertMetersToFeet(it.stdAltitude.toFloat())
            } else {
                stdLatitude = it.stdLatitude.toFloat()
                stdLongitude = it.stdLongitude.toFloat()
                stdAltitude = it.stdAltitude.toFloat()
            }
        }
        standardDeviationProcessor.onNext(
            StandardDeviation(
                stdLatitude,
                stdLongitude,
                stdAltitude,
                unitTypeProcessor.value
            )
        )
    }

    /**
     * Sends the latest network service state or base station state to the corresponding flowable.
     */
    fun updateRTKConnectionState() {
        rtkSystemStateProcessor.value.rtkReferenceStationSource?.let {
            if (isNetworkServiceOpen(it)) {
                updateNetworkServiceState()
            } else {
                updateBaseStationState()
            }
        }

    }


    /**
     * The state of the network service.
     */
    data class RTKNetworkServiceState(
        val state: RTKServiceState?,
        val isRTKBeingUsed: Boolean?,
        val isNetworkServiceOpen: Boolean?,
        @get:JvmName("getRTKSignal")
        val rtkSignal: RTKReferenceStationSource?,
    )

    /**
     * The state of the RTK base station
     */
    enum class RTKBaseStationState {
        /**
         * The RTK base station is connected and in use.
         */
        CONNECTED_IN_USE,

        /**
         * The RTK base station is connected and not in use.
         */
        CONNECTED_NOT_IN_USE,

        /**
         * The RTK base station is disconnected.
         */
        DISCONNECTED
    }

    /**
     * The standard deviation of the location accuracy.
     */
    data class StandardDeviation(
        val latitude: Float,
        val longitude: Float,
        val altitude: Float,
        val unitType: UnitConversionUtil.UnitType,
    )

    //region Helper methods
    private fun updateNetworkServiceState() {
        rtkNetworkServiceStateProcessor.onNext(
            RTKNetworkServiceState(
                rtkNetworkServiceInfoProcessor.value,
                rtkSystemStateProcessor.value.rtkHealthy,
                isNetworkServiceOpen(rtkSystemStateProcessor.value.rtkReferenceStationSource),
                rtkSystemStateProcessor.value.rtkReferenceStationSource
            )
        )
    }

    private fun updateBaseStationState() {
        if (rtkStationConnectStateProcessor.value == RTKStationConnetState.CONNECTED && productConnectionProcessor.value) {
            if (rtkSystemStateProcessor.value.rtkHealthy) {
                rtkBaseStationStateProcessor.onNext(RTKBaseStationState.CONNECTED_IN_USE)
            } else {
                rtkBaseStationStateProcessor.onNext(RTKBaseStationState.CONNECTED_NOT_IN_USE)
            }
        } else {
            rtkBaseStationStateProcessor.onNext(RTKBaseStationState.DISCONNECTED)
        }
    }


    private fun isNetworkServiceOpen(rtkSignal: RTKReferenceStationSource?): Boolean {
        return rtkSignal == RTKReferenceStationSource.QX_NETWORK_SERVICE
                || rtkSignal == RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE
                || rtkSignal == RTKReferenceStationSource.NTRIP_NETWORK_SERVICE
    }
}