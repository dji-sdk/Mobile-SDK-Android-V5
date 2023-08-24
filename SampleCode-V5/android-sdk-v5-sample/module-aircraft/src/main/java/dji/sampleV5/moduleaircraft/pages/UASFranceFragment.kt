package dji.sampleV5.moduleaircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.viewModels
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.models.UASFranceVM
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.sampleV5.modulecommon.util.ToastUtils
import kotlinx.android.synthetic.main.frag_uas_france_page.*

/**
 * Description :法国无人机远程识别示例
 *
 * @author: Byte.Cai
 *  date : 2022/6/27
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class UASFranceFragment : DJIFragment(), CompoundButton.OnCheckedChangeListener {
    private val uasFranceVM: UASFranceVM by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_uas_france_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListener()

    }

    private fun initListener() {
        tb_eid_enable_switch.setOnCheckedChangeListener(this)
        uasFranceVM.addElectronicIDStatusListener()
        uasFranceVM.electronicIDStatus.observe(viewLifecycleOwner) {
            tv_eid_enable_tip.text = "isElectronicIDEnabled:${it.isElectronicIDEnabled}"

            tb_eid_enable_switch.setOnCheckedChangeListener(null)
            tb_eid_enable_switch.isChecked = it.isElectronicIDEnabled
            tb_eid_enable_switch.setOnCheckedChangeListener(this@UASFranceFragment)
        }
    }


    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        uasFranceVM.setElectronicIDEnabled(isChecked, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                ToastUtils.showToast("setElectronicIDEnabled success")
            }


            override fun onFailure(error: IDJIError) {
                tb_eid_enable_switch.setOnCheckedChangeListener(null)
                tb_eid_enable_switch.isChecked = !isChecked
                tb_eid_enable_switch.setOnCheckedChangeListener(this@UASFranceFragment)
                ToastUtils.showToast(error.toString())
            }

        })
    }

    override fun onDestroy() {
        super.onDestroy()
        uasFranceVM.clearAllElectronicIDStatusListener()
    }
}