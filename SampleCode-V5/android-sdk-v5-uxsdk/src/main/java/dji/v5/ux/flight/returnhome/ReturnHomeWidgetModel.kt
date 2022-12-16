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

package dji.v5.ux.flight.returnhome

import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.DJIKey
import dji.sdk.keyvalue.key.RemoteControllerKey
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.sdk.keyvalue.value.flightcontroller.FCAutoRTHReason
import dji.sdk.keyvalue.value.flightcontroller.FCFlightMode
import dji.sdk.keyvalue.value.remotecontroller.RCMode
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.GlobalPreferenceKeys
import dji.v5.ux.core.communication.GlobalPreferencesInterface
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.communication.UXKeys
import dji.v5.ux.core.util.DataProcessor
import dji.v5.utils.common.LocationUtil
import dji.v5.ux.core.util.UnitConversionUtil
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable

/**
 * Widget Model for the [ReturnHomeWidget] used to define
 * the underlying logic and communication
 */
class ReturnHomeWidgetModel(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore,
    private val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields
    private val isGoingHomeDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isFlyingDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isAutoLandingDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val areMotorsOnDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val returnHomeDataProcessor: DataProcessor<ReturnHomeState> = DataProcessor.create(ReturnHomeState.DISCONNECTED)
    private val isCancelReturnToHomeDisabledProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val rcModeDataProcessor: DataProcessor<RCMode> = DataProcessor.create(RCMode.UNKNOWN)
//    private val flyZoneReturnToHomeStateProcessor: DataProcessor<FlyZoneReturnToHomeState> = DataProcessor.create(FlyZoneReturnToHomeState.UNKNOWN)
    private val flightModeProcessor: DataProcessor<FCFlightMode> = DataProcessor.create(FCFlightMode.UNKNOWN)
    private val autoRTHReasonProcessor: DataProcessor<FCAutoRTHReason> = DataProcessor.create(FCAutoRTHReason.UNKNOWN)
    private val unitTypeProcessor: DataProcessor<UnitConversionUtil.UnitType> = DataProcessor.create(UnitConversionUtil.UnitType.METRIC)
    //endregion

    //region Data
    /**
     * Get the return home state
     */
    val returnHomeState: Flowable<ReturnHomeState>
        get() = returnHomeDataProcessor.toFlowable().distinctUntilChanged()

    /**
     * Get the distance from the aircraft to the home point
     */
    val distanceToHome: ReturnHomeDistance
        get() {
            val goHomeHeightKey: DJIKey<Int> = KeyTools.createKey(FlightControllerKey.KeyGoHomeHeight)
            val homeLocationKey: DJIKey<LocationCoordinate2D> = KeyTools.createKey(FlightControllerKey.KeyHomeLocation)
            val currentHeightKey: DJIKey<Double> = KeyTools.createKey(FlightControllerKey.KeyAltitude)
            val aircraftLocationKey: DJIKey<LocationCoordinate2D> = KeyTools.createKey(FlightControllerKey.KeyAircraftLocation)
            var currentHeight = 0f
            var goHomeHeight = 0f
            var homeLatitude = Double.NaN
            var homeLongitude = Double.NaN
            var aircraftLocationLat = 0.0
            var aircraftLocationLong = 0.0
            val unitType = unitTypeProcessor.value

            djiSdkModel.getCacheValue(currentHeightKey)?.let {
                if (unitType == UnitConversionUtil.UnitType.IMPERIAL) {
                    currentHeight = UnitConversionUtil.convertMetersToFeet(currentHeight.toFloat())
                }
            }
            djiSdkModel.getCacheValue(goHomeHeightKey)?.let {
                if (unitType == UnitConversionUtil.UnitType.IMPERIAL) {
                    goHomeHeight = UnitConversionUtil.convertMetersToFeet(goHomeHeight.toFloat())
                }
            }
            djiSdkModel.getCacheValue(homeLocationKey)?.let {
                homeLatitude = it.latitude
                homeLongitude = it.longitude
            }
            djiSdkModel.getCacheValue(aircraftLocationKey)?.let {
                aircraftLocationLat = it.latitude
                aircraftLocationLong = it.longitude
            }
            var distanceToHome = 0f
            if (!homeLatitude.isNaN() && !homeLongitude.isNaN()) {
                distanceToHome = distanceBetween(homeLatitude, homeLongitude, aircraftLocationLat, aircraftLocationLong)
            }
            return ReturnHomeDistance(distanceToHome, currentHeight, goHomeHeight, unitType)
        }

    /**
     * Get whether returning to home at the current altitude is enabled
     */
    val isRTHAtCurrentAltitudeEnabled: Boolean
        get() {
//            val rthAtCurrentHeightKey = FlightControllerKey.create(FlightControllerKey.CONFIG_RTH_IN_CURRENT_ALTITUDE)
//            var isRTHAtCurrentAltitudeEnabled = true
//            djiSdkModel.getCacheValue(rthAtCurrentHeightKey)?.let {
//                isRTHAtCurrentAltitudeEnabled = it as Boolean
//            }
//            return isRTHAtCurrentAltitudeEnabled
            return true
        }

    /**
     * Get the latest [FlyZoneReturnToHomeState]
     */
//    val flyZoneReturnToHomeState: FlyZoneReturnToHomeState
//        get() = flyZoneReturnToHomeStateProcessor.value
    //endregion

    //region Constructor
    init {
        if (preferencesManager != null) {
            unitTypeProcessor.onNext(preferencesManager.unitType)
        }
    }
    //endregion

    //region Actions
    /**
     * Performs return to home action
     */
    fun performReturnHomeAction(): Completable {
        return djiSdkModel.performActionWithOutResult(KeyTools.createKey(FlightControllerKey.KeyStartGoHome))
    }

    /**
     * Performs cancel return to home action
     */
    fun performCancelReturnHomeAction(): Completable {
        return djiSdkModel.performActionWithOutResult(KeyTools.createKey(FlightControllerKey.KeyStopGoHome))
    }

    //endregion

    //region Lifecycle
    override fun inSetup() {
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyIsFlying), isFlyingDataProcessor)
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyIsInLandingMode), isAutoLandingDataProcessor)
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyAreMotorsOn), areMotorsOnDataProcessor)
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyFCFlightMode), flightModeProcessor) {
            isGoingHomeDataProcessor.onNext(it == FCFlightMode.GO_HOME || it == FCFlightMode.AUTO_LANDING)
        }
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyAutoRTHReason), autoRTHReasonProcessor){
            isCancelReturnToHomeDisabledProcessor.onNext(it == FCAutoRTHReason.MC_PROTECT_GOHOME)
        }
        bindDataProcessor(KeyTools.createKey(RemoteControllerKey.KeyRcMachineMode), rcModeDataProcessor)
