package dji.v5.ux.core.base;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;



import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.flightcontroller.IMUSensorState;
import dji.sdk.keyvalue.value.flightcontroller.IMUState;
import dji.sdk.keyvalue.value.flightcontroller.RedundancySensorUsedStateMsg;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.util.ViewUtil;

/**
 * <p> </p>
 *
 * @author Create by Luca.Wu@dji.com at 2017/11/27 14:39
 * @version v1.0
 */

public class ImuStatusWidget extends ConstraintLayout  {

    private static final String TAG = "ImuStatusWidget";
    private int[] IMU_STATE_RES = new int[]{
            R.string.uxsdk_setting_ui_redundancy_sensor_imu_stat_1,
            R.string.uxsdk_setting_ui_redundancy_sensor_imu_stat_2,
            R.string.uxsdk_setting_ui_redundancy_sensor_imu_stat_3,
            R.string.uxsdk_setting_ui_redundancy_sensor_imu_stat_4,
            R.string.uxsdk_setting_ui_redundancy_sensor_imu_stat_5,
            R.string.uxsdk_setting_ui_redundancy_sensor_imu_stat_6,
            R.string.uxsdk_setting_ui_redundancy_sensor_imu_stat_0,
    };

    private static final int ONE_IMU = 1;
    private static final int TWO_IMU = 2;
    private static final int THREE_IMU = 3;

    private Group mSettingUiFlycImuLy2;
    private Group mSettingUiFlycImuLy3;

    private List<TextView> mSettingUiFlycAccTitles = new ArrayList<>();

    private List<ProgressStatusWidget>
        mSettingUiFlycImuAccs = new ArrayList<>();

    private List<ProgressStatusWidget>
        mSettingUiFlycImuGyros = new ArrayList<>();


    private void initView(View view) {
        mSettingUiFlycImuLy2 = view.findViewById(R.id.setting_ui_flyc_imu_ly2);
        mSettingUiFlycImuLy3 = view.findViewById(R.id.setting_ui_flyc_imu_ly3);

        mSettingUiFlycAccTitles.add(view.findViewById(R.id.setting_ui_flyc_acc_0_title));
        mSettingUiFlycAccTitles.add(view.findViewById(R.id.setting_ui_flyc_acc_1_title));
        mSettingUiFlycAccTitles.add(view.findViewById(R.id.setting_ui_flyc_acc_2_title));

        mSettingUiFlycImuAccs.add(view.findViewById(R.id.setting_ui_flyc_imu_acc_1));
        mSettingUiFlycImuAccs.add(view.findViewById(R.id.setting_ui_flyc_imu_acc_2));
        mSettingUiFlycImuAccs.add(view.findViewById(R.id.setting_ui_flyc_imu_acc_3));

        mSettingUiFlycImuGyros.add(view.findViewById(R.id.setting_ui_flyc_imu_gyr_1));
        mSettingUiFlycImuGyros.add(view.findViewById(R.id.setting_ui_flyc_imu_gyr_2));
        mSettingUiFlycImuGyros.add(view.findViewById(R.id.setting_ui_flyc_imu_gyr_3));

        findViewById(R.id.setting_menu_imu_calibrate).setOnClickListener(view1 -> startCompassCalibrating());
    }
    //private KeyPresenter mPresenter;
//    private DJIKey mUsedStateKey;

    private int mImuCount;
    private List<IMUState> mIMUStateList = new ArrayList<>();
    private boolean mAreMotorOn;
    private RedundancySensorUsedStateMsg mSensorUsedState;

    public ImuStatusWidget(Context context) {
        this(context, null);
    }

    public ImuStatusWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImuStatusWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        View view = inflate(context, R.layout.uxsdk_setting_menu_imu_status_layout, this);
        initView(view);

        mSettingUiFlycAccTitles.get(0).setSelected(true);
        initKeys();
    }


    private void initKeys() {
//        mPresenter = new KeyModel.Builder().setView(this).build().create();
//        mUsedStateKey = FlightControllerKey.create(FlightControllerKey.REDUNDANCY_SENSOR_USED_STATE);

//        mPresenter.addKey(mUsedStateKey);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isInEditMode()) {
            return;
        }
        //mPresenter.onStart();


        KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyIMUCount), new CommonCallbacks.CompletionCallbackWithParam<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                LogUtils.d(TAG, "listen imu count=" + count);
                mImuCount = count;
                updateViewByImuCount();
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                LogUtils.d(TAG, "listen imu get error" );
            }
        });


        KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyAreMotorsOn), new CommonCallbacks.CompletionCallbackWithParam<Boolean>() {
            @Override
            public void onSuccess(Boolean isOn) {
                mAreMotorOn = isOn;
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                LogUtils.d(TAG, "get KeyAreMotorsOn error" );
            }
        });



        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyIMUStatus), this, (oldValue, newValue) -> {
            mIMUStateList = newValue;
            updateSensorsStatus(mSettingUiFlycImuAccs, false);
            updateSensorsStatus(mSettingUiFlycImuGyros, true);
        });

        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyRedundancySensorUsedState), this, (oldValue, newValue) -> {
            mSensorUsedState = newValue;
            updateSelectedSensor(mSensorUsedState.getGyroIndex());
        });

