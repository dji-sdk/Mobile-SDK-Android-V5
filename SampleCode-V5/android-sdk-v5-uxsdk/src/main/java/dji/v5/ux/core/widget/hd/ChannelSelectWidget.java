package dji.v5.ux.core.widget.hd;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import dji.sdk.keyvalue.value.airlink.ChannelSelectionMode;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.TabSelectCell;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;

public class ChannelSelectWidget extends ConstraintLayoutWidget<Object> implements ChannelContract.View, TabSelectCell.OnTabChangeListener{
    private static final String TAG = "ChannelSelectWidget";
    TabSelectCell mChannelModeTabCell;
    private boolean mIsChangingChannelMode;

    ChannelSelectWidgetModel widgetModel = new ChannelSelectWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());

    public ChannelSelectWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ChannelSelectWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ChannelSelectWidget(@NonNull Context context) {
        super(context);
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
    public void onTabChanged(TabSelectCell cell, int oldIndex, int newIndex) {
        addDisposable(widgetModel.setChannelSelectionMode(newIndex == 0 ? ChannelSelectionMode.AUTO : ChannelSelectionMode.MANUAL).subscribe(() -> {

        }, throwable -> {
            LogUtils.e(TAG, "setChannelSelectionMode fail: " + throwable);
            onChannelSelectionModeFailed(oldIndex);
            onDidSetChannelSelectionMode();
        }));
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_setting_menu_sdr_channel_select_layout, this);
        mChannelModeTabCell = findViewById(R.id.tsc_setting_menu_channel_select_tab_cell);
        mChannelModeTabCell.setOnTabChangeListener(this);
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getChannelSelectionMode().subscribe(value -> {
            updateChannelMode(ChannelSelectionMode.AUTO == value);
        }));
    }

    @Override
    public void updateSupportDataRates(List<String> bands) {
        //do nothing
    }

    @Override
    public void initSlaveViews() {
        //do nothing
    }

    @Override
    public void updateVideoCameras(boolean enable) {
        //do nothing
    }

    @Override
    public void updateChannelMode(boolean auto) {
        if (mChannelModeTabCell != null) {
            mChannelModeTabCell.setOnTabChangeListener(null);
            mChannelModeTabCell.setCurrentTab(auto ? 0 : 1);
            mChannelModeTabCell.setOnTabChangeListener(this);
        }
    }

    private void onDidSetChannelSelectionMode() {
        mIsChangingChannelMode = false;
        updateChannelModeTabEnable();
    }

    private void updateChannelModeTabEnable() {
        if (mChannelModeTabCell != null) {
            mChannelModeTabCell.setEnabled(!mIsChangingChannelMode);
        }
    }

    private void onChannelSelectionModeFailed(int oldIndex) {
        if (mChannelModeTabCell != null) {
            mChannelModeTabCell.setOnTabChangeListener(null);
            mChannelModeTabCell.setCurrentTab(oldIndex);
            mChannelModeTabCell.setOnTabChangeListener(ChannelSelectWidget.this);
        }
    }
}
