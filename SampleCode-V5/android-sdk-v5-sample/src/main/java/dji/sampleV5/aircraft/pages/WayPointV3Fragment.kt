package dji.sampleV5.aircraft.pages

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.fragment.app.activityViewModels
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.models.WayPointV3VM
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.utils.common.*
import kotlinx.android.synthetic.main.frag_waypointv3_page.*
import java.io.File
import java.util.*


import dji.v5.manager.aircraft.waypoint3.model.WaypointMissionExecuteState
import java.io.IOException
import android.content.DialogInterface

import android.content.DialogInterface.OnMultiChoiceClickListener
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.widget.*
import com.dji.industry.mission.DocumentsUtils
import com.dji.wpmzsdk.common.data.Template
import com.dji.wpmzsdk.common.utils.kml.model.WaypointActionType
import com.dji.wpmzsdk.manager.WPMZManager
import dji.sampleV5.aircraft.utils.KMZTestUtil
import dji.sampleV5.aircraft.utils.KMZTestUtil.createWaylineMission
import dji.sampleV5.aircraft.utils.wpml.WaypointInfoModel


import dji.sampleV5.aircraft.BuildConfig


import dji.sampleV5.aircraft.util.DialogUtil
import dji.sdk.wpmz.jni.JNIWPMZManager
import dji.sdk.wpmz.value.mission.*
import dji.v5.manager.aircraft.waypoint3.WPMZParserManager
import dji.v5.manager.aircraft.waypoint3.WaylineExecutingInfoListener
import dji.v5.manager.aircraft.waypoint3.WaypointActionListener
import dji.v5.manager.aircraft.waypoint3.model.WaylineExecutingInfo
import dji.v5.utils.common.DeviceInfoUtil.getPackageName
import dji.v5.ux.map.MapWidget

import dji.v5.ux.mapkit.core.maps.DJIMap

import dji.v5.ux.mapkit.core.models.DJIBitmapDescriptor
import dji.v5.ux.mapkit.core.models.DJIBitmapDescriptorFactory

import dji.v5.ux.mapkit.core.models.DJILatLng

import dji.v5.ux.mapkit.core.models.annotations.DJIMarkerOptions

import dji.v5.ux.mapkit.core.models.annotations.DJIPolylineOptions


import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.view_mission_setting_home.*

import android.widget.EditText
import com.dji.wpmzsdk.common.data.HeightMode
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.v5.common.utils.GpsUtils
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.simulator.SimulatorManager
import dji.v5.manager.aircraft.waypoint3.WaypointMissionManager
import dji.v5.manager.aircraft.waypoint3.model.BreakPointInfo
import dji.v5.ux.accessory.DescSpinnerCell
import dji.v5.ux.mapkit.core.models.annotations.DJIMarker
import kotlinx.android.synthetic.main.dialog_add_waypoint.view.*
import dji.sampleV5.aircraft.util.ToastUtils


/**
 * @author feel.feng
 * @time 2022/02/27 9:30 上午
 * @description:
 */
class WayPointV3Fragment : DJIFragment() {

    private val wayPointV3VM: WayPointV3VM by activityViewModels()
    private val WAYPOINT_SAMPLE_FILE_NAME: String = "waypointsample.kmz"
    private val WAYPOINT_SAMPLE_FILE_DIR: String = "waypoint/"
    private val WAYPOINT_SAMPLE_FILE_CACHE_DIR: String = "waypoint/cache/"
    private val WAYPOINT_FILE_TAG = ".kmz"
    private var unzipChildDir = "temp/"
    private var unzipDir = "wpmz/"
    private var mDisposable : Disposable ?= null
    private val OPEN_FILE_CHOOSER = 0
    private val OPEN_DOCUMENT_TREE = 1
    private val OPEN_MANAGE_EXTERNAL_STORAGE  = 2



