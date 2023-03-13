package dji.sampleV5.moduleaircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.data.PipelineAdapter
import dji.sampleV5.moduleaircraft.models.MopVM
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sdk.keyvalue.value.mop.PipelineDeviceType
import dji.sdk.keyvalue.value.mop.TransmissionControlType
import kotlinx.android.synthetic.main.frag_mop_down_page.*

import java.util.ArrayList

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2023/2/22
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class MopDownFragment : DJIFragment() {
    private val mopVM: MopVM by viewModels()
    private var adapter: PipelineAdapter? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_mop_down_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListener()

    }

    private fun initView() {
        adapter = PipelineAdapter(context, ArrayList())
        rc_pipeline.layoutManager = LinearLayoutManager(context)
        rc_pipeline.adapter = adapter
        rc_pipeline.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

    }

    private fun getType(checkedRadioButtonId: Int): PipelineDeviceType {
        return when (checkedRadioButtonId) {
            R.id.rb_on_board -> PipelineDeviceType.ONBOARD
            R.id.rb_payload -> PipelineDeviceType.PAYLOAD
            else -> PipelineDeviceType.PAYLOAD
        }
    }

    private fun initListener() {
        mopVM.initListener()
        tv_connect.setOnClickListener {
            val deviceType = getType(rg_mop_type.checkedRadioButtonId)
            val transferType = if (cb_reliable.isChecked) TransmissionControlType.STABLE else TransmissionControlType.UNRELIABLE
            val id = et_channel_id.text.toString().trim().toInt()
            mopVM.connect(id, deviceType, transferType, true)
        }

        mopVM.pipelineMapLivData.observe(viewLifecycleOwner) {
            it.forEach { map ->
                adapter?.addItem(map.value)
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        mopVM.stopMop()
    }
}