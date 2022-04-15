package dji.sampleV5.moduleaircraft.pages

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.annotation.IntDef
import androidx.fragment.app.activityViewModels
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.models.WayPointV3VM
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.utils.common.*
import kotlinx.android.synthetic.main.frag_waypointv3_page.*
import java.io.File
import java.util.*
import kotlin.collections.HashMap


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
import com.dji.mapkit.core.models.annotations.DJIMarker
import com.dji.mapkit.core.models.annotations.DJIMarkerOptions
import com.dji.mapkit.google.provider.GoogleProvider
import com.dji.mapkit.maplibre.provider.MapLibreProvider
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.v5.manager.aircraft.waypoint3.model.WaypointMissionExecuteState


/**
 * @author feel.feng
 * @time 2022/02/27 9:30 上午
 * @description:
 */
class WayPointV3Fragment : DJIFragment() {

    private val wayPointV3VM: WayPointV3VM by activityViewModels()
    private val WAYPOINT_SAMPLE_FILE_NAME : String = "waypointsample.kmz"
    private val WAYPOINT_SAMPLE_FILE_DIR : String = "waypoint/"
    private val WAYPOINT_SAMPLE_FILE_CACHE_DIR : String = "waypoint/cache/"
    private val MISSION_ID = "sample"  // 每个航线任务的唯一ID ，由用户自行定义
    val missionHashMap : HashMap<String,String> = HashMap<String,String>()
    var mapView : DJIMapView? = null
    var map :DJIMap? = null
    var mapkitOptions:MapkitOptions? =null
    var droneBitmap: DJIBitmapDescriptor? = null
    var homePointBitMap:DJIBitmapDescriptor? = null
    var mAircraftMarker: DJIMarker? = null
    var homePointMarKer: DJIMarker? = null
    var curMissionPath : String? = null
    var validLenth :Int = 2


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        return  inflater.inflate(R.layout.frag_waypointv3_page, container, false)

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
        val cachedirName = DiskUtil.getExternalCacheDirPath(ContextUtil.getContext(), WAYPOINT_SAMPLE_FILE_CACHE_DIR)
        val cachedir = File(cachedirName)
        if (!cachedir.exists()) {
            cachedir.mkdirs()
        }
        val destPath = dirName + WAYPOINT_SAMPLE_FILE_NAME
        if (!File(destPath).exists()) {
            FileUtils.copyAssetsFile(
                ContextUtil.getContext(),
                WAYPOINT_SAMPLE_FILE_NAME,
                destPath)
        }
    }

    private fun initView(savedInstanceState: Bundle?) {

        sp_map_switch.adapter = wayPointV3VM.getMapSpinnerAdapter()
        wayPointV3VM.addMissionStateListener() {
            mission_execute_state_tv?.text = "Mission Execute State : ${it.name}"
            btn_mission_upload.isEnabled = it == WaypointMissionExecuteState.READY
        }
        wayPointV3VM.addWaylineExecutingInfoListener(){
            wayline_execute_state_tv?.text = "Wayline Execute Info WaylineID:${it.waylineID} \n" +
                    "WaypointIndex:${it.currentWaypointIndex}"
        }

       btn_mission_upload?.setOnClickListener {

           var missionPath  = curMissionPath?:DiskUtil.getExternalCacheDirPath(ContextUtil.getContext(), WAYPOINT_SAMPLE_FILE_DIR + WAYPOINT_SAMPLE_FILE_NAME)
           var cachePath = DiskUtil.getExternalCacheDirPath(ContextUtil.getContext(), WAYPOINT_SAMPLE_FILE_CACHE_DIR)
           val waypointFile = File( missionPath )

           // 上传时生成一个uuid 将missionid与uuid绑定
           val uuid =   UUID.randomUUID().toString().replace("-", "_")
           missionHashMap.put(MISSION_ID , uuid)
           val file = File(cachePath)
           val renameFile = File(file.parentFile, "$uuid.kmz")
           renameFile.createNewFile()

           FileUtils.copyFileByChannel(missionPath, renameFile.path)

           if (waypointFile.exists()) {
               wayPointV3VM.pushKMZFileToAircraft(MISSION_ID, renameFile.path)
           } else{
               ToastUtils.showToast("Mission file not found!");
           }
       }

        wayPointV3VM.missionUploadState.observe(viewLifecycleOwner) {
            it?.let {
              if (it.error != null) {
                  mission_upload_state_tv?.text = "Upload State: error:${it.error.description()} "
              } else if (it.tips.isNotEmpty()) {
                  mission_upload_state_tv?.text = it.tips
              } else{
                  mission_upload_state_tv?.text = "Upload State: progress:${it.updateProgress} "
              }

            }
        }


        btn_mission_start.setOnClickListener {
            val missionId : String? = missionHashMap.get(MISSION_ID)
            if (missionId != null) {
                wayPointV3VM.startMission(missionId ,object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        ToastUtils.showToast("startMission Success")
                    }

                    override fun onFailure(error: IDJIError) {
                        ToastUtils.showToast("startMission Failed " + error.description())
                    }

                } )
            } else{
                ToastUtils.showToast("Please Upload Mission")
            }
        }

        btn_mission_pause.setOnClickListener {
            wayPointV3VM.pauseMission(object :CommonCallbacks.CompletionCallback{
                override fun onSuccess() {
                    ToastUtils.showToast("pauseMission Success")
                }
                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("pauseMission Failed " + error.description())
                }
            })

        }

        btn_mission_resume.setOnClickListener {
            wayPointV3VM.resumeMission(object :CommonCallbacks.CompletionCallback{
                override fun onSuccess() {
                    ToastUtils.showToast("resumeMission Success")
                }
                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("resumeMission Failed " + error.description())
                }
            })

        }

        kmz_btn.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(
                Intent.createChooser(intent, "Select KMZ File"), 0)
        }

        sp_map_switch.setSelection(wayPointV3VM.getMapType(context))
        sp_map_switch.onItemSelectedListener = object :AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
               mapSwitch(pos)
               wayPointV3VM.saveMapType(context,pos)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                // donothing
            }

        }
        btn_mission_stop.setOnClickListener {
            val missionId : String? = missionHashMap.get(MISSION_ID)
            missionId ?.let {
                wayPointV3VM.stopMission(missionId , object :CommonCallbacks.CompletionCallback{
                    override fun onSuccess() {
                        ToastUtils.showToast("stopMission Success")
                    }
                    override fun onFailure(error: IDJIError) {
                        ToastUtils.showToast("stopMission Failed " + error.description())
                    }
                })
            }
        }


        mapView?.onCreate(savedInstanceState)


        droneBitmap = DJIBitmapDescriptorFactory.fromResource(R.mipmap.aircraft)
        homePointBitMap  = DJIBitmapDescriptorFactory.fromResource(R.mipmap.home_point)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.apply {
            getData()?.let {
                curMissionPath = getPath(context, it)
                if (curMissionPath?.contains(".kmz") == false) {
                    ToastUtils.showToast("Please choose KMZ file")
                } else{
                    ToastUtils.showToast("KMZ file path:${curMissionPath}")
                }
            }
        }
    }
    fun getPath(context: Context?, uri: Uri?): String? {
        if (DocumentsContract.isDocumentUri(context, uri) && isExternalStorageDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":".toRegex()).toTypedArray()
            if (split.size != validLenth ) {
                return null
            }
            val type = split[0]
            if ("primary".equals(type, ignoreCase = true)) {
                return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            }
        }
        return null
    }

    fun isExternalStorageDocument(uri: Uri?): Boolean {
        return "com.android.externalstorage.documents" == uri?.authority
    }

    private fun initData() {
        wayPointV3VM.listenFlightControlState()

        wayPointV3VM.flightControlState.observe(viewLifecycleOwner) {
            it?.let {
                wayline_aircraft_height?.text = String.format("Aircraft Height: %.2f", it.height)
                wayline_aircraft_distance?.text  = String.format("Aircraft Distance: %.2f", it.distance)
                updateAircraftLocation(it.latitude , it.longtitude , it.head , it.homeLocation)
            }
        }
    }


    private fun updateAircraftLocation(aircraftLat: Double, aircraftLng: Double , aircraftHead : Float , homePoint: LocationCoordinate2D) {
        if(map == null) {
            return
        }
        val pos = DJILatLng(aircraftLat, aircraftLng)
        val zoomLevel = map?.getCameraPosition()!!.zoom

        val cameraPosition = DJICameraPosition.Builder()
            .target(pos)
            .zoom(if (wayPointV3VM.getMapType(context) == MapProvider.MAPLIBRE_PROVIDER)  -1.0f else zoomLevel )
            .build()

        val cu : DJICameraUpdate = DJICameraUpdateFactory.newCameraPosition(cameraPosition)
        val markerOptions = DJIMarkerOptions()
        markerOptions.position(pos)
        markerOptions.icon(droneBitmap)
        markerOptions.anchor(0.5f, 0.5f)


        val marOptionHomePoint = DJIMarkerOptions()
        marOptionHomePoint.position(DJILatLng(homePoint.latitude , homePoint.longitude))
        marOptionHomePoint.icon(homePointBitMap)
        marOptionHomePoint.anchor(0.5f,0.5f)



        if (isLocationValid(homePoint.latitude , homePoint.longitude)){
            if (homePointMarKer == null) {
                homePointMarKer = map?.addMarker(marOptionHomePoint)
            } else{
                homePointMarKer?.position = DJILatLng(homePoint.latitude , homePoint.longitude)
            }

        }

        if (isLocationValid(aircraftLat, aircraftLng)) {
            if (mAircraftMarker != null) {
               // mAircraftMarker!!.remove()
                mAircraftMarker?.position = pos;
            } else {
                mAircraftMarker = map?.addMarker(markerOptions)
            }

            mAircraftMarker!!.rotation = (aircraftHead - map!!.getCameraPosition().bearing)
            map!!.moveCamera(cu)

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


    fun createMapView(@MapProvider type:Int){
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

        mapView = DJIMapView(context, builder.build())
        mapView?.getDJIMapAsync(OnDJIMapReadyCallback {
            map = it
            resetMarker()
        })
        wp_map.addView(mapView)
    }

    fun resetMarker(){
        homePointMarKer = null
        mAircraftMarker = null
    }
    fun mapSwitch(@MapProvider type: Int){
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
    }

}