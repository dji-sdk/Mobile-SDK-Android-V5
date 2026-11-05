package com.dji.open.pilot.plugin.sampleitem.waypoint

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.globalViewModels
import com.dji.open.pilot.plugin.databinding.ActivityWaypointDemoBinding
import dji.v5.utils.common.DocumentUtil

/**
 * 航线任务 Demo 页：选择本地 KMZ 文件并导入 Pilot。
 * 若无全部文件访问权限，点击按钮时先进入系统授权页。
 */
class WaypointDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWaypointDemoBinding
    private val viewModel: WaypointDemoViewModel by globalViewModels()

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                val path = DocumentUtil.getPath(this, uri)
                if (path.isNullOrEmpty()) {
                    binding.tvResult.text = getString(R.string.waypoint_path_resolve_failed)
                } else {
                    viewModel.pushKMZFile(path)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWaypointDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvBack.setOnClickListener { finish() }
        binding.btnImportKmz.setOnClickListener { onImportClicked() }
        viewModel.pushResult.observe(this) { binding.tvResult.text = it }
    }

    private fun onImportClicked() {
        if (!hasAllFilesPermission()) {
            binding.tvResult.text = getString(R.string.waypoint_permission_denied)
            requestAllFilesPermission()
            return
        }
        showKMZFileChooser()
    }

    private fun hasAllFilesPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    private fun requestAllFilesPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:$packageName")
        )
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }
    }

    private fun showKMZFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(intent)
    }
}
