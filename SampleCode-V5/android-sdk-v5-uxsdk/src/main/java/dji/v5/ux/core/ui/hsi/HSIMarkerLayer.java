package dji.v5.ux.core.ui.hsi;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.SensorManager;
import android.location.Location;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Size;
import android.view.ViewDebug;

import java.util.concurrent.CopyOnWriteArrayList;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;



import dji.sdk.keyvalue.value.common.LocationCoordinate3D;
import dji.sdk.keyvalue.value.common.LocationCoordinate2D;
import dji.v5.utils.common.LocationUtil;
import dji.v5.ux.R;
import dji.v5.ux.core.ui.hsi.dashboard.FpvStrokeConfig;
import dji.v5.utils.common.AndUtil;
import dji.v5.common.utils.UnitUtils;
import dji.v5.ux.core.util.DrawUtils;
import dji.v5.ux.mapkit.core.utils.DJIGpsUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class HSIMarkerLayer implements HSIContract.HSILayer {

    @NonNull
    private static final Path PATH = new Path();

//    @NonNull
//    private static final int WAYPOINT_HEIGHT_THRESHOLD_FPV = 1;
//    private static final int WAYPOINT_HEIGHT_THRESHOLD_LIVEVIEW = 5;
    private static final int MAX_DISPLAY_DISTANCE_NUMBER = 999;
    private static final float FLOAT_THRESHOLD = 0.000001f;
    private static final int HSI_HOME_RC_MERGE_THRESHOLD = 5;

    @NonNull
    private HSIContract.HSIContainer mHSIContainer;

    @NonNull
    private final float[] mFloats = new float[2];

//    @NonNull
//    private static final BearingDistance INVALID_BEARING_DISTANCE = new BearingDistance();

    @NonNull
    private final Bitmap mHomePointBitmap;
    private final Bitmap mHomePointCornerBitmap;

    private final int mHomePointMarkerSize;

    @NonNull
    private final Bitmap mWayPointBitmap;
//    private final Bitmap mBreakBitmap;

//    private int mWaypointThrosholdInMeter = WAYPOINT_HEIGHT_THRESHOLD_FPV;

//    private Bitmap mWayPointDownBitmap;

//    private Bitmap mWayPointUpBitmap;

    @ViewDebug.ExportedProperty(category = HSIView.VIEW_DEBUG_CATEGORY_DJI)
    private final int mMarkerIndicatorTextSize;

    private float mMarkerIndicatorTextMaxWidth;

//    @NonNull
//    private Bitmap mPinPointBitmap;

    //    private final int mPinPointMarkerSize;

    @NonNull
    private final Bitmap mRngPointBitmap;

    @NonNull
    private final Bitmap mSmartTrackPointBitmap;
    private final Bitmap mRemoteControlPointBitmap;
    private final Bitmap mRemoteControlDirectionNormalBitmap;
    private final Bitmap mRemoteControlDirectionMergeBitmap;
//    private final boolean mShowSmartTrack;

    @NonNull
    private Bitmap mAdsbRedBitmap;

    @NonNull
    private Bitmap mAdsbYellowBitmap;

    private final int mAdsbMarkerSize;

    private final int mMarkerMarginVertical;

    private final int mMarkerMarginHorizontal;

    private int mMarkerNameInset;
    private float mMarkerNameTriangleWidth;
    private float mMarkerNameTriangleHeight;

    @NonNull
    private final BehaviorSubject<LocationCoordinate3D> mSubject = BehaviorSubject.create();

    @Nullable
    private LocationCoordinate3D mAircraftLocation;

    @Nullable
    private LocationCoordinate2D mHomeLocation;

    @NonNull
    private IndicatorInfo mHomeInfo = new IndicatorInfo();
    @NonNull
    private IndicatorInfo mRngInfo = new IndicatorInfo();

//    @NonNull
//    private final List<BearingDistance> mPinPointsDistances = new ArrayList<>();

    private IndicatorInfo mSelectPinInfo = new IndicatorInfo(true);

//    @Nullable
//    private BearingDistance mSmartTrackDistance;

    @NonNull
    private IndicatorInfo mSmartTrackInfo = new IndicatorInfo();

    @NonNull
    private IndicatorInfo mWayPointInfo = new IndicatorInfo();

    @NonNull
    private IndicatorInfo mRemoteControlInfo = new IndicatorInfo();
    private IndicatorInfo mRemoteControlDirectionInfo = new IndicatorInfo();

    HSICompassProcesser mCompassProcesser;

    @NonNull
    private final CopyOnWriteArrayList<BearingDistance> mAdsbYellowDistances = new CopyOnWriteArrayList<>();

    @NonNull
    private final CopyOnWriteArrayList<BearingDistance> mAdsbRedDistances = new CopyOnWriteArrayList<>();

    private Paint mStrokePaint = new Paint(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);

    @Nullable
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private Location mLastLocation;
    private float mRcDegree;
//    private boolean mAdsbShow;
//    private FlashTimer.Listener mFlashListener;
    FpvStrokeConfig mStrokeConfig;
    @NonNull
    private HSIWidgetModel widgetModel;

    public HSIMarkerLayer(@NonNull Context context, @Nullable AttributeSet attrs, @NonNull HSIContract.HSIContainer container,
                          HSIWidgetModel widgetModel) {
        mHSIContainer = container;
        mStrokeConfig = new FpvStrokeConfig(context);
        this.widgetModel = widgetModel;


        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.HSIView);
        mHomePointMarkerSize = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_home_point_marker_size, 0);
        mHomePointBitmap = DrawUtils.drawableRes2Bitmap(typedArray.getResourceId(R.styleable.HSIView_uxsdk_hsi_home_point_marker,
                R.drawable.uxsdk_fpv_pfd_hsi_home_point));
        mHomePointCornerBitmap = DrawUtils.drawableRes2Bitmap(typedArray.getResourceId(R.styleable.HSIView_uxsdk_hsi_home_point_marker,
                R.drawable.uxsdk_fpv_hsi_compass_home_point));
        mWayPointBitmap = DrawUtils.drawableRes2Bitmap(typedArray.getResourceId(R.styleable.HSIView_uxsdk_hsi_waypoint_marker,
                R.drawable.uxsdk_fpv_hsi_waypoint));
