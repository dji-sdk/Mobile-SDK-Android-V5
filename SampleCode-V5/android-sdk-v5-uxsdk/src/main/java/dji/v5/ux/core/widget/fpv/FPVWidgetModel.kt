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
package dji.v5.ux.core.widget.fpv

import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.camera.CameraOrientation
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType
import dji.sdk.keyvalue.value.camera.VideoResolutionFrameRate
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.video.channel.VideoChannelType
import dji.v5.common.video.interfaces.IVideoChannel
import dji.v5.common.video.stream.StreamSource
import dji.v5.et.create
import dji.v5.et.createCamera
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.utils.common.JsonUtil
import dji.v5.utils.common.LogUtils
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.ICameraIndex
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.module.FlatCameraModule
import dji.v5.ux.core.util.CameraUtil
import dji.v5.ux.core.util.DataProcessor
import dji.v5.ux.core.util.RxUtil
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable

/**
 * Widget Model for the [FPVWidget] used to define
 * the underlying logic and communication
 */
class FPVWidgetModel(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore,
    private val flatCameraModule: FlatCameraModule,
) : WidgetModel(djiSdkModel, keyedStore), ICameraIndex {

    private var currentLensType = CameraLensType.CAMERA_LENS_DEFAULT
    val streamSourceCameraTypeProcessor = DataProcessor.create(CameraVideoStreamSourceType.UNKNOWN)
    val orientationProcessor: DataProcessor<CameraOrientation> = DataProcessor.create(CameraOrientation.UNKNOWN)
    val resolutionAndFrameRateProcessor: DataProcessor<VideoResolutionFrameRate> = DataProcessor.create(VideoResolutionFrameRate())
    val cameraNameProcessor: DataProcessor<String> = DataProcessor.create("")
    val cameraSideProcessor: DataProcessor<String> = DataProcessor.create("")
    val videoViewChangedProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    var streamSourceListener: FPVStreamSourceListener? = null
    var videoChannelType: VideoChannelType = VideoChannelType.PRIMARY_STREAM_CHANNEL

    /**
     * The current camera index. This value should only be used for video size calculation.
     * To get the camera side, use [FPVWidgetModel.cameraSide] instead.
     */
    private var currentCameraIndex: ComponentIndexType = ComponentIndexType.LEFT_OR_MAIN

    var streamSource: StreamSource? = null
        set(value) {
            field = value
            restart()
        }

    /**
     * Get whether the video view has changed
     */
    @get:JvmName("hasVideoViewChanged")
    val hasVideoViewChanged: Flowable<Boolean>
        get() = videoViewChangedProcessor.toFlowable()

    init {
        addModule(flatCameraModule)
    }

    override fun getCameraIndex(): ComponentIndexType {
        return currentCameraIndex
    }

    override fun getLensType(): CameraLensType {
        return currentLensType
    }

    override fun updateCameraSource(cameraIndex: ComponentIndexType, lensType: CameraLensType) {
        // 无需实现
    }

    fun initStreamSource(){
        streamSource = getVideoChannel()?.streamSource
    }

    //region Lifecycle
    override fun inSetup() {
        streamSource?.let { source ->
            currentCameraIndex = CameraUtil.getCameraIndex(source.physicalDevicePosition)
            val videoViewChangedConsumer = { _: Any -> videoViewChangedProcessor.onNext(true) }
            bindDataProcessor(CameraKey.KeyCameraOrientation.create(currentCameraIndex), orientationProcessor, videoViewChangedConsumer)
            bindDataProcessor(CameraKey.KeyCameraVideoStreamSource.create(currentCameraIndex), streamSourceCameraTypeProcessor) {
                currentLensType = when (it) {
                    CameraVideoStreamSourceType.WIDE_CAMERA -> CameraLensType.CAMERA_LENS_WIDE
                    CameraVideoStreamSourceType.ZOOM_CAMERA -> CameraLensType.CAMERA_LENS_ZOOM
                    CameraVideoStreamSourceType.INFRARED_CAMERA -> CameraLensType.CAMERA_LENS_THERMAL
                    CameraVideoStreamSourceType.NDVI_CAMERA -> CameraLensType.CAMERA_LENS_MS_NDVI
                    CameraVideoStreamSourceType.MS_G_CAMERA -> CameraLensType.CAMERA_LENS_MS_G
                    CameraVideoStreamSourceType.MS_R_CAMERA -> CameraLensType.CAMERA_LENS_MS_R
                    CameraVideoStreamSourceType.MS_RE_CAMERA -> CameraLensType.CAMERA_LENS_MS_RE
                    CameraVideoStreamSourceType.MS_NIR_CAMERA -> CameraLensType.CAMERA_LENS_MS_NIR
                    CameraVideoStreamSourceType.RGB_CAMERA -> CameraLensType.CAMERA_LENS_RGB
                    else -> CameraLensType.CAMERA_LENS_DEFAULT
                }
                sourceUpdate()
            }
            bindDataProcessor(CameraKey.KeyVideoResolutionFrameRate.createCamera(currentCameraIndex, currentLensType), resolutionAndFrameRateProcessor)
            addDisposable(
                flatCameraModule.cameraModeDataProcessor.toFlowable()
                    .doOnNext(videoViewChangedConsumer)
                    .subscribe({ }, RxUtil.logErrorConsumer(tag, "camera mode: "))
            )
            sourceUpdate()
        }
        LogUtils.i(tag, "inSetup,streamSource:", JsonUtil.toJson(streamSource), currentCameraIndex)
    }

    override fun inCleanup() {
        currentLensType = CameraLensType.CAMERA_LENS_DEFAULT
    }

    private fun sourceUpdate() {
        updateCameraDisplay()
        onStreamSourceUpdated()
    }


    public override fun updateStates() {
        //无需实现
    }

    private fun updateCameraDisplay() {
        streamSource?.let {
            var cameraName = it.physicalDeviceCategory.name + "_" + it.physicalDeviceType.deviceType
            if (currentLensType != CameraLensType.CAMERA_LENS_DEFAULT) {
                cameraName = cameraName + "_" + currentLensType.name
            }
            cameraNameProcessor.onNext(cameraName)
            cameraSideProcessor.onNext(it.physicalDevicePosition.name)
        }
    }

    private fun onStreamSourceUpdated() {
        streamSource?.let {
            streamSourceListener?.onStreamSourceUpdated(it.physicalDevicePosition, currentLensType)
        }
    }

    private fun getVideoChannel(): IVideoChannel? = MediaDataCenter.getInstance().videoStreamManager.getAvailableVideoChannel(videoChannelType)
}