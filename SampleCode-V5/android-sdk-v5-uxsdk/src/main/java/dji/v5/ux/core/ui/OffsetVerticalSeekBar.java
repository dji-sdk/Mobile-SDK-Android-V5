package dji.v5.ux.core.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;

import dji.v5.utils.common.DisplayUtil;
import dji.v5.ux.R;


public class OffsetVerticalSeekBar extends VerticalSeekBar {

    private Rect mRect;
    private Paint mPaint;
    private int mSeekBarHeight;
    private int mRectColor;
    private boolean mIsOnlyDragSupported = false;
    private boolean mIsDragging = false;

    public OffsetVerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews(context);
    }

    private void initViews(Context context) {
        mRect = new Rect();
        mPaint = new Paint();
        mSeekBarHeight = DisplayUtil.dip2px(getContext(), 4);
        mRectColor = context.getResources().getColor(R.color.uxsdk_white_0_percent);
    }

    @Override
    public void setProgressDrawable(Drawable d) {
        super.setProgressDrawable(d);
        updateProgressDrawableBounds();
    }

    @Override
    public void setThumb(Drawable thumb) {
        super.setThumb(thumb);
        updateProgressDrawableBounds();
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        mRect.set(0,
                (getHeight() / 2) - (mSeekBarHeight / 2),
                getWidth(),
                (getHeight() / 2) + (mSeekBarHeight / 2));

        mPaint.setColor(Color.TRANSPARENT);

        canvas.drawRect(mRect, mPaint);

        if (getProgress() < getMax() / 2) {
            mRect.set(getWidth() / 2 - mSeekBarHeight / 2,
                    (getHeight() / 2),
                    getWidth() / 2 + mSeekBarHeight / 2,
                    getThumb().getBounds().top);

            mPaint.setColor(mRectColor);
            canvas.drawRect(mRect, mPaint);

        }

        if (getProgress() > getMax() / 2) {
            mRect.set(getWidth() / 2 - mSeekBarHeight / 2,
                    (getThumb().getBounds().bottom),
                    getWidth() / 2 + mSeekBarHeight / 2,
                    getHeight() / 2);

            mPaint.setColor(mRectColor);
            canvas.drawRect(mRect, mPaint);
        }

        super.onDraw(canvas);
    }

    @Override
    protected void updateDrawableBounds(int w, int h) {
        updateProgressDrawableBounds();
        Drawable thumb = this.mThumb;

        int max = this.mMax;
        float scale = max > 0 ? (float) this.mProgress / (float) max : 0.0F;
        if (thumb != null) {
            this.setDrawableBounds(w, h, thumb, scale);
        }
    }

    public void updateProgressDrawableBounds() {
        Drawable d = this.mProgressDrawable;
        Drawable thumb = this.mThumb;

        int trackWidth = this.mProgressWidth;
        int thumbWidth = thumb == null ? 0 : thumb.getIntrinsicWidth();
        int thumbHeight = thumb == null ? 0 : thumb.getIntrinsicHeight();
        int gapForCenteringTrack = (thumbWidth - trackWidth) / 2;
        int top = thumbHeight / 2 + this.getPaddingTop();
        int bottom = this.getHeight() - this.getPaddingBottom() - thumbHeight / 2;

        if (d != null) {
            d.setBounds(gapForCenteringTrack, top, gapForCenteringTrack + trackWidth, bottom);
        }
    }

    public void setIsOnlyDrag(boolean isOnlyDragSupported) {
        this.mIsOnlyDragSupported = isOnlyDragSupported;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && mIsOnlyDragSupported
                && (event.getY() < mThumb.getBounds().top || event.getY() > mThumb.getBounds().bottom)) {
            mIsDragging = true;
            return true;
        }

        if (mIsOnlyDragSupported && mIsDragging
                && (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)) {
            mIsDragging = false;
            return true;
        }

        return super.onTouchEvent(event);
    }
}
