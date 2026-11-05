package com.dji.open.pilot.plugin.main

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dji.open.pilot.plugin.main.PluginApplication
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.main.DemoStatus
import com.dji.open.pilot.plugin.main.ModuleItem
import com.dji.open.pilot.plugin.sampleitem.remoteviewpage.RemoteViewPageDemoActivity
import com.dji.open.pilot.plugin.sampleitem.armanager.ARDemoActivity
import com.dji.open.pilot.plugin.sampleitem.cameradrawing.CameraDrawingDemoActivity
import com.dji.open.pilot.plugin.sampleitem.dialogmanager.DialogDemoActivity
import com.dji.open.pilot.plugin.sampleitem.filetransfer.FileTransferDemoActivity
import com.dji.open.pilot.plugin.sampleitem.keymanager.KeyDemoActivity
import com.dji.open.pilot.plugin.sampleitem.mapmanager.MapDemoActivity
import com.dji.open.pilot.plugin.sampleitem.mediatask.MediaTaskDemoActivity
import com.dji.open.pilot.plugin.sampleitem.navigatemanager.NavigateDemoActivity
import com.dji.open.pilot.plugin.sampleitem.waypoint.WaypointDemoActivity
import com.dji.open.pilot.plugin.sampleitem.pipeline.PipelineDemoActivity
import com.dji.open.pilot.plugin.common.drawing.DrawingContext
import com.dji.open.pilot.plugin.common.drawing.FloatingWindowOwner
import com.dji.open.pilot.plugin.common.data.SharedDataManager
import com.dji.open.pilot.plugin.common.data.SharedApiDataManager
import com.dji.open.pilot.plugin.common.drawing.DrawingContext.Companion.OPEN_PILOT_PLUGIN_APP_ICON
import dji.v5.ux.util.RemoteLangUtils
import dji.open.pilot.base.manager.PluginAPPConfig
import dji.open.pilot.base.ui.NavigateAction
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.ViewPageType
import dji.open.pilot.base.ui.model.ViewSide
import dji.open.pilot.client.manager.ServerState
import dji.open.pilot.client.manager.ui.NavigateManager
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.common.register.InitSettings
import dji.v5.common.register.SDKLinKMode
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback
import dji.v5.utils.common.FileUtils
import dji.v5.ux.remoteview.RemoteViewHelper
import dji.v5.ux.util.Helper
import java.io.File

class MainViewModel : ViewModel() {

    companion object {
        private const val OWNER_TAG_MAIN = "main"
    }

    val serverState = MutableLiveData<ServerState>()

    /** true = 注册成功，false = 注册失败或尚未注册 */
    val isRegistered = MutableLiveData<Boolean>(false)

    /** 当前国家码文本，null 表示尚未获取。数据来源于 SharedApiDataManager。 */
    val countryCode = MutableLiveData<String?>(null)

    /** 20 个模块卡片数据，静态构建；DONE 卡片稳定排序置顶 */
    val moduleItems: List<ModuleItem> by lazy {
        buildModuleItems()
            .filter { it.status != DemoStatus.TODO }
            .sortedBy { if (it.status == DemoStatus.DONE) 0 else 1 }
    }

    private val TAG = "MainViewModel"

