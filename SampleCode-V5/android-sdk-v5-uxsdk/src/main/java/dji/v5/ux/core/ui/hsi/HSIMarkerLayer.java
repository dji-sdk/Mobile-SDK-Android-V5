package dji.v5.ux.core.ui.hsi;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.location.Location;
import android.text.TextUtils;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.sdk.keyvalue.value.common.LocationCoordinate2D;
import dji.sdk.keyvalue.value.flightcontroller.AirSenseAirplaneState;
import dji.sdk.keyvalue.value.flightcontroller.AirSenseSystemInformation;
import dji.sdk.keyvalue.value.flightcontroller.AirSenseWarningLevel;
import dji.v5.utils.common.ContextUtil;
import dji.v5.utils.common.LocationUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.util.AndUtil;
import dji.v5.ux.core.util.DisplayUtil;
import dji.v5.ux.core.util.UnitUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class HSIMarkerLayer implements HSIContract.HSILayer {

    @NonNull
    private static final Path PATH = new Path();

    @NonNull
    private static final Path CIRCLE_PATH = new Path();

    @NonNull
    private HSIContract.HSIContainer mHSIContainer;

    @NonNull
    private final float[] mFloats = new float[2];

    @NonNull
    private static final BearingDistance INVALID_BEARING_DISTANCE = new BearingDistance();

    @NonNull
    private final Bitmap mHomePointBitmap;

    private final int mHomePointMarkerSize;

    private final int mHomePointMarkerTextColor;

//    @NonNull
//    private final Bitmap mWayPointBitmap;
//
//    private final int mWayPointMarkerSize;
//
//    private final int mWayPointMarkerTextColor;

    private final int mMarkerIndicatorTextSize;

    private final int mMarkerIndicatorHeight;

    private final int mMarkerIndicatorWidth;

    private final int mMarkerIndicatorStrokeColor;

    private final int mMarkerIndicatorBackgroundColor;

    private final int mMarkerIndicatorMarginToCompass;

    @NonNull
    private Bitmap mPinPointBitmap;

    private final int mPinPointMarkerSize;

    private final int mPinPointMarkerTextColor;

    @NonNull
    private final Bitmap mRngPointBitmap;

    private final int mRngPointMarkerSize;

    private final int mRngPointMarkerTextColor;

    @NonNull
    private final Bitmap mSmartTrackPointBitmap;

    private final int mSmartTrackPointMarkerSize;

    private final int mSmartTrackPointMarkerTextColor;

    @NonNull
    private Bitmap mAdsbRedBitmap;

    @NonNull
    private Bitmap mAdsbYellowBitmap;

    private final int mAdsbMarkerSize;

    private final int mMarkerMarginVertical;

    private final int mMarkerMarginHorizontal;

    @NonNull
    private final BehaviorSubject<LocationCoordinate2D> mSubject = BehaviorSubject.create();

    @Nullable
    private LocationCoordinate2D mAircraftLocation;

    @Nullable
    private LocationCoordinate2D mHomeLocation;

    @Nullable
    private BearingDistance mHomeDistance;

    @NonNull
    private final List<BearingDistance> mPinPointsDistances = new ArrayList<>();

    private BearingDistance mSelectPinDistance;

    @Nullable
    private BearingDistance mSmartTrackDistance;

    @Nullable
    private BearingDistance mRngDistance;

    @NonNull
    private final CopyOnWriteArrayList<BearingDistance> mAdsbYellowDistances = new CopyOnWriteArrayList<>();

    @NonNull
    private final CopyOnWriteArrayList<BearingDistance> mAdsbRedDistances = new CopyOnWriteArrayList<>();

    private long mAdsbTime = 0;

    private Disposable mAdsbTimeDisposable;

    @Nullable
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    @NonNull
    private HSIWidgetModel widgetModel;

    public HSIMarkerLayer(@NonNull Context context, @Nullable AttributeSet attrs, @NonNull HSIContract.HSIContainer container, @NonNull HSIWidgetModel widgetModel) {
        mHSIContainer = container;

        this.widgetModel = widgetModel;

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.HSIView);
        mHomePointMarkerSize = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_home_point_marker_size,
                context.getResources().getDimensionPixelSize(R.dimen.uxsdk_12_dp));
        mHomePointBitmap = BitmapFactory.decodeResource(context.getResources(),
                typedArray.getResourceId(R.styleable.HSIView_uxsdk_hsi_home_point_marker, R.drawable.uxsdk_fpv_pfd_hsi_home_point));
        mHomePointMarkerTextColor = typedArray.getColor(R.styleable.HSIView_uxsdk_hsi_home_point_marker_text_color,
                Color.parseColor("#FFCC00"));
