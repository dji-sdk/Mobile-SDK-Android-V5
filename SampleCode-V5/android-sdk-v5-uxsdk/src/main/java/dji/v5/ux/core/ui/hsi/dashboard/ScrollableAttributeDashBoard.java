package dji.v5.ux.core.ui.hsi.dashboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;

public class ScrollableAttributeDashBoard extends View {

    protected static final Rect RECT = new Rect();

    private static final Path PATH = new Path();

    private static final Float DEFAULT_OFFSET_PER_UNIT = 1f;

    /**
     * 最多显示多少个刻度
     */
    private static final int DEFAULT_VISIBLE_CALIBRATION_UNIT_COUNT = 18;

    private static final int SMALL_CALIBRATION_UNIT_COUNT_BETWEEN_TWO_BIG_CALIBRATION_UNIT = 5;

    private static final int DASH_BOARD_ALIGN_LEFT = 0;

    private static final int DASH_BOARD_ALIGN_RIGHT = 1;

    private static final int DEFAULT_DASH_BOARD_ALIGN = DASH_BOARD_ALIGN_LEFT;

    /**
     * 属性名称
     */
    private String mAttributeName;

    /**
     * 属性单位
     */
    private String mAttributeUnit;

    /**
     * 每个小刻度的偏移值
     */
    protected final float mAttributeOffsetPerUnit;

    /**
     * 刻度的字体大小
     */
    private final int mAttributeCalibrationTextSize;

    /**
     * 当前刻度的字体大小
     */
    private final int mAttributeCurrentCalibrationTextSize;

    /**
     * 当前属性单位字体大小
     */
    private final int mAttributeUnitTextSize;

    /**
     * 当前属性名称的字体大小
     */
    private final int mAttributeNameTextSize;

    /**
     * 最大值
     */
    private final float mAttributeMaxValue;

    /**
     * 最小值
     */
    private final float mAttributeMinValue;

    /**
     * 仪表盘的朝向（左/右）
     */
    private final int mAttributeDashBoardAlign;

    /**
     * 属性的当前值
     */
    private float mCurrentValue;

    /**
     * 仪表盘中可视的刻度数量
     */
    protected int mVisibleCalibrationUnitCount;

    /**
     * dashboard框架线条的宽度
     */
    protected int mFrameworkStrokeWidth;

    /**
     * 是否显示dashboard框架
     */
    private final boolean mShowFramework;

    /**
     * dashboard框架的宽度
     */
    private final int mFrameworkWidth;

    /**
     * dashboard框架的高度
     */
    private final int mFrameworkHeight;

    /**
     * dashboard指针的高度
     */
    private final int mPointerHeight;

    /**
     * dashboard指针的宽度
     */
    private final int mPointerWidth;

    /**
     * 框架相对于起始位置的距离
     */
    protected final int mFrameworkPaddingStart;

    protected final int mPointerLineInnerWidth;

    protected final int mPointerLineOuterWidth;
    protected final int mPointerTriangleWidth;

    private final int mDegreeLineLongWidth;

    protected final int mDegreeLineShortWidth;

    private final int mPointerDividerTopWidth;

    private final int mPointerDividerBottomWidth;

    protected Paint mPaint;

    private Drawable mSpeedIcon;
    private Drawable mAltitudeIcon;
    private float mSpeedValue;
    private float mAltitudeValue;

    /**
     * 100ms，减少性能压力
     */
    private static final int INVALIDATE_INTERVAL_TIME = 100;
    private static final int MSG_INVALIDATE = 0x01;
    @NonNull
    private final Handler.Callback mCallback = msg -> {
        if (msg.what == MSG_INVALIDATE) {
            invalidate();
            return true;
        }
        return false;
    };

    @Nullable
    private Handler mHandler;

    public ScrollableAttributeDashBoard(Context context) {
        this(context, null);
    }

