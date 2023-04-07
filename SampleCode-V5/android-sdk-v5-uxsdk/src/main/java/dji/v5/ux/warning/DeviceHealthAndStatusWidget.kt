package dji.v5.ux.warning

import android.content.Context
import android.graphics.Outline
import android.graphics.PostProcessor
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.TextView
import dji.v5.manager.diagnostic.WarningLevel
import dji.v5.utils.common.AndUtil
import dji.v5.utils.common.JsonUtil
import dji.v5.utils.common.LogUtils
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.WidgetSizeDescription
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.base.widget.FrameLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.popover.Popover
import dji.v5.ux.core.popover.PopoverHelper
import dji.v5.ux.core.widget.gpssignal.GpsSignalPopoverView
import dji.v5.ux.visualcamera.ndvi.NDVIStreamPopoverViewWidget
import kotlinx.android.synthetic.main.uxsdk_camera_status_action_item_content.view.*
import kotlinx.android.synthetic.main.uxsdk_fpv_top_bar_widget_warning_message.view.*
import kotlin.math.roundToInt

/**
 * Topbar上的告警提示
 */
open class DeviceHealthAndStatusWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayoutWidget<Any>(context, attrs, defStyleAttr) {

    lateinit var warningMessageCountWrapper: ViewGroup
    lateinit var cardViewWarningWrapper: FrameLayout
    lateinit var tvWarningMessage: TextView
    lateinit var tvLevel3Count: TextView
    lateinit var tvLevel2Count: TextView
    lateinit var tvNoMessage: TextView

    private val popupView: View by lazy {
        FpvWarningMessagePopoverView(context)
    }

    var popover: Popover? = null

    private val widgetModel by lazy {
        DeviceHealthAndStatusWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_fpv_top_bar_widget_warning_message, this)

        warningMessageCountWrapper = findViewById(R.id.warning_message_count_wrapper)
        warningMessageCountWrapper.clipToOutline = true
        warningMessageCountWrapper.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val radius = resources.getDimension(R.dimen.uxsdk_2_dp)
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }

        tvWarningMessage = findViewById(R.id.tv_warning_message)
        tvLevel3Count = findViewById(R.id.tv_level3_count)
        tvLevel2Count = findViewById(R.id.tv_level2_count)
        tvNoMessage = findViewById(R.id.tv_warning_no_message)
        cardViewWarningWrapper = findViewById(R.id.cardview_warning_message)
        cardViewWarningWrapper.clipToOutline = true
        cardViewWarningWrapper.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val radius = resources.getDimension(R.dimen.uxsdk_2_dp)
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }

        setOnClickListener {
            if (popover?.isShowing() == true) {
                return@setOnClickListener
            }
            if (popover == null) {
                val isEmpty: Boolean = widgetModel.deviceMessageProcessor.value.isEmpty()
                popover = PopoverHelper.baseBuilder(if (isEmpty) tvNoMessage else warning_message_root_view)
                    .yOffset(
                        if (isEmpty) AndUtil.getDimension(R.dimen.uxsdk_10_dp)
                            .roundToInt() else AndUtil.getDimension(R.dimen.uxsdk_2_dp)
                            .roundToInt()
                    )
                    .customView(popupView)
                    .bottomScreenMargin(AndUtil.getDimension(R.dimen.uxsdk_96_dp).toInt())
                    .leftScreenMargin(AndUtil.getDimension(R.dimen.uxsdk_40_dp).roundToInt())
                    .align(Popover.Align.CENTER)
                    .arrowColor(AndUtil.getResColor(R.color.uxsdk_fpv_popover_content_background_color))
                    .onDismiss {}
                    .build()
            }
            popover?.show()
        }
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.deviceMessageProcessor.toFlowable()
            .observeOn(SchedulerProvider.ui())
            .subscribe {
                LogUtils.i(logTag, JsonUtil.toJson(it))
                updateDisplayMessage()
                updateLevelCount()
            }
        )
    }

    override val widgetSizeDescription: WidgetSizeDescription =
        WidgetSizeDescription(
            WidgetSizeDescription.SizeType.OTHER,
            widthDimension = WidgetSizeDescription.Dimension.WRAP,
            heightDimension = WidgetSizeDescription.Dimension.EXPAND
        )

    private fun updateDisplayMessage() {
        widgetModel.deviceMessageProcessor.value.let {
            if (it.isNotEmpty()) {
                tvWarningMessage.text = it[0].validDescription()
                cardViewWarningWrapper.setBackgroundColor(AndUtil.getResColor(colorResId(it[0].warningLevel)))
                tvNoMessage.visibility = GONE
                tvWarningMessage.visibility = VISIBLE
                popover?.builder?.anchor = this
                popover?.let { p ->
                    p.builder.anchor = this
                    p.builder.yOffset = AndUtil.getDimension(R.dimen.uxsdk_2_dp).toInt()
                    p.requestLayout()
                }
            } else {
                cardViewWarningWrapper.setBackgroundColor(AndUtil.getResColor(R.color.uxsdk_fpv_popover_content_background_color))
                tvNoMessage.visibility = VISIBLE
                tvWarningMessage.visibility = GONE
                if (!widgetModel.isConnectedProcessor.value) {
                    tvNoMessage.text = "N/A"
                } else {
                    tvNoMessage.text = AndUtil.getResString(R.string.uxsdk_fpv_message_box_empty_content_v2)
                }
                popover?.let { p ->
                    p.builder.anchor = tvNoMessage
                    p.builder.yOffset = AndUtil.getDimension(R.dimen.uxsdk_10_dp).toInt()
                    p.requestLayout()
                }
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

    /**
     * 更新告警信息的数字
     */
    private fun updateLevelCount() {

        val level3Count = widgetModel.level3Count()
        val level2Count = widgetModel.level2Count()

        if (level2Count == 0) {
            tvLevel2Count.visibility = GONE
        } else {
            tvLevel2Count.visibility = VISIBLE
            tvLevel2Count.text = level2Count.toString()
        }

        if (level3Count == 0) {
            tvLevel3Count.visibility = GONE
        } else {
            tvLevel3Count.visibility = VISIBLE
            tvLevel3Count.text = level3Count.toString()
        }

        warningMessageCountWrapper.visibility = if (level2Count == 0 && level3Count == 0) INVISIBLE else VISIBLE

        // 主动请求一次popover重新layout，条目变化高度可能需要更新
        popover?.requestLayout()
    }

    companion object {
        fun colorResId(warningLevel: WarningLevel): Int {
            return when (warningLevel) {
                WarningLevel.NORMAL -> R.color.uxsdk_green_material_400
                WarningLevel.NOTICE -> R.color.uxsdk_orange_in_dark
                WarningLevel.CAUTION -> R.color.uxsdk_orange_in_dark
                WarningLevel.WARNING -> R.color.uxsdk_red_in_dark
                WarningLevel.SERIOUS_WARNING -> R.color.uxsdk_red_in_dark
                else -> {
                    R.color.uxsdk_orange_in_dark
                }
            }
        }

        fun popColorResId(warningLevel: WarningLevel): Int {
            return when (warningLevel) {
                WarningLevel.NORMAL -> R.color.uxsdk_green_material_800_67_percent
                WarningLevel.NOTICE -> R.color.uxsdk_orange_in_dark_050
                WarningLevel.CAUTION -> R.color.uxsdk_orange_in_dark_050
                WarningLevel.WARNING -> R.color.uxsdk_red_in_dark_050
                WarningLevel.SERIOUS_WARNING -> R.color.uxsdk_red_in_dark_050
                else -> {
                    R.color.uxsdk_orange_in_dark
                }
            }
        }
    }
}