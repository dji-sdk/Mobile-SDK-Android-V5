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

package dji.v5.ux.cameracore.widget.cameracapture.recordvideo;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
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
import dji.sdk.keyvalue.value.camera.CameraSDCardState;
import dji.sdk.keyvalue.value.camera.CameraStorageLocation;
import dji.sdk.keyvalue.value.camera.SSDOperationState;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.ux.R;
import dji.v5.ux.cameracore.util.CameraActionSound;
import dji.v5.ux.cameracore.widget.cameracapture.recordvideo.RecordVideoWidgetModel.RecordingState;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.CameraUtil;
import dji.v5.ux.core.util.ProductUtil;
import dji.v5.ux.core.util.RxUtil;
import io.reactivex.rxjava3.core.Completable;

/**
 * Record Video Widget
 * <p>
 * Widget can be used for recording video. The widget displays the current video mode. It also
 * displays the storage state and errors associated with it.
 */
public class RecordVideoWidget extends ConstraintLayoutWidget implements OnClickListener, ICameraIndex {

    //region Fields
    private static final String TAG = "RecordVideoWidget";
    private RecordVideoWidgetModel widgetModel;
    private ImageView centerImageView;
    private TextView videoTimerTextView;
    private ImageView storageStatusOverlayImageView;
    private Map<StorageIconState, Drawable> storageInternalIconMap;
    private Map<StorageIconState, Drawable> storageSSDIconMap;
    private Map<StorageIconState, Drawable> storageSDCardIconMap;
    private Drawable recordVideoStartDrawable;
    private Drawable recordVideoStopDrawable;
    private Drawable recordVideoStartHasselbladDrawable;
    private Drawable recordVideoStopHasselbladDrawable;
    private CameraActionSound cameraActionSound;
    //endregion

    //region Lifecycle
    public RecordVideoWidget(Context context) {
        super(context);
    }

