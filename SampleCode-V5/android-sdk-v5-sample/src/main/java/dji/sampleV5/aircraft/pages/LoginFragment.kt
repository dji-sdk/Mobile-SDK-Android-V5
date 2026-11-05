package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.databinding.FragLoginAccountPageBinding
import dji.sampleV5.aircraft.models.LoginVM
import dji.v5.ux.util.ToastUtils
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.DJILoginError
import dji.v5.common.error.IDJIError
import dji.v5.manager.account.LoginInfo
import dji.v5.utils.common.JsonUtil
import dji.v5.utils.common.LogUtils
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

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
    private var userLogin: UserLogin = UserLogin()
    private var binding: FragLoginAccountPageBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragLoginAccountPageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListener()
    }

    private fun initListener() {
        //尝试获取已有的登录信息
        showLoginInfo()
        loginVM.addLoginStateChangeListener {
            mainHandler.post {
                it?.run {
                    binding?.tvLoginAccountInfo?.text = if (TextUtils.isEmpty(account)) {
                        "UNKNOWN"
                    } else {
                        account
                    }
                    binding?.tvLoginStateInfo?.text = loginState.name
                }
            }
        }

        binding?.btnLogin?.setOnClickListener {
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

        binding?.btnUserLogin?.setOnClickListener {
            val loginParam = JsonUtil.toJson(userLogin)
            showDialog("Please enter login information ", loginParam.toString()) {
                it?.let {
                    val toBean = JsonUtil.toBean(it.trim(), UserLogin::class.java)
                    userLogin = toBean
                    if (toBean == null) {
                        ToastUtils.showToast("login information error")
                        return@let
                    }
                    userLogin(toBean)

                }
            }
        }

        binding?.btnLogout?.setOnClickListener {
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

        binding?.btnGetLoginInfo?.setOnClickListener {
            val loginInfo = showLoginInfo()
            //同时将获取到的结果Toast出来
            ToastUtils.showToast(binding?.tvLoginAccountInfo?.text.toString() + "-" + loginInfo.loginState.name)
        }

    }

    private fun showLoginInfo(): LoginInfo {
        //将获取到的结果刷新到UI
        val loginInfo = loginVM.getLoginInfo()
        binding?.tvLoginAccountInfo?.text = if (TextUtils.isEmpty(loginInfo.account)) {
            "UNKNOWN"
        } else {
            loginInfo.account
        }
        binding?.tvLoginStateInfo?.text = loginInfo.loginState.name
        return loginInfo
    }


    override fun onDestroy() {
        super.onDestroy()
        loginVM.clearAllLoginStateChangeListener()
    }

    private fun clearErrMsg() {
        AndroidSchedulers.mainThread().scheduleDirect {
            if (isFragmentShow()) {
                mainHandler.post {
                    binding?.tvLoginErrorInfo?.text = ""
                }
                binding?.tvVerificationCodeImage?.visibility = View.GONE
                binding?.ivVerificationCodeImageInfo?.visibility = View.GONE
            }
        }
    }

    private fun showErrMsg(msg: String) {
        if (isFragmentShow()) {
            mainHandler.post {
                binding?.tvLoginErrorInfo?.text = msg
            }

        }
    }


    private fun showVerificationCodeImage() {
        AndroidSchedulers.mainThread().scheduleDirect {
            binding?.tvVerificationCodeImage?.visibility = View.VISIBLE
            binding?.ivVerificationCodeImageInfo?.visibility = View.VISIBLE
            val verificationCodeImageURL = loginVM.getVerificationCodeImageURL()
            LogUtils.d(tag, "verificationCodeImageURL=$verificationCodeImageURL")
            binding?.ivVerificationCodeImageInfo?.let {
                Glide.with(requireActivity())
                    .asBitmap()
                    .load(verificationCodeImageURL)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .fitCenter()
                    .error(R.mipmap.ic_no_connection)
                    .into(it)
            }
        }
    }

    private fun userLogin(userLogin: UserLogin) {
        loginVM.userLogin(
            userLogin.name,
            userLogin.password,
            userLogin.verificationCode,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("Login success!")
                    clearErrMsg()
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast(error.toString())
                    showErrMsg(error.toString())
                    if (error.errorCode().equals(DJILoginError.LOGIN_NEED_VERIFICATION_CODE) || error.errorCode()
                            .equals(DJILoginError.LOGIN_VERIFICATION_CODE_ERROR)
                    ) {
                        showVerificationCodeImage()
                    }
                }

            })
    }


    data class UserLogin(
        var name: String = "",
        var password: String = "",
        var verificationCode: String = "",
    )
}