//        mBreakBitmap = DrawUtils.drawableRes2Bitmap(typedArray.getResourceId(R.styleable.HSIView_uxsdk_hsi_break_marker,
//                R.drawable.uxsdk_ic_fpv_hsi_ar_waypoint_break));

        //        mPinPointMarkerSize = typedArray.getDimensionPixelSize(R.styleable.HSIView_hsi_pin_point_marker_size,
        //                context.getResources().getDimensionPixelSize(R.dimen.dp_9_in_sw640));
        //        mPinPointBitmap = DrawUtils.drawableRes2Bitmap(typedArray.getResourceId(R.styleable.HSIView_hsi_pin_point_marker, R.drawable
        //        .fpv_hsi_pin_point_blue));

        int remoteControlRes = typedArray.getResourceId(R.styleable.HSIView_uxsdk_hsi_remote_control_point_marker, R.drawable.uxsdk_fpv_hsi_rc);
        mRemoteControlPointBitmap = DrawUtils.drawableRes2Bitmap(remoteControlRes);
//        mShowSmartTrack = typedArray.getBoolean(R.styleable.HSIView_uxsdk_hsi_smart_track_enable, false);
        mSmartTrackPointBitmap = DrawUtils.drawableRes2Bitmap(typedArray.getResourceId(R.styleable.HSIView_uxsdk_hsi_smart_track_point_marker,
                R.drawable.uxsdk_fpv_hsi_smart_track_point));

        mRngPointBitmap = DrawUtils.drawableRes2Bitmap(typedArray.getResourceId(R.styleable.HSIView_uxsdk_hsi_rng_point_marker,
                R.drawable.uxsdk_fpv_hsi_rng_point));

        mAdsbMarkerSize = context.getResources().getDimensionPixelSize(R.dimen.uxsdk_10_dp);
        mAdsbRedBitmap = DrawUtils.drawableRes2Bitmap(R.drawable.uxsdk_fpv_hsi_plane_danger);
        mAdsbYellowBitmap = DrawUtils.drawableRes2Bitmap(R.drawable.uxsdk_fpv_hsi_plane_caution);


        mMarkerMarginHorizontal = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_marker_margin_horizontal,
                context.getResources().getDimensionPixelSize(R.dimen.uxsdk_2_dp));
        mMarkerNameInset = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_marker_name_inset,
                context.getResources().getDimensionPixelSize(R.dimen.uxsdk_4_dp));
        mMarkerMarginVertical = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_marker_margin_vertical,
                0);
        mMarkerIndicatorTextSize = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_marker_indicator_text_size,
                context.getResources().getDimensionPixelSize(R.dimen.uxsdk_8_dp));
        mMarkerIndicatorTextMaxWidth = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_marker_indicator_text_max_width,
                context.getResources().getDimensionPixelSize(R.dimen.uxsdk_60_dp));
        typedArray.recycle();

        mStrokePaint.setStrokeWidth(mStrokeConfig.getStrokeBoldWidth());
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setColor(mStrokeConfig.getStrokeDeepColor());

        mMarkerNameTriangleWidth = context.getResources().getDimension(R.dimen.uxsdk_4_dp);
        mMarkerNameTriangleHeight = context.getResources().getDimension(R.dimen.uxsdk_6_dp);

        mSelectPinInfo.textSize = mMarkerIndicatorTextSize;
        mSelectPinInfo.horizontalAlign = INDICATOR_ALIGN_LEFT;
        mSelectPinInfo.verticalAlign = INDICATOR_ALIGN_BOTTOM;
        mSelectPinInfo.textOffset.set(0, (int) mHSIContainer.getDegreeIndicatorHeight());

        mHomeInfo.textSize = mMarkerIndicatorTextSize;
        mHomeInfo.horizontalAlign = INDICATOR_ALIGN_RIGHT;
        mHomeInfo.verticalAlign = INDICATOR_ALIGN_BOTTOM;
        mHomeInfo.marker = mHomePointBitmap;
        mHomeInfo.cornerMarker = mHomePointCornerBitmap;
        mHomeInfo.markSize = mHomePointMarkerSize;
        mHomeInfo.textOffset.set(0, (int) mHSIContainer.getDegreeIndicatorHeight());

        mRngInfo.textSize = mMarkerIndicatorTextSize;
        mRngInfo.horizontalAlign = INDICATOR_ALIGN_RIGHT;
        mRngInfo.verticalAlign = INDICATOR_ALIGN_TOP;
        mRngInfo.marker = mRngPointBitmap;
        mRngInfo.showInHsi = false;
        mRngInfo.textOffset.set(0, (int) -mHSIContainer.getDegreeIndicatorHeight());

        mSmartTrackInfo.textSize = mMarkerIndicatorTextSize;
        mSmartTrackInfo.horizontalAlign = INDICATOR_ALIGN_RIGHT;
        mSmartTrackInfo.verticalAlign = INDICATOR_ALIGN_TOP;
        mSmartTrackInfo.marker = mSmartTrackPointBitmap;
        mSmartTrackInfo.showInHsi = false;
        mSmartTrackInfo.textOffset.set(0, (int) -mHSIContainer.getDegreeIndicatorHeight());

        mWayPointInfo.textColor = context.getResources().getColor(R.color.uxsdk_blue_highlight);
        mWayPointInfo.textSize = mMarkerIndicatorTextSize;
        mWayPointInfo.horizontalAlign = INDICATOR_ALIGN_LEFT;
        mWayPointInfo.verticalAlign = INDICATOR_ALIGN_TOP;
        mWayPointInfo.marker = mWayPointBitmap;
        mWayPointInfo.textOffset.set(0, (int) -mHSIContainer.getDegreeIndicatorHeight());

        mRemoteControlInfo.textSize = mMarkerIndicatorTextSize;
        mRemoteControlInfo.marker = mRemoteControlPointBitmap;
        mRemoteControlInfo.showOnCorner = false;

        mRemoteControlDirectionNormalBitmap = DrawUtils.drawableRes2Bitmap(R.drawable.uxsdk_fpv_hsi_rc_arrow);
        mRemoteControlDirectionMergeBitmap = DrawUtils.drawableRes2Bitmap(R.drawable.uxsdk_fpv_hsi_home_arrow);
        mRemoteControlDirectionInfo.showOnCorner = false;

        mCompassProcesser = new HSICompassProcesser(context, this::onRcDegreeChanged);
    }

    public void onRcDegreeChanged(int strength, float degree) {
        mRemoteControlDirectionInfo.showInHsi = strength > SensorManager.SENSOR_STATUS_ACCURACY_LOW;
        mRcDegree = degree;
    }

    //    private void airSenseSystemInformationHandler(AirSenseSystemInformation info) {
    //
    //        mAdsbYellowDistances.clear();
    //        mAdsbRedDistances.clear();
    //        if (info.getWarningLevel().value() < AirSenseWarningLevel.LEVEL_2.value()) {
    //            if (mFlashListener != null) {
    //                FlashTimer.INSTANCE.removeListener(mFlashListener);
    //            }
    //            return;
    //        }
    //        List<AirSenseAirplaneState> airSenseAirplaneStates = info.getAirplaneStates();
    //        if (mFlashListener == null) {
    //            mFlashListener = show -> {
    //                mAdsbShow = show;
    //                mHSIContainer.updateWidget();
    //            };
    //        }
    //        //添加计时器，用于控制 adsb hsi小飞机闪烁时间
    //        FlashTimer.INSTANCE.addListener(mFlashListener);
    //        for (AirSenseAirplaneState airplaneState : airSenseAirplaneStates) {
    //            BearingDistance bearingDistance = computeRelativeLocation(airplaneState.getLatitude(), airplaneState.getLongitude());
    //            if (airplaneState.getWarningLevel() == AirSenseWarningLevel.LEVEL_2) {
    //                mAdsbYellowDistances.add(bearingDistance);
    //            } else if (airplaneState.getWarningLevel() == AirSenseWarningLevel.LEVEL_3
    //                    || airplaneState.getWarningLevel() == AirSenseWarningLevel.LEVEL_4) {
    //                mAdsbRedDistances.add(bearingDistance);
    //            }
    //        }
    //    }

    @Override
    public void onStart() {
        mCompositeDisposable = new CompositeDisposable();

        // HSI 中元素信息
        startListenAircraftLocation();
        startListenHomeLocation();
        //        startListenAirSenseSystem();
        //        startListenSelectPinPoints();
//        startListenRngPoints();
        startListenSmartTrackIfNeed();
        startListenRc();
        mCompassProcesser.start();
    }

    private void startListenAircraftLocation() {
        addDisposable(widgetModel.getAircraftLocationDataProcessor().toFlowable().subscribe(location -> {
            if (location != null) {
                mAircraftLocation = location;
                if (mHomeLocation != null) {
                    updateLocation(mHomeInfo.distance, mHomeLocation.getLatitude(), mHomeLocation.getLongitude());
                }
                mSubject.onNext(mAircraftLocation);
            }
        }));
    }

    private void startListenHomeLocation() {
        addDisposable(widgetModel.getHomeLocationDataProcessor().toFlowable().subscribe(locationCoordinate2D -> {
            mHomeLocation = locationCoordinate2D;
            if (mHomeLocation == null) {
                return;
            }
            if (mLastLocation == null) {
                mLastLocation = new Location("HomeLocation");
                mLastLocation.setLongitude(mHomeLocation.getLongitude());
                mLastLocation.setLatitude(mHomeLocation.getLatitude());
                if (!mRemoteControlInfo.distance.isValidate()) {
                    updateLocation(mRemoteControlInfo.distance, mLastLocation.getLatitude(), mLastLocation.getLongitude());
                }
            }
            mHomeInfo.distance.mLatitude = mHomeLocation.getLatitude();
            mHomeInfo.distance.mLongitude = mHomeLocation.getLongitude();
            updateLocation(mHomeInfo.distance, mHomeLocation.getLatitude(), mHomeLocation.getLongitude());
        }));
    }

