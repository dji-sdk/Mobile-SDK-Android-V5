package dji.v5.ux.core.popover

import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.View.MeasureSpec
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import dji.v5.utils.common.LogUtils
import dji.v5.ux.R
import dji.v5.ux.core.extension.getLandScreenSize


class Popover(val builder: Builder) {
    private val tag = LogUtils.getTag(this)
    private var popupWindow: PopupWindow = PopupWindow()
    private lateinit var popoverView: PopoverView

    private var width = 0
    private var height = 0

    private var mDismissListener: PopupWindow.OnDismissListener? = null

    // 是否是文本内容显示
    private var isTextContent = false

    private val onPopupWindowDismiss = PopupWindow.OnDismissListener {
        popoverView.removeCallbacks(requestLayoutRunnable)
        mDismissListener?.onDismiss()
    }

    private val requestLayoutRunnable = {
        // 只有处理自定义view的时候才进行下面的重新
        if (!isTextContent) {
            val newLocation = configPopupWindow(popoverView)
            val height = if (popupWindow.contentView.rootView.height > popoverView.measuredHeight) {
                popoverView.measuredHeight
            } else {
                popupWindow.height
            }
            val width = if (popupWindow.contentView.rootView.layoutParams.width > popoverView.measuredWidth) {
                popoverView.measuredWidth
            } else {
                popupWindow.width
            }
            popupWindow.update(newLocation[0] + builder.xOffset, newLocation[1] + builder.yOffset, width, height)
        }
    }

    init {
        popupWindow.setOnDismissListener(onPopupWindowDismiss)
    }

    /**
     * 获取popupwindow显示坐标
     */
    private fun getShowLocation(
        anchorViewLocationInScreen: IntArray,
        popoverWidth: Int,
        popoverHeight: Int,
        anchorWidth: Int,
        anchorHeight: Int,
        popoverView: PopoverView,
    ): IntArray {

        val location = IntArray(2)

        when (builder.position) {
            Position.LEFT -> {
                location[0] = anchorViewLocationInScreen[0] - popoverWidth + popoverView.paddingLeft

                when (builder.align) {
                    Align.TOP ->
                        location[1] = anchorViewLocationInScreen[1] - popoverView.paddingTop
                    Align.BOTTOM ->
                        location[1] = anchorViewLocationInScreen[1] + anchorHeight - popoverHeight + popoverView.paddingTop
                    else -> {
                        location[1] = anchorViewLocationInScreen[1] + anchorHeight / 2 - popoverHeight / 2
                    }
                }
            }
            Position.RIGHT -> {
                location[0] = anchorViewLocationInScreen[0] + anchorWidth - popoverView.paddingLeft

                when (builder.align) {
                    Align.TOP ->
                        location[1] = anchorViewLocationInScreen[1] - popoverView.paddingTop
                    Align.BOTTOM ->
                        location[1] = anchorViewLocationInScreen[1] + anchorHeight - popoverHeight + popoverView.paddingTop
                    else -> {
                        location[1] = anchorViewLocationInScreen[1] + anchorHeight / 2 - popoverHeight / 2
                    }
                }
            }
            Position.TOP -> {
                location[1] = anchorViewLocationInScreen[1] - popoverHeight + popoverView.paddingTop
                when (builder.align) {
                    Align.LEFT -> {
                        location[0] = anchorViewLocationInScreen[0] - popoverView.paddingLeft
                    }
                    Align.RIGHT -> {
                        location[0] = anchorViewLocationInScreen[0] + anchorWidth - popoverWidth + popoverView.paddingLeft
                    }
                    else -> {
                        location[0] = anchorViewLocationInScreen[0] + anchorWidth / 2 - popoverWidth / 2
                    }
                }
            }
            Position.BOTTOM -> {
                location[1] = anchorViewLocationInScreen[1] + anchorHeight - popoverView.paddingTop
                when (builder.align) {
                    Align.LEFT -> {
                        location[0] = anchorViewLocationInScreen[0] - popoverView.paddingLeft

                    }
                    Align.RIGHT -> {
                        location[0] = anchorViewLocationInScreen[0] + anchorWidth - popoverWidth + popoverView.paddingLeft

                    }
                    else -> {
                        location[0] = anchorViewLocationInScreen[0] + anchorWidth / 2 - popoverWidth / 2
                    }
                }
            }
        }
        return location
    }

