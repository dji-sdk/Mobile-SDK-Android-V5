package dji.v5.ux.core.widget.hd.frequency;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import dji.v5.ux.R;
import dji.v5.ux.core.base.BaseView;

/**
 * @description : 曲线图左边的纵坐标数字
 */
public class ChartLeftYAxisView extends BaseView {

    private int[] mAxisValues = new int[]{-70, -90, -110};
    private Paint mPaint;
    private float mTxtSize;
    private float mChartHeight;

    private float mXOffset = 0;
    private float mYOffset = 0;

    public ChartLeftYAxisView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTxtSize = getResources().getDimension(R.dimen.uxsdk_text_size_small);
        mPaint = new Paint();
        mPaint.setColor(getResources().getColor(android.R.color.white));
        mPaint.setTextSize(mTxtSize);
        mPaint.setAntiAlias(true);

        mChartHeight = getResources().getDimension(R.dimen.uxsdk_setting_ui_hd_sdr_chart_height);
        mYOffset = mTxtSize / 4f;
        mXOffset = (int) getResources().getDimension(R.dimen.uxsdk_4_dp);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawText(String.valueOf(mAxisValues[0]), mXOffset, mChartHeight / 5 + mYOffset, mPaint);
        canvas.drawText(String.valueOf(mAxisValues[1]), mXOffset, mChartHeight / 5 * 3 + mYOffset, mPaint);
        canvas.drawText(String.valueOf(mAxisValues[2]), mXOffset, mChartHeight + mYOffset, mPaint);

    }
}
