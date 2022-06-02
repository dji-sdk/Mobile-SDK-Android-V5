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

import android.app.Service;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.v5.ux.R;

/**
 * Displays controls for rotating the gimbal.
 */
public class GimbalControlView extends FrameLayout {

    //region Fields
    /**
     * The default duration for the vibration when the gimbal controls appear.
     */
    protected static final int DEFAULT_VIBRATION_DURATION = 100;

    private ImageView gimbalPoint;
    private ImageView gimbalMove;
    private ImageView gimbalArrow;

    private Vibrator vibrator;
    private int gimbalArrowW = 0;
    private int gimbalArrowH = 0;

    private boolean isVibrationEnabled;
    private int vibrationDuration;
    //endregion

    //region Constructor
    public GimbalControlView(@NonNull Context context) {
        super(context);
        initView(context);
    }

    public GimbalControlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public GimbalControlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public void initView(Context context) {
        inflate(context, R.layout.uxsdk_view_gimbal_control, this);
        gimbalPoint = findViewById(R.id.gimbal_control_point);
        gimbalMove = findViewById(R.id.gimbal_control_move);
        gimbalArrow = findViewById(R.id.gimbal_control_arrow);

        if (!isInEditMode()){
            vibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
        }
        isVibrationEnabled = true;
        vibrationDuration = DEFAULT_VIBRATION_DURATION;
    }
    //endregion

    /**
     * Show the gimbal controls and vibrate.
     *
     * @param x position at which to show the gimbal point.
     * @param y position at which to show the gimbal point.
     */
    public void show(float x, float y) {
        gimbalPoint.setX(x - (gimbalPoint.getWidth() / 2f));
        gimbalPoint.setY(y - (gimbalPoint.getHeight() / 2f));
        gimbalMove.setX(x - (gimbalMove.getWidth() / 2f));
        gimbalMove.setY(y - (gimbalMove.getHeight() / 2f));
        gimbalPoint.setVisibility(VISIBLE);
        gimbalMove.setVisibility(VISIBLE);

        if (gimbalArrowW == 0) {
            gimbalArrowW = gimbalArrow.getWidth();
            gimbalArrowH = gimbalArrow.getHeight();
        }
        vibrator.vibrate(vibrationDuration);
    }

    /**
     * Hide the gimbal controls.
     */
    public void hide() {
        if (isVisible()) {
            gimbalPoint.setVisibility(GONE);
            gimbalMove.setVisibility(GONE);
            gimbalArrow.setVisibility(GONE);
        }
    }

    /**
     * Get whether the gimbal controls are visible.
     *
     * @return `true` if the gimbal controls are visible, `false` otherwise.
     */
    public boolean isVisible() {
        return gimbalPoint.getVisibility() == VISIBLE;
    }

    /**
     * Move the gimbal move icon to a new position and update the arrow icon's location and
     * direction.
     *
     * @param firstX             The location of the gimbal point.
     * @param firstY             The location of the gimbal point.
     * @param x                  The location the gimbal move icon was moved to.
     * @param y                  The location the gimbal move icon was moved to.
     * @param canRotateGimbalYaw Whether the connected gimbal is able to move in the yaw direction.
     */
    public void onMove(float firstX, float firstY, float x, float y, boolean canRotateGimbalYaw) {
        //move images
        if (canRotateGimbalYaw) {
            gimbalMove.setX(x - (gimbalMove.getWidth() / 2f));
        }
        gimbalMove.setY(y - (gimbalMove.getHeight() / 2f));
        transformArrow(firstX, firstY, x, y, canRotateGimbalYaw);
    }

