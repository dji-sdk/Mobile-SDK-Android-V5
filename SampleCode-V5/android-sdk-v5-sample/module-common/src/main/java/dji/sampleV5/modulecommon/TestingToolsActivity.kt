package dji.sampleV5.modulecommon

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.navigation.Navigation
import dji.sampleV5.modulecommon.models.MSDKCommonOperateVm
import dji.sampleV5.modulecommon.util.DJIToastUtil
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.sampleV5.modulecommon.views.MSDKInfoFragment
import dji.v5.ux.core.util.ViewUtil
import kotlinx.android.synthetic.main.activity_testing_tools.nav_host_fragment_container

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/7/23
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
abstract class TestingToolsActivity : AppCompatActivity() {

    protected val msdkCommonOperateVm: MSDKCommonOperateVm by viewModels()

    private val testToolsVM: TestToolsVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_testing_tools)

        window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        // 设置Listener防止系统UI获取焦点后进入到非全屏状态
        window.decorView.setOnSystemUiVisibilityChangeListener() {
            if (it and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            }
        }

        loadTitleView()

        DJIToastUtil.dJIToastLD = testToolsVM.djiToastResult
        testToolsVM.djiToastResult.observe(this) { result ->
            result?.msg?.let {
                ToastUtils.showToast(it)
            }
        }

        msdkCommonOperateVm.mainPageInfoList.observe(this) { list ->
            list.iterator().forEach {
                addDestination(it.vavGraphId)
            }
        }

        loadPages()
    }

    override fun onResume() {
        super.onResume()
        ViewUtil.setKeepScreen(this, true)
    }

    override fun onPause() {
        super.onPause()
        ViewUtil.setKeepScreen(this, false)
    }

    /**
     * 本activity的NavController，都是基于nav_host_fragment_container的
     */
    private fun addDestination(id: Int) {
        val v = Navigation.findNavController(nav_host_fragment_container).navInflater.inflate(id)
        Navigation.findNavController(nav_host_fragment_container).graph.addAll(v)
    }

    override fun onDestroy() {
        super.onDestroy()
        DJIToastUtil.dJIToastLD = null
    }

    open fun loadTitleView() {
        supportFragmentManager.commit {
            replace(R.id.main_info_fragment_container, MSDKInfoFragment())
        }
    }

    abstract fun loadPages()
}