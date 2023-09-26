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
import dji.v5.ux.core.base.widget.FrameLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;

public class BatteryGroupWidget extends FrameLayoutWidget<Object> {

    protected BatteryGroupInfoWidgetModel widgetModel = new BatteryGroupInfoWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());

    protected ViewGroup container;

    protected boolean isConnected = false;
    protected boolean isEnableBatteryCells = true;
    protected boolean isEnableSerialNumber = true;


    public BatteryGroupWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BatteryGroupWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BatteryGroupWidget(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_view_group_battery, this);

        container = findViewById(R.id.setting_menu_battery_info_view_layout);
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getBatteryOverview().subscribe(this::updateBatteryWidget));

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

    private void updateBatteryWidget(List<BatteryOverviewValue> batteryOverviewList) {
        List<BatteryOverviewValue> availableList = new ArrayList<>();
        for (BatteryOverviewValue value : batteryOverviewList) {
            if (value.getIsConnected().equals(true)) {
                availableList.add(value);
            }
        }
        int childCount = container.getChildCount();
        int newCount = availableList.size();
        if (childCount < newCount) {
            for (int i = childCount; i < newCount; i++) {
                BatteryInfoWidget view = new BatteryInfoWidget(getContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.topMargin = getResources().getDimensionPixelSize(R.dimen.uxsdk_15_dp);
                container.addView(view, params);
                view.setEnableSerialNumber(isEnableSerialNumber);
                view.setEnableBatteryCells(isEnableBatteryCells);
            }
        } else if (childCount > newCount) {
            for (int i = childCount - 1; i >= newCount; i--) {
                container.removeViewAt(i);
            }
        }

        for (int i = 0; i < container.getChildCount(); i++) {
            BatteryInfoWidget view = (BatteryInfoWidget) container.getChildAt(i);
            view.setBatteryIndex(availableList.get(i).getIndex());
        }

    }

    public void setEnableBatteryCells(boolean isEnable) {
        this.isEnableBatteryCells = isEnable;
        updateBatteryInfoWidget();
    }

    public void setEnableSerialNumber(boolean isEnable) {
        this.isEnableSerialNumber = isEnable;
        updateBatteryInfoWidget();
    }

    private void updateBatteryInfoWidget() {
        for (int i = 0; i < container.getChildCount(); i++) {
            View childView = container.getChildAt(i);
            if (childView instanceof BatteryInfoWidget) {
                BatteryInfoWidget widget = (BatteryInfoWidget) childView;
                widget.setEnableBatteryCells(isEnableBatteryCells);
                widget.setEnableSerialNumber(isEnableSerialNumber);
            }
        }
    }

}
