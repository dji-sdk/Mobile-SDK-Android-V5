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

package dji.v5.ux.core.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import dji.v5.ux.R;

/**
 * Displays a customized seek bar that displays a value positioned above the thumb image. There are
 * also options for displaying either minimum and maximum values or plus and minus buttons to the
 * start and end of the seek bar, as well as a bar showing a baseline as a recommended value.
 */
public class HorizontalSeekBar extends ConstraintLayout implements View.OnTouchListener, View.OnClickListener {

    //region Properties
    private ImageView seekBarTrackImage;
    private TextView seekBarValueText;
    private List<OnSeekBarChangeListener> onSeekBarChangeListeners;
    private int progressMax;
    private int currentProgress;
    // Null status will be used
    private int previousProgress;

    private float boundaryLeft;
    private float boundaryRight;
    private float xThumbStartCenter;
    private float xMoveStart;
    private ImageView seekBarThumbImage;

    //Min and Max
    private boolean minValueVisibility = false;
    private boolean maxValueVisibility = false;
    private TextView seekBarMinText;
    private TextView seekBarMaxText;

    //Plus and minus
    private boolean minusVisibility = false;
    private boolean plusVisibility = false;
    private ImageView seekBarMinus;
    private ImageView seekBarPlus;

    // Baseline
    private boolean baselineVisibility = false;
    private int baselineProgress = -1;
    private View seekBarBaseline;
    private int seekBarBaselineColor;
    //endregion

    //region Life Cycle
    public HorizontalSeekBar(@NonNull Context context) {
        super(context);
        initView(context);
    }

