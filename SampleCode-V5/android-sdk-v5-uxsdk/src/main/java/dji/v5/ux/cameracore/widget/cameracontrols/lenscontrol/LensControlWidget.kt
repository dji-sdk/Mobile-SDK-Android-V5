package dji.v5.ux.cameracore.widget.cameracontrols.lenscontrol

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.utils.common.StringUtils
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.ICameraIndex
import dji.v5.ux.core.base.SchedulerProvider.ui
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import kotlinx.android.synthetic.main.uxsdk_camera_lens_control_widget.view.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/12/13
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class LensControlWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<LensControlWidget.ModelState>(context, attrs, defStyleAttr),
    View.OnClickListener, ICameraIndex {

    private var firstBtnSource = CameraVideoStreamSourceType.ZOOM_CAMERA
    private var secondBtnSource = CameraVideoStreamSourceType.WIDE_CAMERA

    private val widgetModel by lazy {
        LensControlModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_camera_lens_control_widget, this)
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.properCameraVideoStreamSourceRangeProcessor.toFlowable().observeOn(ui()).subscribe {
            updateBtnView()
        })
        addReaction(widgetModel.cameraVideoStreamSourceProcessor.toFlowable().observeOn(ui()).subscribe {
            updateBtnView()
        })
        first_len_btn.setOnClickListener(this)
        second_len_btn.setOnClickListener(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    override fun onClick(v: View?) {
        if (v == first_len_btn) {
            dealLensBtnClicked(firstBtnSource)
        } else if (v == second_len_btn) {
            dealLensBtnClicked(secondBtnSource)
        }
    }

    override fun getCameraIndex() = widgetModel.getCameraIndex()

    override fun getLensType() = widgetModel.getLensType()

    override fun updateCameraSource(cameraIndex: ComponentIndexType, lensType: CameraLensType) {
        if (widgetModel.getCameraIndex() == cameraIndex) {
            return
        }
        widgetModel.updateCameraSource(cameraIndex, lensType)
    }

    private fun dealLensBtnClicked(source: CameraVideoStreamSourceType) {
        if (source == widgetModel.cameraVideoStreamSourceProcessor.value) {
            return
        }
        addDisposable(widgetModel.setCameraVideoStreamSource(source).observeOn(ui()).subscribe())
    }

    private fun updateBtnView() {
        val videoSourceRange = widgetModel.properCameraVideoStreamSourceRangeProcessor.value
        //单源
        if (videoSourceRange.size <= 1) {
            first_len_btn.visibility = INVISIBLE
            second_len_btn.visibility = INVISIBLE
            return
        }
        first_len_btn.visibility = VISIBLE
        //双源
        if (videoSourceRange.size == 2) {
            updateBtnText(first_len_btn, getProperVideoSource(videoSourceRange,widgetModel.cameraVideoStreamSourceProcessor.value).also {
                firstBtnSource = it
            })
            second_len_btn.visibility = INVISIBLE
            return
        }
        //超过2个源
        second_len_btn.visibility = VISIBLE
        updateBtnText(first_len_btn, getProperVideoSource(videoSourceRange, secondBtnSource).also {
            firstBtnSource = it
        })
        updateBtnText(second_len_btn, getProperVideoSource(videoSourceRange, firstBtnSource).also {
            secondBtnSource = it
        })
    }

    private fun updateBtnText(button: Button, source: CameraVideoStreamSourceType) {
        button.text = when (source) {
            CameraVideoStreamSourceType.WIDE_CAMERA -> StringUtils.getResStr(R.string.uxsdk_lens_type_wide)
            CameraVideoStreamSourceType.ZOOM_CAMERA -> StringUtils.getResStr(R.string.uxsdk_lens_type_zoom)
            CameraVideoStreamSourceType.INFRARED_CAMERA -> StringUtils.getResStr(R.string.uxsdk_lens_type_ir)
            CameraVideoStreamSourceType.NDVI_CAMERA -> StringUtils.getResStr(R.string.uxsdk_lens_type_ndvi)
            CameraVideoStreamSourceType.RGB_CAMERA -> StringUtils.getResStr(R.string.uxsdk_lens_type_rgb)
            CameraVideoStreamSourceType.POINT_CLOUD_CAMERA -> StringUtils.getResStr(R.string.uxsdk_lens_type_point_cloud)
            else -> ""
        }
    }

    private fun getProperVideoSource(range: List<CameraVideoStreamSourceType>, exceptSource: CameraVideoStreamSourceType): CameraVideoStreamSourceType {
        for (source in range) {
            if (source != widgetModel.cameraVideoStreamSourceProcessor.value && source != exceptSource) {
                return source
            }
        }
        return exceptSource;
    }

    sealed class ModelState
}