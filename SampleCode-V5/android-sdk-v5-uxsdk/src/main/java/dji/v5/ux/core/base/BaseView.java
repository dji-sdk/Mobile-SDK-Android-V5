package dji.v5.ux.core.base;

import android.content.Context;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import dji.v5.utils.common.LogUtils;

public class BaseView extends View {
    private static final String TAG = BaseView.class.getSimpleName();
    public BaseView(Context context) {
        super(context);
    }

    public BaseView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BaseView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void invalidate() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            super.invalidate();
        } else {
            try {
                BaseFrameLayout.recordInvalidateCallStack(this);
            } catch (Exception e) {
                LogUtils.e(TAG,"invalidate catch runtime error: "+e.getMessage());
            }
        }
    }
}
