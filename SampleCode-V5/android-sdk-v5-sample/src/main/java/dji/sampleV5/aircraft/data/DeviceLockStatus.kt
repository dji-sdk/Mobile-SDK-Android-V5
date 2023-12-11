package dji.sampleV5.aircraft.data

import dji.sdk.keyvalue.value.flightcontroller.AccessLockerDeviceType

/**
 * Description :设备的锁状态
 *
 * @author: Byte.Cai
 *  date : 2022/8/10
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */

/**
 * 设备的锁状态
 */
data class DeviceLockStatus(
    var deviceIndex: AccessLockerDeviceType = AccessLockerDeviceType.UNKNOWN,
    //功能是否支持
    var isFeatureSupported: Boolean = false,
    //功能是否开启
    var isFeatureEnabled: Boolean = false,
    //功能是否需要验证
    var isFeatureNeedToBeVerified: Boolean = false,
)

/**
 * 修改密码数据类
 */
data class ModifyPasswordBean(val currentPassword: String, val newPassword: String){
}