    fun initSDK(context: Context) {
        // 将接口型数据（国家码）变更映射到 LiveData，供 UI 观察（postValue 兼容后台线程回调）
        SharedApiDataManager.onCountryCodeChanged = { code ->
            countryCode.postValue(code)
        }

        val appConfig = PluginAPPConfig().apply {
            title = "Open Pilot Demo"
            iconResFileName = OPEN_PILOT_PLUGIN_APP_ICON
            homePageOnClickAction = NavigateAction(
                NavigateAction.Value.SELF_DEFINE,
                "com.dji.open.pilot.plugin.main.MainActivity"
            )
        }

        val settings = InitSettings(SDKLinKMode.REMOTE_PILOT).also {
            it.pluginAppConfig = appConfig
        }

        SDKManager.getInstance().init(context, settings, object : SDKManagerCallback {
            override fun onInitProcess(event: DJISDKInitEvent, totalProcess: Int) {
                Log.i(TAG, "onInitProcess: $event $totalProcess")
                if (event == DJISDKInitEvent.INITIALIZE_COMPLETE) {
                    SDKManager.getInstance().registerApp()
                }
            }

            override fun onRemoteServerStateUpdated(state: ServerState) {
                serverState.postValue(state)
                Log.i(TAG, "ServerState changed: $state")
                if (state == ServerState.CONNECTED) {
                    RemoteLangUtils.init(context.applicationContext)
                    SharedDataManager.startListening()
                    SharedApiDataManager.startListening()

                    for (res in DrawingContext.markerIconList) {
                        Helper.pushSingleIconToRemote(res, false)
                    }

                    addFloatBoxButton()
                }
            }

            override fun onRemoteServerCompatibleError(error: IDJIError) {
                Log.e(TAG, "Version compatible error: $error")
            }

            override fun onRegisterSuccess() {
                Log.i(TAG, "onRegisterSuccess")
                isRegistered.postValue(true)
            }

            override fun onRegisterFailure(error: IDJIError) {
                Log.e(TAG, "onRegisterFailure: $error")
                isRegistered.postValue(false)
            }

            override fun onProductDisconnect(productId: Int) {
                Log.i(TAG, "onProductDisconnect: $productId")
            }

            override fun onProductConnect(productId: Int) {
                Log.i(TAG, "onProductConnect: $productId")
            }

            override fun onProductChanged(productId: Int) {
                Log.i(TAG, "onProductChanged: $productId")
            }

            @Deprecated("Deprecated in SDKManagerCallback")
            override fun onDatabaseDownloadProgress(current: Long, total: Long) {}
        })
    }

    override fun onCleared() {
        super.onCleared()
        SharedApiDataManager.onCountryCodeChanged = null
        SharedApiDataManager.stopListening()
        SDKManager.getInstance().destroy()
    }

