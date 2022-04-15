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

package dji.v5.ux.visualcamera.storage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.camera.CameraColor;
import dji.sdk.keyvalue.value.camera.CameraSDCardState;
import dji.sdk.keyvalue.value.camera.CameraStorageLocation;
import dji.sdk.keyvalue.value.camera.CameraWorkMode;
import dji.sdk.keyvalue.value.camera.PhotoFileFormat;
import dji.sdk.keyvalue.value.camera.VideoFrameRate;
import dji.sdk.keyvalue.value.camera.VideoResolution;
import dji.sdk.keyvalue.value.camera.VideoResolutionFrameRate;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.module.FlatCameraModule;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Widget Model for the {@link CameraConfigStorageWidget} used to define
 * the underlying logic and communication
 */
public class CameraConfigStorageWidgetModel extends WidgetModel implements ICameraIndex {

    //region Constants
    /**
     * The available capture count is unknown.
     */
    protected static final int INVALID_AVAILABLE_CAPTURE_COUNT = -1;
    /**
     * The available recording time is unknown.
     */
    protected static final int INVALID_AVAILABLE_RECORDING_TIME = -1;
    //endregion

    //region Internal Data
    private final DataProcessor<CameraStorageLocation> storageLocationProcessor;
    private final DataProcessor<VideoResolutionFrameRate> resolutionAndFrameRateProcessor;
    private final DataProcessor<PhotoFileFormat> photoFileFormatProcessor;
    private final DataProcessor<CameraSDCardState> sdCardState;
    private final DataProcessor<CameraSDCardState> innerStorageState;
    private final DataProcessor<Integer> sdAvailableCaptureCount;
    private final DataProcessor<Integer> innerStorageAvailableCaptureCount;
    private final DataProcessor<Integer> sdCardRecordingTime;
    private final DataProcessor<Integer> innerStorageRecordingTime;
    private final DataProcessor<CameraColor> cameraColorProcessor;
    //region Public Data
    private final DataProcessor<ImageFormat> imageFormatProcessor;
    //endregion
    private final DataProcessor<CameraStorageState> cameraStorageState;
    private ComponentIndexType cameraIndex = ComponentIndexType.LEFT_OR_MAIN;
    private CameraLensType lensType = CameraLensType.CAMERA_LENS_ZOOM;
    private FlatCameraModule flatCameraModule;
    //endregion

    //region Constructor
    public CameraConfigStorageWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                          @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        storageLocationProcessor = DataProcessor.create(CameraStorageLocation.UNKNOWN);
        resolutionAndFrameRateProcessor = DataProcessor.create(new VideoResolutionFrameRate());
        photoFileFormatProcessor = DataProcessor.create(PhotoFileFormat.UNKNOWN);
        sdCardState = DataProcessor.create(CameraSDCardState.UNKNOWN);
        innerStorageState = DataProcessor.create(CameraSDCardState.UNKNOWN);
        sdAvailableCaptureCount = DataProcessor.create(INVALID_AVAILABLE_CAPTURE_COUNT);
        innerStorageAvailableCaptureCount = DataProcessor.create(INVALID_AVAILABLE_CAPTURE_COUNT);
        sdCardRecordingTime = DataProcessor.create(INVALID_AVAILABLE_RECORDING_TIME);
        innerStorageRecordingTime = DataProcessor.create(INVALID_AVAILABLE_RECORDING_TIME);
        cameraColorProcessor = DataProcessor.create(CameraColor.UNKNOWN);

