package dji.v5.ux.core.widget.hd.frequency;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import dji.v5.ux.R;
import dji.v5.ux.core.base.BaseView;

/**
 * @description : sdr图标右边所写的文字
 */
public class ChartRightYAxisView extends BaseView {

    private String[] mDistanceUnitStr = new String[]{"≈1km", "≈4km", "MHz"};
    // 1km距离对应的nf值
    private int mFirstLineNfValue = -91;
    // 4km距离对应nf值
    private int mSecondLineNfValue = -103;

    private float mFirstLinePos = 0;
    private float mSecondLinePos = 0;

    private Paint mPaint;
    private float mSmallTxtSize;
    private float mNormalTxtSize;
    private float mChartHeight;

    private float mXOffset = 0;
    private float mYOffset = 0;

    public ChartRightYAxisView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mSmallTxtSize = getResources().getDimension(R.dimen.uxsdk_12_dp);
        mNormalTxtSize = getResources().getDimension(R.dimen.uxsdk_14_dp);
        mPaint = new Paint();
        mPaint.setColor(getResources().getColor(android.R.color.white));
        mPaint.setTextSize(mSmallTxtSize);

        mChartHeight = getResources().getDimension(R.dimen.uxsdk_setting_ui_hd_sdr_chart_height);
        mYOffset = mSmallTxtSize / 4f;
        mXOffset = (int) getResources().getDimension(R.dimen.uxsdk_4_dp);

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

        mPaint.setColor(getResources().getColor(android.R.color.white));
        mPaint.setTextSize(mSmallTxtSize);
        canvas.drawText(String.valueOf(mDistanceUnitStr[0]), mXOffset, mFirstLinePos + mYOffset, mPaint);
        canvas.drawText(String.valueOf(mDistanceUnitStr[1]), mXOffset, mSecondLinePos + mYOffset, mPaint);

        mPaint.setColor(getResources().getColor(android.R.color.darker_gray));
        mPaint.setTextSize(mNormalTxtSize);
        canvas.drawText(String.valueOf(mDistanceUnitStr[2]), mXOffset, mChartHeight + mYOffset, mPaint);
    }
}
