package dji.sampleV5.moduleaircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.models.UpgradeVM
import dji.v5.manager.aircraft.upgrade.UpgradeManager
import dji.v5.manager.aircraft.upgrade.UpgradeableComponent
import kotlinx.android.synthetic.main.frag_upgrade_page.*


/**
 * @author feel.feng
 * @time 2022/01/26 11:22 上午
 * @description:
 */
class UpgradeFragment : DJIFragment(){
    private val UpgradeVM: UpgradeVM by activityViewModels()
    var sb = StringBuffer()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_upgrade_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        UpgradeManager.getInstance()
        initListener();
    }

    private fun showInfo() {

        val components: List<UpgradeableComponent>  = UpgradeVM.getUpgradeableComponents()

        for ( component in components) {
            sb.append("ComponentType  : " + component.componentType)
                    .append("\n")
                    .append("firmwareVersion : " + component.firmwareVersion)
                    .append("\n")
                    .append("latestFwInfo : " + component.latestFwInfo)
                    .append("\n")
                    .append("state : " + component.state )
                    .append("\n")

        }
        upgrade_state_info_tv.setText(sb.toString())
        sb.delete(0, sb.length)


    }

    private fun initListener() {
        btn_get_upgrade_state.setOnClickListener {
            showInfo()

        }
    }


}