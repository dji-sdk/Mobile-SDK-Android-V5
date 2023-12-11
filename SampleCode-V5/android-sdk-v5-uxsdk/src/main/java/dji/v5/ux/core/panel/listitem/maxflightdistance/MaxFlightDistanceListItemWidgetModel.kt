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

package dji.v5.ux.core.panel.listitem.maxflightdistance

import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.IntValueConfig
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.GlobalPreferenceKeys
import dji.v5.ux.core.communication.GlobalPreferencesInterface
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.panel.listitem.maxflightdistance.MaxFlightDistanceListItemWidgetModel.MaxFlightDistanceState.*
import dji.v5.ux.core.util.DataProcessor
import dji.v5.ux.core.util.UnitConversionUtil.*
import io.reactivex.rxjava3.core.Completable
import kotlin.math.roundToInt

/**
 * Widget Model for the [MaxFlightDistanceListItemWidget] used to define
 * the underlying logic and communication
 */
class MaxFlightDistanceListItemWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore,
        private val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {

    private val maxFlightDistanceEnabledProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val maxFlightDistanceProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val maxFlightDistanceRangeProcessor: DataProcessor<IntValueConfig> = DataProcessor.create(IntValueConfig())
    private val unitTypeProcessor: DataProcessor<UnitType> = DataProcessor.create(UnitType.METRIC)
    private val maxFlightDistanceStateProcessor: DataProcessor<MaxFlightDistanceState> = DataProcessor.create(ProductDisconnected)
    private val noviceModeProcessor: DataProcessor<Boolean> = DataProcessor.create(false)

    /**
     * Get the max flight distance state
     */
    val maxFlightDistanceState: Flowable<MaxFlightDistanceState>
        get() = maxFlightDistanceStateProcessor.toFlowable()

    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyDistanceLimitEnabled), maxFlightDistanceEnabledProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyDistanceLimit), maxFlightDistanceProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyDistanceLimitRange), maxFlightDistanceRangeProcessor)
        bindDataProcessor(GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE), unitTypeProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyNoviceModeEnabled), noviceModeProcessor)
        preferencesManager?.setUpListener()
        preferencesManager?.let { unitTypeProcessor.onNext(it.unitType) }
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            when {
                noviceModeProcessor.value -> {
                    maxFlightDistanceStateProcessor.onNext(NoviceMode(unitTypeProcessor.value))
                }
                maxFlightDistanceEnabledProcessor.value -> {
                    maxFlightDistanceStateProcessor.onNext(MaxFlightDistanceValue(
                            flightDistanceLimit = getMaxFlightDistanceValue(),
                            minDistanceLimit = getMinLimit(),
                            maxDistanceLimit = getMaxLimit(),
                            unitType = unitTypeProcessor.value))

                }
                else -> {
                    maxFlightDistanceStateProcessor.onNext(Disabled)
                }
            }
        } else {
            maxFlightDistanceStateProcessor.onNext(ProductDisconnected)
        }
    }

    override fun inCleanup() {
        preferencesManager?.cleanup()
    }

    private fun getMaxFlightDistanceValue(): Int {
        return if (unitTypeProcessor.value == UnitType.IMPERIAL) {
            convertMetersToFeet(maxFlightDistanceProcessor.value.toFloat()).roundToInt()
        } else {
            maxFlightDistanceProcessor.value
        }
    }

    private fun getMinLimit(): Int {
        return if (unitTypeProcessor.value == UnitType.IMPERIAL) {
            convertMetersToFeet(maxFlightDistanceRangeProcessor.value.min.toFloat()).roundToInt()
        } else {
            maxFlightDistanceRangeProcessor.value.min.toInt()
        }
    }

    private fun getMaxLimit(): Int {
        return if (unitTypeProcessor.value == UnitType.IMPERIAL) {
            convertMetersToFeet(maxFlightDistanceRangeProcessor.value.max.toFloat()).roundToInt()
        } else {
            maxFlightDistanceRangeProcessor.value.max.toInt()
        }
    }

    /**
     * Enable or disable max flight distance
     *
     * @return Completable to determine status of action
     */
    fun toggleFlightDistanceAvailability(): Completable {
        return djiSdkModel.setValue(
            KeyTools.createKey(
                FlightControllerKey.KeyDistanceLimitEnabled), !maxFlightDistanceEnabledProcessor.value)
    }

    /**
     * Set max flight distance
     *
     * @return Completable to determine status of action
     */
    fun setMaxFlightDistance(flightDistance: Int): Completable {
        val tempFlightDistance: Int = if (unitTypeProcessor.value == UnitType.IMPERIAL) {
            convertFeetToMeters(flightDistance.toFloat()).toInt()
        } else {
            flightDistance
        }
        return djiSdkModel.setValue(
            KeyTools.createKey(
                FlightControllerKey.KeyDistanceLimit), tempFlightDistance)

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
     * Class represents states of Max Flight Distance State
     */
    sealed class MaxFlightDistanceState {
        /**
         * When product is disconnected
         */
        object ProductDisconnected : MaxFlightDistanceState()

        /**
         * When max flight distance limit is disabled
         */
        object Disabled : MaxFlightDistanceState()

        /**
         * When product is in beginner mode
         * @property unitType - current unit system used
         */
        data class NoviceMode(val unitType: UnitType) : MaxFlightDistanceState()

        /**
         * Flight distance value with unit
         *
         * @property flightDistanceLimit - current flight distance limit
         * @property minDistanceLimit - flight distance limit range minimum
         * @property maxDistanceLimit - flight distance limit range maximum
         * @property unitType - current unit system used
         */
        data class MaxFlightDistanceValue(val flightDistanceLimit: Int,
                                          val minDistanceLimit: Int,
                                          val maxDistanceLimit: Int,
                                          val unitType: UnitType) : MaxFlightDistanceState()

    }

}