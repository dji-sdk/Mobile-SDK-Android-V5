package dji.sampleV5.moduleaircraft.pages

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.models.PayLoadVM
import dji.v5.utils.common.LogUtils
import kotlinx.android.synthetic.main.frag_flight_record_page.*
import kotlinx.android.synthetic.main.frag_payload_page.*


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
        const val TAG = "PayLoadDataTestFragment"
        const val MAX_LENGTH_OF_SEND_DATA = 255

    }

    private var messageList = ArrayList<String>()
    private lateinit var payLoadAdapter: ArrayAdapter<String>
    private val payLoadVM by lazy {
        ViewModelProvider(
            requireActivity(),
            ViewModelProvider.NewInstanceFactory()
        ).get(PayLoadVM::class.java)
    }
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_payload_page, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initBtnListener()

    }

    private fun initView() {
        payLoadAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, messageList)
        message_listview.adapter = payLoadAdapter

        payLoadVM.getSendMessageLiveData()
            .observe(viewLifecycleOwner,
                { t ->
                    t?.run {
                        if (isSuccess) {
                            ToastUtils.showToast("Send success")
                        } else {
                            ToastUtils.showToast("Send fail:$msg")

                        }
                    }
                })

        payLoadVM.getReceiveMessageLiveData()
            .observe(viewLifecycleOwner, { t ->
                t?.run {
                    if (isSuccess) {
                        LogUtils.d(TAG, data)
                        messageList.add(data ?: "Response is empty")
                        payLoadAdapter.notifyDataSetChanged()
                        message_listview.setSelection(messageList.size - 1)
                    } else {
                        ToastUtils.showToast("Receive fail：${msg}")
                    }

                }
            })


        payLoadVM.getProductNameLiveData().observe(viewLifecycleOwner,
            { t ->
                t.run {
                    if (isSuccess) {
                        ToastUtils.showToast("GetProductName success：ProductName=${data}")
                    } else {
                        ToastUtils.showToast("GetProductName fail：${msg}")

                    }
                }
            })

    }


    private fun initBtnListener() {

        ed_data.setOnKeyListener(onKeyListener)

        btn_send_data_to_payload.setOnClickListener {
            LogUtils.i(TAG, "------------------------Start sending data to PSDK----------------------------")
            val sendByteArray = ed_data.text.toString().toByteArray()
            val size = sendByteArray.size
            val sendText = "The content sent is：${ed_data.text},byte length is：$size"
            if (size > MAX_LENGTH_OF_SEND_DATA) {
                ToastUtils.showToast("The length of the sent content is $size bytes, which exceeds the maximum sending length of $MAX_LENGTH_OF_SEND_DATA bytes," +
                        " and the sending fails!!!")
                return@setOnClickListener
            }
            payLoadVM.sendMessageToPayLoadSdk(sendByteArray)
            LogUtils.d(TAG, sendText)
            ToastUtils.showToast(sendText)
        }

        btn_receive_data_from_payload.setOnClickListener {
            LogUtils.i(TAG, "------------------------Start receiving PSDK data----------------------------")
            ToastUtils.showToast("Start receiving PSDK data")
            payLoadVM.receiveFromPayLoadSdk()

        }

        btn_get_payload_product_name.setOnClickListener {
            LogUtils.i(
                TAG,
                "------------------------Start Receive PayLoad_Product_Name----------------------------"
            )
            ToastUtils.showToast("Start getting PayLoad_Product_Name")
            payLoadVM.getProductName()
        }

    }


}