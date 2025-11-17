package dji.sampleV5.aircraft.pages

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.databinding.FragIntelligentFlightPageBinding
import dji.sampleV5.aircraft.keyvalue.KeyValueDialogUtil
import dji.sampleV5.aircraft.models.CameraStreamDetailVM
import dji.sampleV5.aircraft.models.IntelligentFlightVM
import dji.sampleV5.aircraft.util.Helper
import dji.sampleV5.aircraft.util.ToastUtils
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.common.DoubleRect
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.sdk.keyvalue.value.flightcontroller.FlyToMode
import dji.v5.manager.intelligent.AutoSensingInfo
import dji.v5.manager.intelligent.IntelligentFlightInfo
import dji.v5.manager.intelligent.IntelligentModel
import dji.v5.manager.intelligent.flyto.FlyToInfo
import dji.v5.manager.intelligent.flyto.FlyToTarget
import dji.v5.manager.intelligent.poi.POIInfo
import dji.v5.manager.intelligent.poi.POITarget
import dji.v5.manager.intelligent.smarttrack.SmartTrackInfo
import dji.v5.manager.intelligent.spotlight.SpotLightInfo
import dji.v5.manager.interfaces.ICameraStreamManager
import dji.v5.utils.common.AndUtil
import dji.v5.utils.common.JsonUtil
import dji.v5.ux.map.MapWidget
import dji.v5.ux.mapkit.core.maps.DJIMap
import dji.v5.ux.mapkit.core.models.DJIBitmapDescriptor
import dji.v5.ux.mapkit.core.models.DJIBitmapDescriptorFactory
import dji.v5.ux.mapkit.core.models.DJILatLng
import dji.v5.ux.mapkit.core.models.annotations.DJICircle
import dji.v5.ux.mapkit.core.models.annotations.DJICircleOptions
import dji.v5.ux.mapkit.core.models.annotations.DJIMarkerOptions
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.absoluteValue


class IntelligentFlightFragment : DJIFragment() {