//        mWayPointMarkerSize = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_waypoint_marker_size,
//                context.getResources().getDimensionPixelSize(R.dimen.uxsdk_12_dp));
//        mWayPointBitmap = BitmapFactory.decodeResource(context.getResources(),
//                typedArray.getResourceId(R.styleable.HSIView_uxsdk_hsi_waypoint_marker, R.drawable.fpv_pfd_hsi_waypoint));
//        mWayPointMarkerTextColor = typedArray.getColor(R.styleable.HSIView_uxsdk_hsi_waypoint_marker_text_color,
//                ContextCompat.getColor(context, R.color.blue_highlight));
        mPinPointMarkerSize = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_pin_point_marker_size,
                context.getResources().getDimensionPixelSize(R.dimen.uxsdk_12_dp));
        mPinPointBitmap = BitmapFactory.decodeResource(context.getResources(),
                typedArray.getResourceId(R.styleable.HSIView_uxsdk_hsi_pin_point_marker, R.drawable.uxsdk_fpv_pfd_hsi_pin_point));
        mPinPointMarkerTextColor = typedArray.getColor(R.styleable.HSIView_uxsdk_hsi_pin_point_marker_text_color,
                Color.parseColor("#99FFAA"));

        mSmartTrackPointMarkerSize = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_smart_track_point_marker_size,
                context.getResources().getDimensionPixelSize(R.dimen.uxsdk_12_dp));
        mSmartTrackPointBitmap = BitmapFactory.decodeResource(context.getResources(),
                typedArray.getResourceId(R.styleable.HSIView_uxsdk_hsi_smart_track_point_marker, R.drawable.uxsdk_fpv_pfd_hsi_smart_track_point));
        mSmartTrackPointMarkerTextColor = typedArray.getColor(R.styleable.HSIView_uxsdk_hsi_smart_track_point_marker_text_color,
                Color.parseColor("#99FFAA"));

        mRngPointMarkerSize = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_rng_point_marker_size,
                context.getResources().getDimensionPixelSize(R.dimen.uxsdk_12_dp));
        mRngPointBitmap = BitmapFactory.decodeResource(context.getResources(),
                typedArray.getResourceId(R.styleable.HSIView_uxsdk_hsi_rng_point_marker, R.drawable.uxsdk_fpv_pfd_hsi_rng_point));
        mRngPointMarkerTextColor = typedArray.getColor(R.styleable.HSIView_uxsdk_hsi_rng_point_marker_text_color,
                context.getResources().getColor(R.color.uxsdk_pfd_barrier_color));

        mAdsbMarkerSize = (int) DisplayUtil.dipToPx(context, 20f);
        mAdsbRedBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.uxsdk_ic_hsi_plane_danger);
        mAdsbYellowBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.uxsdk_ic_hsi_plane_caution);

        mMarkerMarginHorizontal = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_marker_margin_horizontal,
                0);
        mMarkerMarginVertical = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_marker_margin_vertical,
                0);
        mMarkerIndicatorTextSize = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_marker_indicator_text_size,
                context.getResources().getDimensionPixelSize(R.dimen.uxsdk_text_size_normal_medium));
        mMarkerIndicatorWidth = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_marker_indicator_width,
                context.getResources().getDimensionPixelSize(R.dimen.uxsdk_27_dp));
        mMarkerIndicatorHeight = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_marker_indicator_height,
                context.getResources().getDimensionPixelSize(R.dimen.uxsdk_23_dp));
        mMarkerIndicatorMarginToCompass = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_marker_indicator_margin_to_compass,
                context.getResources().getDimensionPixelSize(R.dimen.uxsdk_1_dp));
        mMarkerIndicatorStrokeColor = typedArray.getColor(R.styleable.HSIView_uxsdk_hsi_marker_indicator_stroke_color,
                Color.parseColor("#3399FFAA"));
        mMarkerIndicatorBackgroundColor = typedArray.getColor(R.styleable.HSIView_uxsdk_hsi_marker_indicator_background_color,
                Color.parseColor("#33000000"));
        typedArray.recycle();

    }

    private void airSenseSystemInformationHandler(AirSenseSystemInformation info) {
        mAdsbYellowDistances.clear();
        mAdsbRedDistances.clear();
        if (info.getWarningLevel().value() < AirSenseWarningLevel.LEVEL_2.value()) {
            return;
        }
        List<AirSenseAirplaneState> airSenseAirplaneStates = info.getAirplaneStates();
        //添加计时器，用于控制 adsb hsi小飞机闪烁时间
        if (mAdsbTimeDisposable == null || mAdsbTimeDisposable.isDisposed()) {
            mAdsbTimeDisposable = Observable.interval(0, 400, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(aLong -> {
                        mAdsbTime = aLong;
                        mHSIContainer.updateWidget();
                    });
            mCompositeDisposable.add(mAdsbTimeDisposable);
        }
        if (airSenseAirplaneStates == null) {
            return;
        }
        for (AirSenseAirplaneState airplaneState : airSenseAirplaneStates) {
            BearingDistance bearingDistance = computeRelativeLocation(airplaneState.getLatitude(), airplaneState.getLongitude());
            if (airplaneState.getWarningLevel() == AirSenseWarningLevel.LEVEL_2) {
                mAdsbYellowDistances.add(bearingDistance);
            } else if (airplaneState.getWarningLevel() == AirSenseWarningLevel.LEVEL_3
                    || airplaneState.getWarningLevel() == AirSenseWarningLevel.LEVEL_4) {
                mAdsbRedDistances.add(bearingDistance);
            }
        }
    }

    @Override
    public void onStart() {
        mCompositeDisposable = new CompositeDisposable();

        addDisposable(widgetModel.aircraftLocationDataProcessor.toFlowable().subscribe(location -> {
            if (location != null) {
                mAircraftLocation = location;
                if (mHomeLocation != null) {
                    mHomeDistance = computeRelativeLocation(mHomeLocation.getLatitude(), mHomeLocation.getLongitude());
                }
                mSubject.onNext(mAircraftLocation);
            }
        }));

        addDisposable(widgetModel.homeLocationDataProcessor.toFlowable().subscribe(locationCoordinate2D -> {
            mHomeLocation = locationCoordinate2D;
            mHomeDistance = computeRelativeLocation(mHomeLocation.getLatitude(), mHomeLocation.getLongitude());
        }));

        addDisposable(Flowable.combineLatest(mSubject.hide().toFlowable(BackpressureStrategy.LATEST),
                widgetModel.airSenseSystemInformationProcessor.toFlowable().filter(info -> info.getWarningLevel() != AirSenseWarningLevel.UNKNOWN).distinctUntilChanged(),
                (locationCoordinate2D, airSenseSystemInformation) -> airSenseSystemInformation)
                .sample(300, TimeUnit.MILLISECONDS)
                .subscribe(this::airSenseSystemInformationHandler));

        // PinPoint，只显示选中的PIN点
//        addDisposable(Observable.combineLatest(mSubject, PinPointService.getInstance().getSelectPinPoints(),
//                (locationCoordinate3D, pinSelectData) -> {
//
//                    mSelectPinDistance = null;
//                    if (pinSelectData == PinSelectData.NONE) {
//                        return INVALID_BEARING_DISTANCE;
//                    }
//
//                    PinPoint pinPoint = pinSelectData.getPinPoint();
//                    Location3D location = pinPoint.getLocation();
//                    BearingDistance distance = computeRelativeLocation(location.latitude, location.longitude);
//                    if (distance.isValidate()) {
//                        mSelectPinDistance = distance;
//                        mSelectPinDistance.mColor = pinPoint.getColor();
//                        mSelectPinDistance.mName = pinPoint.getName();
//                    }
//                    return distance;
//                })
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribeOn(Schedulers.computation())
//                .subscribe(distance -> {
//                    mPinPointsDistances.clear();
//                    if (distance.isValidate()) {
//                        mPinPointsDistances.add(distance);
//                    }
//                    mHSIContainer.updateWidget();
//                }));

        // RNG Point
//        addDisposable(Observable.combineLatest(mSubject, PinPointService.getInstance().getRNGPoint(),
//                (locationCoordinate3D, location3D) -> {
//                    if (!location3D.isAvailable()) {
//                        return INVALID_BEARING_DISTANCE;
//                    }
//                    return computeRelativeLocation(location3D.latitude, location3D.longitude);
//                })
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribeOn(Schedulers.computation())
//                .subscribe(bearingDistance -> {
//                    mRngDistance = bearingDistance;
//                    mHSIContainer.updateWidget();
//                }));

        // Smart Track Point
//        addDisposable(Observable.combineLatest(mSubject, SmartTrackService.getInstance().getTrackTargetPostion(),
//                (locationCoordinate3D, location3D) -> {
//                    if (!location3D.isAvailable()) {
//                        return INVALID_BEARING_DISTANCE;
//                    }
//                    return computeRelativeLocation(location3D.latitude, location3D.longitude);
//                })
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribeOn(Schedulers.computation())
//                .subscribe(bearingDistance -> {
//                    mSmartTrackDistance = bearingDistance;
//                    mHSIContainer.updateWidget();
//                }));
    }

    @Override
    public void onStop() {
        if (mCompositeDisposable != null && !mCompositeDisposable.isDisposed()) {
            mCompositeDisposable.dispose();
            mCompositeDisposable = null;
        }
    }

    @Override
    public void draw(Canvas canvas, Paint paint, int compassSize) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        if (isAircraftLocationAvailable()) {
            drawHomePoint(canvas, paint, compassSize);
            drawPinPoint(canvas, paint, compassSize);
            drawRngPoint(canvas, paint, compassSize);
            drawSmartTrackPoint(canvas, paint, compassSize);
            //drawWayPoint(canvas, paint, compassSize);
            drawAdsbPoint(canvas, paint, compassSize);
        }
    }

    private void addDisposable(Disposable disposable) {
        if (disposable == null) {
            return;
        }
        if (mCompositeDisposable != null && !mCompositeDisposable.isDisposed()) {
            mCompositeDisposable.add(disposable);
        }
    }

    @NonNull
    private BearingDistance computeRelativeLocation(double latitude, double longitude) {
        if (!isAircraftLocationAvailable() || !LocationUtils.isValid(latitude, longitude)) {
            return INVALID_BEARING_DISTANCE;
        }
        Location.distanceBetween(mAircraftLocation.getLatitude(), mAircraftLocation.getLongitude(), latitude, longitude, mFloats);
        BearingDistance distance = new BearingDistance();
        distance.mDistance = mFloats[0];
        distance.mInitialBearing = mFloats[1];
        return distance;
    }

    private boolean isAircraftLocationAvailable() {
        return mAircraftLocation != null
                && LocationUtils.isValid(mAircraftLocation.getLatitude(), mAircraftLocation.getLongitude());
    }

    /**
     * 绘制出Pin Point点位置
     *
     * @param canvas
     * @param compassSize
     */
    private void drawPinPoint(Canvas canvas, Paint paint, int compassSize) {
        for (BearingDistance pinPointDistance : mPinPointsDistances) {
            if (pinPointDistance != null) {
                paint.setColor(pinPointDistance.mColor);
                mPinPointBitmap = mPinPointBitmap.extractAlpha();

                canvas.save();
                drawMarkerOnHsi(canvas, compassSize, pinPointDistance, false);
                drawMarker(canvas, mPinPointBitmap, mPinPointMarkerSize, mPinPointMarkerSize, 0, 0, paint, false);
                canvas.restore();
            }
        }

        if (mSelectPinDistance != null) {
            mPinPointBitmap = mPinPointBitmap.extractAlpha();
            drawMarkerIndicator(canvas, compassSize / 2, -compassSize / 2, (int) (compassSize + mHSIContainer.getDegreeIndicatorHeight()),
                    mPinPointBitmap, mPinPointMarkerSize, mPinPointMarkerSize, mMarkerIndicatorTextSize,
                    mSelectPinDistance.mColor, mSelectPinDistance, INDICATOR_ALIGN_LEFT, INDICATOR_ALIGN_BOTTOM, paint);
        }
    }

    /**
     * 绘制出Rng点位置
     *
     * @param canvas
     * @param compassSize
     */
    private void drawRngPoint(Canvas canvas, Paint paint, int compassSize) {
        // 仅RNG开启时才显示，优先显示SmartTrack
        if (mSmartTrackDistance != null && mSmartTrackDistance.isValidate()) {
            return;
        }
        canvas.save();
        drawMarkerOnHsi(canvas, compassSize, mRngDistance, false);
        drawMarker(canvas, mRngPointBitmap, mRngPointMarkerSize, mRngPointMarkerSize, 0, 0, paint, false);
        canvas.restore();

        drawMarkerIndicator(canvas, compassSize / 2, compassSize / 2, (int) -(mHSIContainer.getDegreeIndicatorHeight()),
                mRngPointBitmap, mRngPointMarkerSize, mRngPointMarkerSize, mMarkerIndicatorTextSize,
                mRngPointMarkerTextColor, mRngDistance, INDICATOR_ALIGN_RIGHT, INDICATOR_ALIGN_TOP, paint);
    }

    /**
     * 绘制出Smart Track点位置
     *
     * @param canvas
     * @param compassSize
     */
    private void drawSmartTrackPoint(Canvas canvas, Paint paint, int compassSize) {
        canvas.save();
        drawMarkerOnHsi(canvas, compassSize, mSmartTrackDistance, false);
        drawMarker(canvas, mSmartTrackPointBitmap, mSmartTrackPointMarkerSize, mSmartTrackPointMarkerSize, 0, 0, paint, false);
        canvas.restore();

        drawMarkerIndicator(canvas, compassSize / 2, compassSize / 2, (int) -(mHSIContainer.getDegreeIndicatorHeight()),
                mSmartTrackPointBitmap, mSmartTrackPointMarkerSize, mSmartTrackPointMarkerSize, mMarkerIndicatorTextSize,
                mSmartTrackPointMarkerTextColor, mSmartTrackDistance, INDICATOR_ALIGN_RIGHT, INDICATOR_ALIGN_TOP, paint);
    }

    /**
     * 绘制出Home点位置
     *
     * @param canvas
     * @param compassSize
     */
    private void drawHomePoint(Canvas canvas, Paint paint, int compassSize) {
        if (mHomeLocation == null || mHomeDistance == null) {
            return;
        }
        canvas.save();
        drawMarkerOnHsi(canvas, compassSize, mHomeDistance, false);
        drawMarker(canvas, mHomePointBitmap, mHomePointMarkerSize, mHomePointMarkerSize, 0, 0, paint, false);
        canvas.restore();
        drawMarkerIndicator(canvas, compassSize / 2, compassSize / 2, (int) (compassSize + mHSIContainer.getDegreeIndicatorHeight()),
                mHomePointBitmap, mHomePointMarkerSize, mHomePointMarkerSize, mMarkerIndicatorTextSize,
                mHomePointMarkerTextColor, mHomeDistance, INDICATOR_ALIGN_RIGHT, INDICATOR_ALIGN_BOTTOM, paint);
    }

    /**
     * 绘制Waypoint
     *
     * @param canvas
     * @param paint
     * @param compassSize
     */
