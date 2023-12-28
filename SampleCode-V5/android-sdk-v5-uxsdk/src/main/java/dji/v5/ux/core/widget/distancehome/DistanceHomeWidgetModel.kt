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

package dji.v5.ux.core.widget.distancehome

import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.sdk.keyvalue.key.KeyTools
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.GlobalPreferenceKeys
import dji.v5.ux.core.communication.GlobalPreferencesInterface
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.toDistance
import dji.v5.ux.core.util.DataProcessor
import dji.v5.utils.common.LocationUtil.*
import dji.v5.ux.core.util.UnitConversionUtil
import dji.v5.ux.core.widget.distancehome.DistanceHomeWidgetModel.DistanceHomeState.CurrentDistanceToHome

/**
 * Widget Model for the [DistanceHomeWidget] used to define
 * the underlying logic and communication
 */
class DistanceHomeWidgetModel(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore,
    private val preferencesManager: GlobalPreferencesInterface?
) : WidgetModel(djiSdkModel, keyedStore) {

    private val homeLocationProcessor = DataProcessor.create(LocationCoordinate2D(Double.NaN, Double.NaN))
    private val aircraftLocationProcessor = DataProcessor.create(LocationCoordinate2D(Double.NaN, Double.NaN))
    private val unitTypeDataProcessor: DataProcessor<UnitConversionUtil.UnitType> = DataProcessor.create(UnitConversionUtil.UnitType.METRIC)
    private val distanceHomeStateProcessor: DataProcessor<DistanceHomeState> = DataProcessor.create(DistanceHomeState.ProductDisconnected)

    /**
     * Value of the distance to home state of the aircraft
     */
    val distanceHomeState: Flowable<DistanceHomeState>
        get() = distanceHomeStateProcessor.toFlowable()

    override fun inSetup() {

        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyHomeLocation), homeLocationProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyAircraftLocation), aircraftLocationProcessor)

        val unitTypeKey = GlobalPreferenceKeys.create(GlobalPreferenceKeys.UNIT_TYPE)
        bindDataProcessor(unitTypeKey, unitTypeDataProcessor)
        preferencesManager?.setUpListener()
        preferencesManager?.let { unitTypeDataProcessor.onNext(it.unitType) }
    }

    override fun updateStates() {
        if (productConnectionProcessor.value) {
            if (checkLatitude(aircraftLocationProcessor.value.latitude)
                && checkLongitude(aircraftLocationProcessor.value.longitude)
                && checkLatitude(homeLocationProcessor.value.latitude)
                && checkLongitude(homeLocationProcessor.value.longitude)) {
                distanceHomeStateProcessor.onNext(
                    CurrentDistanceToHome(
                        distanceBetween(
                            homeLocationProcessor.value.latitude, homeLocationProcessor.value.longitude,
                            aircraftLocationProcessor.value.latitude, aircraftLocationProcessor.value.longitude
                        ).toDistance(unitTypeDataProcessor.value),
                        unitTypeDataProcessor.value
                    )
                )
            } else {
                distanceHomeStateProcessor.onNext(DistanceHomeState.LocationUnavailable)
            }
        } else {
            distanceHomeStateProcessor.onNext(DistanceHomeState.ProductDisconnected)
        }
    }

    override fun inCleanup() {
        preferencesManager?.cleanup()
    }

    /**
     * Class to represent states distance of aircraft from the home point
     */
    sealed class DistanceHomeState {
        /**
         *  Product is disconnected
         */
        object ProductDisconnected : DistanceHomeState()

        /**
         * Product is connected but gps location fix is unavailable
         */
        object LocationUnavailable : DistanceHomeState()

        /**
         * Reflecting the distance to the home point
         */
        data class CurrentDistanceToHome(val distance: Float, val unitType: UnitConversionUtil.UnitType) :
            DistanceHomeState()

    }
}