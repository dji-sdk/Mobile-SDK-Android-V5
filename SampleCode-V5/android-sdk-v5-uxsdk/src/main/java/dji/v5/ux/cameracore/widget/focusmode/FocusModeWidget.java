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

package dji.v5.ux.cameracore.widget.focusmode;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.sdk.keyvalue.value.camera.CameraFocusMode;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.ux.core.base.widget.FrameLayoutWidget;
import dji.v5.ux.core.communication.GlobalPreferencesManager;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DisplayUtil;
import dji.v5.ux.core.util.RxUtil;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Widget will display the current focus mode of aircraft camera.
 * - MF text highlighted (in green) indicates focus mode is Manual Focus.
 * - AF text highlighted (in green) indicates focus mode is Auto Focus.
 * - AFC text highlighted (in green) indicates focus mode is Auto Focus Continuous.
 * <p>
 * Interaction:
 * Tapping will toggle between AF and MF mode.
 */
public class FocusModeWidget extends FrameLayoutWidget implements OnClickListener, ICameraIndex {

    //region constants
    private static final String TAG = "FocusModeWidget";
    //endregion

    //region Fields
    private FocusModeWidgetModel widgetModel;
    private TextView titleTextView;
    private int activeColor;
    private int inactiveColor;
    //endregion

    //region Lifecycle
    public FocusModeWidget(Context context) {
        super(context);
    }

