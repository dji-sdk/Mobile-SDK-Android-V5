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

package dji.v5.ux.core.widget.battery

import androidx.annotation.IntRange
import dji.sdk.keyvalue.key.BatteryKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.battery.BatteryException
import dji.sdk.keyvalue.value.battery.BatteryOverviewValue
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.flightcontroller.FCBatteryThresholdBehavior
import dji.sdk.keyvalue.key.KeyTools
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.milliVoltsToVolts
import dji.v5.ux.core.util.DataProcessor
import java.util.ArrayList

private const val DUAL_BATTERY = 2
private val DEFAULT_ARRAY = arrayOf(0)
private const val DEFAULT_PERCENTAGE = 0

/**
 * Widget Model for [BatteryWidget] used to define the
 * underlying logic and communication
 */
class BatteryWidgetModel(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {


    private val batteryPercentageProcessor1 = DataProcessor.create(DEFAULT_PERCENTAGE)
    private val batteryPercentageProcessor2 = DataProcessor.create(DEFAULT_PERCENTAGE)
    private val batteryVoltageProcessor1: DataProcessor<List<Int>> = DataProcessor.create(ArrayList<Int>())
    private val batteryVoltageProcessor2: DataProcessor<List<Int>> = DataProcessor.create(ArrayList<Int>())
    private val batteryWarningRecordProcessor1 = DataProcessor.create(BatteryException())
    private val batteryWarningRecordProcessor2 = DataProcessor.create(BatteryException())

    private val batteryOverviewsProcessor: DataProcessor<List<BatteryOverviewValue>> = DataProcessor.create(ArrayList<BatteryOverviewValue>())
    private val batteryConnectedProcessor = DataProcessor.create(0)
    private val isAnyBatteryDisconnectedProcessor = DataProcessor.create(false)
    private val isCellDamagedDisconnectedProcessor = DataProcessor.create(false)
    private val isFirmwareDifferenceDetectedProcessor = DataProcessor.create(false)
    private val isVoltageDifferenceDetectedProcessor = DataProcessor.create(false)
    private val isLowCellVoltageDetectedProcessor = DataProcessor.create(false)

    private val batteryStateProcessor: DataProcessor<BatteryState> = DataProcessor.create(BatteryState.DisconnectedState)
    private val batteryThresholdBehaviorProcessor = DataProcessor.create(FCBatteryThresholdBehavior.UNKNOWN)
    private val batteryNeededToGoHomeProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val isAircraftFlyingDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)

    /**
     * Get the current state of the battery of the connected product
     */
    val batteryState: Flowable<BatteryState>
        get() = batteryStateProcessor.toFlowable()

    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyChargeRemainingInPercent, ComponentIndexType.LEFT_OR_MAIN), batteryPercentageProcessor1)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyCellVoltages, ComponentIndexType.LEFT_OR_MAIN), batteryVoltageProcessor1)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyBatteryException, ComponentIndexType.LEFT_OR_MAIN), batteryWarningRecordProcessor1)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyChargeRemainingInPercent, ComponentIndexType.RIGHT), batteryPercentageProcessor2)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyCellVoltages, ComponentIndexType.RIGHT), batteryVoltageProcessor2)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyBatteryException, ComponentIndexType.RIGHT), batteryWarningRecordProcessor2)

        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyNumberOfConnectedBatteries, ComponentIndexType.AGGREGATION), batteryConnectedProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyIsAnyBatteryDisconnected, ComponentIndexType.AGGREGATION), isAnyBatteryDisconnectedProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyIsCellDamaged, ComponentIndexType.AGGREGATION), isCellDamagedDisconnectedProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyIsFirmwareDifferenceDetected, ComponentIndexType.AGGREGATION), isFirmwareDifferenceDetectedProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyIsVoltageDifferenceDetected, ComponentIndexType.AGGREGATION), isVoltageDifferenceDetectedProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyIsLowCellVoltageDetected, ComponentIndexType.AGGREGATION), isLowCellVoltageDetectedProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyBatteryOverviews, ComponentIndexType.AGGREGATION), batteryOverviewsProcessor)

        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyBatteryThresholdBehavior), batteryThresholdBehaviorProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyBatteryPercentNeededToGoHome), batteryNeededToGoHomeProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyIsFlying), isAircraftFlyingDataProcessor)
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            when (batteryConnectedProcessor.value) {
                DUAL_BATTERY -> {
                    val battery1Voltage = calculateAverageVoltage(batteryVoltageProcessor1.value)
                    val battery2Voltage = calculateAverageVoltage(batteryVoltageProcessor2.value)
                    batteryStateProcessor.onNext(
                        BatteryState.DualBatteryState(
                            batteryPercentageProcessor1.value,
                            battery1Voltage,
                            calculateBatteryStatus(
                                batteryWarningRecordProcessor1.value,
                                batteryThresholdBehaviorProcessor.value,
                                batteryPercentageProcessor1.value,
                                batteryNeededToGoHomeProcessor.value,
                                isAircraftFlyingDataProcessor.value,
                                battery1Voltage
                            ),
                            batteryPercentageProcessor2.value,
                            battery2Voltage,
                            calculateBatteryStatus(
                                batteryWarningRecordProcessor2.value,
                                batteryThresholdBehaviorProcessor.value,
                                batteryPercentageProcessor2.value,
                                batteryNeededToGoHomeProcessor.value,
                                isAircraftFlyingDataProcessor.value,
                                battery2Voltage
                            )
                        )
                    )
                }
                else -> {
                    val voltage = calculateAverageVoltage(batteryVoltageProcessor1.value)
                    batteryStateProcessor.onNext(
                        BatteryState.SingleBatteryState(
                            batteryPercentageProcessor1.value,
                            voltage,
                            calculateBatteryStatus(
                                batteryWarningRecordProcessor1.value,
                                batteryThresholdBehaviorProcessor.value,
                                batteryPercentageProcessor1.value,
                                batteryNeededToGoHomeProcessor.value,
                                isAircraftFlyingDataProcessor.value,
                                voltage
                            )
                        )
                    )
                }

            }
        } else {
            batteryStateProcessor.onNext(BatteryState.DisconnectedState)
        }
    }


    override fun inCleanup() {
        // No Code
    }

    private fun calculateAverageVoltage(cellVoltages: List<Int>?): Float {
        return if (cellVoltages != null && cellVoltages.isNotEmpty()) {
            cellVoltages.average().toFloat().milliVoltsToVolts()
        } else 0f
    }

    private fun calculateBatteryStatus(
        batteryException: BatteryException,
        batteryThresholdBehavior: FCBatteryThresholdBehavior,
        percentage: Int,
        goHomeBattery: Int,
        isFlying: Boolean,
        voltage: Float
    ): BatteryStatus {
        if (percentage < 0 || voltage < 0f) {
            return BatteryStatus.UNKNOWN
        } else if (batteryException.firstLevelOverHeating || batteryException.secondLevelOverHeating) {
            return BatteryStatus.OVERHEATING
        } else if (batteryException.isError()) {
            return BatteryStatus.ERROR
        } else if (FCBatteryThresholdBehavior.LAND_IMMEDIATELY == batteryThresholdBehavior) {
            return BatteryStatus.WARNING_LEVEL_2
        } else if (FCBatteryThresholdBehavior.GO_HOME == batteryThresholdBehavior
            || (percentage <= goHomeBattery && isFlying)) {
            return BatteryStatus.WARNING_LEVEL_1
        }
        return BatteryStatus.NORMAL
    }

    fun BatteryException.isError(): Boolean {
        return this.firstLevelOverHeating || this.secondLevelOverHeating || this.communicationException
                || this.hasBrokenCell || this.hasLowVoltageCell || this.shortCircuited
                || this.firstLevelLowTemperature || this.secondLevelLowTemperature
    }

    /**
     * Class representing the current state of the battery
     * based on information received from the product
     */
    sealed class BatteryState {
        /**
         * Product is currently disconnected
         */
        object DisconnectedState : BatteryState()

        /**
         * Product with single battery is connected. The status includes
         *
         * @property percentageRemaining - battery remaining in percentage
         * @property voltageLevel - voltage level of the battery
         * @property batteryStatus - [BatteryStatus] instance representing the battery
         */
        data class SingleBatteryState(
            val percentageRemaining: Int,
            val voltageLevel: Float,
            val batteryStatus: BatteryStatus
        ) : BatteryState()

        /**
         * Product with dual battery is connected. The status includes
         *
         * @property percentageRemaining1 - battery remaining in percentage of battery 1
         * @property voltageLevel1 - voltage level of the battery 1
         * @property batteryStatus1 - [BatteryStatus] instance representing the battery 1
         * @property percentageRemaining2 - battery remaining in percentage of battery 2
         * @property voltageLevel2 - voltage level of the battery 2
         * @property batteryStatus2 - [BatteryStatus] instance representing the battery 2
         */
        data class DualBatteryState(
            val percentageRemaining1: Int,
            val voltageLevel1: Float,
            val batteryStatus1: BatteryStatus,
            val percentageRemaining2: Int,
            val voltageLevel2: Float,
            val batteryStatus2: BatteryStatus
        ) : BatteryState()

        /**
         * Product with more than 2 batteries is connected. The status includes
         *
         * @property aggregatePercentage - aggregate percentage remaining from all batteries
         * @property aggregateVoltage - aggregate voltage level of all batteries
         * @property aggregateBatteryStatus - [BatteryStatus] instance representing the aggregate status
         */
        data class AggregateBatteryState(
            val aggregatePercentage: Int,
            val aggregateVoltage: Float,
            val aggregateBatteryStatus: BatteryStatus
        ) : BatteryState()
    }

    //endregion


    /**
     * Enum representing the state of each battery in the battery bank
     */
    enum class BatteryStatus constructor(val index: Int) {
        /**
         * Battery is operating without issue
         */
        NORMAL(0),

        /**
         * Battery charge is starting to get low, to the point that the aircraft should return home
         */
        WARNING_LEVEL_1(1),

        /**
         * Battery charge is starting to get very low, to the point that the aircraft should
         * land immediately.
         */
        WARNING_LEVEL_2(2),

        /**
         * Battery has an error that is preventing a proper reading
         */
        ERROR(3),

        /**
         * Battery temperature is too high
         */
        OVERHEATING(4),

        /**
         * The state of the battery is unknown or the system is initializing
         */
        UNKNOWN(5);

        companion object {
            @JvmStatic
            val values = values()

            @JvmStatic
            fun find(@IntRange(from = 0, to = 5) index: Int): BatteryStatus {
                return values.find { it.index == index } ?: UNKNOWN
            }
        }
    }


}