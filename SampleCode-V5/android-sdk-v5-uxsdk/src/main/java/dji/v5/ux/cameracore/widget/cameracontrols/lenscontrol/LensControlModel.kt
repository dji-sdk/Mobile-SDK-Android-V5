package dji.v5.ux.cameracore.widget.cameracontrols.lenscontrol

import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.ux.core.base.CameraWidgetModel
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.ICameraIndex
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor
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
) : CameraWidgetModel(djiSdkModel, keyedStore), ICameraIndex {

    val cameraVideoStreamSourceProcessor: DataProcessor<CameraVideoStreamSourceType> = DataProcessor.create(
        CameraVideoStreamSourceType.UNKNOWN)
    private val cameraVideoStreamSourceRangeProcessor: DataProcessor<List<CameraVideoStreamSourceType>> = DataProcessor.create(listOf())
    val properCameraVideoStreamSourceRangeProcessor: DataProcessor<List<CameraVideoStreamSourceType>> = DataProcessor.create(listOf())

    override fun updateCameraSource(cameraIndex: ComponentIndexType, lensType: CameraLensType) {
        if (this.cameraIndex == cameraIndex){
            return
        }
        this.cameraIndex = cameraIndex
        restart()
    }

    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                CameraKey.KeyCameraVideoStreamSource,cameraIndex),cameraVideoStreamSourceProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                CameraKey.KeyCameraVideoStreamSourceRange, cameraIndex),cameraVideoStreamSourceRangeProcessor){
            //部分镜头不在该控件显示
            it.remove(CameraVideoStreamSourceType.MS_NIR_CAMERA)
            it.remove(CameraVideoStreamSourceType.MS_G_CAMERA)
            it.remove(CameraVideoStreamSourceType.MS_RE_CAMERA)
            it.remove(CameraVideoStreamSourceType.MS_R_CAMERA)
            properCameraVideoStreamSourceRangeProcessor.onNext(it)
        }
    }

    override fun inCleanup() {
        //暂未实现
    }

    fun setCameraVideoStreamSource(source: CameraVideoStreamSourceType): Completable {
        return djiSdkModel.setValue(
            KeyTools.createKey(
                CameraKey.KeyCameraVideoStreamSource,cameraIndex), source)
    }
}