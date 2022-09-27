package dji.sampleV5.moduleaircraft.pages

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
import android.widget.AdapterView
import androidx.annotation.IntDef
import androidx.fragment.app.activityViewModels
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.models.WayPointV3VM
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.utils.common.*
import kotlinx.android.synthetic.main.frag_waypointv3_page.*
import java.io.File
import java.util.*


import com.dji.mapkit.amap.provider.AMapProvider
import com.dji.mapkit.core.Mapkit
import com.dji.mapkit.core.MapkitOptions
import com.dji.mapkit.core.camera.DJICameraUpdate
import com.dji.mapkit.core.camera.DJICameraUpdateFactory
import com.dji.mapkit.core.maps.DJIMap
import com.dji.mapkit.core.maps.DJIMapView
import com.dji.mapkit.core.maps.DJIMapView.OnDJIMapReadyCallback
import com.dji.mapkit.core.models.DJIBitmapDescriptor
import com.dji.mapkit.core.models.DJIBitmapDescriptorFactory
import com.dji.mapkit.core.models.DJICameraPosition
import com.dji.mapkit.core.models.DJILatLng
import com.dji.mapkit.google.provider.GoogleProvider
import com.dji.mapkit.maplibre.provider.MapLibreProvider
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.v5.manager.aircraft.waypoint3.model.WaypointMissionExecuteState
import java.io.IOException
import kotlin.collections.ArrayList
import android.content.DialogInterface

