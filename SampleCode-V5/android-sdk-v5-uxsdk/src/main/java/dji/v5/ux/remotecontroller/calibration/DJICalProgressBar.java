package dji.v5.ux.remotecontroller.calibration;
/*
 *   WWWWWW||WWWWWW
 *    W W W||W W W
 *         ||
 *       ( OO )__________
 *        /  |           \
 *       /o o|    DJI     \
 *       \___/||_||__||_|| **
 *            || ||  || ||
 *           _||_|| _||_||
 *          (__|__|(__|__|
 *
 * Copyright (c) 2017, DJI All Rights Reserved.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import dji.v5.ux.R;
import dji.v5.ux.core.ui.BaseView;


/**
 * <p>Description:</p>
 *
 * @author create by Ken.Tian
 * 2018/7/12
 * @version v1.0
 *
 * Copy From DJIG04
 */
public class DJICalProgressBar extends BaseView {

    private int mLineWidth = 2;
    private int mDividerWidth = 2;

    private int mFontSize = 12;

    private int orientation = 0;

    private int mBgColor = Color.parseColor("#727272");
    private int mColor = Color.parseColor("#00d8ff");

    private Paint mPaint;

    private int mLeft = 20;
    private int mRight = 70;

    private Paint mFontPaint;

    public DJICalProgressBar(Context context) {
        this(context, null);
    }

    public DJICalProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DJICalProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttributeSet(context, attrs);
        init();
    }

    private void initAttributeSet(Context context, @Nullable AttributeSet attrs) {
        if (attrs == null) {
            orientation = 0;
            return;
        }
        TypedArray ar = context.obtainStyledAttributes(attrs, R.styleable.DJICalProgressBar);
        orientation = ar.getInt(R.styleable.DJICalProgressBar_uxsdk_orientation_sb, 0);
        ar.recycle();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public DJICalProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttributeSet(context, attrs);
        init();
    }

    private void init() {
        mFontSize = (int)getResources().getDimension(R.dimen.uxsdk_rc_cal_progress_font);
        mLineWidth = (int)getResources().getDimension(R.dimen.uxsdk_rc_cal_progress_line_width);
        mDividerWidth = (int)getResources().getDimension(R.dimen.uxsdk_rc_cal_progress_line_width);

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);
        // 消除锯齿
        mPaint.setAntiAlias(true);
        // 设置画笔的颜色
        mPaint.setColor(mColor);
        // 设置paint的外框宽度
        mPaint.setStrokeWidth(0);

        mFontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFontPaint.setStrokeWidth(0);
        mFontPaint.setTextSize(mFontSize);
        mFontPaint.setColor(Color.WHITE);
        mFontPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setValue(int left, int right) {
        mLeft = left;
        mRight = right;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        //画背景
        mPaint.setColor(mBgColor);
        if (orientation == 1) {
            canvas.drawRect(new Rect((w - mLineWidth) / 2, 0, (w - mLineWidth) / 2 + mLineWidth, h), mPaint);
        } else {
            canvas.drawRect(new Rect(0, (h - mLineWidth) / 2, w, (h - mLineWidth) / 2 + mLineWidth), mPaint);
        }

        //画左右两条进度
        mPaint.setColor(mColor);
        if (orientation == 1) {
            canvas.drawRect(new Rect((w - mLineWidth) / 2, h / 2, (w - mLineWidth) / 2 + mLineWidth, h / 2 + (h / 2) * mLeft / 100), mPaint);
            canvas.drawRect(new Rect((w - mLineWidth) / 2, (h / 2) * (100 - mRight) / 100, (w - mLineWidth) / 2 + mLineWidth, h / 2), mPaint);
        } else {
            canvas.drawRect(new Rect( (w / 2) * (100 - mLeft) / 100, (h - mLineWidth) / 2, w / 2, (h - mLineWidth) / 2 + mLineWidth), mPaint);
            canvas.drawRect(new Rect(w / 2, (h - mLineWidth) / 2, w / 2 + (w / 2) * mRight / 100, (h - mLineWidth) / 2 + mLineWidth), mPaint);
        }

        //画中间的分隔白线
        mPaint.setColor(Color.WHITE);
        if (orientation == 1) {
            canvas.drawRect(new Rect(
                (w - mLineWidth) / 2, (h - mDividerWidth) / 2, (w - mLineWidth) / 2 + mLineWidth, (h - mDividerWidth) / 2 + mDividerWidth), mPaint);
        } else {
            canvas.drawRect(new Rect(
                (w - mDividerWidth) / 2, (h - mLineWidth) / 2, (w - mDividerWidth) / 2 + mDividerWidth, (h - mLineWidth) / 2 + mLineWidth), mPaint);
        }


        //画百分比
/*        int percent = 0;
        if(mLeft > 0) {
            percent = mLeft;
        } else {
            percent = mRight;
        }
        drawCenterText(canvas, percent + "%", new Rect(0, 0, w, h));*/

    }
    /*
    private void drawCenterText(Canvas canvas, String text, Rect rect) {
        //FontMetricsInt fontMetrics = mFontPaint.getFontMetricsInt();
        //int baseline = (rect.bottom + rect.top - fontMetrics.bottom - fontMetrics.top) / 2;
        // 下面这行是实现水平居中，drawText对应改为传入targetRect.centerX()
        canvas.drawText(text, rect.centerX(), 0, mFontPaint);
    }*/

}
