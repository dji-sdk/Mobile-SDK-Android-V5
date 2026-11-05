package dji.sample.flighttask

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import dji.open.pilot.client.manager.ServerState
import dji.open.pilot.base.ui.NavigateAction
import dji.open.pilot.client.manager.ui.NavigateManager
import dji.sample.flighttask.models.ConnectionVM
import dji.sample.flighttask.models.TaskVM
import dji.sample.flighttask.data.BackendConnectionState
import dji.sample.flighttask.pages.AssetFragment
import dji.sample.flighttask.pages.JobFragment
import dji.sample.flighttask.pages.TransferFragment
import dji.sample.flighttask.util.globalViewModels
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.v5.manager.aircraft.simulator.InitializationSettings
import dji.v5.manager.aircraft.simulator.SimulatorManager

/**
 * 插件 App 唯一全屏 Activity：任务 / 资源 / 传输 三 Tab + 顶部连接状态徽标。
 * Pilot 经插件入口、悬浮窗或通知的 Intent 拉起本 Activity（可携带目标 Tab、任务/资源 ID）。
 */
class PluginActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TAB = "extra_tab"
        const val TAB_JOB = 0
        const val TAB_ASSET = 1
        const val TAB_TRANSFER = 2
        private const val STATE_TAB = "state_tab"
    }

    private val connectionVM: ConnectionVM by globalViewModels()
    private val taskVM: TaskVM by globalViewModels()

    private lateinit var pilotBadge: TextView
    private lateinit var backendBadge: TextView
    private lateinit var pilotBadgeGroup: View
    private lateinit var pilotDot: View
    private lateinit var backendBadgeGroup: View
    private lateinit var backendDot: View
    private lateinit var tabButtons: List<Button>
    private var currentTab = TAB_JOB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyImmersiveMode()
        setContentView(R.layout.activity_plugin)

        pilotBadge = findViewById(R.id.tv_pilot_badge)
        backendBadge = findViewById(R.id.tv_backend_badge)
        pilotBadgeGroup = findViewById(R.id.badge_pilot)
        pilotDot = findViewById(R.id.dot_pilot)
        backendBadgeGroup = findViewById(R.id.badge_backend)
        backendDot = findViewById(R.id.dot_backend)
        findViewById<Button>(R.id.btn_back).setOnClickListener { navigateToPilot() }
        findViewById<TextView>(R.id.tv_title).setOnLongClickListener {
            showSimulatorDialog()
            true
        }
        tabButtons = listOf(
            findViewById(R.id.btn_tab_job),
            findViewById(R.id.btn_tab_asset),
            findViewById(R.id.btn_tab_transfer),
        )
        tabButtons.forEachIndexed { index, button ->
            button.setOnClickListener { switchTab(index) }
        }

        // 顶部全局栏连接徽标：三态常驻
        connectionVM.serverState.observe(this) { state ->
            val (textRes, style) = when (state) {
                ServerState.CONNECTED -> R.string.pilot_connected to BadgeStyle.CONNECTED
                ServerState.DISCONNECT -> R.string.pilot_disconnected to BadgeStyle.UNAVAILABLE
                ServerState.INCOMPATIBLE -> R.string.pilot_incompatible to BadgeStyle.UNAVAILABLE
                else -> R.string.pilot_checking to BadgeStyle.NEUTRAL
            }
            applyBadge(pilotBadgeGroup, pilotBadge, pilotDot, textRes, style)
        }
        taskVM.backendConnectionState.observe(this) { state ->
            val (textRes, style) = when (state) {
                BackendConnectionState.CONNECTED -> R.string.backend_connected to BadgeStyle.CONNECTED
                BackendConnectionState.UNAVAILABLE -> R.string.backend_unavailable to BadgeStyle.UNAVAILABLE
                BackendConnectionState.CHECKING -> R.string.backend_checking to BadgeStyle.NEUTRAL
            }
            applyBadge(backendBadgeGroup, backendBadge, backendDot, textRes, style)
        }

        switchTab(savedInstanceState?.getInt(STATE_TAB) ?: intent.getIntExtra(EXTRA_TAB, TAB_JOB))
    }

    /** 顶部徽章沿用既有状态，只统一背景、文字与圆点的视觉映射。 */
    private enum class BadgeStyle(val backgroundRes: Int, val contentColorRes: Int) {
        CONNECTED(R.drawable.bg_status_badge_connected, R.color.demo_top_bar_connected),
        UNAVAILABLE(R.drawable.bg_status_badge_disconnected, R.color.demo_top_bar_error),
        NEUTRAL(R.drawable.bg_status_badge_unregistered, R.color.demo_top_bar_neutral),
    }

    private fun applyBadge(group: View, label: TextView, dot: View, textRes: Int, style: BadgeStyle) {
        val color = ContextCompat.getColor(this, style.contentColorRes)
        group.setBackgroundResource(style.backgroundRes)
        label.setText(textRes)
        label.setTextColor(color)
        dot.backgroundTintList = ColorStateList.valueOf(color)
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveMode()
        if (connectionVM.serverState.value == ServerState.CONNECTED) {
            taskVM.ensurePolling()
            taskVM.refreshTasks()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveMode()
    }

    @Suppress("DEPRECATION")
    private fun applyImmersiveMode() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    private fun switchTab(index: Int) {
        currentTab = index
        val tag = "flight-task-tab-$index"
        val fragment = supportFragmentManager.findFragmentByTag(tag) ?: createFragment(index)
        tabButtons.forEachIndexed { i, button ->
            button.setTextColor(if (i == index) 0xFF087EA4.toInt() else 0xFF65717D.toInt())
        }
        supportFragmentManager.beginTransaction().apply {
            setReorderingAllowed(true)
            supportFragmentManager.fragments.forEach { hide(it) }
            if (fragment.isAdded) show(fragment) else add(R.id.fragment_container, fragment, tag)
        }.commit()
    }

    private fun createFragment(index: Int): Fragment = when (index) {
        TAB_ASSET -> AssetFragment()
        TAB_TRANSFER -> TransferFragment()
        else -> JobFragment()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_TAB, currentTab)
        super.onSaveInstanceState(outState)
    }

    private fun navigateToPilot() {
        NavigateManager.getInstance().navigateToPage(
            NavigateAction(NavigateAction.Value.PILOT_DEFAULT, ""),
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() = Unit
                override fun onFailure(error: IDJIError) {
                    Log.e("PluginActivity", "navigateToPilot failed: $error")
                    runOnUiThread { finish() }
                }
            },
        )
    }

    private fun showSimulatorDialog() {
        val simulatorManager = SimulatorManager.getInstance()
        val enabled = simulatorManager.isSimulatorEnabled
        AlertDialog.Builder(this)
            .setTitle(R.string.simulator_title)
            .setMessage(R.string.simulator_coordinates)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(if (enabled) R.string.simulator_disable else R.string.simulator_enable) { _, _ ->
                val callback = object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        runOnUiThread {
                            Toast.makeText(
                                this@PluginActivity,
                                if (enabled) R.string.simulator_disabled else R.string.simulator_enabled,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }

                    override fun onFailure(error: IDJIError) {
                        runOnUiThread {
                            Toast.makeText(
                                this@PluginActivity,
                                getString(
                                    if (enabled) R.string.simulator_disable_failed else R.string.simulator_enable_failed,
                                    error.description(),
                                ),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }
                if (enabled) {
                    simulatorManager.disableSimulator(callback)
                } else {
                    val settings = InitializationSettings.createInstance(
                        LocationCoordinate2D(22.5799591658722, 113.940530488361),
                        10,
                    )
                    simulatorManager.enableSimulator(settings, callback)
                }
            }
            .show()
    }
}
