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

package dji.v5.ux.core.panel.listitem.rcbattery

import dji.sdk.keyvalue.key.RemoteControllerKey
import dji.sdk.keyvalue.value.remotecontroller.BatteryInfo
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.panel.listitem.rcbattery.RCBatteryListItemWidgetModel.RCBatteryState.RCDisconnected
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.Flowable

/**
 * Widget Model for the [RCBatteryListItemWidget] used to define
 * the underlying logic and communication
 */

class RCBatteryListItemWidgetModel(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields

    private val rcBatteryLevelProcessor: DataProcessor<BatteryInfo> = DataProcessor.create(BatteryInfo())
    private val rcBatteryStateProcessor: DataProcessor<RCBatteryState> = DataProcessor.create(RCDisconnected)
    private val rcConnectionProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    //endregion

    //region Data
    /**
     * Get the RC battery state
     */
    val rcBatteryState: Flowable<RCBatteryState>
        get() = rcBatteryStateProcessor.toFlowable()

    //endregion

    //region Lifecycle
    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                RemoteControllerKey.KeyConnection), rcConnectionProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                RemoteControllerKey.KeyBatteryInfo), rcBatteryLevelProcessor)
    }

    override fun inCleanup() {
        // No Clean up
    }

    override fun updateStates() {
        val rcBatteryLevelPercent = rcBatteryLevelProcessor.value.batteryPercent
        if (rcConnectionProcessor.value && rcBatteryLevelPercent < 30) {
            rcBatteryStateProcessor.onNext(RCBatteryState.Low(rcBatteryLevelPercent))
        } else if (rcConnectionProcessor.value) {
            rcBatteryStateProcessor.onNext(RCBatteryState.Normal(rcBatteryLevelPercent))
        } else {
            rcBatteryStateProcessor.onNext(RCDisconnected)
        }
    }

    /**
     * Class to represent states of RCBatteryListItem
     */
    sealed class RCBatteryState {
        /**
         * When remote controller is disconnected
         */
        object RCDisconnected : RCBatteryState()

        /**
         * When product is connected and rc battery is normal
         */
        data class Normal(val remainingChargePercent: Int) : RCBatteryState()

        /**
         * When product is connected and rc battery is critically low
         */
        data class Low(val remainingChargePercent: Int) : RCBatteryState()
    }
    //endregion
}
