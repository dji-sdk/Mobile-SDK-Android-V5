package dji.v5.ux.core.ui.hsi.dashboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;

import java.util.Locale;

import androidx.annotation.Nullable;
import dji.v5.ux.R;
import dji.v5.ux.core.util.UnitUtils;
import dji.v5.ux.core.widget.hsi.AttitudeDisplayModel;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class AttitudeDashBoard extends ScrollableAttributeDashBoard {

    private static final String TAG = "AttitudeDashBoard";

    private static final String MEASURE_TEXT = "0.1";
    private static final String RTH_TEXT = "RTH";
    private static final int UPWARD = -1;
    private static final int DOWNWARD = 1;

    private static final int PREDICT_TIME_IN_SECONDS = 6;

    private static final int BARRIER_DISTANCE_TEXT_ALIGN_VERTICAL = 0;

    private static final int BARRIER_DISTANCE_TEXT_ALIGN_HORIZONTAL = 1;

    /**
     * 障碍物最远感知距离
     */
    private static final int DEFAULT_MAX_PERCEPTION_DISTANCE_IN_METER = 45;

    private final int mAvoidanceMaxHeight;
    private final int mBarrierDistanceTextSize;
    private final int mBarrierIndicatorColor;
    private final int mBarrierIndicatorHeight;
    private final int mBarrierIndicatorWidth;
    private final int mAvoidanceIndicatorColor;
    private final int mAvoidanceIndicatorWidth;
    private final int mAvoidanceIndicatorStrokeWidth;
    private final int mAvoidanceIndicatorStrokeColor;
    private final int mMaxPerceptionDistanceInMeter;
    private final int mDistancePredictWidth;
    private final int mBarrierDistanceTextAlign;
    private final int mReturnToHomeIndicatorColor;
    private final int mReturnToHomeIndicatorWidth;
    private final int mMaxFlightHeightIndicatorColor;
    private final int mMaxFlightHeightIndicatorWidth;
    private final int mMaxFlightHeightIndicatorHeight;

    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    /**
     * 上视障碍物距离
     */
    private float mUVisionDistance;
    /**
     * 用户设置的上视障碍物避障距离
     */
    private float mUserSetUpBarrierAvoidanceDistance;
    /**
     * 用户设置的上视障碍物告警距离
     */
    private float mUserSetUpBarrierWarnDistance;

    /**
     * 下视障碍物距离
     */
    private float mDownVisionDistance;
    /**
     * 用户设置的下视障碍物避障距离
     */
    private float mUserSetDownBarrierAvoidanceDistance;
    /**
     * 用户设置的下视障碍物告警距离
     */
    private float mUserSetDownBarrierWarnDistance;

    /**
     * 垂直速度
     */
    private float mSpeedZ;

    /**
     * 当前飞行高度
     */
    private float mHeight;

    /**
     * 返航高度
     */
    private int mReturnToHomeHeight = Integer.MAX_VALUE;

    /**
     * 飞控设置中的限飞高度
     */
    private int mLimitHeight = Integer.MAX_VALUE;

    /**
     * 视觉感知避障开关
     */
    private boolean mUpwardVisualEnable;

    private boolean mShowDownwardVisualPerceptionInfo = false;

    /**
     * 雷达感知避障开关
     */
    private boolean mUpwardRadarEnable;

    private float mUpwardRadarDistance;

    private AttitudeDisplayModel widgetModel;

    public void setModel(AttitudeDisplayModel model) {
        widgetModel = model;
    }

    public AttitudeDashBoard(Context context) {
        this(context, null);
    }

    public AttitudeDashBoard(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AttitudeDashBoard(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AttitudeDashBoard);
        mAvoidanceMaxHeight = typedArray.getDimensionPixelSize(R.styleable.AttitudeDashBoard_uxsdk_avoidance_max_height,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_45_dp));
        mBarrierDistanceTextSize = typedArray.getDimensionPixelSize(R.styleable.AttitudeDashBoard_uxsdk_barrier_distance_text_size,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_text_size_normal));
        mBarrierIndicatorHeight = typedArray.getDimensionPixelSize(R.styleable.AttitudeDashBoard_uxsdk_barrier_indicator_height,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_2_dp));
        mBarrierIndicatorWidth = typedArray.getDimensionPixelSize(R.styleable.AttitudeDashBoard_uxsdk_barrier_indicator_width,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_13_dp));
        mAvoidanceIndicatorWidth = typedArray.getDimensionPixelSize(R.styleable.AttitudeDashBoard_uxsdk_avoidance_indicator_width,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_5_dp));
        mAvoidanceIndicatorStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.AttitudeDashBoard_uxsdk_avoidance_indicator_stroke_width,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_1_dp));
        mDistancePredictWidth = typedArray.getDimensionPixelSize(R.styleable.AttitudeDashBoard_uxsdk_distance_predict_line_width,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_2_dp));
        mReturnToHomeIndicatorWidth = typedArray.getDimensionPixelSize(R.styleable.AttitudeDashBoard_uxsdk_return_to_home_indicator_width,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_26_dp));
        mReturnToHomeIndicatorColor = typedArray.getColor(R.styleable.AttitudeDashBoard_uxsdk_return_to_home_indicator_color,
                getResources().getColor(R.color.uxsdk_pfd_avoidance_color));
        mMaxFlightHeightIndicatorWidth = typedArray.getDimensionPixelSize(R.styleable.AttitudeDashBoard_uxsdk_max_flight_height_indicator_width,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_26_dp));
        mMaxFlightHeightIndicatorHeight = typedArray.getDimensionPixelSize(R.styleable.AttitudeDashBoard_uxsdk_max_flight_height_indicator_height,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_2_dp));
        mMaxFlightHeightIndicatorColor = typedArray.getColor(R.styleable.AttitudeDashBoard_uxsdk_max_flight_height_indicator_color,
                getResources().getColor(R.color.uxsdk_pfd_barrier_color));
        mBarrierIndicatorColor = typedArray.getColor(R.styleable.AttitudeDashBoard_uxsdk_barrier_indicator_color,
                getResources().getColor(R.color.uxsdk_pfd_barrier_color));
        mAvoidanceIndicatorColor = typedArray.getColor(R.styleable.AttitudeDashBoard_uxsdk_avoidance_indicator_color,
                getResources().getColor(R.color.uxsdk_pfd_avoidance_color));
        mAvoidanceIndicatorStrokeColor = typedArray.getColor(R.styleable.AttitudeDashBoard_uxsdk_avoidance_indicator_stroke_color,
                getResources().getColor(R.color.uxsdk_pfd_main_color));
        mMaxPerceptionDistanceInMeter = typedArray.getInt(R.styleable.AttitudeDashBoard_uxsdk_max_perception_distance_in_meter,
                DEFAULT_MAX_PERCEPTION_DISTANCE_IN_METER);
        mBarrierDistanceTextAlign = typedArray.getInt(R.styleable.AttitudeDashBoard_uxsdk_barrier_distance_text_align,
                BARRIER_DISTANCE_TEXT_ALIGN_HORIZONTAL);
        mUpwardRadarDistance = mUVisionDistance = mDownVisionDistance = typedArray.getInt(R.styleable.AttitudeDashBoard_uxsdk_barrier_distance, Integer.MAX_VALUE);
        typedArray.recycle();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isInEditMode()) {
            return;
        }

        // 上视障碍物距离
        mCompositeDisposable.add(widgetModel.getOmniPerceptionRadarUpDistanceProcessor().toFlowable().subscribe(distance -> {
            mUVisionDistance = distance.getUpDist() * 1f / 1000;
            updateWidget();
        }));

        // 下视障碍物距离
        mCompositeDisposable.add(widgetModel.getOmniPerceptionRadarDownDistanceProcessor().toFlowable().subscribe(distance -> {
            mDownVisionDistance = distance.getDownDist() * 1f / 1000;
            updateWidget();
        }));

        //用户设置的上视障碍物避障距离
        mCompositeDisposable.add(widgetModel.getUpwardsAvoidanceDistanceProcessor().toFlowable().subscribe(value -> {
            mUserSetUpBarrierAvoidanceDistance = value.floatValue();
            updateWidget();
        }));

        //用户设置的下视障碍物避障距离
        mCompositeDisposable.add(widgetModel.getDownwardsAvoidanceDistanceProcessor().toFlowable().subscribe(value -> {
            mUserSetDownBarrierAvoidanceDistance = value;
            updateWidget();
        }));

        //用户设置的上视障碍物告警距离
        mCompositeDisposable.add(widgetModel.getOmniUpRadarDistanceProcessor().toFlowable().subscribe(value -> {
            mUserSetUpBarrierWarnDistance = value.floatValue();
            updateWidget();
        }));

        //用户设置的下视障碍物告警距离
        mCompositeDisposable.add(widgetModel.getOmniDownRadarDistanceProcessor().toFlowable().subscribe(value -> {
            mUserSetDownBarrierWarnDistance = value.floatValue();
        }));

        //视觉感知避障开关
        mCompositeDisposable.add(widgetModel.getOmniUpwardsObstacleAvoidanceEnabledProcessor().toFlowable().subscribe(aBoolean -> {
            mUpwardVisualEnable = aBoolean;
            updateWidget();
        }));

        mCompositeDisposable.add(widgetModel.getVelocityProcessor().toFlowable().subscribe(velocity -> {
            mSpeedZ = velocity.getZ().floatValue();
            updateWidget();
        }));
        mCompositeDisposable.add(widgetModel.getAltitudeProcessor().toFlowable().subscribe(altitude -> {
            mHeight = altitude.floatValue();
            setCurrentValue(mHeight);
        }));
        mCompositeDisposable.add(widgetModel.getGoHomeHeightProcessor().toFlowable().subscribe(integer -> {
            mReturnToHomeHeight = integer;
            updateWidget();
        }));
        mCompositeDisposable.add(widgetModel.getLimitMaxFlightHeightInMeterProcessor().toFlowable().subscribe(height -> {
            mLimitHeight = height;
            updateWidget();
        }));

        mCompositeDisposable.add(widgetModel.getLandingProtectionEnabledProcessor().toFlowable().subscribe(aBoolean -> {
            mShowDownwardVisualPerceptionInfo = aBoolean;
            updateWidget();
        }));

        //雷达感知避障开关
        mCompositeDisposable.add(widgetModel.getRadarUpwardsObstacleAvoidanceEnabledProcessor().toFlowable().subscribe(aBoolean -> {
            mUpwardRadarEnable = aBoolean;
        }));

        mCompositeDisposable.add(widgetModel.getRadarObstacleAvoidanceStateProcessor().toFlowable().subscribe(perceptionInformation -> {
            mUpwardRadarDistance = perceptionInformation.getUpwardObstacleDistance() * 1f / 1000;
            updateWidget();
        }));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        //mKeyPresenter.onStop();
        mCompositeDisposable.dispose();
    }

    @Override
    protected int getMinHeight() {
        return Math.max(getFrameworkHeight(), getAvoidanceHeight() * 2);
    }

    private int getAvoidanceHeight() {
        mPaint.setTextSize(mBarrierDistanceTextSize);
        mPaint.setTextAlign(Paint.Align.LEFT);
        mPaint.getTextBounds(MEASURE_TEXT, 0, MEASURE_TEXT.length(), RECT);
        return mAvoidanceMaxHeight + mBarrierIndicatorHeight + RECT.height() * 3 / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.translate(mFrameworkPaddingStart, (float) getHeight() / 2);
        drawDistancePredict(canvas);
        if (isShowFramework()) {
            drawReturnToHomeIndicator(canvas);
            drawMaxFlightHeightIndicator(canvas);
        }
        drawBarrierInfo(canvas);
        canvas.restore();
    }

    private void drawMaxFlightHeightIndicator(Canvas canvas) {
        float current = getCurrentValue();
        float offset = mLimitHeight - current;
        float ratio = (float) getFrameworkHeight() / mVisibleCalibrationUnitCount / mAttributeOffsetPerUnit;
        float y = offset * ratio;
        if (Math.abs(y) >= (float) getFrameworkHeight() / 2) {
            return;
        }
        canvas.save();
        float calibrationHorizontalMargin = (float) (mPointerLineInnerWidth - mDegreeLineShortWidth) / 2;
        mPaint.setStrokeWidth(mFrameworkStrokeWidth);
        mPaint.setColor(mMaxFlightHeightIndicatorColor);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawLine(calibrationHorizontalMargin, -y, calibrationHorizontalMargin + mMaxFlightHeightIndicatorWidth, -y, mPaint);
        canvas.drawLine(calibrationHorizontalMargin + mMaxFlightHeightIndicatorWidth, -y, calibrationHorizontalMargin + mMaxFlightHeightIndicatorWidth, -y + mMaxFlightHeightIndicatorHeight, mPaint);
        canvas.restore();
    }

    private void drawReturnToHomeIndicator(Canvas canvas) {
        float current = getCurrentValue();
        float offset = mReturnToHomeHeight - current;
        float ratio = (float) getFrameworkHeight() / mVisibleCalibrationUnitCount / mAttributeOffsetPerUnit;
        float y = offset * ratio;
        if (Math.abs(y) >= (float) getFrameworkHeight() / 2) {
            return;
        }
        canvas.save();
        float calibrationHorizontalMargin = (float) (mPointerLineInnerWidth - mDegreeLineShortWidth) / 2;

        mPaint.setStrokeWidth(mFrameworkStrokeWidth);
        mPaint.setColor(mReturnToHomeIndicatorColor);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize(mBarrierDistanceTextSize);
        mPaint.setTypeface(Typeface.DEFAULT);
        mPaint.getTextBounds(RTH_TEXT, 0, RTH_TEXT.length(), RECT);
        Paint.FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
        canvas.drawLine(calibrationHorizontalMargin, -y, calibrationHorizontalMargin + mReturnToHomeIndicatorWidth, -y, mPaint);
        mPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(RTH_TEXT, calibrationHorizontalMargin * 2 + mReturnToHomeIndicatorWidth,
                -y + (float) (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom, mPaint);
        canvas.restore();
    }

    private void drawDistancePredict(Canvas canvas) {
        canvas.save();
        float predictDistance = PREDICT_TIME_IN_SECONDS * mSpeedZ;
        float ratio = (float) mAvoidanceMaxHeight / mMaxPerceptionDistanceInMeter;
        float predictArea = Math.abs(predictDistance * ratio);

        mPaint.setColor(Color.WHITE);
        canvas.translate((float) mFrameworkStrokeWidth / 2, 0);
        int top, bottom;
        if (predictDistance < 0) {
            top = -(int) predictArea;
            bottom = 0;
        } else {
            bottom = (int) predictArea;
            top = 0;
        }
        RECT.set(0, top, mDistancePredictWidth, bottom);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(RECT, mPaint);
        canvas.restore();
    }

    private void drawBarrierInfo(Canvas canvas) {
        if (mUVisionDistance == Integer.MAX_VALUE
                && mUpwardRadarDistance == Integer.MAX_VALUE
                && mDownVisionDistance == Integer.MAX_VALUE) {
            return;
        }
        canvas.save();
        canvas.translate(-(float) mFrameworkStrokeWidth * 3 / 2, 0);
        // 绘制上视障碍物的信息
        if ((mUVisionDistance <= mMaxPerceptionDistanceInMeter
                && mUpwardVisualEnable)
                || (mUpwardRadarEnable
                && mUpwardRadarDistance <= mMaxPerceptionDistanceInMeter)) {
            canvas.save();
            drawBarrierInfo(canvas, UPWARD);
            canvas.restore();
        }
        // 绘制下视障碍物的信息
        if (mDownVisionDistance <= mMaxPerceptionDistanceInMeter
                && mShowDownwardVisualPerceptionInfo) {
            canvas.save();
            drawBarrierInfo(canvas, DOWNWARD);
            canvas.restore();
        }
        canvas.restore();
    }

    private void drawBarrierInfo(Canvas canvas, int orientation) {
        // 因为Style是FILL_AND_STROKE
        canvas.translate(0, (float) mFrameworkStrokeWidth * 3 / 2 * orientation);
        float barrierValue = getBarrierValue(mMaxPerceptionDistanceInMeter);
        if (barrierValue == 0){
            barrierValue = 1;
        }
        float ratio = (float) mAvoidanceMaxHeight / barrierValue;
        // 绘制上视障碍物最远感知条
        int restArea = drawMaxPerceptionIndicator(canvas, orientation, ratio);
        // 绘制上视障碍物感知条
        if (restArea > 0) {
            restArea = drawSettingPerceptionIndicator(canvas, orientation, restArea, ratio);
            // 绘制上视障碍物指示条
            restArea = drawBarrierIndicator(canvas, orientation, restArea, ratio);
        }
        // 绘制上视障碍物指示条顶部横线及避障距离
        drawBarrier(canvas, orientation, restArea);
    }

    private int drawMaxPerceptionIndicator(Canvas canvas, int orientation, float ratio) {
        float barrierDistance = orientation == UPWARD ? getUpwardBarrierDistance() : mDownVisionDistance;
        float perceptionDistance = orientation == UPWARD ? mUserSetUpBarrierWarnDistance : mUserSetDownBarrierWarnDistance;

        float barrierValue = getBarrierValue(barrierDistance);
        float perceptionValue = getBarrierValue(perceptionDistance);

        mPaint.setColor(mAvoidanceIndicatorStrokeColor);
        mPaint.setStrokeWidth(mAvoidanceIndicatorStrokeWidth);
        mPaint.setStyle(Paint.Style.STROKE);
        float outOfPerceptionDistanceRange = barrierValue - perceptionValue;
        int restArea = mAvoidanceMaxHeight;
        if (outOfPerceptionDistanceRange > 0) {
            float outOfPerceptionArea = outOfPerceptionDistanceRange * ratio;
            if (outOfPerceptionArea > restArea) {
                outOfPerceptionArea = restArea;
            }
            int top;
            int bottom;
            if (orientation == UPWARD) {
                top = -(int) outOfPerceptionArea + mAvoidanceIndicatorStrokeWidth;
                bottom = 0;
            } else {
                top = 0;
                bottom = (int) outOfPerceptionArea - mAvoidanceIndicatorStrokeWidth;
            }
            RECT.set(-mAvoidanceIndicatorWidth, top, 0, bottom);
            canvas.drawRect(RECT, mPaint);
            canvas.translate(0, (float) outOfPerceptionArea * orientation);
            restArea -= (int) outOfPerceptionArea;
        }
        return restArea;
    }

    private int drawSettingPerceptionIndicator(Canvas canvas, int orientation, int restArea, float ratio) {
        float barrierDistance = orientation == UPWARD ? getUpwardBarrierDistance() : mDownVisionDistance;
        float perceptionDistance = orientation == UPWARD ? mUserSetUpBarrierWarnDistance : mUserSetDownBarrierWarnDistance;
        float barrierAvoidanceDistance = orientation == UPWARD ? mUserSetUpBarrierAvoidanceDistance : mUserSetDownBarrierAvoidanceDistance;

        float barrierValue = getBarrierValue(barrierDistance);
        float perceptionValue = getBarrierValue(perceptionDistance);
        float barrierAvoidanceValue = getBarrierValue(barrierAvoidanceDistance);

        mPaint.setColor(mAvoidanceIndicatorColor);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        float upPerceptionArea;
        if (perceptionValue > barrierValue) {
            upPerceptionArea = (barrierValue - barrierAvoidanceValue) * ratio;
        } else {
            upPerceptionArea = (perceptionValue - barrierAvoidanceValue) * ratio;
        }
        if (upPerceptionArea > 0) {
            if (upPerceptionArea > restArea) {
                upPerceptionArea = restArea;
                restArea = 0;
            } else {
                restArea -= upPerceptionArea;
            }
            int top;
            int bottom;
            if (orientation == UPWARD) {
                top = -(int) upPerceptionArea;
                bottom = 0;
            } else {
                top = 0;
                bottom = (int) upPerceptionArea;
            }
            RECT.set(-mAvoidanceIndicatorWidth, top, 0, bottom);
            canvas.drawRect(RECT, mPaint);
            canvas.translate(0, (float) upPerceptionArea * orientation);
        }
        return restArea;
    }

    private int drawBarrierIndicator(Canvas canvas, int orientation, int restArea, float ratio) {
        float barrierDistance = orientation == UPWARD ? getUpwardBarrierDistance() : mDownVisionDistance;
        float barrierAvoidanceDistance = orientation == UPWARD ? mUserSetUpBarrierAvoidanceDistance : mUserSetDownBarrierAvoidanceDistance;

        float barrierValue = getBarrierValue(barrierDistance);
        float barrierAvoidanceValue = getBarrierValue(barrierAvoidanceDistance);


        float upBarrierArea = (Math.min(barrierValue, barrierAvoidanceValue)) * ratio;
        if (upBarrierArea > 0) {
            mPaint.setColor(mBarrierIndicatorColor);
            if (upBarrierArea > restArea) {
                upBarrierArea = restArea;
                restArea = 0;
            } else {
                restArea -= upBarrierArea;
            }
            int top;
            int bottom;
            if (orientation == UPWARD) {
                top = -(int) upBarrierArea;
                bottom = 0;
            } else {
                top = 0;
                bottom = (int) upBarrierArea;
            }
            RECT.set(-mAvoidanceIndicatorWidth, top, 0, bottom);
            canvas.drawRect(RECT, mPaint);
            canvas.translate(0f, (float) upBarrierArea * orientation);
        }
        return restArea;
    }

    private void drawBarrier(Canvas canvas, int orientation, int restArea) {
        float barrierDistance = orientation == UPWARD ? getUpwardBarrierDistance() : mDownVisionDistance;
        float perceptionDistance = orientation == UPWARD ? mUserSetUpBarrierWarnDistance : mUserSetDownBarrierWarnDistance;
        float barrierAvoidanceDistance = orientation == UPWARD ? mUserSetUpBarrierAvoidanceDistance : mUserSetDownBarrierAvoidanceDistance;

        // 绘制上视障碍物指示条顶部横线
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setColor(mBarrierIndicatorColor);
        canvas.translate(-(float) mAvoidanceIndicatorWidth / 2, -(float) mBarrierIndicatorHeight / 2 * orientation);
        RECT.set(-mBarrierIndicatorWidth / 2, -mBarrierIndicatorHeight / 2, mBarrierIndicatorWidth / 2, mBarrierIndicatorHeight / 2);
        canvas.drawRect(RECT, mPaint);
        // 绘制避障距离
        canvas.translate(-(float) mBarrierIndicatorWidth / 2, -(float) mBarrierIndicatorHeight / 2 * orientation);
        String barrierDistanceText;
        if (barrierDistance < 10) {
            barrierDistanceText = String.format(Locale.ENGLISH, "%.1f", getDisplayValue(barrierDistance));
        } else {
            barrierDistanceText = String.format(Locale.ENGLISH, "%.0f", getDisplayValue(barrierDistance));
        }
        int barrierDistanceTextColor;
        if (barrierDistance <= barrierAvoidanceDistance) {
            barrierDistanceTextColor = mBarrierIndicatorColor;
        } else if (barrierDistance <= perceptionDistance) {
            barrierDistanceTextColor = mAvoidanceIndicatorColor;
        } else {
            barrierDistanceTextColor = mAvoidanceIndicatorStrokeColor;
        }
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize(mBarrierDistanceTextSize);
        mPaint.setColor(barrierDistanceTextColor);
        mPaint.setTypeface(Typeface.DEFAULT);
        mPaint.getTextBounds(barrierDistanceText, 0, barrierDistanceText.length(), RECT);
        Paint.FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
        float barrierDistanceTextMiddleY = orientation == UPWARD ? (float) -RECT.height() / 2 - mBarrierIndicatorHeight * 2 : (float) RECT.height() / 2 + mBarrierIndicatorHeight * 2;
        mPaint.setTextAlign(Paint.Align.CENTER);
        float x = (float) mBarrierIndicatorWidth / 2;
        if (mBarrierDistanceTextAlign == BARRIER_DISTANCE_TEXT_ALIGN_HORIZONTAL) {
            // 新需求改成上下显示，调整x坐标不会与刻度相交
            x = (float) mBarrierIndicatorWidth / 3;
        }
        canvas.drawText(barrierDistanceText, x, barrierDistanceTextMiddleY + (float) (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom, mPaint);
    }

    @Override
    protected String getCurrentValueDisplayFormat() {
        return "%05.1f";
    }

    @Override
    protected String getAttributeUnit() {
        if (isInEditMode()){
            return "m";
        }
        return UnitUtils.getUintStrByLength(UnitUtils.isMetricUnits() ? UnitUtils.UnitType.METRIC : UnitUtils.UnitType.IMPERIAL);
    }

    @Override
    protected float getDisplayValue(float value) {
        if (isInEditMode()){
            return 0F;
        }
        return UnitUtils.getValueFromMetricByLength(value, UnitUtils.isMetricUnits() ? UnitUtils.UnitType.METRIC : UnitUtils.UnitType.IMPERIAL);
    }

    private float getUpwardBarrierDistance() {
        if (mUpwardVisualEnable && mUpwardRadarEnable) {
            return Math.min(mUpwardRadarDistance, mUVisionDistance);
        } else if (mUpwardRadarEnable) {
            return mUpwardRadarDistance;
        } else {
            return mUVisionDistance;
        }
    }

    private float getBarrierValue(float barrierDistance) {
        if (barrierDistance <= 0) {
            return 0;
        } else if (barrierDistance <= 2) {
            return barrierDistance / 2;
        } else if (barrierDistance <= 5) {
            return (barrierDistance + 1) / 3;
        } else {
            return (float) (0.3045f+ 1.0769f*Math.log(barrierDistance));
        }
    }
}
