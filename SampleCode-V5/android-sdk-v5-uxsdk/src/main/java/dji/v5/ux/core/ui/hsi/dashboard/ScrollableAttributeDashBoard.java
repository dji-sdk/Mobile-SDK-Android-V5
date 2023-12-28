package dji.v5.ux.core.ui.hsi.dashboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewDebug;


import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import dji.v5.utils.common.DJIExecutor;
import dji.v5.ux.R;
import dji.v5.utils.common.AndUtil;
import dji.v5.ux.core.util.FontUtils;

public abstract class ScrollableAttributeDashBoard extends View {
    public static final String TAG = "ScrollableAttributeDashBoard";
    protected static final Rect RECT = new Rect();

    private static final Path PATH = new Path();

    private static final Float DEFAULT_OFFSET_PER_UNIT = 1f;

    /**
     * 最多显示多少个刻度
     */
    private static final int DEFAULT_VISIBLE_CALIBRATION_UNIT_COUNT = 20;

    private static final int SMALL_CALIBRATION_UNIT_COUNT_BETWEEN_TWO_BIG_CALIBRATION_UNIT = 5;

    /**
     * 文字在左侧
     */
    private static final int DASH_BOARD_ALIGN_LEFT = 0;

    /**
     * 文字在右侧
     */
    private static final int DASH_BOARD_ALIGN_RIGHT = 1;

    private static final int DEFAULT_DASH_BOARD_ALIGN = DASH_BOARD_ALIGN_LEFT;
    public static final String VIEW_PROPERTY_CATEGORY_DJI = "dji";

    /**
     * 属性名称
     */
    private final String mAttributeName;

    private float mAttributePadding;

    /**
     * 每个小刻度的偏移值
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    protected final float mAttributeOffsetPerUnit;
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    private boolean mShowBorder;
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    private float mBorderRadius;
    private int mBorderColor;
    private float mBorderWidth;

    /**
     * 刻度的字体大小
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    private final int mAttributeCalibrationTextSize;

    /**
     * 当前刻度的字体大小
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    private final int mAttributeCurrentCalibrationTextSize;

    /**
     * 当前属性单位字体大小
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    private final int mAttributeUnitTextSize;

    /**
     * 当前属性名称的字体大小
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    private final int mAttributeNameTextSize;

    /**
     * 最大值
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    private final float mAttributeMaxValue;

    /**
     * 最小值
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    private final float mAttributeMinValue;

    /**
     * 仪表盘的朝向（左/右）
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI, prefix = "hsi_", mapping = {
            @ViewDebug.IntToString(from = DASH_BOARD_ALIGN_LEFT, to = "left"),
            @ViewDebug.IntToString(from = DASH_BOARD_ALIGN_RIGHT, to = "right")
    })
    private final int mAttributeDashBoardAlign;

    /**
     * 属性的当前值
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI, prefix = "hsi_")
    private float mCurrentValue;

    /**
     * 属性当前值颜色
     */
    private int mCurrentValueColor;

    /**
     * 属性值内边距
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI, prefix = "hsi_")
    private float mCurrentValuePadding;

    private int mAttributePropertyColor;


    /**
     * 仪表盘中可视的刻度数量
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    protected int mVisibleCalibrationUnitCount;

    /**
     * dashboard框架线条的宽度
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    protected int mFrameworkStrokeWidth;

    /**
     * dashboard框架线条的主色
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI, formatToHexString = true)
    protected int mFrameworkPrimaryColor;

    /**
     * dashboard框架线条的次要色
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI, formatToHexString = true)
    protected int mFrameworkSecondaryColor;

    /**
     * dashboard主刻度的颜色
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI, formatToHexString = true)
    protected int mFrameworkFrameValueColor;

    /**
     * dashboard框架线条的描边色
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI, formatToHexString = true)
    protected int mFrameworkStrokeColor;

    /**
     * 是否显示dashboard框架
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    private final boolean mShowFramework;

    /**
     * dashboard框架的宽度
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    private final int mFrameworkWidth;

    /**
     * dashboard框架的高度
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    private final int mFrameworkHeight;

    /**
     * dashboard指针的高度
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    private final int mPointerHeight;

    /**
     * dashboard指针的宽度
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    private final int mPointerWidth;

    /**
     * 框架相对于起始位置的距离
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    protected final int mFrameworkPaddingStart;

    /**
     * 框架垂直方向间距，避免刻度文字被截
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    protected int mFrameworkPaddingVercital;

    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    /**
     * 1. 当前值指示线，刻度内侧
     * 2. 短刻度线在内侧占据宽度，超过线宽部分左右为 margin
     */
    protected final int mPointerLineInnerWidth;