    /**
     * 防止popup window显示超出屏幕
     */
    private fun adjustPopupWindowLayoutParams(
        location: IntArray,
        popoverWidth: Int,
        popoverHeight: Int,
        screenWidth: Int,
        screenHeight: Int,
        popoverView: PopoverView,
    ) {
        if (location[0] + popoverWidth - popoverView.paddingLeft > screenWidth - builder.rightScreenMargin) {
            location[0] = screenWidth - popoverWidth + popoverView.paddingLeft - builder.rightScreenMargin
        }

        if (location[0] < builder.leftScreenMargin) {
            location[0] = -popoverView.paddingLeft + builder.leftScreenMargin

        }


        if (builder.position != Position.TOP && builder.position != Position.BOTTOM) {
            if (location[1] + popoverHeight > screenHeight - builder.bottomScreenMargin) {
                location[1] =
                    screenHeight - popoverHeight + popoverView.paddingBottom - builder.bottomScreenMargin
            }

            if (location[1] < builder.topScreenMargin) {
                location[1] = -popoverView.paddingTop + builder.topScreenMargin
            }
        } else {
            if (builder.position == Position.BOTTOM && location[1] + popoverHeight > screenHeight - builder.bottomScreenMargin) {
                // 向下弹出时，计算最大高度，防止超过距离下方的边距的设置
                popupWindow.height = screenHeight - location[1] - builder.bottomScreenMargin
            } else if (builder.position == Position.TOP && location[1] < builder.topScreenMargin) {
                // 向上弹出时，计算最大高度，防止超过距离上方边距的设置
                location[1] = -popoverView.paddingTop + builder.topScreenMargin
                popupWindow.height = (builder.anchor.y - builder.topScreenMargin).toInt()
            }
        }
    }

    /**
     * 配置阴影
     */
    private fun configShadow(popoverView: PopoverView) {
        if (builder.enableShadow) {
            popoverView.setDropShadow(
                builder.dropShadow.blurRadius,
                builder.dropShadow.dx,
                builder.dropShadow.dy,
                builder.dropShadow.color
            )
        }
    }

    /**
     * 配置PopupWindow
     */
    private fun configPopupWindow(popoverView: PopoverView): IntArray {
        popupWindow.contentView = popoverView
        popupWindow.width = WindowManager.LayoutParams.WRAP_CONTENT
        popupWindow.height = WindowManager.LayoutParams.WRAP_CONTENT
        popupWindow.isFocusable = builder.focusable
        popupWindow.isOutsideTouchable = true
        popupWindow.animationStyle = android.R.style.Animation_Dialog
        // 允许popup window超过屏幕
        popupWindow.isClippingEnabled = false
        mDismissListener = builder.dismissListener

        popoverView.isClickable = true
        popoverView.setOnClickListener { dismiss() }

        val anchorViewLocationInScreen = IntArray(2)
        builder.anchor.getLocationOnScreen(anchorViewLocationInScreen)

        popupWindow.contentView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        var popoverWidth = popupWindow.contentView.measuredWidth
        var popoverHeight = popupWindow.contentView.measuredHeight

        if (isTextContent) {
            popupWindow.width = popoverWidth
            popupWindow.height = popoverHeight
        }
        val anchorWidth = builder.anchor.width
        val anchorHeight = builder.anchor.height
        val screenWidth = builder.anchor.context.getLandScreenSize().width
        val screenHeight = builder.anchor.context.getLandScreenSize().height


        val popupWindowLocation = getShowLocation(anchorViewLocationInScreen, popoverWidth, popoverHeight, anchorWidth, anchorHeight, popoverView)
        adjustPopupWindowLayoutParams(popupWindowLocation, popoverWidth, popoverHeight, screenWidth, screenHeight, popoverView)

        val arrowPosition = when (builder.position) {
            Position.BOTTOM,
            Position.TOP,
            ->
                (anchorViewLocationInScreen[0] + anchorWidth / 2 - popupWindowLocation[0] - popoverView.paddingLeft) / (popoverWidth - popoverView.paddingLeft * 2).toFloat()
            else ->
                (anchorViewLocationInScreen[1] + anchorHeight / 2 - popupWindowLocation[1] - popoverView.paddingTop) / (popoverHeight - popoverView.paddingTop * 2).toFloat()
        }

        popoverView.setArrowOffset(arrowPosition)

        return popupWindowLocation
    }