//        val flyZoneReturnToHomeState: DJIKey = FlightControllerKey.create(FlightControllerKey.RETURN_TO_HOME_STATE)
//        bindDataProcessor(flyZoneReturnToHomeState, flyZoneReturnToHomeStateProcessor)
        val unitKey = UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE)
        bindDataProcessor(unitKey, unitTypeProcessor)
        preferencesManager?.setUpListener()
    }

    override fun inCleanup() {
        preferencesManager?.cleanup()
    }

    override fun updateStates() {
        if (!productConnectionProcessor.value) {
            returnHomeDataProcessor.onNext(ReturnHomeState.DISCONNECTED)
        } else if (!isFlyingDataProcessor.value
            || !areMotorsOnDataProcessor.value) {
            returnHomeDataProcessor.onNext(ReturnHomeState.RETURN_HOME_DISABLED)
        } else if (isAutoLandingDataProcessor.value) {
            returnHomeDataProcessor.onNext(ReturnHomeState.AUTO_LANDING)
        } else if (isGoingHomeDataProcessor.value && !isAutoLandingDataProcessor.value) {
            if (isCancelReturnHomeDisabled()) {
                returnHomeDataProcessor.onNext(ReturnHomeState.FORCED_RETURNING_TO_HOME)
            } else {
                returnHomeDataProcessor.onNext(ReturnHomeState.RETURNING_TO_HOME)
            }
        } else {
            returnHomeDataProcessor.onNext(ReturnHomeState.READY_TO_RETURN_HOME)
        }
    }

    private fun isCancelReturnHomeDisabled(): Boolean {
        return isCancelReturnToHomeDisabledProcessor.value ||
                rcModeDataProcessor.value == RCMode.SLAVE
    }
    //endregion

    //region Helpers for unit testing
    private fun distanceBetween(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Float {
        return LocationUtil.distanceBetween(latitude1, longitude1, latitude2, longitude2)
    }
    //endregion

    //region Classes
    /**
     * The state of the aircraft
     */
    enum class ReturnHomeState {
        /**
         * The aircraft is ready to return to home
         */
        READY_TO_RETURN_HOME,

        /**
         * The aircraft cannot return to home
         */
        RETURN_HOME_DISABLED,

        /**
         * The aircraft has started returning to home
         */
        RETURNING_TO_HOME,

        /**
         * The aircraft has started returning to home and it cannot be canceled
         */
        FORCED_RETURNING_TO_HOME,

        /**
         * The aircraft has started auto landing
         */
        AUTO_LANDING,

        /**
         * The aircraft is disconnected
         */
        DISCONNECTED
    }

    /**
     * The measurements describing the return to home behavior
     *
     * @property distanceToHome The distance to home in meters
     * @property currentHeight The current height of the aircraft in [unitType]
     * @property goToHomeHeight The height at which the aircraft will return to home in [unitType]
     * @property unitType The unit type of [currentHeight] and [goToHomeHeight]
     */
    data class ReturnHomeDistance(
        val distanceToHome: Float,
        val currentHeight: Float,
        val goToHomeHeight: Float,
        val unitType: UnitConversionUtil.UnitType
    )

    //endregion
}