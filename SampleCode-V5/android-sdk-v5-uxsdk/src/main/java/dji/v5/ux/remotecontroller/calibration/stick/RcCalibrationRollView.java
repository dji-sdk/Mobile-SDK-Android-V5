/*
 * Copyright ©2019 DJI All Rights Reserved.
 */

package dji.v5.ux.remotecontroller.calibration.stick;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;



import java.util.Arrays;

import dji.v5.ux.R;
import dji.v5.ux.core.ui.BaseView;

/**
 * ${TODO}
 *
 * @author joe.yang@dji.com
 * @date 3/25/19 5:47 PM
 */
public class RcCalibrationRollView extends BaseView {

    private static final float PROGRESS_DRAW_THRESHOLD = 1.0f;

    /**
     * 中间摇杆图片的边长
     */
    private int mStickBitmapSidePx;

    /**
     * 摇杆每个方向的最长距离
     */
    private int mStickMaxProgressPx;

    //region 向上整体杆量的 起始、结尾 坐标
    private int mTopProgressBarStartX;
    private int mTopProgressBarStartY;
    private int mTopProgressBarEndX;
    private int mTopProgressBarEndY;
    //endregion

    //region 向右整体杆量的 起始、结尾 坐标
    private int mRightProgressBarStartX;
    private int mRightProgressBarStartY;
    private int mRightProgressBarEndX;
    private int mRightProgressBarEndY;
    //endregion

    //region 向下整体杆量的 起始、结尾 坐标
    private int mBottomProgressBarStartX;
    private int mBottomProgressBarStartY;
    private int mBottomProgressBarEndX;
    private int mBottomProgressBarEndY;
    //endregion

    //region 向左整体杆量的 起始、结尾 坐标
    private int mLeftProgressBarStartX;
    private int mLeftProgressBarStartY;
    private int mLeftProgressBarEndX;
    private int mLeftProgressBarEndY;
    //endregion

    /**
     * 水平方向杆量，> 0 时表示右掰杆，< 0 时表示左掰杆
     */
    private float mHorizontalProgress;

    /**
     * 垂直方向杆量，> 0 时表示上掰杆，< 0 时表示下掰杆
     */
    private float mVerticalProgress;

    //region 上下左右的实际杆量坐标
    private float mProgressTopEndY;
    private float mProgressBottomEndY;
    private float mProgressLeftEndX;
    private float mProgressRightEndX;
    //endregion

    private int mFontWidth;
    private Rect mLeftTextRect;
    private Rect mTopTextRect;
    private Rect mBottomTextRect;
    private Rect mRightTextRect;

    private Bitmap mStickBitmap;
    private Rect mStickRect;

    private Paint mStickBitmapPaint;
    private Paint mProgressBackgroundPaint;
    private Paint mHorizontalProgressPaint;
    private Paint mVerticalProgressPaint;
    private Paint mFontPaint;

    //记录是否达到极值
    private Boolean[] mLimitStatus = new Boolean[]{false, false, false, false};

    public RcCalibrationRollView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        float progressBarWidth = getResources().getDimension(R.dimen.uxsdk_2_dp);
        int progressBarAndRollGap = getResources().getDimensionPixelSize(R.dimen.uxsdk_2_dp);

        mFontWidth = getResources().getDimensionPixelSize(R.dimen.uxsdk_32_dp);

        mStickBitmapSidePx = getResources().getDimensionPixelSize(R.dimen.uxsdk_24_dp);
        mStickMaxProgressPx = getResources().getDimensionPixelSize(R.dimen.uxsdk_40_dp);
        mStickBitmap = getBitmapFromVectorDrawable(context, R.drawable.uxsdk_img_fpv_rc_calibration_shell_stick);
        mStickBitmapPaint = new Paint();

        mProgressBackgroundPaint = new Paint();
        mProgressBackgroundPaint.setAntiAlias(true);
        mProgressBackgroundPaint.setColor(Color.GRAY);
        mProgressBackgroundPaint.setStrokeWidth(progressBarWidth);
        mProgressBackgroundPaint.setStrokeCap(Paint.Cap.ROUND);
        mProgressBackgroundPaint.setStyle(Paint.Style.STROKE);

        mHorizontalProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHorizontalProgressPaint.setColor(Color.WHITE);
        mHorizontalProgressPaint.setStrokeWidth(progressBarWidth);
        mHorizontalProgressPaint.setStrokeCap(Paint.Cap.ROUND);
        mHorizontalProgressPaint.setStyle(Paint.Style.STROKE);

        mVerticalProgressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mVerticalProgressPaint.setColor(Color.WHITE);
        mVerticalProgressPaint.setStrokeWidth(progressBarWidth);
        mVerticalProgressPaint.setStrokeCap(Paint.Cap.ROUND);
        mVerticalProgressPaint.setStyle(Paint.Style.STROKE);

        int fontSize = getResources().getDimensionPixelSize(R.dimen.uxsdk_12_dp);
        mFontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFontPaint.setStrokeWidth(0);
        mFontPaint.setTextSize(fontSize);
        mFontPaint.setColor(Color.WHITE);
        mFontPaint.setTextAlign(Paint.Align.CENTER);

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int width = getWidth();
                int height = getHeight();

                int centerX = width / 2;
                int centerY = height / 2;

                int stickRectLeft = centerX - mStickBitmapSidePx / 2;
                int stickRectTop = centerY - mStickBitmapSidePx / 2;
                int stickRectRight = centerX + mStickBitmapSidePx / 2;
                int stickRectBottom = centerY + mStickBitmapSidePx / 2;
                mStickRect = new Rect(stickRectLeft, stickRectTop, stickRectRight, stickRectBottom);

                mTopProgressBarStartX = centerX;
                mTopProgressBarStartY = centerY - mStickBitmapSidePx / 2 - progressBarAndRollGap;
                mTopProgressBarEndX = centerX;
                mTopProgressBarEndY = mTopProgressBarStartY - mStickMaxProgressPx;

                mRightProgressBarStartX = centerX + mStickBitmapSidePx / 2 +  progressBarAndRollGap;
                mRightProgressBarStartY = centerY;
                mRightProgressBarEndX = mRightProgressBarStartX + mStickMaxProgressPx;
                mRightProgressBarEndY = centerY;

                mBottomProgressBarStartX = centerX;
                mBottomProgressBarStartY = centerY + mStickBitmapSidePx / 2 + progressBarAndRollGap;
                mBottomProgressBarEndX = centerX;
                mBottomProgressBarEndY = mBottomProgressBarStartY + mStickMaxProgressPx;

                mLeftProgressBarStartX = centerX - mStickBitmapSidePx / 2 - progressBarAndRollGap;
                mLeftProgressBarStartY = centerY;
                mLeftProgressBarEndX = mLeftProgressBarStartX - mStickMaxProgressPx;
                mLeftProgressBarEndY = centerY;

                mLeftTextRect = new Rect(0, 0, mFontWidth, height);
                mTopTextRect = new Rect(0, 0, width, mFontWidth);
                mRightTextRect = new Rect(width - mFontWidth, 0, width, height);
                mBottomTextRect = new Rect(0, height - mFontWidth, width, height);

