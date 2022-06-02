package dji.sampleV5.modulecommon.models

import android.text.Html
import androidx.lifecycle.MutableLiveData
import dji.v5.utils.common.ContextUtil
import dji.v5.utils.common.DiskUtil
import dji.v5.utils.common.FileUtils
import java.io.File
import java.lang.StringBuilder
import java.util.*

/**
 * ClassName : MSDKLogVM
 * Description : Log展示
 * Author : daniel.chen
 * CreateDate : 2022/5/7 12:17 下午
 * Copyright : ©2022 DJI All Rights Reserved.
 */
class MSDKLogVM:DJIViewModel(){
    val logInfo = MutableLiveData<String>()
    val logCount = MutableLiveData<Int>()

    init {
        logInfo.value = loadLatestLog()
        logCount.value = 0
    }

    fun updateLogInfo(){
        logInfo.value = loadLatestLog()
    }

    private fun loadLatestLog(): String {
        val logDir = DiskUtil.getExternalCacheDirPath(ContextUtil.getContext(), "/LOG/CRASH/")
        val file = File(logDir)
        val list = FileUtils.getAllFile(file)
        if (list.size >= 1) {
            Collections.sort(list, object : Comparator<File> {
                override fun compare(o1: File, o2: File): Int {
                    val diff = o1.lastModified() - o2.lastModified()
                    return -diff.toInt()
                }
            })
            var index = 0
            val stringBuilder = StringBuilder()
            for (file in list) {
                stringBuilder.append("-----------------Crash Info: ${index++}----------------------")
                stringBuilder.append("\n")
                stringBuilder.append(file.absolutePath)
                stringBuilder.append("\n")
                stringBuilder.append(FileUtils.readFile(file.absolutePath,"\n"))
                stringBuilder.append("\n")
                stringBuilder.append("\n")
            }
            logCount.value = index
            return stringBuilder.toString()
        }
        return "N/A"
    }
}