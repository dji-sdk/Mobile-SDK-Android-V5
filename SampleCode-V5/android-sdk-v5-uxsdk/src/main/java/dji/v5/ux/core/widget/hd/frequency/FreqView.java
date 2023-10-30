package dji.v5.ux.core.widget.hd.frequency;

import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import dji.sdk.keyvalue.key.AirLinkKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.value.airlink.Bandwidth;
import dji.sdk.keyvalue.value.airlink.ChannelSelectionMode;
import dji.sdk.keyvalue.value.airlink.FrequencyInterferenceInfo;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.utils.common.LogUtils;
import dji.v5.utils.common.SDRLinkHelper;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.charts.model.Line;
import dji.v5.ux.core.base.charts.model.LineChartData;
import dji.v5.ux.core.base.charts.model.PointValue;
import dji.v5.ux.core.base.charts.model.Viewport;
import dji.v5.ux.core.base.charts.view.LineChartView;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;

public class FreqView extends ConstraintLayoutWidget<Object> {
    private static final String TAG = "FreqView";
    private LineChartView mSdrSnrLine;
    private LineChartData mSnrValues;
    private FreqRangeRectView mRangeRect;
    private FreqRangeTextView mRangeTv;
    private RectCenterTextView mAverageTv;
    private TextView mCustomTipTv;
    private ChartRightYAxisView mQualityTx;
    private DistanceLineView mQualityLine;

    private static final int M_MIN_VALUE_COLOR = 0xff95e33f;
    private static final int M_MAX_VALUE_COLOR = 0xfff84242;

    /** 曲线顶端的最大值, 即-60+110, 参考{@link #NF_BASE_VALUE}, 最小值为0 */
    public static final int LINE_MAX_VALUE = 50;
    /** NF频点对应的值的最低值为-110, 最高值为-60 */
    public static final int NF_BASE_VALUE = -110;
    public static final int NF_MAX_VALUE = -60;
    private final int RSSI_VALUE_NUM_2DOT4G = 41;
    private final int RSSI_VALUE_NUM_5DOT8G = 62;
    private final int RSSI_VALUE_NUM_5DOT7G = 10;
    private final int RSSI_VALUE_NUM_1DOT4G = 7;
    private final int RSSI_VALUE_NUM_5DOT2G = 51;
    private int mNumRssiValues = RSSI_VALUE_NUM_2DOT4G;
    /** rssi起始index对应的nf频点 */
    private final float START_INDEX_2DOT4G = 2400f;
    private final float START_INDEX_5DOT8G = 5725f;
    private final float START_INDEX_5DOT7G = 5650f;
    private final float START_INDEX_1DOT4G = 1430f;
    private final float START_INDEX_5DOT2G = 5150f;

    private float mRssiStartIndex = START_INDEX_2DOT4G;
    /** nf频点的个数, 实际的rssi取值为2400~2482, 但是由于只能设置xxxx.5到sdr, 所以取2400.5~2482.5, 步长为1 */
    private final int NF_VALUE_NUM_2DOT4G = 82;
    private final int NF_VALUE_NUM_5DOT8G = 124;
    private final int NF_VALUE_NUM_5DOT7G = 20;
    private final int NF_VALUE_NUM_1DOT4G = 14;
    private final int NF_VALUE_NUM_5DOT2G = 102;
    private int mNumNfValues = NF_VALUE_NUM_2DOT4G;
    /** 在屏幕坐标系下, 两个nf频点间的步长 */
    private float mWidthInterval = 1;
    /** 当前工作频段 0: 2.4G, 1:5.8G */
    private FreqBand mCurFreqBand = FreqBand.FREQ_BAND_2DOT4G;

