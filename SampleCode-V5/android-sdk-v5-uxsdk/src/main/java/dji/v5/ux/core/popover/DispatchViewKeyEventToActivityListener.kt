package dji.v5.ux.core.popover

import android.view.KeyEvent
import android.view.View
import dji.v5.ux.core.util.ViewUtil

/**
 * 把View的按键事件透传给Activity
 */
open class DispatchViewKeyEventToActivityListener : View.OnKeyListener {
    private var needHandle = false
    private var keyDown = false
    override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
        // 返回键交给上面处理
        if (event.keyCode == KeyEvent.KEYCODE_BACK || v == null) {
            return false
        } else if (!keyDown && event.action == KeyEvent.ACTION_DOWN) {
            // repeatCount == 0代表是第一次按下按键，这时开始处理按键事件
            keyDown = true
            needHandle = event.repeatCount == 0
        } else if (event.action == KeyEvent.ACTION_UP) {
            keyDown = false
        }

        return if (!needHandle) {
            false
        } else {
            ViewUtil.dispatchKeyEvent(v.context, event)
            true
        }

    }

}