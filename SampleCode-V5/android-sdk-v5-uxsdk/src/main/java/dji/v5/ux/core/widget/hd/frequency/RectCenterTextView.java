package dji.v5.ux.core.widget.hd.frequency;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import dji.v5.ux.R;
import dji.v5.ux.core.base.BaseView;
public class RectCenterTextView extends BaseView {

    private String mAverageVal = "-70dBm";
    private float mPosX = 0;

    private Paint mPaint;
    private float mTxtSize = 30;

    private float mMarginLeft = 0;

    public RectCenterTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTxtSize = getResources().getDimension(R.dimen.uxsdk_text_size_small);
        mPaint = new Paint();
        mPaint.setColor(getResources().getColor(android.R.color.white));
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(mTxtSize);

        mMarginLeft = getResources().getDimension(R.dimen.uxsdk_setting_ui_hd_sdr_chart_left_axis_width);
    }

    public void setCenterAverageText(String val) {
        mAverageVal = val;
        postInvalidate();
    }

    /**
     *
     * @param pos
     * @param isRectDragging 如果处于拽托状态, 要使这个view消失
     */
    public void setCenterPos(float pos, boolean isRectDragging) {
        mPosX = pos;
        if(isRectDragging && getVisibility() != INVISIBLE) {
            setVisibility(INVISIBLE);
        } else if(!isRectDragging && getVisibility() != VISIBLE) {
            setVisibility(VISIBLE);
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // offset处理居中
        float offset = mTxtSize / 4.0f * mAverageVal.length();
        canvas.drawText(mAverageVal, mMarginLeft + mPosX - offset, mTxtSize, mPaint);
    }
}
