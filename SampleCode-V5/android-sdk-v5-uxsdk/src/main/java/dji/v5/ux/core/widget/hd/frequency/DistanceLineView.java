package dji.v5.ux.core.widget.hd.frequency;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import dji.v5.ux.R;
import dji.v5.ux.core.base.BaseView;

public class DistanceLineView extends BaseView {

    // 1km距离对应的nf值
    private int mFirstLineNfValue = -91;
    // 4km距离对应nf值
    private int mSecondLineNfValue = -103;

    private float mFirstLinePos = 0;
    private float mSecondLinePos = 0;

    private Paint mPaint;
    private float mChartHeight;

    public DistanceLineView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPaint = new Paint();
        mPaint.setColor(getResources().getColor(android.R.color.darker_gray));
        mPaint.setStrokeWidth(1);
        mPaint.setAlpha(125);
        mPaint.setAntiAlias(true);
        mChartHeight = getResources().getDimension(R.dimen.uxsdk_setting_ui_hd_sdr_chart_height);

        mFirstLinePos = mChartHeight * (FreqView.LINE_MAX_VALUE - (mFirstLineNfValue - FreqView.NF_BASE_VALUE)) / FreqView.LINE_MAX_VALUE;
        mSecondLinePos = mChartHeight * (FreqView.LINE_MAX_VALUE - (mSecondLineNfValue - FreqView.NF_BASE_VALUE)) / FreqView.LINE_MAX_VALUE;
    }

    /**
     * 设置1km线的位置
     * @param _val
     */
    public void set1KmNfValue(int _val) {
        mFirstLineNfValue = _val;
        mSecondLineNfValue = mFirstLineNfValue - 12;
        mFirstLinePos = mChartHeight * (FreqView.LINE_MAX_VALUE - (mFirstLineNfValue - FreqView.NF_BASE_VALUE)) / FreqView.LINE_MAX_VALUE;
        mSecondLinePos = mChartHeight * (FreqView.LINE_MAX_VALUE - (mSecondLineNfValue - FreqView.NF_BASE_VALUE)) / FreqView.LINE_MAX_VALUE;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawLine(0, mFirstLinePos,
                canvas.getWidth(), mFirstLinePos, mPaint);

        canvas.drawLine(0, mSecondLinePos,
                canvas.getWidth(), mSecondLinePos, mPaint);
    }
}
