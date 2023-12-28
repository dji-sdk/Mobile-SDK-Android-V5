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

package dji.v5.ux.core.panel.listitem.overview

import dji.v5.manager.diagnostic.DJIDeviceStatus
import dji.v5.manager.diagnostic.DJIDeviceStatusChangeListener
import dji.v5.manager.diagnostic.DeviceStatusManager
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.panel.listitem.overview.OverviewListItemWidgetModel.OverviewState.CurrentStatus
import dji.v5.ux.core.util.DataProcessor

/**
 * Widget Model for the [OverviewListItemWidget] used to define
 * the underlying logic and communication
 */
class OverviewListItemWidgetModel(
        djiSdkModel: DJISDKModel,
        keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    private val systemStatusProcessor: DataProcessor<DJIDeviceStatus> = DataProcessor.create(
        DJIDeviceStatus.NORMAL)
    private val overviewStateProcessor: DataProcessor<OverviewState> = DataProcessor.create(OverviewState.ProductDisconnected)

    private val deviceStatusChangeListener = DJIDeviceStatusChangeListener { _, to ->
        systemStatusProcessor.onNext(to)
        updateStates()
    }

    /**
     * Get the overview status
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    val overviewStatus: Flowable<OverviewState>
        get() = overviewStateProcessor.toFlowable()

    override fun inSetup() {
        DeviceStatusManager.getInstance().addDJIDeviceStatusChangeListener(deviceStatusChangeListener)
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            overviewStateProcessor.onNext(CurrentStatus(systemStatusProcessor.value))
        } else {
            overviewStateProcessor.onNext(OverviewState.ProductDisconnected)
        }
    }

    override fun inCleanup() {
        DeviceStatusManager.getInstance().removeDJIDeviceStatusChangeListener(deviceStatusChangeListener)
    }

    sealed class OverviewState {

        object ProductDisconnected : OverviewState()

        data class CurrentStatus(val warningStatusItem: DJIDeviceStatus) : OverviewState()
    }


}