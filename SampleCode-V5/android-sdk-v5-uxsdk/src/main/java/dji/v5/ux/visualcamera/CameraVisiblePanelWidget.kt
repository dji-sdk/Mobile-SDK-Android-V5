package dji.v5.ux.visualcamera

import android.content.Context
import android.util.AttributeSet
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.ux.R
import dji.v5.ux.core.base.ICameraIndex
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import kotlinx.android.synthetic.main.uxsdk_panel_common_camera.view.*

open class CameraVisiblePanelWidget @JvmOverloads constructor(
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
        widget_camera_config_iso_and_ei.updateCameraSource(cameraIndex,lensType)
        widget_camera_config_shutter.updateCameraSource(cameraIndex,lensType)
        widget_camera_config_aperture.updateCameraSource(cameraIndex,lensType)
        widget_camera_config_ev.updateCameraSource(cameraIndex,lensType)
        widget_camera_config_wb.updateCameraSource(cameraIndex,lensType)
        widget_camera_config_storage.updateCameraSource(cameraIndex,lensType)

        //NDVI镜头下不支持这类操作
        widget_camera_config_iso_and_ei.visibility = if (lensType == CameraLensType.CAMERA_LENS_MS_NDVI) INVISIBLE else VISIBLE
        widget_camera_config_shutter.visibility = if (lensType == CameraLensType.CAMERA_LENS_MS_NDVI) INVISIBLE else VISIBLE
        widget_camera_config_aperture.visibility = if (lensType == CameraLensType.CAMERA_LENS_MS_NDVI) INVISIBLE else VISIBLE
        widget_camera_config_ev.visibility = if (lensType == CameraLensType.CAMERA_LENS_MS_NDVI) INVISIBLE else VISIBLE
        widget_camera_config_wb.visibility = if (lensType == CameraLensType.CAMERA_LENS_MS_NDVI) INVISIBLE else VISIBLE
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_panel_common_camera, this)
        if (background == null) {
            setBackgroundResource(R.drawable.uxsdk_background_black_rectangle)
        }
    }

    override fun reactToModelChanges() {
        //do nothing
    }
}