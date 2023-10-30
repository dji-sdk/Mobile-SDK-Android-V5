package dji.v5.ux.core.widget.battery;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.v5.ux.R;
import dji.v5.ux.core.base.widget.FrameLayoutWidget;

public class BatterySettingWidget extends FrameLayoutWidget<Object> {

    public BatterySettingWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BatterySettingWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BatterySettingWidget(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_setting_battery_widget, this);
        setBackgroundResource(R.drawable.uxsdk_background_black_rectangle);
    }

    @Override
    protected void reactToModelChanges() {
        //Do NothingBasicRangeSeekBarWidget
    }
}
