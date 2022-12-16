package dji.v5.ux.core.widget.hsi

import android.content.Context
import android.util.AttributeSet
import android.view.View
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.utils.common.LogUtils
import dji.v5.ux.R
import dji.v5.ux.core.base.ICameraIndex
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import kotlinx.android.synthetic.main.uxsdk_fpv_view_horizontal_situation_indicator.view.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/11/25
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class HorizontalSituationIndicatorWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayoutWidget<HorizontalSituationIndicatorWidget.ModelState>(context, attrs, defStyleAttr), ICameraIndex {

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_fpv_view_horizontal_situation_indicator, this)
    }

    override fun reactToModelChanges() {
//        do nothing
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    fun setSimpleModeEnable(isEnable: Boolean) {
        pfd_hsi_speed_display.visibility = if (isEnable) VISIBLE else GONE
        pfd_hsi_attitude_display.visibility = if (isEnable) VISIBLE else GONE
        pfd_hsi_gimbal_pitch_display.visibility = if (isEnable) VISIBLE else GONE
    }

    sealed class ModelState

    override fun getCameraIndex(): ComponentIndexType {
       return pfd_hsi_gimbal_pitch_display.getCameraIndex()
    }

    override fun getLensType(): CameraLensType {
        return pfd_hsi_gimbal_pitch_display.getLensType()

    }

    override fun updateCameraSource(cameraIndex: ComponentIndexType, lensType: CameraLensType) {
        pfd_hsi_gimbal_pitch_display.updateCameraSource(cameraIndex, lensType)
    }
}