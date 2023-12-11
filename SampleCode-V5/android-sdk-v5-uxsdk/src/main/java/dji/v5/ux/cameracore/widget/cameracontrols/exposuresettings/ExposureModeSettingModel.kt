package dji.v5.ux.cameracore.widget.cameracontrols.exposuresettings

import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.value.camera.CameraExposureMode
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.ICameraIndex
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.Completable

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/10/19
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class ExposureModeSettingModel constructor(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore), ICameraIndex {

    val exposureModeProcessor: DataProcessor<CameraExposureMode> = DataProcessor.create(CameraExposureMode.UNKNOWN)
    val exposureModeRangeProcessor: DataProcessor<List<CameraExposureMode>> = DataProcessor.create(listOf())

    private var cameraIndex = ComponentIndexType.LEFT_OR_MAIN
    private var lensType = CameraLensType.CAMERA_LENS_ZOOM

    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createCameraKey(
                CameraKey.KeyExposureMode, cameraIndex, lensType), exposureModeProcessor)
        bindDataProcessor(
            KeyTools.createCameraKey(
                CameraKey.KeyExposureModeRange, cameraIndex, lensType), exposureModeRangeProcessor)
    }

    override fun inCleanup() {
        //暂未实现
    }

    override fun updateStates() {
        //暂未实现
    }

    override fun getCameraIndex() = cameraIndex

    override fun getLensType() = lensType

    override fun updateCameraSource(cameraIndex: ComponentIndexType, lensType: CameraLensType) {
        this.cameraIndex = cameraIndex
        this.lensType = lensType
        restart()
    }

    fun setExposureMode(mode: CameraExposureMode): Completable {
        return djiSdkModel.setValue(
            KeyTools.createCameraKey(
                CameraKey.KeyExposureMode, cameraIndex, lensType), mode)
    }
}