package dji.v5.ux.core.ui;

import android.content.Context;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class BaseView extends View {
    protected final String TAG = getClass().getSimpleName();
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

    /**
     * 接管处理下可能存在的异步invalidate
     * https://cloud.tencent.com/developer/article/1846821
     */
    @Override
    public void invalidate() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            super.invalidate();
        } else {
            BaseFrameLayout.recordInvalidateCallStack(this);
        }
    }
}
