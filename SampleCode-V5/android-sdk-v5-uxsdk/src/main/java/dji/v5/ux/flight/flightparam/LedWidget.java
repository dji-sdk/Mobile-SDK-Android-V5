package dji.v5.ux.flight.flightparam;


import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.v5.ux.R;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;

public class LedWidget extends ConstraintLayoutWidget<Object> {

    public LedWidget(@NonNull Context context) {
        super(context);
    }

    public LedWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LedWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_common_led, this);

    }

    @Override
    protected void reactToModelChanges() {
        //add log
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }


}
