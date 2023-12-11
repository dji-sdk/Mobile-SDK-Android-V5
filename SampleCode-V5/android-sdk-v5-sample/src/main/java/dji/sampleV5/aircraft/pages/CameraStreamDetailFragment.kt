package dji.sampleV5.aircraft.pages

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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.models.CameraStreamDetailVM
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.manager.interfaces.ICameraStreamManager
import kotlinx.android.synthetic.main.fragment_camera_stream_detail.sv_camera

class CameraStreamDetailFragment : Fragment() {

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
    private lateinit var cameraSurfaceView: SurfaceView
    private lateinit var btnDownloadYUV: Button
    private lateinit var tvCameraName: TextView
    private lateinit var btnCloseOrOpen: Button
    private lateinit var cameraIndex: ComponentIndexType
    private var onlyOneCamera = false
    private var isNeedPreviewCamera = false
    private var surface: Surface? = null
    private var width = -1
    private var height = -1
    private var scaleType = ICameraStreamManager.ScaleType.CENTER_INSIDE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraIndex = ComponentIndexType.find(arguments?.getInt(KEY_CAMERA_INDEX, 0) ?: 0)
        onlyOneCamera = arguments?.getBoolean(KEY_ONLY_ONE_CAMERA, false) ?: false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layoutId: Int = if (onlyOneCamera) {
            R.layout.fragment_camera_stream_detail_single
        } else {
            R.layout.fragment_camera_stream_detail
        }
        return inflater.inflate(layoutId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rgScaleLayout = view.findViewById(R.id.rg_scale)
        mrgLensTypeLayout = view.findViewById(R.id.rg_lens_type)
        cameraSurfaceView = view.findViewById(R.id.sv_camera)
        btnDownloadYUV = view.findViewById(R.id.btn_download_yuv)
        tvCameraName = view.findViewById(R.id.tv_camera_name)
        btnCloseOrOpen = view.findViewById(R.id.btn_close_or_open)
        rgScaleLayout.setOnCheckedChangeListener(onScaleChangeListener)
        mrgLensTypeLayout.setOnCheckedChangeListener(mOnLensChangeListener)
        btnCloseOrOpen.setOnClickListener(onOpenOrCloseCheckListener)
        cameraSurfaceView.holder.addCallback(cameraSurfaceCallback)

        btnDownloadYUV.setOnClickListener {
            downloadYUVImage()
        }

        initViewModel()

        onOpenOrCloseCheckListener.onClick(btnCloseOrOpen)
    }

    private fun initViewModel() {
        viewModel.setCameraIndex(cameraIndex)

        viewModel.availableLensListData.observe(viewLifecycleOwner) { availableLensList ->
            for (i in 0 until mrgLensTypeLayout.childCount) {
                val childView = mrgLensTypeLayout.getChildAt(i)
                if (childView is RadioButton) {
                    childView.visibility = View.GONE
                }
            }
            if (availableLensList.isEmpty()) {
                val childView = mrgLensTypeLayout.findViewWithTag<View>(CameraVideoStreamSourceType.DEFAULT_CAMERA.toString())
                if (childView != null) {
                    childView.visibility = View.VISIBLE
                }
            } else {
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
    }

    private fun updateCameraStream() {
        if (isNeedPreviewCamera) {
            sv_camera.visibility = View.VISIBLE
        } else {
            sv_camera.visibility = View.GONE
        }
        if (width <= 0 || height <= 0 || surface == null || !isNeedPreviewCamera) {
            if (surface != null) {
                viewModel.removeCameraStreamSurface(surface!!)
            }
            return
        }
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
        AlertDialog.Builder(requireContext(), R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
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
                    val name =  formatList[selectedIndex[0]]
                    viewModel.downloadYUVImageToLocal(format!!,name)
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

    private val onOpenOrCloseCheckListener = View.OnClickListener { _ ->
        btnCloseOrOpen.isSelected = !btnCloseOrOpen.isSelected
        isNeedPreviewCamera = btnCloseOrOpen.isSelected
        if (btnCloseOrOpen.isSelected) {
            btnCloseOrOpen.text = "close"
        } else {
            btnCloseOrOpen.text = "open"
        }
        updateCameraStream()
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