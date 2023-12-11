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

package dji.v5.ux.core.panel.listitem.novicemode

import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.panel.listitem.novicemode.NoviceModeListItemWidgetModel.NoviceModeState.ProductDisconnected
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.Completable

/**
 * Widget Model for the [NoviceModeListItemWidget] used to define
 * the underlying logic and communication
 */
class NoviceModeListItemWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    private val noviceModeDataProcessor = DataProcessor.create(false)
    private val noviceModeStateDataProcessor: DataProcessor<NoviceModeState> = DataProcessor.create(ProductDisconnected)

    /**
     * Get the novice mode state
     */
    val noviceModeState: Flowable<NoviceModeState>
        get() = noviceModeStateDataProcessor.toFlowable()


    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyNoviceModeEnabled), noviceModeDataProcessor)
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (noviceModeDataProcessor.value) {
                noviceModeStateDataProcessor.onNext(NoviceModeState.Enabled)
            } else {
                noviceModeStateDataProcessor.onNext(NoviceModeState.Disabled)
            }
        } else {
            noviceModeStateDataProcessor.onNext(ProductDisconnected)
        }
    }

    override fun inCleanup() {
        // no clean up required
    }

    /**
     * Toggle novice mode on/off
     */
    fun toggleNoviceMode(): Completable {
        return djiSdkModel.setValue(
            KeyTools.createKey(
                FlightControllerKey.KeyNoviceModeEnabled), !noviceModeDataProcessor.value)
    }

    /**
     * Class represents states of Novice Mode Item
     */
    sealed class NoviceModeState {
        /**
         * When product is disconnected
         */
        object ProductDisconnected : NoviceModeState()

        /**
         * When novice (beginner) mode is enabled
         */
        object Enabled : NoviceModeState()

        /**
         * When novice (beginner) mode is disabled
         */
        object Disabled : NoviceModeState()
    }
}