package dji.v5.ux.visualcamera.ndvi

import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.value.camera.ThermalDisplayMode
import dji.sdk.keyvalue.value.camera.ThermalPIPPosition
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.v5.et.createCamera
import dji.v5.et.set
import dji.v5.utils.common.LogUtils
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
class SpectralDisplayModeWidgetModel constructor(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore,
) : CameraWidgetModel(djiSdkModel, keyedStore) {

    val isSBSOnProcessor = DataProcessor.create(false)
    private val thermalDisplayModeProcessor = DataProcessor.create(ThermalDisplayMode.UNKNOWN)

    override fun inSetup() {
        bindDataProcessor(CameraKey.KeyThermalDisplayMode.createCamera(cameraIndex, CameraLensType.CAMERA_LENS_MS_NDVI), thermalDisplayModeProcessor) {
            isSBSOnProcessor.onNext(it == ThermalDisplayMode.PIP)
        }
    }

    fun changeDisplayMode() {
        val newDisplayMode = if (thermalDisplayModeProcessor.value == ThermalDisplayMode.THERMAL_ONLY) ThermalDisplayMode.PIP else ThermalDisplayMode.THERMAL_ONLY
        LogUtils.i(tag, newDisplayMode)
        CameraKey.KeyThermalDisplayMode.createCamera(cameraIndex, CameraLensType.CAMERA_LENS_MS_NDVI).set(newDisplayMode, {
            //PIP模式下，需要进一步设置为SIDE_BY_SIDE
            if (newDisplayMode == ThermalDisplayMode.PIP) {
                CameraKey.KeyThermalPIPPosition.createCamera(cameraIndex, CameraLensType.CAMERA_LENS_MS_NDVI).set(ThermalPIPPosition.SIDE_BY_SIDE)
            }
        })
    }

    override fun inCleanup() {
        // nothing to clean
    }
}