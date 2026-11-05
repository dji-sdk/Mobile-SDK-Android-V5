package dji.sampleV5.aircraft.pages

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import dji.sampleV5.aircraft.databinding.FragPayloadDataPageBinding
import dji.sampleV5.aircraft.models.PayLoadDataVM
import dji.v5.ux.util.ToastUtils
import dji.v5.manager.aircraft.payload.PayloadIndexType
import dji.v5.utils.common.LogUtils

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2022/2/22
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class PayLoadDataFragment : DJIFragment() {
    companion object {
        const val TAG = "PayLoadDataFragment"
        const val MAX_LENGTH_OF_SEND_DATA = 255
    }

    private var binding: FragPayloadDataPageBinding? = null
    private var payloadIndexType: PayloadIndexType = PayloadIndexType.UNKNOWN
    private var messageList = ArrayList<String>()
    private lateinit var payLoadAdapter: ArrayAdapter<String>
    private val payLoadDataVM: PayLoadDataVM by viewModels()
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
        binding = FragPayloadDataPageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListener()
    }

    private fun initView() {
        arguments?.run {
            payloadIndexType = PayloadIndexType.find(getInt(PayloadCenterFragment.KEY_PAYLOAD_INDEX_TYPE, PayloadIndexType.UP.value()))
        }
        payLoadAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, messageList)
        binding?.messageListview?.adapter = payLoadAdapter

        payLoadDataVM.receiveMessageLiveData.observe(viewLifecycleOwner) { t ->
            LogUtils.i(TAG, t)
            messageList.add(t)
            payLoadAdapter.notifyDataSetChanged()
            binding?.messageListview?.setSelection(messageList.size - 1)
        }
        binding?.tvPayloadTitle?.text = "This is ${payloadIndexType.name.lowercase()} payloadManager data page!"
    }


    private fun initListener() {
        payLoadDataVM.initPayloadDataListener(payloadIndexType)
        binding?.edData?.setOnKeyListener(onKeyListener)

        binding?.btnSendDataToPayload?.setOnClickListener {
            LogUtils.i(TAG, "------------------------Start sending data to PSDK----------------------------")
            val sendByteArray = binding?.edData?.text.toString().toByteArray()
            val size = sendByteArray.size
            val sendText = "The content sent is：${binding?.edData?.text},byte length is：$size"
            if (size > MAX_LENGTH_OF_SEND_DATA) {
                ToastUtils.showToast(
                    "The length of the sent content is $size bytes, which exceeds the maximum sending length of $MAX_LENGTH_OF_SEND_DATA bytes," +
                            " and the sending fails!!!"
                )
                return@setOnClickListener
            }
            payLoadDataVM.sendMessageToPayLoadSdk(sendByteArray)
            LogUtils.i(TAG, sendText)
            ToastUtils.showToast(sendText)
        }

    }


}