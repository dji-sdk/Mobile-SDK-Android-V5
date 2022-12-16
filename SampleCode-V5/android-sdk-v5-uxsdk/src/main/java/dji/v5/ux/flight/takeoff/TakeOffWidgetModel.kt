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

package dji.v5.ux.flight.takeoff

import dji.sdk.keyvalue.key.FlightAssistantKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.ProductKey
import dji.sdk.keyvalue.key.RemoteControllerKey
import dji.sdk.keyvalue.value.flightassistant.LandingProtectionState
import dji.sdk.keyvalue.value.flightcontroller.FCAutoRTHReason
import dji.sdk.keyvalue.value.flightcontroller.FCFlightMode
import dji.sdk.keyvalue.value.product.ProductType
import dji.sdk.keyvalue.value.remotecontroller.RCMode
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.GlobalPreferenceKeys
import dji.v5.ux.core.communication.GlobalPreferencesInterface
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.communication.UXKeys
import dji.v5.ux.core.util.DataProcessor
import dji.v5.ux.core.util.UnitConversionUtil
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable

private const val TAKEOFF_HEIGHT: Float = 1.2f
private const val PRECISION_TAKEOFF_HEIGHT: Float = 6f
private const val LAND_HEIGHT: Float = 0.3f

/**
 * Widget Model for the [TakeOffWidget] used to define
 * the underlying logic and communication
 */
