package dji.v5.ux.core.base;

import android.content.Context;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.v5.utils.common.LogUtils;

public class BaseFrameLayout extends FrameLayout {

    private static final String TAG = BaseFrameLayout.class.getSimpleName();

    public BaseFrameLayout(@NonNull Context context) {
        super(context);
    }

    public BaseFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BaseFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void invalidate() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            super.invalidate();
        } else {
            try {
                recordInvalidateCallStack(this);
            } catch (Exception e) {
                LogUtils.e(TAG, "invalidate runtime error: " + e.getMessage());
            }
        }
    }

    public static void recordInvalidateCallStack(View view) {
        view.postInvalidate();
        StringBuilder stringBuilder = new StringBuilder();
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            stringBuilder.append(stackTraceElement);
            stringBuilder.append("\n");
        }
        LogUtils.e("View", " async call invalidate \n" + stringBuilder);
    }
}