    private val showWaypoints : ArrayList<WaypointInfoModel> = ArrayList()
    private val pointMarkers : ArrayList<DJIMarker?> = ArrayList()
    var curMissionPath: String = DiskUtil.getExternalCacheDirPath(
        ContextUtil.getContext(),
        WAYPOINT_SAMPLE_FILE_DIR + WAYPOINT_SAMPLE_FILE_NAME
    )
    val rootDir = DiskUtil.getExternalCacheDirPath(ContextUtil.getContext(), WAYPOINT_SAMPLE_FILE_DIR)
    var validLenth: Int = 2
    var curMissionExecuteState: WaypointMissionExecuteState? = null
    var selectWaylines: ArrayList<Int> = ArrayList()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.frag_waypointv3_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareMissionData()
        initView(savedInstanceState)
        initData()
        WPMZManager.getInstance().init(ContextUtil.getContext())
    }

    private fun prepareMissionData() {

        val dir = File(rootDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val cachedirName = DiskUtil.getExternalCacheDirPath(
            ContextUtil.getContext(),
            WAYPOINT_SAMPLE_FILE_CACHE_DIR
        )
        val cachedir = File(cachedirName)
        if (!cachedir.exists()) {
            cachedir.mkdirs()
        }
        val destPath = rootDir + WAYPOINT_SAMPLE_FILE_NAME
        if (!File(destPath).exists()) {
            FileUtils.copyAssetsFile(
                ContextUtil.getContext(),
                WAYPOINT_SAMPLE_FILE_NAME,
                destPath
            )
        }
    }

    private fun initView(savedInstanceState: Bundle?) {
        sp_map_switch.adapter = wayPointV3VM.getMapSpinnerAdapter()

        addListener()
        btn_mission_upload?.setOnClickListener {
            val waypointFile = File(curMissionPath)
            if (waypointFile.exists()) {
                wayPointV3VM.pushKMZFileToAircraft(curMissionPath)
            } else {
                ToastUtils.showToast("Mission file not found!");
            }
            markWaypoints()
        }

        wayPointV3VM.missionUploadState.observe(viewLifecycleOwner) {
            it?.let {
                when {
                    it.error != null -> {
                        mission_upload_state_tv?.text = "Upload State: error:${getErroMsg(it.error)} "
                    }
                    it.tips.isNotEmpty() -> {
                        mission_upload_state_tv?.text = it.tips
                    }
                    else -> {
                        mission_upload_state_tv?.text = "Upload State: progress:${it.updateProgress} "
                    }
                }

            }
        }


        btn_mission_start.setOnClickListener {
            wayPointV3VM.startMission(
                FileUtils.getFileName(curMissionPath, WAYPOINT_FILE_TAG),
                selectWaylines,
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        ToastUtils.showToast("startMission Success")
                    }

                    override fun onFailure(error: IDJIError) {
                        ToastUtils.showToast("startMission Failed " + getErroMsg(error))
                    }
                })

        }

        btn_mission_pause.setOnClickListener {
            wayPointV3VM.pauseMission(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("pauseMission Success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("pauseMission Failed " + getErroMsg(error))
                }
            })

        }

        observeBtnResume()


        btn_wayline_select.setOnClickListener {
            selectWaylines.clear()
            var waylineids = wayPointV3VM.getAvailableWaylineIDs(curMissionPath)
            showMultiChoiceDialog(waylineids)
        }

        kmz_btn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                var intent = Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION")
                startActivityForResult(intent , OPEN_MANAGE_EXTERNAL_STORAGE)
            } else {
                showFileChooser()
            }
        }

        map_locate.setOnClickListener {
            map_widget.setMapCenterLock(MapWidget.MapCenterLock.AIRCRAFT)
        }

        sp_map_switch.setSelection(wayPointV3VM.getMapType(context))

        btn_mission_stop.setOnClickListener {
            if (curMissionExecuteState == WaypointMissionExecuteState.READY) {
                ToastUtils.showToast("Mission not start")
                return@setOnClickListener
            }
            wayPointV3VM.stopMission(
                FileUtils.getFileName(curMissionPath, WAYPOINT_FILE_TAG),
                object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        ToastUtils.showToast("stopMission Success")
                    }

                    override fun onFailure(error: IDJIError) {
                        ToastUtils.showToast("stopMission Failed " + getErroMsg(error))
                    }
                })
        }
        btn_editKmz.setOnClickListener {
            showEditDialog()
        }

        waypoints_clear.setOnClickListener {
            showWaypoints.clear()
            removeAllPoint()
            updateSaveBtn()
        }

        kmz_save.setOnClickListener {
            val kmzOutPath = rootDir + "generate_test.kmz"
            val waylineMission: WaylineMission = createWaylineMission()
            val missionConfig: WaylineMissionConfig = KMZTestUtil.createMissionConfig()
            val template: Template = KMZTestUtil.createTemplate(showWaypoints)
            WPMZManager.getInstance()
                .generateKMZFile(kmzOutPath, waylineMission, missionConfig, template)
            curMissionPath  = kmzOutPath
            ToastUtils.showToast("Save Kmz Success Path is : $kmzOutPath")

            waypoint_add.isChecked = false;
        }

        btn_breakpoint_resume.setOnClickListener{
            var missionName = FileUtils.getFileName(curMissionPath , WAYPOINT_FILE_TAG );
            WaypointMissionManager.getInstance().queryBreakPointInfoFromAircraft(missionName
                , object :CommonCallbacks.CompletionCallbackWithParam<BreakPointInfo>{
                override fun onSuccess(breakPointInfo: BreakPointInfo?) {
                    breakPointInfo?.let {
                        resumeFromBreakPoint(missionName , it)
                    }
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("queryBreakPointInfo error $error")
                }

            })
        }

        addMapListener()

        createMapView(savedInstanceState)
        observeAircraftLocation()
    }

    private fun observeAircraftLocation() {
        val location = KeyManager.getInstance()
            .getValue(KeyTools.createKey(FlightControllerKey.KeyAircraftLocation), LocationCoordinate2D(0.0,0.0))
        val isEnable = SimulatorManager.getInstance().isSimulatorEnabled
        if (!GpsUtils.isLocationValid(location) && !isEnable) {
            ToastUtils.showToast("please open simulator")
        }
    }

    private fun observeBtnResume() {
        btn_mission_resume.setOnClickListener {
            var wp_breakinfo_index = wp_break_index.text.toString()
            var wp_breakinfo_progress = wp_break_progress.text.toString()
            if (!TextUtils.isEmpty(wp_breakinfo_index) && !TextUtils.isEmpty(wp_breakinfo_progress)) {

                var breakPointInfo = BreakPointInfo(0 ,wp_breakinfo_index.toInt(),wp_breakinfo_progress.toDouble())
                wayPointV3VM.resumeMission(breakPointInfo , object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        ToastUtils.showToast("resumeMission with BreakInfo Success")
                    }

                    override fun onFailure(error: IDJIError) {
                        ToastUtils.showToast("resumeMission with BreakInfo Failed " + getErroMsg(error))
                    }
                })
            }
            else {
                wayPointV3VM.resumeMission(object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        ToastUtils.showToast("resumeMission Success")
                    }

                    override fun onFailure(error: IDJIError) {
                        ToastUtils.showToast("resumeMission Failed " + getErroMsg(error))
                    }
                })
            }
        }
    }

    private fun resumeFromBreakPoint(missionName :String , breakPointInfo: BreakPointInfo ){
        var wp_breakinfo_index = wp_break_index.text.toString()
        var wp_breakinfo_progress = wp_break_progress.text.toString()
        if (!TextUtils.isEmpty(wp_breakinfo_index) && !TextUtils.isEmpty(wp_breakinfo_progress)) {
            breakPointInfo.segmentProgress = wp_breakinfo_progress.toDouble()
            breakPointInfo.waypointID = wp_breakinfo_index.toInt()
        }
        wayPointV3VM.startMission(missionName , breakPointInfo , object :CommonCallbacks.CompletionCallback{
            override fun onSuccess() {
                ToastUtils.showToast("resume success");
            }

            override fun onFailure(error: IDJIError) {
               ToastUtils.showToast("resume error $error")
            }

        })
    }

    private  fun addMapListener(){

        waypoint_add.setOnCheckedChangeListener { _, isOpen ->
            if (isOpen) {
                map_widget.map?.setOnMapClickListener{
                    showWaypointDlg(it , object :CommonCallbacks.CompletionCallbackWithParam<WaypointInfoModel>{
                        override fun onSuccess(waypointInfoModel: WaypointInfoModel) {
                            showWaypoints.add( waypointInfoModel)
                            showWaypoints()
                            updateSaveBtn()
                            ToastUtils.showToast("lat" + it.latitude + " lng" + it.longitude)
                        }
                        override fun onFailure(error: IDJIError) {
                            ToastUtils.showToast("add Failed " )
                        }
                    })
                }
            } else {
                map_widget.map?.removeAllOnMapClickListener()
            }
        }
    }

    private fun addListener(){
        wayPointV3VM.addMissionStateListener() {
            mission_execute_state_tv?.text = "Mission Execute State : ${it.name}"
            btn_mission_upload.isEnabled = it == WaypointMissionExecuteState.READY
            curMissionExecuteState = it
            if (it == WaypointMissionExecuteState.FINISHED) {
                ToastUtils.showToast("Mission Finished")
            }
            LogUtils.i(logTag , "State is ${it.name}")
        }
        wayPointV3VM.addWaylineExecutingInfoListener(object :WaylineExecutingInfoListener {
            override fun onWaylineExecutingInfoUpdate(it: WaylineExecutingInfo) {
                wayline_execute_state_tv?.text = "Wayline Execute Info WaylineID:${it.waylineID} \n" +
                        "WaypointIndex:${it.currentWaypointIndex} \n" +
                        "MissionName : ${ it.missionFileName}"
            }

            override fun onWaylineExecutingInterruptReasonUpdate(error: IDJIError?) {
                if (error != null) {
                    LogUtils.e(logTag , "interrupt error${error.description()}")
                }
            }

        });


        wayPointV3VM.addWaypointActionListener(object :WaypointActionListener{
            override fun onExecutionStart(actionId: Int) {
                waypint_action_state_tv?.text = "onExecutionStart: ${actionId} "
            }

            override fun onExecutionStart(actionGroup: Int , actionId: Int ) {
                waypint_action_state_tv?.text = "onExecutionStart:${actionGroup}: ${actionId} "
            }

            override fun onExecutionFinish(actionId: Int, error: IDJIError?) {
                waypint_action_state_tv?.text = "onExecutionFinish: ${actionId} "
            }

            override fun onExecutionFinish(actionGroup: Int, actionId: Int,  error: IDJIError?) {
                waypint_action_state_tv?.text = "onExecutionFinish:${actionGroup}: ${actionId} "
            }

        })
    }

    fun updateSaveBtn(){
        kmz_save.isEnabled = showWaypoints.isNotEmpty()
    }
    private fun showEditDialog() {
        val waypointFile = File(curMissionPath)
        if (!waypointFile.exists()) {
            ToastUtils.showToast("Please upload kmz file")
            return
        }

        val unzipFolder = File(rootDir, unzipChildDir)
        // 解压后的waylines路径
        val templateFile = File(rootDir + unzipChildDir + unzipDir, WPMZParserManager.TEMPLATE_FILE)
        val waylineFile = File(rootDir + unzipChildDir + unzipDir, WPMZParserManager.WAYLINE_FILE)

        mDisposable = Single.fromCallable {
            //在cache 目录创建一个wmpz文件夹，并将template.kml 与 waylines.wpml 拷贝进wpmz ，然后压缩wpmz文件夹
            WPMZParserManager.unZipFolder(ContextUtil.getContext(), curMissionPath, unzipFolder.path, false)
            FileUtils.readFile(waylineFile.path , null)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { wpmlContent: String? ->
                    DialogUtil.showInputDialog(requireActivity() ,"",wpmlContent , "", false , object :CommonCallbacks.CompletionCallbackWithParam<String> {
                        override fun onSuccess(newContent: String?) {
                            newContent?.let {
                                updateWPML(it)
                            }
                        }
                        override fun onFailure(error: IDJIError) {
                            LogUtils.e(logTag , "show input Dialog Failed ${error.description()} ")
                        }

                    })
                }
            ) { throwable: Throwable ->
                LogUtils.e(logTag , "show input Dialog Failed ${throwable.message} ")
            }
    }

    private fun updateWPML(newContent: String) {
        val waylineFile = File(rootDir + unzipChildDir + unzipDir, WPMZParserManager.WAYLINE_FILE)

        Single.fromCallable {
            FileUtils.writeFile(waylineFile.path, newContent, false)
            //将修改后的waylines.wpml重新压缩打包成 kmz
            val zipFiles = mutableListOf<String>()
            val cacheFolder = File(rootDir, unzipChildDir + unzipDir)
            var zipFile = File(rootDir + unzipChildDir + "waypoint.kmz")
            if (waylineFile.exists()) {
                zipFiles.add(cacheFolder.path)
                zipFile.createNewFile()
                WPMZParserManager.zipFiles(ContextUtil.getContext(), zipFiles, zipFile.path)
            }
            //将用户选择的kmz用修改的后的覆盖
            FileUtils.copyFileByChannel(zipFile.path, curMissionPath)
        }.subscribeOn(Schedulers.io()).subscribe()

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OPEN_FILE_CHOOSER) {
            data?.apply {
                getData()?.let {
                    curMissionPath = getPath(context, it)
                    checkPath()
                }
            }

        }

        if (requestCode == OPEN_DOCUMENT_TREE) {
            grantUriPermission(  data)
        }


        if (requestCode == OPEN_MANAGE_EXTERNAL_STORAGE
             && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            showFileChooser()
        }


    }

    fun checkPath(){
        if (!curMissionPath.contains(".kmz") && !curMissionPath.contains(".kml")) {
            ToastUtils.showToast("Please choose KMZ/KML file")
        } else {

            // Choose a directory using the system's file picker.
            showPermisssionDucument()

            if (curMissionPath.contains(".kml") ){
                if (WPMZManager.getInstance().transKMLtoKMZ(curMissionPath , "" , getHeightMode())) {
                    curMissionPath  =   Environment.getExternalStorageDirectory()
                        .toString() + "/DJI/" + requireContext().packageName + "/KMZ/OutPath/" + getName(curMissionPath) + ".kmz"
                    ToastUtils.showToast("Trans kml success " + curMissionPath)
                } else {
                    ToastUtils.showToast("Trans kml failed!")
                }
            } else {
                ToastUtils.showToast("KMZ file path:${curMissionPath}")
            }
        }
    }
    fun getName(path: String): String? {
        val start = path.lastIndexOf("/")
        val end = path.lastIndexOf(".")
        return if (start != -1 && end != -1) {
            path.substring(start + 1, end)
        } else {
            "unknow"
        }
    }
    fun showPermisssionDucument() {
        val canWrite: Boolean =
            DocumentsUtils.checkWritableRootPath(context, curMissionPath)
        if (!canWrite && Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            val storageManager =
                requireActivity().getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val volume: StorageVolume? =
                storageManager.getStorageVolume(File(curMissionPath))
            if (volume != null) {
                val intent = volume.createOpenDocumentTreeIntent()
                startActivityForResult(intent, OPEN_DOCUMENT_TREE)
                return
            }
        }
    }

    fun showFileChooser(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(
            Intent.createChooser(intent, "Select KMZ File"), OPEN_FILE_CHOOSER
        )
    }
    fun grantUriPermission(data: Intent?) {

        val uri = data!!.data
        requireActivity().grantUriPermission(getPackageName(), uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val takeFlags = data.flags and (Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_READ_URI_PERMISSION)
        requireActivity().getContentResolver().takePersistableUriPermission(uri!!, takeFlags)
    }

    fun getPath(context: Context?, uri: Uri?): String {
        if (DocumentsContract.isDocumentUri(context, uri) && isExternalStorageDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).toTypedArray()
            if (split.size != validLenth) {
                return ""
            }
            val type = split[0]
            if ("primary".equals(type, ignoreCase = true)) {
                return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            } else {
                return getExtSdCardPaths(requireContext()).get(0)!! + "/" + split[1]
            }
        }
        return ""
    }

    private fun getExtSdCardPaths(context: Context): ArrayList<String?> {
        var sExtSdCardPaths = ArrayList<String?>()
        for (file in context.getExternalFilesDirs("external")) {
            if (file != null && file != context.getExternalFilesDir("external")) {
                val index = file.absolutePath.lastIndexOf("/Android/data")
                if (index >= 0) {
                    var path: String? = file.absolutePath.substring(0, index)
                    try {
                        path = File(path).canonicalPath
                    } catch (e: IOException) {
                        LogUtils.e(logTag, e.message)
                    }
                    sExtSdCardPaths.add(path)
                }
            }
        }
        if (sExtSdCardPaths.isEmpty()) {
            sExtSdCardPaths.add("/storage/sdcard1")
        }
        return sExtSdCardPaths
    }

    fun isExternalStorageDocument(uri: Uri?): Boolean {
        return "com.android.externalstorage.documents" == uri?.authority
    }

    private fun initData() {
        wayPointV3VM.listenFlightControlState()

        wayPointV3VM.flightControlState.observe(viewLifecycleOwner) {
            it?.let {
                wayline_aircraft_height?.text = String.format("Aircraft Height: %.2f", it.height)
                wayline_aircraft_distance?.text =
                    String.format("Aircraft Distance: %.2f", it.distance)
                wayline_aircraft_speed?.text = String.format("Aircraft Speed: %.2f", it.speed)
            }
        }
    }



    @IntDef(
        MapProvider.MAP_AUTO,
        MapProvider.AMAP_PROVIDER,
        MapProvider.MAPLIBRE_PROVIDER,
        MapProvider.GOOGLE_PROVIDER
    )
    annotation class MapProvider {
        companion object {
            const val MAP_AUTO = 0
            const val AMAP_PROVIDER = 1
            const val MAPLIBRE_PROVIDER = 2
            const val GOOGLE_PROVIDER = 3
        }
    }

    private fun createMapView(savedInstanceState: Bundle?) {
        val onMapReadyListener = MapWidget.OnMapReadyListener { map ->
            map.setMapType(DJIMap.MapType.NORMAL)
        }
        map_widget.initAMap(onMapReadyListener)

        map_widget.onCreate(savedInstanceState) //需要再init后调用否则Amap无法显示
    }

    override fun onPause() {
        super.onPause()
        map_widget.onPause()
    }

    override fun onResume() {
        super.onResume()
        map_widget.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        map_widget.onDestroy()
    }

    override fun onDestroy() {
        super.onDestroy()
        wayPointV3VM.cancelListenFlightControlState()
        wayPointV3VM.removeAllMissionStateListener()
        wayPointV3VM.clearAllWaylineExecutingInfoListener()
        wayPointV3VM.clearAllWaypointActionListener()

        mDisposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
    }

    fun getErroMsg(error: IDJIError): String {
        if (!TextUtils.isEmpty(error.description())) {
            return error.description();
        }
        return error.errorCode()
    }


    fun showMultiChoiceDialog(waylineids: List<Int>) {
        var items: ArrayList<String> = ArrayList()
        waylineids
            .filter {
                it >= 0
            }
            .map {
                items.add(it.toString())
            }

        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        builder.setTitle("Select Wayline")
        builder.setPositiveButton("OK", null)
        builder.setMultiChoiceItems(
            items.toTypedArray(),
            null,
            object : OnMultiChoiceClickListener {
                override fun onClick(p0: DialogInterface?, index: Int, isSelect: Boolean) {
                    if (isSelect) {
                        selectWaylines.add(index)
                    } else {
                        selectWaylines.remove(index)
                    }
                }
            }).create().show()

    }

    fun markWaypoints() {
        // version参数实际未用到
        var waypoints: ArrayList<WaylineExecuteWaypoint> = ArrayList<WaylineExecuteWaypoint>()
        val parseInfo = JNIWPMZManager.getWaylines("1.0.0", curMissionPath)
        var waylines = parseInfo.waylines
        waylines.forEach() {
            waypoints.addAll(it.waypoints)
            markLine(it.waypoints)
        }
        waypoints.forEach() {
            markWaypoint(DJILatLng(it.location.latitude, it.location.longitude), it.waypointIndex)
        }
    }

    fun markWaypoint(latlong: DJILatLng, waypointIndex: Int) : DJIMarker?{
        var markOptions = DJIMarkerOptions()
        markOptions.position(latlong)
        markOptions.icon(getMarkerRes(waypointIndex, 0f))
        markOptions.title(waypointIndex.toString())
        markOptions.isInfoWindowEnable = true
       return map_widget.map?.addMarker(markOptions)
    }

    fun markLine(waypoints: List<WaylineExecuteWaypoint>) {

        var djiwaypoints = waypoints.filter {
            true
        }.map {
            DJILatLng(it.location.latitude, it.location.longitude)
        }
        var lineOptions = DJIPolylineOptions()
        lineOptions.width(5f)
        lineOptions.color(Color.GREEN)
        lineOptions.addAll(djiwaypoints)
        map_widget.map?.addPolyline(lineOptions)
    }



    /**
     * Convert view to bitmap
     * Notice: recycle the bitmap after use
     */
    fun getMarkerBitmap(
        index: Int,
        rotation: Float,
    ): Bitmap? {
        // create View for marker
        @SuppressLint("InflateParams") val markerView: View =
            LayoutInflater.from(activity)
                .inflate(R.layout.waypoint_marker_style_layout, null)
        val markerBg = markerView.findViewById<ImageView>(R.id.image_content)
        val markerTv = markerView.findViewById<TextView>(R.id.image_text)
        markerTv.text = index.toString()
        markerTv.setTextColor(AndUtil.getResColor(R.color.blue))
        markerTv.textSize =
            AndUtil.getDimension(R.dimen.mission_waypoint_index_text_large_size)

        markerBg.setImageResource(R.mipmap.mission_edit_waypoint_normal)

        markerBg.rotation = rotation
        // convert view to bitmap
        markerView.destroyDrawingCache()
        markerView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)
        markerView.isDrawingCacheEnabled = true
        return markerView.getDrawingCache(true)
    }

    private fun getMarkerRes(
        index: Int,
        rotation: Float,
    ): DJIBitmapDescriptor? {
        return DJIBitmapDescriptorFactory.fromBitmap(
            getMarkerBitmap(index , rotation)
        )
    }

    fun showWaypoints(){
        var loction2D = showWaypoints.last().waylineWaypoint.location
        val waypoint =  DJILatLng(loction2D.latitude , loction2D.longitude)
       var pointMarker =  markWaypoint(waypoint , getCurWaypointIndex())
        pointMarkers.add(pointMarker)
    }

    fun getCurWaypointIndex():Int{
        if (showWaypoints.size <= 0) {
            return 0
        }
        return showWaypoints.size
    }
    private fun showWaypointDlg( djiLatLng: DJILatLng ,callbacks: CommonCallbacks.CompletionCallbackWithParam<WaypointInfoModel>) {
        val builder = AlertDialog.Builder(requireActivity())
        val dialog = builder.create()
        val dialogView = View.inflate(requireActivity(), R.layout.dialog_add_waypoint, null)
        dialog.setView(dialogView)

        val etHeight = dialogView.findViewById<View>(R.id.et_height) as EditText
        val etSpd = dialogView.findViewById<View>(R.id.et_speed) as EditText
        val viewActionType = dialogView.findViewById<View>(R.id.action_type) as DescSpinnerCell
        val btnLogin = dialogView.findViewById<View>(R.id.btn_add) as Button
        val btnCancel = dialogView.findViewById<View>(R.id.btn_cancel) as Button

        btnLogin.setOnClickListener {
            var waypointInfoModel =  WaypointInfoModel()
            val waypoint = WaylineWaypoint()
            waypoint.waypointIndex = getCurWaypointIndex()
            val location = WaylineLocationCoordinate2D(djiLatLng.latitude , djiLatLng.longitude)
            waypoint.location = location
            waypoint.height = etHeight.text.toString().toDouble()
            // 根据坐标类型，如果为egm96 需要加上高程差
            waypoint.ellipsoidHeight = etHeight.text.toString().toDouble()
            waypoint.speed = etSpd.text.toString().toDouble()
            waypoint.useGlobalTurnParam = true
            waypointInfoModel.waylineWaypoint = waypoint
            val actionInfos: MutableList<WaylineActionInfo> = ArrayList()
            actionInfos.add(KMZTestUtil.createActionInfo(getCurActionType(viewActionType)))
            waypointInfoModel.waylineWaypoint = waypoint
            waypointInfoModel.actionInfos = actionInfos
            callbacks.onSuccess(waypointInfoModel)
            dialog.dismiss()
        }
        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun getHeightMode(): HeightMode {
        return  when(heightmode.getSelectPosition()){
           0 -> HeightMode.WGS84
           1-> HeightMode.EGM96
           2 -> HeightMode.RELATIVE
            else -> {
                HeightMode.WGS84
            }
        }
    }

    private fun getCurActionType(viewActionType: DescSpinnerCell): WaypointActionType? {
        return when (viewActionType.getSelectPosition()) {
            0 -> WaypointActionType.START_TAKE_PHOTO
            1 -> WaypointActionType.START_RECORD
            2 -> WaypointActionType.STOP_RECORD
            3 -> WaypointActionType.GIMBAL_PITCH
            else -> {
                WaypointActionType.START_TAKE_PHOTO
            }
        }
    }
    private  fun removeAllPoint(){
        pointMarkers.forEach{
            it?.let {
                it.remove()
            }
        }
    }
}