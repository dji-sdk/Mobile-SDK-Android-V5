package dji.sampleV5.modulecommon.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import dji.sampleV5.modulecommon.MainFragmentListAdapter
import dji.sampleV5.modulecommon.R
import dji.sampleV5.modulecommon.data.FragmentPageInfo
import dji.sampleV5.modulecommon.data.FragmentPageInfoItem
import dji.sampleV5.modulecommon.data.MAIN_FRAGMENT_PAGE_TITLE
import dji.sampleV5.modulecommon.models.MSDKCommonOperateVm
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.interfaces.SDKManagerCallback
import dji.v5.utils.common.ContextUtil
import dji.v5.utils.common.StringUtils
import kotlinx.android.synthetic.main.frag_main_page.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/x5/8lp
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class MainFragment : DJIFragment() {

    private val msdkCommonOperateVm: MSDKCommonOperateVm by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_main_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initMainList()
        initBtnClickListener()
        msdkInfoVm.initListener()
    }

    override fun updateTitle() {
        msdkInfoVm.mainTitle.value = StringUtils.getResStr(context, R.string.testing_tools)
    }

    private fun initMainList() {
        val adapter = MainFragmentListAdapter { item -> adapterOnItemClick(item) }
        view_list.adapter = adapter
        view_list.layoutManager = LinearLayoutManager(context)
        msdkCommonOperateVm.mainPageInfoList.observe(viewLifecycleOwner) {
            it?.let {
                val itemList = LinkedHashSet<FragmentPageInfoItem>()
                for (info: FragmentPageInfo in it) {
                    itemList.addAll(info.items)
                    addDestination(info.vavGraphId)
                }
                adapter.submitList(itemList.toList())
            }
        }
    }

    private fun addDestination(id: Int) {
        view?.let {
            val v = Navigation.findNavController(it).navInflater.inflate(id)
            Navigation.findNavController(it).graph.addAll(v)
        }
    }

    private fun adapterOnItemClick(item: FragmentPageInfoItem) {
        view?.let {
            Navigation.findNavController(it)
                .navigate(item.id, bundleOf(MAIN_FRAGMENT_PAGE_TITLE to item.title))
        }
    }

    private fun initBtnClickListener() {
        init_msdk_btn.setOnClickListener {
            initMSDK()
        }
        unInit_msdk_btn.setOnClickListener {
            unInitSDK()
        }
        register_app_btn.setOnClickListener {
            msdkCommonOperateVm.registerApp()
        }
        enableLDM_btn.setOnClickListener {
            msdkCommonOperateVm.enableLDM(requireContext(), object:CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    msdkInfoVm.updateLDMStatus()
                    ToastUtils.showToast(ContextUtil.getContext(),"LDM enabled success");
                }

                override fun onFailure(error: IDJIError) {
                    msdkInfoVm.updateLDMStatus()
                    ToastUtils.showToast(ContextUtil.getContext(), "LDM enabled failed:$error");
                }

            })
        }
        disableLDM_btn.setOnClickListener {
            msdkCommonOperateVm.disableLDM(object:CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    msdkInfoVm.updateLDMStatus()
                    ToastUtils.showToast(ContextUtil.getContext(),"LDM disabled success");
                }

                override fun onFailure(error: IDJIError) {
                    msdkInfoVm.updateLDMStatus()
                    ToastUtils.showToast(ContextUtil.getContext(), "LDM disabled failed:$error");
                }

            })
        }

    }

    private fun initMSDK() {
        context?.let {
            msdkCommonOperateVm.initMSDK(it, object :
                SDKManagerCallback {
                override fun onRegisterSuccess() {
                    ToastUtils.showToast(it,"Register Success!!!")
                    msdkInfoVm.initListener()
                    MediaDataCenter.getInstance().videoStreamManager
                }

                override fun onRegisterFailure(error: IDJIError?) {
                    ToastUtils.showToast(it,"Register Failure: (errorCode: ${error?.errorCode()}, description: ${error?.description()})")
                }

                override fun onProductDisconnect(product: Int) {
                    ToastUtils.showToast(it,"Product: $product Disconnect")
                }

                override fun onProductConnect(product: Int) {
                    ToastUtils.showToast(it,"Product: $product Connect")
                }

                override fun onProductChanged(product: Int) {
                    ToastUtils.showToast(it,"Product: $product Changed")
                }

                override fun onInitProcess(event: DJISDKInitEvent?, totalProcess: Int) {
                    ToastUtils.showToast(it,"Init Process event: ${event?.name}")
                }

                override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                    ToastUtils.showToast(it,"Database Download Progress current: $current, total: $total")
                }
            })
        }
    }

    private fun unInitSDK() {
        msdkCommonOperateVm.unInitSDK()
        ToastUtils.showToast(ContextUtil.getContext(), "uninit sdk success")
    }
}