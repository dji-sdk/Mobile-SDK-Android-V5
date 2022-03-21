package dji.sampleV5.moduleaircraft.data

/**
 * Description :面向Model使用的基础类
 *
 * @author: Byte.Cai
 *  date : 2022/2/22
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class DJIBaseResult<T>(var isSuccess: Boolean, var msg: String, var data: T? = null) {

    companion object {
        fun <T> success(t: T?=null): DJIBaseResult<T> {
            return DJIBaseResult(true, "Ok", t)
        }

        fun <T> failed(errMsg:String): DJIBaseResult<T> {
            return DJIBaseResult(false, errMsg, null)
        }
    }
}

