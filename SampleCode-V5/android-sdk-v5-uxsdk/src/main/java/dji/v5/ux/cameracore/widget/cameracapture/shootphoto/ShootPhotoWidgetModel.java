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

package dji.v5.ux.cameracore.widget.cameracapture.shootphoto;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.utils.ProductUtil;
import dji.sdk.keyvalue.value.camera.CameraMode;
import dji.sdk.keyvalue.value.camera.CameraShootPhotoMode;
import dji.sdk.keyvalue.value.camera.CameraStorageInfo;
import dji.sdk.keyvalue.value.camera.CameraStorageInfos;
import dji.sdk.keyvalue.value.camera.CameraStorageLocation;
import dji.sdk.keyvalue.value.camera.CameraType;
import dji.sdk.keyvalue.value.camera.PhotoAEBPhotoCount;
import dji.sdk.keyvalue.value.camera.PhotoAEBSettings;
import dji.sdk.keyvalue.value.camera.PhotoBurstCount;
import dji.sdk.keyvalue.value.camera.PhotoIntervalShootSettings;
import dji.sdk.keyvalue.value.camera.PhotoPanoramaMode;
import dji.sdk.keyvalue.value.camera.SDCardLoadState;
import dji.sdk.keyvalue.value.camera.SSDOperationState;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.manager.KeyManager;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.module.FlatCameraModule;
import dji.v5.ux.core.util.CameraUtil;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Shoot Photo Widget Model
 * <p>
 * Widget Model for {@link ShootPhotoWidget} used to define underlying
 * logic and communication
 */
public class ShootPhotoWidgetModel extends WidgetModel implements ICameraIndex {

    //region Constants
    private static final int INVALID_AVAILABLE_CAPTURE_COUNT = -1;
    //endregion

    //region Public data
    private final DataProcessor<CameraPhotoState> cameraPhotoState;
    private final DataProcessor<CameraPhotoStorageState> cameraStorageState;
    private final DataProcessor<Boolean> isShootingPhoto;
    private final DataProcessor<Boolean> isStoringPhoto;
    private final DataProcessor<Boolean> canStartShootingPhoto;
    private final DataProcessor<Boolean> shootPhotoNotAllowed;
    private final DataProcessor<Boolean> canStopShootingPhoto;
    private final DataProcessor<String> cameraDisplayName;
    private final DataProcessor<Boolean> isShootingInterval;
    private final DataProcessor<CameraType> cameraType;
    //endregion

    //region Internal data
    private final DataProcessor<Boolean> isShootingPanorama;
    private final DataProcessor<PhotoAEBSettings> aebSettings;
    private final DataProcessor<PhotoAEBPhotoCount> aebCount;
    private final DataProcessor<PhotoBurstCount> burstCount;
    private final DataProcessor<PhotoBurstCount> rawBurstCount;
    private final DataProcessor<PhotoIntervalShootSettings> timeIntervalSettings;
    private final DataProcessor<PhotoPanoramaMode> panoramaMode;
    private final DataProcessor<CameraStorageInfos> storageInfosProcessor;
    private final DataProcessor<CameraStorageLocation> storageLocation;
    private final DataProcessor<SDCardLoadState> sdCardState;
    private final DataProcessor<SDCardLoadState> innerStorageState;
    private final DataProcessor<SSDOperationState> ssdState;
    private final DataProcessor<Integer> sdAvailableCaptureCount;
    private final DataProcessor<Integer> innerStorageAvailableCaptureCount;
    private final DataProcessor<Integer> rawPhotoBurstCaptureCount;
    private final DataProcessor<Boolean> isProductConnected;
    //endregion

    //region Other fields
    private final PhotoIntervalShootSettings defaultIntervalSettings;
    private ComponentIndexType cameraIndex = ComponentIndexType.LEFT_OR_MAIN;
    private CameraLensType lensType = CameraLensType.UNKNOWN;
    private FlatCameraModule flatCameraModule;
    //endregion

    //region Constructor
    public ShootPhotoWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                 @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        defaultIntervalSettings = new PhotoIntervalShootSettings();
        CameraPhotoState photoState = new CameraPhotoState(CameraShootPhotoMode.UNKNOWN);

