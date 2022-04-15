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

package dji.v5.ux.core.widget.gpssignal

import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.RtkBaseStationKey
import dji.sdk.keyvalue.key.RtkMobileStationKey
import dji.sdk.keyvalue.value.flightcontroller.GPSSignalLevel
import dji.sdk.keyvalue.value.flightcontroller.RedundancySensorUsedStateMsg
import dji.sdk.keyvalue.value.rtkbasestation.DroneNestRtkPostionType
import dji.sdk.keyvalue.value.rtkmobilestation.RTKReceiverInfo
import dji.sdk.keyvalue.value.rtkmobilestation.RTKSatelliteInfo
import dji.sdk.keyvalue.key.KeyTools
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.functions.Consumer
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor
import dji.v5.ux.core.util.RxUtil

/**
 * Widget Model for the [GPSSignalWidget] used to define
 * the underlying logic and communication
 */
class GPSSignalWidgetModel(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    //region Fields
    private val gpsSignalQualityProcessor: DataProcessor<GPSSignalLevel> = DataProcessor.create(GPSSignalLevel.UNKNOWN)
    private val satelliteCountProcessor: DataProcessor<Int> = DataProcessor.create(0)
    private val rtkEnabledProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val rtkSupportedProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val rtkSatelliteInfoProcessor: DataProcessor<RTKSatelliteInfo> = DataProcessor.create(RTKSatelliteInfo())
    private val redundancySensorUsedStateProcessor: DataProcessor<RedundancySensorUsedStateMsg> = DataProcessor.create(RedundancySensorUsedStateMsg())
    private val satelliteNumberProcessor = DataProcessor.create(0)
    private val aircraftNestRtkPositionTypeProcessor = DataProcessor.create(DroneNestRtkPostionType.UNKNOWN)
    //endregion

    //region Data
    /**
     * Get the value of the strength of the GPS signal as a [GPSSignalLevel].
     */
    val gpsSignalQuality: Flowable<GPSSignalLevel>
        @JvmName("getGPSSignalQuality")
        get() = gpsSignalQualityProcessor.toFlowable()

    /**
     * Get the number of satellites as an integer value.
     */
    val satelliteNumber: Flowable<Int>
        get() = satelliteNumberProcessor.toFlowable()

    /**
     * Get if RTK is enabled on supported aircraft as a boolean value.
     */
    val rtkEnabled: Flowable<Boolean>
        @JvmName("getRTKEnabled")
        get() = rtkEnabledProcessor.toFlowable()

    /**
     * Get whether an external GPS device is in use.
     */
    val isExternalGPSUsed: Flowable<Boolean>
        get() = redundancySensorUsedStateProcessor.toFlowable()
            .concatMap { state: RedundancySensorUsedStateMsg -> Flowable.just(state.gpsIndex == 2) }

    /**
     * Get whether RTK is using the most accurate positioning solution.
     */
    val isRTKAccurate: Flowable<Boolean>
        get() = aircraftNestRtkPositionTypeProcessor.toFlowable()
            .concatMap { state: DroneNestRtkPostionType -> Flowable.just(state == DroneNestRtkPostionType.FIXED_POINT) }

    //region Lifecycle
    override fun inSetup() {
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyGPSSignalLevel), gpsSignalQualityProcessor)
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyGPSSatelliteCount), satelliteCountProcessor)

        val rtkEnabledKey = KeyTools.createKey(RtkMobileStationKey.KeyRTKEnable)
        bindDataProcessor(rtkEnabledKey, rtkEnabledProcessor)
        //Use the supported key to begin getting the RTK Enabled values
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyIsRtkSupported), rtkSupportedProcessor) {
            addDisposable(
                djiSdkModel.getValue(rtkEnabledKey)
                    .observeOn(SchedulerProvider.io())
                    .subscribe(Consumer { }, RxUtil.logErrorConsumer("GPSSignalWidget", "isRTKSupported: "))
            )
        }
        bindDataProcessor(KeyTools.createKey(RtkMobileStationKey.KeyRTKSatelliteInfo), rtkSatelliteInfoProcessor)
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyRedundancySensorUsedState), redundancySensorUsedStateProcessor)
        bindDataProcessor(KeyTools.createKey(RtkBaseStationKey.KeyDroneNestRtkPostionType), aircraftNestRtkPositionTypeProcessor)
    }

    override fun inCleanup() {
        // Nothing to clean
    }

    override fun updateStates() {
        rtkEnabledProcessor.value.let {
            if (it) {
                satelliteNumberProcessor.onNext(rtkSatelliteInfoProcessor.value.mobileStationReceiver1Info.satelliteCount())
            } else {
                satelliteNumberProcessor.onNext(satelliteCountProcessor.value)
            }
        }
    }

    override fun onProductConnectionChanged(isConnected: Boolean) {
        if (!isConnected) {
            rtkEnabledProcessor.onNext(false)
            gpsSignalQualityProcessor.onNext(GPSSignalLevel.LEVEL_NONE)
        }
    }

    private fun List<RTKReceiverInfo>.satelliteCount(): Int {
        var count = 0
        for (info in this) {
            count += info.count
        }
        return count
    }
}