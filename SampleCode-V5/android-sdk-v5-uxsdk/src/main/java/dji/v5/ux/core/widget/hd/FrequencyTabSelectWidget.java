package dji.v5.ux.core.widget.hd;

import android.content.Context;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.sdk.keyvalue.value.airlink.ChannelSelectionMode;
import dji.sdk.keyvalue.value.airlink.FrequencyBand;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.TabSelectCell;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;


public class FrequencyTabSelectWidget extends ConstraintLayoutWidget<Object> implements TabSelectCell.OnTabChangeListener {
    private static final String TAG = "FrequencyTabSelectWidget";

    private TabSelectCell frequencySelectTab;
    private FrequencyBand mOcuSyncFrequencyBand = FrequencyBand.BAND_MULTI;

    private List<FrequencyBand> mSupportOcuSyncFrequencyBands;

    private ChannelSelectionMode mChannelSelectionMode;

    private FrequencyTabSelectWidgetModel widgetModel = new FrequencyTabSelectWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());

    private String[] mAllFrequencyNames;

    public FrequencyTabSelectWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FrequencyTabSelectWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FrequencyTabSelectWidget(@NonNull Context context) {
        super(context);
    }

    @Override
    public void onTabChanged(TabSelectCell cell, int oldIndex, int newIndex) {
        if (oldIndex == newIndex) {
            return;
        }

        if (mSupportOcuSyncFrequencyBands != null && newIndex < mSupportOcuSyncFrequencyBands.size()) {
            FrequencyBand newBand = mSupportOcuSyncFrequencyBands.get(newIndex);
            if (newBand != mOcuSyncFrequencyBand) {
                addDisposable(widgetModel.setFrequencyBand(newBand).subscribe(() -> {

                }, throwable -> {
                    LogUtils.e(TAG, "setFrequencyBand fail: " + throwable);
                    frequencySelectTab.setCurrentTab(oldIndex);
                }));
            }
        }
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

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_setting_menu_frequency_tab_select_layout, this);
        frequencySelectTab = findViewById(R.id.tsc_setting_menu_hd_frequency_tab_cell);
        frequencySelectTab.setOnTabChangeListener(this);
        mAllFrequencyNames = getResources().getStringArray(R.array.uxsdk_sdr_frequency_names);
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getFrequencyBand().subscribe(value -> {
            mOcuSyncFrequencyBand = value;
            updateSelection();
        }));
        addReaction(widgetModel.getFrequencyBandRange().subscribe(value->{
            mSupportOcuSyncFrequencyBands = value;
            List<String> names = new ArrayList<>();
            boolean is1dot4Support = find1dot4Band(mSupportOcuSyncFrequencyBands);
            if(mSupportOcuSyncFrequencyBands != null && mAllFrequencyNames != null) {
                for (FrequencyBand band : mSupportOcuSyncFrequencyBands) {
                    if (band.value() < mAllFrequencyNames.length) {
                        if (band == FrequencyBand.BAND_MULTI
                                && mChannelSelectionMode != ChannelSelectionMode.AUTO) {
                            continue;
                        }
                        names.add(mAllFrequencyNames[convertOcuSyncBandValue(band, is1dot4Support)]);
                    }
                }
            }
            frequencySelectTab.addTabs(0, names);
            updateSelection();
        }));
        addReaction(widgetModel.getChannelSelectionMode().subscribe(value->{
            mChannelSelectionMode = value;
            updateSelection();
        }));
        addReaction(widgetModel.getConnection().subscribe(isConnected->{
            frequencySelectTab.setEnabled(isConnected);
            if (Boolean.TRUE.equals(isConnected)) {
                return;
            }
            frequencySelectTab.setCurrentTab(0);
        }));
    }

    private void updateSelection() {
        if (mSupportOcuSyncFrequencyBands != null) {
            for (int i = 0; i < mSupportOcuSyncFrequencyBands.size(); i++) {
                FrequencyBand band = mSupportOcuSyncFrequencyBands.get(i);
                if (band.value() == mOcuSyncFrequencyBand.value()) {
                    frequencySelectTab.setCurrentTab(i);
                    break;
                }
            }
        }
    }

    private boolean find1dot4Band(List<FrequencyBand> bands) {

        if (bands != null) {
            for (FrequencyBand band : bands) {
                if (band == FrequencyBand.BAND_1_DOT_4G) {
                    return true;
                }
            }
        }

        return false;
    }


    public int convertOcuSyncBandValue(FrequencyBand band, boolean is1Dot4Support) {
        if (band == null) {
            return 0;
        }

        switch (band) {
            case BAND_MULTI:
                if (is1Dot4Support) {
                    //三频
                    return 5;
                } else {
                    //双频
                    return 0;
                }
            case BAND_2_DOT_4G:
                return 1;
            case BAND_5_DOT_8G:
                return 2;
            case BAND_1_DOT_4G:
                return 3;
            case BAND_5_DOT_7G:
                return 4;
            default:
                return 0;
        }
    }
}
