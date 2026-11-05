package dji.sampleV5.aircraft.models

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dji.open.pilot.base.ui.ActivityConfig
import dji.open.pilot.base.ui.IRemoteActivityLifecycleListener
import dji.open.pilot.base.ui.NavigateAction
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ViewPageType
import dji.open.pilot.base.ui.model.ViewSide
import dji.open.pilot.base.ui.model.ViewType
import dji.open.pilot.client.manager.ServerState
import dji.open.pilot.client.manager.ui.NavigateManager
import dji.open.pilot.client.manager.ui.RemoteViewPageManager
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.models.NormalPluginVM.Companion.MARKER_IMAGE_IC_NAV_LOGO
import dji.sampleV5.aircraft.models.NormalPluginVM.Companion.MARKER_IMAGE_VIEW_M3M
import dji.sampleV5.aircraft.models.NormalPluginVM.Companion.MARKER_IMAGE_VIEW_M4E
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.common.register.InitSettings
import dji.v5.common.register.SDKLinKMode
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback
import dji.v5.network.DJINetworkManager
import dji.v5.utils.inner.SDKConfig
import dji.v5.ux.remoteview.RemoteViewHelper
import dji.v5.ux.util.Helper
import dji.v5.ux.util.ToastUtils

class MSDKManagerVM : ViewModel() {
    companion object {
        const val ACTIVITY_ACTIVITY_BUTTON_BC = 0x380055FF
        const val TEXT_BG_COLOR = 0xCC303030.toInt()

        val activityPageType = ViewPageType.ACTIVITY
        val openPluginBoxPageType = ViewPageType.FLOATING_BOX
    }

    // The data is held in livedata mode, but you can also save the results of the sdk callbacks any way you like.
    val lvRegisterState = MutableLiveData<Pair<Boolean, IDJIError?>>()
    val lvProductConnectionState = MutableLiveData<Pair<Boolean, Int>>()
    val lvProductChanges = MutableLiveData<Int>()
    val lvInitProcess = MutableLiveData<Pair<DJISDKInitEvent, Int>>()
    val lvDBDownloadProgress = MutableLiveData<Pair<Long, Long>>()
//    val lvServerState = MutableLiveData<ServerState>()
    var isInit = false
    private val imageViewAircraftNameList = listOf(MARKER_IMAGE_VIEW_M3M, MARKER_IMAGE_VIEW_M4E)
    private var currentImageIndex = 1
    val activityActivityButtonBC = 0x380055FF
    private val navigateActionToMsdkManger: NavigateAction = NavigateAction(
        NavigateAction.Value.PLUGIN_APP,
        "dji.sampleV5.aircraft.DJIAircraftMainActivity"
    )

    private val activityLifecycleListener: IRemoteActivityLifecycleListener = object :
        IRemoteActivityLifecycleListener {
        override fun onActivityCreated() {
            addInitialComponentsToActivity()
        }

        override fun onActivityStarted() {
        }

        override fun onActivityResumed() {
        }

        override fun onActivityPaused() {
        }

        override fun onActivityStopped() {
        }

        override fun onActivityDestroyed() {
        }
    }

