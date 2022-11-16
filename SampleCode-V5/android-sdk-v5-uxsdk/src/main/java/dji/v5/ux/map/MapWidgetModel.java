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

package dji.v5.ux.map;



import androidx.annotation.NonNull;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.GimbalKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.common.LocationCoordinate2D;
import dji.sdk.keyvalue.value.common.LocationCoordinate3D;
import dji.v5.ux.core.base.DJISDKModel;

import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import dji.v5.ux.core.base.WidgetModel;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Map Widget Model
 * <p>
 * Widget Model for {@link MapWidgetModel} used to define the
 * underlying logic and communication
 */
public class MapWidgetModel extends WidgetModel {

    public static final double INVALID_COORDINATE = 181;  //valid longitude range is -180 to 180.


    private final DataProcessor<LocationCoordinate3D> aircraftLocationDataProcessor;

    private final DataProcessor<LocationCoordinate2D> homeLocationDataProcessor;
    private final DataProcessor<Double> gimbalYawDataProcessor;
    private final DataProcessor<Double> aircraftHeadingDataProcessor;
    private final DataProcessor<String> flightControllerSerialNumberDataProcessor;



    public MapWidgetModel(@NonNull DJISDKModel djiSdkModel,
                          @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        aircraftLocationDataProcessor =
                DataProcessor.create(new LocationCoordinate3D(INVALID_COORDINATE, INVALID_COORDINATE, -1d));

        homeLocationDataProcessor =
                DataProcessor.create(new LocationCoordinate2D(INVALID_COORDINATE, INVALID_COORDINATE));
        gimbalYawDataProcessor = DataProcessor.create(0.0d);
        aircraftHeadingDataProcessor = DataProcessor.create(0.0d);
        flightControllerSerialNumberDataProcessor = DataProcessor.create("");

    }

    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D), aircraftLocationDataProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyHomeLocation) , homeLocationDataProcessor);
        bindDataProcessor(KeyTools.createKey(GimbalKey.KeyYawRelativeToAircraftHeading), gimbalYawDataProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeySerialNumber), flightControllerSerialNumberDataProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyCompassHeading), aircraftHeadingDataProcessor);
    }

    @Override
    protected void inCleanup() {
        // No code
    }

    @Override
    protected void updateStates() {
        // No code
    }


    /**
     * Get aircraft location data including latitude, longitude, altitude
     *
     * @return Flowable with {@link LocationCoordinate3D} instance
     */
    public Flowable<LocationCoordinate3D> getAircraftLocation() {
        return aircraftLocationDataProcessor.toFlowable();
    }

    /**
     * Get home location data including latitude, longitude
     *
     * @return Flowable with {@link LocationCoordinate2D} instance
     */
    public Flowable<LocationCoordinate2D> getHomeLocation() {
        return homeLocationDataProcessor.toFlowable();
    }

    /**
     * Get gimbal yaw angle in degrees
     *
     * @return Flowable with float value representing angle
     */
    public Flowable<Double> getGimbalHeading() {
        return gimbalYawDataProcessor.toFlowable();
    }

    /**
     * Get aircraft yaw angle in degrees
     *
     * @return Flowable with float value representing angle
     */
    public Flowable<Double> getAircraftHeading() {
        return aircraftHeadingDataProcessor.toFlowable();
    }


    //endregion
}
