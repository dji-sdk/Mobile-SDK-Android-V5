package dji.v5.ux.core.base;
/*
 * Copyright (c) 2017, DJI All Rights Reserved.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import dji.v5.utils.common.AndUtil;
import dji.v5.ux.R;


public class DividerConstraintLayout extends ConstraintLayout implements DividerLayout{

    private boolean mTopDividerEnable;
    private boolean mBottomDividerEnable;
    private int mTopDividerColor;
    private int mBottomDividerColor;
    private int mTopDividerHeight;
    private int mBottomDividerHeight;
    private int mTopDividerMarginLeft;
    private int mBottomDividerMarginLeft;
    private Paint sPaint = new Paint();
    
    public DividerConstraintLayout(Context context) {
        this(context, null);
    }

    public DividerConstraintLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DividerConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs);
    }

    private void initialize(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DividerConstraintLayout);
        mTopDividerEnable = a.getBoolean(R.styleable.DividerConstraintLayout_uxsdk_topDividerEnable, false);
        mBottomDividerEnable = a.getBoolean(R.styleable.DividerConstraintLayout_uxsdk_bottomDividerEnable, true);
        int defColor = ContextCompat.getColor(getContext(), R.color.uxsdk_white_10_percent);
        mTopDividerColor = a.getColor(R.styleable.DividerConstraintLayout_uxsdk_topDividerColor, defColor);
        mBottomDividerColor = a.getColor(R.styleable.DividerConstraintLayout_uxsdk_bottomDividerColor, defColor);
        mTopDividerHeight = a.getDimensionPixelSize(R.styleable.DividerConstraintLayout_uxsdk_topDividerHeight, AndUtil.dip2px(context, 1));
        mBottomDividerHeight = a.getDimensionPixelSize(R.styleable.DividerConstraintLayout_uxsdk_bottomDividerHeight, AndUtil.dip2px(context, 1));
        mTopDividerMarginLeft = a.getDimensionPixelSize(R.styleable.DividerConstraintLayout_uxsdk_topDividerMarginLeft, 0);
        mBottomDividerMarginLeft = a.getDimensionPixelSize(R.styleable.DividerConstraintLayout_uxsdk_bottomDividerMarginLeft, 0);
        sPaint.setColor(mBottomDividerColor);
        a.recycle();
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBottomDividerEnable) {
            sPaint.setColor(mBottomDividerColor);
            canvas.drawRect(mBottomDividerMarginLeft, (float)getHeight() - mBottomDividerHeight, getWidth(), getHeight(), sPaint);
        }
        if (mTopDividerEnable) {
            sPaint.setColor(mTopDividerColor);
            canvas.drawRect(mTopDividerMarginLeft, 0, getWidth(), mTopDividerHeight, sPaint);
        }
    }

    @Override
    public void setTopDividerEnable(boolean enabled) {
        mTopDividerEnable = enabled;
        invalidate();
    }

    @Override
    public void setBottomDividerEnable(boolean enabled) {
        mBottomDividerEnable = enabled;
        invalidate();
    }

    @Override
    public void setTopDividerColor(int color) {
        //do nothing
    }

    @Override
    public void setBottomDividerColor(int color) {
        mBottomDividerColor = color;
        invalidate();
    }

    @Override
    public void setTopDividerHeight(int height) {
        //do nothing
    }

    @Override
    public void setBottomDividerHeight(int height) {
        mBottomDividerHeight = height;
        invalidate();
    }

    @Override
    public void setTopMarginLeft(int marginLeft) {
        mTopDividerMarginLeft = marginLeft;
        invalidate();
    }

    @Override
    public void setBottomMarginLeft(int marginLeft) {
        mBottomDividerMarginLeft = marginLeft;
        invalidate();
    }
}
