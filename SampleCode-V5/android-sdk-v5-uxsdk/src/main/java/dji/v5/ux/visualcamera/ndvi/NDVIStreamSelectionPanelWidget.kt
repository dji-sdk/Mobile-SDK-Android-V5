package dji.v5.ux.visualcamera.ndvi

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.utils.common.AndUtil
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.ICameraIndex
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.R
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.extension.showShortToast
import dji.v5.ux.core.ui.component.PaletteItemDecoration
import kotlinx.android.synthetic.main.uxsdk_camera_status_action_item_content.view.*
import kotlin.math.roundToInt

/**
 *  M3M码流选择面板:植被指数和窄带
 **/
open class NDVIStreamSelectionPanelWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<Any>(context, attrs, defStyleAttr), ICameraIndex {

    private val widgetModel by lazy {
        NDVIStreamSelectionPanelWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }

    private lateinit var vegetationList: RecyclerView
    private var vegetationAdapter: StreamAdapter<StreamPanelUtil.VegetationModel> = StreamAdapter {
        if (widgetModel.isEnableProcessor.value) {
            setCurrentVegetationPosition(it)
            widgetModel.setVegetationModel(it).subscribe()
        } else {
            showShortToast(R.string.uxsdk_switch_stream_unsupported)
        }
    }

    private lateinit var narrowBandList: RecyclerView
    private lateinit var narrowBandTv: TextView
    private var narrowBandAdapter: StreamAdapter<StreamPanelUtil.NarrowBandModel> = StreamAdapter {
        if (widgetModel.isEnableProcessor.value) {
            setCurrentNarrowBandPosition(it)
            widgetModel.setNarrowBandModel(it).subscribe()
        } else {
            showShortToast(R.string.uxsdk_switch_stream_unsupported)
        }
    }

    override fun getCameraIndex(): ComponentIndexType = widgetModel.getCameraIndex()

    override fun getLensType(): CameraLensType = widgetModel.getLensType()

    override fun updateCameraSource(cameraIndex: ComponentIndexType, lensType: CameraLensType) {
        widgetModel.updateCameraSource(cameraIndex, lensType)
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_ux_ndvi_stream_selection_panel, this)
        val layoutManager: GridLayoutManager = object : GridLayoutManager(getContext(), 3) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }

        val layoutManager2: GridLayoutManager = object : GridLayoutManager(getContext(), 3) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }

        vegetationList = findViewById(R.id.vegetation_index_list)
        vegetationList.layoutManager = layoutManager
        vegetationList.itemAnimator = null

        narrowBandList = findViewById(R.id.narrow_band_list)
        narrowBandTv = findViewById(R.id.narrow_band_tv)
        narrowBandList.layoutManager = layoutManager2
        narrowBandList.itemAnimator = null

        val hEdgeSpacing = AndUtil.getDimension(R.dimen.uxsdk_8_dp).roundToInt()
        val vSpacing = AndUtil.getDimension(R.dimen.uxsdk_12_dp).roundToInt()
        val hSpacing = AndUtil.getDimension(R.dimen.uxsdk_4_dp).roundToInt()
        val decoration = PaletteItemDecoration(3, hEdgeSpacing, vSpacing, 0, hSpacing, vSpacing)
        vegetationList.addItemDecoration(decoration)
        narrowBandList.addItemDecoration(decoration)
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.currentNarrowBandModelProcessor.toFlowable()
            .observeOn(SchedulerProvider.ui())
            .subscribe {
                setCurrentNarrowBandPosition(it)
            }
        )
        addReaction(widgetModel.currentVegetationModelProcessor.toFlowable()
            .observeOn(SchedulerProvider.ui())
            .subscribe {
                setCurrentVegetationPosition(it)
            }
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
        vegetationList.adapter = vegetationAdapter
        narrowBandList.adapter = narrowBandAdapter
        setVegetationData(widgetModel.vegetationModelList)
        setNarrowBandData(widgetModel.narrowBandModelList)
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    private fun setVegetationData(models: List<StreamPanelUtil.VegetationModel>) {
        vegetationAdapter.models.clear()
        vegetationAdapter.models.addAll(models)
        vegetationAdapter.notifyDataSetChanged()
    }

    private fun setNarrowBandData(models: List<StreamPanelUtil.NarrowBandModel>) {
        narrowBandAdapter.models.clear()
        narrowBandAdapter.models.addAll(models)
        narrowBandAdapter.notifyDataSetChanged()
    }

    private fun setCurrentVegetationPosition(position: StreamPanelUtil.VegetationModel) {
        vegetationAdapter.currentPosition = position
        vegetationAdapter.notifyDataSetChanged()
    }

    private fun setCurrentNarrowBandPosition(position: StreamPanelUtil.NarrowBandModel) {
        narrowBandAdapter.currentPosition = position
        narrowBandAdapter.notifyDataSetChanged()
    }

}