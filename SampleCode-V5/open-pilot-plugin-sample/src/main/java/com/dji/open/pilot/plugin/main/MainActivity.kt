package com.dji.open.pilot.plugin.main

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.databinding.ActivityMainBinding
import com.dji.open.pilot.plugin.main.ModuleAdapter
import com.dji.open.pilot.plugin.common.widget.GridSpacingItemDecoration
import com.dji.open.pilot.plugin.main.MainViewModel
import com.dji.open.pilot.plugin.common.globalViewModels
import dji.open.pilot.client.manager.ServerState

@Keep
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by globalViewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 防止从桌面重复启动
        if (!isTaskRoot && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
            && Intent.ACTION_MAIN == intent.action
        ) {
            finish()
            return
        }

        // 全屏沉浸
        window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        val adapter = ModuleAdapter(mainViewModel.moduleItems) { item ->
            item.demoClass?.let { clazz ->
                if (mainViewModel.isRegistered.value != true) {
                    Toast.makeText(this, R.string.toast_not_registered, Toast.LENGTH_SHORT).show()
                    return@ModuleAdapter
                }
                if (mainViewModel.serverState.value != ServerState.CONNECTED) {
                    Toast.makeText(this, R.string.toast_not_connected, Toast.LENGTH_SHORT).show()
                    return@ModuleAdapter
                }
                startActivity(Intent(this, clazz))
            }
        }
        val spacingPx = (10 * resources.displayMetrics.density).toInt()
        binding.rvModules.layoutManager = GridLayoutManager(this, 3)
        binding.rvModules.addItemDecoration(
            GridSpacingItemDecoration(spanCount = 3, spacing = spacingPx, includeEdge = true)
        )
        binding.rvModules.adapter = adapter
    }

    private fun observeViewModel() {
        mainViewModel.serverState.observe(this) { state ->
            val isConnected = state == ServerState.CONNECTED
            updateConnectionBadge(isConnected)
        }

        mainViewModel.isRegistered.observe(this) { registered ->
            updateRegisteredBadge(registered)
            updateCountryCodeBadge()
        }

        mainViewModel.countryCode.observe(this) {
            updateCountryCodeBadge()
        }
    }

    private fun updateConnectionBadge(connected: Boolean) {
        val color = ContextCompat.getColor(
            this, if (connected) R.color.demo_top_bar_connected else R.color.demo_top_bar_error
        )
        if (connected) {
            binding.badgeConnected.setBackgroundResource(R.drawable.bg_status_badge_connected)
            binding.tvConnected.setText(R.string.badge_connected)
        } else {
            binding.badgeConnected.setBackgroundResource(R.drawable.bg_status_badge_disconnected)
            binding.tvConnected.setText(R.string.badge_disconnected)
        }
        binding.tvConnected.setTextColor(color)
        binding.dotConnected.backgroundTintList = ColorStateList.valueOf(color)
    }

    private fun updateRegisteredBadge(registered: Boolean) {
        val color = ContextCompat.getColor(
            this, if (registered) R.color.demo_top_bar_blue else R.color.demo_top_bar_neutral
        )
        if (registered) {
            binding.badgeRegistered.setBackgroundResource(R.drawable.bg_status_badge_registered)
            binding.tvRegistered.setText(R.string.badge_registered)
        } else {
            binding.badgeRegistered.setBackgroundResource(R.drawable.bg_status_badge_unregistered)
            binding.tvRegistered.setText(R.string.badge_unregistered)
        }
        binding.tvRegistered.setTextColor(color)
        binding.dotRegistered.backgroundTintList = ColorStateList.valueOf(color)
    }

    /**
     * 刷新标题栏国家码徽章：已注册且已取到国家码时常显，否则隐藏。
     */
    private fun updateCountryCodeBadge() {
        val isRegistered = mainViewModel.isRegistered.value == true
        val code = mainViewModel.countryCode.value

        if (isRegistered && !code.isNullOrEmpty()) {
            binding.tvCountryCode.text = getString(R.string.badge_country_code_prefix, code)
            binding.dotCountryCode.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.demo_top_bar_blue)
            )
            binding.badgeCountryCode.visibility = View.VISIBLE
        } else {
            binding.badgeCountryCode.visibility = View.GONE
        }
    }
}
