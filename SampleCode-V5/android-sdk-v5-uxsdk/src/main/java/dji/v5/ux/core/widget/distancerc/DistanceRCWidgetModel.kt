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

package dji.v5.ux.core.widget.distancerc

import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.RemoteControllerKey
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.sdk.keyvalue.value.remotecontroller.RcGPSInfo
import dji.sdk.keyvalue.key.KeyTools
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.GlobalPreferenceKeys
import dji.v5.ux.core.communication.GlobalPreferencesInterface
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.toDistance
import dji.v5.ux.core.util.DataProcessor
import dji.v5.utils.common.LocationUtil
import dji.v5.utils.common.LocationUtil.distanceBetween
import dji.v5.ux.core.util.UnitConversionUtil
import dji.v5.ux.core.widget.distancerc.DistanceRCWidgetModel.DistanceRCState.*

/**
 * Widget Model for the [DistanceRCWidget] used to define
 * the underlying logic and communication
 */
class DistanceRCWidgetModel(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore,
    private val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {

    private val rcGPSDataProcessor: DataProcessor<RcGPSInfo> = DataProcessor.create(RcGPSInfo())
    private val unitTypeDataProcessor: DataProcessor<UnitConversionUtil.UnitType> = DataProcessor.create(UnitConversionUtil.UnitType.METRIC)
    private val aircraftLocationProcessor = DataProcessor.create(LocationCoordinate2D(Double.NaN, Double.NaN))
    private val distanceRCStateProcessor: DataProcessor<DistanceRCState> = DataProcessor.create(ProductDisconnected)

    /**
     * Value of the distance to RC state of the aircraft
     */
    val distanceRCState: Flowable<DistanceRCState>
        get() = distanceRCStateProcessor.toFlowable()

    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyAircraftLocation), aircraftLocationProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                RemoteControllerKey.KeyRcGPSInfo), rcGPSDataProcessor)
        bindDataProcessor(GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE), unitTypeDataProcessor)
        preferencesManager?.setUpListener()
        preferencesManager?.let { unitTypeDataProcessor.onNext(it.unitType) }
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (LocationUtil.checkLatitude(aircraftLocationProcessor.value.latitude)
                && LocationUtil.checkLongitude(aircraftLocationProcessor.value.longitude)
                && rcGPSDataProcessor.value.isValid) {
                distanceRCStateProcessor.onNext(
                    CurrentDistanceToRC(
                        distanceBetween(
                            aircraftLocationProcessor.value.latitude,
                            aircraftLocationProcessor.value.longitude,
                            rcGPSDataProcessor.value.location.latitude,
                            rcGPSDataProcessor.value.location.longitude
                        )
                            .toDistance(unitTypeDataProcessor.value),
                        unitTypeDataProcessor.value
                    )
                )
            } else {
                distanceRCStateProcessor.onNext(LocationUnavailable)
            }
        } else {
            distanceRCStateProcessor.onNext(ProductDisconnected)
        }

    }

    override fun inCleanup() {
        preferencesManager?.cleanup()
    }

    /**
     * Class to represent states distance of aircraft from the remote controller
     */
    sealed class DistanceRCState {
        /**
         *  Product is disconnected
         */
        object ProductDisconnected : DistanceRCState()

        /**
         * Product is connected but GPS location fix is unavailable
         */
        object LocationUnavailable : DistanceRCState()

        /**
         * Reflecting the distance to the remote controller
         */
        data class CurrentDistanceToRC(val distance: Float, val unitType: UnitConversionUtil.UnitType) :
            DistanceRCState()

    }

}