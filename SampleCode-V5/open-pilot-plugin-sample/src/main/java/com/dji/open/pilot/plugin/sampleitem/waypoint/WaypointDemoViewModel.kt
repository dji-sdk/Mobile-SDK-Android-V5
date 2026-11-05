package com.dji.open.pilot.plugin.sampleitem.waypoint

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.waypoint3.WaypointMissionManager

/**
 * 航线任务 Demo：通过 pushKMZFileToRemote 将 KMZ 航线导入 Pilot。
 */
class WaypointDemoViewModel : ViewModel() {

    /** 导入过程/结果文案 */
    val pushResult = MutableLiveData<String>()

    /** 是否正在导入，避免重复触发 */
    val isPushing = MutableLiveData(false)

    fun pushKMZFile(path: String) {
        if (isPushing.value == true) {
            return
        }
        isPushing.postValue(true)
        pushResult.postValue("importing: $path")
        WaypointMissionManager.getInstance().pushKMZFileToRemote(path,
            object : CommonCallbacks.CompletionCallbackWithProgress<Double> {
                override fun onProgressUpdate(progress: Double) {
                    pushResult.postValue("progress: ${(progress * 100).toInt()}%")
                }

                override fun onSuccess() {
                    isPushing.postValue(false)
                    pushResult.postValue("import success: $path")
                }

                override fun onFailure(error: IDJIError) {
                    isPushing.postValue(false)
                    pushResult.postValue("import failed: ${error.errorCode()} ${error.description()}")
                }
            })
    }
}
