package dji.v5.ux.core.widget.battery;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;


import java.util.Locale;

import dji.v5.ux.R;


public class BatteryCellView extends RelativeLayout {

    private static final float[] SECTION_PART_VOLTAGE = new float[]{
            3.0f, 4.35f
    };

    ProgressBar batteryPartPgb;
    TextView batteryVoltageTv;

    private float mCurrentVoltage;

    public BatteryCellView(Context context) {
        this(context, null);
    }

    public BatteryCellView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryCellView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.uxsdk_view_battery_detail_cell, this);
        batteryPartPgb = findViewById(R.id.battery_part_pgb);
        batteryVoltageTv = findViewById(R.id.battery_part_tv);
    }

    public void setVoltage(float voltage) {
        if (mCurrentVoltage == voltage) {
            return;
        }
        int progress = calculatePartVoltageProgress(voltage);
        if (batteryPartPgb.getProgress() != progress) {
            batteryPartPgb.setProgress(progress);
        }
        batteryVoltageTv.setText(String.format(Locale.getDefault(), "%.2fV", voltage));
        mCurrentVoltage = voltage;
    }

    private int calculatePartVoltageProgress(final float voltage) {
        int progress;
        if (voltage >= SECTION_PART_VOLTAGE[1]) {
            progress = 100;
        } else if (voltage <= SECTION_PART_VOLTAGE[0]) {
            progress = 0;
        } else {
            progress = (int) (((voltage - SECTION_PART_VOLTAGE[0]) * 100) / (SECTION_PART_VOLTAGE[1] - SECTION_PART_VOLTAGE[0]));
        }
        return progress;
    }

}
