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

package dji.v5.ux.accessory

import dji.sdk.keyvalue.key.DJIKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.key.RtkMobileStationKey
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.Flowable

private const val TAG = "RTKWidgetModel"

/**
 * Widget Model for the [RTKWidget] used to define
 * the underlying logic and communication
 */
class RTKWidgetModel(
    djiSdkModel: DJISDKModel,
    uxKeyManager: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, uxKeyManager) {

    //region Fields
    private val rtkEnabledProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    //endregion

    //region Data
    /**
     * Get whether the RTK is enabled.
     */
    val rtkEnabled: Flowable<Boolean>
        @JvmName("getRTKEnabled")
        get() = rtkEnabledProcessor.toFlowable()


    //region Lifecycle
    override fun inSetup() {
        val rtkEnabledKey: DJIKey<Boolean> = KeyTools.createKey(
            RtkMobileStationKey.KeyRTKEnable)
        bindDataProcessor(rtkEnabledKey, rtkEnabledProcessor)
    }

    override fun inCleanup() {
        // do nothing
    }

    override fun updateStates() {
        // Nothing to update
    }
    //endregion
}