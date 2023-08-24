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

package dji.v5.ux.cameracore.widget.cameracapture.recordvideo;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.camera.CameraSDCardState;
import dji.sdk.keyvalue.value.camera.CameraStorageInfo;
import dji.sdk.keyvalue.value.camera.CameraStorageInfos;
import dji.sdk.keyvalue.value.camera.CameraStorageLocation;
import dji.sdk.keyvalue.value.camera.CameraType;
import dji.sdk.keyvalue.value.camera.SSDOperationState;
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
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Record Video Widget Model
 * <p>
 * Widget Model for {@link RecordVideoWidget} used to define underlying
 * logic and communication
 */
public class RecordVideoWidgetModel extends WidgetModel implements ICameraIndex {

    private static final int INVALID_AVAILABLE_RECORDING_TIME = -1;
    private static final int MAX_VIDEO_TIME_THRESHOLD_MINUTES = 29;
    private static final int SECONDS_PER_MIN = 60;

    private final DataProcessor<CameraVideoStorageState> cameraVideoStorageState;
    private final DataProcessor<Boolean> isRecording;
    private final DataProcessor<String> cameraDisplayName;
    private final DataProcessor<CameraType> cameraType;
    private final DataProcessor<Integer> recordingTimeInSeconds;
    private final DataProcessor<VideoResolutionFrameRate> recordedVideoParameters;
    private final DataProcessor<VideoResolutionFrameRate> nonSSDRecordedVideoParameters;
    private final DataProcessor<VideoResolutionFrameRate> ssdRecordedVideoParameters;
    private final DataProcessor<CameraStorageLocation> storageLocation;
    private final DataProcessor<CameraSDCardState> sdCardState;
    private final DataProcessor<CameraSDCardState> storageState;
    private final DataProcessor<CameraSDCardState> innerStorageState;
    private final DataProcessor<SSDOperationState> ssdState;
    private final DataProcessor<CameraStorageInfos> cameraStorageInfos;
    private final DataProcessor<RecordingState> recordingStateProcessor;
    private final FlatCameraModule flatCameraModule;
    private ComponentIndexType cameraIndex = ComponentIndexType.LEFT_OR_MAIN;
    private CameraLensType lensType = CameraLensType.CAMERA_LENS_ZOOM;
    private boolean lastIsRecording = false;
    //endregion

