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

package dji.v5.ux.cameracore.widget.fpvinteraction;

import android.graphics.Point;
import android.graphics.PointF;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.DJIKey;
import dji.sdk.keyvalue.key.GimbalKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.camera.CameraMeteringMode;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.common.DoublePoint2D;
import dji.sdk.keyvalue.value.common.IntPoint2D;
import dji.sdk.keyvalue.value.gimbal.CtrlInfo;
import dji.sdk.keyvalue.value.gimbal.GimbalSpeedRotation;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.GlobalPreferenceKeys;
import dji.v5.ux.core.communication.GlobalPreferencesInterface;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.communication.UXKey;
import dji.v5.ux.core.communication.UXKeys;
import dji.v5.ux.core.util.DataProcessor;
import dji.v5.ux.core.util.SettingDefinitions;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Widget Model for the {@link FPVInteractionWidget} used to define
 * the underlying logic and communication
 */
public class FPVInteractionWidgetModel extends WidgetModel implements ICameraIndex {

    private final DataProcessor<SettingDefinitions.ControlMode> controlModeProcessor;
    private final DataProcessor<CameraMeteringMode> meteringModeProcessor;
    private final DataProcessor<Boolean> aeLockedProcessor;
    private final DataProcessor<Boolean> isYawAdjustSupportedProcessor;
    private final GlobalPreferencesInterface preferencesManager;
    private final ObservableInMemoryKeyedStore keyedStore;
    //region Fields
    private ComponentIndexType cameraIndex = ComponentIndexType.LEFT_OR_MAIN;
    private CameraLensType lensIndex = CameraLensType.CAMERA_LENS_WIDE;
    private DJIKey<DoublePoint2D> focusTargetKey;
    private DJIKey<DoublePoint2D> meteringPointKey;
    private DJIKey<CameraMeteringMode> meteringModeKey;
    private UXKey controlModeKey;
    //endregion