        imageFormatProcessor = DataProcessor.create(new ImageFormat(
                CameraWorkMode.UNKNOWN,
                PhotoFileFormat.UNKNOWN,
                VideoResolution.UNKNOWN,
                VideoFrameRate.UNKNOWN));
        CameraStorageState cameraSSDStorageState = new CameraStorageState(
                CameraWorkMode.UNKNOWN,
                CameraStorageLocation.UNKNOWN,
                CameraSDCardState.UNKNOWN,
                INVALID_AVAILABLE_CAPTURE_COUNT,
                INVALID_AVAILABLE_RECORDING_TIME);
        cameraStorageState = DataProcessor.create(cameraSSDStorageState);
        flatCameraModule = new FlatCameraModule();
        addModule(flatCameraModule);
    }
    //endregion

    //region Data

    @NonNull
    public ComponentIndexType getCameraIndex() {
        return cameraIndex;
    }

    @NonNull
    public CameraLensType getLensType() {
        return lensType;
    }

    @Override
    public void updateCameraSource(@NonNull ComponentIndexType cameraIndex, @NonNull CameraLensType lensType) {
        this.cameraIndex = cameraIndex;
        this.lensType = lensType;
        restart();
    }

    /**
     * Get the current image format.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<ImageFormat> getImageFormat() {
        return imageFormatProcessor.toFlowable();
    }

    /**
     * Get the current camera photo storage location.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<CameraStorageState> getCameraStorageState() {
        return cameraStorageState.toFlowable();
    }

    /**
     * Get the current camera color.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<CameraColor> getCameraColor() {
        return cameraColorProcessor.toFlowable();
    }
    //endregion

    //region LifeCycle
    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyCameraStorageLocation, cameraIndex), storageLocationProcessor);
        bindDataProcessor(KeyTools.createCameraKey(CameraKey.KeyVideoResolutionFrameRate, cameraIndex, lensType), resolutionAndFrameRateProcessor);
        bindDataProcessor(KeyTools.createCameraKey(CameraKey.KeyPhotoFileFormat, cameraIndex, lensType), photoFileFormatProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyCameraSDCardState, cameraIndex), sdCardState);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyInternalStorageState, cameraIndex), innerStorageState);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeySDCardAvailablePhotoCount, cameraIndex), sdAvailableCaptureCount);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyInternalStorageAvailablePhotoCount, cameraIndex), innerStorageAvailableCaptureCount);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeySSDAvailableRecordingTimeInSeconds, cameraIndex), sdCardRecordingTime);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyInternalStorageAvailableVideoDuration, cameraIndex), innerStorageRecordingTime);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyCameraColor, cameraIndex), cameraColorProcessor);
    }

    @Override
    protected void inCleanup() {
        // do nothing
    }

    @Override
    protected void updateStates() {
        imageFormatProcessor.onNext(new ImageFormat(flatCameraModule.getCameraModeDataProcessor().getValue(),
                photoFileFormatProcessor.getValue(),
                resolutionAndFrameRateProcessor.getValue().getResolution(),
                resolutionAndFrameRateProcessor.getValue().getFrameRate()));
        updateCameraStorageState();
    }
    //endregion

    //region Helpers
    private void updateCameraStorageState() {
        CameraStorageLocation currentStorageLocation = storageLocationProcessor.getValue();
        if (CameraStorageLocation.UNKNOWN.equals(currentStorageLocation)) {
            return;
        }

        CameraSDCardState sdCardOperationState = null;
        switch (currentStorageLocation){
            case SDCARD:
                if (!CameraSDCardState.UNKNOWN.equals(sdCardState.getValue())) {
                    sdCardOperationState = sdCardState.getValue();
                }
                break;
            case INTERNAL:
                if (!CameraSDCardState.UNKNOWN.equals(innerStorageState.getValue())){
                    sdCardOperationState = innerStorageState.getValue();
                }
                break;
            default:
                break;
        }

        if (sdCardOperationState != null) {
            cameraStorageState.onNext(new CameraStorageState(flatCameraModule.getCameraModeDataProcessor().getValue(),
                    currentStorageLocation,
                    sdCardOperationState,
                    getAvailableCaptureCount(currentStorageLocation),
                    getAvailableRecordingTime(currentStorageLocation)));
        }
    }

    private long getAvailableCaptureCount(CameraStorageLocation storageLocation) {
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

    private int getAvailableRecordingTime(CameraStorageLocation storageLocation) {
        switch (storageLocation) {
            case SDCARD:
                return sdCardRecordingTime.getValue();
            case INTERNAL:
                return innerStorageRecordingTime.getValue();
            case UNKNOWN:
            default:
                return INVALID_AVAILABLE_RECORDING_TIME;
        }
    }
    //endregion

    //region States

    /**
     * The image format info
     */
    public static class ImageFormat {
        private CameraWorkMode cameraMode;
        private PhotoFileFormat photoFileFormat;
        private VideoResolution resolution;
        private VideoFrameRate frameRate;

        protected ImageFormat(@Nullable CameraWorkMode cameraMode,
                              @Nullable PhotoFileFormat photoFileFormat,
                              @Nullable VideoResolution resolution,
                              @Nullable VideoFrameRate frameRate) {
            this.cameraMode = cameraMode;
            this.photoFileFormat = photoFileFormat;
            this.resolution = resolution;
            this.frameRate = frameRate;
        }

        /**
         * Get the current camera mode.
         *
         * @return The current camera mode.
         */
        @Nullable
        public CameraWorkMode getCameraMode() {
            return cameraMode;
        }

        /**
         * Get the current photo file format.
         *
         * @return The current photo file format.
         */
        @Nullable
        public PhotoFileFormat getPhotoFileFormat() {
            return photoFileFormat;
        }

        /**
         * Get the current video resolution.
         *
         * @return The current video resolution.
         */
        @Nullable
        public VideoResolution getResolution() {
            return resolution;
        }

        /**
         * Get the current video frame rate.
         *
         * @return The current video frame rate.
         */
        @Nullable
        public VideoFrameRate getFrameRate() {
            return frameRate;
        }
    }

    /**
     * The camera storage state info.
     */
    public static class CameraStorageState {
        private final CameraWorkMode cameraMode;
        private final long availableCaptureCount;
        private final int availableRecordingTime;
        private CameraStorageLocation storageLocation;
        private CameraSDCardState storageOperationState;

        @VisibleForTesting
        protected CameraStorageState(@NonNull CameraWorkMode cameraMode,
                                     @NonNull CameraStorageLocation storageLocation,
                                     @NonNull CameraSDCardState storageOperationState,
                                     long availableCaptureCount, int availableRecordingTime) {
            this.cameraMode = cameraMode;
            this.storageLocation = storageLocation;
            this.storageOperationState = storageOperationState;
            this.availableCaptureCount = availableCaptureCount;
            this.availableRecordingTime = availableRecordingTime;
        }

        /**
         * Get the current camera mode.
         *
         * @return The current camera mode.
         */
        @NonNull
        public CameraWorkMode getCameraMode() {
            return cameraMode;
        }

        /**
         * Get the current storage location.
         *
         * @return The current storage location.
         */
        @NonNull
        public CameraStorageLocation getStorageLocation() {
            return storageLocation;
        }

        /**
         * Get the current storage operation state.
         *
         * @return The current storage operation state.
         */
        @NonNull
        public CameraSDCardState getStorageOperationState() {
            return storageOperationState;
        }

        /**
         * Get the available capture count in the current storage location.
         *
         * @return The available capture count in the current storage location.
         */
        public long getAvailableCaptureCount() {
            return availableCaptureCount;
        }

        /**
         * Get the available recording time in the current storage location.
         *
         * @return The available recording time in the current storage location.
         */
        public int getAvailableRecordingTime() {
            return availableRecordingTime;
        }

        @Override
        @NonNull
        public String toString() {
            return "CameraStorageState{" +
                    "cameraMode=" + cameraMode +
                    ", storageLocation=" + storageLocation +
                    ", storageOperationState=" + storageOperationState +
                    ", availableCaptureCount=" + availableCaptureCount +
                    ", availableRecordingTime=" + availableRecordingTime +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CameraStorageState that = (CameraStorageState) o;

            if (availableCaptureCount != that.availableCaptureCount) return false;
            if (availableRecordingTime != that.availableRecordingTime) return false;
            if (cameraMode != that.cameraMode) return false;
            if (storageLocation != that.storageLocation) return false;
            return storageOperationState == that.storageOperationState;
        }

        @Override
        public int hashCode() {
            int result = cameraMode != null ? cameraMode.hashCode() : 0;
            result = 31 * result + (storageLocation != null ? storageLocation.hashCode() : 0);
            result = 31 * result + (storageOperationState != null ? storageOperationState.hashCode() : 0);
            result = 31 * result + (int) (availableCaptureCount ^ (availableCaptureCount >>> 32));
            result = 31 * result + availableRecordingTime;
            return result;
        }
    }
    //endregion
}
