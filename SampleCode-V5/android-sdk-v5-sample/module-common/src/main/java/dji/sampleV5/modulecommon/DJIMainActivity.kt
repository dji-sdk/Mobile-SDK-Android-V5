package dji.sampleV5.modulecommon

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import dji.sampleV5.modulecommon.models.BaseMainActivityVm
import dji.sampleV5.modulecommon.models.MSDKInfoVm
import dji.sampleV5.modulecommon.util.Helper
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.manager.interfaces.SDKManagerCallback
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.StringUtils
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/2/10
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
abstract class DJIMainActivity : AppCompatActivity() {

    val tag: String = LogUtils.getTag(this)

    private val baseMainActivityVm: BaseMainActivityVm by viewModels()
    protected val msdkInfoVm: MSDKInfoVm by viewModels()
    private val REQUEST_CODE: Int = 100

    abstract fun prepareUxActivity()

    abstract fun prepareTestingToolsActivity()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        initMSDKInfoView()
        requestPermission()
        registerApp()
        prepareTestingToolsActivity()
    }

    @SuppressLint("SetTextI18n")
    private fun initMSDKInfoView() {
        msdkInfoVm.msdkInfo.observe(this) {
            text_view_version.text = StringUtils.getResStr(R.string.sdk_version, it.SDKVersion + " " + it.buildVer)
            text_view_product_name.text = StringUtils.getResStr(R.string.product_name, it.productType.name)
            text_view_package_product_category.text = StringUtils.getResStr(R.string.package_product_category, it.packageProductCategory)
            text_view_is_debug.text = StringUtils.getResStr(R.string.is_debug, it.isDebug)
        }
        baseMainActivityVm.registerState.observe(this) {
            text_view_registered.text = StringUtils.getResStr(R.string.registration_status, it)
        }
        baseMainActivityVm.sdkNews.observe(this) {
            item_news_msdk.setTitle(StringUtils.getResStr(it.title))
            item_news_msdk.setDescription(StringUtils.getResStr(it.description))
            item_news_msdk.setDate(it.date)

            item_news_uxsdk.setTitle(StringUtils.getResStr(it.title))
            item_news_uxsdk.setDescription(StringUtils.getResStr(it.description))
            item_news_uxsdk.setDate(it.date)
        }

        icon_sdk_forum.setOnClickListener {
            Helper.startBrowser(this, StringUtils.getResStr(R.string.sdk_forum_url))
        }
        icon_release_node.setOnClickListener {
            Helper.startBrowser(this, StringUtils.getResStr(R.string.release_node_url))
        }
        icon_tech_support.setOnClickListener {
            Helper.startBrowser(this, StringUtils.getResStr(R.string.tech_support_url))
        }
    }

    private fun registerApp() {
        baseMainActivityVm.registerApp(this, object : SDKManagerCallback {
            override fun onRegisterSuccess() {
                ToastUtils.showToast("Register Success")
                msdkInfoVm.initListener()
                runOnUiThread {
                    prepareUxActivity()
                }
            }

            override fun onRegisterFailure(error: IDJIError?) {
                ToastUtils.showToast("Register Failure: (errorCode: ${error?.errorCode()}, description: ${error?.description()})")
            }

            override fun onProductDisconnect(product: Int) {
                ToastUtils.showToast("Product: $product Disconnect")
            }

            override fun onProductConnect(product: Int) {
                ToastUtils.showToast("Product: $product Connect")
            }

            override fun onProductChanged(product: Int) {
                ToastUtils.showToast("Product: $product Changed")
            }

            override fun onInitProcess(event: DJISDKInitEvent?, totalProcess: Int) {
                ToastUtils.showToast("Init Process event: ${event?.name}")
            }

            override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                ToastUtils.showToast("Database Download Progress current: $current, total: $total")
            }
        })
    }

    fun <T> enableDefaultLayout(cl: Class<T>) {
        enableShowCaseButton(default_layout_button, cl)
    }

    fun <T> enableWidgetList(cl: Class<T>) {
        enableShowCaseButton(widget_list_button, cl)
    }

    fun <T> enableTestingTools(cl: Class<T>) {
        enableShowCaseButton(testing_tool_button, cl)
    }

    private fun <T> enableShowCaseButton(view: View, cl: Class<T>) {
        view.isEnabled = true
        view.setOnClickListener {
            Intent(this, cl).also {
                startActivity(it)
            }
        }
    }

    private fun requestPermission() {
        val permissionExternalRead = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        val permissionExternalWrite = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val permissionRecordAudio = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        if (permissionExternalRead != PackageManager.PERMISSION_GRANTED
            && permissionExternalWrite != PackageManager.PERMISSION_GRANTED
            && permissionRecordAudio != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    "android.permission.READ_EXTERNAL_STORAGE",
                    "android.permission.WRITE_EXTERNAL_STORAGE",
                    "android.permission.RECORD_AUDIO"
                ), REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ToastUtils.showToast(this, "Request Permission Success")
            } else {
                ToastUtils.showToast(this, "Request Permission Failed")
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}