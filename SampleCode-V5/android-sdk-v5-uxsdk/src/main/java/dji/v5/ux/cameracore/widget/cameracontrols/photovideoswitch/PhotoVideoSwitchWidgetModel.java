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

package dji.v5.ux.cameracore.widget.cameracontrols.photovideoswitch;

import androidx.annotation.NonNull;
import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.camera.CameraWorkMode;
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
 * Photo Video Switch Widget Model
 * <p>
 * Widget Model for the {@link PhotoVideoSwitchWidget} used to define the
 * underlying logic and communication
 */
public class PhotoVideoSwitchWidgetModel extends WidgetModel implements ICameraIndex {

    //region Fields
    private final DataProcessor<Boolean> isCameraConnectedDataProcessor;
    private final DataProcessor<Boolean> isRecordingDataProcessor;
    private final DataProcessor<Boolean> isShootingDataProcessor;
    private final DataProcessor<Boolean> isShootingIntervalDataProcessor;
    private final DataProcessor<Boolean> isShootingBurstDataProcessor;
    private final DataProcessor<Boolean> isShootingRawBurstDataProcessor;
    private final DataProcessor<Boolean> isShootingPanoramaDataProcessor;
    private final DataProcessor<Boolean> isEnabledDataProcessor;
    private ComponentIndexType cameraIndex = ComponentIndexType.LEFT_OR_MAIN;
    private CameraLensType lensType = CameraLensType.UNKNOWN;
    private FlatCameraModule flatCameraModule;
    //endregion

    //region Lifecycle
    public PhotoVideoSwitchWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                       @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        isCameraConnectedDataProcessor = DataProcessor.create(false);
        isRecordingDataProcessor = DataProcessor.create(false);
        isShootingDataProcessor = DataProcessor.create(false);
        isShootingIntervalDataProcessor = DataProcessor.create(false);
        isShootingBurstDataProcessor = DataProcessor.create(false);
        isShootingRawBurstDataProcessor = DataProcessor.create(false);
        isShootingPanoramaDataProcessor = DataProcessor.create(false);
        isEnabledDataProcessor = DataProcessor.create(false);
        flatCameraModule = new FlatCameraModule();
        addModule(flatCameraModule);
    }

    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyConnection,cameraIndex), isCameraConnectedDataProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyIsRecording,cameraIndex), isRecordingDataProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyIsShootingPhoto,cameraIndex), isShootingDataProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyCameraShootingContinuousPhotos,cameraIndex), isShootingIntervalDataProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyIsShootingBurstPhoto,cameraIndex), isShootingBurstDataProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyIsShootingRAWBurstPhoto,cameraIndex), isShootingRawBurstDataProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyIsShootingPhotoPanorama,cameraIndex), isShootingPanoramaDataProcessor);
    }

    @Override
    protected void inCleanup() {
        // do nothing
    }

    @Override
    protected void updateStates() {
        boolean isEnabled = productConnectionProcessor.getValue()
                && isCameraConnectedDataProcessor.getValue()
                && !isRecordingDataProcessor.getValue()
                && !isShootingDataProcessor.getValue()
                && !isShootingBurstDataProcessor.getValue()
                && !isShootingIntervalDataProcessor.getValue()
                && !isShootingRawBurstDataProcessor.getValue()
                && !isShootingPanoramaDataProcessor.getValue();

        isEnabledDataProcessor.onNext(isEnabled);
    }
    //endregion

    //region Data

    /**
     * Check if the widget should be enabled.
     *
     * @return Flowable with boolean value
     */
    public Flowable<Boolean> isEnabled() {
        return isEnabledDataProcessor.toFlowable();
    }

    /**
     * Get whether the current camera mode is picture mode.
     *
     * @return Flowable with boolean value
     */
    public Flowable<Boolean> isPictureMode() {
        return flatCameraModule.getCameraModeDataProcessor().toFlowable().map(cameraMode ->
                cameraMode == CameraWorkMode.SHOOT_PHOTO
        );
    }

    /**
     * Toggle between photo mode and video mode
     *
     * @return Completable
     */
    public Completable toggleCameraMode() {
        if (flatCameraModule.getCameraModeDataProcessor().getValue() == CameraWorkMode.SHOOT_PHOTO) {
            return flatCameraModule.setCameraMode(djiSdkModel, CameraWorkMode.RECORD_VIDEO);
        } else {
            return flatCameraModule.setCameraMode(djiSdkModel, CameraWorkMode.SHOOT_PHOTO);
        }
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
}
