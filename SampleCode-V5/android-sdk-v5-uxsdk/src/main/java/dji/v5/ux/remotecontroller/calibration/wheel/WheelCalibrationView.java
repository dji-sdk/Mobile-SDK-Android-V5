package dji.v5.ux.remotecontroller.calibration.wheel;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import dji.sdk.keyvalue.value.remotecontroller.RcCalibrateState;
import dji.v5.ux.R;
import dji.v5.ux.remotecontroller.RcCalibrationWidgetModel;
import dji.v5.ux.remotecontroller.calibration.DJICalProgressBar;
import dji.v5.ux.remotecontroller.calibration.IRcCalibrationView;
import dji.v5.ux.remotecontroller.calibration.OnCalibrationListener;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Created by richard.liao on 2018/5/30:20:55
 */

public class WheelCalibrationView extends LinearLayout implements IRcCalibrationView {

    protected RcCalibrationWidgetModel rcCalibrationWidgetModel;

    private static final int ITEM_LEFT = 0;
    private static final int ITEM_RIGHT = 1;

    protected DJICalProgressBar mLeftWheelPgb;
    protected DJICalProgressBar mRightWheelPgb;

    private RcCalibrateState mCalibrationModeNow = RcCalibrateState.UNKNOWN;


    private OnCalibrationListener mListener = null;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    public WheelCalibrationView(Context context, AttributeSet attrs) {
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


        mLeftWheelPgb = findViewById(R.id.fpv_rcsetting_cele_item_pgb);
        mRightWheelPgb = findViewById(R.id.fpv_rcsetting_cele_item_right_wheel_pgb);


        mLeftWheelPgb.setValue(0, 0);
        mRightWheelPgb.setValue(0, 0);


    }

    private int transformValue(final int value) {
        //CSDK波轮范围为[-660,660]
        return (int) (value / 6.6f);
    }

    private void updateWheel(final int item, final int gyro) {
        final int gPgb = transformValue(gyro);

        DJICalProgressBar pgb;
        if (ITEM_LEFT == item) {
            pgb = mLeftWheelPgb;
        } else {
            pgb = mRightWheelPgb;
        }

        pgb.setValue(gPgb < 0 ? -gPgb : 0, gPgb > 0 ? gPgb : 0);
    }


    @Override
    public void updateViewByMode(RcCalibrateState mode) {
        mCalibrationModeNow = mode;
        if (isCalibrated() && mListener != null) {
            mListener.onCalibrationFinished(OnCalibrationListener.TYPE_WHEEL);
        }
    }

    @Override
    public void setListener(OnCalibrationListener listener) {
        mListener = listener;
    }

    public void setViewModel(RcCalibrationWidgetModel rcCalibrationWidgetModel) {
        this.rcCalibrationWidgetModel = rcCalibrationWidgetModel;
        mCompositeDisposable.add(rcCalibrationWidgetModel.calibrationInfoDataProcessor.toFlowableOnUI().subscribe(smartControllerCalibrationInfo -> {
            updateWheel(ITEM_LEFT, smartControllerCalibrationInfo.leftGyroValue);
            updateWheel(ITEM_RIGHT, smartControllerCalibrationInfo.rightGyroValue);

            if (isCalibrated()) {
                return;
            }

            if (RcCalibrateState.LIMITVALUE == smartControllerCalibrationInfo.calibrationState
                    || RcCalibrateState.EXIT == smartControllerCalibrationInfo.calibrationState) {
                if (isCalibrated() && mListener != null) {
                    mListener.onCalibrationFinished(OnCalibrationListener.TYPE_WHEEL);
                }

                updateViewByMode(smartControllerCalibrationInfo.calibrationState);
            }
        }));
    }

    @Override
    public void removeListener() {
        mListener = null;
    }

    @Override
    public void reset() {
        mCalibrationModeNow = RcCalibrateState.UNKNOWN;
        if (mListener != null) {
            mListener.onCalibrationCanceled(OnCalibrationListener.TYPE_WHEEL);
        }
    }

    @Override
    public boolean isCalibrated() {
        return mCalibrationModeNow == RcCalibrateState.EXIT;
    }

    @Override
    protected void onDetachedFromWindow() {
        mCompositeDisposable.dispose();
        super.onDetachedFromWindow();
    }

}
