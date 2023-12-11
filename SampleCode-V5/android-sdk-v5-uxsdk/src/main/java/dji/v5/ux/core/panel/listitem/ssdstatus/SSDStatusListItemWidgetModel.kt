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

package dji.v5.ux.core.panel.listitem.ssdstatus

import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.value.camera.SSDOperationState
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.ComponentIndexType
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.Completable

/**
 * Widget Model for the [SSDStatusListItemWidget] used to define
 * the underlying logic and communication
 */
class SSDStatusListItemWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    private val ssdStateProcessor: DataProcessor<SSDState> = DataProcessor.create(SSDState.ProductDisconnected)
    private val ssdRemainingCapacityProcessor = DataProcessor.create(0)
    private val ssdOperationStateProcessor = DataProcessor.create(SSDOperationState.UNKNOWN)
    private val ssdSupportedProcessor = DataProcessor.create(false)

    /**
     * Index of sdCard
     */
    var cameraIndex = ComponentIndexType.LEFT_OR_MAIN
        set(value) {
            field = value
            restart()
        }

    /**
     * Get the sd card state
     */
    val ssdState: Flowable<SSDState> = ssdStateProcessor.toFlowable()

    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                CameraKey.KeyIsInternalSSDSupported, cameraIndex), ssdSupportedProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                CameraKey.KeySSDRemainingSpaceInMB, cameraIndex), ssdRemainingCapacityProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                CameraKey.KeySSDOperationState, cameraIndex), ssdOperationStateProcessor)
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (ssdSupportedProcessor.value) {
                ssdStateProcessor.onNext(SSDState.CurrentSSDState(ssdOperationStateProcessor.value,
                        ssdRemainingCapacityProcessor.value))
            } else {
                ssdStateProcessor.onNext(SSDState.NotSupported)
            }
        } else {
            ssdStateProcessor.onNext(SSDState.ProductDisconnected)
        }
    }

    override fun inCleanup() {
        //No clean up necessary
    }

    /**
     * Format SSD
     */
    fun formatSSD(): Completable {
        return djiSdkModel.performActionWithOutResult(
            KeyTools.createKey(
                CameraKey.KeyFormatStorage, cameraIndex))
    }


    /**
     * Class represents states of SSD List Item
     */
    sealed class SSDState {
        /**
         * When product is disconnected
         */
        object ProductDisconnected : SSDState()

        /**
         * When product does not support SSD
         */
        object NotSupported : SSDState()

        /**
         * When product is connected
         *
         * @property ssdOperationState - Current operation State of SSD
         * @property remainingSpace - Remaining space in MB
         */
        data class CurrentSSDState(val ssdOperationState: SSDOperationState,
                                   val remainingSpace: Int) : SSDState()

    }
}