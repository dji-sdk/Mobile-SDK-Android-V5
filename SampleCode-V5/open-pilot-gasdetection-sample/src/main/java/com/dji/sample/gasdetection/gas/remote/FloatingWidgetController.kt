package com.dji.sample.gasdetection.gas.remote

import android.content.Context
import android.util.Log
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.EllipsizeType
import dji.open.pilot.base.ui.model.GravityType
import dji.open.pilot.base.ui.model.FontStyleType
import dji.open.pilot.base.ui.model.OperationResult
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ShapeDescriptor
import dji.open.pilot.base.ui.model.VisibilityType
import dji.open.pilot.base.ui.model.WidgetLayoutParam
import dji.v5.ux.remoteview.RemoteButton
import dji.v5.ux.remoteview.RemoteLinearLayout
import dji.v5.ux.remoteview.RemoteText
import dji.v5.ux.remoteview.RemoteViewHelper
import dji.v5.ux.remoteview.RemoteViewProvider
import dji.v5.ux.util.RemoteViewColors
import com.dji.sample.gasdetection.R
import com.dji.sample.gasdetection.gas.alert.AlertSeverity

/** 浮窗单个通道行。 */
data class FloatingChannelLine(
    val name: String,
    val value: String,
    val unit: String,
)

/** 任务链节点状态；配色与徽章样式按该状态推导。 */
enum class ChainStatus { DONE, CURRENT, FAILED, PENDING }

/**
 * 任务链单个节点。
 *
 * 设计意图：与 [FloatingViewModel] 一致，节点名与副文案由调用方按当前语言生成，
 * 渲染层只负责按 [status] 着色、加粗与显隐。
 */
data class FloatingChainNode(
    /** 节点名（如「任务接收」）。 */
    val label: String,
    val status: ChainStatus,
    /** 焦点节点的状态短语；`null` 时不占副文案高度。 */
    val sub: String? = null,
)

/**
 * 摘要态要展示的一次快照。
 *
 * 设计意图：渲染层不读业务状态，所有文案（阶段、时效、通道）由调用方按当前语言生成，
 * 便于入口页与浮窗共用同一套判定。
 */
data class FloatingViewModel(
    val title: String,
    val stage: String,
    /** 最高优先级告警摘要；无活动告警时为「暂无异常」。 */
    val alert: String,
    /** 告警等级；`null` 表示无活动告警，使用弱化色。 */
    val alertSeverity: AlertSeverity?,
    val channels: List<FloatingChannelLine>,
    val freshness: String,
    val offline: Boolean,
    /** 是否处于自动飞行阶段；仅此时展示「退出自动」按钮。 */
    val autoActive: Boolean = false,
    /** 船舶识别状态行；`null` 时不占位。 */
    val vesselStatus: String? = null,
    /** 任务链节点；空列表时不显示任务链（仅理论兜底，摘要态正常都会提供 5 个节点）。 */
    val chain: List<FloatingChainNode> = emptyList(),
    /** 是否就绪可启动自动飞行（阶段为可启动且预检全部通过）。 */
    val autoStartReady: Boolean = false,
    /** 是否处于搜索锁定态、可确认船舶信息。 */
    val vesselConfirmable: Boolean = false,
    /** 当前锁定的船舶名称或编号；确认框展示用。 */
    val vesselLabel: String? = null,
    /** 是否可开始搜索（已到达目标）。 */
    val vesselSearchable: Boolean = false,
    /** 是否可重试识别（识别降级）。 */
    val vesselRetryable: Boolean = false,
    /** 是否可结束任务（与 App 任务页同一阶段集合）。 */
    val finishable: Boolean = false,
)

/**
 * 待接收态的展示快照。
 *
 * 设计意图：与 [FloatingViewModel] 一致，渲染层不读业务状态；位置、标题与截止时间文案
 * 全部由调用方按当前语言生成，浮窗只负责排布与显隐。
 */
data class FloatingJobAcceptModel(
    /** 当前待接收任务的序号（1 起）。 */
    val position: Int,
    /** 待接收任务总数；决定「上一个/下一个」切换行是否显示。 */
    val total: Int,
    /** 任务阶段文案（如「任务接收」）。 */
    val stage: String,
    /** 任务标题，单行省略展示。 */
    val title: String,
    /** 截止时间整行文案（含「截止：」前缀或「未提供」）。 */
    val deadlineLine: String,
)

/**
 * Pilot 飞行界面的检测任务浮窗。
 *
 * 设计意图：浮窗在 Pilot 连接后常驻，分三种显示态——
 * - [FloatingMode.ENTRY]：没有进行中的检测任务，展示提示与「打开气体检测 APP」入口；
 * - [FloatingMode.JOB_ACCEPT]：当前任务待接收，按 patrol 的信息结构展示阶段/任务/截止时间、
 *   多单切换、接受/拒绝与查看详情；
 * - [FloatingMode.SUMMARY]：有已接受的进行中任务，展示收起条与可展开的读数摘要。
 *
 * 状态切换用 `clearLayout()` 重建整页，同态内只更新属性（避免每次刷新都重建）；摘要快照
 * 缓存在 [latestModel]，展开动作立即渲染最近一次快照，不依赖下一次数据流发射。
 * 所有 RemoteView 操作失败通过 [verifyMounted] 与 [degrade] 显式记录，不静默降级。
 */
