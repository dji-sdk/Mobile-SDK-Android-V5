package dji.sampleV5.modulecommon.util

import android.os.Handler
import android.os.HandlerThread
import android.widget.Toast
import dji.v5.utils.common.ContextUtil
import dji.v5.utils.common.LogUtils
import java.lang.IllegalStateException

private const val TAG = "ToastUtils"

object ToastUtils {
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var toast: Toast? = null

    private var isActivityDestroy = false


    //Toast必须在子线程，否则SmartController遥控器（Android 版本7.1.2）会奔溃：https://www.jianshu.com/p/ccfc5fa3130c
    fun init() {
        handlerThread = HandlerThread("ToastUtils")
        handlerThread?.start()
        handler = Handler(handlerThread!!.looper)
        isActivityDestroy = false
    }

    fun destroy() {
        LogUtils.e(TAG, "destroy")
        handler?.removeCallbacksAndMessages(null)
        isActivityDestroy = true
        handlerThread?.quitSafely()
    }

    fun showToast(msg: String) {
        if (handlerThread == null || handlerThread?.looper == null) {
            throw IllegalStateException("ToastUtil has not been initialized yet, please call the init method before calling this method")
        }
        if (isActivityDestroy) {
            LogUtils.e(TAG, "Activity has destroy！")
            return
        }

        handler?.post {
            toast?.cancel()
            toast = Toast.makeText(ContextUtil.getContext(), msg, Toast.LENGTH_SHORT)
            toast?.show()
        }
    }
}
