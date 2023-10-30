package dji.v5.ux.core.base;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import dji.v5.ux.R;


public class ProgressStatusWidget extends DividerLinearLayout {

    public TextView mValueView;
    public TextView mDescView;
    public ProgressBar mProgressBar;

    public ProgressStatusWidget(Context context) {
        this(context, null);
    }

    public ProgressStatusWidget(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressStatusWidget(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ProgressStatusWidget(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initialize(Context context) {
        LayoutInflater.from(context).inflate(R.layout.uxsdk_widget_progress_bar_layout, this, true);

        if (isInEditMode()) {
            return;
        }
        setBottomDividerEnable(false);
        mValueView = (TextView) findViewById(R.id.status_widget_value);
        mDescView = (TextView) findViewById(R.id.status_widget_desc);
        mProgressBar = (ProgressBar) findViewById(R.id.status_widget_progress);
    }

    public String getValue() {
        return mValueView.getText().toString();
    }

    public void setValue(String value) {
        mValueView.setText(value);
    }

    public String getDesc() {
        return mDescView.getText().toString();
    }

    public void setDesc(String desc) {
        mDescView.setText(desc);
    }

    public int getProgress() {
        return mProgressBar.getProgress();
    }

    public void setProgress(int progress) {
        mProgressBar.setProgress(progress);
    }

    public void setProgressDrawable(Drawable drawable) {
        mProgressBar.setProgressDrawable(drawable);
    }
}
