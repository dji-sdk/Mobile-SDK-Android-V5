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

package dji.v5.ux.cameracore.widget.cameracapture;

import androidx.annotation.NonNull;
import dji.sdk.keyvalue.value.camera.CameraMode;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.module.FlatCameraModule;
import io.reactivex.rxjava3.core.Flowable;

/**
 * Camera Capture Widget Model
 * <p>
 * Widget Model for {@link CameraCaptureWidget} used to define underlying logic
 * and communication
 */
public class CameraCaptureWidgetModel extends WidgetModel implements ICameraIndex {

    //region Fields
    private FlatCameraModule flatCameraModule;
    private ComponentIndexType cameraIndex = ComponentIndexType.LEFT_OR_MAIN;
    private CameraLensType lensType = CameraLensType.CAMERA_LENS_ZOOM;

    //region Lifecycle
    public CameraCaptureWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                    @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        flatCameraModule = new FlatCameraModule();
        addModule(flatCameraModule);
    }

    @Override
    protected void inSetup() {
        // do nothing
    }

    @Override
    protected void inCleanup() {
        // do nothing
    }

    @Override
    public void updateCameraSource(@NonNull ComponentIndexType cameraIndex, @NonNull CameraLensType lensType) {
        this.cameraIndex = cameraIndex;
        this.lensType = lensType;
        flatCameraModule.updateCameraSource(cameraIndex,lensType);
        restart();
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
    protected void updateStates() {
        // Empty function
    }
    //endregion

    //region Data

    /**
     * Get the current camera Mode
     *
     * @return Flowable with {@link CameraMode} instance
     */
    public Flowable<CameraMode> getCameraMode() {
        return flatCameraModule.getCameraModeDataProcessor().toFlowable();
    }
    //endregion
}
