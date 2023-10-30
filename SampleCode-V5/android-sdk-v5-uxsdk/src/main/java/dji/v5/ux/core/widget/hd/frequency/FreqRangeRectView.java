package dji.v5.ux.core.widget.hd.frequency;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;

import dji.sdk.keyvalue.value.airlink.Bandwidth;
import dji.sdk.keyvalue.value.airlink.ChannelSelectionMode;
import dji.sdk.keyvalue.value.airlink.FrequencyBand;
import dji.sdk.keyvalue.value.airlink.FrequencyInterferenceInfo;
import dji.sdk.keyvalue.value.airlink.SDRHdOffsetParams;
import dji.v5.utils.common.LogUtils;
import dji.v5.utils.common.SDRLinkHelper;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;

public class FreqRangeRectView extends ConstraintLayoutWidget<Object> implements OnTouchListener {
    private final static String TAG = FreqRangeRectView.class.getSimpleName();

    public interface OnRangeChangedListener {
        void onRangeChanged(float leftVal, float rightVal, float leftPos, float rightPos, boolean isRectDragging);
        void onAverageValChanged(String averageVal);
    }

    private ImageView mCenterDown;
    private ImageView mCenterUp;
    private ProgressBar mAveragePgb;

    /** 当前白色选框是否处于拽托状态 */
    private boolean mIsDragging = false;
    /** 拽托白色选框时, 手指的触摸位置*/
    private float mRangeRectTouchX = 0;

    private int mNumNfValues = Integer.MAX_VALUE;
    /** 频点的起始位置, 见{@link #mNumNfValues}说明 */
    private float mNfStartIndex = 2400.5f;
    /** 频点的范围, 10M范围是+-5, 20M范围是+-10 */
    private int mRangeSize = 5;
    /** 在屏幕坐标系下, 两个nf频点间的步长, 以x轴为参考坐标系 */
    private float mWidthInterval = Float.MAX_VALUE;
    /** 白色选框中心频点的值, 即24xx.5的形式 */
    private Integer mCurNfIndex = Integer.MAX_VALUE;
    /** 白色选框最左边, 不一定等于({@link #mCurNfIndex} - {@link #mRangeSize}), 会随着用户拖动的变化而变化 */
    private float mLeftNfIndex = Float.MAX_VALUE;

    /** 上一次出于2。4G时的频点值 */
    private int mLastNfIndex2dot4G = Integer.MIN_VALUE;

    /** 当前中心频点所处于的x轴位置 */
    private float mCurCenterX = Float.MAX_VALUE;
    /** 父view的宽度, 这里指整个sdr曲线图的宽度 */
    private int mParentWidth = Integer.MAX_VALUE;

    private FrequencyBand mCurBandChannelMode = FrequencyBand.UNKNOWN;
    private ChannelSelectionMode mCurChannelMode = ChannelSelectionMode.AUTO;
    private List<FrequencyInterferenceInfo> mRssis;

    private OnRangeChangedListener mListener;

    private Drawable mNormalBg;
    private Drawable mHoverBg;


    private float mRectX = -1.0f;
    private float mUnusedRectX = -1.0f;
    private float mAlpha = 1;
    private float mLeftX;
    private float mRightX;
    private Handler mSkipPointHandler;

