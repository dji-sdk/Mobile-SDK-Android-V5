package com.dji.open.pilot.plugin.sampleitem.armanager

import android.os.Bundle
import com.dji.open.pilot.plugin.common.widget.applyRoundedPreview
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.BaseDemoActivity
import com.dji.open.pilot.plugin.common.globalViewModels
import com.dji.open.pilot.plugin.databinding.ActivityArDemoBinding
import com.dji.open.pilot.plugin.sampleitem.armanager.adapter.ARMenuAdapter
import dji.open.pilot.client.manager.ui.ARManager

/**
 * AR 投射演示页。
 * 左侧菜单（点/线/面/圆/雷达）+ 右侧效果预览图 + "查看效果"按钮。
 */
class ARDemoActivity : BaseDemoActivity() {

    private lateinit var binding: ActivityArDemoBinding
    private val viewModel: ARDemoViewModel by globalViewModels()

    private val menuAdapter by lazy {
        ARMenuAdapter { type ->
            viewModel.selectedType.value = type
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ivPreview.applyRoundedPreview(showBorder = false)
        setupMenu()
        setupObservers()
        setupButtons()
        viewModel.uploadDefaultConfig()
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

                // 效果预览图：previewResId 为 0（如雷达）时暂无图，保持原样留空
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
        binding.switchLiveStreamAr.setOnCheckedChangeListener { _, checked ->
            ARManager.getInstance().setLiveStreamAREnabled(checked)
        }
    }
}
