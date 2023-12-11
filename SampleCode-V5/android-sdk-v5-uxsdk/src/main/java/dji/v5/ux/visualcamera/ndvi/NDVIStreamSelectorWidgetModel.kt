package dji.v5.ux.visualcamera.ndvi

import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType
import dji.sdk.keyvalue.value.camera.MultiSpectralFusionType
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.et.create
import dji.v5.et.createCamera
import dji.v5.ux.core.base.CameraWidgetModel
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor

class NDVIStreamSelectorWidgetModel constructor(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore,
) : CameraWidgetModel(djiSdkModel, keyedStore) {

    val cameraVideoStreamSourceProcessor: DataProcessor<CameraVideoStreamSourceType> = DataProcessor.create(
        CameraVideoStreamSourceType.UNKNOWN)
    val cameraMultiSpectralFusionTypeProcessor: DataProcessor<MultiSpectralFusionType> = DataProcessor.create(MultiSpectralFusionType.UNKNOWN)

    override fun inSetup() {
        bindDataProcessor(CameraKey.KeyCameraVideoStreamSource.create(cameraIndex), cameraVideoStreamSourceProcessor)
        bindDataProcessor(CameraKey.KeyMultiSpectralFusionType.createCamera(cameraIndex, CameraLensType.CAMERA_LENS_MS_NDVI), cameraMultiSpectralFusionTypeProcessor)
    }

    override fun inCleanup() {
        // nothing to clean
    }

    override fun updateCameraSource(cameraIndex: ComponentIndexType, lensType: CameraLensType) {
        if (this.cameraIndex == cameraIndex) {
            return
        }
        this.cameraIndex = cameraIndex
        restart()
    }
}