class TakeOffWidgetModel(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore,
    private val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields
    private val isFlyingDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isAutoLandingDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isLandingConfNeededDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val forceLandingHeightDataProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val areMotorsOnDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isGoingHomeDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val flightModeStringDataProcessor: DataProcessor<String> = DataProcessor.create("")
    private val isCancelAutoLandingDisabledProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val autoRTHReasonProcessor: DataProcessor<FCAutoRTHReason> = DataProcessor.create(FCAutoRTHReason.UNKNOWN)
    private val rcModeDataProcessor: DataProcessor<RCMode> = DataProcessor.create(RCMode.UNKNOWN)
    private val productModelProcessor: DataProcessor<ProductType> = DataProcessor.create(ProductType.UNKNOWN)
    private val unitTypeProcessor: DataProcessor<UnitConversionUtil.UnitType> = DataProcessor.create(UnitConversionUtil.UnitType.METRIC)
    private val landingProtectionStateDataProcessor: DataProcessor<LandingProtectionState> = DataProcessor.create(LandingProtectionState.UNKNOWN)
    private val flightModeProcessor: DataProcessor<FCFlightMode> = DataProcessor.create(FCFlightMode.UNKNOWN)

    private val takeOffLandingStateDataProcessor: DataProcessor<TakeOffLandingState> =
        DataProcessor.create(TakeOffLandingState.DISCONNECTED)
    private val isInAttiModeDataProcessor: DataProcessor<Boolean> =
        DataProcessor.create(false)
    //endregion

    //region Data
    /**
     * Get the takeoff landing state
     */
    val takeOffLandingState: Flowable<TakeOffLandingState>
        get() = takeOffLandingStateDataProcessor.toFlowable().distinctUntilChanged()

    /**
     * Get whether the product is in ATTI mode
     */
    val isInAttiMode: Flowable<Boolean>
        get() = isInAttiModeDataProcessor.toFlowable()

    /**
     * Get the height the aircraft will reach after takeoff
     */
    val takeOffHeight: Height
        get() = getHeightFromValue(TAKEOFF_HEIGHT)

    /**
     * Get the height the aircraft will reach after a precision takeoff
     */
    val precisionTakeOffHeight: Height
        get() = getHeightFromValue(PRECISION_TAKEOFF_HEIGHT)

    /**
     * Get the current height of the aircraft while waiting for landing confirmation
     */
    val landHeight: Height
        get() = getHeightFromValue(getLandHeight())
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
     * Performs take off action
     */
    fun performTakeOffAction(): Completable {
        return djiSdkModel.performActionWithOutResult(KeyTools.createKey(FlightControllerKey.KeyStartTakeoff))
            .onErrorResumeNext { error: Throwable ->
                if (areMotorsOnDataProcessor.value) {
                    return@onErrorResumeNext Completable.complete()
                } else {
                    return@onErrorResumeNext Completable.error(error)
                }
            }
    }

    /**
     * Performs precision take off action
     */
    fun performPrecisionTakeOffAction(): Completable {
        //TODO KeyPrecisionStartTakeoff存在问题，csdk那边修复时间未知，这里先换成takeoff
        return djiSdkModel.performActionWithOutResult(KeyTools.createKey(FlightControllerKey.KeyStartTakeoff))
            .onErrorResumeNext { error: Throwable? ->
                if (areMotorsOnDataProcessor.value) {
                    return@onErrorResumeNext Completable.complete()
                } else {
                    return@onErrorResumeNext Completable.error(error)
                }
            }
    }

    /**
     * Performs landing action
     */
    fun performLandingAction(): Completable {
        return djiSdkModel.performActionWithOutResult(KeyTools.createKey(FlightControllerKey.KeyStartAutoLanding))
    }

    /**
     * Performs cancel landing action
     */
    fun performCancelLandingAction(): Completable {
        return djiSdkModel.performActionWithOutResult(KeyTools.createKey(FlightControllerKey.KeyStopAutoLanding))
    }

    /**
     * Performs the landing confirmation action. This allows aircraft to land when
     * landing confirmation is received.
     */
    fun performLandingConfirmationAction(): Completable {
        return djiSdkModel.performActionWithOutResult(KeyTools.createKey(FlightControllerKey.KeyConfirmLanding))
    }

    //endregion

    //region Lifecycle
    override fun inSetup() {
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyIsFlying), isFlyingDataProcessor)
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyIsInLandingMode), isAutoLandingDataProcessor)
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyIsLandingConfirmationNeeded), isLandingConfNeededDataProcessor)
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyTouchDownConfirmLimitHeight), forceLandingHeightDataProcessor)
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyFlightModeString), flightModeStringDataProcessor) { value: Any ->
            isInAttiModeDataProcessor.onNext((value as String).contains("atti", ignoreCase = true))
        }
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyAreMotorsOn), areMotorsOnDataProcessor)
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyFCFlightMode), flightModeProcessor) {
            isGoingHomeDataProcessor.onNext(it == FCFlightMode.GO_HOME || it == FCFlightMode.AUTO_LANDING)
        }
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyAutoRTHReason), autoRTHReasonProcessor) {
            isCancelAutoLandingDisabledProcessor.onNext(
                it == FCAutoRTHReason.WARNING_POWER_LANDING
                        || it == FCAutoRTHReason.SMART_POWER_LANDING
                        || it == FCAutoRTHReason.LOW_VOLTAGE_LANDING
                        || it == FCAutoRTHReason.SERIOUS_LOW_VOLTAGE_LANDING
                        || it == FCAutoRTHReason.NO_FLY_ZONE_LANDING
                        || it == FCAutoRTHReason.BATTERY_FORCE_LANDING
            )
        }
        bindDataProcessor(KeyTools.createKey(RemoteControllerKey.KeyRcMachineMode), rcModeDataProcessor)
        bindDataProcessor(KeyTools.createKey(ProductKey.KeyProductType), productModelProcessor)
        val unitKey = UXKeys.create(GlobalPreferenceKeys.UNIT_TYPE)
        bindDataProcessor(unitKey, unitTypeProcessor)
        bindDataProcessor(KeyTools.createKey(FlightAssistantKey.KeyLandingProtectionState), landingProtectionStateDataProcessor)
        preferencesManager?.setUpListener()
    }

    override fun inCleanup() {
        preferencesManager?.cleanup()
    }

    override fun updateStates() {
        if (!productConnectionProcessor.value) {
            takeOffLandingStateDataProcessor.onNext(TakeOffLandingState.DISCONNECTED)
        } else if (isAutoLandingDataProcessor.value) {
            updateAutoLandingData()
        } else if (isGoingHomeDataProcessor.value && !isAutoLandingDataProcessor.value) {
            takeOffLandingStateDataProcessor.onNext(TakeOffLandingState.RETURNING_TO_HOME)
        } else if (!areMotorsOnDataProcessor.value) {
            if (rcModeDataProcessor.value == RCMode.SLAVE) {
                takeOffLandingStateDataProcessor.onNext(TakeOffLandingState.TAKE_OFF_DISABLED)
            } else {
                takeOffLandingStateDataProcessor.onNext(TakeOffLandingState.READY_TO_TAKE_OFF)
            }
        } else {
            if (rcModeDataProcessor.value == RCMode.SLAVE) {
                takeOffLandingStateDataProcessor.onNext(TakeOffLandingState.LAND_DISABLED)
            } else {
                takeOffLandingStateDataProcessor.onNext(TakeOffLandingState.READY_TO_LAND)
            }
        }
    }

    private fun updateAutoLandingData() {
        if (isLandingConfNeededDataProcessor.value) {
            takeOffLandingStateDataProcessor.onNext(TakeOffLandingState.WAITING_FOR_LANDING_CONFIRMATION)
        } else if (isCancelAutoLandingDisabled()) {
            takeOffLandingStateDataProcessor.onNext(TakeOffLandingState.FORCED_AUTO_LANDING)
        } else if (landingProtectionStateDataProcessor.value == LandingProtectionState.NOT_SAFE_TO_LAND) {
            takeOffLandingStateDataProcessor.onNext(TakeOffLandingState.UNSAFE_TO_LAND)
        } else {
            takeOffLandingStateDataProcessor.onNext(TakeOffLandingState.AUTO_LANDING)
        }
    }

    private fun isCancelAutoLandingDisabled(): Boolean {
        return isCancelAutoLandingDisabledProcessor.value ||
                rcModeDataProcessor.value == RCMode.SLAVE
    }

    private fun getHeightFromValue(value: Float): Height {
        return Height(
            if (unitTypeProcessor.value == UnitConversionUtil.UnitType.IMPERIAL) {
                UnitConversionUtil.convertMetersToFeet(value)
            } else {
                value
            },
            unitTypeProcessor.value
        )
    }

    private fun getLandHeight(): Float {
        return if (forceLandingHeightDataProcessor.value != Int.MIN_VALUE) {
            forceLandingHeightDataProcessor.value * 0.1f
        } else {
            LAND_HEIGHT
        }
    }
    //endregion

    //region Classes
    /**
     * The state of the aircraft
     */
    enum class TakeOffLandingState {
        /**
         * The aircraft is ready to take off
         */
        READY_TO_TAKE_OFF,

        /**
         * The aircraft is currently flying and is ready to land
         */
        READY_TO_LAND,

        /**
         * The aircraft has started auto landing
         */
        AUTO_LANDING,

        /**
         * The aircraft has started auto landing and it cannot be canceled
         */
        FORCED_AUTO_LANDING,

        /**
         * The aircraft has paused auto landing and is waiting for confirmation before continuing
         */
        WAITING_FOR_LANDING_CONFIRMATION,

        /**
         * The aircraft has determined it is unsafe to land while auto landing is in progress
         */
        UNSAFE_TO_LAND,

        /**
         * The aircraft is returning to its home point
         */
        RETURNING_TO_HOME,

        /**
         * The aircraft cannot take off
         */
        TAKE_OFF_DISABLED,

        /**
         * The aircraft cannot land
         */
        LAND_DISABLED,

        /**
         * The aircraft is disconnected
         */
        DISCONNECTED
    }

    /**
     * Represents a height and the height's unit.
     *
     * @property height The current height of the aircraft in [unitType]
     * @property unitType The unit type of [height]
     */
    data class Height(
        val height: Float,
        val unitType: UnitConversionUtil.UnitType
    )
    //endregion
}