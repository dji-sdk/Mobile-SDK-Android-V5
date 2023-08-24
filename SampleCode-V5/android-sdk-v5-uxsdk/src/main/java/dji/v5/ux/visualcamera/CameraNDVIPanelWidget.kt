package dji.v5.ux.visualcamera

import android.content.Context
import android.util.AttributeSet
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.ux.R
import dji.v5.ux.core.base.ICameraIndex
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import kotlinx.android.synthetic.main.uxsdk_panel_ndvl.view.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/12/1
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
open class CameraNDVIPanelWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<Any>(context, attrs, defStyleAttr),
    ICameraIndex {

    var mCameraIndex = ComponentIndexType.LEFT_OR_MAIN
    var mLensType = CameraLensType.CAMERA_LENS_ZOOM

    override fun getCameraIndex(): ComponentIndexType {
        return mCameraIndex
    }

    override fun getLensType(): CameraLensType {
        return mLensType
    }

    override fun updateCameraSource(cameraIndex: ComponentIndexType, lensType: CameraLensType) {
        mCameraIndex = cameraIndex
        mLensType = lensType
        widget_ndvi_stream_selector.updateCameraSource(cameraIndex, lensType)
        widget_spectral_display_mode.updateCameraSource(cameraIndex, lensType)
        widget_ndvi_stream_palette_bar.updateCameraSource(cameraIndex, lensType)
        widget_ndvi_stream_palette_bar.visibility = if (lensType == CameraLensType.CAMERA_LENS_MS_NDVI) VISIBLE else INVISIBLE
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_panel_ndvl, this)
        if (background == null) {
            setBackgroundResource(R.drawable.uxsdk_background_black_rectangle)
        }
    }

    override fun reactToModelChanges() {
        //do nothing
    }

    override fun getIdealDimensionRatioString(): String? = null
}