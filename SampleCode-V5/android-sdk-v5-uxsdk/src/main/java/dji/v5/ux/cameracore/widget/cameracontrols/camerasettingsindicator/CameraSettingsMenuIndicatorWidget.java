/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.v5.ux.cameracore.widget.cameracontrols.camerasettingsindicator;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.v5.ux.R;
import dji.v5.ux.cameracore.widget.cameracontrols.CameraControlsWidget;
import dji.v5.ux.core.base.widget.FrameLayoutWidget;
import dji.v5.ux.core.communication.OnStateChangeCallback;

/**
 * Camera Settings Menu Indicator Widget
 * <p>
 * The widget is part of the {@link CameraControlsWidget}.
 * Tapping the widget can be used to open the camera settings panel
 */
public class CameraSettingsMenuIndicatorWidget extends FrameLayoutWidget<Object> implements View.OnClickListener {

    //region Fields
    private TextView foregroundTextView;
    private OnStateChangeCallback<Object> stateChangeCallback = null;
    //endregion

    //region Lifecycle
    public CameraSettingsMenuIndicatorWidget(@NonNull Context context) {
        super(context);
    }

    public CameraSettingsMenuIndicatorWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraSettingsMenuIndicatorWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_camera_settings_menu_indicator, this);
        foregroundTextView = findViewById(R.id.text_view_menu);
        setOnClickListener(this);

        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }

    @Override
    protected void reactToModelChanges() {
        // No Code
    }

    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_default_ratio);
    }

    @Override
    public void onClick(View v) {
        if (stateChangeCallback != null) {
            stateChangeCallback.onStateChange(null);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        initializeListener();
    }

    @Override
    protected void onDetachedFromWindow() {
        destroyListener();
        super.onDetachedFromWindow();
    }

    //endregion

    //region private methods

    private void initializeListener() {
        //暂未实现
    }

    private void destroyListener() {
        stateChangeCallback = null;
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CameraSettingsMenuIndicatorWidget);

        setLabelTextSize(typedArray.getDimension(R.styleable.CameraSettingsMenuIndicatorWidget_uxsdk_settingsTextSize, 12));
        setLabelTextColor(typedArray.getColor(R.styleable.CameraSettingsMenuIndicatorWidget_uxsdk_settingsTextColor, Color.WHITE));
        setLabelTextBackground(typedArray.getDrawable(R.styleable.CameraSettingsMenuIndicatorWidget_uxsdk_settingsTextBackground));
        typedArray.recycle();
    }

    //endregion

    //region customizations

    /**
     * Set callback for when the widget is tapped.
     * This can be used to link the widget to //TODO camera settings menu panel
     *
     * @param stateChangeCallback listener to handheld callback
     */
    public void setStateChangeCallback(@NonNull OnStateChangeCallback<Object> stateChangeCallback) {
        this.stateChangeCallback = stateChangeCallback;
    }

    /**
     * Get the color of the widget label text
     *
     * @return integer color value
     */
    @ColorInt
    public int getLabelTextColor() {
        return foregroundTextView.getCurrentTextColor();
    }

    /**
     * Set the color of the widget label text
     *
     * @param color integer value to be used
     */
    public void setLabelTextColor(@ColorInt int color) {
        foregroundTextView.setTextColor(color);
    }

    /**
     * Set the background of the widget label
     *
     * @param resourceId to be used
     */
    public void setLabelTextBackground(@DrawableRes int resourceId) {
        foregroundTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the widget label
     *
     * @param drawable to be used
     */
    public void setLabelTextBackground(@Nullable Drawable drawable) {
        foregroundTextView.setBackground(drawable);
    }

    /**
     * Get the background of the widget label
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getLabelBackground() {
        return foregroundTextView.getBackground();
    }

    /**
     * Get the text size of the widget label
     *
     * @return float value representing text size
     */
    @Dimension
    public float getLabelTextSize() {
        return foregroundTextView.getTextSize();
    }

    /**
     * Set the text size of the widget label
     *
     * @param textSize float value representing text size
     */
    public void setLabelTextSize(@Dimension float textSize) {
        foregroundTextView.setTextSize(textSize);
    }
    //endregion
}
