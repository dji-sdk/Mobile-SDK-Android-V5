package dji.sampleV5.modulecommon.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dji.sampleV5.modulecommon.data.*
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.ProductKey
import dji.v5.common.error.DJINetworkError
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.et.listen
import dji.v5.manager.KeyManager
import dji.v5.manager.SDKManager
import dji.v5.manager.areacode.AreaCodeChangeListener
import dji.v5.manager.areacode.AreaCodeManager
import dji.v5.manager.ldm.LDMManager
import dji.v5.network.DJINetworkManager
import dji.v5.network.IDJINetworkStatusListener
import dji.v5.utils.common.LogUtils
import dji.v5.utils.inner.SDKConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 提供SDKInfo View相关接口，操作MSDKInfoModel相关功能
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class MSDKInfoVm : DJIViewModel() {

    val tag: String = LogUtils.getTag(this)

    val msdkInfo = MutableLiveData<MSDKInfo>()
    val mainTitle = MutableLiveData<String>()
    private val isInited = AtomicBoolean(false)
    private val msdkInfoModel: MSDKInfoModel = MSDKInfoModel()
    private var areaCodeChangeListener: AreaCodeChangeListener
    private var netWorkStatusListener: IDJINetworkStatusListener

    init {
        msdkInfo.value = MSDKInfo(msdkInfoModel.getSDKVersion())
        msdkInfo.value?.buildVer = msdkInfoModel.getBuildVersion()
        msdkInfo.value?.isDebug = msdkInfoModel.isDebug()
        msdkInfo.value?.packageProductCategory = msdkInfoModel.getPackageProductCategory()
        msdkInfo.value?.isLDMEnabled = LDMManager.getInstance().isLDMEnabled.toString()
        msdkInfo.value?.isLDMLicenseLoaded = LDMManager.getInstance().isLDMLicenseLoaded.toString()
        msdkInfo.value?.coreInfo = msdkInfoModel.getCoreInfo()

        areaCodeChangeListener = AreaCodeChangeListener { _, changed ->
            LogUtils.i(logTag, "areaCodeData", changed)
            msdkInfo.value?.countryCode = if (changed == null) DEFAULT_STR else changed.areaCode
            refreshMSDKInfo()
        }
        netWorkStatusListener = IDJINetworkStatusListener {
            LogUtils.i(logTag, "isNetworkAvailable", it)
            updateNetworkInfo(it)
            refreshMSDKInfo()
        }

        refreshMSDKInfo()
    }

    override fun onCleared() {
        removeListener()
    }

    fun refreshMSDKInfo() {
        msdkInfo.postValue(msdkInfo.value)
    }

    /**
     * 需要在register成功后，再调用
     */
    fun initListener() {
        if (!SDKManager.getInstance().isRegistered) {
            return
        }
        if (isInited.getAndSet(true)) {
            return
        }
        FlightControllerKey.KeyConnection.create().listen(this) {
            LogUtils.i(tag, "KeyConnection:$it")
            updateFirmwareVersion()
        }

        AreaCodeManager.getInstance().addAreaCodeChangeListener(areaCodeChangeListener)
        DJINetworkManager.getInstance().addNetworkStatusListener(netWorkStatusListener)
        ProductKey.KeyProductType.create().listen(this) {
            LogUtils.i(tag, "KeyProductType:$it")
            it?.let {
                msdkInfo.value?.productType = it
                refreshMSDKInfo()
            }
        }
    }

    private fun removeListener() {
        KeyManager.getInstance().cancelListen(this)
        AreaCodeManager.getInstance().removeAreaCodeChangeListener(areaCodeChangeListener)
        DJINetworkManager.getInstance().removeNetworkStatusListener(netWorkStatusListener)
    }

    private fun updateNetworkInfo(isAvailable: Boolean) {
        msdkInfo.value?.networkInfo = if(isAvailable) ONLINE_STR else NO_NETWORK_STR
//        viewModelScope.launch {
//            var isInInnerNetwork: Boolean
//            withContext(Dispatchers.IO) {
//                isInInnerNetwork = SDKConfig.getInstance().isInInnerNetwork
//            }
//            msdkInfo.value?.networkInfo =
//                if (isInInnerNetwork) IN_INNER_NETWORK_STR else IN_OUT_NETWORK_STR
//        }
    }

    private fun updateFirmwareVersion() {
        ProductKey.KeyFirmwareVersion.create().get({
            LogUtils.i(tag, "updateFirmwareVersion onSuccess:$it")
            msdkInfo.value?.firmwareVer = it ?: DEFAULT_STR
            refreshMSDKInfo()
        }) {
            LogUtils.i(tag, "updateFirmwareVersion onFailure:$it")
            msdkInfo.value?.firmwareVer = DEFAULT_STR
            refreshMSDKInfo()
        }
    }

    fun updateLDMStatus() {
        msdkInfo.value?.isLDMEnabled = LDMManager.getInstance().isLDMEnabled.toString()
        msdkInfo.value?.isLDMLicenseLoaded = LDMManager.getInstance().isLDMLicenseLoaded.toString()
        refreshMSDKInfo()
    }
}
