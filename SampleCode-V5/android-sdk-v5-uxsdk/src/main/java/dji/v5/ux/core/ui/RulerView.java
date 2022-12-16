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
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.Scroller;

import androidx.annotation.ColorInt;
import dji.v5.ux.R;

/**
 *
 * A custom view used to display a scale with increase and decrease buttons
 *
 */
public class RulerView extends View {


    protected final static int DEFAULT_INTERVAL = 10; // Default scale interval value
    protected final static int DEFAULT_NUMBER = 13;

    protected int width = 0;
    protected int height = 0;
    protected Paint drawPaint = null;

    protected Drawable selectDrawable = null;
    protected int scaleColor = 0;
    protected int scalePadding = 0;

    protected float density = 0;
    protected int minVelocity = 0;
    protected int maxVelocity = 0;
    protected Scroller scroller = null;
    protected VelocityTracker velocityTracker = null;
    protected int offsetY = 0;
    protected int lastTouchY = 0;
    protected final RectF tmpRect = new RectF();

    protected int maxSize = 2000;
    protected int curSize = 0;
    protected int interval = DEFAULT_INTERVAL;

    protected OnRulerScrollListener onScrollListener = null;
    protected OnRulerChangeListener onChangeListener = null;
    protected boolean isRulerEnabled = true;

    public RulerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initDatas(context);

