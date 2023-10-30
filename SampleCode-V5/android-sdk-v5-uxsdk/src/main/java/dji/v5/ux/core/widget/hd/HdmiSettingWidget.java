package dji.v5.ux.core.widget.hd;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.v5.ux.R;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;

public class HdmiSettingWidget extends ConstraintLayoutWidget<Object> {
    public HdmiSettingWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HdmiSettingWidget(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_hdmi_setting_layout, this);
    }

    @Override
    protected void reactToModelChanges() {
        //do nothing

    }
}
