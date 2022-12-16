package dji.v5.ux.core.widget.hsi;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import dji.sdk.keyvalue.key.GimbalKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.common.Attitude;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.gimbal.GimbalAttitudeRange;
import dji.v5.manager.KeyManager;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;

/**
 * Description :
 *
 * @author: Byte.Cai
 * date : 2022/11/1
 * <p>
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
public class GimbalPitchBarModel extends WidgetModel implements ICameraIndex {
    private ComponentIndexType cameraIndex = ComponentIndexType.LEFT_OR_MAIN;
    private CameraLensType lensType = CameraLensType.CAMERA_LENS_ZOOM;
    private final List<DataProcessor<Attitude>> gimbalAttitudeInDegreesProcessorList = new ArrayList<>();
    private final DataProcessor<Attitude> gimbalAttitudeInDegrees0Processor = DataProcessor.create(new Attitude());
    private final DataProcessor<Attitude> gimbalAttitudeInDegrees1Processor = DataProcessor.create(new Attitude());
    private final DataProcessor<Attitude> gimbalAttitudeInDegrees2Processor = DataProcessor.create(new Attitude());


    private final List<DataProcessor<GimbalAttitudeRange>> gimbalAttitudeGimbalAttitudeRangeProcessorList = new ArrayList<>();
    private final DataProcessor<GimbalAttitudeRange> gimbalAttitudeGimbalAttitudeRange0Processor = DataProcessor.create(new GimbalAttitudeRange());
    private final DataProcessor<GimbalAttitudeRange> gimbalAttitudeGimbalAttitudeRange1Processor = DataProcessor.create(new GimbalAttitudeRange());
    private final DataProcessor<GimbalAttitudeRange> gimbalAttitudeGimbalAttitudeRange2Processor = DataProcessor.create(new GimbalAttitudeRange());

    protected GimbalPitchBarModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }



    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createKey(GimbalKey.KeyGimbalAttitude, ComponentIndexType.LEFT_OR_MAIN), gimbalAttitudeInDegrees0Processor);
        bindDataProcessor(KeyTools.createKey(GimbalKey.KeyGimbalAttitude, ComponentIndexType.RIGHT), gimbalAttitudeInDegrees1Processor);
        bindDataProcessor(KeyTools.createKey(GimbalKey.KeyGimbalAttitude, ComponentIndexType.UP), gimbalAttitudeInDegrees2Processor);
        gimbalAttitudeInDegreesProcessorList.add(gimbalAttitudeInDegrees0Processor);
        gimbalAttitudeInDegreesProcessorList.add(gimbalAttitudeInDegrees1Processor);
        gimbalAttitudeInDegreesProcessorList.add(gimbalAttitudeInDegrees2Processor);

        bindDataProcessor(KeyTools.createKey(GimbalKey.KeyGimbalAttitudeRange, ComponentIndexType.LEFT_OR_MAIN),gimbalAttitudeGimbalAttitudeRange0Processor);
        bindDataProcessor(KeyTools.createKey(GimbalKey.KeyGimbalAttitudeRange, ComponentIndexType.RIGHT), gimbalAttitudeGimbalAttitudeRange1Processor);
        bindDataProcessor(KeyTools.createKey(GimbalKey.KeyGimbalAttitudeRange, ComponentIndexType.UP), gimbalAttitudeGimbalAttitudeRange2Processor);
        gimbalAttitudeGimbalAttitudeRangeProcessorList.add(gimbalAttitudeGimbalAttitudeRange0Processor);
        gimbalAttitudeGimbalAttitudeRangeProcessorList.add(gimbalAttitudeGimbalAttitudeRange1Processor);
        gimbalAttitudeGimbalAttitudeRangeProcessorList.add(gimbalAttitudeGimbalAttitudeRange2Processor);
    }

    @Override
    protected void inCleanup() {
        KeyManager.getInstance().cancelListen(this);
        gimbalAttitudeInDegreesProcessorList.clear();
        gimbalAttitudeGimbalAttitudeRangeProcessorList.clear();

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

    public List<DataProcessor<Attitude>> getGimbalAttitudeInDegreesProcessorList() {
        return gimbalAttitudeInDegreesProcessorList;
    }

    public List<DataProcessor<GimbalAttitudeRange>> getGimbalAttitudeGimbalAttitudeRangeProcessorList() {
        return gimbalAttitudeGimbalAttitudeRangeProcessorList;
    }

}
