package dji.v5.ux.cameracore.widget.fpvinteraction;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.camera.CameraExposureCompensation;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Completable;

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/8/2
 * <p>
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
public class ExposureMeteringWidgetModel extends WidgetModel implements ICameraIndex {

    public final DataProcessor<List<CameraExposureCompensation>> compensationRangeProcessor = DataProcessor.create(new ArrayList<>());
    public final DataProcessor<CameraExposureCompensation> exposureCompensationProcessor = DataProcessor.create(CameraExposureCompensation.NEG_0EV);

    private ComponentIndexType cameraIndex = ComponentIndexType.LEFT_OR_MAIN;
    private CameraLensType lensType = CameraLensType.CAMERA_LENS_ZOOM;

    public ExposureMeteringWidgetModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    public Completable setEV(int value) {
        if (compensationRangeProcessor.getValue().size() <= value){
            return Completable.complete();
        }
        return djiSdkModel.setValue(KeyTools.createCameraKey(CameraKey.KeyExposureCompensation, cameraIndex, lensType),
                compensationRangeProcessor.getValue().get(value));
    }

    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createCameraKey(CameraKey.KeyExposureCompensationRange, cameraIndex, lensType), compensationRangeProcessor);
        bindDataProcessor(KeyTools.createCameraKey(CameraKey.KeyExposureCompensation, cameraIndex, lensType), exposureCompensationProcessor);
    }

    @Override
    protected void inCleanup() {
        //do nothing
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
