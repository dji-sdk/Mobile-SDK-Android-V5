package dji.v5.ux.visualcamera.ndvi

import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.value.camera.CameraMode
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType
import dji.sdk.keyvalue.value.camera.MultiSpectralFusionType
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.v5.et.create
import dji.v5.et.createCamera
import dji.v5.ux.core.base.CameraWidgetModel
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.Completable

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/12/1
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class NDVIStreamSelectionPanelWidgetModel constructor(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore,
) : CameraWidgetModel(djiSdkModel, keyedStore) {

    //窄带
    val narrowBandModelList = StreamPanelUtil.NARROW_BAND_MODEL_LIST
    val cameraVideoStreamSourceProcessor: DataProcessor<CameraVideoStreamSourceType> = DataProcessor.create(
        CameraVideoStreamSourceType.UNKNOWN)
    val currentNarrowBandModelProcessor: DataProcessor<StreamPanelUtil.NarrowBandModel> = DataProcessor.create(StreamPanelUtil.NarrowBandModel(
        CameraVideoStreamSourceType.UNKNOWN, "UNKNOWN", -1))
    val cameraModeDataProcessor: DataProcessor<CameraMode> = DataProcessor.create(
        CameraMode.UNKNOWN)
    val visibleNarrowBandModeProcessor: DataProcessor<Boolean> = DataProcessor.create(false)

    //绿植
    val vegetationModelList = StreamPanelUtil.VEGETATION_MODEL_LIST
    val cameraMultiSpectralFusionTypeProcessor: DataProcessor<MultiSpectralFusionType> = DataProcessor.create(MultiSpectralFusionType.UNKNOWN)
    val currentVegetationModelProcessor: DataProcessor<StreamPanelUtil.VegetationModel> = DataProcessor.create((StreamPanelUtil.VegetationModel(MultiSpectralFusionType.UNKNOWN, "UNKNOWN", -1)))
    val isShootingContinuousPhotosProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    val isShootingVisionPanoramaPhotoProcessor: DataProcessor<Boolean> = DataProcessor.create(false)

    val isEnableProcessor: DataProcessor<Boolean> = DataProcessor.create(false)

    override fun inSetup() {
        bindDataProcessor(CameraKey.KeyCameraVideoStreamSource.create(cameraIndex), cameraVideoStreamSourceProcessor) {
            updateCurrentNarrowBandModelPosition(it)
        }
        bindDataProcessor(CameraKey.KeyCameraMode.create(cameraIndex), cameraModeDataProcessor) {
            visibleNarrowBandModeProcessor.onNext(it == CameraMode.PHOTO_NORMAL)
        }
        bindDataProcessor(CameraKey.KeyMultiSpectralFusionType.createCamera(cameraIndex, CameraLensType.CAMERA_LENS_MS_NDVI), cameraMultiSpectralFusionTypeProcessor) {
            updateCurrentVegetationModelPosition(it)
        }
        bindDataProcessor(CameraKey.KeyCameraShootingContinuousPhotos.create(cameraIndex), isShootingContinuousPhotosProcessor){
            updateEnable()
        }
        bindDataProcessor(CameraKey.KeyIsShootingPhotoPanorama.create(cameraIndex), isShootingVisionPanoramaPhotoProcessor){
            updateEnable()
        }
    }

    private fun updateCurrentNarrowBandModelPosition(type: CameraVideoStreamSourceType) {
        val model = narrowBandModelList.find { it.sourceType == type }
        model?.let {
            currentNarrowBandModelProcessor.onNext(it)
            return
        }
        currentNarrowBandModelProcessor.onNext(StreamPanelUtil.NarrowBandModel(CameraVideoStreamSourceType.UNKNOWN, "UNKNOWN", -1))
    }

    fun setNarrowBandModel(narrowBandModel: StreamPanelUtil.NarrowBandModel): Completable {
        if (narrowBandModel.sourceType == cameraVideoStreamSourceProcessor.value) {
            return Completable.complete()
        }
        return djiSdkModel.setValue(CameraKey.KeyCameraVideoStreamSource.create(cameraIndex), narrowBandModel.sourceType)
    }

    private fun updateCurrentVegetationModelPosition(type: MultiSpectralFusionType) {
        if (type == MultiSpectralFusionType.UNKNOWN) {
            currentVegetationModelProcessor.onNext(StreamPanelUtil.VegetationModel(MultiSpectralFusionType.UNKNOWN, "UNKNOWN", -1))
            return
        }
        val model = vegetationModelList.find { it.sourceType == type }
        model?.let {
            currentVegetationModelProcessor.onNext(it)
        }
    }

    fun setVegetationModel(model: StreamPanelUtil.VegetationModel): Completable {
        if (model.sourceType == cameraMultiSpectralFusionTypeProcessor.value) {
            return Completable.complete()
        }
        return djiSdkModel.setValue(CameraKey.KeyMultiSpectralFusionType.createCamera(cameraIndex, CameraLensType.CAMERA_LENS_MS_NDVI), model.sourceType)
    }

    private fun updateEnable() {
        isEnableProcessor.onNext(!isShootingContinuousPhotosProcessor.value && !isShootingVisionPanoramaPhotoProcessor.value)
    }

    override fun inCleanup() {
        //do nothing
    }
}