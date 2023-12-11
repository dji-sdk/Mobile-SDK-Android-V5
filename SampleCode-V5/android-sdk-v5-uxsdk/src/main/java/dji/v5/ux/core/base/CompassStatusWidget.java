package dji.v5.ux.core.base;
/*
 * Copyright (c) 2017, DJI All Rights Reserved.
 */

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseLongArray;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;


import java.util.ArrayList;
import java.util.List;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.ProductKey;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.sdk.keyvalue.value.flightcontroller.CompassSensorState;
import dji.sdk.keyvalue.value.flightcontroller.CompassState;
import dji.sdk.keyvalue.value.flightcontroller.RedundancySensorUsedStateMsg;
import dji.sdk.keyvalue.value.product.ProductType;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.aircraft.simulator.SimulatorManager;
import dji.v5.ux.R;
import dji.v5.ux.core.util.ViewUtil;


public class CompassStatusWidget extends ConstraintLayout {

    private static final int THREE_COMPASS = 3;

    private int[] mCompassStasActionStrIds = new int[] {
        R.string.uxsdk_setting_ui_redundancy_sensor_compass_stat_action_0,
        R.string.uxsdk_setting_ui_redundancy_sensor_compass_stat_empty,
        R.string.uxsdk_setting_ui_redundancy_sensor_compass_stat_empty,
        R.string.uxsdk_setting_ui_redundancy_sensor_compass_stat_action_3,
        R.string.uxsdk_setting_ui_redundancy_sensor_compass_stat_action_4,
        R.string.uxsdk_setting_ui_redundancy_sensor_compass_stat_empty,
        R.string.uxsdk_setting_ui_redundancy_sensor_compass_stat_empty,
        R.string.uxsdk_setting_ui_redundancy_sensor_compass_stat_action_7,
        R.string.uxsdk_setting_ui_redundancy_sensor_compass_stat_action_8,
        R.string.uxsdk_setting_ui_redundancy_sensor_compass_stat_action_9 };


    Group mSettingUiFlycCompassLy2;

    Group mSettingUiFlycCompassLy3;

    List<TextView> mSettingUiFlycTitles = new ArrayList<>();

    List<ProgressStatusWidget> mSettingUiFlyValues = new ArrayList<>();

    List<TextView> mSettingUiFlyDescs = new ArrayList<>();


    private void initView() {
        mSettingUiFlycCompassLy2 = findViewById(R.id.setting_ui_flyc_compass_ly2);

        mSettingUiFlycCompassLy3 = findViewById(R.id.setting_ui_flyc_compass_ly3);

        mSettingUiFlycTitles.add(findViewById(R.id.setting_ui_flyc_compass_1_txt));
        mSettingUiFlycTitles.add(findViewById(R.id.setting_ui_flyc_compass_2_txt));
        mSettingUiFlycTitles.add(findViewById(R.id.setting_ui_flyc_compass_3_txt));


        mSettingUiFlyValues.add(findViewById(R.id.setting_ui_flyc_compass_1_progress));
        mSettingUiFlyValues.add(findViewById(R.id.setting_ui_flyc_compass_2_progress));
        mSettingUiFlyValues.add(findViewById(R.id.setting_ui_flyc_compass_3_progress));

        mSettingUiFlyDescs.add(findViewById(R.id.setting_ui_flyc_compass_1_desc));
        mSettingUiFlyDescs.add(findViewById(R.id.setting_ui_flyc_compass_2_desc));
        mSettingUiFlyDescs.add(findViewById(R.id.setting_ui_flyc_compass_3_desc));

         findViewById(R.id.setting_menu_compass_calibrate).setOnClickListener(view -> startCompassCalibrating());

    }

    private boolean mAreMotorOn;
    private boolean mSimulatorStarted;
    private List<CompassState> mCompassStateList;
    private RedundancySensorUsedStateMsg mSensorUsedState;
    private CompassCalibrationDialog mCalibrationDialog;

    public CompassStatusWidget(Context context) {
        this(context, null);
    }

    public CompassStatusWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CompassStatusWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        inflate(context, R.layout.uxsdk_setting_menu_compass_status_layout, this);
        initView();
        mSettingUiFlycTitles.get(0).setSelected(true);
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(isInEditMode()) {
            return;
        }