    /**
     * 创建popupwindow的内容
     */
    private fun createPopoverView(): PopoverView {
        isTextContent = false

        val arrowPosition = when (builder.position) {
            Position.LEFT -> PopoverView.ArrowPosition.RIGHT
            Position.RIGHT -> PopoverView.ArrowPosition.LEFT
            Position.TOP -> PopoverView.ArrowPosition.BOTTOM
            Position.BOTTOM -> PopoverView.ArrowPosition.TOP
        }

        val popoverView = PopoverView(builder.anchor.context,
            popoverBackgroundColor = builder.backgroundColor,
            arrowColor = builder.arrowColor,
            showArrow = builder.showArrow,
            arrowPosition = arrowPosition,
            borderRadius = builder.anchor.resources.getDimension(R.dimen.uxsdk_4_dp))

        if (builder.customView != null) {
            popoverView.setContentView(builder.customView!!, builder.layoutParams)
        } else if (builder.customLayoutRes != 0) {
            val view = LayoutInflater.from(builder.anchor.context).inflate(builder.customLayoutRes, popoverView, false)
            popoverView.setContentView(view, view.layoutParams)
        } else if (builder.content != null) {
            // 纯文本提示的Popover
            isTextContent = true
            val textView = TextView(builder.anchor.context)
            textView.text = builder.content
            textView.setTextColor(builder.anchor.resources.getColor(builder.textColor))
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12F)
            textView.setTypeface(textView.typeface, Typeface.BOLD)
            val padding = builder.anchor.resources.getDimension(R.dimen.uxsdk_8_dp).toInt()
            textView.setPadding(padding, padding, padding, padding)
            popoverView.setContentView(textView, builder.layoutParams)
        }

