package dji.v5.ux.util

import android.text.TextWatcher
import android.text.TextUtils
import android.text.Editable

internal class RtkSettingWatcher(private val mListener: OnEditTextEmptyChangedListener?) : TextWatcher {
    private var mIsBeforeTextEmpty = false
    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        mIsBeforeTextEmpty = TextUtils.isEmpty(s)
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        //do no thing
    }
    override fun afterTextChanged(s: Editable) {
        val isTextEmpty = TextUtils.isEmpty(s)
        if (isTextEmpty != mIsBeforeTextEmpty) {
            mListener?.isTextEmptyChanged()
        }
    }

    interface OnEditTextEmptyChangedListener {
        fun isTextEmptyChanged()
    }
}