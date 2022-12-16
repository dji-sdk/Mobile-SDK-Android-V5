package dji.v5.ux.visualcamera.ndvi

import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.value.camera.MultiSpectralFusionDisplayRange
import dji.sdk.keyvalue.value.camera.MultiSpectralFusionDisplayRangeType
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.v5.et.createCamera
import dji.v5.et.set
import dji.v5.ux.core.base.CameraWidgetModel
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/11/29
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class NDVIStreamPaletteBarPanelModel constructor(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore,
) : CameraWidgetModel(djiSdkModel, keyedStore) {

    val multiSpectralFusionDisplayRangeProcessor = DataProcessor.create(MultiSpectralFusionDisplayRange())

    override fun inSetup() {
        bindDataProcessor(CameraKey.KeyMultiSpectralFusionDisplayRange.createCamera(cameraIndex, CameraLensType.CAMERA_LENS_MS_NDVI), multiSpectralFusionDisplayRangeProcessor)
    }

    fun setFusionRange(leftValue: Int, rightValue: Int) {
        CameraKey.KeyMultiSpectralFusionDisplayRange.createCamera(cameraIndex, CameraLensType.CAMERA_LENS_MS_NDVI)
            .set(MultiSpectralFusionDisplayRange(MultiSpectralFusionDisplayRangeType.CUSTOMIZED, leftValue, rightValue))
    }

    override fun inCleanup() {
        // nothing to clean
    }
}