package dji.v5.ux.core.widget.hsi;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.common.DoubleMinMax;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.widget.FrameLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.ui.hsi.dashboard.FpvStrokeConfig;
import dji.v5.ux.core.util.FontUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 云台Pitch角度展示Widget
 * <p>
 * 注意：GimbalPitchBarWidget必须关联到相机，所以只能使用在主界面，比如DefaultLayoutActivity中
 */
public class GimbalPitchBarWidget extends FrameLayoutWidget<Boolean> implements ICameraIndex {
    protected int mMinValue = -120;
    protected int mMaxValue = 120;
    protected int mValue = 0;
    protected boolean mInvalidDrawable = true;
    private Paint mBarPaint;
    private Paint mBarStrokePaint;
    private Paint mHighlightPaint;
    private Paint mInnerPaint;
    private Paint mOnBarPaint;
    private Paint mTextPaint;
    private Paint mTextStrokePaint;

    /**
     * 主标尺宽度
     */
    private float mBarWidth;
    /**
     * 原点宽度
     */
    private float mOriginWidth;
    /**
     * 原点长度
     */
    private float mOriginLength;
    /**
     * 刻度条
     */
    private int[] mHighlightValues;
    /**
     * 文字高亮颜色
     */
    private int mTextHighlightColor;
    /**
     * 内部的指示条
     */
    private float mInnerWidth;
    private int mInnerValue;
    private int mInnerMaxValue;
    private int mOnBarMaxValue;
    private int[] mOnBarColors;
    /**
     * 叠加的指示条
     */
    @Nullable
    private int[] mOnBarIndicator;

    /**
     * 整体的边距
     */
    private float mOriginPadding;
    /**
     * 当前值刻度指示图标
     */
    private Drawable mIndicatorDrawable;
    /**
     * 元素朝向
     */
    private boolean mAlignLeft;

    /**
     * 数值是否在顶部
     */
    private boolean mTextOnTop;

    /**
     * 云台指示图标放大倍数
     */
    private float mDrawableSizeFactor = 1.0f;

    /**
     * 绘制时存储主标尺的位置不含描边
     */
    private final RectF mBarRect = new RectF();
    /**
     * 绘制时候的临时对象，使用前需赋值
     */
    private final RectF mTmpRect = new RectF();

    private String mText;
    private float mTextSize;
    private float mTextPadding;

