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

package dji.v5.ux.visualcamera.iso;

import org.reactivestreams.Publisher;

import androidx.annotation.NonNull;
import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.camera.CameraExposureSettings;
import dji.sdk.keyvalue.value.camera.CameraISO;
import dji.sdk.keyvalue.value.camera.ExposureSensitivityMode;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Function;

/**
 * Widget Model for the {@link CameraConfigISOAndEIWidget} used to define
 * the underlying logic and communication
 */
public class CameraConfigISOAndEIWidgetModel extends WidgetModel implements ICameraIndex {

    //region Constants
    /**
     * The value to display when the ISO is locked.
     */
    protected static final String LOCKED_ISO_VALUE = "500";
    //endregion

    //region Fields
    private final DataProcessor<CameraExposureSettings> exposureSettingsProcessor;
    private final DataProcessor<CameraISO> isoProcessor;
    private final DataProcessor<ExposureSensitivityMode> exposureSensitivityModeProcessor;
    private final DataProcessor<Integer> eiValueProcessor;
    private final DataProcessor<String> isoAndEIValueProcessor;
    private ComponentIndexType cameraIndex = ComponentIndexType.LEFT_OR_MAIN;
    private CameraLensType lensType = CameraLensType.CAMERA_LENS_ZOOM;
    //endregion

    //region Constructor
    public CameraConfigISOAndEIWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                           @NonNull ObservableInMemoryKeyedStore keyedStore) {
        super(djiSdkModel, keyedStore);
        exposureSettingsProcessor = DataProcessor.create(new CameraExposureSettings());
        isoProcessor = DataProcessor.create(CameraISO.UNKNOWN);
        exposureSensitivityModeProcessor = DataProcessor.create(ExposureSensitivityMode.UNKNOWN);
        eiValueProcessor = DataProcessor.create(0);
        isoAndEIValueProcessor = DataProcessor.create("");
    }
    //endregion

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
     * Get the ISO.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<CameraISO> getISO() {
        return isoProcessor.toFlowable();
    }

    /**
     * Get either the ISO or the exposure index value as a displayable String.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<String> getISOAndEIValue() {
        return isoAndEIValueProcessor.toFlowable();
    }

    /**
     * Get whether the camera is in EI mode.
     *
     * @return Flowable for the DataProcessor that user should subscribe to.
     */
    public Flowable<Boolean> isEIMode() {
        return exposureSensitivityModeProcessor.toFlowable()
                .concatMap((Function<ExposureSensitivityMode, Publisher<Boolean>>) exposureSensitivityMode ->
                        Flowable.just(exposureSensitivityMode == ExposureSensitivityMode.EI));
    }
    //endregion

    //region LifeCycle
    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createCameraKey(CameraKey.KeyExposureSettings, cameraIndex, lensType), exposureSettingsProcessor);
        bindDataProcessor(KeyTools.createCameraKey(CameraKey.KeyISO, cameraIndex, lensType), isoProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyExposureSensitivityMode, cameraIndex), exposureSensitivityModeProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyEIValue, cameraIndex), eiValueProcessor);
    }

    @Override
    protected void inCleanup() {
        //Nothing to cleanup
    }

    @Override
    protected void updateStates() {
        updateConsolidatedISOValue();
    }
    //endregion

    //region Helpers
    private void updateConsolidatedISOValue() {
        if (exposureSensitivityModeProcessor.getValue() == ExposureSensitivityMode.EI) {
            isoAndEIValueProcessor.onNext(String.valueOf(eiValueProcessor.getValue()));
        } else {
            if (isoProcessor.getValue() == CameraISO.ISO_FIXED && exposureSettingsProcessor.getValue().getIso() == 0) {
                isoAndEIValueProcessor.onNext(LOCKED_ISO_VALUE);
            } else {
                isoAndEIValueProcessor.onNext(String.valueOf(exposureSettingsProcessor.getValue().getIso()));
            }
        }
    }
    //endregion
}
