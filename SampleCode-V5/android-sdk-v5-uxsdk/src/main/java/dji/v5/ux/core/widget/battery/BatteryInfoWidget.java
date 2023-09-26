package dji.v5.ux.core.widget.battery;


import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;


import java.util.List;
import java.util.Locale;

import dji.sdk.keyvalue.utils.ProductUtil;
import dji.sdk.keyvalue.value.battery.BatteryConnectionState;
import dji.sdk.keyvalue.value.battery.BatteryException;
import dji.sdk.keyvalue.value.battery.BatteryOverviewValue;
import dji.sdk.keyvalue.value.battery.BatterySelfCheckState;
import dji.sdk.keyvalue.value.battery.IndustryBatteryType;
import dji.v5.common.utils.UnitUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.popover.Popover;

public class BatteryInfoWidget extends ConstraintLayoutWidget<Object> {

    private static final String SEPARATOR = ",";

    @IntDef
    public @interface Temperature {
        int TEMPERATURE_FAHRENHEIT = 0; // 华氏度
        int TEMPERATURE_CELSIUS = 1; // 摄氏度
        int TEMPERATURE_KELVIN = 2; // 开氏度
    }

    protected BatteryInfoWidgetModel widgetModel = new BatteryInfoWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());

    protected TextView batteryHighVoltageSave;
    protected ImageView batteryHighVoltageHint;
    protected TextView batteryCycleTime;
    protected TextView batteryVoltage;
    protected TextView batteryTemperature;
    protected TextView batteryChargeRemaining;
    protected TextView batteryStatus;
    protected TextView battery;
    protected TextView serialNumber;
    protected TextView productionDate;
    protected ViewGroup cellsLayout;
    protected ViewGroup batteryBottomLayout;
    protected Group highVoltage;

    protected int temperatureType = Temperature.TEMPERATURE_CELSIUS;
    protected float temperatureValue = 0.0f;
    protected boolean supportHighVoltageHint = false;
    protected boolean isConnected;

    protected int batteryCount;
    protected IndustryBatteryType batteryType;
    protected BatteryConnectionState batteryConnectionState;
    protected BatteryException warningRecord;

    public BatteryInfoWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BatteryInfoWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BatteryInfoWidget(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_view_battery_info, this);

        batteryHighVoltageSave = findViewById(R.id.setting_menu_battery_high_voltage_save_value_tv);
        batteryHighVoltageHint = findViewById(R.id.icon_high_voltage);
        batteryCycleTime = findViewById(R.id.setting_menu_battery_cycle_time_value_tv);
        batteryVoltage = findViewById(R.id.setting_menu_battery_voltage_value_tv);
        batteryTemperature = findViewById(R.id.setting_menu_battery_temperature_value_tv);
        batteryChargeRemaining = findViewById(R.id.setting_menu_battery_charge_remain_value_tv);
        batteryStatus = findViewById(R.id.setting_menu_battery_status);
        battery = findViewById(R.id.setting_menu_battery_tv);
        serialNumber = findViewById(R.id.setting_menu_battery_sn);
        productionDate = findViewById(R.id.setting_menu_battery_production_date);
        highVoltage = findViewById(R.id.group_high_voltage);
        cellsLayout = findViewById(R.id.setting_menu_battery_cells_view_stub);
        batteryBottomLayout = findViewById(R.id.setting_menu_battery_bottom_view);

        setBackgroundResource(R.drawable.uxsdk_background_fpv_setting);
        showHighVoltage(supportHighVoltage());
        batteryHighVoltageHint.setOnClickListener(v -> {
            int[] outLocation = new int[2];
            batteryHighVoltageHint.getLocationOnScreen(outLocation);
            int heightPixels = getResources().getDisplayMetrics().heightPixels;
            Popover.Position pos = outLocation[1] > heightPixels / 2 ? Popover.Position.TOP : Popover.Position.BOTTOM;
            int color = ContextCompat.getColor(getContext(), R.color.uxsdk_fpv_popover_content_background_color);
            new Popover.Builder(batteryHighVoltageHint)
                    .content(supportHighVoltageHint ? R.string.uxsdk_hms_carepage_maintenance_highvoltage_info : R.string.uxsdk_hms_carepage_maintenance_highvoltage_need_upgrade_info)
                    .textColor(R.color.uxsdk_white)
                    .showArrow(true)
                    .arrowColor(color)
                    .size(getResources().getDimensionPixelSize(R.dimen.uxsdk_240_dp), ViewGroup.LayoutParams.WRAP_CONTENT)
                    .allScreenMargin(getResources().getDimensionPixelSize(R.dimen.uxsdk_8_dp))
                    .backgroundColor(color)
                    .position(pos)
                    .focusable(false)
                    .align(Popover.Align.CENTER)
                    .build()
                    .show();
        });
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

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel
                .getBatteryException()
                .subscribe(e -> {
                    warningRecord = e;
                    updateStatus();
                }));

        addReaction(widgetModel
                .getBatteryTemperature()
                .subscribe(temperature -> {
                    temperatureValue = temperature.floatValue();
                    updateTemperature();
                }));

        addReaction(widgetModel
                .getBatteryConnection()
                .subscribe(this::updateConnectionState));

        addReaction(widgetModel
                .getBatteryIsCommunicationException()
                .subscribe(batteryConnectionStateMsg -> {
                    batteryConnectionState = batteryConnectionStateMsg.getState();
                    updateStatus();
                }));

        addReaction(widgetModel
                .getBatteryBatteryHighVoltageStorageSec()
                .subscribe(this::updateBatteryBatteryHighVoltageStorage));

        addReaction(widgetModel
                .getBatterySerialNumber()
                .subscribe(this::updateSerialNumber));

        addReaction(widgetModel
                .getBatteryManufacturedDate()
                .subscribe(date -> {
                    String text = getContext().getString(R.string.uxsdk_setting_ui_battery_product_date) + date.getYear() + "-" + (date.getMonth());
                    productionDate.setText(text);
                }));

        addReaction(widgetModel
                .getBatteryCellVoltages()
                .subscribe(this::updateBatteryCells));

        addReaction(widgetModel
                .getBatteryVoltage()
                .subscribe(voltage -> batteryVoltage.setText(String.format(Locale.getDefault(), "%.2fV", voltage / 1000.0f))));

        addReaction(widgetModel
                .getBatteryChargeRemaining()
                .subscribe(percent -> batteryChargeRemaining.setText(getContext().getString(R.string.uxsdk_battery_percent, percent))));

        addReaction(widgetModel
                .getProductType()
                .subscribe(product -> showHighVoltage(supportHighVoltage())));

        addReaction(widgetModel
                .getBatteryNumberOfDischarges()
                .subscribe(number -> batteryCycleTime.setText(String.valueOf(number))));

        addReaction(widgetModel
                .getBatteryIndustryBatteryType()
                .subscribe(type -> {
                    batteryType = type;
                    showHighVoltage(supportHighVoltage());
                    updateBatteryTitle();
                }));

        addReaction(widgetModel
                .getBatteryOverview()
                .subscribe(this::updateBatteryOverview));
    }

    private void showHighVoltage(boolean show) {
        int v = show ? VISIBLE : GONE;
        highVoltage.setVisibility(v);
    }

    private void updateBatteryOverview(List<BatteryOverviewValue> overviewValueList) {
        batteryCount = overviewValueList.size();
        updateBatteryTitle();
    }

    private void updateConnectionState(boolean connection) {
        if (isConnected == connection) {
            return;
        }
        isConnected = connection;
        if (!connection) {
            widgetModel.reset();
            supportHighVoltageHint = false;
            batteryHighVoltageSave.setText(R.string.uxsdk_not_a_num);
            batteryHighVoltageSave.setTextColor(ContextCompat.getColor(getContext(), R.color.uxsdk_white));
        }
        widgetModel.restart();
    }

    private void updateSerialNumber(String serial) {
        String text = getContext().getString(R.string.uxsdk_setting_ui_battery_serial_number);
        if (serial != null) {
            text += serial;
        }
        serialNumber.setText(text);
    }


    private void updateBatteryBatteryHighVoltageStorage(long result) {
        if (result > 0) {
            long sec = result;
            long days = (sec + BatteryConfig.SECONDS_IN_DAY / 2) / BatteryConfig.SECONDS_IN_DAY;
            int colorRes;
            if (days >= BatteryConfig.HIGH_VOLTAGE_SAVE_DAYS_DANGER) {
                colorRes = R.color.uxsdk_tips_danger_in_dark;
            } else if (days >= BatteryConfig.HIGH_VOLTAGE_SAVE_DAYS_WARN) {
                colorRes = R.color.uxsdk_warning_state_color;
            } else {
                colorRes = R.color.uxsdk_white;
            }
            supportHighVoltageHint = true;
            batteryHighVoltageSave.setTextColor(ContextCompat.getColor(getContext(), colorRes));
            batteryHighVoltageSave.setText(String.format(getResources().getString(R.string.uxsdk_setting_ui_battery_discharge_day), days));
        } else {
            supportHighVoltageHint = false;
            batteryHighVoltageSave.setText(R.string.uxsdk_not_a_num);
            batteryHighVoltageSave.setTextColor(ContextCompat.getColor(getContext(), R.color.uxsdk_white));
        }
    }

    private void updateStatus() {
        String text = getContext().getString(R.string.uxsdk_setting_ui_battery_history_normal_status);
        int textColor = ContextCompat.getColor(getContext(), R.color.uxsdk_setting_ui_battery_green);

        if (batteryConnectionState != null && batteryConnectionState != BatteryConnectionState.NORMAL) {
            if (batteryConnectionState == BatteryConnectionState.INVALID) {
                text = getContext().getString(R.string.uxsdk_setting_ui_battery_history_invalid_status);
            } else {
                text = getContext().getString(R.string.uxsdk_setting_ui_battery_history_exception_status);
            }
            textColor = ContextCompat.getColor(getContext(), R.color.uxsdk_setting_ui_battery_red);
        } else {
            String warning = getWarning();
            if (!TextUtils.isEmpty(warning)) {
                text = warning;
                textColor = ContextCompat.getColor(getContext(), R.color.uxsdk_setting_ui_battery_red);
            }
        }

        batteryStatus.setText(text);
        batteryStatus.setTextColor(textColor);
    }

    private String getWarning() {
        if (warningRecord == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        if (warningRecord.getFirstLevelOverCurrent() || warningRecord.getSecondLevelOverCurrent()) {
            appendWarningContent(sb, getContext().getString(R.string.uxsdk_setting_ui_battery_history_firstlevel_current));
        }
        if (warningRecord.getFirstLevelOverHeating() || warningRecord.getSecondLevelOverHeating()) {
            appendWarningContent(sb, getContext().getString(R.string.uxsdk_setting_ui_battery_history_firstlevel_over_temperature));
        }
        if (warningRecord.getFirstLevelLowTemperature() || warningRecord.getSecondLevelLowTemperature()) {
            sb.append(getContext().getString(R.string.uxsdk_setting_ui_battery_history_firstlevel_low_temperature));
        }
        if (Boolean.TRUE.equals(warningRecord.getShortCircuited())) {
            appendWarningContent(sb, getContext().getString(R.string.uxsdk_setting_ui_battery_history_short_circuit));
        }
        if (warningRecord.getLowVoltageCellIndex() > 0) {
            appendWarningContent(sb, getContext().getString(R.string.uxsdk_setting_ui_battery_history_under_voltage));
        }
        if (warningRecord.getBrokenCellIndex() > 0) {
            appendWarningContent(sb, getContext().getString(R.string.uxsdk_setting_ui_battery_history_invalid));
        }
        if (warningRecord.getSelfCheckState() != BatterySelfCheckState.NORMAL && warningRecord.getSelfCheckState() != BatterySelfCheckState.UNKNOWN) {
            appendWarningContent(sb, getContext().getString(R.string.uxsdk_setting_ui_battery_history_discharge));
        }
        return sb.toString();
    }

    private void appendWarningContent(StringBuilder builder, String content) {
        if (builder.length() > 0) {
            builder.append(SEPARATOR);
        }
        builder.append(content);
    }

    private void updateBatteryTitle() {
        if (batteryType == null || batteryCount <= 0) {
            return;
        }

        String title;
        int batteryIndex = widgetModel.getBatteryIndex();
        switch (batteryCount) {
            case 1:
                title = getResources().getString(R.string.uxsdk_setting_ui_general_battery);
                break;
            case 2:
                if (batteryIndex == 0) {
                    title = getResources().getString(R.string.uxsdk_fpv_top_bar_battery_left_battery);
                } else {
                    title = getResources().getString(R.string.uxsdk_fpv_top_bar_battery_right_battery);
                }
                break;
            default:
                title = getResources().getString(R.string.uxsdk_setting_ui_general_battery) + " " + (batteryIndex + 1);
        }
        String typeName;
        switch (batteryType) {
            case TB60:
                typeName = "（TB60）";
                break;
            case TB65:
                typeName = "（TB65）";
                break;
            default:
                typeName = "";
                break;
        }
        battery.setText(String.format(Locale.getDefault(), "%s%s", title, typeName));
    }

    private void updateTemperature() {
        String unit;
        float value;
        if (Temperature.TEMPERATURE_FAHRENHEIT == temperatureType) {
            unit = getResources().getString(R.string.uxsdk_fahrenheit);
            value = UnitUtils.celsiusToFahrenheit(temperatureValue);
        } else if (Temperature.TEMPERATURE_KELVIN == temperatureType) {
            unit = getResources().getString(R.string.uxsdk_kelvins);
            value = UnitUtils.celsiusToKelvin(temperatureValue);
        } else {
            value = temperatureValue;
            unit = getResources().getString(R.string.uxsdk_celsius);
        }
        batteryTemperature.setText(String.format(Locale.getDefault(), "%.1f%s", value, unit));
    }

    private void updateBatteryCells(List<Integer> cellVoltages) {
        if (cellVoltages == null || cellVoltages.isEmpty()) {
            cellsLayout.removeAllViews();
            return;
        }
        int cellNumber = cellVoltages.size();
        int childCount = cellsLayout.getChildCount();
        if (childCount < cellNumber) {
            for (int i = childCount; i <= cellNumber; i++) {
                BatteryCellView cellView = new BatteryCellView(getContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.weight = 1;
                cellsLayout.addView(cellView, params);
            }
        }
        for (int i = 0; i < cellsLayout.getChildCount(); i++) {
            BatteryCellView cellView = (BatteryCellView) cellsLayout.getChildAt(i);
            if (i < cellNumber) {
                cellView.setVisibility(VISIBLE);
            } else {
                cellView.setVisibility(GONE);
                continue;
            }
            cellView.setVoltage(1f * cellVoltages.get(i) / 1000);
        }
    }

    public void setEnableBatteryCells(boolean isEnable) {
        cellsLayout.setVisibility(isEnable ? VISIBLE : GONE);
    }

    public void setEnableSerialNumber(boolean isEnable) {
        batteryBottomLayout.setVisibility(isEnable ? VISIBLE : GONE);
    }

    /**
     * 是否支持高压存储，M350新电池、M30新版本电池
     */
    private boolean supportHighVoltage() {
        return ProductUtil.isM30Product() || batteryType == IndustryBatteryType.TB65;
    }


    public void setBatteryIndex(int index) {
        widgetModel.setBatteryIndex(index);
    }

    public void setTemperatureType(@Temperature int temperatureType) {
        this.temperatureType = temperatureType;
        updateTemperature();
    }
}