                mProgressLeftEndX = mLeftProgressBarStartX;
                mProgressRightEndX = mRightProgressBarStartX;
                mProgressBottomEndY = mBottomProgressBarStartY;
                mProgressTopEndY = mTopProgressBarStartY;
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getResources().getDimensionPixelSize(R.dimen.uxsdk_184_dp);
        int height = getResources().getDimensionPixelSize(R.dimen.uxsdk_184_dp);
        setMeasuredDimension(width, height);
    }

    public boolean isGetLimit(){
        boolean result = true;
        for( Boolean status : mLimitStatus) {
            result &= status;
        }
        return result;
    }

    public void resetLimit(){
        Arrays.fill(mLimitStatus, false);
    }

    private void handlerLimitStatus(int horizontal, int vertical) {
        if (horizontal == 100) {
            mLimitStatus[0] = true;
        } else if (horizontal == -100) {
            mLimitStatus[1] = true;
        }
        if (vertical == 100) {
            mLimitStatus[2] = true;
        } else if (vertical == -100) {
            mLimitStatus[3] = true;
        }
    }

    public void setProgress(int horizontal, int vertical) {
        handlerLimitStatus(horizontal,vertical);
        if (Math.abs(horizontal) == 100) {
            mHorizontalProgressPaint.setColor(Color.GREEN);
        } else {
            mHorizontalProgressPaint.setColor(Color.WHITE);
        }
        if (Math.abs(vertical) == 100) {
            mVerticalProgressPaint.setColor(Color.GREEN);
        } else {
            mVerticalProgressPaint.setColor(Color.WHITE);
        }
        float horizontalProgressPx = mStickMaxProgressPx * horizontal / 100.0f;
        if (horizontalProgressPx < 0) {
            mProgressLeftEndX = mLeftProgressBarStartX + horizontalProgressPx;
            mProgressRightEndX = mRightProgressBarStartX;
        } else {
            mProgressRightEndX = mRightProgressBarStartX + horizontalProgressPx;
            mProgressLeftEndX = mLeftProgressBarStartX;
        }

        float verticalProgressPx = mStickMaxProgressPx * vertical / 100.0f;
        if (verticalProgressPx < 0) {
            mProgressBottomEndY = mBottomProgressBarStartY - verticalProgressPx;
            mProgressTopEndY = mTopProgressBarStartY;
        } else {
            mProgressTopEndY = mTopProgressBarStartY - verticalProgressPx;
            mProgressBottomEndY = mBottomProgressBarStartY;
        }
        mHorizontalProgress = horizontal;
        mVerticalProgress = vertical;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mStickBitmap, null, mStickRect, mStickBitmapPaint);
        drawProgressBackground(canvas);
        drawProgress(canvas);

        drawText(canvas);
    }

    private void drawText(Canvas canvas) {
        int absHorizontalProgress = (int) Math.abs(mHorizontalProgress);
        if (mHorizontalProgress > 0) {
            drawCenterText(canvas, absHorizontalProgress + "%", mRightTextRect);
            drawCenterText(canvas, "0%", mLeftTextRect);
        } else {
            drawCenterText(canvas, absHorizontalProgress + "%", mLeftTextRect);
            drawCenterText(canvas, "0%", mRightTextRect);
        }
        int absVerticalProgress = (int) Math.abs(mVerticalProgress);
        if (mVerticalProgress > 0) {
            drawCenterText(canvas, absVerticalProgress + "%", mTopTextRect);
            drawCenterText(canvas, "0%", mBottomTextRect);
        } else {
            drawCenterText(canvas, absVerticalProgress + "%", mBottomTextRect);
            drawCenterText(canvas, "0%", mTopTextRect);
        }
    }

    private void drawProgress(Canvas canvas) {
        if (Math.abs(mTopProgressBarStartY - mProgressTopEndY) > PROGRESS_DRAW_THRESHOLD) {
            canvas.drawLine(mTopProgressBarStartX, mTopProgressBarStartY, mTopProgressBarEndX, mProgressTopEndY, mVerticalProgressPaint);
        }
        if (Math.abs(mRightProgressBarStartX - mProgressRightEndX) > PROGRESS_DRAW_THRESHOLD) {
            canvas.drawLine(mRightProgressBarStartX, mRightProgressBarStartY, mProgressRightEndX, mRightProgressBarEndY, mHorizontalProgressPaint);
        }
        if (Math.abs(mBottomProgressBarStartY - mProgressBottomEndY) > PROGRESS_DRAW_THRESHOLD) {
            canvas.drawLine(mBottomProgressBarStartX, mBottomProgressBarStartY, mBottomProgressBarEndX, mProgressBottomEndY, mVerticalProgressPaint);
        }
        if (Math.abs(mLeftProgressBarStartX - mProgressLeftEndX) > PROGRESS_DRAW_THRESHOLD) {
            canvas.drawLine(mLeftProgressBarStartX, mLeftProgressBarStartY, mProgressLeftEndX, mLeftProgressBarEndY, mHorizontalProgressPaint);
        }
    }

    private void drawProgressBackground(Canvas canvas) {
        canvas.drawLine(mTopProgressBarStartX, mTopProgressBarStartY, mTopProgressBarEndX, mTopProgressBarEndY, mProgressBackgroundPaint);
        canvas.drawLine(mRightProgressBarStartX, mRightProgressBarStartY, mRightProgressBarEndX, mRightProgressBarEndY, mProgressBackgroundPaint);
        canvas.drawLine(mBottomProgressBarStartX, mBottomProgressBarStartY, mBottomProgressBarEndX, mBottomProgressBarEndY, mProgressBackgroundPaint);
        canvas.drawLine(mLeftProgressBarStartX, mLeftProgressBarStartY, mLeftProgressBarEndX, mLeftProgressBarEndY, mProgressBackgroundPaint);
    }

    private Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private void drawCenterText(Canvas canvas, String text, Rect rect) {
        Paint.FontMetricsInt fontMetrics = mFontPaint.getFontMetricsInt();
        int baseline = (rect.bottom + rect.top - fontMetrics.bottom - fontMetrics.top) / 2;
        canvas.drawText(text, rect.centerX(), baseline, mFontPaint);
    }
}
