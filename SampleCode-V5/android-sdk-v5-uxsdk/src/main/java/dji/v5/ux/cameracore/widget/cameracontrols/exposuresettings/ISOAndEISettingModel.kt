package dji.v5.ux.cameracore.widget.cameracontrols.exposuresettings

import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.value.camera.*
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.ICameraIndex
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.CameraUtil
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.Completable

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/11/2
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class ISOAndEISettingModel constructor(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore), ICameraIndex {

    private var cameraIndex = ComponentIndexType.LEFT_OR_MAIN
    private var lensType = CameraLensType.CAMERA_LENS_ZOOM

    val exposureSettingsProcessor: DataProcessor<CameraExposureSettings> = DataProcessor.create(CameraExposureSettings())
    val ISOProcessor: DataProcessor<CameraISO> = DataProcessor.create(CameraISO.UNKNOWN)
    val ISORangeProcessor: DataProcessor<List<CameraISO>> = DataProcessor.create(listOf())
    val exposureSensitivityModeProcessor: DataProcessor<ExposureSensitivityMode> = DataProcessor.create(ExposureSensitivityMode.UNKNOWN)
    val eiValueProcessor: DataProcessor<EIType> = DataProcessor.create(EIType.UNKNOWN)
    val eiRecommendedValueProcessor: DataProcessor<EIType> = DataProcessor.create(EIType.UNKNOWN)
    val eiValueRangeProcessor: DataProcessor<List<EIType>> = DataProcessor.create(listOf())
    val exposureModeProcessor: DataProcessor<CameraExposureMode> = DataProcessor.create(CameraExposureMode.UNKNOWN)
    val cameraModeProcessor: DataProcessor<CameraWorkMode> = DataProcessor.create(CameraWorkMode.UNKNOWN)
    val flatCameraModeProcessor: DataProcessor<CameraFlatMode> = DataProcessor.create(CameraFlatMode.UNKNOWN)

    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createCameraKey(
                CameraKey.KeyISO, cameraIndex, lensType), ISOProcessor)
        bindDataProcessor(
            KeyTools.createCameraKey(
                CameraKey.KeyExposureSettings, cameraIndex, lensType), exposureSettingsProcessor)
        bindDataProcessor(
            KeyTools.createCameraKey(
                CameraKey.KeyISORange, cameraIndex, lensType), ISORangeProcessor)
        bindDataProcessor(
            KeyTools.createCameraKey(
                CameraKey.KeyExposureSensitivityMode, cameraIndex, lensType), exposureSensitivityModeProcessor)
        bindDataProcessor(
            KeyTools.createCameraKey(
                CameraKey.KeyEI, cameraIndex, lensType), eiValueProcessor)
        bindDataProcessor(
            KeyTools.createCameraKey(
                CameraKey.KeyRecommendedEI, cameraIndex, lensType), eiRecommendedValueProcessor)
        bindDataProcessor(
            KeyTools.createCameraKey(
                CameraKey.KeyEIRange, cameraIndex, lensType), eiValueRangeProcessor)
        bindDataProcessor(
            KeyTools.createCameraKey(
                CameraKey.KeyCameraWorkMode, cameraIndex, lensType), cameraModeProcessor)
        bindDataProcessor(
            KeyTools.createCameraKey(
                CameraKey.KeyCameraFlatMode, cameraIndex, lensType), flatCameraModeProcessor)
        bindDataProcessor(
            KeyTools.createCameraKey(
                CameraKey.KeyExposureMode, cameraIndex, lensType), exposureModeProcessor)
    }

    override fun inCleanup() {
        //暂未实现
    }

    override fun getCameraIndex() = cameraIndex

    override fun getLensType() = lensType

    override fun updateCameraSource(cameraIndex: ComponentIndexType, lensType: CameraLensType) {
        this.cameraIndex = cameraIndex
        this.lensType = lensType
        restart()
    }

    fun setISO(iso: CameraISO): Completable {
        return djiSdkModel.setValue(
            KeyTools.createCameraKey(
                CameraKey.KeyISO, cameraIndex, lensType), iso)
    }

    fun setEI(ei: EIType): Completable {
        return djiSdkModel.setValue(
            KeyTools.createCameraKey(
                CameraKey.KeyEI, cameraIndex, lensType), ei)
    }

    fun isEIEnable(): Boolean {
        return exposureSensitivityModeProcessor.value == ExposureSensitivityMode.EI
    }

    fun isRecordVideoEIMode(): Boolean {
        return ((!CameraUtil.isPictureMode(flatCameraModeProcessor.value) || cameraModeProcessor.value == CameraWorkMode.RECORD_VIDEO)
                && isEIEnable())
    }
}