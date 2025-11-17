package dji.v5.ux.core.base;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;


import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.utils.ProductUtil;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.sdk.keyvalue.value.flightcontroller.CompassCalibrationState;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.utils.common.AndUtil;
import dji.v5.ux.R;
import dji.v5.ux.core.ui.setting.dialog.BaseDialog;

import dji.v5.ux.core.util.ViewUtil;


/**
 * Compass校准弹窗
 *
 */

public class CompassCalibrationDialog extends BaseDialog implements View.OnClickListener {

    private View rootView;

    private boolean isCalibrating = true;

    private CompassCalibrationState mLastState = CompassCalibrationState.UNKNOWN;
    /**
     * 指南针校准是否正常结束
     */
    private boolean mFinish;

    private TextView mTitleTv;
    private TextView mStepTv;
    private ImageView mIllustationIv;
    private View mBottomDivider;
    private TextView mRightBtn;
    private TextView mLeftBtn;


    private void initView(View view ) {
        mTitleTv = view.findViewById(R.id.compass_calibration_title);
        mStepTv = view.findViewById(R.id.compass_calibration_step);
        mIllustationIv = view.findViewById(R.id.compass_calibration_illustration);
        mBottomDivider = view.findViewById(R.id.compass_calibration_bottom_divider);
        mRightBtn = view.findViewById(R.id.compass_calibration_right_btn);
        mRightBtn.setOnClickListener(this);
        mLeftBtn = view.findViewById(R.id.compass_calibration_left_btn);
        mLeftBtn.setOnClickListener(this);
    }

    private static int[] aircraftCompassIconM30 = new int[] {R.drawable.uxsdk_fpv_compass_horizontal_m30, R.drawable.uxsdk_fpv_compass_vertical_m30};
    private static int[] aircraftCompassIconM300 = new int[] {R.drawable.uxsdk_fpv_compass_horizontal_m300, R.drawable.uxsdk_fpv_compass_vertical_m300};
    private static int[] aircraftCompassIconM3 = new int[] {R.drawable.uxsdk_fpv_compass_horizontal_m3e, R.drawable.uxsdk_fpv_compass_vertical_m3e};

    public CompassCalibrationDialog(@NonNull Context context) {
        this(context, R.style.NoTitleDialog);
    }

