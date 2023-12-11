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

package dji.v5.ux.core.widget.airsense

import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.flightcontroller.AirSenseSystemInformation
import dji.sdk.keyvalue.value.flightcontroller.AirSenseWarningLevel
import dji.sdk.keyvalue.key.KeyTools
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.MessagingKeys
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.communication.UXKey
import dji.v5.ux.core.communication.UXKeys
import dji.v5.ux.core.model.WarningMessage
import dji.v5.ux.core.model.WarningMessageError
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.Completable

/**
 * Widget Model for the [AirSenseWidget] used to define
 * the underlying logic and communication
 */
class AirSenseWidgetModel @JvmOverloads constructor(
        djiSdkModel: DJISDKModel,
        private val keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    private val airSenseConnectedProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val airSenseSystemInformationProcessor : DataProcessor<AirSenseSystemInformation> = DataProcessor.create(AirSenseSystemInformation())
    private val airSenseWarningLevelProcessor: DataProcessor<AirSenseWarningLevel> = DataProcessor.create(AirSenseWarningLevel.UNKNOWN)
    private val sendWarningMessageKey: UXKey = UXKeys.create(MessagingKeys.SEND_WARNING_MESSAGE)
    private val airSenseStateProcessor: DataProcessor<AirSenseState> = DataProcessor.create(AirSenseState.DISCONNECTED)

    //region Data
    /**
     * Get the AirSense warning level.
     */
    val airSenseWarningLevel: Flowable<AirSenseWarningLevel>
        get() = airSenseWarningLevelProcessor.toFlowable().distinctUntilChanged()

    /**
     * Get the number of airplanes detected by AirSense
     */
    val airSenseState: Flowable<AirSenseState>
        get() = airSenseStateProcessor.toFlowable()
    //endregion

    //region Actions
    /**
     * Send two warning messages with the given solutions for warning and dangerous levels. Based
     * on the warning level, only one message at a time will be displayed, and the other will be
     * removed.
     *
     * @param reason The reason to display on the warning message.
     * @param warningSolution The solution to display if the level is [WarningMessage.Level.WARNING]
     * @param dangerousSolution The solution to display if the level is [WarningMessage.Level.DANGEROUS]
     * @param warningLevel The current AirSense warning level.
     * @return Completable representing the success/failure of the set action.
     */
    fun sendWarningMessages(reason: String?, warningSolution: String?, dangerousSolution: String?, warningLevel: AirSenseWarningLevel): Completable {
        return when (warningLevel) {
            AirSenseWarningLevel.LEVEL_2 -> sendWarningMessage(reason, warningSolution, WarningMessage.Level.WARNING, WarningMessage.Action.INSERT)
                    .andThen(sendWarningMessage(reason, dangerousSolution, WarningMessage.Level.DANGEROUS, WarningMessage.Action.REMOVE))
            AirSenseWarningLevel.LEVEL_3, AirSenseWarningLevel.LEVEL_4 -> sendWarningMessage(reason, warningSolution, WarningMessage.Level.WARNING, WarningMessage.Action.REMOVE)
                    .andThen(sendWarningMessage(reason, dangerousSolution, WarningMessage.Level.DANGEROUS, WarningMessage.Action.INSERT))
            else -> sendWarningMessage(reason, warningSolution, WarningMessage.Level.WARNING, WarningMessage.Action.REMOVE)
                    .andThen(sendWarningMessage(reason, dangerousSolution, WarningMessage.Level.DANGEROUS, WarningMessage.Action.REMOVE))
        }
    }

    private fun sendWarningMessage(reason: String?, solution: String?, level: WarningMessage.Level?, action: WarningMessage.Action?): Completable {
        val builder = WarningMessage.Builder(WarningMessage.WarningType.FLY_SAFE)
                .code(-1)
                .subCode(WarningMessageError.OTHER_AIRCRAFT_NEARBY.value())
                .reason(reason)
                .solution(solution)
                .level(level)
                .type(WarningMessage.Type.PINNED)
                .action(action)
        val warningMessage = builder.build()
        return keyedStore.setValue(sendWarningMessageKey, warningMessage)
    }

    //endregion

    //region Lifecycle
    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyAirSenseSystemConnected), airSenseConnectedProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyAirSenseSystemInformation), airSenseSystemInformationProcessor){
            airSenseWarningLevelProcessor.onNext(it.warningLevel)
        }
    }

    override fun inCleanup() {
        // do nothing
    }

    override fun updateStates() {
        airSenseStateProcessor.onNext(
                if (!productConnectionProcessor.value) {
                    AirSenseState.DISCONNECTED
                } else if (!airSenseConnectedProcessor.value) {
                    AirSenseState.NO_AIR_SENSE_CONNECTED
                } else if (airSenseSystemInformationProcessor.value.airplaneStates.isEmpty()) {
                    AirSenseState.NO_AIRPLANES_NEARBY
                } else {
                    when (airSenseSystemInformationProcessor.value.warningLevel) {
                        AirSenseWarningLevel.LEVEL_0 -> AirSenseState.WARNING_LEVEL_0
                        AirSenseWarningLevel.LEVEL_1 -> AirSenseState.WARNING_LEVEL_1
                        AirSenseWarningLevel.LEVEL_2 -> AirSenseState.WARNING_LEVEL_2
                        AirSenseWarningLevel.LEVEL_3 -> AirSenseState.WARNING_LEVEL_3
                        AirSenseWarningLevel.LEVEL_4 -> AirSenseState.WARNING_LEVEL_4
                        else -> AirSenseState.UNKNOWN
                    }
                }
        )
    }
    //endregion

    //region States
    /**
     * The status of the AirSense system.
     */
    enum class AirSenseState {
        /**
         * There is no product connected.
         */
        DISCONNECTED,

        /**
         * The connected product does not have DJI AirSense.
         */
        NO_AIR_SENSE_CONNECTED,

        /**
         * A product that has DJI AirSense is connected and no airplanes are nearby.
         */
        NO_AIRPLANES_NEARBY,

        /**
         * A product that has DJI AirSense is connected and the system detects an airplane but the
         * DJI aircraft is either far away from the airplane or is in the opposite direction of the
         * airplane's heading.
         */
        WARNING_LEVEL_0,

        /**
         * A product that has DJI AirSense is connected and the system detects an airplane. The
         * probability that it will pass through the location of the DJI aircraft is considered
         * low.
         */
        WARNING_LEVEL_1,

        /**
         * A product that has DJI AirSense is connected and the system detects an airplane. The
         * probability that it will pass through the location of the DJI aircraft is considered
         * medium.
         */
        WARNING_LEVEL_2,

        /**
         * A product that has DJI AirSense is connected and the system detects an airplane. The
         * probability that it will pass through the location of the DJI aircraft is considered
         * high.
         */
        WARNING_LEVEL_3,

        /**
         * A product that has DJI AirSense is connected and the system detects an airplane. The
         * probability that it will pass through the location of the DJI aircraft is very high.
         */
        WARNING_LEVEL_4,

        /**
         * Unknown.
         */
        UNKNOWN
    }
    //endregion
}