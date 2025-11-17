package dji.sampleV5.aircraft.pages

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.databinding.FragMopInterfacePageBinding
import dji.sampleV5.aircraft.models.MopVM
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.mop.PipelineDeviceType
import dji.sdk.keyvalue.value.mop.TransmissionControlType
import dji.v5.utils.common.LogUtils

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2023/1/31
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class MopInterfaceFragment : DJIFragment() {
    companion object {
        const val TAG = "MopFragment"
    }

    private val mopVM: MopVM by viewModels()
    private var binding: FragMopInterfacePageBinding? = null
    private var messageList = ArrayList<String>()
    private lateinit var payLoadAdapter: ArrayAdapter<String>
    private var payloadIndex: ComponentIndexType = ComponentIndexType.LEFT_OR_MAIN

    private val onKeyListener: View.OnKeyListener = object : View.OnKeyListener {
        override
        fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                //隐藏软键盘
                val inputMethodManager =
                    activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                if (inputMethodManager.isActive) {
                    inputMethodManager.hideSoftInputFromWindow(v.applicationWindowToken, 0)
                }
                return true
            }
            return false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragMopInterfacePageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListener()
    }

    private fun initListener() {
        mopVM.initListener()
        binding?.edData?.setOnKeyListener(onKeyListener)
        binding?.btnConnect?.setOnClickListener {
            val deviceType = getType(binding?.rgMopType?.checkedRadioButtonId ?: -1)
            val transferType = if (binding?.cbReliable?.isChecked ?: false) TransmissionControlType.STABLE else TransmissionControlType.UNRELIABLE
            val id = binding?.etChannelId?.text.toString().trim().toInt()
            mopVM.connect(payloadIndex, id, deviceType, transferType)
        }

        binding?.btnDisconnect?.setOnClickListener {
            mopVM.stopMop()
        }

        binding?.btnSendDataToPayload?.setOnClickListener {
            LogUtils.i(PayLoadDataFragment.TAG, "------------------------Start sending data ---------------------------")
            val sendByteArray = binding?.edData?.text.toString().toByteArray()
            mopVM.sendData(sendByteArray)
        }
    }

    private fun initView() {
        payLoadAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, messageList)
        binding?.messageListview?.adapter = payLoadAdapter

        mopVM.receiveMessageLiveData.observe(viewLifecycleOwner) { t ->
            LogUtils.i(TAG, t)
            messageList.add(t)
            payLoadAdapter.notifyDataSetChanged()
            binding?.messageListview?.setSelection(messageList.size - 1)
        }

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

    override fun onDestroy() {
        super.onDestroy()
        mopVM.stopMop()
    }

}