    private val cameraViewModel: CameraStreamDetailVM by viewModels()
    private var surface: Surface? = null
    private var width = -1
    private var height = -1
    private val cameraSurfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            surface = holder.surface
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            this@IntelligentFlightFragment.width = width
            this@IntelligentFlightFragment.height = height
            updateCameraStream()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            width = 0
            height = 0
            updateCameraStream()
        }
    }

    private var startX = 0f
    private var startY = 0f
    private var needSendBoundInfo = false

    private var binding: FragIntelligentFlightPageBinding? = null
    private val intelligentVM: IntelligentFlightVM by viewModels()
    private val pinPoints: MutableList<DJILatLng> = mutableListOf()

    private var intelligentFlightInfo: IntelligentFlightInfo = IntelligentFlightInfo()
    private var autoSensingInfo: AutoSensingInfo = AutoSensingInfo()
    private var runningModelIndex: Int = 0
    private var intelligentModels: List<IntelligentModel> = listOf()

    private var aircraftHeight = 0.0
    private var aircraftLocation = LocationCoordinate2D()

    /**
     * POI
     */
    private var poiCircleMarker: DJICircle? = null
    private var poiCircleStrokeMarker: DJICircle? = null
    private var poiInfo = POIInfo()
    private var lastPoiLocation: LocationCoordinate3D = LocationCoordinate3D()
    private var lastPoiRadius: Double = 0.0
    private val normalIcon: DJIBitmapDescriptor = DJIBitmapDescriptorFactory.fromResource(R.mipmap.ic_pin_map_v2)
    private val bound = DoubleRect()

    /**
     * fly to
     */
    private var flyToInfo = FlyToInfo()

    /**
     * spot light
     */
    private var spotLightInfo = SpotLightInfo()

    /**
     * smart track
     */
    private var smartTrackInfo = SmartTrackInfo()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragIntelligentFlightPageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initForCommon()
        initForPoi()
        initForFlyTo()
        initForSpotLight()
        initForSmartTrack()

        createMapView(savedInstanceState)
        addMapListener()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initForCommon() {
        binding?.svCamera?.holder?.addCallback(cameraSurfaceCallback)
        binding?.intelligentFlightInfoTv?.movementMethod = ScrollingMovementMethod.getInstance()

        intelligentVM.initListener()
        intelligentVM.intelligentFlightInfo.observe(viewLifecycleOwner) {
            intelligentFlightInfo = it
            updateIntelligentFlightInfo()
        }
        intelligentVM.autoSensingInfo.observe(viewLifecycleOwner) {
            autoSensingInfo = it
            binding?.overLayerView?.onTargetChange(autoSensingInfo.targets)
        }
        intelligentVM.aircraftHeight.observe(viewLifecycleOwner) {
            aircraftHeight = it
            updateIntelligentFlightInfo()
        }
        intelligentVM.aircraftLocation.observe(viewLifecycleOwner) {
            aircraftLocation = it
            updateIntelligentFlightInfo()
        }
        intelligentVM.runningModelIndex.observe(viewLifecycleOwner){
            runningModelIndex = it
            updateIntelligentFlightInfo()
        }
        intelligentVM.intelligentModels.observe(viewLifecycleOwner){
            intelligentModels = it
            updateIntelligentFlightInfo()
        }
        binding?.btnSelectCamera?.setOnClickListener {
            selectCamera()
        }
        binding?.btnSelectLens?.setOnClickListener {
            selectLens()
        }
        binding?.btnHideMsg?.setOnClickListener {
            if (binding?.intelligentFlightInfoTv?.isVisible == true) {
                binding?.intelligentFlightInfoTv?.visibility = View.INVISIBLE
            } else {
                binding?.intelligentFlightInfoTv?.visibility = View.VISIBLE
                binding?.intelligentFlightInfoTv?.visibility = View.VISIBLE
            }
        }
        binding?.btnStartAutoSensing?.setOnClickListener {
            intelligentVM.startAutoSensing()
        }
        binding?.btnStopAutoSensing?.setOnClickListener {
            intelligentVM.stopAutoSensing()
        }
        binding?.btnSelectIntelligentModel?.setOnClickListener {
            val index = intelligentModels.map {
                it.modelIndex
            }
            initPopupNumberPicker(Helper.makeList(index)) {
                intelligentVM.selectIntelligentModel(index[indexChosen[0]])
                resetIndex()
            }
        }
        binding?.btnShowMap?.setOnClickListener {
            binding?.mapWidget?.visibility = View.VISIBLE
            binding?.svCamera?.visibility = View.INVISIBLE
            binding?.overLayerView?.visibility = View.INVISIBLE
        }
        binding?.btnShowFpv?.setOnClickListener {
            binding?.mapWidget?.visibility = View.INVISIBLE
            binding?.svCamera?.visibility = View.VISIBLE
            binding?.overLayerView?.visibility = View.VISIBLE
        }
        binding?.svCamera?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    binding?.overLayerView?.onSelectBoundInfo(null)
                    needSendBoundInfo = false
                }

                MotionEvent.ACTION_MOVE -> {
                    if (calcManhattanDistance(startX, startY, event.getX(), event.getY()) < 20) {
                        needSendBoundInfo = false
                        return@setOnTouchListener true
                    }
                    bound.x = ((startX + event.x) / 2 / v.width).toDouble()
                    bound.y = ((startY + event.y) / 2 / v.height).toDouble()
                    bound.width = abs((event.x - startX).toDouble()) / v.width
                    bound.height = abs((event.y - startY).toDouble()) / v.height
                    binding?.overLayerView?.onSelectBoundInfo(bound)
                    needSendBoundInfo = true
                }

                MotionEvent.ACTION_UP -> {
                    binding?.overLayerView?.onSelectBoundInfo(null)
                    if (needSendBoundInfo) {
                        intelligentVM.selectManualTarget(bound)
                    }
                }

                else -> {}
            }
            return@setOnTouchListener true
        }
    }

    private fun initForPoi() {
        binding?.btnStartPoiMissionForIndustry?.setOnClickListener {
            selectPinPoints {
                val target = POITarget()
                target.targetLocation = it
                intelligentVM.startPOIMission(target)
            }
        }
        binding?.btnStartPoiMissionForConsume?.setOnClickListener {
            intelligentVM.startPOIMission(POITarget())
        }
        binding?.btnStopPoiMission?.setOnClickListener {
            intelligentVM.stopPOIMission()
        }
        binding?.btnUpdatePoiMissionTarget?.setOnClickListener {
            val index = autoSensingInfo.targets.map {
                it.targetIndex
            }
            initPopupNumberPicker(Helper.makeList(index)) {
                val target = POITarget()
                target.index = index[indexChosen[0]]
                intelligentVM.updatePOIMissionTarget(target)
                resetIndex()
            }
        }
        binding?.btnLockPoiCircularVelocity?.setOnClickListener {
            val index = arrayListOf(true, false)
            initPopupNumberPicker(Helper.makeList(index)) {
                intelligentVM.lockCircularVelocity(index[indexChosen[0]])
                resetIndex()
            }
        }
        binding?.btnLockPoiGimbalPitch?.setOnClickListener {
            val index = arrayListOf(true, false)
            initPopupNumberPicker(Helper.makeList(index)) {
                intelligentVM.lockGimbalPitch(index[indexChosen[0]])
                resetIndex()
            }
        }
        binding?.btnSetPoiCircleSpeed?.setOnClickListener {
            val index = generateSequence(-1.0) { BigDecimal(it + 0.1).setScale(1, RoundingMode.HALF_UP).toDouble() }.takeWhile { it <= 1.0 }.toList()
            initPopupNumberPicker(Helper.makeList(index)) {
                intelligentVM.updatePOICircleSpeed(index[indexChosen[0]])
                resetIndex()
            }
        }
        intelligentVM.poiInfo.observe(viewLifecycleOwner) {
            poiInfo = it
            updateIntelligentFlightInfo()
            if (!it.isRunning) {
                clearPoiInfo()
                return@observe
            }
            intelligentVM.poiTarget.value?.targetLocation?.let { targetLocation ->
                if (lastPoiLocation != targetLocation || (lastPoiRadius - it.radius).absoluteValue > 0.1) {
                    updatePOICircleLocation(targetLocation, it.radius)
                }
                lastPoiLocation = targetLocation
                lastPoiRadius = it.radius
            }
        }
    }

    private fun initForFlyTo() {
        binding?.btnStartFlyToMission?.setOnClickListener {
            selectPinPoints { location ->
                KeyValueDialogUtil.showInputDialog(
                    activity, "(MaxSpeed,SecurityTakeoffHeight,TargetHeight)",
                    "14,20,50", "", false
                ) {
                    var maxSpeed = 0
                    var securityTakeoffHeight = 0
                    var targetHeight = 0
                    it?.split(",")?.apply {
                        if (this.size != 3 && this[0].toIntOrNull() == null && this[1].toIntOrNull() == null && this[2].toIntOrNull() == null) {
                            ToastUtils.showToast("Value Parse Error")
                            return@showInputDialog
                        }
                        maxSpeed = this[0].toInt()
                        securityTakeoffHeight = this[1].toInt()
                        targetHeight = this[2].toInt()
                    }
                    val target = FlyToTarget()
                    target.targetLocation = location
                    target.targetLocation.altitude = targetHeight.toDouble()
                    target.maxSpeed = maxSpeed
                    target.securityTakeoffHeight = securityTakeoffHeight
                    intelligentVM.startFlyTo(target)
                }
            }
        }
        binding?.btnStopFlyToMission?.setOnClickListener {
            intelligentVM.stopFlyTo()
        }
        binding?.btnSetFlyToMode?.setOnClickListener {
            val index = arrayListOf(FlyToMode.SET_HEIGHT, FlyToMode.SMART_HEIGHT)
            initPopupNumberPicker(Helper.makeList(index)) {
                intelligentVM.setFlyToMode(index[indexChosen[0]])
                resetIndex()
            }
        }
        binding?.btnSetFlyToHeight?.setOnClickListener {
            KeyValueDialogUtil.showInputDialog(
                activity, "(Height)",
                "50", "", false
            ) {
                if (it?.toIntOrNull() == null) {
                    ToastUtils.showToast("Value Parse Error")
                    return@showInputDialog
                }
                intelligentVM.setFlyToHeight(it.toInt())
            }
        }
        intelligentVM.flyToInfo.observe(viewLifecycleOwner) {
            flyToInfo = it
            updateIntelligentFlightInfo()
        }
        intelligentVM.spotLightInfo.observe(viewLifecycleOwner) {
            spotLightInfo = it
            updateIntelligentFlightInfo()
        }
    }

    private fun initForSpotLight() {
        binding?.btnStartSpotlightForIndustry?.setOnClickListener {
            val index = autoSensingInfo.targets.map {
                it.targetIndex
            }
            if (index.isEmpty()) return@setOnClickListener
            initPopupNumberPicker(Helper.makeList(index)) {
                intelligentVM.selectAutoTarget(index[indexChosen[0]])
                resetIndex()
            }
        }
        binding?.btnEnterSpotlightMode?.setOnClickListener {
            intelligentVM.enterSpotLightMode()
        }
        binding?.btnExitSpotlightMode?.setOnClickListener {
            intelligentVM.exitSpotLightMode()
        }
        binding?.btnStartSpotlightForConsume?.setOnClickListener {
            intelligentVM.startSpotlight()
        }
        binding?.btnSelectAutoTarget?.setOnClickListener {
            val index = autoSensingInfo.targets.map {
                it.targetIndex
            }
            if (index.isEmpty()) return@setOnClickListener
            initPopupNumberPicker(Helper.makeList(index)) {
                intelligentVM.selectAutoTarget(index[indexChosen[0]])
                resetIndex()
            }
        }
        binding?.btnConfirmTarget?.setOnClickListener {
            intelligentVM.confirmTarget()
        }
        binding?.btnStopSpotlight?.setOnClickListener {
            intelligentVM.stopSpotlight()
        }
    }

    private fun initForSmartTrack() {
        binding?.btnStartSmartTrack?.setOnClickListener {
            intelligentVM.startSmartTrack()
        }
        binding?.btnStopSmartTrack?.setOnClickListener {
            intelligentVM.stopSmartTrack()
        }
        binding?.btnSelectSmartTrackIndex?.setOnClickListener {
            val index = autoSensingInfo.targets.map {
                it.targetIndex
            }
            if (index.isEmpty()) return@setOnClickListener
            initPopupNumberPicker(Helper.makeList(index)) {
                intelligentVM.selectTrackingTarget(index[indexChosen[0]])
                resetIndex()
            }
        }
    }

    private fun createMapView(savedInstanceState: Bundle?) {
        val onMapReadyListener = MapWidget.OnMapReadyListener { map ->
            map.setMapType(DJIMap.MapType.NORMAL)
        }

        binding?.mapWidget?.initMapLibreMap(requireContext(), onMapReadyListener)
        binding?.mapWidget?.onCreate(savedInstanceState)
    }

    private fun addMapListener() {
        binding?.waypointAdd?.setOnCheckedChangeListener { _, isOpen ->
            if (isOpen) {
                binding?.mapWidget?.map?.setOnMapClickListener {
                    pinPoints.add(it)
                    showWaypoints(it)
                }
            } else {
                binding?.mapWidget?.map?.removeAllOnMapClickListener()
            }
        }
    }

    private fun showWaypoints(pos: DJILatLng) {
        val options = DJIMarkerOptions()
        options.position(pos)
            .draggable(false)
            .anchor(0.5f, 0.5f)
            .icon(normalIcon)
        binding?.mapWidget?.map?.addMarker(options)
    }

    private fun updatePOICircleLocation(location: LocationCoordinate3D, radius: Double) {
        val position = DJILatLng(location.latitude, location.longitude)
        if (!position.isAvailable) {
            return
        }
        if (poiCircleMarker != null && poiCircleStrokeMarker != null) {
            poiCircleMarker?.setCircle(position, radius)
            poiCircleMarker?.setVisible(true)
            poiCircleStrokeMarker?.setCircle(position, radius)
            poiCircleStrokeMarker?.setVisible(true)
        } else {
            initPoiCircleOnMap(position, radius)
        }
    }

    /**
     * 初始化 POI 环绕
     *
     * @param poiLocation
     */
    private fun initPoiCircleOnMap(poiLocation: DJILatLng, radius: Double) {
        if (!poiLocation.isAvailable) {
            return
        }
        if (poiCircleStrokeMarker == null) {
            val poiCircleStrokeOptions: DJICircleOptions = DJICircleOptions()
                .center(poiLocation)
                .radius(radius)
                .strokeWidth(AndUtil.getDimension(dji.v5.ux.R.dimen.uxsdk_2_dp) + AndUtil.getDimension(dji.v5.ux.R.dimen.uxsdk_1_dp))
                .strokeColor(AndUtil.getResColor(R.color.black_half))
            poiCircleStrokeMarker = binding?.mapWidget?.map?.addSingleCircle(poiCircleStrokeOptions)
        }
        if (poiCircleMarker == null) {
            val poiCircleOptions: DJICircleOptions = DJICircleOptions()
                .center(poiLocation)
                .radius(radius)
                .strokeWidth(AndUtil.getDimension(dji.v5.ux.R.dimen.uxsdk_2_dp))
                .strokeColor(AndUtil.getResColor(dji.v5.ux.R.color.uxsdk_green_in_dark))
            poiCircleMarker = binding?.mapWidget?.map?.addSingleCircle(poiCircleOptions)
        }
    }

    private fun clearPoiInfo() {
        poiCircleMarker?.isVisible = false
        poiCircleStrokeMarker?.isVisible = false
    }

    private fun selectPinPoints(onAction: ((LocationCoordinate3D) -> Unit)) {
        val points = pinPoints.map {
            "lat:${String.format("%.3f", it.latitude)}\nlng:${String.format("%.3f", it.longitude)}"
        }
        if (points.isEmpty()) return
        initPopupNumberPicker(Helper.makeList(points)) {
            onAction(
                LocationCoordinate3D(
                    pinPoints[indexChosen[0]].latitude,
                    pinPoints[indexChosen[0]].longitude,
                    pinPoints[indexChosen[0]].altitude,
                )
            )
            resetIndex()
        }
    }

    private fun updateIntelligentFlightInfo() {
        val sb = StringBuffer()
        sb.append("lat:${String.format("%.3f", aircraftLocation.latitude)},lng:${String.format("%.3f", aircraftLocation.longitude)},")
            .append("height:${String.format("%.2f", aircraftHeight)}\n")
        sb.append("${JsonUtil.toJson(intelligentFlightInfo)}\n")
        sb.append("runningModelIndex:$runningModelIndex\n")
        sb.append("intelligentModels:${JsonUtil.toJson(intelligentModels)}\n")
        sb.append("\n")
        sb.append("-- Intelligent Flight End --\n")
        sb.append("\n")
        sb.append("-- POI Info --\n")
        sb.append("$poiInfo\n")
        sb.append("-- POI End --\n")
        sb.append("\n")
        sb.append("-- Fly To Info Information --\n")
        sb.append("$flyToInfo\n")
        sb.append("-- Fly To Info End --\n")
        sb.append("\n")
        sb.append("-- Spot Light Info --\n")
        sb.append("$spotLightInfo\n")
        sb.append("-- Spot Light Info End --\n")
        sb.append("\n")
        sb.append("-- Smart Track Info --\n")
        sb.append("$smartTrackInfo\n")
        sb.append("-- Smart Track Info End --\n")
        sb.append("\n")
        binding?.intelligentFlightInfoTv?.text = sb.toString()
    }

    private fun updateCameraStream() {
        if (width <= 0 || height <= 0 || surface == null) {
            if (surface != null) {
                cameraViewModel.removeCameraStreamSurface(surface!!)
            }
            return
        }
        cameraViewModel.putCameraStreamSurface(
            surface!!,
            width,
            height,
            ICameraStreamManager.ScaleType.FIX_XY
        )
    }

    private fun selectCamera() {
        val index = arrayListOf(
            ComponentIndexType.LEFT_OR_MAIN, ComponentIndexType.RIGHT,
            ComponentIndexType.UP, ComponentIndexType.FPV
        )
        initPopupNumberPicker(Helper.makeList(index)) {
            cameraViewModel.setCameraIndex(index[indexChosen[0]])
            updateCameraStream()
            resetIndex()
        }
    }

    private fun selectLens() {
        val index = arrayListOf(
            CameraVideoStreamSourceType.WIDE_CAMERA, CameraVideoStreamSourceType.ZOOM_CAMERA,
            CameraVideoStreamSourceType.INFRARED_CAMERA
        )
        initPopupNumberPicker(Helper.makeList(index)) {
            cameraViewModel.changeCameraLens(index[indexChosen[0]])
            updateCameraStream()
            resetIndex()
        }
    }

    private fun calcManhattanDistance(point1X: Float, point1Y: Float, point2X: Float, point2Y: Float): Double {
        return abs((point1X - point2X).toDouble()) + abs((point1Y - point2Y).toDouble())
    }

    override fun onPause() {
        super.onPause()
        binding?.mapWidget?.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding?.mapWidget?.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.mapWidget?.onDestroy()
        intelligentVM.cleanListener()
        binding?.svCamera?.holder?.removeCallback(cameraSurfaceCallback)
    }
}