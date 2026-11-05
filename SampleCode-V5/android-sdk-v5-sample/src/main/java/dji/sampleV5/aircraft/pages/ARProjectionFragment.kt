package dji.sampleV5.aircraft.pages

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import dji.open.pilot.base.manager.ar.ARCircleInfo
import dji.open.pilot.base.manager.ar.ARLineInfo
import dji.open.pilot.base.manager.ar.ARPointConfig
import dji.open.pilot.base.manager.ar.ARPointInfo
import dji.open.pilot.base.manager.ar.ARPolygonInfo
import dji.open.pilot.base.ui.NavigateAction
import dji.open.pilot.client.manager.file.FileTransferManager
import dji.sampleV5.aircraft.databinding.FragArProjectionPageBinding
import dji.sampleV5.aircraft.models.ARProjectionVM
import dji.v5.ux.util.Helper
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.utils.common.FileUtils
import dji.v5.utils.common.JsonUtil
import dji.v5.ux.util.ToastUtils
import java.io.File

class ARProjectionFragment : DJIFragment() {
    enum class ArMessageType {
        CONFIG, INFO, LINE_INFO, POLYGON_INFO, CIRCLE_INFO
    }
    private val TAG:String = this.javaClass.name
    private val arProjectionVM: ARProjectionVM by activityViewModels()
    private lateinit var binding: FragArProjectionPageBinding
    private var arRemotePointInfos: List<ARPointInfo> = mutableListOf()
    private var arPointConfigs: List<ARPointConfig> = mutableListOf()
    private var arRemoteLineInfos: List<ARLineInfo> = mutableListOf()
    private var arRemotePolygonInfos: List<ARPolygonInfo> = mutableListOf()
    private var arRemoteCircleInfos: List<ARCircleInfo> = mutableListOf()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragArProjectionPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initMap()
    }

    private fun initMap() {
        arProjectionVM.arPointConfigs.observe(viewLifecycleOwner) {
            arPointConfigs = it.toList()
            updateArMessageView(ArMessageType.CONFIG)
        }

        arProjectionVM.arRemotePointInfos.observe(viewLifecycleOwner) {
            arRemotePointInfos = it
            updateArMessageView(ArMessageType.INFO)
        }

        arProjectionVM.arRemoteLineInfos.observe(viewLifecycleOwner) {
            arRemoteLineInfos = it
            updateArMessageView(ArMessageType.LINE_INFO)
        }

        arProjectionVM.arRemotePolygonInfos.observe(viewLifecycleOwner) {
            arRemotePolygonInfos = it
            updateArMessageView(ArMessageType.POLYGON_INFO)
        }

        arProjectionVM.arRemoteCircleInfos.observe(viewLifecycleOwner) {
            arRemoteCircleInfos = it
            updateArMessageView(ArMessageType.CIRCLE_INFO)
        }

        binding.btnJumpToHome.setOnClickListener {
            arProjectionVM.jumpTo(NavigateAction(NavigateAction.Value.PILOT_HOME))
        }

        binding.btnJumpToMap.setOnClickListener {
            arProjectionVM.jumpTo(NavigateAction(NavigateAction.Value.PILOT_MAP))
        }

        binding.btnPrepareRes.setOnClickListener {
            if (arProjectionVM.markerIconList.isEmpty()) {
                ToastUtils.showToast("no res")
                return@setOnClickListener
            }

            for (res in arProjectionVM.markerIconList) {
                val resFile = File(requireContext().getExternalFilesDir("/"), res)
                FileUtils.copyAssetsFileIfNeed(requireContext(), "plugin/$res", resFile)
                Helper.pushFileToRemote(resFile.path)
            }
            arProjectionVM.uploadARPointConfigs()
        }

        binding.btnQueryFileListFromRemote.setOnClickListener {
            queryFileListFromRemote()
        }

        // 1. 查询AR文本配置（你已有的）
        binding.btnQueryArConfig.setOnClickListener {
            arProjectionVM.queryARPointConfig()
        }

        // 2. 上传AR文本配置
        binding.btnUploadArConfig.setOnClickListener {
            arProjectionVM.uploadARPointConfigs()
        }

        // 3. 查询AR文本信息
        binding.btnQueryArInfo.setOnClickListener {
            arProjectionVM.queryARPointInfo()
        }

        // 4. 添加AR文本信息
        binding.btnAddArInfo.setOnClickListener {
            arProjectionVM.uploadARPointInfoList()
        }

        // 5. 删除AR文本信息
        binding.btnDeleteArInfo.setOnClickListener {
            arProjectionVM.queryARPointInfo()

            if (arRemotePointInfos.isEmpty()) {
                arProjectionVM.notifyUser("ARPointInfo为空！")
                return@setOnClickListener
            }

            for (it in arRemotePointInfos) {
                arProjectionVM.deleteARPointInfo(it)
            }
        }

        // 6. 更新AR文本信息
        binding.btnUpdateArInfo.setOnClickListener {
            arProjectionVM.queryARPointInfo()
            if (arRemotePointInfos.isEmpty()) {
                arProjectionVM.notifyUser("当前ARPointInfo为空，请先上传！")
                return@setOnClickListener
            }
            val arPointVisible = !binding.cbHideArPoint.isChecked
            val arPointIconVisible = !binding.cbHideIcon.isChecked
            val arPointTextVisible = !binding.cbHideText.isChecked

            for (arPointInfoImp in arRemotePointInfos){
                if (arPointInfoImp.name == "First") {
                    arPointInfoImp.isVisible = arPointVisible
                    arPointInfoImp.isIconVisible = arPointIconVisible
                    arPointInfoImp.isTextVisible = arPointTextVisible

                    arPointInfoImp.textColor = Color.BLACK
                    arPointInfoImp.iconColor = ARProjectionVM.colorMagenta
                    arPointInfoImp.text = "黑字，紫星"
                    arPointInfoImp.isShowEdgeGuide = true
                } else if (arPointInfoImp.name == "Second") {
                    arPointInfoImp.isVisible = arPointVisible
                    arPointInfoImp.isIconVisible = arPointIconVisible
                    arPointInfoImp.isTextVisible = arPointTextVisible

                    arPointInfoImp.configId = ARProjectionVM.arPointConfigImp03.id
                    arPointInfoImp.textColor = Color.BLACK
                    arPointInfoImp.iconColor = ARProjectionVM.colorMagenta
                    arPointInfoImp.text = "DJI--Pilot--Bus"
                    arPointInfoImp.isShowEdgeGuide = true
                } else if (arPointInfoImp.name == "Third") {
                    arPointInfoImp.isVisible = arPointVisible
                    arPointInfoImp.isIconVisible = arPointIconVisible
                    arPointInfoImp.isTextVisible = arPointTextVisible

                    arPointInfoImp.configId = ARProjectionVM.arPointConfigImp05.id
                    arPointInfoImp.textColor = Color.BLACK
                    arPointInfoImp.iconColor = ARProjectionVM.colorMagenta
                    arPointInfoImp.text = "Person(近大远小)"
                    arPointInfoImp.isShowEdgeGuide = true
                    arPointInfoImp.location.altitude -= 30
                    arPointInfoImp.location.longitude += 0.0001
                } else {
                    arPointInfoImp.textColor = Color.LTGRAY
                    arPointInfoImp.iconColor = Color.YELLOW
                    arPointInfoImp.isShowEdgeGuide = true
                }
                arProjectionVM.updateARPointInfo(arPointInfoImp)
            }
        }

        // ==================== AR Line ====================
        binding.btnUploadArLine.setOnClickListener {
            arProjectionVM.uploadARLineInfoList()
        }

        binding.btnQueryArLine.setOnClickListener {
            arProjectionVM.queryARLineInfo()
        }

        binding.btnUpdateArLine.setOnClickListener {
            if (arRemoteLineInfos.isEmpty()) {
                arProjectionVM.notifyUser("当前ARLineInfo为空，请先上传！")
                return@setOnClickListener
            }
            for (lineInfo in arRemoteLineInfos) {
                arProjectionVM.updateARLineInfo(lineInfo)
            }
        }

        binding.btnDeleteArLine.setOnClickListener {
            if (arRemoteLineInfos.isEmpty()) {
                arProjectionVM.notifyUser("当前ARLineInfo为空，请先查询！")
                return@setOnClickListener
            }
            for (lineInfo in arRemoteLineInfos) {
                arProjectionVM.deleteARLineInfo(lineInfo)
            }
        }

        // ==================== AR Polygon ====================
        binding.btnUploadArPolygon.setOnClickListener {
            arProjectionVM.uploadARPolygonInfoList()
        }

        binding.btnQueryArPolygon.setOnClickListener {
            arProjectionVM.queryARPolygonInfo()
        }

        binding.btnUpdateArPolygon.setOnClickListener {
            if (arRemotePolygonInfos.isEmpty()) {
                arProjectionVM.notifyUser("当前ARPolygonInfo为空，请先上传！")
                return@setOnClickListener
            }
            for (polygonInfo in arRemotePolygonInfos) {
                arProjectionVM.updateARPolygonInfo(polygonInfo)
            }
        }

        binding.btnDeleteArPolygon.setOnClickListener {
            if (arRemotePolygonInfos.isEmpty()) {
                arProjectionVM.notifyUser("当前ARPolygonInfo为空，请先查询！")
                return@setOnClickListener
            }
            for (polygonInfo in arRemotePolygonInfos) {
                arProjectionVM.deleteARPolygonInfo(polygonInfo)
            }
        }

        // ==================== AR Circle ====================
        binding.btnUploadArCircle.setOnClickListener {
            arProjectionVM.uploadARCircleInfoList()
        }

        binding.btnQueryArCircle.setOnClickListener {
            arProjectionVM.queryARCircleInfo()
        }

        binding.btnUpdateArCircle.setOnClickListener {
            if (arRemoteCircleInfos.isEmpty()) {
                arProjectionVM.notifyUser("当前ARCircleInfo为空，请先上传！")
                return@setOnClickListener
            }
            for (circleInfo in arRemoteCircleInfos) {
                arProjectionVM.updateARCircleInfo(circleInfo)
            }
        }

        binding.btnDeleteArCircle.setOnClickListener {
            if (arRemoteCircleInfos.isEmpty()) {
                arProjectionVM.notifyUser("当前ARCircleInfo为空，请先查询！")
                return@setOnClickListener
            }
            for (circleInfo in arRemoteCircleInfos) {
                arProjectionVM.deleteARCircleInfo(circleInfo)
            }
        }

        // 7. 开启雷达扫描范围
        binding.btnEnableArRadar.setOnClickListener {
            arProjectionVM.enableARRadar("Radar Scanning Range")
        }

        // 8. 关闭雷达扫描范围
        binding.btnDisableArRadar.setOnClickListener {
            arProjectionVM.disableARRadar()
        }

        // ============================== AR Line ==============================
        arProjectionVM.arLineInfos.observe(viewLifecycleOwner) {
            val sb = StringBuilder("AR Line Infos: ${it.size}\n")
            for (info in it) {
                sb.append(info.toString()).append("\n" + "-".repeat(50) + "\n")
            }
            binding.btnSensingTxt.text = sb.toString()
        }

        arProjectionVM.arPolygonInfos.observe(viewLifecycleOwner) {
            val sb = StringBuilder("AR Polygon Infos: ${it.size}\n")
            for (info in it) {
                sb.append(info.toString()).append("\n" + "-".repeat(50) + "\n")
            }
            binding.btnSensingTxt.text = sb.toString()
        }

        arProjectionVM.arCircleInfos.observe(viewLifecycleOwner) {
            val sb = StringBuilder("AR Circle Infos: ${it.size}\n")
            for (info in it) {
                sb.append(info.toString()).append("\n" + "-".repeat(50) + "\n")
            }
            binding.btnSensingTxt.text = sb.toString()
        }

        binding.btnUploadArLine.setOnClickListener { arProjectionVM.uploadARLine() }
        binding.btnQueryArLine.setOnClickListener { arProjectionVM.queryARLine() }
        binding.btnUpdateArLine.setOnClickListener { arProjectionVM.updateARLine() }
        binding.btnDeleteArLine.setOnClickListener { arProjectionVM.deleteARLine() }

        binding.btnUploadArPolygon.setOnClickListener { arProjectionVM.uploadARPolygon() }
        binding.btnQueryArPolygon.setOnClickListener { arProjectionVM.queryARPolygon() }
        binding.btnUpdateArPolygon.setOnClickListener { arProjectionVM.updateARPolygon() }
        binding.btnDeleteArPolygon.setOnClickListener { arProjectionVM.deleteARPolygon() }

        binding.btnUploadArCircle.setOnClickListener { arProjectionVM.uploadARCircle() }
        binding.btnQueryArCircle.setOnClickListener { arProjectionVM.queryARCircle() }
        binding.btnUpdateArCircle.setOnClickListener { arProjectionVM.updateARCircle() }
        binding.btnDeleteArCircle.setOnClickListener { arProjectionVM.deleteARCircle() }
    }

    private fun updateArMessageView(arTextType: ArMessageType) {
        val textView = binding.btnSensingTxt
        val sb = StringBuilder()
        when (arTextType) {
            ArMessageType.INFO -> {
                if (arRemotePointInfos.isEmpty()) {
                    sb.append("arPointInfo is empty")
                    return
                }
                for (info in arRemotePointInfos) {
                    sb.append(info.toString())
                    sb.append("\n" + "-".repeat(50) + "\n")
                }
            }
            ArMessageType.CONFIG -> {
                if (arPointConfigs.isEmpty()) {
                    sb.append("arPointConfig is empty")
                    return
                }
                for (config in arPointConfigs) {
                    sb.append(config.toString())
                    sb.append("\n" + "-".repeat(50) + "\n")
                }
            }
            ArMessageType.LINE_INFO -> {
                if (arRemoteLineInfos.isEmpty()) {
                    sb.append("arLineInfo is empty")
                    return
                }
                for (info in arRemoteLineInfos) {
                    sb.append(info.toString())
                    sb.append("\n" + "-".repeat(50) + "\n")
                }
            }
            ArMessageType.POLYGON_INFO -> {
                if (arRemotePolygonInfos.isEmpty()) {
                    sb.append("arPolygonInfo is empty")
                    return
                }
                for (info in arRemotePolygonInfos) {
                    sb.append(info.toString())
                    sb.append("\n" + "-".repeat(50) + "\n")
                }
            }
            ArMessageType.CIRCLE_INFO -> {
                if (arRemoteCircleInfos.isEmpty()) {
                    sb.append("arCircleInfo is empty")
                    return
                }
                for (info in arRemoteCircleInfos) {
                    sb.append(info.toString())
                    sb.append("\n" + "-".repeat(50) + "\n")
                }
            }
        }
        textView.text = sb.toString()
    }

    private fun queryFileListFromRemote() {
        FileTransferManager.getInstance().queryFileListFromRemote(object :
            CommonCallbacks.CompletionCallbackWithParam<List<String>> {
            override fun onSuccess(t: List<String>) {
                ToastUtils.showToast("queryFileListFromRemote onSuccess ${JsonUtil.toJson(t)}\n")
            }

            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast("queryFileListFromRemote onFailure $error\n")
            }
        })
    }
}