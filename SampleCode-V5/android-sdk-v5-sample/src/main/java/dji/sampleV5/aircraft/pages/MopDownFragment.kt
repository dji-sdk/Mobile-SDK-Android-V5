package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.data.PipelineAdapter
import dji.sampleV5.aircraft.databinding.FragMopDownPageBinding
import dji.sampleV5.aircraft.models.MopVM
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.mop.PipelineDeviceType
import dji.sdk.keyvalue.value.mop.TransmissionControlType
import dji.v5.manager.mop.Pipeline

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
    private var binding: FragMopDownPageBinding? = null
    private var payloadIndex: ComponentIndexType = ComponentIndexType.LEFT_OR_MAIN

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragMopDownPageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListener()
    }

    private fun initView() {
        adapter = PipelineAdapter(context, ArrayList())
        binding?.rcPipeline?.layoutManager = LinearLayoutManager(context)
        binding?.rcPipeline?.adapter = adapter
        binding?.rcPipeline?.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

        binding?.spChooseComponent?.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                val selectedItem = parent?.getItemAtPosition(index).toString()
                payloadIndex = ComponentIndexType.valueOf(selectedItem)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                //do nothing
            }
        }
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
        binding?.tvConnect?.setOnClickListener {
            val deviceType = getType(binding?.rgMopType?.checkedRadioButtonId ?: -1)
            val transferType = if (binding?.cbReliable?.isChecked ?: false) TransmissionControlType.STABLE else TransmissionControlType.UNRELIABLE
            val id = binding?.etChannelId?.text.toString().trim().toInt()
            mopVM.connect(payloadIndex, id, deviceType, transferType, true)
        }

        mopVM.pipelineMapLivData.observe(viewLifecycleOwner) {
            it.forEach { map ->
                adapter?.addItem(payloadIndex, map.value)
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        mopVM.stopMop()
    }
}