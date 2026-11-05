package dji.sampleV5.aircraft.models

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import dji.open.pilot.base.manager.OpenLocation
import dji.open.pilot.base.manager.intelligent.AutoSensingInfo
import dji.open.pilot.base.manager.intelligent.OpenDoubleRect
import dji.open.pilot.base.manager.intelligent.OpenPoint
import dji.open.pilot.base.manager.map.MapCircleOptions
import dji.open.pilot.base.manager.map.MapDrawerType
import dji.open.pilot.base.manager.map.MapElement
import dji.open.pilot.base.manager.map.MapElementColor
import dji.open.pilot.base.manager.map.MapMarkerOptions
import dji.open.pilot.base.manager.map.MapPolygonOptions
import dji.open.pilot.base.manager.map.MapPolylineOptions
import dji.open.pilot.base.manager.map.MapWidget
import dji.open.pilot.base.manager.map.MapWidgetItem
import dji.open.pilot.base.manager.map.MapWidgetItemType
import dji.open.pilot.base.manager.map.MapWidgetLayer
import dji.open.pilot.base.ui.IRemoteViewTextWatcher
import dji.open.pilot.base.ui.NavigateAction
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.cameraview.DrawingBoxInfo
import dji.open.pilot.base.ui.cameraview.DrawingPolygonInfo
import dji.open.pilot.base.ui.cameraview.DrawingTextInfo
import dji.open.pilot.base.ui.model.DividerConfig
import dji.open.pilot.base.ui.model.EllipsizeType
import dji.open.pilot.base.ui.model.FontStyleType
import dji.open.pilot.base.ui.model.GravityType
import dji.open.pilot.base.ui.model.InputType
import dji.open.pilot.base.ui.model.LayoutParam
import dji.open.pilot.base.ui.model.ListItemData
import dji.open.pilot.base.ui.model.ProgressStatusType
import dji.open.pilot.base.ui.model.RemoteViewProperty
import dji.open.pilot.base.ui.model.ScaleType
import dji.open.pilot.base.ui.model.ShapeDescriptor
import dji.open.pilot.base.ui.model.TextDecorationType
import dji.open.pilot.base.ui.model.ViewPageType
import dji.open.pilot.base.ui.model.ViewSide
import dji.open.pilot.base.ui.model.ViewType
import dji.open.pilot.base.ui.model.VisibilityType
import dji.open.pilot.base.ui.model.WidgetLayoutParam
import dji.open.pilot.client.manager.disconnectaction.RemoteDisconnectActionManager
import dji.open.pilot.client.manager.file.FileTransferManager
import dji.open.pilot.client.manager.intelligent.IntelligentAutoSensingListener
import dji.open.pilot.client.manager.intelligent.PilotIntelligentManager
import dji.open.pilot.client.manager.modeling.PilotModelingManager
import dji.open.pilot.client.manager.ui.CameraViewDrawingManager
import dji.open.pilot.client.manager.ui.MapWidgetManager
import dji.open.pilot.client.manager.ui.NavigateManager
import dji.open.pilot.client.manager.ui.RemoteViewPageManager
import dji.sampleV5.aircraft.R
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.common.Attitude
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.et.listen
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.waypoint3.WaylineExecutingInfoListener
import dji.v5.manager.aircraft.waypoint3.WaypointActionListener
import dji.v5.manager.aircraft.waypoint3.WaypointMissionManager
import dji.v5.manager.aircraft.waypoint3.model.WaylineExecutingInfo
import dji.v5.manager.aircraft.waypoint3.model.WaypointMissionExecuteState
import dji.v5.utils.common.ContextUtil
import dji.v5.utils.common.DJIExecutor
import dji.v5.utils.common.JsonUtil
import dji.v5.ux.remoteview.RemoteButton
import dji.v5.ux.remoteview.RemoteLinearLayout
import dji.v5.ux.remoteview.RemoteText
import dji.v5.ux.remoteview.RemoteViewHelper
import dji.v5.ux.util.RemoteLangUtils
import dji.v5.ux.util.ToastUtils
import java.io.File

class NormalPluginVM : DJIViewModel() {
    val activityPageType = ViewPageType.ACTIVITY
    val openPluginBoxPageType = ViewPageType.FLOATING_BOX
    private val activityActivityButtonBC = 0x380055FF
    private val semiTransparentGray = Color.argb(80, 128, 128, 128)

    companion object {
        const val MARKER_NORMAL_ICON = "icon_open_wind_mill_normal.webp"
        const val MARKER_CLICKED_ICON = "icon_open_wind_mill_clicked.webp"
        const val MARKER_IMAGE_VIEW_RC = "img_open_wind_imageView_RC.webp"
        const val MARKER_IMAGE_VIEW_M4E = "img_open_wind_imageView_m4e.webp"
        const val MARKER_IMAGE_VIEW_M3M = "img_open_pilot_device_m3m.webp"
        const val MARKER_IMAGE_IC_NAV_LOGO = "icon_dji_care_osmo.webp"
        const val MARKER_WIND_TURBINE = "map_mark_wind_turbine.webp"
    }

    val markerIconList = listOf(
        MARKER_NORMAL_ICON, MARKER_CLICKED_ICON, MARKER_IMAGE_VIEW_RC,
        MARKER_IMAGE_VIEW_M4E, MARKER_IMAGE_VIEW_M3M, MARKER_IMAGE_IC_NAV_LOGO, MARKER_IMAGE_IC_NAV_LOGO,MARKER_WIND_TURBINE
    )

    val mapElements = MutableLiveData<List<MapElement>>()
    val autoSensingInfo = MutableLiveData<AutoSensingInfo>()
    val aircraftAttitude = MutableLiveData<Attitude>()
    private val imageViewTestNameList = listOf(MARKER_IMAGE_VIEW_RC, MARKER_IMAGE_VIEW_M4E)
    private var currentImageIndex = 1
    private val intelligentAutoSensingListener = IntelligentAutoSensingListener {
        autoSensingInfo.postValue(it)
    }

    private var curMissionExecuteState: WaypointMissionExecuteState = WaypointMissionExecuteState.UNKNOWN
    @Volatile
    private lateinit var outputTextView: RemoteText
    @Volatile private var wayPointMissionPointExecuteState: String = ""
    @Volatile private var waylineExecuteState: String = ""
    @Volatile private var waylineExecuteAction: String = ""
    @Volatile private var perfTestRunning = false

    // ── 图层化场景演示用字段（图层A + 图层B，覆盖多图层管理） ──
    private var layerA: MapWidgetLayer? = null
    private var layerB: MapWidgetLayer? = null
    private val widgetsInLayerA = mutableListOf<MapWidget>()
    private val widgetsInLayerB = mutableListOf<MapWidget>()
    private var layerAVisible = true
    private var layerBVisible = true

    fun initListener() {
        PilotIntelligentManager.getInstance().addIntelligentSensingInfoListener(intelligentAutoSensingListener)
        FlightControllerKey.KeyAircraftAttitude.create().listen(this) {
            aircraftAttitude.postValue(it)
        }

        WaypointMissionManager.getInstance().addWaypointMissionExecuteStateListener {
            wayPointMissionPointExecuteState = "状态 : ${it.name}"
            curMissionExecuteState = it
            updateWayPointMissionExecuteState()
        }

        WaypointMissionManager.getInstance().addWaylineExecutingInfoListener(object :
            WaylineExecutingInfoListener {
            override fun onWaylineExecutingInfoUpdate(it: WaylineExecutingInfo) {
                waylineExecuteState = "航线名:  ${it.missionFileName}\n" +
                        "航点编号: ${it.currentWaypointIndex} \n" +
                        "航线编号: ${it.waylineID}"
                updateWayPointMissionExecuteState();
            }

            override fun onWaylineExecutingInterruptReasonUpdate(error: IDJIError?) {
                if (error != null) {
                    val originStr = wayPointMissionPointExecuteState
                    wayPointMissionPointExecuteState = "$originStr\n中断原因: ${error.errorCode()}"
                    updateWayPointMissionExecuteState()
                }
            }
        })

        WaypointMissionManager.getInstance().addWaypointActionListener(object :
            WaypointActionListener {
            @Deprecated("Deprecated in Java")
            override fun onExecutionStart(actionId: Int) {
//                waylineExecuteAction = "动作编号: ${actionId}  \n" + "动作组编号: null"
//                updateWayPointMissionExecuteState()
            }

            override fun onExecutionStart(actionGroup: Int, actionId: Int) {
                waylineExecuteAction = "动作编号: ${actionId}   \n" + "动作组编号: $actionGroup"
                updateWayPointMissionExecuteState()
            }

            @Deprecated("Deprecated in Java")
            override fun onExecutionFinish(actionId: Int, error: IDJIError?) {
//                waylineExecuteAction = "动作编号: ${actionId}   \n" + "动作组编号: null"
//                updateWayPointMissionExecuteState()
            }

            override fun onExecutionFinish(actionGroup: Int, actionId: Int, error: IDJIError?) {
                waylineExecuteAction = "动作编号: ${actionId}   \n" + "动作组编号: $actionGroup"
                updateWayPointMissionExecuteState()
            }

        })
    }

    override fun onCleared() {
        clearListener()
    }

    private fun clearListener() {
        PilotIntelligentManager.getInstance().removeIntelligentSensingInfoListener(intelligentAutoSensingListener)
        KeyManager.getInstance().cancelListen(this)
    }

