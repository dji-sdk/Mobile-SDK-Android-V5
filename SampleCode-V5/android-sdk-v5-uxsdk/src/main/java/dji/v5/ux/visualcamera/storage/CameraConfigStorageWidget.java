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

package dji.v5.ux.visualcamera.storage;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import dji.sdk.keyvalue.value.camera.CameraColor;
import dji.sdk.keyvalue.value.camera.CameraSDCardState;
import dji.sdk.keyvalue.value.camera.CameraStorageLocation;
import dji.sdk.keyvalue.value.camera.CameraWorkMode;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.CameraUtil;
import dji.v5.ux.core.util.DisplayUtil;
import dji.v5.ux.core.util.RxUtil;

/**
 * Shows the camera's current capacity and other information for internal and SD card storage
 * locations.
 */
public class CameraConfigStorageWidget extends ConstraintLayoutWidget  implements ICameraIndex {
    //region Fields
    private static final String TAG = "ConfigStorageWidget";
    private CameraConfigStorageWidgetModel widgetModel;
    private ImageView storageIconImageView;
    private TextView cameraColorTextView;
    private TextView imageFormatTextView;
    private TextView statusCapacityTitleTextView;
    private TextView statusCapacityValueTextView;
    private Map<StorageIconState, Drawable> storageInternalIconMap;
    private Map<StorageIconState, Drawable> storageSDCardIconMap;
    private String[] cameraColorNameArray;
    //endregion

    //region Constructor
    public CameraConfigStorageWidget(@NonNull Context context) {
        super(context);
    }