//    private void startListenAirSenseSystem() {
        //        addDisposable(Observable.combineLatest(mSubject.hide(),
        //                Pilot2Repo.FlightController().AirSenseSystemInformation().observable(mHSIContainer.getView())
        //                        .filter(info -> info.getWarningLevel() != AirSenseWarningLevel.UNKNOWN)
        //                        .distinctUntilChanged(),
        //                ((locationCoordinate3D, airSenseSystemInformation) -> airSenseSystemInformation))
        //                .sample(300, TimeUnit.MILLISECONDS)
        //                .subscribe(this::airSenseSystemInformationHandler));
//    }

//    private void startListenSelectPinPoints() {
        // PinPoint，只显示选中的PIN点
        //        addDisposable(Observable.combineLatest(mSubject, PinPointService.getInstance().getSelectPinPoints(),
        //                (locationCoordinate3D, pinSelectData) -> {
        //
        //                    if (pinSelectData == PinSelectData.NONE) {
        //                        mSelectPinInfo.distance.setInvalid();
        //                        return mSelectPinInfo.distance;
        //                    }
        //
        //                    PinPoint pinPoint = pinSelectData.getPinPoint();
        //                    Location3D location = pinPoint.getLocation();
        //                    BearingDistance distance = computeRelativeLocation(location.latitude, location.longitude);
        //                    updateLocation(mSelectPinInfo.distance, location.latitude, location.longitude);
        //                    if (distance.isValidate()) {
        //                        if (pinPoint.getColor() != mSelectPinInfo.distance.mColor) {
        //                            mSelectPinInfo.distance.mColor = pinPoint.getColor();
        //                            mSelectPinInfo.textColor = pinPoint.getColor();
        //                        }
        //                        mSelectPinInfo.distance.mName = pinPoint.getName();
        //                    } else {
        //                        mSelectPinInfo.distance.setInvalid();
        //                    }
        //                    return mSelectPinInfo.distance;
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
//    }

//    private void startListenRngPoints() {
        // RNG Point
        //        addDisposable(Observable.combineLatest(mSubject, PinPointService.getInstance().getRNGPoint(),
        //                (locationCoordinate3D, location3D) -> {
        //                    if (!location3D.isAvailable()) {
        //                        mRngInfo.distance.setInvalid();
        //                    } else {
        //                        updateLocation(mRngInfo.distance, location3D.latitude, location3D.longitude);
        //                    }
        //                    return mRngInfo.distance;
        //                })
        //                .observeOn(AndroidSchedulers.mainThread())
        //                .subscribeOn(Schedulers.computation())
        //                .subscribe(bearingDistance -> mHSIContainer.updateWidget()));