    //region Constructor
    public RecordVideoWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                  @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
        CameraSDVideoStorageState cameraSDVideoStorageState = new CameraSDVideoStorageState(
                CameraStorageLocation.SDCARD,
                0,
                CameraSDCardState.NOT_INSERTED);
        cameraVideoStorageState = DataProcessor.create(cameraSDVideoStorageState);
        isRecording = DataProcessor.create(false);
        cameraDisplayName = DataProcessor.create("");
        cameraType = DataProcessor.create(CameraType.NOT_SUPPORTED);
        recordingTimeInSeconds = DataProcessor.create(0);
        VideoResolutionFrameRate resolutionAndFrameRate = new VideoResolutionFrameRate(
                VideoResolution.UNKNOWN,
                VideoFrameRate.UNKNOWN);
        recordedVideoParameters = DataProcessor.create(resolutionAndFrameRate);
        nonSSDRecordedVideoParameters = DataProcessor.create(resolutionAndFrameRate);
        ssdRecordedVideoParameters = DataProcessor.create(resolutionAndFrameRate);
        storageLocation = DataProcessor.create(CameraStorageLocation.SDCARD);
        sdCardState = DataProcessor.create(CameraSDCardState.UNKNOWN_ERROR);
        storageState = DataProcessor.create(CameraSDCardState.NORMAL);
        innerStorageState = DataProcessor.create(CameraSDCardState.UNKNOWN_ERROR);
        ssdState = DataProcessor.create(SSDOperationState.UNKNOWN);
        cameraStorageInfos = DataProcessor.create(new CameraStorageInfos(CameraStorageLocation.UNKNOWN, new ArrayList<>()));
        recordingStateProcessor = DataProcessor.create(RecordingState.UNKNOWN);
        flatCameraModule = new FlatCameraModule();
        addModule(flatCameraModule);
    }
    //endregion

    //region Lifecycle
    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyIsRecording, cameraIndex), isRecording, newValue -> {
            if (newValue == null) {
                recordingStateProcessor.onNext(RecordingState.UNKNOWN);
                return;
            }
            if (lastIsRecording && !newValue) { //只有从ture变化为false时，才发stopped
                recordingStateProcessor.onNext(RecordingState.RECORDING_STOPPED);
            } else if (newValue) {
                recordingStateProcessor.onNext(RecordingState.RECORDING_IN_PROGRESS);
            } else {
                recordingStateProcessor.onNext(RecordingState.RECORDING_NOT_STARED);
            }
            lastIsRecording = newValue;
        });
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyRecordingTime, cameraIndex), recordingTimeInSeconds);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyCameraType, cameraIndex), cameraType, type -> cameraDisplayName.onNext(type.name()));
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyCameraStorageLocation, cameraIndex), storageLocation);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyCameraSDCardState, cameraIndex), sdCardState);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyInternalStorageState, cameraIndex), innerStorageState);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeySSDOperationState, cameraIndex), ssdState);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyCameraStorageInfos, cameraIndex), cameraStorageInfos);

        // Resolution and Frame Rates
        bindDataProcessor(KeyTools.createCameraKey(CameraKey.KeyVideoResolutionFrameRate, cameraIndex, lensType), nonSSDRecordedVideoParameters);
        bindDataProcessor(KeyTools.createCameraKey(CameraKey.KeySSDVideoResolutionFrameRate, cameraIndex, lensType), ssdRecordedVideoParameters);
    }

    @Override
    protected void inCleanup() {
        // Do nothing
    }

    @Override
    protected void updateStates() {
        updateVideoStorageState();
        if (isRecording.getValue()) {
            checkIsOverRecordTime(recordingTimeInSeconds.getValue());
        }
    }

    @Override
    protected void onProductConnectionChanged(boolean isConnected) {
        super.onProductConnectionChanged(isConnected);
        if (!isConnected) {
            recordingStateProcessor.onNext(RecordingState.UNKNOWN);
        }
    }

    public boolean isVideoMode() {
        return flatCameraModule.getCameraModeDataProcessor().getValue().isVideoMode();
    }

    /**
     * Get the current camera video storage state
     *
     * @return Flowable with {@link CameraVideoStorageState} instance
     */
    public Flowable<CameraVideoStorageState> getCameraVideoStorageState() {
        return cameraVideoStorageState.toFlowable();
    }

    /**
     * Check the recording state of the camera
     *
     * @return Flowable with {@link RecordingState} value
     */
    public Flowable<RecordingState> getRecordingState() {
        return recordingStateProcessor.toFlowable();
    }

    /**
     * Get the display name of the camera which the model is reacting to
     *
     * @return Flowable with string of camera name
     */
    public Flowable<String> getCameraDisplayName() {
        return cameraDisplayName.toFlowable();
    }

    /**
     * Get the duration of on going video recording in seconds
     *
     * @return Flowable with integer value representing seconds
     */
    public Flowable<Integer> getRecordingTimeInSeconds() {
        return recordingTimeInSeconds.toFlowable();
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
        restart();
    }
    //endregion

    //region Actions

    /**
     * Start video recording
     *
     * @return Completable to determine the status of the action
     */
    public Completable startRecordVideo() {
        if (isRecording.getValue()) {
            return Completable.complete();
        }
        return djiSdkModel.performActionWithOutResult(KeyTools.createKey(CameraKey.KeyStartRecord, cameraIndex));
    }

    /**
     * Stop video recording
     *
     * @return Completable to determine the status of the action
     */
    public Completable stopRecordVideo() {
        if (!isRecording.getValue()) {
            return Completable.complete();
        }

        return djiSdkModel.performActionWithOutResult(KeyTools.createKey(CameraKey.KeyStopRecord, cameraIndex));
    }
    //endregion

    //region Helpers
    private void updateVideoStorageState() {
        CameraStorageLocation currentStorageLocation = storageLocation.getValue();
        if (CameraStorageLocation.UNKNOWN.equals(currentStorageLocation)) {
            return;
        }

        int availableRecordingTime = getAvailableRecordingTime();

        CameraVideoStorageState newCameraVideoStorageState = null;
        if (CameraStorageLocation.SDCARD.equals(currentStorageLocation)) {
            if (!CameraSDCardState.UNKNOWN_ERROR.equals(sdCardState.getValue())) {
                newCameraVideoStorageState = new CameraSDVideoStorageState(currentStorageLocation, availableRecordingTime, sdCardState.getValue());
            } else if (!CameraSDCardState.UNKNOWN_ERROR.equals(storageState.getValue())) {
                newCameraVideoStorageState = new CameraSDVideoStorageState(currentStorageLocation, availableRecordingTime, storageState.getValue());
            }
            recordedVideoParameters.onNext(nonSSDRecordedVideoParameters.getValue());
        } else if (CameraStorageLocation.INTERNAL.equals(currentStorageLocation)) {
            newCameraVideoStorageState = new CameraSDVideoStorageState(currentStorageLocation, availableRecordingTime, innerStorageState.getValue());
        }

        if (newCameraVideoStorageState != null) {
            cameraVideoStorageState.onNext(newCameraVideoStorageState);
        }
    }

    private int getAvailableRecordingTime() {
        CameraStorageInfo info = cameraStorageInfos.getValue().getCurrentCameraStorageInfo();
        if (info == null) {
            return INVALID_AVAILABLE_RECORDING_TIME;
        }
        Integer availableVideoDuration = info.getAvailableVideoDuration();
        return availableVideoDuration == null ? INVALID_AVAILABLE_RECORDING_TIME : availableVideoDuration;
    }

    /**
     * Determine if the time is exceeded, and close the video in
     * {@link RecordVideoWidgetModel#MAX_VIDEO_TIME_THRESHOLD_MINUTES} minutes.
     *
     * @param recordTime The time to check.
     */
    private void checkIsOverRecordTime(int recordTime) {
        if (recordTime > (MAX_VIDEO_TIME_THRESHOLD_MINUTES * SECONDS_PER_MIN)) {
            stopRecordVideo();
        }
    }

    //endregion

    //region Classes

    /**
     * The recording state of the camera.
     */
    public enum RecordingState {

        /**
         * No product is connected, or the recording state is unknown.
         */
        UNKNOWN,

        RECORDING_NOT_STARED,

        /**
         * The camera is recording video.
         */
        RECORDING_IN_PROGRESS,

        /**
         * The camera is not recording video.
         */
        RECORDING_STOPPED


    }
    //endregion
}
