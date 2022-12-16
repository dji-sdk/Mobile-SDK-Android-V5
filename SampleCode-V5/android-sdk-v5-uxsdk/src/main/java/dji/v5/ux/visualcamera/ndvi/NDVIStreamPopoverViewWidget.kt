package dji.v5.ux.visualcamera.ndvi

import android.content.Context
import android.util.AttributeSet
import androidx.lifecycle.*
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.ux.R
import dji.v5.ux.core.base.ICameraIndex
import dji.v5.ux.core.base.widget.FrameLayoutWidget
import dji.v5.ux.core.ui.component.SegmentedButtonGroup
import kotlinx.android.synthetic.main.uxsdk_m3m_stream_palette_popover_view.view.*

/**
 * 码流切换及调色弹窗
 */
class NDVIStreamPopoverViewWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayoutWidget<Any>(context, attrs, defStyleAttr), ICameraIndex {

    private var cameraIndex = ComponentIndexType.LEFT_OR_MAIN
    private var lensType = CameraLensType.CAMERA_LENS_ZOOM
    private var group: SegmentedButtonGroup? = null

    var selectIndex = 0
        set(value) {
            group?.check(if (value == 0) R.id.stream_selection else R.id.stream_palette_bar)
            field = value
        }

    override fun getCameraIndex(): ComponentIndexType = cameraIndex

    override fun getLensType(): CameraLensType = lensType

    override fun updateCameraSource(cameraIndex: ComponentIndexType, lensType: CameraLensType) {
        this.cameraIndex = cameraIndex
        this.lensType = lensType

        palette_selection_panel.updateCameraSource(cameraIndex, lensType)
        isotherm_selection_panel.updateCameraSource(cameraIndex, lensType)
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_m3m_stream_palette_popover_view, this)
        group = findViewById(R.id.segmented_button_group)
        group?.onCheckedChangedListener = SegmentedButtonGroup.OnCheckedListener {
            post {
                if (it == R.id.stream_selection) {
                    palette_selection_panel.visibility = VISIBLE
                    isotherm_selection_panel.visibility = GONE
                } else {
                    isotherm_selection_panel.visibility = VISIBLE
                    palette_selection_panel.visibility = GONE
                }
            }
        }
        selectIndex = 0
    }

    override fun reactToModelChanges() {
        //do nothing
    }
}