    /**
     * 当前值指示线，刻度外侧
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    protected final int mPointerLineOuterWidth;
    /**
     * 当前值离框架额外边距
     */
    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    protected final int mTextPadding;

    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    protected final int mDegreeLineLongWidth;

    @ViewDebug.ExportedProperty(category = VIEW_PROPERTY_CATEGORY_DJI)
    protected final int mDegreeLineShortWidth;

    protected Paint mPaint;
    protected Paint mStrokePaint;

    protected Drawable mWaypointIcon;

    private float mCalibrationTextPadding;

    protected float mWaypointIconPadding;

    private final Object mDisplayValueLock = new Object();
    protected DisplayValue mDisplayValue;

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


    //    @NonNull
    //    private final Object mRxObject = new Object() {
    //        @Subscribe(thread = EventThread.MAIN_THREAD, tags = {
    //                @Tag(BusAction.TAG_ON_UNIT_CHANGED)
    //        })
    //        @SuppressWarnings("unused")
    //        public void onEventUnitChanged(Integer unit) {
    //            // 单位变化后，更新一下显示的值
    //            setCurrentValue(mCurrentValue);
    //            updateWidget();
    //        }
    //    };
    protected FpvStrokeConfig mStrokeConfig;
    private Typeface mDefaultTypeface;
    private Typeface mCalibrationTextTypeface;
    private final int mStrokeColor = AndUtil.getResColor(R.color.uxsdk_black_30_percent);
    private final float mCalibrationLineStrokeWidth = (float) getResources().getDimensionPixelSize(R.dimen.uxsdk_1_dp) / 2;
    private final int mTextColor = AndUtil.getResColor(R.color.uxsdk_green_in_dark);

    protected Handler mDataHandler;

    protected ScrollableAttributeDashBoard(Context context) {
        this(context, null);
    }

