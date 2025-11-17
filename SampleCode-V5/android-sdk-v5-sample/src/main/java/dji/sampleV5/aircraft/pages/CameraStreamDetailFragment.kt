package dji.sampleV5.aircraft.pages

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.keyvalue.KeyValueDialogUtil
import dji.sampleV5.aircraft.models.CameraStreamDetailVM
import dji.sampleV5.aircraft.util.Helper
import dji.sampleV5.aircraft.util.ToastUtils
import dji.sdk.keyvalue.value.camera.CameraMode
import dji.sdk.keyvalue.value.airlink.ChannelPriority
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.flightassistant.VisionAssistDirection
import dji.v5.manager.interfaces.ICameraStreamManager
import dji.v5.utils.common.LogPath
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.StringUtils
import dji.v5.ux.core.extension.textColor

class CameraStreamDetailFragment : DJIFragment() {

    companion object {
        private const val KEY_CAMERA_INDEX = "cameraIndex"
        private const val KEY_ONLY_ONE_CAMERA = "onlyOneCamera"
        private val SUPPORT_YUV_FORMAT = mapOf(
            "YUV420（i420）" to ICameraStreamManager.FrameFormat.YUV420_888,
            "YUV444（i444）" to ICameraStreamManager.FrameFormat.YUV444_888,
            "NV21" to ICameraStreamManager.FrameFormat.NV21,
            "YUY2" to ICameraStreamManager.FrameFormat.YUY2,
            "RGBA" to ICameraStreamManager.FrameFormat.RGBA_8888
        )

        fun newInstance(cameraIndex: ComponentIndexType, onlyOneCamera: Boolean): CameraStreamDetailFragment {
            val args = Bundle()
            args.putInt(KEY_CAMERA_INDEX, cameraIndex.value())
            args.putBoolean(KEY_ONLY_ONE_CAMERA, onlyOneCamera)
            val fragment = CameraStreamDetailFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private val viewModel: CameraStreamDetailVM by viewModels()

    private lateinit var rgScaleLayout: RadioGroup
    private lateinit var mrgLensTypeLayout: RadioGroup
    private lateinit var mAssistViewDirectionLayout: RadioGroup
    private lateinit var mStreamPriorityLayout: RadioGroup
    private lateinit var cameraSurfaceView: SurfaceView
    private lateinit var btnDownloadYUV: Button
    private lateinit var tvCameraName: TextView
    private lateinit var btnCloseOrOpen: Button
    private lateinit var btnCloseOrOpenVisionAssist: Button
    private lateinit var btnBeginDownloadStream: Button
    private lateinit var btnStopDownloadStream: Button
    private lateinit var btnSetStreamEncodeBitrate: Button
    private lateinit var btnGetStreamEncodeBitrate: Button
    private lateinit var btnChangeCameraMode: Button
    private lateinit var cameraIndex: ComponentIndexType
    private var onlyOneCamera = false
    private var surface: Surface? = null
    private var width = -1
    private var height = -1
    private var scaleType = ICameraStreamManager.ScaleType.CENTER_INSIDE
    private var assistantVideoMode = VisionAssistDirection.AUTO
    private var streamPriority = ChannelPriority.HIGH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraIndex = ComponentIndexType.find(arguments?.getInt(KEY_CAMERA_INDEX, 0) ?: 0)
        onlyOneCamera = arguments?.getBoolean(KEY_ONLY_ONE_CAMERA, false) ?: false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        LogUtils.i(LogPath.SAMPLE, "onCreateView,onlyOneCamera:", onlyOneCamera)
        val layoutId: Int = if (onlyOneCamera) {
            R.layout.fragment_camera_stream_detail_single
        } else {
            R.layout.fragment_camera_stream_detail
        }
        return inflater.inflate(layoutId, container, false)
    }

    override fun updateTitle() {
        // do nothing
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rgScaleLayout = view.findViewById(R.id.rg_scale)
        mrgLensTypeLayout = view.findViewById(R.id.rg_lens_type)
        mAssistViewDirectionLayout = view.findViewById(R.id.rg_assist_view_direction)
        mStreamPriorityLayout = view.findViewById(R.id.rg_stream_priority)
        cameraSurfaceView = view.findViewById(R.id.sv_camera)
        btnDownloadYUV = view.findViewById(R.id.btn_download_yuv)
        tvCameraName = view.findViewById(R.id.tv_camera_name)
        btnCloseOrOpen = view.findViewById(R.id.btn_close_or_open)
        btnCloseOrOpenVisionAssist = view.findViewById(R.id.btn_vision_assist_close_or_open)
        btnBeginDownloadStream = view.findViewById(R.id.btn_begin_download_stream)
        btnStopDownloadStream = view.findViewById(R.id.btn_stop_download_stream)
        btnSetStreamEncodeBitrate = view.findViewById(R.id.btn_set_stream_encode_bitrate)
        btnGetStreamEncodeBitrate = view.findViewById(R.id.btn_get_stream_encode_bitrate)
        btnChangeCameraMode = view.findViewById(R.id.btn_change_camera_mode)
        rgScaleLayout.setOnCheckedChangeListener(onScaleChangeListener)
        mrgLensTypeLayout.setOnCheckedChangeListener(mOnLensChangeListener)
        mAssistViewDirectionLayout.setOnCheckedChangeListener(mOnAssistViewDirectionChangeListener)
        mStreamPriorityLayout.setOnCheckedChangeListener(mOnStreamPriorityChangeListener)
        btnCloseOrOpen.setOnClickListener(onOpenOrCloseCheckListener)
        btnCloseOrOpenVisionAssist.setOnClickListener(onOpenOrCloseVisionAssistCheckListener)
        cameraSurfaceView.holder.addCallback(cameraSurfaceCallback)

        btnDownloadYUV.setOnClickListener {
            downloadYUVImage()
        }

        btnBeginDownloadStream.setOnClickListener {
            viewModel.beginDownloadStreamToLocal()
        }

        btnStopDownloadStream.setOnClickListener {
            viewModel.stopDownloadStreamToLocal()
        }

        initViewModel()

        btnSetStreamEncodeBitrate.setOnClickListener {
            KeyValueDialogUtil.showInputDialog(
                activity, "Stream Encode Bitrate(bps)",
                viewModel.getStreamEncoderBitrate().toString(), "", false
            ) {
                it?.apply {
                    if (this.toIntOrNull() == null) {
                        ToastUtils.showToast("Value Parse Error")
                        return@showInputDialog
                    }
                    viewModel.setStreamEncoderBitrate(this.toInt())
                    ToastUtils.showToast("set success,it will take effect while encoder is working.")
                }
            }
        }

        btnGetStreamEncodeBitrate.setOnClickListener {
            ToastUtils.showToast("Stream Encoder Bitrate:${viewModel.getStreamEncoderBitrate()}")
        }

        btnChangeCameraMode.setOnClickListener {
            val index = arrayListOf(
                CameraMode.PHOTO_NORMAL, CameraMode.VIDEO_NORMAL
            )
            initPopupNumberPicker(Helper.makeList(index)) {
                viewModel.changeCameraMode(index[indexChosen[0]])
                resetIndex()
            }
        }


        if (cameraIndex == ComponentIndexType.VISION_ASSIST || cameraIndex == ComponentIndexType.FPV) {
            mAssistViewDirectionLayout.visibility = View.VISIBLE
            btnCloseOrOpenVisionAssist.visibility = View.VISIBLE
        } else {
            mAssistViewDirectionLayout.visibility = View.GONE
            btnCloseOrOpenVisionAssist.visibility = View.GONE
        }
    }

    private fun initViewModel() {
        LogUtils.i(LogPath.SAMPLE, "initViewModel,cameraIndex:", cameraIndex)

        viewModel.setCameraIndex(cameraIndex)
        viewModel.availableLensListData.observe(viewLifecycleOwner) { availableLensList ->
            for (i in 0 until mrgLensTypeLayout.childCount) {
                val childView = mrgLensTypeLayout.getChildAt(i)
                if (childView is RadioButton) {
                    childView.visibility = View.GONE
                }
            }
            if (availableLensList.isEmpty()) {
                mrgLensTypeLayout.visibility = View.GONE
            } else {
                mrgLensTypeLayout.visibility = View.VISIBLE
                for (lens in availableLensList) {
                    val childView = mrgLensTypeLayout.findViewWithTag<View>(lens.value().toString())
                    if (childView != null) {
                        childView.visibility = View.VISIBLE
                    }
                }
            }
        }
        viewModel.currentLensData.observe(viewLifecycleOwner) { lens ->
            val childView = mrgLensTypeLayout.findViewWithTag<View>(lens.value().toString())
            if (childView is RadioButton) {
                mrgLensTypeLayout.setOnCheckedChangeListener(null)
                mrgLensTypeLayout.check(childView.id)
                mrgLensTypeLayout.setOnCheckedChangeListener(mOnLensChangeListener)
                childView.visibility = View.VISIBLE
            }
        }

        viewModel.cameraName.observe(viewLifecycleOwner) { name ->
            tvCameraName.text = name
        }

        viewModel.isVisionAssistEnabled.observe(viewLifecycleOwner) {
            btnCloseOrOpenVisionAssist.isSelected = it == true
            if (btnCloseOrOpenVisionAssist.isSelected) {
                btnCloseOrOpenVisionAssist.text = "Close Vision Assist"
            } else {
                btnCloseOrOpenVisionAssist.text = "Open Vision Assist"
            }
        }

        viewModel.visionAssistViewDirection.observe(viewLifecycleOwner) {
            val childView = mAssistViewDirectionLayout.findViewWithTag<View>(it.name)
            if (childView is RadioButton) {
                mAssistViewDirectionLayout.setOnCheckedChangeListener(null)
                mAssistViewDirectionLayout.check(childView.id)
                mAssistViewDirectionLayout.setOnCheckedChangeListener(mOnAssistViewDirectionChangeListener)
                childView.visibility = View.VISIBLE
            }
        }

        viewModel.visionAssistViewDirectionRange.observe(viewLifecycleOwner) { availableDirectionList ->
            if (cameraIndex != ComponentIndexType.VISION_ASSIST && cameraIndex != ComponentIndexType.FPV) {
                return@observe
            }
            for (i in 0 until mAssistViewDirectionLayout.childCount) {
                val childView = mAssistViewDirectionLayout.getChildAt(i)
                if (childView is RadioButton) {
                    childView.visibility = View.GONE
                }
            }
            if (availableDirectionList.isEmpty()) {
                mAssistViewDirectionLayout.visibility = View.GONE
            } else {
                mAssistViewDirectionLayout.visibility = View.VISIBLE
                for (direction in availableDirectionList) {
                    val childView = mAssistViewDirectionLayout.findViewWithTag<View>(direction.name)
                    if (childView != null) {
                        childView.visibility = View.VISIBLE
                    }
                }
            }
        }

        viewModel.cameraStreamEnableMap.observe(viewLifecycleOwner) { map ->
            map[cameraIndex]?.let {
                btnCloseOrOpen.isSelected = it == true
                if (btnCloseOrOpen.isSelected) {
                    btnCloseOrOpen.text = "Close"
                    tvCameraName.textColor = Color.GREEN
                } else {
                    btnCloseOrOpen.text = "Open"
                    tvCameraName.textColor = Color.RED
                }
            }
        }
    }

    private fun updateStreamPriority() {
        val childView = mStreamPriorityLayout.findViewWithTag<View>(viewModel.getStreamPriority().name)
        if (childView is RadioButton) {
            mStreamPriorityLayout.setOnCheckedChangeListener(null)
            mStreamPriorityLayout.check(childView.id)
            mStreamPriorityLayout.setOnCheckedChangeListener(mOnStreamPriorityChangeListener)
        }
    }

    private fun updateCameraStream() {
        if (width <= 0 || height <= 0 || surface == null) {
            if (surface != null) {
                viewModel.removeCameraStreamSurface(surface!!)
            }
            return
        }
        updateStreamPriority()
        viewModel.putCameraStreamSurface(
            surface!!,
            width,
            height,
            scaleType
        )
    }

    private fun downloadYUVImage() {
        val selectedIndex = arrayOf(-1)
        val formatList = SUPPORT_YUV_FORMAT.keys.toTypedArray()
        AlertDialog.Builder(requireContext(), androidx.appcompat.R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
            .setIcon(android.R.drawable.ic_input_get)
            .setTitle(R.string.title_select_yuv_format)
            .setCancelable(true)
            .setSingleChoiceItems(formatList, -1) { _, i ->
                if (i >= 0) {
                    selectedIndex[0] = i
                }
            }
            .setPositiveButton(R.string.title_select_yuv_format_ok) { dialog, _ ->
                if (selectedIndex[0] >= 0) {
                    val format = SUPPORT_YUV_FORMAT[formatList[selectedIndex[0]]]
                    val name = formatList[selectedIndex[0]]
                    viewModel.downloadYUVImageToLocal(format!!, name)
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.title_select_yuv_format_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private val onScaleChangeListener = RadioGroup.OnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
        when (checkedId) {
            R.id.rb_center_crop -> {
                scaleType = ICameraStreamManager.ScaleType.CENTER_CROP
            }

            R.id.rb_center_inside -> {
                scaleType = ICameraStreamManager.ScaleType.CENTER_INSIDE
            }

            R.id.rb_fix_xy -> {
                scaleType = ICameraStreamManager.ScaleType.FIX_XY
            }
        }
        updateCameraStream()
    }

    private val mOnLensChangeListener = RadioGroup.OnCheckedChangeListener { rg, checkedId ->
        rg.findViewById<RadioButton>(checkedId)?.let {
            val tag = it.tag
            val type = CameraVideoStreamSourceType.find((tag as String).toInt())
            viewModel.changeCameraLens(type)
        }
    }

    private val mOnStreamPriorityChangeListener = RadioGroup.OnCheckedChangeListener { rg, checkedId ->
        when (checkedId) {
            R.id.rb_priority_low -> {
                streamPriority = ChannelPriority.LOW
            }

            R.id.rb_priority_medium -> {
                streamPriority = ChannelPriority.MEDIUM
            }

            R.id.rb_priority_high -> {
                streamPriority = ChannelPriority.HIGH
            }

            R.id.rb_priority_highest -> {
                streamPriority = ChannelPriority.HIGHEST
            }
        }
        viewModel.setStreamPriority(streamPriority)
    }

    private val mOnAssistViewDirectionChangeListener = RadioGroup.OnCheckedChangeListener { rg, checkedId ->
        when (checkedId) {
            R.id.rb_direction_auto -> {
                assistantVideoMode = VisionAssistDirection.AUTO
            }

            R.id.rb_direction_front -> {
                assistantVideoMode = VisionAssistDirection.FRONT
            }

            R.id.rb_direction_back -> {
                assistantVideoMode = VisionAssistDirection.BACK
            }

            R.id.rb_direction_left -> {
                assistantVideoMode = VisionAssistDirection.LEFT
            }

            R.id.rb_direction_right -> {
                assistantVideoMode = VisionAssistDirection.RIGHT
            }

            R.id.rb_direction_up -> {
                assistantVideoMode = VisionAssistDirection.UP
            }

            R.id.rb_direction_down -> {
                assistantVideoMode = VisionAssistDirection.DOWN
            }

            R.id.rb_direction_off -> {
                assistantVideoMode = VisionAssistDirection.OFF
            }
        }
        viewModel.setVisionAssistViewDirection(assistantVideoMode)
    }

    private val onOpenOrCloseCheckListener = View.OnClickListener { _ ->
        viewModel.enableStream(!btnCloseOrOpen.isSelected)
    }

    private val onOpenOrCloseVisionAssistCheckListener = View.OnClickListener { _ ->
        viewModel.enableVisionAssist(!btnCloseOrOpenVisionAssist.isSelected)
    }

    private val cameraSurfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            surface = holder.surface
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            this@CameraStreamDetailFragment.width = width
            this@CameraStreamDetailFragment.height = height
            updateCameraStream()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            width = 0
            height = 0
            updateCameraStream()
        }
    }
}