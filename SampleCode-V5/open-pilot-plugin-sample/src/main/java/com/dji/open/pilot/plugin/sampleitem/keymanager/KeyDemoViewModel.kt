package com.dji.open.pilot.plugin.sampleitem.keymanager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dji.sdk.keyvalue.key.ProductKey
import dji.v5.et.create
import dji.v5.et.listen
import dji.v5.manager.KeyManager

/**
 * 承接 KeyManager demo 的机型监听生命周期，替代原 sample 的 MSDKInfoVm。
 */
class KeyDemoViewModel : ViewModel() {

    private val _productTypeName = MutableLiveData<String?>()
    val productTypeName: LiveData<String?> = _productTypeName

    init {
        ProductKey.KeyProductType.create().listen(this) {
            _productTypeName.postValue(it?.name)
        }
    }

    fun getProductTypeName(): String? = _productTypeName.value

    override fun onCleared() {
        KeyManager.getInstance().cancelListen(this)
    }
}
