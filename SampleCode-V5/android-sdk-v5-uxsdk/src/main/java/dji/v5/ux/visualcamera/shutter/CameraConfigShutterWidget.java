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

package dji.v5.ux.visualcamera.shutter;

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
import dji.sdk.keyvalue.value.camera.CameraShutterSpeed;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.utils.common.DisplayUtil;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.CameraUtil;

/**
 * Shows the camera's current shutter speed.
 */
public class CameraConfigShutterWidget extends ConstraintLayoutWidget<Object> implements ICameraIndex {

    //region Fields
    private CameraConfigShutterWidgetModel widgetModel;
    private TextView shutterTitleTextView;
    private TextView shutterValueTextView;
    //endregion

    //region Constructor
    public CameraConfigShutterWidget(@NonNull Context context) {
        super(context);
    }

    public CameraConfigShutterWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraConfigShutterWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_base_camera_info, this);
        shutterTitleTextView = findViewById(R.id.textview_title);
        shutterValueTextView = findViewById(R.id.textview_value);

        if (!isInEditMode()) {
            widgetModel = new CameraConfigShutterWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance());
            shutterTitleTextView.setText(getResources().getString(R.string.uxsdk_shutter_title));
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
        addReaction(widgetModel.getShutterSpeed().observeOn(SchedulerProvider.ui()).subscribe(this::updateUI));
    }
    //endregion

    //region Reactions to model
    private void updateUI(@NonNull CameraShutterSpeed shutterSpeed) {
        shutterValueTextView.setText(CameraUtil.shutterSpeedDisplayName(shutterSpeed));
    }

    //endregion

    //region Customizations
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_base_camera_info_ratio);
    }

    @NonNull
    public ComponentIndexType getCameraIndex() {
        return widgetModel.getCameraIndex();
    }

    @Override
    public void updateCameraSource(@NonNull ComponentIndexType cameraIndex, @NonNull CameraLensType lensType) {
        widgetModel.updateCameraSource(cameraIndex, lensType);
    }

    @NonNull
    public CameraLensType getLensType() {
        return widgetModel.getLensType();
    }

    /**
     * Set text appearance of the shutter title text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setShutterTitleTextAppearance(@StyleRes int textAppearance) {
        shutterTitleTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the shutter title text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getShutterTitleTextColors() {
        return shutterTitleTextView.getTextColors();
    }

    /**
     * Get current text color of the shutter title text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getShutterTitleTextColor() {
        return shutterTitleTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the shutter title text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setShutterTitleTextColor(@NonNull ColorStateList colorStateList) {
        shutterTitleTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the shutter title text view
     *
     * @param color color integer resource
     */
    public void setShutterTitleTextColor(@ColorInt int color) {
        shutterTitleTextView.setTextColor(color);
    }

    /**
     * Get current text size of the shutter title text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getShutterTitleTextSize() {
        return shutterTitleTextView.getTextSize();
    }

    /**
     * Set the text size of the shutter title text view
     *
     * @param textSize text size float value
     */
    public void setShutterTitleTextSize(@Dimension float textSize) {
        shutterTitleTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the shutter title text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getShutterTitleTextBackground() {
        return shutterTitleTextView.getBackground();
    }

    /**
     * Set the resource ID for the background of the shutter title text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setShutterTitleTextBackground(@DrawableRes int resourceId) {
        shutterTitleTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background for the shutter title text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setShutterTitleTextBackground(@Nullable Drawable drawable) {
        shutterTitleTextView.setBackground(drawable);
    }

    /**
     * Set text appearance of the shutter value text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setShutterValueTextAppearance(@StyleRes int textAppearance) {
        shutterValueTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the shutter value text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getShutterValueTextColors() {
        return shutterValueTextView.getTextColors();
    }

    /**
     * Get current text color of the shutter value text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getShutterValueTextColor() {
        return shutterValueTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the shutter value text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setShutterValueTextColor(@NonNull ColorStateList colorStateList) {
        shutterValueTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the shutter value text view
     *
     * @param color color integer resource
     */
    public void setShutterValueTextColor(@ColorInt int color) {
        shutterValueTextView.setTextColor(color);
    }

    /**
     * Get current text size of the shutter value text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getShutterValueTextSize() {
        return shutterValueTextView.getTextSize();
    }

    /**
     * Set the text size of the shutter value text view
     *
     * @param textSize text size float value
     */
    public void setShutterValueTextSize(@Dimension float textSize) {
        shutterValueTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the shutter value text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getShutterValueTextBackground() {
        return shutterValueTextView.getBackground();
    }

    /**
     * Set the resource ID for the background of the shutter value text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setShutterValueTextBackground(@DrawableRes int resourceId) {
        shutterValueTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background for the shutter value text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setShutterValueTextBackground(@Nullable Drawable drawable) {
        shutterValueTextView.setBackground(drawable);
    }
    //endregion

    //region Customization helpers
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CameraConfigShutterWidget);

        if (!isInEditMode()){
            updateCameraSource(ComponentIndexType.find(typedArray.getInt(R.styleable.CameraConfigShutterWidget_uxsdk_cameraIndex, 0)),
                    CameraLensType.find(typedArray.getInt(R.styleable.CameraConfigShutterWidget_uxsdk_lensType, 0)));
        }
        int shutterTitleTextAppearanceId =
                typedArray.getResourceId(R.styleable.CameraConfigShutterWidget_uxsdk_shutterTitleTextAppearance,
                        INVALID_RESOURCE);
        if (shutterTitleTextAppearanceId != INVALID_RESOURCE) {
            setShutterTitleTextAppearance(shutterTitleTextAppearanceId);
        }

        float shutterTitleTextSize =
                typedArray.getDimension(R.styleable.CameraConfigShutterWidget_uxsdk_shutterTitleTextSize, INVALID_RESOURCE);
        if (shutterTitleTextSize != INVALID_RESOURCE) {
            setShutterTitleTextSize(DisplayUtil.pxToSp(context, shutterTitleTextSize));
        }

        int shutterTitleTextColor =
                typedArray.getColor(R.styleable.CameraConfigShutterWidget_uxsdk_shutterTitleTextColor, INVALID_COLOR);
        if (shutterTitleTextColor != INVALID_COLOR) {
            setShutterTitleTextColor(shutterTitleTextColor);
        }

        Drawable shutterTitleTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.CameraConfigShutterWidget_uxsdk_shutterTitleBackgroundDrawable);
        if (shutterTitleTextBackgroundDrawable != null) {
            setShutterTitleTextBackground(shutterTitleTextBackgroundDrawable);
        }

        int shutterValueTextAppearanceId =
                typedArray.getResourceId(R.styleable.CameraConfigShutterWidget_uxsdk_shutterValueTextAppearance,
                        INVALID_RESOURCE);
        if (shutterValueTextAppearanceId != INVALID_RESOURCE) {
            setShutterValueTextAppearance(shutterValueTextAppearanceId);
        }

        float shutterValueTextSize =
                typedArray.getDimension(R.styleable.CameraConfigShutterWidget_uxsdk_shutterValueTextSize, INVALID_RESOURCE);
        if (shutterValueTextSize != INVALID_RESOURCE) {
            setShutterValueTextSize(DisplayUtil.pxToSp(context, shutterValueTextSize));
        }

        int shutterValueTextColor =
                typedArray.getColor(R.styleable.CameraConfigShutterWidget_uxsdk_shutterValueTextColor, INVALID_COLOR);
        if (shutterValueTextColor != INVALID_COLOR) {
            setShutterValueTextColor(shutterValueTextColor);
        }

        Drawable shutterValueTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.CameraConfigShutterWidget_uxsdk_shutterValueBackgroundDrawable);
        if (shutterValueTextBackgroundDrawable != null) {
            setShutterValueTextBackground(shutterValueTextBackgroundDrawable);
        }
        typedArray.recycle();
    }
    //endregion
}