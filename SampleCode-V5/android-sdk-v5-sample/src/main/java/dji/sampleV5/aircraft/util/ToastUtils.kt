package dji.sampleV5.aircraft.util

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import dji.v5.utils.common.ContextUtil
import java.lang.ref.WeakReference

object ToastUtils {
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var toastRef: WeakReference<Toast>? = null

    fun showToast(msg: String) {
        showLongToast(msg)
    }

    fun showLongToast(msg: String) {
        showToast(msg, Toast.LENGTH_LONG)
    }

    fun showShortToast(msg: String) {
        showToast(msg, Toast.LENGTH_SHORT)
    }

    @Synchronized
    fun showToast(msg: String, duration: Int) {
        handler.post {
            toastRef?.let {
                it.get()?.cancel()
                it.clear()
            }
            toastRef = null;
            val toast = Toast.makeText(ContextUtil.getContext(), msg, duration)
            toastRef = WeakReference(toast)
            toast.show()
        }
    }

}
