package com.dji.open.pilot.plugin.sampleitem.pipeline

import android.content.Context
import android.os.Bundle
import android.util.Pair
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.graphics.Color
import android.widget.AbsListView
import android.widget.TextView
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.dji.open.pilot.plugin.R
import com.dji.open.pilot.plugin.common.BaseDemoActivity
import com.dji.open.pilot.plugin.common.globalViewModels
import com.dji.open.pilot.plugin.databinding.ActivityPipelineDemoBinding
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.mop.PipelineDeviceType
import dji.sdk.keyvalue.value.mop.TransmissionControlType
import dji.v5.manager.mop.Pipeline

/**
 * MOP 演示（迁移自 android-sdk-v5-sample 的 MopInterfaceFragment + MopDownFragment，整合为单页）。
 * 上半部分：连接 + 文本收发；下半部分：连接 + 多管道文件上传/下载列表。
 */
class PipelineDemoActivity : BaseDemoActivity() {

    private lateinit var binding: ActivityPipelineDemoBinding
    private val viewModel: PipelineDemoViewModel by globalViewModels()

    private val messageList = ArrayList<String>()
    private lateinit var messageAdapter: ArrayAdapter<String>
    private var adapter: PipelineItemAdapter? = null

    // 收发区选中的组件索引
    private var interfaceIndex: ComponentIndexType = ComponentIndexType.LEFT_OR_MAIN

    // 上传下载区选中的组件索引
    private var downIndex: ComponentIndexType = ComponentIndexType.LEFT_OR_MAIN

    // 回车收起软键盘（与原 MopInterfaceFragment 行为一致）
    private val onKeyListener = View.OnKeyListener { v, keyCode, event ->
        if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
            val inputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (inputMethodManager.isActive) {
                inputMethodManager.hideSoftInputFromWindow(v.applicationWindowToken, 0)
            }
            true
        } else {
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPipelineDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initListener()
    }

    private fun initView() {
        // 消息列表
        messageAdapter = ArrayAdapter(this, R.layout.item_message_list, messageList)
        binding.messageListview.adapter = messageAdapter
        binding.edData.setOnKeyListener(onKeyListener)

        // 上传下载 RecyclerView
        adapter = PipelineItemAdapter(this, ArrayList<Pair<ComponentIndexType, Pipeline>>())
        binding.rcPipeline.layoutManager = LinearLayoutManager(this)
        binding.rcPipeline.adapter = adapter
        binding.rcPipeline.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        // 自定义 Spinner 适配器（无提示词，纯净枚举选项，保留 Antd 选中高亮态）
        val components = resources.getStringArray(R.array.component_index_arrays)
        
        val spinnerAdapter = object : ArrayAdapter<String>(this, R.layout.item_spinner_antd, components) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.text = components[position]
                view.setTextColor(Color.parseColor("#333333"))
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                val listView = parent as? AdapterView<*>
                val isSelected = listView?.selectedItemPosition == position

                view.layoutParams = AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                val density = context.resources.displayMetrics.density
                val paddingH = (12 * density).toInt()
                val paddingV = (8 * density).toInt()
                view.setPadding(paddingH, paddingV, paddingH, paddingV)

                // 还原 Antd 选中态
                if (isSelected) {
                    view.setBackgroundColor(Color.parseColor("#E6F7FF"))
                    view.setTextColor(Color.parseColor("#1890FF"))
                } else {
                    view.setBackgroundResource(R.drawable.bg_spinner_item_selector)
                    view.setTextColor(Color.parseColor("#333333"))
                }
                
                return view
            }
        }
        spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown_antd)

        binding.spChooseComponent.adapter = spinnerAdapter
        binding.spChooseComponentDown.adapter = spinnerAdapter

        // 收发区组件下拉
        binding.spChooseComponent.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                interfaceIndex = ComponentIndexType.valueOf(parent?.getItemAtPosition(index).toString())
            }

            override fun onNothingSelected(p0: AdapterView<*>?) = Unit
        }
        // 上传下载区组件下拉
        binding.spChooseComponentDown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                downIndex = ComponentIndexType.valueOf(parent?.getItemAtPosition(index).toString())
            }

            override fun onNothingSelected(p0: AdapterView<*>?) = Unit
        }
    }

    private fun initListener() {
        viewModel.initListener()

        binding.tvBack.setOnClickListener { finish() }

        // ---- 收发区 ----
        binding.btnConnect.setOnClickListener {
            val deviceType = getType(binding.rgMopType.checkedRadioButtonId)
            val transferType = if (binding.cbReliable.isChecked) TransmissionControlType.STABLE else TransmissionControlType.UNRELIABLE
            val id = binding.etChannelId.text.toString().trim().toInt()
            viewModel.connect(interfaceIndex, id, deviceType, transferType)
        }
        binding.btnDisconnect.setOnClickListener { viewModel.stopInterface() }
        binding.btnSendDataToPayload.setOnClickListener {
            viewModel.sendData(binding.edData.text.toString().toByteArray())
        }

        // ---- 上传下载区 ----
        binding.tvConnect.setOnClickListener {
            val deviceType = getType(binding.rgMopTypeDown.checkedRadioButtonId)
            val transferType = if (binding.cbReliableDown.isChecked) TransmissionControlType.STABLE else TransmissionControlType.UNRELIABLE
            val id = binding.etChannelIdDown.text.toString().trim().toInt()
            viewModel.connect(downIndex, id, deviceType, transferType, true)
        }

        // ---- 观察 ----
        viewModel.receiveMessageLiveData.observe(this) { t ->
            messageList.add(t)
            messageAdapter.notifyDataSetChanged()
            binding.messageListview.setSelection(messageList.size - 1)
        }
        viewModel.pipelineMapLivData.observe(this) { map ->
            map.forEach { entry -> adapter?.addItem(downIndex, entry.value) }
        }
        viewModel.toastMessage.observe(this) { msg ->
            if (msg.isNullOrEmpty()) return@observe
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    private fun getType(checkedRadioButtonId: Int): PipelineDeviceType = when (checkedRadioButtonId) {
        R.id.rb_on_board -> PipelineDeviceType.ONBOARD
        R.id.rb_payload -> PipelineDeviceType.PAYLOAD
        else -> PipelineDeviceType.PAYLOAD
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopMop()
    }
}