class FloatingWidgetController(
    private val context: Context,
    private val onOpenDetail: () -> Unit,
    private val onExitAuto: () -> Unit = {},
    private val onAcceptJob: () -> Unit = {},
    private val onRejectJob: () -> Unit = {},
    /** 切换待接收任务；参数为相对偏移（-1 上一个、+1 下一个）。 */
    private val onSwitchPending: (Int) -> Unit = {},
    /** 浮窗内点击「启动自动飞行」；确认框与启动由调用方处理。 */
    private val onStartAuto: () -> Unit = {},
    /** 浮窗内点击「确认船舶信息」；确认框与写入由调用方处理。 */
    private val onConfirmVessel: () -> Unit = {},
    /** 浮窗内点击「开始搜索」；门控与提示由调用方处理。 */
    private val onStartSearch: () -> Unit = {},
    /** 浮窗内点击「重试识别」；重试由调用方处理。 */
    private val onRetrySearch: () -> Unit = {},
    /** 浮窗内点击「结束任务」；确认与收尾由调用方处理。 */
    private val onFinish: () -> Unit = {},
) {

    /** 浮窗当前显示态；`NONE` 表示尚未创建或已释放。 */
    private enum class FloatingMode { NONE, ENTRY, JOB_ACCEPT, SUMMARY }

    private val helper = RemoteViewHelper.floatingBoxHelper
    private var mode = FloatingMode.NONE
    private var failed = false

    /** 最近一次摘要快照；展开时直接用它渲染。 */
    private var latestModel: FloatingViewModel? = null

    // ---- 入口态控件 ----
    private var entryRoot: RemoteLinearLayout? = null
    private var entryNotice: RemoteText? = null

    // ---- 摘要态控件 ----
    private var pageRoot: RemoteLinearLayout? = null
    private var summaryContent: RemoteLinearLayout? = null
    private var titleText: RemoteText? = null
    private var vesselText: RemoteText? = null
    private var alertText: RemoteText? = null
    private var freshnessText: RemoteText? = null
    private val channelLines = mutableListOf<ChannelRow>()
    private var channelGroup: RemoteLinearLayout? = null
    private var startAutoButton: RemoteButton? = null
    private var vesselSearchButton: RemoteButton? = null
    private var vesselRetryButton: RemoteButton? = null
    private var vesselConfirmButton: RemoteButton? = null
    private var finishButton: RemoteButton? = null
    private var detailButton: RemoteButton? = null
    private var exitAutoButton: RemoteButton? = null

    // ---- 任务链控件 ----
    private var chainBlock: RemoteLinearLayout? = null
    private var chainToggle: RemoteButton? = null
    private val chainRows = mutableListOf<RemoteLinearLayout>()
    private val chainBadges = mutableListOf<RemoteText>()
    private val chainTitles = mutableListOf<RemoteText>()
    private val chainSubs = mutableListOf<RemoteText>()
    private val chainRails = mutableListOf<RemoteLinearLayout>()

    /** 任务链是否展开全部节点；默认折叠只显示焦点节点。 */
    private var chainExpanded = false

    /** 链上一次渲染的签名，用于跳过无变化的 IPC 同步。 */
    private var chainSignature: String? = null

    /** 已下发的浮窗页面高度；`WRAP_CONTENT` 表示自适应。 */
    private var pageHeight = RemoteView.WRAP_CONTENT

    // ---- 待接收态控件 ----
    private var jobRoot: RemoteLinearLayout? = null
    private var jobPositionText: RemoteText? = null
    private var jobStageText: RemoteText? = null
    private var jobTitleText: RemoteText? = null
    private var jobDeadlineText: RemoteText? = null
    private var jobNoticeText: RemoteText? = null
    private var jobSwitchRow: RemoteLinearLayout? = null
    private var jobAcceptButton: RemoteButton? = null
    private var jobRejectButton: RemoteButton? = null
    private var jobDetailButton: RemoteButton? = null

    private data class ChannelRow(val text: RemoteText)

    /** 是否已经因为创建失败进入降级状态。 */
    val isDegraded: Boolean get() = failed

    /**
     * 渲染入口态：无进行中任务时展示提示与「打开气体检测 APP」。
     *
     * @param notice 临时提示（如导航失败）；`null` 时隐藏提示行。
     */
    fun renderEntry(notice: String? = null) {
        if (failed) return
        if (mode != FloatingMode.ENTRY && !ensureEntryLayout()) return
        restoreRootVisibility()
        applyEntryNotice(notice)
    }

    /** 渲染摘要态；内容直接可见，没有摘要级折叠。 */
    fun renderSummary(model: FloatingViewModel) {
        if (failed) return
        latestModel = model
        if (mode != FloatingMode.SUMMARY && !ensureSummaryLayout()) return
        restoreRootVisibility()
        applySummary(model)
    }

    /**
     * 渲染待接收态；结构与 patrol 的工单列表态一致。
     *
     * @param notice 操作失败等临时提示；`null` 时隐藏提示行。
     */
    fun renderJobAccept(model: FloatingJobAcceptModel, notice: String? = null) {
        if (failed) return
        if (mode != FloatingMode.JOB_ACCEPT && !ensureJobAcceptLayout()) return
        restoreRootVisibility()
        applyJobAccept(model, notice)
    }

    /** 隐藏浮窗（Pilot 断开时），不清空布局，重连后可直接恢复可见。 */
    fun hide() {
        val root = currentRoot() ?: return
        root.setVisibility(VisibilityType.GONE)
        root.syncToRemote()
        // 隐藏时收回链的展开态，避免重连后仍停留在固定页面高度。
        chainExpanded = false
        applyPageHeight(RemoteView.WRAP_CONTENT)
    }

    /** 释放页面，宿主断开时调用；同时复位降级标记，下一次连接允许重建。 */
    fun release() {
        if (mode == FloatingMode.NONE && !failed) return
        runCatching { helper.clearLayout() }
        resetReferences()
        mode = FloatingMode.NONE
        failed = false
        chainExpanded = false
        chainSignature = null
        pageHeight = RemoteView.WRAP_CONTENT
        latestModel = null
    }

    private fun ensureEntryLayout(): Boolean = try {
        helper.clearLayout()
        helper.setPageLayoutParamWrapContent()
        resetReferences()

        val root = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        root.addToRoot(helper.margin(top = TOP_MARGIN, left = SIDE_MARGIN, right = SIDE_MARGIN))

        // 入口卡片不再绘制底色，直接叠在 Pilot 浮窗自身背景上，避免与宿主半透明底叠加后透出页面内容。
        val card = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setPadding(CARD_PADDING, CARD_PADDING, CARD_PADDING, CARD_PADDING)
        }
        root.addToRemote(card, helper.margin(CARD_MARGIN))

        val hint = helper.createText().apply {
            setText(context.getString(R.string.gas_floating_entry_hint))
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setTextSize(11f)
            setTextColor(RemoteViewColors.WHITE_60)
            setMaxLines(2)
        }
        card.addToRemote(hint, helper.margin(bottom = TEXT_GAP))
        hint.syncToRemote()

        // 导航失败等临时提示：默认不占位，失败时以红字显示。
        val notice = helper.createText().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setTextSize(11f)
            setTextColor(RemoteViewColors.RED_PRIMARY)
            setVisibility(VisibilityType.GONE)
        }
        card.addToRemote(notice, helper.margin(bottom = TEXT_GAP))
        notice.syncToRemote()

        val openButton = helper.createButton().apply {
            setText(context.getString(R.string.gas_floating_entry_open_app))
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(ENTRY_BUTTON_HEIGHT)
            setTextSize(11f)
            setFontStyle(FontStyleType.BOLD)
            setTextColor(RemoteViewColors.WHITE)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(RemoteViewColors.BLUE_PRIMARY)
                    .cornerRadius(BUTTON_RADIUS)
                    .build()
            )
            // 浮窗回调运行在 Binder 线程，跳转由调用方切主线程处理。
            setOnClickListener { onOpenDetail() }
        }
        card.addToRemote(openButton)
        openButton.syncToRemote()

        entryRoot = root
        entryNotice = notice
        mode = FloatingMode.ENTRY

        when {
            !verifyMounted(root, VisibilityType.VISIBLE) -> {
                degrade("入口容器挂载后不可见")
                false
            }

            !verifyMounted(card, VisibilityType.VISIBLE) -> {
                degrade("入口卡片挂载后不可见")
                false
            }

            else -> {
                Log.d(TAG, "floating mode -> ENTRY")
                true
            }
        }
    } catch (error: Exception) {
        degrade(error.message ?: "入口态创建异常")
        false
    }

    private fun ensureSummaryLayout(): Boolean = try {
        helper.clearLayout()
        helper.setPageLayoutParamWrapContent()
        resetReferences()

        val root = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        root.addToRoot(helper.margin(top = TOP_MARGIN, left = SIDE_MARGIN, right = SIDE_MARGIN))

        // 摘要内容直接可见：没有摘要级折叠条，进入摘要态即展示全部内容。
        val content = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setPadding(6, 6, 6, 6)
        }
        root.addToRemote(content, helper.margin(0))

        // 标题行：只有任务标题；摘要不再提供收起入口。
        val titleRow = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setGravity(GravityType.CENTER_VERTICAL)
        }
        content.addToRemote(titleRow, helper.margin(2))

        val title = helper.createText().apply {
            setWidth(0)
            setTextSize(13f)
            setTextColor(RemoteViewColors.WHITE)
            setSingleLine(true)
            setEllipsize(EllipsizeType.END)
        }
        titleRow.addToRemote(title, WidgetLayoutParam().apply { setWeight(1f) })

        // 任务链：默认折叠只显示焦点节点；链下方紧接实时检测数据。
        addChainBlock(content)

        // 实时检测数据：任务配置的前 3 路通道，随每次测量刷新；链展开时继续显示。
        val channels = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        content.addToRemote(channels, helper.margin(top = 2))
        repeat(MAX_CHANNELS) {
            val line = helper.createText().apply {
                setWidth(RemoteView.MATCH_PARENT)
                setTextSize(11f)
                setTextColor(RemoteViewColors.WHITE)
                setSingleLine(true)
                setVisibility(VisibilityType.GONE)
            }
            channels.addToRemote(line, helper.margin(1))
            channelLines.add(ChannelRow(line))
        }
        channelGroup = channels

        // 船舶识别状态：只占一行，未搜索时 GONE 不占高度。
        val vessel = helper.createText().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setTextSize(10f)
            setTextColor(RemoteViewColors.WHITE_60)
            setSingleLine(true)
            setEllipsize(EllipsizeType.END)
            setVisibility(VisibilityType.GONE)
        }
        content.addToRemote(vessel, helper.margin(2))

        // 开始搜索：到达目标后出现；与重试/确认按钮互斥。
        val vesselSearch = helper.createButton().apply {
            setText(context.getString(R.string.gas_vessel_search_start))
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(30)
            setTextSize(11f)
            setTextColor(RemoteViewColors.WHITE)
            setFontStyle(FontStyleType.BOLD)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(RemoteViewColors.BLUE_PRIMARY)
                    .cornerRadius(BUTTON_RADIUS)
                    .build()
            )
            setVisibility(VisibilityType.GONE)
            setOnClickListener { onStartSearch() }
        }
        content.addToRemote(vesselSearch, helper.margin(top = 4, left = 4, right = 4))
        vesselSearchButton = vesselSearch

        // 重试识别：识别降级后出现。
        val vesselRetry = helper.createButton().apply {
            setText(context.getString(R.string.gas_vessel_retry))
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(30)
            setTextSize(11f)
            setTextColor(RemoteViewColors.WHITE)
            setFontStyle(FontStyleType.BOLD)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(RemoteViewColors.BLUE_PRIMARY)
                    .cornerRadius(BUTTON_RADIUS)
                    .build()
            )
            setVisibility(VisibilityType.GONE)
            setOnClickListener { onRetrySearch() }
        }
        content.addToRemote(vesselRetry, helper.margin(top = 4, left = 4, right = 4))
        vesselRetryButton = vesselRetry

        // 船舶确认：仅在搜索阶段锁定目标时出现，复用 App 任务页的确认语义。
        val vesselConfirm = helper.createButton().apply {
            setText(context.getString(R.string.gas_vessel_confirm))
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(30)
            setTextSize(11f)
            setTextColor(RemoteViewColors.WHITE)
            setFontStyle(FontStyleType.BOLD)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(RemoteViewColors.BLUE_PRIMARY)
                    .cornerRadius(BUTTON_RADIUS)
                    .build()
            )
            setVisibility(VisibilityType.GONE)
            // 浮窗回调运行在 Binder 线程，确认与写入由调用方切主线程处理。
            setOnClickListener { onConfirmVessel() }
        }
        content.addToRemote(vesselConfirm, helper.margin(top = 4, left = 4, right = 4))
        vesselConfirmButton = vesselConfirm

        // 告警摘要：错误级红色、提醒级橙色、无告警弱化；单行省略以控制 140dp 宽度预算。
        val alert = helper.createText().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setTextSize(10f)
            setTextColor(RemoteViewColors.WHITE_60)
            setSingleLine(true)
            setEllipsize(EllipsizeType.END)
        }
        content.addToRemote(alert, helper.margin(2))

        // 载荷状态：在线显示「载荷在线」，离线显示「载荷离线」。
        val freshness = helper.createText().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setTextSize(9f)
            setTextColor(RemoteViewColors.WHITE_60)
            setSingleLine(true)
        }
        content.addToRemote(freshness, helper.margin(2))

        // 启动自动飞行：仅就绪时可见，放在「查看详情」上方。
        val startAuto = helper.createButton().apply {
            setText(context.getString(R.string.gas_auto_start))
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(30)
            setTextSize(11f)
            setTextColor(RemoteViewColors.WHITE)
            setFontStyle(FontStyleType.BOLD)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(RemoteViewColors.BLUE_PRIMARY)
                    .cornerRadius(BUTTON_RADIUS)
                    .build()
            )
            setVisibility(VisibilityType.GONE)
            // 浮窗回调运行在 Binder 线程，确认与启动由调用方切主线程处理。
            setOnClickListener { onStartAuto() }
        }
        content.addToRemote(startAuto, helper.margin(top = 4, left = 4, right = 4))
        startAutoButton = startAuto

        val detail = helper.createButton().apply {
            setText(context.getString(R.string.gas_floating_detail))
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(30)
            setTextSize(11f)
            setTextColor(RemoteViewColors.WHITE)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(RemoteViewColors.BLUE_PRIMARY)
                    .cornerRadius(BUTTON_RADIUS)
                    .build()
            )
            // 浮窗回调运行在 Binder 线程，交出动作由调用方切主线程处理。
            setOnClickListener { onOpenDetail() }
        }
        content.addToRemote(detail, helper.margin(4))

        // 结束任务：可结束阶段显示；确认后生成记录并进入收尾。
        val finish = helper.createButton().apply {
            setText(context.getString(R.string.gas_task_finish))
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(30)
            setTextSize(11f)
            setTextColor(RemoteViewColors.WHITE)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(RemoteViewColors.BLUE_PRIMARY)
                    .cornerRadius(BUTTON_RADIUS)
                    .build()
            )
            setVisibility(VisibilityType.GONE)
            setOnClickListener { onFinish() }
        }
        content.addToRemote(finish, helper.margin(4))
        finishButton = finish

        // 自动阶段的一键接管入口：全宽单按钮，4 字文案在 116–128dp 预算内。
        val exitAuto = helper.createButton().apply {
            setText(context.getString(R.string.gas_auto_exit))
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(30)
            setTextSize(11f)
            setTextColor(RemoteViewColors.WHITE)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(RemoteViewColors.RED_PRIMARY)
                    .cornerRadius(BUTTON_RADIUS)
                    .build()
            )
            setVisibility(VisibilityType.GONE)
            // 浮窗回调运行在 Binder 线程，交出动作由调用方切主线程处理。
            setOnClickListener { onExitAuto() }
        }
        content.addToRemote(exitAuto, helper.margin(4))

        pageRoot = root
        summaryContent = content
        titleText = title
        vesselText = vessel
        alertText = alert
        freshnessText = freshness
        detailButton = detail
        exitAutoButton = exitAuto
        mode = FloatingMode.SUMMARY
        chainExpanded = false
        chainSignature = null
        pageHeight = RemoteView.WRAP_CONTENT

        when {
            !verifyMounted(root, VisibilityType.VISIBLE) -> {
                degrade("根容器挂载后不可见")
                false
            }

            !verifyMounted(content, VisibilityType.VISIBLE) -> {
                degrade("摘要内容挂载后不可见")
                false
            }

            // 任务链默认 GONE，挂载探测同样不改变可见状态。
            !(chainBlock?.let { verifyMounted(it, VisibilityType.GONE) } == true) -> {
                degrade("任务链挂载后不可见")
                false
            }

            else -> {
                Log.d(TAG, "floating mode -> SUMMARY")
                true
            }
        }
    } catch (error: Exception) {
        degrade(error.message ?: "浮窗创建异常")
        false
    }

    /**
     * 建树任务链：卡片容器内 5 个纵向节点行，每行「左侧序号徽章 + 连接线段 / 右侧双行文案」。
     *
     * 设计意图：与 patrol / flighttask 的浮窗任务链同构；节点只在状态/显隐变化时更新，
     * 不重建视图树。默认折叠（只显示焦点节点），展开时显示全部节点与连接线。
     * 所有容器都先挂载到父级再添加子 View（RemoteView 的 parentId 查表契约）。
     */
    private fun addChainBlock(parent: RemoteLinearLayout) {
        val block = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setPadding(CARD_PADDING, CARD_PADDING, CARD_PADDING, CARD_PADDING)
            setBackgroundShape(cardShape(RemoteViewColors.WHITE_20))
            setVisibility(VisibilityType.GONE)
        }
        parent.addToRemote(block, helper.margin(2))
        chainBlock = block

        // 展开/收起箭头：整行可点，视觉取弱提示档，不与正文和操作按钮争抢注意力。
        val toggle = helper.createButton().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(CHAIN_TOGGLE_HEIGHT)
            setTextSize(TEXT_TINY)
            setTextColor(RemoteViewColors.WHITE_60)
            setText(CHEVRON_COLLAPSED)
            setOnClickListener { toggleChainExpanded() }
        }
        block.addToRemote(toggle, helper.margin(bottom = 2))
        chainToggle = toggle

        repeat(CHAIN_NODE_COUNT) { index ->
            // 整行容器：横向「gutter + 文案列」，顶部对齐；整行点击同样切换展开。
            val row = helper.createLinearLayoutHorizontal().apply {
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(RemoteView.WRAP_CONTENT)
                setGravity(GravityType.TOP)
                setOnClickListener { toggleChainExpanded() }
            }
            block.addToRemote(row)
            chainRows.add(row)

            val gutter = helper.createLinearLayoutVertical().apply {
                setWidth(CHAIN_BADGE_SIZE)
                setHeight(RemoteView.WRAP_CONTENT)
            }
            row.addToRemote(gutter)

            val badge = helper.createText().apply {
                setWidth(CHAIN_BADGE_SIZE)
                setHeight(CHAIN_BADGE_SIZE)
                setGravity(GravityType.CENTER)
                setTextSize(CHAIN_BADGE_TEXT_SIZE)
                setText((index + 1).toString())
                setBackgroundShape(dotShape(RemoteViewColors.WHITE_33))
            }
            gutter.addToRemote(badge)
            chainBadges.add(badge)

            if (index < CHAIN_NODE_COUNT - 1) {
                val railMargin = (CHAIN_BADGE_SIZE - CHAIN_RAIL_WIDTH) / 2
                val rail = helper.createLinearLayoutVertical().apply {
                    setWidth(CHAIN_RAIL_WIDTH)
                    setHeight(CHAIN_RAIL_HEIGHT)
                    setBackgroundShape(cardShape(RemoteViewColors.WHITE_33))
                }
                gutter.addToRemote(rail, helper.margin(top = 2, left = railMargin))
                chainRails.add(rail)
            }

            val textCol = helper.createLinearLayoutVertical().apply {
                setWidth(0)
                setHeight(RemoteView.WRAP_CONTENT)
            }
            row.addToRemote(
                textCol,
                WidgetLayoutParam().apply {
                    weight = 1f
                    marginLeft = CHAIN_TEXT_GAP
                },
            )

            val title = helper.createText().apply {
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(RemoteView.WRAP_CONTENT)
                setTextSize(TEXT_BODY)
                setTextColor(RemoteViewColors.WHITE_60)
                setSingleLine(true)
                setEllipsize(EllipsizeType.END)
            }
            textCol.addToRemote(title)
            chainTitles.add(title)

            val sub = helper.createText().apply {
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(RemoteView.WRAP_CONTENT)
                setTextSize(TEXT_TINY)
                setTextColor(RemoteViewColors.WHITE_60)
                setSingleLine(true)
                setEllipsize(EllipsizeType.END)
                setVisibility(VisibilityType.GONE)
            }
            textCol.addToRemote(sub, helper.margin(top = 2))
            chainSubs.add(sub)
        }
    }

    /**
     * 建树待接收态：面板 + 位置行 + 信息卡 + 失败提示 + 切换行 + 接受/拒绝 + 查看详情。
     *
     * 设计意图：与 patrol 的工单列表态同构，让两类任务的待接收界面可互相参照。所有容器都先
     * 挂载到父级再添加子 View（RemoteView 的 parentId 查表契约），建树后逐个探测挂载结果。
     */
    private fun ensureJobAcceptLayout(): Boolean = try {
        helper.clearLayout()
        helper.setPageLayoutParamWrapContent()
        resetReferences()

        // 根容器不再绘制深色面板底：浅色信息卡与按钮直接叠在飞行画面上。
        val root = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setPadding(JOB_PANEL_PADDING, JOB_PANEL_PADDING, JOB_PANEL_PADDING, JOB_PANEL_PADDING)
        }
        root.addToRoot(helper.margin(top = TOP_MARGIN, left = SIDE_MARGIN, right = SIDE_MARGIN))

        // 位置行：只表达「第几个 / 共几个」，弱化小字。
        val position = helper.createText().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setTextSize(TEXT_TINY)
            setTextColor(RemoteViewColors.WHITE_60)
            setGravity(GravityType.CENTER)
            setSingleLine(true)
        }
        root.addToRemote(position, helper.margin(top = 2, bottom = 2))

        // 信息卡：标准卡片材质（WHITE_20 / 4dp padding / 4dp 圆角）。
        val card = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setPadding(CARD_PADDING, CARD_PADDING, CARD_PADDING, CARD_PADDING)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(RemoteViewColors.WHITE_20)
                    .cornerRadius(JOB_RADIUS)
                    .build()
            )
        }
        root.addToRemote(card, helper.margin(CARD_MARGIN))

        val stage = addJobLabeledRow(card, R.string.gas_floating_job_label_stage)
        val title = addJobLabeledRow(card, R.string.gas_floating_job_label_task, bold = true)

        val deadline = helper.createText().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setTextSize(TEXT_TINY)
            setTextColor(RemoteViewColors.WHITE_60)
            setSingleLine(true)
            setEllipsize(EllipsizeType.END)
        }
        card.addToRemote(deadline, helper.margin(top = 2, bottom = 2, left = 4, right = 4))

        // 失败提示行：默认不占位，失败时以红字显示在操作行上方。
        val notice = helper.createText().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setTextSize(TEXT_BODY)
            setTextColor(RemoteViewColors.RED_PRIMARY)
            setMaxLines(2)
            setVisibility(VisibilityType.GONE)
        }
        root.addToRemote(notice, helper.margin(top = 2, bottom = 2, left = 4, right = 4))

        // 多单切换行：只有多个待接收任务时显示，两列等宽。
        val switchRow = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setVisibility(VisibilityType.GONE)
        }
        root.addToRemote(switchRow, helper.margin(top = 4, left = 4, right = 4))
        switchRow.addToRemote(
            createJobButton(R.string.gas_floating_job_previous, filled = false) { onSwitchPending(-1) },
            WidgetLayoutParam().apply { weight = 1f; marginRight = 2 },
        )
        switchRow.addToRemote(
            createJobButton(R.string.gas_floating_job_next, filled = false) { onSwitchPending(1) },
            WidgetLayoutParam().apply { weight = 1f; marginLeft = 2 },
        )

        // 接受 / 拒绝：主操作填充、次操作描边。
        val actionRow = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        root.addToRemote(actionRow, helper.margin(top = 4, left = 4, right = 4))
        val accept = createJobButton(R.string.gas_task_accept, filled = true) {
            disableJobActions()
            onAcceptJob()
        }
        actionRow.addToRemote(
            accept,
            WidgetLayoutParam().apply { weight = 1f; marginRight = 2 },
        )
        val reject = createJobButton(R.string.gas_task_reject, filled = false) {
            disableJobActions()
            onRejectJob()
        }
        actionRow.addToRemote(
            reject,
            WidgetLayoutParam().apply { weight = 1f; marginLeft = 2 },
        )

        val detail = createJobButton(
            R.string.gas_floating_detail,
            filled = false,
            width = RemoteView.MATCH_PARENT,
        ) { onOpenDetail() }
        root.addToRemote(detail, helper.margin(top = 4, left = 4, right = 4))

        jobRoot = root
        jobPositionText = position
        jobStageText = stage
        jobTitleText = title
        jobDeadlineText = deadline
        jobNoticeText = notice
        jobSwitchRow = switchRow
        jobAcceptButton = accept
        jobRejectButton = reject
        jobDetailButton = detail
        mode = FloatingMode.JOB_ACCEPT

        when {
            !verifyMounted(root, VisibilityType.VISIBLE) -> {
                degrade("待接收容器挂载后不可见")
                false
            }

            !verifyMounted(card, VisibilityType.VISIBLE) -> {
                degrade("待接收信息卡挂载后不可见")
                false
            }

            !verifyMounted(actionRow, VisibilityType.VISIBLE) -> {
                degrade("待接收操作行挂载后不可见")
                false
            }

            else -> {
                Log.d(TAG, "floating mode -> JOB_ACCEPT")
                true
            }
        }
    } catch (error: Exception) {
        degrade(error.message ?: "待接收态创建异常")
        false
    }

    /** 建树一行「标签 + 取值」；行容器先挂载到卡片，再填子 View。返回取值控件。 */
    private fun addJobLabeledRow(
        parent: RemoteLinearLayout,
        labelRes: Int,
        bold: Boolean = false,
    ): RemoteText {
        val row = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        parent.addToRemote(row, helper.margin(top = 2, bottom = 2, left = 2, right = 2))
        val label = helper.createText().apply {
            setWidth(JOB_LABEL_WIDTH)
            setHeight(RemoteView.WRAP_CONTENT)
            setTextSize(TEXT_TINY)
            setTextColor(RemoteViewColors.WHITE_60)
            setSingleLine(true)
            // 与浮窗其余文案一致：直接取资源文案，不依赖语言工具的全局初始化。
            setText(context.getString(labelRes))
        }
        row.addToRemote(label)
        val value = helper.createText().apply {
            setWidth(0)
            setHeight(RemoteView.WRAP_CONTENT)
            setTextSize(TEXT_BODY)
            setTextColor(RemoteViewColors.WHITE)
            setSingleLine(true)
            setEllipsize(EllipsizeType.END)
            if (bold) setFontStyle(FontStyleType.BOLD)
        }
        row.addToRemote(value, WidgetLayoutParam().apply { weight = 1f })
        return value
    }

    /** 待接收态按钮：填充为主操作，描边为次操作；点击回调交给调用方切主线程。 */
    private fun createJobButton(
        labelRes: Int,
        filled: Boolean,
        width: Int = 0,
        onClick: () -> Unit,
    ): RemoteButton = helper.createButton().apply {
        // 与浮窗其余文案一致：直接取资源文案，不依赖语言工具的全局初始化。
        setText(context.getString(labelRes))
        setWidth(width)
        setHeight(JOB_BUTTON_HEIGHT)
        setTextSize(TEXT_BODY)
        if (filled) {
            setTextColor(RemoteViewColors.WHITE)
            setFontStyle(FontStyleType.BOLD)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(RemoteViewColors.BLUE_PRIMARY)
                    .cornerRadius(JOB_RADIUS)
                    .build()
            )
        } else {
            setTextColor(RemoteViewColors.BLUE_PRIMARY)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .stroke(1, RemoteViewColors.BLUE_PRIMARY)
                    .cornerRadius(JOB_RADIUS)
                    .build()
            )
        }
        // 浮窗回调运行在 Binder 线程，业务动作由调用方切主线程处理。
        setOnClickListener { onClick() }
    }

    /** 点击接受/拒绝后立即置灰，防止连续点击重复提交；下一次渲染恢复。 */
    private fun disableJobActions() {
        listOf(jobAcceptButton, jobRejectButton).forEach { button ->
            button?.setEnabled(false)
            button?.setAlpha(0.6f)
            button?.syncToRemote()
        }
    }

    /** 写入待接收快照：位置、阶段、标题、截止时间、切换行与提示行显隐。 */
    private fun applyJobAccept(model: FloatingJobAcceptModel, notice: String?) {
        jobPositionText?.setText(
            context.getString(R.string.gas_floating_job_position, model.position, model.total)
        )
        jobPositionText?.syncToRemote()
        jobStageText?.setText(model.stage)
        jobStageText?.syncToRemote()
        jobTitleText?.setText(model.title)
        jobTitleText?.syncToRemote()
        jobDeadlineText?.setText(model.deadlineLine)
        jobDeadlineText?.syncToRemote()

        jobSwitchRow?.setVisibility(
            if (model.total > 1) VisibilityType.VISIBLE else VisibilityType.GONE
        )
        jobSwitchRow?.syncToRemote()

        if (notice == null) {
            jobNoticeText?.setVisibility(VisibilityType.GONE)
        } else {
            jobNoticeText?.setText(notice)
            jobNoticeText?.setVisibility(VisibilityType.VISIBLE)
        }
        jobNoticeText?.syncToRemote()

        // 上一次点击可能把按钮置灰；渲染新快照即代表上一次请求已有结果，恢复可点击。
        listOf(jobAcceptButton, jobRejectButton).forEach { button ->
            button?.setEnabled(true)
            button?.setAlpha(1f)
            button?.syncToRemote()
        }
    }

    /** 入口态提示行：有提示时显示红字，无提示时隐藏。 */
    private fun applyEntryNotice(notice: String?) {
        val view = entryNotice ?: return
        if (notice == null) {
            view.setVisibility(VisibilityType.GONE)
        } else {
            view.setText(notice)
            view.setVisibility(VisibilityType.VISIBLE)
        }
        view.syncToRemote()
    }

    /**
     * 恢复根容器可见性。
     *
     * `hide()` 会把根容器置为 GONE；重新渲染（重连或退出隐藏）时必须显式恢复，
     * 否则子视图再可见也整块不显示。
     */
    private fun restoreRootVisibility() {
        val root = currentRoot() ?: return
        root.setVisibility(VisibilityType.VISIBLE)
        root.syncToRemote()
    }

    private fun currentRoot(): RemoteLinearLayout? = when (mode) {
        FloatingMode.ENTRY -> entryRoot
        FloatingMode.JOB_ACCEPT -> jobRoot
        FloatingMode.SUMMARY -> pageRoot
        FloatingMode.NONE -> null
    }

    private fun applySummary(model: FloatingViewModel) {
        titleText?.setText(model.title)
        titleText?.syncToRemote()
        applyChain(model)
        val vessel = model.vesselStatus
        if (vessel == null) {
            vesselText?.setVisibility(VisibilityType.GONE)
        } else {
            vesselText?.setVisibility(VisibilityType.VISIBLE)
            vesselText?.setText(vessel)
        }
        vesselText?.syncToRemote()
        alertText?.setText(model.alert)
        alertText?.setTextColor(
            when (model.alertSeverity) {
                AlertSeverity.ERROR -> RemoteViewColors.RED_PRIMARY
                AlertSeverity.WARNING -> RemoteViewColors.ORANGE_PRIMARY
                null -> RemoteViewColors.WHITE_60
            }
        )
        alertText?.syncToRemote()
        freshnessText?.setText(model.freshness)
        freshnessText?.syncToRemote()
        exitAutoButton?.setVisibility(
            if (model.autoActive) VisibilityType.VISIBLE else VisibilityType.GONE
        )
        exitAutoButton?.syncToRemote()

        channelLines.forEachIndexed { index, row ->
            val line = model.channels.getOrNull(index)
            if (line == null) {
                row.text.setVisibility(VisibilityType.GONE)
            } else {
                row.text.setVisibility(VisibilityType.VISIBLE)
                row.text.setText("${line.name} ${line.value} ${line.unit}")
            }
            row.text.syncToRemote()
        }
        startAutoButton?.setVisibility(
            if (model.autoStartReady) VisibilityType.VISIBLE else VisibilityType.GONE
        )
        startAutoButton?.syncToRemote()
        vesselConfirmButton?.setVisibility(
            if (model.vesselConfirmable) VisibilityType.VISIBLE else VisibilityType.GONE
        )
        vesselConfirmButton?.syncToRemote()
        vesselSearchButton?.setVisibility(
            if (model.vesselSearchable) VisibilityType.VISIBLE else VisibilityType.GONE
        )
        vesselSearchButton?.syncToRemote()
        vesselRetryButton?.setVisibility(
            if (model.vesselRetryable) VisibilityType.VISIBLE else VisibilityType.GONE
        )
        vesselRetryButton?.syncToRemote()
        finishButton?.setVisibility(
            if (model.finishable) VisibilityType.VISIBLE else VisibilityType.GONE
        )
        finishButton?.syncToRemote()
        summaryContent?.syncToRemote()
    }

    /**
     * 刷新任务链：节点文案/配色、折叠态显隐与浮窗页面高度。
     *
     * 折叠态只显示焦点节点（当前或失败）与箭头；展开态显示全部节点与连接线。实时检测数据
     * 始终保留在链路下方，不随展开隐藏。序列未变化时跳过属性同步，避免高频 IPC。
     */
    private fun applyChain(model: FloatingViewModel) {
        val block = chainBlock ?: return
        val nodes = model.chain
        if (nodes.isEmpty()) {
            block.setVisibility(VisibilityType.GONE)
            block.syncToRemote()
            return
        }
        block.setVisibility(VisibilityType.VISIBLE)
        block.syncToRemote()

        val focus = nodes.indexOfFirst {
            it.status == ChainStatus.CURRENT || it.status == ChainStatus.FAILED
        }.let { if (it < 0) 0 else it }
        val expanded = chainExpanded
        val signature = buildString {
            nodes.forEach {
                append(it.label).append(':').append(it.status).append(':')
                    .append(it.sub.orEmpty()).append('|')
            }
            append(expanded).append('|').append(focus)
        }
        if (signature != chainSignature) {
            chainSignature = signature
            nodes.forEachIndexed { index, node ->
                val dot = dotColor(node.status)
                chainBadges.getOrNull(index)?.apply {
                    setBackgroundShape(dotShape(dot))
                    setTextColor(
                        if (node.status == ChainStatus.PENDING) {
                            RemoteViewColors.WHITE_60
                        } else {
                            RemoteViewColors.WHITE
                        }
                    )
                    syncToRemote()
                }
                chainTitles.getOrNull(index)?.apply {
                    setText(node.label)
                    setTextColor(labelColor(node.status))
                    setFontStyle(
                        if (node.status == ChainStatus.CURRENT || node.status == ChainStatus.FAILED) {
                            FontStyleType.BOLD
                        } else {
                            FontStyleType.NORMAL
                        }
                    )
                    syncToRemote()
                }
                chainSubs.getOrNull(index)?.apply {
                    if (node.sub == null) {
                        setVisibility(VisibilityType.GONE)
                    } else {
                        setText(node.sub)
                        setTextColor(labelColor(node.status))
                        setVisibility(VisibilityType.VISIBLE)
                    }
                    syncToRemote()
                }
                chainRails.getOrNull(index)?.apply {
                    setBackgroundShape(cardShape(dot))
                    syncToRemote()
                }
                chainRows.getOrNull(index)?.apply {
                    setVisibility(
                        if (expanded || index == focus) VisibilityType.VISIBLE else VisibilityType.GONE
                    )
                    syncToRemote()
                }
            }
            chainToggle?.apply {
                setText(if (expanded) CHEVRON_EXPANDED else CHEVRON_COLLAPSED)
                syncToRemote()
            }
            chainRails.forEach { rail ->
                rail.setVisibility(if (expanded) VisibilityType.VISIBLE else VisibilityType.GONE)
                rail.syncToRemote()
            }
        }

        // 实时数据不随链展开隐藏；只切换页面高度。
        applyPageHeight(if (expanded) CHAIN_EXPANDED_PAGE_HEIGHT else RemoteView.WRAP_CONTENT)
    }

    /** 切换任务链展开/收起；立即用缓存快照重绘，不等待下一次数据流发射。 */
    private fun toggleChainExpanded() {
        chainExpanded = !chainExpanded
        latestModel?.let { applyChain(it) }
    }

    /** 切换浮窗页面高度：折叠态自适应，展开态请求固定高度以完整容纳链条。 */
    private fun applyPageHeight(height: Int) {
        if (height == pageHeight) return
        pageHeight = height
        if (height == RemoteView.WRAP_CONTENT) {
            helper.setPageLayoutParamWrapContent()
        } else {
            helper.setPageLayoutParam(height)
        }
    }

    private fun cardShape(color: Int) = ShapeDescriptor.Builder()
        .fillColor(color)
        .cornerRadius(CHAIN_RADIUS)
        .build()

    private fun dotShape(color: Int) = ShapeDescriptor.Builder()
        .fillColor(color)
        .shape(ShapeDescriptor.SHAPE_OVAL)
        .build()

    private fun dotColor(status: ChainStatus): Int = when (status) {
        ChainStatus.DONE -> RemoteViewColors.GREEN_PRIMARY
        ChainStatus.CURRENT -> RemoteViewColors.BLUE_PRIMARY
        ChainStatus.FAILED -> RemoteViewColors.RED_PRIMARY
        ChainStatus.PENDING -> RemoteViewColors.WHITE_33
    }

    private fun labelColor(status: ChainStatus): Int = when (status) {
        ChainStatus.DONE -> RemoteViewColors.GREEN_PRIMARY
        ChainStatus.CURRENT -> RemoteViewColors.BLUE_PRIMARY
        ChainStatus.FAILED -> RemoteViewColors.RED_PRIMARY
        ChainStatus.PENDING -> RemoteViewColors.WHITE_60
    }

    /**
     * 用一次 `updateView` 探测视图是否真的挂载成功，并顺带把可见性设为 [visibility]。
     *
     * uxsdk 的 `addToRoot` / `addToRemote` 不返回 `OperationResult`，跨进程失败不会抛异常；
     * 这里写一次可见性属性，拿到 `NO_SUCH_VIEW_ID` / `NO_SUCH_PARENT_VIEW_ID` 即为失败。
     */
    private fun verifyMounted(view: RemoteViewProvider, visibility: VisibilityType): Boolean {
        val result = helper.updateView(
            view.getId(),
            RemoteViewProperty.VISIBILITY,
            visibility.value.toString(),
        )
        if (result != OperationResult.SUCCESS) {
            Log.e(TAG, "mount failed: result=$result viewId=${view.getId()}")
            return false
        }
        return true
    }

    private fun degrade(reason: String) {
        failed = true
        mode = FloatingMode.NONE
        Log.e(TAG, "floating widget degraded: $reason")
        runCatching { helper.clearLayout() }
        resetReferences()
    }

    private fun resetReferences() {
        pageRoot = null
        summaryContent = null
        startAutoButton = null
        vesselSearchButton = null
        vesselRetryButton = null
        vesselConfirmButton = null
        finishButton = null
        titleText = null
        vesselText = null
        alertText = null
        freshnessText = null
        channelLines.clear()
        channelGroup = null
        detailButton = null
        exitAutoButton = null
        entryRoot = null
        entryNotice = null
        jobRoot = null
        jobPositionText = null
        jobStageText = null
        jobTitleText = null
        jobDeadlineText = null
        jobNoticeText = null
        jobSwitchRow = null
        jobAcceptButton = null
        jobRejectButton = null
        jobDetailButton = null
        chainBlock = null
        chainToggle = null
        chainRows.clear()
        chainBadges.clear()
        chainTitles.clear()
        chainSubs.clear()
        chainRails.clear()
        chainExpanded = false
        chainSignature = null
    }

    private companion object {
        const val TAG = "GasFloating"
        const val MAX_CHANNELS = 3
        const val BUTTON_RADIUS = 6f
        const val ENTRY_BUTTON_HEIGHT = 32
        const val TOP_MARGIN = 4
        const val SIDE_MARGIN = 8
        const val CARD_MARGIN = 4
        const val CARD_PADDING = 4
        const val TEXT_GAP = 4

        // 待接收态：按 patrol 同构结构与 ui-spec 默认档位取值。
        /** 待接收面板内边距；比 patrol 的 8dp 小，用于抵消本浮窗已有的左右 8dp 外边距。 */
        const val JOB_PANEL_PADDING = 4

        /** 信息卡标签列宽（9sp 下约 3 个中文字）。 */
        const val JOB_LABEL_WIDTH = 36
        const val JOB_BUTTON_HEIGHT = 32
        const val JOB_RADIUS = 4f
        const val TEXT_BODY = 11f
        const val TEXT_TINY = 9f

        // 任务链：尺寸与配色对齐 patrol 的浮窗任务链。
        const val CHAIN_NODE_COUNT = 5
        const val CHAIN_BADGE_SIZE = 20
        const val CHAIN_BADGE_TEXT_SIZE = 8f
        const val CHAIN_RAIL_WIDTH = 2
        const val CHAIN_RAIL_HEIGHT = 6
        const val CHAIN_TEXT_GAP = 4
        const val CHAIN_TOGGLE_HEIGHT = 20
        const val CHAIN_RADIUS = 4f

        /** 展开链路时请求的浮窗页面高度（dp），与 patrol 取值一致。 */
        const val CHAIN_EXPANDED_PAGE_HEIGHT = 400
        const val CHEVRON_COLLAPSED = "▼"
        const val CHEVRON_EXPANDED = "▲"
    }
}
