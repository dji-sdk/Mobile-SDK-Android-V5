package dji.v5.ux.core.widget.hd;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import dji.sdk.keyvalue.value.airlink.Bandwidth;
import dji.sdk.keyvalue.value.airlink.ChannelSelectionMode;
import dji.sdk.keyvalue.value.airlink.FrequencyBand;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.TabSelectCell;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;

/**
 * SDR 带宽模式选择
 */
public class BandWidthSelectWidget extends ConstraintLayoutWidget<Object> implements TabSelectCell.OnTabChangeListener{
    private static final String TAG = "BandWidthSelectWidget";
    private Bandwidth mBandwidth = Bandwidth.UNKNOWN;
    private List<DJIBandwidth> mBandWidthItems = new ArrayList<>();
    private ChannelSelectionMode mChannelMode = ChannelSelectionMode.UNKNOWN;
    private FrequencyBand mFrequencyBand = FrequencyBand.UNKNOWN;

    private TabSelectCell sdrBandWidthSelectTab;

    BandWidthSelectWidgetModel widgetModel = new BandWidthSelectWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());

    public BandWidthSelectWidget(Context context) {
        this(context, null);
    }

    public BandWidthSelectWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BandWidthSelectWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            widgetModel.setup();
        }

        initBandwidthItems();
        initBandwidthItemsUI();
        setVisibility(GONE);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
    }

    private void updateBandwidthSelect() {
        if(mBandwidth != Bandwidth.UNKNOWN) {
            int position = findBandwidthPosition(mBandwidth);
            if(position >= 0) {
                sdrBandWidthSelectTab.setOnTabChangeListener(null);
                sdrBandWidthSelectTab.setCurrentTab(position);
                sdrBandWidthSelectTab.setOnTabChangeListener(this);
            }
        }
    }

    private int findBandwidthPosition(Bandwidth bandwidth) {

        if (mBandWidthItems.isEmpty()) {
            return -1;
        }

        DJIBandwidth bandwidthMode = DJIBandwidth.find(bandwidth);
        return mBandWidthItems.indexOf(bandwidthMode);
    }

    private void initBandwidthItemsUI() {
        if(!mBandWidthItems.isEmpty()) {
            List<String> items = new ArrayList<>();
            for(DJIBandwidth  bandwidth : mBandWidthItems) {
                items.add(bandwidth.getName());
            }
            int position = findBandwidthPosition(mBandwidth);
            position = Math.max(position, 0);
            sdrBandWidthSelectTab.setOnTabChangeListener(null);
            sdrBandWidthSelectTab.addTabs(position, items);
            sdrBandWidthSelectTab.setOnTabChangeListener(this);
        }
    }

    private void initBandwidthItems() {
        mBandWidthItems.clear();
        mBandWidthItems.add(DJIBandwidth.BW_40);
        mBandWidthItems.add(DJIBandwidth.BW_20);
        mBandWidthItems.add(DJIBandwidth.BW_10);
    }

    private void updateVisible() {
        if (mChannelMode == ChannelSelectionMode.AUTO
            // 1.4G 只有 10M 不能选择
            || mFrequencyBand == FrequencyBand.BAND_1_DOT_4G) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
        }
    }

    @Override
    public void onTabChanged(TabSelectCell cell, int oldIndex, int newIndex) {
        if (oldIndex == newIndex) {
            return;
        }
        if (newIndex < mBandWidthItems.size()) {
            Bandwidth bandwidth = mBandWidthItems.get(newIndex).getValue();
            addDisposable(widgetModel.setBandwidth(bandwidth).subscribe(() -> {

            }, throwable -> {
                LogUtils.e(TAG, "setFrequencyBand fail: " + throwable);
                updateBandwidthSelect();
            }));
        }
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_setting_menu_sdr_band_width_select_layout, this);
        sdrBandWidthSelectTab = findViewById(R.id.tsc_setting_menu_sdr_band_width_select);
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getBandwidth().subscribe(value -> {
            mBandwidth = value;
            int position = findBandwidthPosition(mBandwidth);
            if(position >= 0) {
                sdrBandWidthSelectTab.setOnTabChangeListener(null);
                sdrBandWidthSelectTab.setCurrentTab(position);
                sdrBandWidthSelectTab.setOnTabChangeListener(this);
            }
            updateBandwidthSelect();
        }));
        addReaction(widgetModel.getChannelSelectionMode().subscribe(value -> {
            mChannelMode = value;
            updateVisible();
        }));
        addReaction(widgetModel.getFrequencyBand().subscribe(value -> {
            mFrequencyBand = value;
            updateVisible();
        }));
    }

    public enum DJIBandwidth {

        BW_40("40MHz", Bandwidth.BANDWIDTH_40MHZ),
        BW_20("20MHz", Bandwidth.BANDWIDTH_20MHZ),
        BW_10("10MHz", Bandwidth.BANDWIDTH_10MHZ),
        UNKNOWN("", Bandwidth.UNKNOWN);
        private String mName;
        private Bandwidth mValue;

        DJIBandwidth(String name, Bandwidth value) {
            mName = name;
            mValue = value;
        }

        public String getName() {
            return mName;
        }

        public Bandwidth getValue() {
            return mValue;
        }

        public static DJIBandwidth find(Bandwidth value) {
            DJIBandwidth target = UNKNOWN;
            for(DJIBandwidth bandwidth : values()) {
                if(bandwidth.mValue == value) {
                    target = bandwidth;
                    break;
                }
            }
            return target;
        }
    }
}
