package dji.sampleV5.aircraft

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dji.sampleV5.aircraft.models.BaseMainActivityVm
import dji.sampleV5.aircraft.models.MSDKInfoVm
import dji.sampleV5.aircraft.models.MSDKManagerVM
import dji.sampleV5.aircraft.models.globalViewModels
import dji.sampleV5.aircraft.util.Helper
import dji.sampleV5.aircraft.util.ToastUtils
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.PermissionUtil
import dji.v5.utils.common.StringUtils
import io.reactivex.rxjava3.disposables.CompositeDisposable
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
    private val permissionArray = arrayListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.KILL_BACKGROUND_PROCESSES,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    init {
        permissionArray.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

        }
    }

    private val baseMainActivityVm: BaseMainActivityVm by viewModels()
    private val msdkInfoVm: MSDKInfoVm by viewModels()
    private val msdkManagerVM: MSDKManagerVM by globalViewModels()
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val disposable = CompositeDisposable()

    abstract fun prepareUxActivity()

    abstract fun prepareTestingToolsActivity()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 有一些手机从系统桌面进入的时候可能会重启main类型的activity
        // 需要校验这种情况，业界标准做法，基本所有app都需要这个
        if (!isTaskRoot && intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN == intent.action) {

                finish()
                return

        }

        window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        initMSDKInfoView()
        observeSDKManager()
        checkPermissionAndRequest()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (checkPermission()) {
            handleAfterPermissionPermitted()
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkPermission()) {
            handleAfterPermissionPermitted()
        }
    }

    private fun handleAfterPermissionPermitted() {
        prepareTestingToolsActivity()
    }

    @SuppressLint("SetTextI18n")
    private fun initMSDKInfoView() {
        msdkInfoVm.msdkInfo.observe(this) {
            text_view_version.text = StringUtils.getResStr(R.string.sdk_version, it.SDKVersion + " " + it.buildVer)
            text_view_product_name.text = StringUtils.getResStr(R.string.product_name, it.productType.name)
            text_view_package_product_category.text = StringUtils.getResStr(R.string.package_product_category, it.packageProductCategory)
            text_view_is_debug.text = StringUtils.getResStr(R.string.is_sdk_debug, it.isDebug)
            text_core_info.text = it.coreInfo.toString()
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
        view_base_info.setOnClickListener {
            baseMainActivityVm.doPairing {
                showToast(it)
            }
        }
    }

    private fun observeSDKManager() {
        msdkManagerVM.lvRegisterState.observe(this) { resultPair ->
            val statusText: String?
            if (resultPair.first) {
                ToastUtils.showToast("Register Success")
                statusText = StringUtils.getResStr(this, R.string.registered)
                msdkInfoVm.initListener()
                handler.postDelayed({
                    prepareUxActivity()
                }, 5000)
            } else {
                showToast("Register Failure: ${resultPair.second}")
                statusText = StringUtils.getResStr(this, R.string.unregistered)
            }
            text_view_registered.text = StringUtils.getResStr(R.string.registration_status, statusText)
        }

        msdkManagerVM.lvProductConnectionState.observe(this) { resultPair ->
            showToast("Product: ${resultPair.second} ,ConnectionState:  ${resultPair.first}")
        }

        msdkManagerVM.lvProductChanges.observe(this) { productId ->
            showToast("Product: $productId Changed")
        }

        msdkManagerVM.lvInitProcess.observe(this) { processPair ->
            showToast("Init Process event: ${processPair.first.name}")
        }

        msdkManagerVM.lvDBDownloadProgress.observe(this) { resultPair ->
            showToast("Database Download Progress current: ${resultPair.first}, total: ${resultPair.second}")
        }
    }

    private fun showToast(content: String) {
        ToastUtils.showToast(content)

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

    private fun checkPermissionAndRequest() {
        if (!checkPermission()) {
            requestPermission()
        }
    }

    private fun checkPermission(): Boolean {
        for (i in permissionArray.indices) {
            if (!PermissionUtil.isPermissionGranted(this, permissionArray[i])) {
                return false
            }
        }
        return true
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        result?.entries?.forEach {
            if (it.value == false) {
                requestPermission()
                return@forEach
            }
        }
    }

    private fun requestPermission() {
        requestPermissionLauncher.launch(permissionArray.toArray(arrayOf()))
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        disposable.dispose()
    }
}