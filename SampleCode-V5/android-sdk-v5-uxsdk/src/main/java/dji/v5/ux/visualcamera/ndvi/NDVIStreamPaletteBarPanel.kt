package dji.v5.ux.visualcamera.ndvi

import android.content.Context
import android.util.AttributeSet
import dji.sdk.keyvalue.value.camera.MultiSpectralFusionDisplayRangeType
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.ICameraIndex
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.widget.FrameLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.ui.RangeSeekBar
import dji.v5.ux.core.ui.component.StreamPaletteRangeSeekbar
import kotlinx.android.synthetic.main.uxsdk_camera_status_action_item_content.view.*

/**
= * M3M 码流调色条
 */
open class NDVIStreamPaletteBarPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayoutWidget<Any>(context, attrs), ICameraIndex {

    private lateinit var rangeSeekbar: StreamPaletteRangeSeekbar

    private val widgetModel by lazy {
        NDVIStreamPaletteBarPanelModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }

    override fun getCameraIndex(): ComponentIndexType = widgetModel.getCameraIndex()

    override fun getLensType(): CameraLensType = widgetModel.getLensType()

    override fun updateCameraSource(cameraIndex: ComponentIndexType, lensType: CameraLensType) {
        widgetModel.updateCameraSource(cameraIndex, lensType)
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_palette_bar_panel, this)

        rangeSeekbar = findViewById(R.id.seekbar_isotherm)
        rangeSeekbar.min = -10
        rangeSeekbar.max = 10
        rangeSeekbar.leftValue = -8
        rangeSeekbar.rightValue = 10
        rangeSeekbar.zoomMultiple = 0.1

        initListener()
    }

    override fun reactToModelChanges() {
        addReaction(
            widgetModel.multiSpectralFusionDisplayRangeProcessor.toFlowable()
                .observeOn(SchedulerProvider.ui())
                .subscribe {
                    rangeSeekbar.leftValue = it.displayMin
                    rangeSeekbar.rightValue = it.displayMax
                }
        )
    }

    private fun initListener() {
        rangeSeekbar.onChangedListener = object : RangeSeekBar.OnChangedListener {
            override fun onSeekStart(seekBar: RangeSeekBar?) {
                // do nothing
            }

            override fun onValueChanged(
                seekBar: RangeSeekBar?,
                leftValue: Int,
                rightValue: Int,
                fromUser: Boolean
            ) {
                // do nothing
            }

            override fun onSeekEnd(seekBar: RangeSeekBar?) {
                val range = widgetModel.multiSpectralFusionDisplayRangeProcessor.value
                if ((seekBar != null) && (range.displayRangeType != MultiSpectralFusionDisplayRangeType.UNKNOWN) && (range.displayMin == seekBar.leftValue) && (range.displayMax == seekBar.rightValue)) {
                    return
                }
                seekBar?.let { widgetModel.setFusionRange(seekBar.leftValue, it.rightValue) }
            }
        }
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
}