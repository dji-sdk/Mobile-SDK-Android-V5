package dji.sample.patrol

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import dji.sample.patrol.data.DestinationPoint
import dji.sample.patrol.manager.DestinationArManager
import dji.sample.patrol.manager.DestinationMapManager
import dji.sample.patrol.manager.FlyToController
import dji.sample.patrol.manager.KeyManagerGeoProbe3D
import dji.open.pilot.client.manager.ServerState
import dji.open.pilot.base.ui.NavigateAction
import dji.open.pilot.client.manager.ui.NavigateManager
import dji.sample.patrol.models.ConnectionVM
import dji.sample.patrol.models.TaskVM
import dji.sample.patrol.data.BackendConnectionState
import dji.sample.patrol.pages.AssetFragment
import dji.sample.patrol.pages.JobFragment
import dji.sample.patrol.pages.TransferFragment
import dji.sample.patrol.util.globalViewModels
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError

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
        const val EXTRA_MARK_DESTINATION = "extra_mark_destination"
        private const val STATE_TAB = "state_tab"
        private const val REQUEST_NOTIFICATIONS = 1001
    }

    private val connectionVM: ConnectionVM by globalViewModels()
    private val taskVM: TaskVM by globalViewModels()

    private lateinit var pilotBadgeGroup: View
    private lateinit var pilotDot: View
    private lateinit var pilotBadge: TextView
    private lateinit var backendBadgeGroup: View
    private lateinit var backendDot: View
    private lateinit var backendBadge: TextView
    private lateinit var tabButtons: List<Button>
    private var currentTab = TAB_JOB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyImmersiveMode()
        setContentView(R.layout.activity_plugin)

        pilotBadgeGroup = findViewById(R.id.badge_pilot)
        pilotDot = findViewById(R.id.dot_pilot)
        pilotBadge = findViewById(R.id.tv_pilot_badge)
        backendBadgeGroup = findViewById(R.id.badge_backend)
        backendDot = findViewById(R.id.dot_backend)
        backendBadge = findViewById(R.id.tv_backend_badge)
        findViewById<Button>(R.id.btn_back).setOnClickListener { navigateToPilot() }
        tabButtons = listOf(
            findViewById(R.id.btn_tab_job),
            findViewById(R.id.btn_tab_asset),
            findViewById(R.id.btn_tab_transfer),
        )
        tabButtons.forEachIndexed { index, button ->
            button.setOnClickListener { switchTab(index) }
        }

        // 顶部全局栏连接徽标：常驻，外观三档（已连接 / 不可用 / 检测中）
        connectionVM.serverState.observe(this) { state ->
            val (textRes, style) = when (state) {
                ServerState.CONNECTED -> R.string.pilot_connected to BadgeStyle.CONNECTED
                ServerState.DISCONNECT -> R.string.pilot_disconnected to BadgeStyle.UNAVAILABLE
                // 不兼容是明确的失败态，按错误外观呈现，比中性灰更容易被注意到。
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
        requestNotificationPermission()
        if (intent.getBooleanExtra(EXTRA_MARK_DESTINATION, false)) showDestinationDialog()
    }

    /**
     * 连接状态徽章的三档外观。
     *
     * 背景与前景色都指向公共资源，渲染代码不写色值——以后调整样式只改 colors.xml / drawable。
     */
    private enum class BadgeStyle(
        @DrawableRes val backgroundRes: Int,
        @ColorRes val contentColorRes: Int,
    ) {
        CONNECTED(R.drawable.bg_status_badge_connected, R.color.demo_top_bar_connected),
        UNAVAILABLE(R.drawable.bg_status_badge_disconnected, R.color.demo_top_bar_error),
        NEUTRAL(R.drawable.bg_status_badge_unregistered, R.color.demo_top_bar_neutral),
    }

    /** 把状态文案与三档外观一次性刷到徽章上：背景、文字色、圆点色保持同一语义。 */
    private fun applyBadge(group: View, label: TextView, dot: View, textRes: Int, style: BadgeStyle) {
        val color = ContextCompat.getColor(this, style.contentColorRes)
        group.setBackgroundResource(style.backgroundRes)
        label.setText(textRes)
        label.setTextColor(color)
        dot.backgroundTintList = ColorStateList.valueOf(color)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_MARK_DESTINATION, false)) showDestinationDialog()
    }

    /** 目的地选取入口（面 A）：取当前飞行器位置或手动输入经纬高。 */
    private fun showDestinationDialog() {
        val options = arrayOf(
            getString(R.string.fw_destination_use_current),
            getString(R.string.fw_destination_input_hint),
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.fw_destination_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val location = KeyManagerGeoProbe3D.currentLocation()
                        if (location == null) {
                            toast(R.string.fw_destination_invalid)
                        } else {
                            val point = DestinationPoint(
                                location.latitude,
                                location.longitude,
                                location.altitude,
                            )
                            proposeDestination(point)
                        }
                    }
                    else -> showManualDestinationDialog()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showManualDestinationDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.fw_destination_input_hint)
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.fw_destination_title)
            .setView(container)
            .setPositiveButton(R.string.fw_btn_mark_destination) { _, _ ->
                val parts = input.text.toString().split(",").mapNotNull { it.trim().toDoubleOrNull() }
                if (parts.size < 3) {
                    toast(R.string.fw_destination_invalid)
                } else {
                    proposeDestination(DestinationPoint(parts[0], parts[1], parts[2]))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** 标注目的地并请求二次确认（FlyTo 属飞行安全操作）。 */
    private fun proposeDestination(point: DestinationPoint) {
        if (!point.isValid) {
            toast(R.string.fw_destination_invalid)
            return
        }
        if (!FlyToController.mark(point)) {
            toast(R.string.fw_destination_invalid)
            return
        }
        DestinationMapManager.mark(point, getString(R.string.fw_destination_title))
        DestinationArManager.project(point, getString(R.string.fw_destination_marked))
        AlertDialog.Builder(this)
            .setTitle(R.string.fw_flyto_confirm_title)
            .setMessage(R.string.fw_flyto_confirm_message)
            .setPositiveButton(R.string.fw_btn_confirm_flyto) { _, _ -> FlyToController.confirm() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun toast(resId: Int) = Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()

    /** 申请上传进度通知权限（Android 13+）；被拒绝时上传照常，仅降级为浮窗/页面展示。 */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
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

    /**
     * 切换底部 Tab。
     *
     * `index` 可能来自外部 Intent（[EXTRA_TAB]）或进程恢复（[STATE_TAB]），历史上「点位」Tab
     * 占用的序号 3 在移除该 Tab 后会越界；这里统一夹取到合法范围并回落到「任务」Tab，
     * 保证老调用方与进程恢复都不会崩溃。
     */
    private fun switchTab(index: Int) {
        currentTab = index.coerceIn(0, tabButtons.lastIndex)
        val tag = "patrol-tab-$currentTab"
        val fragment = supportFragmentManager.findFragmentByTag(tag) ?: createFragment(currentTab)
        tabButtons.forEachIndexed { i, button ->
            button.setTextColor(if (i == currentTab) 0xFF087EA4.toInt() else 0xFF65717D.toInt())
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
        // 独立启动（无 Pilot 宿主）或连接未就绪时 navigateToPage 无意义，给出提示而不是静默退出。
        if (connectionVM.serverState.value != ServerState.CONNECTED) {
            toast(R.string.return_pilot_disconnected)
            return
        }
        NavigateManager.getInstance().navigateToPage(
            NavigateAction(NavigateAction.Value.PILOT_DEFAULT, ""),
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() = Unit
                override fun onFailure(error: IDJIError) {
                    // PILOT_DEFAULT 在连接正常时通常不回调；此处兜底跳转失败（退回发起方）。
                    Log.e("PluginActivity", "navigateToPilot failed: $error")
                    runOnUiThread { finish() }
                }
            },
        )
    }

}
