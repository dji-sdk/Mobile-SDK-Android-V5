package dji.v5.ux.core.ui;

import android.content.Context;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.v5.utils.common.LogUtils;


public class BaseFrameLayout extends FrameLayout {

    protected final String TAG = this.getClass().getSimpleName();

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

    /**
     * 接管处理下可能存在的异步invalidate
     * https://cloud.tencent.com/developer/article/1846821
     */
    @Override
    public void invalidate() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            super.invalidate();
        } else {
            recordInvalidateCallStack(this);
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

    public static class NonMainThreadInvalidateException extends RuntimeException {
        public NonMainThreadInvalidateException() {
            super("Dji Only the original thread that created a view hierarchy can touch its views.");
        }

    }

}
