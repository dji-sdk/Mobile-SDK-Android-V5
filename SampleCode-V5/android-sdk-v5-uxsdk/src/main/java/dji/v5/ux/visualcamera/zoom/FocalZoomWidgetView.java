package dji.v5.ux.visualcamera.zoom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;

import java.math.BigDecimal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.ICameraIndex;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.ux.core.base.widget.ViewWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/8/31
 * <p>
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
public class FocalZoomWidgetView extends ViewWidget implements ICameraIndex {

    private static final int FOCAL_CHECK_MESSAGE_TYPE = 0;
    private static final int FOCAL_CHANGE_TIMEOUT = 1000;
    private static final int FOCAL_TOUCH_TIMEOUT = 7000;
    private static final int FOCAL_CLICK_INTERVAL = 200;
    private static final int MIN_VELOCITY = 100;
    private static final float THRESHOLD = 0.000001F;
    private int[] mFocalLengthGears = new int[]{2, 5, 10, 20, 40, 80, 160, 200};
    private final int[] mStepGears = new int[]{10, 16, 25, 250};
    private final float[] mStepValues = new float[]{0.1f, 0.2f, 0.3f, 0.5f};

    private int mCurrentLevel = -1;
    private Handler myHandler;

    private float mCurrentScreenFocalMultiTimes = mFocalLengthGears[2];
    private float mCurrentDroneFocalMultiTimes;

    private float mHighLevelValue;
    private float mLowLevelValue;
    private float mMiddleLevelValue;

    private int mViewWidth;
    private int mViewHeight;

    private Paint mBackgroundPaint;
    private Paint mMarkTextPaint;
    private Paint mDotPaint;
    private Paint mSliderPaint;
    private Paint mSliderTextPaint;
    private Paint mSliderMarkTextPaint;
    private Paint mDividerPaint;
    private Paint mSymbolBitmapPaint;

    private int mBackgroudColor;
    private int mBackgroudShadowColor;
    private int mBackgroudShadowSizeDP = 2;
    private int mBackgroudRadiusDP = 2;
    private int mBackgroundPaddingDP = 2;

    private int mMarkTextColor;
    private int mMarkTextFontSizeDP = 12;

    private int mDotColor;
    private int mDotRadiusDP = 2;
    private int mDotGap_dp = 4;

    private int mSliderColor;
    private int mSliderRadiusDP = 2;
    private int mSliderShadowSizeDP = 2;
    private int mSliderTextColor;
    private int mSliderNumTextFontSizDP = 12;
    private int mSliderMarkTextFontSizDP = 6;
    private int mSliderHeightDP = mSliderNumTextFontSizDP + mSliderMarkTextFontSizDP + 12;

    private int mDividerColor;
    private int mDividerPaddingDP = 2 + mBackgroundPaddingDP;
    private int mDividerHeightDP = 1;

    private Bitmap mPlusBitmap;
    private Bitmap mMinusBitmap;
    private Bitmap mPlusBitmapUnclick;
    private Bitmap mMinusBitmapUnclick;

    private int mSymbolImageSize;
    private float mUpMartTextY;
    private float mDownMarkTextY;
    private float mUpDividerY;
    private float mDownDividerY;
    private float mDensity;
    private float mSliderTopY;
    private float mSliderBottomY;
    private float mMoveStartY;
    private float step = 0.1f;
    private boolean mIsStartSlowMove = false;
    private boolean mIsStartQuickMove = false;

    private VelocityTracker mVelocityTracker;
    private float mTouchDownY = 0;
    private float mTouchDownTime = 0;
    private long mFinalTouchTime;
    private boolean mIsInited;
    private FocalZoomWidgetViewModel widgetModel;

    public FocalZoomWidgetView(Context context) {
        this(context, null, 0);
    }

