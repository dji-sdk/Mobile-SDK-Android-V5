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

package dji.v5.ux.visualcamera.ssd;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import dji.sdk.keyvalue.value.camera.CameraShootPhotoMode;
import dji.sdk.keyvalue.value.camera.CameraWorkMode;
import dji.sdk.keyvalue.value.camera.SSDClipFileNameMsg;
import dji.sdk.keyvalue.value.camera.SSDColor;
import dji.sdk.keyvalue.value.camera.SSDOperationState;
import dji.sdk.keyvalue.value.camera.SSDVideoLicense;
import dji.sdk.keyvalue.value.camera.VideoFrameRate;
import dji.sdk.keyvalue.value.camera.VideoResolution;
import dji.sdk.keyvalue.value.camera.VideoResolutionFrameRate;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DisplayUtil;
import dji.v5.ux.core.util.RxUtil;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Shows the camera's current capacity and other information for the SSD storage.
 */
public class CameraConfigSSDWidget extends ConstraintLayoutWidget implements ICameraIndex {
    //region Constants
    private static final String TAG = "ConfigSSDWidget";
    private static final int CAPACITY_UNIT_SWITCH_LIMIT = 1024;
    private static final String NULL_STRING = "Null";
    //endregion

    //region Fields
    private CameraConfigSSDWidgetModel widgetModel;
    private ImageView ssdImageView;
    private ImageView ssdStatusImageView;
    private TextView ssdClipInfoTextView;
    private TextView ssdCapacityOrLicenseTextView;
    private TextView statusInfoTextView;
    private TextView formatInfoTextView;
    private TextView ssdCapacityValueTextView;
    private String[] videoResolutionArray;
    private String[] frameRateArray;
    private String[] ssdColorArray;
    private Animation ssdSaveAnimation;
    private boolean isSSDRecording;
    private Map<SSDOperationState, Drawable> ssdIconMap;
    //endregion

    //region Constructor
    public CameraConfigSSDWidget(@NonNull Context context) {
        super(context);
    }

