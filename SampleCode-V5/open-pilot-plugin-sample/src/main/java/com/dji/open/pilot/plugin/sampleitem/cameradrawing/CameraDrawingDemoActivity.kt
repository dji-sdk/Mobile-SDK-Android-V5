package com.dji.open.pilot.plugin.sampleitem.cameradrawing

import android.os.Bundle
import com.dji.open.pilot.plugin.common.widget.applyRoundedPreview
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.BaseDemoActivity
import com.dji.open.pilot.plugin.common.globalViewModels
import com.dji.open.pilot.plugin.databinding.ActivityCameraDrawingDemoBinding
import com.dji.open.pilot.plugin.sampleitem.cameradrawing.adapter.CameraDrawingMenuAdapter

/**
 * 图传绘制演示页。
 * 左侧菜单（矩形框/多边形/文字）+ 右侧效果预览图 + "查看效果"按钮。
 */
class CameraDrawingDemoActivity : BaseDemoActivity() {

    private lateinit var binding: ActivityCameraDrawingDemoBinding
    private val viewModel: CameraDrawingDemoViewModel by globalViewModels()

    private val menuAdapter by lazy {
        CameraDrawingMenuAdapter { type ->
            viewModel.selectedType.value = type
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraDrawingDemoBinding.inflate(layoutInflater)
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
