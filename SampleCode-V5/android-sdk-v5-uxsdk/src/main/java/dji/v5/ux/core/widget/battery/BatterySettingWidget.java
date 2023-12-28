package dji.v5.ux.core.widget.battery;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.v5.ux.R;
import dji.v5.ux.core.base.widget.FrameLayoutWidget;

public class BatterySettingWidget extends FrameLayoutWidget<Object> {
    private BatteryGroupInfoWidget batteryGroupInfoWidget;

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
        View view = inflate(context, R.layout.uxsdk_setting_battery_widget, this);
         batteryGroupInfoWidget = view.findViewById(R.id.setting_menu_battery_group);
    }

    @Override
    protected void reactToModelChanges() {
        //Do NothingBasicRangeSeekBarWidget
    }

    public BatteryGroupInfoWidget getBatteryGroupInfoWidget() {
        return batteryGroupInfoWidget;
    }

}
