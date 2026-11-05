package dji.sampleV5.aircraft.models

import androidx.lifecycle.ViewModel
import dji.open.pilot.base.ui.NavigateAction
import dji.open.pilot.client.manager.ui.NavigateManager
import dji.sampleV5.aircraft.util.DJIToastUtil
import dji.v5.ux.util.ToastUtils
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.utils.common.LogUtils

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/7/5
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class DJIViewModel : ViewModel() {
    val toastResult
        get() = DJIToastUtil.dJIToastLD

    val logTag = LogUtils.getTag(this)

    open fun jumpTo(action: NavigateAction) {
        NavigateManager.getInstance().navigateToPage(action,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast("Navigate To $action success")
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast("Navigate To $action :$error")
                }
            }
        )
    }

    open fun notifyUser(msg: String) {
        ToastUtils.showToast(msg)
    }
}