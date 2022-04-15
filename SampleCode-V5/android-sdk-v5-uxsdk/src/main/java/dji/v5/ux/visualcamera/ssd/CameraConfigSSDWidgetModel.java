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

package dji.v5.ux.visualcamera.ssd;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.camera.CameraShootPhotoMode;
import dji.sdk.keyvalue.value.camera.CameraWorkMode;
import dji.sdk.keyvalue.value.camera.SSDClipFileNameMsg;
import dji.sdk.keyvalue.value.camera.SSDColor;
import dji.sdk.keyvalue.value.camera.SSDOperationState;
import dji.sdk.keyvalue.value.camera.SSDVideoLicense;
import dji.sdk.keyvalue.value.camera.VideoResolutionFrameRate;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Widget Model for the {@link CameraConfigSSDWidget} used to define
 * the underlying logic and communication
 */
public class CameraConfigSSDWidgetModel extends WidgetModel implements ICameraIndex {

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

    //region Fields
    private final DataProcessor<Boolean> isSSDSupportedProcessor;
    private final DataProcessor<VideoResolutionFrameRate> ssdVideoResolutionAndFrameRateProcessor;
    private final DataProcessor<Integer> ssdRemainingSpaceInMBProcessor;
    private final DataProcessor<SSDClipFileNameMsg> ssdClipFileNameProcessor;
    private final DataProcessor<CameraWorkMode> cameraModeProcessor;
    private final DataProcessor<CameraShootPhotoMode> shootPhotoModeProcessor;
    private final DataProcessor<SSDOperationState> ssdOperationStateProcessor;
    private final DataProcessor<Integer> ssdAvailableRecordingTimeInSecProcessor;
    private final DataProcessor<List<SSDVideoLicense>> activateSSDVideoLicenseProcessor;
    private final DataProcessor<SSDColor> ssdColorProcessor;
    private ComponentIndexType cameraIndex = ComponentIndexType.LEFT_OR_MAIN;
    private CameraLensType lensType = CameraLensType.CAMERA_LENS_ZOOM;
    //endregion

    //region Constructor
    public CameraConfigSSDWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                      @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
        isSSDSupportedProcessor = DataProcessor.create(false);
        ssdVideoResolutionAndFrameRateProcessor = DataProcessor.create(new VideoResolutionFrameRate());
        ssdRemainingSpaceInMBProcessor = DataProcessor.create(INVALID_AVAILABLE_CAPTURE_COUNT);
        ssdClipFileNameProcessor = DataProcessor.create(new SSDClipFileNameMsg());
        cameraModeProcessor = DataProcessor.create(CameraWorkMode.UNKNOWN);
        shootPhotoModeProcessor = DataProcessor.create(CameraShootPhotoMode.UNKNOWN);
        ssdOperationStateProcessor = DataProcessor.create(SSDOperationState.UNKNOWN);
        ssdAvailableRecordingTimeInSecProcessor = DataProcessor.create(INVALID_AVAILABLE_RECORDING_TIME);
        activateSSDVideoLicenseProcessor = DataProcessor.create(new ArrayList<>());
        ssdColorProcessor = DataProcessor.create(SSDColor.UNKNOWN);
    }
    //endregion

    //region Data
    @Override
    @NonNull
    public ComponentIndexType getCameraIndex() {
        return cameraIndex;
    }

    @Override
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
     * Get whether the products supports SSD storage.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<Boolean> isSSDSupported() {
        return isSSDSupportedProcessor.toFlowable();
    }

    /**
     * Get the SSD license.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<List<SSDVideoLicense>> getSSDLicense() {
        return activateSSDVideoLicenseProcessor.toFlowable();
    }

    /**
     * Get the remaining space in MB on the SSD.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<Integer> getSSDRemainingSpace() {
        return ssdRemainingSpaceInMBProcessor.toFlowable();
    }

    /**
     * Get the SSD clip file name.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<SSDClipFileNameMsg> getSSDClipName() {
        return ssdClipFileNameProcessor.toFlowable();
    }

    /**
     * Get the SSD resolution and frame rate.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<VideoResolutionFrameRate> getSSDResolutionAndFrameRate() {
        return ssdVideoResolutionAndFrameRateProcessor.toFlowable();
    }

    /**
     * Get the SSD operation state.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<SSDOperationState> getSSDOperationState() {
        return ssdOperationStateProcessor.toFlowable();
    }

    /**
     * Get the SSD color.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<SSDColor> getSSDColor() {
        return ssdColorProcessor.toFlowable();
    }

    /**
     * Get the shoot photo mode.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<CameraShootPhotoMode> getShootPhotoMode() {
        return shootPhotoModeProcessor.toFlowable();
    }

    /**
     * Get the camera mode.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<CameraWorkMode> getCameraMode() {
        return cameraModeProcessor.toFlowable();
    }
    //endregion

    //region Lifecycle
    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyIsInternalSSDSupported, cameraIndex), isSSDSupportedProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeySSDVideoResolutionFrameRate, cameraIndex), ssdVideoResolutionAndFrameRateProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeySSDRemainingSpaceInMB, cameraIndex), ssdRemainingSpaceInMBProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeySSDClipFileName, cameraIndex), ssdClipFileNameProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyCameraWorkMode, cameraIndex), cameraModeProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyShootPhotoMode, cameraIndex), shootPhotoModeProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeySSDOperationState, cameraIndex), ssdOperationStateProcessor);
        bindDataProcessor(KeyTools.createCameraKey(CameraKey.KeySSDAvailableRecordingTimeInSeconds,cameraIndex, lensType), ssdAvailableRecordingTimeInSecProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeySSDVideoLicenses, cameraIndex), activateSSDVideoLicenseProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeySSDColor, cameraIndex), ssdColorProcessor);
    }

    @Override
    protected void inCleanup() {
        // Nothing to cleanup
    }

    @Override
    protected void updateStates() {
        // Nothing to update
    }
    //endregion
}
