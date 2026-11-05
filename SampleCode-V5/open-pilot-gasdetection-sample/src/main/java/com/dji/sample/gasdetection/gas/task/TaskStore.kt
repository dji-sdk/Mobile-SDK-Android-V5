package com.dji.sample.gasdetection.gas.task

import android.util.Log
import com.dji.sample.gasdetection.gas.autoflight.AutoFlightStageSink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 任务状态持有者。
 *
 * 设计意图：浮窗与入口页共用同一份 [TaskUiState]，网络结果只在这里落到阶段；阶段推进必须等
 * 状态回写成功，避免「请求发出去了界面就前进了」。冲突任务不覆盖当前任务上下文。
 */
class TaskStore(
    private val api: JobApiClient,
    private val scope: CoroutineScope,
    private val preflightEvaluator: () -> PreflightResult,
) : AutoFlightStageSink {

    private val mutableState = MutableStateFlow(TaskUiState())
    val state: StateFlow<TaskUiState> = mutableState.asStateFlow()

    /** 拉取任务；失败保留已有任务，只更新同步状态。 */
    fun refresh() {
        val snapshot = mutableState.value
        if (snapshot.sync == TaskSyncState.Loading) return
        mutableState.update { it.copy(sync = TaskSyncState.Loading) }
        scope.launch {
            when (val result = api.listJobs()) {
                is ApiResult.Err -> {
                    Log.w(TAG, "jobs refresh failed: ${describe(result.error)}")
                    mutableState.update {
                        it.copy(sync = TaskSyncState.Failed(result.error))
                    }
                }

                is ApiResult.Ok -> {
                    Log.d(TAG, "jobs refreshed count=${result.value.size}")
                    applyJobs(result.value)
                }
            }
        }
    }

    /** 失败原因的单行描述；只用于日志，界面文案仍由 TaskSyncState 决定。 */
    private fun describe(error: ApiError): String = when (error) {
        is ApiError.Network -> "network: ${error.message}"
        is ApiError.Http -> "http ${error.code}: ${error.body.orEmpty().take(200)}"
        is ApiError.Parse -> "parse: ${error.message}"
    }

    /** 接受任务：状态回写成功后才进入预检。 */
    fun accept() {
        val job = mutableState.value.job ?: return
        if (mutableState.value.stage != TaskStage.NEW_ORDER &&
            mutableState.value.stage != TaskStage.TASK_DETAIL
        ) {
            return
        }
        scope.launch {
            when (val result = api.updateStatus(job.jobId, JobStatus.ACCEPTED)) {
                is ApiResult.Err -> mutableState.update { it.copy(sync = TaskSyncState.Failed(result.error)) }
                is ApiResult.Ok -> {
                    val updated = result.value
                    mutableState.update { state ->
                        state.copy(
                            job = updated,
                            jobs = state.jobs.map {
                                if (it.jobId == updated.jobId) updated else it
                            },
                            selectedJobId = updated.jobId,
                            stage = TaskStage.PREP,
                            sync = TaskSyncState.Ready,
                        )
                    }
                    evaluatePreflight()
                }
            }
        }
    }

    /** 拒绝任务：回写成功后清空任务上下文。 */
    fun reject() {
        val job = mutableState.value.job ?: return
        scope.launch {
            when (val result = api.updateStatus(job.jobId, JobStatus.REJECTED)) {
                is ApiResult.Err -> mutableState.update { it.copy(sync = TaskSyncState.Failed(result.error)) }
                is ApiResult.Ok -> {
                    // 拒绝后清空本地上下文并重新拉取，让列表按后端最新状态重排。
                    mutableState.value = TaskUiState()
                    refresh()
                }
            }
        }
    }

    /** 飞手主动改为手动监测。 */
    fun enterManual() {
        val next = TaskStageMachine.next(mutableState.value.stage, TaskStageMachine.Event.StartManual)
        if (!next.rejected) {
            mutableState.update { it.copy(stage = next.stage) }
            reportInProgress()
        }
    }

    /**
     * 飞手确认启动自动飞行。
     *
     * 只有 `PREP_READY` 且最新预检全部通过时才允许进入 `FLYTO`，避免用过期授权启动；
     * 返回 `false` 表示当前阶段或门控不允许。
     */
    override fun beginAutoFlight(): Boolean {
        val current = mutableState.value
        if (!TaskStageMachine.canStartAutoFlight(
                current.stage,
                current.preflight?.allPassed == true,
            )
        ) {
            return false
        }
        val next = TaskStageMachine.next(current.stage, TaskStageMachine.Event.StartAutoFlight)
        if (next.rejected) return false
        mutableState.update { it.copy(stage = next.stage) }
        reportInProgress()
        return true
    }

    /**
     * 飞手在到达后确认开始搜索船舶。
     *
     * 只允许从 `FLYTO` 进入，避免未到点就开启识别；返回 `false` 表示当前阶段不允许。
     */
    fun beginSearch(): Boolean {
        val next = TaskStageMachine.next(mutableState.value.stage, TaskStageMachine.Event.StartSearch)
        if (next.rejected) return false
        mutableState.update { it.copy(stage = next.stage) }
        return true
    }

    /**
     * 虚拟摇杆控制权确认后进入跟随阶段。
     *
     * 只允许从 `SEARCH` 进入；返回 `false` 表示阶段不允许（调用方必须立即释放虚拟摇杆）。
     */
    fun beginFollow(): Boolean {
        val next = TaskStageMachine.next(mutableState.value.stage, TaskStageMachine.Event.FollowStarted)
        if (next.rejected) return false
        mutableState.update { it.copy(stage = next.stage) }
        return true
    }

    /** 跟随退出后进入接管：统一回到手动监测。 */
    fun handoverFollow() {
        handoverAutoFlight()
    }

    /** 自动阶段进入接管（异常或飞手退出）：统一回到手动监测。 */
    override fun handoverAutoFlight() {
        val next = TaskStageMachine.next(
            mutableState.value.stage,
            TaskStageMachine.Event.AutoFlightHandover,
        )
        if (!next.rejected) mutableState.update { it.copy(stage = next.stage) }
    }

    /** 在预检阶段重新执行预检。 */
    fun recheck() {
        if (mutableState.value.stage == TaskStage.PREP ||
            mutableState.value.stage == TaskStage.NOT_READY
        ) {
            evaluatePreflight()
        }
    }

    /** 结束当前任务（收尾占位）。 */
    fun finish() {
        val next = TaskStageMachine.next(mutableState.value.stage, TaskStageMachine.Event.Finish)
        if (!next.rejected) mutableState.update { it.copy(stage = next.stage) }
    }

    /** 外部能力/载荷变化后由接线层触发，仅在预检阶段自动重算。 */
    fun onCapabilitiesChanged() {
        if (mutableState.value.stage == TaskStage.PREP ||
            mutableState.value.stage == TaskStage.NOT_READY
        ) {
            evaluatePreflight()
        }
    }

    private fun evaluatePreflight() {
        val result = preflightEvaluator()
        mutableState.update { current ->
            val next = when (current.stage) {
                TaskStage.PREP -> if (result.allPassed) TaskStage.PREP_READY else TaskStage.NOT_READY
                TaskStage.NOT_READY -> if (result.allPassed) TaskStage.PREP_READY else TaskStage.NOT_READY
                // MANUAL 下重新预检只更新结论，不自动离开手动。
                else -> current.stage
            }
            current.copy(preflight = result, stage = next)
        }
    }

    /**
     * 应用刷新结果：保存全部任务、重算选中项，并按所选任务重建本地阶段。
     *
     * 本地处于 `PREP` / `PREP_READY` / `FLYTO` 时不覆盖阶段，避免刷新把自动作业或已通过的预检
     * 打回；选中项变化时清空上一任务的预检结论。
     */
    /**
     * 任务开始实际作业时回写「进行中」。
     *
     * 失败只记录日志，不阻断本地阶段；结束写入会再按合法迁移补发进行中。
     */
    private fun reportInProgress() {
        val job = mutableState.value.job ?: return
        if (job.status == JobStatus.IN_PROGRESS) return
        scope.launch {
            when (val result = api.updateStatus(job.jobId, JobStatus.IN_PROGRESS)) {
                is ApiResult.Ok -> {
                    val updated = result.value
                    mutableState.update { state ->
                        state.copy(
                            job = if (state.job?.jobId == updated.jobId) updated else state.job,
                            jobs = state.jobs.map {
                                if (it.jobId == updated.jobId) updated else it
                            },
                        )
                    }
                }

                is ApiResult.Err -> Log.w(TAG, "in-progress report failed: ${describe(result.error)}")
            }
        }
    }

    private fun JobStatus.isTerminal(): Boolean =
        this == JobStatus.COMPLETED || this == JobStatus.CANCELLED || this == JobStatus.REJECTED

    private fun applyJobs(jobs: List<GasJob>) {
        val current = mutableState.value
        val selectedId = TaskSelection.chooseSelection(jobs, current.selectedJobId)
        val selected = jobs.firstOrNull { it.jobId == selectedId }
        val multipleActive = jobs.count { it.status in ACTIVE_JOB_STATUSES } > 1
        val stage = when {
            selected == null -> TaskStage.IDLE
            current.job?.jobId == selected.jobId && current.stage in LOCKED_LOCAL_STAGES ->
                current.stage

            // 收尾写入是异步的：后端进入终态前不让轮询把「收尾完成」拉回更早阶段。
            current.job?.jobId == selected.jobId &&
                current.stage == TaskStage.FINALIZING &&
                !selected.status.isTerminal() -> current.stage

            else -> TaskSelection.stageFor(selected.status)
        }
        mutableState.update {
            it.copy(
                job = selected,
                jobs = jobs,
                selectedJobId = selectedId,
                multipleActive = multipleActive,
                stage = stage,
                preflight = if (current.selectedJobId == selectedId) it.preflight else null,
                sync = TaskSyncState.Ready,
            )
        }
        if (stage == TaskStage.PREP) evaluatePreflight()
    }

    /**
     * 切换选中任务。
     *
     * 本地处于预检或自动阶段时拒绝切换，避免丢失上下文；允许切换时按目标任务的
     * 后端状态重建本地阶段。返回 `false` 表示目标不存在或被拒绝。
     */
    fun selectJob(jobId: String): Boolean {
        val current = mutableState.value
        val target = current.jobs.firstOrNull { it.jobId == jobId } ?: return false
        if (target.jobId == current.selectedJobId) return true
        if (!TaskSelection.canSwitch(current.stage)) return false
        val stage = TaskSelection.stageFor(target.status)
        mutableState.update {
            it.copy(
                job = target,
                selectedJobId = target.jobId,
                stage = stage,
                preflight = null,
                sync = TaskSyncState.Ready,
            )
        }
        if (stage == TaskStage.PREP) evaluatePreflight()
        return true
    }

    private companion object {
        const val TAG = "GasTaskStore"

        /** 这些阶段的本地语义比后端状态更精确，刷新时保持不变。 */
        val LOCKED_LOCAL_STAGES = setOf(
            TaskStage.PREP,
            TaskStage.PREP_READY,
            TaskStage.FLYTO,
            TaskStage.SEARCH,
            TaskStage.FOLLOW,
        )

        val ACTIVE_JOB_STATUSES = setOf(
            JobStatus.DISPATCHED,
            JobStatus.ACCEPTED,
            JobStatus.IN_PROGRESS,
        )
    }
}
