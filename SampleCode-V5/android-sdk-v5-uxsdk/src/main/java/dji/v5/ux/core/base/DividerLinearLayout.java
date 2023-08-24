package dji.v5.ux.core.base;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import dji.v5.utils.common.AndUtil;
import dji.v5.ux.R;


public class DividerLinearLayout extends LinearLayout implements DividerLayout{

    private boolean mTopDividerEnable;
    private boolean mBottomDividerEnable;
    private int mBottomDividerColor;
    private int mBottomDividerHeight;
    private int mTopDividerMarginLeft;
    private int mBottomDividerMarginLeft;
    private Paint sPaint = new Paint();

    public DividerLinearLayout(Context context) {
        this(context, null);
    }

    public DividerLinearLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DividerLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs);
    }

    @SuppressWarnings("unused")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DividerLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs);
    }

    private void initialize(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DividerLinearLayout);
        mTopDividerEnable = a.getBoolean(R.styleable.DividerLinearLayout_uxsdk_topDividerEnable, false);
        mBottomDividerEnable = a.getBoolean(R.styleable.DividerLinearLayout_uxsdk_bottomDividerEnable, true);
        int defColor = getResources().getColor(R.color.uxsdk_dic_color_c20_divider);
        mBottomDividerColor = a.getColor(R.styleable.DividerLinearLayout_uxsdk_bottomDividerColor, defColor);
        mBottomDividerHeight = a.getDimensionPixelSize(R.styleable.DividerLinearLayout_uxsdk_bottomDividerHeight, AndUtil.dip2px(context, 1));
        mTopDividerMarginLeft = a.getDimensionPixelSize(R.styleable.DividerLinearLayout_uxsdk_topDividerMarginLeft, 0);
        mBottomDividerMarginLeft = a.getDimensionPixelSize(R.styleable.DividerLinearLayout_uxsdk_bottomDividerMarginLeft, 0);
        sPaint.setColor(mBottomDividerColor);
        a.recycle();
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBottomDividerEnable) {
            canvas.drawRect(mBottomDividerMarginLeft, getHeight() - (float)mBottomDividerHeight, getWidth(), getHeight(), sPaint);
        }
        if (mTopDividerEnable) {
            canvas.drawRect(mTopDividerMarginLeft, 0, getWidth(), mBottomDividerHeight, sPaint);
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
        //add log
    }

    @Override
    public void setBottomDividerColor(int color) {
        mBottomDividerColor = color;
        invalidate();
    }

    @Override
    public void setTopDividerHeight(int height) {
        //add log
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
