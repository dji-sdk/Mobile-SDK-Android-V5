package dji.v5.ux.remotecontroller.calibration.stick;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.RemoteControllerKey;
import dji.sdk.keyvalue.value.remotecontroller.RcCalibrateState;
import dji.sdk.keyvalue.value.remotecontroller.RemoteControllerType;
import dji.v5.manager.KeyManager;
import dji.v5.utils.dpad.DpadProductManager;
import dji.v5.ux.R;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.ux.remotecontroller.RcCalibrationWidgetModel;
import dji.v5.ux.remotecontroller.calibration.IRcCalibrationView;
import dji.v5.ux.remotecontroller.calibration.OnCalibrationListener;
import io.reactivex.rxjava3.disposables.CompositeDisposable;


/**
 * Created by richard.liao on 2018/5/30:20:55
 */

public class StickCalibrationView extends LinearLayout implements IRcCalibrationView {

    private static final int ITEM_LEFT = 0;
    private static final int ITEM_RIGHT = 1;

    private static final int MAX_CALIBRATION_STATUS = 32767;
    protected RcCalibrationWidgetModel rcCalibrationWidgetModel;

    LinearLayout mSmartControllerLayout;
    HallStickCalibrationView mLeftJoystick;
    HallStickCalibrationView mRightJoystick;


    LinearLayout mRcPlusLayout;
    RcCalibrationRollView mRcPlusLeftJoystick;
    RcCalibrationRollView mRcPlusRightJoystick;
    TextView mRcCalibrationDes;

    private boolean mIsStickCalibrated = false;
    private boolean isDjiRcPlus = false;

    private OnCalibrationListener mListener = null;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    public StickCalibrationView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    private void init() {
        if (isInEditMode()) {
            return;
        }
        mSmartControllerLayout = findViewById(R.id.ll_rc_smart_controller);
        mLeftJoystick = findViewById(R.id.mavic_rc_calibration_left_joystick_square);
        mRightJoystick = findViewById(R.id.mavic_rc_calibration_right_joystick_square);
        mRcPlusLayout = findViewById(R.id.ll_rc_puls);
        mRcPlusLeftJoystick = findViewById(R.id.rc_plus_calibration_left_joystick_square);
        mRcPlusRightJoystick = findViewById(R.id.rc_plus_calibration_right_joystick_square);
        mRcCalibrationDes = findViewById(R.id.tv_rc_calibration_des);


        if (isDjiRcPlus() || isDjiRcPro()) {
            isDjiRcPlus = true;
        }

        if (isDjiRcPlus) {
            mRcCalibrationDes.setText(R.string.uxsdk_setting_ui_rc_plus_calibration_1);
            mSmartControllerLayout.setVisibility(View.GONE);
            mRcPlusLayout.setVisibility(View.VISIBLE);
        } else {
            mSmartControllerLayout.setVisibility(View.VISIBLE);
            mRcPlusLayout.setVisibility(View.GONE);
        }

    }

    private void listenSmartControllerCalibrationState() {
        mCompositeDisposable.add(rcCalibrationWidgetModel.rcCalibrateStateDataProcessor.toFlowable().subscribeOn(SchedulerProvider.ui()).subscribe(state -> {
            if (isDjiRcPlus && state == RcCalibrateState.RECORDCENTER) {
                mRcPlusLeftJoystick.resetLimit();
                mRcPlusRightJoystick.resetLimit();
                mIsStickCalibrated = true;
            }

            if (state == RcCalibrateState.TIMEOUT_EXIT) {
                reset();
            }
        }));

        mCompositeDisposable.add(rcCalibrationWidgetModel.connectDataProcessor.toFlowable().subscribe(connect -> {
            if (connect != null && !connect) {
                reset();
            }
        }));

    }

    private void listenStickState() {
        rcCalibrationWidgetModel.stickStateDataProcessor.toFlowableOnUI().subscribe(stickState -> {
            if (stickState.calibrationState == RcCalibrateState.TIMEOUT_EXIT || !stickState.isConnection) {
                reset();
                return;
            }
            if (isCalibrated()) {
                return;
            }
            onStickState(stickState);
        });
    }