    public CompassCalibrationDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
        initialize(context);
    }

    protected CompassCalibrationDialog(@NonNull Context context, boolean cancelable, @Nullable DialogInterface.OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        initialize(context);
    }

    private void initialize(Context context) {
        rootView = LayoutInflater.from(context).inflate(R.layout.uxsdk_dialog_compass_calibration, null);
        initView(rootView);

        // 初始化先显示水平的图片
        updateNoticeImage(getIllustrationResIdByAircraft(CompassCalibrationState.HORIZONTAL));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(rootView);
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }
    
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();


        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyIsCompassCalibrating), this, (oldValue, newValue) -> {
            isCalibrating = newValue != null;
            checkCompassCalibrationState();
        });


        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyCompassCalibrationStatus), this, (oldValue, state) -> {
            mFinish = state == CompassCalibrationState.SUCCEEDED || state == CompassCalibrationState.FAILED;
            mLastState = state;
            if (state != CompassCalibrationState.UNKNOWN && state != CompassCalibrationState.IDLE) {
                updateViews(state);
            } else {
                checkCompassCalibrationState();
            }
        });



        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyConnection), this, (oldValue, connection) -> {
            if (connection != null && !connection) {
                dismiss();
            }
        });

    }

    /**
     * 非正常结束指南针校准时，dismiss 该 dialog（如拨动遥控器档位开关）
     */
    private void checkCompassCalibrationState() {
        if (!mFinish && !isCalibrating && mLastState == CompassCalibrationState.IDLE) {
            dismiss();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyManager.getInstance().cancelListen(this);
    }

    @Override
    public int getDialogWidth() {
        return (int) getContext().getResources().getDimension(R.dimen.uxsdk_dialog_update_width);
    }

    @Override
    public int getDialogMaxHeight() {
        return AndUtil.getLandScreenHeight(getContext());
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.compass_calibration_left_btn) {
            if (isCalibrating) {
                KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStopCompassCalibration), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                    @Override
                    public void onSuccess(EmptyMsg emptyMsg) {
                        //do something
                    }

                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        // add log
                    }
                });
            }
            dismiss();
        } else {
            KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStopCompassCalibration), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    startCali();
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    // add log
                }
            });
        }
    }


    private void startCali(){
        KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStartCompassCalibration), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
            @Override
            public void onSuccess(EmptyMsg emptyMsg) {
                //ViewUtil.showToast(getContext() , R.string.uxsdk_setting_ui_flyc_cali_begin , Toast.LENGTH_SHORT);
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                ViewUtil.showToast(getContext() , R.string.uxsdk_app_check_aircraft_connection , Toast.LENGTH_SHORT);
            }
        });
    }
    private void updateViews(CompassCalibrationState status) {
        String title;

        if (status == CompassCalibrationState.HORIZONTAL) {
            title = getContext().getString(R.string.uxsdk_fpv_checklist_compass_tip_1);

            mTitleTv.setText(title);

            // 跟飞机型号有关
            mStepTv.setVisibility(View.VISIBLE);
            mStepTv.setText(getStepResIdByAircraft(status));
            updateNoticeImage(getIllustrationResIdByAircraft(status));

            mLeftBtn.setText(R.string.uxsdk_fpv_checklist_cancel_cele);
            mRightBtn.setVisibility(View.GONE);
            mBottomDivider.setVisibility(View.GONE);
        } else if (status == CompassCalibrationState.VERTICAL) {
            title = getContext().getString(R.string.uxsdk_fpv_checklist_compass_tip_2);

            mTitleTv.setText(title);

            // 跟飞机型号有关
            mStepTv.setVisibility(View.VISIBLE);
            mStepTv.setText(getStepResIdByAircraft(status));
            updateNoticeImage(getIllustrationResIdByAircraft(status));

            mLeftBtn.setText(R.string.uxsdk_fpv_checklist_cancel_cele);
            mRightBtn.setVisibility(View.GONE);
            mBottomDivider.setVisibility(View.GONE);
        } else if (status == CompassCalibrationState.SUCCEEDED) {
            mTitleTv.setText(R.string.uxsdk_fpv_compass_adjust_complete);
            mStepTv.setVisibility(View.GONE);
            updateNoticeImage(R.drawable.uxsdk_setting_ui_success);
            mLeftBtn.setText(R.string.uxsdk_app_ok);
            mRightBtn.setVisibility(View.GONE);
            mBottomDivider.setVisibility(View.GONE);
        } else if (status == CompassCalibrationState.FAILED) {
            mTitleTv.setText(R.string.uxsdk_fpv_compass_adjust_fail);
            mStepTv.setVisibility(View.GONE);
            updateNoticeImage(R.drawable.uxsdk_setting_ui_fail);
            mLeftBtn.setText(R.string.uxsdk_app_cancel);
            mRightBtn.setVisibility(View.VISIBLE);
            mBottomDivider.setVisibility(View.VISIBLE);
        }
    }

    private void updateNoticeImage(@DrawableRes int resId) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)mIllustationIv.getLayoutParams();
        params.topMargin = 0;
        params.bottomMargin = 0;
        if (resId == R.drawable.uxsdk_setting_ui_success || resId == R.drawable.uxsdk_setting_ui_fail) {
            params.topMargin = getContext().getResources().getDimensionPixelSize(R.dimen.uxsdk_dialog_content_margin_top);
            params.bottomMargin = getContext().getResources().getDimensionPixelSize(R.dimen.uxsdk_dialog_content_margin_top);
        }
        mIllustationIv.setImageResource(resId);
    }
    private int getStepResIdByAircraft(CompassCalibrationState status) {
        if (status == CompassCalibrationState.HORIZONTAL) {
            return R.string.uxsdk_fpv_checklist_compass_tip_1_desc;
        } else {
            return R.string.uxsdk_fpv_checklist_compass_tip_2_desc;
        }
    }

    private int getIllustrationResIdByAircraft(CompassCalibrationState status) {
        int[] resIds;
        if(ProductUtil.isM30Product()){
            resIds = aircraftCompassIconM30;
        } else if (ProductUtil.isM3EProduct() || ProductUtil.isM4EProduct() || ProductUtil.isM4DProduct()) {
            resIds = aircraftCompassIconM3;
        } else{
            resIds = aircraftCompassIconM300;
        }
        if(status == CompassCalibrationState.HORIZONTAL) {
            return resIds[0];
        } else {
            return resIds[1];
        }
    }
}
