package dji.v5.ux.cameracore.widget.cameracontrols.lenscontrol

import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.value.camera.CameraType
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.ICameraIndex
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor
import dji.v5.ux.core.util.SettingDefinitions
import io.reactivex.rxjava3.core.Completable

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/12/13
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class LensControlModel constructor(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore), ICameraIndex {

    private var cameraIndex = ComponentIndexType.LEFT_OR_MAIN

    val cameraTypeProcessor: DataProcessor<CameraType> = DataProcessor.create(CameraType.UNKNOWN)
    val cameraVideoStreamSourceProcessor: DataProcessor<CameraVideoStreamSourceType> = DataProcessor.create(CameraVideoStreamSourceType.UNKNOWN)
    val cameraVideoStreamSourceRangeProcessor: DataProcessor<Array<CameraVideoStreamSourceType>> = DataProcessor.create(arrayOf())

    override fun getCameraIndex() = cameraIndex

    override fun getLensType() = CameraLensType.UNKNOWN

    override fun updateCameraSource(cameraIndex: ComponentIndexType, lensType: CameraLensType) {
        if (this.cameraIndex == cameraIndex){
            return
        }
        this.cameraIndex = cameraIndex
        restart()
    }

    override fun inSetup() {
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyCameraType,cameraIndex),cameraTypeProcessor)
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyCameraVideoStreamSource,cameraIndex),cameraVideoStreamSourceProcessor)
        //bindDataProcessor(CameraKey.create(CameraKey.CAMERA_VIDEO_STREAM_SOURCE_RANGE, cameraIndex),cameraVideoStreamSourceRangeProcessor)
    }

    override fun inCleanup() {
        //暂未实现
    }

    fun setCameraVideoStreamSource(source: CameraVideoStreamSourceType): Completable {
        return djiSdkModel.setValue(KeyTools.createKey(CameraKey.KeyCameraVideoStreamSource,cameraIndex), source)
    }
}