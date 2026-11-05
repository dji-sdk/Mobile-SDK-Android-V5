package dji.sampleV5.aircraft.pages

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dji.open.pilot.base.manager.OpenLocation
import dji.open.pilot.base.manager.map.MapElement
import dji.open.pilot.base.manager.map.MapElementColor
import dji.open.pilot.base.ui.NavigateAction
import dji.open.pilot.base.manager.file.FileTransferDestination
import dji.sampleV5.aircraft.databinding.FragNormalPluginPageBinding
import dji.sampleV5.aircraft.models.DialogServiceVM
import dji.sampleV5.aircraft.models.NormalPluginVM
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.GimbalKey
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.manager.aircraft.waypoint3.WaypointMissionManager
import dji.v5.utils.common.DocumentUtil
import dji.v5.utils.common.FileUtils
import dji.v5.ux.util.Helper
import dji.v5.utils.common.LogUtils
import dji.v5.ux.util.ToastUtils
import java.io.File
import java.io.FileOutputStream

class NormalPluginFragment : DJIFragment() {

    private val TAG = LogUtils.getTag("NormalPluginFragment")

    private val normalVM: NormalPluginVM by viewModels()
    private val dialogServiceVM: DialogServiceVM by viewModels()
    private lateinit var binding: FragNormalPluginPageBinding

    var curMissionPath = ""
    private val OPEN_DOCUMENT_TREE_FOR_WMPZ = 1
    private val OPEN_DOCUMENT_TREE_FOR_RES = 2
    private val OPEN_DOCUMENT_TREE_FOR_AI_PHOTO = 3
    private val OPEN_MANAGE_EXTERNAL_STORAGE = 100
    private var pendingRequestCode: Int = -1
    private var pendingIntentType: String = "application/*"

    private var mapElements: List<MapElement> = mutableListOf()
    private var btnSensingText:String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragNormalPluginPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        normalVM.initListener()
        initMap()
        initOpenRemoteBox()
        initIntelligent()

        binding.kmzBtn.setOnClickListener {
            checkPermissionAndShowFileChooser(OPEN_DOCUMENT_TREE_FOR_WMPZ)
        }

