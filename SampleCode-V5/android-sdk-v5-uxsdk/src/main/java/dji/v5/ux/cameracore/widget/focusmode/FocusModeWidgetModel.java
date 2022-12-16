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

package dji.v5.ux.cameracore.widget.focusmode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.DJIKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.camera.CameraFocusMode;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
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
import dji.v5.ux.core.util.SettingDefinitions;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Focus Mode Widget Model
 * <p>
 * Widget Model for the {@link FocusModeWidget} used to define the
 * underlying logic and communication
 */
public class FocusModeWidgetModel extends WidgetModel implements ICameraIndex {

    //region Fields
    private final DataProcessor<Boolean> isFocusModeSupportedDataProcessor;
    private final DataProcessor<CameraFocusMode> focusModeDataProcessor;
    private final DataProcessor<Boolean> isAFCSupportedProcessor;
    private final DataProcessor<Boolean> isAFCEnabledKeyProcessor;
    private final DataProcessor<Boolean> isAFCEnabledProcessor;
    private final DataProcessor<SettingDefinitions.ControlMode> controlModeProcessor;
    private final ObservableInMemoryKeyedStore keyedStore;
    private final GlobalPreferencesInterface preferencesManager;
    private DJIKey<CameraFocusMode> focusModeKey;
    private UXKey controlModeKey;
    private ComponentIndexType cameraIndex = ComponentIndexType.LEFT_OR_MAIN;
    private CameraLensType lensType = CameraLensType.CAMERA_LENS_ZOOM;
    //endregion

    //region Lifecycle
    public FocusModeWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                @NonNull ObservableInMemoryKeyedStore keyedStore,
                                @Nullable GlobalPreferencesInterface preferencesManager) {
        super(djiSdkModel, keyedStore);
        focusModeDataProcessor = DataProcessor.create(CameraFocusMode.UNKNOWN);
        isAFCSupportedProcessor = DataProcessor.create(false);
        isAFCEnabledKeyProcessor = DataProcessor.create(false);
        isAFCEnabledProcessor = DataProcessor.create(false);
        isFocusModeSupportedDataProcessor = DataProcessor.create(false);
        controlModeProcessor = DataProcessor.create(SettingDefinitions.ControlMode.SPOT_METER);
        if (preferencesManager != null) {
            isAFCEnabledKeyProcessor.onNext(preferencesManager.getAFCEnabled());
            updateAFCEnabledProcessor();
            controlModeProcessor.onNext(preferencesManager.getControlMode());
        }
        this.preferencesManager = preferencesManager;
        this.keyedStore = keyedStore;
    }

    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createCameraKey(CameraKey.KeyCameraFocusMode,cameraIndex, lensType), focusModeDataProcessor);
        bindDataProcessor(KeyTools.createCameraKey(CameraKey.KeyIsAFCSupported,cameraIndex, lensType), isAFCSupportedProcessor);
        UXKey afcEnabledKey = UXKeys.create(GlobalPreferenceKeys.AFC_ENABLED);
        bindDataProcessor(afcEnabledKey, isAFCEnabledKeyProcessor);

        controlModeKey = UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE);
        bindDataProcessor(controlModeKey, controlModeProcessor);

        focusModeKey = KeyTools.createCameraKey(CameraKey.KeyCameraFocusMode,cameraIndex,lensType);
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
        updateAFCEnabledProcessor();
    }

    private void updateAFCEnabledProcessor() {
        isAFCEnabledProcessor.onNext(isAFCEnabledKeyProcessor.getValue() && isAFCSupportedProcessor.getValue());
    }

    @Override
    protected void onProductConnectionChanged(boolean isConnected) {
//        super.onProductConnectionChanged(isConnected);
//        if (isConnected) {
//            isFocusModeSupportedDataProcessor.onNext(djiSdkModel.isKeySupported(focusModeKey));
//        } else {
//            isFocusModeSupportedDataProcessor.onNext(false);
//        }
    }

    //endregion

    //region Actions

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
     * Switch between focus modes
     *
     * @return Completable representing the success/failure of set action
     */
    public Completable toggleFocusMode() {
        final CameraFocusMode currentFocusMode = focusModeDataProcessor.getValue();
        final CameraFocusMode nextFocusMode = getNextFocusMode(currentFocusMode);

        return djiSdkModel.setValue(focusModeKey, nextFocusMode)
                .doOnComplete(() -> onFocusModeUpdate(nextFocusMode))
                .doOnError(error -> focusModeDataProcessor.onNext(currentFocusMode));
    }

    private CameraFocusMode getNextFocusMode(CameraFocusMode currentFocusMode) {
        CameraFocusMode nextFocusMode;
        if (currentFocusMode == CameraFocusMode.MANUAL) {
            if (isAFCEnabledProcessor.getValue()) {
                nextFocusMode = CameraFocusMode.AFC;
            } else {
                nextFocusMode = CameraFocusMode.AF;
            }
        } else {
            nextFocusMode = CameraFocusMode.MANUAL;
        }
        return nextFocusMode;
    }
    //endregion

    //region Data

    /**
     * Get the current {@link CameraFocusMode}
     *
     * @return Flowable with instance of FocusMode
     */
    public Flowable<CameraFocusMode> getFocusMode() {
        return focusModeDataProcessor.toFlowable();
    }

    /**
     * Check if Auto Focus Continuous(AFC) is enabled
     *
     * @return Flowable with boolean true - AFC enabled false - AFC not enabled
     */
    public Flowable<Boolean> isAFCEnabled() {
        return isAFCEnabledProcessor.toFlowable();
    }

    /**
     * Check if focus mode change is supported
     *
     * @return Flowable with boolean true - supported false - not supported
     */
    public Flowable<Boolean> isFocusModeChangeSupported() {
        return isFocusModeSupportedDataProcessor.toFlowable();
    }
    //endregion

    //region Helpers
    private void onFocusModeUpdate(CameraFocusMode focusMode) {
        if (controlModeProcessor.getValue() == SettingDefinitions.ControlMode.SPOT_METER ||
                controlModeProcessor.getValue() == SettingDefinitions.ControlMode.CENTER_METER) {
            return;
        }

        switch (focusMode) {
            case AF:
                preferencesManager.setControlMode(SettingDefinitions.ControlMode.AUTO_FOCUS);
                addDisposable(keyedStore.setValue(controlModeKey, SettingDefinitions.ControlMode.AUTO_FOCUS)
                        .subscribe(() -> {
                            //do nothing
                        }, RxUtil.logErrorConsumer(tag, "setControlModeAutoFocus: ")));
                break;
            case AFC:
                preferencesManager.setControlMode(SettingDefinitions.ControlMode.AUTO_FOCUS_CONTINUE);
                addDisposable(keyedStore.setValue(controlModeKey, SettingDefinitions.ControlMode.AUTO_FOCUS_CONTINUE)
                        .subscribe(() -> {
                            //do nothing
                        }, RxUtil.logErrorConsumer(tag, "setControlModeAutoFocusContinuous: ")));
                break;
            case MANUAL:
                preferencesManager.setControlMode(SettingDefinitions.ControlMode.MANUAL_FOCUS);
                addDisposable(keyedStore.setValue(controlModeKey, SettingDefinitions.ControlMode.MANUAL_FOCUS)
                        .subscribe(() -> {
                            //do nothing
                        }, RxUtil.logErrorConsumer(tag, "setControlModeManualFocus: ")));
                break;
            default:
                break;
        }
    }
    //endregion
}
