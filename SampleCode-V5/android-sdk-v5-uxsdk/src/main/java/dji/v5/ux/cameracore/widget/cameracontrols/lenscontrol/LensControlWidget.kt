package dji.v5.ux.cameracore.widget.cameracontrols.lenscontrol

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import dji.sdk.keyvalue.value.camera.CameraType
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.ICameraIndex
import dji.v5.ux.core.base.SchedulerProvider.ui
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.SettingDefinitions
import kotlinx.android.synthetic.main.uxsdk_camera_lens_control_widget.view.*
import java.util.concurrent.atomic.AtomicInteger

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
    private val currentLensArrayIndex = AtomicInteger(-1)

    private val widgetModel by lazy {
        LensControlModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_camera_lens_control_widget, this)
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.cameraTypeProcessor.toFlowable().observeOn(ui()).subscribe {
            showView(it)
        })
        addReaction(widgetModel.cameraVideoStreamSourceRangeProcessor.toFlowable().observeOn(ui()).subscribe {
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
        if (widgetModel.getCameraIndex() == cameraIndex){
            return
        }
        currentLensArrayIndex.set(-1)
        widgetModel.updateCameraSource(cameraIndex, lensType)
    }

    private fun showView(type: CameraType) {
        if (type == CameraType.H20 || type == CameraType.H20T) {
            this.visibility = VISIBLE
        } else {
            this.visibility = GONE
        }
    }

    private fun dealLensBtnClicked(source: CameraVideoStreamSourceType) {
        if (source == widgetModel.cameraVideoStreamSourceProcessor.value) {
            return
        }
        addDisposable(widgetModel.setCameraVideoStreamSource(source).observeOn(ui()).subscribe {
            updateBtnView()
        })
    }

    private fun updateBtnView() {
        val cameraVideoStreamSourceRange = widgetModel.cameraVideoStreamSourceRangeProcessor.value
        if (cameraVideoStreamSourceRange.isEmpty()) {
            this.visibility = GONE
            return
        }
        this.visibility = VISIBLE
        if (cameraVideoStreamSourceRange.size <= 1) {
            updateBtnText(first_len_btn, cameraVideoStreamSourceRange[getCurrentLensArrayIndexAndIncrease(cameraVideoStreamSourceRange.size)].also {
                firstBtnSource = it
            })
            second_len_btn.visibility = GONE
            return
        }
        second_len_btn.visibility = VISIBLE
        updateBtnText(first_len_btn, cameraVideoStreamSourceRange[getCurrentLensArrayIndexAndIncrease(cameraVideoStreamSourceRange.size)].also {
            firstBtnSource = it
        })
        updateBtnText(second_len_btn, cameraVideoStreamSourceRange[getCurrentLensArrayIndexAndIncrease(cameraVideoStreamSourceRange.size)].also {
            secondBtnSource = it
        })
    }

    private fun updateBtnText(button: Button, source: CameraVideoStreamSourceType) {
        button.text = when (source) {
            CameraVideoStreamSourceType.WIDE_CAMERA -> "WIDE"
            CameraVideoStreamSourceType.ZOOM_CAMERA -> "ZOOM"
            CameraVideoStreamSourceType.INFRARED_CAMERA -> "IR"
            else -> "UNKNOWN"
        }
    }

    private fun getCurrentLensArrayIndexAndIncrease(range: Int): Int {
        currentLensArrayIndex.set(currentLensArrayIndex.incrementAndGet() % range)
        return currentLensArrayIndex.get()
    }

    sealed class ModelState
}