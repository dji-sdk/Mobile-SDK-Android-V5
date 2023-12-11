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

package dji.v5.ux.core.widget.remotecontrollersignal

import dji.sdk.keyvalue.key.AirLinkKey
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.utils.common.LogUtils
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor

/**
 * Widget Model for the [RemoteControllerSignalWidget] used to define
 * the underlying logic and communication
 */
class RemoteControllerSignalWidgetModel(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {
    private val tag = LogUtils.getTag("RemoteControllerSignalWidgetModel")
    private val upLinkQualityRawProcessor: DataProcessor<Int> = DataProcessor.create(0)

    /**
     * Get the value of the strength of the signal between the RC and the aircraft.
     */
    val rcSignalQuality: Flowable<Int>
        get() = upLinkQualityRawProcessor.toFlowable()
    //endregion

    //region Lifecycle
    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                AirLinkKey.KeyUpLinkQualityRaw), upLinkQualityRawProcessor)
    }

    override fun inCleanup() {
        // Nothing to clean
    }

    override fun updateStates() {
        //LogUtils.d(tag,"TODO Method not implemented yet")
    }
}