    public FocalZoomWidgetView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FocalZoomWidgetView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mIsInited = false;
        initPaint();
        initHandler();
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        if (!isInEditMode()) {
            widgetModel = new FocalZoomWidgetViewModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());
        }
    }

    @Override
    protected void reactToModelChanges() {
        addDisposable(widgetModel.focalZoomRatios.toFlowable()
                .observeOn(SchedulerProvider.ui())
                .subscribe(ratios -> pushFocalLength(ratios.floatValue())));
        addDisposable(widgetModel.focalZoomRatiosRange.toFlowable()
                .observeOn(SchedulerProvider.ui())
                .subscribe(range -> pushFocalLengthRange(range.getGears())));
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
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            widgetModel.setup();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
        super.onDetachedFromWindow();
    }

    private void initPaint() {
        mBackgroudColor = getContext().getResources().getColor(R.color.uxsdk_black_60_percent);
        mBackgroudShadowColor = getContext().getResources().getColor(R.color.uxsdk_zoom_slider_background_shadow);
        mMarkTextColor = getContext().getResources().getColor(R.color.uxsdk_zoom_slider_color);
        mDotColor = getContext().getResources().getColor(R.color.uxsdk_zoom_slider_dot_color);
        mSliderColor = getContext().getResources().getColor(R.color.uxsdk_zoom_slider_color);
        mSliderTextColor = getContext().getResources().getColor(R.color.uxsdk_white);
        mDividerColor = getContext().getResources().getColor(R.color.uxsdk_zoom_slider_dot_color);

        mPlusBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.uxsdk_focal_zoom_slider_plus);
        mMinusBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.uxsdk_focal_zoom_slider_minus);
        mPlusBitmapUnclick = BitmapFactory.decodeResource(getResources(), R.drawable.uxsdk_focal_zoom_slider_plus_unclick);
        mMinusBitmapUnclick = BitmapFactory.decodeResource(getResources(), R.drawable.uxsdk_focal_zoom_slider_minus_unclick);

        mDensity = getContext().getResources().getDisplayMetrics().density;
        resetDimen();

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(mBackgroudColor);
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        mBackgroundPaint.setAntiAlias(true);
        mBackgroundPaint.setShadowLayer(mBackgroudShadowSizeDP * mDensity, 0, 0, mBackgroudShadowColor);

        mMarkTextPaint = new Paint();
        mMarkTextPaint.setColor(mMarkTextColor);
        mMarkTextPaint.setStrokeWidth(3);
        mMarkTextPaint.setStyle(Paint.Style.FILL);
        mMarkTextPaint.setTextSize(mMarkTextFontSizeDP * mDensity);
        mMarkTextPaint.setTextAlign(Paint.Align.CENTER);
        mMarkTextPaint.setAntiAlias(true);

        mDotPaint = new Paint();
        mDotPaint.setColor(mDotColor);
        mDotPaint.setStyle(Paint.Style.FILL);
        mDotPaint.setAntiAlias(true);

        mSliderPaint = new Paint();
        mSliderPaint.setColor(mSliderColor);
        mSliderPaint.setStyle(Paint.Style.FILL);
        mSliderPaint.setAntiAlias(true);
        mSliderPaint.setShadowLayer(mSliderShadowSizeDP * mDensity, 0, 0, mBackgroudShadowColor);

        mSliderTextPaint = new Paint();
        mSliderTextPaint.setColor(mSliderTextColor);
        mSliderTextPaint.setAntiAlias(true);
        mSliderTextPaint.setStrokeWidth(3);
        mSliderTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mSliderTextPaint.setTextAlign(Paint.Align.CENTER);
        mSliderTextPaint.setTextSize(mSliderNumTextFontSizDP * mDensity);

        mSliderMarkTextPaint = new Paint();
        mSliderMarkTextPaint.setColor(mSliderTextColor);
        mSliderMarkTextPaint.setAntiAlias(true);
        mSliderMarkTextPaint.setStrokeWidth(1);
        mSliderMarkTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mSliderMarkTextPaint.setTextAlign(Paint.Align.CENTER);
        mSliderMarkTextPaint.setTextSize(mSliderMarkTextFontSizDP * mDensity);

        mDividerPaint = new Paint();
        mDividerPaint.setColor(mDividerColor);
        mDividerPaint.setStyle(Paint.Style.FILL);
        mDividerPaint.setAntiAlias(true);

        mSymbolBitmapPaint = new Paint();
        mSymbolBitmapPaint.setFilterBitmap(true);
        mSymbolBitmapPaint.setDither(true);
        mSymbolBitmapPaint.setAntiAlias(true);

        setBackgroundResource(android.R.color.transparent);
    }

    private void resetDimen() {
        float pixel_benchmark = getContext().getResources().getDimension(R.dimen.uxsdk_pixel_benchmark) / mDensity;

        mBackgroudShadowSizeDP *= pixel_benchmark;
        mBackgroudRadiusDP *= pixel_benchmark;
        mBackgroundPaddingDP *= pixel_benchmark;

        mMarkTextFontSizeDP *= pixel_benchmark;

        mDotRadiusDP *= pixel_benchmark;
        mDotGap_dp *= pixel_benchmark;

        mSliderRadiusDP *= pixel_benchmark;
        mSliderShadowSizeDP *= pixel_benchmark;

        mSliderNumTextFontSizDP *= pixel_benchmark;
        mSliderMarkTextFontSizDP *= pixel_benchmark;
        mSliderHeightDP *= pixel_benchmark;

        mDividerPaddingDP *= pixel_benchmark;
        mDividerHeightDP *= pixel_benchmark;

    }

    private void initHandler() {
        myHandler = new Handler(msg -> {
            if (msg.what == FOCAL_CHECK_MESSAGE_TYPE) {
                checkValue();
            }
            return false;
        });
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        initSize();
    }

    private void initSize() {
        mViewHeight = getHeight();
        mViewWidth = getWidth();
        mSymbolImageSize = (int) ((mViewWidth - mBackgroundPaddingDP * mDensity * 2) / 3);
        mUpMartTextY = mViewWidth + mMarkTextFontSizeDP * mDensity + mMarkTextFontSizeDP * mDensity / 2 + mBackgroundPaddingDP * mDensity;
        mDownMarkTextY = mViewHeight - mViewWidth - mMarkTextFontSizeDP * mDensity - mMarkTextFontSizeDP * mDensity / 2 - mBackgroundPaddingDP * mDensity;
        mUpDividerY = mViewWidth;//是为了正方形的效果
        mDownDividerY = (float) mViewHeight - mViewWidth;
        mSliderTopY = mUpMartTextY - mMarkTextFontSizeDP * mDensity - mDividerHeightDP * mDensity;
        mSliderBottomY = mDownMarkTextY - mSliderHeightDP * mDensity / 2 - mDividerHeightDP * mDensity;
        LogUtils.i(getLogTag(), "mViewHeight", mViewHeight, "mViewWidth", mViewWidth, "mSymbolImageSize", mSymbolImageSize,
                "mUpMartTextY", mUpMartTextY, "mDownMarkTextY", mDownMarkTextY, "mUpDividerY", mUpDividerY,
                "mDownDividerY", mDownDividerY, "mSliderTopY", mSliderTopY, "mSliderBottomY", mSliderBottomY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawBackground(canvas);
        drawMarkText(canvas);
        drawDot(canvas);
        drawSlider(canvas);
        drawSymbol(canvas);
    }

    private void drawBackground(Canvas canvas) {
        RectF rectF = new RectF(mBackgroundPaddingDP * mDensity, mBackgroundPaddingDP * mDensity, mViewWidth - mBackgroundPaddingDP * mDensity, mViewHeight - mBackgroundPaddingDP * mDensity);
        canvas.drawRoundRect(rectF, mBackgroudRadiusDP * mDensity, mBackgroudRadiusDP * mDensity, mBackgroundPaint);
    }

    private void drawMarkText(Canvas canvas) {
        float x = mViewWidth / 2.0F;

        RectF upDividerRectF = new RectF(mDividerPaddingDP * mDensity, mUpDividerY, mViewWidth - mDividerPaddingDP * mDensity, mUpDividerY + mDividerHeightDP * mDensity);
        canvas.drawRect(upDividerRectF, mDividerPaint);
        RectF bottomDividerRectF = new RectF(mDividerPaddingDP * mDensity, mDownDividerY, mViewWidth - mDividerPaddingDP * mDensity, mDownDividerY - mDividerHeightDP * mDensity);
        canvas.drawRect(bottomDividerRectF, mDividerPaint);

        Paint.FontMetrics fontMetrics = mMarkTextPaint.getFontMetrics();
        canvas.drawText(((int) mHighLevelValue) + "X", x, mUpMartTextY - fontMetrics.bottom, mMarkTextPaint);
        canvas.drawText(((int) mLowLevelValue) + "X", x, mDownMarkTextY - fontMetrics.top, mMarkTextPaint);
    }

    private void drawDot(Canvas canvas) {
        int startLineWidth = mViewWidth / 3;
        int endLineWidth = mViewWidth / 12;

        int gap = (int) ((mDotRadiusDP * 2 + mDotGap_dp) * mDensity);
        if (gap == 0) {
            return;
        }
        int lineCount = (int) ((mDownMarkTextY - mUpMartTextY) / gap);
        if (lineCount == 0) {
            return;
        }
        int minusNumber = (startLineWidth - endLineWidth) / lineCount;

        float lineY = mUpMartTextY + gap;
        while (lineY < mDownMarkTextY - gap) {
            RectF rectF = new RectF((mViewWidth - startLineWidth) / 2.0F, lineY, (mViewWidth + startLineWidth) / 2.0F, lineY + mDotRadiusDP * mDensity);
            canvas.drawRect(rectF, mDotPaint);
            startLineWidth -= minusNumber;
            lineY += gap;
        }
    }


    private void drawSlider(Canvas canvas) {
        if (mCurrentLevel == -1) return;

        float sliderY = getCurrentSliderY();
        RectF rectF = new RectF(0, sliderY, mViewWidth, sliderY + mSliderHeightDP * mDensity);

        canvas.drawRoundRect(rectF, mSliderRadiusDP * mDensity, mSliderRadiusDP * mDensity, mSliderPaint);

        float distance;
        float baseline;

        Paint.FontMetrics fontMetrics = mSliderTextPaint.getFontMetrics();
        distance = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom;
        baseline = rectF.top + (mSliderMarkTextFontSizDP + 8) / 2.0F * mDensity + distance;
        canvas.drawText("ZOOM", rectF.centerX(), baseline, mSliderMarkTextPaint);

        mSliderTextPaint.setTextSize(mSliderNumTextFontSizDP * mDensity);
        fontMetrics = mSliderTextPaint.getFontMetrics();
        distance = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom;
        baseline = rectF.top + (mSliderMarkTextFontSizDP + 4) * mDensity + (mSliderNumTextFontSizDP + 8) * mDensity / 2 + distance;

        canvas.drawText(mCurrentScreenFocalMultiTimes + "X", rectF.centerX(), baseline, mSliderTextPaint);
    }

    private void drawSymbol(Canvas canvas) {
        Rect plusDestRect = new Rect(0, 0, mViewWidth, mViewWidth);
        if (mCurrentScreenFocalMultiTimes == mFocalLengthGears[mFocalLengthGears.length - 1]) {
            canvas.drawBitmap(mPlusBitmapUnclick, null, plusDestRect, mSymbolBitmapPaint);
        } else {
            canvas.drawBitmap(mPlusBitmap, null, plusDestRect, mSymbolBitmapPaint);
        }


        int destMinusY = mViewHeight - mViewWidth;
        Rect minusDestRect = new Rect(0, destMinusY, mViewWidth, destMinusY + mViewWidth);
        if (mCurrentScreenFocalMultiTimes == mFocalLengthGears[0]) {
            canvas.drawBitmap(mMinusBitmapUnclick, null, minusDestRect, mSymbolBitmapPaint);
        } else {
            canvas.drawBitmap(mMinusBitmap, null, minusDestRect, mSymbolBitmapPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        mFinalTouchTime = SystemClock.uptimeMillis();
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onTouchEventDown(event);
                break;
            case MotionEvent.ACTION_UP:
                onTouchEventUp(event);
                break;
            case MotionEvent.ACTION_MOVE:
                onTouchEventMove(event);
                break;
            default:
                break;
        }
        return true;
    }

    private void onTouchEventDown(MotionEvent event) {
        mTouchDownY = event.getY();
        mMoveStartY = event.getY();
        mTouchDownTime = SystemClock.uptimeMillis();
    }

    private void onTouchEventUp(MotionEvent event) {
        mIsStartSlowMove = false;
        mIsStartQuickMove = false;
        //这里处理点击事件（包括+、-符号）
        if (SystemClock.uptimeMillis() - mTouchDownTime < FOCAL_CLICK_INTERVAL) {
            if (event.getY() < mUpDividerY) {
                //click plus
                upLevel();
            } else if (event.getY() > mDownDividerY) {
                //click minus
                downLevel();
            } else {
                //click slider
                setCurrentValueByClick(event.getY());
            }
        }
    }

    private void onTouchEventMove(MotionEvent event) {
        float nowY = event.getRawY();
        if (mTouchDownY > mUpDividerY && mTouchDownY < mDownDividerY) {
            mVelocityTracker.computeCurrentVelocity(1000);
            float velocity = mVelocityTracker.getYVelocity();
            float stepDistance = (mSliderBottomY - mSliderTopY) / ((mHighLevelValue - mLowLevelValue) / step);

            if (velocity == 0) {
                //do nothing
            } else if (Math.abs(velocity) <= MIN_VELOCITY) {
                if (!mIsStartSlowMove) {
                    mIsStartQuickMove = false;
                    mIsStartSlowMove = true;
                } else {
                    if (velocity > 0) {
                        //toward down
                        for (int i = 0; i < mStepGears.length; i++) {
                            if (mCurrentScreenFocalMultiTimes == mStepGears[i]) {
                                step = mStepValues[i];
                                break;
                            }
                        }
                        if (Math.abs(nowY - mMoveStartY) > 10) {
                            float value = mCurrentScreenFocalMultiTimes - step;
                            mMoveStartY = nowY;
                            reviseValueAndShow(value);
                        }
                    } else {
                        //toward up
                        for (int i = 0; i < mStepGears.length; i++) {
                            if (mCurrentScreenFocalMultiTimes == mStepGears[i]) {
                                int index = Math.min(i + 1, mStepValues.length - 1);
                                step = mStepValues[index];
                                break;
                            }
                        }
                        if (Math.abs(nowY - mMoveStartY) > 10) {
                            float value = mCurrentScreenFocalMultiTimes + step;
                            mMoveStartY = nowY;
                            reviseValueAndShow(value);
                        }
                    }
                }
            } else {
                if (!mIsStartQuickMove) {
                    mIsStartSlowMove = false;
                    mIsStartQuickMove = true;
                } else {
                    if (Math.abs(nowY - mMoveStartY) > stepDistance) {
                        setCurrentValueByMove(nowY - mMoveStartY);
                        mMoveStartY = nowY;
                    }
                }
            }
        }
        invalidate();
    }

    private void setCurrentValue(float curValue) {
        if (mFocalLengthGears.length < 1) {
            return;
        }
        curValue = curValue < mFocalLengthGears[0] ? mFocalLengthGears[0] : curValue;
        curValue = curValue > mFocalLengthGears[mFocalLengthGears.length - 1] ? mFocalLengthGears[mFocalLengthGears.length - 1] : curValue;
        reviseValueAndShow(curValue);
    }

    private void upLevel() {
        int level = 1;
        if (mCurrentScreenFocalMultiTimes == mHighLevelValue) {
            level++;
        }
        setLevel(mCurrentLevel + level, 0);
    }

    private void downLevel() {
        int level = 1;
        if (mCurrentScreenFocalMultiTimes == mLowLevelValue) {
            level++;
        }
        setLevel(mCurrentLevel - level, 0);
    }

    private void setLevel(int level, float defaultValue) {
        int newLevel;
        if (level < 0) {
            newLevel = 0;
        } else {
            while (level > mFocalLengthGears.length - 1) {
                level--;
            }
            newLevel = level;
        }

        mCurrentLevel = newLevel;

        // refresh range if level changed
        float value;
        if (mCurrentLevel == 0) {
            mLowLevelValue = mFocalLengthGears[mCurrentLevel];
            mMiddleLevelValue = -1;
            mHighLevelValue = mFocalLengthGears[mCurrentLevel + 1];
            value = defaultValue == 0 ? mLowLevelValue : defaultValue;
        } else if (mCurrentLevel == mFocalLengthGears.length - 1) {
            mLowLevelValue = mFocalLengthGears[mCurrentLevel - 1];
            mMiddleLevelValue = -1;
            mHighLevelValue = mFocalLengthGears[mCurrentLevel];
            value = defaultValue == 0 ? mHighLevelValue : defaultValue;
            value = Math.min(value, mHighLevelValue);
        } else {
            mLowLevelValue = mFocalLengthGears[mCurrentLevel - 1];
            mMiddleLevelValue = mFocalLengthGears[mCurrentLevel];
            mHighLevelValue = mFocalLengthGears[mCurrentLevel + 1];
            value = defaultValue == 0 ? mMiddleLevelValue : defaultValue;
        }

        sendFocusCmd(value);
        invalidate();
    }


    private float getCurrentSliderY() {
        float middle = (mSliderBottomY - mSliderTopY) / 2;
        float y;
        if (mMiddleLevelValue == -1) {
            y = mSliderBottomY - (mSliderBottomY - mSliderTopY) * ((mCurrentScreenFocalMultiTimes - mLowLevelValue) / (mHighLevelValue - mLowLevelValue));
        } else {
            if (mCurrentScreenFocalMultiTimes == mMiddleLevelValue) {
                y = mSliderTopY + middle;
            } else if (mCurrentScreenFocalMultiTimes < mMiddleLevelValue) {
                y = mSliderTopY + middle + middle * ((mMiddleLevelValue - mCurrentScreenFocalMultiTimes) / (mMiddleLevelValue - mLowLevelValue));
            } else {
                y = mSliderTopY + middle - middle * ((mCurrentScreenFocalMultiTimes - mMiddleLevelValue) / (mHighLevelValue - mMiddleLevelValue));
            }
        }
        return y;
    }

    // user clicks slider bar to set focal length
    private void setCurrentValueByClick(float pointY) {
        pointY -= mSliderHeightDP * mDensity / 2;
        float middle = (mSliderBottomY - mSliderTopY) / 2;
        float value = 0;

        if (mMiddleLevelValue == -1) {
            if (pointY <= mUpMartTextY) {
                value = mHighLevelValue;
            } else if (pointY >= mDownMarkTextY) {
                value = mLowLevelValue;
            } else {
                float scale = (pointY - mSliderTopY) / (mSliderBottomY - mSliderTopY);
                value = mHighLevelValue - scale * (mHighLevelValue - mLowLevelValue);
            }
        } else {
            if (pointY <= mUpMartTextY) {
                value = mHighLevelValue;
            } else if (pointY >= mDownMarkTextY) {
                value = mLowLevelValue;
            } else {
                float height = pointY - mSliderTopY;
                if (height <= middle) {
                    value = mHighLevelValue - height / middle * (mHighLevelValue - mMiddleLevelValue);
                } else {
                    value = mMiddleLevelValue - (height - middle) / middle * (mMiddleLevelValue - mLowLevelValue);
                }
            }
        }
        reviseValueAndShow(value);
    }

    // user moves slider bar to set focal length
    private void setCurrentValueByMove(float offsetY) {
        if (Math.abs(offsetY) > 100) return;// drop the exception data
        float middle = (mSliderBottomY - mSliderTopY) / 2;
        float value = 0;
        if (mMiddleLevelValue == -1) {
            if (offsetY < 0) {
                // toward up
                value += Math.abs(offsetY) / (mSliderBottomY - mSliderTopY) * (mHighLevelValue - mLowLevelValue);
            } else {
                //toward down
                value -= Math.abs(offsetY) / (mSliderBottomY - mSliderTopY) * (mHighLevelValue - mLowLevelValue);
            }
        } else {
            if (mCurrentScreenFocalMultiTimes > mMiddleLevelValue) {
                if (offsetY < 0) {
                    //toward up
                    value += Math.abs(offsetY) / middle * (mHighLevelValue - mMiddleLevelValue);
                } else {
                    //toward down
                    value -= Math.abs(offsetY) / middle * (mHighLevelValue - mMiddleLevelValue);
                }
            } else {
                if (offsetY < 0) {
                    //toward up
                    value += Math.abs(offsetY) / middle * (mMiddleLevelValue - mLowLevelValue);
                } else {
                    //toward down
                    value -= Math.abs(offsetY) / middle * (mMiddleLevelValue - mLowLevelValue);
                }
            }
        }
        float moveValue = value + mCurrentScreenFocalMultiTimes;
        moveValue = Math.min(moveValue, mFocalLengthGears[mFocalLengthGears.length - 1]);
        moveValue = Math.max(moveValue, mFocalLengthGears[0]);
        reviseValueAndShow(moveValue);
    }

    /**
     * show the value in the slider bar,BUT MUST revise the value,Because the value must follow the rule:
     * 1、be divisible by step
     */
    private void reviseValueAndShow(float value) {
        calculateStep(value);
        value = calculateValue(value);
        calculateLevel(value);

        if (value > mHighLevelValue) {
            value = mHighLevelValue;
        } else if (value < mLowLevelValue) {
            value = mLowLevelValue;
        }

        sendFocusCmd(value);
        invalidate();
    }

    private void calculateLevel(float value) {
        if (value >= mHighLevelValue || value <= mLowLevelValue) {
            int nowLevel = 0;

            for (int i = mFocalLengthGears.length - 1; i > 0; i--) {
                if (value > mFocalLengthGears[i]) {
                    nowLevel = i;
                    break;
                }
            }
            if (mCurrentLevel == -1 || mCurrentLevel != nowLevel) {
                mCurrentLevel = nowLevel;

                if (mCurrentLevel == 0) {
                    mLowLevelValue = mFocalLengthGears[mCurrentLevel];
                    mMiddleLevelValue = -1;
                    mHighLevelValue = mFocalLengthGears[mCurrentLevel + 1];
                } else if (mCurrentLevel == mFocalLengthGears.length - 1) {
                    mLowLevelValue = mFocalLengthGears[mCurrentLevel - 1];
                    mMiddleLevelValue = -1;
                    mHighLevelValue = mFocalLengthGears[mCurrentLevel];
                } else {
                    mLowLevelValue = mFocalLengthGears[mCurrentLevel - 1];
                    mMiddleLevelValue = mFocalLengthGears[mCurrentLevel];
                    mHighLevelValue = mFocalLengthGears[mCurrentLevel + 1];
                }
            }
        }
    }

    private float calculateValue(float value) {
        for (int i = 0; i < mFocalLengthGears.length - 1; i++) {
            if ((int) (Math.abs(mFocalLengthGears[i] * 10 - value * 10)) < step * 10) {
                value = mFocalLengthGears[i];
            }
        }

        value = new BigDecimal(Float.toString(value)).setScale(1, BigDecimal.ROUND_HALF_UP).floatValue();

        int surplus = (int) (value * 10) % (int) (step * 10);
        if (surplus != 0) {
            if (surplus < step * 5) {
                value -= ((float) surplus) / 10.0f;
            } else {
                value += ((step * 10) - surplus) / 10.0f;
            }
        }

        value = new BigDecimal(Float.toString(value)).setScale(1, BigDecimal.ROUND_HALF_UP).floatValue();

        for (int i = 0; i < mFocalLengthGears.length - 1; i++) {
            if ((int) (Math.abs(mFocalLengthGears[i] * 10 - value * 10)) < step * 10) {
                value = mFocalLengthGears[i];
            }
        }
        return value;
    }

    private void calculateStep(float value) {
        for (int i = 0; i < mStepGears.length; i++) {
            if (value < mStepGears[i]) {
                step = mStepValues[i];
                break;
            }
        }
    }

    private void sendFocusCmd(float curValue) {
        if (mCurrentScreenFocalMultiTimes != curValue || !mIsInited) {
            mIsInited = true;
            mCurrentScreenFocalMultiTimes = curValue;
        }

        //说明是手在滑，则发送数据
        if (SystemClock.uptimeMillis() - mFinalTouchTime < FOCAL_TOUCH_TIMEOUT) {
            widgetModel.setFocusDistance(curValue);
            if (myHandler != null) {
                myHandler.removeMessages(FOCAL_CHECK_MESSAGE_TYPE);
            }
        }
    }

    private void checkValue() {
        if (Math.abs(mCurrentDroneFocalMultiTimes - mCurrentScreenFocalMultiTimes) > THRESHOLD) {
            setCurrentValue(mCurrentDroneFocalMultiTimes);
        }
    }

    public void pushFocalLength(float droneFocalMultiTimes) {
        mCurrentDroneFocalMultiTimes = droneFocalMultiTimes;

        if (SystemClock.uptimeMillis() - mFinalTouchTime < FOCAL_TOUCH_TIMEOUT) {
            if (myHandler != null) {
                myHandler.removeMessages(FOCAL_CHECK_MESSAGE_TYPE);
                myHandler.sendEmptyMessageDelayed(FOCAL_CHECK_MESSAGE_TYPE, FOCAL_CHANGE_TIMEOUT);
            }
        } else {
            if (Math.abs(mCurrentDroneFocalMultiTimes - mCurrentScreenFocalMultiTimes) > THRESHOLD) {
                this.post(() -> setCurrentValue(mCurrentDroneFocalMultiTimes));
            }
        }
    }

    public void pushFocalLengthRange(int[] gears) {
        mFocalLengthGears = gears;
        if (myHandler != null) {
            myHandler.removeMessages(FOCAL_CHECK_MESSAGE_TYPE);
            myHandler.sendEmptyMessageDelayed(FOCAL_CHECK_MESSAGE_TYPE, FOCAL_CHANGE_TIMEOUT);
        }
    }

    public void enterInternalSpotChecking() {
        // top mode 模式下禁止滑动操作
        setEnabled(false);
        setAlpha(0.5f);
    }

    public void exitInternalSpotChecking() {
        // 退出 top mode 模式时开启滑动
        setEnabled(true);
        setAlpha(1.0f);
    }

    public void dispose() {
        if (myHandler != null) {
            myHandler.removeCallbacksAndMessages(null);
        }
    }

    @Nullable
    @Override
    public String getIdealDimensionRatioString() {
        return null;
    }
}
