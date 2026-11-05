package dji.sample.patrol.models

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import dji.open.pilot.base.manager.ui.config.DialogConfig
import dji.open.pilot.base.ui.NavigateAction
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.EllipsizeType
import dji.open.pilot.base.ui.model.FontStyleType
import dji.open.pilot.base.ui.model.GravityType
import dji.open.pilot.base.ui.model.OperationResult
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ShapeDescriptor
import dji.open.pilot.base.ui.model.VisibilityType
import dji.open.pilot.base.ui.model.WidgetLayoutParam
import dji.open.pilot.client.manager.ui.NavigateManager
import dji.open.pilot.client.manager.ui.RemoteDialogManager
import dji.sample.patrol.MainApplication
import dji.sample.patrol.R
import dji.sample.patrol.data.Asset
import dji.sample.patrol.data.AssetState
import dji.sample.patrol.data.CapturePoint
import dji.sample.patrol.data.FlowConfig
import dji.sample.patrol.data.FlyToStage
import dji.sample.patrol.data.Job
import dji.sample.patrol.data.JobStatus
import dji.sample.patrol.data.MediaReference
import dji.sample.patrol.data.PatrolStage
import dji.sample.patrol.data.RecognitionStage
import dji.sample.patrol.data.ResultCallback
import dji.sample.patrol.data.RouteSnapshot
import dji.sample.patrol.data.RouteState
import dji.sample.patrol.data.UploadProgress
import dji.sample.patrol.data.WaylineProgress
import dji.sample.patrol.data.resolvePatrolStage
import dji.sample.patrol.data.syncSummary
import dji.sample.patrol.manager.AssetUploadManager
import dji.sample.patrol.manager.CloudManager
import dji.sample.patrol.manager.MediaBoundaryFailure
import dji.sample.patrol.manager.MediaManager
import dji.sample.patrol.manager.PatrolFlowConfig
import dji.sample.patrol.manager.RouteController
import dji.sample.patrol.manager.TaskManager
import dji.sample.patrol.manager.WaylineFailure
import dji.sample.patrol.manager.WaylineManager
import dji.sample.patrol.manager.flowStageFor
import dji.sample.patrol.util.JobTimeFormatter
import dji.sample.patrol.util.routeProgressText
import dji.sdk.keyvalue.value.flightcontroller.GoHomeState
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.DJIWaypointMissionError
import dji.v5.common.error.IDJIError
import dji.v5.utils.common.FileUtils
import dji.v5.ux.remoteview.RemoteButton
import dji.v5.ux.remoteview.RemoteLinearLayout
import dji.v5.ux.remoteview.RemoteProgressBar
import dji.v5.ux.remoteview.RemoteText
import dji.v5.ux.remoteview.RemoteViewHelper
import dji.v5.ux.remoteview.RemoteViewProvider
import dji.v5.ux.util.RemoteViewColors
import java.io.File
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicBoolean

/** Manages task data and the Pilot floating task window. */
class TaskVM : ViewModel() {

    companion object {
        private const val TAG = "TaskVM"
        private const val EMPTY_TASK_ICON = "icon_task_empty_v2.webp"
        private const val SELECTION_PREFERENCES = "patrol-task-selection"

        // ── 面 B 样式 Token（取值依据 best-practices/open-pilot-remoteview-ui-spec.md）──
        // 浮窗宽度固定 140dp；下表尺寸与字号是高度/宽度预算的输入，改动前必须重算预算。
        private const val PANEL_CORNER_RADIUS = 4f
        private const val BUTTON_CORNER_RADIUS = 4f
        private const val BUTTON_HEIGHT = 32
        private const val SMALL_BUTTON_HEIGHT = 20
        private const val PANEL_PADDING = 8

        // ── 任务链样式（纵向时间线：左侧标记 gutter + 右侧双行文案）──
        /** 节点序号徽章直径；ui-spec 推荐圆形进度条取 20dp。 */
        private const val CHAIN_BADGE_SIZE = 20

        /** 徽章内序号字号；ui-spec 规定 20dp 圆环配 8sp。 */
        private const val CHAIN_BADGE_TEXT_SIZE = 8f

        /** 节点间连接线宽度与单段高度（连接线只画在本行标记下方）。 */
        private const val CHAIN_RAIL_WIDTH = 2
        private const val CHAIN_RAIL_HEIGHT = 6

        /** gutter 与右侧文案列的间距。 */
        private const val CHAIN_TEXT_GAP = 4

        /** 主文案与副文案之间的间距。 */
        private const val CHAIN_SUB_GAP = 2

        /** 链条卡片内边距；ui-spec §6.1 标准卡片 4dp。 */
        private const val CHAIN_CARD_PADDING = 4

        private const val TEXT_BODY = 11f
        private const val TEXT_TINY = 9f
        private const val GAP_TINY = 2
        private const val GAP_SMALL = 4

        /**
         * 展开「全流程」时请求的浮窗页面高度（dp）。
         *
         * 设计意图：展开时不裁剪任何区块，改为把整个浮窗页面变高，让内容完整呈现。
         * 注意 FLOATING_BOX 有可视上限（实测包络：约 330dp 完整可见、约 386dp 起底部被裁切，
         * 超出静默裁切不报错），因此该值必须与展开态的实际内容高度（最高约 389dp）一起复核，
         * 不可随意上调；360dp 已在 DJI RC Plus 3 上实测「展开后最底部按钮完整可见」，400dp 待复验。
         */
        private const val EXPANDED_PAGE_HEIGHT = 400

        /** 键值行的标签列宽（9sp 下约 4 个中文字），左侧标签 + 右侧取值。 */
        private const val LABEL_WIDTH = 36

        /**
         * 链条折叠/展开指示符：不写功能名，用方向箭头表达「还有内容可展开」。
         *
         * 视觉上取「弱提示」档（[TEXT_TINY] + [FontStyleType.NORMAL] + [TEXT_SECONDARY_COLOR]），
         * 只承担可发现性提示，不与面板正文和操作按钮争抢注意力；点击热区仍是整行宽度。
         */
        private const val CHEVRON_COLLAPSED = "▼"
        private const val CHEVRON_EXPANDED = "▲"

        /** 面 B 深色半透明面板底色：叠加在图传画面上不抢画面、仍与画面可区分。 */
        private val PANEL_COLOR = RemoteViewColors.BLACK_60

        /**
         * 面 B 卡片 / 分隔线的弱背景色（比面板底亮一档，形成层次）。
         *
         * 取 ui-spec §7.1 标准卡片底色 `WHITE_20`：在 `BLACK_60` 面板上呈半透明浅灰，
         * 与参考实现（windturbineinspectionplugin）的 `0x99595959` 同档，但遵守规范色板。
         */
        private val SURFACE_COLOR = RemoteViewColors.WHITE_20

        /** 面 B 主文字色（深色底）。 */
        private val TEXT_PRIMARY_COLOR = RemoteViewColors.WHITE

        /** 面 B 次级文字色（深色底）。 */
        private val TEXT_SECONDARY_COLOR = RemoteViewColors.WHITE_60

        /**
         * 任务链「未开始」节点与连接线的弱化色。
         *
         * 深色面板上必须仍可辨识：`WHITE_08`（7.8% 白）叠在 `BLACK_60` 面板上近乎不可见
         * （实机表现为「跟底色过于接近」），因此取 `WHITE_33`（33% 白）。本常量是未开始
         * 节点色的唯一取值来源。
         */
        private val PENDING_NODE_COLOR = RemoteViewColors.WHITE_33
    }

    enum class State { IDLE, JOB_LIST, PREPARING, EXECUTING, SYNCING, ERROR, STOPPED, DONE }

    /** 任务链节点，顺序即展示顺序，不可重排。 */
    private enum class ChainNode(val labelRes: Int) {
        JOB_ACCEPT(R.string.fw_chain_job_accept),
        FLIGHT_PREPARE(R.string.fw_chain_flight_prepare),
        WAYLINE_EXECUTION(R.string.fw_chain_wayline_execution),
        DATA_SYNC(R.string.fw_chain_data_sync),
        COMPLETE(R.string.fw_chain_complete),
    }

    /** 任务链节点状态。颜色语义：DONE=成功、CURRENT=主色、FAILED=错误、PENDING=未开始。 */
    private enum class NodeStatus { DONE, CURRENT, FAILED, PENDING }

    /** 失败来源，用于把失败定位到任务链的具体节点。 */
    private enum class FailureSource { NONE, PREPARE, WAYLINE, UPLOAD }

    /** 面 B 按钮三档样式：主操作 / 危险操作 / 次操作（描边）/ 文字按钮。 */
    private enum class ButtonStyle { FILLED, DANGER, OUTLINE, TEXT }

    val state = MutableLiveData(State.IDLE)
    val jobs = TaskManager.jobs
    val activeJob = TaskManager.activeJob
    val syncState = TaskManager.syncState
    val taskError = TaskManager.error
    val backendConnectionState = CloudManager.backendConnectionState

    private val mutableSelectedJobId = MutableLiveData<String?>(null)
    val selectedJobId: LiveData<String?> = mutableSelectedJobId

    private val helper = RemoteViewHelper.floatingBoxHelper
    private val mainHandler = Handler(Looper.getMainLooper())
    private val isLayoutInitialized = AtomicBoolean(false)
    private val isIconUploading = AtomicBoolean(false)
    private val containerIds = mutableMapOf<State, String>()
    private val stageContainerIds = mutableMapOf<String, String>()
    private val stageStatusTexts = mutableMapOf<String, RemoteText>()
    private val stageProgressBars = mutableMapOf<String, RemoteProgressBar>()
    private val stagePauseButtons = mutableMapOf<String, RemoteButton>()

    /** 已停止阶段的[返航]按钮：按 `canReturnHome()` 显隐。 */
    private val stageReturnHomeButtons = mutableMapOf<String, RemoteButton>()

    /** 准备就绪阶段的起飞/继续按钮：按空中状态切换文案。 */
    private val stageTakeOffButtons = mutableMapOf<String, RemoteButton>()

    /** 可注入的巡逻浮窗流程配置；为 `null` 时完整保留既有八状态流程。 */
    var flowConfig: FlowConfig? = null

    /** 可注入的识别阶段源；为 `null` 时不显示 `MATCHED`，完整保留既有流程。 */
    var recognitionStageSource: LiveData<RecognitionStage>? = null

    /** 可注入的识别失败原因源（`MATCHED` 失败待处置时展示）。 */
    var recognitionErrorSource: LiveData<String?>? = null

    /** 可注入的命中等待提示源（如「等待媒体同步完成」）。 */
    var recognitionNoticeSource: LiveData<String?>? = null

    /** 可注入的点位源；为 `null` 时点位计数为 0。 */
    var capturePointsSource: LiveData<List<CapturePoint>>? = null

    /** 可注入的 FlyTo 阶段源；为 `null` 时不显示 `DESTINATION`（回退既有流程）。 */
    var flyToStageSource: LiveData<FlyToStage>? = null

    private val fallbackCapturePoints = MutableLiveData<List<CapturePoint>>(emptyList())
    val capturePoints: LiveData<List<CapturePoint>>
        get() = capturePointsSource ?: fallbackCapturePoints

    private var latestRecognitionStage = RecognitionStage.IDLE
    private var latestMatchError: String? = null
    private var latestMatchNotice: String? = null
    private var latestCapturePoints: List<CapturePoint> = emptyList()
    private var latestFlyToStage = FlyToStage.IDLE

    private var observersRegistered = false
    private var selectionPreferences: SharedPreferences? = null
    private var selectionPreferenceKey: String? = null
    private var pilotConnected = false
    private var pendingJobId: String? = null
    private var activeJobId: String? = null

    /**
     * 「切换任务」建立起的浏览意图（design D1）。
     *
     * 与 [activeJobId] / [pendingJobId] 这类派生缓存分开：缓存每次刷新都会重算，意图只在用户
     * 接受/显式选中工单，或出现新的在途工单时才结束；成立期间刷新不得把已终态工单选回来。
     */
    private var browsingJobList = false
    private var executionStarting = false
    private var latestJobs: List<Job> = emptyList()
    private var latestStage = PatrolStage.IDLE
    private var latestRouteProgress = WaylineProgress()
    private var latestAssets: List<Asset> = emptyList()
    private var latestMediaReferences: List<MediaReference> = emptyList()
    private var latestQueue: List<Asset> = emptyList()
    private var latestUploadProgress: Map<String, UploadProgress> = emptyMap()
    private var errorMessage: String? = null

    private var jobTitleText: RemoteText? = null
    private var jobPositionText: RemoteText? = null
    private var jobDeadlineText: RemoteText? = null
    private var jobSwitchRowId: String? = null
    private var preparingJobText: RemoteText? = null
    private var preparingStatusText: RemoteText? = null
    private var executingStatusText: RemoteText? = null
    private var errorStageText: RemoteText? = null
    private var waypointText: RemoteText? = null
    private var capturedText: RemoteText? = null
    private var uploadedText: RemoteText? = null
    private var executingProgress: RemoteProgressBar? = null
    private var pauseResumeButton: RemoteButton? = null
    private var syncFileText: RemoteText? = null
    private var syncByteText: RemoteText? = null
    private var syncingProgress: RemoteProgressBar? = null
    private var syncDownloadText: RemoteText? = null
    private var errorCountText: RemoteText? = null
    private var errorTitleText: RemoteText? = null
    private var errorText: RemoteText? = null
    private var doneFileText: RemoteText? = null
    private var doneSizeText: RemoteText? = null
    private var doneTitleText: RemoteText? = null

