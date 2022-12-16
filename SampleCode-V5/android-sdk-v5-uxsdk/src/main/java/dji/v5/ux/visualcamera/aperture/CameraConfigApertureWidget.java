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

package dji.v5.ux.visualcamera.aperture;

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
import dji.sdk.keyvalue.value.camera.CameraAperture;
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
 * Shows the camera's current aperture.
 */
public class CameraConfigApertureWidget extends ConstraintLayoutWidget<Object> implements ICameraIndex {
    //region Fields
    private CameraConfigApertureWidgetModel widgetModel;
    private TextView apertureTitleTextView;
    private TextView apertureValueTextView;
    //endregion

    //region Constructor
    public CameraConfigApertureWidget(@NonNull Context context) {
        super(context);
    }

    public CameraConfigApertureWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraConfigApertureWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_base_camera_info, this);
        apertureTitleTextView = findViewById(R.id.textview_title);
        apertureValueTextView = findViewById(R.id.textview_value);

        if (!isInEditMode()) {
            widgetModel = new CameraConfigApertureWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance());
            apertureTitleTextView.setText(getResources().getString(R.string.uxsdk_aperture_title));
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
        addReaction(widgetModel.getAperture()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateUI));
    }
    //endregion

    //region Reactions to model
    private void updateUI(@NonNull CameraAperture aperture) {
        apertureValueTextView.setText(CameraUtil.apertureDisplayName(getResources(), aperture));
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
     * Set text appearance of the aperture title text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setApertureTitleTextAppearance(@StyleRes int textAppearance) {
        apertureTitleTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the aperture title text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getApertureTitleTextColors() {
        return apertureTitleTextView.getTextColors();
    }

    /**
     * Get current text color of the aperture title text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getApertureTitleTextColor() {
        return apertureTitleTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the aperture title text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setApertureTitleTextColor(@NonNull ColorStateList colorStateList) {
        apertureTitleTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the aperture title text view
     *
     * @param color color integer resource
     */
    public void setApertureTitleTextColor(@ColorInt int color) {
        apertureTitleTextView.setTextColor(color);
    }

    /**
     * Get current text size of the aperture title text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getApertureTitleTextSize() {
        return apertureTitleTextView.getTextSize();
    }

    /**
     * Set the text size of the aperture title text view
     *
     * @param textSize text size float value
     */
    public void setApertureTitleTextSize(@Dimension float textSize) {
        apertureTitleTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the aperture title text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getApertureTitleTextBackground() {
        return apertureTitleTextView.getBackground();
    }

    /**
     * Set the resource ID for the background of the aperture title text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setApertureTitleTextBackground(@DrawableRes int resourceId) {
        apertureTitleTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background for the aperture title text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setApertureTitleTextBackground(@Nullable Drawable drawable) {
        apertureTitleTextView.setBackground(drawable);
    }

    /**
     * Set text appearance of the aperture value text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setApertureValueTextAppearance(@StyleRes int textAppearance) {
        apertureValueTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the aperture value text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getApertureValueTextColors() {
        return apertureValueTextView.getTextColors();
    }

    /**
     * Get current text color of the aperture value text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getApertureValueTextColor() {
        return apertureValueTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the aperture value text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setApertureValueTextColor(@NonNull ColorStateList colorStateList) {
        apertureValueTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the aperture value text view
     *
     * @param color color integer resource
     */
    public void setApertureValueTextColor(@ColorInt int color) {
        apertureValueTextView.setTextColor(color);
    }

    /**
     * Get current text size of the aperture value text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getApertureValueTextSize() {
        return apertureValueTextView.getTextSize();
    }

    /**
     * Set the text size of the aperture value text view
     *
     * @param textSize text size float value
     */
    public void setApertureValueTextSize(@Dimension float textSize) {
        apertureValueTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the aperture value text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getApertureValueTextBackground() {
        return apertureValueTextView.getBackground();
    }

    /**
     * Set the resource ID for the background of the aperture value text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setApertureValueTextBackground(@DrawableRes int resourceId) {
        apertureValueTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background for the aperture value text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setApertureValueTextBackground(@Nullable Drawable drawable) {
        apertureValueTextView.setBackground(drawable);
    }
    //endregion

    //region Customization helpers
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CameraConfigApertureWidget);

        if (!isInEditMode()){
            updateCameraSource(ComponentIndexType.find(typedArray.getInt(R.styleable.CameraConfigApertureWidget_uxsdk_cameraIndex, 0)),
                    CameraLensType.find(typedArray.getInt(R.styleable.CameraConfigApertureWidget_uxsdk_lensType, 0)));
        }

        int apertureTitleTextAppearanceId =
                typedArray.getResourceId(R.styleable.CameraConfigApertureWidget_uxsdk_apertureTitleTextAppearance,
                        INVALID_RESOURCE);
        if (apertureTitleTextAppearanceId != INVALID_RESOURCE) {
            setApertureTitleTextAppearance(apertureTitleTextAppearanceId);
        }

        float apertureTitleTextSize =
                typedArray.getDimension(R.styleable.CameraConfigApertureWidget_uxsdk_apertureTitleTextSize, INVALID_RESOURCE);
        if (apertureTitleTextSize != INVALID_RESOURCE) {
            setApertureTitleTextSize(DisplayUtil.pxToSp(context, apertureTitleTextSize));
        }

        int apertureTitleTextColor =
                typedArray.getColor(R.styleable.CameraConfigApertureWidget_uxsdk_apertureTitleTextColor, INVALID_COLOR);
        if (apertureTitleTextColor != INVALID_COLOR) {
            setApertureTitleTextColor(apertureTitleTextColor);
        }

        Drawable apertureTitleTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.CameraConfigApertureWidget_uxsdk_apertureTitleBackgroundDrawable);
        if (apertureTitleTextBackgroundDrawable != null) {
            setApertureTitleTextBackground(apertureTitleTextBackgroundDrawable);
        }

        int apertureValueTextAppearanceId =
                typedArray.getResourceId(R.styleable.CameraConfigApertureWidget_uxsdk_apertureValueTextAppearance,
                        INVALID_RESOURCE);
        if (apertureValueTextAppearanceId != INVALID_RESOURCE) {
            setApertureValueTextAppearance(apertureValueTextAppearanceId);
        }

        float apertureValueTextSize =
                typedArray.getDimension(R.styleable.CameraConfigApertureWidget_uxsdk_apertureValueTextSize, INVALID_RESOURCE);
        if (apertureValueTextSize != INVALID_RESOURCE) {
            setApertureValueTextSize(DisplayUtil.pxToSp(context, apertureValueTextSize));
        }

        int apertureValueTextColor =
                typedArray.getColor(R.styleable.CameraConfigApertureWidget_uxsdk_apertureValueTextColor, INVALID_COLOR);
        if (apertureValueTextColor != INVALID_COLOR) {
            setApertureValueTextColor(apertureValueTextColor);
        }

        Drawable apertureValueTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.CameraConfigApertureWidget_uxsdk_apertureValueBackgroundDrawable);
        if (apertureValueTextBackgroundDrawable != null) {
            setApertureValueTextBackground(apertureValueTextBackgroundDrawable);
        }
        typedArray.recycle();
    }
    //endregion
}
