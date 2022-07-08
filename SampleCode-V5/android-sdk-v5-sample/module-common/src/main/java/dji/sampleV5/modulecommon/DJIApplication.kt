package dji.sampleV5.modulecommon

import android.app.Application
import android.content.Context
import com.tencent.bugly.Bugly
import com.tencent.bugly.crashreport.CrashReport
import com.tencent.bugly.crashreport.CrashReport.setAllThreadStackEnable
import dji.v5.manager.SDKManager
import dji.v5.utils.common.ContextUtil
import dji.v5.utils.common.DeviceInfoUtil
import dji.v5.utils.common.JsonUtil
import dji.v5.utils.common.LogUtils
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

    override fun onCreate() {
        super.onCreate()
        ContextUtil.init(this.applicationContext)
        CrashReport.UserStrategy(this).apply {
            appPackageName = this@DJIApplication.packageName
            appChannel = SDKManager.getInstance().productCategory.name
            appVersion = SDKManager.getInstance().sdkVersion + "_" + SDKConfig.getInstance().buildVersion
            deviceID = SDKConfig.getInstance().deviceId
            deviceModel = DeviceInfoUtil.getDeviceModel()
            isEnableANRCrashMonitor = true
            isEnableCatchAnrTrace = true
            setAllThreadStackEnable(this@DJIApplication, true, true)
            CrashReport.initCrashReport(this@DJIApplication, "0014c41605", false, this)
            CrashReport.setUserId(SDKConfig.getInstance().appKey + "_" + deviceID)
        }
    }
}