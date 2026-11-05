package com.dji.open.pilot.plugin.common.drawing

import com.dji.open.pilot.plugin.R
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.FontStyleType
import dji.open.pilot.base.ui.model.ShapeDescriptor
import dji.open.pilot.base.ui.model.WidgetLayoutParam
import dji.v5.ux.remoteview.RemoteButton
import dji.v5.ux.remoteview.RemoteLinearLayout
import dji.v5.ux.remoteview.RemoteViewHelper
import dji.v5.ux.util.RemoteViewColors
import kotlin.math.max
import kotlin.math.min

/**
 * 绘制上下文，封装各 Drawer 共享的 helper 引用和公共工具函数。
 * 由 RemoteViewPageDemoViewModel 构造并传入各 Drawer。
 */
class DrawingContext(val helper: RemoteViewHelper) {

    /** 卡片背景色（20% 白色透明），所有 card() 使用 */
    val cardBgColor = RemoteViewColors.WHITE_20

    /** 构造 weight + margin 的 WidgetLayoutParam */
    fun weightParam(weight: Float, marginDp: Int = 0): WidgetLayoutParam =
        WidgetLayoutParam().apply {
            setWeight(weight)
            if (marginDp > 0) setMargin(marginDp, marginDp, marginDp, marginDp)
        }

    /** 构造纯 margin 的 WidgetLayoutParam */
    fun marginParam(marginDp: Int = 0): WidgetLayoutParam =
        WidgetLayoutParam().apply {
            if (marginDp > 0) setMargin(marginDp, marginDp, marginDp, marginDp)
        }

    /** 构造纯 margin 的 WidgetLayoutParam */
    fun marginParam(left:Int, top:Int , right:Int, bottom:Int): WidgetLayoutParam =
        WidgetLayoutParam().apply {
            setMargin(max(0,left) , max(0,top), max(0, right), max(0, bottom))
        }

