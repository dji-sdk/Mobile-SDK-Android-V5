package com.dji.sample.gasdetection.gas.record

import android.util.AtomicFile
import java.io.File

/**
 * 生产队列文件：使用 `android.util.AtomicFile` 做原子替换，避免进程中断留下半截 JSON。
 *
 * 设计意图与巡逻 Demo 的 `AssetUploadManager.atomicWrite` 一致；读写失败向上抛出，由队列记录日志。
 */
class AtomicInspectionQueueFile(private val file: File) : InspectionQueueFile {

    override fun read(): String? {
        if (!file.exists()) return null
        return AtomicFile(file).readFully().toString(Charsets.UTF_8)
    }

    override fun write(value: String) {
        file.parentFile?.mkdirs()
        val atomicFile = AtomicFile(file)
        val output = atomicFile.startWrite()
        try {
            output.write(value.toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(output)
        } catch (error: Throwable) {
            atomicFile.failWrite(output)
            throw error
        }
    }
}