    public RecordVideoWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RecordVideoWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_record_video, this);
        centerImageView = findViewById(R.id.image_view_center);
        videoTimerTextView = findViewById(R.id.text_view_video_record_time);
        storageStatusOverlayImageView = findViewById(R.id.image_view_storage_status_overlay);
        storageInternalIconMap = new HashMap<>();
        storageSSDIconMap = new HashMap<>();
        storageSDCardIconMap = new HashMap<>();
        centerImageView.setOnClickListener(this);
        cameraActionSound = new CameraActionSound(context);
        if (!isInEditMode()) {
            widgetModel = new RecordVideoWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());
        }
        initDefaults();
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
        addReaction(widgetModel.getRecordingTimeInSeconds()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateRecordingTime, RxUtil.logErrorConsumer(TAG, "record time: ")));
        addReaction(widgetModel.getRecordingState()
                .observeOn(SchedulerProvider.ui())
                .subscribe(recordingState -> onIsRecordingVideoChange(recordingState, true), RxUtil.logErrorConsumer(TAG, "is recording: ")));
        addReaction(widgetModel.getCameraVideoStorageState()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateCameraForegroundResource, RxUtil.logErrorConsumer(TAG, "camera storage update: ")));
    }

    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_default_ratio);
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
        if (!isInEditMode()) {
            widgetModel.updateCameraSource(cameraIndex, lensType);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.equals(centerImageView)) {
            addDisposable(widgetModel.getRecordingState().firstOrError().flatMapCompletable(recordingState -> {
                if (recordingState == RecordingState.RECORDING_IN_PROGRESS) {
                    return widgetModel.stopRecordVideo();
                } else if (recordingState == RecordingState.RECORDING_STOPPED) {
                    return widgetModel.startRecordVideo();
                } else {
                    return Completable.complete();
                }
            }).observeOn(SchedulerProvider.ui()).subscribe(() -> {
            }, RxUtil.logErrorConsumer(TAG, "START STOP VIDEO")));
        }
    }
    //endregion

    //region private helpers
    private void initDefaults() {
        recordVideoStartDrawable = getResources().getDrawable(R.drawable.uxsdk_selector_start_record_video);
        recordVideoStopDrawable = getResources().getDrawable(R.drawable.uxsdk_selector_stop_record_video);
        recordVideoStartHasselbladDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_hasselblad_shutter_start);
        recordVideoStopHasselbladDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_hasselblad_shutter_stop);
        setInternalStorageIcon(StorageIconState.NOT_INSERTED, R.drawable.uxsdk_ic_internal_storage_not_inserted);
        setInternalStorageIcon(StorageIconState.SLOW, R.drawable.uxsdk_ic_internal_storage_slow);
        setInternalStorageIcon(StorageIconState.FULL, R.drawable.uxsdk_ic_internal_storage_full);
        setSDCardStorageIcon(StorageIconState.NOT_INSERTED, R.drawable.uxsdk_ic_sdcard_not_inserted);
        setSDCardStorageIcon(StorageIconState.SLOW, R.drawable.uxsdk_ic_sdcard_slow);
        setSDCardStorageIcon(StorageIconState.FULL, R.drawable.uxsdk_ic_sdcard_full);
        setSSDStorageIcon(StorageIconState.NOT_INSERTED, R.drawable.uxsdk_ic_ssd_not_inserted);
        setSSDStorageIcon(StorageIconState.FULL, R.drawable.uxsdk_ic_ssd_full);
        setVideoTimerTextColor(Color.WHITE);
    }

    private void initAttributes(@NonNull Context context, @NonNull AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RecordVideoWidget);
        updateCameraSource(ComponentIndexType.find(typedArray.getInt(R.styleable.RecordVideoWidget_uxsdk_cameraIndex, 0)),
                CameraLensType.find(typedArray.getInt(R.styleable.RecordVideoWidget_uxsdk_lensType, 0)));

        int textAppearance = typedArray.getResourceId(R.styleable.RecordVideoWidget_uxsdk_timerTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setTimerTextAppearance(textAppearance);
        }
        setVideoTimerTextColor(typedArray.getColor(R.styleable.RecordVideoWidget_uxsdk_timerTextColor, Color.WHITE));
        if (typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_timerTextBackground) != null) {
            setVideoTimerTextBackground(typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_timerTextBackground));
        }
        setVideoTimerTextSize(typedArray.getDimension(R.styleable.RecordVideoWidget_uxsdk_timerTextSize, 12));
        if (typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_foregroundIconBackground) != null) {
            setForegroundIconBackground(typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_foregroundIconBackground));
        }

        initRecordVideoDrawable(typedArray);
        initInternalStorageIcon(typedArray);
        initSDCardStorageIcon(typedArray);
        initSSDStorageIcon(typedArray);
        typedArray.recycle();
    }

    private void initSSDStorageIcon(TypedArray typedArray) {
        if (typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_ssdNotInsertedIcon) != null) {
            setSSDStorageIcon(StorageIconState.NOT_INSERTED, typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_ssdNotInsertedIcon));
        }
        if (typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_ssdFullIcon) != null) {
            setSSDStorageIcon(StorageIconState.FULL, typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_ssdFullIcon));
        }
        if (typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_ssdSlowIcon) != null) {
            setSSDStorageIcon(StorageIconState.SLOW, typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_ssdSlowIcon));
        }
    }

    private void initSDCardStorageIcon(TypedArray typedArray) {
        if (typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_sdCardNotInsertedIcon) != null) {
            setSDCardStorageIcon(StorageIconState.NOT_INSERTED, typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_sdCardNotInsertedIcon));
        }
        if (typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_sdCardFullIcon) != null) {
            setSDCardStorageIcon(StorageIconState.FULL, typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_sdCardFullIcon));
        }
        if (typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_sdCardSlowIcon) != null) {
            setSDCardStorageIcon(StorageIconState.SLOW, typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_sdCardSlowIcon));
        }

    }

    private void initInternalStorageIcon(TypedArray typedArray) {
        if (typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_internalStorageNotInsertedIcon) != null) {
            setInternalStorageIcon(StorageIconState.NOT_INSERTED, typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_internalStorageNotInsertedIcon));
        }
        if (typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_internalStorageFullIcon) != null) {
            setInternalStorageIcon(StorageIconState.FULL, typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_internalStorageFullIcon));
        }
        if (typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_internalStorageSlowIcon) != null) {
            setInternalStorageIcon(StorageIconState.SLOW, typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_internalStorageSlowIcon));
        }
    }

    private void initRecordVideoDrawable(TypedArray typedArray) {
        if (typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_recordStartIcon) != null) {
            recordVideoStartDrawable = typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_recordStartIcon);
        }
        if (typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_recordStopIcon) != null) {
            recordVideoStopDrawable = typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_recordStopIcon);
        }

        if (typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_recordStartHasselbladIcon) != null) {
            recordVideoStartHasselbladDrawable = typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_recordStartHasselbladIcon);
        }
        if (typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_recordStopHasselbladIcon) != null) {
            recordVideoStopHasselbladDrawable = typedArray.getDrawable(R.styleable.RecordVideoWidget_uxsdk_recordStopHasselbladIcon);
        }
    }

    private void updateCameraForegroundResource(CameraVideoStorageState cameraVideoStorageState) {
        Drawable foregroundResource = null;
        if (cameraVideoStorageState instanceof CameraSDVideoStorageState) {
            if (cameraVideoStorageState.getStorageLocation() == CameraStorageLocation.SDCARD) {
                foregroundResource = updateResourceWithStorageInSDCard(cameraVideoStorageState);
            } else if (cameraVideoStorageState.getStorageLocation() == CameraStorageLocation.INTERNAL) {
                foregroundResource = updateResourceWithStorageInternal(cameraVideoStorageState);
            }
        } else if (cameraVideoStorageState instanceof CameraSSDVideoStorageState) {
            if (((CameraSSDVideoStorageState) cameraVideoStorageState).getSsdOperationState()
                    == SSDOperationState.NOT_FOUND) {
                foregroundResource = getSSDStorageIcon(StorageIconState.NOT_INSERTED);
            } else if (((CameraSSDVideoStorageState) cameraVideoStorageState).getSsdOperationState()
                    == SSDOperationState.FULL) {
                foregroundResource = getSSDStorageIcon(StorageIconState.FULL);
            }
        }

        storageStatusOverlayImageView.setImageDrawable(foregroundResource);
    }

    private Drawable updateResourceWithStorageInSDCard(CameraVideoStorageState cameraVideoStorageState) {
        Drawable foregroundResource = null;
        if (((CameraSDVideoStorageState) cameraVideoStorageState).getSdCardOperationState()
                == CameraSDCardState.NOT_INSERTED) {
            foregroundResource = getSDCardStorageIcon(StorageIconState.NOT_INSERTED);
        } else if (((CameraSDVideoStorageState) cameraVideoStorageState).getSdCardOperationState()
                == CameraSDCardState.FULL) {
            foregroundResource = getSDCardStorageIcon(StorageIconState.FULL);
        } else if (((CameraSDVideoStorageState) cameraVideoStorageState).getSdCardOperationState()
                == CameraSDCardState.SLOW) {
            foregroundResource = getSDCardStorageIcon(StorageIconState.SLOW);
        }
        return foregroundResource;
    }

    private Drawable updateResourceWithStorageInternal(CameraVideoStorageState cameraVideoStorageState) {
        Drawable foregroundResource = null;
        if (((CameraSDVideoStorageState) cameraVideoStorageState).getSdCardOperationState()
                == CameraSDCardState.NOT_INSERTED) {
            foregroundResource = getInternalStorageIcon(StorageIconState.NOT_INSERTED);
        } else if (((CameraSDVideoStorageState) cameraVideoStorageState).getSdCardOperationState()
                == CameraSDCardState.FULL) {
            foregroundResource = getInternalStorageIcon(StorageIconState.FULL);
        } else if (((CameraSDVideoStorageState) cameraVideoStorageState).getSdCardOperationState()
                == CameraSDCardState.SLOW) {
            foregroundResource = getInternalStorageIcon(StorageIconState.SLOW);
        }
        return foregroundResource;
    }

    private void updateRecordingTime(int seconds) {
        videoTimerTextView.setText(CameraUtil.formatVideoTime(getResources(), seconds));
    }

    private void onIsRecordingVideoChange(RecordingState recordingState, boolean playSound) {
        boolean isRecordingVideo = recordingState == RecordingState.RECORDING_IN_PROGRESS;
        Drawable recordStart = ProductUtil.isHasselbladCamera() ? recordVideoStartHasselbladDrawable : recordVideoStartDrawable;
        Drawable recordStop = ProductUtil.isHasselbladCamera() ? recordVideoStopHasselbladDrawable : recordVideoStopDrawable;
        centerImageView.setImageDrawable(isRecordingVideo ? recordStop : recordStart);
        videoTimerTextView.setVisibility(isRecordingVideo ? View.VISIBLE : View.INVISIBLE);
        storageStatusOverlayImageView.setVisibility(isRecordingVideo ? View.GONE : View.VISIBLE);
        if (playSound) {
            if (recordingState == RecordingState.RECORDING_IN_PROGRESS) {
                addDisposable(cameraActionSound.playStartRecordVideo());
            } else if (recordingState == RecordingState.RECORDING_STOPPED) {
                addDisposable(cameraActionSound.playStopRecordVideo());
            }
        }
    }

    private void checkAndUpdateCameraForegroundResource() {
        if (!isInEditMode()) {
            addDisposable(widgetModel.getCameraVideoStorageState().firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(this::updateCameraForegroundResource,
                            RxUtil.logErrorConsumer(TAG, "check and update camera foreground resource: ")));
        }
    }

    private void checkAndUpdateCenterImageView() {
        if (!isInEditMode()) {
            addDisposable(widgetModel.getRecordingState().firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(recordingState -> onIsRecordingVideoChange(recordingState, false),
                            RxUtil.logErrorConsumer(TAG, "check and update camera foreground resource: ")));
        }
    }
    //endregion

    //region customizations

    /**
     * Get the current start recording video icon
     *
     * @return Drawable currently used
     */
    @Nullable
    public Drawable getRecordVideoStartDrawable() {
        return recordVideoStartDrawable;
    }

    /**
     * Set the start record video icon
     *
     * @param resourceId to be used
     */
    public void setRecordVideoStartDrawable(@DrawableRes int resourceId) {
        setRecordVideoStartDrawable(getResources().getDrawable(resourceId));
    }

    /**
     * Set the start record video icon
     *
     * @param drawable to be used
     */
    public void setRecordVideoStartDrawable(@Nullable Drawable drawable) {
        recordVideoStartDrawable = drawable;
        checkAndUpdateCenterImageView();
    }

    /**
     * Get the current stop video recording icon
     *
     * @return Drawable currently used
     */
    @Nullable
    public Drawable getRecordVideoStopDrawable() {
        return recordVideoStopDrawable;
    }

    /**
     * Set stop video recording icon
     *
     * @param resourceId to be used
     */
    public void setRecordVideoStopDrawable(@DrawableRes int resourceId) {
        setRecordVideoStopDrawable(getResources().getDrawable(resourceId));
    }

    /**
     * Set stop video recording icon
     *
     * @param drawable to be used
     */
    public void setRecordVideoStopDrawable(@Nullable Drawable drawable) {
        recordVideoStopDrawable = drawable;
        checkAndUpdateCenterImageView();
    }

    /**
     * Get the current icon for start video recording for Hasselblad camera
     *
     * @return Drawable currently used
     */
    @Nullable
    public Drawable getRecordVideoHasselbladDrawable() {
        return recordVideoStartHasselbladDrawable;
    }

    /**
     * Set the current icon for start video recording for Hasselblad camera
     *
     * @param resourceId to be used
     */
    public void setRecordVideoHasselbladDrawable(@DrawableRes int resourceId) {
        setRecordVideoHasselbladDrawable(getResources().getDrawable(resourceId));
    }

    /**
     * Set the current icon for start video recording for Hasselblad camera
     *
     * @param drawable to be used
     */
    public void setRecordVideoHasselbladDrawable(@Nullable Drawable drawable) {
        recordVideoStartHasselbladDrawable = drawable;
        checkAndUpdateCenterImageView();
    }

    /**
     * Get the current icon for stop video recording for Hasselblad camera
     * Currently on Mavic 2 Pro
     *
     * @return Drawable currently used
     */
    @Nullable
    public Drawable getRecordVideoStopHasselbladDrawable() {
        return recordVideoStopHasselbladDrawable;
    }

    /**
     * Set the icon for stop video recording for Hasselblad camera
     * Currently on Mavic 2 Pro
     *
     * @param resourceId to be used
     */
    public void setRecordVideoStopHasselbladDrawable(@DrawableRes int resourceId) {
        setRecordVideoStopHasselbladDrawable(getResources().getDrawable(resourceId));
    }

    /**
     * Set the icon for stop video recording for Hasselblad camera
     * Currently on Mavic 2 Pro
     *
     * @param drawable to be used
     */
    public void setRecordVideoStopHasselbladDrawable(@Nullable Drawable drawable) {
        recordVideoStopHasselbladDrawable = drawable;
        checkAndUpdateCenterImageView();
    }

    /**
     * Set the icon for internal storage based on storage icon state
     *
     * @param storageIconState for which icon is used
     * @param resourceId       to be used
     */
    public void setInternalStorageIcon(@NonNull StorageIconState storageIconState, @DrawableRes int resourceId) {
        setInternalStorageIcon(storageIconState, getResources().getDrawable(resourceId));
    }

    /**
     * Set the icon for internal storage based on storage icon state
     *
     * @param storageIconState for which icon is used
     * @param drawable         to be used
     */
    public void setInternalStorageIcon(@NonNull StorageIconState storageIconState, @Nullable Drawable drawable) {
        storageInternalIconMap.put(storageIconState, drawable);
        checkAndUpdateCameraForegroundResource();
    }

    /**
     * Get the icon for internal storage based on storage icon state
     *
     * @param storageIconState for which icon is used
     * @return Drawable currently used
     */
    @Nullable
    public Drawable getInternalStorageIcon(@NonNull StorageIconState storageIconState) {
        return storageInternalIconMap.get(storageIconState);
    }

    /**
     * Set the icon for SD card storage based on storage icon state
     *
     * @param storageIconState for which icon is used
     * @param resourceId       to be used
     */
    public void setSDCardStorageIcon(@NonNull StorageIconState storageIconState, @DrawableRes int resourceId) {
        setSDCardStorageIcon(storageIconState, getResources().getDrawable(resourceId));
    }

    /**
     * Set the icon for SD card storage based on storage icon state
     *
     * @param storageIconState for which icon is used
     * @param drawable         to be used
     */
    public void setSDCardStorageIcon(@NonNull StorageIconState storageIconState, @Nullable Drawable drawable) {
        storageSDCardIconMap.put(storageIconState, drawable);
        checkAndUpdateCameraForegroundResource();
    }

    /**
     * Get the icon for SD card storage based on storage icon state
     *
     * @param storageIconState for which icon is used
     * @return Drawable currently used
     */
    @Nullable
    public Drawable getSDCardStorageIcon(@NonNull StorageIconState storageIconState) {
        return storageSDCardIconMap.get(storageIconState);
    }

    /**
     * Set the icon for SSD storage based on storage icon state
     *
     * @param storageIconState for which icon should be used
     * @param resourceId       to be used
     */
    public void setSSDStorageIcon(@NonNull StorageIconState storageIconState, @DrawableRes int resourceId) {
        setSSDStorageIcon(storageIconState, getResources().getDrawable(resourceId));
    }

    /**
     * Set the icon for SSD storage based on storage icon state
     *
     * @param storageIconState for which icon should be used
     * @param drawable         Drawable to be used
     */
    public void setSSDStorageIcon(@NonNull StorageIconState storageIconState, @Nullable Drawable drawable) {
        storageSSDIconMap.put(storageIconState, drawable);
        checkAndUpdateCameraForegroundResource();
    }

    /**
     * Get the icon for SSD storage based on storage icon state
     *
     * @param storageIconState for which icon is used
     * @return Drawable currently used
     */
    @Nullable
    public Drawable getSSDStorageIcon(@NonNull StorageIconState storageIconState) {
        return storageSSDIconMap.get(storageIconState);
    }

    /**
     * Set the background of the foreground icon
     *
     * @param resourceId to be used as background
     */
    public void setForegroundIconBackground(@DrawableRes int resourceId) {
        storageStatusOverlayImageView.setBackgroundResource(resourceId);
    }

    /**
     * Set background of the foreground icon
     *
     * @param drawable to be used as background
     */
    public void setForegroundIconBackground(@Nullable Drawable drawable) {
        storageStatusOverlayImageView.setBackground(drawable);
    }

    /**
     * Get current background of foreground icon
     *
     * @return Drawable being used
     */
    public Drawable getForegroundIconBackground(@DrawableRes int resourceId) {
        return storageStatusOverlayImageView.getBackground();
    }

    /**
     * Set the background of the video record duration text
     *
     * @param resourceId to be used
     */
    public void setVideoTimerTextBackground(@DrawableRes int resourceId) {
        videoTimerTextView.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the video record duration text
     *
     * @param drawable to be used
     */
    public void setVideoTimerTextBackground(@Nullable Drawable drawable) {
        videoTimerTextView.setBackground(drawable);
    }

    /**
     * Get the color state list currently used for video record duration text
     *
     * @return ColorSateList
     */
    @Nullable
    public ColorStateList getVideoTimerTextColors() {
        return videoTimerTextView.getTextColors();
    }

    /**
     * Set the color state list for video record duration text
     *
     * @param colorStateList to be used
     */
    public void setVideoTimerTextColors(@Nullable ColorStateList colorStateList) {
        videoTimerTextView.setTextColor(colorStateList);
    }

    /**
     * Get the current text color of video record duration text
     *
     * @return integer value representing color
     */
    @ColorInt
    public int getVideoTimerTextColor() {
        return videoTimerTextView.getCurrentTextColor();
    }

    /**
     * Set the text color of video record duration text
     *
     * @param color integer value representing color
     */
    public void setVideoTimerTextColor(@ColorInt int color) {
        videoTimerTextView.setTextColor(color);
    }

    /**
     * Get the current text size of video record duration text
     *
     * @return float value representing text size
     */
    @Dimension
    public float getVideoTimerTextSize() {
        return videoTimerTextView.getTextSize();
    }

    /**
     * Set the text size of video record duration text
     *
     * @param textSize float value
     */
    public void setVideoTimerTextSize(@Dimension float textSize) {
        videoTimerTextView.setTextSize(textSize);
    }

    /**
     * Set the text appearance for video record duration text
     *
     * @param textAppearance to be used
     */
    public void setTimerTextAppearance(@StyleRes int textAppearance) {
        videoTimerTextView.setTextAppearance(getContext(), textAppearance);
    }
    //endregion

    /**
     * Enum indicating storage error state
     */
    public enum StorageIconState {
        /**
         * The storage is slow
         */
        SLOW,

        /**
         * The storage is full
         */
        FULL,

        /**
         * The storage is not inserted
         */
        NOT_INSERTED
    }
}
