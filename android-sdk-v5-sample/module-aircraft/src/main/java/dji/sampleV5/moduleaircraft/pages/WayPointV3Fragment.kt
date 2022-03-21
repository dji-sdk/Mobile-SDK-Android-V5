package dji.sampleV5.moduleaircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdate
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*

/**
 * @author feel.feng
 * @time 2022/02/27 9:30 上午
 * @description:
 */
class WayPointV3Fragment : DJIFragment() {

    private val wayPointV3VM: WayPointV3VM by activityViewModels()
    private val WAYPOINT_SAMPLE_FILE_NAME : String = "waypointsample.kmz"
    private val WAYPOINT_SAMPLE_FILE_DIR : String = "waypoint/"
    private val MISSION_ID = "sample"  // 每个航线任务的唯一ID ，由用户自行定义
    val missionHashMap : HashMap<String,String> = HashMap<String,String>()
    var aMap : AMap? = null
    var droneBitmap: BitmapDescriptor? = null
    var mAircraftMarker: Marker? = null

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
        val destPath = dirName + WAYPOINT_SAMPLE_FILE_NAME
        if (dir.listFiles() == null || dir.listFiles().size == 0) {
            FileUtil.copyAssetsFile(
                ContextUtil.getContext(),
                WAYPOINT_SAMPLE_FILE_NAME,
                destPath)
        }
    }

    private fun initView(savedInstanceState: Bundle?) {
        wayPointV3VM.addMissionStateListener() {
            mission_execute_state_tv?.text = "Mission Execute State : ${it.name}"
        }
        wayPointV3VM.addWaylineExecutingInfoListener(){
            wayline_execute_state_tv?.text = "Wayline Execute Info WaylineID:${it.waylineID} \n" +
                    "WaypointIndex:${it.currentWaypointIndex}"
        }

       btn_mission_upload?.setOnClickListener {
           var missionPath  = DiskUtil.getExternalCacheDirPath(ContextUtil.getContext(), WAYPOINT_SAMPLE_FILE_DIR + WAYPOINT_SAMPLE_FILE_NAME)
           val waypointFile = File(missionPath)

           // 上传时生成一个uuid 将missionid与uuid绑定
           val uuid =   UUID.randomUUID().toString().replace("-", "_")
           missionHashMap.put(MISSION_ID , uuid)
           val file = File(missionPath)
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
        wp_map.onCreate(savedInstanceState)
        aMap = wp_map.map
        aMap!!.uiSettings.isScaleControlsEnabled = true
        droneBitmap = BitmapDescriptorFactory.fromResource(R.drawable.aircraft)


    }

    private fun initData() {
        wayPointV3VM.listenFlightControlState()

        wayPointV3VM.flightControlState.observe(viewLifecycleOwner) {
            it?.let {
                wayline_aircraft_height?.text = String.format("Aircraft Height: %.2f", it.height)
                wayline_aircraft_distance?.text  = String.format("Aircraft Distance: %.2f", it.distance)
                updateAircraftLocation(it.latitude , it.longtitude , it.head)
            }
        }
    }

    private fun updateAircraftLocation(aircraftLat: Double, aircraftLng: Double , aircraftHead : Float) {
        val pos = LatLng(aircraftLat, aircraftLng)
        val cu : CameraUpdate = CameraUpdateFactory.changeLatLng(pos)
        val markerOptions = MarkerOptions()
        markerOptions.position(pos)
        markerOptions.icon(droneBitmap)
        markerOptions.anchor(0.5f, 0.5f)

        if (mAircraftMarker != null) {
            mAircraftMarker!!.remove()
        }
        if (isLocationValid(aircraftLat, aircraftLng)) {
            mAircraftMarker = aMap!!.addMarker(markerOptions)
            mAircraftMarker!!.setRotateAngle(aircraftHead * -1.0f)
            aMap!!.moveCamera(cu)
        }

    }

    fun isLocationValid(latitude: Double, longitude: Double): Boolean {
        return latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180 && latitude != 0.0 && longitude != 0.0
    }

    override fun onPause() {
        super.onPause()
        wp_map.onPause()
    }

    override fun onResume() {
        super.onResume()
        wp_map.onResume()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        wp_map.onDestroy()
    }
    override fun onDestroy() {
        super.onDestroy()

        wayPointV3VM.cancelListenFlightControlState()
        wayPointV3VM.removeAllMissionStateListener()
        wayPointV3VM.clearAllWaylineExecutingInfoListener()
    }

}