    protected ScrollableAttributeDashBoard(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    protected ScrollableAttributeDashBoard(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            mDataHandler = new Handler(DJIExecutor.getLooper());
        }
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ScrollableAttributeDashBoard);
        mAttributeName = typedArray.getString(R.styleable.ScrollableAttributeDashBoard_uxsdk_name);
        mAttributeCalibrationTextSize = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_calibration_text_size,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_text_size_normal_medium));
        mPointerWidth = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_pointer_width,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_65_dp));
        mPointerHeight = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_pointer_height,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_21_dp));
        mFrameworkWidth = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_framework_width,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_25_dp));
        mFrameworkHeight = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_framework_height,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_117_dp));
        mFrameworkStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_framework_stroke_width,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_1_dp));
        mFrameworkPrimaryColor = typedArray.getColor(R.styleable.ScrollableAttributeDashBoard_uxsdk_framework_primary_color,
                getResources().getColor(R.color.uxsdk_green_in_dark));
        mFrameworkSecondaryColor = typedArray.getColor(R.styleable.ScrollableAttributeDashBoard_uxsdk_framework_secondary_color,
                getResources().getColor(R.color.uxsdk_green_in_dark_045));
        mFrameworkStrokeColor = typedArray.getColor(R.styleable.ScrollableAttributeDashBoard_uxsdk_framework_stroke_color,
                getResources().getColor(R.color.uxsdk_black_30_percent));
        mPointerLineInnerWidth = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_pointer_line_inner_width, 0);
        mPointerLineOuterWidth = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_pointer_line_outer_width,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_6_dp));
        mTextPadding = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_pointer_text_padding,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_2_dp));
        mFrameworkPaddingStart = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_calibration_framework_padding_start,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_21_dp));
        mFrameworkPaddingVercital =
                typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_calibration_framework_padding_vertical,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_3_dp));
        mDegreeLineLongWidth = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_degree_line_long,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_8_dp));
        mDegreeLineShortWidth = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_degree_line_short,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_6_dp));
        mAttributeUnitTextSize = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_unit_text_size,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_text_size_small));
        mAttributeNameTextSize = typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_name_text_size,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_text_size_normal));
        mAttributeCurrentCalibrationTextSize =
                typedArray.getDimensionPixelSize(R.styleable.ScrollableAttributeDashBoard_uxsdk_current_calibration_text_size,
                getResources().getDimensionPixelOffset(R.dimen.uxsdk_text_size_medium_large));
        mAttributePadding = typedArray.getDimension(R.styleable.ScrollableAttributeDashBoard_uxsdk_padding,
                getResources().getDimension(R.dimen.uxsdk_2_dp));
        mAttributeMaxValue = typedArray.getFloat(R.styleable.ScrollableAttributeDashBoard_uxsdk_max_value, Float.MAX_VALUE);
        mAttributeMinValue = typedArray.getFloat(R.styleable.ScrollableAttributeDashBoard_uxsdk_min_value, -Float.MAX_VALUE);
        mAttributeOffsetPerUnit = typedArray.getFloat(R.styleable.ScrollableAttributeDashBoard_uxsdk_offset_per_unit, DEFAULT_OFFSET_PER_UNIT);
        mCalibrationTextPadding = typedArray.getDimension(R.styleable.ScrollableAttributeDashBoard_uxsdk_calibration_text_padding,
                getResources().getDimension(R.dimen.uxsdk_2_dp));
        mShowBorder = typedArray.getBoolean(R.styleable.ScrollableAttributeDashBoard_uxsdk_show_border, false);
        mBorderColor = typedArray.getColor(R.styleable.ScrollableAttributeDashBoard_uxsdk_border_color,
                getResources().getColor(R.color.uxsdk_green_in_dark));
        mBorderWidth = typedArray.getDimension(R.styleable.ScrollableAttributeDashBoard_uxsdk_border_width,
                getResources().getDimension(R.dimen.uxsdk_0_6_dp));
        mBorderRadius = typedArray.getDimension(R.styleable.ScrollableAttributeDashBoard_uxsdk_border_radius,
                getResources().getDimension(R.dimen.uxsdk_2_dp));
        float currentValue = typedArray.getFloat(R.styleable.ScrollableAttributeDashBoard_uxsdk_current_value, 0);
        mCurrentValueColor = typedArray.getColor(R.styleable.ScrollableAttributeDashBoard_uxsdk_current_value_color, Color.WHITE);
        mCurrentValuePadding = typedArray.getDimension(R.styleable.ScrollableAttributeDashBoard_uxsdk_current_value_padding,
                getResources().getDimension(R.dimen.uxsdk_2_dp));
        mAttributePropertyColor = typedArray.getColor(R.styleable.ScrollableAttributeDashBoard_uxsdk_property_color, Color.WHITE);
        mAttributeDashBoardAlign = typedArray.getInt(R.styleable.ScrollableAttributeDashBoard_uxsdk_dash_board_align, DEFAULT_DASH_BOARD_ALIGN);
        mVisibleCalibrationUnitCount = typedArray.getInt(R.styleable.ScrollableAttributeDashBoard_uxsdk_visible_calibration_unit_count,
                DEFAULT_VISIBLE_CALIBRATION_UNIT_COUNT);
        mShowFramework = typedArray.getBoolean(R.styleable.ScrollableAttributeDashBoard_uxsdk_show_calibration_framework, true);
        typedArray.recycle();

        mDefaultTypeface = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        mCalibrationTextTypeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
        mPaint = new Paint(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
        mStrokePaint = new Paint(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
        restoreTypeface();

        mWaypointIconPadding = getResources().getDimension(R.dimen.uxsdk_1_dp);

        mFrameworkFrameValueColor = mFrameworkPrimaryColor;
        setCurrentValue(currentValue);

        mDisplayValue = new DisplayValue();
        mStrokeConfig = new FpvStrokeConfig(context);
    }

    private void restoreTypeface() {
        mPaint.setTypeface(mDefaultTypeface);
        mStrokePaint.setTypeface(mDefaultTypeface);
    }

    private void setCalibrationTypeface() {
        mPaint.setTypeface(mCalibrationTextTypeface);
        mStrokePaint.setTypeface(mCalibrationTextTypeface);
    }

    /**
     * 线条描边宽度
     */
    protected float getShadowLineStrokeWidth() {
        return mStrokeConfig.getStrokeThinWidth();
    }

    /**
     * 文字描边宽度
     */
    protected float getShadowTextStrokeWidth() {
        return mStrokeConfig.getStrokeBoldWidth();
    }

    /**
     * 线条描边颜色
     */
    protected int getShadowLineStrokeColor() {
        return mStrokeConfig.getStrokeShallowColor();
    }

    /**
     * 文字描边颜色
     */
    protected int getShadowTextStrokeColor() {
        return mStrokeConfig.getStrokeDeepColor();
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
        if (mDataHandler != null) {
            mDataHandler.removeCallbacksAndMessages(null);
        }
    }

    @MainThread
    public void setCurrentValue(float currentValue) {
        if (isInEditMode()){
            return;
        }
        if (currentValue > mAttributeMaxValue) {
            currentValue = mAttributeMaxValue;
        } else if (currentValue < mAttributeMinValue) {
            currentValue = mAttributeMinValue;
        }
        mCurrentValue = currentValue;
        final float value = currentValue;
        mDataHandler.post(() -> {
            calcDisplayValue(value);
            updateWidget();
        });
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
            return mFrameworkHeight + mFrameworkPaddingVercital * 2;
        } else {
            return mPointerHeight + mFrameworkStrokeWidth * 2;
        }
    }

    protected int getMinWidth() {
        return mPointerWidth + mFrameworkPaddingStart + mPointerLineInnerWidth + mTextPadding + mFrameworkStrokeWidth;
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
            drawCalibration(canvas);
            drawCalibrationFramework(canvas);
        }
    }

    protected float getCurrentValue() {
        return mCurrentValue;
    }

    protected boolean isShowFramework() {
        return mShowFramework;
    }

    /**
     * 裁切中间文本部分，避免和刻度重合
     *
     * @see #drawCurrentBackground(Canvas, int, int)
     */
    private void clipPointerArea(Canvas canvas) {
        int currentAreaOffsetX = mFrameworkPaddingStart + mPointerLineInnerWidth + mTextPadding;
        boolean alginLeft = isAlginLeft();
        if (!alginLeft) {
            currentAreaOffsetX = getWidth() - currentAreaOffsetX;
        }
        float currentAreaOffsetY = (getHeight() - mPointerHeight) / 2f;
        PATH.reset();
        PATH.addRoundRect(0, 0, alginLeft ? mPointerWidth : -mPointerWidth, mPointerHeight, mBorderRadius, mBorderRadius, Path.Direction.CW);
        PATH.offset(currentAreaOffsetX, currentAreaOffsetY);
        canvas.clipPath(PATH, Region.Op.DIFFERENCE);
    }


    private Path tmpPath = new Path();

    /**
     * 绘制左边框架
     *
     * @param canvas
     */
    private void drawCalibrationFramework(Canvas canvas) {
        RectF rect = new RectF();
        mPaint.setColor(mFrameworkPrimaryColor);
        mPaint.setStrokeWidth(mFrameworkStrokeWidth);
        mPaint.setStyle(Paint.Style.STROKE);
        int width = getWidth();
        boolean alginLeft = isAlginLeft();
        tmpPath.reset();

        float strokeWidth = mFrameworkStrokeWidth;
        float halfStrokeWidth = mFrameworkStrokeWidth / 2f;
        float shadowStrokeWidth = getShadowLineStrokeWidth();
        float halfShadowWidth = shadowStrokeWidth / 2;

        int offsetX = alginLeft ? mFrameworkPaddingStart : width - mFrameworkPaddingStart;
        float fw = alginLeft ? mFrameworkWidth : -mFrameworkWidth;
        // 设置坐标框架的四个点
        float top = (getHeight() - mFrameworkHeight) / 2f;
        rect.set(offsetX, top, offsetX + fw, top + mFrameworkHeight);
        // 获取框架的四个点坐标
        tmpPath.moveTo(rect.right, rect.top);
        tmpPath.lineTo(rect.left, rect.top);
        tmpPath.lineTo(rect.left, rect.bottom);
        tmpPath.lineTo(rect.right, rect.bottom);

        // 绘制框架
        mPaint.setColor(mFrameworkPrimaryColor);
        mPaint.setStrokeWidth(mFrameworkStrokeWidth);
        canvas.drawPath(tmpPath, mPaint);

        // 获取描边外框四点坐标
        rect.inset(-halfShadowWidth - halfStrokeWidth, -halfShadowWidth - halfStrokeWidth);
        tmpPath.reset();
        tmpPath.moveTo(rect.right, rect.top);
        tmpPath.lineTo(rect.left, rect.top);
        tmpPath.lineTo(rect.left, rect.bottom);
        tmpPath.lineTo(rect.right, rect.bottom);
        // 获取描边内框四个点坐标
        rect.inset(shadowStrokeWidth + strokeWidth, shadowStrokeWidth + strokeWidth);
        tmpPath.lineTo(rect.right, rect.bottom);
        tmpPath.lineTo(rect.left, rect.bottom);
        tmpPath.lineTo(rect.left, rect.top);
        tmpPath.lineTo(rect.right, rect.top);
        tmpPath.close();

        // 绘制框架描边
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(mFrameworkStrokeColor);
        mPaint.setStrokeWidth(getShadowLineStrokeWidth());
        canvas.drawPath(tmpPath, mPaint);
    }

    /**
     * @param canvas
     */
    private void drawCalibration(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        float centerY = height / 2f;
        float calibrationHorizontalMargin = (mPointerLineInnerWidth - mDegreeLineShortWidth) / 2f;
        float offsetMargin = (float) mFrameworkHeight / mVisibleCalibrationUnitCount;
        float currentPerUnitValue = mCurrentValue / mAttributeOffsetPerUnit;
        int currentPerUnitCount = (int) currentPerUnitValue;
        float currentValueOffset = (currentPerUnitValue - currentPerUnitCount) * offsetMargin;

        canvas.save();
        setCalibrationTypeface();

        float offsetY = currentValueOffset + centerY;
        mStrokePaint.setColor(mStrokeColor);
        mStrokePaint.setStrokeWidth(getShadowLineStrokeWidth());
        mStrokePaint.setStyle(Paint.Style.STROKE);
        boolean alignLeft = isAlginLeft();
        int offsetX = alignLeft ? mFrameworkPaddingStart : width - (mFrameworkPaddingStart);
        canvas.translate(offsetX, 0);
        for (int i = -mVisibleCalibrationUnitCount / 2; i <= mVisibleCalibrationUnitCount / 2; i++) {
            int currentCalibrationUnit = currentPerUnitCount - i;
            float y = i * offsetMargin;
            float calibrationValue = currentCalibrationUnit * mAttributeOffsetPerUnit;
            if (needDrawValue(calibrationValue)) {
                continue;
            }
            mPaint.setColor(mFrameworkSecondaryColor);
            mPaint.setStrokeWidth(mCalibrationLineStrokeWidth);
            mPaint.setStyle(Paint.Style.FILL);
            mStrokePaint.setColor(mStrokeColor);
            mStrokePaint.setStrokeWidth(getShadowLineStrokeWidth());
            mStrokePaint.setStyle(Paint.Style.STROKE);

            // 如果是0，则画长的刻度线，以及标上刻度，否则只画小的刻度线
            int drawDirect = alignLeft ? 1 : -1;
            float calibrationStartX = calibrationHorizontalMargin * drawDirect;
            boolean drawText = currentCalibrationUnit % SMALL_CALIBRATION_UNIT_COUNT_BETWEEN_TWO_BIG_CALIBRATION_UNIT == 0;
            int lineLength = drawDirect * (drawText ? mDegreeLineLongWidth : mDegreeLineShortWidth);
            float lineOffsetY = y + offsetY;
            if (lineInFramework(centerY, lineOffsetY)) {
                // 绘制范围超出框架高度范围
                continue;
            }
            drawCalibrationHorizontal(canvas, calibrationStartX, lineOffsetY, lineLength, drawText);
            if (drawText) {
                // 主要刻度文字
                float textStartX = 2 * calibrationStartX + lineLength + (alignLeft ? mCalibrationTextPadding : -mCalibrationTextPadding);
                drawCalibrationText(canvas, offsetMargin, lineOffsetY, calibrationValue, textStartX, alignLeft);
            }
        }
        restoreTypeface();
        canvas.restore();
    }

    private boolean needDrawValue(float calibrationValue) {
        return calibrationValue > mAttributeMaxValue || calibrationValue < mAttributeMinValue;
    }

    private boolean lineInFramework(float centerY, float lineOffsetY) {
        return lineOffsetY > centerY + mFrameworkHeight / 2f || lineOffsetY < centerY - mFrameworkHeight / 2f;
    }

    /**
     * 绘制刻度文字
     */
    private void drawCalibrationText(Canvas canvas, float offsetMargin, float y, float calibrationValue, float textStartX, boolean alignLeft) {
        mPaint.setTextAlign(Paint.Align.LEFT);
        mPaint.setTextSize(mAttributeCalibrationTextSize);
        int value = Math.round(calibrationValue);
        String currentValueString;
        if (value < 0) {
            currentValueString = value > -10 ? "-0" + value : "" + value;
        } else {
            currentValueString = value < 10 ? "0" + value : "" + value;
        }
        mPaint.getTextBounds(currentValueString, 0, currentValueString.length(), RECT);
        Paint.FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
        float baseline = (offsetMargin - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top;
        drawTextWithStroke(canvas, currentValueString, alignLeft ? textStartX : textStartX - RECT.width(), baseline + y - offsetMargin / 2,
                getShadowLineStrokeWidth(), mStrokeColor, mTextColor);
    }

    /**
     * 绘制小刻度线
     */
    private void drawCalibrationHorizontal(Canvas canvas, float x, float y, int length, boolean needText) {
        float width = mPaint.getStrokeWidth();
        // 绘制区域，刻度线不与框架相交
        if (y - width / 2f < (getHeight() - mFrameworkHeight) / 2f - mFrameworkStrokeWidth / 2f || y + width / 2 > (getHeight() + mFrameworkHeight) / 2f - mFrameworkStrokeWidth / 2f) {
            return;
        }
        mPaint.setColor(needText ? mFrameworkFrameValueColor : mFrameworkSecondaryColor);
        canvas.drawLine(x, y, x + length, y, mPaint);
        canvas.drawRect(x, y - width, x + length, y + width, mStrokePaint);
    }

    protected int getIndicatorLineLength() {
        return 0;
    }

    /**
     * 绘制当前值区域
     *
     * @param canvas
     */
    private void drawCurrentValue(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        DisplayValue displayValue;
        synchronized (mDisplayValueLock) {
            displayValue = mDisplayValue;
        }
        if (displayValue == null || displayValue.mDisplayValue == null || displayValue.mDisplayUnit == null) {
            return;
        }
        String currentValueString = displayValue.mDisplayValue;
        int canvasMark = canvas.save();

        boolean alginLeft = isAlginLeft();
        int alginDirect = alginLeft ? 1 : -1;

        // 移动画布到当前值绘制区域，向左时为左上点，向右时为右上点
        int currentAreaOffsetX = mFrameworkPaddingStart + mPointerLineInnerWidth + mTextPadding;
        if (!alginLeft) {
            currentAreaOffsetX = width - currentAreaOffsetX;
        }
        float currentAreaOffsetY = (height - mPointerHeight) / 2f;
        canvas.translate(currentAreaOffsetX, currentAreaOffsetY);

        int currentWidth = mPointerWidth * alginDirect;
        if (mShowBorder) {
            drawCurrentValueLine(canvas, mPointerHeight, alginLeft);
            drawCurrentBackground(canvas, currentWidth, mPointerHeight);
        }
        drawCurrentText(canvas, currentValueString, alginLeft);
        drawCurrentName(canvas, mAttributeName, alginLeft);
        drawCurrentUnit(canvas, displayValue.mDisplayUnit, alginLeft);

        canvas.restoreToCount(canvasMark);
    }

    private void drawCurrentValueLine(Canvas canvas, int pointerHeight, boolean alginLeft) {
        int lineLength = mPointerLineInnerWidth + mTextPadding;
        float left;
        float top;
        float right;
        float bottom;
        if (alginLeft) {
            left = -lineLength;
            right = 0;
        } else {
            left = 0;
            right = lineLength;
        }
        Paint backgroundPaint = getBackgroundPaint();
        float lineWidth = backgroundPaint.getStrokeWidth();
        float centerY = pointerHeight / 2f;
        top = centerY - lineWidth / 2;
        bottom = centerY + lineWidth / 2;
        float lineY = centerY;
        canvas.drawLine(left, lineY, right, lineY, backgroundPaint);
        float strokeWidth = getShadowLineStrokeWidth();
        mStrokePaint.setColor(getShadowLineStrokeColor());
        mStrokePaint.setStrokeWidth(strokeWidth);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        lineY = top - strokeWidth / 2;
        canvas.drawLine(left, lineY, right, lineY, backgroundPaint);
        lineY = bottom + strokeWidth / 2;
        canvas.drawLine(left, lineY, right, lineY, backgroundPaint);
    }

    protected DisplayValue calcDisplayValue(float currentValue) {
        float displayValue = getDisplayValue(currentValue);
        // 数字比较添加 0.5 是为了避免出现 999.95 显示成 1000.0
        boolean shorthand = displayValue + 0.05f > 1000;
        final String unit = getAttributeUnit();
        final String value = String.format(getCurrentValueDisplayFormat(shorthand), displayValue);
        synchronized (mDisplayValueLock) {
            if (mDisplayValue == null) {
                mDisplayValue = new DisplayValue();
            }
            mDisplayValue.set(value, unit);
        }
        return mDisplayValue;
    }

    private boolean isAlginLeft() {
        return mAttributeDashBoardAlign == DASH_BOARD_ALIGN_LEFT;
    }

    /**
     * 绘制当前值单位
     */
    private void drawCurrentUnit(Canvas canvas, String text, boolean alignLeft) {
        Paint paint = getUnitPaint();
        float len = paint.measureText(text);
        // 让属性单位紧贴顶部
        float unitTextOffset;
        if (alignLeft) {
            unitTextOffset = mPointerWidth - len - mAttributePadding;
        } else {
            unitTextOffset = -mPointerWidth + mAttributePadding;
        }
        float unitTextBaseline = FontUtils.getDigitalBaselineFromTop(paint, mPointerHeight / 2f) + 1;
        drawTextWithStroke(canvas, text, unitTextOffset, unitTextBaseline, getShadowTextStrokeWidth(), getShadowTextStrokeColor(),
                mAttributePropertyColor);
    }

    private Paint getUnitPaint() {
        // 画出属性单位
        mPaint.setTextSize(mAttributeUnitTextSize);
        return mPaint;
    }

    /**
     * 绘制当前值名称
     */
    private void drawCurrentName(Canvas canvas, String text, boolean alginLeft) {
        Paint paint = getCurrentNamePaint();
        float len = paint.measureText(text);
        float attrTextOffset;
        if (alginLeft) {
            attrTextOffset = mPointerWidth - len - mAttributePadding;
        } else {
            attrTextOffset = -mPointerWidth + mAttributePadding;
        }
        float baseline = FontUtils.getDigitalBaselineFromBottom(mPaint, mPointerHeight / 2f) - 1;
        drawTextWithStroke(canvas, text, attrTextOffset, baseline, getShadowTextStrokeWidth(), getShadowTextStrokeColor(), mAttributePropertyColor);
    }

    private Paint getCurrentNamePaint() {
        mPaint.setTextSize(mAttributeNameTextSize);
        mPaint.setColor(Color.WHITE);
        return mPaint;
    }

    /**
     * 绘制当前值文字
     * 当前文字为数字和小数点，行高通过 ascent 和 descent 计算，避免 top 和 bottom 过多留白导致的整体偏离过远
     */
    private void drawCurrentText(Canvas canvas, String text, boolean alginLeft) {
        Paint paint = getCurrentPaint();
        float len = paint.measureText(text);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float top = fontMetrics.ascent;
        float bottom = fontMetrics.descent;

        float baseline = ((float) mPointerHeight - bottom + top) / 2 - top;
        float textStart = alginLeft ? mCurrentValuePadding : -mCurrentValuePadding - len;
        drawTextWithStroke(canvas, text, textStart, baseline, mStrokeConfig.getStrokeBoldWidth(), getShadowTextStrokeColor(), mCurrentValueColor);
    }

    private Paint getCurrentPaint() {
        Paint paint = mPaint;
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(mAttributeCurrentCalibrationTextSize);
        return paint;
    }

    /**
     * 绘制中间文字部分背景，后续会对这一部分区域进行裁切，避免覆盖/重叠
     *
     * @see #clipPointerArea(Canvas)
     */
    private void drawCurrentBackground(Canvas canvas, int width, int height) {
        Paint backgroundPaint = getBackgroundPaint();
        canvas.drawRoundRect(0, 0, width, height, mBorderRadius, mBorderRadius, backgroundPaint);
    }

    private Paint getBackgroundPaint() {
        mPaint.setColor(mBorderColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mBorderWidth);
        return mPaint;
    }

    protected void drawTextWithStroke(Canvas canvas, String currentValueString, float textOffset, float baseline, float strokeWidth,
                                      int strokeColor, int textColor) {
        mPaint.setStrokeWidth(strokeWidth);
        mPaint.setColor(strokeColor);
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawText(currentValueString, textOffset, baseline, mPaint);
        mPaint.setColor(textColor);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawText(currentValueString, textOffset, baseline, mPaint);
    }

    protected abstract String getCurrentValueDisplayFormat(boolean shorthand);

    protected abstract String getAttributeUnit();

    protected abstract float getDisplayValue(float value);

    protected static class DisplayValue {
        String mDisplayValue;
        String mDisplayUnit;

        public void set(String displayValue, String displayUnit) {
            mDisplayValue = displayValue;
            mDisplayUnit = displayUnit;
        }
    }

}
