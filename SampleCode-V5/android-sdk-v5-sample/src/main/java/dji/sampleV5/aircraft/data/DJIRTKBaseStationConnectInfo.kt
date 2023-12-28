package dji.sampleV5.aircraft.data

import dji.sdk.keyvalue.value.rtkbasestation.RTKStationInfo
import dji.sdk.keyvalue.value.rtkbasestation.RTKStationConnetState

/**
 * Description :驱动UI的数据模型，基于RTKBaseStationConnectInfo新增一个连接状态属性
 *
 * @author: Byte.Cai
 *  date : 2022/3/6
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class DJIRTKBaseStationConnectInfo(
    var baseStationId: Int,
    var signalLevel: Int,
    var rtkStationName: String,
    var connectStatus: RTKStationConnetState = RTKStationConnetState.IDLE
) : RTKStationInfo(baseStationId, signalLevel, rtkStationName) {
    constructor() : this(0, 0, "")
}