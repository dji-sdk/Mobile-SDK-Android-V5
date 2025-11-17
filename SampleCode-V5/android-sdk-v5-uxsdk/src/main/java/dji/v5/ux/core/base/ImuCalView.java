package dji.v5.ux.core.base;


import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.utils.ProductUtil;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.sdk.keyvalue.value.flightcontroller.IMUCalibrationInfo;
import dji.sdk.keyvalue.value.flightcontroller.IMUCalibrationOrientation;
import dji.sdk.keyvalue.value.flightcontroller.IMUCalibrationState;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.utils.common.AndUtil;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;

import static dji.v5.ux.core.base.IImuResources.INDEX_SIDE_TOP;
import static dji.v5.ux.core.base.IImuResources.MAX_DESC_COUNT;
import static dji.v5.ux.core.base.IImuResources.RESIDS_AIRCRAFT_M300;
import static dji.v5.ux.core.base.IImuResources.RESIDS_AIRCRAFT_M320;
import static dji.v5.ux.core.base.IImuResources.RESIDS_AIRCRAFT_M3E;
import static dji.v5.ux.core.base.IImuResources.RESIDS_PREPARE_DESC;
import static dji.v5.ux.core.base.IImuResources.RESIDS_PREPARE_DESC_REMOVE_DESC1;
import static dji.v5.ux.core.base.IImuResources.RESIDS_PROCESS_DESC;



public class ImuCalView extends LinearLayout implements View.OnClickListener {

    private static final String TAG = ImuCalView.class.getSimpleName();

    // title
     ImageView mCloseImg = null;

    // process
     LinearLayout mProcessLy = null;
     ImageView mProcessAirImg = null;
     LinearLayout mProcessPageLy = null;

     TextView mPrepareStartTv = null;

     ProgressBar mProecessPgb = null;
     TextView mProcessTv = null;
    TextView mProcessPageTv = null;

    // status
   LinearLayout mStatusLy = null;
   ImageView mStatusImg = null;
   TextView mStatusDescTv = null;
   TextView mStatusOptTv = null;
   TextView mStatusRestartTv = null;

    private void initView() {
        mCloseImg = findViewById(R.id.imu_cal_close_img);
        mCloseImg.setOnClickListener(this);
        mProcessLy = findViewById(R.id.imu_cal_process_ly);
        mProcessAirImg = findViewById(R.id.imu_cal_left_content_img);
        mProcessPageLy = findViewById(R.id.imu_cal_left_botom_ly);

        mPrepareStartTv = findViewById(R.id.imu_cal_start_tv);
        mPrepareStartTv.setOnClickListener(this);

        mProecessPgb = findViewById(R.id.imu_cal_pgb);
        mProcessTv = findViewById(R.id.imu_cal_pgb_tv);
        mProcessPageTv = findViewById(R.id.imu_cal_page_tv);

        mStatusLy = findViewById(R.id.imu_cal_status_ly);
        mStatusImg = findViewById(R.id.imu_cal_status_img);
        mStatusDescTv = findViewById(R.id.imu_cal_status_tv);
        mStatusOptTv = findViewById(R.id.imu_cal_status_opt_tv);
        mStatusOptTv.setOnClickListener(this);

        mStatusRestartTv = findViewById(R.id.imu_cal_status_restart_tv);
        mStatusRestartTv.setOnClickListener(this);

    }

    // member
    private final LinearLayout[] mProcessDescLys = new LinearLayout[MAX_DESC_COUNT];
    private final TextView[] mProcessDescTvs = new TextView[MAX_DESC_COUNT];
    private int[] mPrepareDesc = RESIDS_PREPARE_DESC;
    private List<Integer> mNeedCalIndex;

    private OnImuCalListener mOnImuCalListener = null;
    private IMUCalibrationInfo mIMUCalibrationHint;

    int[] sideSequence;

    public ImuCalView(Context context) {
        this(context, null);
    }

