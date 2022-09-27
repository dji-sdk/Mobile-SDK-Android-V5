package dji.sampleV5.modulecommon.keyvalue

import android.util.Log
import dji.sampleV5.modulecommon.data.KeyCheckInfo
import dji.sdk.keyvalue.key.ComponentType
import dji.sdk.keyvalue.key.SubComponentType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.DJICommonError
import dji.v5.manager.capability.CapabilityManager
import dji.v5.utils.common.*
import java.lang.StringBuilder
import java.lang.Thread.sleep

/**
 * @author feel.feng
 * @time 2022/09/13 5:43 下午
 * @description: 能力集key 测试,结果中只展示未通过的key
 * 结果存储在包目录/keycheck/result.txt
 */
 object CapabilityKeyChecker  {

    private val INTERVAL_TIME = 100L;
    private val TAG_GET = "【GET】"
    private val TAG_ERROR = "GetErrorMsg"
    private val TAG_EQUAL = "=="
    private val TAG = LogUtils.getTag(this)


    fun check(productType: String , callback:CommonCallbacks.CompletionCallback){
        var unPassedCount  = 0;
        val allList: MutableList<KeyItem<*, *>> = ArrayList()
        DJIExecutor.getExecutorFor(DJIExecutor.Purpose.IO).execute {
            KeyItemDataUtil.getAllKeyList(allList)
            val capabilityKeyCount = CapabilityManager.getInstance().getCapabilityKeyCount(productType)
            val result : StringBuilder = StringBuilder(" ----- begin check -----\n\n")
            LogUtils.i(TAG, "begin check $capabilityKeyCount")
            allList
                .filter {
                    val keyName = "Key$it"
                    it.canGet()
                       && CapabilityManager.getInstance().isKeySupported(productType, "", ComponentType.find(it.getKeyInfo().componentType), keyName)

                }
                .forEach(){ item ->
                    item.setComponetIndex(ComponentIndexType.LEFT_OR_MAIN.value())
                    item.setSubComponetType(SubComponentType.IGNORE.value())
                    item.setSubComponetIndex(0)
                    item.setKeyOperateCallBack {
                        val resStr = it.toString()
                        val keyNameIndex = resStr.indexOf(TAG_GET)
                        val keyName = resStr.substring(0 , keyNameIndex)
                        val isPassed: Boolean
                        val failedReson = if (resStr.contains(TAG_ERROR)) {
                            isPassed = false
                            resStr.substring(resStr.indexOf(TAG_EQUAL))
                        } else {
                            isPassed = true
                            "N/A"
                        }
                        if (!isPassed) {
                            result.append("${++unPassedCount} KeyName :${keyName} - ${ComponentType.find(item.getKeyInfo().componentType)}\n")
                                .append("IsPassed:${isPassed}\n")
                                .append("FailedReason:${failedReson}\n")
                                .append("\n ----------------------- \n")
                        }
                    }
                    item.doGet()
                    sleep(INTERVAL_TIME)
                }
            result.append(" --------finish---------\n")
                .append("UnPassed Key Count:${unPassedCount}")
            Log.e(TAG, "check finish!"  )
            if (unPassedCount > 0){
                callback.onFailure(DJICommonError.FACTORY.build("unpassed count:${unPassedCount}"));
            } else {
                callback.onSuccess()
            }
            saveResult(result.toString())
        }
    }

    fun saveResult( content : String) {
        var filePath = DiskUtil.getExternalCacheDirPath(ContextUtil.getContext() , "keycheck/result.txt")
        FileUtils.writeFile(filePath, content, false)
    }


}