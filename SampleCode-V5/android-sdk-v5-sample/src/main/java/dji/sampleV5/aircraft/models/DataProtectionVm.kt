package dji.sampleV5.aircraft.models

import dji.v5.ux.util.ToastUtils
import dji.v5.manager.dataprotect.DataProtectionManager
import dji.v5.utils.common.DJIExecutor
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.LogUtils.OnExportLogProgressCallback

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/6/30
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class DataProtectionVm : DJIViewModel() {

    fun agreeToProductImprovement(isAgree: Boolean) {
        DataProtectionManager.getInstance().agreeToProductImprovement(isAgree)
    }

    fun isAgreeToProductImprovement(): Boolean {
        return DataProtectionManager.getInstance().isAgreeToProductImprovement
    }

    fun enableLog(enable: Boolean) {
        DataProtectionManager.getInstance().enableMSDKLog(enable)
    }

    fun isLogEnable(): Boolean {
        return DataProtectionManager.getInstance().isMSDKLogEnabled
    }

    fun logPath(): String {
        return DataProtectionManager.getInstance().msdkLogPath
    }

    fun clearLog(): Boolean {
        return DataProtectionManager.getInstance().clearMSDKLog()
    }

    fun zipAndExportLog() {
        DJIExecutor.getExecutorFor(DJIExecutor.Purpose.IO).execute {
            LogUtils.zipAndExportLog("logs", object : OnExportLogProgressCallback {
                override fun onExportBegin() {
                    ToastUtils.showToast("ZipAndExportLog begin")
                }

                override fun onExportProgress(progress: Int) {
                    ToastUtils.showToast("ZipAndExportLog Progress:$progress")
                }

                override fun onExportFailed(result: String?) {
                    ToastUtils.showToast("ZipAndExportLog Failed:$result")
                }

                override fun onExportSuccess(filePath: String?) {
                    ToastUtils.showToast("ZipAndExportLog Success:$filePath")
                }
            })
        }
    }
}