    // ── 任务链视图（一次性建树，之后只更新属性与显隐）──
    private var chainBlock: RemoteLinearLayout? = null
    private var chainToggleButton: RemoteButton? = null
    /** 每个节点的整行容器（折叠态只显示焦点行）。 */
    private val chainNodeContainers = linkedMapOf<ChainNode, RemoteLinearLayout>()
    /** 节点序号徽章（圆形标记，内含 1–5 序号）。 */
    private val chainBadges = linkedMapOf<ChainNode, RemoteText>()
    /** 主文案：节点名。 */
    private val chainTitles = linkedMapOf<ChainNode, RemoteText>()
    /** 副文案：节点状态短语，无状态时为 GONE。 */
    private val chainSubs = linkedMapOf<ChainNode, RemoteText>()
    /** 本行标记下方的连接线段（末节点无）。 */
    private val chainRails = linkedMapOf<ChainNode, RemoteLinearLayout>()

    /** 任务链是否处于「展开全流程」形态；默认折叠（只显示当前节点）。 */
    private var chainExpanded = false

    /** 已应用的浮窗页面高度（`RemoteView.WRAP_CONTENT` 表示自适应），避免重复设置。 */
    private var pageHeight = RemoteView.WRAP_CONTENT

    /**
     * 支持「展开全流程」的状态。
     *
     * 不包含 [State.JOB_LIST]：该状态在折叠态已用掉约 277dp（工单卡片 + 两行按钮），展开后
     * 约 369dp，超出 FLOATING_BOX 的实测可视包络；且此时链条只多了 4 个「未开始」节点，
     * 展开价值最低。
     */
    private val chainExpandableStates = setOf(
        State.PREPARING,
        State.EXECUTING,
        State.SYNCING,
        State.ERROR,
        State.STOPPED,
        State.DONE,
    )

    /** 任务链上一次渲染的签名，用于跳过无变化的 IPC 同步。 */
    private var chainSignature: String? = null

    /** 任务链上一次应用的「状态 + 展开形态」组合，用于避免重复切换显隐。 */
    private var chainLayoutKey: String? = null

    /** 取消确认框展示中标记，防止重复弹窗（`DIALOG_ALREADY_EXISTS` 之外的第二道保护）。 */
    private var confirmingCancellation = false

    /** 当前错误的来源，用于把失败定位到任务链节点；与 [errorMessage] 同步维护。 */
    private var failureSource = FailureSource.NONE

    private val jobsObserver = Observer<List<Job>> { value ->
        runOnMain {
            latestJobs = value.orEmpty()
            // 出现新的在途工单即结束浏览态（design D1）：此时用户应该看到正在执行的那一单。
            if (browsingJobList && PatrolJobSelection.hasActiveJob(latestJobs)) {
                Log.i(TAG, "browsing job list ended: active job appeared")
                browsingJobList = false
            }
            val selection = PatrolJobSelection.resolve(
                jobs = latestJobs,
                browsing = browsingJobList,
                cachedActiveJobId = activeJobId,
                cachedPendingJobId = pendingJobId,
                currentSelectedJobId = mutableSelectedJobId.value,
            )
            activeJobId = selection.activeJobId
            pendingJobId = selection.pendingJobId
            if (mutableSelectedJobId.value != selection.selectedJobId) {
                publishSelectedJob(selection.selectedJobId)
            }

            val pending = pendingJobs()
            val focus = selection.activeJobId?.let { id -> latestJobs.firstOrNull { it.jobId == id } }
                ?: latestJobs.firstOrNull { it.jobId == selection.selectedJobId }
            val terminal = focus?.status == JobStatus.COMPLETED || focus?.status == JobStatus.CANCELLED
            val hasError = errorMessage != null || jobQueue().any { it.state == AssetState.FAILED }
            val stage = stageFor(focus)
            // 上传失败是数据面问题，不能被航线派生态覆盖；但用户主动停止优先于旧的上传错误。
            if (!terminal && state.value == State.ERROR && stage != PatrolStage.STOPPED &&
                failureSource == FailureSource.UPLOAD && hasError
            ) {
                renderIfConnected()
            } else {
                applyDerivedState(
                    job = focus,
                    stage = stage,
                    hasPending = pending.isNotEmpty(),
                )
            }
        }
    }

    /**
     * 共享阶段派生：浮窗与页面使用同一函数、同一快照输入，避免两侧各自推断。
     */
    private fun stageFor(job: Job?): PatrolStage {
        val snapshot = RouteController.snapshot.value
        return resolvePatrolStage(
            job = job,
            routeState = snapshot?.state ?: RouteState.ROUTE_PENDING,
            routeJobId = snapshot?.jobId,
            executionStarting = executionStarting,
        )
    }

    /**
     * 唯一真相源：浮窗只观察 [RouteController.snapshot]（状态 + 进度 + 错误 + 归属工单）。
     * SDK 原始航线流由 RouteController 过滤后统一对外，避免残留任务把浮窗点亮成可起飞。
     */
    private val routeSnapshotObserver = Observer<RouteSnapshot> { snapshot ->
        runOnMain {
            val stage = resolvePatrolStage(
                job = currentJob(),
                routeState = snapshot?.state ?: RouteState.ROUTE_PENDING,
                routeJobId = snapshot?.jobId,
                executionStarting = executionStarting,
            )
            latestRouteProgress = snapshot?.progress ?: WaylineProgress()
            applyDerivedState(
                job = currentJob(),
                stage = stage,
                hasPending = pendingJobs().isNotEmpty(),
            )
        }
    }

    /**
     * 把「工单 + 共享阶段」派生为唯一的展示态，并同步错误来源。
     *
     * 设计意图（design D2）：阶段、任务链与错误卡只能有一个派生入口；任何观察者都不得自行
     * 用局部启发式改写状态。主动停止（`STOPPED`）优先于旧的上传错误与旧失败。
     */
    private fun applyDerivedState(job: Job?, stage: PatrolStage, hasPending: Boolean) {
        // 阶段缓存只在此处写入：jobsObserver 与 routeSnapshotObserver 都必须先经过本入口，
        // 避免 switchVisibility/update* 读取上一次派生留下的旧阶段（冷启动「工单接收」回归）。
        latestStage = stage
        val target = deriveState(job, stage, hasPending)
        when (target) {
            State.ERROR -> when (stage) {
                PatrolStage.FAILED -> {
                    errorMessage = RouteController.error.value
                    failureSource = FailureSource.WAYLINE
                }
                PatrolStage.ROUTE_PENDING -> if (!executionStarting) {
                    errorMessage = string(R.string.fw_route_interrupted)
                    failureSource = FailureSource.WAYLINE
                }
                else -> Unit
            }
            State.STOPPED -> if (failureSource == FailureSource.WAYLINE) clearError()
            else -> Unit
        }
        setState(target)
    }

    /** 纯派生函数：不读控件、不写副作用，便于按状态组合核对（design D2）。 */
    private fun deriveState(job: Job?, stage: PatrolStage, hasPending: Boolean): State {
        // 浏览态优先于一切航线/工单派生：切换任务后必须稳定停在列表或空态（design D3）。
        if (browsingJobList) return if (hasPending) State.JOB_LIST else State.IDLE
        return when (stage) {
            PatrolStage.IDLE -> if (hasPending) State.JOB_LIST else State.IDLE
            PatrolStage.JOB_LIST -> State.JOB_LIST
            PatrolStage.ROUTE_PENDING,
            PatrolStage.ROUTE_PREPARING,
            PatrolStage.ROUTE_READY,
            -> State.PREPARING
            PatrolStage.EXECUTING, PatrolStage.PAUSED -> State.EXECUTING
            PatrolStage.STOPPED -> State.STOPPED
            PatrolStage.FAILED -> State.ERROR
            PatrolStage.FINISHED, PatrolStage.FINALIZING -> State.SYNCING
            PatrolStage.DONE, PatrolStage.CANCELLED -> State.DONE
        }
    }

    /**
     * 飞行阶段变化观察者。
     *
     * 设计意图：暂停入口的门控判据来自飞控飞行模式，其变化时机与航线状态不同（真机实测：航线
     * 状态早已是「执行中」，飞机却仍在地面），因此必须单独订阅该信号触发重渲染；判据本身在
     * [updateExecutingViews] 里直接读权威值，不另存缓存，避免两份状态发散。
     */
    private val pauseBlockedObserver = Observer<Boolean> { _ ->
        runOnMain { renderIfConnected() }
    }

    /** 返航权威阶段变化：只重渲染，不改变航线状态。 */
    private val goHomeStateObserver = Observer<GoHomeState> { _ ->
        runOnMain { renderIfConnected() }
    }

    /** 空中状态变化：决定[返航]显隐与[继续执行]文案。 */
    private val isFlyingObserver = Observer<Boolean> { _ ->
        runOnMain { renderIfConnected() }
    }

    private val assetsObserver = Observer<List<Asset>> { value ->
        runOnMain {
            latestAssets = value.orEmpty()
            renderIfConnected()
        }
    }

    private val mediaReferencesObserver = Observer<List<MediaReference>> { value ->
        runOnMain {
            latestMediaReferences = value.orEmpty()
            renderIfConnected()
        }
    }

    private val queueObserver = Observer<List<Asset>> { value ->
        runOnMain {
            latestQueue = value.orEmpty()
            val failed = jobQueue().count { it.state == AssetState.FAILED }
            if (failed > 0) {
                errorMessage = null
                failureSource = FailureSource.UPLOAD
                setState(State.ERROR)
            } else {
                renderIfConnected()
            }
        }
    }

    private val uploadProgressObserver = Observer<Map<String, UploadProgress>> { value ->
        runOnMain {
            latestUploadProgress = value.orEmpty()
            renderIfConnected()
        }
    }

    private val recognitionStageObserver = Observer<RecognitionStage> { value ->
        runOnMain {
            latestRecognitionStage = value ?: RecognitionStage.IDLE
            renderIfConnected()
        }
    }

    private val recognitionErrorObserver = Observer<String?> { value ->
        runOnMain {
            latestMatchError = value
            renderIfConnected()
        }
    }

    private val recognitionNoticeObserver = Observer<String?> { value ->
        runOnMain {
            latestMatchNotice = value
            renderIfConnected()
        }
    }

    private val capturePointsObserver = Observer<List<CapturePoint>> { value ->
        runOnMain {
            latestCapturePoints = value.orEmpty()
            fallbackCapturePoints.value = latestCapturePoints
            renderIfConnected()
        }
    }

    private val flyToStageObserver = Observer<FlyToStage> { value ->
        runOnMain {
            latestFlyToStage = value ?: FlyToStage.IDLE
            renderIfConnected()
        }
    }

    var onStartExecution: (Job, ResultCallback<Unit>) -> Unit = { _, callback ->
        callback.onFailure(IllegalStateException("Wayline flow is not connected"))
    }

    var onLoadRoute: (Job, ResultCallback<Unit>) -> Unit = { _, callback ->
        callback.onFailure(IllegalStateException("Route loading is not connected"))
    }
    var onTakeOff: (Job, ResultCallback<Unit>) -> Unit = { _, callback ->
        callback.onFailure(IllegalStateException("Takeoff is not connected"))
    }

    /** 浮窗[停止]：仅终止航线（工单保持进行中），由 MainApplication 接 RouteController.stop。 */
    var onStopMission: (ResultCallback<Unit>) -> Unit = { callback ->
        callback.onFailure(IllegalStateException("Route stop flow is not connected"))
    }

    /** 浮窗[返航]：由 MainApplication 接 `RouteController.requestReturnHome`。 */
    var onReturnHome: (ResultCallback<Unit>) -> Unit = { callback ->
        callback.onFailure(IllegalStateException("Return home flow is not connected"))
    }

    var onRetrySync: () -> Unit = {
        val jobId = currentJobId()
        latestQueue.filter {
            it.jobId == jobId && it.state == AssetState.FAILED &&
                MediaManager.isCurrentRound(it.jobId, assetCapturedAt(it))
        }.forEach { AssetUploadManager.retry(it.jobId, it.assetId) }
    }

    var onRetryFinalization: (Job, ResultCallback<Unit>) -> Unit = { _, callback ->
        callback.onFailure(IllegalStateException("Finalization flow is not connected"))
    }
    var onPauseFinalization: (Job) -> Unit = {}

    /** 取消 FlyTo（浮窗[取消]）；由 MainApplication 接 FlyToController.cancel。 */
    var onCancelFlyTo: () -> Unit = {}

    /** 进入目的地选取（浮窗入口）；由 MainApplication 接全屏页选取。 */
    var onMarkDestination: () -> Unit = {}
    /**
     * 暂停航线的装配钩子，由 `MainApplication` 接到 `WaylineTaskVM` 一侧。
     *
     * 设计意图：悬浮窗不直接调用 SDK，只通过钩子把动作交给 `WaylineTaskVM`；默认实现直接
     * 回调失败，保证未接线时悬浮窗报错而不是崩溃。
     */
    var onPauseWayline: (ResultCallback<Unit>) -> Unit = { callback ->
        callback.onFailure(IllegalStateException("Wayline pause flow is not connected"))
    }

