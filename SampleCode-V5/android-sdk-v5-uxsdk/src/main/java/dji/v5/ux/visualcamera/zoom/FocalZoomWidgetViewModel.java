package dji.v5.ux.visualcamera.zoom;

import android.os.SystemClock;

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.DJIKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.camera.ZoomRatiosRange;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.manager.KeyManager;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Emitter;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/9/5
 * <p>
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class FocalZoomWidgetViewModel extends WidgetModel implements ICameraIndex {

    private static final int SAMPLE_TIME = 500;
    private ComponentIndexType cameraIndex = ComponentIndexType.LEFT_OR_MAIN;
    private CameraLensType lensType = CameraLensType.CAMERA_LENS_ZOOM;
    private Emitter<Double> mSendFocalLengthEmitter;
    private DJIKey<Double> cameraZoomRatiosKey;
    private DJIKey<Double> thermalZoomRatiosKey;
    private DJIKey<ZoomRatiosRange> cameraZoomRatiosRangeKey;
    private DJIKey<ZoomRatiosRange> thermalZoomRatiosRangeKey;
    public final DataProcessor<Double> focalZoomRatios = DataProcessor.create(0.0D);
    public final DataProcessor<ZoomRatiosRange> focalZoomRatiosRange = DataProcessor.create(new ZoomRatiosRange());
    private final DataProcessor<Double> visibleFocalZoomRatios =  DataProcessor.create(0.0D);
    private final DataProcessor<Double> thermalFocalZoomRatios =  DataProcessor.create(0.0D);
    private final DataProcessor<ZoomRatiosRange> visibleFocalZoomRatiosRange =  DataProcessor.create(new ZoomRatiosRange());
    private final DataProcessor<ZoomRatiosRange> thermalFocalZoomRatiosRange =  DataProcessor.create(new ZoomRatiosRange());
    private long mSendFocusDistanceTime = 0;

    public FocalZoomWidgetViewModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @Override
    protected void inSetup() {
        initSendFocalLengthObservable();
        //SDK在Key做过了区分，所以这里直接用
        cameraZoomRatiosKey = KeyTools.createCameraKey(CameraKey.KeyCameraZoomRatios, cameraIndex, CameraLensType.CAMERA_LENS_ZOOM);
        thermalZoomRatiosKey = KeyTools.createCameraKey(CameraKey.KeyThermalZoomRatios, cameraIndex, CameraLensType.CAMERA_LENS_THERMAL);
        bindDataProcessor(cameraZoomRatiosKey, visibleFocalZoomRatios, ratios -> {
            if (lensType == CameraLensType.CAMERA_LENS_ZOOM) {
                focalZoomRatios.onNext(ratios);
            }
        });
        bindDataProcessor(thermalZoomRatiosKey, thermalFocalZoomRatios, ratios -> {
            if (lensType == CameraLensType.CAMERA_LENS_THERMAL) {
                focalZoomRatios.onNext(ratios);
            }
        });

        cameraZoomRatiosRangeKey = KeyTools.createCameraKey(CameraKey.KeyCameraZoomRatiosRange, cameraIndex, CameraLensType.CAMERA_LENS_ZOOM);
        thermalZoomRatiosRangeKey = KeyTools.createCameraKey(CameraKey.KeyThermalZoomRatiosRange, cameraIndex, CameraLensType.CAMERA_LENS_ZOOM);

        bindDataProcessor(cameraZoomRatiosRangeKey, visibleFocalZoomRatiosRange, range -> {
            if (lensType == CameraLensType.CAMERA_LENS_ZOOM) {
                focalZoomRatiosRange.onNext(range);
            }
        });
        bindDataProcessor(thermalZoomRatiosRangeKey, thermalFocalZoomRatiosRange, range -> {
            if (lensType == CameraLensType.CAMERA_LENS_THERMAL) {
                focalZoomRatiosRange.onNext(range);
            }
        });
    }

    @Override
    protected void inCleanup() {
        //do nothig
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

    private void initSendFocalLengthObservable() {
        Disposable disposable = Observable.<Double>create(emitter -> mSendFocalLengthEmitter = emitter)
                .filter((value) -> {
                    if (SystemClock.uptimeMillis() - mSendFocusDistanceTime > SAMPLE_TIME) {
                        sendFocusDistance(value);
                        return false;
                    } else {
                        return true;
                    }
                })
                .throttleLast(SAMPLE_TIME, TimeUnit.MILLISECONDS)
                .subscribe(this::sendFocusDistance);
        addDisposable(disposable);
    }

    public void setFocusDistance(double value) {
        if (mSendFocalLengthEmitter != null) {
            mSendFocalLengthEmitter.onNext(value);
        }
    }

    private void sendFocusDistance(double value) {
        mSendFocusDistanceTime = SystemClock.uptimeMillis();
        if (lensType == CameraLensType.CAMERA_LENS_ZOOM) {
            KeyManager.getInstance().setValue(cameraZoomRatiosKey, value, null);
        } else if (lensType == CameraLensType.CAMERA_LENS_THERMAL) {
            KeyManager.getInstance().setValue(thermalZoomRatiosKey, value, null);
        }
    }
}
