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

package dji.v5.ux.core.panel.listitem.rcstickmode

import dji.sdk.keyvalue.key.RemoteControllerKey
import dji.sdk.keyvalue.value.remotecontroller.ControlMode
import dji.v5.et.create
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor
import dji.v5.ux.core.util.RxUtil
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single

private const val TAG = "RCStickModeListItemWidgetModel"

/**
 * Widget Model for the [RCStickModeListItemWidget] used to define
 * the underlying logic and communication
 */
class RCStickModeListItemWidgetModel(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    private val rcStickModeStateProcessor: DataProcessor<RCStickModeState> = DataProcessor.create(RCStickModeState.ProductDisconnected)
    private val controlModeProcessor: DataProcessor<ControlMode> = DataProcessor.create(ControlMode.UNKNOWN)

    /**
     * Get the current rc stick list item state
     */
    val rcStickModeState: Flowable<RCStickModeState> = rcStickModeStateProcessor.toFlowable()

    /**
     * Set control stick mode to RC
     *
     * @param mode - state representing stick mode to be set to RC
     * @return Completable representing the action
     */
    fun setControlStickMode(mode: ControlMode): Completable {
        return djiSdkModel.setValue(RemoteControllerKey.KeyControlMode.create(), mode)
    }

    override fun inSetup() {
        bindDataProcessor(RemoteControllerKey.KeyControlMode.create(), controlModeProcessor) {
            updateCurrentStickMode(it)
        }
    }

    override fun inCleanup() {
        // No clean up needed
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            addDisposable(
                getControlStickMode()
                    .subscribe(
                        { updateCurrentStickMode(it) },
                        RxUtil.logErrorConsumer(TAG, "getMappingStyle ")
                    )
            )
        } else {
            rcStickModeStateProcessor.onNext(RCStickModeState.ProductDisconnected)
        }
    }

    private fun updateCurrentStickMode(mode: ControlMode) {
        when (mode) {
            ControlMode.JP -> rcStickModeStateProcessor.onNext(RCStickModeState.JP)
            ControlMode.USA -> rcStickModeStateProcessor.onNext(RCStickModeState.USA)
            ControlMode.CH -> rcStickModeStateProcessor.onNext(RCStickModeState.CH)
            ControlMode.CUSTOM -> rcStickModeStateProcessor.onNext(RCStickModeState.Custom)
            ControlMode.UNKNOWN -> rcStickModeStateProcessor.onNext(RCStickModeState.ProductDisconnected)
        }
    }

    private fun getControlStickMode(): Single<ControlMode> {
        return djiSdkModel.getValue(RemoteControllerKey.KeyControlMode.create())
    }

    /**
     * Class representing states for RCStickMode
     */
    sealed class RCStickModeState {
        /**
         * When product is disconnected
         */
        object ProductDisconnected : RCStickModeState()

        /**
         * When product is connected and stick mode is JP
         */
        object JP : RCStickModeState()

        /**
         * When product is connected and stick mode is USA
         */
        object USA : RCStickModeState()

        /**
         * When product is connected and stick mode is CH
         */
        object CH : RCStickModeState()

        /**
         * When product is connected and stick mode is custom
         */
        object Custom : RCStickModeState()
    }

}