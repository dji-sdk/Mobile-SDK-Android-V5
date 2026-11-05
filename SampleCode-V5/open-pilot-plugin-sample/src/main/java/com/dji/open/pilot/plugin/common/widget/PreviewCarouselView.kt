package com.dji.open.pilot.plugin.common.widget

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.dji.open.pilot.plugin.R
import kotlin.math.roundToInt

/**
 * 通用预览图轮播组件。
 *
 * 结构（叠加）：底层 [ViewPager2] 展示图片，其上叠加左右翻页按钮与底部小方块指示器。
 * 对外仅暴露 [setImages]。各演示模块（地图控件 / AR / 对话框 等）右侧预览区可直接复用。
 *
 * 规则：
 * - 传入图片数 >= 2 时显示指示器与左右按钮，可点击 / 滑动翻页；
 * - 传入 1 张时隐藏指示器与按钮，仅展示该图；
 * - 传入空列表时整体隐藏（GONE）。
 *
 * 图片圆角：每页图片按 fitCenter 后的实际显示区域裁 8dp 圆角，不裁剪、不改变比例。
 * 不含自动轮播（按需再加），纯手动翻页。
 */
class PreviewCarouselView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val pager: ViewPager2 = ViewPager2(context)
    private val indicatorBar: LinearLayout = LinearLayout(context)
    private val prevButton: ImageView = ImageView(context)
    private val nextButton: ImageView = ImageView(context)
    private val adapter = PreviewPagerAdapter()

    /** 当前指示条视图列表，索引与页码一一对应 */
    private val indicatorDots = mutableListOf<View>()

    init {
        // 水平 padding 把图片区挤到中间，让左右翻页按钮稳定落在图片外侧；
        // clipToPadding=false 使按钮可绘制到 padding 区。
        setPadding(dp(HORIZONTAL_PADDING_DP), 0, dp(HORIZONTAL_PADDING_DP), 0)
        clipToPadding = false

        pager.adapter = adapter
        addView(pager, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        addPageButton(prevButton, Gravity.START or Gravity.CENTER_VERTICAL, isNext = false)
        addPageButton(nextButton, Gravity.END or Gravity.CENTER_VERTICAL, isNext = true)

        indicatorBar.orientation = LinearLayout.HORIZONTAL
        indicatorBar.gravity = Gravity.CENTER
        addView(indicatorBar, LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply { bottomMargin = dp(INDICATOR_BOTTOM_MARGIN_DP) })

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicator(position)
                updateButtonState(position)
            }
        })
    }

    /**
     * 设置轮播图片资源列表。
     * @param resIds drawable 资源 ID 列表；空列表时组件隐藏。
     */
    fun setImages(resIds: List<Int>) {
        if (resIds.isEmpty()) {
            visibility = View.GONE
            return
        }
        visibility = View.VISIBLE
        adapter.submit(resIds)
        pager.setCurrentItem(0, false)
        buildIndicator(resIds.size)
        updateButtonState(0)
    }

    /** 创建一个左/右翻页按钮并叠加到指定位置 */
    private fun addPageButton(button: ImageView, gravity: Int, isNext: Boolean) {
        val size = dp(ARROW_BUTTON_SIZE_DP)
        button.setImageResource(R.drawable.icon_back)
        button.setBackgroundResource(R.drawable.bg_carousel_arrow)
        button.setPadding(dp(ARROW_PADDING_DP), dp(ARROW_PADDING_DP), dp(ARROW_PADDING_DP), dp(ARROW_PADDING_DP))
        // icon_back 本身为左向箭头，右按钮水平镜像
        if (isNext) button.scaleX = -1f
        button.setOnClickListener { stepPage(if (isNext) 1 else -1) }
        // gravity 对齐到 padding 内沿（即图片边缘）；用负 margin 把按钮推到 padding 区，
        // 使按钮内侧与图片保持 ARROW_IMAGE_GAP_DP 间距，稳定落在图片外侧。
        val inset = -(dp(ARROW_BUTTON_SIZE_DP) + dp(ARROW_IMAGE_GAP_DP))
        addView(button, LayoutParams(size, size, gravity).apply {
            if (isNext) marginEnd = inset else marginStart = inset
        })
    }

    /** 翻页：delta 为 +1（下一张）/ -1（上一张），越界不处理 */
    private fun stepPage(delta: Int) {
        val target = pager.currentItem + delta
        if (target in 0 until adapter.itemCount) {
            pager.setCurrentItem(target, true)
        }
    }

    /** 依据图片数量重建指示条；单图或空图时隐藏指示器与按钮 */
    private fun buildIndicator(count: Int) {
        indicatorBar.removeAllViews()
        indicatorDots.clear()
        val hasMultiple = count > 1
        indicatorBar.visibility = if (hasMultiple) View.VISIBLE else View.GONE
        prevButton.visibility = if (hasMultiple) View.VISIBLE else View.GONE
        nextButton.visibility = if (hasMultiple) View.VISIBLE else View.GONE
        if (!hasMultiple) return

        repeat(count) {
            val dot = View(context)
            val params = LinearLayout.LayoutParams(dp(DOT_NORMAL_WIDTH_DP), dp(DOT_HEIGHT_DP))
            params.marginStart = dp(DOT_GAP_DP)
            params.marginEnd = dp(DOT_GAP_DP)
            dot.layoutParams = params
            dot.setBackgroundResource(R.drawable.bg_carousel_indicator_normal)
            indicatorBar.addView(dot)
            indicatorDots.add(dot)
        }
        updateIndicator(0)
    }

    /** 刷新指示条选中态：选中项加宽蓝色，其余短半透明 */
    private fun updateIndicator(selectedPosition: Int) {
        indicatorDots.forEachIndexed { index, dot ->
            val isSelected = index == selectedPosition
            val widthDp = if (isSelected) DOT_SELECTED_WIDTH_DP else DOT_NORMAL_WIDTH_DP
            dot.layoutParams = (dot.layoutParams as LinearLayout.LayoutParams).apply {
                width = dp(widthDp)
            }
            dot.setBackgroundResource(
                if (isSelected) R.drawable.bg_carousel_indicator_selected
                else R.drawable.bg_carousel_indicator_normal
            )
        }
    }

    /** 首页禁用「上一张」、尾页禁用「下一张」（半透明表示不可用） */
    private fun updateButtonState(position: Int) {
        val lastIndex = adapter.itemCount - 1
        prevButton.isEnabled = position > 0
        prevButton.alpha = if (position > 0) 1f else DISABLED_ALPHA
        nextButton.isEnabled = position < lastIndex
        nextButton.alpha = if (position < lastIndex) 1f else DISABLED_ALPHA
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
    ).roundToInt()

    /**
     * ViewPager2 的图片适配器：每页一个 ImageView（fitCenter），
     * 并按图片实际显示区域裁圆角（不裁剪图片、不改变比例）。
     */
    private class PreviewPagerAdapter : RecyclerView.Adapter<PreviewPagerAdapter.ImageHolder>() {

        private val imageResIds = mutableListOf<Int>()

        fun submit(resIds: List<Int>) {
            imageResIds.clear()
            imageResIds.addAll(resIds)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_preview_carousel, parent, false) as ImageView
            return ImageHolder(view)
        }

        override fun onBindViewHolder(holder: ImageHolder, position: Int) {
            holder.image.setImageResource(imageResIds[position])
            holder.image.invalidateOutline()
        }

        override fun getItemCount(): Int = imageResIds.size

        class ImageHolder(val image: ImageView) : RecyclerView.ViewHolder(image) {
            init {
                image.clipToOutline = true
                image.outlineProvider = ImageContentRoundOutline()
                // 尺寸变化后 imageMatrix 才就绪，重算圆角 outline
                image.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                    if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                        v.invalidateOutline()
                    }
                }
            }
        }
    }

    companion object {
        private const val INDICATOR_BOTTOM_MARGIN_DP = 8
        private const val DOT_HEIGHT_DP = 4
        private const val DOT_NORMAL_WIDTH_DP = 12
        private const val DOT_SELECTED_WIDTH_DP = 24
        private const val DOT_GAP_DP = 3
        private const val ARROW_BUTTON_SIZE_DP = 24
        private const val ARROW_PADDING_DP = 4
        private const val ARROW_IMAGE_GAP_DP = 8
        private const val HORIZONTAL_PADDING_DP = 32
        private const val DISABLED_ALPHA = 0.3f
    }
}
