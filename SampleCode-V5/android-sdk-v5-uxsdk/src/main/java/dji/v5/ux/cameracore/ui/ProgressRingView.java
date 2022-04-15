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

package dji.v5.ux.cameracore.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.ColorInt;

import dji.v5.ux.R;

/**
 * Ring progress bar.
 * The view shows circular animation to indicate progress.
 */
public class ProgressRingView extends View {

    private RectF boundaries;
    private Paint paint;
    private Animation indeterminateAnimation;
    private Shader progressGradient;
    private boolean indeterminate;
    private int ringColor = Color.WHITE;
    private int height;
    private int width;

    public ProgressRingView(Context context) {
        super(context, null, 0);
    }

    public ProgressRingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressRingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        boundaries = new RectF();

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setColor(ringColor);

        if (!isInEditMode()) {
            indeterminateAnimation = AnimationUtils.loadAnimation(context, R.anim.uxsdk_anim_rotate);
            indeterminateAnimation.setAnimationListener(new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {
                    indeterminate = true;
                    if (progressGradient != null) {
                        paint.setShader(progressGradient);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    // Nothing to do here
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    paint.setShader(null);
                    invalidate();
                }
            });
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // StrokeWidth percentage
        // Color

        float strokeWidth = w * .08f;
        paint.setStrokeWidth(strokeWidth / 2);
        width = w;
        height = h;
        initProgressGradient(w, h);
        float padding = strokeWidth / 2;
        boundaries.top = padding;
        boundaries.left = padding;
        boundaries.bottom = getMeasuredWidth() - padding;
        boundaries.right = getMeasuredHeight() - padding;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawArc(boundaries, 0, 360, false, paint);
    }

    /**
     * Set the color of the progress ring.
     *
     * @param color integer value
     */
    public void setRingColor(@ColorInt int color) {
        ringColor = color;
        paint.setColor(color);
        initProgressGradient(width, height);
        invalidate();
    }

    /**
     * Check if the ring is currently animating.
     *
     * @return boolean value
     */
    public boolean isIndeterminate() {
        return indeterminate;
    }

    /**
     * Start/Stop the ring animation.
     *
     * @param indeterminate boolean value
     *                      true - to start animation
     *                      false - to stop animation
     */
    public void setIndeterminate(boolean indeterminate) {
        if (indeterminate == this.indeterminate) return;

        this.indeterminate = indeterminate;
        if (indeterminate) {
            startAnimation(indeterminateAnimation);
        } else {
            clearAnimation();
            indeterminateAnimation.cancel();
            indeterminateAnimation.reset();
        }
    }

    private void initProgressGradient(int w, int h) {
        progressGradient = null;
        float cx = w / 2f;
        float cy = h / 2f;
        int[] colors = {ringColor, Color.TRANSPARENT, Color.TRANSPARENT};
        float[] positions = {0, 0.7f, 1};

        progressGradient = new SweepGradient(cx, cy, colors, positions);

    }
}
