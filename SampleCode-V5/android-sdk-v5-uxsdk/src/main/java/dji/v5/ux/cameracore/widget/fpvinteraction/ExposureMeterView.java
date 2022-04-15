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
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.v5.ux.R;
import dji.v5.ux.core.util.SettingDefinitions.ControlMode;

/**
 * Displays a metering target on the screen.
 */
public class ExposureMeterView extends FrameLayout {

    //region Fields
    /**
     * The default x scaling for the center meter icon.
     */
    protected static final float DEFAULT_CENTER_METER_SCALE_X = 1.376f;
    /**
     * The default y scaling for the center meter icon.
     */
    protected static final float DEFAULT_CENTER_METER_SCALE_Y = 1f;

    private static final int CENTER_WAIT_DURATION = 900;
    private static final float RATIO_X_LEFT = 0.75f;
    private static final float RATIO_X_RIGHT = 1.25f;
    private static final float RATIO_Y_TOP = -0.25f;
    private static final float RATIO_Y_BOTTOM = 0.25f;
    private float lastX = -1, lastY = -1;
    private Drawable centerMeterIcon;
    private Drawable spotMeterIcon;
    private float centerMeterScaleX;
    private float centerMeterScaleY;
    private ControlMode controlMode;
    //endregion

    //region Constructor
    public ExposureMeterView(@NonNull Context context) {
        super(context);
        initView(context);
    }

    public ExposureMeterView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ExposureMeterView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public void initView(Context context) {
        centerMeterIcon = context.getResources().getDrawable(R.drawable.uxsdk_ic_center_metering_target);
        spotMeterIcon = context.getResources().getDrawable(R.drawable.uxsdk_ic_spot_metering_target);
        centerMeterScaleX = DEFAULT_CENTER_METER_SCALE_X;
        centerMeterScaleY = DEFAULT_CENTER_METER_SCALE_Y;
    }
    //endregion

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
                // Toggle to spot meter
                lastX = -1;
                lastY = -1;
                controlMode = ControlMode.SPOT_METER;
                /* fall through, no break */
            case SPOT_METER:
                float xOffset = (x - lastX) / getWidth();
                float yOffset = (y - lastY) / getHeight();
                if (xOffset >= RATIO_X_LEFT && xOffset <= RATIO_X_RIGHT &&
                        yOffset >= RATIO_Y_TOP && yOffset <= RATIO_Y_BOTTOM) {
                    controlMode = ControlMode.CENTER_METER;
                    if (getHandler() != null) {
                        getHandler().postDelayed(() -> {
                            lastX = -1;
                            lastY = -1;
                            removeImageBackground();
                        }, CENTER_WAIT_DURATION);
                    }
                    x = parentWidth / 2;
                    y = parentHeight / 2;
                }
                this.controlMode = controlMode;
                addImageBackground(controlMode);
                x -= getWidth() / 2f;
                y -= getHeight() / 2f;
                setTranslationX(x);
                setTranslationY(y);
                lastX = x;
                lastY = y;
                break;
            default:
                break;
        }
        return controlMode;
    }

    private void addImageBackground(ControlMode controlMode) {
        if (controlMode == ControlMode.CENTER_METER) {
            setBackground(centerMeterIcon);
            setScaleX(centerMeterScaleX);
            setScaleY(centerMeterScaleY);
        } else {
            setBackground(spotMeterIcon);
            setScaleX(1f);
            setScaleY(1f);
        }
    }

    /**
     * Clears the meter icon.
     */
    public void removeImageBackground() {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            setBackgroundDrawable(null);
        } else {
            setBackground(null);
        }
    }

    //region Customization

    /**
     * Get the drawable resource for the center meter icon.
     *
     * @return The drawable resource for the icon.
     */
    @Nullable
    public Drawable getCenterMeterIcon() {
        return centerMeterIcon;
    }

    /**
     * Set the drawable resource for the center meter icon.
     *
     * @param centerMeterIcon The drawable resource for the icon.
     */
    public void setCenterMeterIcon(@Nullable Drawable centerMeterIcon) {
        this.centerMeterIcon = centerMeterIcon;
        if (this.controlMode == ControlMode.CENTER_METER && getBackground() != null) {
            addImageBackground(controlMode);
        }
    }

    /**
     * Get the drawable resource for the spot meter icon.
     *
     * @return The drawable resource for the icon.
     */
    @Nullable
    public Drawable getSpotMeterIcon() {
        return spotMeterIcon;
    }

    /**
     * Set the drawable resource for the spot meter icon.
     *
     * @param spotMeterIcon The drawable resource for the icon.
     */
    public void setSpotMeterIcon(@Nullable Drawable spotMeterIcon) {
        this.spotMeterIcon = spotMeterIcon;
        if (this.controlMode == ControlMode.SPOT_METER && getBackground() != null) {
            addImageBackground(controlMode);
        }
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
        if (this.controlMode == ControlMode.CENTER_METER && getBackground() != null) {
            addImageBackground(controlMode);
        }
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
        if (this.controlMode == ControlMode.CENTER_METER && getBackground() != null) {
            addImageBackground(controlMode);
        }
    }
    //endregion
}