    public CameraConfigStorageWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraConfigStorageWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_camera_config_storage, this);
        storageIconImageView = findViewById(R.id.imageview_storage_icon);
        cameraColorTextView = findViewById(R.id.textview_camera_color);
        imageFormatTextView = findViewById(R.id.textview_image_format);
        statusCapacityTitleTextView = findViewById(R.id.textview_status_capacity_title);
        statusCapacityValueTextView = findViewById(R.id.textview_status_capacity_value);
        storageInternalIconMap = new HashMap<>();
        storageSDCardIconMap = new HashMap<>();
        cameraColorNameArray = getResources().getStringArray(R.array.uxsdk_camera_color_type);

        if (!isInEditMode()) {
            widgetModel = new CameraConfigStorageWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());
        }
        initDefaults();
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
        addReaction(widgetModel.getImageFormat()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateImageFormatText, RxUtil.logErrorConsumer(TAG, "reactToUpdateImageFormat")));

        addReaction(widgetModel.getCameraStorageState()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateStatus, RxUtil.logErrorConsumer(TAG, "reactToUpdateStatus")));

        addReaction(widgetModel.getCameraColor()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateColor, RxUtil.logErrorConsumer(TAG, "reactToUpdateColor")));
    }
    //endregion

    //region Reactions to model
    private void updateImageFormatText(CameraConfigStorageWidgetModel.ImageFormat imageFormat) {
        imageFormatTextView.setText(getImageFormatString(imageFormat));
    }

    private void updateStatus(CameraConfigStorageWidgetModel.CameraStorageState cameraStorageState) {
        CameraWorkMode cameraMode = cameraStorageState.getCameraMode();

        updateForegroundDrawable(cameraStorageState);

        String status = "";
        if (cameraStorageState.getStorageLocation() == CameraStorageLocation.SDCARD) {
            status = getSDCardStatus(cameraStorageState.getStorageOperationState());
        } else if (cameraStorageState.getStorageLocation() == CameraStorageLocation.INTERNAL) {
            status = getInternalStorageStatus(cameraStorageState.getStorageOperationState());
        }

        if (TextUtils.isEmpty(status)) {
            statusCapacityTitleTextView.setText(getResources().getText(R.string.uxsdk_storage_title_capacity));
            if (cameraMode == CameraWorkMode.RECORD_VIDEO) {
                statusCapacityValueTextView.setText(CameraUtil.formatVideoTime(getResources(),
                        cameraStorageState.getAvailableRecordingTime()));
            } else {
                statusCapacityValueTextView.setText(String.valueOf(cameraStorageState.getAvailableCaptureCount()));
            }
        } else {
            statusCapacityTitleTextView.setText(getResources().getText(R.string.uxsdk_storage_title_status));
            statusCapacityValueTextView.setText(status);
        }
    }

    private void updateForegroundDrawable(CameraConfigStorageWidgetModel.CameraStorageState cameraStorageState) {
        Drawable foregroundDrawable = null;
        if (cameraStorageState.getStorageLocation() == CameraStorageLocation.SDCARD) {
            switch (cameraStorageState.getStorageOperationState()) {
                case NOT_INSERTED:
                    foregroundDrawable = getSDCardStorageIcon(StorageIconState.NOT_INSERTED);
                    break;
                case NORMAL:
                    foregroundDrawable = getSDCardStorageIcon(StorageIconState.NORMAL);
                    break;
                default:
                    foregroundDrawable = getSDCardStorageIcon(StorageIconState.WARNING);
                    break;
            }
        } else if (cameraStorageState.getStorageLocation() == CameraStorageLocation.INTERNAL) {
            switch (cameraStorageState.getStorageOperationState()) {
                case NOT_INSERTED:
                    foregroundDrawable = getInternalStorageIcon(StorageIconState.NOT_INSERTED);
                    break;
                case NORMAL:
                    foregroundDrawable = getInternalStorageIcon(StorageIconState.NORMAL);
                    break;
                default:
                    foregroundDrawable = getInternalStorageIcon(StorageIconState.WARNING);
                    break;
            }
        }
        storageIconImageView.setImageDrawable(foregroundDrawable);
    }

    private void updateColor(CameraColor cameraColor) {
        if (cameraColor == CameraColor.NONE || cameraColor == CameraColor.UNKNOWN) {
            cameraColorTextView.setVisibility(GONE);
        } else if (cameraColor.value() < cameraColorNameArray.length) {
            cameraColorTextView.setVisibility(VISIBLE);
            cameraColorTextView.setText(cameraColorNameArray[cameraColor.value()]);
        }
    }

    private void checkAndUpdateForegroundImage() {
        if (!isInEditMode()) {
            addDisposable(widgetModel.getCameraStorageState().firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(this::updateForegroundDrawable, RxUtil.logErrorConsumer(TAG, "checkAndUpdateForegroundImage")));
        }
    }
    //endregion

    //region helpers
    public String getImageFormatString(CameraConfigStorageWidgetModel.ImageFormat imageFormat) {
        if (imageFormat == null || imageFormat.getResolution() == null ||
                imageFormat.getFrameRate() == null || imageFormat.getPhotoFileFormat() == null) {
            return getResources().getString(R.string.uxsdk_string_default_value);
        }

        if (imageFormat.getCameraMode() == CameraWorkMode.RECORD_VIDEO ||
                imageFormat.getCameraMode() == CameraWorkMode.BROADCAST) {
            String processedResolutionString = CameraUtil.resolutionShortDisplayName(imageFormat.getResolution());
            String processedFrameRateString = CameraUtil.frameRateDisplayName(imageFormat.getFrameRate());
            return processedResolutionString + "/" + processedFrameRateString;
        } else {
            return CameraUtil.convertPhotoFileFormatToString(getResources(), imageFormat.getPhotoFileFormat());
        }
    }

    private String getSDCardStatus(CameraSDCardState sdCardOperationState) {
        String valueStr;
        switch (sdCardOperationState) {
            case USB_CONNECTED:
                valueStr = getResources().getString(R.string.uxsdk_sd_card_usb_connected);
                break;
            case NOT_INSERTED:
                valueStr = getResources().getString(R.string.uxsdk_sd_card_missing);
                break;
            case FULL:
                valueStr = getResources().getString(R.string.uxsdk_sd_card_full);
                break;
            case SLOW:
                valueStr = getResources().getString(R.string.uxsdk_sd_card_slow);
                break;
            case INVALID:
                valueStr = getResources().getString(R.string.uxsdk_sd_card_invalid);
                break;
            case READ_ONLY:
                valueStr = getResources().getString(R.string.uxsdk_sd_card_write_protect);
                break;
            case FORMAT_NEEDED:
                valueStr = getResources().getString(R.string.uxsdk_sd_card_not_formatted);
                break;
            case FORMATTING:
                valueStr = getResources().getString(R.string.uxsdk_sd_card_formatting);
                break;
            case BUSY:
                valueStr = getResources().getString(R.string.uxsdk_sd_card_busy);
                break;
            case UNKNOWN_ERROR:
                valueStr = getResources().getString(R.string.uxsdk_sd_card_unknown_error);
                break;
            case INITIALIZING:
                valueStr = getResources().getString(R.string.uxsdk_sd_card_initial);
                break;
            case RECOVERING_FILES:
                valueStr = getResources().getString(R.string.uxsdk_sd_card_recover_file);
                break;
            case FORMAT_RECOMMENDED:
                valueStr = getResources().getString(R.string.uxsdk_sd_card_needs_formatting);
                break;
            case WRITING_SLOWLY:
                valueStr = getResources().getString(R.string.uxsdk_sd_card_write_slow);
                break;
            default:
                valueStr = "";
                break;
        }
        return valueStr;
    }

    private String getInternalStorageStatus(CameraSDCardState sdCardOperationState) {
        String valueStr;
        switch (sdCardOperationState) {
            case NOT_INSERTED:
                valueStr = getResources().getString(R.string.uxsdk_internal_storage_missing);
                break;
            case FULL:
                valueStr = getResources().getString(R.string.uxsdk_internal_storage_full);
                break;
            case SLOW:
                valueStr = getResources().getString(R.string.uxsdk_internal_storage_slow);
                break;
            case INVALID:
                valueStr = getResources().getString(R.string.uxsdk_internal_storage_invalid);
                break;
            case READ_ONLY:
                valueStr = getResources().getString(R.string.uxsdk_internal_storage_write_protect);
                break;
            case FORMAT_NEEDED:
                valueStr = getResources().getString(R.string.uxsdk_internal_storage_not_formatted);
                break;
            case FORMATTING:
                valueStr = getResources().getString(R.string.uxsdk_internal_storage_formatting);
                break;
            case BUSY:
                valueStr = getResources().getString(R.string.uxsdk_internal_storage_busy);
                break;
            case UNKNOWN_ERROR:
                valueStr = getResources().getString(R.string.uxsdk_internal_storage_unknown_error);
                break;
            case INITIALIZING:
                valueStr = getResources().getString(R.string.uxsdk_internal_storage_initial);
                break;
            default:
                valueStr = "";
                break;
        }
        return valueStr;
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
     * Set the icon for internal storage based on storage icon state
     *
     * @param storageIconState The state for which the icon will change to the given image.
     * @param resourceId       The id of the image the icon will change to.
     */
    public void setInternalStorageIcon(@NonNull StorageIconState storageIconState, @DrawableRes int resourceId) {
        setInternalStorageIcon(storageIconState, getResources().getDrawable(resourceId));
    }

    /**
     * Set the icon for internal storage based on storage icon state
     *
     * @param storageIconState The state for which the icon will change to the given image.
     * @param drawable         The image the icon will change to.
     */
    public void setInternalStorageIcon(@NonNull StorageIconState storageIconState, @Nullable Drawable drawable) {
        storageInternalIconMap.put(storageIconState, drawable);
        checkAndUpdateForegroundImage();
    }

    /**
     * Get the icon for internal storage based on storage icon state
     *
     * @param storageIconState The state at which the icon will change.
     * @return The image the icon will change to for the given status.
     */
    @Nullable
    public Drawable getInternalStorageIcon(@NonNull StorageIconState storageIconState) {
        return storageInternalIconMap.get(storageIconState);
    }

    /**
     * Set the icon for SD card storage based on storage icon state
     *
     * @param storageIconState The state at which the icon will change to the given image.
     * @param resourceId       The id of the image the icon will change to.
     */
    public void setSDCardStorageIcon(@NonNull StorageIconState storageIconState, @DrawableRes int resourceId) {
        setSDCardStorageIcon(storageIconState, getResources().getDrawable(resourceId));
    }

    /**
     * Set the icon for SD card storage based on storage icon state
     *
     * @param storageIconState The state at which the icon will change to the given image.
     * @param drawable         The image the icon will change to.
     */
    public void setSDCardStorageIcon(@NonNull StorageIconState storageIconState, @Nullable Drawable drawable) {
        storageSDCardIconMap.put(storageIconState, drawable);
        checkAndUpdateForegroundImage();
    }

    /**
     * Get the icon for SD card storage based on storage icon state
     *
     * @param storageIconState The state at which the icon will change.
     * @return The image the icon will change to for the given status.
     */
    @Nullable
    public Drawable getSDCardStorageIcon(@NonNull StorageIconState storageIconState) {
        return storageSDCardIconMap.get(storageIconState);
    }

    /**
     * Get the drawable resource for the storage icon's background
     *
     * @return Drawable resource for the icon's background
     */
    @Nullable
    public Drawable getStorageIconBackground() {
        return storageIconImageView.getBackground();
    }

    /**
     * Set the resource ID for the storage icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setStorageIconBackground(@DrawableRes int resourceId) {
        storageIconImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the storage icon's background
     *
     * @param icon Drawable resource for the icon's background
     */
    public void setStorageIconBackground(@Nullable Drawable icon) {
        storageIconImageView.setBackground(icon);
    }

    /**
     * Set text appearance of the camera color text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setCameraColorTextAppearance(@StyleRes int textAppearance) {
        cameraColorTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the camera color text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getCameraColorTextColors() {
        return cameraColorTextView.getTextColors();
    }

    /**
     * Get current text color of the camera color text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getCameraColorTextColor() {
        return cameraColorTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the camera color text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setCameraColorTextColor(@NonNull ColorStateList colorStateList) {
        cameraColorTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the camera color text view
     *
     * @param color color integer resource
     */
    public void setCameraColorTextColor(@ColorInt int color) {
        cameraColorTextView.setTextColor(color);
    }

    /**
     * Get current text size of the camera color text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getCameraColorTextSize() {
        return cameraColorTextView.getTextSize();
    }

    /**
     * Set the text size of the camera color text view
     *
     * @param textSize text size float value
     */
    public void setCameraColorTextSize(@Dimension float textSize) {
        cameraColorTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the camera color text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getCameraColorTextBackground() {
        return cameraColorTextView.getBackground();
    }

    /**
     * Set the background of the camera color text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setCameraColorTextBackground(@Nullable Drawable drawable) {
        cameraColorTextView.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the camera color text view
     *
     * @param resourceId Drawable resource for the background
     */
    public void setCameraColorTextBackground(@DrawableRes int resourceId) {
        cameraColorTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set text appearance of the image format text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setImageFormatTextAppearance(@StyleRes int textAppearance) {
        imageFormatTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the image format text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getImageFormatTextColors() {
        return imageFormatTextView.getTextColors();
    }

    /**
     * Get current text color of the image format text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getImageFormatTextColor() {
        return imageFormatTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the image format text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setImageFormatTextColor(@NonNull ColorStateList colorStateList) {
        imageFormatTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the image format text view
     *
     * @param color color integer resource
     */
    public void setImageFormatTextColor(@ColorInt int color) {
        imageFormatTextView.setTextColor(color);
    }

    /**
     * Get current text size of the image format text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getImageFormatTextSize() {
        return imageFormatTextView.getTextSize();
    }

    /**
     * Set the text size of the image format text view
     *
     * @param textSize text size float value
     */
    public void setImageFormatTextSize(float textSize) {
        imageFormatTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the image format text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getImageFormatTextBackground() {
        return imageFormatTextView.getBackground();
    }

    /**
     * Set the background of the image format text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setImageFormatTextBackground(@Nullable Drawable drawable) {
        imageFormatTextView.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the image format text view
     *
     * @param resourceId Drawable resource for the background
     */
    public void setImageFormatTextBackground(@DrawableRes int resourceId) {
        imageFormatTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set text appearance of the text view with the status or capacity title
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setStatusCapacityTitleTextAppearance(@StyleRes int textAppearance) {
        statusCapacityTitleTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the text view with the status or capacity title
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getStatusCapacityTitleTextColors() {
        return statusCapacityTitleTextView.getTextColors();
    }

    /**
     * Get current text color of the text view with the status or capacity title
     *
     * @return color integer resource
     */
    @ColorInt
    public int getStatusCapacityTitleTextColor() {
        return statusCapacityTitleTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the text view with the status or capacity title
     *
     * @param colorStateList ColorStateList resource
     */
    public void setStatusCapacityTitleTextColor(@NonNull ColorStateList colorStateList) {
        statusCapacityTitleTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the text view with the status or capacity title
     *
     * @param color color integer resource
     */
    public void setStatusCapacityTitleTextColor(@ColorInt int color) {
        statusCapacityTitleTextView.setTextColor(color);
    }

    /**
     * Get current text size of the text view with the status or capacity title
     *
     * @return text size of the text view
     */
    @Dimension
    public float getStatusCapacityTitleTextSize() {
        return statusCapacityTitleTextView.getTextSize();
    }

    /**
     * Set the text size of the text view with the status or capacity title
     *
     * @param textSize text size float value
     */
    public void setStatusCapacityTitleTextSize(float textSize) {
        statusCapacityTitleTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the text view with the status or capacity title
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getStatusCapacityTitleTextBackground() {
        return statusCapacityTitleTextView.getBackground();
    }

    /**
     * Set the background of the text view with the status or capacity title
     *
     * @param drawable Drawable resource for the background
     */
    public void setStatusCapacityTitleTextBackground(@Nullable Drawable drawable) {
        statusCapacityTitleTextView.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the text view with the status or capacity title
     *
     * @param resourceId Drawable resource for the background
     */
    public void setStatusCapacityTitleTextBackground(@DrawableRes int resourceId) {
        statusCapacityTitleTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set text appearance of the text view with the status or capacity value
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setStatusCapacityValueTextAppearance(@StyleRes int textAppearance) {
        statusCapacityValueTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the text view with the status or capacity value
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getStatusCapacityValueTextColors() {
        return statusCapacityValueTextView.getTextColors();
    }

    /**
     * Get current text color of the text view with the status or capacity value
     *
     * @return color integer resource
     */
    @ColorInt
    public int getStatusCapacityValueTextColor() {
        return statusCapacityValueTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the text view with the status or capacity value
     *
     * @param colorStateList ColorStateList resource
     */
    public void setStatusCapacityValueTextColor(@NonNull ColorStateList colorStateList) {
        statusCapacityValueTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the text view with the status or capacity value
     *
     * @param color color integer resource
     */
    public void setStatusCapacityValueTextColor(@ColorInt int color) {
        statusCapacityValueTextView.setTextColor(color);
    }

    /**
     * Get current text size of the text view with the status or capacity value
     *
     * @return text size of the text view
     */
    @Dimension
    public float getStatusCapacityValueTextSize() {
        return statusCapacityValueTextView.getTextSize();
    }

    /**
     * Set the text size of the text view with the status or capacity value
     *
     * @param textSize text size float value
     */
    public void setStatusCapacityValueTextSize(float textSize) {
        statusCapacityValueTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the text view with the status or capacity value
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getStatusCapacityValueTextBackground() {
        return statusCapacityValueTextView.getBackground();
    }

    /**
     * Set the background of the text view with the status or capacity value
     *
     * @param drawable Drawable resource for the background
     */
    public void setStatusCapacityValueTextBackground(@Nullable Drawable drawable) {
        statusCapacityValueTextView.setBackground(drawable);
    }

    /**
     * Set the resource ID for the background of the text view with the status or capacity value
     *
     * @param resourceId Drawable resource for the background
     */
    public void setStatusCapacityValueTextBackground(@DrawableRes int resourceId) {
        statusCapacityValueTextView.setBackgroundResource(resourceId);
    }

    //endregion

    //region Customization Helpers
    private void initDefaults() {
        setInternalStorageIcon(StorageIconState.NOT_INSERTED, R.drawable.uxsdk_ic_config_internal_none);
        setInternalStorageIcon(StorageIconState.WARNING, R.drawable.uxsdk_ic_config_internal_warning);
        setInternalStorageIcon(StorageIconState.NORMAL, R.drawable.uxsdk_ic_config_internal_normal);
        setSDCardStorageIcon(StorageIconState.NOT_INSERTED, R.drawable.uxsdk_ic_config_sd_none);
        setSDCardStorageIcon(StorageIconState.WARNING, R.drawable.uxsdk_ic_config_sd_warning);
        setSDCardStorageIcon(StorageIconState.NORMAL, R.drawable.uxsdk_ic_config_sd_normal);
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CameraConfigStorageWidget);

        if (!isInEditMode()){
            updateCameraSource(ComponentIndexType.find(typedArray.getInt(R.styleable.CameraConfigStorageWidget_uxsdk_cameraIndex, 0)),
                    CameraLensType.find(typedArray.getInt(R.styleable.CameraConfigStorageWidget_uxsdk_lensType, 0)));
        }
        if (typedArray.getDrawable(R.styleable.CameraConfigStorageWidget_uxsdk_internalStorageNotInsertedIcon) != null) {
            setInternalStorageIcon(StorageIconState.NOT_INSERTED, typedArray.getDrawable(R.styleable.CameraConfigStorageWidget_uxsdk_internalStorageNotInsertedIcon));
        }
        if (typedArray.getDrawable(R.styleable.CameraConfigStorageWidget_uxsdk_internalStorageWarningIcon) != null) {
            setInternalStorageIcon(StorageIconState.WARNING, typedArray.getDrawable(R.styleable.CameraConfigStorageWidget_uxsdk_internalStorageWarningIcon));
        }
        if (typedArray.getDrawable(R.styleable.CameraConfigStorageWidget_uxsdk_internalStorageNormalIcon) != null) {
            setInternalStorageIcon(StorageIconState.NORMAL, typedArray.getDrawable(R.styleable.CameraConfigStorageWidget_uxsdk_internalStorageNormalIcon));
        }
        if (typedArray.getDrawable(R.styleable.CameraConfigStorageWidget_uxsdk_sdCardNotInsertedIcon) != null) {
            setSDCardStorageIcon(StorageIconState.NOT_INSERTED, typedArray.getDrawable(R.styleable.CameraConfigStorageWidget_uxsdk_sdCardNotInsertedIcon));
        }
        if (typedArray.getDrawable(R.styleable.CameraConfigStorageWidget_uxsdk_sdCardWarningIcon) != null) {
            setSDCardStorageIcon(StorageIconState.WARNING, typedArray.getDrawable(R.styleable.CameraConfigStorageWidget_uxsdk_sdCardWarningIcon));
        }
        if (typedArray.getDrawable(R.styleable.CameraConfigStorageWidget_uxsdk_sdCardNormalIcon) != null) {
            setSDCardStorageIcon(StorageIconState.NORMAL, typedArray.getDrawable(R.styleable.CameraConfigStorageWidget_uxsdk_sdCardNormalIcon));
        }
        if (typedArray.getDrawable(R.styleable.CameraConfigStorageWidget_uxsdk_storageIconBackground) != null) {
            setStorageIconBackground(typedArray.getDrawable(R.styleable.CameraConfigStorageWidget_uxsdk_storageIconBackground));
        }

        int formatInfoTextAppearanceId =
                typedArray.getResourceId(R.styleable.CameraConfigStorageWidget_uxsdk_imageFormatTextAppearance, INVALID_RESOURCE);
        if (formatInfoTextAppearanceId != INVALID_RESOURCE) {
            setImageFormatTextAppearance(formatInfoTextAppearanceId);
        }
        float formatInfoTextSize =
                typedArray.getDimension(R.styleable.CameraConfigStorageWidget_uxsdk_imageFormatTextSize, INVALID_RESOURCE);
        if (formatInfoTextSize != INVALID_RESOURCE) {
            setImageFormatTextSize(DisplayUtil.pxToSp(context, formatInfoTextSize));
        }
        int formatInfoTextColor =
                typedArray.getColor(R.styleable.CameraConfigStorageWidget_uxsdk_imageFormatTextColor, INVALID_COLOR);
        if (formatInfoTextColor != INVALID_COLOR) {
            setImageFormatTextColor(formatInfoTextColor);
        }
        Drawable formatInfoTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.CameraConfigStorageWidget_uxsdk_imageFormatTextBackground);
        if (formatInfoTextBackgroundDrawable != null) {
            setImageFormatTextBackground(formatInfoTextBackgroundDrawable);
        }
        setStatusCapacity(context, typedArray);
        setCameraColorText(context, typedArray);
        typedArray.recycle();
    }

    private void setCameraColorText(Context context, TypedArray typedArray){
        int cameraColorTextAppearanceId =
                typedArray.getResourceId(R.styleable.CameraConfigStorageWidget_uxsdk_cameraColorTextAppearance, INVALID_RESOURCE);
        if (cameraColorTextAppearanceId != INVALID_RESOURCE) {
            setCameraColorTextAppearance(cameraColorTextAppearanceId);
        }
        float cameraColorTextSize =
                typedArray.getDimension(R.styleable.CameraConfigStorageWidget_uxsdk_cameraColorTextSize, INVALID_RESOURCE);
        if (cameraColorTextSize != INVALID_RESOURCE) {
            setCameraColorTextSize(DisplayUtil.pxToSp(context, cameraColorTextSize));
        }
        int cameraColorTextColor =
                typedArray.getColor(R.styleable.CameraConfigStorageWidget_uxsdk_cameraColorTextColor, INVALID_COLOR);
        if (cameraColorTextColor != INVALID_COLOR) {
            setCameraColorTextColor(cameraColorTextColor);
        }
        Drawable cameraColorTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.CameraConfigStorageWidget_uxsdk_cameraColorTextBackground);
        if (cameraColorTextBackgroundDrawable != null) {
            setCameraColorTextBackground(cameraColorTextBackgroundDrawable);
        }
    }

    private void setStatusCapacity(Context context, TypedArray typedArray){
        int capacityTextAppearanceId =
                typedArray.getResourceId(R.styleable.CameraConfigStorageWidget_uxsdk_capacityTextAppearance, INVALID_RESOURCE);
        if (capacityTextAppearanceId != INVALID_RESOURCE) {
            setStatusCapacityTitleTextAppearance(capacityTextAppearanceId);
        }
        float capacityTextSize =
                typedArray.getDimension(R.styleable.CameraConfigStorageWidget_uxsdk_capacityTextSize, INVALID_RESOURCE);
        if (capacityTextSize != INVALID_RESOURCE) {
            setStatusCapacityTitleTextSize(DisplayUtil.pxToSp(context, capacityTextSize));
        }
        int capacityTextColor =
                typedArray.getColor(R.styleable.CameraConfigStorageWidget_uxsdk_capacityTextColor, INVALID_COLOR);
        if (capacityTextColor != INVALID_COLOR) {
            setStatusCapacityTitleTextColor(capacityTextColor);
        }
        Drawable capacityTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.CameraConfigStorageWidget_uxsdk_capacityTextBackground);
        if (capacityTextBackgroundDrawable != null) {
            setStatusCapacityTitleTextBackground(capacityTextBackgroundDrawable);
        }

        int capacityValueTextAppearanceId =
                typedArray.getResourceId(R.styleable.CameraConfigStorageWidget_uxsdk_capacityValueTextAppearance, INVALID_RESOURCE);
        if (capacityValueTextAppearanceId != INVALID_RESOURCE) {
            setStatusCapacityValueTextAppearance(capacityValueTextAppearanceId);
        }
        float capacityValueTextSize =
                typedArray.getDimension(R.styleable.CameraConfigStorageWidget_uxsdk_capacityValueTextSize, INVALID_RESOURCE);
        if (capacityValueTextSize != INVALID_RESOURCE) {
            setStatusCapacityValueTextSize(DisplayUtil.pxToSp(context, capacityValueTextSize));
        }
        int capacityValueTextColor =
                typedArray.getColor(R.styleable.CameraConfigStorageWidget_uxsdk_capacityValueTextColor, INVALID_COLOR);
        if (capacityValueTextColor != INVALID_COLOR) {
            setStatusCapacityValueTextColor(capacityValueTextColor);
        }
        Drawable capacityValueTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.CameraConfigStorageWidget_uxsdk_capacityValueTextBackground);
        if (capacityValueTextBackgroundDrawable != null) {
            setStatusCapacityValueTextBackground(capacityValueTextBackgroundDrawable);
        }
    }
    //endregion

    //region States

    /**
     * Enum indicating storage error state
     */
    public enum StorageIconState {
        /**
         * The storage is full or has another error
         */
        WARNING,
        /**
         * The storage is normal
         */
        NORMAL,
        /**
         * The storage is not inserted
         */
        NOT_INSERTED
    }
    //endregion
}
