package dji.v5.ux.core.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import dji.v5.utils.common.AndUtil;
import dji.v5.ux.R;

/**
 * Range SeekBar
 * @author eleven
 */
public class RangeSeekBar extends View {


    protected Drawable mLeftThumb;
    protected Drawable mRightThumb;
    protected Drawable mBackgroundDrawable;
    protected Drawable mProgressDrawable;
    protected int mThumbSize;

    protected int mMax = 100;
    protected int mMin = 0;
    /**
     * mMax和mMin的当前值，如果有动画的话
     */
    protected float mDrawMax = mMax;
    protected float mDrawMin = mMin;
    protected int mLeftValue = 0;
    protected int mRightValue = mMax;
    protected int mMaxHeight;

    protected float mDownX;
    protected float mDownY;

    protected float mLastTouchX;

    protected Drawable mTouchThumb;
    protected Rect mTouchRect = new Rect();

    protected OnChangedListener mOnChangedListener;

    public RangeSeekBar(Context context) {
        this(context, null);
    }

    public RangeSeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RangeSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RangeSeekBar);
        mLeftThumb = ta.getDrawable(R.styleable.RangeSeekBar_uxsdk_range_leftThumbDrawable);
        if (mLeftThumb == null) {
            mLeftThumb = getResources().getDrawable(R.drawable.uxsdk_ic_temp_cold);
        }
        mRightThumb = ta.getDrawable(R.styleable.RangeSeekBar_uxsdk_range_rightThumbDrawable);
        if (mRightThumb == null) {
            mRightThumb = getResources().getDrawable(R.drawable.uxsdk_ic_temp_hot);
        }
        mBackgroundDrawable = ta.getDrawable(R.styleable.RangeSeekBar_uxsdk_range_backgroundDrawable);
        if (mBackgroundDrawable == null) {
            mBackgroundDrawable = getResources().getDrawable(R.drawable.uxsdk_range_seekbar_bg);
        }
        mProgressDrawable = ta.getDrawable(R.styleable.RangeSeekBar_uxsdk_range_progressDrawable);
        if (mProgressDrawable == null) {
            mProgressDrawable = getResources().getDrawable(R.drawable.uxsdk_ic_isotherm_seekbar);
        }
        mThumbSize = ta.getDimensionPixelSize(R.styleable.RangeSeekBar_uxsdk_range_thumbSize, 0);
        if (mThumbSize == 0) {
            mLeftThumb.getIntrinsicWidth();
        }
        mMaxHeight = ta.getDimensionPixelSize(R.styleable.RangeSeekBar_uxsdk_range_progressHeight, AndUtil.dip2px(context, 3));
        ta.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        // 高度
        int heightNeeded = mThumbSize;

        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);

        if (heightMode == View.MeasureSpec.EXACTLY) {
            heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(heightSize, View.MeasureSpec.EXACTLY);
        } else if (heightMode == View.MeasureSpec.AT_MOST) {
            heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                    heightSize < heightNeeded ? heightSize : heightNeeded, View.MeasureSpec.EXACTLY);
        } else {
            heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                    heightNeeded, View.MeasureSpec.EXACTLY);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 背景
        drawBackground(canvas);
        // 进度
        drawProgress(canvas);

        // thumb
        drawLeftThumb(canvas);
        drawRightThumb(canvas);
    }

    protected void drawBackground(Canvas canvas) {
        // 背景
        int paddingLeft = getPaddingLeft() + mThumbSize/2;
        int paddingRight = getPaddingRight() + mThumbSize/2;
        mBackgroundDrawable.setBounds(paddingLeft, getHeight()/2 - mMaxHeight/2, getWidth() - paddingRight, getHeight()/2 + mMaxHeight/2);
        mBackgroundDrawable.draw(canvas);
    }

    protected void drawProgress(Canvas canvas) {
        int leftX = getThumbPosition(mLeftValue);
        int rightX = getThumbPosition(mRightValue);

        mProgressDrawable.setBounds(leftX, mBackgroundDrawable.getBounds().top, rightX, mBackgroundDrawable.getBounds().bottom);
        mProgressDrawable.draw(canvas);
    }

    protected void drawLeftThumb(Canvas canvas) {
        mLeftThumb.setBounds(getThumbPosition(mLeftValue) - mThumbSize/2, 0, getThumbPosition(mLeftValue) + mThumbSize/2, mThumbSize);
        mLeftThumb.draw(canvas);
    }

    protected void drawRightThumb(Canvas canvas) {
        mRightThumb.setBounds(getThumbPosition(mRightValue) - mThumbSize/2, 0, getThumbPosition(mRightValue) + mThumbSize/2, mThumbSize);
        mRightThumb.draw(canvas);
    }

    /**
     * 获取thumb的x轴坐标
     * @param value
     * @return
     */
    protected int getThumbPosition(int value) {
        // progress的总长度
        float width = getWidth() - getPaddingRight() - (float)mThumbSize/2 - getPaddingLeft() - (float)mThumbSize/2;
        float range = mDrawMax - mDrawMin;
        float v = value - mDrawMin;
        return Math.round((v / range) * width) + getPaddingLeft() + mThumbSize/2;
    }

    protected int getValueByDelta(float delta) {
        int width = getWidth() - getPaddingRight() - getPaddingLeft() - mThumbSize;
        int range = mMax - mMin;
        return Math.round(delta / width * range);
    }


    public int getMax() {
        return mMax;
    }

    public void setMax(int max) {
        if (max == mMax) {
            return;
        }
        mMax = max;
        mDrawMax = mMax;
        postInvalidate();
    }

    public int getMin() {
        return mMin;
    }

    public void setMin(int min) {
        if (min == mMin) {
            return;
        }
        mMin = min;
        mDrawMin = mMin;
        postInvalidate();
    }

    public int getLeftValue() {
        return mLeftValue;
    }

    public void setLeftValue(int leftValue) {
        if (leftValue < mMin) {
            leftValue = mMin;
        }
        if (mLeftValue == leftValue) {
            return;
        }
        mLeftValue = leftValue;
        postInvalidate();
    }

    public int getRightValue() {
        return mRightValue;
    }

    public void setRightValue(int rightValue) {
        if (rightValue > mMax) {
            rightValue = mMax;
        }
        if (mRightValue == rightValue) {
            return;
        }
        mRightValue = rightValue;
        postInvalidate();
    }

    public void animateMax(int max) {
        if (max == mMax || max <= mMin) {
            return;
        }
        ValueAnimator objectAnimator = ValueAnimator.ofFloat(mMax, max);
        objectAnimator.setDuration(200);
        objectAnimator.setInterpolator(new FastOutSlowInInterpolator());
        objectAnimator.addUpdateListener(animation -> {
            mDrawMax = (Float) animation.getAnimatedValue();
            postInvalidate();
        });
        objectAnimator.start();
        mMax = max;
    }

    public void animateMin(int min) {
        if (min == mMin || min >= mMax) {
            return;
        }
        ValueAnimator objectAnimator = ValueAnimator.ofFloat(mMin, min);
        objectAnimator.setDuration(200);
        objectAnimator.setInterpolator(new FastOutSlowInInterpolator());
        objectAnimator.addUpdateListener(animation -> {
            mDrawMin = (Float) animation.getAnimatedValue();
            postInvalidate();
        });
        objectAnimator.start();
        mMin = min;
    }

    public OnChangedListener getOnChangedListener() {
        return mOnChangedListener;
    }

    public void setOnChangedListener(OnChangedListener onChangedListener) {
        mOnChangedListener = onChangedListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (handleTouchDown(event)) return false;
                break;
            case MotionEvent.ACTION_MOVE:
                handleTouchMove(event);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTouchThumb = null;
                onSeekEnd();
                break;
            default:
                break;
        }

        return true;
    }

    private void handleTouchMove(MotionEvent event) {
        float x = event.getX();
        float delta = x - mLastTouchX;
        mLastTouchX = x;
        int value =getValueByDelta(Math.abs(delta));
        if (delta < 0) {
            value = -value;
        }
        if (mTouchThumb == mLeftThumb) {
            int newValue = mLeftValue + value;
            if (newValue < mMin) {
                newValue = mMin;
            }
            if (newValue >= mRightValue) {
                newValue = mRightValue - 1;
            }
            mLeftValue = newValue;
            onValueChanged(true);
        } else if (mTouchThumb == mRightThumb) {
            int newValue = mRightValue + value;
            if (newValue <= mLeftValue) {
                newValue = mLeftValue + 1;
            }
            if (newValue > mMax) {
                newValue = mMax;
            }
            mRightValue = newValue;
            onValueChanged(true);
        }
        postInvalidate();
    }

    private boolean handleTouchDown(MotionEvent event) {
        mDownX = event.getX();
        mDownY = event.getY();
        mLastTouchX = event.getX();

        if (getTouchRect(mLeftThumb).contains((int)mDownX, (int)mDownY)) {
            mTouchThumb = mLeftThumb;
            onSeekStart();
        } else if (getTouchRect(mRightThumb).contains((int)mDownX, (int)mDownY)) {
            mTouchThumb = mRightThumb;
            onSeekStart();
        }
        if (mTouchThumb == null) {
            return true;
        }
        return false;
    }

    protected Rect getTouchRect(Drawable thumb) {
        mTouchRect.set(thumb.getBounds().left - mThumbSize/2, thumb.getBounds().top, thumb.getBounds().right + mThumbSize/2, thumb.getBounds().bottom);
        return mTouchRect;
    }

    private void onSeekStart() {
        if (mOnChangedListener != null) {
            mOnChangedListener.onSeekStart(this);
        }
    }

    private void onValueChanged(boolean fromUser) {
        if (mOnChangedListener != null) {
            mOnChangedListener.onValueChanged(this, mLeftValue, mRightValue, fromUser);
        }
    }

    private void onSeekEnd() {
        if (mOnChangedListener != null) {
            mOnChangedListener.onSeekEnd(this);
        }
    }

    public interface OnChangedListener {
        void onSeekStart(RangeSeekBar seekBar);
        void onValueChanged(RangeSeekBar seekBar, int leftValue, int rightValue, boolean fromUser);
        void onSeekEnd(RangeSeekBar seekBar);
    }
}
