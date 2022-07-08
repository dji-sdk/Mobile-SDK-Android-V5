package dji.sampleV5.modulecommon.pages

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dji.sampleV5.modulecommon.R
import dji.sampleV5.modulecommon.models.LoginVM
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.account.LoginInfo
import dji.v5.utils.common.LogUtils
import kotlinx.android.synthetic.main.frag_login_account_page.*

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2022/4/24
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class LoginFragment : DJIFragment() {
    private val loginVM: LoginVM by viewModels()
    private val TAG = LogUtils.getTag("LoginFragment")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_login_account_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListener()
    }

    private fun initListener() {
        //尝试获取已有的登录信息
        showLoginInfo()
        loginVM.addLoginStateChangeListener {
            activity?.runOnUiThread {
                it?.run {
                    tv_login_account_info.text = if (TextUtils.isEmpty(account)) {
                        "UNKNOWN"
                    } else {
                        account
                    }
                    tv_login_state_info.text = loginState.name
                }
            }
        }
        btn_login.setOnClickListener {
            loginVM.loginAccount(requireActivity(), object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("Login Success!")
                    clearErrMsg()
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast(error.toString())
                    showErrMsg(error.toString())
                }

            })
        }
        btn_logout.setOnClickListener {
            loginVM.logoutAccount(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("Logout Success!")
                    clearErrMsg()
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast(error.toString())
                    showErrMsg(error.toString())
                }

            })
        }
        btn_get_login_info.setOnClickListener {
            val loginInfo=showLoginInfo()
            //同时将获取到的结果Toast出来
            ToastUtils.showToast(tv_login_account_info.text.toString() + "-" + loginInfo.loginState.name)
        }

    }

    private fun showLoginInfo():LoginInfo {
        //将获取到的结果刷新到UI
        val loginInfo = loginVM.getLoginInfo()
        tv_login_account_info.text = if (TextUtils.isEmpty(loginInfo.account)) {
            "UNKNOWN"
        } else {
            loginInfo.account
        }
        tv_login_state_info.text = loginInfo.loginState.name
        return loginInfo
    }


    override fun onDestroy() {
        super.onDestroy()
        loginVM.clearAllLoginStateChangeListener()
    }

    private fun clearErrMsg() {
        activity?.runOnUiThread {
            tv_login_error_info.text = ""
        }

    }

    private fun showErrMsg(msg: String) {
        activity?.runOnUiThread {
            tv_login_error_info.text = msg

        }
    }

}