    //region Constructor
    public FPVInteractionWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                     @NonNull ObservableInMemoryKeyedStore keyedStore,
                                     @Nullable GlobalPreferencesInterface preferencesManager) {
        super(djiSdkModel, keyedStore);
        meteringModeProcessor = DataProcessor.create(CameraMeteringMode.UNKNOWN);
        controlModeProcessor = DataProcessor.create(SettingDefinitions.ControlMode.SPOT_METER);
        if (preferencesManager != null) {
            controlModeProcessor.onNext(preferencesManager.getControlMode());
        }
        aeLockedProcessor = DataProcessor.create(false);
        isYawAdjustSupportedProcessor = DataProcessor.create(true);
        this.preferencesManager = preferencesManager;
        this.keyedStore = keyedStore;
    }
    //endregion

    //region Lifecycle
    @Override
    protected void inSetup() {
        focusTargetKey = KeyTools.createCameraKey(CameraKey.KeyCameraFocusTarget, cameraIndex, lensIndex);
        meteringPointKey = KeyTools.createCameraKey(CameraKey.KeySpotMeteringPoint, cameraIndex, lensIndex);
        meteringModeKey = KeyTools.createCameraKey(CameraKey.KeyCameraMeteringMode, cameraIndex, lensIndex);
        bindDataProcessor(meteringModeKey, meteringModeProcessor, this::setMeteringMode);
        bindDataProcessor(KeyTools.createCameraKey(CameraKey.KeyAELockEnabled, cameraIndex, lensIndex), aeLockedProcessor);
        bindDataProcessor(KeyTools.createKey(GimbalKey.KeyYawAdjustSupported, cameraIndex), isYawAdjustSupportedProcessor);
        controlModeKey = UXKeys.create(GlobalPreferenceKeys.CONTROL_MODE);
        bindDataProcessor(controlModeKey, controlModeProcessor);

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
        // do nothing
    }
    //endregion

    //region Helpers
    private void setMeteringMode(CameraMeteringMode meteringMode) {
        if (meteringMode == CameraMeteringMode.REGION) {
            setControlMode(SettingDefinitions.ControlMode.SPOT_METER);
        } else if (meteringMode == CameraMeteringMode.CENTER) {
            setControlMode(SettingDefinitions.ControlMode.CENTER_METER);
        }
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
        return lensIndex;
    }

    @Override
    public void updateCameraSource(@NonNull ComponentIndexType cameraIndex, @NonNull CameraLensType lensType) {
        this.cameraIndex = cameraIndex;
        this.lensIndex = lensType;
        restart();
    }

    /**
     * Set the control mode.
     *
     * @param controlMode The control mode to set.
     * @return Completable representing the success/failure of the set action.
     */
    @NonNull
    public Completable setControlMode(@NonNull SettingDefinitions.ControlMode controlMode) {
        if (preferencesManager != null) {
            preferencesManager.setControlMode(controlMode);
        }
        return keyedStore.setValue(controlModeKey, controlMode);
    }

    /**
     * Get the control mode.
     *
     * @return A Flowable that will emit the current control mode.
     */
    @NonNull
    public Flowable<SettingDefinitions.ControlMode> getControlMode() {
        return controlModeProcessor.toFlowable();
    }

    /**
     * Get whether the automatic exposure is locked.
     *
     * @return A Flowable that will emit a boolean when the automatic exposure locked state changes.
     */
    @NonNull
    public Flowable<Boolean> isAeLocked() {
        return aeLockedProcessor.toFlowable();
    }
    //endregion

    //region Reactions to user input

    /**
     * Set the focus target to the location (targetX, targetY). This is a relative coordinate
     * represented by a percentage of the width and height of the widget.
     *
     * @param targetX The relative x coordinate of the focus target represented by a percentage of
     *                the width.
     * @param targetY The relative y coordinate of the focus target represented by a percentage of
     *                the height.
     * @return Completable representing the success/failure of the set action.
     */
    @NonNull
    public Completable updateFocusTarget(@FloatRange(from = 0, to = 1) float targetX,
                                         @FloatRange(from = 0, to = 1) float targetY) {
        return djiSdkModel.setValue(focusTargetKey, createPointF(targetX, targetY));
    }

    /**
     * Set the spot metering target to the location (targetX, targetY). This is a relative
     * coordinate represented by a percentage of the width and height of the widget.
     *
     * @param targetX The relative x coordinate of the spot metering target represented by a
     *                percentage of the width.
     * @param targetY The relative y coordinate of the spot metering target represented by a
     *                percentage of the height.
     * @return Completable representing the success/failure of the set action.
     */
    @NonNull
    public Completable updateMetering(@FloatRange(from = 0, to = 1) float targetX,
                                      @FloatRange(from = 0, to = 1) float targetY) {
        if (controlModeProcessor.getValue() == SettingDefinitions.ControlMode.SPOT_METER) {
            return djiSdkModel.setValue(meteringPointKey, new DoublePoint2D((double) targetX, (double) targetY));
        } else if (controlModeProcessor.getValue() == SettingDefinitions.ControlMode.CENTER_METER) {
            return djiSdkModel.setValue(meteringModeKey, CameraMeteringMode.CENTER);
        }
        return Completable.complete();
    }

    /**
     * Determine whether the gimbal is able to move in the yaw direction.
     *
     * @return `true` if the current product supports gimbal yaw rotation, `false` otherwise.
     */
    public boolean canRotateGimbalYaw() {
        return isYawAdjustSupportedProcessor.getValue();
    }

    /**
     * Rotate the gimbal using SPEED.
     *
     * @param yaw   The amount to rotate the gimbal in the yaw direction.
     * @param pitch The amount to rotate the gimbal in the pitch direction.
     * @return Completable representing the success/failure of the set action.
     */
    public Completable rotateGimbalBySpeed(double yaw, double pitch) {
        return djiSdkModel.performActionWithOutResult(KeyTools.createKey(GimbalKey.KeyRotateBySpeed, cameraIndex),
                new GimbalSpeedRotation(pitch, yaw, 0.0, new CtrlInfo()));
    }
    //endregion

    //region Unit test helpers

    /**
     * A wrapper for the {@link PointF} constructor so it can be mocked in unit tests.
     *
     * @return A PointF object.
     */
    @VisibleForTesting
    @NonNull
    protected DoublePoint2D createPointF(float x, float y) {
        return new DoublePoint2D((double) x, (double) y);
    }

    /**
     * A wrapper for the {@link Point} constructor so it can be mocked in unit tests.
     *
     * @return A Point object.
     */
    @VisibleForTesting
    @NonNull
    protected IntPoint2D createPoint(int x, int y) {
        return new IntPoint2D(x, y);
    }
    //endregion
}
