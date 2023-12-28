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

package dji.v5.ux.core.module

import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.value.camera.CameraShootPhotoMode
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.camera.CameraMode
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.ux.core.base.BaseModule
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.ICameraIndex
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.extension.isPictureMode
import dji.v5.ux.core.extension.toFlatCameraMode
import dji.v5.ux.core.extension.toShootPhotoMode
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.Completable

/**
 * Abstraction for getting and setting camera mode and photo mode.
 */
class FlatCameraModule : BaseModule(), ICameraIndex {

    //region Fields
    private val isFlatCameraModeSupportedDataProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private var cameraIndex = ComponentIndexType.LEFT_OR_MAIN
    private var lensType = CameraLensType.CAMERA_LENS_ZOOM

    /**
     * The camera mode.
     */
    val cameraModeDataProcessor: DataProcessor<CameraMode> = DataProcessor.create(
        CameraMode.UNKNOWN)

    /**
     *  The shoot photo mode.
     */
    val shootPhotoModeProcessor: DataProcessor<CameraShootPhotoMode> = DataProcessor.create(CameraShootPhotoMode.UNKNOWN)
    //endregion

    //region Lifecycle
    override fun setup(widgetModel: WidgetModel) {
        bindDataProcessor(widgetModel, KeyTools.createKey(
            CameraKey.KeyCameraMode, cameraIndex), cameraModeDataProcessor)
        bindDataProcessor(widgetModel, KeyTools.createKey(
            CameraKey.KeyShootPhotoMode, cameraIndex), shootPhotoModeProcessor)
        val isFlatCameraModeSupportedKey = KeyTools.createKey(
            CameraKey.KeyCameraFlatModeSupported, cameraIndex)
        bindDataProcessor(widgetModel, isFlatCameraModeSupportedKey, isFlatCameraModeSupportedDataProcessor)
    }

    override fun cleanup() {
        // no code
    }
    //endregion

    //region Actions
    /**
     * Set camera mode
     *
     * @return Completable
     */
    fun setCameraMode(djiSdkModel: DJISDKModel, cameraMode: CameraMode): Completable {
        return djiSdkModel.setValue(
            KeyTools.createKey(
                CameraKey.KeyCameraMode, cameraIndex), cameraMode);
    }

    /**
     * Set photo mode
     *
     * @return Completable
     */
    fun setPhotoMode(djiSdkModel: DJISDKModel, photoMode: CameraShootPhotoMode): Completable {
        return if (isFlatCameraModeSupportedDataProcessor.value) {
            djiSdkModel.setValue(
                KeyTools.createKey(
                    CameraKey.KeyCameraFlatMode, cameraIndex), photoMode.toFlatCameraMode())
        } else {
            djiSdkModel.setValue(
                KeyTools.createKey(
                    CameraKey.KeyShootPhotoMode, cameraIndex), photoMode)
        }
    }
    //endregion

    override fun getCameraIndex() = cameraIndex

    override fun getLensType() = lensType

    override fun updateCameraSource(cameraIndex: ComponentIndexType, lensType: CameraLensType) {
        this.cameraIndex = cameraIndex
        this.lensType = lensType
    }

    //region Helpers
    private fun updateModes(flatCameraMode: CameraMode) {
        cameraModeDataProcessor.onNext(
            if (flatCameraMode.isPictureMode()) {
                CameraMode.PHOTO_NORMAL
            } else {
                CameraMode.VIDEO_NORMAL
            }
        )
        shootPhotoModeProcessor.onNext(flatCameraMode.toShootPhotoMode())
    }
    //endregion
}