    // Initialize and set the sdk callback, which is held internally by the sdk until destroy() is called
    fun initMobileSDK(appContext: Context) {
        val settings = InitSettings(SDKLinKMode.LOCAL)
        SDKManager.getInstance().init(appContext, settings, object :
            SDKManagerCallback {
            override fun onRegisterSuccess() {
                Log.i(TAG, "onRegisterSuccess")
                lvRegisterState.postValue(Pair(true, null))
            }

            override fun onRegisterFailure(error: IDJIError) {
                Log.e(TAG, "onRegisterFailure: error=$error")
                lvRegisterState.postValue(Pair(false, error))
            }

            override fun onProductDisconnect(productId: Int) {
                Log.i(TAG, "onProductDisconnect: productId=$productId")
                lvProductConnectionState.postValue(Pair(false, productId))
            }

            override fun onProductConnect(productId: Int) {
                Log.i(TAG, "onProductConnect: productId=$productId")
                lvProductConnectionState.postValue(Pair(true, productId))
            }

            override fun onProductChanged(productId: Int) {
                Log.i(TAG, "onProductChanged: productId=$productId")
                lvProductChanges.postValue(productId)
            }

            override fun onInitProcess(event: DJISDKInitEvent, totalProcess: Int) {
                Log.i(TAG, "onInitProcess: event=$event, totalProcess=$totalProcess")
                lvInitProcess.postValue(Pair(event, totalProcess))
                // Don't forget to call the registerApp()
                if (event == DJISDKInitEvent.INITIALIZE_COMPLETE) {
                    isInit = true
                    Log.i(TAG, "onInitProcess: INITIALIZE_COMPLETE, calling registerApp()")
                    SDKManager.getInstance().registerApp()
                }
            }

            @Deprecated("Deprecated")
            override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                Log.d(TAG, "onDatabaseDownloadProgress: current=$current, total=$total")
            }

            override fun onRemoteServerStateUpdated(serverState: ServerState) {
//                lvServerState.postValue(serverState)
                if (serverState == ServerState.CONNECTED) {
                    RemoteViewPageManager.getInstance().removeActivityLifecycleListener(activityLifecycleListener)
                    RemoteViewPageManager.getInstance().addActivityLifecycleListener(activityLifecycleListener)
                    Helper.pushSingleIconToRemote(MARKER_IMAGE_IC_NAV_LOGO, false)
                    addButtonJumpToMSDKManager()
//                    initAutoSensing()
                }
            }

            override fun onRemoteServerCompatibleError(error: IDJIError) {
                ToastUtils.showToast("SDK is not compatible with the server.\n $error")
            }
        })