    private void transformArrow(float firstX, float firstY, float x, float y, boolean canRotateGimbalYaw) {
        float offsetX = x - firstX;
        if (!canRotateGimbalYaw) {
            offsetX = 0;
        }
        float offsetY = y - firstY;

        //distance from (firstX,firstY) to (x,y)
        float curDistance = (float) Math.sqrt(offsetX * offsetX + offsetY * offsetY);
        //distance from (firstX,firstY) to arrow
        float dirDistance = (float) Math.pow(curDistance, 0.7);

        if (dirDistance < gimbalArrowW * 0.6f || curDistance < 1f) {
            gimbalArrow.setVisibility(GONE);
        } else {
            gimbalArrow.setVisibility(VISIBLE);

            //offsets from (firstX,firstY) to arrow
            float ratio = dirDistance / curDistance;
            float dirOffsetX = offsetX * ratio;
            float dirOffsetY = offsetY * ratio;

            //set arrow coordinates
            gimbalArrow.setX(firstX + dirOffsetX - gimbalArrowW / 2f);
            gimbalArrow.setY(firstY + dirOffsetY - gimbalArrowH / 2f);

            //set arrow opacity
            float alpha = Math.min(0.7f, dirDistance / gimbalArrowW / 3);
            gimbalArrow.setAlpha(alpha);

            //set arrow rotation
            double angrad = Math.asin(dirOffsetY / dirDistance);
            if (dirOffsetX < 0) {
                angrad = Math.PI - angrad;
            }
            double angDeg = Math.toDegrees(angrad);
            gimbalArrow.setRotation((float) angDeg);

            //set arrow size
            float scale = (float) (1f + Math.min(0.5, dirDistance / gimbalArrowW / 10.0));
            gimbalArrow.setScaleX(scale);
            gimbalArrow.setScaleY(scale);
        }
    }

    //region Customization

    /**
     * Get the drawable resource for the icon that represents the point at which the gimbal
     * started.
     *
     * @return The drawable resource for the icon.
     */
    @Nullable
    public Drawable getGimbalPointIcon() {
        return gimbalPoint.getDrawable();
    }

    /**
     * Set the drawable resource for the icon that represents the point at which the gimbal
     * started.
     *
     * @param gimbalPointIcon The drawable resource for the icon.
     */
    public void setGimbalPointIcon(@Nullable Drawable gimbalPointIcon) {
        gimbalPoint.setImageDrawable(gimbalPointIcon);
    }

    /**
     * Get the drawable resource for the icon that represents the point towards which the gimbal
     * is moving.
     *
     * @return The drawable resource for the icon.
     */
    @Nullable
    public Drawable getGimbalMoveIcon() {
        return gimbalMove.getDrawable();
    }

    /**
     * Set the drawable resource for the icon that represents the point towards which the gimbal
     * is moving.
     *
     * @param gimbalMoveIcon The drawable resource for the icon.
     */
    public void setGimbalMoveIcon(@Nullable Drawable gimbalMoveIcon) {
        gimbalMove.setImageDrawable(gimbalMoveIcon);
    }

    /**
     * Get the drawable resource for the icon that represents the direction the gimbal is moving.
     *
     * @return The drawable resource for the icon.
     */
    @Nullable
    public Drawable getGimbalArrowIcon() {
        return gimbalArrow.getDrawable();
    }

    /**
     * Set the drawable resource for the icon that represents the direction the gimbal is moving.
     *
     * @param gimbalArrowIcon The drawable resource for the icon.
     */
    public void setGimbalArrowIcon(@Nullable Drawable gimbalArrowIcon) {
        gimbalArrow.setImageDrawable(gimbalArrowIcon);
    }

    /**
     * Get whether the device will vibrate when the gimbal control point appears.
     *
     * @return `true` if vibration is enabled, `false` otherwise.
     */
    public boolean isVibrationEnabled() {
        return isVibrationEnabled;
    }

    /**
     * Set whether the device will vibrate when the gimbal control point appears.
     *
     * @param vibrationEnabled `true` if vibration is enabled, `false` otherwise.
     */
    public void setVibrationEnabled(boolean vibrationEnabled) {
        isVibrationEnabled = vibrationEnabled;
    }

    /**
     * Get the duration of the vibration in milliseconds when the gimbal control point appears.
     *
     * @return The duration of the vibration in milliseconds.
     */
    public int getVibrationDuration() {
        return vibrationDuration;
    }

    /**
     * Set the duration of the vibration in milliseconds when the gimbal control point appears.
     *
     * @param vibrationDuration The duration of the vibration in milliseconds.
     */
    public void setVibrationDuration(@IntRange(from = 0) int vibrationDuration) {
        this.vibrationDuration = vibrationDuration;
    }

    //endregion
}
