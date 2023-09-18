package dji.sampleV5.moduleaircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.data.PayloadWidgetIconAdapter
import dji.sampleV5.moduleaircraft.data.PayloadWidgetItem
import dji.sampleV5.moduleaircraft.models.PayloadWidgetVM
import dji.sampleV5.modulecommon.keyvalue.KeyItemActionListener
import dji.sampleV5.modulecommon.keyvalue.KeyValueDialogUtil
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sdk.keyvalue.value.payload.WidgetValue
import dji.v5.manager.aircraft.payload.PayloadIndexType
import dji.v5.manager.aircraft.payload.data.PayloadWidgetInfo
import dji.v5.manager.aircraft.payload.widget.PayloadWidget
import dji.v5.utils.common.LogUtils
import dji.sampleV5.modulecommon.util.ToastUtils
import kotlinx.android.synthetic.main.frag_payload_widget_page.*

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2022/12/1
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class PayloadWidgetFragment : DJIFragment() {
    private val TAG = "PayloadWidgetFragment"
    private var payloadIndexType: PayloadIndexType = PayloadIndexType.UNKNOWN

    private val payloadOtherWidgetInfo: StringBuilder = StringBuilder()
    private val payloadBasicInfo: StringBuilder = StringBuilder()

    private val payloadVM: PayloadWidgetVM by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_payload_widget_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
    }

    private fun initView() {
        arguments?.run {
            payloadIndexType = PayloadIndexType.find(getInt(PayloadCenterFragment.KEY_PAYLOAD_INDEX_TYPE, PayloadIndexType.UP.value()))
        }
        initWidgetList()
        initTitle()
        initWidgetBasicInfo()
        initButtonListener()
    }


    private fun initWidgetList() {
        val adapter = PayloadWidgetIconAdapter()
        payload_main_widget_list.adapter = adapter
        payload_main_widget_list.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        payloadVM.payloadWidgetInfo.observe(viewLifecycleOwner) {
            showMainInterfaceWidgetInfo(it, adapter)
            showPayloadOtherWidgetInfo(it)
        }


    }

    private fun showNotListWidgetInfo(widget: PayloadWidget, data: ArrayList<PayloadWidgetItem>) {
        val selectDesc =
            "selectedIcon=name:${widget.widgetName},index:${widget.widgetIndex},value:${widget.widgetValue},type:${widget.widgetType},hitMsg:${widget.hintMessage}"
        val mainSelectIconPath = widget?.iconFilePath?.selectedIconPath

        val unselectDesc =
            "unSelectedIcon=name:${widget.widgetName},index:${widget.widgetIndex},value:${widget.widgetValue},type:${widget.widgetType},hitMsg:${widget.hintMessage}"
        val mainUnSelectIconPath = widget?.iconFilePath?.unSelectedIconPath

        data.add(PayloadWidgetItem(selectDesc, mainSelectIconPath))
        data.add(PayloadWidgetItem(unselectDesc, mainUnSelectIconPath))
    }

    private fun showListWidgetInfo(widget: PayloadWidget, data: ArrayList<PayloadWidgetItem>) {
        //注意：List类型的Widget没有widget.iconFilePath为空，所以只拿subItemsList的IconFilePath
        for (subItem in widget.subItemsList) {
            val selectDesc =
                "subItemSelectIcon=name:${widget.widgetName},index:${widget.widgetIndex},value:${widget.widgetValue},type:${widget.widgetType}(${widget.widgetType.value()})},subItemsName=${subItem?.subItemsName},hitMsg:${widget.hintMessage}"
            val mainSelectIconPath = subItem?.subItemsIconFilePath?.selectedIconPath


            val unselectDesc =
                "subItemUnSelectIcon=name:${widget.widgetName},index:${widget.widgetIndex},value:${widget.widgetValue},type:${widget.widgetType}(${widget.widgetType.value()}),subItemsName=${subItem?.subItemsName},hitMsg:${widget.hintMessage}"
            val mainUnSelectIconPath = subItem?.subItemsIconFilePath?.unSelectedIconPath

            data.add(PayloadWidgetItem(selectDesc, mainSelectIconPath))
            data.add(PayloadWidgetItem(unselectDesc, mainUnSelectIconPath))
        }

    }


    private fun showMainInterfaceWidgetInfo(payloadWidgetInfo: PayloadWidgetInfo?, adapter: PayloadWidgetIconAdapter) {
        payloadWidgetInfo?.run {
            mainInterfaceWidgetList?.let {
                LogUtils.d(TAG, "initWidgetList mainInterfaceWidgetList=$it")
                val data = arrayListOf<PayloadWidgetItem>()
                for (widget in mainInterfaceWidgetList) {
                    val subItemsSize = widget?.subItemsList?.size ?: -1
                    if (subItemsSize > 0) {
                        showListWidgetInfo(widget, data)
                    } else {
                        showNotListWidgetInfo(widget, data)
                    }
                }
                adapter.submitList(data)
            }

        }
    }


    private fun showPayloadOtherWidgetInfo(payloadWidgetInfo: PayloadWidgetInfo?) {
        payloadWidgetInfo?.run {
            payloadOtherWidgetInfo.apply {
                clear()
                append("\n").append("PayloadWidgetInfo-Part1:").append("\n")
                append("TextInputBoxWidget:$textInputBoxWidget").append("\n")
                append("\n")
                append("speakerWidget:$speakerWidget").append("\n")
                append("\n")
                append("floatingWindowWidget:$floatingWindowWidget").append("\n")
                append("\n")

            }
            if (configInterfaceWidgetList == null) {
                return@run
            }
            for ((index, configWidget) in configInterfaceWidgetList.withIndex()) {
                payloadOtherWidgetInfo.apply {
                    append("configWidget $index-").append("$configWidget").append("\n")
                }
            }
            tv_payload_other_widget_info.text = payloadOtherWidgetInfo.toString()

        }

    }

    private fun initTitle() {
        tv_payload_title.text = "This is ${payloadIndexType.name.lowercase()} payloadManager info page!"
    }

    private fun initWidgetBasicInfo() {
        payloadVM.initListener(payloadIndexType)
        payloadVM.payloadBasicInfo.observe(viewLifecycleOwner) {
            it.apply {
                payloadBasicInfo.apply {
                    clear()
                    append("\n").append("PayloadBasicInfo:").append("\n")
                    append("payloadProductName:$payloadProductName").append("\n")
                    append("payloadType:$payloadType").append("\n")
                    append("isConnected:$isConnected").append("\n")
                    append("serialNumber:$serialNumber").append("\n")
                    append("payloadProductPhaseType:$payloadProductPhaseType").append("\n")
                    append("uploadBandwidth:$uploadBandwidth").append("\n")
                    append("isFeatureOpened:$isFeatureOpened").append("\n")
                    append("firmwareVersion:$firmwareVersion").append("\n")

                }
                tv_payload_basic_info.text = payloadBasicInfo.toString()

            }
        }
    }

    private fun initButtonListener() {
        bt_re_pull_widget_info.setOnClickListener {
            payloadVM.pullWidgetInfo()
        }
        bt_set_widget_value.setOnClickListener {
            val widgetValue = WidgetValue()
            showDialog("Please input Widget Value", widgetValue.toString()) {
                it?.let {
                    val value = it.trim()
                    val fromJson = WidgetValue.fromJson(value)
                    if (fromJson == null) {
                        ToastUtils.showToast("WidgetValue fromJson fail!")
                        return@showDialog
                    }
                    payloadVM.setWidgetValue(fromJson)
                }
            }
        }
    }
}