        normalVM.aircraftAttitude.observe(viewLifecycleOwner) {
            updateMsgView()
        }
    }

    private fun initOpenRemoteBox() {

        binding.btnInitOpenBoxSampleView.setOnClickListener {
            normalVM.initSampleRemoteView()
        }

        binding.btnInitRemoteViewPropertyDemo.setOnClickListener{
            normalVM.initRemoteViewPropertyDemo()
        }

        binding.btnRegisterDisconnectLanding.setOnClickListener {
            normalVM.registerDisconnectLanding()
        }

        binding.btnClearDisconnectActions.setOnClickListener {
            normalVM.clearDisconnectActions()
        }

        binding.btnInitPropertySampleView.setOnClickListener {
            normalVM.initPropertySampleView()
        }

        binding.btnInitWrapContentSampleView.setOnClickListener {
            normalVM.initWrapContentSampleView()
        }
    }

    private fun initMap() {
        normalVM.mapElements.observe(viewLifecycleOwner) {
            mapElements = it
            updateMsgView()
        }

        binding.btnQueryFileListFromRemote.setOnClickListener {
            normalVM.queryFileListFromRemote()
        }

        binding.btnPushFileListToRemote.setOnClickListener {
            checkPermissionAndShowFileChooser(OPEN_DOCUMENT_TREE_FOR_RES)
        }

        binding.btnPrepareRes.setOnClickListener {
            if (normalVM.markerIconList.isEmpty()) {
                ToastUtils.showToast("no res")
                return@setOnClickListener
            }

            for (res in normalVM.markerIconList) {
                val resFile = File(requireContext().getExternalFilesDir("/"), res)
                FileUtils.copyAssetsFileIfNeed(requireContext(), "plugin/$res", resFile)
                Helper.pushFileToRemote(resFile.path)
            }
        }

        binding.btnAddMapWidget.setOnClickListener {
            normalVM.addMapWidget()
        }

        binding.btnLayerSceneAddLayer.setOnClickListener {
            normalVM.addLayerScene()
        }

        binding.btnAddLayerAWidgets.setOnClickListener {
            normalVM.addBatchWidgetsToLayerA()
        }

        binding.btnAddLayerBWidgets.setOnClickListener {
            normalVM.addBatchWidgetsToLayerB()
        }

        binding.btnUpdateLayerABatch.setOnClickListener {
            normalVM.updateBatchWidgetsInLayerA()
        }

        binding.btnToggleLayerAVisible.setOnClickListener {
            normalVM.toggleLayerAVisible()
        }

        binding.btnToggleLayerBVisible.setOnClickListener {
            normalVM.toggleLayerBVisible()
        }

        binding.btnQueryAllWidgets.setOnClickListener {
            normalVM.queryAllWidgets()
        }

        binding.btnQueryLayerA.setOnClickListener {
            normalVM.queryWidgetsInLayerA()
        }

        binding.btnAddMapWidgetCallback.setOnClickListener {
            normalVM.addSingleWidgetWithCallback()
        }

        binding.btnRemoveOneLayerAWidget.setOnClickListener {
            normalVM.removeSingleWidgetInLayerA()
        }

        binding.btnRemoveLayerABatch.setOnClickListener {
            normalVM.removeBatchWidgetsInLayerA()
        }

        binding.btnClearLayerA.setOnClickListener {
            normalVM.clearLayerA()
        }

        binding.btnRemoveLayerA.setOnClickListener {
            normalVM.removeLayerA()
        }

        binding.btnHideArAnnotations.setOnClickListener {
            val values = listOf(true, false)
            initPopupNumberPicker(Helper.makeList(values)) {
                val value = values[indexChosen[0]]
                normalVM.hideArAnnotations(value)
                resetIndex()
            }
        }

        binding.btnQueryMapElement.setOnClickListener {
            normalVM.queryMapElement()
        }

        binding.btnAddMapElement.setOnClickListener {
            normalVM.addMapElement()
        }

        binding.btnUpdateMapElement.setOnClickListener {
            if (mapElements.isEmpty()) {
                ToastUtils.showToast("no map elements")
                return@setOnClickListener
            }
            val values = mapElements.map {
                it.name
            }
            initPopupNumberPicker(Helper.makeList(values)) {
                val element = mapElements[indexChosen[0]]
                element.name += 1
                element.color = MapElementColor.RED
                element.points = element.points.map {
                    OpenLocation(it.latitude + 0.0003, it.longitude + 0.0003, it.altitude + 50)
                }
                normalVM.updateMapElement(element)
                resetIndex()
            }
        }

        binding.btnDeleteMapElement.setOnClickListener {
            if (mapElements.isEmpty()) {
                ToastUtils.showToast("no map elements")
                return@setOnClickListener
            }
            val values = mapElements.map {
                it.name
            }
            initPopupNumberPicker(Helper.makeList(values)) {
                val element = mapElements[indexChosen[0]]
                normalVM.deleteMapElement(element)
                resetIndex()
            }
        }

        binding.btnJumpToHome.setOnClickListener {
            normalVM.jumpTo(NavigateAction(NavigateAction.Value.PILOT_HOME))
        }

        binding.btnJumpToMap.setOnClickListener {
            normalVM.jumpTo(NavigateAction(NavigateAction.Value.PILOT_MAP))
        }

        binding.btnJumpToWaypointMissionList.setOnClickListener {
            normalVM.jumpTo(NavigateAction(NavigateAction.Value.PILOT_WAYPOINT_MISSION_LIST))
        }

        binding.btnJumpToAlbum.setOnClickListener {
            normalVM.jumpTo(NavigateAction(NavigateAction.Value.PILOT_ALBUM, "0"))
        }

        binding.btnJumpToPluginMarketplace.setOnClickListener {
            normalVM.jumpTo(NavigateAction(NavigateAction.Value.PLUGIN_MARKETPLACE))
        }

        binding.btnMoveModelingFileToPublic.setOnClickListener {
            normalVM.moveModelingFileToPublic()
        }

        binding.btnImportAIPhoto.setOnClickListener {
            checkPermissionAndShowFileChooser(OPEN_DOCUMENT_TREE_FOR_AI_PHOTO, "image/*")
        }

        binding.btnUpdateOpenDrawingBox.setOnClickListener {
            normalVM.updateOpenCameraViewDrawing()
        }

        binding.btnClearOpenDrawingBox.setOnClickListener {
            normalVM.clearOpenCameraViewDrawing()
        }

        binding.btnUpdateOpenPolygonDrawing.setOnClickListener {
            normalVM.updateOpenCameraViewPolygonDrawing()
        }

        binding.btnClearOpenPolygonDrawing.setOnClickListener {
            normalVM.clearOpenCameraViewPolygonDrawing()
        }

        binding.btnUpdateOpenTextDrawing.setOnClickListener {
            normalVM.updateOpenCameraViewTextDrawing()
        }
        binding.btnClearOpenTextDrawing.setOnClickListener {
            normalVM.clearOpenCameraViewTextDrawing()
        }

        binding.btnDialogServiceList.setOnClickListener {
            /*推送Icon数据*/
            if (DialogServiceVM.markerIconList.isNotEmpty()) {
                for (res in DialogServiceVM.markerIconList) {
                    val resFile = File(requireContext().getExternalFilesDir("/"), res)
                    FileUtils.copyAssetsFileIfNeed(requireContext(), "plugin/$res", resFile)
                    Helper.pushFileToRemote(resFile.path)
                }
            }

            dialogServiceVM.initSampleRemoteView()
            dialogServiceVM.jumpTo(NavigateAction(NavigateAction.Value.PILOT_HOME))
        }

        binding.btnPushScreenshotToAppLog.setOnClickListener {
            pushScreenshotToAppLog()
        }
    }

    /**
     * 抓取当前 Activity 顶层视图作为一张"系统截屏"，保存为 PNG 后调用
     * FileTransferManager.pushFileToRemoteDest 将其推送到 [FileTransferDestination.APP_LOG] 目录。
     */
    private fun pushScreenshotToAppLog() {
        val rootView = activity?.window?.decorView?.rootView
        if (rootView == null || rootView.width <= 0 || rootView.height <= 0) {
            ToastUtils.showToast("root view not ready, cannot capture screenshot")
            return
        }

        val screenshotFile = try {
            captureViewAsPng(rootView)
        } catch (e: Exception) {
            LogUtils.e(TAG, "capture screenshot failed: ${e.message}")
            ToastUtils.showToast("capture screenshot failed: ${e.message}")
            return
        }

        ToastUtils.showToast("screenshot saved: ${screenshotFile.path}")
        Helper.pushFileToRemoteDest(screenshotFile.path, FileTransferDestination.APP_LOG)
    }

    private fun captureViewAsPng(view: View): File {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        val dir = requireContext().getExternalFilesDir("screenshots")
            ?: requireContext().filesDir
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, "screenshot_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()
        return file
    }

    private fun initIntelligent() {
        normalVM.autoSensingInfo.observe(viewLifecycleOwner) {
            btnSensingText = it.toString()
            updateMsgView()
        }
    }

    private fun checkPermissionAndShowFileChooser(requestCode: Int, intentType: String = "application/*") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            pendingRequestCode = requestCode
            pendingIntentType = intentType
            val intent = Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION")
            startActivityForResult(intent, OPEN_MANAGE_EXTERNAL_STORAGE)
        } else {
            showFileChooser(requestCode, intentType)
        }
    }

    private fun showFileChooser(requestCode: Int, intentType: String = "application/*") {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = intentType
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OPEN_MANAGE_EXTERNAL_STORAGE
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            if (pendingRequestCode != -1) {
                showFileChooser(pendingRequestCode, pendingIntentType)
                pendingRequestCode = -1
            }
            return
        }
        if (requestCode == OPEN_DOCUMENT_TREE_FOR_WMPZ && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // 处理选中的文件
                curMissionPath = DocumentUtil.getPath(requireContext(), uri)
                ToastUtils.showToast(curMissionPath)
                WaypointMissionManager.getInstance().pushKMZFileToRemote(curMissionPath, object :
                    CommonCallbacks.CompletionCallbackWithProgress<Double> {
                    override fun onProgressUpdate(progress: Double) {
                        ToastUtils.showToast("$curMissionPath:$progress")
                    }

                    override fun onSuccess() {
                        ToastUtils.showToast("$curMissionPath:onSuccess")
                    }

                    override fun onFailure(error: IDJIError) {
                        ToastUtils.showToast("$curMissionPath:$error")
                    }
                })
            }
        } else if (requestCode == OPEN_DOCUMENT_TREE_FOR_RES && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // 处理选中的文件
                val path = DocumentUtil.getPath(requireContext(), uri)
                ToastUtils.showToast(path)
                Helper.pushFileToRemote(path)
            }
        } else if (requestCode == OPEN_DOCUMENT_TREE_FOR_AI_PHOTO && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val imagePath = DocumentUtil.getPathFromUri(requireContext(), uri)
                if (imagePath.isNullOrEmpty()) {
                    ToastUtils.showToast("AI Photo Path 解析失败")
                    return@let
                }
                ToastUtils.showToast("AI Photo Path: $imagePath")
                binding.imageViewAIPhoto.setMedia(imagePath)
            }
        }
    }

    private fun updateMsgView() {
        val sb = StringBuffer()
        sb.append("-- Key Value --\n")
        sb.append("aircraft location:${FlightControllerKey.KeyAircraftLocation3D.create().get()}\n")
        sb.append("aircraft attitude:${FlightControllerKey.KeyAircraftAttitude.create().get()}\n")
        sb.append("gimbal attitude:${GimbalKey.KeyGimbalAttitude.create().get()}\n")
        sb.append("laser attitude:${CameraKey.KeyLaserMeasureInformation.create().get()}\n")
        sb.append("-- Key Value End --\n").append("\n")
        sb.append("-- Map Element --\n")
        mapElements.forEach {
            sb.append("$it\n")
        }
        sb.append("-- Map Element End --\n").append("\n")
        sb.append("-- sensing data --\n")
        sb.append(btnSensingText)
        sb.append("-- sensing data End --\n").append("\n")
        binding.msgText.text = sb.toString()
    }
}