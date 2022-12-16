package dji.v5.ux.cameracore.widget.cameracontrols.exposuresettings

import android.content.Context
import android.util.AttributeSet
import android.view.View
import dji.sdk.keyvalue.value.camera.CameraExposureMode
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.utils.common.LogUtils
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.ICameraIndex
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.RxUtil
import dji.v5.ux.core.util.SettingDefinitions
import io.reactivex.rxjava3.functions.Action
import kotlinx.android.synthetic.main.uxsdk_widget_exposure_mode_setting.view.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/10/19
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class ExposureModeSettingWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayoutWidget<ExposureModeSettingWidget.ModelState>(context, attrs, defStyleAttr),
    View.OnClickListener, ICameraIndex {

    private val widgetModel by lazy {
        ExposureModeSettingModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_widget_exposure_mode_setting, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
        layout_camera_mode_a.setOnClickListener(this)
        layout_camera_mode_s.setOnClickListener(this)
        layout_camera_mode_m.setOnClickListener(this)
        layout_camera_mode_p.setOnClickListener(this)
        layout_camera_mode_p.isSelected = true
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    override fun getCameraIndex() = widgetModel.getCameraIndex()

    override fun getLensType() = widgetModel.getLensType()

    override fun updateCameraSource(cameraIndex: ComponentIndexType, lensType: CameraLensType) = widgetModel.updateCameraSource(cameraIndex, lensType)

    override fun reactToModelChanges() {
        addReaction(widgetModel.exposureModeProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            updateExposureMode(it)
        })
        addReaction(widgetModel.exposureModeRangeProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            updateExposureModeRange(it)
        })
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    override fun onClick(v: View?) {

        val previousExposureMode: CameraExposureMode = widgetModel.exposureModeProcessor.value
        var exposureMode: CameraExposureMode = CameraExposureMode.UNKNOWN

        when (v?.id) {
            R.id.layout_camera_mode_p -> exposureMode = CameraExposureMode.PROGRAM
            R.id.layout_camera_mode_a -> exposureMode = CameraExposureMode.APERTURE_PRIORITY
            R.id.layout_camera_mode_s -> exposureMode = CameraExposureMode.SHUTTER_PRIORITY
            R.id.layout_camera_mode_m -> exposureMode = CameraExposureMode.MANUAL
        }

        if (exposureMode == previousExposureMode) {
            return
        }

        updateExposureMode(exposureMode)

        addDisposable(
            widgetModel.setExposureMode(exposureMode)
                .observeOn(SchedulerProvider.ui())
                .subscribe(Action { }, RxUtil.errorConsumer({
                    restoreToCurrentExposureMode()
                }, this.toString(), "setExposureMode: "))
        )
    }

    private fun updateExposureModeRange(range: List<CameraExposureMode>) {
        layout_camera_mode_a.isEnabled = rangeContains(range, CameraExposureMode.APERTURE_PRIORITY)
        layout_camera_mode_s.isEnabled = rangeContains(range, CameraExposureMode.SHUTTER_PRIORITY)
        layout_camera_mode_m.isEnabled = rangeContains(range, CameraExposureMode.MANUAL)
        layout_camera_mode_p.isEnabled = rangeContains(range, CameraExposureMode.PROGRAM)
    }

    private fun updateExposureMode(mode: CameraExposureMode) {
        layout_camera_mode_a.isSelected = false
        layout_camera_mode_s.isSelected = false
        layout_camera_mode_m.isSelected = false
        layout_camera_mode_p.isSelected = false

        when (mode) {
            CameraExposureMode.PROGRAM -> layout_camera_mode_p.isSelected = true
            CameraExposureMode.SHUTTER_PRIORITY -> layout_camera_mode_s.isSelected = true
            CameraExposureMode.APERTURE_PRIORITY -> layout_camera_mode_a.isSelected = true
            CameraExposureMode.MANUAL -> layout_camera_mode_m.isSelected = true
        }
    }

    private fun restoreToCurrentExposureMode() {
        updateExposureMode(widgetModel.exposureModeProcessor.value)
    }

    private fun rangeContains(range: List<CameraExposureMode>?, value: CameraExposureMode): Boolean {
        if (range == null) {
            return false
        }
        for (item in range) {
            if (item == value) {
                return true
            }
        }
        return false
    }

    sealed class ModelState
}