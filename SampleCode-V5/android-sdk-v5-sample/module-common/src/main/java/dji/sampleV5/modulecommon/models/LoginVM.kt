package dji.sampleV5.modulecommon.models

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import dji.sampleV5.modulecommon.data.DJIBaseResult
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.account.LoginInfo
import dji.v5.manager.account.LoginInfoUpdateListener
import dji.v5.manager.account.UserAccountManager

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2022/4/24
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class LoginVM : DJIViewModel() {
    val loginLD = MutableLiveData<DJIBaseResult<Boolean>>()
    val logoutLD = MutableLiveData<DJIBaseResult<Boolean>>()
    private val userAccountManager = UserAccountManager.getInstance()


    fun loginAccount(fragmentActivity: FragmentActivity) {
        userAccountManager.logInDJIUserAccount(fragmentActivity, false, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                loginLD.postValue(DJIBaseResult.success(true))

            }

            override fun onFailure(error: IDJIError) {
                loginLD.postValue(DJIBaseResult.failed(error.toString()))

            }

        })
    }

    fun logoutAccount() {
        userAccountManager.logOutDJIUserAccount(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                logoutLD.postValue(DJIBaseResult.success(true))
            }

            override fun onFailure(error: IDJIError) {
                logoutLD.postValue(DJIBaseResult.failed(error.toString()))

            }
        })
    }

    fun addLoginStateChangeListener(loginInfoUpdateListener: LoginInfoUpdateListener) {
        userAccountManager.addLoginInfoUpdateListener(loginInfoUpdateListener)
    }

    fun clearAllLoginStateChangeListener() {
        userAccountManager.clearAllLoginInfoChangeListener()
    }

    fun getLoginInfo(): LoginInfo {
        return userAccountManager.loginInfo
    }


}