    fun queryFileListFromRemote() {
        FileTransferManager.getInstance().queryFileListFromRemote(object :
            CommonCallbacks.CompletionCallbackWithParam<List<String>> {
            override fun onSuccess(t: List<String>) {
                ToastUtils.showToast("queryFileListFromRemote onSuccess ${JsonUtil.toJson(t)}\n")
            }

            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast("queryFileListFromRemote onFailure $error\n")
            }
        })
    }

    fun initSampleRemoteView() {
        val viewWidth = 140
        val textHeight = 15

        val helper = RemoteViewHelper.obtain(openPluginBoxPageType)
        helper.clearLayout()
        helper.setPageLayoutParam(800)

        val container = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setGravity(GravityType.CENTER)
        }
        container.addToRoot(null)
        container.setConstraintsOnRoot(ViewSide.START, ViewSide.START, 3)
        container.setConstraintsOnRoot(ViewSide.END, ViewSide.END, 3)
        container.setConstraintsOnRoot(ViewSide.TOP, ViewSide.TOP, 2)

        val textOutput = helper.createText().apply {
            setTextByResId(R.string.remote_sample_output_label)
            setWidth(viewWidth)
            setHeight(textHeight)
        }
        container.addToRemote(textOutput)

        outputTextView = helper.createText().apply {
            setText("")
            setWidth(viewWidth)
            setHeight(150)
        }
        container.addToRemote(outputTextView)

        val textButton = helper.createText().apply {
            setTextByResId(R.string.remote_sample_btn_test)
            setWidth(viewWidth)
            setHeight(textHeight)
        }
        container.addToRemote(textButton)

        val container1 = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        container.addToRemote(container1)

        val button1 = helper.createButton().apply {
            setTextByResId(R.string.remote_sample_btn_1)
            setWidth(70)
            setHeight(30)
            setBackground(semiTransparentGray)
        }
        container1.addToRemote(button1, WidgetLayoutParam().apply {
            setMargin(5, 0, 0, 4)
        })

        val button2 = helper.createButton().apply {
            setTextByResId(R.string.remote_sample_btn_2)
            setWidth(70)
            setHeight(30)
            setBackground(semiTransparentGray)
        }
        container1.addToRemote(button2, WidgetLayoutParam().apply {
            setMargin(2, 0, 0, 4)
        })

        val textSwitch = helper.createText().apply {
            setTextByResId(R.string.remote_sample_switch_test)
            setWidth(viewWidth)
            setHeight(textHeight)
        }
        container.addToRemote(textSwitch)

        val switchView = helper.createSwitch().apply {
            setTextByResId(R.string.remote_sample_switch_desc)
            setWidth(viewWidth)
            setHeight(30)
            setBackground(semiTransparentGray)
        }
        container.addToRemote(switchView)

        val textCheck = helper.createText().apply {
            setTextByResId(R.string.remote_sample_checkbox_test)
            setWidth(viewWidth)
            setHeight(textHeight)
        }
        container.addToRemote(textCheck)

        val checkBox = helper.createCheckBox().apply {
            setTextByResId(R.string.remote_sample_checkbox_option)
            setWidth(viewWidth)
            setHeight(30)
            setBackground(semiTransparentGray)
        }
        container.addToRemote(checkBox)

        val containerEdit = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        container.addToRemote(containerEdit)

        val textEdit = helper.createText().apply {
            setTextByResId(R.string.remote_sample_edit_text_label)
            setWidth(55)
            setHeight(textHeight)
        }
        containerEdit.addToRemote(textEdit)

        val editText = helper.createEditText().apply {
            setText("text")
            setWidth(63)
            setHeight(30)
            setBackground(semiTransparentGray)
        }
        containerEdit.addToRemote(editText)

        val imageView = helper.createImage().apply {
            setImageFileName(MARKER_IMAGE_VIEW_RC)
            setWidth(viewWidth)
            setHeight(100)
            setBackground(semiTransparentGray)
        }
        container.addToRemote(imageView)

        val changeImageButton = helper.createButton().apply {
            setTextByResId(R.string.remote_sample_change_image)
            setWidth(viewWidth)
            setHeight(30)
            setBackground(semiTransparentGray)
        }
        container.addToRemote(changeImageButton)

        val activityActionButton = helper.createButton().apply {
            setTextByResId(R.string.remote_sample_full_screen)
            setWidth(viewWidth)
            setHeight(30)
            setBackground(semiTransparentGray)
        }
        container.addToRemote(activityActionButton)

        val textWeb = helper.createText().apply {
            setTextByResId(R.string.remote_sample_web_test)
            setWidth(viewWidth)
            setHeight(25)
        }
        container.addToRemote(textWeb)

        val webView = helper.createWebView().apply {
            setWidth(viewWidth)
            setHeight(100)
            setBackground(semiTransparentGray)
        }
        container.addToRemote(webView)

        val webButton = helper.createButton().apply {
            setTextByResId(R.string.remote_sample_load_web)
            setWidth(viewWidth)
            setHeight(30)
            setBackground(semiTransparentGray)
        }
        container.addToRemote(webButton)

        val textSpinner = helper.createText().apply {
            setTextByResId(R.string.remote_sample_spinner_test)
            setWidth(viewWidth)
            setHeight(textHeight)
        }
        container.addToRemote(textSpinner)

        val spinner = helper.createSpinner().apply {
            setItemsByResIds(listOf(
                R.string.remote_sample_spinner_opt_1,
                R.string.remote_sample_spinner_opt_2,
                R.string.remote_sample_spinner_opt_3
            ))
            setSelectedIndex(0)
            setWidth(viewWidth)
            setHeight(30)
            setBackground(semiTransparentGray)
        }
        container.addToRemote(spinner)

        button1.setOnClickListener {
            outputTextView.setText(RemoteLangUtils.getString(R.string.remote_sample_cb_btn1_clicked))
            outputTextView.syncToRemote()
        }

        button2.setOnClickListener {
            outputTextView.setText(RemoteLangUtils.getString(R.string.remote_sample_cb_btn2_clicked))
            outputTextView.syncToRemote()
        }

        switchView.setOnCheckChangeListener { view, isChecked ->
            outputTextView.setText(
                RemoteLangUtils.getString(R.string.remote_sample_cb_switch)
                    .format(isChecked, view?.type)
            )
            outputTextView.syncToRemote()
        }

        checkBox.setOnCheckChangeListener { view, isChecked ->
            outputTextView.setText(
                RemoteLangUtils.getString(R.string.remote_sample_cb_checkbox)
                    .format(isChecked, view?.type)
            )
            outputTextView.syncToRemote()
        }

        editText.setOnTextChangeListener(object : IRemoteViewTextWatcher {
            override fun beforeTextChanged(s: String, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: String, start: Int, before: Int, count: Int) {
                outputTextView.setText(
                    RemoteLangUtils.getString(R.string.remote_sample_cb_edit_text).format(s)
                )
                outputTextView.syncToRemote()
            }

            override fun afterTextChanged(s: String) {}
        })

        webButton.setOnClickListener {
            val url = "https://www.dji.com/"
            outputTextView.setText(
                RemoteLangUtils.getString(R.string.remote_sample_cb_web_load).format(url)
            )
            outputTextView.syncToRemote()
            webView.setUrl(url)
            webView.syncToRemote()
        }

        spinner.setOnItemSelectedListener { index, item ->
            outputTextView.setText(
                RemoteLangUtils.getString(R.string.remote_sample_cb_spinner).format(item, index)
            )
            outputTextView.syncToRemote()
        }

        imageView.setOnClickListener {
            outputTextView.setText(RemoteLangUtils.getString(R.string.remote_sample_cb_image_clicked))
            outputTextView.syncToRemote()
        }

        changeImageButton.setOnClickListener {
            val nextImageName = imageViewTestNameList[currentImageIndex]
            currentImageIndex = (currentImageIndex + 1) % imageViewTestNameList.size
            imageView.setImageFileName(nextImageName)
            imageView.syncToRemote()
        }

        activityActionButton.setOnClickListener { startRemoteActivity() }

        // ── 列表视图测试 ─────────────────────────────────────────────────────
        val textList = helper.createText().apply {
            setTextByResId(R.string.remote_sample_list_test)
            setWidth(viewWidth)
            setHeight(textHeight)
        }
        container.addToRemote(textList)

        val listView = helper.createListView().apply {
            setWidth(viewWidth)
            setHeight(200)
        }
        container.addToRemote(listView)

        listView.setDividerConfig(DividerConfig(1, "#33FFFFFF", 32, 0, false))

        listView.setItemsBuilder {
            listOf(
                ListItemData.text("t1", RemoteLangUtils.getString(R.string.remote_sample_list_item_text)),
                ListItemData.iconText("it1", MARKER_NORMAL_ICON, RemoteLangUtils.getString(R.string.remote_sample_list_item_normal_icon)),
                ListItemData.iconText("it2", MARKER_CLICKED_ICON, RemoteLangUtils.getString(R.string.remote_sample_list_item_clicked_icon)),
                ListItemData.iconTwoLine("itl1", MARKER_IMAGE_VIEW_M4E,
                    RemoteLangUtils.getString(R.string.remote_sample_list_item_m4e_title),
                    RemoteLangUtils.getString(R.string.remote_sample_list_item_m4e_subtitle)),
                ListItemData.iconTwoLine("itl2", MARKER_IMAGE_VIEW_M3M,
                    RemoteLangUtils.getString(R.string.remote_sample_list_item_m3m_title),
                    RemoteLangUtils.getString(R.string.remote_sample_list_item_m3m_subtitle)),
                ListItemData.iconText("it3", MARKER_IMAGE_VIEW_RC, RemoteLangUtils.getString(R.string.remote_sample_list_item_rc)),
                ListItemData.text("t2", RemoteLangUtils.getString(R.string.remote_sample_list_item_divider)),
                ListItemData.iconText("it4", MARKER_IMAGE_IC_NAV_LOGO, RemoteLangUtils.getString(R.string.remote_sample_list_item_care)),
                ListItemData.iconTwoLine("itl3", MARKER_NORMAL_ICON,
                    RemoteLangUtils.getString(R.string.remote_sample_list_item_battery_title),
                    RemoteLangUtils.getString(R.string.remote_sample_list_item_battery_subtitle))
            )
        }

        listView.setOnItemClickListener { position, itemId ->
            outputTextView.setText(
                RemoteLangUtils.getString(R.string.remote_sample_cb_list_click).format(position, itemId)
            )
            outputTextView.syncToRemote()
        }

        val listInsertButton = helper.createButton().apply {
            setTextByResId(R.string.remote_sample_list_insert)
            setWidth(70)
            setHeight(30)
            setBackground(semiTransparentGray)
        }
        val listRemoveButton = helper.createButton().apply {
            setTextByResId(R.string.remote_sample_list_remove)
            setWidth(70)
            setHeight(30)
            setBackground(semiTransparentGray)
        }

        val listBtnRow = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        container.addToRemote(listBtnRow)
        listBtnRow.addToRemote(listInsertButton, WidgetLayoutParam().apply {
            setMargin(5, 0, 0, 4)
        })
        listBtnRow.addToRemote(listRemoveButton, WidgetLayoutParam().apply {
            setMargin(2, 0, 0, 4)
        })

        var insertCount = 0
        listInsertButton.setOnClickListener {
            insertCount++
            val newItemLabel = RemoteLangUtils.getString(R.string.remote_sample_list_new_item).format(insertCount)
            listView.insertItem(0, ListItemData.iconText("new_$insertCount", MARKER_CLICKED_ICON, newItemLabel))
            outputTextView.setText(
                RemoteLangUtils.getString(R.string.remote_sample_cb_list_inserted).format(insertCount)
            )
            outputTextView.syncToRemote()
        }

        listRemoveButton.setOnClickListener {
            listView.removeItem(0)
            outputTextView.setText(RemoteLangUtils.getString(R.string.remote_sample_cb_list_removed))
            outputTextView.syncToRemote()
        }

        val perfTestButton = helper.createButton().apply {
            setTextByResId(R.string.remote_sample_perf_test)
            setWidth(viewWidth)
            setHeight(30)
            setBackground(semiTransparentGray)
        }
        container.addToRemote(perfTestButton, WidgetLayoutParam().apply {
            setMargin(0, 4, 0, 4)
        })

        perfTestButton.setOnClickListener {
            if (perfTestRunning) {
                perfTestRunning = false
                RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, perfTestButton.getId(), RemoteViewProperty.TEXT, RemoteLangUtils.getString(R.string.remote_sample_perf_test))
                return@setOnClickListener
            }
            perfTestRunning = true
            RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, perfTestButton.getId(), RemoteViewProperty.TEXT, RemoteLangUtils.getString(R.string.remote_sample_perf_test_stop))
            RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, outputTextView.getId(), RemoteViewProperty.TEXT, RemoteLangUtils.getString(R.string.remote_sample_perf_test_starting))

            DJIExecutor.getExecutorFor(DJIExecutor.Purpose.IO).execute {
                val testFile = File(ContextUtil.getContext().filesDir, "perf_test.tmp")
                val dataSize = 500 * 1024 // 500KB
                val data = ByteArray(dataSize) { (it % 256).toByte() }
                val startTime = System.currentTimeMillis()
                var round = 0

                while (perfTestRunning) {
                    testFile.writeBytes(data)
                    testFile.readBytes()
                    round++

                    if (round % 2000 == 0) {
                        val elapsed = System.currentTimeMillis() - startTime
                        val avgMs = if (round > 0) elapsed.toDouble() / round else 0.0
                        val totalMB = (dataSize.toLong() * round * 2) / (1024 * 1024)
                        val speedMBs = if (elapsed > 0) totalMB * 1000.0 / elapsed else 0.0
                        RemoteViewPageManager.getInstance().updateView(
                            openPluginBoxPageType, outputTextView.getId(), RemoteViewProperty.TEXT,
                            RemoteLangUtils.getString(R.string.remote_sample_perf_test_running).format(round, elapsed, "%.2f".format(avgMs), "%.1f".format(speedMBs))
                        )
                    }
                }

                val totalTime = System.currentTimeMillis() - startTime
                val totalMB = (dataSize.toLong() * round * 2) / (1024 * 1024)
                val speedMBs = if (totalTime > 0) totalMB * 1000.0 / totalTime else 0.0
                testFile.delete()

                RemoteViewPageManager.getInstance().updateView(
                    openPluginBoxPageType, outputTextView.getId(), RemoteViewProperty.TEXT,
                    RemoteLangUtils.getString(R.string.remote_sample_perf_test_done).format(round, totalTime, totalMB, "%.1f".format(speedMBs))
                )
            }
        }
    }

    fun initRemoteViewPropertyDemo() {
        RemoteViewPageManager.getInstance().clearLayout(openPluginBoxPageType)

        RemoteViewPageManager.getInstance()
            .setPageLayoutParam(openPluginBoxPageType, LayoutParam(360))

        val demoComponentColor = Color.GRAY
        val demoTextWidth = 140
        val demoTextHeight = 15
        val demoIconSize = 30

        val demoLogoImageView = RemoteView(ViewType.IMAGE_VIEW, "", 80, 50, Color.TRANSPARENT, MARKER_IMAGE_VIEW_M4E)
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, demoLogoImageView)

        val fitCenterImageView = RemoteView(ViewType.IMAGE_VIEW, "", demoIconSize, demoIconSize, Color.TRANSPARENT, MARKER_IMAGE_IC_NAV_LOGO)
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, fitCenterImageView)

        val centerCropImageView = RemoteView(ViewType.IMAGE_VIEW, "", demoIconSize, demoIconSize, Color.TRANSPARENT, MARKER_IMAGE_IC_NAV_LOGO)
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, centerCropImageView)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, centerCropImageView.id, RemoteViewProperty.SCALE_TYPE, ScaleType.CENTER_CROP.value.toString())

        val fitEndImageView = RemoteView(ViewType.IMAGE_VIEW, "", demoIconSize, demoIconSize, Color.TRANSPARENT, MARKER_IMAGE_IC_NAV_LOGO)
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, fitEndImageView)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, fitEndImageView.id, RemoteViewProperty.SCALE_TYPE, ScaleType.FIT_START.value.toString())

        val centerInsideImageView = RemoteView(ViewType.IMAGE_VIEW, "", demoIconSize, demoIconSize, Color.TRANSPARENT, MARKER_IMAGE_IC_NAV_LOGO)
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, centerInsideImageView)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, centerInsideImageView.id, RemoteViewProperty.SCALE_TYPE, ScaleType.FIT_END.value.toString())

        val italicRedTextView = RemoteView(ViewType.TEXT_VIEW, "红色斜体，水平居中", demoTextWidth, demoTextHeight, demoComponentColor, "")
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, italicRedTextView)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, italicRedTextView.id, RemoteViewProperty.TEXT_SIZE, "10f")
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, italicRedTextView.id, RemoteViewProperty.TEXT_COLOR, Color.parseColor("#FF0000").toString())
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, italicRedTextView.id, RemoteViewProperty.FONT_STYLE_TYPE, FontStyleType.ITALIC.value.toString())
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, italicRedTextView.id, RemoteViewProperty.GRAVITY, GravityType.CENTER_HORIZONTAL.value.toString())

        val boldOrangeTextView = RemoteView(ViewType.TEXT_VIEW, "橙色粗体, 垂直居中", demoTextWidth, demoTextHeight * 2, demoComponentColor, "")
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, boldOrangeTextView)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, boldOrangeTextView.id, RemoteViewProperty.TEXT_SIZE, "12f")
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, boldOrangeTextView.id, RemoteViewProperty.TEXT_COLOR, Color.parseColor("#FFA500").toString())
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, boldOrangeTextView.id, RemoteViewProperty.FONT_STYLE_TYPE, FontStyleType.BOLD.value.toString())
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, boldOrangeTextView.id, RemoteViewProperty.GRAVITY, GravityType.CENTER_VERTICAL.value.toString())

        val boldItalicCenterTextView = RemoteView(ViewType.TEXT_VIEW, "粗斜体, 居中", demoTextWidth, demoTextHeight * 2, demoComponentColor, "")
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, boldItalicCenterTextView)
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, boldItalicCenterTextView.id, RemoteViewProperty.TEXT_SIZE, "14f")
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, boldItalicCenterTextView.id, RemoteViewProperty.FONT_STYLE_TYPE, FontStyleType.BOLD_ITALIC.value.toString())
        RemoteViewPageManager.getInstance().updateView(openPluginBoxPageType, boldItalicCenterTextView.id, RemoteViewProperty.GRAVITY, GravityType.CENTER.value.toString())

        val defaultStyleTextView = RemoteView(ViewType.TEXT_VIEW, "默认格式", demoTextWidth, demoTextHeight, demoComponentColor, "")
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, defaultStyleTextView)

        val demoActionButton = RemoteView(ViewType.BUTTON, "按键", 140, 30, activityActivityButtonBC, "")
        RemoteViewPageManager.getInstance().addView(openPluginBoxPageType, demoActionButton)

        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, demoLogoImageView.id, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 10)
        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, demoLogoImageView.id, RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 10)
        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, demoLogoImageView.id, RemoteView.PARENT_ID, ViewSide.TOP, ViewSide.TOP, 10)

        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, italicRedTextView.id, demoLogoImageView.id, ViewSide.TOP, ViewSide.BOTTOM, 10)
        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, italicRedTextView.id, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 5)
        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, italicRedTextView.id, RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 5)

        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, boldOrangeTextView.id, italicRedTextView.id, ViewSide.TOP, ViewSide.BOTTOM, 10)
        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, boldOrangeTextView.id, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 5)
        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, boldOrangeTextView.id, RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 5)

        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, boldItalicCenterTextView.id, boldOrangeTextView.id, ViewSide.TOP, ViewSide.BOTTOM, 10)
        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, boldItalicCenterTextView.id, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 5)
        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, boldItalicCenterTextView.id, RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 5)

        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, defaultStyleTextView.id, boldItalicCenterTextView.id, ViewSide.TOP, ViewSide.BOTTOM, 10)
        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, defaultStyleTextView.id, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 5)
        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, defaultStyleTextView.id, RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 5)

        //
        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, fitCenterImageView.id, defaultStyleTextView.id, ViewSide.TOP, ViewSide.BOTTOM, 10)
        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, fitCenterImageView.id, defaultStyleTextView.id, ViewSide.START, ViewSide.START, 5)

        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, centerCropImageView.id, defaultStyleTextView.id, ViewSide.TOP, ViewSide.BOTTOM, 10)
        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, centerCropImageView.id, defaultStyleTextView.id, ViewSide.END, ViewSide.END, 5)

        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, fitEndImageView.id, fitCenterImageView.id, ViewSide.TOP, ViewSide.BOTTOM, 10)
        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, fitEndImageView.id, defaultStyleTextView.id, ViewSide.START, ViewSide.START, 5)

        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, centerInsideImageView.id, fitCenterImageView.id, ViewSide.TOP, ViewSide.BOTTOM, 10)
        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, centerInsideImageView.id, defaultStyleTextView.id, ViewSide.END, ViewSide.END, 5)

        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, demoActionButton.id, RemoteView.PARENT_ID, ViewSide.START, ViewSide.START, 5)
        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, demoActionButton.id, RemoteView.PARENT_ID, ViewSide.END, ViewSide.END, 5)
        RemoteViewPageManager.getInstance().setConstraints(openPluginBoxPageType, demoActionButton.id, centerInsideImageView.id, ViewSide.TOP, ViewSide.BOTTOM, 20)
    }

    fun clearSampleRemoteView() {
        RemoteViewPageManager.getInstance().clearLayout(openPluginBoxPageType)
    }

    fun addMapWidget() {
        val widget1 = MapWidget().also { widget ->
            widget.drawerType = MapDrawerType.MARKER
            widget.markerOptions = MapMarkerOptions().also { options ->
                options.title = "风机1"
                options.normalIConFileName = MARKER_NORMAL_ICON
                options.clickedIConFileName = MARKER_CLICKED_ICON
                options.isClickable = true
                options.description = "风机1描述"
                options.point = OpenLocation(22.5807650, 113.9411710, 0.0)
            }
            widget.associatedItem = MapWidgetItem().also { item ->
                item.itemType = MapWidgetItemType.DIALOG
                item.title = "风机item1"
                item.content = "读取风机1的航线到航线库"
                item.btnContent = "插件航线读取"
                item.setOnClickListener {
                    jumpTo(NavigateAction(NavigateAction.Value.SELF_DEFINE, "dji.sampleV5.aircraft.DJIAircraftMainActivity"))
                }
            }
        }
        val widget11 = MapWidget().also { widget ->
            widget.drawerType = MapDrawerType.MARKER
            widget.markerOptions = MapMarkerOptions().also { options ->
                options.title = "风机2"
                options.normalIConFileName = MARKER_NORMAL_ICON
                options.clickedIConFileName = MARKER_CLICKED_ICON
                options.isClickable = true
                options.description = "风机2描述"
                options.point = OpenLocation(22.5817650, 113.9421710, 0.0)
            }
            widget.associatedItem = MapWidgetItem().also { item ->
                item.itemType = MapWidgetItemType.DIALOG
                item.title = "风机item2"
                item.content = "读取风机2的航线到航线库"
                item.btnContent = "插件航线读取"
                item.setOnClickListener {
                    jumpTo(NavigateAction(NavigateAction.Value.SELF_DEFINE, "dji.sampleV5.aircraft.DJIAircraftMainActivity"))
                }
            }
        }
        val widget2 = MapWidget().also { widget ->
            widget.drawerType = MapDrawerType.POLYLINE
            widget.polylineOptions = MapPolylineOptions().also { options ->
                options.points = mutableListOf<OpenLocation>().apply {
                    add(OpenLocation(22.5830650, 113.9434710, 0.0))
                    add(OpenLocation(22.5840650, 113.9444710, 0.0))
                    add(OpenLocation(22.5880650, 113.9454710, 0.0))
                }
                options.width = 8F
                options.color = Color.GREEN
                options.isClickable = false
            }
        }
        val widget3 = MapWidget().also { widget ->
            widget.drawerType = MapDrawerType.POLYGON
            widget.polygonOptions = MapPolygonOptions().also { options ->
                options.points = mutableListOf<OpenLocation>().apply {
                    add(OpenLocation(22.584424, 113.946115, 0.0))
                    add(OpenLocation(22.583306, 113.945846, 0.0))
                    add(OpenLocation(22.583273, 113.947589, 0.0))
                }
                options.width = 8F
                options.strokeColor = Color.GREEN
                options.fillColor = Color.RED
                options.isVisible = true
            }
        }
        val widget4 = MapWidget().also { widget ->
            widget.drawerType = MapDrawerType.POLYGON
            widget.polygonOptions = MapPolygonOptions().also { options ->
                options.points = mutableListOf<OpenLocation>().apply {
                    add(OpenLocation(22.586424, 113.946115, 0.0))
                    add(OpenLocation(22.585306, 113.945846, 0.0))
                    add(OpenLocation(22.584273, 113.947589, 0.0))
                }
                options.width = 4F
                options.strokeColor = Color.RED
                options.isVisible = true
            }
        }
        val widget5 = MapWidget().also { widget ->
            widget.drawerType = MapDrawerType.CIRCLE
            widget.circleOptions = MapCircleOptions().also { options ->
                options.center = OpenLocation(22.590424, 113.946115, 0.0)
                options.radius = 1000.0
                options.strokeWidth = 8F
                options.strokeColor = Color.RED
                options.isDotted = true
                options.dashLength = 8F
            }
        }
        MapWidgetManager.getInstance().addMapWidget(widget1)
        MapWidgetManager.getInstance().addMapWidget(widget11)
        MapWidgetManager.getInstance().addMapWidget(widget2)
        MapWidgetManager.getInstance().addMapWidget(widget3)
        MapWidgetManager.getInstance().addMapWidget(widget4)
        MapWidgetManager.getInstance().addMapWidget(widget5)
    }

    // ════════════════════════════════════════════════════════════════
    //  图层化接口演示：图层A + 图层B
    //  覆盖：addLayer / setLayerVisible / clearLayer / removeLayer
    //        addMapWidget(cb) / removeMapWidget(cb) / addMapWidgets
    //        updateMapWidgets / removeMapWidgetsByIds
    //        queryMapWidgets / queryMapWidgetsByLayer
    // ════════════════════════════════════════════════════════════════

    /**
     * 创建两个 MapWidgetLayer（图层A 和 图层B），不同 zIndex 演示叠放顺序。
     */
    fun addLayerScene() {
        layerA = MapWidgetLayer("LayerA").apply { zIndex = 50 }
        layerB = MapWidgetLayer("LayerB").apply { zIndex = 30 }
        MapWidgetManager.getInstance().addLayer(layerA!!, simpleCallback("addLayer(LayerA)"))
        MapWidgetManager.getInstance().addLayer(layerB!!, simpleCallback("addLayer(LayerB)"))
    }

    /**
     * 批量往 LayerA 添加 widget：4 个 MARKER + 1 个 POLYGON。
     */
    fun addBatchWidgetsToLayerA() {
        val layerId = layerA?.id
        if (layerId == null) {
            notifyUser("请先点【创建图层场景】\n")
            return
        }
        widgetsInLayerA.clear()
        val markerPoints = listOf(
            Triple(22.5810650, 113.9410710, "MarkerA-1"),
            Triple(22.5815650, 113.9415710, "MarkerA-2"),
            Triple(22.5820650, 113.9420710, "MarkerA-3"),
            Triple(22.5825650, 113.9425710, "MarkerA-4")
        )
        markerPoints.forEach { (lat, lng, n) ->
            widgetsInLayerA.add(MapWidget().apply {
                name = n
                setLayerId(layerId)
                drawerType = MapDrawerType.MARKER
                markerOptions = MapMarkerOptions().apply {
                    title = n
                    normalIConFileName = MARKER_NORMAL_ICON
                    clickedIConFileName = MARKER_CLICKED_ICON
                    isClickable = true
                    point = OpenLocation(lat, lng, 0.0)
                }
            })
        }
        widgetsInLayerA.add(MapWidget().apply {
            name = "PolygonA-1"
            setLayerId(layerId)
            drawerType = MapDrawerType.POLYGON
            polygonOptions = MapPolygonOptions().apply {
                points = mutableListOf(
                    OpenLocation(22.5808650, 113.9408710, 0.0),
                    OpenLocation(22.5830650, 113.9408710, 0.0),
                    OpenLocation(22.5830650, 113.9430710, 0.0),
                    OpenLocation(22.5808650, 113.9430710, 0.0)
                )
                width = 4F
                strokeColor = Color.RED
                fillColor = 0x33FF0000
                isVisible = true
            }
        })
        MapWidgetManager.getInstance().addMapWidgets(widgetsInLayerA,
            simpleCallback("addMapWidgets(LayerA ${widgetsInLayerA.size}个)"))
    }

    /**
     * 批量往 LayerB 添加 widget：5 个 MARKER + 1 个 POLYLINE + 1 个 CIRCLE。
     */
    fun addBatchWidgetsToLayerB() {
        val layerId = layerB?.id
        if (layerId == null) {
            notifyUser("请先点【创建图层场景】\n")
            return
        }
        widgetsInLayerB.clear()
        val markerPoints = listOf(
            Triple(22.5840650, 113.9440710, "MarkerB-1"),
            Triple(22.5845650, 113.9445710, "MarkerB-2"),
            Triple(22.5850650, 113.9450710, "MarkerB-3"),
            Triple(22.5855650, 113.9455710, "MarkerB-4"),
            Triple(22.5860650, 113.9460710, "MarkerB-5")
        )
        markerPoints.forEach { (lat, lng, n) ->
            widgetsInLayerB.add(MapWidget().apply {
                name = n
                setLayerId(layerId)
                drawerType = MapDrawerType.MARKER
                markerOptions = MapMarkerOptions().apply {
                    title = n
                    normalIConFileName = MARKER_WIND_TURBINE
                    clickedIConFileName = MARKER_WIND_TURBINE
                    isClickable = true
                    point = OpenLocation(lat, lng, 0.0)
                }
            })
        }
        widgetsInLayerB.add(MapWidget().apply {
            name = "PolylineB-1"
            setLayerId(layerId)
            drawerType = MapDrawerType.POLYLINE
            polylineOptions = MapPolylineOptions().apply {
                points = mutableListOf(
                    OpenLocation(22.5840650, 113.9440710, 0.0),
                    OpenLocation(22.5850650, 113.9450710, 0.0),
                    OpenLocation(22.5860650, 113.9460710, 0.0)
                )
                width = 6F
                color = Color.BLUE
            }
        })
        widgetsInLayerB.add(MapWidget().apply {
            name = "CircleB-1"
            setLayerId(layerId)
            drawerType = MapDrawerType.CIRCLE
            circleOptions = MapCircleOptions().apply {
                center = OpenLocation(22.5870650, 113.9470710, 0.0)
                radius = 200.0
                strokeWidth = 4F
                strokeColor = Color.GREEN
                isDotted = true
                dashLength = 6F
            }
        })
        MapWidgetManager.getInstance().addMapWidgets(widgetsInLayerB,
            simpleCallback("addMapWidgets(LayerB ${widgetsInLayerB.size}个)"))
    }

    /**
     * 批量更新 LayerA 中所有 MARKER 的位置（演示就地更新无闪烁 + 不可变字段保护）。
     */
    fun updateBatchWidgetsInLayerA() {
        if (widgetsInLayerA.isEmpty()) {
            notifyUser("LayerA 为空，请先添加 widget\n")
            return
        }
        widgetsInLayerA.forEach { w ->
            if (w.drawerType == MapDrawerType.MARKER) {
                val old = w.markerOptions.point
                w.markerOptions.point = OpenLocation(old.latitude + 0.0002, old.longitude + 0.0002, old.altitude)
            }
        }
        MapWidgetManager.getInstance().updateMapWidgets(widgetsInLayerA,
            simpleCallback("updateMapWidgets(LayerA ${widgetsInLayerA.size}个)"))
    }

    fun toggleLayerAVisible() {
        val layerId = layerA?.id ?: run {
            notifyUser("请先点【创建图层场景】\n")
            return
        }
        layerAVisible = !layerAVisible
        MapWidgetManager.getInstance().setLayerVisible(layerId, layerAVisible,
            simpleCallback("setLayerVisible(LayerA=$layerAVisible)"))
    }

    fun toggleLayerBVisible() {
        val layerId = layerB?.id ?: run {
            notifyUser("请先点【创建图层场景】\n")
            return
        }
        layerBVisible = !layerBVisible
        MapWidgetManager.getInstance().setLayerVisible(layerId, layerBVisible,
            simpleCallback("setLayerVisible(LayerB=$layerBVisible)"))
    }

    fun queryAllWidgets() {
        MapWidgetManager.getInstance().queryMapWidgets(object :
            CommonCallbacks.CompletionCallbackWithParam<List<MapWidget>> {
            override fun onSuccess(t: List<MapWidget>) {
                val names = t.joinToString(",") { it.name.ifEmpty { "<no-name>" } }
                notifyUser("queryMapWidgets onSuccess 总数=${t.size}, names=[$names]\n")
            }
            override fun onFailure(error: IDJIError) {
                notifyUser("queryMapWidgets onFailure: ${error.errorCode()} ${error.description()}\n")
            }
        })
    }

    fun queryWidgetsInLayerA() {
        val layerId = layerA?.id ?: run {
            notifyUser("请先点【创建图层场景】\n")
            return
        }
        MapWidgetManager.getInstance().queryMapWidgetsByLayer(layerId, object :
            CommonCallbacks.CompletionCallbackWithParam<List<MapWidget>> {
            override fun onSuccess(t: List<MapWidget>) {
                val names = t.joinToString(",") { it.name.ifEmpty { "<no-name>" } }
                notifyUser("queryMapWidgetsByLayer(LayerA) onSuccess 数量=${t.size}, names=[$names]\n")
            }
            override fun onFailure(error: IDJIError) {
                notifyUser("queryMapWidgetsByLayer onFailure: ${error.errorCode()} ${error.description()}\n")
            }
        })
    }

    fun clearLayerA() {
        val layerId = layerA?.id ?: run {
            notifyUser("请先点【创建图层场景】\n")
            return
        }
        MapWidgetManager.getInstance().clearLayer(layerId, simpleCallback("clearLayer(LayerA)"))
        widgetsInLayerA.clear()
    }

    fun removeLayerA() {
        val layerId = layerA?.id ?: run {
            notifyUser("请先点【创建图层场景】\n")
            return
        }
        MapWidgetManager.getInstance().removeLayer(layerId, simpleCallback("removeLayer(LayerA)"))
        widgetsInLayerA.clear()
        layerA = null
    }

    /**
     * 用带 callback 版本添加单个 widget（与老 addMapWidget(widget) 无 callback 版形成对比）。
     */
    fun addSingleWidgetWithCallback() {
        val layerId = layerA?.id ?: run {
            notifyUser("请先点【创建图层场景】\n")
            return
        }
        val widget = MapWidget().apply {
            name = "MarkerA-Extra"
            setLayerId(layerId)
            drawerType = MapDrawerType.MARKER
            markerOptions = MapMarkerOptions().apply {
                title = "MarkerA-Extra"
                normalIConFileName = MARKER_NORMAL_ICON
                clickedIConFileName = MARKER_CLICKED_ICON
                isClickable = true
                point = OpenLocation(22.5800650, 113.9400710, 0.0)
            }
        }
        MapWidgetManager.getInstance().addMapWidget(widget, simpleCallback("addMapWidget(${widget.name})"))
        widgetsInLayerA.add(widget)
    }

    /**
     * 用带 callback 版本删除单个 widget（删除 LayerA 第一个 widget）。
     */
    fun removeSingleWidgetInLayerA() {
        if (widgetsInLayerA.isEmpty()) {
            notifyUser("LayerA 为空\n")
            return
        }
        val w = widgetsInLayerA.removeAt(0)
        MapWidgetManager.getInstance().removeMapWidget(w, simpleCallback("removeMapWidget(${w.name})"))
    }

    /**
     * 按 id 列表批量删除（删除 LayerA 前 2 个 widget）。
     */
    fun removeBatchWidgetsInLayerA() {
        if (widgetsInLayerA.isEmpty()) {
            notifyUser("LayerA 为空\n")
            return
        }
        val toRemove = widgetsInLayerA.take(2.coerceAtMost(widgetsInLayerA.size))
        val ids = toRemove.map { it.id }
        MapWidgetManager.getInstance().removeMapWidgetsByIds(ids,
            simpleCallback("removeMapWidgetsByIds(${toRemove.size}个)"))
        widgetsInLayerA.removeAll(toRemove)
    }

    /**
     * 通用 CompletionCallback 封装，用 toast 打印结果。
     */
    private fun simpleCallback(tag: String): CommonCallbacks.CompletionCallback {
        return object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                notifyUser("$tag onSuccess\n")
            }
            override fun onFailure(error: IDJIError) {
                notifyUser("$tag onFailure: ${error.errorCode()} ${error.description()}\n")
            }
        }
    }

    fun hideArAnnotations(isHided: Boolean) {
        MapWidgetManager.getInstance().hideARAnnotations(isHided, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                notifyUser("hideArAnnotations onSuccess ${isHided}\n")
            }

            override fun onFailure(error: IDJIError) {
                notifyUser("hideArAnnotations onFailure ${isHided}\n")
            }
        })
    }

    fun queryMapElement() {
        MapWidgetManager.getInstance().queryMapElement(object :
            CommonCallbacks.CompletionCallbackWithParam<List<MapElement>> {
            override fun onSuccess(t: List<MapElement>) {
                mapElements.postValue(t)
                notifyUser("queryMapElement onSuccess ${t.size}\n")
            }

            override fun onFailure(error: IDJIError) {
                notifyUser("queryMapElement onFailure $error\n")
            }
        })
    }

    fun addMapElement() {
        val location = FlightControllerKey.KeyAircraftLocation3D.create().get()
        if (location == null) {
            notifyUser("no location\n")
            return
        }
        val element1 = MapElement().apply {
            drawerType = MapDrawerType.POLYLINE
            points = mutableListOf<OpenLocation>().apply {
                add(OpenLocation(location.latitude - 0.0005 + 0.001, location.longitude + 0.0005, location.altitude + 50))
                add(OpenLocation(location.latitude + 0.0005 + 0.001, location.longitude + 0.0005, location.altitude + 50))
                add(OpenLocation(location.latitude + 0.0005 + 0.001, location.longitude - 0.0005, location.altitude + 50))
                add(OpenLocation(location.latitude - 0.0005 + 0.001, location.longitude - 0.0005, location.altitude + 50))
            }
            color = MapElementColor.YELLOW
            name = "POLYLINE"
        }

        val element2 = MapElement().apply {
            drawerType = MapDrawerType.POLYGON
            points = mutableListOf<OpenLocation>().apply {
                add(OpenLocation(location.latitude - 0.0003, location.longitude + 0.0003, location.altitude + 50))
                add(OpenLocation(location.latitude + 0.0003, location.longitude + 0.0003, location.altitude + 50))
                add(OpenLocation(location.latitude + 0.0003, location.longitude - 0.0003, location.altitude + 50))
                add(OpenLocation(location.latitude - 0.0003, location.longitude - 0.0003, location.altitude + 50))
            }
            color = MapElementColor.GREEN
            name = "POLYGON"
        }

        val element3 = MapElement().apply {
            drawerType = MapDrawerType.PIN
            points = mutableListOf<OpenLocation>().apply {
                add(OpenLocation(location.latitude + 0.0012, location.longitude, location.altitude + 50))
            }
            name = "PIN"
        }
        MapWidgetManager.getInstance().insertMapElement(element1, null)
        MapWidgetManager.getInstance().insertMapElement(element2, null)
        MapWidgetManager.getInstance().insertMapElement(element3, null)
    }

    fun updateMapElement(element: MapElement) {
        MapWidgetManager.getInstance().updateMapElement(element, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                notifyUser("updateMapElement onSuccess ${element.name}\n")
                queryMapElement()
            }

            override fun onFailure(error: IDJIError) {
                notifyUser("updateMapElement onFailure ${element.name}\n")
            }
        })
    }

    fun deleteMapElement(element: MapElement) {
        MapWidgetManager.getInstance().deleteMapElement(element, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                notifyUser("deleteMapElement onSuccess ${element.name}\n")
                queryMapElement()
            }

            override fun onFailure(error: IDJIError) {
                notifyUser("deleteMapElement onFailure ${element.name}\n")
            }
        })
    }

    fun moveModelingFileToPublic() {
        PilotModelingManager.getInstance().moveModelingFileToPublic(object :
            CommonCallbacks.CompletionCallbackWithParam<String> {
            override fun onSuccess(t: String) {
                notifyUser("moveModelingFileToPublic onSuccess ${t}\n")
            }

            override fun onFailure(error: IDJIError) {
                notifyUser("moveModelingFileToPublic onFailure ${error}\n")
            }
        })
    }

    /**
     * 注册断连降落操作：插件断连时，服务端自动触发飞机降落（KeyStartAutoLanding）。
     * 在插件建连后调用此方法完成注册。
     */
    fun registerDisconnectLanding() {
        val manager = RemoteDisconnectActionManager.getInstance()
        manager.clearPendingActions()
        manager.addPerformAction(FlightControllerKey.KeyStartAutoLanding.create(), null)
        manager.registerDisconnectKeyActions(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                notifyUser("registerDisconnectLanding onSuccess\n")
            }

            override fun onFailure(error: IDJIError) {
                notifyUser("registerDisconnectLanding onFailure: $error\n")
            }
        })
    }

    /**
     * 清除已注册的断连操作：向服务端提交空列表，取消之前注册的所有断连操作。
     */
    fun clearDisconnectActions() {
        val manager = RemoteDisconnectActionManager.getInstance()
        manager.clearPendingActions()
        manager.registerDisconnectKeyActions(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                notifyUser("clearDisconnectActions onSuccess\n")
            }

            override fun onFailure(error: IDJIError) {
                notifyUser("clearDisconnectActions onFailure: $error\n")
            }
        })
    }

    private fun startRemoteActivity() {
        NavigateManager.getInstance().navigateToPage(
            NavigateAction(
                NavigateAction.Value.REMOTE_VIEW_PAGE_ACTIVITY,
                ""
            ), object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    // Do nothing
                }

                override fun onFailure(error: IDJIError) {
                    // Do nothing
                }
            }
        )
    }

    fun updateOpenCameraViewDrawing() {
        val drawList = mutableListOf<DrawingBoxInfo>()
        val draw1 = DrawingBoxInfo().apply {
            rect = OpenDoubleRect(0.2, 0.2, 0.2, 0.2)
            label = "draw1"
            color = Color.RED
            labelColor = Color.GREEN
        }
        val draw2 = DrawingBoxInfo().apply {
            rect = OpenDoubleRect(0.5, 0.5, 0.4, 0.3)
            label = "draw2"
            color = Color.BLUE
            labelColor = Color.YELLOW
        }
        val draw3 = DrawingBoxInfo().apply {
            rect = OpenDoubleRect(0.7, 0.7, 0.2, 0.2)
            label = "draw3"
            color = Color.WHITE
            labelColor = Color.GREEN
        }
        drawList.add(draw1)
        drawList.add(draw2)
        drawList.add(draw3)
        CameraViewDrawingManager.getInstance().updateDrawingBoxInfo(drawList, null)
    }

    fun clearOpenCameraViewDrawing() {
        CameraViewDrawingManager.getInstance().updateDrawingBoxInfo(mutableListOf<DrawingBoxInfo>(), null)
    }

    fun updateOpenCameraViewPolygonDrawing() {
        val polygonList = mutableListOf<DrawingPolygonInfo>()

        // 旋转平面标注：模拟风机叶片旋转平面在图传画面的叠加显示
        // 整体左右居中（x 中心 ≈ 0.52），垂直位于画面中下部（y: 0.710~0.778）
        // 由 3 行 × 2 列共 6 个平行四边形组成，青绿色半透明，略向右倾斜
        // 颜色：0x99 00 FF B0（青绿色，约 60% 透明度）

        val fillColorGreen = 0x5500EE8B.toInt()  // #9900FFB0

        // 第 1 行左侧叶片
        val polygon1 = DrawingPolygonInfo().apply {
            points = mutableListOf(
                OpenPoint(0.415, 0.610), //左上
                OpenPoint(0.495, 0.610), //右上
                OpenPoint(0.495, 0.625), //右下
                OpenPoint(0.400, 0.625) //左下
            )
            fillColor = fillColorGreen
            strokeWidth = 0f
        }

        // 第 1 行右侧叶片
        val polygon2 = DrawingPolygonInfo().apply {
            points = mutableListOf(
                OpenPoint(0.505, 0.610),
                OpenPoint(0.585, 0.610),
                OpenPoint(0.600, 0.625),
                OpenPoint(0.505, 0.625)
            )
            fillColor = fillColorGreen
            strokeWidth = 0f
        }

        // 第 2 行左侧叶片
        val polygon3 = DrawingPolygonInfo().apply {
            points = mutableListOf(
                OpenPoint(0.400, 0.630),
                OpenPoint(0.495, 0.630),
                OpenPoint(0.495, 0.645),
                OpenPoint(0.385, 0.645)
            )
            fillColor = fillColorGreen
            strokeWidth = 0f
        }

        // 第 2 行右侧叶片
        val polygon4 = DrawingPolygonInfo().apply {
            points = mutableListOf(
                OpenPoint(0.505, 0.630),
                OpenPoint(0.600, 0.630),
                OpenPoint(0.615, 0.645),
                OpenPoint(0.505, 0.645)
            )
            fillColor = fillColorGreen
            strokeWidth = 0f
        }

        // 第 3 行左侧叶片
        val polygon5 = DrawingPolygonInfo().apply {
            points = mutableListOf(
                OpenPoint(0.385, 0.650),
                OpenPoint(0.495, 0.650),
                OpenPoint(0.495, 0.665),
                OpenPoint(0.370, 0.665)
            )
            fillColor = fillColorGreen
            strokeWidth = 0f
        }

        // 第 3 行右侧叶片
        val polygon6 = DrawingPolygonInfo().apply {
            points = mutableListOf(
                OpenPoint(0.505, 0.650),
                OpenPoint(0.615, 0.650),
                OpenPoint(0.630, 0.665),
                OpenPoint(0.505, 0.665)
            )
            fillColor = fillColorGreen
            strokeWidth = 0f
        }

        polygonList.add(polygon1)
        polygonList.add(polygon2)
        polygonList.add(polygon3)
        polygonList.add(polygon4)
        polygonList.add(polygon5)
        polygonList.add(polygon6)
        CameraViewDrawingManager.getInstance().updateDrawingPolygonInfo(polygonList, null)
    }

    fun clearOpenCameraViewPolygonDrawing() {
        CameraViewDrawingManager.getInstance().updateDrawingPolygonInfo(mutableListOf<DrawingPolygonInfo>(), null)
    }

    fun updateOpenCameraViewTextDrawing() {
        val textList = mutableListOf<DrawingTextInfo>()

        // 旋转平面距离标注：与上方 6 个多边形叶片配套使用
        // 锚点水平居中（x=0.52），位于叶片组正下方（y=0.790）
        // 黄色文字，透明背景，水平居中对齐
        val text1 = DrawingTextInfo().apply {
            anchor = OpenPoint(0.5, 0.690)
            text = "距旋转平面：25.6 m"
            textSize = 12f
            textColor = 0xff00EE8B.toInt() // #FFCC00 黄色
            backgroundColor = 0x00000000
            horizontalAlignment = DrawingTextInfo.ALIGN_CENTER
        }

        textList.add(text1)
        CameraViewDrawingManager.getInstance().updateDrawingTextInfo(textList, null)
    }

    fun clearOpenCameraViewTextDrawing() {
        CameraViewDrawingManager.getInstance().updateDrawingTextInfo(mutableListOf<DrawingTextInfo>(), null)
    }

    @Synchronized
    fun updateWayPointMissionExecuteState() {
        val content = wayPointMissionPointExecuteState + "\n" + waylineExecuteState + "\n" + waylineExecuteAction
        outputTextView.setText(content)
        outputTextView.syncToRemote()
    }

    /**
     * P0 新增属性验证 Sample
     *
     * 验证组件：
     *  - 组1：VISIBILITY / ENABLED / PADDING（TextView 演示）
     *  - 组2：BACKGROUND_SHAPE（Button 演示）
     *  - 组3：MAX_LINES + ELLIPSIZE（TextView 演示）
     *  - 组4：CHECKED（CheckBox + Switch）
     *  - 组5：INPUT_TYPE + MAX_LENGTH（EditText 演示）
     *  - 组6：PROGRESS_MAX（ProgressBar 演示）
     */
    fun initPropertySampleView() {
        val helper = RemoteViewHelper.obtain(openPluginBoxPageType)
        helper.clearLayout()
        helper.setPageLayoutParamWrapContent()

        val viewWidth = 150
        val labelHeight = 14
        val compHeight = 30
        val btnColor = Color.argb(200, 30, 100, 200)
        val cardBgColor = 0x28FFFFFF
        val sampleBgColor = Color.argb(180, 80, 80, 80)

        // 根容器：垂直滚动列表
        val root = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        root.addToRoot(null)
        root.setConstraintsOnRoot(ViewSide.START, ViewSide.START, 4)
        root.setConstraintsOnRoot(ViewSide.END, ViewSide.END, 4)
        root.setConstraintsOnRoot(ViewSide.TOP, ViewSide.TOP, 2)

        // ── 输出 TextView ────────────────────────────────────────────────────
        val p0Output = helper.createText().apply {
            setText("[输出区] 点击按钮触发测试")
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(40)
            setBackground(Color.argb(200, 20, 20, 20))
            setVerticalScrollable(true)
        }
        root.addToRemote(p0Output)

        // ── 卡片容器 helper ────────────────────────────────────────────────
        fun card(title: String): RemoteLinearLayout {
            val container = helper.createLinearLayoutVertical().apply {
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(RemoteView.WRAP_CONTENT)
                setBackgroundShape(
                    ShapeDescriptor.Builder()
                        .fillColor(cardBgColor)
                        .cornerRadius(4f)
                        .build()
                )
                setPadding(0, 4, 0, 8)
            }
            root.addToRemote(container, WidgetLayoutParam().apply { setMargin(0, 6, 0, 0) })
            val titleText = helper.createText().apply {
                setText(title)
                setTextSize(11f)
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(RemoteView.WRAP_CONTENT)
            }
            container.addToRemote(titleText, WidgetLayoutParam().apply { setMargin(6, 2, 6, 2) })
            // 标题设为粗体
            helper.updateView(titleText.getId(), RemoteViewProperty.FONT_STYLE_TYPE, FontStyleType.BOLD.value.toString())
            return container
        }

        // ── 控制按钮 helper ──────────────────────────────────────────────────
        fun ctrlRow(vararg labels: String, parent: RemoteLinearLayout = root, block: (row: RemoteLinearLayout, btns: List<RemoteButton>) -> Unit) {
            val row = helper.createLinearLayoutHorizontal().apply {
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(RemoteView.WRAP_CONTENT)
            }
            val btns = labels.mapIndexed { i, label ->
                helper.createButton().apply {
                    setText(label)
                    setWidth(viewWidth / labels.size)
                    setHeight(compHeight)
                    setBackground(btnColor)
                }
            }
            parent.addToRemote(row)
            btns.forEach {
                row.addToRemote(it, WidgetLayoutParam().apply { setMargin(2, 2, 0, 0) })
            }
            block(row, btns)
            btns.forEach{
                it.syncToRemote()
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // 组1：VISIBILITY / ENABLED / PADDING（TextView 演示）
        // ════════════════════════════════════════════════════════════════════
        val card1 = card("1. 显隐 / 触控 / 填充")

        val targetTextView = helper.createText().apply {
            setText("我是被测 TextView")
            setWidth(viewWidth)
            setHeight(compHeight)
            setBackground(sampleBgColor)
        }

        targetTextView.setOnClickListener{
            p0Output.setText("测试TextView被点击")
            p0Output.syncToRemote()
        }

        card1.addToRemote(targetTextView, WidgetLayoutParam().apply { setMargin(6, 2, 6, 2) })

        // VISIBILITY：三态循环
        val visibilityStates = listOf(VisibilityType.VISIBLE, VisibilityType.INVISIBLE, VisibilityType.GONE)
        var visibilityIndex = 0
        ctrlRow("切换可见性", parent = card1) { _, btns ->
            btns[0].setOnClickListener {
                visibilityIndex = (visibilityIndex + 1) % visibilityStates.size
                val state = visibilityStates[visibilityIndex]
                targetTextView.setVisibility(state)
                targetTextView.syncToRemote()
                p0Output.setText("VISIBILITY → ${state.name}")
                p0Output.syncToRemote()
            }
        }

        // ENABLED：切换启用/禁用
        var isEnabled = true
        ctrlRow("启用 / 禁用", parent = card1) { _, btns ->
            btns[0].setOnClickListener {
                isEnabled = !isEnabled
                targetTextView.setEnabled(isEnabled)
                targetTextView.syncToRemote()
                p0Output.setText("ENABLED → $isEnabled")
                p0Output.syncToRemote()
            }
        }

        // PADDING：两档切换
        var isPadded = false
        ctrlRow("padding(0) / padding(12)", parent = card1) { _, btns ->
            btns[0].setOnClickListener {
                isPadded = !isPadded
                val pad = if (isPadded) 12 else 0
                targetTextView.setPadding(pad, 4, pad, 4)
                targetTextView.syncToRemote()
                p0Output.setText("PADDING → $pad, 4, $pad, 4 (dp)")
                p0Output.syncToRemote()
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // 组2：BACKGROUND_SHAPE（Button 演示）
        // ════════════════════════════════════════════════════════════════════
        val card2 = card("2. 背景样式")

        val shapeButton = helper.createButton().apply {
            setText("背景形状测试")
            setWidth(viewWidth)
            setHeight(compHeight)
        }
        card2.addToRemote(shapeButton, WidgetLayoutParam().apply { setMargin(6, 2, 6, 2) })

        ctrlRow("圆角蓝色", "白色描边", "红色椭圆", "单侧圆角", parent = card2) { _, btns ->
            // 圆角矩形 + 蓝色填充
            btns[0].setOnClickListener {
                shapeButton.setBackgroundShape(
                    ShapeDescriptor.Builder()
                        .fillColor(0xFF0052CC.toInt())
                        .cornerRadius(16f)
                        .build()
                )
                shapeButton.syncToRemote()
                p0Output.setText("BACKGROUND_SHAPE → 圆角矩形 蓝色")
                p0Output.syncToRemote()
            }
            // 透明填充 + 白色描边
            btns[1].setOnClickListener {
                shapeButton.setBackgroundShape(
                    ShapeDescriptor.Builder()
                        .fillColor(Color.TRANSPARENT)
                        .cornerRadius(8f)
                        .stroke(2, Color.WHITE)
                        .build()
                )
                shapeButton.syncToRemote()
                p0Output.setText("BACKGROUND_SHAPE → 白色描边 无填充")
                p0Output.syncToRemote()
            }
            // 椭圆 + 红色填充
            btns[2].setOnClickListener {
                shapeButton.setBackgroundShape(
                    ShapeDescriptor.Builder()
                        .shape(ShapeDescriptor.SHAPE_OVAL)
                        .fillColor(Color.RED)
                        .build()
                )
                shapeButton.syncToRemote()
                p0Output.setText("BACKGROUND_SHAPE → 椭圆 红色")
                p0Output.syncToRemote()
            }
            // 四角独立圆角：仅右侧两角有圆角
            btns[3].setOnClickListener {
                shapeButton.setBackgroundShape(
                    ShapeDescriptor.Builder()
                        .fillColor(0xFF00AA55.toInt())
                        .cornerRadii(floatArrayOf(0f, 0f, 16f, 16f, 16f, 16f, 0f, 0f))
                        .build()
                )
                shapeButton.syncToRemote()
                p0Output.setText("BACKGROUND_SHAPE → 右侧圆角 绿色")
                p0Output.syncToRemote()
            }
            btns[0].setTextSize(9f)
            btns[1].setTextSize(9f)
            btns[2].setTextSize(9f)
            btns[3].setTextSize(9f)
        }

        // ════════════════════════════════════════════════════════════════════
        // 组3：MAX_LINES + ELLIPSIZE（TextView 演示）
        // ════════════════════════════════════════════════════════════════════
        val card3 = card("3. 最大行 + 省略效果")

        val longText = "这是一段用于测试文本截断效果的长文字，内容足够多才能触发省略效果，请确认显示正确。"
        val ellipsizeTextView = helper.createText().apply {
            setText(longText)
            setWidth(viewWidth)
            setHeight(50)
            setTextSize(10f)
            setBackground(sampleBgColor)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(Color.TRANSPARENT)
                    .cornerRadius(2f)
                    .stroke(1, Color.WHITE)
                    .build()
            )
        }
        card3.addToRemote(ellipsizeTextView, WidgetLayoutParam().apply { setMargin(6, 2, 6, 2) })

        ctrlRow("1行+END", "2行+MIDDLE", "恢复全文", parent = card3) { _, btns ->
            btns[0].setOnClickListener {
                ellipsizeTextView.setMaxLines(1)
                ellipsizeTextView.setEllipsize(EllipsizeType.END)
                ellipsizeTextView.syncToRemote()
                p0Output.setText("MAX_LINES=1 ELLIPSIZE=END")
                p0Output.syncToRemote()
            }
            btns[1].setOnClickListener {
                ellipsizeTextView.setMaxLines(2)
                ellipsizeTextView.setEllipsize(EllipsizeType.MIDDLE)
                ellipsizeTextView.syncToRemote()
                p0Output.setText("MAX_LINES=2 ELLIPSIZE=MIDDLE")
                p0Output.syncToRemote()
            }
            btns[2].setOnClickListener {
                ellipsizeTextView.setMaxLines(100)
                ellipsizeTextView.setEllipsize(EllipsizeType.UNKNOWN)
                ellipsizeTextView.syncToRemote()
                p0Output.setText("MAX_LINES=100 恢复全文")
                p0Output.syncToRemote()
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // 组4：CHECKED（CheckBox + Switch）
        // 验证要点：程序设置 checked 不应触发 onCheckChange listener
        // ════════════════════════════════════════════════════════════════════
        val card4 = card("4. 开关状态")

        val checkedRow = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        card4.addToRemote(checkedRow, WidgetLayoutParam().apply { setMargin(6, 2, 6, 2) })

        val sampleCheckBox = helper.createCheckBox().apply {
            setText("CheckBox")
            setWidth(viewWidth / 2)
            setHeight(compHeight)
            setBackground(sampleBgColor)
        }
        checkedRow.addToRemote(sampleCheckBox)

        val sampleSwitch = helper.createSwitch().apply {
            setText("Switch")
            setWidth(viewWidth / 2)
            setHeight(compHeight)
            setBackground(sampleBgColor)
        }
        checkedRow.addToRemote(sampleSwitch)

        // 用户手动操作时触发 listener → output 打印，验证正常回调
        sampleCheckBox.setOnCheckChangeListener { _, isChecked ->
            p0Output.setText("【用户操作】CheckBox → $isChecked")
            p0Output.syncToRemote()
        }
        sampleSwitch.setOnCheckChangeListener { _, isChecked ->
            p0Output.setText("【用户操作】Switch → $isChecked")
            p0Output.syncToRemote()
        }

        ctrlRow("CB=true", "CB=false", "SW=true", "SW=false", parent = card4) { _, btns ->
            // 程序设置 CheckBox=true，不应触发上面的 listener（output 无变化）
            btns[0].setOnClickListener {
                p0Output.setText("【程序设置】CHECKED CB=true (listener不应触发)")
                p0Output.syncToRemote()
                sampleCheckBox.setChecked(true)
                sampleCheckBox.syncToRemote()
            }
            btns[1].setOnClickListener {
                p0Output.setText("【程序设置】CHECKED CB=false (listener不应触发)")
                p0Output.syncToRemote()
                sampleCheckBox.setChecked(false)
                sampleCheckBox.syncToRemote()
            }
            btns[2].setOnClickListener {
                p0Output.setText("【程序设置】CHECKED SW=true (listener不应触发)")
                p0Output.syncToRemote()
                sampleSwitch.setChecked(true)
                sampleSwitch.syncToRemote()
            }
            btns[3].setOnClickListener {
                p0Output.setText("【程序设置】CHECKED SW=false (listener不应触发)")
                p0Output.syncToRemote()
                sampleSwitch.setChecked(false)
                sampleSwitch.syncToRemote()
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // 组5：INPUT_TYPE + MAX_LENGTH（EditText 演示）
        // ════════════════════════════════════════════════════════════════════
        val card5 = card("5. 输入框属性")

        val sampleEditText = helper.createEditText().apply {
            setHint("在此输入（观察 hint 颜色变化）")
            setWidth(viewWidth)
            setHeight(compHeight)
            setBackground(sampleBgColor)
        }
        card5.addToRemote(sampleEditText, WidgetLayoutParam().apply { setMargin(6, 2, 6, 2) })

        ctrlRow("数字键盘", "密码模式", "普通文本", parent = card5) { _, btns ->
            btns[0].setOnClickListener {
                sampleEditText.setInputType(InputType.NUMBER)
                sampleEditText.syncToRemote()
                p0Output.setText("INPUT_TYPE → NUMBER")
                p0Output.syncToRemote()
            }
            btns[1].setOnClickListener {
                sampleEditText.setInputType(InputType.PASSWORD)
                sampleEditText.syncToRemote()
                p0Output.setText("INPUT_TYPE → PASSWORD")
                p0Output.syncToRemote()
            }
            btns[2].setOnClickListener {
                sampleEditText.setInputType(InputType.TEXT)
                sampleEditText.syncToRemote()
                p0Output.setText("INPUT_TYPE → TEXT")
                p0Output.syncToRemote()
            }
        }

        ctrlRow("限制5字符", "无限制", parent = card5) { _, btns ->
            btns[0].setOnClickListener {
                sampleEditText.setMaxLength(5)
                sampleEditText.syncToRemote()
                p0Output.setText("MAX_LENGTH → 5（第6个字符将被阻断）")
                p0Output.syncToRemote()
            }
            btns[1].setOnClickListener {
                sampleEditText.setMaxLength(Int.MAX_VALUE)
                sampleEditText.syncToRemote()
                p0Output.setText("MAX_LENGTH → 无限制")
                p0Output.syncToRemote()
            }
        }

        ctrlRow("红色hint", "灰色hint", "蓝色hint", parent = card5) { _, btns ->
            btns[0].setOnClickListener {
                sampleEditText.setHintTextColor(0xFFFF3B30.toInt())
                sampleEditText.syncToRemote()
                p0Output.setText("HINT_TEXT_COLOR → 红色")
                p0Output.syncToRemote()
            }
            btns[1].setOnClickListener {
                sampleEditText.setHintTextColor(0xFF999999.toInt())
                sampleEditText.syncToRemote()
                p0Output.setText("HINT_TEXT_COLOR → 灰色")
                p0Output.syncToRemote()
            }
            btns[2].setOnClickListener {
                sampleEditText.setHintTextColor(0xFF2696FF.toInt())
                sampleEditText.syncToRemote()
                p0Output.setText("HINT_TEXT_COLOR → 蓝色")
                p0Output.syncToRemote()
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // 组6：PROGRESS_MAX（ProgressBar 演示）
        // ════════════════════════════════════════════════════════════════════
        val card6 = card("6. 进度条最大值 + 当前进度")

        val sampleProgressBar = helper.createProgressBar().apply {
            setWidth(100)
            setHeight(5)
            setBackground(sampleBgColor)
        }
        card6.addToRemote(sampleProgressBar, WidgetLayoutParam().apply { setMargin(6, 2, 6, 2) })

        ctrlRow("50/200", "50/100", "满进度", parent = card6) { _, btns ->
            // max=200，progress=100 → 视觉上恰好一半
            btns[0].setOnClickListener {
                sampleProgressBar.setMax(200)
                sampleProgressBar.setProgress(50)
                sampleProgressBar.syncToRemote()
                p0Output.setText("PROGRESS_MAX=200 progress=100 (50%)")
                p0Output.syncToRemote()
            }
            // max=100，progress=50 → 同样一半，与上面对比验证 max 生效
            btns[1].setOnClickListener {
                sampleProgressBar.setMax(100)
                sampleProgressBar.setProgress(50)
                sampleProgressBar.syncToRemote()
                p0Output.setText("PROGRESS_MAX=100 progress=50 (50%)")
                p0Output.syncToRemote()
            }
            // 满进度
            btns[2].setOnClickListener {
                sampleProgressBar.setProgress(100)
                sampleProgressBar.syncToRemote()
                p0Output.setText("progress=100 (满进度，当前max=100)")
                p0Output.syncToRemote()
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // 组7：圆形进度条（演示）
        // ════════════════════════════════════════════════════════════════════
        val card7 = card("7. 圆形进度条")

        val circleProgressBar = helper.createCircleProgressBar().apply {
            setWidth(30)
            setHeight(30)
            setText("X")
            setTextSize(9f)
            setStatus(ProgressStatusType.NORMAL)
            setProgressColor(0xFFFF3B30.toInt())
        }
        card7.addToRemote(circleProgressBar, WidgetLayoutParam().apply { setMargin(6, 2, 6, 2) })

        ctrlRow("0%", "50%", "失败", "完成", parent = card7) { _, btns ->
            btns[0].setOnClickListener {
                circleProgressBar.setText("0%")
                circleProgressBar.setTextSize(11f)
                circleProgressBar.setProgress(0)
                circleProgressBar.setTextColor(0xFF1088F2.toInt())
                circleProgressBar.setProgressColor(0xFF1088F2.toInt())
                circleProgressBar.setStatus(ProgressStatusType.NORMAL)
                circleProgressBar.syncToRemote()
            }
            btns[1].setOnClickListener {
                circleProgressBar.setProgress(50)
                circleProgressBar.setText("50%")
                circleProgressBar.setTextColor(0xFF1088F2.toInt())
                circleProgressBar.setProgressColor(0xFF1088F2.toInt())
                circleProgressBar.setStatus(ProgressStatusType.NORMAL)
                circleProgressBar.syncToRemote()
            }
            btns[2].setOnClickListener {
                circleProgressBar.setProgress(75)
                circleProgressBar.setProgressColor(0xFFFF3B30.toInt())
                circleProgressBar.setStatus(ProgressStatusType.FAIL)
                circleProgressBar.syncToRemote()
            }
            btns[3].setOnClickListener {
                circleProgressBar.setProgress(100)
                circleProgressBar.setProgressColor(0xFF00AA55.toInt())
                circleProgressBar.setStatus(ProgressStatusType.SUCCESS)
                circleProgressBar.syncToRemote()
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // 组8：通用属性 — ALPHA / MIN_SIZE
        // ════════════════════════════════════════════════════════════════════
        val card8 = card("8. 透明度 / 最小尺寸")

        val alphaTarget = helper.createText().apply {
            setText("ALPHA / MIN_SIZE 测试目标")
            setWidth(RemoteView.WRAP_CONTENT)
            setHeight(RemoteView.WRAP_CONTENT)
            setTextSize(10f)
            setBackground(sampleBgColor)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(Color.TRANSPARENT)
                    .cornerRadius(2f)
                    .stroke(1, Color.WHITE)
                    .build()
            )
        }
        card8.addToRemote(alphaTarget, WidgetLayoutParam().apply { setMargin(6, 2, 6, 2) })

        ctrlRow("透明50%", "不透明", "最小100x40", "还原", parent = card8) { _, btns ->
            btns[0].setOnClickListener {
                alphaTarget.setAlpha(0.5f)
                alphaTarget.syncToRemote()
                p0Output.setText("ALPHA → 0.5")
                p0Output.syncToRemote()
            }
            btns[1].setOnClickListener {
                alphaTarget.setAlpha(1.0f)
                alphaTarget.syncToRemote()
                p0Output.setText("ALPHA → 1.0")
                p0Output.syncToRemote()
            }
            btns[2].setOnClickListener {
                alphaTarget.setMinSize(140, 50)
                alphaTarget.syncToRemote()
                p0Output.setText("MIN_SIZE → 100dp, 40dp")
                p0Output.syncToRemote()
            }
            btns[3].setOnClickListener {
                alphaTarget.setMinSize(0, 0)
                alphaTarget.syncToRemote()
                p0Output.setText("MIN_SIZE → 0, 0 (还原)")
                p0Output.syncToRemote()
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // 组9：文本装饰 — SINGLE_LINE / LINE_SPACING / TEXT_DECORATION / LETTER_SPACING
        // ════════════════════════════════════════════════════════════════════
        val card9 = card("9. 文本装饰")

        val decorText = "这是一段用于测试文本装饰效果的长文字，包含行间距、字间距、下划线和删除线等样式。"
        val decorTarget = helper.createText().apply {
            setText(decorText)
            setWidth(viewWidth)
            setTextSize(10f)
            setHeight(RemoteView.WRAP_CONTENT)
            setBackground(sampleBgColor)
            setBackgroundShape(
                ShapeDescriptor.Builder()
                    .fillColor(Color.TRANSPARENT)
                    .cornerRadius(2f)
                    .stroke(1, Color.WHITE)
                    .build()
            )
        }
        card9.addToRemote(decorTarget, WidgetLayoutParam().apply { setMargin(6, 2, 6, 2) })

        ctrlRow("单行", "多行", "下划线", "删除线", parent = card9) { _, btns ->
            btns[0].setOnClickListener {
                decorTarget.setSingleLine(true)
                decorTarget.syncToRemote()
                p0Output.setText("SINGLE_LINE → true")
                p0Output.syncToRemote()
            }
            btns[1].setOnClickListener {
                decorTarget.setSingleLine(false)
                decorTarget.syncToRemote()
                p0Output.setText("SINGLE_LINE → false")
                p0Output.syncToRemote()
            }
            btns[2].setOnClickListener {
                decorTarget.setTextDecoration(TextDecorationType.UNDERLINE)
                decorTarget.syncToRemote()
                p0Output.setText("TEXT_DECORATION → UNDERLINE")
                p0Output.syncToRemote()
            }
            btns[3].setOnClickListener {
                decorTarget.setTextDecoration(TextDecorationType.STRIKE_THROUGH)
                decorTarget.syncToRemote()
                p0Output.setText("TEXT_DECORATION → STRIKE_THROUGH")
                p0Output.syncToRemote()
            }
        }

        ctrlRow("行距1.5x", "行距还原", "字距0.15", "字距还原", parent = card9) { _, btns ->
            btns[0].setOnClickListener {
                decorTarget.setLineSpacing(4f, 1.5f)
                decorTarget.syncToRemote()
                p0Output.setText("LINE_SPACING → extra=4dp, multiplier=1.5")
                p0Output.syncToRemote()
            }
            btns[1].setOnClickListener {
                decorTarget.setLineSpacing(0f, 1.0f)
                decorTarget.syncToRemote()
                p0Output.setText("LINE_SPACING → 还原默认")
                p0Output.syncToRemote()
            }
            btns[2].setOnClickListener {
                decorTarget.setLetterSpacing(0.15f)
                decorTarget.syncToRemote()
                p0Output.setText("LETTER_SPACING → 0.15em")
                p0Output.syncToRemote()
            }
            btns[3].setOnClickListener {
                decorTarget.setLetterSpacing(0f)
                decorTarget.syncToRemote()
                p0Output.setText("LETTER_SPACING → 0 (还原)")
                p0Output.syncToRemote()
            }
            btns[0].setTextSize(9f)
            btns[1].setTextSize(9f)
            btns[2].setTextSize(9f)
            btns[3].setTextSize(9f)
        }

        // ════════════════════════════════════════════════════════════════════
        // 组10：CheckBox/Switch 颜色 — CHECKBOX_TINT / SWITCH_THUMB_COLOR / SWITCH_TRACK_COLOR
        // ════════════════════════════════════════════════════════════════════
        val card10 = card("10. CB/SW 颜色")

        val tintRow = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        card10.addToRemote(tintRow, WidgetLayoutParam().apply { setMargin(6, 2, 6, 2) })

        val tintCheckBox = helper.createCheckBox().apply {
            setText("CheckBox")
            setWidth(viewWidth / 2)
            setHeight(compHeight)
            setBackground(sampleBgColor)
        }
        tintRow.addToRemote(tintCheckBox)

        val tintSwitch = helper.createSwitch().apply {
            setText("Switch")
            setWidth(viewWidth / 2)
            setHeight(compHeight)
            setBackground(sampleBgColor)
        }
        tintRow.addToRemote(tintSwitch)

        ctrlRow("CB红色", "CB绿色", "CB蓝色", parent = card10) { _, btns ->
            btns[0].setOnClickListener {
                tintCheckBox.setCheckBoxTint(0xFFFF3B30.toInt())
                tintCheckBox.syncToRemote()
                p0Output.setText("CHECKBOX_TINT → 红色")
                p0Output.syncToRemote()
            }
            btns[1].setOnClickListener {
                tintCheckBox.setCheckBoxTint(0xFF00AA55.toInt())
                tintCheckBox.syncToRemote()
                p0Output.setText("CHECKBOX_TINT → 绿色")
                p0Output.syncToRemote()
            }
            btns[2].setOnClickListener {
                tintCheckBox.setCheckBoxTint(0xFF2696FF.toInt())
                tintCheckBox.syncToRemote()
                p0Output.setText("CHECKBOX_TINT → 蓝色(默认)")
                p0Output.syncToRemote()
            }
        }

        ctrlRow("SW红/蓝", "SW绿/灰", "SW还原", parent = card10) { _, btns ->
            btns[0].setOnClickListener {
                tintSwitch.setThumbColor(0xFFFF3B30.toInt())
                tintSwitch.setTrackColor(0xFF2696FF.toInt())
                tintSwitch.syncToRemote()
                p0Output.setText("SWITCH → thumb红色, track蓝色")
                p0Output.syncToRemote()
            }
            btns[1].setOnClickListener {
                tintSwitch.setThumbColor(0xFF00AA55.toInt())
                tintSwitch.setTrackColor(0xFF666666.toInt())
                tintSwitch.syncToRemote()
                p0Output.setText("SWITCH → thumb绿色, track灰色")
                p0Output.syncToRemote()
            }
            btns[2].setOnClickListener {
                // 恢复系统默认：白色滑块 + 浅灰轨道
                tintSwitch.setThumbColor(0xFFCCCCCC.toInt())
                tintSwitch.setTrackColor(0xFF555555.toInt())
                tintSwitch.syncToRemote()
                p0Output.setText("SWITCH → 还原默认色")
                p0Output.syncToRemote()
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // 组11：图片属性 — IMAGE_TINT
        // ════════════════════════════════════════════════════════════════════
        val card11 = card("11. 图片属性")

        val tintImage = helper.createImage().apply {
            setWidth(40)
            setHeight(40)
            setImageFileName(MARKER_WIND_TURBINE)
            setBackground(sampleBgColor)
        }
        card11.addToRemote(tintImage, WidgetLayoutParam().apply { setMargin(6, 2, 6, 2) })

        ctrlRow("红色着色", "蓝色着色", "清除着色", parent = card11) { _, btns ->
            btns[0].setOnClickListener {
                tintImage.setImageTint(0xFFFF3B30.toInt())
                tintImage.syncToRemote()
                p0Output.setText("IMAGE_TINT → 红色")
                p0Output.syncToRemote()
            }
            btns[1].setOnClickListener {
                tintImage.setImageTint(0xFF2696FF.toInt())
                tintImage.syncToRemote()
                p0Output.setText("IMAGE_TINT → 蓝色")
                p0Output.syncToRemote()
            }
            btns[2].setOnClickListener {
                // 清除着色：设为全透明
                tintImage.setImageTint(0x00000000)
                tintImage.syncToRemote()
                p0Output.setText("IMAGE_TINT → 清除（透明）")
                p0Output.syncToRemote()
            }
        }
    }

    fun initWrapContentSampleView() {
        val helper = RemoteViewHelper.obtain(openPluginBoxPageType)
        helper.clearLayout()
        helper.setPageLayoutParamWrapContent()

        val cardBgColor = 0x28FFFFFF
        val titleBgColor = 0x40FFFFFF
        val contentTextColor = 0xFFCCCCCC.toInt()

        // 根容器
        val root = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        root.addToRoot(null)
        root.setConstraintsOnRoot(ViewSide.START, ViewSide.START, 4)
        root.setConstraintsOnRoot(ViewSide.END, ViewSide.END, 4)
        root.setConstraintsOnRoot(ViewSide.TOP, ViewSide.TOP, 2)

        // 卡片构建局部函数，返回 Pair<titleView, contentArea>
        fun buildCard(titleLabel: String, lines: List<String>): Pair<RemoteText, RemoteLinearLayout> {
            val card = helper.createLinearLayoutVertical().apply {
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(RemoteView.WRAP_CONTENT)
                setBackgroundShape(
                    ShapeDescriptor.Builder()
                        .fillColor(cardBgColor)
                        .cornerRadius(4f)
                        .build()
                )
            }
            root.addToRemote(card, WidgetLayoutParam().apply { setMargin(0, 6, 0, 0) })

            val titleView = helper.createText().apply {
                setText(titleLabel)
                setTextSize(11f)
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(RemoteView.WRAP_CONTENT)
                setBackground(titleBgColor)
            }
            helper.updateView(titleView.getId(), RemoteViewProperty.FONT_STYLE_TYPE, FontStyleType.BOLD.value.toString())
            card.addToRemote(titleView, WidgetLayoutParam().apply { setMargin(6, 4, 6, 2) })

            val contentArea = helper.createLinearLayoutVertical().apply {
                setWidth(RemoteView.MATCH_PARENT)
                setHeight(RemoteView.WRAP_CONTENT)
            }
            card.addToRemote(contentArea, WidgetLayoutParam().apply { setMargin(6, 2, 6, 4) })

            lines.forEach { text ->
                val line = helper.createText().apply {
                    setText(text)
                    setTextSize(10f)
                    setWidth(RemoteView.MATCH_PARENT)
                    setHeight(RemoteView.WRAP_CONTENT)
                    setTextColor(contentTextColor)
                }
                contentArea.addToRemote(line, WidgetLayoutParam().apply { setMargin(2, 2, 2, 0) })
            }

            return Pair(titleView, contentArea)
        }

        // Card 1：短内容（2 行）
        var card1Expanded = true
        val (title1, content1) = buildCard(
            titleLabel = "▼ 短内容 (2行) — 点击折叠",
            lines = listOf(
                "行 1：这是第一行内容文本",
                "行 2：这是第二行内容文本"
            )
        )
        title1.setOnClickListener {
            card1Expanded = !card1Expanded
            if (card1Expanded) {
                title1.setText("▼ 短内容 (2行) — 点击折叠")
                content1.setVisibility(VisibilityType.VISIBLE)
            } else {
                title1.setText("▶ 短内容 (2行) — 点击展开")
                content1.setVisibility(VisibilityType.GONE)
            }
            title1.syncToRemote()
            content1.syncToRemote()
        }

        // Card 2：中等内容（6 行）
        var card2Expanded = true
        val (title2, content2) = buildCard(
            titleLabel = "▼ 中等内容 (6行) — 点击折叠",
            lines = listOf(
                "行 1：中等卡片第一行",
                "行 2：中等卡片第二行",
                "行 3：中等卡片第三行",
                "行 4：中等卡片第四行",
                "行 5：中等卡片第五行",
                "行 6：中等卡片第六行"
            )
        )
        title2.setOnClickListener {
            card2Expanded = !card2Expanded
            if (card2Expanded) {
                title2.setText("▼ 中等内容 (6行) — 点击折叠")
                content2.setVisibility(VisibilityType.VISIBLE)
            } else {
                title2.setText("▶ 中等内容 (6行) — 点击展开")
                content2.setVisibility(VisibilityType.GONE)
            }
            title2.syncToRemote()
            content2.syncToRemote()
        }

        // Card 3：长内容（12 行）— 全展开时超过屏幕 80% 触发滚动
        var card3Expanded = true
        val (title3, content3) = buildCard(
            titleLabel = "▼ 长内容 (12行) — 点击折叠",
            lines = listOf(
                "行 01：长卡片第 01 行，验证内容超出阈值时悬浮窗不再增大",
                "行 02：长卡片第 02 行，此时 ScrollView 应进入滚动模式",
                "行 03：长卡片第 03 行",
                "行 04：长卡片第 04 行",
                "行 05：长卡片第 05 行",
                "行 06：长卡片第 06 行",
                "行 07：长卡片第 07 行",
                "行 08：长卡片第 08 行",
                "行 09：长卡片第 09 行",
                "行 10：长卡片第 10 行",
                "行 11：长卡片第 11 行",
                "行 12：长卡片第 12 行，折叠此卡片后悬浮窗应自适应缩小"
            )
        )
        title3.setOnClickListener {
            card3Expanded = !card3Expanded
            if (card3Expanded) {
                title3.setText("▼ 长内容 (12行) — 点击折叠")
                content3.setVisibility(VisibilityType.VISIBLE)
            } else {
                title3.setText("▶ 长内容 (12行) — 点击展开")
                content3.setVisibility(VisibilityType.GONE)
            }
            title3.syncToRemote()
            content3.syncToRemote()
        }
    }
}