    /** 恢复航线的装配钩子，语义同 [onPauseWayline]。 */
    var onResumeWayline: (ResultCallback<Unit>) -> Unit = { callback ->
        callback.onFailure(IllegalStateException("Wayline resume flow is not connected"))
    }

    /**
     * 「取消任务」的装配钩子：先停止航线，再取消服务端工单。
     *
     * 设计意图：把两段不可回退的动作收敛在装配处（`MainApplication`），悬浮窗只负责
     * 二次确认与结果反馈；任一步失败都必须回调失败而不继续推进。
     */
    var onCancelExecution: (Job, ResultCallback<Unit>) -> Unit = { _, callback ->
        callback.onFailure(IllegalStateException("Task cancel flow is not connected"))
    }

    fun start(context: Context, rcSerialNumber: String) {
        selectionPreferences = context.getSharedPreferences(SELECTION_PREFERENCES, Context.MODE_PRIVATE)
        selectionPreferenceKey = "selected-$rcSerialNumber"
        if (mutableSelectedJobId.value == null) {
            mutableSelectedJobId.value = selectionPreferences?.getString(selectionPreferenceKey, null)
        }
        if (!observersRegistered) {
            observersRegistered = true
            TaskManager.jobs.observeForever(jobsObserver)
            RouteController.snapshot.observeForever(routeSnapshotObserver)
            WaylineManager.pauseBlockedByFlightPhase.observeForever(pauseBlockedObserver)
            WaylineManager.isFlying.observeForever(isFlyingObserver)
            RouteController.goHomeState.observeForever(goHomeStateObserver)
            MediaManager.localAssets.observeForever(assetsObserver)
            MediaManager.mediaReferences.observeForever(mediaReferencesObserver)
            AssetUploadManager.queue.observeForever(queueObserver)
            AssetUploadManager.progress.observeForever(uploadProgressObserver)
            recognitionStageSource?.observeForever(recognitionStageObserver)
            recognitionErrorSource?.observeForever(recognitionErrorObserver)
            recognitionNoticeSource?.observeForever(recognitionNoticeObserver)
            capturePointsSource?.observeForever(capturePointsObserver)
            flyToStageSource?.observeForever(flyToStageObserver)
        }
        TaskManager.start(context, rcSerialNumber)
    }

    fun refreshTasks() = TaskManager.refresh()

    fun ensurePolling() = TaskManager.ensurePolling()

    fun acceptTask(jobId: String) {
        TaskManager.accept(jobId, object : ResultCallback<Job> {
            override fun onSuccess(data: Job) = runOnMain {
                browsingJobList = false
                activeJobId = data.jobId
                pendingJobId = data.jobId
                publishSelectedJob(data.jobId)
                clearError()
                setState(State.PREPARING)
                if (flowConfig != null) autoLoadRoute(data)
            }

            override fun onFailure(error: Throwable) = runOnMain {
                errorMessage = error.message
                setState(State.ERROR)
            }
        })
    }

    fun selectJob(jobId: String) = runOnMain {
        val job = latestJobs.firstOrNull { it.jobId == jobId } ?: return@runOnMain
        // 用户显式选中工单即结束浏览态：即使选中的是已完成工单，也按其真实状态展示（design D1）。
        browsingJobList = false
        publishSelectedJob(jobId)
        if (job.status == JobStatus.COMPLETED || job.status == JobStatus.CANCELLED) {
            activeJobId = null
            setState(State.DONE)
        } else if (job.status == JobStatus.DISPATCHED) {
            pendingJobId = jobId
            val hasActiveTask = latestJobs.any {
                it.status == JobStatus.ACCEPTED || it.status == JobStatus.IN_PROGRESS ||
                    it.status == JobStatus.FINALIZING
            }
            if (!hasActiveTask && state.value != State.JOB_LIST) setState(State.JOB_LIST)
            else updateJobListViews()
        }
    }

    fun ignoreTask() {
        pendingJobId = null
        publishSelectedJob(pendingJobs().firstOrNull()?.jobId)
        setState(if (pendingJobs().isEmpty()) State.IDLE else State.JOB_LIST)
    }

    fun rejectTask(jobId: String) {
        TaskManager.cancel(jobId, object : ResultCallback<Job> {
            override fun onSuccess(data: Job) = runOnMain {
                activeJobId = null
                pendingJobId = null
                publishSelectedJob(data.jobId)
                clearError()
                setState(State.DONE)
            }

            override fun onFailure(error: Throwable) = runOnMain {
                errorMessage = error.message
                setState(State.ERROR)
            }
        })
    }

    fun startTask(jobId: String) = TaskManager.start(jobId)

    fun completeTask(jobId: String) = TaskManager.complete(jobId)

    fun onPilotDisconnected() {
        pilotConnected = false
        isLayoutInitialized.set(false)
        isIconUploading.set(false)
        containerIds.clear()
        stageContainerIds.clear()
        stageStatusTexts.clear()
        stageProgressBars.clear()
        stagePauseButtons.clear()
        stageReturnHomeButtons.clear()
        stageTakeOffButtons.clear()
        clearViewReferences()
        RemoteViewHelper.clearUploadCache()
    }

    override fun onCleared() {
        TaskManager.jobs.removeObserver(jobsObserver)
        RouteController.snapshot.removeObserver(routeSnapshotObserver)
        WaylineManager.pauseBlockedByFlightPhase.removeObserver(pauseBlockedObserver)
        WaylineManager.isFlying.removeObserver(isFlyingObserver)
        RouteController.goHomeState.removeObserver(goHomeStateObserver)
        MediaManager.localAssets.removeObserver(assetsObserver)
        MediaManager.mediaReferences.removeObserver(mediaReferencesObserver)
        AssetUploadManager.queue.removeObserver(queueObserver)
        AssetUploadManager.progress.removeObserver(uploadProgressObserver)
        recognitionStageSource?.removeObserver(recognitionStageObserver)
        recognitionErrorSource?.removeObserver(recognitionErrorObserver)
        recognitionNoticeSource?.removeObserver(recognitionNoticeObserver)
        capturePointsSource?.removeObserver(capturePointsObserver)
        flyToStageSource?.removeObserver(flyToStageObserver)
        TaskManager.stopPolling()
        super.onCleared()
    }

    fun render(target: State = state.value ?: State.IDLE) {
        pilotConnected = true
        if (!isLayoutInitialized.get()) {
            uploadIconAndInit()
            return
        }
        updateDynamicViews()
        updateChainViews()
        switchVisibility(target)
    }

    private fun uploadIconAndInit() {
        if (!isIconUploading.compareAndSet(false, true)) return
        val context = MainApplication.instance
        val iconFile = File(context.getExternalFilesDir(null), EMPTY_TASK_ICON)
        FileUtils.copyAssetsFileIfNeed(context, "plugin/$EMPTY_TASK_ICON", iconFile)
        helper.pushImageFile(iconFile.path, object : CommonCallbacks.CompletionCallbackWithProgress<Double> {
            override fun onProgressUpdate(progress: Double) = Unit

            override fun onSuccess() = runOnMain {
                isIconUploading.set(false)
                if (!pilotConnected) return@runOnMain
                initLayout()
                // 建树后的收尾序列必须与 render() 完全一致：漏掉 updateChainViews() 会让
                // chainBlock 停留在 addChainBlock() 里设定的 GONE，直到下一次 render() 才被
                // 修好——若此后没有任何状态/数据更新（例如后端不可达），任务链会一直不显示。
                updateDynamicViews()
                updateChainViews()
                switchVisibility(state.value ?: State.IDLE)
            }

            override fun onFailure(error: IDJIError) {
                isIconUploading.set(false)
                Log.e(TAG, "upload empty task icon failed, degrade to icon-less layout: $error")
                // 图标资源缺失或上传失败时仍初始化浮窗，避免整个视图树为空（只有 Pilot 标题）。
                runOnMain {
                    if (!pilotConnected) return@runOnMain
                    initLayout()
                    updateDynamicViews()
                    updateChainViews()
                    switchVisibility(state.value ?: State.IDLE)
                }
            }
        })
    }