    public HorizontalSeekBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
        initAttributes(context, attrs);
    }

    public HorizontalSeekBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
        initAttributes(context, attrs);
    }

    public void initView(@NonNull Context context) {
        inflate(context, R.layout.uxsdk_view_seek_bar, this);
        seekBarTrackImage = findViewById(R.id.imageview_track);
        seekBarThumbImage = findViewById(R.id.imageview_thumb);
        seekBarValueText = findViewById(R.id.textview_value);
        seekBarMinText = findViewById(R.id.textview_min_value);
        seekBarMaxText = findViewById(R.id.textview_max_value);
        seekBarBaseline = findViewById(R.id.imageview_baseline);
        seekBarMinus = findViewById(R.id.imageview_minus);
        seekBarPlus = findViewById(R.id.imageview_plus);

        seekBarBaselineColor = getResources().getColor(R.color.uxsdk_green);
    }

    private void initAttributes(@NonNull Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SeekBarView);

        setMinValueVisibility(typedArray.getBoolean(R.styleable.SeekBarView_uxsdk_minValueVisible, false));
        setMaxValueVisibility(typedArray.getBoolean(R.styleable.SeekBarView_uxsdk_maxValueVisible, false));
        setMinusVisibility(typedArray.getBoolean(R.styleable.SeekBarView_uxsdk_minusVisible, false));
        setPlusVisibility(typedArray.getBoolean(R.styleable.SeekBarView_uxsdk_plusVisible, false));
        setBaselineVisibility(typedArray.getBoolean(R.styleable.SeekBarView_uxsdk_baselineVisible, false));

        typedArray.recycle();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        boundaryLeft = seekBarTrackImage.getLeft();
        boundaryRight = seekBarTrackImage.getRight();
        updateTextAndThumbInProgress(currentProgress);
    }
    //endregion

    // region Public Methods

    /**
     * Get the maximum value
     *
     * @return the maximum value
     */
    public int getMax() {
        synchronized (this) {
            return progressMax;
        }
    }

    /**
     * Set the maximum value
     *
     * @param max the maximum value
     */
    public void setMax(@IntRange(from = 0) int max) {
        synchronized (this) {
            progressMax = max;
        }
    }

    /**
     * Set whether the seek bar is enabled
     *
     * @param status True to enable, false to disable
     */
    public void enable(boolean status) {
        seekBarValueText.setEnabled(status);
        //seekBarValueText.requestLayout();
        seekBarThumbImage.setEnabled(status);
        //seekBarThumbImage.requestLayout();

        if (status) {
            setOnTouchListener(this);
            seekBarMinus.setOnClickListener(this);
            seekBarPlus.setOnClickListener(this);
        } else {
            setOnTouchListener(null);
            seekBarMinus.setOnClickListener(null);
            seekBarPlus.setOnClickListener(null);
        }
    }

    /**
     * Add a listener to react when the seek bar has changed.
     *
     * @param listener The listener to add.
     */
    public void addOnSeekBarChangeListener(@NonNull OnSeekBarChangeListener listener) {
        if (onSeekBarChangeListeners == null) {
            onSeekBarChangeListeners = new LinkedList<>();
        }
        onSeekBarChangeListeners.add(listener);
    }

    /**
     * Remove a listener from the seek bar.
     *
     * @param listener The listener to remove.
     */
    public void removeOnSeekBarChangeListener(@NonNull OnSeekBarChangeListener listener) {
        if (onSeekBarChangeListeners != null) {
            onSeekBarChangeListeners.remove(listener);
        }
    }

    /**
     * Remove all listeners from the seek bar.
     */
    public void removeAllOnSeekBarChangeListeners() {
        if (onSeekBarChangeListeners != null) {
            onSeekBarChangeListeners.clear();
        }
    }

    /**
     * Set the text above the seek bar progress indicator
     *
     * @param text The text to display above the progress indicator
     */
    public void setText(@Nullable String text) {
        seekBarValueText.setText(text);
    }

    /**
     * Get the text above the seek bar progress indicator
     */
    public String getText() {
        return seekBarValueText.getText().toString();
    }

    /**
     * Set the minimum value text
     *
     * @param text The minimum value text
     */
    public void setMinValueText(@Nullable String text) {
        seekBarMinText.setText(text);
    }

    /**
     * Set the maximum value text
     *
     * @param text The maximum value text
     */
    public void setMaxValueText(@Nullable String text) {
        seekBarMaxText.setText(text);
    }

    /**
     * Get the progress of the seek bar. This will be in the range 0..max where max was set by
     * {@link #setMax(int)}.
     *
     * @return The progress of the seek bar
     */
    public int getProgress() {
        return currentProgress;
    }

    /**
     * Set the progress of the seek bar. This should be in the range 0..max where max was set by
     * {@link #setMax(int)}.
     *
     * @param progress The progress to set the seek bar to
     */
    public void setProgress(@IntRange(from = 0) int progress) {
        updateSeekBarProgress(progress, false);
    }

    /**
     * Set the progress of the seek bar to the value it was at before it was interacted with.
     */
    public void restorePreviousProgress() {
        setProgress(previousProgress);
        updateTextAndThumbInProgress(previousProgress);
    }
    //endregion

    //region Seek Bar Internal Methods

    private void updateSeekBarProgress(int progress, boolean isFromUI) {
        synchronized (this) {
            if (progress >= progressMax) {
                currentProgress = progressMax;
            } else if (progress < 0) {
                currentProgress = 0;
            } else {
                currentProgress = progress;
            }

            if (onSeekBarChangeListeners != null) {
                for (int i = 0; i < onSeekBarChangeListeners.size(); i++) {
                    onSeekBarChangeListeners.get(i).onProgressChanged(this, currentProgress, isFromUI);
                }
            }
            updateTextAndThumbInProgress(currentProgress);
        }
    }

    private void updateTextAndThumbInProgress(int progress) {
        float newX = boundaryLeft + getIncrement() * progress;
        updateTextAndThumbPosition(newX);
    }

    private void updateTextAndThumbPosition(float newX) {
        float xPosition;
        if (newX < boundaryLeft) {
            xPosition = boundaryLeft;
        } else if (newX > boundaryRight) {
            xPosition = boundaryRight;
        } else {
            xPosition = newX;
        }

        setSeekbarTextPosition(xPosition);
        setSeekbarThumbPosition(xPosition);
    }

    private void setSeekbarTextPosition(float newX) {
        seekBarValueText.setX(newX - seekBarValueText.getWidth() / 2f);
    }

    private void setSeekbarThumbPosition(float newX) {
        seekBarThumbImage.setX(newX - seekBarThumbImage.getWidth() / 2f);
    }

    /**
     * Make the unit smaller to make the room for the last item.
     */
    private float getIncrement() {
        return (boundaryRight - boundaryLeft) / progressMax;
    }

    //endregion

    //region OnTouchListener
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                onStartTracking(event);
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                onEndTracking();
                break;

            case MotionEvent.ACTION_MOVE:
                // perform scrolling
                onTrackMoving(event);
                break;

            default:
                break;

        }

        return true;
    }

    private void onStartTracking(@NonNull MotionEvent event) {
        xMoveStart = event.getX();
        xThumbStartCenter = seekBarThumbImage.getX() + seekBarThumbImage.getWidth() / 2f;

        previousProgress = currentProgress;
        if (onSeekBarChangeListeners != null) {
            for (int i = 0; i < onSeekBarChangeListeners.size(); i++) {
                onSeekBarChangeListeners.get(i).onStartTrackingTouch(this, currentProgress);
            }
        }
    }
    //endregion

    //region OnDragListener
    private void onTrackMoving(@NonNull MotionEvent event) {
        float xDelta = event.getX() - xMoveStart;
        float newX = xThumbStartCenter + xDelta;
        updateSeekBarProgress((int) ((newX - boundaryLeft) / getIncrement()), true);
    }

    private void onEndTracking() {
        if (onSeekBarChangeListeners != null) {
            for (int i = 0; i < onSeekBarChangeListeners.size(); i++) {
                onSeekBarChangeListeners.get(i).onStopTrackingTouch(this, currentProgress);
            }
        }
    }
    //endregion

    //region OnClickListener
    @Override
    public void onClick(View v) {
        if (onSeekBarChangeListeners != null) {
            if (v.equals(seekBarMinus)) {
                for (int i = 0; i < onSeekBarChangeListeners.size(); i++) {
                    onSeekBarChangeListeners.get(i).onMinusClicked(this);
                }
            } else if (v.equals(seekBarPlus)) {
                for (int i = 0; i < onSeekBarChangeListeners.size(); i++) {
                    onSeekBarChangeListeners.get(i).onPlusClicked(this);
                }
            }
        }
    }
    //endregion

    /**
     * Get the visibility of the minimum value
     *
     * @return True if the minimum value is visible, false otherwise.
     */
    public boolean isMinValueVisible() {
        return minValueVisibility;
    }

    /**
     * Set the visibility of the minimum value
     *
     * @param minValueVisibility True to show the minimum value, false to hide it.
     */
    public void setMinValueVisibility(boolean minValueVisibility) {
        this.minValueVisibility = minValueVisibility;
        seekBarMinText.setVisibility(minValueVisibility ? VISIBLE : GONE);
    }

    /**
     * Get the visibility of the maximum value
     *
     * @return True if the maximum value is visible, false otherwise.
     */
    public boolean isMaxValueVisible() {
        return maxValueVisibility;
    }

    /**
     * Set the visibility of the maximum value
     *
     * @param maxValueVisibility True to show the maximum value, false to hide it.
     */
    public void setMaxValueVisibility(boolean maxValueVisibility) {
        this.maxValueVisibility = maxValueVisibility;
        seekBarMaxText.setVisibility(maxValueVisibility ? VISIBLE : GONE);
    }

    /**
     * Get the visibility of the minus button
     *
     * @return True if the minus button is visible, false otherwise.
     */
    public boolean isMinusVisible() {
        return minusVisibility;
    }

    /**
     * Set the visibility of the minus button
     *
     * @param minusVisibility True to show the minus button, false to hide it.
     */
    public void setMinusVisibility(boolean minusVisibility) {
        this.minusVisibility = minusVisibility;
        seekBarMinus.setVisibility(minusVisibility ? VISIBLE : GONE);
    }

    /**
     * Get the visibility of the plus button
     *
     * @return True if the plus button is visible, false otherwise.
     */
    public boolean isPlusVisible() {
        return plusVisibility;
    }

    /**
     * Set the visibility of the plus button
     *
     * @param plusVisibility True to show the plus button, false to hide it.
     */
    public void setPlusVisibility(boolean plusVisibility) {
        this.plusVisibility = plusVisibility;
        seekBarPlus.setVisibility(plusVisibility ? VISIBLE : GONE);
    }

    /**
     * Get the visibility of the baseline
     *
     * @return True if the baseline is visible, false otherwise.
     */
    public boolean isBaselineVisibility() {
        return baselineVisibility;
    }

    /**
     * Set the visibility of the baseline
     *
     * @param baselineVisibility True to show the baseline, false to hide it.
     */
    public void setBaselineVisibility(boolean baselineVisibility) {
        this.baselineVisibility = baselineVisibility;
        seekBarBaseline.setVisibility(baselineVisibility ? VISIBLE : GONE);
    }

    /**
     * Get the progress of the baseline
     *
     * @return The progress of the baseline
     */
    public int getBaselineProgress() {
        return baselineProgress;
    }

    /**
     * Set the baseline progress
     *
     * @param baselineProgress The progress to set the baseline to
     */
    public void setBaselineProgress(int baselineProgress) {
        this.baselineProgress = baselineProgress;
        float newX = boundaryLeft + getIncrement() * baselineProgress;
        seekBarBaseline.setX(newX - seekBarBaseline.getWidth() / 2f);
    }

    /**
     * Get the drawable resource for the track icon
     *
     * @return Drawable resource for the icon
     */
    @Nullable
    public Drawable getTrackIcon() {
        return seekBarTrackImage.getDrawable();
    }

    /**
     * Set the resource ID for the track icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setTrackIcon(@DrawableRes int resourceId) {
        setTrackIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the drawable resource for the track icon
     *
     * @param icon Drawable resource for the icon
     */
    public void setTrackIcon(@Nullable Drawable icon) {
        seekBarTrackImage.setImageDrawable(icon);
    }

    /**
     * Get the drawable resource for the track icon's background
     *
     * @return Drawable resource for the icon's background
     */
    @Nullable
    public Drawable getTrackIconBackground() {
        return seekBarTrackImage.getBackground();
    }

    /**
     * Set the resource ID for the track icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setTrackIconBackground(@DrawableRes int resourceId) {
        seekBarTrackImage.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the track icon's background
     *
     * @param icon Drawable resource for the icon's background
     */
    public void setTrackIconBackground(@Nullable Drawable icon) {
        seekBarTrackImage.setBackground(icon);
    }

    /**
     * Get the drawable resource for the thumb icon
     *
     * @return Drawable resource for the icon
     */
    @Nullable
    public Drawable getThumbIcon() {
        return seekBarThumbImage.getDrawable();
    }

    /**
     * Set the resource ID for the thumb icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setThumbIcon(@DrawableRes int resourceId) {
        setThumbIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the drawable resource for the thumb icon
     *
     * @param icon Drawable resource for the icon
     */
    public void setThumbIcon(@Nullable Drawable icon) {
        seekBarThumbImage.setImageDrawable(icon);
    }

    /**
     * Get the drawable resource for the thumb icon's background
     *
     * @return Drawable resource for the icon's background
     */
    @Nullable
    public Drawable getThumbIconBackground() {
        return seekBarThumbImage.getBackground();
    }

    /**
     * Set the resource ID for the thumb icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setThumbIconBackground(@DrawableRes int resourceId) {
        seekBarThumbImage.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the thumb icon's background
     *
     * @param icon Drawable resource for the icon's background
     */
    public void setThumbIconBackground(@Nullable Drawable icon) {
        seekBarThumbImage.setBackground(icon);
    }

    /**
     * Get the drawable resource for the minus icon
     *
     * @return Drawable resource for the icon
     */
    @Nullable
    public Drawable getMinusIcon() {
        return seekBarMinus.getDrawable();
    }

    /**
     * Set the resource ID for the minus icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setMinusIcon(@DrawableRes int resourceId) {
        setMinusIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the drawable resource for the minus icon
     *
     * @param icon Drawable resource for the icon
     */
    public void setMinusIcon(@Nullable Drawable icon) {
        seekBarMinus.setImageDrawable(icon);
    }

    /**
     * Get the drawable resource for the minus icon's background
     *
     * @return Drawable resource for the icon's background
     */
    @Nullable
    public Drawable getMinusIconBackground() {
        return seekBarMinus.getBackground();
    }

    /**
     * Set the resource ID for the minus icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setMinusIconBackground(@DrawableRes int resourceId) {
        seekBarMinus.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the minus icon's background
     *
     * @param icon Drawable resource for the icon's background
     */
    public void setMinusIconBackground(@Nullable Drawable icon) {
        seekBarMinus.setBackground(icon);
    }

    /**
     * Get the drawable resource for the plus icon
     *
     * @return Drawable resource for the icon
     */
    @Nullable
    public Drawable getPlusIcon() {
        return seekBarPlus.getDrawable();
    }

    /**
     * Set the resource ID for the plus icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    public void setPlusIcon(@DrawableRes int resourceId) {
        setPlusIcon(getResources().getDrawable(resourceId));
    }

    /**
     * Set the drawable resource for the plus icon
     *
     * @param icon Drawable resource for the icon
     */
    public void setPlusIcon(@Nullable Drawable icon) {
        seekBarPlus.setImageDrawable(icon);
    }

    /**
     * Get the drawable resource for the plus icon's background
     *
     * @return Drawable resource for the icon's background
     */
    @Nullable
    public Drawable getPlusIconBackground() {
        return seekBarPlus.getBackground();
    }

    /**
     * Set the resource ID for the plus icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    public void setPlusIconBackground(@DrawableRes int resourceId) {
        seekBarPlus.setBackgroundResource(resourceId);
    }

    /**
     * Set the drawable resource for the plus icon's background
     *
     * @param icon Drawable resource for the icon's background
     */
    public void setPlusIconBackground(@Nullable Drawable icon) {
        seekBarPlus.setBackground(icon);
    }

    /**
     * Set text appearance of the value text view
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    public void setValueTextAppearance(@StyleRes int textAppearanceResId) {
        seekBarValueText.setTextAppearance(getContext(), textAppearanceResId);
    }

    /**
     * Get current text color of the value text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getValueTextColor() {
        return seekBarValueText.getCurrentTextColor();
    }

    /**
     * Set text color for the value text view
     *
     * @param color color integer resource
     */
    public void setValueTextColor(@ColorInt int color) {
        seekBarValueText.setTextColor(color);
    }

    /**
     * Set text color state list for the value text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setValueTextColor(@NonNull ColorStateList colorStateList) {
        seekBarValueText.setTextColor(colorStateList);
    }

    /**
     * Get current color state list of the value text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getValueTextColors() {
        return seekBarValueText.getTextColors();
    }

    /**
     * Get current background of the value text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getValueTextBackground() {
        return seekBarValueText.getBackground();
    }

    /**
     * Set the resource ID for the background of the value text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setValueTextBackground(@DrawableRes int resourceId) {
        seekBarValueText.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the value text view
     *
     * @param background Drawable resource for the background
     */
    public void setValueTextBackground(@Nullable Drawable background) {
        seekBarValueText.setBackground(background);
    }

    /**
     * Get current text size of the value text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getValueTextSize() {
        return seekBarValueText.getTextSize();
    }

    /**
     * Set the text size of the value text view
     *
     * @param textSize text size float value
     */
    public void setValueTextSize(@Dimension float textSize) {
        seekBarValueText.setTextSize(textSize);
    }

    /**
     * Set text appearance of the min value text view
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    public void setMinValueTextAppearance(@StyleRes int textAppearanceResId) {
        seekBarMinText.setTextAppearance(getContext(), textAppearanceResId);
    }

    /**
     * Get current text color of the min value text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getMinValueTextColor() {
        return seekBarMinText.getCurrentTextColor();
    }

    /**
     * Set text color for the min value text view
     *
     * @param color color integer resource
     */
    public void setMinValueTextColor(@ColorInt int color) {
        seekBarMinText.setTextColor(color);
    }

    /**
     * Set text color state list for the min value text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setMinValueTextColor(@NonNull ColorStateList colorStateList) {
        seekBarMinText.setTextColor(colorStateList);
    }

    /**
     * Get current color state list of the min value text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getMinValueTextColors() {
        return seekBarMinText.getTextColors();
    }

    /**
     * Get current background of the min value text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getMinValueTextBackground() {
        return seekBarMinText.getBackground();
    }

    /**
     * Set the resource ID for the background of the min value text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setMinValueTextBackground(@DrawableRes int resourceId) {
        seekBarMinText.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the min value text view
     *
     * @param background Drawable resource for the background
     */
    public void setMinValueTextBackground(@Nullable Drawable background) {
        seekBarMinText.setBackground(background);
    }

    /**
     * Get current text size of the min value text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getMinValueTextSize() {
        return seekBarMinText.getTextSize();
    }

    /**
     * Set the text size of the min value text view
     *
     * @param textSize text size float value
     */
    public void setMinValueTextSize(@Dimension float textSize) {
        seekBarMinText.setTextSize(textSize);
    }

    /**
     * Set text appearance of the max value text view
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    public void setMaxValueTextAppearance(@StyleRes int textAppearanceResId) {
        seekBarMaxText.setTextAppearance(getContext(), textAppearanceResId);
    }

    /**
     * Get current text color of the max value text view
     *
     * @return color integer resource
     */
    @ColorInt
    public int getMaxValueTextColor() {
        return seekBarMaxText.getCurrentTextColor();
    }

    /**
     * Set text color for the max value text view
     *
     * @param color color integer resource
     */
    public void setMaxValueTextColor(@ColorInt int color) {
        seekBarMaxText.setTextColor(color);
    }

    /**
     * Set text color state list for the max value text view
     *
     * @param colorStateList ColorStateList resource
     */
    public void setMaxValueTextColor(@NonNull ColorStateList colorStateList) {
        seekBarMaxText.setTextColor(colorStateList);
    }

    /**
     * Get current color state list of the max value text view
     *
     * @return ColorStateList resource
     */
    @NonNull
    public ColorStateList getMaxValueTextColors() {
        return seekBarMaxText.getTextColors();
    }

    /**
     * Get current background of the max value text view
     *
     * @return Drawable resource of the background
     */
    @Nullable
    public Drawable getMaxValueTextBackground() {
        return seekBarMaxText.getBackground();
    }

    /**
     * Set the resource ID for the background of the max value text view
     *
     * @param resourceId Integer ID of the drawable resource for the background
     */
    public void setMaxValueTextBackground(@DrawableRes int resourceId) {
        seekBarMaxText.setBackgroundResource(resourceId);
    }

    /**
     * Set the background of the max value text view
     *
     * @param background Drawable resource for the background
     */
    public void setMaxValueTextBackground(@Nullable Drawable background) {
        seekBarMaxText.setBackground(background);
    }

    /**
     * Get current text size of the max value text view
     *
     * @return text size of the text view
     */
    @Dimension
    public float getMaxValueTextSize() {
        return seekBarMaxText.getTextSize();
    }

    /**
     * Set the text size of the max value text view
     *
     * @param textSize text size float value
     */
    public void setMaxValueTextSize(@Dimension float textSize) {
        seekBarMaxText.setTextSize(textSize);
    }

    /**
     * Get the color of the baseline
     *
     * @return color integer resource
     */
    @ColorInt
    public int getBaselineColor() {
        return seekBarBaselineColor;
    }

    /**
     * Set the color of the baseline
     *
     * @param color color integer resource
     */
    public void setBaselineColor(@ColorInt int color) {
        seekBarBaselineColor = color;
        seekBarBaseline.setBackgroundColor(color);
    }

    /**
     * A callback that notifies clients when the progress level has been
     * changed. This includes changes that were initiated by the user through a
     * touch gesture or arrow key/trackball as well as changes that were initiated
     * programmatically.
     */
    public interface OnSeekBarChangeListener {

        /**
         * Notification that the progress level has changed. Clients can use the fromUser parameter
         * to distinguish user-initiated changes from those that occurred programmatically.
         *
         * @param object   The SeekBarView whose progress changed.
         * @param progress The current progress level. This will be in the range 0..max where max
         *                 was set by {@link #setMax(int)}.
         */
        void onProgressChanged(@NonNull HorizontalSeekBar object, @IntRange(from = 0) int progress, boolean isFromUI);

        /**
         * Notification that the user has started a touch gesture. Clients may want to use this
         * to disable advancing the seek bar.
         *
         * @param object   The SeekBarView that was touched.
         * @param progress The current progress level. This will be in the range 0..max where max
         *                 was set by {@link #setMax(int)}.
         */
        void onStartTrackingTouch(@NonNull HorizontalSeekBar object, @IntRange(from = 0) int progress);

        /**
         * Notification that the user has finished a touch gesture. Clients may want to use this
         * to re-enable advancing the seek bar.
         *
         * @param object   The SeekBarView that was touched.
         * @param progress The current progress level. This will be in the range 0..max where max
         *                 was set by {@link #setMax(int)}.
         */
        void onStopTrackingTouch(@NonNull HorizontalSeekBar object, @IntRange(from = 0) int progress);

        /**
         * Notification that the user has clicked the plus symbol. Clients should use this
         * to increment the value and update the seek bar.
         *
         * @param object The SeekBarView whose plus symbol was clicked.
         */
        void onPlusClicked(@NonNull HorizontalSeekBar object);

        /**
         * Notification that the user has clicked the minus symbol. Clients should use this
         * to decrement the value and update the seek bar.
         *
         * @param object The SeekBarView whose minus symbol was clicked.
         */
        void onMinusClicked(@NonNull HorizontalSeekBar object);
    }
}
