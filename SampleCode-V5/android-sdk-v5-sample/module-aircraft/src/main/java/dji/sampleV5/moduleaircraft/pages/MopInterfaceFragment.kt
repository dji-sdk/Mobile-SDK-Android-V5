package dji.sampleV5.moduleaircraft.pages

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.models.MopVM
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sdk.keyvalue.value.mop.PipelineDeviceType
import dji.sdk.keyvalue.value.mop.TransmissionControlType
import dji.v5.utils.common.LogUtils
import kotlinx.android.synthetic.main.frag_mop_interface_page.*
import kotlinx.android.synthetic.main.frag_mop_interface_page.btn_disconnect
import kotlinx.android.synthetic.main.frag_mop_interface_page.cb_reliable
import kotlinx.android.synthetic.main.frag_mop_interface_page.et_channel_id
import kotlinx.android.synthetic.main.frag_mop_interface_page.rg_mop_type
import kotlinx.android.synthetic.main.frag_payload_data_page.btn_send_data_to_payload
import kotlinx.android.synthetic.main.frag_payload_data_page.ed_data
import kotlinx.android.synthetic.main.frag_payload_data_page.message_listview

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
    private var messageList = ArrayList<String>()
    private lateinit var payLoadAdapter: ArrayAdapter<String>


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
        return inflater.inflate(R.layout.frag_mop_interface_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListener()
    }

    private fun initListener() {
        mopVM.initListener()
        ed_data.setOnKeyListener(onKeyListener)
        btn_connect.setOnClickListener {
            val deviceType = getType(rg_mop_type.checkedRadioButtonId)
            val transferType = if (cb_reliable.isChecked) TransmissionControlType.STABLE else TransmissionControlType.UNRELIABLE
            val id = et_channel_id.text.toString().trim().toInt()
            mopVM.connect(id, deviceType, transferType)
        }

        btn_disconnect.setOnClickListener {
            mopVM.stopMop()
        }

        btn_send_data_to_payload.setOnClickListener {
            LogUtils.i(PayLoadDataFragment.TAG, "------------------------Start sending data ---------------------------")
            val sendByteArray = ed_data.text.toString().toByteArray()
            mopVM.sendData(sendByteArray)
        }
    }

    private fun initView() {
        payLoadAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, messageList)
        message_listview.adapter = payLoadAdapter


        mopVM.receiveMessageLiveData.observe(viewLifecycleOwner, { t ->
            LogUtils.i(TAG, t)
            messageList.add(t)
            payLoadAdapter.notifyDataSetChanged()
            message_listview.setSelection(messageList.size - 1)
        })
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