    public ScrollableAttributeDashBoard(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollableAttributeDashBoard(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ScrollableAttributeDashBoard);
        mAttributeName = typedArray.getString(R.styleable.ScrollableAttributeDashBoard_uxsdk_name);
        mAttributeName = (mAttributeName == null ? "" : mAttributeName);
        mAttributeCalibrationTextSize = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_calibration_text_size,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_text_size_normal_medium));
        mPointerWidth = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_pointer_width,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_78_dp));
        mPointerHeight = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_pointer_height,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_25_dp));
        mFrameworkWidth = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_framework_width,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_30_dp));
        mFrameworkHeight = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_framework_height,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_140_dp));
        mFrameworkStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_framework_stroke_width,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_1_dp));
        mPointerLineInnerWidth = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_pointer_line_inner_width,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_7_dp));
        mPointerLineOuterWidth = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_pointer_line_outer_width,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_7_dp));
        mPointerTriangleWidth = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_pointer_triangle_width,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_4_dp));
        mPointerDividerBottomWidth = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_pointer_divider_bottom_width,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_28_dp));
        mPointerDividerTopWidth = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_pointer_divider_top_width,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_35_dp));
        mFrameworkPaddingStart = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_calibration_framework_padding_start,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_25_dp));
        mDegreeLineLongWidth = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_degree_line_long,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_9_dp));
        mDegreeLineShortWidth = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_degree_line_short,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_3_dp));
        mAttributeUnitTextSize = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_unit_text_size,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_text_size_small));
        mAttributeNameTextSize = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_name_text_size,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_text_size_normal));
        mAttributeCurrentCalibrationTextSize = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_current_calibration_text_size,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_text_size_medium_large));
        mAttributeUnit = typedArray.getString(R.styleable.ScrollableAttributeDashBoard_uxsdk_unit);
        mAttributeMaxValue = typedArray.getFloat(R.styleable.ScrollableAttributeDashBoard_uxsdk_max_value, Float.MAX_VALUE);
        mAttributeMinValue = typedArray.getFloat(R.styleable.ScrollableAttributeDashBoard_uxsdk_min_value, -Float.MAX_VALUE);
        mAttributeOffsetPerUnit = typedArray.getFloat(R.styleable.ScrollableAttributeDashBoard_uxsdk_offset_per_unit, DEFAULT_OFFSET_PER_UNIT);
        float currentValue = typedArray.getFloat(R.styleable.ScrollableAttributeDashBoard_uxsdk_current_value, 0);
        mAttributeDashBoardAlign = typedArray.getInt(R.styleable.ScrollableAttributeDashBoard_uxsdk_dash_board_align, DEFAULT_DASH_BOARD_ALIGN);
        mVisibleCalibrationUnitCount = typedArray.getInt(R.styleable.ScrollableAttributeDashBoard_attribute_visible_calibration_unit_count,
                DEFAULT_VISIBLE_CALIBRATION_UNIT_COUNT);
        mShowFramework = typedArray.getBoolean(R.styleable.ScrollableAttributeDashBoard_uxsdk_show_calibration_framework, true);
        typedArray.recycle();

        mPaint = new Paint(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);

        mSpeedIcon = ContextCompat.getDrawable(getContext(), R.drawable.uxsdk_ic_attr_speed);
        mSpeedIcon.setBounds(0, 0, mSpeedIcon.getIntrinsicWidth(), mSpeedIcon.getIntrinsicHeight());

        mAltitudeIcon = ContextCompat.getDrawable(getContext(), R.drawable.uxsdk_ic_attr_altitude);
        mAltitudeIcon.setBounds(0, 0, mAltitudeIcon.getIntrinsicWidth(), mAltitudeIcon.getIntrinsicHeight());

        setCurrentValue(currentValue);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mHandler = new Handler(Looper.getMainLooper(), mCallback);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }

    @MainThread
    public void setCurrentValue(float currentValue) {
        if (currentValue > mAttributeMaxValue) {
            currentValue = mAttributeMaxValue;
        } else if (currentValue < mAttributeMinValue) {
            currentValue = mAttributeMinValue;
        }
        mCurrentValue = currentValue;
        updateWidget();
    }

    public void setSpeed(int speed) {
        mSpeedValue = speed;
    }

    public void setAltitude(int altitude) {
        mAltitudeValue = altitude;
    }

    protected void updateWidget() {
        if (mHandler != null && !mHandler.hasMessages(MSG_INVALIDATE)) {
            mHandler.sendEmptyMessageDelayed(MSG_INVALIDATE, INVALIDATE_INTERVAL_TIME);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        int measuredWidth = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int measuredHeight = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        if (widthSpecMode != MeasureSpec.EXACTLY) {
            int minWidth = getMinWidth();
            if (widthSpecMode == MeasureSpec.UNSPECIFIED) {
                measuredWidth = minWidth;
            } else {
                measuredWidth = Math.min(minWidth, widthSpecSize);
            }
        }
        if (heightSpecMode != MeasureSpec.EXACTLY) {
            int minHeight = getMinHeight();
            if (widthSpecMode == MeasureSpec.UNSPECIFIED) {
                measuredHeight = minHeight;
            } else {
                measuredHeight = Math.min(minHeight, heightSpecSize);
            }
        }
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    protected int getMinHeight() {
        if (mShowFramework) {
            return mFrameworkHeight;
        } else {
            return mPointerHeight + mFrameworkStrokeWidth * 2;
        }
    }

    protected int getMinWidth() {
        return mPointerWidth + mFrameworkPaddingStart + mPointerLineInnerWidth + mPointerTriangleWidth / 2 + mFrameworkStrokeWidth;
    }

    protected int getFrameworkHeight() {
        return mFrameworkHeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawCurrentValue(canvas);
        clipPointerArea(canvas);
        if (mShowFramework) {
            drawCalibrationFramework(canvas);
            drawCalibration(canvas);
        }
        drawWaypointAttr(canvas);
    }

    protected float getCurrentValue() {
        return mCurrentValue;
    }

    protected boolean isShowFramework() {
        return mShowFramework;
    }

    private void clipPointerArea(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        if (mAttributeDashBoardAlign == DASH_BOARD_ALIGN_LEFT) {
            float x = mFrameworkPaddingStart + mPointerLineInnerWidth + (float) mPointerTriangleWidth / 2;
            float y = (float) height / 2 - (float) mPointerHeight / 2;
            PATH.reset();
            PATH.moveTo(x, y);
            PATH.rLineTo(mPointerWidth, 0);
            PATH.rLineTo(0, mPointerHeight);
            PATH.rLineTo(-mPointerWidth, 0);
            PATH.rLineTo(0, -(float) (mPointerHeight - mPointerTriangleWidth) / 2);
            PATH.rLineTo(-(float) mPointerTriangleWidth / 2, -(float) mPointerTriangleWidth / 2);
            PATH.rLineTo((float) mPointerTriangleWidth / 2, -(float) mPointerTriangleWidth / 2);
            PATH.close();
        } else {
            float x = width - mFrameworkPaddingStart - mPointerLineInnerWidth - (float) mPointerTriangleWidth / 2;
            float y = (float) height / 2 - (float) mPointerHeight / 2;
            PATH.reset();
            PATH.moveTo(x, y);
            PATH.rLineTo(-mPointerWidth, 0);
            PATH.rLineTo(0, mPointerHeight);
            PATH.rLineTo(mPointerWidth, 0);
            PATH.rLineTo(0, -(float) (mPointerHeight - mPointerTriangleWidth) / 2);
            PATH.rLineTo((float) mPointerTriangleWidth / 2, -(float) mPointerTriangleWidth / 2);
            PATH.rLineTo(-(float) mPointerTriangleWidth / 2, -(float) mPointerTriangleWidth / 2);
            PATH.close();
        }
        canvas.clipPath(PATH, Region.Op.DIFFERENCE);
    }

    /**
     * @param canvas
     */
    private void drawCalibrationFramework(Canvas canvas) {
        mPaint.setColor(getResources().getColor(R.color.uxsdk_pfd_main_color));
        mPaint.setStrokeWidth(mFrameworkStrokeWidth);
        int width = getWidth();
        int height = getHeight();
        float topMargin = (float) (height - mFrameworkHeight) / 2;
        canvas.save();
        if (mAttributeDashBoardAlign == DASH_BOARD_ALIGN_LEFT) {
            canvas.translate(mFrameworkPaddingStart, 0);
            canvas.drawLine(0, topMargin + mPaint.getStrokeWidth() / 2, mFrameworkWidth, topMargin + mPaint.getStrokeWidth() / 2, mPaint);
            canvas.drawLine(0, topMargin + mFrameworkHeight - mPaint.getStrokeWidth() / 2, mFrameworkWidth, topMargin + mFrameworkHeight - mPaint.getStrokeWidth() / 2, mPaint);
            canvas.drawLine(0, topMargin, 0, topMargin + mFrameworkHeight, mPaint);
        } else {
            canvas.translate((float) (width - mFrameworkPaddingStart - mFrameworkWidth), 0);
            canvas.drawLine(0, topMargin + mPaint.getStrokeWidth() / 2, mFrameworkWidth, topMargin + mPaint.getStrokeWidth() / 2, mPaint);
            canvas.drawLine(0, topMargin + mFrameworkHeight - mPaint.getStrokeWidth() / 2, mFrameworkWidth, topMargin + mFrameworkHeight - mPaint.getStrokeWidth() / 2, mPaint);
            canvas.translate(mFrameworkWidth, 0);
            canvas.drawLine(0, topMargin, 0, topMargin + mFrameworkHeight, mPaint);
        }
        canvas.restore();
    }

    /**
     * @param canvas
     */
    private void drawCalibration(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        float calibrationHorizontalMargin = (float) (mPointerLineInnerWidth - mDegreeLineShortWidth) / 2;
        float offsetMargin = (float) mFrameworkHeight / mVisibleCalibrationUnitCount;
        float currentPerUnitValue = mCurrentValue / mAttributeOffsetPerUnit;
        int currentPerUnitCount = (int) currentPerUnitValue;
        float currentValueOffset = (currentPerUnitValue - currentPerUnitCount) * offsetMargin;
        canvas.save();
        canvas.translate(0, currentValueOffset + (float) height / 2);
        mPaint.setColor(getResources().getColor(R.color.uxsdk_pfd_main_color));
        mPaint.setStrokeWidth((float) getResources().getDimensionPixelSize(R.dimen.uxsdk_1_dp) / 2);
        if (mAttributeDashBoardAlign == DASH_BOARD_ALIGN_LEFT) {
            // 画出刻度
            canvas.translate(mFrameworkPaddingStart, 0);
            for (int i = -mVisibleCalibrationUnitCount / 2; i <= mVisibleCalibrationUnitCount / 2; i++) {
                int currentCalibrationUnit = currentPerUnitCount - i;
                float y = i * offsetMargin;
                float calibrationValue = currentCalibrationUnit * mAttributeOffsetPerUnit;
                if (calibrationValue > mAttributeMaxValue || calibrationValue < mAttributeMinValue) {
                    continue;
                }
                //drawWaypointAltitudeIcon(canvas, calibrationHorizontalMargin, y, calibrationValue);
                // 如果是0，则画长的刻度线，以及标上刻度，否则只画小的刻度线
                if (currentCalibrationUnit % SMALL_CALIBRATION_UNIT_COUNT_BETWEEN_TWO_BIG_CALIBRATION_UNIT == 0) {
                    // 画刻度线
                    canvas.drawLine(calibrationHorizontalMargin, y, calibrationHorizontalMargin + mDegreeLineLongWidth, y, mPaint);
                    // 标上刻度
                    mPaint.setTextAlign(Paint.Align.LEFT);
                    mPaint.setTextSize(mAttributeCalibrationTextSize);
                    String currentValueString = String.format(Locale.ENGLISH, "%02.0f", getDisplayValue(calibrationValue));
                    mPaint.getTextBounds(currentValueString, 0, currentValueString.length(), RECT);
                    Paint.FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
                    float baseline = (offsetMargin - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top;
                    canvas.drawText(currentValueString, 2 * calibrationHorizontalMargin + mDegreeLineLongWidth,
                            baseline + y - offsetMargin / 2, mPaint);
                } else {
                    // 画刻度线
                    canvas.drawLine(calibrationHorizontalMargin, y, calibrationHorizontalMargin + mDegreeLineShortWidth, y, mPaint);
                }
            }
        } else {
            doCanvasTranslate(canvas, width, currentPerUnitCount, offsetMargin, calibrationHorizontalMargin);
        }
        canvas.restore();
    }

    private void doCanvasTranslate(Canvas canvas, int width, int currentPerUnitCount, float offsetMargin, float calibrationHorizontalMargin) {
        // 画出刻度
        canvas.translate((float) width - (mFrameworkPaddingStart), 0);
        for (int i = -mVisibleCalibrationUnitCount / 2; i <= mVisibleCalibrationUnitCount / 2; i++) {
            int currentCalibrationUnit = currentPerUnitCount - i;
            float y = i * offsetMargin;
            float calibrationValue = currentCalibrationUnit * mAttributeOffsetPerUnit;
            //LogUtil.saveLog(TAG,"calibrationValue:"+calibrationValue);
            // 超过最大值最小值，不作处理
            if (calibrationValue > mAttributeMaxValue || calibrationValue < mAttributeMinValue) {
                continue;
            }
            //drawWaypointSpeedIcon(canvas, (int) calibrationHorizontalMargin, y, calibrationValue);
            // 如果是0，则画长的刻度线，以及标上刻度，否则只画小的刻度线
            if (currentCalibrationUnit % SMALL_CALIBRATION_UNIT_COUNT_BETWEEN_TWO_BIG_CALIBRATION_UNIT == 0) {
                // 画刻度线
                canvas.drawLine(-calibrationHorizontalMargin, y, -(calibrationHorizontalMargin + mDegreeLineLongWidth), y, mPaint);
                // 标上刻度
                mPaint.setTextAlign(Paint.Align.LEFT);
                mPaint.setTextSize(mAttributeCalibrationTextSize);
                String currentValueString = String.format(Locale.ENGLISH, "%02.0f", getDisplayValue(calibrationValue));
                mPaint.getTextBounds(currentValueString, 0, currentValueString.length(), RECT);
                Paint.FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
                float baseline = (offsetMargin - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top;
                canvas.drawText(currentValueString, -(2 * calibrationHorizontalMargin + mDegreeLineLongWidth + RECT.width()),
                        baseline + y - offsetMargin / 2, mPaint);
            } else {
                // 画刻度线
                canvas.drawLine(-calibrationHorizontalMargin, y, -(calibrationHorizontalMargin + mDegreeLineShortWidth), y, mPaint);
            }
        }
    }

//    private void drawWaypointSpeedIcon(Canvas canvas,
//                                       int calibrationHorizontalMargin,
//                                       float y,
//                                       float calibrationValue) {
//        // 只在航线执行过程中显示
//        MissionExecutePointInfo pointInfo = MissionManagerDelegate.INSTANCE.getExecutePointInfo();
//        if (!MissionManagerDelegate.INSTANCE.isRunningMission() || pointInfo == null) {
//            return;
//        }
//        mSpeedValue = pointInfo.getSpeed();
//        if (Math.abs(mSpeedValue - calibrationValue) <= 0.1) {
//            // 当前航线速度值与某个刻度值绝对值相差0.1认为相等
//            int left = calibrationHorizontalMargin;
//            int top = (int) Math.ceil(y - mSpeedIcon.getIntrinsicHeight() / 2.0);
//            drawSpeedIcon(canvas,left,top);
//        }
//    }

//    private void drawWaypointAltitudeIcon(Canvas canvas,
//                                          float calibrationHorizontalMargin,
//                                          float y,
//                                          float calibrationValue) {
//        // 只在航线执行过程中显示
//        MissionExecutePointInfo pointInfo = MissionManagerDelegate.INSTANCE.getExecutePointInfo();
//        if (!MissionManagerDelegate.INSTANCE.isRunningMission() || pointInfo == null) {
//            return;
//        }
//        mAltitudeValue = pointInfo.getAltitude();
//        if (Math.abs(mAltitudeValue - calibrationValue) <= 0.1) {
//            // 当前航线设置高度与某个刻度值绝对值相差0.1认为相等
//            int left = (int) Math.floor(calibrationHorizontalMargin - mAltitudeIcon.getIntrinsicWidth() - AndUtil.dip2px(getContext(), 3));
//            int top = (int) Math.ceil(y - mAltitudeIcon.getIntrinsicHeight() / 2.0);
//            drawAltitudeIcon(canvas,left,top);
//        }
//    }

    private void drawSpeedIcon(Canvas canvas,int left,int top) {
        int right = left + mSpeedIcon.getIntrinsicWidth();
        int bottom = top + mSpeedIcon.getIntrinsicHeight();
        mSpeedIcon.setBounds(left,top,right,bottom);
        mSpeedIcon.draw(canvas);
    }

    private void drawAltitudeIcon(Canvas canvas, int left, int top) {
        int right = left + mAltitudeIcon.getIntrinsicWidth();
        int bottom = top + mAltitudeIcon.getIntrinsicHeight();
        mAltitudeIcon.setBounds(left,top,right,bottom);
        mAltitudeIcon.draw(canvas);
    }

    /**
     * @param canvas
     */
    private void drawCurrentValue(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        String currentValueString = String.format(Locale.ENGLISH, getCurrentValueDisplayFormat(), getDisplayValue(mCurrentValue));
        //当数值字符串超过5位时，需要增加偏移量，避免数值与边线过紧。
        int textOffset = currentValueString.length() > 5 ? getResources().getDimensionPixelSize(R.dimen.uxsdk_3_dp) : 0;
        String unit = String.format(Locale.ENGLISH, "(%s)", getAttributeUnit());
        int background = getContext().getResources().getColor(R.color.uxsdk_black_20_percent);
        canvas.save();
        if (mAttributeDashBoardAlign == DASH_BOARD_ALIGN_LEFT) {
            canvas.translate(mFrameworkPaddingStart, 0);
            canvas.translate(mPointerLineInnerWidth, 0);
            canvas.translate(0, (float) height / 2);
            canvas.save();
            // 画背景颜色
            canvas.translate((float) mPointerTriangleWidth / 2, -(float) mPointerHeight / 2);
            PATH.reset();
            PATH.moveTo(0, 0);
            PATH.rLineTo(mPointerWidth, 0);
            PATH.rLineTo(0, mPointerHeight);
            PATH.rLineTo(-mPointerWidth, 0);
            PATH.rLineTo(0, -(float) (mPointerHeight - mPointerTriangleWidth) / 2);
            PATH.rLineTo(-(float) mPointerTriangleWidth / 2, -(float) mPointerTriangleWidth / 2);
            PATH.rLineTo((float) mPointerTriangleWidth / 2, -(float) mPointerTriangleWidth / 2);
            PATH.close();
            mPaint.setColor(background);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawPath(PATH, mPaint);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.uxsdk_1_dp));
            mPaint.setColor(getResources().getColor(R.color.uxsdk_pfd_main_color));
            canvas.drawPath(PATH, mPaint);
            // 画出当前值
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(getResources().getColor(R.color.uxsdk_pfd_main_color));
            mPaint.setTextAlign(Paint.Align.LEFT);
            mPaint.setTextSize(mAttributeCurrentCalibrationTextSize);
            mPaint.setTypeface(Typeface.DEFAULT_BOLD);
            mPaint.getTextBounds(currentValueString, 0, currentValueString.length(), RECT);
            Paint.FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
            float baseline = (float) (mPointerHeight - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top;
            float valueUnitDivider = (float)(mPointerWidth - mPointerDividerTopWidth);
            canvas.drawText(currentValueString, (valueUnitDivider - (float) RECT.width()) / 2 + textOffset, baseline, mPaint);
            // 画出属性名称和属性单位的背景
            canvas.translate(valueUnitDivider, 0);
            PATH.reset();
            PATH.moveTo(0, 0);
            PATH.rLineTo(mPointerWidth - valueUnitDivider, 0);
            PATH.rLineTo(0, mPointerHeight);
            PATH.rLineTo(-mPointerDividerBottomWidth, 0);
            PATH.close();
            mPaint.setColor(background);
            canvas.drawPath(PATH, mPaint);
            // 画出属性名称
            canvas.translate((float)(mPointerDividerTopWidth - mPointerDividerBottomWidth), 0);
            mPaint.setTextSize(mAttributeNameTextSize);
            mPaint.setColor(Color.WHITE);
            mPaint.getTextBounds(mAttributeName, 0, mAttributeName.length(), RECT);
            // 让属性名称紧贴底部
            canvas.drawText(mAttributeName, (float) (mPointerDividerBottomWidth - RECT.width()) / 2, (float) mPointerHeight / 2, mPaint);
            // 画出属性单位
            mPaint.setTypeface(Typeface.DEFAULT);
            mPaint.setTextSize(mAttributeUnitTextSize);
            mPaint.getTextBounds(unit, 0, unit.length(), RECT);
            fontMetrics = mPaint.getFontMetricsInt();
            // 让属性单位紧贴顶部
            canvas.drawText(unit, (float) (mPointerDividerBottomWidth - RECT.width()) / 2, (float) mPointerHeight / 2 - fontMetrics.top, mPaint);
            // 画出指针的线段
            canvas.restore();
            mPaint.setColor(getResources().getColor(R.color.uxsdk_pfd_main_color));
            mPaint.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.uxsdk_1_dp));
            canvas.drawLine(-(mPointerLineInnerWidth + mPointerLineOuterWidth), 0, 0, 0, mPaint);
        } else {
            canvas.translate((float)(width - mFrameworkPaddingStart), 0);
            canvas.translate(-mPointerLineInnerWidth, 0);
            canvas.translate(0, (float) height / 2);
            canvas.save();
            // 画背景颜色
            canvas.translate(-(float) mPointerTriangleWidth / 2, -(float) mPointerHeight / 2);
            mPaint.setColor(background);
            mPaint.setStyle(Paint.Style.FILL);
            PATH.reset();
            PATH.moveTo(0, 0);
            PATH.rLineTo(-mPointerWidth, 0);
            PATH.rLineTo(0, mPointerHeight);
            PATH.rLineTo(mPointerWidth, 0);
            PATH.rLineTo(0, -(float) (mPointerHeight - mPointerTriangleWidth) / 2);
            PATH.rLineTo((float) mPointerTriangleWidth / 2, -(float) mPointerTriangleWidth / 2);
            PATH.rLineTo(-(float) mPointerTriangleWidth / 2, -(float) mPointerTriangleWidth / 2);
            PATH.close();
            mPaint.setColor(background);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawPath(PATH, mPaint);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.uxsdk_1_dp));
            mPaint.setColor(getResources().getColor(R.color.uxsdk_pfd_main_color));
            canvas.drawPath(PATH, mPaint);
            // 画出当前值
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(getResources().getColor(R.color.uxsdk_pfd_main_color));
            mPaint.setTextAlign(Paint.Align.LEFT);
            mPaint.setTextSize(mAttributeCurrentCalibrationTextSize);
            mPaint.setTypeface(Typeface.DEFAULT_BOLD);
            mPaint.getTextBounds(currentValueString, 0, currentValueString.length(), RECT);
            Paint.FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
            float baseline = (float) (mPointerHeight - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top;
            float valueUnitDivider = (float)(mPointerWidth - mPointerDividerTopWidth);
            canvas.translate(-valueUnitDivider, 0);
            canvas.drawText(currentValueString, (valueUnitDivider - (float) RECT.width()) / 2 - textOffset, baseline, mPaint);
            // 画出属性名称和属性单位的背景
            PATH.reset();
            PATH.moveTo(0, 0);
            PATH.rLineTo(-(mPointerWidth - valueUnitDivider), 0);
            PATH.rLineTo(0, mPointerHeight);
            PATH.rLineTo(mPointerDividerBottomWidth, 0);
            PATH.close();
            mPaint.setColor(background);
            canvas.drawPath(PATH, mPaint);
            // 画出属性名称
            canvas.translate(-(mPointerDividerTopWidth - mPointerDividerBottomWidth), 0);
            mPaint.setTextSize(mAttributeNameTextSize);
            mPaint.setColor(Color.WHITE);
            mPaint.getTextBounds(mAttributeName, 0, mAttributeName.length(), RECT);
            // 让属性名称紧贴底部
            canvas.drawText(mAttributeName, -(float) (mPointerDividerBottomWidth - RECT.width()) / 2 - RECT.width(), (float) mPointerHeight / 2, mPaint);
            // 画出属性单位
            mPaint.setTypeface(Typeface.DEFAULT);
            mPaint.setTextSize(mAttributeUnitTextSize);
            mPaint.getTextBounds(unit, 0, unit.length(), RECT);
            fontMetrics = mPaint.getFontMetricsInt();
            // 让属性单位紧贴顶部
            canvas.drawText(unit, -(float) (mPointerDividerBottomWidth - RECT.width()) / 2 - RECT.width(), (float) mPointerHeight / 2 - fontMetrics.top, mPaint);
            // 画出指针的线段
            canvas.restore();
            mPaint.setColor(getResources().getColor(R.color.uxsdk_pfd_main_color));
            mPaint.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.uxsdk_1_dp));
            canvas.drawLine(0, 0, (float)(mPointerLineInnerWidth + mPointerLineOuterWidth), 0, mPaint);
        }
        canvas.restore();
    }

    private void drawWaypointAttr(Canvas canvas) {
        LogUtils.d(LogUtils.getTag(this), "drawWaypointAttr");
    }

    protected String getCurrentValueDisplayFormat() {
        return "%02.1f";
    }

    protected String getAttributeUnit() {
        return mAttributeUnit;
    }

    protected float getDisplayValue(float value) {
        return value;
    }

}
