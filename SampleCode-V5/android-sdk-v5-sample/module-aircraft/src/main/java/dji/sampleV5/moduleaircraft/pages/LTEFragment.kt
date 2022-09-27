package dji.sampleV5.moduleaircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.models.LTEVM
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.v5.utils.common.JsonUtil
import kotlinx.android.synthetic.main.frag_lte_page.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/8/12
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class LTEFragment : DJIFragment() {

    private val lteVm: LTEVM by viewModels()
    private val lteMsgBuilder = StringBuffer()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_lte_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lteVm.initListener()
        lteVm.wlmLinkQualityLevel.observe(viewLifecycleOwner) {
            updateLteMsg()
        }
        lteVm.wlmAircraftDongleListInfo.observe(viewLifecycleOwner) {
            updateLteMsg()
        }
        lteVm.wlmRcDongleListInfo.observe(viewLifecycleOwner) {
            updateLteMsg()
        }
    }

    private fun updateLteMsg() {
        lteMsgBuilder.setLength(0)

        lteMsgBuilder.append("WlmLinkQualityLevel:").append("\n")
        lteVm.wlmLinkQualityLevel.value.let {
            lteMsgBuilder.append(JsonUtil.toJson(it))
        }

        lteMsgBuilder.append("WlmAircraftDongleListInfo:").append("\n")
        lteVm.wlmAircraftDongleListInfo.value.let {
            lteMsgBuilder.append(JsonUtil.toJson(it))
        }

        lteMsgBuilder.append("WlmRcDongleListInfo:").append("\n")
        lteVm.wlmRcDongleListInfo.value.let {
            lteMsgBuilder.append(JsonUtil.toJson(it))
        }

        lte_msg.text = lteMsgBuilder.toString()
    }
}