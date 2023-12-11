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

import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.DJIKey
import dji.sdk.keyvalue.key.RtkMobileStationKey
import dji.sdk.keyvalue.value.rtkmobilestation.RTKHomePointDataSource
import dji.sdk.keyvalue.value.rtkmobilestation.RTKHomePointInfo
import dji.sdk.keyvalue.value.rtkmobilestation.RTKTakeoffAltitudeInfo
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.manager.KeyManager
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable

/**
 * Widget Model for the [RTKEnabledWidget] used to define
 * the underlying logic and communication
 */
private const val TAG = "RTKEnabledWidgetModel"

class RTKEnabledWidgetModel(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields
    private val isRTKEnabledKey: DJIKey<Boolean> = KeyTools.createKey(
        RtkMobileStationKey.KeyRTKEnable)
    private val isRTKEnabledProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val isMotorOnProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val homePointDataSourceProcessor: DataProcessor<RTKHomePointInfo> = DataProcessor.create(RTKHomePointInfo())
    private val isRTKTakeoffHeightSetProcessor: DataProcessor<RTKTakeoffAltitudeInfo> = DataProcessor.create(RTKTakeoffAltitudeInfo())
    private val canEnableRTKProcessor: DataProcessor<Boolean> = DataProcessor.create(true)
    //endregion

    //region Data
    /**
     * Get whether RTK is enabled.
     */
    val rtkEnabled: Flowable<Boolean>
        @JvmName("getRTKEnabled")
        get() = isRTKEnabledProcessor.toFlowable()

    /**
     * Get whether RTK can be enabled.
     */
    val canEnableRTK: Flowable<Boolean>
        get() = canEnableRTKProcessor.toFlowable()

    //endregion

    //region Lifecycle
    override fun inSetup() {
        bindDataProcessor(isRTKEnabledKey, isRTKEnabledProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyAreMotorsOn), isMotorOnProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                RtkMobileStationKey.KeyRTKHomePointInfo), homePointDataSourceProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                RtkMobileStationKey.KeyRTKTakeoffAltitudeInfo), isRTKTakeoffHeightSetProcessor)
    }

    override fun inCleanup() {
        KeyManager.getInstance().cancelListen(this)
    }

    /**
     * RTK能否开启，这里捆绑了几个逻辑：
     * 1、只有在电机关闭时才可以设置RTK开启/关闭状态；
     * 2、如果电机已开启，则需要飞行高度已设置+返航点的类型设置为RTK，这时才可以开启/关闭RTK
     */
    override fun updateStates() {
        canEnableRTKProcessor.onNext(
            !isMotorOnProcessor.value || (isRTKTakeoffHeightSetProcessor.value.valid
                    && homePointDataSourceProcessor.value.homePointDataSource == RTKHomePointDataSource.RTK)
        )
    }
    //endregion

    //region User interaction
    fun setRTKEnabled(enabled: Boolean): Completable {
        return djiSdkModel.setValue(isRTKEnabledKey, enabled)
    }
    //endregion
}