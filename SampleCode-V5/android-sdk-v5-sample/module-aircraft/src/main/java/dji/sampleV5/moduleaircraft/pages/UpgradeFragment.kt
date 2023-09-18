package dji.sampleV5.moduleaircraft.pages

import android.content.Intent
import android.os.Bundle

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.models.UpgradeVM
import dji.upgrade.component.firmware.model.FirmwareUpgradingProcessState
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.upgrade.UpgradeProgressState
import dji.v5.manager.aircraft.upgrade.UpgradeableComponent
import dji.v5.manager.aircraft.upgrade.model.ComponentType
import dji.v5.utils.common.DocumentUtil
import dji.sampleV5.modulecommon.util.ToastUtils
import kotlinx.android.synthetic.main.firmware_offline_upgrade_item.*
import kotlinx.android.synthetic.main.firmware_offline_upgrade_item.view.*
import kotlinx.android.synthetic.main.frag_upgrade_page.*
import java.util.*


/**
 * @author feel.feng
 * @time 2022/01/26 11:22 上午
 * @description:
 */
class UpgradeFragment : DJIFragment(){
    private val UpgradeVM: UpgradeVM by activityViewModels()
    var sb = StringBuffer()
    var componentType: ComponentType = ComponentType.AIRCRAFT

    var lancher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
       result?.apply {
            var uri = data?.data
            var path = DocumentUtil.getPath(requireContext() , uri);
           offline_component_package_name.setText(path)
           ToastUtils.showToast("offline path:" + path)
       }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_upgrade_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initBtnListener()

        UpgradeVM.addUpgradeInfoListener()
        UpgradeVM.upgradeStateInfo.observe(viewLifecycleOwner){
            var formatProgress = String.format(Locale.US , "%d%%" , it.progress);
            offline_upgrade_progress_tv.text = "state:${it.upgradeState.name} progress:${formatProgress} error:${it.error?.description}"
            updateOfflineBtn(it.upgradeState)
        }

    }

    private fun showInfo() {

        val components: List<UpgradeableComponent>  = UpgradeVM.getUpgradeableComponents()

        for ( component in components) {
            sb.append("ComponentType  : " + component.componentType)
                    .append("\n")
                    .append("firmwareVersion : " + component.firmwareInformation?.version)
                    .append("\n")
                    .append("latestFwInfo : " + component.latestFirmwareInformation?.version)
                    .append("\n")
                    .append("state : " + component.state )
                    .append("\n")

        }
        upgrade_state_info_tv?.setText(sb.toString())
        sb.delete(0, sb.length)


    }

    private fun initBtnListener() {
        btn_get_upgrade_state.setOnClickListener {
            UpgradeVM.checkUpgradeableComponents(object : CommonCallbacks.CompletionCallbackWithParam<ComponentType>{
                override fun onSuccess(t: ComponentType?) {
                    mainHandler.post{
                        showInfo()
                    }
                }
                override fun onFailure(error: IDJIError) {
                    mainHandler.post{
                        showInfo()
                    }
                }

            })
        }

        offline_component_package_select_btn.setOnClickListener {
            openFileFolder()
        }


        offline_start_upgrade.setOnClickListener{
            var filePath = offline_component_package_name.text
            if (TextUtils.isEmpty(filePath)) {
                ToastUtils.showToast("Please select offline firmware version ")
                return@setOnClickListener
            }
            ToastUtils.showToast("start Offline Upgrade")
            UpgradeVM.startOfflineUpgrade(componentType , filePath.toString())
        }

        rg_component_select.setOnCheckedChangeListener(object :RadioGroup.OnCheckedChangeListener{
            override fun onCheckedChanged(view: RadioGroup?, checkId: Int) {
                when (checkId) {
                    R.id.rb_aircraft -> {componentType = ComponentType.AIRCRAFT}
                    R.id.rb_rc -> {componentType = ComponentType.REMOTE_CONTROLLER}
                }
            }

        })

        btn_show_offline.setOnClickListener{
            updateView()
        }
    }

    private fun updateView(){
        when {
            lyt_offline.isVisible -> {
                lyt_offline.visibility = View.INVISIBLE
                btn_show_offline.text = "show offline upgrade"
            }
            else -> {
                lyt_offline.visibility = View.VISIBLE
                btn_show_offline.text = "hide offline upgrade"
            }
        }

    }

    private fun updateOfflineBtn(  state : UpgradeProgressState){
        when (state) {
            UpgradeProgressState.UPGRADE_SUCCESS, UpgradeProgressState.INITIALIZING , UpgradeProgressState.TRANSFER_END -> {
                offline_start_upgrade.isEnabled = true
                offline_start_upgrade.alpha = 1f

            }
            else -> {
                offline_start_upgrade.isEnabled = false
                offline_start_upgrade.alpha = 0.5f
            }
        }
    }
    private fun openFileFolder(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/zip"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        lancher.launch(intent)
    }


}