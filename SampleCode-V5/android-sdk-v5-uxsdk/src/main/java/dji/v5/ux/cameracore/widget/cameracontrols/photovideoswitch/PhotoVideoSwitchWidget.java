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

package dji.v5.ux.cameracore.widget.cameracontrols.photovideoswitch;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.ux.core.base.widget.FrameLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.RxUtil;

/**
 * Widget can be used to switch between shoot photo mode and record video mode
 */
public class PhotoVideoSwitchWidget extends FrameLayoutWidget implements View.OnClickListener, ICameraIndex {

    //region Fields
    private static final String TAG = "PhotoVideoSwitchWidget";
    private ImageView foregroundImageView;
    private Drawable photoModeDrawable;
    private Drawable videoModeDrawable;
    private PhotoVideoSwitchWidgetModel widgetModel;
    //endregion

    //region Lifecycle
    public PhotoVideoSwitchWidget(@NonNull Context context) {
        super(context);
    }

    public PhotoVideoSwitchWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PhotoVideoSwitchWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_photo_video_switch, this);
        foregroundImageView = findViewById(R.id.image_view_foreground);
        if (!isInEditMode()) {
            widgetModel =
                    new PhotoVideoSwitchWidgetModel(DJISDKModel.getInstance(),
                            ObservableInMemoryKeyedStore.getInstance());
            setOnClickListener(this);
        }
        photoModeDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_camera_mode_photo);
        videoModeDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_camera_mode_video);
        if (attrs != null) {
            initAttributes(context, attrs);
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

    @Override
    protected void reactToModelChanges() {
        addReaction(
                widgetModel.isEnabled()
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(this::enableWidget));

        addReaction(
                widgetModel.isPictureMode()
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(this::updateUI));
    }

    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_default_ratio);
    }

    @Override
    public void onClick(View v) {
        addDisposable(widgetModel.toggleCameraMode()
                .observeOn(SchedulerProvider.ui())
                .subscribe(
                        () -> {
                        }, RxUtil.logErrorConsumer(TAG, "Switch camera Mode")
                ));
    }
    //endregion

    //region private helpers
    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PhotoVideoSwitchWidget);
        if (!isInEditMode()){
            updateCameraSource(ComponentIndexType.find(typedArray.getInt(R.styleable.PhotoVideoSwitchWidget_uxsdk_cameraIndex, 0)), CameraLensType.UNKNOWN);
        }

        if (typedArray.getDrawable(R.styleable.PhotoVideoSwitchWidget_uxsdk_photoModeIcon) != null) {
            photoModeDrawable = typedArray.getDrawable(R.styleable.PhotoVideoSwitchWidget_uxsdk_photoModeIcon);
        }

        if (typedArray.getDrawable(R.styleable.PhotoVideoSwitchWidget_uxsdk_videoModeIcon) != null) {
            videoModeDrawable = typedArray.getDrawable(R.styleable.PhotoVideoSwitchWidget_uxsdk_videoModeIcon);
        }
        setIconBackground(typedArray.getDrawable(R.styleable.PhotoVideoSwitchWidget_uxsdk_iconBackground));

        typedArray.recycle();
    }

    private void updateUI(boolean isPictureMode) {
        if (isPictureMode) {
            foregroundImageView.setImageDrawable(photoModeDrawable);
        } else {
            foregroundImageView.setImageDrawable(videoModeDrawable);
        }
    }

    private void enableWidget(Boolean isEnabled) {
        setEnabled(isEnabled);
    }

    private void checkAndUpdateUI() {
        if (!isInEditMode()) {
            addDisposable(widgetModel.isPictureMode().firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(this::updateUI, RxUtil.logErrorConsumer(TAG, "Update UI ")));
        }
    }
    //endregion

    //region customization

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
     * Get photo mode icon
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getPhotoModeIcon() {
        return photoModeDrawable;
    }

    /**
     * Set photo mode drawable resource
     *
     * @param resourceId resource id of  photo mode icon
     */
    public void setPhotoModeIcon(@DrawableRes int resourceId) {
        setPhotoModeIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set photo mode drawable
     *
     * @param drawable Drawable to be used as photo mode icon
     */
    public void setPhotoModeIcon(@Nullable Drawable drawable) {
        photoModeDrawable = drawable;
        checkAndUpdateUI();
    }

    /**
     * Get video mode icon
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getVideoModeIcon() {
        return videoModeDrawable;
    }

    /**
     * Set video mode drawable resource
     *
     * @param resourceId resource id of  video mode icon
     */
    public void setVideoModeIcon(@DrawableRes int resourceId) {
        setVideoModeIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set video mode drawable
     *
     * @param drawable Drawable to be used as video mode icon
     */
    public void setVideoModeIcon(@Nullable Drawable drawable) {
        videoModeDrawable = drawable;
        checkAndUpdateUI();
    }

    /**
     * Get current background of icon
     *
     * @return Drawable
     */
    @Nullable
    public Drawable getIconBackground() {
        return foregroundImageView.getBackground();
    }

    /**
     * Set background to icon
     *
     * @param resourceId resource id of background
     */
    public void setIconBackground(@DrawableRes int resourceId) {
        foregroundImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set background to icon
     *
     * @param drawable Drawable to be used as background
     */
    public void setIconBackground(@Nullable Drawable drawable) {
        foregroundImageView.setBackground(drawable);
    }
    //endregion
}