    /** Creates every state container once. Later renders only update properties and visibility. */
    private fun initLayout() {
        if (!isLayoutInitialized.compareAndSet(false, true)) return
        helper.clearLayout()
        helper.setPageLayoutParamWrapContent()

        // 深色半透明面板：与图传画面可区分，且不遮挡画面主体。
        val root = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setPadding(PANEL_PADDING, PANEL_PADDING, PANEL_PADDING, PANEL_PADDING)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(PANEL_COLOR)
                    .cornerRadius(PANEL_CORNER_RADIUS)
                    .build()
            )
        }
        root.addToRoot()
        addChainBlock(root)
        addIdleContainer(root)
        addJobListContainer(root)
        addPreparingContainer(root)
        addExecutingContainer(root)
        addSyncingContainer(root)
        addErrorContainer(root)
        addDoneContainer(root)
        addStageContainers(root)
    }

    /**
     * 按 [FlowConfig.stages] 一次性创建业务阶段容器（design D5）；未注入配置则不创建，
     * 浮窗行为与旧流程完全一致。容器仅切可见性，不重建视图树。
     */
    private fun addStageContainers(root: RemoteLinearLayout) {
        val config = flowConfig ?: return
        config.stages.forEach { stage ->
            val container = helper.createLinearLayoutVertical().apply {
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(RemoteView.WRAP_CONTENT)
                setPadding(0, 8, 0, 8)
                setVisibility(VisibilityType.GONE)
                root.addToRemote(this)
            }
            stageContainerIds[stage.id] = container.getId()
            val card = addSectionCard(container)
            addLabeledRow(card, R.string.fw_label_stage, bold = true).apply {
                setText(string(stage.labelResId))
                syncToRemote()
            }
            val status = addDynamicText(card)
            status.setText(string(R.string.fw_patrol_route_pending))
            status.syncToRemote()
            stageStatusTexts[stage.id] = status
            if (stage.id == PatrolFlowConfig.STAGE_EXECUTING ||
                stage.id == PatrolFlowConfig.STAGE_PAUSED ||
                stage.id == PatrolFlowConfig.STAGE_FINALIZING
            ) {
                stageProgressBars[stage.id] = helper.createProgressBar().apply {
                    setWidth(RemoteView.MATCH_PARENT)
                    setHeight(8)
                    setProgressColor(RemoteViewColors.BLUE_PRIMARY)
                }.also { card.addToRemote(it, contentMargin()) }
            }
            stage.controls.forEach { control ->
                val style = when {
                    control.action == PatrolFlowConfig.ACTION_STOP -> ButtonStyle.DANGER
                    control.primary -> ButtonStyle.FILLED
                    else -> ButtonStyle.OUTLINE
                }
                val button = createButton(control.labelResId, style = style) { onFlowAction(control.action) }
                container.addToRemote(button, contentMargin())
                when (control.action) {
                    PatrolFlowConfig.ACTION_PAUSE -> stagePauseButtons[stage.id] = button
                    PatrolFlowConfig.ACTION_RETURN_HOME -> stageReturnHomeButtons[stage.id] = button
                    PatrolFlowConfig.ACTION_TAKE_OFF ->
                        if (stage.id == PatrolFlowConfig.STAGE_ROUTE_READY) {
                            stageTakeOffButtons[stage.id] = button
                        }
                }
            }
            container.addToRemote(
                createButton(R.string.fw_btn_task_detail, style = ButtonStyle.OUTLINE) { navigateToPluginApp() },
                contentMargin(),
            )
        }
    }

    /**
     * 建树任务链：卡片容器内 5 个纵向节点行，每行「左侧序号徽章 + 连接线段 / 右侧双行文案」。
     *
     * 设计意图：节点只在切换显隐/颜色/文案时更新，不重建、不 `clearLayout()`；每行、徽章、
     * 主副文案与连接线段都留引用，供 [updateChainViews] / [renderChainVisibility] 复用同一批 id。
     * 默认折叠（只显示当前节点），以适配浮窗高度预算。
     */
    private fun addChainBlock(root: RemoteLinearLayout) {
        // 标准卡片容器：与浮窗其余信息卡同材质（ui-spec §7.1）。
        val block = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setPadding(CHAIN_CARD_PADDING, CHAIN_CARD_PADDING, CHAIN_CARD_PADDING, CHAIN_CARD_PADDING)
            setBackgroundShape(cardShape(SURFACE_COLOR))
            setVisibility(VisibilityType.GONE)
        }
        root.addToRemote(block, contentMargin())
        chainBlock = block

        // 不写功能名：用一个全宽的箭头指示「还有内容可展开」，同时整块节点行也可点。
        // 视觉取「弱提示」档（9sp / 不加粗 / 次级色），把点击热区与视觉权重解耦。
        val toggle = helper.createButton().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(SMALL_BUTTON_HEIGHT)
            setTextSize(TEXT_TINY)
            setTextColor(TEXT_SECONDARY_COLOR)
            setFontStyle(FontStyleType.NORMAL)
            setText(CHEVRON_COLLAPSED)
            setOnClickListener { runOnMain { toggleChainExpanded() } }
        }
        block.addToRemote(toggle, helper.margin(bottom = CHAIN_SUB_GAP))
        chainToggleButton = toggle

        val nodes = ChainNode.values()
        nodes.forEachIndexed { index, node ->
            // 整行容器：横向「gutter + 文案列」，顶部对齐。
            val row = helper.createLinearLayoutHorizontal().apply {
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(RemoteView.WRAP_CONTENT)
                setGravity(GravityType.TOP)
                setOnClickListener { runOnMain { toggleChainExpanded() } }
            }
            // 先挂载容器，再往容器里加子 View（违反会静默丢弃子 View）。
            block.addToRemote(row)
            chainNodeContainers[node] = row

            // 左侧 gutter：序号徽章 + 本行向下的连接线段，线段的圆心与徽章对齐。
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
                setTextColor(TEXT_PRIMARY_COLOR)
                setBackgroundShape(dotShape(PENDING_NODE_COLOR))
                setText((index + 1).toString())
            }
            gutter.addToRemote(badge)
            chainBadges[node] = badge

            if (index < nodes.size - 1) {
                val railMargin = (CHAIN_BADGE_SIZE - CHAIN_RAIL_WIDTH) / 2
                val rail = helper.createLinearLayoutVertical().apply {
                    setWidth(CHAIN_RAIL_WIDTH)
                    setHeight(CHAIN_RAIL_HEIGHT)
                    setBackgroundShape(cardShape(PENDING_NODE_COLOR))
                }
                gutter.addToRemote(rail, helper.margin(top = CHAIN_SUB_GAP, left = railMargin))
                chainRails[node] = rail
            }

            // 右侧文案列：主文案（节点名）+ 副文案（状态短语，无状态时隐藏）。
            val textCol = helper.createLinearLayoutVertical().apply {
                setWidth(0)
                setHeight(RemoteView.WRAP_CONTENT)
            }
            row.addToRemote(textCol, WidgetLayoutParam().apply {
                weight = 1f
                marginLeft = CHAIN_TEXT_GAP
            })

            val title = helper.createText().apply {
                setTextByResId(node.labelRes)
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(RemoteView.WRAP_CONTENT)
                setTextSize(TEXT_BODY)
                setTextColor(TEXT_SECONDARY_COLOR)
            }
            textCol.addToRemote(title)
            chainTitles[node] = title

            val sub = helper.createText().apply {
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(RemoteView.WRAP_CONTENT)
                setTextSize(TEXT_TINY)
                setTextColor(TEXT_SECONDARY_COLOR)
                setVisibility(VisibilityType.GONE)
            }
            textCol.addToRemote(sub, helper.margin(top = CHAIN_SUB_GAP))
            chainSubs[node] = sub
        }
    }

    private fun addIdleContainer(root: RemoteLinearLayout) {
        // 空态只保留「一句提示 + 一个出口」：文案直接显示在面板上、不套卡片底色，
        // 按钮全宽置于文案下方。高度保持自适应——固定高度会静默裁切底部按钮。
        val container = createContainer(root, State.IDLE).apply { setGravity(GravityType.CENTER) }

        val hint = createText(R.string.fw_idle).apply { setGravity(GravityType.CENTER) }
        container.addToRemote(hint, contentMargin())

        container.addToRemote(
            createButton(R.string.fw_btn_task_detail, style = ButtonStyle.OUTLINE) { navigateToPluginApp() },
            contentMargin(),
        )
    }

    private fun addJobListContainer(root: RemoteLinearLayout) {
        val container = createContainer(root, State.JOB_LIST)
        jobPositionText = addDynamicText(container, color = TEXT_SECONDARY_COLOR, size = TEXT_TINY).apply {
            setGravity(GravityType.CENTER)
            syncToRemote()
        }
        val card = addSectionCard(container)
        addLabeledRow(card, R.string.fw_label_stage).apply {
            setTextByResId(R.string.fw_chain_job_accept)
            syncToRemote()
        }
        jobTitleText = addLabeledRow(card, R.string.fw_label_task, bold = true)
        jobDeadlineText = addDynamicText(card, color = TEXT_SECONDARY_COLOR, size = TEXT_TINY).apply {
            // 截止时间是一行长文案：卡片可用宽仅 116dp（140dp − 面板 8×2 − 卡片 4×2），因此
            // 单行约束 + 省略号两道防线必须成对设置——只设 singleLine 会「截断但不显示省略号」，
            // 完全不加则超宽会换行抬高卡片（浮窗高度超限是静默裁切，不报错也不打日志）。
            setSingleLine(true)
            setEllipsize(EllipsizeType.END)
            // 注意：addDynamicText 内部已完成 addToRemote（控件已挂载），此处属「挂载后修改属性」，
            // 必须配对 syncToRemote()，否则只改本地缓存、Pilot 端不生效且不报错。
            syncToRemote()
        }
        val switchRow = addButtonRow(container)
        jobSwitchRowId = switchRow.getId()
        switchRow.addToRemote(
            createButton(R.string.fw_btn_previous, style = ButtonStyle.OUTLINE, width = 0) {
                selectPendingJob(-1)
            },
            weightedMargin(),
        )
        switchRow.addToRemote(
            createButton(R.string.fw_btn_next, style = ButtonStyle.OUTLINE, width = 0) { selectPendingJob(1) },
            weightedMargin(),
        )
        val actionRow = addButtonRow(container)
        actionRow.addToRemote(createButton(R.string.fw_btn_confirm, width = 0) {
            selectedPendingJob()?.let { acceptTask(it.jobId) }
        }, weightedMargin())
        actionRow.addToRemote(createButton(R.string.fw_btn_reject, style = ButtonStyle.OUTLINE, width = 0) {
            selectedPendingJob()?.let { rejectTask(it.jobId) }
        }, weightedMargin())
        container.addToRemote(
            createButton(R.string.fw_btn_task_detail, style = ButtonStyle.OUTLINE) { navigateToPluginApp() },
            contentMargin(),
        )
    }

    private fun addPreparingContainer(root: RemoteLinearLayout) {
        val container = createContainer(root, State.PREPARING)
        val card = addSectionCard(container)
        addLabeledRow(card, R.string.fw_label_stage).apply {
            setTextByResId(R.string.fw_chain_flight_prepare)
            syncToRemote()
        }
        preparingJobText = addLabeledRow(card, R.string.fw_label_task, bold = true)
        preparingStatusText = addLabeledRow(card, R.string.fw_label_status, valueColor = RemoteViewColors.BLUE_PRIMARY)
        card.addToRemote(
            createText(R.string.fw_hint_launch, color = TEXT_SECONDARY_COLOR, size = TEXT_TINY),
            contentMargin(),
        )
        container.addToRemote(createButton(R.string.fw_btn_start_execution) { startExecution() }, contentMargin())
        container.addToRemote(
            createButton(R.string.fw_btn_task_detail, style = ButtonStyle.OUTLINE) { navigateToPluginApp() },
            contentMargin(),
        )
    }

    private fun addExecutingContainer(root: RemoteLinearLayout) {
        val container = createContainer(root, State.EXECUTING)
        val card = addSectionCard(container)
        addLabeledRow(card, R.string.fw_label_stage).apply {
            setTextByResId(R.string.fw_chain_wayline_execution)
            syncToRemote()
        }
        executingStatusText = addLabeledRow(
            card,
            R.string.fw_label_status,
            valueColor = RemoteViewColors.BLUE_PRIMARY,
        )
        executingProgress = helper.createProgressBar().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(8)
            setProgressColor(RemoteViewColors.BLUE_PRIMARY)
        }.also { card.addToRemote(it, contentMargin()) }
        // 带自解释前缀的长句走全宽单行：140dp 面板里两列会把中文挤断。
        waypointText = addDynamicText(card)
        capturedText = addDynamicText(card)
        uploadedText = addDynamicText(card)
        val actionRow = addButtonRow(container)
        pauseResumeButton = createButton(R.string.fw_btn_pause_wayline, width = 0) {
            toggleWaylinePause()
        }.also { actionRow.addToRemote(it, weightedMargin()) }
        actionRow.addToRemote(
            createButton(R.string.fw_btn_cancel_execution, style = ButtonStyle.DANGER, width = 0) {
                confirmCancelExecution()
            },
            weightedMargin(),
        )
        container.addToRemote(
            createButton(R.string.fw_btn_task_detail, style = ButtonStyle.OUTLINE) { navigateToPluginApp() },
            contentMargin(),
        )
    }

    private fun addSyncingContainer(root: RemoteLinearLayout) {
        val container = createContainer(root, State.SYNCING)
        val card = addSectionCard(container)
        addLabeledRow(card, R.string.fw_label_stage).apply {
            setTextByResId(R.string.fw_chain_data_sync)
            syncToRemote()
        }
        syncingProgress = helper.createProgressBar().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(8)
            setProgressColor(RemoteViewColors.BLUE_PRIMARY)
        }.also { card.addToRemote(it, contentMargin()) }
        syncDownloadText = addDynamicText(card)
        syncFileText = addDynamicText(card)
        syncByteText = addDynamicText(card, color = TEXT_SECONDARY_COLOR, size = TEXT_TINY)
        val actionRow = addButtonRow(container)
        actionRow.addToRemote(createButton(R.string.fw_btn_retry_photos, width = 0) {
            currentJob()?.let(::retryFinalizationWithFeedback)
        }, weightedMargin())
        actionRow.addToRemote(
            createButton(R.string.fw_btn_pause_sync, style = ButtonStyle.OUTLINE, width = 0) {
                currentJob()?.let { job ->
                    onPauseFinalization(job)
                    showToast(R.string.fw_sync_paused_toast)
                }
            },
            weightedMargin(),
        )
        container.addToRemote(
            createButton(R.string.fw_btn_task_detail, style = ButtonStyle.OUTLINE) { navigateToPluginApp() },
            contentMargin(),
        )
    }

    private fun addErrorContainer(root: RemoteLinearLayout) {
        val container = createContainer(root, State.ERROR)
        val card = addSectionCard(container)
        errorStageText = addLabeledRow(card, R.string.fw_label_stage)
        errorTitleText = addLabeledRow(
            card,
            R.string.fw_label_status,
            valueColor = RemoteViewColors.RED_PRIMARY,
            bold = true,
        )
        errorCountText = addDynamicText(card, color = TEXT_SECONDARY_COLOR, size = TEXT_TINY)
        // 失败原因是变长句子，走全宽单行而不是窄的取值列，避免折成多行。
        errorText = addDynamicText(card, color = RemoteViewColors.RED_PRIMARY)
        container.addToRemote(createButton(R.string.fw_btn_retry) {
            clearError()
            val job = currentJob()
            if (job?.status == JobStatus.FINALIZING) {
                setState(State.SYNCING)
                onRetrySync()
                retryFinalizationWithFeedback(job)
            } else if (currentUploadProgress().failed > 0) {
                setState(State.SYNCING)
                onRetrySync()
            } else {
                setState(State.PREPARING)
                startExecution()
            }
        }, contentMargin())
        container.addToRemote(
            createButton(R.string.fw_btn_task_detail, style = ButtonStyle.OUTLINE) { navigateToPluginApp() },
            contentMargin(),
        )
    }

    private fun addDoneContainer(root: RemoteLinearLayout) {
        val container = createContainer(root, State.DONE)
        val card = addSectionCard(container)
        addLabeledRow(card, R.string.fw_label_stage).apply {
            setTextByResId(R.string.fw_chain_complete)
            syncToRemote()
        }
        doneTitleText = addLabeledRow(
            card,
            R.string.fw_label_status,
            valueColor = RemoteViewColors.GREEN_PRIMARY,
            bold = true,
        )
        doneFileText = addDynamicText(card)
        doneSizeText = addDynamicText(card, color = TEXT_SECONDARY_COLOR, size = TEXT_TINY)
        container.addToRemote(createButton(R.string.fw_btn_back_jobs) { backToJobList() }, contentMargin())
    }

    private fun createContainer(
        root: RemoteLinearLayout,
        state: State,
        height: Int = RemoteView.WRAP_CONTENT,
    ): RemoteLinearLayout {
        return helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(height)
            setPadding(0, 8, 0, 8)
            setVisibility(VisibilityType.GONE)
            root.addToRemote(this)
            containerIds[state] = getId()
        }
    }

    private fun createText(
        resId: Int,
        bold: Boolean = false,
        color: Int = TEXT_PRIMARY_COLOR,
        size: Float = TEXT_BODY,
    ): RemoteText = helper.createText().apply {
        setTextByResId(resId)
        setWidth(RemoteView.MATCH_PARENT)
        setHeight(RemoteView.WRAP_CONTENT)
        setTextSize(size)
        setTextColor(color)
        if (bold) setFontStyle(FontStyleType.BOLD)
    }

    private fun addDynamicText(
        parent: RemoteLinearLayout,
        bold: Boolean = false,
        color: Int = TEXT_PRIMARY_COLOR,
        size: Float = TEXT_BODY,
    ): RemoteText {
        return helper.createText().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setTextSize(size)
            setTextColor(color)
            if (bold) setFontStyle(FontStyleType.BOLD)
            parent.addToRemote(this, contentMargin())
        }
    }

    /**
     * 建树一个信息分组卡片：弱背景 + 4dp 内边距 + 9sp 加粗小节标题。
     *
     * 设计意图：把「任务信息 / 航线状态 / 同步状态」等信息块从纯文本流里分离出来，让飞手一眼
     * 分辨当前状态与数据分区（用户反馈「无明显区分、像纯文本混在一起」）。卡片用 WHITE_08
     * 弱背景：比面板底亮一档形成层次，又不与图传画面抢对比度。卡片内的行一律全宽单行，
     * 避免两列在 100dp 可用宽度下把中文文案挤断。
     */
    private fun addSectionCard(parent: RemoteLinearLayout): RemoteLinearLayout {
        val card = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setPadding(GAP_SMALL, GAP_SMALL, GAP_SMALL, GAP_SMALL)
            setBackgroundShape(cardShape(SURFACE_COLOR))
        }
        parent.addToRemote(card, contentMargin())
        return card
    }

    /**
     * 建树一行「键：值」行，返回取值控件供动态刷新。
     *
     * 设计意图：把混在一起的信息按语义分开——「阶段 / 任务 / 状态」这类字段用固定宽度的弱色
     * 标签列 + 主色取值列，飞手不必再从上下文猜每个字符串是什么（用户反馈「更清晰易懂些」）。
     * 带自解释前缀的长句（如「当前航点：6/20」）仍走全宽单行，避免右侧取值列过窄而截断。
     */
    private fun addLabeledRow(
        parent: RemoteLinearLayout,
        keyRes: Int,
        valueColor: Int = TEXT_PRIMARY_COLOR,
        bold: Boolean = false,
    ): RemoteText {
        val row = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        parent.addToRemote(row, contentMargin())
        row.addToRemote(createText(keyRes, color = TEXT_SECONDARY_COLOR, size = TEXT_TINY).apply {
            setWidth(LABEL_WIDTH)
        })
        val value = helper.createText().apply {
            setWidth(0)
            setHeight(RemoteView.WRAP_CONTENT)
            setTextSize(TEXT_BODY)
            setTextColor(valueColor)
            if (bold) setFontStyle(FontStyleType.BOLD)
        }
        row.addToRemote(value, WidgetLayoutParam().apply { weight = 1f })
        return value
    }

    /** 折叠/展开「全流程」；箭头按钮与当前节点列共用同一入口。 */
    private fun toggleChainExpanded() {
        chainExpanded = !chainExpanded
        renderChainVisibility()
    }

    private fun addButtonRow(parent: RemoteLinearLayout): RemoteLinearLayout {
        return helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            parent.addToRemote(this, helper.margin(top = GAP_SMALL, left = GAP_SMALL, right = GAP_SMALL))
        }
    }

    /**
     * 面 B 按钮工厂：三档样式 + 文字按钮。
     *
     * 主操作 = 主色填充白字；危险操作 = 失败语义色填充白字；次操作 = 主色 1dp 描边；
     * 文字按钮 = 无背景无描边的主色文字（`TEXT` 档）。
     * 注：链条展开/收起箭头不走本工厂——它按「弱提示」档单建（9sp + `NORMAL` + 次级色），
     * 视觉权重低于本工厂的全部按钮。
     * 不可用的按钮不渲染禁用态占位，改由调用点用显隐表达（浮窗高度预算紧张）。
     */
    private fun createButton(
        resId: Int,
        style: ButtonStyle = ButtonStyle.FILLED,
        width: Int = RemoteView.MATCH_PARENT,
        height: Int = BUTTON_HEIGHT,
        onClick: () -> Unit,
    ) = helper.createButton().apply {
        setTextByResId(resId)
        setWidth(width)
        setHeight(height)
        setTextSize(TEXT_BODY)
        when (style) {
            ButtonStyle.FILLED -> {
                setTextColor(RemoteViewColors.WHITE)
                setFontStyle(FontStyleType.BOLD)
                setBackgroundShape(
                    ShapeDescriptor.Builder()
                        .fillColor(RemoteViewColors.BLUE_PRIMARY)
                        .cornerRadius(BUTTON_CORNER_RADIUS)
                        .build(),
                )
            }
            ButtonStyle.DANGER -> {
                setTextColor(RemoteViewColors.WHITE)
                setFontStyle(FontStyleType.BOLD)
                setBackgroundShape(
                    ShapeDescriptor.Builder()
                        .fillColor(RemoteViewColors.RED_PRIMARY)
                        .cornerRadius(BUTTON_CORNER_RADIUS)
                        .build(),
                )
            }
            ButtonStyle.OUTLINE -> {
                setTextColor(RemoteViewColors.BLUE_PRIMARY)
                setBackgroundShape(
                    ShapeDescriptor.Builder()
                        .stroke(1, RemoteViewColors.BLUE_PRIMARY)
                        .cornerRadius(BUTTON_CORNER_RADIUS)
                        .build(),
                )
            }
            ButtonStyle.TEXT -> {
                setTextColor(RemoteViewColors.BLUE_PRIMARY)
                setFontStyle(FontStyleType.BOLD)
            }
        }
        setOnClickListener { runOnMain(onClick) }
    }

    private fun contentMargin() = helper.margin(top = 3, bottom = 3, left = GAP_SMALL, right = GAP_SMALL)

    private fun weightedMargin() = WidgetLayoutParam().apply {
        weight = 1f
        marginLeft = GAP_SMALL
        marginRight = GAP_SMALL
    }

    private fun cardShape(color: Int) = ShapeDescriptor.Builder()
        .fillColor(color)
        .cornerRadius(PANEL_CORNER_RADIUS)
        .build()

    private fun dotShape(color: Int) = ShapeDescriptor.Builder()
        .fillColor(color)
        .shape(ShapeDescriptor.SHAPE_OVAL)
        .build()

    private fun selectPendingJob(offset: Int) {
        val pending = pendingJobs()
        if (pending.isEmpty()) return
        val current = pending.indexOfFirst { it.jobId == pendingJobId }.let { if (it < 0) 0 else it }
        pendingJobId = pending[(current + offset + pending.size) % pending.size].jobId
        publishSelectedJob(pendingJobId)
        updateJobListViews()
    }

    private fun startExecution() {
        if (executionStarting) return
        val job = currentJob() ?: return
        executionStarting = true
        updatePreparingViews()
        onStartExecution(job, object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = runOnMain {
                executionStarting = false
                // 命令被接受：航线真实状态由 RouteController 快照驱动，这里只清错误。
                Log.i(TAG, "start execution accepted: jobId=${job.jobId}")
                clearError()
                setState(State.EXECUTING)
            }

            override fun onFailure(error: Throwable) = runOnMain {
                executionStarting = false
                // 启动门禁：按失败来源给可读文案，原始错误只落日志。
                Log.w(TAG, "start execution failed: ${error.javaClass.simpleName} ${error.message}")
                failureSource = FailureSource.PREPARE
                errorMessage = when (error) {
                    is MediaBoundaryFailure -> string(R.string.fw_media_prepare_failed)
                    is WaylineFailure -> waylineFailureMessage(error)
                    else -> error.message ?: error.javaClass.simpleName
                }
                setState(State.ERROR)
            }
        })
    }

    /** 失败来源对应的任务链节点名，用作错误卡「阶段」行的取值。 */
    private fun errorStageLabel(): String = string(
        when (failureSource) {
            FailureSource.WAYLINE -> R.string.fw_chain_wayline_execution
            FailureSource.UPLOAD -> R.string.fw_chain_data_sync
            else -> R.string.fw_chain_flight_prepare
        },
    )

    /**
     * 航线/飞控侧失败的可读文案。
     *
     * 设计意图：只映射知识库已写明恢复策略的错误码（`msdk-practices/capabilities/waypoint-mission.md`
     * 的错误与恢复表），未列入的码不猜测语义、退回通用兜底文案；原始 `errorCode` 与完整 `IDJIError`
     * 已由调用处的日志保留。错误码可能带模块前缀，因此按包含语义匹配，不做字符串等值比较。
     */
    private fun waylineFailureMessage(failure: WaylineFailure): String {
        val code = failure.errorCode.orEmpty()
        return when {
            code.contains("HOME_POINT_NOT_RECORDED") -> string(R.string.fw_start_failed_home_point)
            code.contains("GPS_INVALID") || code.contains("CANNOT_START_TASK_ON_WEAK_GPS") ->
                string(R.string.fw_start_failed_gps)
            code.contains("CANT_EXCUTE_IN_CURRENT_STATUS") ||
                code.contains("CANNOT_START_WAYLINE_WHEN_WAYLINE_RUNNING") ->
                string(R.string.fw_start_failed_state)
            code.contains("LOWER_BATTERY") -> string(R.string.fw_start_failed_battery)
            code.contains("HEIGHT_LIMIT") || code.contains("RADIUS_LIMIT") ||
                code.contains("OBSTACAL_STOP") -> string(R.string.fw_start_failed_limits)
            else -> string(R.string.fw_wayline_start_failed)
        }
    }

    private fun setState(target: State) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { setState(target) }
            return
        }
        if (state.value != target) {
            Log.i(TAG, "state: ${state.value} -> $target")
            state.value = target
        }
        renderIfConnected()
    }

    /** 清除错误信息并复位失败来源；失败来源决定任务链把哪个节点标为失败。 */
    private fun clearError() {
        errorMessage = null
        failureSource = FailureSource.NONE
    }

    private fun renderIfConnected() {
        if (pilotConnected) render(state.value ?: State.IDLE)
    }

    private fun updateDynamicViews() {
        updateJobListViews()
        updatePreparingViews()
        updateExecutingViews()
        updateSyncViews()
        updateErrorViews()
        updateDoneViews()
    }

    private fun updateJobListViews() {
        val job = selectedPendingJob() ?: return
        val pending = pendingJobs()
        val index = pending.indexOfFirst { it.jobId == job.jobId }.coerceAtLeast(0)
        setRemoteText(jobPositionText, string(R.string.fw_job_position, index + 1, pending.size))
        setRemoteText(jobTitleText, job.title)
        setRemoteText(
            jobDeadlineText,
            string(
                R.string.fw_job_deadline,
                JobTimeFormatter.formatDeadline(job.extras["deadline"], TimeZone.getDefault())
                    ?: string(R.string.detail_value_none),
            ),
        )
        jobSwitchRowId?.let { id ->
            helper.updateView(
                id,
                RemoteViewProperty.VISIBILITY,
                (if (pending.size > 1) VisibilityType.VISIBLE else VisibilityType.GONE).value.toString(),
            )
        }
    }

    private fun updatePreparingViews() {
        val job = currentJob() ?: return
        setRemoteText(preparingJobText, job.title)
        setRemoteText(
            preparingStatusText,
            string(
                if (executionStarting || latestStage == PatrolStage.ROUTE_PREPARING) {
                    R.string.fw_uploading_wayline
                } else {
                    R.string.fw_ready_to_start
                },
            ),
        )
    }

    private fun updateExecutingViews() {
        val progress = latestRouteProgress
        setRemoteText(
            executingStatusText,
            string(
                if (latestStage == PatrolStage.PAUSED) {
                    R.string.fw_chain_wayline_paused
                } else {
                    R.string.fw_chain_wayline_running
                },
            ),
        )
        setRemoteText(
            waypointText,
            string(R.string.fw_current_waypoint, progress.currentWaypoint, progress.totalWaypoints),
        )
        executingProgress?.apply {
            val max = if (progress.totalWaypoints > 0) progress.totalWaypoints else 100
            setMax(max)
            setProgress(progress.currentWaypoint.coerceIn(0, max))
            syncToRemote()
        }
        val jobId = executionJobId()
        val photos = jobId?.let { id ->
            latestMediaReferences.count { it.jobId == id && MediaManager.isCurrentRound(id, it.capturedAtMillis) }
        } ?: 0
        val uploaded = jobId?.let { id ->
            latestQueue.count {
                it.jobId == id && it.typeId == "photo" && it.state == AssetState.UPLOADED &&
                    MediaManager.isCurrentRound(id, assetCapturedAt(it))
            }
        } ?: 0
        setRemoteText(capturedText, string(R.string.fw_photos_captured, photos))
        setRemoteText(uploadedText, string(R.string.fw_photos_uploaded, uploaded))
        // 暂停/继续按钮文案只由权威航线状态驱动，命令回调不改它。
        // 「暂停」另需飞机不处于地面/起飞/降落/返航阶段：真机实测任务被接受后约 1.5 s 飞机仍在
        // 地面时飞控必然拒绝中断（`CANNOT_BREAK_WAYLINE_IN_CUR_STATE`），此时不应给出可点入口；
        // 「继续」只由中断状态决定（暂停后飞机悬停，恢复不受飞行阶段限制）。
        // 不可用时用显隐表达，不渲染禁用态占位。
        val interrupted = latestStage == PatrolStage.PAUSED
        val executing = latestStage == PatrolStage.EXECUTING
        val pausable = interrupted ||
            (executing && WaylineManager.pauseBlockedByFlightPhase.value != true)
        setVisible(pauseResumeButton, pausable)
        pauseResumeButton?.apply {
            setTextByResId(
                if (interrupted) {
                    R.string.fw_btn_resume_wayline
                } else {
                    R.string.fw_btn_pause_wayline
                },
            )
            syncToRemote()
        }
    }

    private fun updateSyncViews() {
        val jobId = executionJobId()
        val summary = syncSummary(
            jobId = jobId,
            uploadProgress = jobId?.let(::currentRoundProgress),
            references = jobId?.let(::currentRoundReferences).orEmpty(),
            assets = jobId?.let(::currentRoundAssets).orEmpty(),
        )
        val downloading = summary.totalPhotos > summary.downloadedPhotos
        val max = if (downloading) summary.totalPhotos.coerceAtLeast(1) else summary.totalFiles.coerceAtLeast(1)
        val value = if (downloading) summary.downloadedPhotos else summary.uploadedFiles
        syncingProgress?.apply {
            setMax(max)
            setProgress(value.coerceIn(0, max))
            syncToRemote()
        }
        setRemoteText(
            syncDownloadText,
            string(R.string.fw_download_progress, summary.downloadedPhotos, summary.totalPhotos),
        )
        setRemoteText(
            syncFileText,
            string(R.string.fw_file_progress, summary.uploadedFiles, summary.totalFiles),
        )
        setRemoteText(
            syncByteText,
            string(
                R.string.fw_byte_progress,
                formatBytes(summary.uploadedBytes),
                formatBytes(summary.totalBytes),
            ),
        )
    }

    private fun updateErrorViews() {
        val progress = currentUploadProgress()
        setRemoteText(errorStageText, errorStageLabel())
        setRemoteText(
            errorTitleText,
            string(if (progress.failed > 0) R.string.fw_failed else R.string.fw_execution_failed),
        )
        setRemoteText(errorCountText, string(R.string.fw_failed_count, progress.failed))
        setRemoteText(errorText, errorMessage ?: string(R.string.fw_upload_failed_message))
    }

    private fun updateDoneViews() {
        val jobId = executionJobId()
        val summary = syncSummary(
            jobId = jobId,
            uploadProgress = jobId?.let(::currentRoundProgress),
            references = jobId?.let(::currentRoundReferences).orEmpty(),
            assets = jobId?.let(::currentRoundAssets).orEmpty(),
        )
        setRemoteText(
            doneTitleText,
            string(if (currentJob()?.status == JobStatus.CANCELLED) R.string.fw_cancelled else R.string.fw_done),
        )
        setRemoteText(doneFileText, string(R.string.fw_done_files, summary.uploadedFiles))
        setRemoteText(doneSizeText, string(R.string.fw_done_size, formatBytes(summary.uploadedBytes)))
    }

    private fun switchVisibility(target: State) {
        // 浏览态直接走 legacy 容器：不参与流程阶段解析，避免「状态 IDLE + 阶段 DONE」并存（design D3）。
        if (browsingJobList) {
            legacySwitchVisibility(if (pendingJobs().isEmpty()) State.IDLE else State.JOB_LIST)
            return
        }
        // 已停止优先于命中/目的地旁路：停止后必须落到 STOPPED，不能被残留的旁路阶段覆盖。
        val stopped = latestStage == PatrolStage.STOPPED
        if (!stopped && latestRecognitionStage == RecognitionStage.MATCHED &&
            stageContainerIds.containsKey(PatrolFlowConfig.STAGE_MATCHED)
        ) {
            containerIds.values.forEach { id -> setContainerVisibility(id, false) }
            stageContainerIds.forEach { (stageId, id) ->
                setContainerVisibility(id, stageId == PatrolFlowConfig.STAGE_MATCHED)
            }
            renderStage(PatrolFlowConfig.STAGE_MATCHED)
            return
        }
        // 目的地旁路优先（FlyTo 请求/飞行中），到达/取消后回落到航线阶段。
        if (!stopped && latestFlyToStage in setOf(FlyToStage.REQUESTED, FlyToStage.FLYING) &&
            stageContainerIds.containsKey(PatrolFlowConfig.STAGE_DESTINATION)
        ) {
            containerIds.values.forEach { id -> setContainerVisibility(id, false) }
            stageContainerIds.forEach { (stageId, id) ->
                setContainerVisibility(id, stageId == PatrolFlowConfig.STAGE_DESTINATION)
            }
            renderStage(PatrolFlowConfig.STAGE_DESTINATION)
            return
        }
        val stage = flowStageFor(flowConfig, latestStage)
        if (stage == null) {
            legacySwitchVisibility(target)
            return
        }
        if (stage == PatrolFlowConfig.STAGE_JOB_LIST) {
            legacySwitchVisibility(State.JOB_LIST)
            return
        }
        containerIds.values.forEach { id -> setContainerVisibility(id, false) }
        stageContainerIds.forEach { (stageId, id) -> setContainerVisibility(id, stageId == stage) }
        renderStage(stage)
    }

    /** 未注入配置或工单已终态时的既有八状态渲染。 */
    private fun legacySwitchVisibility(target: State) {
        containerIds.forEach { (containerState, id) -> setContainerVisibility(id, containerState == target) }
        stageContainerIds.values.forEach { id -> setContainerVisibility(id, false) }
    }

    private fun setContainerVisibility(id: String, visible: Boolean) {
        helper.updateView(
            id,
            RemoteViewProperty.VISIBILITY,
            (if (visible) VisibilityType.VISIBLE else VisibilityType.GONE).value.toString(),
        )
    }

    private fun renderStage(stage: String) {
        val status = stageStatusTexts[stage] ?: return
        when (stage) {
            PatrolFlowConfig.STAGE_ROUTE_PENDING ->
                setRemoteText(status, string(R.string.fw_patrol_route_pending))
            PatrolFlowConfig.STAGE_ROUTE_PREPARING ->
                setRemoteText(
                    status,
                    routeProgressText(MainApplication.instance, latestRouteProgress)
                        ?: string(R.string.fw_route_loading_toast),
                )
            PatrolFlowConfig.STAGE_ROUTE_READY -> {
                setRemoteText(status, currentJob()?.title ?: string(R.string.fw_patrol_route_ready))
                stageTakeOffButtons[stage]?.apply {
                    setTextByResId(
                        if (isAirborne()) {
                            R.string.fw_btn_continue_execution
                        } else {
                            R.string.fw_btn_take_off
                        },
                    )
                    syncToRemote()
                }
            }
            PatrolFlowConfig.STAGE_EXECUTING ->
                setRemoteText(
                    status,
                    string(
                        R.string.fw_current_waypoint,
                        latestRouteProgress.currentWaypoint,
                        latestRouteProgress.totalWaypoints,
                    ) + " · " + string(R.string.fw_point_count, capturedPointCount()),
                )
            PatrolFlowConfig.STAGE_MATCHED ->
                setRemoteText(status, matchStatusText())
            PatrolFlowConfig.STAGE_PAUSED ->
                setRemoteText(
                    status,
                    string(
                        R.string.fw_current_waypoint,
                        latestRouteProgress.currentWaypoint,
                        latestRouteProgress.totalWaypoints,
                    ),
                )
            PatrolFlowConfig.STAGE_STOPPED ->
                setRemoteText(status, stoppedStatusText())
            PatrolFlowConfig.STAGE_FINISHED ->
                setRemoteText(status, string(R.string.fw_patrol_finished))
            PatrolFlowConfig.STAGE_FAILED ->
                setRemoteText(
                    status,
                    errorMessage ?: RouteController.snapshot.value?.error ?: string(R.string.fw_execution_failed),
                )
            PatrolFlowConfig.STAGE_FINALIZING -> {
                val progress = currentUploadProgress()
                val failed = jobQueue().count { it.state == AssetState.FAILED }
                val text = string(R.string.fw_upload_progress, progress.uploaded, progress.total) +
                    if (failed > 0) " · " + string(R.string.fw_upload_failed_count, failed) else ""
                setRemoteText(status, text)
            }
            PatrolFlowConfig.STAGE_DONE -> {
                val uploaded = jobQueue().filter { it.state == AssetState.UPLOADED }
                val totalBytes = uploaded.sumOf { it.sizeBytes }
                setRemoteText(
                    status,
                    string(R.string.fw_upload_done_summary, uploaded.size, formatBytes(totalBytes)),
                )
            }
            PatrolFlowConfig.STAGE_DESTINATION ->
                setRemoteText(status, string(R.string.fw_flyto_flying))
        }
        // 与旧容器一致：飞机仍在地面/起飞/降落/返航时飞控必然拒绝中断，不渲染不可用的暂停入口。
        stagePauseButtons[stage]?.let {
            setVisible(it, WaylineManager.pauseBlockedByFlightPhase.value != true)
        }
        // 返航入口只在飞控权威状态满足前置时出现；不可用时用 GONE，不渲染必失败入口。
        stageReturnHomeButtons[stage]?.let {
            setVisible(it, RouteController.canReturnHome())
        }
        stageProgressBars[stage]?.apply {
            if (stage == PatrolFlowConfig.STAGE_FINALIZING) {
                val progress = currentUploadProgress()
                val max = progress.total.coerceAtLeast(1)
                setMax(max)
                setProgress(progress.uploaded.coerceIn(0, max))
            } else {
                val max = if (latestRouteProgress.totalWaypoints > 0) latestRouteProgress.totalWaypoints else 100
                setMax(max)
                setProgress(latestRouteProgress.currentWaypoint.coerceIn(0, max))
            }
            syncToRemote()
        }
    }

    private fun capturedPointCount(): Int = latestCapturePoints.size

    private fun matchStatusText(): String {
        val error = latestMatchError ?: recognitionErrorSource?.value
        if (!error.isNullOrBlank()) return error
        val notice = latestMatchNotice ?: recognitionNoticeSource?.value
        if (!notice.isNullOrBlank()) return notice
        return string(R.string.fw_point_count, capturedPointCount())
    }

    private fun onFlowAction(action: String) {
        when (action) {
            PatrolFlowConfig.ACTION_LOAD_ROUTE, PatrolFlowConfig.ACTION_RETRY -> loadRouteWithFeedback()
            PatrolFlowConfig.ACTION_TAKE_OFF ->
                if (isAirborne()) confirmContinueExecution() else takeOffWithFeedback()
            PatrolFlowConfig.ACTION_PAUSE -> pauseWaylineExecution(currentJob())
            PatrolFlowConfig.ACTION_RESUME -> {
                if (latestRecognitionStage == RecognitionStage.MATCHED) {
                    confirmResumeFromMatch()
                } else {
                    resumeWaylineExecution(currentJob())
                }
            }
            PatrolFlowConfig.ACTION_STOP -> confirmStopRoute()
            PatrolFlowConfig.ACTION_RETURN_HOME -> confirmReturnHome()
            PatrolFlowConfig.ACTION_RETRY_SYNC -> currentJob()?.let(::retryFinalizationWithFeedback)
            PatrolFlowConfig.ACTION_PAUSE_SYNC -> currentJob()?.let { job ->
                onPauseFinalization(job)
                showToast(R.string.fw_sync_paused_toast)
            }
            PatrolFlowConfig.ACTION_BACK -> backToJobList()
            PatrolFlowConfig.ACTION_CANCEL_FLYTO -> onCancelFlyTo()
            PatrolFlowConfig.ACTION_MARK_DESTINATION -> onMarkDestination()
        }
    }

    /**
     * 返回任务列表（收尾完成/取消后的收敛）。
     *
     * 「切换任务」是一次用户意图，不只是清缓存：仅清空 [activeJobId] / [pendingJobId] 会在下一次
     * 工单刷新时被兜底逻辑重新选回已完成的工单，表现为浮窗先清空、随后又弹回完成态。
     */
    private fun backToJobList() {
        browsingJobList = true
        activeJobId = null
        pendingJobId = pendingJobs().firstOrNull()?.jobId
        publishSelectedJob(pendingJobId)
        Log.i(TAG, "switch to job list: pending=${pendingJobId != null}")
        setState(if (pendingJobId == null) State.IDLE else State.JOB_LIST)
    }

    private fun autoLoadRoute(job: Job) {
        onLoadRoute(job, object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = runOnMain {
                showToast(R.string.fw_route_loaded_toast)
            }

            override fun onFailure(error: Throwable) = runOnMain {
                showFailureToast(R.string.fw_route_load_failed_toast, error)
            }
        })
    }

    private fun loadRouteWithFeedback() {
        val job = currentJob() ?: return
        showToast(R.string.fw_route_loading_toast)
        onLoadRoute(job, object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = runOnMain {
                showToast(R.string.fw_route_loaded_toast)
            }

            override fun onFailure(error: Throwable) = runOnMain {
                showFailureToast(R.string.fw_route_load_failed_toast, error)
            }
        })
    }

    private fun takeOffWithFeedback() {
        val job = currentJob() ?: return
        onTakeOff(job, object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = runOnMain {
                showToast(R.string.fw_takeoff_confirmed_toast)
            }

            override fun onFailure(error: Throwable) = runOnMain {
                showFailureToast(R.string.fw_takeoff_rejected_toast, error)
            }
        })
    }

    private fun showFailureToast(resId: Int, error: Throwable) {
        val reason = localizedFailureReason(error)
        Toast.makeText(MainApplication.instance, string(resId, reason), Toast.LENGTH_LONG).show()
    }

    /** 就绪门禁的已知失败原因映射为本地化文案；其余保留原始技术信息便于排查。 */
    private fun localizedFailureReason(error: Throwable): String {
        FailureReasonText.resIdOf(error.message)?.let { return string(it) }
        // 原始错误只落日志，避免把英文内部串直接展示给飞手。
        Log.w(TAG, "unmapped failure reason: ${error.javaClass.simpleName} ${error.message}")
        return string(R.string.fw_reason_generic_failure)
    }

    // ────────────────────────── 任务链 ──────────────────────────

    /**
     * 把当前状态与失败来源映射为 5 个节点的状态序列。
     *
     * 设计意图：节点状态是纯函数结果，便于逐状态核对（见 design D5 映射表）；渲染层只负责
     * 把状态翻译成颜色与文案。`IDLE` 返回空列表，表示不显示任务链。
     */
    private fun chainNodeStatuses(): List<NodeStatus> {
        val done = NodeStatus.DONE
        val current = NodeStatus.CURRENT
        val failed = NodeStatus.FAILED
        val pending = NodeStatus.PENDING
        return when (state.value ?: State.IDLE) {
            State.IDLE -> emptyList()
            State.JOB_LIST -> listOf(current, pending, pending, pending, pending)
            State.PREPARING -> listOf(done, current, pending, pending, pending)
            State.EXECUTING -> listOf(done, done, current, pending, pending)
            // 主动停止是中性态：航线节点保持“当前”，由标签后缀说明已停止，不标失败。
            State.STOPPED -> listOf(done, done, current, pending, pending)
            State.SYNCING -> listOf(done, done, done, current, pending)
            State.DONE -> if (currentJob()?.status == JobStatus.CANCELLED) {
                listOf(done, done, failed, pending, pending)
            } else {
                listOf(done, done, done, done, done)
            }
            State.ERROR -> when (failureSource) {
                FailureSource.PREPARE -> listOf(done, failed, pending, pending, pending)
                FailureSource.WAYLINE -> listOf(done, done, failed, pending, pending)
                FailureSource.UPLOAD -> listOf(done, done, done, failed, pending)
                FailureSource.NONE -> listOf(done, done, pending, pending, pending)
            }
        }
    }

    /** 刷新任务链的徽章、主副文案与连接线；序列无变化时跳过同步，避免高频 IPC。 */
    private fun updateChainViews() {
        val block = chainBlock ?: return
        val statuses = chainNodeStatuses()
        if (statuses.isEmpty()) {
            chainSignature = null
            chainLayoutKey = null
            setVisible(block, false)
            return
        }
        setVisible(block, true)
        val signature = "$statuses|$chainExpanded|$latestStage|${failureSource.name}"
        if (signature != chainSignature) {
            chainSignature = signature
            Log.i(TAG, "task chain: expanded=$chainExpanded, nodes=$statuses")
            ChainNode.values().forEachIndexed { index, node ->
                val status = statuses.getOrElse(index) { NodeStatus.PENDING }

                // 标记：圆形徽章填充色随状态变化，序号文字在未开始态降为次级色。
                setShape(chainBadges[node], dotShape(dotColor(status)))
                chainBadges[node]?.apply {
                    setTextColor(
                        if (status == NodeStatus.PENDING) TEXT_SECONDARY_COLOR else TEXT_PRIMARY_COLOR,
                    )
                    syncToRemote()
                }

                // 主文案：节点名；当前/失败加粗，让「走到哪一步」在扫视时更醒目。
                chainTitles[node]?.apply {
                    setText(chainTitleText(node))
                    setTextColor(labelColor(status))
                    setFontStyle(
                        if (status == NodeStatus.CURRENT || status == NodeStatus.FAILED) {
                            FontStyleType.BOLD
                        } else {
                            FontStyleType.NORMAL
                        },
                    )
                    syncToRemote()
                }

                // 副文案：仅有状态短语时显示，其余节点保持 GONE，避免空白行占用高度。
                val subText = chainSubText(node, status)
                chainSubs[node]?.apply {
                    if (subText == null) {
                        setVisibility(VisibilityType.GONE)
                    } else {
                        setText(subText)
                        setTextColor(labelColor(status))
                        setVisibility(VisibilityType.VISIBLE)
                    }
                    syncToRemote()
                }

                chainRails[node]?.let { rail ->
                    setShape(rail, cardShape(connectorColor(status)))
                }
            }
        }
        renderChainVisibility()
    }

    /**
     * 折叠态只显示当前（或失败）节点；展开态显示全部节点与连接线，并把整个浮窗页面调高。
     *
     * 「状态 + 展开形态」未变化时跳过切换，避免每次 render 都刷一遍显隐与页面高度。
     */
    private fun renderChainVisibility() {
        val target = state.value ?: State.IDLE
        val expandable = target in chainExpandableStates
        val expanded = chainExpanded && expandable
        val key = "$target|$expanded|$expandable"
        if (key == chainLayoutKey) return
        chainLayoutKey = key
        val statuses = chainNodeStatuses()
        if (statuses.isEmpty()) return
        val focus = statuses.indexOfFirst { it == NodeStatus.CURRENT || it == NodeStatus.FAILED }
            .let { if (it < 0) 0 else it }

        setVisible(chainToggleButton, expandable)
        chainToggleButton?.apply {
            setText(if (expanded) CHEVRON_EXPANDED else CHEVRON_COLLAPSED)
            syncToRemote()
        }
        ChainNode.values().forEachIndexed { index, node ->
            setVisible(chainNodeContainers[node], expanded || index == focus)
            chainRails[node]?.let { setVisible(it, expanded) }
        }
        applyPageHeight(if (expanded) EXPANDED_PAGE_HEIGHT else RemoteView.WRAP_CONTENT)
    }

    /**
     * 切换浮窗页面高度：折叠态自适应内容，展开态请求固定高度以完整容纳链条。
     *
     * 设计意图：用「整体变高」代替滚动或裁剪；FLOATING_BOX 的可视上限是硬约束，因此只在
     * 高度真正变化时下发，失败不做补偿（Helper 未返回 OperationResult，见 design 的风险节）。
     */
    private fun applyPageHeight(height: Int) {
        if (height == pageHeight) return
        pageHeight = height
        if (height == RemoteView.WRAP_CONTENT) {
            helper.setPageLayoutParamWrapContent()
        } else {
            helper.setPageLayoutParam(height)
        }
    }

    /** 节点主文案：始终是节点名，不再拼接状态后缀（后缀改由副文案承载）。 */
    private fun chainTitleText(node: ChainNode): String = string(node.labelRes)

    /**
     * 节点副文案：「航线执行」节点附带权威航线状态，其余节点不显示副文案。
     *
     * 返回值 `null` 表示该节点本行没有副文案，调用方需把控件置 GONE 以节省浮窗高度。
     */
    private fun chainSubText(node: ChainNode, status: NodeStatus): String? {
        if (node != ChainNode.WAYLINE_EXECUTION) return null
        val suffix = when {
            status == NodeStatus.FAILED && currentJob()?.status == JobStatus.CANCELLED ->
                R.string.fw_chain_wayline_cancelled
            status == NodeStatus.FAILED -> R.string.fw_chain_wayline_failed
            status == NodeStatus.CURRENT && latestStage == PatrolStage.STOPPED ->
                R.string.fw_chain_wayline_stopped
            status == NodeStatus.CURRENT && latestStage == PatrolStage.PAUSED ->
                R.string.fw_chain_wayline_paused
            status == NodeStatus.CURRENT -> R.string.fw_chain_wayline_running
            else -> null
        }
        return suffix?.let { string(it) }
    }

    /**
     * 节点序号徽章填充色：已完成＝亮绿、当前＝主色蓝、失败＝红、未开始＝33% 白。
     *
     * 不得复用 `SURFACE_COLOR`（卡片弱背景）——它在卡片底上会让「未开始」徽章与卡片几乎
     * 无法区分；也不得用 `GREEN_DEEP`（深绿只适合浅底）。
     */
    private fun dotColor(status: NodeStatus): Int = when (status) {
        NodeStatus.DONE -> RemoteViewColors.GREEN_PRIMARY
        NodeStatus.CURRENT -> RemoteViewColors.BLUE_PRIMARY
        NodeStatus.FAILED -> RemoteViewColors.RED_PRIMARY
        NodeStatus.PENDING -> PENDING_NODE_COLOR
    }

    /** 节点主/副文案文字色：与 [dotColor] 同一套语义映射，未开始用次级文字色。 */
    private fun labelColor(status: NodeStatus): Int = when (status) {
        NodeStatus.DONE -> RemoteViewColors.GREEN_PRIMARY
        NodeStatus.CURRENT -> RemoteViewColors.BLUE_PRIMARY
        NodeStatus.FAILED -> RemoteViewColors.RED_PRIMARY
        NodeStatus.PENDING -> TEXT_SECONDARY_COLOR
    }

    /** 连接线颜色：按前一个节点的状态着色，未到达的段落用未开始色。 */
    private fun connectorColor(source: NodeStatus): Int = when (source) {
        NodeStatus.DONE -> RemoteViewColors.GREEN_PRIMARY
        NodeStatus.CURRENT -> RemoteViewColors.BLUE_PRIMARY
        else -> PENDING_NODE_COLOR
    }

    // ────────────────── 航线执行控制（暂停 / 继续 / 取消）──────────────────

    /** 暂停或继续当前航线；两个方向都可逆，点击即执行、无需二次确认。 */
    private fun toggleWaylinePause() {
        val job = currentJob()
        if (latestStage == PatrolStage.PAUSED) {
            resumeWaylineExecution(job)
        } else {
            pauseWaylineExecution(job)
        }
    }

    /**
     * 请求暂停航线。
     *
     * 命令回调只代表「命令被接受」，因此这里不切换按钮文案、不改状态；成败都留日志锚点，
     * 失败时额外提示用户，按钮文案仍由 [RouteController.snapshot] 的权威阶段驱动。
     */
    private fun pauseWaylineExecution(job: Job?) {
        Log.i(TAG, "wayline pause requested: jobId=${job?.jobId}, stage=$latestStage")
        onPauseWayline(object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = runOnMain {
                Log.i(TAG, "wayline pause accepted: jobId=${job?.jobId}, mission=${WaylineManager.missionName}")
            }

            override fun onFailure(error: Throwable) = runOnMain {
                Log.w(TAG, "wayline pause failed: jobId=${job?.jobId}, error=$error")
                showWaylineFailureToast(error, R.string.fw_wayline_pause_failed_toast)
            }
        })
    }

    /** 请求继续航线；失败处理与 [pauseWaylineExecution] 对称。 */
    private fun resumeWaylineExecution(job: Job?) {
        Log.i(TAG, "wayline resume requested: jobId=${job?.jobId}, stage=$latestStage")
        onResumeWayline(object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = runOnMain {
                Log.i(TAG, "wayline resume accepted: jobId=${job?.jobId}, mission=${WaylineManager.missionName}")
            }

            override fun onFailure(error: Throwable) = runOnMain {
                Log.w(TAG, "wayline resume failed: jobId=${job?.jobId}, error=$error")
                showWaylineFailureToast(error, R.string.fw_wayline_resume_failed_toast)
            }
        })
    }

    /**
     * 取消任务的入口：先弹 Pilot 原生确认框，用户确认后才停止航线并取消工单。
     *
     * 设计意图：取消是不可逆的高风险动作。确认框弹不出来时**绝不放行**——宁可提示失败，
     * 也不在缺少二次确认的情况下停止航线。`confirmingCancellation` 与 SDK 的
     * `DIALOG_ALREADY_EXISTS` 构成双保险，避免重复弹窗。
     */
    private fun confirmCancelExecution() {
        val job = currentJob() ?: return
        showConfirmDialog(
            titleRes = R.string.fw_cancel_confirm_title,
            messageRes = R.string.fw_cancel_confirm_message,
            positiveRes = R.string.fw_cancel_confirm_positive,
            unavailableRes = R.string.fw_cancel_dialog_unavailable_toast,
            logTag = "cancel jobId=${job.jobId}",
        ) { cancelExecution(job) }
    }

    /** 浮窗[停止]的二次确认：确认后只终止航线，工单保持进行中。 */
    /** 命中失败后恢复航线：空中高风险动作，必须先二次确认。 */
    private fun confirmResumeFromMatch() {
        showConfirmDialog(
            titleRes = R.string.fw_match_resume_confirm_title,
            messageRes = R.string.fw_match_resume_confirm_message,
            positiveRes = R.string.fw_match_resume_confirm_positive,
            unavailableRes = R.string.fw_match_resume_dialog_unavailable_toast,
            logTag = "resume-from-match jobId=${currentJob()?.jobId}",
        ) { resumeWaylineExecution(currentJob()) }
    }

    private fun confirmStopRoute() {
        showConfirmDialog(
            titleRes = R.string.fw_stop_confirm_title,
            messageRes = R.string.fw_stop_confirm_message,
            positiveRes = R.string.fw_stop_confirm_positive,
            unavailableRes = R.string.fw_stop_dialog_unavailable_toast,
            logTag = "stop jobId=${currentJob()?.jobId}",
        ) { stopRoute() }
    }

    private fun showConfirmDialog(
        titleRes: Int,
        messageRes: Int,
        positiveRes: Int,
        unavailableRes: Int,
        logTag: String,
        onConfirm: () -> Unit,
    ) {
        if (confirmingCancellation) return
        confirmingCancellation = true
        val config = DialogConfig.Builder()
            .setTitle(string(titleRes))
            .setMessage(string(messageRes))
            .setStartButtonText(string(R.string.fw_cancel_confirm_negative))
            .setEndButtonText(string(positiveRes))
            .setOnStartButtonClick { runOnMain { confirmingCancellation = false } }
            .setOnEndButtonClick {
                runOnMain {
                    confirmingCancellation = false
                    onConfirm()
                }
            }
            .setOnDismiss { runOnMain { confirmingCancellation = false } }
            .build()
        val result = RemoteDialogManager.getInstance().showConfirmDialog(config)
        Log.i(TAG, "confirm dialog shown: $logTag, result=$result")
        if (result != OperationResult.SUCCESS) {
            confirmingCancellation = false
            showToast(unavailableRes)
        }
    }

    private fun stopRoute() {
        Log.i(TAG, "route stop requested: stage=$latestStage")
        onStopMission(object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = runOnMain {
                Log.i(TAG, "route stop accepted")
            }

            override fun onFailure(error: Throwable) = runOnMain {
                Log.w(TAG, "route stop failed: error=$error")
                showToastMessage(
                    string(R.string.fw_wayline_stop_failed_toast, error.message ?: error.javaClass.simpleName),
                )
            }
        })
    }

    /** 返航入口：仅在空中且前置满足时可达；确认框失败绝不放行。 */
    private fun confirmReturnHome() {
        if (!RouteController.canReturnHome()) {
            Log.w(TAG, "return home entry rejected: canReturnHome=false, stage=$latestStage")
            return
        }
        showConfirmDialog(
            titleRes = R.string.fw_return_home_confirm_title,
            messageRes = R.string.fw_return_home_confirm_message,
            positiveRes = R.string.fw_return_home_confirm_positive,
            unavailableRes = R.string.fw_return_home_dialog_unavailable_toast,
            logTag = "return home jobId=${currentJob()?.jobId}",
        ) { requestReturnHome() }
    }

    private fun requestReturnHome() {
        Log.i(TAG, "return home requested: jobId=${currentJob()?.jobId}, stage=$latestStage")
        onReturnHome(object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = runOnMain {
                Log.i(TAG, "return home accepted: jobId=${currentJob()?.jobId}")
                showToast(R.string.fw_return_home_accepted_toast)
            }

            override fun onFailure(error: Throwable) = runOnMain {
                Log.w(TAG, "return home failed: ${error.javaClass.simpleName} ${error.message}")
                showToastMessage(
                    string(R.string.fw_return_home_failed_toast, localizedFailureReason(error)),
                )
            }
        })
    }

    /** 悬停中点击[继续执行]：先说明“从起点重飞”，确认后再走既有起飞门禁。 */
    private fun confirmContinueExecution() {
        showConfirmDialog(
            titleRes = R.string.fw_continue_confirm_title,
            messageRes = R.string.fw_continue_confirm_message,
            positiveRes = R.string.fw_continue_confirm_positive,
            unavailableRes = R.string.fw_continue_dialog_unavailable_toast,
            logTag = "continue from hover jobId=${currentJob()?.jobId}",
        ) { takeOffWithFeedback() }
    }

    /** 飞控权威空中状态；信号缺失时按地面处理（不显示返航入口、使用起飞文案）。 */
    private fun isAirborne(): Boolean = WaylineManager.isFlying.value == true

    /** 已停止阶段的进度文案：返航权威状态优先，其次区分空中悬停与地面停止。 */
    private fun stoppedStatusText(): String = when (RouteController.goHomeState.value) {
        GoHomeState.RETURNING_TO_HOME, GoHomeState.LANDING -> string(R.string.fw_patrol_returning_home)
        GoHomeState.COMPLETED -> string(R.string.fw_patrol_returned_home)
        else -> if (isAirborne()) {
            string(R.string.fw_patrol_stopped_hovering)
        } else {
            string(R.string.fw_patrol_stopped)
        }
    }

    /**
     * 已确认取消：停止航线 → 取消工单。
     *
     * 任一步失败都不结束任务、保持执行态并提示原因；任务最终状态始终以服务端返回为准，
     * 客户端不伪造结算。
     */
    private fun cancelExecution(job: Job) {
        Log.i(TAG, "task cancel requested: jobId=${job.jobId}, stage=$latestStage")
        onCancelExecution(job, object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = runOnMain {
                Log.i(TAG, "job cancel accepted: jobId=${job.jobId}")
                clearError()
            }

            override fun onFailure(error: Throwable) = runOnMain {
                Log.w(TAG, "task cancel failed: jobId=${job.jobId}, error=$error")
                val resId = if (error is WaylineFailure) {
                    R.string.fw_wayline_stop_failed_toast
                } else {
                    R.string.fw_task_cancel_failed_toast
                }
                showToastMessage(string(resId, error.message ?: error.javaClass.simpleName))
            }
        })
    }

    /**
     * 航线命令失败的可读提示。
     *
     * 设计意图：错误码带模块前缀，判定必须用 `IDJIError.isError(...)`（后缀匹配），不做字符串
     * 等值或包含比较。其中「时机不合法」类错误（`CANNOT_BREAK_WAYLINE_IN_CUR_STATE` /
     * `CANT_EXCUTE_IN_CURRENT_STATUS`）在本工程有确定的现实成因——任务刚开始、飞机尚未离地并
     * 进入航线飞行时飞控不接受中断（真机实测见 design D1），故按**瞬时**状态表述并引导用户稍候
     * 重试，而不是当成功能失败；其余码走通用提示并保留原始原因。
     */
    private fun showWaylineFailureToast(error: Throwable, fallbackRes: Int) {
        val sdkError = (error as? WaylineFailure)?.sdkError
        val notBreakableYet = sdkError?.isError(DJIWaypointMissionError.CANNOT_BREAK_WAYLINE_IN_CUR_STATE) == true ||
            sdkError?.isError(DJIWaypointMissionError.CANT_EXCUTE_IN_CURRENT_STATUS) == true
        if (notBreakableYet) {
            showToast(R.string.fw_wayline_not_breakable_toast)
            return
        }
        showToastMessage(string(fallbackRes, error.message ?: error.javaClass.simpleName))
    }

    private fun selectedPendingJob(): Job? = pendingJobs().firstOrNull { it.jobId == pendingJobId }
        ?: pendingJobs().firstOrNull()

    private fun pendingJobs(): List<Job> = latestJobs.filter { it.status == JobStatus.DISPATCHED }

    private fun currentJobId(): String? {
        // 浏览态不把任何终态工单当作「当前工单」，否则阶段解析会回到完成态（design D3）。
        if (browsingJobList) return null
        val selected = latestJobs.firstOrNull { it.jobId == mutableSelectedJobId.value }
        if (selected?.status == JobStatus.COMPLETED || selected?.status == JobStatus.CANCELLED) return selected.jobId
        return activeJobId ?: pendingJobId ?: selected?.jobId
    }

    /** 航线归属工单优先：与页面/浮窗的航线快照同源，避免用 SDK 适配层的内部状态当业务身份。 */
    private fun executionJobId(): String? =
        RouteController.snapshot.value?.jobId ?: currentJobId()

    private fun currentJob(): Job? = currentJobId()?.let { id -> latestJobs.firstOrNull { it.jobId == id } }

    private fun jobQueue(): List<Asset> {
        val jobId = executionJobId()
        return latestQueue.filter { it.jobId == jobId }
    }

    private fun currentUploadProgress(): UploadProgress {
        val jobId = executionJobId().orEmpty()
        return currentRoundProgress(jobId) ?: UploadProgress(jobId, 0, 0, 0)
    }

    private fun assetCapturedAt(asset: Asset): Long =
        asset.meta["capturedAtMillis"]?.toLongOrNull() ?: 0L

    /** 本轮媒体引用：浮窗与收尾进度只统计本轮。 */
    private fun currentRoundReferences(jobId: String): List<MediaReference> =
        latestMediaReferences.filter {
            it.jobId == jobId && MediaManager.isCurrentRound(jobId, it.capturedAtMillis)
        }

    /** 本轮本地资产：浮窗与收尾进度只统计本轮。 */
    private fun currentRoundAssets(jobId: String): List<Asset> =
        latestAssets.filter {
            it.jobId == jobId && MediaManager.isCurrentRound(jobId, assetCapturedAt(it))
        }

    /**
     * 上传进度只统计本轮照片。
     *
     * 上传队列同时保留历史项；当队列里存在历史项时，按本轮队列重新汇总，避免「已上传」数字
     * 把上一轮计入。队列全部属于本轮时直接复用上传引擎进度，保留分片字节的实时精度。
     */
    private fun currentRoundProgress(jobId: String): UploadProgress? {
        val engine = latestUploadProgress[jobId] ?: return null
        val jobQueue = latestQueue.filter { it.jobId == jobId }
        val roundQueue = jobQueue.filter { MediaManager.isCurrentRound(jobId, assetCapturedAt(it)) }
        if (roundQueue.size == jobQueue.size) return engine
        return UploadProgress(
            jobId = jobId,
            total = roundQueue.size,
            uploaded = roundQueue.count { it.state == AssetState.UPLOADED },
            failed = roundQueue.count { it.state == AssetState.FAILED },
            totalBytes = roundQueue.sumOf { it.sizeBytes },
            uploadedBytes = roundQueue.filter { it.state == AssetState.UPLOADED }.sumOf { it.sizeBytes },
        )
    }

    private fun setRemoteText(view: RemoteText?, text: String) {
        view?.apply {
            setText(text)
            syncToRemote()
        }
    }

    /** 更新背板形状并同步；属性变更必须配对 `syncToRemote()`，否则 Pilot 端不刷新。 */
    private fun setShape(view: RemoteViewProvider?, shape: ShapeDescriptor) {
        view?.apply {
            setBackgroundShape(shape)
            syncToRemote()
        }
    }

    /** 切换可见性并同步；已挂载组件只改显隐，不重建、不 `clearLayout()`。 */
    private fun setVisible(view: RemoteViewProvider?, visible: Boolean) {
        view?.apply {
            setVisibility(if (visible) VisibilityType.VISIBLE else VisibilityType.GONE)
            syncToRemote()
        }
    }

    private fun showToastMessage(message: String) {
        Toast.makeText(MainApplication.instance, message, Toast.LENGTH_LONG).show()
    }

    private fun retryFinalizationWithFeedback(job: Job) {
        showToast(R.string.fw_retry_started_toast)
        onRetryFinalization(job, object : ResultCallback<Unit> {
            override fun onSuccess(data: Unit) = runOnMain {
                showToast(R.string.fw_retry_success_toast)
            }

            override fun onFailure(error: Throwable) = runOnMain {
                val reason = if (error.message?.contains("Pending photos", ignoreCase = true) == true ||
                    error.message?.contains("still pending", ignoreCase = true) == true
                ) {
                    string(R.string.fw_photos_not_found_toast)
                } else {
                    error.message ?: error.javaClass.simpleName
                }
                Toast.makeText(
                    MainApplication.instance,
                    string(R.string.fw_retry_failed_toast, reason),
                    Toast.LENGTH_LONG,
                ).show()
            }
        })
    }

    private fun showToast(resId: Int) {
        Toast.makeText(MainApplication.instance, resId, Toast.LENGTH_SHORT).show()
    }

    private fun string(resId: Int, vararg args: Any): String =
        MainApplication.instance.getString(resId, *args)

    private fun publishSelectedJob(jobId: String?) {
        if (mutableSelectedJobId.value != jobId) mutableSelectedJobId.value = jobId
        val key = selectionPreferenceKey ?: return
        selectionPreferences?.edit()?.apply {
            if (jobId == null) remove(key) else putString(key, jobId)
        }?.apply()
    }

    private fun formatBytes(bytes: Long): String =
        Formatter.formatShortFileSize(MainApplication.instance, bytes)

    private fun clearViewReferences() {
        jobTitleText = null
        jobPositionText = null
        jobDeadlineText = null
        jobSwitchRowId = null
        preparingJobText = null
        preparingStatusText = null
        executingStatusText = null
        errorStageText = null
        waypointText = null
        capturedText = null
        uploadedText = null
        executingProgress = null
        pauseResumeButton = null
        syncDownloadText = null
        syncFileText = null
        syncByteText = null
        syncingProgress = null
        errorCountText = null
        errorTitleText = null
        errorText = null
        doneFileText = null
        doneSizeText = null
        doneTitleText = null
        // 任务链：id 与句柄只在当前连接/布局代内有效，断连后必须全部废弃并重建。
        chainBlock = null
        chainToggleButton = null
        chainNodeContainers.clear()
        chainBadges.clear()
        chainTitles.clear()
        chainSubs.clear()
        chainRails.clear()
        chainSignature = null
        chainLayoutKey = null
        // 页面高度参数随布局代一起废弃，下一次建树时重新设置为自适应。
        pageHeight = RemoteView.WRAP_CONTENT
    }

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else mainHandler.post(action)
    }

    private fun navigateToPluginApp() {
        NavigateManager.getInstance().navigateToPage(
            NavigateAction(NavigateAction.Value.SELF_DEFINE, "dji.sample.patrol.PluginActivity"),
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() = Unit
                override fun onFailure(error: IDJIError) {
                    Log.e(TAG, "navigateToPluginApp failed: $error")
                }
            },
        )
    }
}
