package dji.sampleV5.aircraft.models

import androidx.fragment.app.FragmentActivity
import dji.v5.common.callback.CommonCallbacks
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
    private val userAccountManager = UserAccountManager.getInstance()


    fun loginAccount(fragmentActivity: FragmentActivity, callbacks: CommonCallbacks.CompletionCallback) {
        userAccountManager.logInDJIUserAccount(fragmentActivity, false, callbacks)
    }

    fun logoutAccount(callbacks: CommonCallbacks.CompletionCallback) {
        userAccountManager.logOutDJIUserAccount(callbacks)
    }

    fun addLoginStateChangeListener(loginInfoUpdateListener: LoginInfoUpdateListener) {
        userAccountManager.addLoginInfoUpdateListener(loginInfoUpdateListener)
    }

    fun clearAllLoginStateChangeListener() {
        userAccountManager.clearAllLoginInfoUpdateListener()
    }

    fun getLoginInfo(): LoginInfo {
        return userAccountManager.loginInfo
    }

    fun userLogin(
        userName: String,
        password: String,
        verificationCode: String? = null,
        callbacks: CommonCallbacks.CompletionCallback,
    ) {
        userAccountManager.logInDJIUserAccount(userName, password, verificationCode, callbacks)
    }

    fun getVerificationCodeImageURL(): String {
        return userAccountManager.verificationCodeImageURL
    }


}