        DJINetworkManager.getInstance().addNetworkStatusListener { isAvailable ->
            Log.i(TAG, "NetworkStatusListener: isAvailable=$isAvailable, isInit=$isInit, isRegistered=${SDKManager.getInstance().isRegistered}")
            if (isInit && isAvailable && !SDKManager.getInstance().isRegistered) {
                Log.i(TAG, "NetworkStatusListener: calling registerApp()")
                SDKManager.getInstance().registerApp()
            }
        }
        Log.i(TAG, "initMobileSDK: done")
    }

    fun destroyMobileSDK() {
        SDKManager.getInstance().destroy()
    }

    fun addButtonJumpToMSDKManager() {
        val helper = RemoteViewHelper.obtain(openPluginBoxPageType)
        helper.clearLayout()
        helper.setPageLayoutParamWrapContent()

        val container = helper.createConstraintLayout().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.MATCH_PARENT)
        }
        container.addToRoot(null)

        val jumpMsdkButton = helper.createButton().apply {
            setTextByResId(R.string.msdk_btn_jump_to_msdk)
            setWidth(0)
            setHeight(30)
            setBackground(activityActivityButtonBC)
        }
        container.addToRemote(jumpMsdkButton)

        helper.setConstraints(container.getId(), jumpMsdkButton.getId(), RemoteView.PARENT_ID, ViewSide.TOP, ViewSide.TOP, 50)
        helper.setConstraints(container.getId(), jumpMsdkButton.getId(), RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 4)
        helper.setConstraints(container.getId(), jumpMsdkButton.getId(), RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 4)
        helper.setConstraints(container.getId(), jumpMsdkButton.getId(), RemoteView.PARENT_ID, ViewSide.BOTTOM, ViewSide.BOTTOM, 8)

        jumpMsdkButton.setOnClickListener {
            NavigateManager.getInstance().navigateToPage(navigateActionToMsdkManger, object :
                CommonCallbacks.CompletionCallback {
                override fun onSuccess() {}
                override fun onFailure(error: IDJIError) {}
            })
        }
    }

    fun addInitialComponentsToActivity() {
        RemoteViewPageManager.getInstance().setActivityInfo(ActivityConfig("Open Pilot Activity", "退出全屏"))
        RemoteViewPageManager.getInstance().clearLayout(activityPageType)

        val textBgColor = 0xCC303030.toInt()
        val viewWidth = 400
        val viewHeight = 400

        val textSample = """
            这是一条可以翻滚的文案
            这是一条可以翻滚的文案
            这是一条可以翻滚的文案
            这是一条可以翻滚的文案
            这是一条可以翻滚的文案
            这是一条可以翻滚的文案
            这是一条可以翻滚的文案
            """.trimIndent()
        val textOutput = RemoteView(ViewType.TEXT_VIEW, textSample, 300, 30, textBgColor, "")
        RemoteViewPageManager.getInstance().addView(activityPageType, textOutput)

        val editText = RemoteView(ViewType.EDIT_TEXT, "111222333444555666777888999", 300, 40, textBgColor, "")

        RemoteViewPageManager.getInstance().addView(activityPageType, editText)

        val webView = RemoteView(ViewType.WEB_VIEW, "", 0, 0, textBgColor, "")
        webView.setWidth(0)
        webView.setHeight(400)
        RemoteViewPageManager.getInstance().addView(activityPageType, webView)

        val webButton = RemoteView(ViewType.BUTTON, "", 0, 40, textBgColor, "")
        webButton.setText("加载web")
        webButton.setWidth(viewWidth)
        RemoteViewPageManager.getInstance().addView(activityPageType, webButton)

        val imageView = RemoteView(ViewType.IMAGE_VIEW, "", 0, viewHeight, textBgColor, MARKER_IMAGE_VIEW_M3M)
        RemoteViewPageManager.getInstance().addView(activityPageType, imageView)

        val changeImageButton = RemoteView(ViewType.BUTTON, "", 0, 40, textBgColor, "")
        changeImageButton.setText("切换图片")
        changeImageButton.setWidth(viewWidth)
        RemoteViewPageManager.getInstance().addView(activityPageType, changeImageButton)

        RemoteViewPageManager.getInstance().setConstraints(activityPageType, imageView.getId(), RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 0)
        RemoteViewPageManager.getInstance().setConstraints(activityPageType, imageView.getId(), RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 0)
        RemoteViewPageManager.getInstance().setConstraints(activityPageType, imageView.getId(), RemoteView.PARENT_ID, ViewSide.TOP, ViewSide.TOP, 10)

        RemoteViewPageManager.getInstance().setConstraints(activityPageType, changeImageButton.getId(), RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 0)
        RemoteViewPageManager.getInstance().setConstraints(activityPageType, changeImageButton.getId(), RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 0)
        RemoteViewPageManager.getInstance().setConstraints(activityPageType, changeImageButton.getId(), imageView.getId(), ViewSide.TOP, ViewSide.BOTTOM, 8)

        RemoteViewPageManager.getInstance().setConstraints(activityPageType, webView.getId(), RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 0)
        RemoteViewPageManager.getInstance().setConstraints(activityPageType, webView.getId(), RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 0)
        RemoteViewPageManager.getInstance().setConstraints(activityPageType, webView.getId(), changeImageButton.getId(), ViewSide.TOP, ViewSide.BOTTOM, 15)

        RemoteViewPageManager.getInstance().setConstraints(activityPageType, webButton.getId(), RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 0)
        RemoteViewPageManager.getInstance().setConstraints(activityPageType, webButton.getId(), RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 0)
        RemoteViewPageManager.getInstance().setConstraints(activityPageType, webButton.getId(), webView.getId(), ViewSide.TOP, ViewSide.BOTTOM, 8)

        RemoteViewPageManager.getInstance().setConstraints(activityPageType, textOutput.getId(), RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 0)
        RemoteViewPageManager.getInstance().setConstraints(activityPageType, textOutput.getId(), RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 0)
        RemoteViewPageManager.getInstance().setConstraints(activityPageType, textOutput.getId(), webButton.getId(), ViewSide.TOP, ViewSide.BOTTOM, 40)

        RemoteViewPageManager.getInstance().setConstraints(activityPageType, editText.getId(), RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 0)
        RemoteViewPageManager.getInstance().setConstraints(activityPageType, editText.getId(), RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 0)
        RemoteViewPageManager.getInstance().setConstraints(activityPageType, editText.getId(), textOutput.getId(), ViewSide.TOP, ViewSide.BOTTOM, 20)


        webButton.setOnClickListener {
            val url = "https://www.dji.com/"
            RemoteViewPageManager.getInstance().updateView(activityPageType, webView.getId(), RemoteViewProperty.WEB_VIEW_URL, url)
        }

        changeImageButton.setOnClickListener {
            val nextImageName = imageViewAircraftNameList[currentImageIndex]
            currentImageIndex = (currentImageIndex + 1) % imageViewAircraftNameList.size
            RemoteViewPageManager.getInstance().updateView(
                activityPageType, imageView.getId(),
                RemoteViewProperty.IMAGE, nextImageName
            )
        }
    }
}