package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dji.open.pilot.base.ui.RemoteView
import dji.open.pilot.base.ui.model.ViewPageType
import dji.open.pilot.base.ui.model.ViewSide
import dji.open.pilot.base.ui.model.WidgetLayoutParam
import dji.open.pilot.client.manager.remotecontrol.RemoteControlManager
import dji.sampleV5.aircraft.databinding.FragRemoteControlPageBinding
import dji.v5.ux.util.ToastUtils
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.ux.remoteview.RemoteViewHelper

class RemoteControlFragment : DJIFragment() {

    private lateinit var binding: FragRemoteControlPageBinding

    private val pageType = ViewPageType.FLOATING_BOX

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragRemoteControlPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        binding.btnInitRemoteView.setOnClickListener {
            initRemoteView()
        }
        binding.btnClearRemoteView.setOnClickListener {
            clearRemoteView()
        }
    }

    /**
     * 在 Pilot 浮框上创建相机切换按钮组。
     * 按钮渲染在 Pilot 界面，BaseFpvActivity 保持前台，TAG_MAIN_CAM_SWITCH 事件可被正常接收。
     */
    private fun initRemoteView() {
        val helper = RemoteViewHelper.obtain(pageType)
        helper.clearLayout()
        helper.setPageLayoutParam(420)

        // 根容器：垂直线性布局，锚定到浮框顶部
        val container = helper.createLinearLayoutVertical().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        container.addToRoot(null)
        container.setConstraintsOnRoot(ViewSide.START, ViewSide.START, 4)
        container.setConstraintsOnRoot(ViewSide.END, ViewSide.END, 4)
        container.setConstraintsOnRoot(ViewSide.TOP, ViewSide.TOP, 4)

        // 标题
        val title = helper.createText().apply {
            setText("Camera View Switch")
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(20)
        }
        container.addToRemote(title)

        // 按钮水平容器
        val row = helper.createLinearLayoutHorizontal().apply {
            setWidth(RemoteView.MATCH_PARENT)
            setHeight(RemoteView.WRAP_CONTENT)
        }
        container.addToRemote(row)

        // 4 个相机切换按钮
        val cameras = listOf(
            "MAIN"  to ComponentIndexType.LEFT_OR_MAIN,
            "FPV"   to ComponentIndexType.FPV,
            "RIGHT" to ComponentIndexType.RIGHT,
            "UP"    to ComponentIndexType.UP
        )

        cameras.forEach { (label, index) ->
            val btn = helper.createButton().apply {
                setText(label)
                setWidth(35)
                setHeight(30)
            }
            row.addToRemote(btn, WidgetLayoutParam().apply {
                setMargin(4, 0, 0, 4)
            })
            btn.setOnClickListener {
                switchCamera(index)
            }
        }

        appendResult("Remote view initialized.\nGo to Pilot floating box and tap buttons.")
    }

    private fun clearRemoteView() {
        RemoteViewHelper.obtain(pageType).clearLayout()
        appendResult("Remote view cleared.")
    }

    private fun switchCamera(index: ComponentIndexType) {
        RemoteControlManager.getInstance().switchToPrimaryCameraView(
            index,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    val msg = "switchMainCameraView(${index.name}) onSuccess"
                    appendResult(msg)
                    ToastUtils.showToast(msg)
                }

                override fun onFailure(error: IDJIError) {
                    val msg = "switchMainCameraView(${index.name}) onFailure: ${error.errorCode()}"
                    appendResult(msg)
                    ToastUtils.showToast(msg)
                }
            }
        )
    }

    private fun appendResult(msg: String) {
        activity?.runOnUiThread {
            val current = binding.tvResult.text.toString()
            val updated = if (current == "Result will be shown here") msg
                          else "$current\n$msg"
            binding.tvResult.text = updated
        }
    }
}
