package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dji.sampleV5.aircraft.databinding.FragLookAtPageBinding
import dji.sampleV5.aircraft.keyvalue.KeyValueDialogUtil
import dji.sampleV5.aircraft.models.CameraStreamDetailVM
import dji.sampleV5.aircraft.models.LookAtVM
import dji.sampleV5.aircraft.util.Helper
import dji.sampleV5.aircraft.util.ToastUtils
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.sdk.keyvalue.value.flightcontroller.LookAtInfo
import dji.sdk.keyvalue.value.flightcontroller.LookAtMode
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.manager.interfaces.ICameraStreamManager
import dji.v5.utils.common.JsonUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit

class LookAtFragment : DJIFragment() {

    private val cameraViewModel: CameraStreamDetailVM by viewModels()
    private val lookAtViewModel: LookAtVM by viewModels()
    private var disposable: Disposable? = null
    private var binding: FragLookAtPageBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragLookAtPageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    private var surface: Surface? = null
    private var width = -1
    private var height = -1
    private val cameraSurfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            surface = holder.surface
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            this@LookAtFragment.width = width
            this@LookAtFragment.height = height
            updateCameraStream()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            width = 0
            height = 0
            updateCameraStream()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.svCamera?.holder?.addCallback(cameraSurfaceCallback)
        disposable = Observable.interval(2000, 500, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                lookAtViewModel.pointInfos.value?.map {
                    it.pinPointInfo = lookAtViewModel.getLiveViewLocationWithGPS(it.pos)
                }
                updatePointView()
                binding?.overLayerView?.onPointsChanged(lookAtViewModel.pointInfos.value)
            }
        binding?.btnSelectCamera?.setOnClickListener {
            selectCamera()
        }
        binding?.btnAddNewPinPoint?.setOnClickListener {
            addNewPinPoint()
        }
        binding?.btnClearPinPoint?.setOnClickListener {
            lookAtViewModel.clearPointInfos()
        }
        binding?.btnLookAt?.setOnClickListener {
            lookAt()
        }
        binding?.svCamera?.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val viewHeight = binding?.svCamera?.height ?: -1
                val viewWidth = binding?.svCamera?.width ?: -1
                if (viewHeight == -1 || viewWidth == -1) {
                    return@setOnTouchListener false
                }
                lookAtViewModel.startTapZoomPoint(event.x / viewHeight.toDouble(), event.y / viewWidth.toDouble())
            }
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.svCamera?.holder?.removeCallback(cameraSurfaceCallback)
        disposable?.dispose()
    }

    private fun selectCamera() {
        val index = ComponentIndexType.values()
        initPopupNumberPicker(Helper.makeList(index)) {
            cameraViewModel.setCameraIndex(index[indexChosen[0]])
            lookAtViewModel.currentComponentIndexType.value = index[indexChosen[0]]
            updateCameraStream()
            lookAtViewModel.clearPointInfos()
            resetIndex()
        }
    }

    private fun addNewPinPoint() {
        val location = FlightControllerKey.KeyAircraftLocation3D.create().get(LocationCoordinate3D())
        //默认值偏移一点，方便演示，否则飞机不动的话，会固定在正中间
        var showLocation: LocationCoordinate3D? = LocationCoordinate3D(location.latitude + 0.1, location.longitude + 0.1, location.altitude)
        KeyValueDialogUtil.showInputDialog(
            activity, "(Aircraft Location)",
            JsonUtil.toJson(showLocation), "", false
        ) {
            it?.apply {
                showLocation = JsonUtil.toBean(this, LocationCoordinate3D::class.java)
                if (showLocation == null) {
                    ToastUtils.showToast("Value Parse Error")
                    return@showInputDialog
                }
                lookAtViewModel.addNewPinPoint(showLocation!!)
            }
        }
    }

    private fun lookAt() {
        val locationList = lookAtViewModel.pointInfos.value?.map {
            it.pos
        }
        if (locationList.isNullOrEmpty()) {
            ToastUtils.showToast("No Pin Points")
            return
        }
        val lookAtModeList = LookAtMode.values()
        initPopupNumberPicker(Helper.makeList(locationList), Helper.makeList(lookAtModeList)) {
            val info = LookAtInfo()
            info.location = locationList[indexChosen[0]]
            info.mode = lookAtModeList[indexChosen[0]]
            lookAtViewModel.lookAt(info)
            resetIndex()
        }
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
            ICameraStreamManager.ScaleType.CENTER_INSIDE
        )
    }

    private fun updatePointView() {
        val buffer = StringBuffer()
        buffer.append("currentCameraIndex:").append(lookAtViewModel.currentComponentIndexType.value).append("\n")
        lookAtViewModel.pointInfos.value?.forEach {
            buffer.append(it.toString()).append("\n")
        }
        binding?.pinPointInfoTv?.text = buffer
    }
}