//    private void drawWayPoint(Canvas canvas, Paint paint, int compassSize) {
//        LocationCoordinate2D location = getWayPointLocation();
//        if (location == null) {
//            return;
//        }
//        BearingDistance distance = computeRelativeLocation(location.getLatitude(), location.getLongitude());
//        MissionExecutePointInfo pointInfo = MissionManagerDelegate.INSTANCE.getExecutePointInfo();
//        if (pointInfo != null) {
//            distance.mName = String.format("%02d", pointInfo.getTargetIndex() + 1);
//        }
//        canvas.save();
//        drawMarkerOnHsi(canvas, compassSize, distance, false);
//        drawMarker(canvas, mWayPointBitmap, mWayPointMarkerSize, mWayPointMarkerSize, 0, 0, paint, false);
//        canvas.restore();
//
//        drawMarkerIndicator(canvas, compassSize / 2, -compassSize / 2, (int) -(mHSIContainer.getDegreeIndicatorHeight()),
//                mWayPointBitmap, mWayPointMarkerSize, mWayPointMarkerSize, mMarkerIndicatorTextSize,
//                mWayPointMarkerTextColor, distance, INDICATOR_ALIGN_LEFT, INDICATOR_ALIGN_TOP, paint);
//    }

    /**
     * 绘制Adsb Point
     */
    private void drawAdsbPoint(Canvas canvas, Paint paint, int compassSize) {
        //绘制黄色小飞机(长显)
        for (BearingDistance adsbDistance : mAdsbYellowDistances) {
            canvas.save();
            drawMarkerOnHsi(canvas, compassSize, adsbDistance, true);
            drawMarker(canvas, mAdsbYellowBitmap, mAdsbMarkerSize, mAdsbMarkerSize, 0, 0, paint, true);
            canvas.restore();
        }

        if (mAdsbTime % 2 < 1) {
            //绘制红色小飞机
            for (BearingDistance adsbDistance : mAdsbRedDistances) {
                canvas.save();
                drawMarkerOnHsi(canvas, compassSize, adsbDistance, true);
                drawMarker(canvas, mAdsbRedBitmap, mAdsbMarkerSize, mAdsbMarkerSize, 0, 0, paint, true);
                canvas.restore();
            }
        }
    }

    /**
     * 将图标朝上的方向转向HSI罗盘中心所需的度数（如将载人飞机图标飞机头方向朝向HSI罗盘中心）
     *
     * @param initialBearing 原位置与无人机夹角
     * @return 转向HSI罗盘中心所需的度数
     */
    private float transformDegreePointToAircraft(float initialBearing) {
        float degree = -mHSIContainer.getCurrentDegree() + initialBearing;
        if (degree > 180) {
            degree = degree - 360;
        }
        return 180 + degree;
    }