    private void onStickState(StickState state) {
        if (!isDjiRcPlus) {
            mLeftJoystick.setSegmentNum(state.segmentNum);
            mRightJoystick.setSegmentNum(state.segmentNum);

            if (mLeftJoystick.hasSegNumSet() && mRightJoystick.hasSegNumSet() && state.calibrationState == RcCalibrateState.LIMITVALUE) {
                mRightJoystick.setProgress(state.rightLeft, state.rightTop, state.rightRight, state.rightBottom);
                mLeftJoystick.setProgress(state.leftLeft, state.leftTop, state.leftRight, state.leftBottom);
            }
        }

        if (state.rightLeft == MAX_CALIBRATION_STATUS && state.rightTop == MAX_CALIBRATION_STATUS
                && state.rightRight == MAX_CALIBRATION_STATUS && state.rightBottom == MAX_CALIBRATION_STATUS
                && state.leftLeft == MAX_CALIBRATION_STATUS && state.leftTop == MAX_CALIBRATION_STATUS
                && state.leftRight == MAX_CALIBRATION_STATUS && state.leftBottom == MAX_CALIBRATION_STATUS
        ) {
            mIsStickCalibrated = true;
            if (isCalibrated() && mListener != null) {
                mListener.onCalibrationFinished(OnCalibrationListener.TYPE_STICK);
            }

        }
    }


    private void updateByStop() {
        updateStick(ITEM_LEFT, 0, 0);
        updateStick(ITEM_RIGHT, 0, 0);
    }

    private void updateStick(final int item, final int vertical, final int horizontal) {
        final int vPgb = transformValue(vertical);
        final int hPgb = transformValue(horizontal);

        if (isDjiRcPlus) {
            RcCalibrationRollView stickView = null;
            if (ITEM_LEFT == item) {
                stickView = mRcPlusLeftJoystick;
            } else {
                stickView = mRcPlusRightJoystick;
            }

            stickView.setProgress(hPgb, vPgb);
            if (mRcPlusLeftJoystick.isGetLimit() && mRcPlusRightJoystick.isGetLimit()) {
                updateViewByMode(RcCalibrateState.LIMITVALUE);
            }
        } else {

            HallStickCalibrationView stickView = null;
            if (ITEM_LEFT == item) {
                stickView = mLeftJoystick;
            } else {
                stickView = mRightJoystick;
            }

            stickView.setCircleCenter(getAbsValue(hPgb),
                    Math.max(vPgb, 0),
                    Math.max(hPgb, 0),
                    getAbsValue(vPgb));
        }
    }

    private int getAbsValue(int value) {
        return value < 0 ? -value : 0;
    }

    private int transformValue(final int value) {
        //CSDK 值范围为[-660,660]
        return (int) (value / 6.6f);
    }

    @Override
    protected void onDetachedFromWindow() {
        updateByStop();
        mCompositeDisposable.dispose();

        super.onDetachedFromWindow();
    }


    @Override
    public void updateViewByMode(RcCalibrateState mode) {
        if (mIsStickCalibrated && mListener != null) {
            mIsStickCalibrated = false;
            mListener.onCalibrationFinished(OnCalibrationListener.TYPE_STICK);
        }
    }

    @Override
    public void setListener(OnCalibrationListener listener) {
        mListener = listener;
    }

    public void setViewModel(RcCalibrationWidgetModel rcCalibrationWidgetModel) {
        this.rcCalibrationWidgetModel = rcCalibrationWidgetModel;

        mCompositeDisposable.add(rcCalibrationWidgetModel.leftStickValueDataProcessor.toFlowableOnUI().subscribe(stickValue -> updateStick(ITEM_LEFT, stickValue.vertical,
                stickValue.horizontal)));

        mCompositeDisposable.add(rcCalibrationWidgetModel.rightStickValueDataProcessor.toFlowableOnUI().subscribe(stickValue -> updateStick(ITEM_RIGHT, stickValue.vertical,
                stickValue.horizontal)));

        listenSmartControllerCalibrationState();
        listenStickState();
    }

    @Override
    public void removeListener() {
        mListener = null;
    }

    @Override
    public boolean isCalibrated() {
        return mIsStickCalibrated;
    }

    @Override
    public void reset() {
        mLeftJoystick.reset();
        mRightJoystick.reset();
        mIsStickCalibrated = false;
        mRcPlusLeftJoystick.resetLimit();
        mRcPlusRightJoystick.resetLimit();
        if (mListener != null) {
            mListener.onCalibrationCanceled(OnCalibrationListener.TYPE_STICK);
        }
    }

    private boolean isDjiRcPlus() {
        if (DpadProductManager.getInstance().isDjiRcPlus()) {
            return true;
        }
        return KeyManager.getInstance().getValue(KeyTools.createKey(RemoteControllerKey.KeyRemoteControllerType), RemoteControllerType.UNKNOWN) == RemoteControllerType.DJI_RC_PLUS;
    }

    private boolean isDjiRcPro() {
        if (DpadProductManager.getInstance().isSmartController()) {
            return true;
        }
        return KeyManager.getInstance().getValue(KeyTools.createKey(RemoteControllerKey.KeyRemoteControllerType), RemoteControllerType.UNKNOWN) == RemoteControllerType.DJI_RC_PRO;
    }


}
