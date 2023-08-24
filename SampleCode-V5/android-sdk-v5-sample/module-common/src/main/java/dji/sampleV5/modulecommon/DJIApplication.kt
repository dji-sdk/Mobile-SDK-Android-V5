package dji.sampleV5.modulecommon

import android.app.Application
import com.tencent.bugly.crashreport.CrashReport
import dji.sampleV5.modulecommon.models.MSDKManagerVM
import dji.sampleV5.modulecommon.models.globalViewModels
import dji.v5.manager.SDKManager
import dji.v5.utils.common.DeviceInfoUtil
import dji.v5.utils.inner.SDKConfig

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/3/1
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
open class DJIApplication : Application() {

    private val msdkManagerVM: MSDKManagerVM by globalViewModels()

    override fun onCreate() {
        super.onCreate()

        // Ensure initialization is called first
        msdkManagerVM.initMobileSDK(this)

        CrashReport.UserStrategy(this).apply {
            appPackageName = this@DJIApplication.packageName
            appChannel = SDKManager.getInstance().productCategory.name
            appVersion = SDKManager.getInstance().sdkVersion
            if ("LOCAL" == SDKConfig.getInstance().buildVersion) {
                appVersion = appVersion + "_" + "LOCAL"
            }
            deviceID = SDKConfig.getInstance().deviceId
            deviceModel = DeviceInfoUtil.getDeviceModel()
            isEnableANRCrashMonitor = true
            isEnableCatchAnrTrace = true
            CrashReport.setAllThreadStackEnable(this@DJIApplication, true, true)
            CrashReport.initCrashReport(this@DJIApplication, "0014c41605", false, this)
            CrashReport.setUserId(SDKConfig.getInstance().appKey + "_" + deviceID)
        }
    }

}
