package dji.v5.ux.core.base

import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/10/20
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
interface ICameraIndex {

    /**
     * Get the camera index for which the model is reacting.
     *
     * @return int representing [ComponentIndexType].
     */
    fun getCameraIndex(): ComponentIndexType

    /**
     * Get the current type of the lens the widget model is reacting to
     *
     * @return current lens type.
     */
    fun getLensType(): CameraLensType

    /**
     * Update camera/lens index to which the model should react.
     *
     * @param cameraIndex index of the camera.
     * @param lensType index of the lens.
     */
    fun updateCameraSource(cameraIndex: ComponentIndexType, lensType: CameraLensType)
}