    private FreqViewModel widgetModel = new FreqViewModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        initView();
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getFrequencyPointIndex().subscribe(value -> {
            mFreqIndex = value;
            onFreqIndexChanged();
        }));
        addReaction(widgetModel.getChannelSelectionMode().subscribe(value -> {
            mSelectionMode = value;
            updateCustomTipVisibility(mSelectionMode);
        }));
        addReaction(widgetModel.getBandwidth().subscribe(value -> {
            mCurBandWidth = value;
        }));
        KeyManager.getInstance().listen(KeyTools.createKey(AirLinkKey.KeyFrequencyInterference), this, (oldValue, newValue) -> setValues(newValue));
        addReaction(widgetModel.getFrequencyPointIndexRange().subscribe(value -> {
            mValidRanges = new Integer[2];
            mValidRanges[0] = value.getMin();
            mValidRanges[1] = value.getMax();
            onValidRangeChanged();
        }));
        addReaction(widgetModel.getSDRHdOffsetParams().subscribe(value -> {
            mDisOffset = value.getDistOffset().byteValue();
            onDistOffsetChanged();
        }));
        addReaction(widgetModel.is5Dot7GSupported().subscribe(value -> {
            is5Dot7Supported = value;
        }));
    }

    private enum FreqBand{
        FREQ_BAND_2DOT4G,
        FREQ_BAND_5DOT8G,
        FREQ_BAND_1DOT4G,
        FREQ_BAND_5DOT7G,
        FREQ_BAND_840M,
        FREQ_BAND_5DOT2G
    }


    private Bandwidth mCurBandWidth;
    private ChannelSelectionMode mSelectionMode;
    private Integer[] mValidRanges;
    private int mDisOffset = 0;
    private Integer mFreqIndex;
    private boolean is5Dot7Supported;
    public FreqView(Context context) {
        this(context, null);
    }

    public FreqView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void initView() {
        View.inflate(getContext(), R.layout.uxsdk_sdr_freq_view, this);
        mSdrSnrLine = (LineChartView) findViewById(R.id.sdr_snr_line);
        KeyManager.getInstance().getValue(KeyTools.createKey(AirLinkKey.KeyFrequencyPoint), new CommonCallbacks.CompletionCallbackWithParam<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                mFreqIndex = integer;
                onFreqIndexChanged();

                initValues();

                mSdrSnrLine.setLineChartData(mSnrValues);
                mSdrSnrLine.setZoomEnabled(false);
                mSdrSnrLine.setLineShader(
                        new LinearGradient(0,
                                getResources().getDimension(R.dimen.uxsdk_setting_ui_hd_sdr_chart_height),
                                0, 0, M_MIN_VALUE_COLOR, M_MAX_VALUE_COLOR, Shader.TileMode.MIRROR));
                // 这样设置才能看到曲线图的整个port
                mSdrSnrLine.setViewportCalculationEnabled(true);
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                LogUtils.e(TAG,"get frequency point failed: "+error.description());
            }
        });


        resetViewport();

        mQualityTx =  findViewById(R.id.sdr_quality_value);
        mQualityLine =  findViewById(R.id.sdr_quality_line);
        mCustomTipTv =  findViewById(R.id.sdr_custom_alert_tip);
        mRangeTv = findViewById(R.id.sdr_freq_range_tv);
        mAverageTv =  findViewById(R.id.sdr_rect_average_value);

        mRangeRect = findViewById(R.id.sdr_snr_freq_range_rect);
        mRangeRect.setOnRangeChangedListener(new FreqRangeRectView.OnRangeChangedListener() {
            @Override
            public void onRangeChanged(float leftVal, float rightVal, float leftPos, float rightPos, boolean isRectDragging) {
                if (!isRectDragging) {
                    mRangeTv.setMinMaxValue(leftVal + 0.5f, rightVal + 0.5f, leftPos, rightPos);
                } else {
                    mRangeTv.setMinMaxValue(leftVal, rightVal, leftPos, rightPos);
                }
                mAverageTv.setCenterPos((leftPos + rightPos) / 2f, isRectDragging);
            }

            @Override
            public void onAverageValChanged(String averageVal) {
                mAverageTv.setCenterAverageText(averageVal);
            }

        });
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
        KeyManager.getInstance().cancelListen(this);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if(isInEditMode()) {
            return;
        }
        if(changed) {
            mWidthInterval = mSdrSnrLine.getWidth() * 1f / (mNumNfValues - 1);
            mRangeRect.setWidthInterval(mWidthInterval)
                      .setNumValues(mNumNfValues)
                      .setParentWidth(mSdrSnrLine.getWidth())
                      .onValidRangeChanged();

            onValidRangeChanged();
            onDistOffsetChanged();
        }
    }

    private void initValues() {
        List<PointValue> values = new ArrayList<PointValue>();
        values.add(new PointValue(0, 0));
        for (int i = 0; i < mNumRssiValues; ++i) {
            values.add(new PointValue(i + 0.5f, 0));
        }
        values.add(new PointValue(mNumRssiValues, 0));

        Line line = new Line(values);
        line.setStrokeWidth(2);
        line.setHasPoints(false);

        List<Line> lines = new ArrayList<Line>();
        lines.add(line);

        mSnrValues = new LineChartData(lines);

    }

    private void resetViewport() {
        final Viewport v = new Viewport(mSdrSnrLine.getMaximumViewport());
        v.bottom = 0;
        v.top = LINE_MAX_VALUE;
        v.left = 0;
        v.right = mNumRssiValues;
        mSdrSnrLine.setMaximumViewport(v);
        mSdrSnrLine.setCurrentViewport(v);
    }

    private void setValues(List<FrequencyInterferenceInfo> values) {
        if (values == null) {
            return;
        }
        mSdrSnrLine.clearAnimation();
        Line line = mSdrSnrLine.getLineChartData().getLines().get(0);

        int rssiValIndex = 0;
        int pointIndex = 0;
        int size = line.getValues().size();
        for (PointValue value : line.getValues()) {
            /** 对首尾值进行了处理, 理由{@link mNumRssiValues}的注释 */
            float y = 0;
            if(pointIndex == 0) {
                y = (float)values.get(rssiValIndex).getRssi() + 10 - NF_BASE_VALUE;

            } else if(pointIndex == values.size() + 1) {
                y = (float)values.get(pointIndex - 2).getRssi() + 10- NF_BASE_VALUE;
            } else {
                if (rssiValIndex >= size) {
                    LogUtils.e(TAG,"Illegal state,rssiValIndex >= line.getValues().size(),");
                    return;
                }
                y = (float)values.get(rssiValIndex++).getRssi() - NF_BASE_VALUE;
            }

            if( y > NF_MAX_VALUE - NF_BASE_VALUE) {
                y = (float)NF_MAX_VALUE - NF_BASE_VALUE;
            }
            if(y < 0) {
                y = 0;
            }
            value.setTarget(value.getX(), y);
            ++pointIndex;
        }

        mSdrSnrLine.startDataAnimation(300);

    }

    private void onValidRangeChanged() {

        if(mCurBandWidth == null || mSelectionMode == null || mValidRanges == null) {
            return;
        }

        float selectRange = SDRLinkHelper.RANGE_SIZE_10MHZ;
        if(mCurBandWidth == Bandwidth.BANDWIDTH_20MHZ) {
            selectRange = SDRLinkHelper.RANGE_SIZE_20MHZ;
        } else if(mCurBandWidth == Bandwidth.BANDWIDTH_40MHZ) {
            selectRange = SDRLinkHelper.RANGE_SIZE_40MHZ;
        }

        float left = mValidRanges[0];
        float right = mValidRanges[1];
        if(left != SDRLinkHelper.ORIGINAL_NF_2DOT4G_START_FREQ) {
            left -= selectRange;
            right += selectRange;
        }

        final Viewport v = new Viewport(mSdrSnrLine.getMaximumViewport());
        v.bottom = 0;
        v.top = LINE_MAX_VALUE;
        v.left = (left - mRssiStartIndex) / 2;
        v.right = (right - mRssiStartIndex) / 2;
        if(v.left < 0) {
            v.left = 0;
        }
        if(v.right > mNumRssiValues) {
            v.right = mNumRssiValues;
        }
        mSdrSnrLine.setMaximumViewport(v);
        mSdrSnrLine.setCurrentViewport(v);

        updateCustomTipVisibility(mSelectionMode);

    }

    private void updateCustomTipVisibility(ChannelSelectionMode selectionMode) {
        if(selectionMode == ChannelSelectionMode.AUTO) {
            mCustomTipTv.setVisibility(GONE);
        } else {
            mCustomTipTv.setVisibility(VISIBLE);
        }
    }

    private void onDistOffsetChanged() {
        mQualityTx.set1KmNfValue(mDisOffset);
        mQualityLine.set1KmNfValue(mDisOffset);
    }

    private void onFreqIndexChanged() {
        FreqBand old = mCurFreqBand;

        if (SDRLinkHelper.isFrequencyIndexIn2dot4G(mFreqIndex)) {
            mCurFreqBand = FreqBand.FREQ_BAND_2DOT4G;
            mNumRssiValues = RSSI_VALUE_NUM_2DOT4G;
            mRssiStartIndex = START_INDEX_2DOT4G;
            mNumNfValues = NF_VALUE_NUM_2DOT4G;
        } else if (SDRLinkHelper.isFrequencyIndexIn5dot8G(mFreqIndex) && !is5Dot7Supported) {
            mCurFreqBand = FreqBand.FREQ_BAND_5DOT8G;
            mNumRssiValues = RSSI_VALUE_NUM_5DOT8G;
            mRssiStartIndex = START_INDEX_5DOT8G;
            mNumNfValues = NF_VALUE_NUM_5DOT8G;
        } else if (SDRLinkHelper.isFrequencyIndex5dot2G(mFreqIndex)) {
            mCurFreqBand = FreqBand.FREQ_BAND_5DOT2G;
            mNumRssiValues = RSSI_VALUE_NUM_5DOT2G;
            mRssiStartIndex = START_INDEX_5DOT2G;
            mNumNfValues = NF_VALUE_NUM_5DOT2G;
        } else if(SDRLinkHelper.isFrequencyIndexIn5dot7G(mFreqIndex)) {
            mCurFreqBand = FreqBand.FREQ_BAND_5DOT7G;
            mNumRssiValues = RSSI_VALUE_NUM_5DOT7G;
            mRssiStartIndex = START_INDEX_5DOT7G;
            mNumNfValues = NF_VALUE_NUM_5DOT7G;
        } else if(SDRLinkHelper.isFrequencyIndexIn840M(mFreqIndex) || SDRLinkHelper.isFrequencyIndexIn1dot4G(mFreqIndex)) {
            mCurFreqBand = FreqBand.FREQ_BAND_1DOT4G;
            mNumRssiValues = RSSI_VALUE_NUM_1DOT4G;
            mRssiStartIndex = START_INDEX_1DOT4G;
            mNumNfValues = NF_VALUE_NUM_1DOT4G;
        }
        if (mCurFreqBand != old) {
            initValues();
            mSdrSnrLine.setLineChartData(mSnrValues);
            resetViewport();
            onValidRangeChanged();
        }
        LogUtils.d(TAG,"onFreqIndexChanged mNumRssiValues: "+mNumRssiValues+"  mCurFreqBand: "+mCurFreqBand+"  old: "+old);
    }
}
