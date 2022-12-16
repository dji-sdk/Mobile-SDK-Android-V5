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

package dji.v5.ux.visualcamera.iso;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import dji.sdk.keyvalue.value.camera.CameraISO;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.utils.common.DisplayUtil;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.RxUtil;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Shows the camera's current ISO or exposure index setting.
 */
public class CameraConfigISOAndEIWidget extends ConstraintLayoutWidget<Object> implements ICameraIndex {
    //region Fields
    private static final String TAG = "ConfigISOAndEIWidget";
    private CameraConfigISOAndEIWidgetModel widgetModel;
    private TextView isoTitleTextView;
    private TextView isoValueTextView;
    //endregion

    //region Constructor
    public CameraConfigISOAndEIWidget(@NonNull Context context) {
        super(context);
    }

    public CameraConfigISOAndEIWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraConfigISOAndEIWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_base_camera_info, this);
        isoTitleTextView = findViewById(R.id.textview_title);
        isoValueTextView = findViewById(R.id.textview_value);

        if (!isInEditMode()) {
            widgetModel = new CameraConfigISOAndEIWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance());
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
        addReaction(reactToUpdateTitle());
        addReaction(widgetModel.getISOAndEIValue()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateValue));
    }
    //endregion

    //region Reactions to model
    private Disposable reactToUpdateTitle() {
        return Flowable.combineLatest(widgetModel.isEIMode(), widgetModel.getISO(), Pair::new)
                .observeOn(SchedulerProvider.ui())
                .subscribe(values -> updateTitle(values.first, values.second),
                        RxUtil.logErrorConsumer(TAG, "react to update title: "));
    }

    private void updateTitle(boolean isEIMode, CameraISO iso) {
        if (isEIMode) {
            isoTitleTextView.setText(R.string.uxsdk_ei_title);
        } else if (iso == CameraISO.ISO_AUTO) {
            isoTitleTextView.setText(R.string.uxsdk_exposure_auto_iso_title);
        } else if (iso == CameraISO.ISO_FIXED) {
            isoTitleTextView.setText(R.string.uxsdk_exposure_locked_iso_title);
        } else {
            isoTitleTextView.setText(R.string.uxsdk_exposure_iso_title);
        }
    }

    private void updateValue(String value) {
        if (TextUtils.isEmpty(value)) {
            isoValueTextView.setText(R.string.uxsdk_string_default_value);
        } else {
            isoValueTextView.setText(value);
        }
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
     * Set text appearance of the ISO and EI title text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setISOAndEITitleTextAppearance(@StyleRes int textAppearance) {
        isoTitleTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the ISO and EI title text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getISOAndEITitleTextColors() {
        return isoTitleTextView.getTextColors();
    }

    /**
     * Get current text color of the ISO and EI title text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getISOAndEITitleTextColor() {
        return isoTitleTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the ISO and EI title text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setISOAndEITitleTextColor(@NonNull ColorStateList colorStateList) {
        isoTitleTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the ISO and EI title text view
     *
     * @param color color integer resource
     */
    public void setISOAndEITitleTextColor(@ColorInt int color) {
        isoTitleTextView.setTextColor(color);
    }

    /**
     * Get current text size of the ISO and EI title text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getISOAndEITitleTextSize() {
        return isoTitleTextView.getTextSize();
    }

    /**
     * Set the text size of the ISO and EI title text view
     *
     * @param textSize text size float value
     */
    public void setISOAndEITitleTextSize(@Dimension float textSize) {
        isoTitleTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the ISO and EI title text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getISOAndEITitleTextBackground() {
        return isoTitleTextView.getBackground();
    }

    /**
     * Set the resource ID for the background of the ISO and EI title text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setISOAndEITitleTextBackground(@DrawableRes int resourceId) {
        isoTitleTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background for the ISO and EI title text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setISOAndEITitleTextBackground(@Nullable Drawable drawable) {
        isoTitleTextView.setBackground(drawable);
    }

    /**
     * Set text appearance of the ISO and EI value text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setISOAndEIValueTextAppearance(@StyleRes int textAppearance) {
        isoValueTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the ISO and EI value text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getISOAndEIValueTextColors() {
        return isoValueTextView.getTextColors();
    }

    /**
     * Get current text color of the ISO and EI value text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getISOAndEIValueTextColor() {
        return isoValueTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the ISO and EI value text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setISOAndEIValueTextColor(@NonNull ColorStateList colorStateList) {
        isoValueTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the ISO and EI value text view
     *
     * @param color color integer resource
     */
    public void setISOAndEIValueTextColor(@ColorInt int color) {
        isoValueTextView.setTextColor(color);
    }

    /**
     * Get current text size of the ISO and EI value text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getISOAndEIValueTextSize() {
        return isoValueTextView.getTextSize();
    }

    /**
     * Set the text size of the ISO and EI value text view
     *
     * @param textSize text size float value
     */
    public void setISOAndEIValueTextSize(@Dimension float textSize) {
        isoValueTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the ISO and EI value text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getISOAndEIValueTextBackground() {
        return isoValueTextView.getBackground();
    }

    /**
     * Set the resource ID for the background of the ISO and EI value text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setISOAndEIValueTextBackground(@DrawableRes int resourceId) {
        isoValueTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background for the ISO and EI value text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setISOAndEIValueTextBackground(@Nullable Drawable drawable) {
        isoValueTextView.setBackground(drawable);
    }
    //endregion

    //region Customization helpers
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CameraConfigISOAndEIWidget);

        if (!isInEditMode()){
            updateCameraSource(ComponentIndexType.find(typedArray.getInt(R.styleable.CameraConfigISOAndEIWidget_uxsdk_cameraIndex, 0)),
                    CameraLensType.find(typedArray.getInt(R.styleable.CameraConfigISOAndEIWidget_uxsdk_lensType, 0)));
        }

        int isoAndEITitleTextAppearanceId =
                typedArray.getResourceId(R.styleable.CameraConfigISOAndEIWidget_uxsdk_isoAndEITitleTextAppearance,
                        INVALID_RESOURCE);
        if (isoAndEITitleTextAppearanceId != INVALID_RESOURCE) {
            setISOAndEITitleTextAppearance(isoAndEITitleTextAppearanceId);
        }

        float isoAndEITitleTextSize =
                typedArray.getDimension(R.styleable.CameraConfigISOAndEIWidget_uxsdk_isoAndEITitleTextSize, INVALID_RESOURCE);
        if (isoAndEITitleTextSize != INVALID_RESOURCE) {
            setISOAndEITitleTextSize(DisplayUtil.pxToSp(context, isoAndEITitleTextSize));
        }

        int isoAndEITitleTextColor =
                typedArray.getColor(R.styleable.CameraConfigISOAndEIWidget_uxsdk_isoAndEITitleTextColor, INVALID_COLOR);
        if (isoAndEITitleTextColor != INVALID_COLOR) {
            setISOAndEITitleTextColor(isoAndEITitleTextColor);
        }

        Drawable isoAndEITitleTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.CameraConfigISOAndEIWidget_uxsdk_isoAndEITitleBackgroundDrawable);
        if (isoAndEITitleTextBackgroundDrawable != null) {
            setISOAndEITitleTextBackground(isoAndEITitleTextBackgroundDrawable);
        }

        int isoAndEIValueTextAppearanceId =
                typedArray.getResourceId(R.styleable.CameraConfigISOAndEIWidget_uxsdk_isoAndEIValueTextAppearance,
                        INVALID_RESOURCE);
        if (isoAndEIValueTextAppearanceId != INVALID_RESOURCE) {
            setISOAndEIValueTextAppearance(isoAndEIValueTextAppearanceId);
        }

        float isoAndEIValueTextSize =
                typedArray.getDimension(R.styleable.CameraConfigISOAndEIWidget_uxsdk_isoAndEIValueTextSize, INVALID_RESOURCE);
        if (isoAndEIValueTextSize != INVALID_RESOURCE) {
            setISOAndEIValueTextSize(DisplayUtil.pxToSp(context, isoAndEIValueTextSize));
        }

        int isoAndEIValueTextColor =
                typedArray.getColor(R.styleable.CameraConfigISOAndEIWidget_uxsdk_isoAndEIValueTextColor, INVALID_COLOR);
        if (isoAndEIValueTextColor != INVALID_COLOR) {
            setISOAndEIValueTextColor(isoAndEIValueTextColor);
        }

        Drawable isoAndEIValueTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.CameraConfigISOAndEIWidget_uxsdk_isoAndEIValueBackgroundDrawable);
        if (isoAndEIValueTextBackgroundDrawable != null) {
            setISOAndEIValueTextBackground(isoAndEIValueTextBackgroundDrawable);
        }
        typedArray.recycle();
    }
    //endregion
}
