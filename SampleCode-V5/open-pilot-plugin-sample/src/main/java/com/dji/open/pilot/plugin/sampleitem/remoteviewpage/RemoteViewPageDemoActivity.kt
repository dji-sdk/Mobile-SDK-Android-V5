package com.dji.open.pilot.plugin.sampleitem.remoteviewpage

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.Keep
import androidx.recyclerview.widget.LinearLayoutManager
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.BaseDemoActivity
import com.dji.open.pilot.plugin.common.globalViewModels
import com.dji.open.pilot.plugin.databinding.ActivityRemoteViewPageDemoBinding
import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.adapter.ComponentMenuAdapter
import dji.v5.utils.inner.SDKConfig

@Keep
class RemoteViewPageDemoActivity : BaseDemoActivity() {

    private lateinit var binding: ActivityRemoteViewPageDemoBinding
    private val viewModel: RemoteViewPageDemoViewModel by globalViewModels()

    private val menuItems = ComponentType.values().toList()

    private lateinit var menuAdapter: ComponentMenuAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRemoteViewPageDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvBack.setOnClickListener { finish() }

        setupMenuList()
        setupCaseList()
        observeViewModel()
        // 子线程异步执行全量绘制，避免阻塞主线程（AIDL 调用无需主线程）
        Thread { viewModel.initAllLayouts() }.start()
        SDKConfig.getInstance().isExtendMode = true
    }

    private fun setupMenuList() {
        menuAdapter = ComponentMenuAdapter(menuItems) { type ->
            viewModel.loadCases(type)
        }
        binding.rvMenu.layoutManager = LinearLayoutManager(this)
        binding.rvMenu.adapter = menuAdapter
    }

    private fun setupCaseList() {
        // 配置 WebView
        binding.wvPreview.apply {
            settings.javaScriptEnabled = true
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            // 页面加载完成后，由 Android 侧把 App 当前语言权威地喂给 HTML，
            // 保证预览页文案与 App 其它界面语言完全一致（不依赖 WebView 的 navigator.language）。
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.evaluateJavascript(
                        "if (window.setLang) { setLang('${currentLangTag()}'); }",
                        null
                    )
                }
            }
        }

        binding.btnViewEffect.setOnClickListener {
            viewModel.drawAndNavigate()
        }
    }

    /** 返回 App 当前语言：中文返回 "zh"，其余一律 "en"。 */
    private fun currentLangTag(): String {
        val locale = resources.configuration.locales[0]
        return if (locale.language.equals("zh", ignoreCase = true)) "zh" else "en"
    }

    private fun observeViewModel() {
        // 订阅选中组件，切换右侧显示内容
        viewModel.selectedType.observe(this) { type ->
            if (type == null) {
                binding.tvEmptyHint.visibility = View.VISIBLE
                binding.layoutDetail.visibility = View.GONE
            } else {
                binding.tvEmptyHint.visibility = View.GONE
                binding.layoutDetail.visibility = View.VISIBLE
                binding.tvCompTitle.setText(type.titleRes)
                binding.tvCompDesc.setText(type.descRes)
                
                // 加载对应的 HTML 预览图
                val url = when(type) {
                    ComponentType.BUTTON -> "file:///android_asset/rvcase_button_preview.html"
                    ComponentType.TEXT_VIEW -> "file:///android_asset/rvcase_textview_preview.html"
                    ComponentType.PROGRESS_BAR -> "file:///android_asset/rvcase_progressbar_preview.html"
                    ComponentType.CHECK_BOX -> "file:///android_asset/rvcase_checkbox_preview.html"
                    ComponentType.IMAGE_VIEW -> "file:///android_asset/rvcase_imageview_preview.html"
                    ComponentType.SWITCH -> "file:///android_asset/rvcase_switch_preview.html"
                    ComponentType.EDIT_TEXT -> "file:///android_asset/rvcase_edittext_preview.html"
                    ComponentType.LAYOUT -> "file:///android_asset/rvcase_layout_preview.html"
                    ComponentType.WEB_VIEW -> "file:///android_asset/rvcase_webview_preview.html"
                    ComponentType.LIST_VIEW -> "file:///android_asset/rvcase_listview_preview.html"
                    ComponentType.SPINNER -> "file:///android_asset/rvcase_spinner_preview.html"
                    else -> "about:blank"
                }
                binding.wvPreview.loadUrl(url)

                // 更新参考代码路径
                binding.tvReferenceCode.text = getString(
                    R.string.label_reference_code,
                    type.componentRef,
                    type.drawerRef
                )

                // 更新左侧高亮
                menuAdapter.updateSelection(type)
            }
        }
    }
}
