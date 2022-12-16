/**
 * @filename : DJIVSeekBar.java
 * @package : dji.pilot.publics.widget
 * @date : 2015-10-30 下午12:18:20
 * @author : gashion.fang
 * <p>
 * Copyright (c) 2015, DJI All Rights Reserved.
 */

package dji.v5.ux.core.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;

import dji.v5.utils.common.DisplayUtil;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;


public class VerticalSeekBar extends View {

    public interface OnVSBChangeListener {
        void onProgressChanged(VerticalSeekBar seekBar, int progress, boolean fromUser);

        void onStartTrackingTouch(VerticalSeekBar seekBar);

        void onStopTrackingTouch(VerticalSeekBar seekBar);
    }

    private static final int MAX_LEVEL = 10000;
    protected final String logTag = LogUtils.getTag(this);

    protected Drawable mProgressDrawable = null;
    protected int mProgressWidth = 0;
    protected Drawable mThumb = null;
    protected int mMax = 1;
    protected int mProgress = 0;
    protected int mSecondaryProgress = 0;
    // 宽度必须比Thumb的宽度要小，如果有需要，请重写或者修改该控件
    protected Drawable mSecondaryThumb = null;

    private int mScaledTouchSlop = 0;
    private float mTouchDownY = 0.0f;
    private boolean mIsDragging = false;

    private OnVSBChangeListener mOnChangedListener = null;

    public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        initAttrs(context);

