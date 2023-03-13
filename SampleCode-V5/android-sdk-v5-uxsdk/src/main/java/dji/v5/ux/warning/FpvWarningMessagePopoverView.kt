package dji.v5.ux.warning

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dji.v5.manager.diagnostic.WarningLevel
import dji.v5.utils.common.AndUtil
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.widget.FrameLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.ui.hsi.FlashTimer
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import kotlin.math.roundToInt

class FpvWarningMessagePopoverView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayoutWidget<Any>(context, attrs, defStyleAttr) {

    companion object {
        const val ALPHA_SHOW = 1f
        const val ALPHA_HIDE = 0.3f
    }

    private lateinit var recycleView: RecyclerView

    private val widgetModel by lazy {
        DeviceHealthAndStatusWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_fpv_top_bar_widget_warning_message_popover, this)
        recycleView = findViewById(R.id.rv_warning_message)
        recycleView.layoutManager = LinearLayoutManager(context)
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.deviceMessageProcessor.toFlowable()
            .observeOn(SchedulerProvider.ui())
            .subscribe {
                updateData()
            }
        )
        addReaction(widgetModel.isConnectedProcessor.toFlowable()
            .observeOn(SchedulerProvider.ui())
            .subscribe {
                updateData()
            }
        )
    }

    private fun updateData() {
        val newList = mutableListOf<Any>()
        widgetModel.deviceMessageProcessor.value.run {
            newList.addAll(this)
        }

        if (widgetModel.deviceMessageProcessor.value.size == 0) {
            widgetModel.isConnectedProcessor.value.let {
                val textResId = if (it) {
                    R.string.uxsdk_fpv_message_box_empty_content_v2
                } else {
                    R.string.uxsdk_fpv_tip_remote_disconnect
                }
                newList.add(0, NoWarning(textResId))
            }
        }

        (recycleView.adapter as Adapter).setNewData(newList)

    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()

            //初始化adapter
            val adapter = Adapter(widgetModel)
            recycleView.adapter = adapter
            recycleView.itemAnimator = null
            recycleView.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    itemPosition: Int,
                    parent: RecyclerView
                ) {
                    val isLast = (parent.adapter?.itemCount ?: 0) == itemPosition + 1
                    val bottom = if (isLast) {
                        AndUtil.getDimension(R.dimen.uxsdk_4_dp).roundToInt()
                    } else {
                        0
                    }
                    outRect.set(0, 0, 0, bottom)
                }
            })
            if (recycleView.adapter is Adapter) {
                (recycleView.adapter as Adapter).startFlash()
            }
            recycleView.scrollToPosition(0)
            // post一下，界面的高度才会计算正确，否则会是上次的高度
            AndroidSchedulers.mainThread().scheduleDirect { }
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        if (recycleView.adapter is Adapter) {
            (recycleView.adapter as Adapter).stopFlash()
        }
        super.onDetachedFromWindow()
    }

    class WarningMessageViewHolder(itemView: View, val vm: DeviceHealthAndStatusWidgetModel) :
        RecyclerView.ViewHolder(itemView) {

        private val tvMessage: TextView
        private val tvMessageTime: TextView
        private val wrapper: View
        private val levelView: View
        private val ivArrow: ImageView

        init {
            tvMessage = itemView.findViewById(R.id.tv_warning_message)
            tvMessageTime = itemView.findViewById(R.id.tv_warning_message_time)
            wrapper = itemView.findViewById(R.id.wrapper)
            levelView = itemView.findViewById(R.id.level_color_view)
            ivArrow = itemView.findViewById(R.id.iv_arrow)

            itemView.isClickable = true
        }

        fun bind(item: DeviceHealthAndStatusWidgetModel.DeviceMessage, alphaValue: Float) {
            levelView.setBackgroundResource(DeviceHealthAndStatusWidget.popColorResId(item.warningLevel))

            tvMessageTime.visibility = GONE
            wrapper.setBackgroundResource(DeviceHealthAndStatusWidget.popColorResId(item.warningLevel))

            if (item.warningLevel == WarningLevel.WARNING) {
                levelView.alpha = alphaValue
            } else {
                levelView.alpha = 1F
            }

            tvMessage.text = item.validDescription()

            setClickListener()
        }

        private fun setClickListener() {
            ivArrow.visibility = GONE
            itemView.setOnClickListener(null)
        }
    }

    class NoWarningItemViewHolder(itemView: View, val vm: DeviceHealthAndStatusWidgetModel) :
        RecyclerView.ViewHolder(itemView) {
        var splitLine = itemView.findViewById<View>(R.id.split_line)
        fun bind(item: NoWarning) {
            itemView.findViewById<TextView>(R.id.tv_no_message).setText(item.contentRes)
            splitLine.visibility = View.GONE
        }
    }

    class UnknownViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class Adapter(var vm: DeviceHealthAndStatusWidgetModel) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            const val VIEW_TYPE_UNKNOWN = -1
            const val VIEW_TYPE_WARNING_MESSAGE = 1
            const val VIEW_TYPE_NO_WARNING = 3
        }

        private var items: MutableList<Any> = mutableListOf()
        private var alphaValue = 1F

        fun setNewData(newItems: List<Any>) {
            this.items.clear()
            this.items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is DeviceHealthAndStatusWidgetModel.DeviceMessage -> {
                    VIEW_TYPE_WARNING_MESSAGE
                }
                is NoWarning -> {
                    VIEW_TYPE_NO_WARNING
                }
                else -> {
                    VIEW_TYPE_UNKNOWN
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == VIEW_TYPE_WARNING_MESSAGE) {
                WarningMessageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.uxsdk_fpv_top_bar_widget_warning_message_list_item, parent, false), vm)
            } else if (viewType == VIEW_TYPE_NO_WARNING) {
                NoWarningItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.uxsdk_fpv_top_bar_widget_warning_message_list_item_no_warning, parent, false), vm)
            } else {
                UnknownViewHolder(View(parent.context))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is WarningMessageViewHolder) {
                holder.bind(items[position] as DeviceHealthAndStatusWidgetModel.DeviceMessage, alphaValue)
            } else if (holder is NoWarningItemViewHolder) {
                holder.bind(items[position] as NoWarning)
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }

        private val flashListener = FlashTimer.Listener {
            alphaValue = if (it) ALPHA_SHOW else ALPHA_HIDE

            items.forEachIndexed { index, any ->
                if (any is DeviceHealthAndStatusWidgetModel.DeviceMessage && any.warningLevel == WarningLevel.WARNING) {
                    notifyItemChanged(index)
                }
            }
        }

        fun startFlash() {
            FlashTimer.addListener(flashListener)
        }

        fun stopFlash() {
            FlashTimer.removeListener(flashListener)
        }
    }

    /**
     * 没有告警时的对象
     */
    data class NoWarning(val contentRes: Int)
}