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
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;

/**
 * Map Widget Model
 * <p>
 * Widget Model for {@link MapWidgetModel} used to define the
 * underlying logic and communication
 */
public class MapWidgetModel extends WidgetModel {
//
//    public static final double INVALID_COORDINATE = 181;  //valid longitude range is -180 to 180.
//    //region Fields
//    private static final String TAG = "MapWidgetModel";
//    private static final int FIRST_TIME_DELAY = 3;
//    private final DataProcessor<LocationCoordinate3D> aircraftLocationDataProcessor;
//    private final DataProcessor<Double> homeLatitudeDataProcessor;
//    private final DataProcessor<Double> homeLongitudeDataProcessor;
//    private final DataProcessor<LocationCoordinate2D> homeLocationDataProcessor;
//    private final DataProcessor<Float> gimbalYawDataProcessor;
//    private final DataProcessor<Float> aircraftHeadingDataProcessor;
//    private final DataProcessor<String> flightControllerSerialNumberDataProcessor;
//    private List<FlyZoneInformation> flyZoneList;
//    private List<CustomUnlockZone> customFlyZoneList;
//    private Map<Integer, CustomUnlockZone> customUnlockZoneMap;
//    private boolean isFirstFlyZoneListRequest = true;
//    //endregion
//
//    //region life-cycle
//    public MapWidgetModel(@NonNull DJISDKModel djiSdkModel,
//                          @NonNull ObservableInMemoryKeyedStore keyedStore) {
//        super(djiSdkModel, keyedStore);
//        aircraftLocationDataProcessor =
//                DataProcessor.create(new LocationCoordinate3D(INVALID_COORDINATE, INVALID_COORDINATE, -1f));
//        homeLatitudeDataProcessor = DataProcessor.create(INVALID_COORDINATE);
//        homeLongitudeDataProcessor = DataProcessor.create(INVALID_COORDINATE);
//        homeLocationDataProcessor =
//                DataProcessor.create(new LocationCoordinate2D(INVALID_COORDINATE, INVALID_COORDINATE));
//        gimbalYawDataProcessor = DataProcessor.create(0.0f);
//        aircraftHeadingDataProcessor = DataProcessor.create(0.0f);
//        flightControllerSerialNumberDataProcessor = DataProcessor.create("");
//        flyZoneList = new ArrayList<>();
//        customUnlockZoneMap = new HashMap<>();
//        customFlyZoneList = new ArrayList<>();
//    }
//
//    @Override
//    protected void inSetup() {
//        DJIKey aircraftLocationKey = FlightControllerKey.create(FlightControllerKey.AIRCRAFT_LOCATION);
//        bindDataProcessor(aircraftLocationKey, aircraftLocationDataProcessor);
//        DJIKey homeLatKey = FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LATITUDE);
//        bindDataProcessor(homeLatKey, homeLatitudeDataProcessor, newValue -> homeLocationDataProcessor.onNext(
//                new LocationCoordinate2D((double) newValue, homeLongitudeDataProcessor.getValue())));
//        DJIKey homeLngKey = FlightControllerKey.create(FlightControllerKey.HOME_LOCATION_LONGITUDE);
//        bindDataProcessor(homeLngKey, homeLongitudeDataProcessor, newValue -> homeLocationDataProcessor.onNext(
//                new LocationCoordinate2D(homeLatitudeDataProcessor.getValue(), (double) newValue)));
//        DJIKey gimbalHeadingKey = GimbalKey.create(GimbalKey.YAW_ANGLE_WITH_AIRCRAFT_IN_DEGREE);
//        bindDataProcessor(gimbalHeadingKey, gimbalYawDataProcessor);
//        DJIKey serialNumberKey = FlightControllerKey.create(FlightControllerKey.SERIAL_NUMBER);
//        bindDataProcessor(serialNumberKey, flightControllerSerialNumberDataProcessor);
//        DJIKey aircraftHeadingKey = FlightControllerKey.create(FlightControllerKey.COMPASS_HEADING);
//        bindDataProcessor(aircraftHeadingKey, aircraftHeadingDataProcessor);
//    }
//
//    @Override
//    protected void inCleanup() {
//        // No code
//    }
//
//    @Override
//    protected void updateStates() {
//        // No code
//    }
//
//    //endregion
//
//    //region Data
//
//    /**
//     * Get aircraft location data including latitude, longitude, altitude
//     *
//     * @return Flowable with {@link LocationCoordinate3D} instance
//     */
//    public Flowable<LocationCoordinate3D> getAircraftLocation() {
//        return aircraftLocationDataProcessor.toFlowable();
//    }
//
//    /**
//     * Get home location data including latitude, longitude
//     *
//     * @return Flowable with {@link LocationCoordinate2D} instance
//     */
//    public Flowable<LocationCoordinate2D> getHomeLocation() {
//        return homeLocationDataProcessor.toFlowable();
//    }
//
//    /**
//     * Get gimbal yaw angle in degrees
//     *
//     * @return Flowable with float value representing angle
//     */
//    public Flowable<Float> getGimbalHeading() {
//        return gimbalYawDataProcessor.toFlowable();
//    }
//
//    /**
//     * Get aircraft yaw angle in degrees
//     *
//     * @return Flowable with float value representing angle
//     */
//    public Flowable<Float> getAircraftHeading() {
//        return aircraftHeadingDataProcessor.toFlowable();
//    }
//
//    /**
//     * Get list of FlyZones in surrounding area
//     *
//     * @return Single with ArrayList of {@link FlyZoneInformation}
//     */
//    public Single<ArrayList<FlyZoneInformation>> getFlyZoneList() {
//        if (getFlyZoneManager() == null) {
//            return Single.just(new ArrayList<>());
//        }
//        Single flyZoneListSingle =
//                Single.zip(getFlyZonesInSurroundingArea(), getSelfUnlockedFlyZones(), Pair::new).flatMap(arrayListPair -> {
//                    if (arrayListPair.first.size() > 0) {
//                        HashSet<Integer> duplicateCheck = new HashSet<>();
//                        for (FlyZoneInformation zone : arrayListPair.second) {
//                            duplicateCheck.add(zone.getFlyZoneID());
//                        }
//                        for (FlyZoneInformation zone : arrayListPair.first) {
//                            if (!duplicateCheck.contains(zone.getFlyZoneID())) {
//                                arrayListPair.second.add(zone);
//                            }
//                        }
//                        flyZoneList = arrayListPair.second;
//                        return Single.just(flyZoneList);
//                    } else {
//                        return Single.just(new ArrayList<FlyZoneInformation>());
//                    }
//                });
//        if (isFirstFlyZoneListRequest) {
//            isFirstFlyZoneListRequest = false;
//            return Single.timer(FIRST_TIME_DELAY, TimeUnit.SECONDS, SchedulerProvider.computation())
//                    .flatMap(aLong -> flyZoneListSingle);
//        } else {
//            return flyZoneListSingle.subscribeOn(SchedulerProvider.computation());
//        }
//    }
//
//    /**
//     * Get the cached FlyZone list. List may be empty if nothing was cached
//     * User should call {@link MapWidgetModel#getFlyZoneList()} prior to this
//     *
//     * @return Single with list of {@link FlyZoneInformation}
//     */
//    public Single<List<FlyZoneInformation>> getCachedFlyZoneList() {
//        return Single.just(flyZoneList);
//    }
//
//    /**
//     * Get custom unlocked zones from aircraft
//     *
//     * @return Single with Map<{@link CustomUnlockZone#getID()} {@link CustomUnlockZone}>
//     */
//    public Single<Map<Integer, CustomUnlockZone>> getCustomUnlockZonesFromAircraft() {
//        return Single.create((SingleOnSubscribe<Map<Integer, CustomUnlockZone>>) singleEmitter -> {
//            if (getFlyZoneManager() == null && !singleEmitter.isDisposed()) {
//                singleEmitter.onError(new UXSDKError(UXSDKErrorDescription.FLYZONE_ERROR));
//                return;
//            }
//            getFlyZoneManager().getCustomUnlockZonesFromAircraft(new CommonCallbacks.CompletionCallbackWith<List<CustomUnlockZone>>() {
//                @Override
//                public void onSuccess(List<CustomUnlockZone> customUnlockZones) {
//                    customUnlockZoneMap = new HashMap<>();
//                    for (CustomUnlockZone customUnlockZone : customUnlockZones) {
//                        customUnlockZoneMap.put(customUnlockZone.getID(), customUnlockZone);
//                    }
//                    if (!singleEmitter.isDisposed()) {
//                        singleEmitter.onSuccess(customUnlockZoneMap);
//                    }
//                }
//
//                @Override
//                public void onFailure(DJIError error) {
//                    if (!singleEmitter.isDisposed()) {
//                        singleEmitter.onError(new UXSDKError(error));
//                    }
//                }
//            });
//        }).subscribeOn(SchedulerProvider.computation());
//    }
//
//    /**
//     * Get cached custom unlock zones from aircraft. Map may be empty if nothing was cached
//     * User should call {@link MapWidgetModel#getCustomUnlockZonesFromAircraft()} prior to this
//     *
//     * @return Single with Map<{@link CustomUnlockZone#getID()} {@link CustomUnlockZone}>
//     */
//    public Single<Map<Integer, CustomUnlockZone>> getCachedCustomUnlockedZonesFromAircraft() {
//        return Single.just(customUnlockZoneMap);
//    }
//
//    /**
//     * Get the list of custom unlock zones from server
//     *
//     * @return Single with list of {@link CustomUnlockZone}
//     */
//    public Single<List<CustomUnlockZone>> getCustomUnlockZonesFromServer() {
//        return Single.create((SingleOnSubscribe<List<CustomUnlockZone>>) singleEmitter -> {
//            if (getFlyZoneManager() == null && !singleEmitter.isDisposed()) {
//                singleEmitter.onError(new UXSDKError(UXSDKErrorDescription.FLYZONE_ERROR));
//                return;
//            }
//            getFlyZoneManager().getLoadedUnlockedZoneGroups(new CommonCallbacks.CompletionCallbackWith<List<UnlockedZoneGroup>>() {
//                @Override
//                public void onSuccess(List<UnlockedZoneGroup> unlockedZoneGroups) {
//                    for (UnlockedZoneGroup unlockedZoneGroup : unlockedZoneGroups) {
//                        if (flightControllerSerialNumberDataProcessor.getValue().equals(unlockedZoneGroup.getSn())
//                                && !singleEmitter.isDisposed()) {
//                            singleEmitter.onSuccess(unlockedZoneGroup.getCustomUnlockZones());
//                            break;
//                        }
//                    }
//                    if (!singleEmitter.isDisposed()) {
//                        singleEmitter.onSuccess(new ArrayList<>());
//                    }
//                }
//
//                @Override
//                public void onFailure(DJIError error) {
//                    if (!singleEmitter.isDisposed()) {
//                        singleEmitter.onError(new UXSDKError(error));
//                    }
//                }
//            });
//        }).subscribeOn(SchedulerProvider.computation());
//    }
//
//    /**
//     * Get the cached custom unlock zone list. List may be empty if nothing was cached
//     * User should call {@link MapWidgetModel#getCustomUnlockZonesFromServer()} prior to this
//     *
//     * @return Single with list of {@link CustomUnlockZone}
//     */
//    public Single<List<CustomUnlockZone>> getCachedCustomUnlockZones() {
//        return Single.just(customFlyZoneList);
//    }
//
//    /**
//     * Unlock the FlyZones
//     *
//     * @param flyZoneIdList integer list of FlyZone Ids which should be unlocked
//     * @return Completable representing success or failure of action
//     */
//    public Completable unlockFlyZone(final ArrayList<Integer> flyZoneIdList) {
//        return Completable.create(completableEmitter -> {
//            if (getFlyZoneManager() == null && !completableEmitter.isDisposed()) {
//                completableEmitter.onError(new UXSDKError(UXSDKErrorDescription.FLYZONE_ERROR));
//                return;
//            }
//            getFlyZoneManager().unlockFlyZones(flyZoneIdList, error -> {
//                if (error == null) {
//                    if (!completableEmitter.isDisposed()) {
//                        completableEmitter.onComplete();
//                    }
//                } else {
//                    if (!completableEmitter.isDisposed()) {
//                        completableEmitter.onError(new UXSDKError(error));
//                    }
//                }
//            });
//        }).subscribeOn(SchedulerProvider.computation());
//    }
//
//    /**
//     * Upload custom unlock zones downloaded from server to aircraft
//     *
//     * @return Completable representing success or failure of action
//     */
//    public Completable syncZonesToAircraft() {
//        return Completable.create(completableEmitter -> {
//            if (getFlyZoneManager() == null && !completableEmitter.isDisposed()) {
//                completableEmitter.onError(new UXSDKError(UXSDKErrorDescription.FLYZONE_ERROR));
//                return;
//            }
//            getFlyZoneManager().syncUnlockedZoneGroupToAircraft(error -> {
//                if (error == null) {
//                    if (!completableEmitter.isDisposed()) {
//                        completableEmitter.onComplete();
//                    }
//                } else {
//                    if (!completableEmitter.isDisposed()) {
//                        completableEmitter.onError(new UXSDKError(error));
//                    }
//                }
//            });
//        });
//    }
//
//    /**
//     * Enable the custom unlock zone on aircraft. Once the zone is enabled the aircraft will
//     * not be able to take off anywhere other than the zone.
//     *
//     * @param customUnlockZone {@link CustomUnlockZone} instance which should be enabled
//     * @return Completable representing success or failure of action
//     */
//    public Completable enableCustomUnlockZoneOnAircraft(@NonNull CustomUnlockZone customUnlockZone) {
//        return enableDisableFlyZone(customUnlockZone);
//    }
//
//    /**
//     * Disable the custom unlocked zone currently enabled on aircraft
//     *
//     * @return Completable representing success or failure of action
//     */
//    public Completable disableCustomUnlockZoneOnAircraft() {
//        return enableDisableFlyZone(null);
//    }
//
//    //endregion
//
//    //region private methods
//
//    private Completable enableDisableFlyZone(CustomUnlockZone customUnlockZone) {
//        return Completable.create(completableEmitter -> {
//            if (getFlyZoneManager() == null && !completableEmitter.isDisposed()) {
//                completableEmitter.onError(new UXSDKError(UXSDKErrorDescription.FLYZONE_ERROR));
//                return;
//            }
//            getFlyZoneManager().enableCustomUnlockZone(customUnlockZone, error -> {
//                if (error == null) {
//                    if (!completableEmitter.isDisposed()) {
//                        completableEmitter.onComplete();
//                    }
//                } else {
//                    if (!completableEmitter.isDisposed()) {
//                        completableEmitter.onError(new UXSDKError(error));
//                    }
//                }
//            });
//        });
//    }
//
//    private Single<ArrayList<FlyZoneInformation>> getFlyZonesInSurroundingArea() {
//        return Single.create((SingleOnSubscribe<ArrayList<FlyZoneInformation>>) singleEmitter -> {
//            if (getFlyZoneManager() == null && !singleEmitter.isDisposed()) {
//                singleEmitter.onError(new UXSDKError(UXSDKErrorDescription.FLYZONE_ERROR));
//                return;
//            }
//
//            getFlyZoneManager().getFlyZonesInSurroundingArea(
//                    new CommonCallbacks.CompletionCallbackWith<ArrayList<FlyZoneInformation>>() {
//                        @Override
//                        public void onSuccess(ArrayList<FlyZoneInformation> flyZoneInformations) {
//
//                            if (!singleEmitter.isDisposed()) {
//                                singleEmitter.onSuccess(flyZoneInformations);
//                            }
//                        }
//
//                        @Override
//                        public void onFailure(DJIError error) {
//                            if (!singleEmitter.isDisposed()) {
//                                singleEmitter.onError(new UXSDKError(error));
//                            }
//                        }
//                    });
//        }).subscribeOn(SchedulerProvider.computation());
//    }
//
//    private Single<List<FlyZoneInformation>> getSelfUnlockedFlyZones() {
//        return Single.create((SingleOnSubscribe<List<FlyZoneInformation>>) singleEmitter -> {
//            if (getFlyZoneManager() == null && !singleEmitter.isDisposed()) {
//                singleEmitter.onError(new UXSDKError(UXSDKErrorDescription.FLYZONE_ERROR));
//                return;
//            }
//            getFlyZoneManager().getUnlockedFlyZonesForAircraft(new CommonCallbacks.CompletionCallbackWith<List<FlyZoneInformation>>() {
//                @Override
//                public void onSuccess(List<FlyZoneInformation> flyZoneInformations) {
//                    if (!singleEmitter.isDisposed()) {
//                        singleEmitter.onSuccess(flyZoneInformations);
//                    }
//                }
//
//                @Override
//                public void onFailure(DJIError error) {
//                    if (error.equals(DJIFlySafeError.ACCOUNT_NOT_LOGGED_IN_OR_NOT_AUTHORIZED)) {
//                        if (!singleEmitter.isDisposed()) {
//                            singleEmitter.onSuccess(new ArrayList<>());
//                        }
//                    } else {
//                        if (!singleEmitter.isDisposed()) {
//                            singleEmitter.onError(new UXSDKError(error));
//                        }
//                    }
//                }
//            });
//        }).subscribeOn(SchedulerProvider.computation());
//    }
//
//    private FlyZoneManager getFlyZoneManager() {
//        return DJISDKManager.getInstance().getFlyZoneManager();
//    }
    //endregion


    @Override
    protected void inSetup() {
        //暂无实现
    }

    @Override
    protected void inCleanup() {
        //暂无实现
    }

    public MapWidgetModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }
}