        if (isInEditMode()) {
            return;
        }
        initDefaultAttrs();
    }

    /**
     * Set ruler scroll listener
     *
     * @param listener instance of {@link OnRulerScrollListener}
     */
    public void setOnScrollListener(final OnRulerScrollListener listener) {
        onScrollListener = listener;
    }

    /**
     * Set ruler change listener
     *
     * @param listener instance of {@link OnRulerChangeListener}
     */
    public void setOnChangeListener(final OnRulerChangeListener listener) {
        onChangeListener = listener;
    }

    /**
     * Set the max size of the ruler
     *
     * @param max integer value
     */
    public void setMaxSize(final int max) {
        if (max != maxSize) {
            maxSize = max;
            if (curSize > max) {
                final int beforeSize = curSize;
                curSize = max;
                if (null != onChangeListener) {
                    onChangeListener.onChanged(this, max, beforeSize, false);
                }
                offsetY = (int) ((max + 1) * density);
            }
            postInvalidate();
        }
    }

    /**
     * Get the max size of the ruler
     *
     * @return integer value
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Check if the ruler cursor is at minimum position
     *
     * @return boolean value true - cursor at minimum false - cursor more than minimum
     */
    public boolean isAtMin() {
        return (curSize == 0);
    }

    /**
     * Check if the ruler cursor is at maximum position
     *
     * @return boolean value true - cursor at maximum false - cursor less than maximum
     */
    public boolean isAtMax() {
        return (curSize == maxSize);
    }

    /**
     * Sets the new value and updates to the new state immediately.
     *
     * @param size integer value
     */
    public void setCurSizeNow(final int size) {
        final int beforeSize = curSize;
        curSize = size;
        if (null != onChangeListener) {
            onChangeListener.onChanged(this, size, beforeSize, false);
        }
        offsetY = (int) (size * density);
        postInvalidate();
    }

    /**
     * Sets the new value and uses a scrolling animation to update the new state.
     *
     * @param size integer value
     */
    public void setCurSize(int size) {
        if (size != curSize) {
            if (size > maxSize) {
                size = maxSize;
            } else if (size < 0) {
                size = 0;
            }
            final int step = (int) (Math.abs(curSize - size) * 1.0f / 8 + 1);
            post(new ScrollRunnable(curSize, size, step));
        }
    }

    /**
     * Get the cursor position of the ruler
     *
     * @return integer value
     */
    public int getCurSize() {
        return curSize;
    }

    /**
     * Increase the value by the given step
     *
     * @param step integer delta to increase
     * @return updated integer value
     */
    public int stepUp(final int step) {
        int size = curSize;
        if (curSize < maxSize) {
            size = (curSize + step);
            if (size > maxSize) {
                size = maxSize;
            }
            post(new ScrollRunnable(curSize, size));
        }
        return size;
    }

    /**
     * Reduce the value by the given step
     *
     * @param step integer delta to decrease
     * @return updated integer value
     */
    public int stepDown(final int step) {
        int size = curSize;
        if (curSize > 0) {
            size = (curSize - step);
            if (size < 0) {
                size = 0;
            }
            post(new ScrollRunnable(curSize, size));
        }
        return size;
    }

    /**
     * Increase the value by the
     * default interval size {@link #DEFAULT_INTERVAL}.
     */
    public void stepNext() {
        if (curSize < maxSize) {
            int size = (curSize + interval);
            if (size > maxSize) {
                size = maxSize;
            }
            post(new ScrollRunnable(curSize, size));
        }
    }

    /**
     * Decrease the value by the
     * default interval size {@link #DEFAULT_INTERVAL}
     */
    public void stepPrev() {
        if (curSize > 0) {
            int size = (curSize - interval);
            if (size < 0) {
                size = 0;
            }
            post(new ScrollRunnable(curSize, size));
        }
    }

    protected void initDefaultAttrs() {
        final Resources res = getResources();
        scaleColor = res.getColor(R.color.uxsdk_white);
        scalePadding = res.getDimensionPixelSize(R.dimen.uxsdk_gen_corner_radius);
        drawPaint.setColor(scaleColor);
    }

    protected void initDatas(final Context context) {
        scroller = new Scroller(context);

        drawPaint = new Paint();
        drawPaint.setAntiAlias(true);
        drawPaint.setStyle(Paint.Style.FILL);

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        minVelocity = configuration.getScaledMinimumFlingVelocity();
        maxVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(widthSize, heightSize);

        float targetDensity = getResources().getDisplayMetrics().density * 2;
        if (targetDensity < 4.0f) {
            targetDensity = 4.0f;
        }

        int number = DEFAULT_NUMBER - 1;
        final float fHeight = heightSize * 1.0f;
        density = fHeight / (number * interval + 1);
        while (density > targetDensity) {
            number += 2;
            density = fHeight / (number * interval + 1);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        if (selectDrawable != null) {
            final int selectHeight = selectDrawable.getIntrinsicHeight();
            final int selectWidth = selectDrawable.getIntrinsicWidth();
            selectDrawable.setBounds((w - selectWidth) / 2, (h - selectHeight) / 2, (w + selectWidth) / 2, (h + selectHeight) / 2);
        }
    }

    private void obtainTracker() {
        if (null == velocityTracker) {
            velocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleTracker() {
        if (null != velocityTracker) {
            velocityTracker.clear();
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    private void requestInterceptEvent() {
        final ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    private void onOffsetChanged(final int offset) {
        final int size = (int) (offset / density);
        if (size != curSize) {
            final int beforeSize = curSize;
            curSize = size;
            if (null != onChangeListener) {
                onChangeListener.onChanged(this, size, beforeSize, true);
            }
        }
    }

    private void scrollOverY(final int deltaY) {
        final int maxOffset = (int) ((maxSize + 1) * density);
        offsetY += deltaY;
        if (offsetY < 0) {
            offsetY = 0;
        } else if (offsetY > maxOffset) {
            offsetY = maxOffset;
        }
        onOffsetChanged(offsetY);
        postInvalidate();
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            offsetY = scroller.getCurrY();
            onOffsetChanged(offsetY);
            if (scroller.isFinished() && null != onScrollListener) {
                onScrollListener.onScrollingFinished(this);
            }
            postInvalidateOnAnimation();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isRulerEnabled) {
            obtainTracker();
            velocityTracker.addMovement(event);

            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    requestInterceptEvent();
                    if (!scroller.isFinished()) {
                        scroller.abortAnimation();
                    }
                    lastTouchY = (int) event.getY();
                    if (null != onScrollListener) {
                        onScrollListener.onScrollingStarted(this);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    final int y = (int) event.getY();
                    int deltaY = lastTouchY - y;
                    lastTouchY = y;
                    scrollOverY(deltaY);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    velocityTracker.computeCurrentVelocity(1000, maxVelocity);
                    int initialVelocity = (int) velocityTracker.getYVelocity();
                    if ((Math.abs(initialVelocity) > minVelocity)) {
                        final int maxOffset = (int) ((maxSize + 1) * density);
                        scroller.fling(0, offsetY, 0, -initialVelocity, 0, 0, 0, maxOffset);
                    } else if (null != onScrollListener) {
                        onScrollListener.onScrollingFinished(this);
                    }
                    recycleTracker();
                    break;
                default:
                    break;
            }
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (selectDrawable != null) {
            selectDrawable.draw(canvas);
        }

        final float offset = offsetY;
        final float maxOffset = (maxSize + 1) * density;
        final float halfH = height * 1.0f / 2;
        int offDensity = (int) ((offset / density) % interval);
        if (offDensity != 0) {
            offDensity = interval - offDensity;
        }

        final float left = scalePadding;
        final float right = (float) width - scalePadding;
        final float radius = density / 2;

        float top = offDensity * density;
        final float halfDensity = density / 2;

        while (top < height) {
            if ((halfH <= (top + offset + density)) && ((top + offset + halfDensity) <= (maxOffset + halfH))) {
                tmpRect.set(left, top, right, top + density);
                drawPaint.setAlpha(recalAlpha(top, halfH));
                canvas.drawRoundRect(tmpRect, radius, radius, drawPaint);
            }
            top += interval * density;
        }
    }

    private int recalAlpha(final float top, final float halfH) {
        final float pos = (top + density * 1.0f / 2);
        final float factor = Math.abs(pos - halfH) * 1.0f / halfH;
        return (int) (((1 - factor) * (1 - factor) * 0.95f + 0.05f) * 255);
    }

    private final class ScrollRunnable implements Runnable {
        private int mStartSize;
        private int mEndSize;
        private int mStep;
        private boolean mbAdd = false;

        private ScrollRunnable(final int start, final int end) {
            this(start, end, 2);
        }

        private ScrollRunnable(final int start, final int end, final int step) {
            mStartSize = start;
            mEndSize = end;
            mStep = step;
            if (start < end) {
                mbAdd = true;
            }
        }

        private void doAdd(){
            if (mEndSize <= mStartSize + mStep + 1) {
                final int beforeSize = curSize;
                curSize = mEndSize;
                if (null != onChangeListener) {
                    onChangeListener.onChanged(RulerView.this, mEndSize, beforeSize, true);
                }

                offsetY = (int) (mEndSize * density);
                postInvalidate();
            } else {
                curSize += mStep;
                if (curSize >= mEndSize) {
                    final int beforeSize = curSize;
                    curSize = mEndSize;
                    if (null != onChangeListener) {
                        onChangeListener.onChanged(RulerView.this, mEndSize, beforeSize, true);
                    }

                    offsetY = (int) (mEndSize * density);
                    postInvalidate();
                } else {
                    offsetY = (int) (curSize * density);
                    invalidate();
                    postDelayed(this, 10);
                }
            }
        }

        @Override
        public void run() {
            if (mbAdd) {
                doAdd();
            } else {
                if (mEndSize + mStep + 1 >= mStartSize) {
                    final int beforeSize = curSize;
                    curSize = mEndSize;
                    if (null != onChangeListener) {
                        onChangeListener.onChanged(RulerView.this, mEndSize, beforeSize, true);
                    }

                    offsetY = (int) (mEndSize * density);
                    postInvalidate();
                } else {
                    performListenerChange();
                }
            }
        }

        private void performListenerChange(){
            curSize -= mStep;
            if (curSize <= mEndSize) {
                final int beforeSize = curSize;
                curSize = mEndSize;
                if (null != onChangeListener) {
                    onChangeListener.onChanged(RulerView.this, mEndSize, beforeSize, true);
                }

                offsetY = (int) (mEndSize * density);
                postInvalidate();
            } else {
                offsetY = (int) (curSize * density);
                invalidate();
                postDelayed(this, 10);
            }
        }
    }

    /**
     * Interface to track updates of the ruler srcoll
     */
    public interface OnRulerScrollListener {
        /**
         * Indicate start of scroll action
         *
         * @param rulerView current instance of {@link RulerView}
         */
        void onScrollingStarted(final RulerView rulerView);

        /**
         * Indicate stop of scroll action
         *
         * @param rulerView current instance of {@link RulerView}
         */
        void onScrollingFinished(final RulerView rulerView);
    }


    /**
     * Interface to track changes in ruler
     *
     */
    public interface OnRulerChangeListener {

        /**
         * Indicate ruler changed
         *
         * @param rulerView current instance of {@link RulerView}
         * @param newSize update size
         * @param oldSize old size
         * @param fromUser is the update from user interaction
         */
        void onChanged(final RulerView rulerView, final int newSize, final int oldSize, final boolean fromUser);
    }

    /**
     * Enable or disable the scroll
     *
     * @param isEnabled true- scroll enabled false - scroll disabled
     */
    public void setRulerEnabled(boolean isEnabled) {
        isRulerEnabled = isEnabled;
    }

    /**
     * Check if ruler scrolling is enabled
     *
     * @return boolean value true- enabled false - disabled
     */
    public boolean isRulerEnabled() {
        return isRulerEnabled;
    }


    /**
     * Set the ruler scale color
     *
     * @param scaleColor integer value representing color
     */
    public void setScaleColor(@ColorInt int scaleColor) {
        this.scaleColor = scaleColor;
        drawPaint.setColor(scaleColor);
        invalidate();
    }

    /**
     * Get the ruler scale color
     *
     * @return integer value representing color
     */
    @ColorInt
    public int getScaleColor() {
        return scaleColor;
    }

}
