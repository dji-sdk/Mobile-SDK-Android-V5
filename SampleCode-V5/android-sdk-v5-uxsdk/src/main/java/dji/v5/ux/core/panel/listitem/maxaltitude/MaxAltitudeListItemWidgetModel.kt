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

package dji.v5.ux.core.panel.listitem.maxaltitude

import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.IntValueConfig
import dji.v5.et.create
import dji.v5.utils.common.LogUtils
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.GlobalPreferenceKeys
import dji.v5.ux.core.communication.GlobalPreferencesInterface
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.panel.listitem.maxaltitude.MaxAltitudeListItemWidgetModel.MaxAltitudeState.MaxAltitudeValue
import dji.v5.ux.core.panel.listitem.maxaltitude.MaxAltitudeListItemWidgetModel.MaxAltitudeState.ProductDisconnected
import dji.v5.ux.core.util.DataProcessor
import dji.v5.ux.core.util.UnitConversionUtil.*
import io.reactivex.rxjava3.core.Completable
import kotlin.math.roundToInt

private const val TAG = "MaxAltitudeListItemWidgetModel"

/**
 * Widget Model for the [MaxAltitudeListItemWidget] used to define
 * the underlying logic and communication
 */
class MaxAltitudeListItemWidgetModel(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore,
    private val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {

    private val minFlightHeight = 20
    private val maxFlightHeight = 120
    private val maxFlightHeightProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val returnHomeFlightHeightProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val noviceModeProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val maxFlightHeightRangeProcessor: DataProcessor<IntValueConfig> = DataProcessor.create(IntValueConfig(minFlightHeight, maxFlightHeight, minFlightHeight))
    private val maxAltitudeStateProcessor: DataProcessor<MaxAltitudeState> = DataProcessor.create(ProductDisconnected)
    private val unitTypeDataProcessor: DataProcessor<UnitType> = DataProcessor.create(UnitType.METRIC)

    /**
     * Get the max altitude state
     */
    val maxAltitudeState: Flowable<MaxAltitudeState>
        get() = maxAltitudeStateProcessor.toFlowable()

    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyHeightLimit), maxFlightHeightProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyGoHomeHeight), returnHomeFlightHeightProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyHeightLimitRange), maxFlightHeightRangeProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyNoviceModeEnabled), noviceModeProcessor)
        bindDataProcessor(GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE), unitTypeDataProcessor)
        preferencesManager?.setUpListener()
        preferencesManager?.let { unitTypeDataProcessor.onNext(it.unitType) }
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (noviceModeProcessor.value) {
                maxAltitudeStateProcessor.onNext(MaxAltitudeState.NoviceMode(unitTypeDataProcessor.value))

            } else {
                val minLimit = getMinLimit()
                val maxLimit = getMaxLimit()
                val currentMaxFlightValue = getCurrentMaxFlightValue()
                maxAltitudeStateProcessor.onNext(
                    MaxAltitudeValue(
                        altitudeLimit = currentMaxFlightValue,
                        minAltitudeLimit = minLimit,
                        maxAltitudeLimit = maxLimit,
                        unitType = unitTypeDataProcessor.value,
                        returnToHomeHeight = getReturnHomeHeightByUnit()
                    )
                )
            }
        } else {
            maxAltitudeStateProcessor.onNext(ProductDisconnected)
        }
    }

    private fun getCurrentMaxFlightValue(): Int {
        return if (unitTypeDataProcessor.value == UnitType.METRIC) {
            maxFlightHeightProcessor.value
        } else {
            convertMetersToFeet(maxFlightHeightProcessor.value.toFloat()).roundToInt()
        }
    }

    private fun getMinLimit(): Int {
        val tempMinValue: Int = maxFlightHeightRangeProcessor.value.min
        return if (unitTypeDataProcessor.value == UnitType.METRIC) {
            tempMinValue
        } else {
            convertMetersToFeet(tempMinValue.toFloat()).roundToInt()
        }
    }

    private fun getMaxLimit(): Int {
        val tempMaxValue: Int = maxFlightHeightRangeProcessor.value.max
        return if (unitTypeDataProcessor.value == UnitType.METRIC) {
            tempMaxValue
        } else {
            convertMetersToFeet(tempMaxValue.toFloat()).roundToInt()
        }
    }

    private fun getReturnHomeHeightByUnit(): Int {
        return if (unitTypeDataProcessor.value == UnitType.METRIC) {
            returnHomeFlightHeightProcessor.value
        } else {
            convertMetersToFeet(returnHomeFlightHeightProcessor.value.toFloat()).roundToInt()
        }
    }

    /**
     * Set flight height limit
     *
     * @return Completable to determine status of action
     */
    fun setFlightMaxAltitude(maxAltitudeLimit: Int): Completable {
        val tempLimit: Int = if (unitTypeDataProcessor.value == UnitType.IMPERIAL) {
            convertFeetToMeters(maxAltitudeLimit.toFloat()).roundToInt()
        } else {
            maxAltitudeLimit
        }
        return djiSdkModel.setValue(
            KeyTools.createKey(
                FlightControllerKey.KeyHeightLimit), tempLimit)
            .doOnComplete {
                if (tempLimit < returnHomeFlightHeightProcessor.value) {
                    addDisposable(setReturnHomeMaxAltitude(tempLimit).subscribe({
                    }, {
                        LogUtils.e(TAG, it.message)
                    }))
                }
            }
    }


    private fun setReturnHomeMaxAltitude(maxAltitudeLimit: Int): Completable {
        return djiSdkModel.setValue(FlightControllerKey.KeyGoHomeHeight.create(), maxAltitudeLimit)
    }

    /**
     * Check if input is in range
     *
     * @return Boolean
     * true - if the input is in range
     * false - if the input is out of range
     */
    fun isInputInRange(input: Int): Boolean {
        return input >= getMinLimit() && input <= getMaxLimit()
    }

    override fun inCleanup() {
        preferencesManager?.cleanup()
    }

    /**
     * Class represents states of Max Altitude List Item
     */
    sealed class MaxAltitudeState {
        /**
         * When product is disconnected
         */
        object ProductDisconnected : MaxAltitudeState()

        /**
         * When product is in beginner mode
         * @property unitType - current unit system used
         */
        data class NoviceMode(val unitType: UnitType) : MaxAltitudeState()

        /**
         * Product returns max altitude levels and unit settings
         * are metric scale
         *
         * @property altitudeLimit - current altitude limit
         * @property minAltitudeLimit - altitude limit range minimum
         * @property maxAltitudeLimit - altitude limit range maximum
         * @property unitType - current unit system used
         * @property needFlightLimit - needs flight limit
         * @property returnToHomeHeight - altitude used while returning to home
         */
        data class MaxAltitudeValue(
            val altitudeLimit: Int,
            val minAltitudeLimit: Int,
            val maxAltitudeLimit: Int,
            val unitType: UnitType,
            val returnToHomeHeight: Int
        ) : MaxAltitudeState()

    }
}