package dji.sampleV5.aircraft.pages

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dji.sampleV5.aircraft.databinding.FragIntelligentBoxPageBinding
import dji.sampleV5.aircraft.models.IntelligentBoxVM
import dji.sampleV5.aircraft.pages.PayloadCenterFragment.Companion.KEY_PAYLOAD_INDEX_TYPE
import dji.sampleV5.aircraft.util.Helper
import dji.v5.manager.aircraft.aibox.IntelligentBoxAppInfo
import dji.v5.manager.aircraft.aibox.IntelligentBoxInfo
import dji.v5.manager.aircraft.payload.PayloadIndexType
import dji.v5.utils.common.JsonUtil

class IntelligentBoxFragment : DJIFragment() {

    private val TAG = "IntelligentBoxFragment"

    private var payloadIndexType: PayloadIndexType = PayloadIndexType.UNKNOWN
    private var intelligentBoxInfo: IntelligentBoxInfo = IntelligentBoxInfo()
    private var intelligentBoxAppInfos: List<IntelligentBoxAppInfo> = emptyList()
    private val intelligentBoxVM: IntelligentBoxVM by viewModels()

    private var binding: FragIntelligentBoxPageBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragIntelligentBoxPageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        arguments?.run {
            payloadIndexType = PayloadIndexType.find(getInt(KEY_PAYLOAD_INDEX_TYPE, PayloadIndexType.UP.value()))
        }
        intelligentBoxVM.initListener(payloadIndexType)
        intelligentBoxVM.intelligentBoxInfo.observe(viewLifecycleOwner) {
            intelligentBoxInfo = it
            updateInfo()
        }
        intelligentBoxVM.intelligentBoxAppInfos.observe(viewLifecycleOwner) {
            intelligentBoxAppInfos = it
            updateInfo()
        }

        binding?.btnGetBoxSerialNumber?.setOnClickListener {
            intelligentBoxVM.getBoxSerialNumber()
        }
        binding?.btnEnableApp?.setOnClickListener {
            val values = intelligentBoxAppInfos.map {
                it.appID
            }
            if (values.isEmpty()) return@setOnClickListener

            initPopupNumberPicker(Helper.makeList(values)) {
                intelligentBoxVM.enableApp(values[indexChosen[0]])
                resetIndex()
            }
        }
        binding?.btnDisableApp?.setOnClickListener {
            val values = intelligentBoxAppInfos.map {
                it.appID
            }
            if (values.isEmpty()) return@setOnClickListener

            initPopupNumberPicker(Helper.makeList(values)) {
                intelligentBoxVM.disableApp(values[indexChosen[0]])
                resetIndex()
            }
        }
        binding?.btnUninstallApp?.setOnClickListener {
            val values = intelligentBoxAppInfos.map {
                it.appID
            }
            if (values.isEmpty()) return@setOnClickListener

            initPopupNumberPicker(Helper.makeList(values)) {
                intelligentBoxVM.uninstallApp(values[indexChosen[0]])
                resetIndex()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateInfo() {
        activity?.runOnUiThread {
            binding?.boxInfo?.text = JsonUtil.toJson(intelligentBoxInfo) + "\n"
            binding?.appInfo?.text = JsonUtil.toJson(intelligentBoxAppInfos)
        }
    }
}