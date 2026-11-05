package dji.sample.flighttask

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import dji.sample.flighttask.data.CloudConfig
import dji.sample.flighttask.data.ResultCallback
import dji.sample.flighttask.manager.CloudManager
import dji.sample.flighttask.manager.TaskManager
import dji.sample.flighttask.models.ConnectionVM
import dji.sample.flighttask.models.TaskVM
import dji.sample.flighttask.models.WaylineTaskVM
import dji.sample.flighttask.pilot.RemoteControllerSerialNumberMonitor
import dji.sample.flighttask.util.globalViewModel

/**
 * 插件 App 入口：MSDK 初始化（REMOTE_PILOT）+ 全局 VM 组装。
 *
 * 组装职责：把悬浮窗按钮动作接到业务 VM，业务初始化统一收敛到 CONNECTED 后触发（连接门控）。
 */
class FlightTaskApplication : Application() {

    companion object {
        private const val DISCONNECT_GRACE_MS = 3_000L

        lateinit var instance: FlightTaskApplication
            private set
    }

    private val rcSerialNumberMonitor by lazy { RemoteControllerSerialNumberMonitor() }
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pilotConnected = false
    private var disconnectCleanup: Runnable? = null

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        com.cySdkyc.clx.Helper.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        val connectionVM = globalViewModel<ConnectionVM>()
        val taskVM = globalViewModel<TaskVM>()
        val waylineTaskVM = globalViewModel<WaylineTaskVM>()

        CloudManager.startHealthChecks(BuildConfig.FLIGHT_TASK_SERVER_URL)

        taskVM.onStartExecution = { job, callback ->
            waylineTaskVM.loadWayline(job, object : ResultCallback<Unit> {
                override fun onSuccess(data: Unit) {
                    waylineTaskVM.startMission(callback)
                }

                override fun onFailure(error: Throwable) {
                    callback.onFailure(error)
                }
            })
        }
        taskVM.onRetryFinalization = { job, callback -> waylineTaskVM.retryFinalization(job, callback) }
        taskVM.onPauseFinalization = { job -> waylineTaskVM.pauseFinalization(job) }

        // Pilot 连接后继续等待 RC SN，避免用错误身份初始化任何云端业务。
        connectionVM.onConnected {
            pilotConnected = true
            disconnectCleanup?.let(mainHandler::removeCallbacks)
            disconnectCleanup = null
            rcSerialNumberMonitor.await { rcSerialNumber ->
                CloudManager.init(
                    CloudConfig(
                        baseUrl = BuildConfig.FLIGHT_TASK_SERVER_URL,
                        rcSerialNumber = rcSerialNumber,
                    )
                )
                taskVM.start(this, rcSerialNumber)
                waylineTaskVM.onPilotConnected(this)
                TaskManager.startPolling()
                taskVM.render()
            }
        }
        connectionVM.onDisconnected {
            pilotConnected = false
            rcSerialNumberMonitor.cancel()
            TaskManager.stopPolling()
            taskVM.onPilotDisconnected()
            disconnectCleanup?.let(mainHandler::removeCallbacks)
            disconnectCleanup = Runnable {
                if (!pilotConnected) {
                    TaskManager.deactivateIdentity()
                    CloudManager.clear()
                    waylineTaskVM.onPilotDisconnected()
                }
            }.also { mainHandler.postDelayed(it, DISCONNECT_GRACE_MS) }
        }
        // 先挂好状态回调再初始化，避免快速连接时漏掉 CONNECTED。
        connectionVM.initSDK(this)
    }
}