import android.content.DialogInterface.OnMultiChoiceClickListener
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.ImageView
import android.widget.TextView
import com.dji.mapkit.core.models.annotations.*
import dji.sdk.wpmz.jni.JNIWPMZManager
import dji.sdk.wpmz.value.mission.WaylineExecuteWaypoint
import dji.v5.ux.core.util.AndUtil


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

    var mapView: DJIMapView? = null
    var map: DJIMap? = null
    var mapkitOptions: MapkitOptions? = null
    var droneBitmap: DJIBitmapDescriptor? = null
    var waypointBitmap: DJIBitmapDescriptor? = null
    var homePointBitMap: DJIBitmapDescriptor? = null
    var mAircraftMarker: DJIMarker? = null
    var homePointMarKer: DJIMarker? = null
    var curMissionPath: String = DiskUtil.getExternalCacheDirPath(
        ContextUtil.getContext(),
        WAYPOINT_SAMPLE_FILE_DIR + WAYPOINT_SAMPLE_FILE_NAME
    )
    var validLenth: Int = 2
    var curMissionExecuteState: WaypointMissionExecuteState? = null
    var selectWaylines: ArrayList<Int> = ArrayList()
    var homeLine: DJIPolyline? = null
    var AircraftPos: DJILatLng? = null
    var isNeedToCenter: Boolean = true

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
    }

    private fun prepareMissionData() {
        val dirName =
            DiskUtil.getExternalCacheDirPath(ContextUtil.getContext(), WAYPOINT_SAMPLE_FILE_DIR)
        val dir = File(dirName)
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
        val destPath = dirName + WAYPOINT_SAMPLE_FILE_NAME
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
        wayPointV3VM.addMissionStateListener() {
            mission_execute_state_tv?.text = "Mission Execute State : ${it.name}"
            btn_mission_upload.isEnabled = it == WaypointMissionExecuteState.READY
            curMissionExecuteState = it
        }
        wayPointV3VM.addWaylineExecutingInfoListener() {
            wayline_execute_state_tv?.text = "Wayline Execute Info WaylineID:${it.waylineID} \n" +
                    "WaypointIndex:${it.currentWaypointIndex} \n" +
                    "MissionName : ${if (curMissionExecuteState == WaypointMissionExecuteState.READY) "" else it.missionID}"

        }

        btn_mission_upload?.setOnClickListener {

            //  var missionPath  = curMissionPath?:DiskUtil.getExternalCacheDirPath(ContextUtil.getContext(), WAYPOINT_SAMPLE_FILE_DIR + WAYPOINT_SAMPLE_FILE_NAME)
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
                if (it.error != null) {
                    mission_upload_state_tv?.text = "Upload State: error:${getErroMsg(it.error)} "
                } else if (it.tips.isNotEmpty()) {
                    mission_upload_state_tv?.text = it.tips
                } else {
                    mission_upload_state_tv?.text = "Upload State: progress:${it.updateProgress} "
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

        btn_mission_resume.setOnClickListener {
            wayPointV3VM.resumeMission(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("resumeMission Success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("resumeMission Failed " + getErroMsg(error))
                }
            })

        }

        btn_wayline_select.setOnClickListener {
            if (curMissionPath == null) {
                ToastUtils.showToast("please upload mission")
                return@setOnClickListener
            }
            selectWaylines.clear()
            var waylineids = wayPointV3VM.getAvailableWaylineIDs(curMissionPath)
            showMultiChoiceDialog(waylineids)
        }

        kmz_btn.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(
                Intent.createChooser(intent, "Select KMZ File"), 0
            )
        }

        map_locate.setOnClickListener {
            isNeedToCenter = true
            AircraftPos?.let {
                moveToCenter(map?.getCameraPosition()!!.zoom, AircraftPos!!)
            }
        }

        sp_map_switch.setSelection(wayPointV3VM.getMapType(context))
        sp_map_switch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                mapSwitch(pos)
                wayPointV3VM.saveMapType(context, pos)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                // donothing
            }
        }
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

        mapView?.onCreate(savedInstanceState)
        droneBitmap = DJIBitmapDescriptorFactory.fromResource(R.mipmap.aircraft)
        waypointBitmap = DJIBitmapDescriptorFactory.fromResource(R.mipmap.waypoint_position)
        homePointBitMap = DJIBitmapDescriptorFactory.fromResource(R.mipmap.home_point)


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.apply {
            getData()?.let {
                curMissionPath = getPath(context, it)
                if (curMissionPath?.contains(".kmz") == false) {
                    ToastUtils.showToast("Please choose KMZ file")
                } else {
                    ToastUtils.showToast("KMZ file path:${curMissionPath}")
                }
            }
        }
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
                updateAircraftLocation(it.latitude, it.longtitude, it.head, it.homeLocation)
            }
        }
    }

    private fun updateAircraftLocation(
        aircraftLat: Double,
        aircraftLng: Double,
        aircraftHead: Float,
        homePoint: LocationCoordinate2D,
    ) {
        if (map == null) {
            return
        }
        AircraftPos = DJILatLng(aircraftLat, aircraftLng)
        val zoomLevel = map?.getCameraPosition()!!.zoom


        val markerOptions = DJIMarkerOptions()
        markerOptions.position(AircraftPos)
        markerOptions.icon(droneBitmap)
        markerOptions.zIndex(1)
        markerOptions.anchor(0.5f, 0.5f)

        val marOptionHomePoint = DJIMarkerOptions()
        var homeCoordinate = DJILatLng(homePoint.latitude, homePoint.longitude)
        marOptionHomePoint.position(homeCoordinate)
        marOptionHomePoint.icon(homePointBitMap)
        marOptionHomePoint.anchor(0.5f, 0.5f)

        if (isLocationValid(homePoint.latitude, homePoint.longitude)) {
            if (homePointMarKer == null) {
                homePointMarKer = map?.addMarker(marOptionHomePoint)
            } else {
                homePointMarKer?.position = DJILatLng(homePoint.latitude, homePoint.longitude)
            }
        }

        if (isLocationValid(aircraftLat, aircraftLng)) {
            if (mAircraftMarker != null) {
                // mAircraftMarker!!.remove()
                mAircraftMarker?.position = AircraftPos
            } else {
                mAircraftMarker = map?.addMarker(markerOptions)
            }

            mAircraftMarker!!.rotation = (aircraftHead - map!!.getCameraPosition().bearing)
            moveToCenter(zoomLevel, AircraftPos)

        }
        updateHomeLine(mAircraftMarker!!, homeCoordinate)
    }

    fun moveToCenter(zoomLevel: Float, pos: DJILatLng?) {
        if (isNeedToCenter) {
            val cameraPosition = DJICameraPosition.Builder()
                .target(pos)
                .zoom(if (wayPointV3VM.getMapType(context) == MapProvider.MAPLIBRE_PROVIDER) -1.0f else zoomLevel)
                .build()
            val cu: DJICameraUpdate = DJICameraUpdateFactory.newCameraPosition(cameraPosition)
            map!!.animateCamera(cu)
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

    fun createMapView(@MapProvider type: Int) {
        // 初始化MapKit
        Mapkit.inHongKong(wayPointV3VM.isHongKong())
        Mapkit.inMacau(wayPointV3VM.isMacau())
        Mapkit.inMainlandChina(wayPointV3VM.isInMainlandChina())

        val useAmap = wayPointV3VM.isInMainlandChina();
        val builder = MapkitOptions.Builder()

        builder.addMapProvider(
            if (type == MapProvider.AMAP_PROVIDER) {
                AMapProvider().providerType
            } else if (type == MapProvider.MAPLIBRE_PROVIDER) {
                MapLibreProvider().providerType
            } else if (type == MapProvider.GOOGLE_PROVIDER) {
                GoogleProvider().providerType
            } else {
                if (useAmap) AMapProvider().providerType else MapLibreProvider().providerType
            }
        )
        mapkitOptions = builder.build()
        mapView = DJIMapView(activity, builder.build())
        mapView?.getDJIMapAsync(OnDJIMapReadyCallback {
            map = it
            resetMarker()
            map?.addOnCameraChangeListener {
                isNeedToCenter = false
            }

        })
        wp_map.addView(mapView)
    }

    fun resetMarker() {
        homePointMarKer = null
        mAircraftMarker = null
    }

    fun mapSwitch(@MapProvider type: Int) {
        //销毁
        mapView?.apply {
            onPause()
            onStop()
            onDestroy()
            wp_map.removeView(this)
        }
        //重建
        createMapView(type)
        mapView?.apply {
            onCreate(null)
            onStart()
            onResume()
        }
    }

    fun isLocationValid(latitude: Double, longitude: Double): Boolean {
        return latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180 && latitude != 0.0 && longitude != 0.0
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDestroy()
    }

    override fun onDestroy() {
        super.onDestroy()
        wayPointV3VM.cancelListenFlightControlState()
        wayPointV3VM.removeAllMissionStateListener()
        wayPointV3VM.clearAllWaylineExecutingInfoListener()
        map?.removeAllOnCameraChangeListeners()
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

    fun markWaypoint(latlong: DJILatLng, waypointIndex: Int) {
        var markOptions = DJIMarkerOptions()
        markOptions.position(latlong)
        markOptions.icon(getMarkerRes(waypointIndex, 0f))
        markOptions.title(waypointIndex.toString())
        markOptions.isInfoWindowEnable = true
        map?.addMarker(markOptions)
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
        map?.addPolyline(lineOptions)
    }

    fun updateHomeLine(aircraftMarker: DJIMarker, homeCoordinate: DJILatLng) {

        if (homeLine != null) {
            val points: MutableList<DJILatLng> = java.util.ArrayList()
            points.add(aircraftMarker.getPosition())
            points.add(homeCoordinate)
            homeLine!!.setPoints(points)
        } else {
            //create new line
            val homeLineOptions = DJIPolylineOptions().add(aircraftMarker.getPosition())
                .add(homeCoordinate)
                .color(Color.WHITE)
                .width(5f)

            //draw new line
            homeLine = map!!.addPolyline(homeLineOptions)
        }
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
            getMarkerBitmap(index + 1, rotation)
        )
    }

}