    private fun addFloatBoxButton() {
        val helper = RemoteViewHelper.obtain(ViewPageType.FLOATING_BOX)
        helper.clearLayout()
        helper.setPageLayoutParamWrapContent()

        val container = helper.createConstraintLayout().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.MATCH_PARENT)
        }
        container.addToRoot(null)

        val button = helper.createButton().apply {
            setTextByResId(R.string.btn_back_to_plugin_app)
            setWidth(0)
            setHeight(30)
            setBackground(0x380055FF)
        }
        container.addToRemote(button)

        helper.setConstraints(container.getId(), button.getId(), RemoteView.PARENT_ID, ViewSide.TOP,    ViewSide.TOP,    50)
        helper.setConstraints(container.getId(), button.getId(), RemoteView.PARENT_ID, ViewSide.START,  ViewSide.START,  4)
        helper.setConstraints(container.getId(), button.getId(), RemoteView.PARENT_ID, ViewSide.END,    ViewSide.END,    4)
        helper.setConstraints(container.getId(), button.getId(), RemoteView.PARENT_ID, ViewSide.BOTTOM, ViewSide.BOTTOM, 8)

        button.setOnClickListener {
            NavigateManager.getInstance().navigateToPage(
                NavigateAction(
                    NavigateAction.Value.SELF_DEFINE,
                    PluginApplication.currentActivityClassName
                ),
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {}
                    override fun onFailure(error: IDJIError) {
                        Log.e(TAG, "navigateToPage failed: $error")
                    }
                }
            )
        }

        FloatingWindowOwner.claim(OWNER_TAG_MAIN)
    }

    private fun buildModuleItems(): List<ModuleItem> = listOf(
        ModuleItem(R.drawable.ic_module_ui,       "IRemoteViewPageManager",     R.string.module_remote_view_page, R.string.desc_remote_view_page, status = DemoStatus.DONE, demoClass = RemoteViewPageDemoActivity::class.java),
        ModuleItem(R.drawable.ic_module_ar,       "IARManager",                 R.string.module_ar,                R.string.desc_ar,                status = DemoStatus.DONE, demoClass = ARDemoActivity::class.java),
        ModuleItem(R.drawable.ic_module_map,      "IMapWidgetManager",          R.string.module_map,               R.string.desc_map,               status = DemoStatus.DONE, demoClass = MapDemoActivity::class.java),
        ModuleItem(R.drawable.ic_module_dialog,   "IRemoteDialogManager",       R.string.module_dialog,            R.string.desc_dialog,            status = DemoStatus.DONE, demoClass = DialogDemoActivity::class.java),
        ModuleItem(R.drawable.ic_module_drawing,  "ICameraViewDrawingManager",  R.string.module_camera_drawing,    R.string.desc_camera_drawing,    status = DemoStatus.DONE, demoClass = CameraDrawingDemoActivity::class.java),
        ModuleItem(R.drawable.ic_module_navigate, "INavigateManager",           R.string.module_navigate,          R.string.desc_navigate,          status = DemoStatus.DONE, demoClass = NavigateDemoActivity::class.java),
        ModuleItem(R.drawable.ic_module_default,  "IWaypointMissionManager",    R.string.module_waypoint,          R.string.desc_waypoint,          status = DemoStatus.DONE, demoClass = WaypointDemoActivity::class.java),
        ModuleItem(R.drawable.ic_module_default,  "IKeyManager",                R.string.module_key,               R.string.desc_key,               status = DemoStatus.DONE, demoClass = KeyDemoActivity::class.java),
        ModuleItem(R.drawable.ic_module_default,  "IFileTransferManager",       R.string.module_file_transfer,     R.string.desc_file_transfer,     status = DemoStatus.DONE, demoClass = FileTransferDemoActivity::class.java),
        ModuleItem(R.drawable.ic_module_default,  "IRemotePipelineManager",     R.string.module_pipeline,          R.string.desc_pipeline,          status = DemoStatus.DONE, demoClass = PipelineDemoActivity::class.java),
        ModuleItem(R.drawable.ic_module_default,  "ICameraStreamManager",       R.string.module_camera_stream,     R.string.desc_camera_stream,     status = DemoStatus.TODO),
        ModuleItem(R.drawable.ic_module_default,  "IRemoteAreaCodeManager",     R.string.module_area_code,         R.string.desc_area_code,         status = DemoStatus.TODO),
        ModuleItem(R.drawable.ic_module_default,  "IRemoteServerConfigManager", R.string.module_language,          R.string.desc_language,          status = DemoStatus.TODO),
        ModuleItem(R.drawable.ic_module_default,  "IRemoteControlManager",      R.string.module_remote_control,    R.string.desc_remote_control,    status = DemoStatus.TODO),
        ModuleItem(R.drawable.ic_module_default,  "IDisconnectActionManager",   R.string.module_disconnect_action, R.string.desc_disconnect_action, status = DemoStatus.TODO),
        ModuleItem(R.drawable.ic_module_default,  "IPilotIntelligentManager",   R.string.module_intelligent,       R.string.desc_intelligent,       status = DemoStatus.TODO),
        ModuleItem(R.drawable.ic_module_default,  "IPilotModelingManager",      R.string.module_modeling,          R.string.desc_modeling,          status = DemoStatus.TODO),
        ModuleItem(R.drawable.ic_module_media_task, "IJNIMediaTaskManager", R.string.module_media_task, R.string.desc_media_task, status = DemoStatus.DONE, demoClass = MediaTaskDemoActivity::class.java),
        ModuleItem(R.drawable.ic_module_default,  "RemoteLinkManager",          R.string.module_link,              R.string.desc_link,              status = DemoStatus.TODO)
    )
}
