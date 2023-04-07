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

package dji.v5.ux.visualcamera.wb;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import dji.sdk.keyvalue.value.camera.CameraWhiteBalanceInfo;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.utils.common.DisplayUtil;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;

/**
 * Shows the camera's current white balance.
 */
public class CameraConfigWBWidget extends ConstraintLayoutWidget<Object> implements ICameraIndex {
    //region Constants
    private final static String EMPTY_STRING = "";
    private final static int COLOR_TEMP_MULTIPLIER = 100;
    //endregion

    //region Fields
    private CameraConfigWBWidgetModel widgetModel;
    private TextView wbTitleTextView;
    private TextView wbValueTextView;
    private String[] wbNameArray;
    //endregion

    //region Constructor
    public CameraConfigWBWidget(@NonNull Context context) {
        super(context);
    }

    public CameraConfigWBWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraConfigWBWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_base_camera_info, this);
        wbTitleTextView = findViewById(R.id.textview_title);
        wbValueTextView = findViewById(R.id.textview_value);

        if (!isInEditMode()) {
            widgetModel = new CameraConfigWBWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance());
            wbTitleTextView.setText(getResources().getString(R.string.uxsdk_white_balance_title, EMPTY_STRING));
            wbNameArray = getResources().getStringArray(R.array.uxsdk_camera_white_balance_name_array);
        }

        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }
    //endregion

    //region LifeCycle
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            widgetModel.setup();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getWhiteBalance()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateUI));
    }
    //endregion

    //region Reactions to model
    private void updateUI(@NonNull CameraWhiteBalanceInfo whiteBalance) {
        int wbPresetValue = whiteBalance.getWhiteBalanceMode().value();
        if (wbPresetValue < wbNameArray.length) {
            wbTitleTextView.setText(getResources().getString(R.string.uxsdk_white_balance_title,
                    wbNameArray[wbPresetValue]));
            wbValueTextView.setText(getResources().getString(R.string.uxsdk_white_balance_temp,
                    whiteBalance.getColorTemperature() * COLOR_TEMP_MULTIPLIER));
        } else {
            wbTitleTextView.setText(getResources().getString(R.string.uxsdk_white_balance_title, EMPTY_STRING));
            wbValueTextView.setText(getResources().getString(R.string.uxsdk_string_default_value));
        }
    }
    //endregion

    //region Customizations
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_base_camera_info_ratio);
    }

    @Override
    @NonNull
    public ComponentIndexType getCameraIndex() {
        return widgetModel.getCameraIndex();
    }

    @Override
    public void updateCameraSource(@NonNull ComponentIndexType cameraIndex, @NonNull CameraLensType lensType) {
        widgetModel.updateCameraSource(cameraIndex, lensType);
    }

    @Override
    @NonNull
    public CameraLensType getLensType() {
        return widgetModel.getLensType();
    }

    /**
     * Set text appearance of the white balance title text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setWBTitleTextAppearance(@StyleRes int textAppearance) {
        wbTitleTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the white balance title text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getWBTitleTextColors() {
        return wbTitleTextView.getTextColors();
    }

    /**
     * Get current text color of the white balance title text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getWBTitleTextColor() {
        return wbTitleTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the white balance title text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setWBTitleTextColor(@NonNull ColorStateList colorStateList) {
        wbTitleTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the white balance title text view
     *
     * @param color color integer resource
     */
    public void setWBTitleTextColor(@ColorInt int color) {
        wbTitleTextView.setTextColor(color);
    }

    /**
     * Get current text size of the white balance title text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getWBTitleTextSize() {
        return wbTitleTextView.getTextSize();
    }

    /**
     * Set the text size of the white balance title text view
     *
     * @param textSize text size float value
     */
    public void setWBTitleTextSize(@Dimension float textSize) {
        wbTitleTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the white balance title text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getWBTitleTextBackground() {
        return wbTitleTextView.getBackground();
    }

    /**
     * Set the resource ID for the background of the white balance title text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setWBTitleTextBackground(@DrawableRes int resourceId) {
        wbTitleTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background for the white balance title text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setWBTitleTextBackground(@Nullable Drawable drawable) {
        wbTitleTextView.setBackground(drawable);
    }

    /**
     * Set text appearance of the white balance value text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setWBValueTextAppearance(@StyleRes int textAppearance) {
        wbValueTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the white balance value text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getWBValueTextColors() {
        return wbValueTextView.getTextColors();
    }

    /**
     * Get current text color of the white balance value text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getWBValueTextColor() {
        return wbValueTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the white balance value text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setWBValueTextColor(@NonNull ColorStateList colorStateList) {
        wbValueTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the white balance value text view
     *
     * @param color color integer resource
     */
    public void setWBValueTextColor(@ColorInt int color) {
        wbValueTextView.setTextColor(color);
    }

    /**
     * Get current text size of the white balance value text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getWBValueTextSize() {
        return wbValueTextView.getTextSize();
    }

    /**
     * Set the text size of the white balance value text view
     *
     * @param textSize text size float value
     */
    public void setWBValueTextSize(@Dimension float textSize) {
        wbValueTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the white balance value text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getWBValueTextBackground() {
        return wbValueTextView.getBackground();
    }

    /**
     * Set the resource ID for the background of the white balance value text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setWBValueTextBackground(@DrawableRes int resourceId) {
        wbValueTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background for the white balance value text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setWBValueTextBackground(@Nullable Drawable drawable) {
        wbValueTextView.setBackground(drawable);
    }
    //endregion

    //region Customization helpers
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CameraConfigWBWidget);

        if (!isInEditMode()) {
            updateCameraSource(ComponentIndexType.find(typedArray.getInt(R.styleable.CameraConfigWBWidget_uxsdk_cameraIndex, 0)),
                    CameraLensType.find(typedArray.getInt(R.styleable.CameraConfigWBWidget_uxsdk_lensType, 0)));
        }

        int wbTitleTextAppearanceId =
                typedArray.getResourceId(R.styleable.CameraConfigWBWidget_uxsdk_wbTitleTextAppearance, INVALID_RESOURCE);
        if (wbTitleTextAppearanceId != INVALID_RESOURCE) {
            setWBTitleTextAppearance(wbTitleTextAppearanceId);
        }

        float wbTitleTextSize =
                typedArray.getDimension(R.styleable.CameraConfigWBWidget_uxsdk_wbTitleTextSize, INVALID_RESOURCE);
        if (wbTitleTextSize != INVALID_RESOURCE) {
            setWBTitleTextSize(DisplayUtil.pxToSp(context, wbTitleTextSize));
        }

        int wbTitleTextColor = typedArray.getColor(R.styleable.CameraConfigWBWidget_uxsdk_wbTitleTextColor, INVALID_COLOR);
        if (wbTitleTextColor != INVALID_COLOR) {
            setWBTitleTextColor(wbTitleTextColor);
        }

        Drawable wbTitleTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.CameraConfigWBWidget_uxsdk_wbTitleBackgroundDrawable);
        if (wbTitleTextBackgroundDrawable != null) {
            setWBTitleTextBackground(wbTitleTextBackgroundDrawable);
        }

        int wbValueTextAppearanceId =
                typedArray.getResourceId(R.styleable.CameraConfigWBWidget_uxsdk_wbValueTextAppearance, INVALID_RESOURCE);
        if (wbValueTextAppearanceId != INVALID_RESOURCE) {
            setWBValueTextAppearance(wbValueTextAppearanceId);
        }

        float wbValueTextSize =
                typedArray.getDimension(R.styleable.CameraConfigWBWidget_uxsdk_wbValueTextSize, INVALID_RESOURCE);
        if (wbValueTextSize != INVALID_RESOURCE) {
            setWBValueTextSize(DisplayUtil.pxToSp(context, wbValueTextSize));
        }

        int wbValueTextColor = typedArray.getColor(R.styleable.CameraConfigWBWidget_uxsdk_wbValueTextColor, INVALID_COLOR);
        if (wbValueTextColor != INVALID_COLOR) {
            setWBValueTextColor(wbValueTextColor);
        }

        Drawable wbValueTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.CameraConfigWBWidget_uxsdk_wbValueBackgroundDrawable);
        if (wbValueTextBackgroundDrawable != null) {
            setWBValueTextBackground(wbValueTextBackgroundDrawable);
        }
        typedArray.recycle();
    }
    //endregion
}
