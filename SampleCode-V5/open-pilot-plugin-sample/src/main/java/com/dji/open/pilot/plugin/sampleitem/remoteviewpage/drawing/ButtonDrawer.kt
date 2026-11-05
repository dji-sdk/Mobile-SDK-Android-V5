package com.dji.open.pilot.plugin.sampleitem.remoteviewpage.drawing

import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.drawing.DrawingContext

import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.FontStyleType
import dji.open.pilot.base.ui.model.ShapeDescriptor
import dji.open.pilot.base.ui.model.WidgetLayoutParam
import dji.v5.ux.remoteview.RemoteButton
import dji.v5.ux.remoteview.RemoteLinearLayout
import dji.v5.ux.util.RemoteViewColors

/**
 * Button 组件演示：基础按钮（填充/描边/文字）、组合按钮（TabLayout 效果）、
 * 禁用态、按钮尺寸、按钮形状、自定义颜色。
 */
object ButtonDrawer {

    private const val CORNER_DEFAULT = 4f
    private const val CORNER_LARGE   = 24f
    private const val BTN_HEIGHT     = 30
    private val CUSTOM_GREEN         = 0xFF00C774.toInt()

    fun draw(ctx: DrawingContext, root: RemoteLinearLayout) {
        val helper = ctx.helper

        // ── 工具函数：创建填充按钮 ───────────────────────────────────────────
        fun filledBtn(
            textResId: Int, bgColor: Int, textColor: Int,
            corner: Float = CORNER_DEFAULT, textSize: Float = 12f, height: Int = BTN_HEIGHT
        ) = helper.createButton().apply {
            setTextByResId(textResId)
            setWidth(RemoteView.MATCH_PARENT); setHeight(height); setTextSize(textSize)
            setTextColor(textColor); setFontStyle(FontStyleType.BOLD)
            setBackgroundShape(ShapeDescriptor.Builder().fillColor(bgColor).cornerRadius(corner).build())
        }

        // ── 工具函数：创建描边按钮 ───────────────────────────────────────────
        fun strokeBtn(
            textResId: Int, borderColor: Int, textColor: Int,
            corner: Float = CORNER_DEFAULT, textSize: Float = 12f, height: Int = BTN_HEIGHT, widthDp:Int = 1
        ) = helper.createButton().apply {
            setTextByResId(textResId)
            setWidth(RemoteView.MATCH_PARENT); setHeight(height); setTextSize(textSize)
            setTextColor(textColor); setFontStyle(FontStyleType.BOLD)
            setBackgroundShape(ShapeDescriptor.Builder().stroke(widthDp, borderColor).cornerRadius(corner).build())
        }

        // ── 工具函数：创建文字按钮（无背景）─────────────────────────────────
        fun textBtn(
            textResId: Int, textColor: Int,
            textSize: Float = 12f, height: Int = BTN_HEIGHT
        ) = helper.createButton().apply {
            setTextByResId(textResId)
            setWidth(RemoteView.MATCH_PARENT); setHeight(height); setTextSize(textSize)
            setFontStyle(FontStyleType.BOLD); setTextColor(textColor)
        }

//        // ── 工具函数：card 内 section 标题 ──────────────────────────────────
//        fun addSectionTitle(card: RemoteLinearLayout, title: String) {
//            card.addToRemote(helper.createText().apply {
//                setText(title)
//                setWidth(RemoteView.MATCH_PARENT); setHeight(RemoteView.WRAP_CONTENT)
//                setTextSize(12f)
//            }, ctx.marginParam(4))
//        }

        fun hRow(height: Int = RemoteView.WRAP_CONTENT) =
            helper.createLinearLayoutHorizontal().apply {
                setWidth(RemoteView.MATCH_PARENT); setHeight(height)
            }

        // ── 01 组件类型 ──────────────────────────────────────────────────────
        ctx.addCardTitle(root, R.string.demo_card_type)
        val typeCard = ctx.card(root)

        ctx.addSectionLabel(typeCard, R.string.demo_button_basic)
        typeCard.addToRemote(filledBtn(R.string.demo_button_filled, RemoteViewColors.BLUE_PRIMARY, RemoteViewColors.WHITE), ctx.marginParam(6,2,6,2))
        typeCard.addToRemote(strokeBtn(R.string.demo_button_stroke, RemoteViewColors.WHITE, RemoteViewColors.WHITE), ctx.marginParam(6,2,6,2))
        typeCard.addToRemote(textBtn(R.string.demo_button_text,   RemoteViewColors.BLUE_PRIMARY), ctx.marginParam(6,2,6,2))

        ctx.addDivider(typeCard)
        ctx.addSectionLabel(typeCard, R.string.demo_button_group)

        // TabLayout 效果：左右各自圆角，中间直角
        val leftCorner  = floatArrayOf(CORNER_DEFAULT, CORNER_DEFAULT, 0f, 0f, 0f, 0f, CORNER_DEFAULT, CORNER_DEFAULT)
        val rightCorner = floatArrayOf(0f, 0f, CORNER_DEFAULT, CORNER_DEFAULT, CORNER_DEFAULT, CORNER_DEFAULT, 0f, 0f)

        fun setActive(btn: RemoteButton, corner: FloatArray) {
            btn.setBackgroundShape(ShapeDescriptor.Builder()
                .fillColor(RemoteViewColors.BLUE_PRIMARY).cornerRadii(corner).build())
            btn.setTextColor(RemoteViewColors.WHITE)
            btn.setFontStyle(FontStyleType.BOLD)
            btn.syncToRemote()
        }
        fun setInactive(btn: RemoteButton, corner: FloatArray) {
            btn.setBackgroundShape(ShapeDescriptor.Builder()
                .fillColor(RemoteViewColors.LIGHT_GREY_53).cornerRadii(corner).build())
            btn.setFontStyle(FontStyleType.NORMAL)
            btn.syncToRemote()
        }

        val leftButton = helper.createButton().apply {
            setTextByResId(R.string.demo_button_primary)
            setWidth(0); setHeight(BTN_HEIGHT); setTextSize(12f)
            setFontStyle(FontStyleType.BOLD)
            setTextColor(RemoteViewColors.WHITE)
            setBackgroundShape(ShapeDescriptor.Builder()
                .fillColor(RemoteViewColors.BLUE_PRIMARY).cornerRadii(leftCorner).build())
        }
        val rightButton = helper.createButton().apply {
            setTextByResId(R.string.demo_button_secondary)
            setWidth(0); setHeight(BTN_HEIGHT); setTextSize(12f)
            setFontStyle(FontStyleType.NORMAL)
            setBackgroundShape(ShapeDescriptor.Builder()
                .fillColor(RemoteViewColors.NEAR_WHITE_20).cornerRadii(rightCorner).build())
        }
        leftButton.setOnClickListener  { setActive(leftButton, leftCorner);   setInactive(rightButton, rightCorner) }
        rightButton.setOnClickListener { setActive(rightButton, rightCorner); setInactive(leftButton, leftCorner) }

        val tabRow = hRow()
        typeCard.addToRemote(tabRow, ctx.marginParam(2))
        tabRow.addToRemote(leftButton,  WidgetLayoutParam().apply { weight = 1f; marginLeft  = 8 })
        tabRow.addToRemote(rightButton, WidgetLayoutParam().apply { weight = 1f; marginRight = 8; marginLeft = 1 })

        // ── 02 组件状态 ──────────────────────────────────────────────────────
        ctx.addCardTitle(root, R.string.demo_card_state)
        val stateCard = ctx.card(root)

        ctx.addSectionLabel(stateCard, R.string.demo_button_disabled)
        listOf(
            filledBtn(R.string.demo_button_filled, RemoteViewColors.BLUE_PRIMARY, RemoteViewColors.WHITE),
            strokeBtn(R.string.demo_button_stroke, RemoteViewColors.WHITE,       RemoteViewColors.WHITE),
            textBtn(R.string.demo_button_text,   RemoteViewColors.BLUE_PRIMARY)
        ).forEach { btn ->
            btn.setEnabled(false); btn.setAlpha(0.6f)
            stateCard.addToRemote(btn, ctx.marginParam(6,2,6,2))
        }

        // ── 03 组件样式 ──────────────────────────────────────────────────────
        ctx.addCardTitle(root, R.string.demo_card_style)
        val styleCard = ctx.card(root)

        ctx.addSectionLabel(styleCard, R.string.demo_button_size)
        listOf(Pair(R.string.demo_button_size_l, 32), Pair(R.string.demo_button_size_m, 26), Pair(R.string.demo_button_size_s, 20))
            .forEach { (textResId, height) ->
                styleCard.addToRemote(
                    filledBtn(textResId, RemoteViewColors.BLUE_PRIMARY, RemoteViewColors.WHITE, height = height),
                    ctx.marginParam(6,2,6,2)
                )
            }

        ctx.addDivider(styleCard)
        ctx.addSectionLabel(styleCard, R.string.demo_button_shape)
        listOf(
            Pair(R.string.demo_button_shape_square,  0f),
            Pair(R.string.demo_button_shape_round,   CORNER_DEFAULT),
            Pair(R.string.demo_button_shape_circle,  CORNER_LARGE)
        ).forEach { (textResId, corner) ->
            styleCard.addToRemote(
                filledBtn(textResId, RemoteViewColors.BLUE_PRIMARY, RemoteViewColors.WHITE, corner = corner),
                ctx.marginParam(6,2,6,2)
            )
        }

        ctx.addDivider(styleCard)
        ctx.addSectionLabel(styleCard, R.string.demo_custom_color)
        styleCard.addToRemote(filledBtn(R.string.demo_button_filled, CUSTOM_GREEN,                    RemoteViewColors.WHITE),       ctx.marginParam(6,2,6,2))
        styleCard.addToRemote(strokeBtn(R.string.demo_button_stroke, RemoteViewColors.BLUE_PRIMARY,   RemoteViewColors.BLUE_PRIMARY, widthDp = 2), ctx.marginParam(6,2,6,2))
        styleCard.addToRemote(textBtn(R.string.demo_button_text,   RemoteViewColors.RED_PRIMARY),                                   ctx.marginParam(6,2,6,2))

    }
}
