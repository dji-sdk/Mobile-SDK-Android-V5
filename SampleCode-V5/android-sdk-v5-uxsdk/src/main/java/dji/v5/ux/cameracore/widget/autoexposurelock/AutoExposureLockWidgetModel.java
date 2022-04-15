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

package dji.v5.ux.cameracore.widget.autoexposurelock;

import androidx.annotation.NonNull;
import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Auto Exposure Lock Widget Model
 * <p>
 * Widget Model for the {@link AutoExposureLockWidget} used to define the
 * underlying logic and communication
 */
public class AutoExposureLockWidgetModel extends WidgetModel implements ICameraIndex {
    //region Fields
    private final DataProcessor<Boolean> autoExposureLockBooleanProcessor;
    private ComponentIndexType cameraIndex = ComponentIndexType.LEFT_OR_MAIN;
    private CameraLensType lensType = CameraLensType.CAMERA_LENS_ZOOM;
    //endregion

    public AutoExposureLockWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                       @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
        autoExposureLockBooleanProcessor = DataProcessor.create(false);
    }

    //region Data

    /**
     * Check if the auto exposure lock is enabled
     *
     * @return Flowable with boolean true - enabled  false - disabled
     */
    public Flowable<Boolean> isAutoExposureLockOn() {
        return autoExposureLockBooleanProcessor.toFlowable();
    }

    //endregion

    //region Actions

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

    /**
     * Set auto exposure lock the opposite of its current state
     *
     * @return Completable representing success and failure of action
     */
    public Completable toggleAutoExposureLock() {
        return djiSdkModel.setValue(KeyTools.createCameraKey(CameraKey.KeyAELockEnabled, cameraIndex, lensType), !autoExposureLockBooleanProcessor.getValue());
    }
    //endregion

    //region Lifecycle
    @Override
    protected void inSetup() {
        KeyTools.createCameraKey(CameraKey.KeyAELockEnabled, cameraIndex, lensType);
        bindDataProcessor(KeyTools.createCameraKey(CameraKey.KeyAELockEnabled, cameraIndex, lensType), autoExposureLockBooleanProcessor);
    }

    @Override
    protected void inCleanup() {
        // nothing to clean
    }

    @Override
    protected void updateStates() {
        // No States
    }

    //endregion
}
