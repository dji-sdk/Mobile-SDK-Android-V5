package dji.v5.ux.core.ui.hsi;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.sdk.keyvalue.value.flightcontroller.FCFlightMode;
import dji.v5.ux.R;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class HSIPerceptionLayer implements HSIContract.HSILayer {

    private static final String TAG = HSIPerceptionLayer.class.getSimpleName();

    private static final String PERCEPTION_DISABLED = "NA";

    private static final String PERCEPTION_TOF_ONLY = "TOF";

    private static final int CHECK_RADAR_STATUS = 0;

    /**
     * 雷达路径操作超时
     */
    private static final int RADAR_PATH_OP_TIMEOUT = 500;

    /**
     * 障碍物最远感知距离
     */
    private static final int DEFAULT_MAX_PERCEPTION_DISTANCE_IN_METER = 45;

    private static final int DEFAULT_PERCEPTION_BLIND_AREA_ANGLE = 16;

    private static final int DEFAULT_PERCEPTION_BLIND_AREA_COUNT = 4;

    private static final int DEFAULT_PERCEPTION_AREA_COUNT = 4;

    private static final int DEFAULT_RADAR_START_ANGLE_OFFSET = -15;

    /**
     * 分多少个区块
     */
    private static final int SECTOR_COUNT = 36;

    @NonNull
    private final Path mPath = new Path();

    @NonNull
    private final RectF mRect = new RectF();

    private final int[] mPerceptionLevelColor = new int[3];

    private final int[] mRadarLevelColor = new int[3];

    private List<Integer> mRadarHorizontalDistances = new ArrayList<>();

    private List<Integer> mPerceptionHorizontalDistances = new ArrayList<>();

    private List<Integer> mToFHorizontalDistances = new ArrayList<>();

    @NonNull
    private final GradientDrawable mPerceptionAreaDrawable;

    @NonNull
    private final PublishSubject<List<Integer>> mPerceptionPublisher = PublishSubject.create();
    @NonNull
    private final BehaviorSubject<List<Integer>> mToFPublisher = BehaviorSubject.createDefault(new ArrayList<>());
    @NonNull
    private final PublishSubject<List<Integer>> mRadarPublisher = PublishSubject.create();

//    @NonNull
//    private final PublishSubject<Shape> mPathRecycler = PublishSubject.create();

    private final int mPerceptionDisabledColor;
    private final int mPerceptionDisabledStrokeColor;
    private final int mPerceptionDisabledTextColor;
    private final int mPerceptionDisabledTextSize;

    private final int mMaxPerceptionStrokeWidth;
    private final int mRadarMaxPerceptionStrokeWidth;

    private boolean mIsRadarConnected;

    private HSIContract.HSIContainer mHSIContainer;

    private double mHorizontalBarrierAvoidanceDistance;

    private double mHorizontalPerceptionDistance;

    private int mCompassSize;

    private boolean mShowVisualPerceptionInfo;

    private boolean mShowRadarPerceptionInfo;

    private boolean mInSportMode = false;

    private FCFlightMode mFlightMode = null;

    private boolean mIsMultiModeOpen = false;

    @NonNull
    private final List<Shape> mShapeList = new ArrayList<>();

    @NonNull
    private final List<Shape> mRadarShapeList = new ArrayList<>();

    @Nullable
    private CompositeDisposable mDisposable;

    @NonNull
    private HSIWidgetModel widgetModel;

    /**
     * 前、右、后、左四个方向的视觉感知是否正常工作
     */
    @NonNull
    private final boolean[] mVisionPerceptionEnableState = new boolean[4];

    /**
     * 前、右、后、左四个方向的ToF感知是否正常工作，目前认定ToF几乎不可能不工作，所以默认为True
     */
    @NonNull
    private final boolean[] mToFPerceptionEnableState = new boolean[]{true, true, true, true};

    private final PathPool mPathPool = new PathPool(128);
    private final PathPool mRadarPathPool = new PathPool(128);

    public HSIPerceptionLayer(@NonNull Context context, @Nullable AttributeSet attrs, HSIContract.HSIContainer container,HSIWidgetModel widgetModel) {
        mHSIContainer = container;
        this.widgetModel = widgetModel;

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AttitudeDashBoard);
        mPerceptionLevelColor[2] = typedArray.getColor(R.styleable.HSIView_uxsdk_hsi_avoidance_setting_area_color,
                context.getResources().getColor(R.color.uxsdk_pfd_hsi_barrier_color));
        mPerceptionLevelColor[1] = typedArray.getColor(R.styleable.HSIView_uxsdk_hsi_perception_setting_area_color,
                context.getResources().getColor(R.color.uxsdk_pfd_hsi_avoidance_color));
        mPerceptionLevelColor[0] = typedArray.getColor(R.styleable.HSIView_uxsdk_hsi_max_perception_area_color,
                context.getResources().getColor(R.color.uxsdk_pfd_hsi_main_color));
        mMaxPerceptionStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_max_perception_stroke_width,
                context.getResources().getDimensionPixelSize(R.dimen.uxsdk_3_dp));


        mRadarLevelColor[2] = typedArray.getColor(R.styleable.HSIView_uxsdk_hsi_radar_avoidance_setting_area_color,
                context.getResources().getColor(R.color.uxsdk_pfd_hsi_radar_barrier_color));
        mRadarLevelColor[1] = typedArray.getColor(R.styleable.HSIView_uxsdk_hsi_radar_perception_setting_area_color,
                context.getResources().getColor(R.color.uxsdk_pfd_hsi_radar_avoidance_color));
        mRadarLevelColor[0] = typedArray.getColor(R.styleable.HSIView_uxsdk_hsi_radar_max_perception_area_color,
                context.getResources().getColor(R.color.uxsdk_pfd_hsi_radar_main_color));
        mRadarMaxPerceptionStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_radar_max_perception_stroke_width,
                context.getResources().getDimensionPixelSize(R.dimen.uxsdk_5_dp));
        typedArray.recycle();

        mPerceptionDisabledColor = context.getResources().getColor(R.color.uxsdk_black_30_percent);
        mPerceptionDisabledStrokeColor = context.getResources().getColor(R.color.uxsdk_pfd_hsi_perception_disabled_stroke_color);
        mPerceptionDisabledTextColor = context.getResources().getColor(R.color.uxsdk_white);
        mPerceptionDisabledTextSize = context.getResources().getDimensionPixelSize(R.dimen.uxsdk_text_size_normal);

        mPerceptionAreaDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{
                        Color.parseColor("#26FFFFFF"),
                        Color.parseColor("#0CFFFFFF")
                });
        mPerceptionAreaDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        mPerceptionAreaDrawable.setShape(GradientDrawable.RECTANGLE);
    }

    @Override
    public void onStart() {
        mDisposable = new CompositeDisposable();

        mDisposable.add(widgetModel.perceptionTOFDistanceProcessor.toFlowable()
                .subscribe(status -> mToFPublisher.onNext(status.getDists())));

        mDisposable.add(widgetModel.obstacleAvoidanceSensorStateProcessor.toFlowable().subscribe(omniAvoidanceState -> {
            mVisionPerceptionEnableState[0] = omniAvoidanceState.getIsFrontObstacleAvoidanceEnable();
            mVisionPerceptionEnableState[1] = omniAvoidanceState.getIsRightObstacleAvoidanceEnable();
            mVisionPerceptionEnableState[2] = omniAvoidanceState.getIsBackObstacleAvoidanceEnable();
            mVisionPerceptionEnableState[3] = omniAvoidanceState.getIsLeftObstacleAvoidanceEnable();
        }));

        mDisposable.add(
                Flowable.combineLatest(
                        widgetModel.omniHorizontalAvoidanceEnabledProcessor.toFlowable(),
                        widgetModel.perceptionFullDistanceProcessor.toFlowable(),
                        widgetModel.omniHorizontalRadarDistanceProcessor.toFlowable(),
                        widgetModel.horizontalAvoidanceDistanceProcessor.toFlowable(),
                        (avoidanceEnabled, radarStatus, radarDistance, avoidanceDistance) -> {
                            mHorizontalPerceptionDistance = radarDistance;
                            mHorizontalBarrierAvoidanceDistance = avoidanceDistance;

                            mShowVisualPerceptionInfo = avoidanceEnabled;
                            if (mShowVisualPerceptionInfo) {
                                mPerceptionPublisher.onNext(radarStatus.getDists());
                            } else {
                                mPerceptionPublisher.onNext(new ArrayList<>());
                            }
                            return true;
                        }).subscribe()
        );

        mDisposable.add(
                Flowable.combineLatest(
                        widgetModel.radarConnectionProcessor.toFlowable(),
                        widgetModel.radarHorizontalObstacleAvoidanceEnabledProcessor.toFlowable(),
                        widgetModel.radarObstacleAvoidanceStateProcessor.toFlowable(),
                        (isRadarConnected, radarEnable, avoidanceState) -> {
                            mIsRadarConnected = isRadarConnected;
                            mShowRadarPerceptionInfo = radarEnable;
                            if (!mShowRadarPerceptionInfo || !mIsRadarConnected) {
                                mRadarPublisher.onNext(new ArrayList<>());
                            } else {
                                mRadarPublisher.onNext(avoidanceState.getEveryAngleDistance());
                            }
                            return true;
                        }
                ).subscribe()
        );

        mDisposable.add(Flowable.combineLatest(
                widgetModel.flightModeProcessor.toFlowable(),
                widgetModel.multipleFlightModeEnabledProcessor.toFlowable(),
                (fcFlightMode, isMultiModeOpen) -> {
                    mFlightMode = fcFlightMode;
                    mIsMultiModeOpen = isMultiModeOpen;
                    mInSportMode = mIsMultiModeOpen && mFlightMode == FCFlightMode.GPS_SPORT;
                    Arrays.fill(mToFPerceptionEnableState, !mInSportMode);
                    return true;
                }).subscribe()
        );

        doDisposableAdd();

        //radar推送用RxJava debonounce操作会导致雷达显示时隐时现, 改成自定义的定时器
        mDisposable.add(mRadarPublisher.subscribe(distances -> mRadarHorizontalDistances = distances));
        mDisposable.add(getRadarDisposable());
    }

    private void doDisposableAdd() {
        mDisposable.add(mPerceptionPublisher.subscribe(distances -> mPerceptionHorizontalDistances = distances));

        mDisposable.add(mToFPublisher.subscribe(distances -> mToFHorizontalDistances = distances));

        mDisposable.add(Observable.interval(HSIView.INVALIDATE_INTERVAL_TIME, TimeUnit.MILLISECONDS)
                .map(aLong -> {
                    return performDisposableMap();
                })
                .subscribeOn(AndroidSchedulers.mainThread()) //shape 计算要在mainthread，不然有可能会导致native内存使用错误
                .observeOn(AndroidSchedulers.mainThread())
                .map(data -> updatePerceptionDrawShape(optimizationData(data)))
                .subscribe(list -> {
                    mShapeList.clear();
                    mShapeList.addAll(list);
                    if (mHSIContainer != null) {
                        mHSIContainer.updateWidget();
                    }
                }));
    }

    private List<Integer> performDisposableMap(){
        List<Integer> perception = mPerceptionHorizontalDistances;
        List<Integer> tof = mToFHorizontalDistances;
        if (tof.size() == 0 || perception.size() == 0) {
            return perception;
        }

        // 如果视觉不可用了就把tof的融合进去
        int perceptionAngleTotal = 360 - DEFAULT_PERCEPTION_BLIND_AREA_COUNT * DEFAULT_PERCEPTION_BLIND_AREA_ANGLE;
        int rotationOffset = 360 / perception.size();
        int perceptionAngleEach = perceptionAngleTotal / DEFAULT_PERCEPTION_AREA_COUNT / rotationOffset;
        int arrayLength = perception.size();

        int srcPos = -perceptionAngleEach / 2;
        if (!mVisionPerceptionEnableState[0]) {
            int fromIndex = arrayLength + srcPos;
            int toIndex = fromIndex + perceptionAngleEach / 2;
            for (int i = fromIndex; i < toIndex && i < perception.size(); i++) {
                perception.set(i, tof.get(0));
            }
            fromIndex = srcPos + perceptionAngleEach / 2;
            toIndex = fromIndex + perceptionAngleEach / 2;
            for (int i = fromIndex; i < toIndex && i < perception.size(); i++) {
                perception.set(i, tof.get(0));
            }
        }
        for (int i = 1; i < 4; i++) {
            srcPos += (perceptionAngleEach + DEFAULT_PERCEPTION_BLIND_AREA_ANGLE);
            if (!mVisionPerceptionEnableState[i]) {
                for (int j = srcPos; j < srcPos + perceptionAngleEach; j++) {
                    perception.set(j, tof.get(i));
                }
            }
        }

        return perception;
    }

    @Override
    public void onStop() {
        if (mDisposable != null) {
            mDisposable.dispose();
        }

        mPathPool.clear();
        mRadarPathPool.clear();

        mHSIContainer = null;
    }

    private Disposable getRadarDisposable() {
        return Observable.interval(HSIView.INVALIDATE_INTERVAL_TIME, TimeUnit.MILLISECONDS)
                .map(aLong -> mRadarHorizontalDistances)
                .observeOn(Schedulers.newThread())
                .map(this::updateRadarDrawShape)
                .timeout(RADAR_PATH_OP_TIMEOUT, TimeUnit.MILLISECONDS)
                .retry()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    synchronized (mRadarShapeList) {
                        mRadarShapeList.clear();
                        mRadarShapeList.addAll(list);
                    }
                    if(mHSIContainer !=null) {
                        mHSIContainer.updateWidget();
                    }
                });
    }

    @Override
    public void draw(Canvas canvas, Paint paint, int compassSize) {
        mCompassSize = compassSize;
        canvas.save();
        canvas.translate(0, (float) compassSize / 2);
        drawPerception(canvas, paint, compassSize);
        drawRadarBarrier(canvas, paint, compassSize);
        drawBarrier(canvas, paint, compassSize);
        canvas.restore();
    }


    private List<Integer> optimizationData(List<Integer> horizontalBarrierDistance) {

        if (horizontalBarrierDistance.size() == 0) {
            return horizontalBarrierDistance;
        }

        int sectorLength = horizontalBarrierDistance.size() / SECTOR_COUNT;
        List<Integer> sector = new ArrayList<>(sectorLength);

        for (int i = 0; i < SECTOR_COUNT; i++) {
            // 每一个扇形区的数据，排序
            int srcPos = sectorLength * i;
            for (int j = 0; j < sectorLength; j++) {
                sector.add(j, horizontalBarrierDistance.get(srcPos + j));
            }

            calculateSector(sectorLength, sector, i, horizontalBarrierDistance);
        }
        return horizontalBarrierDistance;
    }

    private void calculateSector(int sectorLength, List<Integer> sector, int i, List<Integer> horizontalBarrierDistance) {
        float sum = 0;
        for (int j = 0; j < sectorLength; j++) {
            sum += sector.get(j);
        }
        float avg = sum / sectorLength;

        sum = 0;
        for (int j = 0; j < sectorLength; j++) {
            sum += (sector.get(j) - avg) * (sector.get(j) - avg);
        }
        double standardDeviation = Math.sqrt(sum / sectorLength);

        int divider = 0;
        sum = 0;
        for (int j = 0; j < sectorLength; j++) {
            if (sector.get(j) <= avg + standardDeviation && sector.get(j) >= avg - standardDeviation) {
                sum += sector.get(j);
                divider += 1;
            }
        }

        if (divider >= 2) {
            avg = sum / divider;
            for (int j = sectorLength * i; j < sectorLength * (i + 1); j++) {
                horizontalBarrierDistance.set(j, Integer.parseInt(String.valueOf((int) avg)));
            }
        }
    }

    private void drawPerception(Canvas canvas, Paint paint, int compassSize) {
        if(mHSIContainer == null) return;
        canvas.save();

        int perceptionAngleTotal = 360 - DEFAULT_PERCEPTION_BLIND_AREA_COUNT * DEFAULT_PERCEPTION_BLIND_AREA_ANGLE;
        int perceptionAngleEach = perceptionAngleTotal / DEFAULT_PERCEPTION_AREA_COUNT;

        paint.setStyle(Paint.Style.FILL);
        float radius = (float) compassSize / 2 - mHSIContainer.getCalibrationAreaWidth() - mHSIContainer.getCompassBitmapOffset();
        mRect.set(-radius, -radius, radius, radius);

        mPerceptionAreaDrawable.setSize((int) radius * 2, (int) radius * 2);
        float offsetX = (float) (Math.sin(Math.PI * perceptionAngleEach / 2 / 180) * radius);
        float offsetY = (float) (Math.cos(Math.PI * perceptionAngleEach / 2 / 180) * radius);
        mPath.reset();
        mPath.lineTo(offsetX, -offsetY);
        offsetX = (float) (Math.sin(Math.PI * (360 - (float) perceptionAngleEach / 2) / 180) * radius);
        offsetY = (float) (Math.cos(Math.PI * (360 - (float) perceptionAngleEach / 2) / 180) * radius);
        mPath.lineTo(offsetX, -offsetY);
        mPath.close();
        mPath.addArc(mRect, 270 - (float) perceptionAngleEach / 2, perceptionAngleEach);
        // 按照前、右、后、左的顺序绘制
        for (int i = 0; i < DEFAULT_PERCEPTION_AREA_COUNT; i++) {
            canvas.save();
            if (!mInSportMode && (mVisionPerceptionEnableState[i] || !mShowVisualPerceptionInfo)) {
                canvas.clipPath(mPath);
                mPerceptionAreaDrawable.setBounds((int) -radius, (int) -radius, (int) radius, 0);
                mPerceptionAreaDrawable.draw(canvas);
            } else {
                paint.setColor(mPerceptionDisabledColor);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawArc(mRect, 270 - (float) perceptionAngleEach / 2, perceptionAngleEach, true, paint);
                paint.setColor(mPerceptionDisabledStrokeColor);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawArc(mRect, 270 - (float) perceptionAngleEach / 2, perceptionAngleEach, true, paint);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(mPerceptionDisabledTextColor);
                paint.setTextSize(mPerceptionDisabledTextSize);
                paint.setTextAlign(Paint.Align.CENTER);
                String text = mToFPerceptionEnableState[i] ? PERCEPTION_TOF_ONLY : PERCEPTION_DISABLED;
                paint.getTextBounds(text, 0, text.length(), HSIView.RECT);
                Paint.FontMetrics fontMetrics = paint.getFontMetrics();
                float baselineOffsetY = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom;
                canvas.save();
                canvas.translate(0, -(radius + mHSIContainer.getAircraftSize()) / 2);
                canvas.rotate(-90 * (float) i);
                canvas.drawText(text, 0, baselineOffsetY, paint);
                canvas.restore();
            }
            canvas.restore();
            canvas.rotate((float) perceptionAngleEach + DEFAULT_PERCEPTION_BLIND_AREA_ANGLE);
        }

        canvas.restore();
    }

    private void drawBarrier(Canvas canvas, Paint paint, int compassSize) {
        if(mHSIContainer == null) return;

        if (mShapeList.isEmpty()) {
            return;
        }

        float calibrationAreaWidth = mHSIContainer.getCalibrationAreaWidth();
        float radius = (float) compassSize / 2 - calibrationAreaWidth - mHSIContainer.getCompassBitmapOffset();
        // 这里把盲区也考虑上，从盲区中间算起，算起来比较简单
        int perceptionAngleEach = 360 / DEFAULT_PERCEPTION_AREA_COUNT;

        for (Shape shape : mShapeList) {
            canvas.save();
            // 判断该方向的感知系统是否正常工作
            boolean skip = judgeSkip(shape, perceptionAngleEach);
            if (!skip) {
                if (shape instanceof PathShape) {
                    canvas.rotate(shape.mFromAngle);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(shape.mColor);
                    canvas.drawPath(((PathShape) shape).mPath, paint);
                    mPathPool.recycle(((PathShape) shape).mPath);
                } else if (shape instanceof ArcShape) {
                    canvas.rotate(shape.mFromAngle);
                    paint.setColor(shape.mColor);
                    paint.setStyle(Paint.Style.STROKE);
                    float lastStrokeWidth = paint.getStrokeWidth();
                    paint.setStrokeWidth(mMaxPerceptionStrokeWidth);
                    float arcRadius = radius - (float) mMaxPerceptionStrokeWidth / 2;
                    canvas.drawArc(-arcRadius, -arcRadius, arcRadius, arcRadius, 270,
                            ((ArcShape) shape).mToAngle - (float)shape.mFromAngle, false, paint);
                    paint.setStrokeWidth(lastStrokeWidth);
                }
            }
            canvas.restore();
        }
    }

    private boolean judgeSkip(Shape shape, int perceptionAngleEach) {
        int fromAngle = shape.mFromAngle;
        boolean skip = mInSportMode;
        if (fromAngle > perceptionAngleEach * 7 / 2 || fromAngle < perceptionAngleEach / 2) {
            skip |= !mVisionPerceptionEnableState[0] && !mToFPerceptionEnableState[0];
        } else if (fromAngle > perceptionAngleEach / 2 && fromAngle < perceptionAngleEach * 3 / 2) {
            skip |= !mVisionPerceptionEnableState[1] && !mToFPerceptionEnableState[1];
        } else if (fromAngle > perceptionAngleEach * 3 / 2 && fromAngle < perceptionAngleEach * 5 / 2) {
            skip |= !mVisionPerceptionEnableState[2] && !mToFPerceptionEnableState[2];
        } else if (fromAngle > perceptionAngleEach * 5 / 2 && fromAngle < perceptionAngleEach * 7 / 2) {
            skip |= !mVisionPerceptionEnableState[3] && !mToFPerceptionEnableState[3];
        }
        return skip;
    }

    private void drawRadarBarrier(Canvas canvas, Paint paint, int compassSize) {
        if(mHSIContainer == null){
            return;
        }

        if (mRadarShapeList.isEmpty()) {
            return;
        }

        float calibrationAreaWidth = mHSIContainer.getCalibrationAreaWidth();
        float radius = (float) compassSize / 2 - calibrationAreaWidth - mHSIContainer.getCompassBitmapOffset();

        synchronized (mRadarShapeList) {
            for (Shape shape : mRadarShapeList) {
                canvas.save();
                if (shape instanceof PathShape) {
                    canvas.rotate((float)shape.mFromAngle + DEFAULT_RADAR_START_ANGLE_OFFSET);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(shape.mColor);
                    canvas.drawPath(((PathShape) shape).mPath, paint);
                    mRadarPathPool.recycle(((PathShape) shape).mPath);
                } else if (shape instanceof ArcShape) {
                    canvas.rotate((float)shape.mFromAngle + DEFAULT_RADAR_START_ANGLE_OFFSET);
                    paint.setColor(shape.mColor);
                    paint.setStyle(Paint.Style.STROKE);
                    float lastStrokeWidth = paint.getStrokeWidth();
                    paint.setStrokeWidth(mRadarMaxPerceptionStrokeWidth);
                    float arcRadius = radius - (float) mRadarMaxPerceptionStrokeWidth / 2;
                    canvas.drawArc(-arcRadius, -arcRadius, arcRadius, arcRadius, 270,
                            ((ArcShape) shape).mToAngle - (float)shape.mFromAngle, false, paint);
                    paint.setStrokeWidth(lastStrokeWidth);
                }
                canvas.restore();
            }
        }
    }

    private List<Shape> updatePerceptionDrawShape(List<Integer> horizontalBarrierDistance) {
        if (horizontalBarrierDistance.size() == 0) {
            return new ArrayList<>();
        }
        int perceptionAngleTotal = 360 - DEFAULT_PERCEPTION_BLIND_AREA_COUNT * DEFAULT_PERCEPTION_BLIND_AREA_ANGLE;
        int perceptionAngleEach = perceptionAngleTotal / DEFAULT_PERCEPTION_AREA_COUNT;
        int rotationOffset = 360 / horizontalBarrierDistance.size();
        int startOffset = -perceptionAngleEach / rotationOffset / 2;

        return updateDrawShape(horizontalBarrierDistance, startOffset, mPerceptionLevelColor);
    }

    private List<Shape> updateRadarDrawShape(List<Integer> horizontalBarrierDistance) {
        if (horizontalBarrierDistance == null || horizontalBarrierDistance.size() == 0) {
            return new ArrayList<>();
        }
        Thread currentThread = Thread.currentThread();
        long preTime = System.currentTimeMillis();
        List<Shape> result = updateDrawShape2(horizontalBarrierDistance, 0, mRadarLevelColor);
        return result;
    }

    private List<Shape> updateDrawShape2(List<Integer> horizontalBarrierDistance, int startOffset, int[] levelColor) {

        List<Shape> shapeList = new ArrayList<>();
        if(mHSIContainer == null){
            return shapeList;
        }


        int rotationOffset = 360 / horizontalBarrierDistance.size();
        int offset = mHSIContainer.getAircraftSize() / 2;
        int visibleDistanceInHsi = mHSIContainer.getVisibleDistanceInHsiInMeters();
        float calibrationAreaWidth = mHSIContainer.getCalibrationAreaWidth();
        float radius = (float) mCompassSize / 2 - calibrationAreaWidth - mHSIContainer.getCompassBitmapOffset();

        float minDistanceInMeter = Integer.MAX_VALUE;

        PathShape lastShape = null;
        float barrierRotation = 0;
        Path path1 = mRadarPathPool.acquire(), path2;
        path1.reset();

        for (int i = 0; i < horizontalBarrierDistance.size(); i++) {
            int angle = i + startOffset;
            angle = angle < 0 ? angle + horizontalBarrierDistance.size() : angle;
            float distanceInMeter = (float) horizontalBarrierDistance.get(angle) / 1000;
            if (distanceInMeter >= visibleDistanceInHsi) {
                doShape(distanceInMeter, levelColor, angle, rotationOffset, shapeList);
            } else {
                float c = offset + distanceInMeter / visibleDistanceInHsi * (radius - offset);
                if (barrierRotation == 0) {
                    lastShape = new PathShape(angle * rotationOffset);
                    path1.reset();
                    path1.lineTo(0, -c);
                }
                barrierRotation += rotationOffset;
                double sin = Math.sin(Math.PI * barrierRotation / 180);
                double cos = Math.cos(Math.PI * barrierRotation / 180);
                float offsetX = (float) (sin * c);
                float offsetY = (float) (cos * c);
                path1.lineTo(offsetX, -offsetY);
                if (distanceInMeter < minDistanceInMeter) {
                    minDistanceInMeter = distanceInMeter;
                }
            }
            if (needShape(distanceInMeter, visibleDistanceInHsi, i, horizontalBarrierDistance, path1, lastShape) && lastShape != null) {
                path1.close();
                path2 = mRadarPathPool.acquire();
                path2.reset();
                path2.lineTo(0, -radius);
                float offsetX = (float) (Math.sin(Math.PI * barrierRotation / 180) * radius);
                float offsetY = (float) (Math.cos(Math.PI * barrierRotation / 180) * radius);
                path2.lineTo(offsetX, -offsetY);
                path2.close();
                path2.addArc(-radius, -radius, radius, radius, 270, barrierRotation);
                //必须重构path的布尔操作算法，怀疑是雷达传递数据有问题，导致方法卡死
                path2.op(path1, Path.Op.DIFFERENCE);
                int areaColor = getAreaColor(minDistanceInMeter, levelColor);
                PathShape pathShape = lastShape;
                pathShape.mColor = areaColor;
                pathShape.mPath = path2;
                barrierRotation = 0;
                shapeList.add(pathShape);
                path1.reset();
                lastShape = null;
                minDistanceInMeter = Integer.MAX_VALUE;
            }
        }
        mRadarPathPool.recycle(path1);
        return shapeList;
    }

    private boolean needShape(float distanceInMeter,int visibleDistanceInHsi,int i,List<Integer> horizontalBarrierDistance,Path path1,PathShape lastShape){
        return (distanceInMeter >= visibleDistanceInHsi || i == horizontalBarrierDistance.size() - 1)
                && !path1.isEmpty() && lastShape != null;
    }

    private void doShape(float distanceInMeter, int[] levelColor, int angle, int rotationOffset, List<Shape> shapeList) {
        if (distanceInMeter <= DEFAULT_MAX_PERCEPTION_DISTANCE_IN_METER) {
            int color;
            if (distanceInMeter > mHorizontalPerceptionDistance) {
                color = levelColor[0];
            } else {
                color = levelColor[1];
            }
            ArcShape shape = new ArcShape(angle * rotationOffset);
            shape.mColor = color;
            shape.mToAngle += rotationOffset;
            shapeList.add(shape);
        }
    }

    private List<Shape> updateDrawShape(List<Integer> horizontalBarrierDistance, int startOffset, int[] levelColor) {
        List<Shape> shapeList = new ArrayList<>();

        if(mHSIContainer == null) return shapeList;


        int rotationOffset = 360 / horizontalBarrierDistance.size();
        int offset = mHSIContainer.getAircraftSize() / 2;
        int visibleDistanceInHsi = mHSIContainer.getVisibleDistanceInHsiInMeters();
        float calibrationAreaWidth = mHSIContainer.getCalibrationAreaWidth();
        float radius = (float) mCompassSize / 2 - calibrationAreaWidth - mHSIContainer.getCompassBitmapOffset();

        float minDistanceInMeter = Integer.MAX_VALUE;

        Shape lastShape = null;
        float barrierRotation = 0;
        Path path1 = mPathPool.acquire(), path2;
        path1.reset();

        for (int i = 0; i < horizontalBarrierDistance.size(); i++) {
            int angle = i + startOffset;
            angle = angle < 0 ? angle + horizontalBarrierDistance.size() : angle;
            float distanceInMeter = (float) horizontalBarrierDistance.get(angle) / 1000;
            if (distanceInMeter >= visibleDistanceInHsi) {
                performDistanceInMeter(distanceInMeter, levelColor, angle, rotationOffset, shapeList);
            } else {
                float c = offset + distanceInMeter / visibleDistanceInHsi * (radius - offset);
                if (barrierRotation == 0) {
                    lastShape = new PathShape(angle * rotationOffset);
                    path1.reset();
                    path1.lineTo(0, -c);
                }
                barrierRotation += rotationOffset;
                double sin = Math.sin(Math.PI * barrierRotation / 180);
                double cos = Math.cos(Math.PI * barrierRotation / 180);
                float offsetX = (float) (sin * c);
                float offsetY = (float) (cos * c);
                path1.lineTo(offsetX, -offsetY);
                if (distanceInMeter < minDistanceInMeter) {
                    minDistanceInMeter = distanceInMeter;
                }
            }
            if (judgeDistanceInMeter(distanceInMeter, visibleDistanceInHsi, i, horizontalBarrierDistance, path1, lastShape) && lastShape != null) {
                path1.close();
                path2 = mPathPool.acquire();
                path2.reset();
                path2.lineTo(0, -radius);
                float offsetX = (float) (Math.sin(Math.PI * barrierRotation / 180) * radius);
                float offsetY = (float) (Math.cos(Math.PI * barrierRotation / 180) * radius);
                path2.lineTo(offsetX, -offsetY);
                path2.close();
                path2.addArc(-radius, -radius, radius, radius, 270, barrierRotation);
//                DJILog.d(TAG, "updateRadar op 1 size=" + shapeList.size() + " " + minDistanceInMeter);
//                sendRadarMsg();
                path2.op(path1, Path.Op.DIFFERENCE);
//                DJILog.d(TAG, "updateRadar op 2");
//                removeRadarMsg();
                int areaColor = getAreaColor(minDistanceInMeter, levelColor);
                PathShape pathShape = (PathShape) lastShape;
                pathShape.mColor = areaColor;
                pathShape.mPath = path2;
                barrierRotation = 0;
                shapeList.add(pathShape);
                path1.reset();
                lastShape = null;
                minDistanceInMeter = Integer.MAX_VALUE;
            }
        }
        mPathPool.recycle(path1);
        return shapeList;
    }

    private boolean judgeDistanceInMeter(float distanceInMeter, int visibleDistanceInHsi, int i, List<Integer> horizontalBarrierDistance, Path path1, Shape lastShape) {
        return (distanceInMeter >= visibleDistanceInHsi || i == horizontalBarrierDistance.size() - 1)
                && !path1.isEmpty() && lastShape instanceof PathShape;
    }

    private int getAreaColor(float minDistanceInMeter,int[] levelColor){
        int areaColor;
        if (minDistanceInMeter > mHorizontalPerceptionDistance) {
            areaColor = levelColor[0];//
        } else if (minDistanceInMeter > mHorizontalBarrierAvoidanceDistance + 2) {
            // feature HYAPP-10551 避障变红由【刹停距离】改为【刹停距离+2m】
            areaColor = levelColor[1];
        } else {
            areaColor = levelColor[2];
        }
        return areaColor;
    }

    private void performDistanceInMeter(float distanceInMeter, int[] levelColor, int angle, int rotationOffset, List<Shape> shapeList) {
        if (distanceInMeter <= DEFAULT_MAX_PERCEPTION_DISTANCE_IN_METER) {
            int color;
            if (distanceInMeter > mHorizontalPerceptionDistance) {
                color = levelColor[0];
            } else {
                color = levelColor[1];
            }
            ArcShape shape = new ArcShape(angle * rotationOffset);
            shape.mColor = color;
            shape.mToAngle += rotationOffset;
            shapeList.add(shape);
        }
    }

    private static class Shape {
        int mFromAngle;
        int mColor;

        public Shape(int fromAngle) {
            mFromAngle = fromAngle;
        }
    }

    private static class PathShape extends Shape {
        Path mPath;

        public PathShape(int fromAngle) {
            super(fromAngle);
        }

        @Override
        public String toString() {
            return "PathShape{" +
                    "mFromAngle=" + mFromAngle +
                    '}';
        }
    }

    private static class ArcShape extends Shape {
        int mToAngle;

        public ArcShape(int fromAngle) {
            super(fromAngle);
            mToAngle = fromAngle;
        }

        @Override
        public String toString() {
            return "ArcShape{" +
                    "mFromAngle=" + mFromAngle +
                    ", mToAngle=" + mToAngle +
                    '}';
        }
    }

    private static class PathPool extends RecyclerPool<Path> {

        public PathPool(int maxPoolSize) {
            super(maxPoolSize);
        }

        @NonNull
        @Override
        protected Path create() {
            return new Path();
        }
    }
}
