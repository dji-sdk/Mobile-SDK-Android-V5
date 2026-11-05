package com.dji.open.pilot.plugin.sampleitem.navigatemanager

import android.os.Bundle
import com.dji.open.pilot.plugin.common.widget.applyRoundedPreview
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.BaseDemoActivity
import com.dji.open.pilot.plugin.common.globalViewModels
import com.dji.open.pilot.plugin.databinding.ActivityNavigateDemoBinding
import com.dji.open.pilot.plugin.sampleitem.navigatemanager.adapter.NavigateMenuAdapter

/**
 * 页面跳转演示页。
 * 左侧菜单（8 种跳转目标）+ 右侧预览图占位 + 参考代码路径 + "查看效果"按钮。
 */
class NavigateDemoActivity : BaseDemoActivity() {

    private lateinit var binding: ActivityNavigateDemoBinding
    private val viewModel: NavigateDemoViewModel by globalViewModels()

    private val menuAdapter by lazy {
        NavigateMenuAdapter { type ->
            viewModel.selectedType.value = type
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigateDemoBinding.inflate(layoutInflater)
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
