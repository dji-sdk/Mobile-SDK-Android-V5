package dji.v5.ux.core.widget.hd.interference;


import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.v5.ux.core.base.BaseFrameLayout;
import dji.v5.ux.core.widget.hd.frequency.FreqView;

public class HDInterferenceWidget extends BaseFrameLayout {
    private FreqView mOcuInterferenceView;
    public HDInterferenceWidget(@NonNull Context context) {
        super(context);
    }

    public HDInterferenceWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HDInterferenceWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        changeInterferenceView(context);
    }

    private void changeInterferenceView(Context ctx) {
        removeAllViews();
        mOcuInterferenceView = new FreqView(ctx);
        addView(mOcuInterferenceView);
    }
}
