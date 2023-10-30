package dji.v5.ux.core.widget.battery;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

import dji.v5.utils.common.AndUtil;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.widget.BasicRangeSeekBar;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;

public class BatteryAlertWidget extends ConstraintLayoutWidget<Object> implements BasicRangeSeekBar.OnRangeSeekBarListener {

    private static final String TAG = "BatteryAlertWidget";

    private static final int GO_HOME_MAX = 50;
    private static final int LANDING_MIN = 10;
    private static final int LANDING_GAP = 5;

    protected BatteryAlertWidgetModel widgetModel = new BatteryAlertWidgetModel(
            DJISDKModel.getInstance(),
            ObservableInMemoryKeyedStore.getInstance());
    protected TextView mTvSeriousLowBattery;
    protected TextView mTvLowBattery;
    protected BasicRangeSeekBar mBasicRangeSeekBar;

    protected int lowBatteryValue = -1;
    protected int seriousLowBatteryValue = -1;

    public BatteryAlertWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BatteryAlertWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BatteryAlertWidget(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_setting_menu_battery_controller_layout, this);
        mTvSeriousLowBattery = findViewById(R.id.tv_serious_low_battery_title);
        mTvLowBattery = findViewById(R.id.tv_low_battery_title);
        mBasicRangeSeekBar = findViewById(R.id.checklist_item_range_seek_bar);

        mBasicRangeSeekBar.setRange(LANDING_MIN, GO_HOME_MAX, 1, LANDING_GAP);
        mBasicRangeSeekBar.setListener(this);
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getLowBatteryWarning().subscribe(value -> {
            lowBatteryValue = value;
            updateProgress();
        }));

        addReaction(widgetModel.getSeriousLowBatteryWarning().subscribe(integer -> {
            seriousLowBatteryValue = integer;
            updateProgress();
        }));

        addReaction(widgetModel.getConnection().subscribe(isConnected -> {
            mBasicRangeSeekBar.setEnabled(isConnected);
            if (Boolean.TRUE.equals(isConnected)) {
                mTvLowBattery.setVisibility(View.VISIBLE);
                mTvSeriousLowBattery.setVisibility(View.VISIBLE);
                return;
            }
            mTvLowBattery.setVisibility(View.GONE);
            mTvSeriousLowBattery.setVisibility(View.GONE);
            lowBatteryValue = -1;
            seriousLowBatteryValue = -1;
            onValuesChanging(0.0f, 0.0f);
            mBasicRangeSeekBar.setCurrentValues(LANDING_MIN, GO_HOME_MAX);
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

    @Override
    public void onValuesChanging(float newSeriousLow, float newLow) {
        String newSeriousLowText = String.format(Locale.getDefault(), "%02d%%", (int) newSeriousLow);
        String newLowText = String.format(Locale.getDefault(), "%02d%%", (int) newLow);

        updateText(mTvSeriousLowBattery, R.string.uxsdk_checklist_manual_serious_low_battery_percent, newSeriousLowText, AndUtil.getResColor(R.color.uxsdk_red_in_dark));
        updateText(mTvLowBattery, R.string.uxsdk_checklist_manual_low_battery_percent, newLowText, AndUtil.getResColor(R.color.uxsdk_orange_in_dark));
    }

    @Override
    public void onValuesChanged(float newSeriousLow, float newLow) {
        if (lowBatteryValue != (int) newLow) {
            addDisposable(widgetModel.changeLowBatteryWarning((int) newLow).subscribe(() -> {

            }, throwable -> {
                LogUtils.e(TAG, "changeLowBatteryWarning fail: " + throwable);
                updateProgress();
            }));
        }

        if (seriousLowBatteryValue != (int) newSeriousLow) {
            addDisposable(widgetModel.changeSeriousLowBatteryWarning((int) newSeriousLow).subscribe(() -> {

            }, throwable -> {
                LogUtils.e(TAG, "changeSeriousLowBatteryWarning fail: " + throwable);
                updateProgress();
            }));
        }
    }

    private void updateProgress() {
        if (seriousLowBatteryValue <= 0 || lowBatteryValue <= 0 || !mBasicRangeSeekBar.isEnabled()) {
            return;
        }

        mBasicRangeSeekBar.setCurrentValues(seriousLowBatteryValue, lowBatteryValue);
        onValuesChanging(seriousLowBatteryValue, lowBatteryValue);

    }

    private void updateText(TextView textView, int res, String value, int valueColor) {
        String rawText = AndUtil.getResString(res, value);

        SpannableStringBuilder spannableString = new SpannableStringBuilder(rawText);
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(valueColor);
        int start = rawText.indexOf(value);
        int end = start + value.length();
        spannableString.setSpan(foregroundColorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(spannableString);
    }
}
