package dji.sampleV5.aircraft

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dji.sampleV5.aircraft.data.DJIToastResult

/**
 * Description :TestingToolsActivity对应的ViewModel，主要用于创建djiToastResult,用于统一发送和观察需要Toast的内容
 *
 * @author: Byte.Cai
 *  date : 2022/6/16
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class TestToolsVM : ViewModel() {
    val djiToastResult = MutableLiveData<DJIToastResult>()

}