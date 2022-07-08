package dji.sampleV5.modulecommon.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import dji.v5.utils.common.ContextUtil

object ToastUtils {
    private val handler = Handler(Looper.getMainLooper())

    fun showToast(context: Context?, msg: String) {
        context?.let {
            it.applicationContext.apply {
                handler.post {
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun showToast(msg: String) {
        showToast(ContextUtil.getContext(), msg)
    }
}