//
    }

    @Override
    protected void onDetachedFromWindow() {
        //mPresenter.onStop();
        KeyManager.getInstance().cancelListen(this);
        super.onDetachedFromWindow();
    }

//    @Override
//    public void transformValue(DJIKey key, Object value) {
//        if (key.equals(mUsedStateKey)) {
//            mSensorUsedState = (RedundancySensorUsedState) value;
//        }
//    }
//
//    @Override
//    public void updateWidget(DJIKey key, Object value) {
//        if (key.equals(mUsedStateKey)) {
//            updateSelectedSensor(mSensorUsedState.getGyroIndex());
//        }
//    }


    protected void startCompassCalibrating() {

        if (mAreMotorOn) {
            ViewUtil.showToast(getContext() ,R.string.uxsdk_setting_ui_imu_tip , Toast.LENGTH_SHORT);
        } else {
            if (KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection) ,false)) {
                ImuCalibrateDialog dialog = new ImuCalibrateDialog(getContext());
                dialog.show();

            } else {
                ViewUtil.showToast(getContext() , R.string.uxsdk_app_check_aircraft_connection , Toast.LENGTH_SHORT);
            }
        }
    }

    private void updateViewByImuCount() {
        if (mImuCount == TWO_IMU) {
            mSettingUiFlycImuLy3.setVisibility(View.GONE);
        } else if (mImuCount == ONE_IMU) {
            mSettingUiFlycImuLy2.setVisibility(View.GONE);
            mSettingUiFlycImuLy3.setVisibility(View.GONE);
        } else {
            mSettingUiFlycImuLy2.setVisibility(View.VISIBLE);
            mSettingUiFlycImuLy3.setVisibility(View.VISIBLE);
        }
    }

    private void updateSelectedSensor(int index) {
        for (int i = 0; i < THREE_IMU; ++i) {
            mSettingUiFlycAccTitles.get(i).setSelected(index == (i+1));
        }
    }

    private void updateSensorsStatus(List<ProgressStatusWidget> viewList, boolean isGyro) {
        if (mIMUStateList == null) {
            return;
        }
        for (int i = 0; i < mImuCount; ++i) {
            if (i >= mIMUStateList.size()) {
                break;
            }
            IMUState imuState = mIMUStateList.get(i);
            IMUSensorState state = isGyro ? imuState.getGyroscopeState() : imuState.getAccelerometerState();
            float value = isGyro ? imuState.getGyroscopeBias().floatValue() : imuState.getAccelerometerBias().floatValue();

            ProgressStatusWidget statusView = viewList.get(i);

            if (state == IMUSensorState.NORMAL_BIAS) {
                statusView.setProgressDrawable(getContext().getResources().getDrawable(R.drawable.uxsdk_setting_ui_status_pgb_green));
            } else if (state == IMUSensorState.MEDIUM_BIAS) {
                statusView.setProgressDrawable(getContext().getResources().getDrawable(R.drawable.uxsdk_setting_ui_status_pgb_yellow));
            } else if (state == IMUSensorState.LARGE_BIAS) {
                statusView.setProgressDrawable(getContext().getResources().getDrawable(R.drawable.uxsdk_setting_ui_status_pgb_red));
            }

            statusView.mProgressBar.setVisibility(View.VISIBLE);
            statusView.mValueView.setVisibility(View.VISIBLE);
            //
            updateStatus(state , statusView , isGyro , value);

        }
    }

    private  void updateStatus(IMUSensorState state , ProgressStatusWidget statusView , boolean isGyro , float value ){
        if (state ==  IMUSensorState.NORMAL_BIAS || state ==  IMUSensorState.MEDIUM_BIAS || state ==  IMUSensorState.LARGE_BIAS) {
            statusView.setValue(String.format(Locale.US, "%.3f", value));
            if (isGyro) {
                statusView.setProgress((int) (value / 0.05f * 100));
            } else {
                statusView.setProgress((int) (value / 0.1f * 100));
            }
            statusView.mDescView.setVisibility(View.GONE);
        } else {
            statusView.mProgressBar.setProgress(0);
            if (state != null && state.ordinal() >= 0 && state.ordinal() <= IMUSensorState.IN_MOTION.ordinal()) {
                statusView.mDescView.setText(IMU_STATE_RES[state.ordinal()]);
                statusView.mDescView.setVisibility(View.VISIBLE);
                statusView.mProgressBar.setVisibility(View.GONE);
                statusView.mValueView.setVisibility(View.GONE);
            } else {
                statusView.mDescView.setVisibility(View.GONE);
            }
        }
    }
}