    /**
     * 创建统一样式的卡片容器，并添加到 root。
     * padding=4，圆角 4dp，背景色 cardBgColor，上方 margin=4。
     */
    fun card(root: RemoteLinearLayout): RemoteLinearLayout {
        val card = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setPadding(4, 4, 4, 4)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(cardBgColor)
                    .cornerRadius(4f)
                    .build()
            )
        }
        root.addToRemote(card, marginParam(4))
        return card
    }

    /**
     * 通用带标题卡片：卡片外加粗标题（可选）+ 统一样式卡片容器。
     * 内容通过 [block] 自由填充，支持任意组件混排（按钮 / 输入框 / TextView / 开关等），
     * 而非局限于某一种固定布局。
     *
     * 示例：绘制"提示文本 + 输入框 + 提交按钮"混排：
     * ```
     * ctx.titledCard(root, "设置") { card ->
     *     card.addToRemote(helper.createText().apply { setText("请输入名称") }, ctx.marginParam(2))
     *     card.addToRemote(helper.createEditText().apply { setWidth(RemoteView.MATCH_PARENT) }, ctx.marginParam(2))
     *     card.addToRemote(ctx.primaryButton(R.string.submit, ::onSubmit), ctx.marginParam(2))
     * }
     * ```
     *
     * @param title 卡片外加粗标题，为空则不绘制标题
     * @param block 内容填充回调，参数为卡片容器
     * @return 卡片容器，供调用方继续 addToRemote
     */
    fun titledCard(
        root: RemoteLinearLayout,
        title: String? = null,
        block: (card: RemoteLinearLayout) -> Unit = {}
    ): RemoteLinearLayout {
        if (!title.isNullOrEmpty()) addCardTitle(root, title)
        val card = card(root)
        block(card)
        return card
    }

    /** [titledCard] 的多语言重载：标题通过资源 ID 设置。 */
    fun titledCard(
        root: RemoteLinearLayout,
        titleResId: Int,
        block: (card: RemoteLinearLayout) -> Unit = {}
    ): RemoteLinearLayout {
        addCardTitle(root, titleResId)
        val card = card(root)
        block(card)
        return card
    }

    /**
     * 创建统一样式的主按钮（蓝底白字加粗圆角），不负责添加到父容器。
     * 宽高由调用方决定：全宽垂直堆叠传 [RemoteView.MATCH_PARENT]，横向等分传 0 并配合 [weightParam]。
     *
     * @param textResId 按钮文案资源 ID（多语言）
     * @param onClick 点击回调
     * @param width 宽度，默认全宽
     * @param height 高度，默认 32
     */
    fun primaryButton(
        textResId: Int,
        onClick: () -> Unit,
        width: Int = RemoteView.MATCH_PARENT,
        height: Int = 32
    ): RemoteButton {
        val btn = helper.createButton().apply {
            setTextByResId(textResId)
            setWidth(width); setHeight(height); setTextSize(11f)
            setTextColor(RemoteViewColors.WHITE)
            setFontStyle(FontStyleType.BOLD)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(RemoteViewColors.BLUE_PRIMARY)
                    .cornerRadius(CORNER_DEFAULT)
                    .build()
            )
        }
        btn.setOnClickListener { onClick() }
        return btn
    }

    /**
     * 在 root 末尾追加"返回插件 APP"按钮。
     * 全宽圆角按钮，点击后通过 onNavigateToApp 回调触发跳转。
     */
    fun addBackButton(root: RemoteLinearLayout, onNavigateToApp: () -> Unit) {
        val btn = helper.createButton().apply {
            setTextByResId(R.string.btn_back_to_plugin_app)
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(36)
            setTextSize(12f)
            setTextColor(RemoteViewColors.WHITE)
            setBackground(RemoteViewColors.BLUE_PRIMARY)
            setFontStyle(FontStyleType.BOLD)
        }
        root.addToRemote(btn, marginParam(4))
        btn.setOnClickListener { onNavigateToApp() }
    }

    /**
     * 在 card 内追加 section 小标题（灰色，12sp，上间距 4dp）。
     * 所有 Drawer 统一使用，不在各自文件中重复定义。
     */
    fun addSectionLabel(card: RemoteLinearLayout, text: String) {
        card.addToRemote(helper.createText().apply {
            setText(text)
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setTextSize(12f)
        }, marginParam(4))
    }

    /** [addSectionLabel] 的多语言重载：文案通过资源 ID 设置。 */
    fun addSectionLabel(card: RemoteLinearLayout, textResId: Int) {
        card.addToRemote(helper.createText().apply {
            setTextByResId(textResId)
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setTextSize(12f)
        }, marginParam(4))
    }

    /**
     * 在 root 上追加卡片外的加粗标题（如 "01 组件类型"）。
     * 与卡片内部 section 标题（addSectionLabel）区分。
     */
    fun addCardTitle(root: RemoteLinearLayout, title: String) {
        root.addToRemote(helper.createText().apply {
            setText(title)
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setFontStyle(FontStyleType.BOLD)
        }, marginParam(2))
    }

    /** [addCardTitle] 的多语言重载：文案通过资源 ID 设置。 */
    fun addCardTitle(root: RemoteLinearLayout, titleResId: Int) {
        root.addToRemote(helper.createText().apply {
            setTextByResId(titleResId)
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setFontStyle(FontStyleType.BOLD)
        }, marginParam(2))
    }

    /**
     * 在 card 内 Section 之间插入一条水平分割线（1dp 高，20% 白色透明）。
     * 仅在同一 card 内有 2 组及以上 Section 时使用。
     */
    fun addDivider(card: RemoteLinearLayout) {
        val divider = helper.createText().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(1)
            setAlpha(0.5f)
            setBackgroundShape(ShapeDescriptor.Builder().fillColor(RemoteViewColors.WHITE_20).build())
        }
        card.addToRemote(divider, WidgetLayoutParam().apply {
            setMargin(6, 4, 6, 4)
        })
    }

    companion object {
        /** 标准圆角值，所有 Drawer 统一使用 */
        const val CORNER_DEFAULT = 4f
        const val CORNER_LARGE   = 24f

        const val ICON_FRONT_ARROW      = "icon_front_arrow.webp"
        const val ICON_NEXT_ARROW       = "icon_next_arrow.webp"
        const val MARKER_WIND_TURBINE   = "map_mark_wind_turbine.webp"
        const val PHOTO_SEA_VIEW   = "sea_view.webp"
        const val ICON_IDF_STATUS_SUCCESS = "icon_idf_status_success.webp"
        const val ICON_IDF_STATUS_ERROR = "icon_idf_status_error.webp"
        const val OPEN_PILOT_PLUGIN_APP_ICON = "open_pilot_plugin_app_icon.webp"

        val markerIconList = listOf(
            OPEN_PILOT_PLUGIN_APP_ICON,
            "icon_open_wind_mill_normal.webp",
            "icon_open_wind_mill_clicked.webp",
            "img_open_wind_imageView_RC.webp",
            "img_open_wind_imageView_m4e.webp",
            "img_open_pilot_device_m3m.webp",
            "icon_dji_care_osmo.webp",
            "icon_dji_care_osmo.webp",
            ICON_IDF_STATUS_SUCCESS,
            ICON_IDF_STATUS_ERROR,
            MARKER_WIND_TURBINE,
            ICON_FRONT_ARROW,
            ICON_NEXT_ARROW,
            PHOTO_SEA_VIEW,
            // AR 模块图标
            "ar_cable_highlight_dot.svg",
            "ar_point_star_max_icon.svg",
            "ar_point_star_min_icon.svg",
            // 地图控件模块 marker 图标
            "map_marker_rc.png",
            "map_marker_direction.png",
            "map_marker_center.png"
        )
    }

    // ── 公共 CRUD 卡片绘制能力 ──────────────────────────────────────────────

    /**
     * 绘制结果展示卡片：卡片内含单个 TextView，用于展示查询/操作结果。
     * @return TextView 的 viewId，供后续 updateView 写入内容
     */
    fun drawResultCard(root: RemoteLinearLayout): String {
        addCardTitle(root, R.string.card_title_result)
        val resultCard = card(root)
        val resultText = helper.createText().apply {
            setText("")
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setTextSize(11f)
        }
        resultCard.addToRemote(resultText, marginParam(4))
        return resultText.getId()
    }

    /**
     * 绘制操作按钮卡片：卡片内含 N 个水平等分的填充按钮。
     * 按钮样式为蓝底白字加粗圆角（与 remoteviewpage ButtonDrawer 的 filledBtn 一致）。
     * @param buttons 按钮配置列表
     */
    fun drawActionCard(root: RemoteLinearLayout, buttons: List<ActionButtonConfig>) {
        titledCard(root, R.string.card_title_action) { actionCard ->
            val row = helper.createLinearLayoutHorizontal().apply {
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(RemoteView.WRAP_CONTENT)
            }
            actionCard.addToRemote(row, marginParam(2))

            buttons.forEach { config ->
                // 横向等分：width=0 + weightParam(1)
                val btn = primaryButton(config.textResId, config.onClick, width = 0)
                row.addToRemote(btn, weightParam(1f, 2))
            }
        }
    }
}

/**
 * 操作按钮配置。
 * @param textResId 按钮文案的字符串资源 ID（通过 setTextByResId 设置，支持多语言）
 * @param onClick 点击回调
 */
data class ActionButtonConfig(
    val textResId: Int,
    val onClick: () -> Unit
)
