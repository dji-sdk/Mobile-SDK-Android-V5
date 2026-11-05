package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dji.sampleV5.aircraft.databinding.FragCableInspectionPageBinding
import dji.sampleV5.aircraft.models.CableInspectionVM
import dji.sampleV5.aircraft.models.globalViewModels
import dji.v5.ux.util.ToastUtils

class CableInspectionFragment : DJIFragment() {

    private val cableVM: CableInspectionVM by globalViewModels()
    private lateinit var binding: FragCableInspectionPageBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragCableInspectionPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 方案 A：用户导航离开页面时确保飞控退出导线巡检状态
        // 覆盖场景：用户按返回键、导航到其他 Fragment 等正常页面离开行为
        cableVM.exit()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cableVM.initListener()
        initOpenRemoteBox()
        observeViewModel()
    }

    private fun observeViewModel() {
        cableVM.flyVer.observe(viewLifecycleOwner) { updateMsgView() }
        cableVM.availableCameraInfoList.observe(viewLifecycleOwner) { updateMsgView() }
        cableVM.areaCode.observe(viewLifecycleOwner) { updateMsgView() }
        cableVM.isSupport.observe(viewLifecycleOwner) { updateMsgView() }
        cableVM.selectedCameraIndex.observe(viewLifecycleOwner) { updateMsgView() }
        // 导线巡检状态与电线数据
        cableVM.inspectionState.observe(viewLifecycleOwner) { updateMsgView() }
        cableVM.cableArInfoList.observe(viewLifecycleOwner) { updateMsgView() }
    }

    private fun initOpenRemoteBox() {
        binding.btnChoiceCamera.setOnClickListener {
            selectCamera()
        }

        binding.btnInitCableInspection.setOnClickListener {
            val cameraList = cableVM.availableCameraInfoList.value ?: emptyList()
            when {
                cameraList.size > 1 && cableVM.selectedCameraIndex.value == null -> {
                    ToastUtils.showToast("检测到多个相机，请先选择相机")
                }
                cableVM.isSupport.value == true -> {
                    cableVM.initCableInspection()
                }
                else -> {
                    ToastUtils.showToast("当前设备不支持导线巡检")
                }
            }
        }
    }

    private fun selectCamera() {
        val cameraList = cableVM.availableCameraInfoList.value ?: emptyList()
        if (cameraList.isEmpty()) {
            ToastUtils.showToast("暂无符合要求的相机（仅支持H30、H30T）")
            return
        }
        // 只传入每个 CameraInfo 的 index 名称，不传整个对象
        val indexNames = ArrayList(cameraList.map { it.index.name })
        initPopupNumberPicker(indexNames) {
            // indexChosen[0] 由 DJIFragment.initPopupNumberPicker 写入，为用户选择的位置
            val selectedInfo = cameraList.getOrNull(indexChosen[0])
            selectedInfo?.let { cableVM.selectCamera(it.index) }
        }
    }

    private fun updateMsgView() {
        val cameraList = cableVM.availableCameraInfoList.value ?: emptyList()
        val selectedIndex = cableVM.selectedCameraIndex.value

        val sb = StringBuffer()
        sb.append("-------- Initialization --------\n")
        sb.append("flight controller version: ${cableVM.flyVer.value}\n")

        if (selectedIndex != null) {
            // 已选中相机：只展示该相机的参数
            val info = cameraList.find { it.index == selectedIndex }
            if (info != null) {
                sb.append("selected camera index: ${info.index}\n")
                sb.append("camera version: ${info.camVer}\n")
                sb.append("camera type: ${info.camType}\n")
            } else {
                sb.append("selected camera (${selectedIndex.name}): disconnected\n")
            }
        } else {
            // 尚未选择：展示全部可用相机
            if (cameraList.isEmpty()) {
                sb.append("camera: 暂无符合要求的相机（仅支持H30、H30T）\n")
            } else {
                cameraList.forEachIndexed { i, info ->
                    sb.append("camera[$i] index: ${info.index}\n")
                    sb.append("camera[$i] version: ${info.camVer}\n")
                    sb.append("camera[$i] type: ${info.camType}\n")
                }
            }
        }

        sb.append("country area: ${cableVM.areaCode.value}\n")
        sb.append("is support: ${cableVM.isSupport.value}\n")
        sb.append("-------- Initialization --------\n")

        // ── 导线巡检运行时状态 ────────────────────────────────────────
        sb.append("\n-------- Cable Inspection --------\n")
        sb.append("inspection state: ${cableVM.inspectionState.value}\n")
        val cables = cableVM.cableArInfoList.value ?: emptyList()
        sb.append("cables detected: ${cables.size}\n")
        cables.forEachIndexed { i, cable ->
            sb.append("  cable[$i] id=${cable.cable_id}\n")
        }
        sb.append("selected cable id: ${cableVM.getSelectedCableId()}\n")
        sb.append("-------- Cable Inspection --------\n")
        binding.msgText.text = sb.toString()

        binding.msgTextForInitialization.text =
            "最低飞控版本要求：${cableVM.flightMinVer}\n" +
            "\n" +
            "最低相机版本要求：${cableVM.cameraMinVer}\n" +
            "\n" +
            "国家地域要求：国内或者云端白名单国家支持\n" +
            "\n" +
            "相机机型要求：当前仅支持H30、H30T"
    }
}
