package dji.v5.ux.core.widget.hd.frequency;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import java.util.Locale;

import dji.v5.ux.R;
import dji.v5.ux.core.base.BaseView;
public class FreqRangeTextView extends BaseView {

    private float mMinValue = 0;
    private float mMaxValue = 0;
    private float mPosMinX = 0;
    private float mPosMaxX = 0;

    private float mMarginLeft = 0;

    private Paint mPaint;
    private float mTxtSize = 30;
    // 数字的个数, 如'1'为1个, '1257'为4个
    private int mTxtNum = 6;

    public FreqRangeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTxtSize = getResources().getDimension(R.dimen.uxsdk_text_size_small);
        mPaint = new Paint();
        mPaint.setColor(getResources().getColor(android.R.color.white));
        mPaint.setTextSize(mTxtSize);
        mPaint.setAntiAlias(true);

        mMarginLeft = getResources().getDimension(R.dimen.uxsdk_setting_ui_hd_sdr_chart_left_axis_width);
    }

    public void setMinMaxValue(float _min, float _max, float posMin, float posMax) {
        mMinValue = _min;
        mMaxValue = _max;
        mPosMinX = posMin;
        mPosMaxX = posMax;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // offset处理居中
        float offset = mTxtSize / 4.0f * mTxtNum;
        // 初始化offset之后再+-mTxtSize / 2, 是让文字往两边靠, 让两个文字的间隙变大(10M带宽下可能挨在一起)
        canvas.drawText(String.format(Locale.US, "%.1f", mMinValue), mMarginLeft + mPosMinX - offset - mTxtSize / 2, mTxtSize * 1.5f, mPaint);
        canvas.drawText(String.format(Locale.US, "%.1f", mMaxValue), mMarginLeft + mPosMaxX - offset + mTxtSize / 2, mTxtSize * 1.5f, mPaint);

    }
}
