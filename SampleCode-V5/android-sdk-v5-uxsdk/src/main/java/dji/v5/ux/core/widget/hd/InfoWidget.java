package dji.v5.ux.core.widget.hd;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import dji.sdk.keyvalue.value.airlink.Bandwidth;
import dji.sdk.keyvalue.value.airlink.FrequencyInterferenceInfo;
import dji.sdk.keyvalue.value.airlink.SDRHdOffsetParams;
import dji.v5.utils.common.AndUtil;
import dji.v5.utils.common.SDRLinkHelper;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.TextCell;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;

public class InfoWidget extends ConstraintLayoutWidget<Object> {
    private SignalInfo mSdrSignal = new SignalInfo();;

    /***
     *
     *   计算信号状态
     */
    private List<FrequencyInterferenceInfo> mRssis = new ArrayList<>();

    /** 中心频点的值, 即24xx.5的形式 */
    private Integer mCurNfIndex = Integer.MAX_VALUE;
    /** 频点的范围, 10M范围是+-5, 20M范围是+-10 */
    private int mRangeSize = 5;
    private Integer[] mValidRanges;
    private SDRHdOffsetParams mOffsetParams;

    /** 频点的起始位置  ***/
    private float mNfStartIndex = 2400.5f;
    /** 白色选框最左边, 不一定等于({@link #mCurNfIndex} - {@link #mRangeSize}), 会随着用户拖动的变化而变化 */
    private float mLeftNfIndex = Float.MAX_VALUE;

