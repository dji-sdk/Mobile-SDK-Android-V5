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

package dji.v5.ux.core.widget.location

import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.sdk.keyvalue.key.KeyTools
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor
import dji.v5.utils.common.LocationUtil
import dji.v5.ux.core.widget.location.LocationWidgetModel.LocationState.ProductDisconnected

/**
 * Widget Model for the [LocationWidget] used to define
 * the underlying logic and communication
 */
class LocationWidgetModel(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    val aircraftLocationProcessor = DataProcessor.create(LocationCoordinate2D(Double.NaN, Double.NaN))
    private val locationStateProcessor: DataProcessor<LocationState> = DataProcessor.create(ProductDisconnected)

    /**
     * Value of the location state of aircraft
     */
    val locationState: Flowable<LocationState>
        get() = locationStateProcessor.toFlowable()

    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyAircraftLocation), aircraftLocationProcessor)
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (LocationUtil.checkLatitude(aircraftLocationProcessor.value.latitude)
                && LocationUtil.checkLongitude(aircraftLocationProcessor.value.longitude)) {
                locationStateProcessor.onNext(
                    LocationState.CurrentLocation(
                        aircraftLocationProcessor.value.latitude,
                        aircraftLocationProcessor.value.longitude
                    )
                )
            } else {
                locationStateProcessor.onNext(LocationState.LocationUnavailable)
            }
        } else {
            locationStateProcessor.onNext(ProductDisconnected)
        }
    }


    override fun inCleanup() {
        // No code required
    }

    /**
     * Class to represent states of location widget
     */
    sealed class LocationState {
        /**
         *  Product is disconnected
         */
        object ProductDisconnected : LocationState()

        /**
         * Product is connected but GPS location fix is unavailable
         */
        object LocationUnavailable : LocationState()

        /**
         * Reflecting the current location
         */
        data class CurrentLocation(val latitude: Double, val longitude: Double) : LocationState()

    }
}