    public CameraConfigSSDWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraConfigSSDWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_camera_config_ssd, this);
        ssdImageView = findViewById(R.id.imageview_ssd_icon);
        ssdStatusImageView = findViewById(R.id.imageview_ssd_status_icon);
        ssdClipInfoTextView = findViewById(R.id.textview_ssd_clip_info);
        ssdCapacityOrLicenseTextView = findViewById(R.id.textview_ssd_capacity_license);
        statusInfoTextView = findViewById(R.id.textview_status_info);
        formatInfoTextView = findViewById(R.id.textview_format_info);
        ssdCapacityValueTextView = findViewById(R.id.textview_ssd_capacity_value);

        videoResolutionArray = getResources().getStringArray(R.array.uxsdk_camera_video_resolution_name_array);
        frameRateArray = getResources().getStringArray(R.array.uxsdk_camera_video_frame_rate_real_value_array);
        ssdColorArray = getResources().getStringArray(R.array.uxsdk_camera_ssd_color_array);
        ssdSaveAnimation = AnimationUtils.loadAnimation(context, R.anim.uxsdk_anim_blink);

        if (!isInEditMode()) {
            widgetModel = new CameraConfigSSDWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance());
        }

        initDefaultIcons();
        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }
    //endregion

    //region Lifecycle
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
        addReaction(widgetModel.isSSDSupported()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateWidgetVisibility));
        addReaction(widgetModel.getSSDLicense()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateCapacityTitle));
        addReaction(widgetModel.getSSDRemainingSpace()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateCapacityValue));
        addReaction(widgetModel.getSSDResolutionAndFrameRate()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateSSDResolutionAndFrameRate));
        addReaction(reactToUpdateClipInfo());
        addReaction(reactToUpdateSSDState());
    }
    //endregion

    //region Reaction Helpers
    private Disposable reactToUpdateClipInfo() {
        return Flowable.combineLatest(widgetModel.getSSDClipName(), widgetModel.getSSDColor(), Pair::new)
                .observeOn(SchedulerProvider.ui())
                .subscribe(values -> updateClipInfo(values.first, values.second),
                        RxUtil.logErrorConsumer(TAG, "reactToUpdateClipInfo: "));
    }

    private Flowable<Pair<SSDOperationState, Boolean>> getSSDState() {
        return Flowable.combineLatest(widgetModel.getSSDOperationState(),
                widgetModel.getCameraMode(),
                widgetModel.getShootPhotoMode(),
                (ssdOperationState, cameraMode, shootPhotoMode) -> {
                    boolean cameraState =
                            (shootPhotoMode == CameraShootPhotoMode.RAW_BURST
                                    && cameraMode == CameraWorkMode.SHOOT_PHOTO);
                    return Pair.create(ssdOperationState, cameraState);
                });
    }

    private Disposable reactToUpdateSSDState() {
        return getSSDState()
                .observeOn(SchedulerProvider.ui())
                .subscribe(values -> updateSSDState(values.first, values.second),
                        RxUtil.logErrorConsumer(TAG, "reactToUpdateSSDState: "));
    }

    private void checkAndUpdateSSDState() {
        if (!isInEditMode()) {
            addDisposable(getSSDState()
                    .firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(values -> updateSSDState(values.first, values.second),
                            RxUtil.logErrorConsumer(TAG, "checkAndUpdateSSDState: ")));
        }
    }
    //endregion

    //region Reactions to model
    private void updateWidgetVisibility(boolean isSSDSupported) {
        if (isSSDSupported) {
            setVisibility(VISIBLE);
        } else {
            setVisibility(GONE);
        }
    }

    private void updateCapacityTitle(@NonNull List<SSDVideoLicense> licenses) {
        if (licenses.size() <= 0) {
            return;
        }
        SSDVideoLicense license = licenses.get(0);
        int titleResource = R.string.uxsdk_storage_title_capacity;
        if (license == SSDVideoLicense.CINEMA_DNG) {
            titleResource = R.string.uxsdk_camera_ssd_video_license_cdng;
        } else if (license == SSDVideoLicense.PRO_RES_422HQ) {
            titleResource = R.string.uxsdk_camera_ssd_video_license_422hq;
        } else if (license == SSDVideoLicense.PRO_RES_4444XQ) {
            titleResource = R.string.uxsdk_camera_ssd_video_license_4444xq;
        }
        ssdCapacityOrLicenseTextView.setText(titleResource);
    }

    private void updateCapacityValue(long remainingSpace) {
        String remainingSpaceString;
        if (remainingSpace > CAPACITY_UNIT_SWITCH_LIMIT) {
            remainingSpaceString = (remainingSpace / CAPACITY_UNIT_SWITCH_LIMIT) + "G";
        } else {
            remainingSpaceString = remainingSpace + "M";
        }
        ssdCapacityValueTextView.setText(remainingSpaceString);
    }

    private void updateSSDResolutionAndFrameRate(@NonNull VideoResolutionFrameRate ssdResolutionAndFrameRate) {
        formatInfoTextView.setText(convertResolutionAndFrameRateToString(ssdResolutionAndFrameRate.getResolution(),
                ssdResolutionAndFrameRate.getFrameRate()));
    }

    private void updateClipInfo(@NonNull SSDClipFileNameMsg ssdClipFileName,
                                @NonNull SSDColor ssdColor) {

        String ssdColorName = "";
        Integer index = getSSDColorIndex(ssdColor);
        if (index != null && ssdColorArray != null && index < ssdColorArray.length) {
            ssdColorName = ssdColorArray[index];
        }
        String clipInfoString = ssdClipFileName.toString() + " " + ssdColorName;
        ssdClipInfoTextView.setText(clipInfoString);
    }

    private void updateSSDState(@NonNull SSDOperationState ssdOperationState, boolean cameraInShootPhotoRawBurstMode) {
        boolean needShowStatus = true;
        if (ssdOperationState != SSDOperationState.SWITCHING_LICENSE) {
            ssdImageView.setImageDrawable(ssdIconMap.get(ssdOperationState));
        }
        switch (ssdOperationState) {
            case NOT_FOUND:
            case UNKNOWN:
                statusInfoTextView.setText(R.string.uxsdk_ssd_status_error_nossd);
                needShowStatus = false;
                break;
            case SAVING:
                needShowStatus = false;
                statusInfoTextView.setText(R.string.uxsdk_camera_ssd_saving);
                break;
            case FORMATTING:
                statusInfoTextView.setText(R.string.uxsdk_ssd_status_formatting);
                break;
            case INITIALIZING:
                statusInfoTextView.setText(R.string.uxsdk_ssd_status_init);
                break;
            case STATE_ERROR:
                statusInfoTextView.setText(R.string.uxsdk_ssd_status_verify_failed);
                break;
            case FULL:
                statusInfoTextView.setText(R.string.uxsdk_ssd_status_full);
                break;
            case POOR_CONNECTION:
                statusInfoTextView.setText(R.string.uxsdk_ssd_status_poor_connection);
                break;
            case SWITCHING_LICENSE:
                if (cameraInShootPhotoRawBurstMode) {
                    needShowStatus = false;
                } else {
                    statusInfoTextView.setText(R.string.uxsdk_ssd_status_switching_mode);
                    ssdImageView.setImageDrawable(ssdIconMap.get(ssdOperationState));
                }
                break;
            case FORMATTING_REQUIRED:
                statusInfoTextView.setText(R.string.uxsdk_ssd_status_need_format);
                break;
            default:
                needShowStatus = false;
                break;
        }

        performNeedShowStatus(needShowStatus, ssdOperationState);
    }

    private void performNeedShowStatus(boolean needShowStatus, SSDOperationState ssdOperationState) {
        if (needShowStatus) {
            statusInfoTextView.setVisibility(VISIBLE);
            formatInfoTextView.setVisibility(GONE);
            ssdCapacityOrLicenseTextView.setVisibility(GONE);
            ssdCapacityValueTextView.setVisibility(GONE);
        } else {
            if (ssdOperationState == SSDOperationState.UNKNOWN || ssdOperationState == SSDOperationState.NOT_FOUND) {
                ssdCapacityOrLicenseTextView.setVisibility(GONE);
                ssdCapacityValueTextView.setVisibility(GONE);
            } else {
                ssdCapacityOrLicenseTextView.setVisibility(VISIBLE);
                ssdCapacityValueTextView.setVisibility(VISIBLE);
            }
            statusInfoTextView.setVisibility(GONE);
            formatInfoTextView.setVisibility(VISIBLE);
        }

        // Update the the status icon based on the state
        if (ssdOperationState == SSDOperationState.SAVING) {
            if (!isSSDRecording) {
                isSSDRecording = true;
                ssdStatusImageView.setVisibility(VISIBLE);
                ssdStatusImageView.startAnimation(ssdSaveAnimation);
            }
        } else {
            if (isSSDRecording) {
                ssdStatusImageView.clearAnimation();
                ssdStatusImageView.setVisibility(GONE);
                isSSDRecording = false;
            }
        }
    }
    //endregion

    //region Helper methods

    private void initDefaultIcons() {
        ssdIconMap = new HashMap<>();
        if (isInEditMode()) {
            return;
        }
        ssdIconMap.put(SSDOperationState.NOT_FOUND, getResources().getDrawable(R.drawable.uxsdk_ic_ssd_not_inserted_gray));
        ssdIconMap.put(SSDOperationState.UNKNOWN, getResources().getDrawable(R.drawable.uxsdk_ic_ssd_not_inserted_gray));
        ssdIconMap.put(SSDOperationState.IDLE, getResources().getDrawable(R.drawable.uxsdk_ic_ssd_icon));
        ssdIconMap.put(SSDOperationState.SAVING, getResources().getDrawable(R.drawable.uxsdk_ic_ssd_icon));
        ssdIconMap.put(SSDOperationState.FORMATTING, getResources().getDrawable(R.drawable.uxsdk_ic_ssd_icon));
        ssdIconMap.put(SSDOperationState.INITIALIZING, getResources().getDrawable(R.drawable.uxsdk_ic_ssd_icon));
        ssdIconMap.put(SSDOperationState.STATE_ERROR, getResources().getDrawable(R.drawable.uxsdk_ic_ssd_not_inserted_gray));
        ssdIconMap.put(SSDOperationState.FULL, getResources().getDrawable(R.drawable.uxsdk_ic_ssd_full));
        ssdIconMap.put(SSDOperationState.POOR_CONNECTION, getResources().getDrawable(R.drawable.uxsdk_ic_ssd_warning));
        ssdIconMap.put(SSDOperationState.SWITCHING_LICENSE, getResources().getDrawable(R.drawable.uxsdk_ic_ssd_icon));
        ssdIconMap.put(SSDOperationState.FORMATTING_REQUIRED, getResources().getDrawable(R.drawable.uxsdk_ic_ssd_warning));
    }

    private String convertResolutionAndFrameRateToString(@Nullable VideoResolution resolution,
                                                         @Nullable VideoFrameRate frameRate) {
        String processedResolutionString = NULL_STRING;
        if (videoResolutionArray != null && resolution != null && resolution.value() < videoResolutionArray.length) {
            processedResolutionString = videoResolutionArray[resolution.value()];
        }

        if (resolution == VideoResolution.RESOLUTION_UNSET) {
            return processedResolutionString;
        }

        String processedFrameRateString = NULL_STRING;
        if (frameRateArray != null && frameRate != null && frameRate.value() < frameRateArray.length) {
            processedFrameRateString = frameRateArray[frameRate.value()];
        }
        return processedResolutionString + "/" + processedFrameRateString;
    }

    private Integer getSSDColorIndex(@NonNull SSDColor ssdColor) {
        SSDColor[] ssdColorValueArray = SSDColor.values();
        for (int i = 0; i < ssdColorValueArray.length; i++) {
            if (ssdColorValueArray[i] == ssdColor) {
                return i;
            }
        }
        return null;
    }
    //endregion

    //region Customizations
    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_camera_config_ssd_ratio);
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
     * Set text appearance of the SSD clip info text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setSSDClipInfoTextAppearance(@StyleRes int textAppearance) {
        ssdClipInfoTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the SSD clip info text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getSSDClipInfoTextColors() {
        return ssdClipInfoTextView.getTextColors();
    }

    /**
     * Get current text color of the SSD clip info text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getSSDClipInfoTextColor() {
        return ssdClipInfoTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the SSD clip info text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setSSDClipInfoTextColor(@NonNull ColorStateList colorStateList) {
        ssdClipInfoTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the SSD clip info text view
     *
     * @param color color integer resource
     */
    public void setSSDClipInfoTextColor(@ColorInt int color) {
        ssdClipInfoTextView.setTextColor(color);
    }

    /**
     * Get current text size of the SSD clip info text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getSSDClipInfoTextSize() {
        return ssdClipInfoTextView.getTextSize();
    }

    /**
     * Set the text size of the SSD clip info text view
     *
     * @param textSize text size float value
     */
    public void setSSDClipInfoTextSize(@Dimension float textSize) {
        ssdClipInfoTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the SSD clip info text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getSSDClipInfoTextBackground() {
        return ssdClipInfoTextView.getBackground();
    }

    /**
     * Set the resource ID for the background of the SSD clip info text view
     *
     * @param resourceId Drawable resource for the background
     */
    public void setSSDClipInfoTextBackground(@DrawableRes int resourceId) {
        ssdClipInfoTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background for the SSD clip info text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setSSDClipInfoTextBackground(@Nullable Drawable drawable) {
        ssdClipInfoTextView.setBackground(drawable);
    }

    /**
     * Set text appearance of the SSD capacity or license text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setSSDCapacityOrLicenseTextAppearance(@StyleRes int textAppearance) {
        ssdCapacityOrLicenseTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the SSD capacity or license text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getSSDCapacityOrLicenseTextColors() {
        return ssdCapacityOrLicenseTextView.getTextColors();
    }

    /**
     * Get current text color of the SSD capacity or license text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getSSDCapacityOrLicenseTextColor() {
        return ssdCapacityOrLicenseTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the SSD capacity or license text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setSSDCapacityOrLicenseTextColor(@NonNull ColorStateList colorStateList) {
        ssdCapacityOrLicenseTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the SSD capacity or license text view
     *
     * @param color color integer resource
     */
    public void setSSDCapacityOrLicenseTextColor(@ColorInt int color) {
        ssdCapacityOrLicenseTextView.setTextColor(color);
    }

    /**
     * Get current text size of the SSD capacity or license text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getSSDCapacityOrLicenseTextSize() {
        return ssdCapacityOrLicenseTextView.getTextSize();
    }

    /**
     * Set the text size of the SSD capacity or license text view
     *
     * @param textSize text size float value
     */
    public void setSSDCapacityOrLicenseTextSize(@Dimension float textSize) {
        ssdCapacityOrLicenseTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the SSD capacity or license text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getSSDCapacityOrLicenseTextBackground() {
        return ssdCapacityOrLicenseTextView.getBackground();
    }

    /**
     * Set the resource ID for the background of the SSD capacity or license text view
     *
     * @param resourceId Drawable resource for the background
     */
    public void setSSDCapacityOrLicenseTextBackground(@DrawableRes int resourceId) {
        ssdCapacityOrLicenseTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background for the SSD capacity or license text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setSSDCapacityOrLicenseTextBackground(@Nullable Drawable drawable) {
        ssdCapacityOrLicenseTextView.setBackground(drawable);
    }

    /**
     * Set text appearance of the SSD status info text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setStatusInfoTextAppearance(@StyleRes int textAppearance) {
        statusInfoTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the SSD status info text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getStatusInfoTextColors() {
        return statusInfoTextView.getTextColors();
    }

    /**
     * Get current text color of the SSD status info text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getStatusInfoTextColor() {
        return statusInfoTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the SSD status info text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setStatusInfoTextColor(@NonNull ColorStateList colorStateList) {
        statusInfoTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the SSD status info text view
     *
     * @param color color integer resource
     */
    public void setStatusInfoTextColor(@ColorInt int color) {
        statusInfoTextView.setTextColor(color);
    }

    /**
     * Get current text size of the SSD status info text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getStatusInfoTextSize() {
        return statusInfoTextView.getTextSize();
    }

    /**
     * Set the text size of the SSD status info text view
     *
     * @param textSize text size float value
     */
    public void setStatusInfoTextSize(@Dimension float textSize) {
        statusInfoTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the SSD status info text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getStatusInfoTextBackground() {
        return statusInfoTextView.getBackground();
    }

    /**
     * Set the resource ID for the background of the SSD status info text view
     *
     * @param resourceId Drawable resource for the background
     */
    public void setStatusInfoTextBackground(@DrawableRes int resourceId) {
        statusInfoTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background for the SSD status info text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setStatusInfoTextBackground(@Nullable Drawable drawable) {
        statusInfoTextView.setBackground(drawable);
    }

    /**
     * Set text appearance of the SSD format info text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setFormatInfoTextAppearance(@StyleRes int textAppearance) {
        formatInfoTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the SSD format info text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getFormatInfoTextColors() {
        return formatInfoTextView.getTextColors();
    }

    /**
     * Get current text color of the SSD format info text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getFormatInfoTextColor() {
        return formatInfoTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the SSD format info text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setFormatInfoTextColor(@NonNull ColorStateList colorStateList) {
        formatInfoTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the SSD format info text view
     *
     * @param color color integer resource
     */
    public void setFormatInfoTextColor(@ColorInt int color) {
        formatInfoTextView.setTextColor(color);
    }

    /**
     * Get current text size of the SSD format info text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getFormatInfoTextSize() {
        return formatInfoTextView.getTextSize();
    }

    /**
     * Set the text size of the SSD format info text view
     *
     * @param textSize text size float value
     */
    public void setFormatInfoTextSize(@Dimension float textSize) {
        formatInfoTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the SSD format info text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getFormatInfoTextBackground() {
        return formatInfoTextView.getBackground();
    }

    /**
     * Set the resource ID for the background of the SSD format info text view
     *
     * @param resourceId Drawable resource for the background
     */
    public void setFormatInfoTextBackground(@DrawableRes int resourceId) {
        formatInfoTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background for the SSD format info text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setFormatInfoTextBackground(@Nullable Drawable drawable) {
        formatInfoTextView.setBackground(drawable);
    }

    /**
     * Set text appearance of the SSD capacity value text view
     *
     * @param textAppearance Style resource for text appearance
     */
    public void setSSDCapacityValueTextAppearance(@StyleRes int textAppearance) {
        ssdCapacityValueTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get current text color state list of the SSD capacity value text view
     *
     * @return ColorStateList resource
     */
    @Nullable
    public ColorStateList getSSDCapacityValueTextColors() {
        return ssdCapacityValueTextView.getTextColors();
    }

    /**
     * Get current text color of the SSD capacity value text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getSSDCapacityValueTextColor() {
        return ssdCapacityValueTextView.getCurrentTextColor();
    }

    /**
     * Set text color state list for the SSD capacity value text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setSSDCapacityValueTextColor(@NonNull ColorStateList colorStateList) {
        ssdCapacityValueTextView.setTextColor(colorStateList);
    }

    /**
     * Set the text color for the SSD capacity value text view
     *
     * @param color color integer resource
     */
    public void setSSDCapacityValueTextColor(@ColorInt int color) {
        ssdCapacityValueTextView.setTextColor(color);
    }

    /**
     * Get current text size of the SSD capacity value text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getSSDCapacityValueTextSize() {
        return ssdCapacityValueTextView.getTextSize();
    }

    /**
     * Set the text size of the SSD capacity value text view
     *
     * @param textSize text size float value
     */
    public void setSSDCapacityValueTextSize(@Dimension float textSize) {
        ssdCapacityValueTextView.setTextSize(textSize);
    }

    /**
     * Get current background of the SSD capacity value text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getSSDCapacityValueTextBackground() {
        return ssdCapacityValueTextView.getBackground();
    }

    /**
     * Set the resource ID for the background of the SSD capacity value text view
     *
     * @param resourceId Drawable resource for the background
     */
    public void setSSDCapacityValueTextBackground(@DrawableRes int resourceId) {
        ssdCapacityValueTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background for the SSD capacity value text view
     *
     * @param drawable Drawable resource for the background
     */
    public void setSSDCapacityValueTextBackground(@Nullable Drawable drawable) {
        ssdCapacityValueTextView.setBackground(drawable);
    }

    /**
     * Get the drawable resource for the SSD status icon
     *
     * @return Drawable for the SSD status icon
     */
    @NonNull
    public Drawable getSSDStatusIcon() {
        return ssdStatusImageView.getDrawable();
    }

    /**
     * Set the resource ID for the SSD status icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setSSDStatusIcon(@DrawableRes int resourceId) {
        ssdStatusImageView.setImageResource(resourceId);
    }

    /**
     * Set the drawable resource for the SSD status icon
     *
     * @param icon Drawable resource for the image
     */
    public void setSSDStatusIcon(@NonNull Drawable icon) {
        ssdStatusImageView.setImageDrawable(icon);
    }

    /**
     * Get the background drawable resource for the SSD status icon
     *
     * @return Drawable for the SSD status icon's background
     */
    @Nullable
    public Drawable getSSDStatusIconBackground() {
        return ssdStatusImageView.getBackground();
    }

    /**
     * Set the resource ID for the SSD status icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setSSDStatusIconBackground(@DrawableRes int resourceId) {
        ssdStatusImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the SSD status icon's background
     *
     * @param background Drawable resource for the background
     */
    public void setSSDStatusIconBackground(@Nullable Drawable background) {
        ssdStatusImageView.setBackground(background);
    }

    /**
     * Sets the icon to the given image when the {@link SSDOperationState} is the given value.
     *
     * @param state      The state at which the icon will change to the given image.
     * @param resourceId The id of the image the icon will change to.
     */
    public void setSSDIcon(@NonNull SSDOperationState state, @DrawableRes int resourceId) {
        setSSDIcon(state, getResources().getDrawable(resourceId));
    }

    /**
     * Sets the icon to the given image when the {@link SSDOperationState} is the given value.
     *
     * @param state    The state at which the icon will change to the given image.
     * @param drawable The image the icon will change to.
     */
    public void setSSDIcon(@NonNull SSDOperationState state, @Nullable Drawable drawable) {
        ssdIconMap.put(state, drawable);
        checkAndUpdateSSDState();
    }

    /**
     * Gets the image that the icon will change to when the {@link SSDOperationState} is the
     * given value.
     *
     * @param state The state at which the icon will change to the given image.
     */
    @Nullable
    public Drawable getSSDIcon(@NonNull SSDOperationState state) {
        return ssdIconMap.get(state);
    }

    /**
     * Get the background drawable resource for the SSD icon
     *
     * @return Drawable for the SSD icon's background
     */
    @Nullable
    public Drawable getSSDIconBackground() {
        return ssdImageView.getBackground();
    }

    /**
     * Set the resource ID for the SSD icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setSSDIconBackground(@DrawableRes int resourceId) {
        ssdImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the SSD icon's background
     *
     * @param background Drawable resource for the background
     */
    public void setSSDIconBackground(@Nullable Drawable background) {
        ssdImageView.setBackground(background);
    }

    //Initialize all customizable attributes
    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CameraConfigSSDWidget);

        if (!isInEditMode()) {
            updateCameraSource(ComponentIndexType.find(typedArray.getInt(R.styleable.CameraConfigSSDWidget_uxsdk_cameraIndex, 0)), CameraLensType.UNKNOWN);
        }

        int ssdClipInfoTextAppearanceId =
                typedArray.getResourceId(R.styleable.CameraConfigSSDWidget_uxsdk_ssdClipInfoTextAppearance, INVALID_RESOURCE);
        if (ssdClipInfoTextAppearanceId != INVALID_RESOURCE) {
            setSSDClipInfoTextAppearance(ssdClipInfoTextAppearanceId);
        }

        float ssdClipInfoTextSize =
                typedArray.getDimension(R.styleable.CameraConfigSSDWidget_uxsdk_ssdClipInfoTextSize, INVALID_RESOURCE);
        if (ssdClipInfoTextSize != INVALID_RESOURCE) {
            setSSDClipInfoTextSize(DisplayUtil.pxToSp(context, ssdClipInfoTextSize));
        }

        int ssdClipInfoTextColor =
                typedArray.getColor(R.styleable.CameraConfigSSDWidget_uxsdk_ssdClipInfoTextColor, INVALID_COLOR);
        if (ssdClipInfoTextColor != INVALID_COLOR) {
            setSSDClipInfoTextColor(ssdClipInfoTextColor);
        }

        Drawable ssdClipInfoTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.CameraConfigSSDWidget_uxsdk_ssdClipInfoTextBackground);
        if (ssdClipInfoTextBackgroundDrawable != null) {
            setSSDClipInfoTextBackground(ssdClipInfoTextBackgroundDrawable);
        }

        performSetSSDCapacity(context, typedArray);

        int statusInfoTextAppearanceId =
                typedArray.getResourceId(R.styleable.CameraConfigSSDWidget_uxsdk_statusInfoTextAppearance, INVALID_RESOURCE);
        if (statusInfoTextAppearanceId != INVALID_RESOURCE) {
            setStatusInfoTextAppearance(statusInfoTextAppearanceId);
        }

        float statusInfoTextSize =
                typedArray.getDimension(R.styleable.CameraConfigSSDWidget_uxsdk_statusInfoTextSize, INVALID_RESOURCE);
        if (statusInfoTextSize != INVALID_RESOURCE) {
            setStatusInfoTextSize(DisplayUtil.pxToSp(context, statusInfoTextSize));
        }

        int statusInfoTextColor =
                typedArray.getColor(R.styleable.CameraConfigSSDWidget_uxsdk_statusInfoTextColor, INVALID_COLOR);
        if (statusInfoTextColor != INVALID_COLOR) {
            setStatusInfoTextColor(statusInfoTextColor);
        }

        Drawable statusInfoTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.CameraConfigSSDWidget_uxsdk_statusInfoTextBackground);
        if (statusInfoTextBackgroundDrawable != null) {
            setStatusInfoTextBackground(statusInfoTextBackgroundDrawable);
        }

        setFormatInfoAndSSDCapacity(context, typedArray);

        performSetSSDIcon(typedArray);

        typedArray.recycle();
    }

    private void setFormatInfoAndSSDCapacity(Context context, TypedArray typedArray){
        int formatInfoTextAppearanceId =
                typedArray.getResourceId(R.styleable.CameraConfigSSDWidget_uxsdk_imageFormatTextAppearance, INVALID_RESOURCE);
        if (formatInfoTextAppearanceId != INVALID_RESOURCE) {
            setFormatInfoTextAppearance(formatInfoTextAppearanceId);
        }

        float formatInfoTextSize =
                typedArray.getDimension(R.styleable.CameraConfigSSDWidget_uxsdk_imageFormatTextSize, INVALID_RESOURCE);
        if (formatInfoTextSize != INVALID_RESOURCE) {
            setFormatInfoTextSize(DisplayUtil.pxToSp(context, formatInfoTextSize));
        }

        int formatInfoTextColor =
                typedArray.getColor(R.styleable.CameraConfigSSDWidget_uxsdk_imageFormatTextColor, INVALID_COLOR);
        if (formatInfoTextColor != INVALID_COLOR) {
            setFormatInfoTextColor(formatInfoTextColor);
        }

        Drawable formatInfoTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.CameraConfigSSDWidget_uxsdk_imageFormatTextBackground);
        if (formatInfoTextBackgroundDrawable != null) {
            setFormatInfoTextBackground(formatInfoTextBackgroundDrawable);
        }

        int ssdCapacityValueTextAppearanceId =
                typedArray.getResourceId(R.styleable.CameraConfigSSDWidget_uxsdk_capacityValueTextAppearance,
                        INVALID_RESOURCE);
        if (ssdCapacityValueTextAppearanceId != INVALID_RESOURCE) {
            setSSDCapacityValueTextAppearance(ssdCapacityValueTextAppearanceId);
        }

        float ssdCapacityValueTextSize =
                typedArray.getDimension(R.styleable.CameraConfigSSDWidget_uxsdk_capacityValueTextSize, INVALID_RESOURCE);
        if (ssdCapacityValueTextSize != INVALID_RESOURCE) {
            setSSDCapacityValueTextSize(DisplayUtil.pxToSp(context, ssdCapacityValueTextSize));
        }

        int ssdCapacityValueTextColor =
                typedArray.getColor(R.styleable.CameraConfigSSDWidget_uxsdk_capacityValueTextColor, INVALID_COLOR);
        if (ssdCapacityValueTextColor != INVALID_COLOR) {
            setSSDCapacityValueTextColor(ssdCapacityValueTextColor);
        }
    }

    private void performSetSSDCapacity(Context context, TypedArray typedArray){
        int ssdCapacityOrLicenseTextAppearanceId =
                typedArray.getResourceId(R.styleable.CameraConfigSSDWidget_uxsdk_capacityOrLicenseTextAppearance,
                        INVALID_RESOURCE);
        if (ssdCapacityOrLicenseTextAppearanceId != INVALID_RESOURCE) {
            setSSDCapacityOrLicenseTextAppearance(ssdCapacityOrLicenseTextAppearanceId);
        }

        float ssdCapacityOrLicenseTextSize =
                typedArray.getDimension(R.styleable.CameraConfigSSDWidget_uxsdk_capacityOrLicenseTextSize, INVALID_RESOURCE);
        if (ssdCapacityOrLicenseTextSize != INVALID_RESOURCE) {
            setSSDCapacityOrLicenseTextSize(DisplayUtil.pxToSp(context, ssdCapacityOrLicenseTextSize));
        }

        int ssdCapacityOrLicenseTextColor =
                typedArray.getColor(R.styleable.CameraConfigSSDWidget_uxsdk_capacityOrLicenseTextColor, INVALID_COLOR);
        if (ssdCapacityOrLicenseTextColor != INVALID_COLOR) {
            setSSDCapacityOrLicenseTextColor(ssdCapacityOrLicenseTextColor);
        }

        Drawable ssdCapacityOrLicenseTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.CameraConfigSSDWidget_uxsdk_capacityOrLicenseTextBackground);
        if (ssdCapacityOrLicenseTextBackgroundDrawable != null) {
            setSSDCapacityOrLicenseTextBackground(ssdCapacityOrLicenseTextBackgroundDrawable);
        }
    }

    private void performSetSSDIcon(TypedArray typedArray){
        Drawable ssdCapacityValueTextBackgroundDrawable =
                typedArray.getDrawable(R.styleable.CameraConfigSSDWidget_uxsdk_capacityValueTextBackground);
        if (ssdCapacityValueTextBackgroundDrawable != null) {
            setSSDCapacityValueTextBackground(ssdCapacityValueTextBackgroundDrawable);
        }

        Drawable ssdStatusIcon = typedArray.getDrawable(R.styleable.CameraConfigSSDWidget_uxsdk_ssdStatusIcon);
        if (ssdStatusIcon != null) {
            setSSDStatusIcon(ssdStatusIcon);
        }

        Drawable ssdNotFoundIcon = typedArray.getDrawable(R.styleable.CameraConfigSSDWidget_uxsdk_ssdNotFoundIcon);
        if (ssdNotFoundIcon != null) {
            setSSDIcon(SSDOperationState.NOT_FOUND, ssdNotFoundIcon);
        }

        Drawable ssdUnknownIcon = typedArray.getDrawable(R.styleable.CameraConfigSSDWidget_uxsdk_ssdUnknownIcon);
        if (ssdUnknownIcon != null) {
            setSSDIcon(SSDOperationState.UNKNOWN, ssdUnknownIcon);
        }

        Drawable ssdIdleIcon = typedArray.getDrawable(R.styleable.CameraConfigSSDWidget_uxsdk_ssdIdleIcon);
        if (ssdIdleIcon != null) {
            setSSDIcon(SSDOperationState.IDLE, ssdIdleIcon);
        }

        Drawable ssdSavingIcon = typedArray.getDrawable(R.styleable.CameraConfigSSDWidget_uxsdk_ssdSavingIcon);
        if (ssdSavingIcon != null) {
            setSSDIcon(SSDOperationState.SAVING, ssdSavingIcon);
        }

        Drawable ssdFormattingIcon = typedArray.getDrawable(R.styleable.CameraConfigSSDWidget_uxsdk_ssdFormattingIcon);
        if (ssdFormattingIcon != null) {
            setSSDIcon(SSDOperationState.FORMATTING, ssdFormattingIcon);
        }

        Drawable ssdInitializingIcon = typedArray.getDrawable(R.styleable.CameraConfigSSDWidget_uxsdk_ssdInitializingIcon);
        if (ssdInitializingIcon != null) {
            setSSDIcon(SSDOperationState.INITIALIZING, ssdInitializingIcon);
        }

        Drawable ssdErrorIcon = typedArray.getDrawable(R.styleable.CameraConfigSSDWidget_uxsdk_ssdErrorIcon);
        if (ssdErrorIcon != null) {
            setSSDIcon(SSDOperationState.STATE_ERROR, ssdErrorIcon);
        }

        Drawable ssdFullIcon = typedArray.getDrawable(R.styleable.CameraConfigSSDWidget_uxsdk_ssdFullIcon);
        if (ssdFullIcon != null) {
            setSSDIcon(SSDOperationState.FULL, ssdFullIcon);
        }

        Drawable ssdPoorConnectionIcon = typedArray.getDrawable(R.styleable.CameraConfigSSDWidget_uxsdk_ssdPoorConnectionIcon);
        if (ssdPoorConnectionIcon != null) {
            setSSDIcon(SSDOperationState.POOR_CONNECTION, ssdPoorConnectionIcon);
        }

        Drawable ssdSwitchingLicenseIcon = typedArray.getDrawable(R.styleable.CameraConfigSSDWidget_uxsdk_ssdSwitchingLicenseIcon);
        if (ssdSwitchingLicenseIcon != null) {
            setSSDIcon(SSDOperationState.SWITCHING_LICENSE, ssdSwitchingLicenseIcon);
        }

        Drawable ssdFormattingRequiredIcon = typedArray.getDrawable(R.styleable.CameraConfigSSDWidget_uxsdk_ssdFormattingRequiredIcon);
        if (ssdFormattingRequiredIcon != null) {
            setSSDIcon(SSDOperationState.FORMATTING_REQUIRED, ssdFormattingRequiredIcon);
        }
    }
    //endregion
}
