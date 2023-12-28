package dji.sampleV5.aircraft.data


/**
 * Description :面向Model使用的执行结果封装类，简单封装了外部需要Toast的信息
 *
 * @author: Byte.Cai
 *  date : 2022/2/22
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class DJIToastResult(var isSuccess: Boolean, var msg: String? = null) {

    companion object {
        fun success(msg: String? = null): DJIToastResult {
            return DJIToastResult(true, "success ${msg ?: ""}")
        }

        fun failed(msg: String): DJIToastResult {
            return DJIToastResult(false, msg)
        }
    }
}