//    }

    private void startListenRc() {
        mLastLocation = LocationUtil.getLastLocation();
        addDisposable(mSubject
                .doOnNext(myLocation -> {
                    Location location = mLastLocation;
                    if (location != null && DJIGpsUtils.isAvailable(location.getLatitude(), location.getLongitude())) {
                        updateLocation(mRemoteControlInfo.distance, location.getLatitude(), location.getLongitude());
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.computation())
                .subscribe()
        );

        mCompositeDisposable.add(widgetModel.getLocationDataProcessor().toFlowable().subscribe(location -> {
            mLastLocation = location;
            updateLocation(mRemoteControlInfo.distance, location.getLatitude(), location.getLongitude());
        }));
        //        LocationClient.getInstance().addLocationChangedListener(mOnLocationChangedListener);
    }

    private void startListenSmartTrackIfNeed() {
        // Smart Track Point
        //        if (mShowSmartTrack) {
        //            addDisposable(Observable.combineLatest(mSubject, SmartTrackService.getInstance().getTrackTargetPostion(),
        //                    (locationCoordinate3D, location3D) -> {
        //                        if (!location3D.isAvailable()) {
        //                            mSmartTrackInfo.distance.setInvalid();
        //                        } else {
        //                            updateLocation(mSmartTrackInfo.distance, location3D.latitude, location3D.longitude);
        //                        }
        //                        return mSmartTrackInfo.distance;
        //                    })
        //                    .observeOn(AndroidSchedulers.mainThread())
        //                    .subscribeOn(Schedulers.computation())
        //                    .subscribe(bearingDistance -> mHSIContainer.updateWidget()));
        //        }
    }

    @Override
    public void onStop() {
//        if (mFlashListener != null) {
//            FlashTimer.INSTANCE.removeListener(mFlashListener);
//        }
        mCompassProcesser.stop();
        if (mCompositeDisposable != null && !mCompositeDisposable.isDisposed()) {
            mCompositeDisposable.dispose();
            mCompositeDisposable = null;
        }
    }

    @Override
    public void enterFpvMode(boolean fpv) {
//        mWaypointThrosholdInMeter = fpv ? WAYPOINT_HEIGHT_THRESHOLD_FPV : WAYPOINT_HEIGHT_THRESHOLD_LIVEVIEW;
    }

    /**
     * 按顺序绘制 HSI 中图标</a>
     */
    @Override
    public void draw(Canvas canvas, Paint paint, int compassSize) {
        canvas.save();
        canvas.translate(0, compassSize / 2f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        // canvas 中心位于罗盘水平中间，垂直上方

        if (isAircraftLocationAvailable()) {
            //            drawPinPoint(canvas, paint, compassSize);
            //            drawRngPoint(canvas, paint, compassSize);
            //            drawSmartTrackPoint(canvas, paint, compassSize);
            //            drawWayPoint(canvas, paint, compassSize);
            boolean mergeRc = mergeRcAndHome();
            if (!mergeRc) {
                drawRemoteControlPoint(canvas, paint, compassSize);
            }
            // 绘制遥控器方向
            drawRemoteControlDirection(canvas, paint, compassSize, mergeRc);
            drawHomePoint(canvas, paint, compassSize);
            drawAdsbPoint(canvas, paint, compassSize);
        }
        canvas.restore();
    }

    private void drawRemoteControlDirection(Canvas canvas, Paint paint, int compassSize, boolean mergeRc) {
        IndicatorInfo draw = mRemoteControlDirectionInfo;
        IndicatorInfo from = mergeRc ? mHomeInfo : mRemoteControlInfo;
        draw.marker = mergeRc ? mRemoteControlDirectionMergeBitmap : mRemoteControlDirectionNormalBitmap;
        draw.distance.mDistance = from.distance.mDistance;
        draw.distance.mInitialBearing = from.distance.mInitialBearing;
        draw.markerRotate = mRcDegree - mHSIContainer.getCurrentDegree();

        drawIndicatorInfo(canvas, paint, compassSize, draw);
    }


    private boolean mergeRcAndHome() {
        if (mHomeInfo.distance.isValidate() && mRemoteControlInfo.distance.isValidate()) {
            float[] results = new float[1];
            Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(), mHomeInfo.distance.mLatitude,
                    mHomeInfo.distance.mLongitude, results);
            return results[0] < HSI_HOME_RC_MERGE_THRESHOLD;
        }

        return false;
    }

    private void addDisposable(Disposable disposable) {
        if (disposable == null) {
            return;
        }
        if (mCompositeDisposable != null && !mCompositeDisposable.isDisposed()) {
            mCompositeDisposable.add(disposable);
        }
    }

    private void updateLocation(BearingDistance target, double latitude, double longitude) {
        if (!isAircraftLocationAvailable() || !isAvailable(latitude, longitude)) {
            target.setInvalid();
            return;
        }
        if (mAircraftLocation == null) {
            return;
        }
        Location.distanceBetween(mAircraftLocation.getLatitude(), mAircraftLocation.getLongitude(), latitude, longitude, mFloats);
        target.mDistance = mFloats[0];
        target.mInitialBearing = mFloats[1];

    }
    //
    //    @NonNull
    //    private BearingDistance computeRelativeLocation(double latitude, double longitude) {
    //        if (!isAircraftLocationAvailable() || !isAvailable(latitude, longitude)) {
    //            return INVALID_BEARING_DISTANCE;
    //        }
    //        Location.distanceBetween(mAircraftLocation.getLatitude(), mAircraftLocation.getLongitude(), latitude, longitude, mFloats);
    //        BearingDistance distance = new BearingDistance();
    //        distance.mDistance = mFloats[0];
    //        distance.mInitialBearing = mFloats[1];
    //        return distance;
    //    }

    private boolean isAircraftLocationAvailable() {
        return mAircraftLocation != null
                && isAvailable(mAircraftLocation.getLatitude(), mAircraftLocation.getLongitude());
    }

    /**
     * 绘制出Pin Point点位置
     *
     * @param canvas
     * @param compassSize
     */
    //    private void drawPinPoint(Canvas canvas, Paint paint, int compassSize) {
    //        for (BearingDistance pinPointDistance : mPinPointsDistances) {
    //            if (pinPointDistance != null) {
    //                paint.setColor(pinPointDistance.mColor);
    //                mPinPointBitmap = getPinDrawableFromColor(pinPointDistance.mColor);
    //                canvas.save();
    //                drawMarkerOnHsi(canvas, compassSize, pinPointDistance, false);
    //                drawMarker(canvas, mPinPointBitmap, getScaleBitmapSize(mPinPointBitmap, mPinPointMarkerSize), new PointF(0, 0), paint, false);
    //                canvas.restore();
    //            }
    //        }
    //
    //        if (mSelectPinInfo.distance.isValidate()) {
    //            mPinPointBitmap = getPinDrawableFromColor(mSelectPinInfo.distance.mColor);
    //            drawMarkerIndicator(canvas,
    //                    compassSize,
    //                    mSelectPinInfo.textOffset,
    //                    mPinPointBitmap,
    //                    mSelectPinInfo,
    //                    paint);
    //        }
    //    }

    /**
     * 根据原始位图，返回缩放后的图标尺寸
     *
     * @param pinPointBitmap 图标原尺寸
     * @param markerSize     罗盘中图标大小，用来限制较长的一边
     */
    @NonNull
    private Size getScaleBitmapSize(Bitmap pinPointBitmap, int markerSize) {
        int width = pinPointBitmap.getWidth();
        int height = pinPointBitmap.getHeight();
        if (markerSize == 0) {
            return new Size(width, height);
        }
        if (height > width) {
            return new Size(Math.round((float) width * markerSize / height), markerSize);
        } else {
            return new Size(markerSize, Math.round((float) height * markerSize / width));
        }
    }

    //    @NonNull
    //    private Bitmap getPinDrawableFromColor(int color) {
    //        int resId;
    //        if (color == DrawingElementColor.BLUE.getColor()) {
    //            resId = R.drawable.fpv_hsi_pin_point_blue;
    //        } else if (color == DrawingElementColor.RED.getColor()) {
    //            resId = R.drawable.fpv_hsi_pin_point_red;
    //        } else if (color == DrawingElementColor.GREEN.getColor()) {
    //            resId = R.drawable.fpv_hsi_pin_point_green;
    //        } else if (color == DrawingElementColor.ORANGE.getColor()) {
    //            resId = R.drawable.fpv_hsi_pin_point_peach;
    //        } else if (color == DrawingElementColor.YELLOW.getColor()) {
    //            resId = R.drawable.fpv_hsi_pin_point_yellow;
    //        } else {
    //            resId = R.drawable.fpv_hsi_pin_point_blue;
    //        }
    //        return DrawUtils.drawableRes2Bitmap(resId);
    //    }

    /**
     * 绘制出Rng点位置
     *
     * @param canvas
     * @param compassSize
     */
    //    private void drawRngPoint(Canvas canvas, Paint paint, int compassSize) {
    //        // 仅RNG开启时才显示，优先显示SmartTrack
    //        if (mShowSmartTrack && mSmartTrackInfo.distance.isValidate()) {
    //            return;
    //        }
    //        drawIndicatorInfo(canvas, paint, compassSize, mRngInfo);
    //    }

    /**
     * 绘制出Smart Track点位置
     *
     * @param canvas
     * @param compassSize
     */
    //    private void drawSmartTrackPoint(Canvas canvas, Paint paint, int compassSize) {
    //        drawIndicatorInfo(canvas, paint, compassSize, mSmartTrackInfo);
    //    }

    /**
     * 绘制出遥控位置
     */
    private void drawRemoteControlPoint(Canvas canvas, Paint paint, int compassSize) {
        BearingDistance distance = mRemoteControlInfo.distance;
        if (distance == null || !distance.isValidate()) {
            return;
        }
        Bitmap bitmap = mRemoteControlPointBitmap;
        if (bitmap == null) {
            return;
        }
        drawIndicatorInfo(canvas, paint, compassSize, mRemoteControlInfo);
    }

    /**
     * 绘制出Home点位置
     *
     * @param canvas
     * @param compassSize
     */
    private void drawHomePoint(Canvas canvas, Paint paint, int compassSize) {
        IndicatorInfo indicatorInfo = mHomeInfo;
        drawIndicatorInfo(canvas, paint, compassSize, indicatorInfo);
    }

    private void drawIndicatorInfo(Canvas canvas, Paint paint, int compassSize, IndicatorInfo indicatorInfo) {
        if (!indicatorInfo.distance.isValidate()) {
            return;
        }
        if (indicatorInfo.showInHsi) {
            canvas.save();
            // 当前 canvas 中心位于罗盘中间上方
            drawMarkerOnHsi(canvas, compassSize, indicatorInfo.distance, false);
            drawMarker(canvas, indicatorInfo.markerRotate, indicatorInfo.marker, getScaleBitmapSize(indicatorInfo.marker, indicatorInfo.markSize),
                    indicatorInfo.markerOffset, paint, false);
            canvas.restore();
        }
        if (indicatorInfo.showOnCorner) {
            drawMarkerIndicator(canvas,
                    compassSize,
                    indicatorInfo.textOffset,
                    indicatorInfo.cornerMarker != null ? indicatorInfo.cornerMarker : indicatorInfo.marker,
                    indicatorInfo,
                    paint);
        }
    }

    /**
     * 绘制Waypoint
     *
     * @param canvas
     * @param paint
     * @param compassSize
     */
//    private void drawWayPoint(Canvas canvas, Paint paint, int compassSize) {
        //        List<MissionManagerDelegate.WaypointLocation> arPoints = MissionManagerDelegate.INSTANCE.getArPoints();
        //        if (arPoints.size() == 0) {
        //            return;
        //        }
        //        MissionManagerDelegate.WaypointLocation waypointLocation = arPoints.get(0);
        //        String name;
        //        int idx = waypointLocation.getIdx();
        //        if (idx <= 0) {
        //            name = null;
        //        } else if (idx > 9) {
        //            name = String.valueOf(idx);
        //        } else {
        //            name = "0" + idx;
        //        }
        //
        //        Bitmap marker;
        //        if (waypointLocation.getType() == MissionManagerDelegate.WaypointLocation.TYPE_BREAK) {
        //            marker = mBreakBitmap;
        //            name = null;
        //        } else {
        //            marker = mWayPointBitmap;
        //        }
        //        mWayPointInfo.distance.mName = name;
        //        mWayPointInfo.marker = marker;
        //        Location3D location3d = waypointLocation.getLoc();
        //        mWayPointInfo.cornerMarkerSecondary = getWaypointIndicator(location3d);
        //        updateLocation(mWayPointInfo.distance, location3d.getLatitude(), location3d.getLongitude());
        //        drawIndicatorInfo(canvas, paint, compassSize, mWayPointInfo);
//    }
    //
    //    private Bitmap getWaypointIndicator(Location3D location3d) {
    //        if (mAircraftLocation != null && isAircraftLocationAvailable()) {
    //            double heightDelta = location3d.getAltitude() - mAircraftLocation.getAltitude();
    //            if (heightDelta > mWaypointThrosholdInMeter) {
    //                if (mWayPointUpBitmap == null) {
    //                    mWayPointUpBitmap = DrawUtils.drawableRes2Bitmap(R.drawable.fpv_hsi_waypoint_up);
    //                }
    //                return mWayPointUpBitmap;
    //            } else if (heightDelta < -mWaypointThrosholdInMeter) {
    //                if (mWayPointDownBitmap == null) {
    //                    mWayPointDownBitmap = DrawUtils.drawableRes2Bitmap(R.drawable.fpv_hsi_waypoint_down);
    //                }
    //                return mWayPointDownBitmap;
    //            } else {
    //                return null;
    //            }
    //        }
    //        return null;
    //    }

    /**
     * 绘制Adsb Point
     */
    private void drawAdsbPoint(Canvas canvas, Paint paint, int compassSize) {
        //绘制黄色小飞机(长显)
        for (BearingDistance adsbDistance : mAdsbYellowDistances) {
            canvas.save();
            drawMarkerOnHsi(canvas, compassSize, adsbDistance, true);
            drawMarker(canvas, mAdsbYellowBitmap, new Size(mAdsbMarkerSize, mAdsbMarkerSize), new PointF(0, 0), paint, true);
            canvas.restore();
        }

//        if (mAdsbShow) {
//            //绘制红色小飞机
//            for (BearingDistance adsbDistance : mAdsbRedDistances) {
//                canvas.save();
//                drawMarkerOnHsi(canvas, compassSize, adsbDistance, true);
//                drawMarker(canvas, mAdsbRedBitmap, new Size(mAdsbMarkerSize, mAdsbMarkerSize), new PointF(0, 0), paint, true);
//                canvas.restore();
//            }
//        }
    }

    /**
     * 将图标朝上的方向转向HSI罗盘中心所需的度数（如将载人飞机图标飞机头方向朝向HSI罗盘中心）
     *
     * @param initialBearing 原位置与无人机夹角
     * @return 转向HSI罗盘中心所需的度数
     */
    private float transformDegreePointToDrone(float initialBearing) {
        float degree = -mHSIContainer.getCurrentDegree() + initialBearing;
        if (degree > 180) {
            degree = degree - 360;
        }
        return 180 + degree;
    }

    private void drawMarkerOnHsi(Canvas canvas, int compassSize, BearingDistance distance, boolean isPointToDrone) {
        if (distance == null || !distance.isValidate()) {
            return;
        }

        canvas.rotate(-mHSIContainer.getCurrentDegree() + distance.mInitialBearing);
        float ratio = distance.mDistance / mHSIContainer.getVisibleDistanceInHsiInMeters();
        //超过16米或者adsb小飞机需要显示在罗盘边缘
        if (ratio > 1 || isPointToDrone) {
            ratio = 1;
        }
        canvas.translate(0, -((float) compassSize / 2 - mHSIContainer.getCalibrationAreaWidth() - mHSIContainer.getCompassBitmapOffset()) * ratio);
        canvas.rotate(-(-mHSIContainer.getCurrentDegree() + distance.mInitialBearing));
        if (isPointToDrone) {
            //图标朝上的方向指向HSI罗盘中心（如载人飞机图标飞机头方向）
            canvas.rotate(transformDegreePointToDrone(distance.mInitialBearing));
        }
    }

    private void drawMarker(Canvas canvas, Bitmap marker, Size size, PointF position, Paint paint, boolean isPointToDrone) {
        drawMarker(canvas, 0, marker, size, position, paint, isPointToDrone);
    }

    /**
     * 绘制 HSI 中图标
     *
     * @param canvas         画布
     * @param marker         图片信息
     * @param size           绘制大小
     * @param position       图标绘制起始点偏移
     * @param isPointToDrone 是否为 ADSB
     */
    private void drawMarker(Canvas canvas, float rotate, Bitmap marker, Size size, PointF position, Paint paint, boolean isPointToDrone) {
        int flag = paint.getFlags();
        paint.setFlags(flag | Paint.FILTER_BITMAP_FLAG);
        canvas.save();
        //ADSB 飞机头位置需要在罗盘边缘
        int offsetX = size.getWidth() / 2;
        int offsetY = isPointToDrone ? 0 : size.getHeight() / 2;
        canvas.translate(position.x, position.y);
        HSIView.RECT.set(0, 0, marker.getWidth(), marker.getHeight());
        HSIView.RECT2.set(-offsetX, -offsetY, -offsetX + size.getWidth(), -offsetY + size.getHeight());
        if (needRotateCanvas(rotate)) {
            canvas.rotate(rotate);
        }
        canvas.drawBitmap(marker, HSIView.RECT, HSIView.RECT2, paint);
        canvas.restore();
        paint.setFlags(flag);
    }

    private boolean needRotateCanvas(float rotate) {
        return rotate > FLOAT_THRESHOLD || rotate < -FLOAT_THRESHOLD;
    }

    /**
     * 绘制HSI四个角的Pin/Home/ST/Rng数据
     */
    private void drawMarkerIndicator(Canvas canvas, int compassSize, Point position,
                                     Bitmap marker, IndicatorInfo indicatorInfo, Paint paint) {
        if (indicatorInfo.distance == null || !indicatorInfo.distance.isValidate()) {
            return;
        }
        canvas.save();
        float radius = compassSize / 2f;
        canvas.translate(
                position.x + (indicatorInfo.horizontalAlign == INDICATOR_ALIGN_LEFT ? -radius : radius),
                position.y + (indicatorInfo.verticalAlign == INDICATOR_ALIGN_TOP ? -radius : radius)
        );
        drawMarkerIndicatorContent(canvas, marker, indicatorInfo, paint);
        canvas.restore();
    }

    private void drawMarkerIndicatorContent(Canvas canvas,
                                            Bitmap marker,
                                            IndicatorInfo indicatorInfo,
                                            Paint paint) {
        int textSize = indicatorInfo.textSize;
        BearingDistance distance = indicatorInfo.distance;
        canvas.save();
        String text = getText(distance.mDistance);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(textSize);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        int horizontalAlign = indicatorInfo.horizontalAlign;
        int verticalAlign = indicatorInfo.verticalAlign;
        canvas.translate(horizontalAlign == INDICATOR_ALIGN_LEFT ? -mMarkerMarginHorizontal : mMarkerMarginHorizontal,
                verticalAlign == INDICATOR_ALIGN_TOP ? mMarkerMarginVertical : -mMarkerMarginVertical);

        // 绘制文字
        paint.setTextAlign(horizontalAlign == INDICATOR_ALIGN_LEFT ? Paint.Align.LEFT : Paint.Align.RIGHT);
        if (verticalAlign == INDICATOR_ALIGN_TOP) {
            canvas.translate(0, -fontMetrics.ascent);
            drawTextWithStroke(canvas, 0, 0, text, paint);
            canvas.translate(0, fontMetrics.descent);
        } else {
            canvas.translate(0, -fontMetrics.descent);
            drawTextWithStroke(canvas, 0, 0, text, paint);
            canvas.translate(0, fontMetrics.ascent - marker.getHeight());
        }

        int flag = paint.getFlags();
        paint.setFlags(flag | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(marker, horizontalAlign == INDICATOR_ALIGN_LEFT ? 0 : -marker.getWidth(), 0, paint);
        Bitmap markerSecondary = indicatorInfo.cornerMarkerSecondary;
        if (markerSecondary != null) {
            canvas.drawBitmap(markerSecondary, horizontalAlign == INDICATOR_ALIGN_LEFT ? marker.getWidth() :
                    -marker.getWidth() - markerSecondary.getWidth(), (marker.getHeight() - markerSecondary.getHeight()) / 2f, paint);
        }
        paint.setFlags(flag);
        canvas.restore();

        // 画name
        float offsetY = fontMetrics.descent - fontMetrics.ascent + marker.getHeight() / 2f - mMarkerMarginVertical;
        drawName(canvas, indicatorInfo, paint, distance, offsetY);
    }

    private void drawName(Canvas canvas, IndicatorInfo indicatorInfo, Paint paint, BearingDistance distance, float centerY) {
        String name = distance.mName;
        if (!TextUtils.isEmpty(name)) {
            paint.setTextAlign(Paint.Align.LEFT);
            TextPaint textPaint = new TextPaint(paint);
            int firstLineCount = textPaint.breakText(name, true, mMarkerIndicatorTextMaxWidth, null);
            String firstLine = name.substring(0, firstLineCount);
            String secondLine = null;
            if (firstLineCount != name.length()) {
                secondLine =
                        TextUtils.ellipsize(name.substring(firstLineCount), textPaint, mMarkerIndicatorTextMaxWidth, TextUtils.TruncateAt.END).toString();
            }
            boolean multiLine = secondLine != null;
            Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
            float height = fontMetrics.descent - fontMetrics.ascent;
            float baselineOffset = height / 2 - fontMetrics.descent;
            float textHeight;
            float textWidth;
            Rect rect = HSIView.RECT;
            if (multiLine) {
                textHeight = height * 2;
                rect.set(0, 0, Math.round(mMarkerIndicatorTextMaxWidth), Math.round(textHeight));
                textWidth = mMarkerIndicatorTextMaxWidth;
            } else {
                textHeight = height;
                textWidth = paint.measureText(name);
                rect.set(0, 0, Math.round(textWidth), Math.round(textHeight));
            }
            int textColor = indicatorInfo.textColor;
            paint.setColor(textColor);
            Rect bgRect = HSIView.RECT2;
            bgRect.set(rect);
            bgRect.inset(-mMarkerNameInset, -mMarkerNameInset);

            float bgOffset = getNameBgOffset(distance);
            int horizontalAlign = indicatorInfo.horizontalAlign;
            float dx = getNameOffsetX(textWidth, bgOffset, horizontalAlign);
            float dy = getNameOffsetY(indicatorInfo, centerY, textHeight);
            canvas.translate(dx, dy);

            if (distance.mDrawBg) {
                drawNameBg(canvas, bgRect, paint, horizontalAlign == INDICATOR_ALIGN_LEFT);
            }
            float baseline;
            if (multiLine) {
                baseline = (rect.top * 3 + rect.bottom) / 4f + baselineOffset;
                drawTextWithStroke(canvas, 0, baseline, firstLine, paint);
                baseline += rect.height() / 2f;
                drawTextWithStroke(canvas, 0, baseline, secondLine, paint);
            } else {
                baseline = rect.centerY() + baselineOffset;
                drawTextWithStroke(canvas, 0, baseline, name, paint);
            }
        }
    }

    private float getNameBgOffset(BearingDistance distance) {
        return distance.mDrawBg ? mMarkerNameTriangleWidth + mMarkerNameInset + mMarkerMarginHorizontal : 0;
    }

    private float getNameOffsetX(float textWidth, float bgOffset, int horizontalAlign) {
        float dx;
        if (horizontalAlign == INDICATOR_ALIGN_LEFT) {
            dx = -mMarkerMarginHorizontal - textWidth - bgOffset;
        } else {
            dx = mMarkerMarginHorizontal + bgOffset;
        }
        return dx;
    }

    private float getNameOffsetY(IndicatorInfo indicatorInfo, float centerY, float textHeight) {
        float dy;
        int verticalAlign = indicatorInfo.verticalAlign;
        if (verticalAlign == INDICATOR_ALIGN_TOP) {
            dy = Math.max(centerY - textHeight / 2f, 0);
        } else {
            dy = Math.min(-centerY - textHeight / 2f, -textHeight);
        }
        return dy;
    }

    private void drawNameBg(Canvas canvas, Rect bgRect, Paint paint, boolean alignLeft) {
        Paint.Style style = paint.getStyle();
        int color = paint.getColor();
        // 绘制圆角矩形
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(AndUtil.getResColor(R.color.uxsdk_black_60_percent));
        canvas.drawRoundRect(bgRect.left, bgRect.top, bgRect.right, bgRect.bottom, 5, 5, paint);

        // 绘制三角形
        Path path = PATH;
        path.reset();
        float offset = mMarkerNameTriangleHeight / 2f;
        int lineX = alignLeft ? bgRect.right : bgRect.left;
        float pointX = alignLeft ? bgRect.right + mMarkerNameTriangleWidth : bgRect.left - mMarkerNameTriangleWidth;
        path.moveTo(lineX, bgRect.centerY() + offset);
        path.lineTo(pointX, bgRect.centerY());
        path.lineTo(lineX, bgRect.centerY() - offset);
        path.close();
        canvas.drawPath(path, paint);

        paint.setStyle(style);
        paint.setColor(color);
    }

    private void drawTextWithStroke(Canvas canvas, int x, float baseline, String text, Paint paint) {
        mStrokePaint.setTypeface(paint.getTypeface());
        mStrokePaint.setTextSize(paint.getTextSize());
        mStrokePaint.setTextAlign(paint.getTextAlign());
        canvas.drawText(text, x, baseline, mStrokePaint);
        canvas.drawText(text, x, baseline, paint);
    }

    private String getText(float value) {
        String unit;
        String extra = "";
        boolean hasDecimals = false;
        if (!UnitUtils.isMetricUnits()) {
            value = UnitUtils.getValueFromMetricByLength(value, UnitUtils.UnitType.IMPERIAL);
            unit = UnitUtils.getUintStrByLength(UnitUtils.UnitType.IMPERIAL);
            // 加 0.5f 是为了避免出现 999.5m 出现 1000m 这样的场景
            if (UnitUtils.moreThanMile(value + 0.5f)) {
                unit = "mi";
                value = UnitUtils.footToMile(value);
                hasDecimals = value < 99.5f;
                if (value > MAX_DISPLAY_DISTANCE_NUMBER) {
                    extra = "+";
                    value = MAX_DISPLAY_DISTANCE_NUMBER;
                }
            }
        } else {
            unit = UnitUtils.getUintStrByLength(UnitUtils.UnitType.METRIC);
            if (value + 0.5f >= 1000) {
                value /= 1000;
                extra = "k";
                // 不能直接使用 100，99.95 显示一位小数是 100.0 共 5 字符
                hasDecimals = value < 99.95f;
                if (value > MAX_DISPLAY_DISTANCE_NUMBER) {
                    value = MAX_DISPLAY_DISTANCE_NUMBER;
                    extra = "+k";
                }
            }
        }

        /**
         * 原来代码format字符串会比较耗CPU时间
         * String format = hasDecimals ? "%.1f%s%s" : "%.0f%s%s";
         * return String.format(Locale.getDefault(), format, value, extra, unit);
         */
        if (hasDecimals) {
            return "" + (float) Math.round(value * 10) / 10 + extra + unit;
        } else {
            return "" + Math.round(value) + extra + unit;
        }
    }

    private static final int INDICATOR_ALIGN_LEFT = 0;
    private static final int INDICATOR_ALIGN_RIGHT = 1;
    private static final int INDICATOR_ALIGN_TOP = 2;
    private static final int INDICATOR_ALIGN_BOTTOM = 3;

    @IntDef({INDICATOR_ALIGN_LEFT, INDICATOR_ALIGN_RIGHT, INDICATOR_ALIGN_TOP, INDICATOR_ALIGN_BOTTOM})
    @interface IndicatorAlign {
    }

    /**
     * HSI 绘制元素的指示信息
     */
    private static class IndicatorInfo {
        /**
         * 文字颜色，航点和 Pin 点
         */
        int textColor;
        /**
         * 文字大小
         */
        int textSize;
        /**
         * 距离和朝向信息
         */
        BearingDistance distance;
        /**
         * HSI 中图标大小
         */
        int markSize;

        /**
         * HSI 中图标绘制时画布偏移量，相对中心
         */
        PointF markerOffset = new PointF();

        /**
         * HSI 边上文字偏移量，罗盘角
         */
        Point textOffset = new Point();

        /**
         * 元素图标
         */
        Bitmap marker;

        /**
         * 罗盘周边使用图标，为空默认使用元素图标
         */
        Bitmap cornerMarker;

        /**
         * 紧挨罗盘周边图标，航点相对位置指示
         */
        Bitmap cornerMarkerSecondary;

        /**
         * 图标旋转角度
         */
        float markerRotate;

        /**
         * 显示在 HSI 里面
         */
        boolean showInHsi = true;
        /**
         * 显示在 HSI 边上
         */
        boolean showOnCorner = true;

        /**
         * 角落绘制的水平朝向
         */
        @IndicatorAlign
        int horizontalAlign;
        /**
         * 角落绘制的垂直朝向
         */
        @IndicatorAlign
        int verticalAlign;

        private IndicatorInfo() {
            this(false);
        }

        private IndicatorInfo(boolean drawNameBg) {
            this.distance = new BearingDistance();
            distance.mDrawBg = drawNameBg;
        }
    }

    private static class BearingDistance {
        private static final float INVALID_DISTANCE = -1;

        /**
         * 距离
         */
        private float mDistance = INVALID_DISTANCE;
        /**
         * 朝向
         */
        private float mInitialBearing = 0.0f;

        /**
         * 纬度
         * 经纬度信息在计算 Home 点和遥控距离时需要使用
         */
        private double mLatitude;
        /**
         * 经度
         */
        private double mLongitude;

        /**
         * 名称背景
         */
        private boolean mDrawBg = false;

        /**
         * 名称
         */
        private String mName;

        boolean isValidate() {
            return mDistance >= 0 && !Float.isNaN(mDistance);
        }

        void setInvalid() {
            mDistance = INVALID_DISTANCE;
        }
    }



    /**
     * 实测发现飞机刚获取GPS信号时坐标值有可能在 1E-8 ~ 1E-7 之间
     */
    public boolean isAvailable(double latitude, double longitude) {
        return Math.abs(latitude) > 1.0E-7D
                && Math.abs(longitude) > 1.0E-7D
                && Math.abs(latitude) <= 90.0D
                && Math.abs(longitude) <= 180.0D;
    }

}
