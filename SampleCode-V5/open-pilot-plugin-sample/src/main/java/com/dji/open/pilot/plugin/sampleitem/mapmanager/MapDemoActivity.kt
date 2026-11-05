package com.dji.open.pilot.plugin.sampleitem.mapmanager

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.BaseDemoActivity
import com.dji.open.pilot.plugin.common.globalViewModels
import com.dji.open.pilot.plugin.databinding.ActivityMapDemoBinding
import com.dji.open.pilot.plugin.sampleitem.mapmanager.adapter.MapMenuAdapter

/**
 * 地图控件演示页。
 * 左侧菜单（控件 / 批量 / 图层 / 元素）+ 右侧预览图轮播 + 参考代码路径 + "查看效果"按钮。
 * 地图状态监听（addMapStateListener/removeMapStateListener）常开，回调实时回显在悬浮窗结果卡片。
 */
class MapDemoActivity : BaseDemoActivity() {

    private lateinit var binding: ActivityMapDemoBinding
    private val viewModel: MapDemoViewModel by globalViewModels()

    private val menuAdapter by lazy {
        MapMenuAdapter { type ->
            viewModel.selectedType.value = type
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            if (type != null && type !in MapCaseType.VISIBLE_CASES) {
                viewModel.selectedType.value = MapCaseType.WIDGET
                return@observe
            }
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

                // 效果预览图：交给轮播组件，多张时可左右滑动，空列表时组件自动隐藏
                binding.carouselPreview.setImages(type.previewResIds)
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