    FreqRangeRectViewModel widgetModel = new FreqRangeRectViewModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());

    public FreqRangeRectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSkipPointHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        //do nothing
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getFrequencyPointIndex().subscribe(value -> {
            mCurNfIndex = value;
            updatePosition();
        }));
        addReaction(widgetModel.getChannelSelectionMode().subscribe(value -> {
            mCurChannelMode = value;
            updateAllView();
        }));
        addReaction(widgetModel.getBandwidth().subscribe(value -> {
            mBandwidth = value;
            updateAllView();
        }));
        addReaction(widgetModel.getFrequencyPointRSSIInfo().subscribe(value -> {
            mRssis = value;
            updateAverageValue();
        }));
        addReaction(widgetModel.getFrequencyPointIndexRange().subscribe(value -> {
            mValidRanges = new Integer[2];
            mValidRanges[0] = value.getMin();
            mValidRanges[1] = value.getMax();
            onValidRangeChanged();
        }));
        addReaction(widgetModel.getSDRHdOffsetParams().subscribe(value -> {
            mOffsetParams = value;
        }));
        addReaction(widgetModel.getFrequencyBand().subscribe(value -> {
            mCurBandChannelMode = value;
            onDualBandModeChange();
            updateAllView();
        }));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if(isInEditMode()) {
            return;
        }

        mCenterDown = (ImageView) findViewById(R.id.sdr_snr_freq_range_center_down);
        mCenterUp = (ImageView) findViewById(R.id.sdr_snr_freq_range_center_up);
        mAveragePgb = (ProgressBar) findViewById(R.id.sdr_snr_freq_rect_pgb);

        mNormalBg = getResources().getDrawable(R.drawable.uxsdk_freq_range_rect);
        mHoverBg = getResources().getDrawable(R.drawable.uxsdk_freq_range_rect_hover);

        setOnTouchListener(this);
        mAlpha = getAlpha();
        super.setAlpha(0);    //初始化时先隐藏
        updateAllView();
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

    public FreqRangeRectView setParentWidth(int _width) {
        mParentWidth = _width;
        return this;
    }

    public FreqRangeRectView setNumValues(int _num) {
        mNumNfValues = _num;
        return this;
    }

    public FreqRangeRectView setWidthInterval(float _interval) {
        mWidthInterval = _interval;
        return this;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        //自动模式或辅控都不能设置频点信息
        if(mCurChannelMode != ChannelSelectionMode.MANUAL) {
            return false;
        }

        // 处理框体的拖动
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            setParentInterceptTouchEvent(true);
            mRangeRectTouchX = event.getRawX();
            mIsDragging = true;
            updateRectBg(mIsDragging);
        }
        else if(event.getAction() == MotionEvent.ACTION_MOVE) {
            float curX = event.getRawX();
            float setX = v.getX() + curX - mRangeRectTouchX;
            // 处理挪出范围的情况
            if(setX < 0) {
                setX = 0;
            } else if(setX + v.getWidth() > mParentWidth) {
                setX = (float)mParentWidth - v.getWidth();
            }
            v.setX(setX);
            mRangeRectTouchX = curX;
            mLeftNfIndex = convertX2Index(setX);
            onRangeChanged(mLeftNfIndex, mLeftNfIndex + mRangeSize * 2, setX, setX + v.getWidth(), mIsDragging);
        } else if(event.getAction() == MotionEvent.ACTION_UP
            || event.getAction() == MotionEvent.ACTION_CANCEL) {
            mIsDragging = false;

            float curX = v.getX();
            // 与频点对齐
            if(curX % mWidthInterval != 0) {
                float multiple = curX / mWidthInterval;
                float preMulX = multiple * mWidthInterval;
                float aftMulX = (multiple + 1) * mWidthInterval;
                if(curX - preMulX < aftMulX - curX || multiple + 1 >= (mNumNfValues - 1)) {
                    curX = preMulX;
                } else {
                    curX = aftMulX;
                }
                v.setX(curX);

            }
            mLeftNfIndex = convertX2Index(curX);
            onRangeChanged(mLeftNfIndex, mLeftNfIndex + mRangeSize * 2, curX, curX + v.getWidth(), mIsDragging);
            updateAverageValue();
            updateRectBg(mIsDragging);
            sendNfIndex2Sdr(curX);
            setParentInterceptTouchEvent(false);
        }
        return true;
    }

    //请求禁止父容器拦截触摸事件
    public void setParentInterceptTouchEvent(boolean disallow) {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
        }
    }

    /**
     * 自定义时, 白色选框拽托结束后, 设置当前的nf频点值到sdr
     * @param leftX
     */
    private void sendNfIndex2Sdr(float leftX) {
        float centerX = leftX + mRangeSize * mWidthInterval;
        float caculNf = convertX2Index(centerX);
        final int sendNf = (int) caculNf;
        addDisposable(widgetModel.setFrequencyPointIndex(sendNf).subscribe(() -> {

        }, throwable -> {
            LogUtils.e(TAG, "setFrequencyPointIndex fail: " + throwable);
        }));
    }

    public void setOnRangeChangedListener(OnRangeChangedListener l) {
        mListener = l;
    }

    private Bandwidth mBandwidth;
    private Integer[] mValidRanges;
    private SDRHdOffsetParams mOffsetParams;

    public void onValidRangeChanged() {

        if(mBandwidth == null || mCurChannelMode == null || mParentWidth == Integer.MAX_VALUE
            || mValidRanges == null) {
            return;
        }

        float selectRange = SDRLinkHelper.RANGE_SIZE_10MHZ;
        if(mBandwidth == Bandwidth.BANDWIDTH_20MHZ) {
            selectRange = SDRLinkHelper.RANGE_SIZE_20MHZ;
        } else if(mBandwidth == Bandwidth.BANDWIDTH_40MHZ) {
            selectRange = SDRLinkHelper.RANGE_SIZE_40MHZ;
        }

        float left = mValidRanges[0];
        float right = mValidRanges[1];
        if(left != SDRLinkHelper.ORIGINAL_NF_2DOT4G_START_FREQ
            && left != SDRLinkHelper.ORIGINAL_NF_5DOT8G_START_FREQ) {
            left -= selectRange;
            right += selectRange;
        }

        mNumNfValues = (int) (right - left);
        mWidthInterval = mParentWidth * 1f / (mNumNfValues);
        mNfStartIndex = left;
        if (null == mCurNfIndex || mCurNfIndex == Integer.MAX_VALUE) {
            return;
        }
        checkCurNfInRange(mCurNfIndex);

        updateAllView();
    }

    private void checkCurNfInRange(float curNfIndex) {

        if (mValidRanges == null) {
            return;
        }

        if (curNfIndex < mValidRanges[0]) {
            addDisposable(widgetModel.setFrequencyPointIndex(mValidRanges[0]).subscribe(() -> {

            }, throwable -> {
                LogUtils.e(TAG, "setFrequencyPointIndex fail: " + throwable);
            }));
        } else if (curNfIndex > mValidRanges[1]) {
            addDisposable(widgetModel.setFrequencyPointIndex(mValidRanges[1]).subscribe(() -> {

            }, throwable -> {
                LogUtils.e(TAG, "setFrequencyPointIndex fail: " + throwable);
            }));
        }
    }

    private void onDualBandModeChange() {

        if (mCurBandChannelMode == FrequencyBand.BAND_5_DOT_8G) {
            mLastNfIndex2dot4G = mCurNfIndex;
        } else if (mCurBandChannelMode == FrequencyBand.BAND_2_DOT_4G){
            addDisposable(widgetModel.setFrequencyPointIndex(mLastNfIndex2dot4G).subscribe(() -> {

            }, throwable -> {
                LogUtils.e(TAG, "setFrequencyPointIndex fail: " + throwable);
            }));
        }
        updateAllView();
    }

    /**
     * 更新白色选框内rssi的平均值, 选框位置改变, 大小改变, rssi改变时都应该更新
     */
    private void updateAverageValue() {

        if(mRssis == null || mCurNfIndex == Float.MAX_VALUE) {
            return;
        }

        int rssiStartIndex = (int) ((mLeftNfIndex - mNfStartIndex) / 2);
        if(rssiStartIndex < 0) {
            rssiStartIndex = 0;
        }
        float averageVal = 0;
        for(int i = rssiStartIndex; i < rssiStartIndex + mRangeSize && i < mRssis.size(); ++i) {
            averageVal += mRssis.get(i).getRssi();
        }
        averageVal = averageVal / mRangeSize;

        // 一下计算offset值
        if(mOffsetParams != null) {
            averageVal += ((mOffsetParams.getRcLinkOffset().byteValue() > 0 ? mOffsetParams.getRcLinkOffset() : 0)
                + mOffsetParams.getPathLossOffset().byteValue() + mOffsetParams.getTxPowerOffset().byteValue());
        }
        // 由于最大值是50, Progress最大值为100, 所以转化成Progress直接乘以2
        mAveragePgb.setProgress((int) ((averageVal - FreqView.NF_BASE_VALUE) * 2));
        if(mListener != null) {
            mListener.onAverageValChanged(String.format(Locale.US, "%.1f", averageVal) + "dBm");
        }
    }

    private void updateAllView() {
        resizeRect();
        updatePosition();
        onChannelModeChanged();
    }

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        mAlpha = alpha;
    }

    private void updatePosition() {
        if(mCurNfIndex == null || mCurNfIndex == Integer.MAX_VALUE) {
            return;
        }
        // 各项长度还未初始化
        if(mWidthInterval == Float.MAX_VALUE) {
            return;
        }

        if(mIsDragging) {
            return;
        }
        Log.i(getClass().getSimpleName(), "convertIndex: " + mCurNfIndex);
        mCurCenterX = (mCurNfIndex - mNfStartIndex) * mWidthInterval;

        if(getAlpha() != mAlpha) {
            super.setAlpha(mAlpha);
        }
        setRectBorder();
    }

    private void resizeRect() {
        if(mBandwidth == null) {
            return;
        }

        if(mBandwidth == Bandwidth.BANDWIDTH_10MHZ) {
            mRangeSize = SDRLinkHelper.RANGE_SIZE_10MHZ;
        } else if(mBandwidth == Bandwidth.BANDWIDTH_20MHZ){
            mRangeSize = SDRLinkHelper.RANGE_SIZE_20MHZ;
        } else if(mBandwidth == Bandwidth.BANDWIDTH_40MHZ){
            mRangeSize = SDRLinkHelper.RANGE_SIZE_40MHZ;
        }

        // 重新计算框的大小
        setRectBorder();
    }

    private void setRectBorder() {
        if(mCurCenterX == Float.MAX_VALUE) {
            // 中心线未设置
            return;
        }

        // 更新左边频点值
        mLeftNfIndex = (float)mCurNfIndex - mRangeSize;
        float leftX = Math.max(mCurCenterX - mRangeSize * mWidthInterval, 0f);
        float rightX = mCurCenterX + mRangeSize * mWidthInterval;
        onRangeChanged((float)mCurNfIndex - mRangeSize, (float)mCurNfIndex + mRangeSize, leftX, rightX, mIsDragging);
    }

    private void reDrawRectBorder(float leftX, float rightX) {
        if(mLeftX == leftX && mRightX == rightX) {
            return;
        }
        mLeftX = leftX;
        mRightX = rightX;

        this.setX(leftX);
        ViewGroup.LayoutParams lp = this.getLayoutParams();
        lp.width = (int) (rightX - leftX);
        this.setLayoutParams(lp);
        updateAverageValue();
    }

    //屏蔽差值较大的值，防止rect view回跳
    private void onRangeChanged(float leftVal, float rightVal, float leftPos, float rightPos, boolean isRectDragging) {
        mSkipPointHandler.removeCallbacksAndMessages(null);
        if ((mRectX == -1.0f || Math.abs(leftVal - mRectX) <= 5.0f) || (mUnusedRectX != -1.0f && Math.abs(leftVal - mUnusedRectX) <= 5.0f)) {

            mRectX = leftVal;
            if (mListener != null) {
                mListener.onRangeChanged(leftVal, rightVal, leftPos, rightPos, isRectDragging);
            }
            if (!mIsDragging) {
                reDrawRectBorder(leftPos, rightPos);
            }
        } else {
            mUnusedRectX = leftVal;
            //如果跳跃比较大，而又没有再次更新，则应该取最后的结果显示出来
            mSkipPointHandler.postDelayed(() -> {
                onRangeChanged(leftVal, rightVal, leftPos, rightPos, isRectDragging);
            }, 1000);
        }
    }

    /**
     * 当前的坐标值转化为频点值
     * @param coordX
     * @return
     */
    private float convertX2Index(float coordX) {
        float caculNf = coordX / mWidthInterval + mNfStartIndex;
        return (float) (Math.floor(caculNf) + 0.5);
    }

    /**
     * 信道模式改变时, 需要改变bg
     */
    private void onChannelModeChanged() {
        if(mCurChannelMode == null) {
            return;
        }

        if(mCurChannelMode == ChannelSelectionMode.AUTO ) {
            mCenterDown.setVisibility(INVISIBLE);
            mCenterUp.setVisibility(INVISIBLE);
            mIsDragging = false;
            setBackground(mNormalBg);
        } else {
            mCenterDown.setVisibility(VISIBLE);
            mCenterUp.setVisibility(VISIBLE);
            // 切换到自定义时闪一下背景
            setBackground(mHoverBg);
            postDelayed(() -> setBackground(mNormalBg), 200);
        }

    }

    /**
     * 根据矩形框的拖动状态, 设置矩形框的hover bg以及水平线的消失和显示
     * @param _isdragging
     */
    private void updateRectBg(boolean _isdragging) {
        if(_isdragging) {
            mAveragePgb.setVisibility(INVISIBLE);
            setBackground(mHoverBg);
        } else {
            mAveragePgb.setVisibility(VISIBLE);
            setBackground(mNormalBg);
        }
    }
}