        return popoverView
    }

    fun show() {
        val popupWindowLocation = initPopover()
        popupWindow.showAtLocation(builder.anchor,
            Gravity.NO_GRAVITY,
            popupWindowLocation[0] + builder.xOffset,
            popupWindowLocation[1] + builder.yOffset)

    }

    fun show(gravity: Int, x: Int, y: Int) {
        initPopover()
        popupWindow.showAtLocation(builder.anchor, gravity, x, y)

    }

    fun getContentView(): View? {
        return popupWindow.contentView
    }

    private fun initPopover(): IntArray {
        popoverView = createPopoverView()
        configShadow(popoverView)
        val popupWindowLocation = configPopupWindow(popoverView)
        width = popoverView.measuredWidth
        height = popoverView.measuredHeight

        popoverView.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            val newWidth = right - left
            val newHeight = bottom - top

            if (newWidth != width || newHeight != height) {
                width = newWidth
                height = newHeight
                requestLayout()
            }
        }

        if (popupWindow.isFocusable) {
            popupWindow.contentView?.let {
                it.isFocusable = true
                it.isFocusableInTouchMode = true
                it.requestFocus()
                it.setOnKeyListener(DispatchViewKeyEventToActivityListener())
            }
        }

        return popupWindowLocation
    }

    fun dismiss() {
        popupWindow.dismiss()
    }

    fun isShowing(): Boolean {
        return popupWindow.isShowing
    }

    fun setOnDismissListener(dismissListener: PopupWindow.OnDismissListener) {
        mDismissListener = dismissListener;
    }

    fun requestLayout() {
        popoverView?.post(requestLayoutRunnable)
    }

    /**
     * 对齐Anchor View的方式
     */
    enum class Align {
        LEFT,
        RIGHT,
        CENTER,
        TOP,
        BOTTOM
    }

    /**
     * Popover相对Anchor View弹出的位置
     */
    enum class Position {
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }


    class Builder(var anchor: View) {
        var content: CharSequence? = null
        var customView: View? = null
        var customLayoutRes: Int = 0
        var layoutParams: ViewGroup.LayoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        var align: Align = Align.CENTER
        var position: Position = Position.BOTTOM
        var enableShadow: Boolean = true
        var showArrow: Boolean = true
        var focusable: Boolean = true
        var backgroundColor: Int = Color.parseColor("#FFCC00")
        var dropShadow: DropShadow
        var dismissListener: PopupWindow.OnDismissListener? = null
        var xOffset: Int = 0
        var yOffset: Int = 0
        var leftScreenMargin: Int = 0
        var rightScreenMargin: Int = 0
        var topScreenMargin: Int = 0
        var bottomScreenMargin: Int = 0
        var arrowColor: Int = backgroundColor
        var textColor: Int = R.color.uxsdk_black

        init {
            dropShadow = DropShadow(
                color = anchor.resources.getColor(R.color.uxsdk_black_20_percent),
                dx = 0F,
                dy = anchor.resources.getDimension(R.dimen.uxsdk_4_dp),
                blurRadius = anchor.resources.getDimension(R.dimen.uxsdk_16_dp)
            )

        }

        /**
         * 显示的文本
         */
        fun content(content: CharSequence?): Builder {
            this.content = content
            return this
        }

        fun content(@StringRes contentRes: Int): Builder {
            this.content = anchor.context.getString(contentRes)
            return this
        }

        /**
         * 设置自定义的view
         */
        fun customView(view: View?): Builder {
            this.customView = view
            return this
        }

        fun customView(@LayoutRes layoutRes: Int): Builder {
            this.customLayoutRes = layoutRes
            return this
        }

        /**
         * 自定义view添加到popover里的layout参数
         * 默认是wrap_content
         */
        fun layoutParams(layoutParams: ViewGroup.LayoutParams): Builder {
            this.layoutParams = layoutParams
            return this
        }

        /**
         * 自定义view的尺寸
         */
        fun size(width: Int, height: Int): Builder {
            this.layoutParams = ViewGroup.LayoutParams(width, height)
            return this
        }

        /**
         * 对齐anchor的方式
         */
        fun align(align: Align): Builder {
            this.align = align
            return this
        }

        /**
         * 显示在anchor的哪个位置上
         */
        fun position(position: Position): Builder {
            this.position = position
            return this
        }

        /**
         * 是否使用阴影
         */
        fun enableShadow(enableShadow: Boolean): Builder {
            this.enableShadow = enableShadow
            return this
        }

        /**
         * 是否显示小箭头
         */
        fun showArrow(showArrow: Boolean): Builder {
            this.showArrow = showArrow
            return this
        }

        /**
         * focusable设置为true时，popupwindow会处理返回键等事件，设置为false时，点击事件可以穿透popupwindow传递给下层
         * 默认是true
         */
        fun focusable(focusable: Boolean): Builder {
            this.focusable = focusable
            return this
        }

        /**
         * 背景颜色
         */
        fun backgroundColor(backgroundColor: Int): Builder {
            this.backgroundColor = backgroundColor
            return this
        }

        /**
         * 阴影参数
         * color: 阴影颜色
         * dx: x轴偏移
         * dy: y轴偏移
         * blurRadius: 模糊范围，数值越大模糊范围越大
         */
        fun dropShadow(dropShadow: DropShadow): Builder {
            this.dropShadow = dropShadow
            return this
        }

        /**
         * popover消失时的回调
         */
        fun onDismiss(dismissListener: PopupWindow.OnDismissListener): Builder {
            this.dismissListener = dismissListener
            return this
        }

        /**
         * 显示坐标x轴偏移
         * 默认0
         */
        fun xOffset(xOffset: Int): Builder {
            this.xOffset = xOffset
            return this
        }

        /**
         * 显示坐标y轴偏移
         * 默认是0
         */
        fun yOffset(yOffset: Int): Builder {
            this.yOffset = yOffset
            return this
        }

        /**
         * 距离屏幕边缘的边距
         */
        fun allScreenMargin(margin: Int): Builder {
            this.topScreenMargin = margin
            this.rightScreenMargin = margin
            this.bottomScreenMargin = margin
            this.leftScreenMargin = margin
            return this
        }

        /**
         * 距离右边屏幕边距
         */
        fun rightScreenMargin(margin: Int): Builder {
            this.rightScreenMargin = margin
            return this
        }

        /**
         * 距离下边屏幕的边距
         */
        fun bottomScreenMargin(margin: Int): Builder {
            this.bottomScreenMargin = margin
            return this
        }

        /**
         * 距离左边屏幕的边距
         */
        fun leftScreenMargin(margin: Int): Builder {
            this.leftScreenMargin = margin
            return this
        }

        /**
         * 距离上边屏幕的边距
         */
        fun topScreenMargin(margin: Int): Builder {
            this.topScreenMargin = margin
            return this
        }

        /**
         * 指定箭头颜色
         */
        fun arrowColor(color: Int): Builder {
            this.arrowColor = color
            return this
        }


        fun build(): Popover {
            return Popover(this)
        }

        fun textColor(color: Int): Builder {
            this.textColor = color
            return this
        }
    }

    /**
     * 阴影配置
     */
    class DropShadow(
        var color: Int = 0,
        var dx: Float = 0F,
        var dy: Float = 0F,
        var blurRadius: Float = 0F,
    )

}