//    private LocationCoordinate2D getWayPointLocation() {
//        List<Location3D> points = MissionManagerDelegate.INSTANCE.getTargetAndNextPointList();
//        if (points.size() > 0) {
//            Location3D location = points.get(0);
//            return new LocationCoordinate2D(location.latitude, location.longitude);
//        }
//        return null;
//    }

    private void drawMarkerOnHsi(Canvas canvas, int compassSize, BearingDistance distance, boolean isPointToAircraft) {
        if (distance == null || !distance.isValidate()) {
            return;
        }

        canvas.translate(0, (float) compassSize / 2);
        canvas.rotate(-mHSIContainer.getCurrentDegree() + distance.mInitialBearing);
        float ratio = distance.mDistance / mHSIContainer.getVisibleDistanceInHsiInMeters();
        //超过16米或者adsb小飞机需要显示在罗盘边缘
        if (ratio > 1 || isPointToAircraft) {
            ratio = 1;
        }
        canvas.translate(0, -((float) compassSize / 2 - mHSIContainer.getCalibrationAreaWidth() - mHSIContainer.getCompassBitmapOffset()) * ratio);
        canvas.rotate(-(-mHSIContainer.getCurrentDegree() + distance.mInitialBearing));
        if (isPointToAircraft) {
            //图标朝上的方向指向HSI罗盘中心（如载人飞机图标飞机头方向）
            canvas.rotate(transformDegreePointToAircraft(distance.mInitialBearing));
        }
    }

    private void drawMarker(Canvas canvas, Bitmap marker, int markerWidth, int markerHeight,
                            float offsetMiddleX, float offsetMiddleY, Paint paint, boolean isPointToAircraft) {
        int flag = paint.getFlags();
        paint.setFlags(flag | Paint.FILTER_BITMAP_FLAG);
        canvas.save();
        //ADSB 飞机头位置需要在罗盘边缘
        canvas.translate(offsetMiddleX - (float) markerWidth / 2, offsetMiddleY - (isPointToAircraft ? 0 : (float) markerHeight / 2));
        HSIView.RECT.set(0, 0, marker.getWidth(), marker.getHeight());
        HSIView.RECT2.set(0, 0, markerWidth, markerHeight);
        canvas.drawBitmap(marker, HSIView.RECT, HSIView.RECT2, paint);
        canvas.restore();
        paint.setFlags(flag);
    }

    /**
     * 绘制HSI四个角的Pin/Home/ST/Rng数据
     */
    private void drawMarkerIndicator(Canvas canvas, int compassRadius, int translationX, int translationY,
                                     Bitmap marker, int markerWidth, int markerHeight, int textSize,
                                     int textColor, BearingDistance distance, @IndicatorAlign int horizontalAlign,
                                     @IndicatorAlign int verticalAlign, Paint paint) {
        if (distance == null || !distance.isValidate()) {
            return;
        }
        canvas.save();
        canvas.translate(translationX, translationY);
        drawMarkerIndicatorBackground(canvas, compassRadius, translationX, translationY, horizontalAlign, verticalAlign, distance.mName, textSize, paint);
        drawMarkerIndicatorContent(canvas, marker, markerWidth, markerHeight, textSize, textColor,
                distance, horizontalAlign, verticalAlign, paint);
        canvas.restore();
    }

    private void drawMarkerIndicatorBackground(Canvas canvas, int compassRadius, int translationX, int translationY,
                                               @IndicatorAlign int horizontalAlign,
                                               @IndicatorAlign int verticalAlign,
                                               String name, int textSize, Paint paint) {
        CIRCLE_PATH.reset();
        CIRCLE_PATH.addCircle((float) -translationX, (float) -translationY + compassRadius, (float) compassRadius + mMarkerIndicatorMarginToCompass * 2, Path.Direction.CW);

        PATH.reset();
        PATH.rLineTo(horizontalAlign == INDICATOR_ALIGN_LEFT ? mMarkerIndicatorWidth : -mMarkerIndicatorWidth, 0);
        PATH.rLineTo(0, verticalAlign == INDICATOR_ALIGN_TOP ? mMarkerIndicatorHeight : -mMarkerIndicatorHeight);
        PATH.rLineTo(horizontalAlign == INDICATOR_ALIGN_LEFT ? -mMarkerIndicatorWidth : mMarkerIndicatorWidth, 0);
        if (!TextUtils.isEmpty(name)) {
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(textSize);
            paint.getTextBounds(name, 0, name.length(), HSIView.RECT);
            int nWidth = HSIView.RECT.width() + mMarkerIndicatorWidth / 2;
            PATH.rLineTo(horizontalAlign == INDICATOR_ALIGN_LEFT ? -nWidth : nWidth, 0);
            PATH.rLineTo(0, verticalAlign == INDICATOR_ALIGN_TOP ? -mMarkerIndicatorHeight : mMarkerIndicatorHeight);
        }
        PATH.close();
        PATH.op(CIRCLE_PATH, Path.Op.DIFFERENCE);

        paint.setColor(mMarkerIndicatorBackgroundColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(PATH, paint);
        paint.setColor(mMarkerIndicatorStrokeColor);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(PATH, paint);
    }

    private void drawMarkerIndicatorContent(Canvas canvas, Bitmap marker, int markerWidth, int markerHeight, int textSize,
                                            int textColor, BearingDistance distance, @IndicatorAlign int horizontalAlign,
                                            @IndicatorAlign int verticalAlign, Paint paint) {
        canvas.save();
        String text = getText(distance.mDistance);
        paint.setColor(textColor);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(textSize);
        paint.getTextBounds(text, 0, text.length(), HSIView.RECT);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float baselineOffsetY = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom;
        float baseline = HSIView.RECT.centerY() + baselineOffsetY;
        canvas.translate(horizontalAlign == INDICATOR_ALIGN_LEFT ? mMarkerMarginHorizontal : -mMarkerMarginHorizontal,
                verticalAlign == INDICATOR_ALIGN_TOP ? mMarkerMarginVertical : -mMarkerMarginVertical);

        drawText(canvas, baseline, text, markerHeight, horizontalAlign, verticalAlign, paint);

        int flag = paint.getFlags();
        paint.setFlags(flag | Paint.FILTER_BITMAP_FLAG);
        HSIView.RECT.set(0, 0, marker.getWidth(), marker.getHeight());
        HSIView.RECT2.set(0, 0, horizontalAlign == INDICATOR_ALIGN_LEFT ? markerWidth : -markerWidth, markerHeight);
        canvas.drawBitmap(marker, HSIView.RECT, HSIView.RECT2, paint);
        paint.setFlags(flag);
        canvas.restore();

        // 画name
        String name = distance.mName;
        if (!TextUtils.isEmpty(name)) {
            paint.getTextBounds(name, 0, name.length(), HSIView.RECT);
            baseline = HSIView.RECT.centerY() + baselineOffsetY;
            float dx = (float) ((double) mMarkerIndicatorWidth / 4 + (double) HSIView.RECT.width());
            float dy = (float) ((double) mMarkerIndicatorHeight / 2 + (double) HSIView.RECT.height() / 2);
            canvas.translate(horizontalAlign == INDICATOR_ALIGN_LEFT ? -dx : dx, verticalAlign == INDICATOR_ALIGN_TOP ? dy : -dy + HSIView.RECT.height());
            // 加阴影
            int shadowColor = AndUtil.getResColor(ContextUtil.getContext(), R.color.uxsdk_black_47_percent);
            paint.setShadowLayer(3, 2, 2, shadowColor);
            canvas.drawText(name, 0, baseline, paint);
        }

    }

    private void drawText(Canvas canvas, float baseline, String text, int markerHeight, @IndicatorAlign int horizontalAlign,
                          @IndicatorAlign int verticalAlign, Paint paint) {
        if (horizontalAlign == INDICATOR_ALIGN_LEFT) {
            paint.setTextAlign(Paint.Align.LEFT);
            if (verticalAlign == INDICATOR_ALIGN_TOP) {
                canvas.translate(0, HSIView.RECT.height());
                canvas.drawText(text, 0, baseline, paint);
            } else {
                canvas.drawText(text, 0, baseline, paint);
                canvas.translate(0, -(HSIView.RECT.height() + markerHeight));
            }
        } else {
            paint.setTextAlign(Paint.Align.RIGHT);
            if (verticalAlign == INDICATOR_ALIGN_TOP) {
                canvas.translate(0, HSIView.RECT.height());
                canvas.drawText(text, 0, baseline, paint);
            } else {
                canvas.drawText(text, 0, baseline, paint);
                canvas.translate(0, -(HSIView.RECT.height() + markerHeight));
            }
        }
    }

    private String getText(float value) {
        String unit;
        if (!UnitUtils.isMetricUnits()) {
            value = UnitUtils.getValueFromMetricByLength(value, UnitUtils.UnitType.IMPERIAL);
            unit = UnitUtils.getUintStrByLength(UnitUtils.UnitType.IMPERIAL);
        } else {
            unit = UnitUtils.getUintStrByLength(UnitUtils.UnitType.METRIC);
            if (value > 1000) {
                unit = "k" + unit;
                value = value / 1000;
                return String.format(Locale.getDefault(), "%.01f%s", value, unit);
            }
        }
        return String.format(Locale.getDefault(), "%.0f%s", value, unit);
    }

    private static final int INDICATOR_ALIGN_LEFT = 0;
    private static final int INDICATOR_ALIGN_RIGHT = 1;
    private static final int INDICATOR_ALIGN_TOP = 2;
    private static final int INDICATOR_ALIGN_BOTTOM = 3;

    @IntDef({INDICATOR_ALIGN_LEFT, INDICATOR_ALIGN_RIGHT, INDICATOR_ALIGN_TOP, INDICATOR_ALIGN_BOTTOM})
    @interface IndicatorAlign {
    }

    private static class BearingDistance {
        private static final float INVALID_DISTANCE = -1;

        private float mDistance = INVALID_DISTANCE;
        private float mInitialBearing = 0.0f;
        private int mColor;
        private String mName;

        boolean isValidate() {
            return mDistance >= 0 && !Float.isNaN(mDistance);
        }
    }
}
