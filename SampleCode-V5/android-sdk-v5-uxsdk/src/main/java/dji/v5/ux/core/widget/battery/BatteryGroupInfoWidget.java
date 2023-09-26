package dji.v5.ux.core.widget.battery;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dji.sdk.keyvalue.value.battery.BatteryOverviewValue;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;

public class BatteryGroupInfoWidget extends ConstraintLayoutWidget<Object> {


    protected BatteryGroupInfoWidgetModel widgetModel = new BatteryGroupInfoWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());

    protected BatteryGroupWidget batteryGroupWidget;
    protected TextView batteryChargeRemaining;
    protected TextView flightTime;
    protected Button batteryDetail;

    protected boolean isConnected;


    public BatteryGroupInfoWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BatteryGroupInfoWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BatteryGroupInfoWidget(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_view_group_battery_info, this);

        batteryChargeRemaining = findViewById(R.id.setting_menu_battery_charge_remain);
        flightTime = findViewById(R.id.setting_menu_battery_fly_time);
        batteryDetail = findViewById(R.id.setting_menu_battery_info_detail_view);
        batteryGroupWidget = findViewById(R.id.setting_menu_battery_group_view);

        setBackgroundResource(R.drawable.uxsdk_background_fpv_setting_battery_group);

        batteryGroupWidget.setEnableBatteryCells(false);
        batteryGroupWidget.setEnableSerialNumber(false);
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getBatteryChargeRemaining().subscribe(this::updateBatteryChargeRemaining));

        addReaction(widgetModel.getFlightTimeInSeconds().subscribe(this::updateFlightTime));

        addReaction(widgetModel.getConnection().subscribe(connection -> {
            if (Boolean.compare(isConnected, connection) == 0) {
                return;
            }
            isConnected = connection;
            if (!isConnected) {
                widgetModel.reset();
            }
        }));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            widgetModel.setup();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
    }

    private void updateFlightTime(int time) {
        // 除以10才是正确的值
        time /= 10;
        int seconds = time % 60;
        int minute = time / 60;
        String data = String.format(Locale.getDefault(), "%1$02d:%2$02d", minute, seconds);
        flightTime.setText(data);
    }

    private void updateBatteryChargeRemaining(int percent) {
        batteryChargeRemaining.setText(getResources().getString(R.string.uxsdk_battery_percent, percent));
    }

    public void setOnDetailOnClickListener(@Nullable View.OnClickListener listener) {
        batteryDetail.setVisibility(listener == null ? GONE : VISIBLE);
        batteryDetail.setOnClickListener(listener);
    }
}
