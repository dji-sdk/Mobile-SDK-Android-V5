package dji.v5.ux.core.widget.hd;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.sdk.keyvalue.value.airlink.Bandwidth;
import dji.sdk.keyvalue.value.airlink.ChannelSelectionMode;
import dji.sdk.keyvalue.value.airlink.FrequencyBand;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.TextCell;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;

/**
 * 信道模式为自动模式时显示带宽分配
 */
public class BandWidthWidget extends ConstraintLayoutWidget<Object> {
    private ChannelSelectionMode mChannelMode = ChannelSelectionMode.UNKNOWN;
    private FrequencyBand mFrequencyBand = FrequencyBand.UNKNOWN;

    private TextCell bandWidthTextCell;

    BandWidthSelectWidgetModel widgetModel = new BandWidthSelectWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());

    public BandWidthWidget(Context context) {
        this(context, null);
    }

    public BandWidthWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BandWidthWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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


    private void updateVisible() {
        if (mChannelMode == ChannelSelectionMode.AUTO || mFrequencyBand == FrequencyBand.BAND_1_DOT_4G) {
            setVisibility(VISIBLE);
        } else {
            setVisibility(GONE);
        }
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_setting_menu_sdr_band_width_layout, this);
        bandWidthTextCell = findViewById(R.id.tsc_setting_menu_sdr_band_width);
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getBandwidth().subscribe(bandwidth -> {
            if (bandwidth == Bandwidth.BANDWIDTH_10MHZ) {
                bandWidthTextCell.setContent("10MHz");
            } else if (bandwidth == Bandwidth.BANDWIDTH_20MHZ) {
                bandWidthTextCell.setContent("20MHz");
            } else if(bandwidth == Bandwidth.BANDWIDTH_40MHZ) {
                bandWidthTextCell.setContent("40MHz");
            }
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
}
