package dji.sampleV5.aircraft.util

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialog
import dji.sampleV5.aircraft.R
import dji.v5.common.callback.CommonCallbacks

/**
 * @author feel.feng
 * @time 2022/10/13 8:57 下午
 * @description: Dialog工具类
 */
object DialogUtil {

    fun showInputDialog(
        context: Activity,
        title: String?,
        msg: String?,
        hint: String?,
        singleLine: Boolean,
        callback: CommonCallbacks.CompletionCallbackWithParam<String>
    ) {
        val dialogView: View = context.layoutInflater.inflate(R.layout.dialog_param_input, null)
        dialogView.setBackgroundColor(context.resources.getColor(R.color.gray))
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()
        dialog!!.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        val tvTitle = dialogView.findViewById<TextView>(R.id.title)
        tvTitle.text = title
        val input = dialogView.findViewById<EditText>(R.id.input)
        input.isSingleLine = singleLine
        if (Util.isNotBlank(msg)) {
            input.setText(msg)
        }
        if (Util.isNotBlank(hint)) {
            input.hint = hint
        }
        input.movementMethod = ScrollingMovementMethod.getInstance()
        dialogView.findViewById<View>(R.id.confirm).setOnClickListener {
            callback.onSuccess(input.text.toString())
            if (dialog != null) {
                dialog.dismiss()
            }
        }
        dialogView.findViewById<View>(R.id.cancel).setOnClickListener {
            callback?.onSuccess(null)
            if (dialog != null) {
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    fun showTwoButtonDialog(
        context: Context,
        content: String,
        left: String, leftClick: () -> Unit,
        right: String, rightClick: () -> Unit
    ): Dialog {
        val dialog = AppCompatDialog(context, R.style.MSDKDialog)
        val contentView = LayoutInflater.from(context).inflate(R.layout.dialog_two_button, FrameLayout(context), false).apply {

            findViewById<TextView>(R.id.tv_left).apply {
                text = left
                setOnClickListener {
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                    leftClick()
                }
            }
            findViewById<TextView>(R.id.tv_right).apply {
                text = right
                setOnClickListener {
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                    rightClick()
                }
            }
            findViewById<TextView>(R.id.tv_comment).text = content
        }
        dialog.setContentView(contentView)
        dialog.show()

        return dialog
    }
}