    private FpvStrokeConfig mStrokeConfig;
    private ComponentIndexType componentIndexType = ComponentIndexType.UNKNOWN;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private GimbalPitchBarModel widgetModel = new GimbalPitchBarModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());

    public GimbalPitchBarWidget(Context context) {
        this(context, null);
    }

    public GimbalPitchBarWidget(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GimbalPitchBarWidget(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private int getColor(int colorId) {
        return getResources().getColor(colorId);
    }

    private float getDimen(int dimenId) {
        return getResources().getDimension(dimenId);
    }

    /**
     * @see #drawBackground(Canvas, RectF)
     */
    protected void initParams(Context context, AttributeSet attrs) {
        super.setWillNotDraw(false);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.GimbalPitchBarV2);
        // 方向
        mAlignLeft = typedArray.getBoolean(R.styleable.GimbalPitchBarV2_uxsdk_gpb_align_left, false);
        // 主标尺
        mMaxValue = typedArray.getInt(R.styleable.GimbalPitchBarV2_uxsdk_gpb_bar_max_value, 0);
        mMinValue = typedArray.getInt(R.styleable.GimbalPitchBarV2_uxsdk_gpb_bar_min_value, 0);
        mValue = typedArray.getInt(R.styleable.GimbalPitchBarV2_uxsdk_gpb_bar_value, 0);
        mText = typedArray.getString(R.styleable.GimbalPitchBarV2_uxsdk_gpb_bar_text);
        mTextSize = typedArray.getDimension(R.styleable.GimbalPitchBarV2_uxsdk_gpb_bar_text_size, getDimen(R.dimen.uxsdk_7_dp));
        mTextPadding = typedArray.getDimension(R.styleable.GimbalPitchBarV2_uxsdk_gpb_bar_text_padding, getDimen(R.dimen.uxsdk_2_dp));
        mTextOnTop = typedArray.getBoolean(R.styleable.GimbalPitchBarV2_uxsdk_gpb_bar_text_on_top, false);
        mBarWidth = typedArray.getDimension(R.styleable.GimbalPitchBarV2_uxsdk_gpb_bar_width, getDimen(R.dimen.uxsdk_3_dp));
        // 主标尺颜色
        int barColor = typedArray.getColor(R.styleable.GimbalPitchBarV2_uxsdk_gpb_bar_color, getColor(R.color.uxsdk_white_10_percent));
        // 主标尺描边宽度
        float barStrokeWidth = typedArray.getDimension(R.styleable.GimbalPitchBarV2_uxsdk_gpb_bar_stroke_width, getDimen(R.dimen.uxsdk_0_5_dp));
        // 主标尺描边颜色
        int barStrokeColor = typedArray.getColor(R.styleable.GimbalPitchBarV2_uxsdk_gpb_bar_stroke_color, getColor(R.color.uxsdk_black_30_percent));
        // 原点
        mOriginPadding = typedArray.getDimension(R.styleable.GimbalPitchBarV2_uxsdk_gpb_bar_origin_padding, getDimen(R.dimen.uxsdk_2_dp));
        mOriginWidth = typedArray.getDimension(R.styleable.GimbalPitchBarV2_uxsdk_gpb_origin_width, getDimen(R.dimen.uxsdk_1_dp));
        mOriginLength = typedArray.getDimension(R.styleable.GimbalPitchBarV2_uxsdk_gpb_origin_length, getDimen(R.dimen.uxsdk_6_dp));
        int highlightValuesId = typedArray.getResourceId(R.styleable.GimbalPitchBarV2_uxsdk_gpb_highlight_values, 0);
        if (highlightValuesId != 0) {
            mHighlightValues = getResources().getIntArray(highlightValuesId);
        }
        /* 高亮刻度 */
        // 刻度条宽度
        float highlightWidth = typedArray.getDimension(R.styleable.GimbalPitchBarV2_uxsdk_gpb_highlight_width, getDimen(R.dimen.uxsdk_0_5_dp));
        // 刻度条颜色
        int highlightColor = typedArray.getColor(R.styleable.GimbalPitchBarV2_uxsdk_gpb_highlight_color, getColor(R.color.uxsdk_white));
        mIndicatorDrawable = typedArray.getDrawable(R.styleable.GimbalPitchBarV2_uxsdk_gpb_indicator_drawable);
        mDrawableSizeFactor = typedArray.getFloat(R.styleable.GimbalPitchBarV2_uxsdk_gpb_indicator_drawable_factor, 1.0f);
        // 内部指示条
        mInnerWidth = typedArray.getDimension(R.styleable.GimbalPitchBarV2_uxsdk_gpb_inner_width, getDimen(R.dimen.uxsdk_3_dp));
        mInnerValue = typedArray.getInt(R.styleable.GimbalPitchBarV2_uxsdk_gpb_inner_value, 0);
        mInnerMaxValue = typedArray.getInt(R.styleable.GimbalPitchBarV2_uxsdk_gpb_inner_max_value, 0);
        // 色块指示条
        mOnBarMaxValue = typedArray.getInt(R.styleable.GimbalPitchBarV2_uxsdk_gpb_on_bar_max_value, 0);
        int onBarValues = typedArray.getResourceId(R.styleable.GimbalPitchBarV2_uxsdk_gpb_on_bar_values, 0);
        if (onBarValues != 0) {
            mOnBarIndicator = getResources().getIntArray(onBarValues);
        }
        int onBarColors = typedArray.getResourceId(R.styleable.GimbalPitchBarV2_uxsdk_gpb_on_bar_colors, 0);
        if (onBarColors != 0) {
            mOnBarColors = getResources().getIntArray(onBarColors);
        }
        typedArray.recycle();

        mTextHighlightColor = getResources().getColor(R.color.uxsdk_yellow_in_light);

        mStrokeConfig = new FpvStrokeConfig(context);

        Typeface typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD);

        // 背景画笔
        Paint barPaint = new Paint(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setColor(barColor);
        mBarPaint = barPaint;

        Paint barStrokePaint = new Paint(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
        barStrokePaint.setColor(barStrokeColor);
        barStrokePaint.setStyle(Paint.Style.STROKE);
        barStrokePaint.setStrokeWidth(barStrokeWidth);
        mBarStrokePaint = barStrokePaint;

        Paint highlightPaint = new Paint(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(highlightColor);
        highlightPaint.setStyle(Paint.Style.FILL);
        highlightPaint.setStrokeWidth(highlightWidth);
        mHighlightPaint = highlightPaint;

        Paint textPaint = new Paint(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setStrokeWidth(10);
        textPaint.setTextSize(mTextSize);
        textPaint.setTypeface(typeface);
        mTextPaint = textPaint;

        Paint paint = new Paint(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
        paint.setColor(mStrokeConfig.getStrokeDeepColor());
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(mStrokeConfig.getStrokeBoldWidth());
        paint.setTextSize(mTextSize);
        paint.setTypeface(typeface);
        mTextStrokePaint = paint;

        Paint onBarPaint = new Paint(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
        onBarPaint.setStyle(Paint.Style.FILL);
        mOnBarPaint = onBarPaint;

        Paint innerPaint = new Paint(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
        innerPaint.setStyle(Paint.Style.FILL);
        innerPaint.setColor(Color.WHITE);
        mInnerPaint = innerPaint;

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        RectF bar = mBarRect;
        if (mAlignLeft) {
            bar.right = mOriginPadding + mOriginLength + mBarWidth;
        } else {
            bar.right = getWidth() - (mOriginPadding + mOriginLength) + mBarWidth;
        }
        bar.left = bar.right - mBarWidth;
        bar.top = getPaddingTop();
        bar.bottom = (float) getHeight() - getPaddingBottom();

        drawBackground(canvas, bar);
        if (mMaxValue != mMinValue) {
            drawHighlightValues(canvas, bar);
        }
        drawOnBarIndicator(canvas, bar);
        drawInnerIndicator(canvas, bar);

        if (mOriginLength > 0) {
            drawOriginValue(canvas, bar);
        }
        if (mMaxValue != mMinValue) {
            drawIndicator(canvas, bar);
        }
        drawCurrentValue(canvas, bar);

    }

    private void drawCurrentValue(Canvas canvas, RectF bar) {
        if (TextUtils.isEmpty(mText)) {
            return;
        }
        float len = mTextPaint.measureText(mText);
        float baseline;
        float left;
        if (mTextOnTop) {
            baseline = FontUtils.getDigitalBaselineFromBottom(mTextPaint, bar.top - mTextPadding) - mTextPaint.getFontMetrics().bottom;
        } else {
            baseline = FontUtils.getDigitalBaselineFromTop(mTextPaint, bar.top);
        }
        if (mTextOnTop) {
            left = (bar.left + bar.right - len) / 2;
            if (!mAlignLeft) {
                left = Math.min(left, getWidth() - len);
            }
        } else {
            left = mAlignLeft ? bar.right + mTextPadding : bar.left - mTextPadding - len;
        }
        canvas.drawText(mText, left, baseline, mTextStrokePaint);
        canvas.drawText(mText, left, baseline, mTextPaint);
    }

    private void drawInnerIndicator(Canvas canvas, RectF rect) {
        if (mInnerMaxValue == 0) {
            return;
        }
        int lastValue = mInnerValue;
        float centerY = rect.centerY();
        float lastY = centerY - innerValue2Height(rect.height(), lastValue);
        float left;
        if (mAlignLeft) {
            left = rect.right;
        } else {
            left = rect.left - mInnerWidth;
        }
        float right = left + mInnerWidth;
        canvas.drawRect(left, centerY, right, lastY, mInnerPaint);
    }

    private void drawOnBarIndicator(Canvas canvas, RectF rect) {
        if (mOnBarMaxValue == 0) {
            return;
        }
        int[] onBarIndicator = mOnBarIndicator;
        if (onBarIndicator == null || onBarIndicator.length < 2) {
            return;
        }
        if (mOnBarColors == null || mOnBarColors.length == 0) {
            return;
        }
        float centerY = rect.centerY();
        float lastY = centerY - onBarValue2Height(rect.height(), onBarIndicator[0]);
        float currentY;
        for (int i = 1; i < onBarIndicator.length; i++) {
            currentY = centerY - onBarValue2Height(rect.height(), onBarIndicator[i]);
            mOnBarPaint.setColor(mOnBarColors[(i - 1) % mOnBarColors.length]);
            canvas.drawRect(rect.left, lastY, rect.right, currentY, mOnBarPaint);
            lastY = currentY;
        }
    }

    private void drawOriginValue(Canvas canvas, RectF rect) {
        float barHeight = rect.height();
        float halfStrokeWidth = mBarStrokePaint.getStrokeWidth() / 2;
        float y;
        if (getMaxValue() == getMinValue()) {
            y = rect.centerY();
        } else {
            y = indicatorValue2Height(barHeight, 0) + rect.top;
        }
        float left;
        float right;
        if (mAlignLeft) {
            right = rect.right - halfStrokeWidth;
            left = right - Math.max(mOriginLength, rect.width());
        } else {
            left = rect.left + halfStrokeWidth;
            right = left + Math.max(rect.width(), mOriginLength);
        }
        float halfWidth = mOriginWidth / 2;
        canvas.drawRect(left, y - halfWidth, right, y + halfWidth, mHighlightPaint);
        canvas.drawRect(left - halfStrokeWidth, y - halfWidth - halfStrokeWidth, right + halfStrokeWidth, y + halfWidth + halfStrokeWidth,
                mBarStrokePaint);
    }

    private void drawIndicator(Canvas canvas, RectF rect) {
        Drawable drawable = mIndicatorDrawable;
        if (drawable == null) {
            return;
        }
        float barHeight = rect.height();
        float y = indicatorValue2Height(barHeight, getValue()) + rect.top;

        float width = drawable.getMinimumWidth() * mDrawableSizeFactor;
        float height = drawable.getMinimumHeight() * mDrawableSizeFactor;
        int right;
        if (mAlignLeft) {
            right = (int) (rect.left + width);
        } else {
            right = (int) rect.right;
        }
        int left = (int) (right - width);
        int top = (int) (y - height / 2f);
        int bottom = (int) (top + height);
        drawable.setBounds(left, top, right, bottom);
        drawable.draw(canvas);
    }

    private void drawHighlightValues(Canvas canvas, RectF rect) {
        if (mHighlightValues == null) {
            return;
        }
        float barHeight = rect.height();
        float halfStrokeWidth = mBarStrokePaint.getStrokeWidth() / 2;
        float halfHighLightWidth = mHighlightPaint.getStrokeWidth() / 2;
        for (int highlightValue : mHighlightValues) {
            float highlightY = indicatorValue2Height(barHeight, highlightValue) + rect.top;
            if (highlightValue != 0 || mOriginLength == 0) {
                canvas.drawRect(rect.left + halfStrokeWidth, highlightY - halfStrokeWidth - halfHighLightWidth, rect.right - halfStrokeWidth,
                        highlightY + halfStrokeWidth + halfHighLightWidth, mBarStrokePaint);
                canvas.drawLine(rect.left + halfStrokeWidth, highlightY, rect.right - halfStrokeWidth, highlightY, mHighlightPaint);
            }
        }
    }

    private float indicatorValue2Height(float barHeight, int value) {
        return barHeight * (getMaxValue() - value) / (getMaxValue() - getMinValue());
    }

    private float onBarValue2Height(float barHeight, int value) {
        return value * barHeight / 2f / mOnBarMaxValue;
    }

    private float innerValue2Height(float barHeight, int value) {
        return value * barHeight / 2f / mInnerMaxValue;
    }

    private void drawBackground(Canvas canvas, RectF bar) {
        mTmpRect.set(bar);
        float halfStrokeWidth = mBarStrokePaint.getStrokeWidth() / 2f;
        mTmpRect.inset(-halfStrokeWidth, -halfStrokeWidth);
        canvas.drawRect(mTmpRect, mBarStrokePaint);
        canvas.drawRect(bar, mBarPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mInvalidDrawable = true;
    }

    protected void onValueChanged(int value) {
        mTextPaint.setColor(isTextHighLight(value) ? mTextHighlightColor : Color.WHITE);
        mText = value + "°";
    }

    /**
     * 云台度数是否需要高亮
     */
    private boolean isTextHighLight(int value) {
        return value == 0 || value == -45 || value == -90;
    }

    public int getOnBarMaxValue() {
        return mOnBarMaxValue;
    }

    public int[] getOnBarIndicator() {
        return mOnBarIndicator;
    }

    public void setOnBarIndicator(int[] onBarIndicator) {
        mOnBarIndicator = onBarIndicator;
    }

    public int[] getHighlightValues() {
        return mHighlightValues;
    }

    public void setHighlightValues(int[] highlightValues) {
        mHighlightValues = highlightValues;
    }


    protected void init(Context context, AttributeSet attrs) {
        initParams(context, attrs);
    }

    public int getMinValue() {
        return mMinValue;
    }

    public void setMinValue(int minValue) {
        mMinValue = minValue;
        mInvalidDrawable = true;
        postInvalidate();
    }

    public int getMaxValue() {
        return mMaxValue;
    }

    public void setMaxValue(int maxValue) {
        mMaxValue = maxValue;
        mInvalidDrawable = true;
        postInvalidate();
    }

    public int getValue() {
        return mValue;
    }

    public void setValue(int value) {
        if (mValue != value) {
            mValue = value;
            if (mValue > mMaxValue) {
                mValue = mMaxValue;
            }
            if (value < mMinValue) {
                mValue = mMinValue;
            }
        }
        onValueChanged(value);
        postInvalidate();
    }

    public void setGimbalDrawable(ComponentIndexType cameraIndex) {
        int resId;
        if (cameraIndex == ComponentIndexType.LEFT_OR_MAIN) {
            resId = R.drawable.uxsdk_fpv_hsi_pitch_guide_gimbal_3;
        } else if (cameraIndex == ComponentIndexType.RIGHT) {
            resId = R.drawable.uxsdk_fpv_hsi_pitch_guide_gimbal_1;
        } else if (cameraIndex == ComponentIndexType.UP) {
            resId = R.drawable.uxsdk_fpv_hsi_pitch_guide_gimbal_2;
        } else {
            resId = 0;
        }
        mIndicatorDrawable = resId == 0 ? null : getResources().getDrawable(resId);
    }

    @NonNull
    @Override
    public ComponentIndexType getCameraIndex() {
        return widgetModel.getCameraIndex();
    }

    @NonNull
    @Override
    public CameraLensType getLensType() {
        return widgetModel.getLensType();
    }

    @Override
    public void updateCameraSource(@NonNull ComponentIndexType cameraIndex, @NonNull CameraLensType lensType) {
        widgetModel.updateCameraSource(cameraIndex, lensType);

        Log.e("testMM", "updateCameraSource" + componentIndexType + "---" + cameraIndex);
        if (componentIndexType != cameraIndex) {
            componentIndexType = cameraIndex;
            onStop();
            onStart(cameraIndex);

        }
    }


    private void startListener() {
        if (getCameraIndex().value() >= 3 || widgetModel.getGimbalAttitudeInDegreesProcessorList().size() <= 0) {
            return;
        }

        compositeDisposable.add(widgetModel.getGimbalAttitudeInDegreesProcessorList().get(getCameraIndex().value()).toFlowable()
                .observeOn(AndroidSchedulers.mainThread())
                .map(attitude -> (int) Math.round(attitude.getPitch()))
                .subscribeOn(Schedulers.io())
                .subscribe(this::setValue));

        compositeDisposable.add(widgetModel.getGimbalAttitudeGimbalAttitudeRangeProcessorList().get(getCameraIndex().value()).toFlowable().subscribe(gimbalAttitudeRange -> {
            DoubleMinMax pitch = gimbalAttitudeRange.getPitch();
            if (pitch == null){
                return;
            }
            int pitchMax = (int) Math.round(pitch.getMax());
            int pitchMin = (int) Math.round(pitch.getMin());
            int absMax = Math.abs(pitchMax);
            int absMin = Math.abs(pitchMin);
            int max = Math.max(absMax, absMin);
            int min = -max;
            setMaxValue(max);
            setMinValue(min);
            if (absMax < absMin) {
                setHighlightValues(new int[]{30, -45, -90});
            } else {
                setHighlightValues(new int[]{-30, 45, 90});
            }
        }));
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            widgetModel.setup();
            startListener();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
        onStop();
    }

    private void onStart(ComponentIndexType cameraIndex) {
        if (compositeDisposable != null) {
            compositeDisposable.clear();
        } else {
            compositeDisposable = new CompositeDisposable();
        }
        setGimbalDrawable(cameraIndex);
        startListener();
    }

    private void onStop() {
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
            compositeDisposable = null;
        }
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        //do nothing
    }

    @Override
    protected void reactToModelChanges() {
        //do nothing
    }

    @Nullable
    @Override
    public String getIdealDimensionRatioString() {
        return null;
    }


    // endregion
}
