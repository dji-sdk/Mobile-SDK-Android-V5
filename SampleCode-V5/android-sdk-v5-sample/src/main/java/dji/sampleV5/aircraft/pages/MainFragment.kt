package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.data.FragmentPageItem
import dji.sampleV5.aircraft.data.FragmentPageItemList
import dji.sampleV5.aircraft.data.MAIN_FRAGMENT_PAGE_TITLE
import dji.sampleV5.aircraft.models.MSDKCommonOperateVm
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.ldm.LDMExemptModule
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.StringUtils
import kotlinx.android.synthetic.main.frag_main_page.*
import dji.sampleV5.aircraft.util.ToastUtils

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/x5/8lp
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class MainFragment : DJIFragment() {

    //默认勾选了MSDK_INIT_AND_REGISTRATION
    private val checkItem = BooleanArray(LDMExemptModule.values().size) {
        it == 1
    }

    private val selectLDMExemptModule = arrayListOf(LDMExemptModule.MSDK_INIT_AND_REGISTRATION)

    private val msdkCommonOperateVm: MSDKCommonOperateVm by activityViewModels()

    private var ldmCharArray: Array<CharSequence?>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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
                val itemSet = LinkedHashSet<FragmentPageItem>()
                for (itemList: FragmentPageItemList in it) {
                    itemSet.addAll(itemList.items)
                }
                adapter.submitList(itemSet.toList())
            }
        }
    }

    private fun adapterOnItemClick(item: FragmentPageItem) {
        view?.let {
            Navigation.findNavController(it)
                .navigate(item.id, bundleOf(MAIN_FRAGMENT_PAGE_TITLE to item.title))
        }
    }

    private fun initBtnClickListener() {
        enableLDM_btn.setOnClickListener {
            showLDMExemptMultipleChoiceDialog()
        }
        disableLDM_btn.setOnClickListener {
            msdkCommonOperateVm.disableLDM(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    msdkInfoVm.updateLDMStatus()
                    ToastUtils.showToast("LDM disabled success");
                }

                override fun onFailure(error: IDJIError) {
                    msdkInfoVm.updateLDMStatus()
                    ToastUtils.showToast("LDM disabled failed:$error");
                }
            })
        }
    }

    private var ldmMultipleChoiceDialog: AlertDialog? = null
    private fun showLDMExemptMultipleChoiceDialog() {
        if (ldmCharArray == null) {
            ldmCharArray = getLDMCharArray()
        }
        if (ldmMultipleChoiceDialog == null) {
            ldmCharArray?.let {
                ldmMultipleChoiceDialog = AlertDialog.Builder(requireContext())
                    .setTitle(R.string.tip_ldn_choice_title)
                    .setMultiChoiceItems(ldmCharArray, checkItem) { _, which, isChecked ->
                        if (isChecked) {
                            selectLDMExemptModule.add(LDMExemptModule.find(it[which].toString()))
                        } else {
                            selectLDMExemptModule.remove(LDMExemptModule.find(it[which].toString()))
                        }
                        for (i in selectLDMExemptModule.indices) {
                            LogUtils.d(tag, "selectLDMExemptModule[$i]=" + selectLDMExemptModule[i])
                        }
                    }
                    .setPositiveButton(R.string.tip_ldn_choice_confirm) { dialog, _ ->
                        enableLDM()
                        dialog?.dismiss()
                    }
                    .setNegativeButton(R.string.tip_ldn_choice_cancel) { dialog, _ -> dialog?.dismiss() }
                    .create()

            }

        }
        if (isFragmentShow() && ldmMultipleChoiceDialog?.isShowing == false) {
            ldmMultipleChoiceDialog?.show()
        }
    }


    private fun enableLDM() {
        val ldmArray = arrayOfNulls<LDMExemptModule>(selectLDMExemptModule.size)
        for (i in selectLDMExemptModule.indices) {
            ldmArray[i] = selectLDMExemptModule[i]
        }

        msdkCommonOperateVm.enableLDM(requireContext(), object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                msdkInfoVm.updateLDMStatus()
                ToastUtils.showToast("LDM enabled success");
            }

            override fun onFailure(error: IDJIError) {
                msdkInfoVm.updateLDMStatus()
                ToastUtils.showToast("LDM enabled failed:$error");
            }

        }, ldmArray)
    }

    private fun getLDMCharArray(): Array<CharSequence?> {
        val ldmExemptModules = LDMExemptModule.values()
        val lDMCharArray = arrayOfNulls<CharSequence>(ldmExemptModules.size)
        for (i in ldmExemptModules.indices) {
            lDMCharArray[i] = ldmExemptModules[i].name
        }
        return lDMCharArray
    }

}