        KeyManager.getInstance().getValue(KeyTools.createKey(ProductKey.KeyProductType), new CommonCallbacks.CompletionCallbackWithParam<ProductType>() {
            @Override
            public void onSuccess(ProductType productType) {
                updateViewsByState();
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                //add log
            }
        });


        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyCompassState), this, (oldValue, newValue) -> {
            mCompassStateList = newValue;
            updateViewsByState();
        });


        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyRedundancySensorUsedState), this, (oldValue, newValue) -> {
            mSensorUsedState = newValue;
            updateSelectedSensor(mSensorUsedState.getMagIndex());
        });


        //是否起浆
        KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyAreMotorsOn), new CommonCallbacks.CompletionCallbackWithParam<Boolean>() {
            @Override
            public void onSuccess(Boolean isOn) {
                mAreMotorOn = isOn;
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                //add log
            }
        });
        //是否开启模拟器
        mSimulatorStarted = SimulatorManager.getInstance().isSimulatorEnabled();

        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyIsCompassCalibrating), this, (oldValue, newValue) -> {
            if (newValue != null && newValue) {
                showCompassCalibrationDialog(getContext());
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyManager.getInstance().cancelListen(this);
    }

    protected void startCompassCalibrating() {

        if (!KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection) ,false)) {
            ViewUtil.showToast(getContext() , R.string.uxsdk_app_check_aircraft_connection , Toast.LENGTH_SHORT);
            return;
        }
        if (mAreMotorOn) {
            ViewUtil.showToast(getContext() ,R.string.uxsdk_setting_ui_redundance_compass_cal_tip , Toast.LENGTH_SHORT);
        } else if (mSimulatorStarted) {
            ViewUtil.showToast(getContext() ,R.string.uxsdk_setting_ui_compass_cal_in_sim_tip , Toast.LENGTH_SHORT);
        } else if (!isFastClick(R.id.setting_menu_compass_calibrate, 500)) {
            KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStartCompassCalibration), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    ViewUtil.showToast(getContext() , R.string.uxsdk_setting_ui_flyc_cali_begin , Toast.LENGTH_SHORT);
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    ViewUtil.showToast(getContext() , R.string.uxsdk_setting_ui_flyc_cali_failed , Toast.LENGTH_SHORT);
                }
            });
        }




    }

    private void updateSelectedSensor(int index) {
        for (int i = 0; i < THREE_COMPASS; ++i) {
            mSettingUiFlycTitles.get(i).setSelected(index == (i+1));
        }
    }

    private void updateViewsByState() {
        if (mCompassStateList == null) {
            return;
        }
        for (int i = 0; i < mCompassStateList.size(); i++) {
            CompassState compassState = mCompassStateList.get(i);
            ProgressStatusWidget statusView = mSettingUiFlyValues.get(i);
            TextView tvDesc = mSettingUiFlyDescs.get(i);
            updateSettingUiFlyCompassLy(i, compassState);
            updateCompassStateView(compassState, statusView);

            int compassSensorStateValue = compassState.getCompassSensorState() == CompassSensorState.UNKNOWN
                    ? 0 : compassState.getCompassSensorState().value();
            if (compassSensorStateValue >= 0 && compassSensorStateValue < mCompassStasActionStrIds.length) {
                tvDesc.setText(mCompassStasActionStrIds[compassSensorStateValue]);
            }

            if (compassState.getCompassSensorValue() != -1) {
                statusView.setValue(String.valueOf(compassState.getCompassSensorValue()));
                statusView.mProgressBar.setProgress((int) (compassState.getCompassSensorValue() * 1.0f / 999 * 100));
            } else {
                updateCompassSensorErrorStateView(statusView, tvDesc, compassSensorStateValue);
            }

            if (TextUtils.isEmpty(tvDesc.getText())){
                tvDesc.setVisibility(GONE);
            }
        }
        // 一个指南针的情况下内置指南针显示为指南针，多个指南针的情况下内置指南针显示为指南针1
        if (mSettingUiFlycCompassLy2.getVisibility() == VISIBLE || mSettingUiFlycCompassLy3.getVisibility() == VISIBLE) {
            mSettingUiFlycTitles.get(0).setText(R.string.uxsdk_setting_ui_redundancy_sensor_compass1_label);
        } else {
            mSettingUiFlycTitles.get(0).setText(R.string.uxsdk_setting_ui_redundancy_sensor_compass);
        }
    }

    private void updateSettingUiFlyCompassLy(int i, CompassState compassState) {
        boolean visible = compassState.getCompassSensorState() != CompassSensorState.DISCONNECTED;
        if (i == 1) {
            mSettingUiFlycCompassLy2.setVisibility(visible ? VISIBLE : GONE);
        } else if (i == 2) {
            mSettingUiFlycCompassLy3.setVisibility(visible ? VISIBLE : GONE);
        }
    }

    private void updateCompassStateView(CompassState compassState, ProgressStatusWidget statusView) {
        if (compassState.getCompassSensorState() == CompassSensorState.NORMAL_MODULUS) {
            statusView.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.uxsdk_setting_ui_status_pgb_green));
        } else if (compassState.getCompassSensorState() == CompassSensorState.WEAK_MODULUS) {
            statusView.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.uxsdk_setting_ui_status_pgb_yellow));
        } else if (compassState.getCompassSensorState() == CompassSensorState.SERIOUS_MODULUS) {
            statusView.setProgressDrawable(ContextCompat.getDrawable(getContext(), R.drawable.uxsdk_setting_ui_status_pgb_red));
        }

        statusView.mProgressBar.setVisibility(View.VISIBLE);
        statusView.mValueView.setVisibility(View.VISIBLE);
    }

    private void updateCompassSensorErrorStateView(ProgressStatusWidget statusView, TextView tvDesc, int compassSensorStateValue) {
        if (compassSensorStateValue >= 0 && compassSensorStateValue < mCompassStasActionStrIds.length) {
            tvDesc.setText(mCompassStasActionStrIds[compassSensorStateValue]);
            tvDesc.setVisibility(View.VISIBLE);
            if (compassSensorStateValue == 1) {
                statusView.mProgressBar.setVisibility(View.GONE);
                statusView.mValueView.setVisibility(View.GONE);
            }
        } else {
            tvDesc.setVisibility(View.GONE);
        }
    }
    private  SparseLongArray sClickTimes = new SparseLongArray();
    public  boolean isFastClick(int viewId, int duration) {
        long prevTime = sClickTimes.get(viewId);
        long now = System.currentTimeMillis();
        boolean isFast = now - prevTime < duration;
        if (!isFast) {
            sClickTimes.put(viewId, now);
        }
        return isFast;
    }

    public void showCompassCalibrationDialog(Context context) {
        if (mCalibrationDialog == null) {
            mCalibrationDialog = new CompassCalibrationDialog(context);
            mCalibrationDialog.setOnDismissListener(dialog -> mCalibrationDialog = null);
        }

        if (! mCalibrationDialog.isShowing()) {
            mCalibrationDialog.show();
        }
    }
}
