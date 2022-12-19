package dji.v5.ux.core.ui.setting.dialog

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.view.KeyEvent


/**
 * 把对话框的按钮事件传给Activity
 */
open class DispatchDialogKeyEventToActivityListener(val context: Context?) : DialogInterface.OnKeyListener {
    private var needHandle = false
    private var keyDown = false
    override fun onKey(dialog: DialogInterface?, keyCode: Int, event: KeyEvent): Boolean {
        // 返回键交给上面处理
        if (event.keyCode == KeyEvent.KEYCODE_BACK || dialog == null || context == null) {
            return false
        } else if (!keyDown && event.action == KeyEvent.ACTION_DOWN) {
            // repeatCount == 0代表是第一次按下按键，这时开始处理按键事件
            keyDown = true
            needHandle = event.repeatCount == 0
        } else if (event.action == KeyEvent.ACTION_UP) {
            keyDown = false
        }

        if (needHandle) {
            dispatchKeyEvent(context, event)
        }

        return false

    }

    private fun dispatchKeyEvent(context: Context?, keyEvent: KeyEvent?): Boolean {
        val activity: Activity? = contextToActivity(context)
        return if (activity != null) {
            activity.dispatchKeyEvent(keyEvent)
            true
        } else {
            false
        }
    }

    private fun contextToActivity(context: Context?): Activity? {
        if (context == null) {
            return null
        }
        if (context is Activity) {
            return context
        } else if (context is ContextWrapper) {
            val context2 = context.baseContext
            if (context2 is Activity) {
                return context2
            }
        }
        return null
    }
}