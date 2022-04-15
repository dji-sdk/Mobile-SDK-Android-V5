package dji.v5.ux.core.ui.hsi;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.MatrixUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
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

    /**
     * 当前飞机罗盘的角度
     */
    private float mCurrentDegree;

    private final boolean[] mGimbalConnected = new boolean[3];
    private final float[] mGimbalCurrentDegree = new float[3];

    private boolean mIsRadarConnected;

    private boolean mIsRadarAvailable;

    private float mAircraftHeadingOffsetDistanceX;

    private float mAircraftHeadingOffsetDistanceY;

    private final int mGimbalIndicatorSize;
    private final int mGimbalIndicatorMaxScope;
    private final int mAircraftIndicatorSize;
    private final int mDegreeIndicatorTextSize;
    private final int mDegreeIndicatorWidth;
    private final int mDegreeIndicatorHeight;
    private final int mDegreeIndicatorColor;
    private final int mGimbal1IndicatorColor;
    private final int mGimbal2IndicatorColor;
    private final int mGimbal3IndicatorColor;
    private final int mHeadingLineWidth;

    private float mYaw;
    private float mRoll;
    private float mPitch;

    private float mSpeedX;
    private float mSpeedY;
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
    private final HSIContract.HSILayer mMarkerLayer;

    @NonNull
    private final HSIContract.HSILayer mPerceptionLayer;

    @Nullable
    private Disposable mDisposable;

    @Nullable
    private OnAircraftAttitudeChangeListener mListener;

    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    private HSIWidgetModel widgetModel;

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
        mBitmapOffset = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_bitmap_offset,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_2_dp));
        mAircraftIndicatorSize = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_aircraft_indicator_size,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_10_dp));
        mGimbalIndicatorSize = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_gimbal_indicator_size,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_5_dp));
        mGimbalIndicatorMaxScope = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_gimbal_indicator_max_scope,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_10_dp));
        mDegreeIndicatorWidth = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_degree_indicator_width,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_7_dp));
        mDegreeIndicatorHeight = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_degree_indicator_height,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_3_dp));
        mDegreeIndicatorTextSize = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_degree_indicator_text_size,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_text_size_normal));
        mDegreeIndicatorColor = typedArray.getColor(R.styleable.HSIView_uxsdk_hsi_degree_indicator_color,
                getResources().getColor(R.color.uxsdk_pfd_main_color));
        mAircraftHeadingOffsetDistanceX = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_aircraft_heading_offset_x, 0);
        mAircraftHeadingOffsetDistanceY = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_aircraft_heading_offset_y, 0);
        mHeadingLineWidth = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_heading_line_width,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_1_dp));
        typedArray.recycle();

        mGimbal1IndicatorColor = getResources().getColor(R.color.uxsdk_gimbal_mark_1);
        mGimbal2IndicatorColor = getResources().getColor(R.color.uxsdk_gimbal_mark_2);
        mGimbal3IndicatorColor = getResources().getColor(R.color.uxsdk_gimbal_mark_3);

        mPaint = new Paint(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.uxsdk_1_dp));
        mCompassBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.uxsdk_hsi_compass);
        mAircraftBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.uxsdk_hsiview_aircraft);
        mRadarAvailableBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.uxsdk_hsiview_aircraft_radar_available);
        mRadarUnavailableBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.uxsdk_hsiview_aircraft_radar_unavailable);
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

        mCompositeDisposable.add(widgetModel.radarConnectionProcessor.toFlowable().subscribe(aBoolean -> {
            mIsRadarConnected = aBoolean;
            updateWidget();
        }));
        mCompositeDisposable.add(widgetModel.radarHorizontalObstacleAvoidanceEnabledProcessor.toFlowable().subscribe(aBoolean -> {
            mIsRadarAvailable = aBoolean;
            updateWidget();
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

        mCompositeDisposable.add(widgetModel.velocityProcessor.toFlowable().subscribe(velocity3D -> {
            mSpeedX = velocity3D.getX().floatValue();
            mSpeedY = velocity3D.getY().floatValue();
            mSpeedZ = velocity3D.getZ().floatValue();
            recalculateAndInvalidate();
        }));

        mCompositeDisposable.add(widgetModel.aircraftAttitudeProcessor.toFlowable().subscribe(attitude -> {
            mYaw = attitude.getYaw().floatValue();
            mCurrentDegree = mYaw + (mYaw < 0 ? 359f : 0);
            mRoll = attitude.getRoll().floatValue();
            mPitch = attitude.getPitch().floatValue();
            recalculateAndInvalidate();
        }));

        mCompositeDisposable.add(Flowable.fromArray(new Integer[]{0, 1, 2})
                .flatMap(index -> Flowable.combineLatest(
                        //云台连接状态
                        widgetModel.gimbalConnectionProcessorList.get(index).toFlowable(),
                        //云台相对机身的Yaw夹角
                        widgetModel.gimbalAttitudeInDegreesProcessorList.get(index).toFlowable(),

                        ((isConnected, attitude) -> {
                            mGimbalConnected[index] = isConnected;
                            mGimbalCurrentDegree[index] = attitude.getYaw().floatValue();
                            return true;
                        })
                )).subscribe(aBoolean -> updateWidget()));
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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.translate((float) getWidth() / 2, 0);
        int compassSize = drawDegreeIndicator(canvas);
        int degreeIndicatorTextHeight = RECT.height();
        int compassStartY = degreeIndicatorTextHeight + mDegreeIndicatorHeight;
        canvas.translate(0, compassStartY);
        drawCompass(canvas, compassSize);
        drawGimbalIndicators(canvas, compassSize);

        mPerceptionLayer.draw(canvas, mPaint, compassSize);
        mMarkerLayer.draw(canvas, mPaint, compassSize);

        drawAircraftHeadingLine(canvas, compassSize,
                mAircraftHeadingOffsetDistanceX, mAircraftHeadingOffsetDistanceY);
        drawAircraftIndicator(canvas, compassSize);
    }

    /**
     * 绘制当前飞机朝向的度数
     *
     * @param canvas
     * @return
     */
    private int drawDegreeIndicator(Canvas canvas) {
        canvas.save();
        mPaint.setTextSize(mDegreeIndicatorTextSize);
        mPaint.setColor(mDegreeIndicatorColor);
        String degree = String.format(Locale.ENGLISH, "%03.0f", mCurrentDegree);
        mPaint.getTextBounds(degree, 0, degree.length(), RECT);
        Paint.FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
        float baseline = (float) (RECT.height() - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top;
        mPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(degree, -(float) RECT.width() / 2, baseline, mPaint);
        canvas.translate(0, (float) RECT.height() + mBitmapOffset);
        mPath.reset();
        mPath.rMoveTo(-(float) mDegreeIndicatorWidth / 2, 0);
        mPath.rLineTo(mDegreeIndicatorWidth, 0);
        mPath.rLineTo(-(float) mDegreeIndicatorWidth / 2, mDegreeIndicatorHeight);
        mPath.close();
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(mPath, mPaint);

        int compassWidth = getWidth() - mDegreeIndicatorHeight * 2;
        int compassHeight = getHeight() - RECT.height() - mDegreeIndicatorHeight * 2;
        int compassSize = Math.min(compassWidth, compassHeight);

        canvas.restore();
        return compassSize;
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
        canvas.save();
        canvas.translate((float) -compassSize / 2, 0);
        RECT.set(0, 0, mCompassBitmap.getWidth(), mCompassBitmap.getHeight());
        RECT2.set(0, 0, compassSize, compassSize);
        canvas.save();
        canvas.translate((float) compassSize / 2, (float) compassSize / 2);
        canvas.rotate(-mCurrentDegree);
        canvas.translate((float) -compassSize / 2, (float) -compassSize / 2);
        canvas.drawBitmap(mCompassBitmap, RECT, RECT2, mPaint);
        canvas.restore();
        mPaint.setFlags(flag);
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

    /**
     * 绘制各个云台的指示图
     *
     * @param canvas
     * @param compassSize
     */
    private void drawGimbalIndicators(Canvas canvas, int compassSize) {
        canvas.save();
        canvas.translate(0, 0);
        float gimbalPadding = (float) (mGimbalIndicatorMaxScope - mGimbalIndicatorSize) / (MAX_GIMBAL_COUNT - 1);
        drawEachGimbalIndicator(canvas, getGimbalYawDegreeWithAircraft(mGimbalCurrentDegree[0], mYaw),
                mGimbal1IndicatorColor, mGimbalConnected[0], compassSize / 2f, mBitmapOffset);
        drawEachGimbalIndicator(canvas, getGimbalYawDegreeWithAircraft(mGimbalCurrentDegree[1], mYaw),
                mGimbal2IndicatorColor, mGimbalConnected[1], compassSize / 2f,
                mBitmapOffset + gimbalPadding);
        drawEachGimbalIndicator(canvas, getGimbalYawDegreeWithAircraft(mGimbalCurrentDegree[2], mYaw),
                mGimbal3IndicatorColor, mGimbalConnected[2], compassSize / 2f,
                mBitmapOffset + gimbalPadding * 2);
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
     * @param color
     * @param isConnected
     * @param centerY
     * @param offsetY
     */
    private void drawEachGimbalIndicator(Canvas canvas, float yawDegree, int color,
                                         boolean isConnected, float centerY, float offsetY) {
        if (isInEditMode() || isConnected) {
            canvas.save();
            canvas.translate(0, centerY);
            canvas.rotate(yawDegree);
            canvas.translate(0, offsetY - centerY);
            drawGimbalIndicator(canvas, color);
            canvas.restore();
        }
    }

    /**
     * 绘制单个云台的指示图
     *
     * @param canvas
     * @param color
     */
    private void drawGimbalIndicator(Canvas canvas, int color) {
        mPaint.setColor(color);
        mPaint.setStyle(Paint.Style.FILL);
        mPath.reset();
        float indicatorSize = mGimbalIndicatorSize;
        mPath.rLineTo(indicatorSize / 2, indicatorSize / 3);
        mPath.rLineTo(0, indicatorSize / 3 * 2);
        mPath.rLineTo(-indicatorSize, 0);
        mPath.rLineTo(0, -indicatorSize / 3 * 2);
        mPath.close();
        canvas.drawPath(mPath, mPaint);
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
        mAircraftHeadingLineDrawable.setSize(mHeadingLineWidth, lineHeight);
        int lineWidth = mAircraftHeadingLineDrawable.getIntrinsicWidth();
        mAircraftHeadingLineDrawable.setBounds(-lineWidth / 2,
                -mAircraftHeadingLineDrawable.getIntrinsicHeight(), lineWidth / 2, 0);
        mAircraftHeadingLineDrawable.draw(canvas);
        canvas.restore();
    }

    private void drawMarker(Canvas canvas, Path path, Paint paint, float offsetX, float offsetY) {
        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.drawPath(path, paint);
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

    public void onEventUnitChanged(Integer unit) {
        updateWidget();
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
