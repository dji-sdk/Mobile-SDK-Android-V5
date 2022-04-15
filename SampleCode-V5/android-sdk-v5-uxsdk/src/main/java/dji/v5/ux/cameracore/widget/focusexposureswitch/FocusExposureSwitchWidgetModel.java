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

package dji.v5.ux.cameracore.widget.focusexposureswitch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.camera.CameraFocusMode;
import dji.sdk.keyvalue.value.camera.CameraMeteringMode;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.GlobalPreferenceKeys;
import dji.v5.ux.core.communication.GlobalPreferencesInterface;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.communication.UXKey;
import dji.v5.ux.core.communication.UXKeys;
import dji.v5.ux.core.util.DataProcessor;
import dji.v5.ux.core.util.RxUtil;
import dji.v5.ux.core.util.SettingDefinitions.ControlMode;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Focus Exposure Switch Widget Model
 * <p>
 * Widget Model for the {@link FocusExposureSwitchWidget} used to define the
 * underlying logic and communication
 */
public class FocusExposureSwitchWidgetModel extends WidgetModel implements ICameraIndex {

    //region Fields
    private final String TAG = LogUtils.getTag(this);
    private final DataProcessor<Boolean> isFocusModeSupportedDataProcessor;
    private final DataProcessor<CameraFocusMode> focusModeDataProcessor;
    private final DataProcessor<CameraMeteringMode> meteringModeDataProcessor;
    private final DataProcessor<ControlMode> controlModeDataProcessor;
    private final ObservableInMemoryKeyedStore keyedStore;
    private final GlobalPreferencesInterface preferencesManager;
    private UXKey controlModeKey;
    private ComponentIndexType cameraIndex = ComponentIndexType.LEFT_OR_MAIN;
    private CameraLensType lensType = CameraLensType.CAMERA_LENS_ZOOM;
    //endregion

    //region Lifecycle
    public FocusExposureSwitchWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                          @NonNull ObservableInMemoryKeyedStore keyedStore,
                                          @Nullable GlobalPreferencesInterface preferencesManager) {
        super(djiSdkModel, keyedStore);
        focusModeDataProcessor = DataProcessor.create(CameraFocusMode.UNKNOWN);
        meteringModeDataProcessor = DataProcessor.create(CameraMeteringMode.UNKNOWN);
        controlModeDataProcessor = DataProcessor.create(ControlMode.SPOT_METER);
        isFocusModeSupportedDataProcessor = DataProcessor.create(false);
        if (preferencesManager != null) {
            controlModeDataProcessor.onNext(preferencesManager.getControlMode());
        }
        this.preferencesManager = preferencesManager;
        this.keyedStore = keyedStore;
    }


    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createCameraKey(CameraKey.KeyCameraFocusMode,cameraIndex, lensType), focusModeDataProcessor);
        bindDataProcessor(KeyTools.createCameraKey(CameraKey.KeyCameraMeteringMode,cameraIndex, lensType), meteringModeDataProcessor);

        controlModeKey = UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE);
        bindDataProcessor(controlModeKey, controlModeDataProcessor);

        if (preferencesManager != null) {
            preferencesManager.setUpListener();
        }
    }

    @Override
    protected void inCleanup() {
        if (preferencesManager != null) {
            preferencesManager.cleanup();
        }
    }

    @Override
    protected void updateStates() {
        updateFocusMode();
    }

    @Override
    protected void onProductConnectionChanged(boolean isConnected) {
        super.onProductConnectionChanged(isConnected);
        if (isConnected) {
            isFocusModeSupportedDataProcessor.onNext(djiSdkModel.isKeySupported(KeyTools.createCameraKey(CameraKey.KeyCameraFocusMode,cameraIndex, lensType)));
        } else {
            isFocusModeSupportedDataProcessor.onNext(false);
        }
    }
    //endregion

    //region Data

    /**
     * Check if focus mode change is supported
     *
     * @return Flowable with boolean true - supported false - not supported
     */
    public Flowable<Boolean> isFocusModeChangeSupported() {
        return isFocusModeSupportedDataProcessor.toFlowable();
    }


    /**
     * Get control mode
     *
     * @return Flowable with instance of {@link ControlMode}
     */
    public Flowable<ControlMode> getControlMode() {
        return controlModeDataProcessor.toFlowable();
    }

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
     * Switch between exposure/metering mode and focus mode
     *
     * @return Completable representing the success/failure of the set action.
     */
    public Completable switchControlMode() {
        ControlMode currentControlMode = controlModeDataProcessor.getValue();
        if (currentControlMode == ControlMode.SPOT_METER || currentControlMode == ControlMode.CENTER_METER) {
            return setFocusMode();
        } else {
            return setMeteringMode();
        }
    }
    //endregion

    // region private methods
    private void updateFocusMode() {
        ControlMode currentControlMode = controlModeDataProcessor.getValue();
        if (currentControlMode != ControlMode.SPOT_METER && currentControlMode != ControlMode.CENTER_METER) {
            setFocusMode();
        }
    }

    private Completable setMeteringMode() {
        return djiSdkModel.setValue(KeyTools.createCameraKey(CameraKey.KeyCameraMeteringMode,cameraIndex, lensType), CameraMeteringMode.SPOT)
                .doOnComplete(
                        () -> {
                            preferencesManager.setControlMode(ControlMode.SPOT_METER);
                            addDisposable(keyedStore.setValue(controlModeKey, ControlMode.SPOT_METER)
                                    .subscribe(() -> {
                                        //do nothing
                                    }, RxUtil.logErrorConsumer(TAG, "setMeteringMode: ")));
                        }).doOnError(
                        error -> {
                            setFocusMode();
                        }
                );
    }

    private Completable setFocusMode() {
        if (controlModeKey == null || preferencesManager == null) {
            return Completable.complete();
        }
        LogUtils.d(TAG, "In setFocusMode ControlModeKey Value Type " + controlModeKey.getValueType());
        if (focusModeDataProcessor.getValue() == CameraFocusMode.MANUAL) {
            preferencesManager.setControlMode(ControlMode.MANUAL_FOCUS);
            return keyedStore.setValue(controlModeKey, ControlMode.MANUAL_FOCUS);
        } else if (focusModeDataProcessor.getValue() == CameraFocusMode.AFC) {
            preferencesManager.setControlMode(ControlMode.AUTO_FOCUS_CONTINUE);
            return keyedStore.setValue(controlModeKey, ControlMode.AUTO_FOCUS_CONTINUE);
        } else {
            preferencesManager.setControlMode(ControlMode.AUTO_FOCUS);
            return keyedStore.setValue(controlModeKey, ControlMode.AUTO_FOCUS);
        }
    }
    //endregion


}
