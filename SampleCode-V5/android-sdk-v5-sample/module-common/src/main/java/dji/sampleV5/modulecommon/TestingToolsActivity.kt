package dji.sampleV5.modulecommon

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import dji.sampleV5.modulecommon.models.MSDKCommonOperateVm
import dji.sampleV5.modulecommon.views.MSDKInfoFragment

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_testing_tools)

        window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        // 设置监听防止系统UI获取焦点后进入到非全屏状态
        window.decorView.setOnSystemUiVisibilityChangeListener(){
            if (it and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_IMMERSIVE or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            }
        }

        supportFragmentManager.commit {
            replace(R.id.main_info_fragment_container, MSDKInfoFragment())
        }

        loadPages()
    }

    abstract fun loadPages()
}