        this.cameraPhotoState = DataProcessor.create(photoState);
        CameraSDPhotoStorageState cameraSDStorageState = new CameraSDPhotoStorageState(
                CameraStorageLocation.SDCARD, 0, SDCardLoadState.NOT_INSERTED);
        cameraStorageState = DataProcessor.create(cameraSDStorageState);
        canStartShootingPhoto = DataProcessor.create(false);
        shootPhotoNotAllowed = DataProcessor.create(true);
        canStopShootingPhoto = DataProcessor.create(false);
        cameraDisplayName = DataProcessor.create("");
        aebSettings = DataProcessor.create(new PhotoAEBSettings());
        aebCount = DataProcessor.create(PhotoAEBPhotoCount.UNKNOWN);
        burstCount = DataProcessor.create(PhotoBurstCount.UNKNOWN);
        rawBurstCount = DataProcessor.create(PhotoBurstCount.UNKNOWN);
        timeIntervalSettings = DataProcessor.create(defaultIntervalSettings);
        panoramaMode = DataProcessor.create(PhotoPanoramaMode.UNKNOWN);
        isShootingPhoto = DataProcessor.create(false);
        isShootingInterval = DataProcessor.create(false);
        cameraType = DataProcessor.create(CameraType.NOT_SUPPORTED);
        isShootingPanorama = DataProcessor.create(false);
        isStoringPhoto = DataProcessor.create(false);
        storageLocation = DataProcessor.create(CameraStorageLocation.SDCARD);
        sdCardState = DataProcessor.create(SDCardLoadState.UNKNOWN);
        innerStorageState = DataProcessor.create(SDCardLoadState.UNKNOWN);
        ssdState = DataProcessor.create(SSDOperationState.UNKNOWN);
        sdAvailableCaptureCount = DataProcessor.create(INVALID_AVAILABLE_CAPTURE_COUNT);
        innerStorageAvailableCaptureCount = DataProcessor.create(INVALID_AVAILABLE_CAPTURE_COUNT);
        rawPhotoBurstCaptureCount = DataProcessor.create(INVALID_AVAILABLE_CAPTURE_COUNT);
        isProductConnected = DataProcessor.create(false);
        storageInfosProcessor = DataProcessor.create(new CameraStorageInfos(CameraStorageLocation.UNKNOWN, new ArrayList<>()));
        flatCameraModule = new FlatCameraModule();
        addModule(flatCameraModule);
    }
    //endregion

    //region Data

    /**
     * Get the current shoot photo mode
     *
     * @return Flowable with {@link CameraPhotoState} instance
     */
    public Flowable<CameraPhotoState> getCameraPhotoState() {
        return cameraPhotoState.toFlowable();
    }

    /**
     * Get the current camera photo storage location
     *
     * @return Flowable with {@link CameraPhotoStorageState} instance
     */
    public Flowable<CameraPhotoStorageState> getCameraStorageState() {
        return cameraStorageState.toFlowable();
    }

    /**
     * Check if the device is currently shooting photo
     *
     * @return Flowable with boolean value
     * true - if camera is shooting photo false - camera is not shooting photo
     */
    public Flowable<Boolean> isShootingPhoto() {
        return isShootingPhoto.toFlowable();
    }

    /**
     * Check if the device is currently in the process of storing photo
     *
     * @return Flowable with boolean value
     * true - if device is storing photo false - device is not storing photo
     */
    public Flowable<Boolean> isStoringPhoto() {
        return isStoringPhoto.toFlowable();
    }

    /**
     * Check if the device is ready to shoot photo.
     *
     * @return Flowable with boolean value
     * true - device ready  false - device not ready
     */
    public Flowable<Boolean> canStartShootingPhoto() {
        return canStartShootingPhoto.toFlowable();
    }

    /**
     * Check if the device is currently shooting photo and is ready to stop
     *
     * @return Flowable with boolean value
     * true - can stop shooting false - can not stop shooting photo
     */
    public Flowable<Boolean> canStopShootingPhoto() {
        return canStopShootingPhoto.toFlowable();
    }

    @NonNull
    public ComponentIndexType getCameraIndex() {
        return cameraIndex;
    }

    @NonNull
    @Override
    public CameraLensType getLensType() {
        return lensType;
    }

    @Override
    public void updateCameraSource(@NonNull ComponentIndexType cameraIndex, @NonNull CameraLensType lensType) {
        this.cameraIndex = cameraIndex;
        this.lensType = lensType;
        flatCameraModule.updateCameraSource(cameraIndex,lensType);
        restart();
    }

    /**
     * Get the display name of the camera which the model is reacting to
     *
     * @return Flowable with string of camera name
     */
    public Flowable<String> getCameraDisplayName() {
        return cameraDisplayName.toFlowable();
    }
    //endregion

    //region Actions

    /**
     * Start shooting photo
     *
     * @return Completable to determine the status of the action
     */
    public Completable startShootPhoto() {
        if (!canStartShootingPhoto.getValue()) {
            return Completable.complete();
        }
        return djiSdkModel.performActionWithOutResult(KeyTools.createKey(CameraKey.KeyStartShootPhoto, cameraIndex));
    }

    /**
     * Stop shooting photo
     *
     * @return Completable to determine the status of the action
     */
    public Completable stopShootPhoto() {
        if (!canStopShootingPhoto.getValue()) {
            return Completable.complete();
        }
        return djiSdkModel.performActionWithOutResult(KeyTools.createKey(CameraKey.KeyStopShootPhoto, cameraIndex));
    }
    //endregion

    //region Lifecycle
    @Override
    protected void inSetup() {
        // Product connection
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyConnection, cameraIndex), isProductConnected, newValue -> onCameraConnected((boolean) newValue));

        // Photo mode
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyAEBSettings, cameraIndex), aebSettings, photoAEBSettings -> {
            aebCount.onNext(photoAEBSettings.getCount());
        });
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyPhotoBurstCount, cameraIndex), burstCount);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyRawBurstCount, cameraIndex), rawBurstCount);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyPhotoIntervalShootSettings, cameraIndex), timeIntervalSettings);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyPhotoPanoramaMode, cameraIndex), panoramaMode);

        // Is shooting photo state
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyIsShootingPhoto, cameraIndex), isShootingPhoto);

        // Is storing photo state
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyCameraStoringFile, cameraIndex), isStoringPhoto);

        // Can start shooting photo
        // can't take photo when product is not connected
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyShootPhotoNotAllowed, cameraIndex), shootPhotoNotAllowed, aBoolean -> {
            canStartShootingPhoto.onNext(!aBoolean);
        });

        // Can stop shooting photo
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyCameraShootingContinuousPhotos, cameraIndex), isShootingInterval, this::onCanStopShootingPhoto);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyIsShootingPhotoPanorama, cameraIndex), isShootingPanorama, newValue -> onCanStopShootingPhoto((boolean) newValue));

        // Display name
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyCameraType, cameraIndex), cameraType, type -> cameraDisplayName.onNext(type.name()));

        // Storage
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyCameraStorageInfos, cameraIndex), storageInfosProcessor, cameraStorageInfos -> {
            storageLocation.onNext(cameraStorageInfos.getCurrentStorageType());

            CameraStorageInfo internalInfo = cameraStorageInfos.getCameraStorageInfoByLocation(CameraStorageLocation.INTERNAL);
            if (internalInfo != null) {
                innerStorageState.onNext(internalInfo.getStorageState());
                innerStorageAvailableCaptureCount.onNext(internalInfo.getAvailablePhotoCount());
            }

            CameraStorageInfo sdcardInfo = cameraStorageInfos.getCameraStorageInfoByLocation(CameraStorageLocation.SDCARD);
            if (sdcardInfo != null) {
                sdCardState.onNext(sdcardInfo.getStorageState());
                sdAvailableCaptureCount.onNext(sdcardInfo.getAvailablePhotoCount());
            }
        });
        bindDataProcessor(KeyTools.createKey(CameraKey.KeySSDOperationState, cameraIndex), ssdState);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeySDCardAvailablePhotoCount, cameraIndex), sdAvailableCaptureCount);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyInternalStorageAvailablePhotoCount, cameraIndex), innerStorageAvailableCaptureCount);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyH1PhotoBurstCount, cameraIndex), rawPhotoBurstCaptureCount);
    }

    @Override
    protected void inCleanup() {
        // do nothing
    }

    @Override
    protected void updateStates() {
        updateCameraPhotoState();
        updateCameraStorageState();
    }

    public boolean isPhotoMode(){
        return flatCameraModule.getCameraModeDataProcessor().getValue().isPhotoMode()
                || (flatCameraModule.getCameraModeDataProcessor().getValue() == CameraMode.VIDEO_NORMAL && isSupportShootPhoto());
    }

    private boolean isSupportShootPhoto(){
        CameraType cameraType = KeyManager.getInstance().getValue(KeyTools.createKey(CameraKey.KeyCameraType ,cameraIndex) ,CameraType.NOT_SUPPORTED);
        return cameraType == CameraType.M3T || cameraType == CameraType.M3E || cameraType == CameraType.M3TA || cameraType == CameraType.M3M || cameraType == CameraType.ZENMUSE_H20T;
    }

    //region Helpers
    private void updateCameraPhotoState() {
        CameraPhotoState state = null;
        CameraShootPhotoMode shootPhotoMode = flatCameraModule.getShootPhotoModeProcessor().getValue();
        switch (shootPhotoMode) {
            case NORMAL:
            case HDR:
            case HYPER_LIGHT:
                //case SHALLOW_FOCUS:
            case EHDR:
                state = new CameraPhotoState(shootPhotoMode);
                break;
            case BURST:
                if (!PhotoBurstCount.UNKNOWN.equals(burstCount.getValue())) {
                    state = new CameraBurstPhotoState(
                            shootPhotoMode,
                            burstCount.getValue());
                }
                break;
            case RAW_BURST:
                if (!PhotoBurstCount.UNKNOWN.equals(rawBurstCount.getValue())) {
                    state = new CameraBurstPhotoState(
                            shootPhotoMode,
                            rawBurstCount.getValue()
                    );
                }
                break;
            case AEB:
                if (!PhotoAEBPhotoCount.UNKNOWN.equals(aebCount.getValue())) {
                    state = new CameraAEBPhotoState(
                            shootPhotoMode,
                            aebCount.getValue()
                    );
                }
                break;
            case INTERVAL:
                PhotoIntervalShootSettings intervalSettings = timeIntervalSettings.getValue();
                if (!defaultIntervalSettings.equals(timeIntervalSettings.getValue())) {
                    state = new CameraIntervalPhotoState(
                            shootPhotoMode,
                            intervalSettings.getCount(),
                            intervalSettings.getInterval().intValue()
                    );
                }
                break;
            case PANO_APP:
                if (!PhotoPanoramaMode.UNKNOWN.equals(panoramaMode.getValue())) {
                    state = new CameraPanoramaPhotoState(
                            shootPhotoMode,
                            panoramaMode.getValue()
                    );
                }
                break;
            default:
                break;
        }

        if (state != null) {
            this.cameraPhotoState.onNext(state);
        }
    }

    private void updateCameraStorageState() {
        CameraStorageLocation currentStorageLocation = storageLocation.getValue();
        if (CameraStorageLocation.UNKNOWN.equals(currentStorageLocation)) {
            return;
        }

        CameraShootPhotoMode currentShootPhotoMode = flatCameraModule.getShootPhotoModeProcessor().getValue();
        long availableCaptureCount = getAvailableCaptureCount(currentStorageLocation, currentShootPhotoMode);
        CameraPhotoStorageState newCameraPhotoStorageState = null;
        if (currentShootPhotoMode == CameraShootPhotoMode.RAW_BURST) {
            newCameraPhotoStorageState = new CameraSSDPhotoStorageState(CameraStorageLocation.UNKNOWN, availableCaptureCount, ssdState.getValue());
        } else if (CameraStorageLocation.SDCARD.equals(currentStorageLocation)) {
            if (!SDCardLoadState.UNKNOWN.equals(sdCardState.getValue())) {
                newCameraPhotoStorageState = new CameraSDPhotoStorageState(currentStorageLocation, availableCaptureCount, sdCardState.getValue());
            }
        } else if (CameraStorageLocation.INTERNAL.equals(currentStorageLocation)) {
            newCameraPhotoStorageState = new CameraSDPhotoStorageState(currentStorageLocation, availableCaptureCount, innerStorageState.getValue());
        }

        if (newCameraPhotoStorageState != null) {
            cameraStorageState.onNext(newCameraPhotoStorageState);
        }
    }

    private long getAvailableCaptureCount(CameraStorageLocation storageLocation,
                                          CameraShootPhotoMode shootPhotoMode) {
        if (shootPhotoMode == CameraShootPhotoMode.RAW_BURST) {
            return rawPhotoBurstCaptureCount.getValue();
        }

        switch (storageLocation) {
            case SDCARD:
                return sdAvailableCaptureCount.getValue();
            case INTERNAL:
                return innerStorageAvailableCaptureCount.getValue();
            case UNKNOWN:
            default:
                return INVALID_AVAILABLE_CAPTURE_COUNT;
        }
    }

    private void onCanStopShootingPhoto(boolean canStopShootingPhoto) {
        this.canStopShootingPhoto.onNext(canStopShootingPhoto);
    }

    private void onCameraConnected(boolean isCameraConnected) {
        if (!isCameraConnected) {
            // Reset storage state
            sdCardState.onNext(SDCardLoadState.UNKNOWN);
            innerStorageState.onNext(SDCardLoadState.UNKNOWN);
            ssdState.onNext(SSDOperationState.UNKNOWN);
        }
    }
    //endregion
}
