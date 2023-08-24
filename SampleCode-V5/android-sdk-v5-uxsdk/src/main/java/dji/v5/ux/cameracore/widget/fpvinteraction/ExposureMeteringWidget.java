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

package dji.v5.ux.cameracore.widget.fpvinteraction;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import dji.sdk.keyvalue.value.camera.CameraExposureCompensation;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.ui.VerticalSeekBar;
import dji.v5.ux.core.ui.exposure.ExposeVSeekBar;
import dji.v5.ux.core.util.SettingDefinitions.ControlMode;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.kotlin.Flowables;
import kotlin.collections.CollectionsKt;

/**
 * Displays a metering target on the screen.
 */
public class ExposureMeteringWidget extends ConstraintLayoutWidget<Object> implements ICameraIndex {

    //region Fields
    /**
     * The default x scaling for the center meter icon.
     */
    protected static final float DEFAULT_CENTER_METER_SCALE_X = 1.376f;
    /**
     * The default y scaling for the center meter icon.
     */
    protected static final float DEFAULT_CENTER_METER_SCALE_Y = 1f;

    private float centerMeterScaleX = DEFAULT_CENTER_METER_SCALE_X;
    private float centerMeterScaleY = DEFAULT_CENTER_METER_SCALE_Y;
    private ExposureMeteringWidgetModel widgetModel;
    private ExposeVSeekBar mExposeVSeekBar;

    public ExposureMeteringWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ExposureMeteringWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ExposureMeteringWidget(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_expose_focus_view, this);

        if (!isInEditMode()) {
            widgetModel = new ExposureMeteringWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());
        }
        initSeekBar();
    }

    @Override
    protected void reactToModelChanges() {
        Disposable d = Flowables.INSTANCE.combineLatest(
                        widgetModel.exposureCompensationProcessor.toFlowable(),
                        widgetModel.compensationRangeProcessor.toFlowable())
                .observeOn(SchedulerProvider.ui()).subscribe(cameraExposureCompensationListPair -> {
                    // 获取EV值列表 + 当前EV值
                    List<CameraExposureCompensation> evList = cameraExposureCompensationListPair.component2();
                    CameraExposureCompensation ev = cameraExposureCompensationListPair.component1();
                    // 判断EV值列表是否有效
                    if (evList == null || evList.size() <= 0) {
                        return;
                    }
                    // 判断当前EV值是否有效
                    int index = CollectionsKt.indexOf(evList, ev);
                    if (index < 0) {
                        return;
                    }
                    // 更新SeekBar UI
                    mExposeVSeekBar.setMax(evList.size() - 1);
                    mExposeVSeekBar.setProgress(index);
                }, throwable -> {
                    // do nothing
                });
        addReaction(d);
    }

    @Nullable
    @Override
    public String getIdealDimensionRatioString() {
        return null;
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


    public void initSeekBar() {
        mExposeVSeekBar = findViewById(R.id.expose_level_seekbar);
        mExposeVSeekBar.setMax(100);
        mExposeVSeekBar.setShowSeekBar(true);
        mExposeVSeekBar.setOnChangeListener(new VerticalSeekBar.OnVSBChangeListener() {
            @Override
            public void onProgressChanged(VerticalSeekBar bar, int value, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                invalidate();
                addDisposable(widgetModel.setEV(value)
                        .observeOn(SchedulerProvider.ui())
                        .subscribe(() -> {
                            // do nothing
                        }));
            }

            @Override
            public void onStartTrackingTouch(VerticalSeekBar bar) {
                showProgressBar();
            }

            @Override
            public void onStopTrackingTouch(VerticalSeekBar bar) {
                hideProgressBar();
            }
        });
    }

    private void showProgressBar() {
        invalidate();
    }

    private void hideProgressBar() {
        invalidate();
    }

    /**
     * Calculate the next ControlMode and show the icon.
     *
     * @param controlMode  The current ControlMode.
     * @param x            position of click.
     * @param y            position of click.
     * @param parentWidth  The width of the parent view.
     * @param parentHeight The height of the parent view.
     * @return The next ControlMode.
     */
    public ControlMode clickEvent(@NonNull ControlMode controlMode, float x, float y,
                                  float parentWidth, float parentHeight) {
        switch (controlMode) {
            case CENTER_METER:
            case SPOT_METER:
                addImageBackground();
                x -= getWidth() / 2f;
                y -= getHeight() / 2f;
                setX(x);
                setY(y);
                break;
            default:
                break;
        }
        return ControlMode.SPOT_METER;
    }

    private void addImageBackground() {
        setScaleX(1f);
        setScaleY(1f);
    }

    /**
     * Clears the meter icon.
     */
    public void removeImageBackground() {
        this.setBackground(null);
    }

    /**
     * Gets the scaleX of the center meter icon compared to the spot meter icon. For example, if
     * center meter icon is twice as wide as the spot meter icon, the scaleX would be 2.
     *
     * @return The scaleX of the center meter icon
     */
    public float getCenterMeterScaleX() {
        return centerMeterScaleX;
    }

    /**
     * Sets the scaleX of the center meter icon compared to the spot meter icon. For example, if
     * center meter icon is twice as wide as the spot meter icon, the scaleX would be 2.
     *
     * @param centerMeterScaleX The scaleX of the center meter icon
     */
    public void setCenterMeterScaleX(float centerMeterScaleX) {
        this.centerMeterScaleX = centerMeterScaleX;
    }

    /**
     * Gets the scaleY of the center meter icon compared to the spot meter icon. For example, if
     * center meter icon is twice as tall as the spot meter icon, the scaleY would be 2.
     *
     * @return The scaleY of the center meter icon
     */
    public float getCenterMeterScaleY() {
        return centerMeterScaleY;
    }

    /**
     * Sets the scaleY of the center meter icon compared to the spot meter icon. For example, if
     * center meter icon is twice as tall as the spot meter icon, the scaleY would be 2.
     *
     * @param centerMeterScaleY The scaleY of the center meter icon
     */
    public void setCenterMeterScaleY(float centerMeterScaleY) {
        this.centerMeterScaleY = centerMeterScaleY;
    }

    @NonNull
    @Override
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
}