        final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.VerticalSeekBar, 0, 0);
        mProgressWidth = ta.getDimensionPixelSize(R.styleable.VerticalSeekBar_uxsdk_progressHeight, mProgressWidth);
        Drawable drawable = ta.getDrawable(R.styleable.VerticalSeekBar_uxsdk_progressDrawable);
        if (null != drawable) {
            setProgressDrawable(drawable);
        }
        setMax(ta.getInt(R.styleable.VerticalSeekBar_uxsdk_max, mMax));
        setProgress(ta.getInt(R.styleable.VerticalSeekBar_uxsdk_progress, mProgress));
        setSecondaryProgress(ta.getInt(R.styleable.VerticalSeekBar_uxsdk_secondaryProgress, mSecondaryProgress));
        Drawable thumb = ta.getDrawable(R.styleable.VerticalSeekBar_uxsdk_thumb);
        if (null != thumb) {
            setThumb(thumb);
        }

        Drawable sThumb = ta.getDrawable(R.styleable.VerticalSeekBar_uxsdk_secondaryThumb);
        if (null != sThumb) {
            setSecondaryThumb(sThumb);
        }
        ta.recycle();
    }

    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs);
    }

    public VerticalSeekBar(Context context) {
        this(context, null);
    }

    public void setOnChangeListener(final OnVSBChangeListener listener) {
        mOnChangedListener = listener;
    }

    public void setProgressDrawable(Drawable d) {
        if (!isInEditMode()){
            d = tileify(d, false, 0);
        }
        boolean needUpdate;
        if (mProgressDrawable != null && d != mProgressDrawable) {
            mProgressDrawable.setCallback(null);
            needUpdate = true;
        } else {
            needUpdate = false;
        }

        if (d != null) {
            d.setCallback(this);
        }

        mProgressDrawable = d;
        postInvalidate();
        if (needUpdate) {
            updateDrawableBounds(getWidth(), getHeight());
            doRefreshProgress(android.R.id.progress, mProgress, false, false);
            doRefreshProgress(android.R.id.secondaryProgress, mSecondaryProgress, false, false);
        }
    }

    public void setThumb(Drawable thumb) {
        boolean needUpdate = false;
        if (mThumb != null && thumb != mThumb) {
            needUpdate = true;
        }
        if (thumb != null && needUpdate
                && (thumb.getIntrinsicWidth() != mThumb.getIntrinsicWidth() || thumb.getIntrinsicHeight() != mThumb.getIntrinsicHeight())) {
            requestLayout();
        }
        mThumb = thumb;
        invalidate();
        if (needUpdate) {
            updateDrawableBounds(getWidth(), getHeight());
        }
    }

    public void setSecondaryThumb(final Drawable thumb) {
        mSecondaryThumb = thumb;
        setDrawableBounds(getWidth(), getHeight(), mSecondaryThumb, (mMax == 0 ? 0.0f : (float) mSecondaryProgress / mMax));
        invalidate();
    }

    public int getMax() {
        return mMax;
    }

    public void setMax(int max) {
        if (max < 0) {
            max = 0;
        }
        if (max != mMax) {
            mMax = max;

            if (mProgress > max) {
                mProgress = max;
            }
            setDrawableBounds(getWidth(), getHeight(), mThumb, (mMax == 0 ? 0.0f : (float) mProgress / mMax));
            postInvalidate();
            doRefreshProgress(android.R.id.progress, mProgress, false, false);
            doRefreshProgress(android.R.id.secondaryProgress, mSecondaryProgress, false, false);
        }
    }

    public int getProgress() {
        return mProgress;
    }

    public void setProgress(int progress) {
        setProgress(progress, false);
    }

    private void setProgress(int progress, final boolean fromUser) {
        if (progress < 0) {
            progress = 0;
        }

        if (progress > mMax) {
            progress = mMax;
        }

        if (progress != mProgress) {
            mProgress = progress;
            doRefreshProgress(android.R.id.progress, mProgress, fromUser, true);
        }
    }

    public int getSecondaryProgress() {
        return mSecondaryProgress;
    }

    public void setSecondaryProgress(int secondProgress) {
        if (secondProgress < 0) {
            secondProgress = 0;
        }

        if (secondProgress > mMax) {
            secondProgress = mMax;
        }

        if (secondProgress != mSecondaryProgress) {
            mSecondaryProgress = secondProgress;
            doRefreshProgress(android.R.id.secondaryProgress, mSecondaryProgress, false, false);
        }
    }

    protected void updateDrawableBounds(int w, int h) {
        Drawable d = mProgressDrawable;
        Drawable thumb = mThumb;
        final int thumbWidth = thumb == null ? 0 : thumb.getIntrinsicWidth();
        final int trackWidth = mProgressWidth;

        int max = mMax;
        float scale = max > 0 ? (float) mProgress / (float) max : 0;

        setDrawableBounds(w, h, thumb, scale);

        int gapForCenteringTrack = (thumbWidth - trackWidth) / 2;
        int bottom = h - getPaddingTop() - getPaddingBottom();
        if (d != null) {
            d.setBounds(gapForCenteringTrack, 0, gapForCenteringTrack + trackWidth, bottom);
        }
    }

    protected void setDrawableBounds(final int w, final int h, final Drawable thumb, final float scale) {
        if (thumb == null) {
            return;
        }
        int available = h - getPaddingTop() - getPaddingBottom();
        int thumbWidth = thumb.getIntrinsicWidth();
        int thumbHeight = thumb.getIntrinsicHeight();
        available -= thumbHeight;

        int thumbLeft = (w - thumbWidth) / 2;
        int thumbPos = available + getPaddingTop() - (int) (scale * available);
        thumb.setBounds(thumbLeft, thumbPos, thumbLeft + thumbWidth, thumbPos + thumbHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onTouchDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                onTouchMove(event);
                break;
            case MotionEvent.ACTION_UP:
                onTouchUp(event);
                break;
            case MotionEvent.ACTION_CANCEL:
                onTouchCancel();
                break;
            default:
                break;
        }
        return true;
    }

    private void onTouchDown(MotionEvent event) {
        if (isInScrollingContainer()) {
            mTouchDownY = event.getY();
        } else {
            setPressed(true);
            if (mThumb != null) {
                invalidate(mThumb.getBounds());
            }
            onStartTrackingTouch();
            trackTouchEvent(event);
            attemptClaimDrag();
        }
    }

    private void onTouchMove(MotionEvent event) {
        if (mIsDragging) {
            trackTouchEvent(event);
        } else {
            final float y = event.getY();
            if (Math.abs(y - mTouchDownY) > mScaledTouchSlop) {
                setPressed(true);
                if (mThumb != null) {
                    invalidate(mThumb.getBounds());
                }
                onStartTrackingTouch();
                trackTouchEvent(event);
                attemptClaimDrag();
            }
        }
    }

    private void onTouchUp(MotionEvent event) {
        if (mIsDragging) {
            trackTouchEvent(event);
            onStopTrackingTouch();
            setPressed(false);
        } else {
            onStartTrackingTouch();
            trackTouchEvent(event);
            onStopTrackingTouch();
        }
        invalidate();
    }

    private void onTouchCancel() {
        if (mIsDragging) {
            onStopTrackingTouch();
            setPressed(false);
        }
        invalidate();
    }

    private void trackTouchEvent(MotionEvent event) {
        final int height = getHeight();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();

        final int available = height - paddingTop - paddingBottom;
        int y = (int) event.getY();
        float scale;
        float progress = 0;

        if (y > height - paddingBottom) {
            scale = 0.0f;
        } else if (y < paddingTop) {
            scale = 1.0f;
        } else {
            scale = (float) (available - y + paddingTop) / (float) available;
            progress = 0.0f;
        }

        final int max = getMax();
        progress += scale * max;

        setProgress((int) progress, true);
        // doRefreshProgress(android.R.id.progress, (int) progress, true, true);
    }

    private void attemptClaimDrag() {
        ViewParent vp = getParent();
        if (null != vp) {
            vp.requestDisallowInterceptTouchEvent(true);
        }
    }

    void onStartTrackingTouch() {
        mIsDragging = true;
        if (null != mOnChangedListener) {
            mOnChangedListener.onStartTrackingTouch(this);
        }
    }

    void onStopTrackingTouch() {
        mIsDragging = false;
        if (null != mOnChangedListener) {
            mOnChangedListener.onStopTrackingTouch(this);
        }
    }

    protected boolean isInScrollingContainer() {
        ViewParent vp = getParent();
        while (vp instanceof ViewGroup) {
            if (((ViewGroup) vp).shouldDelayChildPressedState()) {
                return true;
            }
            vp = vp.getParent();
        }
        return false;
    }

    public Drawable getThumb() {
        return mThumb;
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mProgressDrawable || who == mThumb || who == mSecondaryThumb || super.verifyDrawable(who);
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        if (verifyDrawable(drawable)) {
            final Rect dirty = drawable.getBounds();
            final int scrollX = getScrollX() + getPaddingLeft();
            final int scrollY = getScrollY() + getPaddingRight();

            invalidate(dirty.left + scrollX, dirty.top + scrollY, dirty.right + scrollX, dirty.bottom + scrollY);
        } else {
            super.invalidateDrawable(drawable);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateDrawableBounds(w, h);
        setDrawableBounds(w, h, mSecondaryThumb, (mMax == 0 ? 0.0f : (float) mSecondaryProgress / mMax));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int thumbWidth = mThumb == null ? 0 : mThumb.getIntrinsicWidth();
        int dw = mProgressWidth; // MeasureSpec.getSize(widthMeasureSpec);
        int dh = MeasureSpec.getSize(heightMeasureSpec);
        dw = Math.max(thumbWidth, dw);
        dw += getPaddingLeft() + getPaddingRight();
        dh += getPaddingTop() + getPaddingBottom();
        LogUtils.i(logTag, "onMeasure", dw, dh);
        setMeasuredDimension(dw, dh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final Drawable d = mProgressDrawable;
        if (null != d) {
            canvas.save();
            canvas.translate(getPaddingLeft(), getPaddingTop());
            d.draw(canvas);
            canvas.restore();
        }

        if (null != mSecondaryThumb) {
            canvas.save();
            canvas.translate(getPaddingLeft(), getPaddingTop());
            mSecondaryThumb.draw(canvas);
            canvas.restore();
        }

        if (null != mThumb) {
            canvas.save();
            canvas.translate(getPaddingLeft(), getPaddingTop());
            mThumb.draw(canvas);
            canvas.restore();
        }
    }

    private synchronized void doRefreshProgress(int id, int progress, final boolean fromUser, final boolean callToApp) {
        float scale = mMax > 0 ? (float) progress / (float) mMax : 0;
        final Drawable d = mProgressDrawable;
        if (d != null) {
            Drawable progressDrawable = null;

            if (d instanceof LayerDrawable) {
                progressDrawable = ((LayerDrawable) d).findDrawableByLayerId(id);
            }

            final int level = (int) (scale * MAX_LEVEL);
            (progressDrawable != null ? progressDrawable : d).setLevel(level);
        } else {
            invalidate();
        }

        if (id == android.R.id.progress) {
            setDrawableBounds(getWidth(), getHeight(), mThumb, scale);
            invalidate();
            if (callToApp && null != mOnChangedListener) {
                mOnChangedListener.onProgressChanged(this, progress, fromUser);
            }
        } else if (id == android.R.id.secondaryProgress) {
            setDrawableBounds(getWidth(), getHeight(), mSecondaryThumb, scale);
            invalidate();
        }
    }

    protected void initAttrs(final Context context) {
        mProgressWidth = DisplayUtil.dip2px(context, 4);
        mMax = 100;
        mProgress = 0;
        mSecondaryProgress = 0;
        mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    private Shape getDrawableShape() {
        final float[] roundedCorners = new float[]{
                5, 5, 5, 5, 5, 5, 5, 5
        };
        return new RoundRectShape(roundedCorners, null, null);
    }

    private Drawable tileify(Drawable drawable, final boolean clip, final int drawableId) {
        if (drawable instanceof LayerDrawable) {
            LayerDrawable background = (LayerDrawable) drawable;
            final int N = background.getNumberOfLayers();
            Drawable[] outDrawables = new Drawable[N];

            for (int i = 0; i < N; i++) {
                int id = background.getId(i);
                outDrawables[i] = tileify(background.getDrawable(i), (id == android.R.id.progress || id == android.R.id.secondaryProgress),
                        id);
            }

            LayerDrawable newBg = new LayerDrawable(outDrawables);

            for (int i = 0; i < N; i++) {
                newBg.setId(i, background.getId(i));
            }

            return newBg;
        } else if (drawable instanceof BitmapDrawable) {
            final Bitmap tileBitmap = ((BitmapDrawable) drawable).getBitmap();

            final ShapeDrawable shapeDrawable = new ShapeDrawable(getDrawableShape());

            final BitmapShader bitmapShader = new BitmapShader(tileBitmap, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP);
            shapeDrawable.getPaint().setShader(bitmapShader);

            if (android.R.id.background == drawableId) {
                mProgressWidth = tileBitmap.getWidth();
            }

            return (clip) ? new ClipDrawable(shapeDrawable, Gravity.BOTTOM, ClipDrawable.VERTICAL) : shapeDrawable;
        }
        return drawable;
    }
}
