package com.dji.open.pilot.plugin.sampleitem.dialogmanager

import android.os.Bundle
import com.dji.open.pilot.plugin.common.widget.applyRoundedPreview
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.BaseDemoActivity
import com.dji.open.pilot.plugin.common.globalViewModels
import com.dji.open.pilot.plugin.databinding.ActivityDialogDemoBinding
import com.dji.open.pilot.plugin.sampleitem.dialogmanager.adapter.DialogMenuAdapter

/**
 * 对话框演示页。
 * 左侧菜单（提示/确认/输入/单选/多选）+ 右侧预览图占位 + 参考代码路径 + "查看效果"按钮。
 */
class DialogDemoActivity : BaseDemoActivity() {

    private lateinit var binding: ActivityDialogDemoBinding
    private val viewModel: DialogDemoViewModel by globalViewModels()

    private val menuAdapter by lazy {
        DialogMenuAdapter { type ->
            viewModel.selectedType.value = type
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDialogDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ivPreview.applyRoundedPreview(showBorder = false)
        setupMenu()
        setupObservers()
        setupButtons()
    }

    private fun setupMenu() {
        binding.rvMenu.layoutManager = LinearLayoutManager(this)
        binding.rvMenu.adapter = menuAdapter
    }

    private fun setupObservers() {
        viewModel.selectedType.observe(this) { type ->
            if (type == null) {
                binding.tvEmptyHint.visibility = View.VISIBLE
                binding.layoutDetail.visibility = View.GONE
            } else {
                binding.tvEmptyHint.visibility = View.GONE
                binding.layoutDetail.visibility = View.VISIBLE
                menuAdapter.updateSelection(type)

                // 更新参考代码路径（源码位置提示）
                binding.tvReferenceCode.text = getString(
                    R.string.label_reference_interface,
                    type.interfaceRef,
                    type.sampleRef
                )

                // 效果预览图：previewResId 为 0 时暂无图，保持原样留空
                if (type.previewResId != 0) {
                    binding.ivPreview.setImageResource(type.previewResId)
                    binding.ivPreview.invalidateOutline()
                } else {
                    binding.ivPreview.setImageDrawable(null)
                }
            }
        }
    }

    private fun setupButtons() {
        binding.tvBack.setOnClickListener { finish() }
        binding.btnViewEffect.setOnClickListener {
            viewModel.drawAndNavigate()
        }
    }
}
