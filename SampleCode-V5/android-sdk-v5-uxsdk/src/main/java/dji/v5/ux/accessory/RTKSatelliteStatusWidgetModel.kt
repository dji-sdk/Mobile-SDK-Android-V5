/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.v5.ux.accessory

import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.GlobalPreferencesInterface
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore

private const val TAG = "RTKStatusWidgetModel"

/**
 * Widget Model for the [RTKSatelliteStatusWidget] used to define
 * the underlying logic and communication
 */
class RTKSatelliteStatusWidgetModel(
    djiSdkModel: DJISDKModel,
    uxKeyManager: ObservableInMemoryKeyedStore,
    private val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, uxKeyManager)/**, NetworkServiceState.Callback **/{

//    //region Fields
//    private val rtkStateProcessor: DataProcessor<RTKState> = DataProcessor.create(RTKState.Builder().build())
//    private val isRTKConnectedProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
//    private val modelProcessor: DataProcessor<Model> = DataProcessor.create(Model.UNKNOWN_AIRCRAFT)
//    private val referenceStationSourceProcessor: DataProcessor<ReferenceStationSource> = DataProcessor.create(ReferenceStationSource.UNKNOWN)
//    private val unitTypeProcessor: DataProcessor<UnitConversionUtil.UnitType> = DataProcessor.create(UnitConversionUtil.UnitType.METRIC)
//    private val networkServiceStateProcessor: DataProcessor<NetworkServiceChannelState> = DataProcessor.create(NetworkServiceChannelState.UNKNOWN)
//
//    private val rtkBaseStationStateProcessor: DataProcessor<RTKBaseStationState> = DataProcessor.create(RTKBaseStationState.DISCONNECTED)
//    private val rtkNetworkServiceStateProcessor: DataProcessor<RTKNetworkServiceState> = DataProcessor.create(
//            RTKNetworkServiceState(NetworkServiceChannelState.UNKNOWN,
//                    isRTKBeingUsed = false,
//                    isNetworkServiceOpen = false,
//                    rtkSignal = RTKSignal.BASE_STATION))
//    private val standardDeviationProcessor: DataProcessor<StandardDeviation> = DataProcessor.create(StandardDeviation(
//            0f,
//            0f,
//            0f,
//            UnitConversionUtil.UnitType.METRIC))
//    private val rtkSignalProcessor: DataProcessor<RTKSignal> = DataProcessor.create(RTKSignal.BASE_STATION)
//    //endregion
//
//    //region Data
//    /**
//     * Get whether the RTK is connected.
//     */
//    val isRTKConnected: Flowable<Boolean>
//        get() = Flowable.combineLatest(isRTKConnectedProcessor.toFlowable(), productConnection,
//                BiFunction { isRTKConnected: Boolean, isProductConnected: Boolean -> isRTKConnected && isProductConnected })
//
//    /**
//     * Get the RTK state.
//     */
//    @get:JvmName("getRTKState")
//    val rtkState: Flowable<RTKState>
//        get() = rtkStateProcessor.toFlowable()
//
//    /**
//     * Get the model of the product.
//     */
//    val model: Flowable<Model>
//        get() = modelProcessor.toFlowable()
//
//    /**
//     * Get the source of the RTK signal.
//     */
//    @get:JvmName("getRTKSignal")
//    val rtkSignal: Flowable<RTKSignal>
//        get() = rtkSignalProcessor.toFlowable()
//
//    /**
//     * Get the standard deviation of the location accuracy.
//     */
//    val standardDeviation: Flowable<StandardDeviation>
//        get() = standardDeviationProcessor.toFlowable()
//
//    /**
//     * Get the state of the RTK base station.
//     */
//    @get:JvmName("getRTKBaseStationState")
//    val rtkBaseStationState: Flowable<RTKBaseStationState>
//        get() = rtkBaseStationStateProcessor.toFlowable()
//
//    /**
//     * Get the state of the network service.
//     */
//    @get:JvmName("getRTKNetworkServiceState")
//    val rtkNetworkServiceState: Flowable<RTKNetworkServiceState>
//        get() = rtkNetworkServiceStateProcessor.toFlowable()
//
//    /**
//     * Sends the latest network service state or base station state to the corresponding flowable.
//     */
//    fun updateRTKConnectionState() {
//        if (isNetworkServiceOpen(referenceStationSourceProcessor.value)) {
//            updateNetworkServiceState()
//        } else {
//            updateBaseStationState()
//        }
//    }
//    //endregion
//
//    //region Constructor
//    init {
//        if (preferencesManager != null) {
//            unitTypeProcessor.onNext(preferencesManager.unitType)
//        }
//    }
//    //endregion
//
//    //region Lifecycle
//    override fun inSetup() {
//        val rtkStateKey: DJIKey = FlightControllerKey.createRTKKey(FlightControllerKey.RTK_STATE)
//        bindDataProcessor(rtkStateKey, rtkStateProcessor)
//        val isRTKConnectedKey: DJIKey = FlightControllerKey.createRTKKey(FlightControllerKey.IS_RTK_CONNECTED)
//        bindDataProcessor(isRTKConnectedKey, isRTKConnectedProcessor)
//        val modelKey: DJIKey = ProductKey.create(ProductKey.MODEL_NAME)
//        bindDataProcessor(modelKey, modelProcessor) { value: Any ->
//            if (value == Model.MATRICE_210_RTK) {
//                rtkSignalProcessor.onNext(RTKSignal.BASE_STATION)
//            }
//        }
//        val rtkSignalKey: DJIKey = FlightControllerKey.createRTKKey(FlightControllerKey.RTK_REFERENCE_STATION_SOURCE)
//        bindDataProcessor(rtkSignalKey, referenceStationSourceProcessor) { value: Any ->
//            when (value) {
//                ReferenceStationSource.NETWORK_RTK -> {
//                    rtkSignalProcessor.onNext(RTKSignal.NETWORK_RTK)
//                }
//                ReferenceStationSource.BASE_STATION -> {
//                    rtkSignalProcessor.onNext(RTKSignal.D_RTK_2)
//                }
//                ReferenceStationSource.CUSTOM_NETWORK_SERVICE -> {
//                    rtkSignalProcessor.onNext(RTKSignal.CUSTOM_NETWORK)
//                }
//            }
//        }
//        val unitKey = UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE)
//        bindDataProcessor(unitKey, unitTypeProcessor)
//        RTKNetworkServiceProvider.getInstance().addNetworkServiceStateCallback(this)
//        preferencesManager?.setUpListener()
//    }
//
//    override fun inCleanup() {
//        RTKNetworkServiceProvider.getInstance().removeNetworkServiceStateCallback(this)
//        preferencesManager?.cleanup()
//    }
//
//    override fun updateStates() {
//        updateRTKConnectionState()
//
//        var stdLatitude = 0f
//        var stdLongitude = 0f
//        var stdAltitude = 0f
//        val standardDeviation = rtkStateProcessor.value.mobileStationStandardDeviation
//        standardDeviation?.let {
//            if (unitTypeProcessor.value == UnitConversionUtil.UnitType.IMPERIAL) {
//                stdLatitude = UnitConversionUtil.convertMetersToFeet(it.stdLatitude)
//                stdLongitude = UnitConversionUtil.convertMetersToFeet(it.stdLongitude)
//                stdAltitude = UnitConversionUtil.convertMetersToFeet(it.stdAltitude)
//            } else {
//                stdLatitude = it.stdLatitude
//                stdLongitude = it.stdLongitude
//                stdAltitude = it.stdAltitude
//            }
//        }
//        standardDeviationProcessor.onNext(StandardDeviation(
//                stdLatitude,
//                stdLongitude,
//                stdAltitude,
//                unitTypeProcessor.value))
//    }
//
//    override fun onNetworkServiceStateUpdate(networkServiceState: NetworkServiceState?) {
//        if (networkServiceState != null && networkServiceStateProcessor.value != networkServiceState.channelState) {
//            networkServiceStateProcessor.onNext(networkServiceState.channelState)
//            updateNetworkServiceState()
//        }
//    }
//
//    //endregion
//
//    //region Helper methods
//    private fun updateNetworkServiceState() {
//        rtkNetworkServiceStateProcessor.onNext(RTKNetworkServiceState(networkServiceStateProcessor.value,
//                rtkStateProcessor.value.isRTKBeingUsed,
//                isNetworkServiceOpen(referenceStationSourceProcessor.value),
//                rtkSignalProcessor.value))
//    }
//
//    private fun updateBaseStationState() {
//        if (isRTKConnectedProcessor.value && productConnectionProcessor.value) {
//            if (rtkStateProcessor.value.isRTKBeingUsed) {
//                rtkBaseStationStateProcessor.onNext(RTKBaseStationState.CONNECTED_IN_USE)
//            } else {
//                rtkBaseStationStateProcessor.onNext(RTKBaseStationState.CONNECTED_NOT_IN_USE)
//            }
//        } else {
//            rtkBaseStationStateProcessor.onNext(RTKBaseStationState.DISCONNECTED)
//        }
//    }
//
//    private fun isNetworkServiceOpen(rtkSignal: ReferenceStationSource): Boolean {
//        return rtkSignal == ReferenceStationSource.NETWORK_RTK
//                || rtkSignal == ReferenceStationSource.DPS
//                || rtkSignal == ReferenceStationSource.CUSTOM_NETWORK_SERVICE
//    }
//
//    //endregion
//
//    //region Classes
//    /**
//     * The state of the RTK base station
//     */
//    enum class RTKBaseStationState {
//        /**
//         * The RTK base station is connected and in use.
//         */
//        CONNECTED_IN_USE,
//
//        /**
//         * The RTK base station is connected and not in use.
//         */
//        CONNECTED_NOT_IN_USE,
//
//        /**
//         * The RTK base station is disconnected.
//         */
//        DISCONNECTED
//    }
//
//    /**
//     * The state of the network service.
//     */
//    data class RTKNetworkServiceState(val state: NetworkServiceChannelState,
//                                      val isRTKBeingUsed: Boolean,
//                                      val isNetworkServiceOpen: Boolean,
//                                      @get:JvmName("getRTKSignal")
//                                       val rtkSignal: RTKSignal)
//
//    /**
//     * The standard deviation of the location accuracy.
//     */
//    data class StandardDeviation(val latitude: Float,
//                                 val longitude: Float,
//                                 val altitude: Float,
//                                 val unitType: UnitConversionUtil.UnitType)
//
//    enum class RTKSignal {
//        /**
//         * Network RTK
//         */
//        NETWORK_RTK,
//
//        /**
//         * D-RTK 2 Mobile Station
//         */
//        D_RTK_2,
//
//        /**
//         * D-RTK Base Station
//         */
//        BASE_STATION,
//
//        /**
//         * Custom Network RTK
//         */
//        CUSTOM_NETWORK
//    }
    //endregion

    override fun inSetup() {
        //暂无实现
    }

    override fun inCleanup() {
        //暂无实现
    }
}