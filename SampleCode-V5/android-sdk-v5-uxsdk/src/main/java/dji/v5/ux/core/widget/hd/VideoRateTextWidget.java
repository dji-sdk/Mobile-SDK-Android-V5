package dji.v5.ux.core.widget.hd;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Locale;

import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.TextCell;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;

/**
 * 图传码率显示控件
 */
public class VideoRateTextWidget extends ConstraintLayoutWidget<Object> {

    private String formatStr = "%.2fMbps";

    private TextCell videoRateTextCell;

    VideoRateTextWidgetModel widgetModel = new VideoRateTextWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());

    public VideoRateTextWidget(Context context) {
        this(context, null);
    }

    public VideoRateTextWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoRateTextWidget(Context context, AttributeSet attrs, int defStyleAttr) {
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

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_setting_menu_sdr_video_rate_text_layout, this);
        videoRateTextCell = findViewById(R.id.tc_setting_menu_sdr_video_rate_text);
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getDynamicDataRate().subscribe(value -> {
            float codeRate = value.floatValue();
            videoRateTextCell.setContent(String.format(Locale.US, formatStr, codeRate));
        }));
    }
}
