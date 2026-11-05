package dji.sampleV5.aircraft.pages

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
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.databinding.FragUpgradePageBinding
import dji.sampleV5.aircraft.models.UpgradeVM
import dji.v5.ux.util.ToastUtils
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.upgrade.UpgradeProgressState
import dji.v5.manager.aircraft.upgrade.UpgradeableComponent
import dji.v5.manager.aircraft.upgrade.model.ComponentType
import dji.v5.utils.common.DocumentUtil
import java.util.Locale


/**
 * @author feel.feng
 * @time 2022/01/26 11:22 上午
 * @description:
 */
class UpgradeFragment : DJIFragment() {
    private val UpgradeVM: UpgradeVM by activityViewModels()
    private var binding: FragUpgradePageBinding? = null
    var sb = StringBuffer()
    var componentType: ComponentType = ComponentType.AIRCRAFT

    var lancher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result?.apply {
            val uri = data?.data
            val path = DocumentUtil.getPath(requireContext(), uri)
            binding?.offlineComponentAircraft?.offlineComponentPackageName?.setText(path)
            ToastUtils.showToast("offline path:$path")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragUpgradePageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initBtnListener()

        UpgradeVM.addUpgradeInfoListener()
        UpgradeVM.upgradeStateInfo.observe(viewLifecycleOwner) {
            val formatProgress = String.format(Locale.US, "%d%%", it.progress)
            binding?.offlineUpgradeProgressTv?.text = "state:${it.upgradeState.name} progress:${formatProgress} error:${it.error?.description}"
            updateOfflineBtn(it.upgradeState)
        }

    }

    private fun showInfo() {

        val components: List<UpgradeableComponent> = UpgradeVM.getUpgradeableComponents()

        for (component in components) {
            sb.append("ComponentType  : " + component.componentType)
                .append("\n")
                .append("firmwareVersion : " + component.firmwareInformation?.version)
                .append("\n")
                .append("latestFwInfo : " + component.latestFirmwareInformation?.version)
                .append("\n")
                .append("state : " + component.state)
                .append("\n")

        }
        binding?.upgradeStateInfoTv?.setText(sb.toString())
        sb.delete(0, sb.length)


    }

    private fun initBtnListener() {
        binding?.btnGetUpgradeState?.setOnClickListener {
            UpgradeVM.checkUpgradeableComponents(object :
                CommonCallbacks.CompletionCallbackWithParam<ComponentType> {
                override fun onSuccess(t: ComponentType?) {
                    mainHandler.post {
                        showInfo()
                    }
                }

                override fun onFailure(error: IDJIError) {
                    mainHandler.post {
                        showInfo()
                    }
                }

            })
        }

        binding?.offlineComponentAircraft?.offlineComponentPackageSelectBtn?.setOnClickListener {
            openFileFolder()
        }

        binding?.offlineStartUpgrade?.setOnClickListener {
            var filePath = binding?.offlineComponentAircraft?.offlineComponentPackageName?.text
            if (TextUtils.isEmpty(filePath)) {
                ToastUtils.showToast("Please select offline firmware version ")
                return@setOnClickListener
            }
            ToastUtils.showToast("start Offline Upgrade")
            UpgradeVM.startOfflineUpgrade(componentType, filePath.toString())
        }

        binding?.rgComponentSelect?.setOnCheckedChangeListener(object :
            RadioGroup.OnCheckedChangeListener {
            override fun onCheckedChanged(view: RadioGroup?, checkId: Int) {
                when (checkId) {
                    R.id.rb_aircraft -> {
                        componentType = ComponentType.AIRCRAFT
                    }

                    R.id.rb_rc -> {
                        componentType = ComponentType.REMOTE_CONTROLLER
                    }
                }
            }

        })

        binding?.btnShowOffline?.setOnClickListener {
            updateView()
        }
    }

    private fun updateView() {
        when {
            binding?.lytOffline?.isVisible == true -> {
                binding?.lytOffline?.visibility = View.INVISIBLE
                binding?.btnShowOffline?.text = "show offline upgrade"
            }

            else -> {
                binding?.lytOffline?.visibility = View.VISIBLE
                binding?.btnShowOffline?.text = "hide offline upgrade"
            }
        }
    }

    private fun updateOfflineBtn(state: UpgradeProgressState) {
        when (state) {
            UpgradeProgressState.UPGRADE_SUCCESS, UpgradeProgressState.INITIALIZING, UpgradeProgressState.TRANSFER_END -> {
                binding?.offlineStartUpgrade?.isEnabled = true
                binding?.offlineStartUpgrade?.alpha = 1f
            }

            else -> {
                binding?.offlineStartUpgrade?.isEnabled = false
                binding?.offlineStartUpgrade?.alpha = 0.5f
            }
        }
    }

    private fun openFileFolder() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/zip"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        lancher.launch(intent)
    }
}