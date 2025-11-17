package dji.sampleV5.aircraft.pages

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.databinding.FragDataProtectionPageBinding
import dji.sampleV5.aircraft.models.DataProtectionVm
import dji.sampleV5.aircraft.util.Helper
import dji.sampleV5.aircraft.util.ToastUtils
import dji.v5.common.error.IDJIError
import dji.v5.common.ldm.LDMExemptModule
import dji.v5.network.DJIHttpCallback
import dji.v5.network.DJIHttpRequest
import dji.v5.network.DJIHttpResponse
import dji.v5.network.DJINetworkManager
import dji.v5.utils.common.DiskUtil

class DataProtectionFragment : DJIFragment() {

    private val diagnosticVm: DataProtectionVm by activityViewModels()
    private var binding: FragDataProtectionPageBinding? = null
    private var urlCache = "https://"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragDataProtectionPageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.productImprovementSwitch?.isChecked = diagnosticVm.isAgreeToProductImprovement()
        binding?.productImprovementSwitch?.setOnCheckedChangeListener { _: CompoundButton?,
            isChecked: Boolean ->
            diagnosticVm.agreeToProductImprovement(isChecked)
        }

        binding?.msdkLogSwitch?.isChecked = diagnosticVm.isLogEnable()
        binding?.msdkLogSwitch?.setOnCheckedChangeListener { _: CompoundButton?,
            isChecked: Boolean ->
            diagnosticVm.enableLog(isChecked)
        }

        binding?.logPathTv?.text = diagnosticVm.logPath()

        binding?.btnOpenLogPath?.setOnClickListener {
            val path = diagnosticVm.logPath()
            if (!path.contains(DiskUtil.SDCARD_ROOT)) {
                return@setOnClickListener
            }
            val uriPath = path.substring(DiskUtil.SDCARD_ROOT.length + 1, path.length - 1).replace("/", "%2f")
            Helper.openFileChooser(uriPath, activity)
        }

        binding?.btnClearLog?.setOnClickListener {
            val configDialog = requireContext().let {
                AlertDialog.Builder(it, androidx.appcompat.R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
                    .setTitle(R.string.clear_msdk_log)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ad_confirm) { configDialog, _ ->
                        kotlin.run {
                            diagnosticVm.clearLog()
                            configDialog.dismiss()
                        }
                    }
                    .setNegativeButton(R.string.ad_cancel) { configDialog, _ ->
                        kotlin.run {
                            configDialog.dismiss()
                        }
                    }
                    .create()
            }
            configDialog.show()
        }

        binding?.btnExportAndZipLog?.setOnClickListener {
            checkPermission()
            diagnosticVm.zipAndExportLog()
        }

        binding?.btnPing?.setOnClickListener {
            openInputDialog(urlCache, "Ping") {
                urlCache = it
                val request = DJIHttpRequest.Builder.newBuilder()
                    .ldmExemptModule(LDMExemptModule.MSDK_INIT_AND_REGISTRATION)
                    .requestType(DJIHttpRequest.RequestType.GET)
                    .url(it)
                    .build()
                DJINetworkManager.getInstance().enqueue(request, object :
                    DJIHttpCallback<DJIHttpResponse> {
                    override fun onFailure(error: IDJIError?) {
                        ToastUtils.showToast("url:$it,error:$error")
                    }

                    override fun onResponse(response: DJIHttpResponse) {
                        ToastUtils.showToast("url:$it,response:${response}")
                    }

                    override fun onLoading(current: Long, total: Long) {
//                    super.onLoading(current, total)
                    }
                })
            }
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val intent = Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION")
            startActivityForResult(intent, 0)
        }
    }
}
