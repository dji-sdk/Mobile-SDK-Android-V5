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

package dji.v5.ux.core.panel.listitem.returntohomealtitude

import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.IntValueConfig
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.GlobalPreferenceKeys
import dji.v5.ux.core.communication.GlobalPreferencesInterface
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.panel.listitem.returntohomealtitude.ReturnToHomeAltitudeListItemWidgetModel.ReturnToHomeAltitudeState.*
import dji.v5.ux.core.util.DataProcessor
import dji.v5.ux.core.util.UnitConversionUtil.*
import io.reactivex.rxjava3.core.Completable
import kotlin.math.roundToInt

private const val MIN_LIMIT = 20
private const val MAX_LIMIT = 500

/**
 * Widget Model for the [ReturnToHomeAltitudeListItemWidget] used to define
 * the underlying logic and communication
 */
class ReturnToHomeAltitudeListItemWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore,
        private val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {

    private val maxFlightAltitudeProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val returnToHomeAltitudeProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val unitTypeProcessor: DataProcessor<UnitType> = DataProcessor.create(UnitType.METRIC)
    private val returnToHomeAltitudeStateProcessor: DataProcessor<ReturnToHomeAltitudeState> = DataProcessor.create(ProductDisconnected)
    private val noviceModeProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val maxFlightHeightRangeProcessor: DataProcessor<IntValueConfig> = DataProcessor.create(IntValueConfig())

    /**
     * Get the return to home altitude state
     */
    val returnToHomeAltitudeState: Flowable<ReturnToHomeAltitudeState>
        get() = returnToHomeAltitudeStateProcessor.toFlowable()

    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyGoHomeHeight), returnToHomeAltitudeProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyHeightLimit), maxFlightAltitudeProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyHeightLimitRange), maxFlightHeightRangeProcessor)
        val unitTypeKey = GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE)
        bindDataProcessor(unitTypeKey, unitTypeProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyNoviceModeEnabled), noviceModeProcessor)
        preferencesManager?.setUpListener()
        preferencesManager?.let { unitTypeProcessor.onNext(it.unitType) }
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (noviceModeProcessor.value) {
                returnToHomeAltitudeStateProcessor.onNext(NoviceMode(unitTypeProcessor.value))
            } else {
                returnToHomeAltitudeStateProcessor.onNext(
                        ReturnToHomeAltitudeValue(returnToHomeAltitude = getReturnToHomeAltitudeValue(),
                                minLimit = getMinLimit(),
                                maxLimit = getMaxLimit(),
                                unitType = unitTypeProcessor.value,
                                maxFlightAltitude = getMaxAltitudeLimitByUnit()))
            }
        } else {
            returnToHomeAltitudeStateProcessor.onNext(ProductDisconnected)
        }
    }

    private fun getMaxAltitudeLimitByUnit(): Int {
        return if (unitTypeProcessor.value == UnitType.IMPERIAL) {
            convertMetersToFeet(maxFlightAltitudeProcessor.value.toFloat()).roundToInt()
        } else {
            maxFlightAltitudeProcessor.value
        }
    }


    override fun inCleanup() {
        preferencesManager?.cleanup()
    }

    private fun getReturnToHomeAltitudeValue(): Int {
        return if (unitTypeProcessor.value == UnitType.IMPERIAL) {
            convertMetersToFeet(returnToHomeAltitudeProcessor.value.toFloat()).roundToInt()
        } else {
            returnToHomeAltitudeProcessor.value
        }
    }

    private fun getMinLimit(): Int {
        val tempMinValue: Int = maxFlightHeightRangeProcessor.value.min
       return if (unitTypeProcessor.value == UnitType.METRIC) {
            tempMinValue
        } else {
            convertMetersToFeet(tempMinValue.toFloat()).roundToInt()
        }
    }

    private fun getMaxLimit(): Int {
        val tempMaxValue: Int = maxFlightHeightRangeProcessor.value.max
        return if (unitTypeProcessor.value == UnitType.METRIC) {
            tempMaxValue
        } else {
            convertMetersToFeet(tempMaxValue.toFloat()).roundToInt()
        }
    }

    /**
     * Set return to home altitude
     *
     * @return Completable to determine status of action
     */
    fun setReturnToHomeAltitude(returnToHomeAltitude: Int): Completable {
        val tempAltitude: Int = if (unitTypeProcessor.value == UnitType.IMPERIAL) {
            convertFeetToMeters(returnToHomeAltitude.toFloat()).toInt()
        } else {
            returnToHomeAltitude
        }
        return djiSdkModel.setValue(
            KeyTools.createKey(
                FlightControllerKey.KeyGoHomeHeight), tempAltitude)
    }

    /**
     * Check if input is in range
     *
     * @return Boolean
     * true - if the input is in range
     * false - if the input is out of range
     */
    fun isInputInRange(input: Int): Boolean = input >= getMinLimit() && input <= getMaxLimit()

    /**
     * Class represents states of Return To Home Altitude List Item
     */
    sealed class ReturnToHomeAltitudeState {
        /**
         * When product is disconnected
         */
        object ProductDisconnected : ReturnToHomeAltitudeState()

        /**
         * When product is in beginner mode
         * @property unitType - current unit system used
         */
        data class NoviceMode(val unitType: UnitType) : ReturnToHomeAltitudeState()


        /**
         * Return to home value and range
         * along with unit
         * @property returnToHomeAltitude - Return to home altitude
         * @property minLimit - Minimum limit of return to home altitude
         * @property maxLimit - Maximum limit of return to home altitude
         * @property unitType - Unit of values
         * @property maxFlightAltitude - Maximum permitted flight altitude.
         */
        data class ReturnToHomeAltitudeValue(val returnToHomeAltitude: Int,
                                             val minLimit: Int,
                                             val maxLimit: Int,
                                             val unitType: UnitType,
                                             val maxFlightAltitude: Int) : ReturnToHomeAltitudeState()
    }

}