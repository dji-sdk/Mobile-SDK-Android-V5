package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.activityViewModels
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.models.DataProtectionVm
import kotlinx.android.synthetic.main.frag_data_protection_page.product_improvement_switch

class DataProtectionFragment : DJIFragment() {

    private val diagnosticVm: DataProtectionVm by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_data_protection_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        product_improvement_switch.isChecked = diagnosticVm.isAgreeToProductImprovement()
        product_improvement_switch.setOnCheckedChangeListener { _: CompoundButton?,
            isChecked: Boolean ->
            diagnosticVm.agreeToProductImprovement(isChecked)
        }
    }
}