    public ImuCalView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImuCalView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
         LayoutInflater.from(context).inflate(R.layout.uxsdk_setting_ui_imu_cal_view, this, true);
        initView();

    }



    public void setOnImuCalListener(final OnImuCalListener listener) {
        mOnImuCalListener = listener;
    }

    private void initViews() {
        mPrepareDesc = RESIDS_PREPARE_DESC_REMOVE_DESC1;
        sideSequence = (ProductUtil.isM3EProduct() || ProductUtil.isM4EProduct() || ProductUtil.isM4DProduct())? IImuResources.SIDE_SEQUENCE_M2E : IImuResources.SIDE_SEQUENCE;

        final int[] descResIds = new int[]{
            R.id.imu_cal_right_desc_ly1, R.id.imu_cal_right_desc_ly2, R.id.imu_cal_right_desc_ly3
        };

        for (int i = 0; i < MAX_DESC_COUNT; i++) {
            mProcessDescLys[i] = (LinearLayout) findViewById(descResIds[i]);
            mProcessDescTvs[i] = (TextView) mProcessDescLys[i].findViewById(R.id.setting_ui_imucal_desc_tv);
        }

        // prepare ui
        showPrepare();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isInEditMode()) {
            return;
        }
        setKeepScreenOn(true);
        initViews();

        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyIMUCalibrationInfo) ,this, (oldValue, newValue) ->{
           if (newValue != null) {
               updateViews(newValue);
           }
        });

    }

    @Override
    protected void onDetachedFromWindow() {
        // todo 飞机断开连接后关闭对话框
        super.onDetachedFromWindow();
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();

        if (id == R.id.imu_cal_start_tv) {
            // 开始校准
            startCalibrate();
        } else if (id == R.id.imu_cal_status_opt_tv) {
            // 失败重试
            if (mIMUCalibrationHint != null && mIMUCalibrationHint.getCalibrationState() == IMUCalibrationState.FAILED) {
                startCalibrate();
            } else {
                closeSelf(0);
            }
        } else if (id == R.id.imu_cal_close_img) {
            // 关闭IMU校准界面
            closeSelf(0);
        } else if (id == R.id.imu_cal_status_restart_tv) {
            // 重启飞机
            KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyRebootDevice) , new CommonCallbacks.CompletionCallbackWithParam< EmptyMsg >(){

                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    LogUtils.d(TAG, "rebootDevice success");
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    LogUtils.d(TAG, "rebootDevice failed!" );
                }
            });

            closeSelf(0);
        }
    }

    private void startCalibrate() {
        showProcess();
        KeyManager.getInstance().performAction(KeyTools.createKey(FlightControllerKey.KeyStartIMUCalibration) , new CommonCallbacks.CompletionCallbackWithParam< EmptyMsg >(){

            @Override
            public void onSuccess(EmptyMsg emptyMsg) {
                LogUtils.d(TAG, "startCalibrate success!");
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                LogUtils.e(TAG, "startCalibrate failed!" );
            }
        });

    }

    /**
     * 操作提示描述： 开始前和校准过程中会动态更新
     * @param descResIds view ids
     */
    private void updateDesc(final int[] descResIds) {
        final int offset = descResIds.length;
        for (int i = 0; i < offset; i++) {
            if(mProcessDescLys[i] != null){
                mProcessDescLys[i].setVisibility(VISIBLE);
            }
            if(mProcessDescTvs[i] != null){
                mProcessDescTvs[i].setText(descResIds[i]);
            }
        }
        for (int i = offset; i < MAX_DESC_COUNT; i++) {
            if(mProcessDescLys[i] != null){
                mProcessDescLys[i].setVisibility(GONE);
            }
        }
    }

    /**
     * IMU校准回调更新UI
     * @param imuCalibrationHint 最新状态
     */
    private void updateViews(IMUCalibrationInfo imuCalibrationHint) {

        // 状态变更的时候才去更新
        if (mIMUCalibrationHint == null || mIMUCalibrationHint.getCalibrationState() != imuCalibrationHint.getCalibrationState()) {
            updateStatus(imuCalibrationHint);
        }

        if (imuCalibrationHint.getCalibrationState() == IMUCalibrationState.CALIBRATING) {
            updatePageIndex(imuCalibrationHint);
            int totalProgress = imuCalibrationHint.getOrientationsToCalibrate().size()
                    + imuCalibrationHint.getOrientationsCalibrated().size();
            updateProgress(100 * imuCalibrationHint.getOrientationsCalibrated().size() / totalProgress);
        } else if (imuCalibrationHint.getCalibrationState() == IMUCalibrationState.FAILED) {
            mStatusDescTv.setText(getContext().getString(R.string.uxsdk_setting_ui_imu_fail));
            // TODO 添加IMU校准失败的错误码或错误原因
        } else if (imuCalibrationHint.getCalibrationState() == IMUCalibrationState.SUCCESSFUL) {
            // 校准成功后拉取下IMU状态刷新其状态值。SDK里面IMU状态直接读的内存中的调参值，如果没有其他地方去拉取的话校准成功后状态还是异常
            //Pilot2Repo.FlightController().IMUCheckStatus().getValueAsync().subscribe();
        }
        mIMUCalibrationHint = imuCalibrationHint;
    }

    private void updatePageIndex(IMUCalibrationInfo imuCalibrationHint) {
        if (mNeedCalIndex == null) {
            mNeedCalIndex = new ArrayList<>();
            for (int i = 0; i < imuCalibrationHint.getOrientationsToCalibrate().size(); ++i) {
                mNeedCalIndex.add(imuCalibrationHint.getOrientationsToCalibrate().get(i).value());
            }
        }

        int total = mNeedCalIndex.size();
        if  (total == 0) {
            return;
        }

        int childCount = mProcessPageLy.getChildCount();
        if (childCount != total) {
            if (childCount > total) {
                while (childCount-- > total) {
                    mProcessPageLy.removeViewAt(childCount);
                }
            } else {
                final int radius = AndUtil.dip2px(getContext(), 9);
                final int margin = AndUtil.dip2px(getContext(), 5);
                while (childCount++ < total) {
                    final LayoutParams param = new LayoutParams(radius, radius);
                    param.setMargins(margin, margin, margin, margin);
                    final View v = LayoutInflater.from(getContext()).inflate(R.layout.uxsdk_setting_circle_view, null);
                    mProcessPageLy.addView(v, param);
                }
            }
        }
        updatePageIndexEx(total , imuCalibrationHint);
    }
    private  void updatePageIndexEx(int total  , IMUCalibrationInfo imuCalibrationHint){
        int[] mCalibrationOrder = {0, 1, 2, 3, 4, 5};
        int current = -1;
        for (int i = 0; i < total; ++i) {
            final View v = mProcessPageLy.getChildAt(i);
            int index = getSDKIndex(mCalibrationOrder[i]);
            boolean calibrated = false;
            for (IMUCalibrationOrientation orientation : imuCalibrationHint.getOrientationsCalibrated()) {
                if (orientation.value() == index) {
                    v.setHovered(false);
                    v.setSelected(true);
                    calibrated = true;
                    break;
                }
            }
            if (!calibrated){   // 尚未校准
                // 取第一个为校准的作为当前校准目标
                if (current == -1) {
                    current = mCalibrationOrder[i];
                    mProcessPageTv.setText(String.format(Locale.US, "%d/%d", i + 1, total));
                    v.setHovered(true);
                } else {
                    v.setHovered(false);
                }
                v.setSelected(false);
            }
        }
        updateCurrentSide(getSDKIndex(current));
    }

    /**
     * 获取SDK校准的顺序index
     * @param current
     * @return
     */
    private int getSDKIndex(int current) {
        if (current >= 0 && current < sideSequence.length) {
            return sideSequence[current];
        }
        return INDEX_SIDE_TOP;
    }

    private void updateCurrentSide(int currentSide) {
        final int resId = getAircraft(currentSide);
        if (0 != resId) {
            mProcessAirImg.setImageResource(resId);
        }
    }

    private void updateProgress(int progress) {
        if (progress < 0) {
            progress = 0;
        } else if (progress > 100) {
            progress = 100;
        }
        mProecessPgb.setProgress(progress);
    }

    private void updateStatus(IMUCalibrationInfo imuCalibrationHint) {
        if (IMUCalibrationState.NONE == imuCalibrationHint.getCalibrationState()) {
            showPrepare();
        } else if (IMUCalibrationState.CALIBRATING == imuCalibrationHint.getCalibrationState()) {
            showProcess();
        } else if (IMUCalibrationState.SUCCESSFUL == imuCalibrationHint.getCalibrationState()) {
            showSuccess();
        } else if (IMUCalibrationState.FAILED == imuCalibrationHint.getCalibrationState()) {
            showFail();
        }
//        if (imuCalibrationHint.getCalibrationState() == IMUCalibrationState.CALIBRATING) {
//            // 告知另外一个控正在校准
//            //MultiRcManager.getInstance().sendMultiRcSyncData(MultiRcSyncDataType.MULTI_RC_IMU_CALIBRATION,new byte[]{1});
//        }else{
//            // 告知另外一个控退出校准（或者校准失败、已结束等）
//            //MultiRcManager.getInstance().sendMultiRcSyncData(MultiRcSyncDataType.MULTI_RC_IMU_CALIBRATION,new byte[]{0});
//        }
    }

    private void showPrepare() {
        mCloseImg.setVisibility(VISIBLE);
        mPrepareStartTv.setVisibility(VISIBLE);

        mProcessLy.setVisibility(VISIBLE);
        mProcessPageLy.setVisibility(INVISIBLE);
        mProcessAirImg.setImageResource(getReadyResId());

        updateDesc(mPrepareDesc);

        mProecessPgb.setVisibility(GONE);
        mProcessTv.setVisibility(GONE);
        mProcessPageTv.setVisibility(INVISIBLE);
        mStatusLy.setVisibility(GONE);
    }

    private void showProcess() {
        mCloseImg.setVisibility(GONE);
        mPrepareStartTv.setVisibility(GONE);

        mProcessLy.setVisibility(VISIBLE);
        mProcessPageLy.setVisibility(VISIBLE);
        mProcessAirImg.setImageResource(getAircraft(IMUCalibrationOrientation.BOTTOM_DOWN.value()));

        updateDesc(RESIDS_PROCESS_DESC);
        mProecessPgb.setVisibility(VISIBLE);
        mProecessPgb.setProgress(0);
        mProcessTv.setVisibility(VISIBLE);
        mProcessPageTv.setVisibility(VISIBLE);

        mStatusLy.setVisibility(GONE);
    }

    private void showSuccess() {
        mCloseImg.setVisibility(VISIBLE);

        mProcessLy.setVisibility(GONE);

        mStatusLy.setVisibility(VISIBLE);
        mStatusDescTv.setText(R.string.uxsdk_setting_ui_imu_success);
        mStatusImg.setBackgroundResource(R.drawable.uxsdk_setting_ui_success);
        mStatusOptTv.setText(R.string.uxsdk_setting_ui_imu_back);
        mStatusRestartTv.setVisibility(VISIBLE);
    }

    private void showFail() {
        mCloseImg.setVisibility(VISIBLE);

        mProcessLy.setVisibility(GONE);

        mStatusLy.setVisibility(VISIBLE);
        mStatusDescTv.setText(R.string.uxsdk_setting_ui_imu_fail);
        mStatusImg.setBackgroundResource(R.drawable.uxsdk_setting_ui_fail);
        mStatusOptTv.setText(R.string.uxsdk_setting_ui_imu_retry);
        mStatusRestartTv.setVisibility(GONE);
    }

    /**
     * 根据机型获取示例图片
     * @return resId
     */
    private int getReadyResId() {
        if(ProductUtil.isM30Product()){
            return R.drawable.uxsdk_setting_ui_imu_ready_m320;
        } else if (ProductUtil.isM3EProduct() || ProductUtil.isM4EProduct() || ProductUtil.isM4DProduct()) {
            return R.drawable.uxsdk_img_device_home_m3e;
        } else{
            return R.drawable.uxsdk_setting_ui_imu_ready_m300;
        }
    }

    private int getAircraft(final int index) {
        int[] resIds;

        if (ProductUtil.isM30Product()) {
            resIds = RESIDS_AIRCRAFT_M320;
        } else if (ProductUtil.isM3EProduct() || ProductUtil.isM4EProduct() || ProductUtil.isM4DProduct()) {
            resIds = RESIDS_AIRCRAFT_M3E;
        } else {
            resIds = RESIDS_AIRCRAFT_M300;
        }

        if (0 <= index && index < resIds.length) {
            return resIds[index];
        } else {
            return 0;
        }
    }

    private void closeSelf(final int arg) {
        if (null != mOnImuCalListener) {
            mOnImuCalListener.onClose(arg);
        }
    }

    public interface OnImuCalListener {
        /**
         * 关闭校准对话框
         * @param arg1
         */
        void onClose(final int arg1);
    }
}