    public FocusModeWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FocusModeWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_focus_mode_switch, this);
        if (getBackground() == null) {
            setBackgroundResource(R.drawable.uxsdk_background_black_rectangle);
        }
        titleTextView = findViewById(R.id.text_view_camera_control_af);
        if (!isInEditMode()) {
            widgetModel = new FocusModeWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance(),
                    GlobalPreferencesManager.getInstance());
        }
        setOnClickListener(this);
        activeColor = getResources().getColor(R.color.uxsdk_green);
        inactiveColor = getResources().getColor(R.color.uxsdk_white);
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
        addReaction(widgetModel.isFocusModeChangeSupported()
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateVisibility));
        addReaction(reactToFocusModeChange());
    }


    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_default_ratio);
    }

    @Override
    public void onClick(View v) {
        addDisposable(widgetModel.toggleFocusMode()
                .observeOn(SchedulerProvider.ui())
                .subscribe(() -> {
                    // Do nothing
                }, RxUtil.logErrorConsumer(TAG, "switch focus mode: ")));
    }

    //endregion

    //region private helpers

    private void checkAndUpdateUI() {
        if (!isInEditMode()) {
            addDisposable(Flowable.combineLatest(widgetModel.isAFCEnabled(), widgetModel.getFocusMode(), Pair::new)
                    .firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(values -> updateUI(values.first, values.second),
                            RxUtil.logErrorConsumer(TAG, "check and update focus mode: ")));
        }
    }

    private Disposable reactToFocusModeChange() {
        return Flowable.combineLatest(widgetModel.isAFCEnabled(), widgetModel.getFocusMode(), Pair::new)
                .observeOn(SchedulerProvider.ui())
                .subscribe(values -> updateUI(values.first, values.second),
                        RxUtil.logErrorConsumer(TAG, "react to Focus Mode Change: "));
    }

    private void updateVisibility(boolean isFocusModeChangeSupported) {
        if (isFocusModeChangeSupported) {
            setVisibility(VISIBLE);
        } else {
            setVisibility(GONE);
        }
    }

    private void updateUI(boolean isAFCEnabled, CameraFocusMode focusMode) {
        int autoFocusTextColor;
        int manualFocusTextColor;
        if (focusMode == CameraFocusMode.MANUAL) {
            manualFocusTextColor = activeColor;
            autoFocusTextColor = inactiveColor;
        } else {
            autoFocusTextColor = activeColor;
            manualFocusTextColor = inactiveColor;
        }

        String autoFocusText;
        if (isAFCEnabled) {
            autoFocusText = getResources().getString(R.string.uxsdk_widget_focus_mode_afc);
        } else {
            autoFocusText = getResources().getString(R.string.uxsdk_widget_focus_mode_auto);

        }

        makeSpannableString(autoFocusText, autoFocusTextColor, manualFocusTextColor);
    }

    private void makeSpannableString(String autoFocusText, int autoFocusColor, int manualFocusColor) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        SpannableString str1 = new SpannableString(autoFocusText);
        str1.setSpan(new ForegroundColorSpan(autoFocusColor), 0, str1.length(), 0);
        builder.append(str1);

        SpannableString str2 = new SpannableString(getResources().getString(R.string.uxsdk_widget_focus_mode_separator));
        str2.setSpan(new ForegroundColorSpan(inactiveColor), 0, str2.length(), 0);
        builder.append(str2);

        SpannableString str3 = new SpannableString(getResources().getString(R.string.uxsdk_widget_focus_mode_manual));
        str3.setSpan(new ForegroundColorSpan(manualFocusColor), 0, str3.length(), 0);
        builder.append(str3);
        titleTextView.setText(builder, TextView.BufferType.SPANNABLE);
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FocusModeWidget);
        if (!isInEditMode()) {
            updateCameraSource(ComponentIndexType.find(typedArray.getInt(R.styleable.FocusModeWidget_uxsdk_cameraIndex, 0)),
                    CameraLensType.find(typedArray.getInt(R.styleable.FocusModeWidget_uxsdk_lensType, 0)));
        }
        activeColor = typedArray.getColor(R.styleable.FocusModeWidget_uxsdk_activeModeTextColor, getResources().getColor(R.color.uxsdk_green));
        inactiveColor = typedArray.getColor(R.styleable.FocusModeWidget_uxsdk_inactiveModeTextColor, getResources().getColor(R.color.uxsdk_white));
        Drawable background = typedArray.getDrawable(R.styleable.FocusModeWidget_uxsdk_widgetTitleBackground);
        setTitleBackground(background);
        float textSize =
                typedArray.getDimension(R.styleable.FocusModeWidget_uxsdk_widgetTitleTextSize, INVALID_RESOURCE);
        if (textSize != INVALID_RESOURCE) {
            setTitleTextSize(DisplayUtil.pxToSp(context, textSize));
        }
        typedArray.recycle();
    }
    //endregion

    //region customizations

    /**
     * Get the index of the camera to which the widget is reacting
     *
     * @return {@link ComponentIndexType}
     */
    @NonNull
    public ComponentIndexType getCameraIndex() {
        return widgetModel.getCameraIndex();
    }

    /**
     * Get the current type of the lens the widget is reacting to
     *
     * @return current lens type
     */
    @NonNull
    public CameraLensType getLensType() {
        return widgetModel.getLensType();
    }

    @Override
    public void updateCameraSource(@NonNull ComponentIndexType cameraIndex, @NonNull CameraLensType lensType) {
        widgetModel.updateCameraSource(cameraIndex, lensType);
    }

    /**
     * Get active mode text color
     *
     * @return color integer
     */
    @ColorInt
    public int getActiveModeTextColor() {
        return activeColor;
    }

    /**
     * Set active mode text color
     *
     * @param color color integer
     */
    public void setActiveModeTextColor(@ColorInt int color) {
        activeColor = color;
        checkAndUpdateUI();
    }

    /**
     * Get in-active mode text color
     *
     * @return color integer
     */
    @ColorInt
    public int getInactiveModeTextColor() {
        return inactiveColor;
    }

    /**
     * Set in-active mode text color
     *
     * @param color color integer
     */
    public void setInactiveModeTextColor(@ColorInt int color) {
        inactiveColor = color;
        checkAndUpdateUI();
    }

    /**
     * Get current background of title text
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getTitleBackground() {
        return titleTextView.getBackground();
    }

    /**
     * Set background to title text
     *
     * @param resourceId resource id of background
     */
    public void setTitleBackground(@DrawableRes int resourceId) {
        setTitleBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set background to title text
     *
     * @param drawable Drawable to be used as background
     */
    public void setTitleBackground(@Nullable Drawable drawable) {
        titleTextView.setBackground(drawable);
    }

    /**
     * Get current text size
     *
     * @return text size of the title
     */
    @Dimension
    public float getTitleTextSize() {
        return titleTextView.getTextSize();
    }

    /**
     * Sets the text size of the widget text
     *
     * @param textSize text size float value
     */
    public void setTitleTextSize(@Dimension float textSize) {
        titleTextView.setTextSize(textSize);
    }

    //endregion
}
