package dji.sampleV5.modulecommon.models

import android.content.Context
import androidx.lifecycle.MutableLiveData
import dji.sampleV5.modulecommon.data.FragmentPageInfo
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.ldm.LDMExemptModule
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback
import dji.v5.manager.ldm.LDMManager

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/5/10
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class MSDKCommonOperateVm : DJIViewModel() {

    val mainPageInfoList = MutableLiveData<LinkedHashSet<FragmentPageInfo>>()

    fun loaderItem(itemList: LinkedHashSet<FragmentPageInfo>) {
        if (mainPageInfoList.value == null) {
            mainPageInfoList.value = LinkedHashSet<FragmentPageInfo>()
        }
        mainPageInfoList.value?.addAll(itemList)
        mainPageInfoList.postValue(mainPageInfoList.value)
    }

    fun initMSDK(context: Context, callback: SDKManagerCallback) {
        SDKManager.getInstance().init(context, callback)
    }

    fun unInitSDK(){
        SDKManager.getInstance().destroy();
    }

    fun registerApp() {
        SDKManager.getInstance().registerApp()
    }

    fun enableLDM(context: Context, callback: CommonCallbacks.CompletionCallback) {
        LDMManager.getInstance().enableLDM(context, callback, LDMExemptModule.MSDK_INIT_AND_REGISTRATION)
    }

    fun disableLDM(callback: CommonCallbacks.CompletionCallback) {
        LDMManager.getInstance().disableLDM(callback);
    }
}