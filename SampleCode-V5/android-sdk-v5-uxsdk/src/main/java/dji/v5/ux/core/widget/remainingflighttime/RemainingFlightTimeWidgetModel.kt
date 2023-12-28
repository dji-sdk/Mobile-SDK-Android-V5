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

package dji.v5.ux.core.widget.remainingflighttime

import dji.sdk.keyvalue.key.BatteryKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.flightcontroller.LowBatteryRTHInfo
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.Flowable

/**
 * Remaining Flight Time Widget Model
 *
 *
 * Widget Model for the [RemainingFlightTimeWidget] used to define the
 * underlying logic and communication
 */
class RemainingFlightTimeWidgetModel(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {
    //region Fields
    private val chargeRemainingProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val goHomeAssessmentProcessor = DataProcessor.create(LowBatteryRTHInfo())

    private val seriousLowBatteryThresholdProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val lowBatteryThresholdProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val remainingFlightProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val remainingFlightTimeDataProcessor: DataProcessor<RemainingFlightTimeData> =
        DataProcessor.create(
            RemainingFlightTimeData(
                0, 0,
                0, 0, 0, 0
            )
        )
    private val isAircraftFlyingDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)

    //endregi\

    override fun inSetup() {
        // For total percentage and flight time

        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyChargeRemainingInPercent), chargeRemainingProcessor)

        // For red bar batteryPercentNeededToLand
        // For H image and yellow bar batteryPercentNeededToGoHome
        // For flight time text remainingFlightTime
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyLowBatteryRTHInfo), goHomeAssessmentProcessor)

        // For white dot on the left
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeySeriousLowBatteryWarningThreshold), seriousLowBatteryThresholdProcessor)

        // For white dot on the right
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyLowBatteryWarningThreshold), lowBatteryThresholdProcessor)

        // To check if aircraft is flying
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyIsFlying), isAircraftFlyingDataProcessor)
    }

    override fun inCleanup() { // No Clean up required
    }

    override fun updateStates() {
        val remainingFlightTimeData = RemainingFlightTimeData(
            chargeRemainingProcessor.value,
            goHomeAssessmentProcessor.value.batteryPercentNeededToLand,
            goHomeAssessmentProcessor.value.batteryPercentNeededToGoHome,
            seriousLowBatteryThresholdProcessor.value,
            lowBatteryThresholdProcessor.value,
            goHomeAssessmentProcessor.value.remainingFlightTime
        )
        remainingFlightTimeDataProcessor.onNext(remainingFlightTimeData)
    }

    //region Data
    /**
     * Get the latest data for remaining flight based on battery level
     */
    val remainingFlightTimeData: Flowable<RemainingFlightTimeData>
        get() = remainingFlightTimeDataProcessor.toFlowable()

    /**
     * Check to see if aircraft is flying
     */
    val isAircraftFlying: Flowable<Boolean>
        get() = isAircraftFlyingDataProcessor.toFlowable()

    //endregion
    /**
     * Class representing data for remaining flight time
     */
    data class RemainingFlightTimeData(
        /**
         * Remaining battery charge in percent
         */
        val remainingCharge: Int,

        /**
         * Battery charge required to land
         */
        val batteryNeededToLand: Int,

        /**
         * Battery charge needed to go home
         */
        val batteryNeededToGoHome: Int,

        /**
         * Serious low battery level threshold
         */
        val seriousLowBatteryThreshold: Int,

        /**
         * Low battery level threshold
         */
        val lowBatteryThreshold: Int,

        /**
         * Flight time in micro seconds
         */
        val flightTime: Int
    )

}