    private int sdrDownQuality = -1;
    private int sdrUpQuality = -1;
    private int linkSignalQuality = -1;
    private boolean fcConnected = false;
    private SignalLevel mSignalLevel = SignalLevel.LEVEL_0;
    private TextCell sdrInfoTextCell;
    private TextView summaryText ;
    private InfoWidgetModel widgetModel = new InfoWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());

    public InfoWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public InfoWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public InfoWidget(@NonNull Context context) {
        super(context);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            widgetModel.setup();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_setting_menu_sdr_info_text_layout, this);
        sdrInfoTextCell = findViewById(R.id.tc_setting_menu_sdr_info_text);
        summaryText = findViewById(R.id.summaryText);
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getBandwidth().subscribe(value -> {
            mSdrSignal.bandwidth = value;
            summaryText.setText(mSdrSignal.toString());
        }));

        addReaction(widgetModel.getFrequencyInterfaceInfoList().subscribe(value -> {
            mRssis = value;
            updateAverageValue();
            summaryText.setText(mSdrSignal.toString());
        }));

        addReaction(widgetModel.getFrequencyPointIndexRange().subscribe(value -> {
            mValidRanges = new Integer[2];
            mValidRanges[0] = value.getMin();
            mValidRanges[1] = value.getMax();
            onValidRangeChanged();
            summaryText.setText(mSdrSignal.toString());
        }));

        addReaction(widgetModel.getSdrHdOffsetParamsData().subscribe(value -> {
            mOffsetParams = value;
            updateAverageValue();
            summaryText.setText(mSdrSignal.toString());
        }));

        addReaction(widgetModel.getDynamicDataRate().subscribe(value -> {
            mSdrSignal.dataRate = value.floatValue();
            summaryText.setText(mSdrSignal.toString());
        }));

        addReaction(widgetModel.getFrequencyPointIndex().subscribe(value -> {
            mCurNfIndex = value;
            mLeftNfIndex = (float)mCurNfIndex - mRangeSize;
            updateAverageValue();
            summaryText.setText(mSdrSignal.toString());
        }));

        addReaction(widgetModel.getDownLinkQuality().subscribe(value -> {
            sdrDownQuality = value;
            updateSdrSignalLevelLiveData();
        }));

        addReaction(widgetModel.getUpLinkQuality().subscribe(value -> {
            sdrUpQuality = value;
            updateSdrSignalLevelLiveData();
        }));

        addReaction(widgetModel.getLinkSignalQuality().subscribe(value -> {
            linkSignalQuality = value;
            updateSdrSignalLevelLiveData();
        }));

        addReaction(widgetModel.getFcConnection().subscribe(value -> {
            fcConnected = value;
            updateSdrSignalLevelLiveData();
        }));
    }

    public void onValidRangeChanged() {

        if(mSdrSignal.bandwidth == null || mValidRanges == null) {
            return;
        }

        float selectRange = SDRLinkHelper.RANGE_SIZE_10MHZ;
        if(mSdrSignal.bandwidth == Bandwidth.BANDWIDTH_20MHZ) {
            selectRange = SDRLinkHelper.RANGE_SIZE_20MHZ;
        } else if(mSdrSignal.bandwidth == Bandwidth.BANDWIDTH_40MHZ) {
            selectRange = SDRLinkHelper.RANGE_SIZE_40MHZ;
        }

        float left = mValidRanges[0];
        if(left != SDRLinkHelper.ORIGINAL_NF_2DOT4G_START_FREQ
                && left != SDRLinkHelper.ORIGINAL_NF_5DOT8G_START_FREQ) {
            left -= selectRange;
        }

        mNfStartIndex = left;
    }

    private void updateAverageValue() {

        if(mRssis == null || mCurNfIndex == Float.MAX_VALUE || mLeftNfIndex == Float.MAX_VALUE) {
            return;
        }

        int rssiStartIndex = (int) ((mLeftNfIndex - mNfStartIndex) / 2);
        if(rssiStartIndex < 0) {
            rssiStartIndex = 0;
        }
        float average = 0;
        for(int i = rssiStartIndex; i < rssiStartIndex + mRangeSize && i < mRssis.size(); ++i) {
            average += mRssis.get(i).getRssi();
        }
        average = average / mRangeSize;

        // 计算offset值
        if(mOffsetParams != null) {
            average += ((mOffsetParams.getRcLinkOffset().byteValue() > 0 ? mOffsetParams.getRcLinkOffset().byteValue() : 0)
                    + mOffsetParams.getPathLossOffset().byteValue() + mOffsetParams.getTxPowerOffset().byteValue());
        }
        mSdrSignal.strength = average;
    }

    private void updateSdrSignalLevelLiveData() {
        int value = Integer.min(sdrDownQuality, sdrUpQuality);
        if (!fcConnected) {
            mSignalLevel = SignalLevel.LEVEL_0;
        } else {
            if (linkSignalQuality > 0) {
                switch (linkSignalQuality) {
                    case 4:
                    case 5:
                        mSignalLevel = SignalLevel.LEVEL_3;
                        break;
                    case 3:
                        mSignalLevel = SignalLevel.LEVEL_2;
                        break;
                    case 1:
                    case 2:
                        mSignalLevel = SignalLevel.LEVEL_1;
                        break;
                    default:
                        break;
                }
            } else {
                if (value > 60) {
                    mSignalLevel = SignalLevel.LEVEL_3;
                } else if (value <= 60 && value >= 40) {
                    mSignalLevel = SignalLevel.LEVEL_2;
                } else {
                    mSignalLevel = SignalLevel.LEVEL_1;
                }
            }
        }
        setTextCellContent(mSignalLevel);
    }

    private void setTextCellContent(SignalLevel level){
        switch (level) {
            case LEVEL_1:
                sdrInfoTextCell.setContent(getResources().getString(R.string.uxsdk_fpv_top_bar_gps_signal_state_weak));
                sdrInfoTextCell.setContentColor(AndUtil.getResColor(R.color.uxsdk_tips_danger_in_dark));
                break;
            case LEVEL_2:
                sdrInfoTextCell.setContent(getResources().getString(R.string.uxsdk_fpv_top_bar_gps_signal_state_normal));
                sdrInfoTextCell.setContentColor(AndUtil.getResColor(R.color.uxsdk_tips_caution_in_dark));
                break;
            case LEVEL_3:
                sdrInfoTextCell.setContent(getResources().getString(R.string.uxsdk_fpv_top_bar_gps_signal_state_strong));
                sdrInfoTextCell.setContentColor(AndUtil.getResColor(R.color.uxsdk_green));
                break;
            default:
                sdrInfoTextCell.setContent("N/A");
                sdrInfoTextCell.setContentColor(AndUtil.getResColor(R.color.uxsdk_tips_danger_in_dark));
                break;
        }
    }

    private enum SignalLevel {
        /**
         * 飞机断开，红色叉叉
         */
        LEVEL_0,
        /**
         * 红色
         */
        LEVEL_1,
        /**
         * 黄色
         */
        LEVEL_2,
        /**
         * 白色
         */
        LEVEL_3
    }
}
