package dji.v5.ux.remotecontroller.calibration;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;


import java.util.ArrayList;


import dji.sdk.keyvalue.value.remotecontroller.RcCalibrateState;
import dji.v5.utils.common.LogUtils;
import dji.v5.utils.common.StringUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.ViewUtil;
import dji.v5.ux.remotecontroller.RcCalibrationWidgetModel;
import dji.v5.ux.remotecontroller.calibration.stick.StickCalibrationView;
import dji.v5.ux.remotecontroller.calibration.wheel.WheelCalibrationView;

/**
 * Created by richard.liao on 2018/5/30:20:55
 */

public class SmartControllerCalibrationView extends LinearLayout implements OnCalibrationListener, View.OnClickListener {
    Button mInteractionBtn;
    protected Button outerInteractionBtn;

    ViewPager mViewPager;
    protected ArrayList<View> mViewList;

    CalibrationRadioButton mRgStick;
    CalibrationRadioButton mRgWheel;
    protected StickCalibrationView mStickCalibrationView;
    protected WheelCalibrationView mWheelCalibrationView;
    protected RcCalibrationWidgetModel rcCalibrationWidgetModel;

    private RcCalibrateState mCalibrationMode = RcCalibrateState.UNKNOWN;
    private boolean isStartCalibration = false;

