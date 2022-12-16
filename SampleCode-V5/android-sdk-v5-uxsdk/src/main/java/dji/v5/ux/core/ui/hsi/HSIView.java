package dji.v5.ux.core.ui.hsi;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewDebug;

import java.lang.ref.WeakReference;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.sdk.keyvalue.value.product.ProductType;
import dji.v5.manager.aircraft.perception.data.ObstacleAvoidanceType;
import dji.v5.manager.aircraft.perception.data.PerceptionInfo;
import dji.v5.utils.common.DisplayUtil;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.ui.hsi.dashboard.FpvStrokeConfig;
import dji.v5.utils.common.AndUtil;
import dji.v5.ux.core.util.DrawUtils;
import dji.v5.ux.core.util.MatrixUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class HSIView extends View implements HSIContract.HSIContainer {

    public static final Rect RECT = new Rect();

    public static final Rect RECT2 = new Rect();

    public static final Path mPath = new Path();

    /**
     * HSI 固定可视化半径为16m
     */
    public static final int HSI_VISIBLE_DISTANCE_IN_METERS = 16;

    /**
     * 最大云台数量
     */
    private static final int MAX_GIMBAL_COUNT = 3;

    /**
     * 性能原因,绘制间隔调整为:100ms
     */
    public static final int INVALIDATE_INTERVAL_TIME = 200;
    private static final int MSG_INVALIDATE = 0x01;
    public static final String VIEW_DEBUG_CATEGORY_DJI = "dji";
    public static final String VIEW_DEBUG_PREFIX_MARKER = "hsi_marker_";
    public static final String VIEW_DEBUG_PREFIX_PERCEPTION = "hsi_perception_";

    private final static String APAS_TEXT = "APAS";

    @NonNull
    private final Handler.Callback mCallback = msg -> {
        if (msg.what == MSG_INVALIDATE) {
            //            invalidate();
            postInvalidate();
            return true;
        }
        return false;
    };

    @Nullable
    private Handler mHandler;

    /**
     * 当前飞机罗盘的角度
     */
    @ViewDebug.ExportedProperty(category = "hsi")
    private float mCurrentDegree;
    private String mCurrentDegreeText;

    private boolean[] mGimbalConnected = new boolean[3];
    private float[] mGimbalCurrentDegree = new float[3];

    private boolean mIsRadarConnected;

    private boolean mIsRadarAvailable;

    private boolean isHideGimbalDrawable = false;

    private float mAircraftHeadingOffsetDistanceX;

    private float mAircraftHeadingOffsetDistanceY;

    /**
     * 云台指示器圆环最大宽度，内部为避障区域
     */
    @ViewDebug.ExportedProperty(category = "hsi")
    private final int mGimbalIndicatorMaxScope;
    @ViewDebug.ExportedProperty(category = "hsi")
    private final int mAircraftIndicatorSize;
    @ViewDebug.ExportedProperty(category = "hsi")
    private final int mDegreeIndicatorTextSize;
    @ViewDebug.ExportedProperty(category = "hsi")
    private final int mDegreeIndicatorTextHeight;
    @ViewDebug.ExportedProperty(category = "hsi")
    private final int mCompassMargin;

    /**
     * 角度指示
     */
    @ViewDebug.ExportedProperty(category = "hsi")
    private final int mDegreeIndicatorHeight;
    @ViewDebug.ExportedProperty(category = "hsi")
    private final float mDegreeIndicatorHeightInCompass;
    @ViewDebug.ExportedProperty(category = "hsi")
    private final float mDegreeIndicatorWidth;
    @ViewDebug.ExportedProperty(category = "hsi", formatToHexString = true)
    private final int mDegreeIndicatorColor;
    @ViewDebug.ExportedProperty(category = "hsi")
    private final int mHeadingLineWidth;


    @ViewDebug.ExportedProperty(category = "hsi")
    private float mYaw;
    @ViewDebug.ExportedProperty(category = "hsi", formatToHexString = true)
    private float mRoll;
    @ViewDebug.ExportedProperty(category = "hsi", formatToHexString = true)
    private float mPitch;

    @ViewDebug.ExportedProperty(category = "hsi", formatToHexString = true)
    private float mSpeedX;
    @ViewDebug.ExportedProperty(category = "hsi", formatToHexString = true)
    private float mSpeedY;
    @ViewDebug.ExportedProperty(category = "hsi", formatToHexString = true)
    private float mSpeedZ;

    /**
     * 罗盘的image边上有空隙，在计算的时候需要考虑到
     */
    private final int mBitmapOffset;

    @NonNull
    private final Paint mPaint;

    @Nullable
    private final Bitmap mCompassBitmap;

    @NonNull
    private final Bitmap mAircraftBitmap;

    @NonNull
    private final Bitmap mRadarAvailableBitmap;

    @NonNull
    private final Bitmap mRadarUnavailableBitmap;

    @NonNull
    private final GradientDrawable mAircraftHeadingLineDrawable;

    @NonNull
    @ViewDebug.ExportedProperty(category = VIEW_DEBUG_CATEGORY_DJI, prefix = VIEW_DEBUG_PREFIX_MARKER, deepExport = true)
    private final HSIContract.HSILayer mMarkerLayer;

    @NonNull
    @ViewDebug.ExportedProperty(category = VIEW_DEBUG_CATEGORY_DJI, prefix = VIEW_DEBUG_PREFIX_PERCEPTION, deepExport = true)
    private final HSIContract.HSILayer mPerceptionLayer;

    FpvStrokeConfig mStrokeConfig;

    private boolean isApasMode = false;
    private float mApasTextLength = 0;
    private float mApasTextHeight = 0;
    private float mApasTextSize = AndUtil.getDimension(R.dimen.uxsdk_6_dp);
    private float mApasTextStrokeWith = AndUtil.getDimension(R.dimen.uxsdk_1_dp);
    private int mApasTextColor = AndUtil.getResColor(R.color.uxsdk_white);
    private int mApasTextStrokeColor = AndUtil.getResColor(R.color.uxsdk_black_60_percent);


    @Nullable
    private Disposable mDisposable;

    private HSIWidgetModel widgetModel;


    @Nullable
    private OnAircraftAttitudeChangeListener mListener;



    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();


    public HSIView(Context context) {
        this(context, null);
    }

    public HSIView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HSIView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            widgetModel = new HSIWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());
        }

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.HSIView);
        mBitmapOffset = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_bitmap_offset, 0);
        mAircraftIndicatorSize = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_aircraft_indicator_size,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_10_dp));
        mGimbalIndicatorMaxScope = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_gimbal_indicator_max_scope,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_10_dp));
        mDegreeIndicatorHeight = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_degree_indicator_width,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_5_dp));
        mDegreeIndicatorTextSize = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_degree_indicator_text_size,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_text_size_normal));
        mDegreeIndicatorTextHeight = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_degree_indicator_height,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_11_dp));
        mCompassMargin = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_compass_margin,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_5_dp));
        mDegreeIndicatorColor = typedArray.getColor(R.styleable.HSIView_uxsdk_hsi_degree_indicator_color,
                getResources().getColor(R.color.uxsdk_green_in_dark));
        mAircraftHeadingOffsetDistanceX = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_aircraft_heading_offset_x, 0);
        mAircraftHeadingOffsetDistanceY = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_aircraft_heading_offset_y, 0);
        mHeadingLineWidth = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_heading_line_width,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_1_dp));
        typedArray.recycle();
        mDegreeIndicatorHeightInCompass = getResources().getDimension(R.dimen.uxsdk_1_dp);
        mDegreeIndicatorWidth = DisplayUtil.dip2px(context, 1.4f);

        mPaint = new Paint(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.uxsdk_1_dp));
        Typeface typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        mPaint.setTypeface(typeface);
        // 屏幕相比视觉稿放大 1.2 倍，使用 3 倍图尺寸最后会导致模糊
        mCompassBitmap = getBitmap(R.drawable.uxsdk_fpv_hsi_compass_list);
        mAircraftBitmap = getBitmap(R.drawable.uxsdk_fpv_hsi_aircraft);
        mRadarAvailableBitmap = getBitmap(R.drawable.uxsdk_hsi_aircraft_radar_normal);
        mRadarUnavailableBitmap = getBitmap(R.drawable.uxsdk_hsi_aircraft_radar_disable);
        mAircraftHeadingLineDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{
                        getResources().getColor(R.color.uxsdk_white_30_percent),
                        getResources().getColor(R.color.uxsdk_white)
                });
        mAircraftHeadingLineDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        mAircraftHeadingLineDrawable.setShape(GradientDrawable.RECTANGLE);
        mAircraftHeadingLineDrawable.setCornerRadius(getResources().getDimensionPixelSize(R.dimen.uxsdk_1_dp));

        mMarkerLayer = new HSIMarkerLayer(context, attrs, this, widgetModel);
        mPerceptionLayer = new HSIPerceptionLayer(context, attrs, this, widgetModel);

        mStrokeConfig = new FpvStrokeConfig(getContext());
        initApasParam();
    }

    private void initApasParam() {
        mPaint.setTextSize(getContext().getResources().getDimension(R.dimen.uxsdk_6_dp));
        mApasTextLength = mPaint.measureText(APAS_TEXT);
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        mApasTextHeight = fontMetrics.bottom - fontMetrics.top;
    }

    private Bitmap getBitmap(int resId) {
        if (isInEditMode()) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            return DrawUtils.drawableRes2Bitmap(resId);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isInEditMode()) {
            return;
        }
        widgetModel.setup();

        mHandler = new Handler(Looper.getMainLooper(), mCallback);

        mMarkerLayer.onStart();
        mPerceptionLayer.onStart();

        mCompositeDisposable.add(widgetModel.getProductTypeDataProcessor().toFlowable()
                .subscribe(productType -> isHideGimbalDrawable = productType == ProductType.DJI_MAVIC_3_ENTERPRISE_SERIES));

        mCompositeDisposable.add(widgetModel.getRadarInformationDataProcessor().toFlowable().subscribe(info -> {
            mIsRadarConnected = info.isConnected();
            mIsRadarAvailable = info.isHorizontalObstacleAvoidanceEnabled();
            updateWidget();
        }));

        mCompositeDisposable.add(widgetModel.getPerceptionInformationDataProcessor().toFlowable().subscribe(new Consumer<PerceptionInfo>() {
            @Override
            public void accept(PerceptionInfo info) throws Throwable {
                isApasMode = info.getObstacleAvoidanceType() == ObstacleAvoidanceType.BYPASS;
            }
        }));

        mDisposable = Observable.create(new ObservableSource(this))
                .observeOn(Schedulers.computation())
                .map(data -> updateSpeedVectorMark(data[0], data[1], data[2], data[3], data[4], data[5]))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(p -> {
                    mAircraftHeadingOffsetDistanceX = p[0];
                    mAircraftHeadingOffsetDistanceY = p[1];
                    updateWidget();
                });


        mCompositeDisposable.add(widgetModel.getVelocityProcessor().toFlowable().subscribe(velocity3D -> {
            mSpeedX = velocity3D.getX().floatValue();
            mSpeedY = velocity3D.getY().floatValue();
            mSpeedZ = velocity3D.getZ().floatValue();
            recalculateAndInvalidate();
        }));

        //获取飞机角度
        mCompositeDisposable.add(widgetModel.getAircraftAttitudeProcessor().toFlowable().subscribe(attitude -> {
            mYaw = attitude.getYaw().floatValue();
            mCurrentDegree = mYaw + (mYaw < 0 ? 359f : 0);
            mCurrentDegreeText = String.format(Locale.ENGLISH, "%03.0f", mCurrentDegree);
            mRoll = attitude.getRoll().floatValue();
            mPitch = attitude.getPitch().floatValue();
            recalculateAndInvalidate();
        }));

        //云台Yaw夹角
        mCompositeDisposable.add(Flowable.fromArray(new Integer[]{0, 1, 2})
                .flatMap(cameraIndex -> Flowable.combineLatest(
                        //云台连接状态
                        widgetModel.getGimbalConnectionProcessorList().get(cameraIndex).toFlowable(),
                        //云台相对机身的Yaw夹角
                        widgetModel.getGimbalYawInDegreesProcessorList().get(cameraIndex).toFlowable(),
                        ((isConnected, yaw) -> {
                            mGimbalConnected[cameraIndex] = isConnected;
                            mGimbalCurrentDegree[cameraIndex] = yaw.floatValue();
                            return true;
                        })
                ))
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> updateWidget()));


    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mMarkerLayer.onStop();
        mPerceptionLayer.onStop();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
        }
        mCompositeDisposable.dispose();
        widgetModel.cleanup();
    }

    public void enterFpvMode(boolean fpv) {
        mMarkerLayer.enterFpvMode(fpv);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 画布中心移动到 HSI (0.5, 0) 位置
        canvas.translate(getWidth() / 2f, 0);

        // 计算罗盘尺寸
        int textHeight = mDegreeIndicatorTextHeight;
        int compassHeight = getHeight() - textHeight - mCompassMargin * 2;
        int compassWidth = getWidth() - mCompassMargin * 2;
        int compassSize = Math.min(compassWidth, compassHeight);

        drawDegreeIndicator(canvas);

        int compassStartY = textHeight + mCompassMargin;
        // 画布中心移动到罗盘（0.5, 0）位置
        canvas.translate(0, compassStartY);
        drawCompass(canvas, compassSize);
        drawDegreeLine(canvas);
        drawGimbalIndicators(canvas, compassSize);

        drawAircraftHeadingLine(canvas, compassSize,
                mAircraftHeadingOffsetDistanceX, mAircraftHeadingOffsetDistanceY);
        drawAircraftIndicator(canvas, compassSize);
        drawApasModeText(canvas, compassSize);

        mPerceptionLayer.draw(canvas, mPaint, compassSize);
        mMarkerLayer.draw(canvas, mPaint, compassSize);
    }

    private void drawDegreeLine(Canvas canvas) {
        mPaint.setColor(getResources().getColor(R.color.uxsdk_green_in_dark));
        float width = mDegreeIndicatorWidth;
        mPaint.setStrokeWidth(width);
        mPaint.setStyle(Paint.Style.FILL);
        float top = -mDegreeIndicatorHeight + mDegreeIndicatorHeightInCompass;
        canvas.drawLine(0, top, 0, mDegreeIndicatorHeight + top, mPaint);


        float strokeWidth = mStrokeConfig.getStrokeThinWidth();
        mPaint.setStrokeWidth(strokeWidth);
        mPaint.setColor(mStrokeConfig.getStrokeShallowColor());
        mPaint.setStyle(Paint.Style.STROKE);
        float halfWidth = width / 2;
        float halfStrokeWidth = strokeWidth / 2;
        float bottom = top + mDegreeIndicatorHeight + halfStrokeWidth;
        canvas.drawRect(-halfWidth - halfStrokeWidth, top - halfStrokeWidth, halfStrokeWidth + halfWidth, bottom, mPaint);
    }

    /**
     * 绘制当前飞机朝向的度数
     *
     * @param canvas
     * @return
     */
    private void drawDegreeIndicator(Canvas canvas) {
        if (mCurrentDegreeText == null) {
            return;
        }
        canvas.save();
        mPaint.setTextSize(mDegreeIndicatorTextSize);
        String degree = mCurrentDegreeText;
        mPaint.getTextBounds(degree, 0, degree.length(), RECT);
        Paint.FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
        float baseline = (mDegreeIndicatorTextHeight - fontMetrics.bottom + fontMetrics.top) / 2f - fontMetrics.top;
        mPaint.setTextAlign(Paint.Align.LEFT);
        drawTextWithStroke(canvas, degree, -(float) RECT.width() / 2, baseline, mStrokeConfig.getStrokeBoldWidth(),
                mStrokeConfig.getStrokeDeepColor(), mDegreeIndicatorColor);
        canvas.restore();
    }

    /**
     * 绘制罗盘
     *
     * @param canvas
     * @param compassSize
     */
    private void drawCompass(Canvas canvas, int compassSize) {
        if (mCompassBitmap == null) {
            return;
        }
        int flag = mPaint.getFlags();
        mPaint.setFlags(flag | Paint.FILTER_BITMAP_FLAG);
        // save 1
        canvas.save();
        canvas.translate(-compassSize / 2f, 0);
        RECT.set(0, 0, mCompassBitmap.getWidth(), mCompassBitmap.getHeight());
        RECT2.set(0, 0, compassSize, compassSize);
        // save 2
        canvas.save();
        canvas.translate(compassSize / 2f, compassSize / 2f);
        canvas.rotate(-mCurrentDegree);
        canvas.translate(-compassSize / 2f, -compassSize / 2f);
        canvas.drawBitmap(mCompassBitmap, RECT, RECT2, mPaint);
        // restore 2
        canvas.restore();
        mPaint.setFlags(flag);
        // restore 1
        canvas.restore();
    }

    /**
     * 绘制飞行器的指示图标
     *
     * @param canvas
     * @param compassSize
     */
    private void drawAircraftIndicator(Canvas canvas, int compassSize) {
        canvas.save();
        canvas.translate(-(float) compassSize / 2, 0);
        Bitmap marker = mAircraftBitmap;
        if (mIsRadarConnected) {
            if (mIsRadarAvailable) {
                marker = mRadarAvailableBitmap;
            } else {
                marker = mRadarUnavailableBitmap;
            }
        }
        drawMarker(canvas, marker, mAircraftIndicatorSize, mAircraftIndicatorSize,
                (float) compassSize / 2, (float) compassSize / 2, mPaint);
        canvas.restore();
    }

    private void drawApasModeText(Canvas canvas, int compassSize) {
        if (!isApasMode) {
            return;
        }

        mPaint.setColor(mApasTextStrokeColor);
        mPaint.setTextSize(mApasTextSize);
        mPaint.setStrokeWidth(mApasTextStrokeWith);

        float offX = -mApasTextLength / 2;
        float offY = compassSize / 2f + mAircraftIndicatorSize / 2f + mApasTextHeight;

        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawText(APAS_TEXT, offX, offY, mPaint);

        mPaint.setColor(mApasTextColor);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawText(APAS_TEXT, offX, offY, mPaint);
    }

    /**
     * 绘制各个云台的指示图
     *
     * @param canvas
     * @param compassSize
     */
    private void drawGimbalIndicators(Canvas canvas, int compassSize) {
        if (isHideGimbalDrawable) {
            return;
        }
        canvas.save();
        canvas.translate(0, 0);
        Drawable drawable = DrawUtils.getDrawable(R.drawable.uxsdk_fpv_hsi_outer_guide_gimbal_3);
        float gimbalPadding = (float) (mGimbalIndicatorMaxScope - drawable.getMinimumHeight()) / (MAX_GIMBAL_COUNT - 1);
        drawEachGimbalIndicator(canvas, getGimbalYawDegreeWithAircraft(mGimbalCurrentDegree[0], mYaw),
                mGimbalConnected[0], compassSize / 2f, mBitmapOffset, R.drawable.uxsdk_fpv_hsi_outer_guide_gimbal_3);
        drawEachGimbalIndicator(canvas, getGimbalYawDegreeWithAircraft(mGimbalCurrentDegree[1], mYaw),
                mGimbalConnected[1], compassSize / 2f,
                mBitmapOffset + gimbalPadding, R.drawable.uxsdk_fpv_hsi_outer_guide_gimbal_1);
        drawEachGimbalIndicator(canvas, getGimbalYawDegreeWithAircraft(mGimbalCurrentDegree[2], mYaw),
                mGimbalConnected[2], compassSize / 2f,
                mBitmapOffset + gimbalPadding * 2, R.drawable.uxsdk_fpv_hsi_outer_guide_gimbal_2);
        canvas.restore();
    }

    private float getGimbalYawDegreeWithAircraft(float gimbalYaw, float aircraftYaw) {
        return gimbalYaw - aircraftYaw;
    }


    /**
     * 绘制单个云台的指示图
     *
     * @param canvas
     * @param yawDegree
     * @param isConnected
     * @param centerY
     * @param offsetY
     */
    private void drawEachGimbalIndicator(Canvas canvas, float yawDegree,
                                         boolean isConnected, float centerY, float offsetY, int gimbalDrawable) {
        if (isInEditMode() || isConnected) {
            canvas.save();
            canvas.translate(0, centerY);
            canvas.rotate(yawDegree);
            Drawable drawable = DrawUtils.getDrawable(gimbalDrawable);
            int width = drawable.getMinimumWidth();
            int height = drawable.getMinimumHeight();
            float halfWidth = width / 2f;
            canvas.translate(-halfWidth, offsetY - centerY);
            drawable.setBounds(0, 0, width, height);
            drawable.draw(canvas);
            canvas.restore();
        }
    }

    /**
     * 绘制速度矢量球的线段
     *
     * @param canvas
     * @param compassSize
     * @param distanceX
     * @param distanceY
     */
    private void drawAircraftHeadingLine(Canvas canvas, int compassSize,
                                         float distanceX, float distanceY) {
        // distanceX,distanceY 实际分别对应Y轴和X轴
        canvas.save();
        int radius = (int) (compassSize / 2 - mGimbalIndicatorMaxScope - mBitmapOffset);
        float offsetX = distanceX / HSI_VISIBLE_DISTANCE_IN_METERS * radius;
        float offsetY = distanceY / HSI_VISIBLE_DISTANCE_IN_METERS * radius;
        int lineHeight = (int) Math.sqrt(Math.pow(offsetX, 2) + Math.pow(offsetY, 2));
        if (lineHeight > radius) {
            lineHeight = radius;
        }
        double atan = Math.atan(offsetY / offsetX);
        float angle = (float) (atan * 180 / Math.PI);
        if (!Float.isNaN(angle) && distanceX < 0) {
            angle += 180;
        }
        canvas.translate(0, compassSize / 2f);
        canvas.rotate(angle);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(mHeadingLineWidth);
        canvas.drawLine(0, -lineHeight, 0, 0, mPaint);
        canvas.restore();
    }

    private void drawMarker(Canvas canvas, Bitmap marker, int markerWidth, int markerHeight,
                            float offsetMiddleX, float offsetMiddleY, Paint paint) {
        int flag = paint.getFlags();
        paint.setFlags(flag | Paint.FILTER_BITMAP_FLAG);
        canvas.save();
        canvas.translate(offsetMiddleX - (float) markerWidth / 2, offsetMiddleY - (float) markerHeight / 2);
        RECT.set(0, 0, marker.getWidth(), marker.getHeight());
        RECT2.set(0, 0, markerWidth, markerHeight);
        canvas.drawBitmap(marker, RECT, RECT2, paint);
        canvas.restore();
        paint.setFlags(flag);
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

    private void setListener(@Nullable OnAircraftAttitudeChangeListener listener) {
        mListener = listener;
    }

    private void recalculateAndInvalidate() {
        if (mListener != null) {
            mListener.onAttitudeChanged(mPitch, mYaw, mRoll, mSpeedX, mSpeedY, mSpeedZ);
        }
    }

    @Override
    public float getCurrentDegree() {
        return mCurrentDegree;
    }

    @Override
    public int getAircraftSize() {
        return mAircraftIndicatorSize;
    }

    @Override
    public int getVisibleDistanceInHsiInMeters() {
        return HSI_VISIBLE_DISTANCE_IN_METERS;
    }

    @Override
    public float getCalibrationAreaWidth() {
        return mGimbalIndicatorMaxScope;
    }

    @Override
    public float getDegreeIndicatorHeight() {
        return mDegreeIndicatorHeight;
    }

    @Override
    public float getCompassBitmapOffset() {
        return mBitmapOffset;
    }

    @Override
    public void updateWidget() {
        if (mHandler != null && !mHandler.hasMessages(MSG_INVALIDATE)) {
            mHandler.sendEmptyMessageDelayed(MSG_INVALIDATE, INVALIDATE_INTERVAL_TIME);
        }
    }

    @Override
    public View getView() {
        return this;
    }


    private static final class ObservableSource implements ObservableOnSubscribe<float[]> {

        private final WeakReference<HSIView> mReference;

        public ObservableSource(HSIView view) {
            mReference = new WeakReference<>(view);
        }

        @Override
        public void subscribe(ObservableEmitter<float[]> e) throws Exception {
            final HSIView view = mReference.get();
            if (view == null) {
                return;
            }
            OnAircraftAttitudeChangeListener listener =
                    (pitch, yaw, roll, speedX, speedY, speedZ) -> {
                        if (!e.isDisposed()) {
                            e.onNext(new float[]{pitch, yaw, roll, speedX, speedY, speedZ});
                        }
                    };
            e.setCancellable(() -> view.setListener(null));
            view.setListener(listener);
        }

    }

    @NonNull
    private static float[] updateSpeedVectorMark(float pitch, float yaw, float roll,
                                                 float speedX, float speedY, float speedZ) {
        float[] i2gMat = MatrixUtils.createRotationMatrix(yaw, pitch, roll);
        float[] g2iMat = MatrixUtils.transposeMatrix(i2gMat);

        float[] speedVector = new float[]{speedX, speedY, speedZ};

        float[] vi = MatrixUtils.rotateVector(speedVector, g2iMat);

        return MatrixUtils.multiple(vi, 2);
    }

    private interface OnAircraftAttitudeChangeListener {
        void onAttitudeChanged(float pitch, float yaw, float roll,
                               float speedX, float speedY, float speedZ);
    }
}
