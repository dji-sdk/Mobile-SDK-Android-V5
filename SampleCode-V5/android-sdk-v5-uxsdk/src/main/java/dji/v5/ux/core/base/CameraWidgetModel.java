package dji.v5.ux.core.base;

import androidx.annotation.NonNull;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;


public abstract class CameraWidgetModel extends WidgetModel implements ICameraIndex {

    protected ComponentIndexType cameraIndex = ComponentIndexType.LEFT_OR_MAIN;
    protected CameraLensType lensType = CameraLensType.CAMERA_LENS_ZOOM;

    protected CameraWidgetModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @NonNull
    @Override
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
}
