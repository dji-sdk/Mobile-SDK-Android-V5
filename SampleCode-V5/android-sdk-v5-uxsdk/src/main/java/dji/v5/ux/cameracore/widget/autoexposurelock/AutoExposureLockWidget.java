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

package dji.v5.ux.cameracore.widget.autoexposurelock;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.RxUtil;
import dji.v5.ux.core.util.ViewUtil;

/**
 * Auto Exposure Lock Widget will display the current state of exposure lock.
 * <p>
 * When locked the exposure of the camera will remain constant.
 * Changing the exposure parameters manually will release the lock.
 */
public class AutoExposureLockWidget extends ConstraintLayoutWidget<Object> implements View.OnClickListener, ICameraIndex {

    //region Fields
    private static final String TAG = "AutoExposureLockWidget";
    private ImageView foregroundImageView;
    private TextView titleTextView;
    private AutoExposureLockWidgetModel widgetModel;
    private Drawable autoExposureLockDrawable;
    private Drawable autoExposureUnlockDrawable;
    private ColorStateList lockDrawableTint;
    private ColorStateList unlockDrawableTint;
    //endregion

    //region Lifecycle
    public AutoExposureLockWidget(@NonNull Context context) {
        super(context);
    }

    public AutoExposureLockWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoExposureLockWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_auto_exposure_lock, this);
        if (getBackground() == null) {
            setBackgroundResource(R.drawable.uxsdk_background_black_rectangle);
        }
        foregroundImageView = findViewById(R.id.auto_exposure_lock_widget_foreground_image_view);
        titleTextView = findViewById(R.id.auto_exposure_lock_widget_title_text_view);
        if (!isInEditMode()) {
            widgetModel =
                    new AutoExposureLockWidgetModel(DJISDKModel.getInstance(),
                            ObservableInMemoryKeyedStore.getInstance());
        }
        initDefaults();
        if (attrs != null) {
            initAttributes(context, attrs);
        }
        setOnClickListener(this);
    }


    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.isAutoExposureLockOn().observeOn(SchedulerProvider.ui()).subscribe(this::onAELockChange));
    }

    @Override
    public void onClick(View v) {
        if (v == this) {
            setAutoExposureLock();
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
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
        super.onDetachedFromWindow();
    }

    //endregion

    //region private methods
    @MainThread
    private void onAELockChange(boolean isLocked) {
        if (isLocked) {
            foregroundImageView.setImageDrawable(autoExposureLockDrawable);
            if (lockDrawableTint != null) ViewUtil.tintImage(foregroundImageView, lockDrawableTint);
        } else {
            foregroundImageView.setImageDrawable(autoExposureUnlockDrawable);
            if (unlockDrawableTint != null)
                ViewUtil.tintImage(foregroundImageView, unlockDrawableTint);
        }
    }

    private void setAutoExposureLock() {
        addDisposable(widgetModel.toggleAutoExposureLock()
                .observeOn(SchedulerProvider.ui())
                .subscribe(() -> {
                    // Do nothing
                }, RxUtil.logErrorConsumer(TAG, "set auto exposure lock: ")));
    }

    private void checkAndUpdateAELock() {
        if (!isInEditMode()) {
            addDisposable(widgetModel.isAutoExposureLockOn().firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(this::onAELockChange, RxUtil.logErrorConsumer(TAG, "Update AE Lock ")));
        }
    }

    private void initDefaults() {
        autoExposureLockDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_auto_exposure_lock);
        autoExposureUnlockDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_auto_exposure_unlock);
        setTitleTextColor(getResources().getColorStateList(R.color.uxsdk_color_selector_auto_exposure_lock));
    }

    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AutoExposureLockWidget);
        if (!isInEditMode()) {
            updateCameraSource(ComponentIndexType.find(typedArray.getInt(R.styleable.AutoExposureLockWidget_uxsdk_cameraIndex, 0)), CameraLensType.find(typedArray.getInt(R.styleable.AutoExposureLockWidget_uxsdk_lensType, 0)));
        }
        ColorStateList colorStateList = typedArray.getColorStateList(R.styleable.AutoExposureLockWidget_uxsdk_widgetTitleTextColor);
        if (colorStateList != null) {
            setTitleTextColor(colorStateList);
        }

        int colorResource = typedArray.getColor(R.styleable.AutoExposureLockWidget_uxsdk_widgetTitleTextColor, INVALID_COLOR);
        if (colorResource != INVALID_COLOR) {
            setTitleTextColor(colorResource);
        }

        colorStateList = typedArray.getColorStateList(R.styleable.AutoExposureLockWidget_uxsdk_autoExposureUnlockDrawableTint);
        if (colorStateList != null) {
            setAutoExposureUnlockIconTint(colorStateList);
        }

        colorResource = typedArray.getColor(R.styleable.AutoExposureLockWidget_uxsdk_autoExposureUnlockDrawableTint, INVALID_COLOR);
        if (colorResource != INVALID_COLOR) {
            setAutoExposureUnlockIconTint(colorResource);
        }
        colorStateList = typedArray.getColorStateList(R.styleable.AutoExposureLockWidget_uxsdk_autoExposureLockDrawableTint);
        if (colorStateList != null) {
            setAutoExposureLockIconTint(colorStateList);
        }
        colorResource = typedArray.getColor(R.styleable.AutoExposureLockWidget_uxsdk_autoExposureLockDrawableTint, INVALID_COLOR);
        if (colorResource != INVALID_COLOR) {
            setAutoExposureLockIconTint(colorResource);
        }
        int textAppearance = typedArray.getResourceId(R.styleable.AutoExposureLockWidget_uxsdk_widgetTitleTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setTitleTextAppearance(textAppearance);
        }

        if (typedArray.getDrawable(R.styleable.AutoExposureLockWidget_uxsdk_autoExposureLockDrawable) != null) {
            autoExposureLockDrawable = typedArray.getDrawable(R.styleable.AutoExposureLockWidget_uxsdk_autoExposureLockDrawable);
        }
        if (typedArray.getDrawable(R.styleable.AutoExposureLockWidget_uxsdk_autoExposureUnlockDrawable) != null) {
            autoExposureUnlockDrawable = typedArray.getDrawable(R.styleable.AutoExposureLockWidget_uxsdk_autoExposureUnlockDrawable);
        }

        typedArray.recycle();
    }
    //endregion

    //region customization
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_auto_exposure_lock_ratio);
    }

    @NonNull
    public ComponentIndexType getCameraIndex() {
        return widgetModel.getCameraIndex();
    }

    @NonNull
    @Override
    public CameraLensType getLensType() {
        return widgetModel.getLensType();
    }

    @Override
    public void updateCameraSource(@NonNull ComponentIndexType cameraIndex, @NonNull CameraLensType lensType) {
        widgetModel.updateCameraSource(cameraIndex, lensType);
    }

    /**
     * Set drawable for auto exposure lock in locked state
     *
     * @param resourceId to be used
     */
    public void setAutoExposureLockIcon(@DrawableRes int resourceId) {
        setAutoExposureLockIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set drawable for auto exposure lock in locked state
     *
     * @param drawable to be used
     */
    public void setAutoExposureLockIcon(@Nullable Drawable drawable) {
        this.autoExposureLockDrawable = drawable;
        checkAndUpdateAELock();
    }

    /**
     * Get current drawable resource for auto exposure lock in locked state
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getAutoExposureLockDrawable() {
        return autoExposureLockDrawable;
    }

    /**
     * Set resource for auto exposure lock in unlocked state
     *
     * @param resourceId to be used
     */
    public void setAutoExposureUnlockIcon(@DrawableRes int resourceId) {
        setAutoExposureUnlockIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set drawable for auto exposure lock in unlocked state
     *
     * @param drawable to be used
     */
    public void setAutoExposureUnlockIcon(@Nullable Drawable drawable) {
        this.autoExposureUnlockDrawable = drawable;
        checkAndUpdateAELock();
    }

    /**
     * Get current drawable resource for auto exposure lock in unlocked state
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getAutoExposureUnlockDrawable() {
        return autoExposureUnlockDrawable;
    }

    /**
     * Get current text color state list of widget title
     *
     * @return ColorStateList used
     */
    @Nullable
    public ColorStateList getTitleTextColors() {
        return titleTextView.getTextColors();
    }

    /**
     * Get the current color of title text
     *
     * @return integer value representing color
     */
    @ColorInt
    public int getTitleTextColor() {
        return titleTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list to the widget title
     *
     * @param colorStateList to be used
     */
    public void setTitleTextColor(@Nullable ColorStateList colorStateList) {
        titleTextView.setTextColor(colorStateList);
    }

    /**
     * Set the color of title text
     *
     * @param color integer value
     */
    public void setTitleTextColor(@ColorInt int color) {
        titleTextView.setTextColor(color);
    }

    /**
     * Set text appearance of the widget title
     *
     * @param textAppearance to be used
     */
    public void setTitleTextAppearance(@StyleRes int textAppearance) {
        titleTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current background of icon
     *
     * @return Drawable
     */
    @NonNull
    public Drawable getIconBackground() {
        return foregroundImageView.getBackground();
    }

    /**
     * Set background to icon
     *
     * @param resourceId to be used
     */
    public void setIconBackground(@DrawableRes int resourceId) {
        setIconBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set background to icon
     *
     * @param drawable to be used
     */
    public void setIconBackground(@Nullable Drawable drawable) {
        foregroundImageView.setBackground(drawable);
    }

    /**
     * Get current background of title text
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getTitleBackground() {
        return titleTextView.getBackground();
    }

    /**
     * Set background to title text
     *
     * @param resourceId to be used
     */
    public void setTitleBackground(@DrawableRes int resourceId) {
        setTitleBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set background to title text
     *
     * @param drawable to be used
     */
    public void setTitleBackground(@Nullable Drawable drawable) {
        titleTextView.setBackground(drawable);
    }

    @Override
    public void setEnabled(boolean enabled) {
        titleTextView.setEnabled(enabled);
        foregroundImageView.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    /**
     * Get the color tint for exposure settings unlocked icon
     *
     * @return ColorStateList used as color tint
     */
    @Nullable
    public ColorStateList getAutoExposureUnlockIconTint() {
        return unlockDrawableTint;
    }

    /**
     * Set the color of tint for exposure settings unlocked icon
     *
     * @param color int value
     */
    public void setAutoExposureUnlockIconTint(@ColorInt int color) {
        setAutoExposureUnlockIconTint(ColorStateList.valueOf(color));
    }

    /**
     * Set the color of tint for the exposure settings unlocked icon
     *
     * @param colorStateList to be used
     */
    public void setAutoExposureUnlockIconTint(@Nullable ColorStateList colorStateList) {
        unlockDrawableTint = colorStateList;
        checkAndUpdateAELock();
    }

    /**
     * Get the color tint for exposure settings locked icon
     *
     * @return ColorStateList used as color tint
     */
    @Nullable
    public ColorStateList getAutoExposureLockIconTint() {
        return lockDrawableTint;
    }

    /**
     * Set the color of tint for exposure settings locked icon
     *
     * @param color int value
     */
    public void setAutoExposureLockIconTint(@ColorInt int color) {
        setAutoExposureLockIconTint(ColorStateList.valueOf(color));
    }

    /**
     * Set the color of tint for the exposure settings locked icon
     *
     * @param colorStateList to be used
     */
    public void setAutoExposureLockIconTint(@Nullable ColorStateList colorStateList) {
        lockDrawableTint = colorStateList;
        checkAndUpdateAELock();
    }
    //endregion
}