    public SmartControllerCalibrationView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (isInEditMode()) {
            return;
        }
        init();
    }

    private void init() {
        if (isInEditMode()) {
            return;
        }

        rcCalibrationWidgetModel = new RcCalibrationWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());
        mInteractionBtn = findViewById(R.id.fpv_rcsetting_cele_btn);
        mViewPager = findViewById(R.id.calibration_pages);
        mRgStick = findViewById(R.id.setting_ui_rc_calibration_stick_rg);
        mRgWheel = findViewById(R.id.setting_ui_rc_calibration_wheel_rg);

        mInteractionBtn.setOnClickListener(startCalibrationListener);

        mRgStick.setOnClickListener(this);
        mRgWheel.setOnClickListener(this);

        mStickCalibrationView =
                (StickCalibrationView) LayoutInflater.from(getContext()).inflate(R.layout.uxsdk_setting_ui_rc_smart_controller_stick_calibration_view, null);
        mWheelCalibrationView =
                (WheelCalibrationView) LayoutInflater.from(getContext()).inflate(R.layout.uxsdk_setting_ui_rc_smart_controller_wheel_calibration_view, null);
        mStickCalibrationView.setListener(this);
        mStickCalibrationView.setViewModel(rcCalibrationWidgetModel);
        mWheelCalibrationView.setListener(this);
        mWheelCalibrationView.setViewModel(rcCalibrationWidgetModel);

        // view Pager
        mViewList = new ArrayList<>(2);
        mViewList.add(mStickCalibrationView);
        mViewList.add(mWheelCalibrationView);

        mViewPager.setOffscreenPageLimit(1);
        mViewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return mViewList.size();
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                View targetView = mViewList.get(position);
                if (targetView.getParent() != null) {
                    ((ViewGroup) targetView.getParent()).removeView(targetView);
                }
                container.addView(targetView);
                return mViewList.get(position);
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView(mViewList.get(position));
            }
        });
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //do nothing
            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    mRgStick.setChecked(true);
                } else {
                    mRgWheel.setChecked(true);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                //do nothing
            }
        });
        mViewPager.setCurrentItem(0);

        rcCalibrationWidgetModel.rcCalibrateStateDataProcessor.toFlowableOnUI().subscribe(rcCalibrateState -> {
            mCalibrationMode = rcCalibrateState;
            updateViewByMode();
            if (mCalibrationMode == RcCalibrateState.UNKNOWN && !isStartCalibration) {
                reset();
            }
        });
        rcCalibrationWidgetModel.isCalibrateStartProcessor.toFlowableOnUI().subscribe(aBoolean -> {
            isStartCalibration = aBoolean;
            if (mCalibrationMode == RcCalibrateState.UNKNOWN && aBoolean) {
                reset();
            }
        });

    }


    @Override
    public void onCalibrationFinished(int partType) {
        LogUtils.e("Calibration", "partType = " + partType + " Done");
        if (partType == OnCalibrationListener.TYPE_STICK) {
            mRgStick.setCalirationComplete(true);
            if (!mWheelCalibrationView.isCalibrated()) {
                mViewPager.setCurrentItem(1);
            }
        } else if (partType == OnCalibrationListener.TYPE_WHEEL) {
            mRgWheel.setCalirationComplete(true);
            if (!mStickCalibrationView.isCalibrated()) {
                mViewPager.setCurrentItem(0);
            }
        }
        updateViewByMode();
    }

    @Override
    public void onCalibrationCanceled(int partType) {
        LogUtils.e("Calibration", "partType = " + partType + " Canceled");
        if (partType == OnCalibrationListener.TYPE_STICK) {
            mRgStick.setCalirationComplete(false);
        } else if (partType == OnCalibrationListener.TYPE_WHEEL) {
            mRgWheel.setCalirationComplete(false);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.setting_ui_rc_calibration_stick_rg) {
            mViewPager.setCurrentItem(0);
        } else if (v.getId() == R.id.setting_ui_rc_calibration_wheel_rg) {
            mViewPager.setCurrentItem(1);
        }
    }

    private void updateViewByMode() {
        final RcCalibrateState mode = mCalibrationMode;
        LogUtils.e("Calibration", "mode = " + mode);
        if (mode == RcCalibrateState.NORMAL || mode == RcCalibrateState.UNKNOWN) {
            mInteractionBtn.setEnabled(true);
            mInteractionBtn.setText(R.string.uxsdk_setting_ui_rc_cele);
            if (outerInteractionBtn != null) {
                outerInteractionBtn.setEnabled(true);
                outerInteractionBtn.setText(R.string.uxsdk_setting_ui_rc_cele);
            }
        } else if (mode == RcCalibrateState.RECORDCENTER) {
            mInteractionBtn.setEnabled(false);
            mInteractionBtn.setText(R.string.uxsdk_setting_ui_rc_cele_in_progress_hint);
            if (outerInteractionBtn != null) {
                outerInteractionBtn.setText(R.string.uxsdk_setting_ui_rc_cele_in_progress_hint);
                outerInteractionBtn.setEnabled(false);
            }

        } else if (mode == RcCalibrateState.LIMITVALUE) {
            mInteractionBtn.setText(R.string.uxsdk_setting_ui_rc_cele_in_progress_hint);
            mInteractionBtn.setEnabled(false);
            if (mStickCalibrationView.isCalibrated() && mWheelCalibrationView.isCalibrated()) {
                mInteractionBtn.setEnabled(true);
            }
            if (outerInteractionBtn != null) {
                outerInteractionBtn.setText(R.string.uxsdk_setting_ui_rc_cele_in_progress_hint);
                outerInteractionBtn.setEnabled(false);
                if (mStickCalibrationView.isCalibrated() && mWheelCalibrationView.isCalibrated()) {
                    outerInteractionBtn.setEnabled(true);
                }
            }

        } else if (mode == RcCalibrateState.TIMEOUT_EXIT) {
            String content = StringUtils.getResStr(R.string.uxsdk_setting_ui_rc_calibration_time_out);
            ViewUtil.showToast(getContext(),content);


        } else if (mode == RcCalibrateState.EXIT) {
            mInteractionBtn.setText(R.string.uxsdk_setting_ui_rc_finish);
            mInteractionBtn.setEnabled(true);
            if (outerInteractionBtn != null) {
                outerInteractionBtn.setText(R.string.uxsdk_setting_ui_rc_finish);
                outerInteractionBtn.setEnabled(true);
            }
        }
    }

    private void reset() {
        mCalibrationMode = RcCalibrateState.UNKNOWN;
        mStickCalibrationView.reset();
        mWheelCalibrationView.reset();
        mViewPager.setCurrentItem(0);
    }

    private OnClickListener startCalibrationListener = view -> {
        if (mCalibrationMode == RcCalibrateState.UNKNOWN) {
            rcCalibrationWidgetModel.startCalibration();
        } else if (mCalibrationMode == RcCalibrateState.EXIT) {
            rcCalibrationWidgetModel.finishCalibration();
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            rcCalibrationWidgetModel.setup();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!isInEditMode()) {
            rcCalibrationWidgetModel.cleanup();
        }

    }

}
