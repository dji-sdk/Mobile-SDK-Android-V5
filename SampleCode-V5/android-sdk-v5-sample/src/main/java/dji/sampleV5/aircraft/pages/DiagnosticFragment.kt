package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.models.DiagnosticVm
import kotlinx.android.synthetic.main.frag_diagnostic_page.*

class DiagnosticFragment : DJIFragment() {

    private val diagnosticVm: DiagnosticVm by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_diagnostic_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        diagnosticVm.startListenDeviceHealthInfoChange()
        diagnosticVm.startListenDeviceStatusChange()

        diagnosticVm.deviceHealthInfos.observe(viewLifecycleOwner) {
            updateDiagnosticMsg()
        }

        diagnosticVm.currentDeviceStatus.observe(viewLifecycleOwner) {
            updateDiagnosticMsg()
        }

        diagnosticVm.currentDeviceStatus.observe(viewLifecycleOwner) {
            updateDiagnosticMsg()
        }
    }

    private fun updateDiagnosticMsg() {
        val builder = StringBuffer()

        builder.append("DJIDeviceHealthInfo List:").append("\n")
        diagnosticVm.deviceHealthInfos.value?.forEach {
            builder.append("    ").append("code:").append(it.informationCode()).append("\n")
            builder.append("    ").append("warningLevel:").append(it.warningLevel()).append("\n")
            builder.append("    ").append("title:").append(it.title()).append("\n")
            builder.append("    ").append("componentId:").append(it.componentId()).append("\n")
            builder.append("    ").append("sensorIndex:").append(it.sensorIndex()).append("\n")
            builder.append("    ").append("description:").append(it.description()).append("\n")
            builder.append("*******************************").append("\n")
        }

        builder.append("Last DJIDeviceStatus:")
        diagnosticVm.lastDeviceStatus.value?.let {
            builder.append(it).append("(").append(it.description()).append(")")
            builder.append(":").append(it.statusCode()).append("(").append(it.warningLevel()).append(")")
            builder.append("\n")
        }

        builder.append("Current DJIDeviceStatus:")
        diagnosticVm.currentDeviceStatus.value?.let {
            builder.append(it).append("(").append(it.description()).append(")")
            builder.append(":").append(it.statusCode()).append("(").append(it.warningLevel()).append(")")
            builder.append("\n")
        }

        diagnostic_msg.text = builder.toString()
    }
}
