package dji.v5.ux.core.ui.hsi.dashboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import dji.sdk.keyvalue.utils.ProductUtil;
import dji.v5.manager.aircraft.perception.data.ObstacleAvoidanceType;
import dji.v5.manager.aircraft.perception.data.ObstacleData;
import dji.v5.manager.aircraft.perception.radar.RadarInformation;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;
import dji.v5.common.utils.UnitUtils;
import dji.v5.ux.core.ui.hsi.config.IOmniAbility;
import dji.v5.utils.common.AndUtil;
import dji.v5.ux.core.util.DrawUtils;
import dji.v5.ux.core.util.FontUtils;
import dji.v5.ux.core.widget.hsi.AttitudeDisplayModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class AttitudeDashBoard extends ScrollableAttributeDashBoard {


    private static final String TAG = "AttitudeDashBoard";

    private static final int UPWARD = -1;
    private static final int DOWNWARD = 1;

    private static final int PREDICT_TIME_IN_SECONDS = 6;

    /**
     * 障碍物最远感知距离
     */
    private int mUpMaxPerceptionDistanceInMeter;
    private int mDownMaxPerceptionDistanceInMeter;
    private static final float SPEED_THRESHOLD = 0.001f;
    private static final int PFD_BARRIER_OFF_TEXT_DISTANCE = 20;

    private final int mAvoidanceMaxHeight;
    private final int mBarrierDistanceTextSize;
    private final int mBarrierIndicatorColor;
    private final int mAvoidanceIndicatorColor;
    /**
     * 避障条宽度，因描边存在不同粗细，为保证显示宽度一致，此宽度包含描边
     */
    private final int mAvoidanceIndicatorWidth;
    private final int mAvoidanceIndicatorStrokeWidth;
    private final int mDistancePredictWidth;
    private final int mReturnToHomeIndicatorWidth;

    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();


    /**
     * 用户设置的上视障碍物避障距离
     */
    private float mUserSetUpBarrierAvoidanceDistance;
    /**
     * 用户设置的上视障碍物告警距离
     */
    private float mUserSetUpBarrierWarnDistance;

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

    //    /**
    //     * 返航高度
    //     */
    //    private int mReturnToHomeHeight = Integer.MAX_VALUE;

    //    /**
    //     * 飞控设置中的限飞高度，会处理限飞区，但是不包含解禁证书
    //     */
    //    private int mLimitHeight = Integer.MAX_VALUE;

    /**
     * 视觉感知避障开关
     */
    private boolean mUpwardVisualEnable;

    private boolean mShowDownwardVisualPerceptionInfo;

    /**
     * 雷达感知避障开关
     */
    private boolean mUpwardRadarEnable;
    private RadarInformation radarInformation = new RadarInformation();

    private float mUpwardRadarDistance;

    private Bitmap mBarrierUp;
    private Bitmap mBarrierDown;

    private float mBarrierOriginLineWidth;
    private float mBarrierOriginLineLength;

    /**
     * 垂直避障条文字与坐标轴间距
     */
    private float mBarrierTextMargin;

    private IBarrierDistanceStrategy mBarrierDistanceStrategy;
    //    private float mTextExtraPadding;
    /**
     * 上下两个方向的视觉感知是否正常工作
     */
    @NonNull
    private final boolean[] mVisionPerceptionWorkingState = new boolean[]{false, false};


    private AttitudeDisplayModel mWidgetModel;
    private ObstacleData mPerceptionObstacleData = new ObstacleData();


    public void setModel(AttitudeDisplayModel model) {
        mWidgetModel = model;
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
                getResources().getDimensionPixelSize(R.dimen.uxsdk_38_dp));
        mBarrierDistanceTextSize = typedArray.getDimensionPixelSize(R.styleable.AttitudeDashBoard_uxsdk_barrier_distance_text_size,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_7_dp));
        mAvoidanceIndicatorWidth = typedArray.getDimensionPixelSize(R.styleable.AttitudeDashBoard_uxsdk_avoidance_indicator_width,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_3_dp));
        mAvoidanceIndicatorStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.AttitudeDashBoard_uxsdk_avoidance_indicator_stroke_width,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_0_5_dp));
        mDistancePredictWidth = typedArray.getDimensionPixelSize(R.styleable.AttitudeDashBoard_uxsdk_distance_predict_line_width,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_2_dp));
        mReturnToHomeIndicatorWidth = typedArray.getDimensionPixelSize(R.styleable.AttitudeDashBoard_uxsdk_return_to_home_indicator_width,
                getResources().getDimensionPixelSize(R.dimen.uxsdk_22_dp));

        mBarrierIndicatorColor = typedArray.getColor(R.styleable.AttitudeDashBoard_uxsdk_barrier_indicator_color,
                getResources().getColor(R.color.uxsdk_pfd_barrier_color));
        Drawable up = typedArray.getDrawable(R.styleable.AttitudeDashBoard_uxsdk_barrier_drawable_up);
        if (up != null) {
            mBarrierUp = DrawUtils.drawable2Bitmap(up);
        }
        Drawable down = typedArray.getDrawable(R.styleable.AttitudeDashBoard_uxsdk_barrier_drawable_down);
        if (up != null) {
            mBarrierDown = DrawUtils.drawable2Bitmap(down);
        }
        mAvoidanceIndicatorColor = typedArray.getColor(R.styleable.AttitudeDashBoard_uxsdk_avoidance_indicator_color,
                getResources().getColor(R.color.uxsdk_pfd_avoidance_color));
        mUpwardRadarDistance = typedArray.getInt(R.styleable.AttitudeDashBoard_uxsdk_barrier_distance, Integer.MAX_VALUE);
        mBarrierOriginLineWidth = getResources().getDimension(R.dimen.uxsdk_1_dp);
        mBarrierOriginLineLength = getResources().getDimension(R.dimen.uxsdk_6_dp);
        typedArray.recycle();

        if (!isInEditMode()){
            mUpMaxPerceptionDistanceInMeter = IOmniAbility.Companion.getCurrent().getUpDetectionCapability();
            mDownMaxPerceptionDistanceInMeter = IOmniAbility.Companion.getCurrent().getDownDetectionCapability();
        }

        mBarrierTextMargin = getResources().getDimension(R.dimen.uxsdk_1_dp);
        //TODO  临时新增日志。这里很奇怪，获取到的mBarrierDistanceStrategy=HsiBarrierDistanceStrategy，因为拿到的isShowFramework=false
        Log.d(TAG, "isShowFramework=" + isShowFramework());
        mBarrierDistanceStrategy = isShowFramework() ? new PfdBarrierDistanceStrategy() : new HsiBarrierDistanceStrategy();
        //        mTextExtraPadding = getResources().getDimensionPixelSize(R.dimen.uxsdk_2_dp);
        mWaypointIcon = ContextCompat.getDrawable(getContext(), R.drawable.uxsdk_fpv_pfd_waypoint_right);

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isInEditMode()) {
            return;
        }

        //视觉开关信息
        mCompositeDisposable.add(mWidgetModel.getPerceptionInfoProcessor().toFlowable().subscribe(info -> {
            boolean isBypass = info.getObstacleAvoidanceType() == ObstacleAvoidanceType.BYPASS;
            //用户设置的上视避障开关
            mUpwardVisualEnable = info.isUpwardObstacleAvoidanceEnabled() || isBypass;
            //用户设置的上视障碍物避障距离
            mUserSetUpBarrierAvoidanceDistance = (float) info.getUpwardObstacleAvoidanceBrakingDistance();
            //用户设置的上视障碍物告警距离
            mUserSetUpBarrierWarnDistance = (float) info.getUpwardObstacleAvoidanceWarningDistance();

            //用户设置的下视避障开关
            mShowDownwardVisualPerceptionInfo = info.isDownwardObstacleAvoidanceEnabled() || isBypass;
            //用户设置的下视障碍物避障距离
            mUserSetDownBarrierAvoidanceDistance = (float) info.getDownwardObstacleAvoidanceBrakingDistance();
            //用户设置的下视障碍物告警距离
            mUserSetDownBarrierWarnDistance = (float) info.getDownwardObstacleAvoidanceWarningDistance() / 10f;

            //上下避障工作状态
            mVisionPerceptionWorkingState[0] = info.getUpwardObstacleAvoidanceWorking();
            mVisionPerceptionWorkingState[1] = info.getDownwardObstacleAvoidanceWorking();

            updateWidget();
        }));

        //视觉避数据信息
        mCompositeDisposable.add(mWidgetModel.getPerceptionObstacleDataProcessor().toFlowable().subscribe(data -> {
            mPerceptionObstacleData = data;
            updateWidget();
        }));

        //雷达开关信息
        mCompositeDisposable.add(mWidgetModel.getRadarInfoProcessor().toFlowable().subscribe(info -> {
            mUpwardRadarEnable = info.isUpwardObstacleAvoidanceEnabled();
            radarInformation = info;
            updateWidget();
        }));

        //雷达避数据信息
        mCompositeDisposable.add(mWidgetModel.getRadarObstacleDataProcessor().toFlowable().subscribe(data -> {
            if (radarInformation.isConnected()) {
                LogUtils.e(TAG, "雷达已连接");
                //mUpwardRadarDistance 的值分两种情况，针对320这种没有雷达的有一个默认值，而这个默认值在绘制逻辑中会起作用，所以不能覆盖掉，不然会出现问题。只有针对检测到雷达有连接上才使用其真实值
                mUpwardRadarDistance = data.getUpwardObstacleDistance() * 1f / 1000;
                updateWidget();
            }
        }));


        mCompositeDisposable.add(mWidgetModel.getVelocityProcessor().toFlowable().subscribe(velocity3D -> {
            mSpeedZ = velocity3D.getZ().floatValue();
            updateWidget();
        }));
        mCompositeDisposable.add(mWidgetModel.getAltitudeProcessor().toFlowable().subscribeOn(AndroidSchedulers.mainThread()).subscribe(altitude -> {
            mHeight = altitude.floatValue();
            setCurrentValue(mHeight);
        }));
        mCompositeDisposable.add(mWidgetModel.getGoHomeHeightProcessor().toFlowable().subscribe(integer -> {
            //            mReturnToHomeHeight = integer;
            updateWidget();
        }));
        mCompositeDisposable.add(mWidgetModel.getLimitMaxFlightHeightInMeterProcessor().toFlowable().subscribe(integer -> {
            //            mLimitHeight = integer;
            updateWidget();
        }));

    }


    //    /**
    //     * 是否需要显示限高
    //     * 存在解禁证书时，获取到的限高值不准确，不显示限高
    //     */
    //    private void updateLimitVisiable() {
    //        mCompositeDisposable.add(mFlyZoneRepository.fetchWhiteListLicenseFromAircraft()
    //                .subscribeOn(Schedulers.io())
    //                .map(licenses -> {
    //                    boolean showLim = true;
    //                    for (WhiteListLicense license : licenses) {
    //                        if (license.isEnabled() && license.isValid()) {
    //                            showLim = false;
    //                            break;
    //                        }
    //                    }
    //                    return showLim;
    //                })
    //                .observeOn(AndroidSchedulers.mainThread())
    //                .doOnNext(showLim -> {
    //                    mShowLimit = showLim;
    //                    updateWidget();
    //                })
    //                .subscribe()
    //        );
    //    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mCompositeDisposable.dispose();
    }

    @Override
    protected int getMinHeight() {
        return Math.max(getFrameworkHeight(), getAvoidanceHeight());
    }

    private int getAvoidanceHeight() {
        return mAvoidanceMaxHeight * 2 + getBarrierUpIndicatorHeight() + getBarrierDownIndicatorHeight();
    }

    private int getBarrierUpIndicatorHeight() {
        if (mBarrierUp == null) {
            return 0;
        }
        return mBarrierUp.getHeight();
    }

    private int getBarrierDownIndicatorHeight() {
        if (mBarrierDown == null) {
            return 0;
        }
        return mBarrierDown.getHeight();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();

        // 为简化 PFD 中障碍物的位置信息，将障碍物右侧定义为 canvas 水平零点
        float barrierRightOffset;
        if (isShowFramework()) {
            barrierRightOffset = mFrameworkPaddingStart - mFrameworkStrokeWidth / 2f;
        } else {
            barrierRightOffset = mFrameworkPaddingStart + mBarrierOriginLineLength;
        }
        canvas.translate(barrierRightOffset, (float) getHeight() / 2);
        drawDistancePredict(canvas);
        //        if (isShowFramework()) {
        //            drawAttitudeIndicator(canvas);
        //        }
        drawBarrierBackground(canvas);
        drawBarrierInfo(canvas);
        drawBarrierOriginLine(canvas);
        canvas.restore();
    }

    /**
     * 绘制刻度上的标识
     *
     * @param canvas
     */
    //    private void drawAttitudeIndicator() {
    // 只在航线执行过程中显示
    //        boolean hasWp = false;
    //        float waypointAltitude = 0;
    //
    //        MissionManagerDelegate instance = MissionManagerDelegate.INSTANCE;
    //        if (instance.isRunningMission()) {
    //            List<Location3D> targetAndNextPointList = instance.getTargetAndNextPointList();
    //            if (!targetAndNextPointList.isEmpty()) {
    //                hasWp = true;
    //                waypointAltitude = targetAndNextPointList.get(0).getAltitude();
    //            }
    //        }
    //
    //        int limitHeight = mShowLimit ? mLimitHeight : Integer.MAX_VALUE;
    //
    //        float perHeight = (float) getFrameworkHeight() / mVisibleCalibrationUnitCount / mAttributeOffsetPerUnit;
    //
    //        mPaint.setTextSize(mAttitudeIndicatorTextSize);
    //        mPaint.getTextBounds(RTH_TEXT, 0, RTH_TEXT.length(), RECT);
    //        float rthHeight = RECT.height();
    //
    //        mPaint.getTextBounds(LIM_TEXT, 0, LIM_TEXT.length(), RECT);
    //        float limWidth = RECT.width();
    //        float limHeight = RECT.height();
    //
    //        int iconWidth = 0;
    //        int iconHeight = 0;
    //
    //        float deltaHeight;
    //        float deltaValue;
    //        float rthExtraOffsetX = 0;
    //        float limExtraOffsetX = 0;
    //        if (hasWp) {
    //            // 计算 LIM 和 waypoint 是否重叠
    //            iconWidth = mWaypointIcon.getMinimumWidth();
    //            iconHeight = mWaypointIcon.getMinimumHeight();
    //            deltaHeight = (iconHeight + limHeight) / 2;
    //            deltaValue = deltaHeight / perHeight;
    //            if (deltaValue >= Math.abs(waypointAltitude - limitHeight)) {
    //                limExtraOffsetX = iconWidth + mTextExtraPadding;
    //            }
    //        }
    //
    //        // 计算 RTH 和 LIM 是否重叠
    //        deltaHeight = (limHeight + rthHeight) / 2;
    //        deltaValue = deltaHeight / perHeight;
    //        if (deltaValue >= Math.abs(limitHeight - mReturnToHomeHeight)) {
    //            rthExtraOffsetX = limWidth + mTextExtraPadding + limExtraOffsetX;
    //        } else {
    //            if (hasWp) {
    //                // RTH 和 LIM 不重叠，需要再检查 RTH 和 waypoint 重叠
    //                deltaHeight = (iconHeight + rthHeight) / 2;
    //                deltaValue = deltaHeight / perHeight;
    //                if (deltaValue >= Math.abs(mReturnToHomeHeight - waypointAltitude)) {
    //                    rthExtraOffsetX = iconWidth + mTextExtraPadding;
    //                }
    //            }
    //        }
    //
    //        if (hasWp) {
    //            drawAttitudeIndicator(canvas, waypointAltitude, mWayPointHeightIndicatorColor, "", 0, mWaypointIcon);
    //        }
    //        drawAttitudeIndicator(canvas, mReturnToHomeHeight, mReturnToHomeIndicatorColor, RTH_TEXT, -rthExtraOffsetX, null);
    //        drawAttitudeIndicator(canvas, limitHeight, mMaxFlightHeightIndicatorColor, LIM_TEXT, -limExtraOffsetX, null);
    //    }
    private void drawBarrierBackground(Canvas canvas) {
        drawBarrierBackground(canvas, UPWARD);
        drawBarrierBackground(canvas, DOWNWARD);
    }

    private void drawBarrierBackground(Canvas canvas, int orientation) {
        if (isShowFramework() && !isUserDisable(orientation)) {
            return;
        }
        float top;
        float bottom;
        float bgHeight;
        if (isShowFramework()) {
            bgHeight = getViewHeightForDistance(PFD_BARRIER_OFF_TEXT_DISTANCE);
        } else {
            bgHeight = mAvoidanceMaxHeight;
        }
        if (orientation == UPWARD) {
            top = bgHeight * orientation;
            bottom = 0;
        } else {
            top = 0;
            bottom = bgHeight * orientation;
        }
        mPaint.setStyle(Paint.Style.STROKE);
        float strokeWidth = getBarStrokeWidth();
        float halfStrokeWidth = strokeWidth / 2f;
        mPaint.setStrokeWidth(strokeWidth);
        int strokeColor = getBarStrokeColor();
        mPaint.setColor(strokeColor);
        // 为保证不同描边粗细整体宽度一致，左右以描边外位置计算
        float left = -mAvoidanceIndicatorWidth;
        float right = 0;
        canvas.drawRect(left + halfStrokeWidth, top - halfStrokeWidth * orientation, right - halfStrokeWidth, bottom + halfStrokeWidth, mPaint);

        int bgColor = getBarFillColor();
        mPaint.setColor(bgColor);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(left + strokeWidth, top, right - strokeWidth, bottom, mPaint);

        if (!isShowFramework() && (needBarrierInfo(orientation) || isUserDisable(orientation))) {
            float baseline = orientation == UPWARD ? FontUtils.getDigitalBaselineFromTop(mPaint, top) :
                    FontUtils.getDigitalBaselineFromBottom(mPaint, bottom);
            drawBarrierText(canvas, orientation, baseline, (mBarrierUp.getWidth() - mAvoidanceIndicatorWidth) / 2f);
        }
    }

    private int getBarFillColor() {
        return AndUtil.getResColor(R.color.uxsdk_white_20_percent);
    }

    private int getBarStrokeColor() {
        return mStrokeConfig.getStrokeDeepColor();
    }

    private float getBarStrokeWidth() {
        if (isShowFramework()) {
            return mStrokeConfig.getStrokeBoldWidth();
        }
        return mStrokeConfig.getStrokeThinWidth();
    }

    private String mUpBarrierString;
    private final Object mUpBarrierStringLock = new Object();
    private String mDownBarrierString;
    private final Object mDownBarrierStringLock = new Object();

    /**
     * 在异步线程计算要显示的BarrierString
     *
     * @param orientation
     */
    private void calcBarrierString(final int orientation) {
        mDataHandler.post(() -> {
            float barrierDistance;
            String newString = "";
            if (orientation == UPWARD) {
                barrierDistance = getUpwardBarrierDistance();
                newString = getBarrierString(orientation, barrierDistance);
                synchronized (mUpBarrierStringLock) {
                    if (!newString.equals(mUpBarrierString)) {
                        mUpBarrierString = newString;
                        updateWidget();
                    }
                }
            } else {
                barrierDistance = getDownVisionDistance();
                newString = getBarrierString(orientation, barrierDistance);
                synchronized (mDownBarrierStringLock) {
                    if (!newString.equals(mDownBarrierString)) {
                        mDownBarrierString = newString;
                        updateWidget();
                    }
                }
            }
        });
    }

    private String getBarrierString(int orientation, float barrierDistance) {
        if (isUserDisable(orientation)) {
            return "OFF";
        } else {
            float displayValue = getDisplayValue(barrierDistance);
            if (displayValue >= 99.95f) {
                return String.format("%3.0f", displayValue);
            } else {
                return String.format("%2.1f", displayValue);
            }
        }
    }

    private void drawBarrierText(Canvas canvas, final int orientation, float baseline, float textOffsetX) {
        calcBarrierString(orientation);
        String barrierString;
        float barrierDistance;
        float dangerDistance;
        mPaint.setTextSize(mBarrierDistanceTextSize);

        if (orientation == UPWARD) {
            barrierDistance = getUpwardBarrierDistance();
            dangerDistance = mUserSetUpBarrierAvoidanceDistance;
            synchronized (mUpBarrierStringLock) {
                barrierString = mUpBarrierString;
            }
        } else {
            barrierDistance = getDownVisionDistance();
            dangerDistance = mUserSetDownBarrierAvoidanceDistance;
            synchronized (mUpBarrierStringLock) {
                barrierString = mDownBarrierString;
            }
        }

        if (barrierString == null) {
            return;
        }

        if (isShowFramework()) {
            textOffsetX -= mPaint.measureText(barrierString);
        }
        int textColor = getBarrierTextColor(orientation, barrierDistance, dangerDistance);
        drawTextWithStroke(canvas, barrierString, textOffsetX, baseline, mStrokeConfig.getStrokeBoldWidth(), mStrokeConfig.getStrokeDeepColor(),
                textColor);
    }

    private int getBarrierTextColor(int orientation, float barrierDistance, float dangerDistance) {
        int result;
        if (isUserDisable(orientation) || barrierDistance <= dangerDistance) {
            result = mBarrierIndicatorColor;
        } else {
            result = mAvoidanceIndicatorColor;
        }
        return result;
    }

    /**
     * 避障被关闭
     */
    boolean isUserDisable(int orientation) {
        return orientation == UPWARD && isUpwardUserDisable()
                || (orientation == DOWNWARD && isDownwardUserDisable());
    }

    /**
     * 上方避障关闭
     */
    boolean isUpwardUserDisable() {
        return !mUpwardVisualEnable && !mUpwardRadarEnable;
    }

    /**
     * 下方避障关闭
     */
    boolean isDownwardUserDisable() {
        return !mShowDownwardVisualPerceptionInfo;
    }

    //    /**
    //     * 绘制高度标识和文字
    //     */
    //    private void drawAttitudeIndicator(Canvas canvas, float height, int color, String text, float extraOffsetX, Drawable icon) {
    //        float current = getCurrentValue();
    //        float offset = height - current;
    //        float ratio = (float) getFrameworkHeight() / mVisibleCalibrationUnitCount / mAttributeOffsetPerUnit;
    //        float y = offset * ratio;
    //        if (Math.abs(y) >= (float) getFrameworkHeight() / 2) {
    //            return;
    //        }
    //        canvas.save();
    //        float calibrationHorizontalMargin = (float) (mPointerLineInnerWidth - mDegreeLineShortWidth) / 2;
    //
    //        int lineWidth = mFrameworkStrokeWidth;
    //        mPaint.setStrokeWidth(lineWidth);
    //        mPaint.setColor(color);
    //        mPaint.setStyle(Paint.Style.FILL);
    //        mPaint.setTextSize(mAttitudeIndicatorTextSize);
    //
    //        mStrokePaint.setColor(getResources().getColor(R.color.uxsdk_black_30_percent));
    //        mStrokePaint.setStrokeWidth(getShadowLineStrokeWidth());
    //        mStrokePaint.setStyle(Paint.Style.STROKE);
    //
    //        float endX = calibrationHorizontalMargin + mDegreeLineLongWidth;
    //        float startX = Math.min(0, endX - mReturnToHomeIndicatorWidth);
    //        canvas.drawLine(startX, -y, endX, -y, mPaint);
    //        float halfShadowWidth = getShadowLineStrokeWidth() / 2;
    //        float halfWidth = lineWidth / 2f + halfShadowWidth;
    //        canvas.drawRect(startX - halfShadowWidth, -y - halfWidth, endX + halfShadowWidth, -y + halfWidth, mStrokePaint);
    //
    //        if (!TextUtils.isEmpty(text)) {
    //            // 绘制文字
    //            mPaint.setTextAlign(Paint.Align.LEFT);
    //            float textWidth = mPaint.measureText(text);
    //            float textStart = startX - textWidth - mWaypointIconPadding + extraOffsetX;
    //            float textY = FontUtils.getDigitalBaselineFromCenter(mPaint, -y, text);
    //            drawTextWithStroke(canvas, text, textStart, textY, getShadowLineStrokeWidth(), getResources().getColor(R.color
    //            .uxsdk_black_60_percent),
    //                    color);
    //        } else if (icon != null) {
    //            // 绘制 icon
    //            int iconWidth = icon.getMinimumWidth();
    //            int iconHeight = icon.getMinimumHeight();
    //            int left = Math.round(startX - 2 - iconWidth + extraOffsetX);
    //            int top = Math.round(-y - icon.getMinimumHeight() / 2f);
    //            int right = left + iconWidth;
    //            int bottom = top + iconHeight;
    //            icon.setBounds(left, top, right, bottom);
    //            icon.draw(canvas);
    //        }
    //        canvas.restore();
    //    }

    /**
     * 绘制速度条
     */
    private void drawDistancePredict(Canvas canvas) {
        if (Math.abs(mSpeedZ) <= SPEED_THRESHOLD) {
            return;
        }
        canvas.save();
        float predictDistance = PREDICT_TIME_IN_SECONDS * mSpeedZ;
        float ratio = (float) mAvoidanceMaxHeight / getMaxPerceptionDistanceInMeter(mSpeedZ > 0 ? UPWARD : DOWNWARD);
        float predictArea = Math.abs(predictDistance * ratio);

        mPaint.setColor(Color.WHITE);
        if (isShowFramework()) {
            canvas.translate((float) mFrameworkStrokeWidth / 2, 0);
        }
        float halfHeight = mBarrierOriginLineWidth / 2;
        float top, bottom;
        if (predictDistance < 0) {
            top = -(int) predictArea;
            bottom = halfHeight;
        } else {
            bottom = (int) predictArea;
            top = -halfHeight;
        }
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, top, mDistancePredictWidth, bottom, mPaint);
        canvas.restore();
    }

    /**
     * 获取高度在刻度上对应的实际长度
     */
    private float getViewHeightForDistance(float distance) {
        if (!isShowFramework()) {
            return 0;
        }
        return distance / mAttributeOffsetPerUnit * getFrameworkHeight() / mVisibleCalibrationUnitCount;
    }

    private void drawBarrierOriginLine(Canvas canvas) {
        float halfHeight = mBarrierOriginLineWidth / 2;
        // 绘制速度原点线条
        if (!isShowFramework()) {
            mPaint.setColor(Color.WHITE);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(-mBarrierOriginLineLength, halfHeight, 0, -halfHeight, mPaint);
            mPaint.setColor(mStrokeConfig.getStrokeDeepColor());
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(getShadowLineStrokeWidth());
            canvas.drawRect(-mBarrierOriginLineLength, halfHeight, 0, -halfHeight, mPaint);
        }
    }

    private void drawBarrierInfo(Canvas canvas) {
        if (isInEditMode()){
            return;
        }
        if (getUpVisionDistance() == Integer.MAX_VALUE
                && mUpwardRadarDistance == Integer.MAX_VALUE
                && getDownVisionDistance() == Integer.MAX_VALUE) {
            return;
        }
        canvas.save();
        // 绘制上视障碍物的信息
        if (ProductUtil.isM300Product() && isUserDisable(UPWARD)) {
            if (isShowFramework()) {
                drawPfdBarrierUserDisableText(canvas, UPWARD);
            }
        } else if (needUpwardBarrierInfo()) {
            canvas.save();
            drawBarrierInfo(canvas, UPWARD);
            canvas.restore();
        }
        // 绘制下视障碍物的信息
        if (ProductUtil.isM300Product() && isUserDisable(DOWNWARD)) {
            if (isShowFramework()) {
                drawPfdBarrierUserDisableText(canvas, DOWNWARD);
            }
        } else if (needDownwardBarrierInfo()) {
            canvas.save();
            drawBarrierInfo(canvas, DOWNWARD);
            canvas.restore();
        }
        canvas.restore();
    }

    private void drawPfdBarrierUserDisableText(Canvas canvas, int orientation) {
        float strokeWidth = mStrokeConfig.getStrokeThinWidth();
        mPaint.setStrokeWidth(strokeWidth);
        float textOffsetX = -mAvoidanceIndicatorWidth - mBarrierTextMargin;
        float textOffsetY = getViewHeightForDistance(PFD_BARRIER_OFF_TEXT_DISTANCE);
        float baseline = orientation == UPWARD ? FontUtils.getDigitalBaselineFromTop(mPaint, -textOffsetY) :
                FontUtils.getDigitalBaselineFromBottom(mPaint, textOffsetY);
        drawBarrierText(canvas, orientation, baseline, textOffsetX);
    }

    public int getMaxPerceptionDistanceInMeter(int orientation) {
        return orientation == UPWARD ? mUpMaxPerceptionDistanceInMeter : mDownMaxPerceptionDistanceInMeter;
    }

    boolean needBarrierInfo(int orientation) {
        return orientation == UPWARD && needUpwardBarrierInfo()
                || (orientation == DOWNWARD && needDownwardBarrierInfo());
    }

    private boolean needDownwardBarrierInfo() {
        return getDownVisionDistance() <= getMaxPerceptionDistanceInMeter(DOWNWARD)
                && (!ProductUtil.isM300Product() || mShowDownwardVisualPerceptionInfo);
    }

    private boolean needUpwardBarrierInfo() {
        int maxPerceptionDistanceInMeter = getMaxPerceptionDistanceInMeter(UPWARD);
        return (getUpVisionDistance() <= maxPerceptionDistanceInMeter
                && (!ProductUtil.isM300Product() || mUpwardVisualEnable))
                || ((!ProductUtil.isM300Product() || mUpwardRadarEnable)
                && mUpwardRadarDistance <= maxPerceptionDistanceInMeter);
    }

    private void drawBarrierInfo(Canvas canvas, int orientation) {
        // 记录画布移动距离，在绘制障碍物距离时候，需要知道位移
        float[] canvasOffsetY = new float[1];
        // 原点宽度不算在避障高度里面
        float offsetY = isShowFramework() ? 0 : mBarrierOriginLineWidth / 2;
        float avoidanceMaxHeight = mAvoidanceMaxHeight;
        int maxPerceptionDistanceInMeter = getMaxPerceptionDistanceInMeter(orientation);
        if (isShowFramework()) {
            // pfd 中不考虑设置的避障条高度，使用实际刻度对应高度
            avoidanceMaxHeight = getViewHeightForDistance(maxPerceptionDistanceInMeter);
        }
        float barrierIndicatorHeight = avoidanceMaxHeight - offsetY;
        float maxPerceptionDistanceBarrierValue = mBarrierDistanceStrategy.getBarrierValue((float) maxPerceptionDistanceInMeter);
        if (maxPerceptionDistanceBarrierValue <= 0) {
            LogUtils.e(TAG, "mMaxPerceptionDistanceInMeter must large than zero.");
            return;
        }

        canvas.translate(0, offsetY * orientation);
        float ratio = barrierIndicatorHeight / maxPerceptionDistanceBarrierValue;
        // 绘制上视障碍物最远感知条
        int restArea = drawMaxPerceptionIndicator(canvas, orientation, (int) barrierIndicatorHeight, ratio, canvasOffsetY);
        // 绘制上视障碍物感知条
        if (restArea > 0) {
            restArea = drawSettingPerceptionIndicator(canvas, orientation, restArea, ratio, canvasOffsetY);
            // 绘制上视障碍物指示条
            drawBarrierIndicator(canvas, orientation, restArea, ratio, canvasOffsetY);
        }
        // 绘制上视障碍物指示条顶部横线及避障距离
        drawBarrier(canvas, orientation, canvasOffsetY);

        canvas.translate(0, -offsetY * orientation);
    }

    private int drawMaxPerceptionIndicator(Canvas canvas, int orientation, int barrierIndicatorHeight, float ratio, float[] canvasOffsetY) {
        float barrierDistance = orientation == UPWARD ? getUpwardBarrierDistance() : getDownVisionDistance();
        float perceptionDistance = orientation == UPWARD ? mUserSetUpBarrierWarnDistance : mUserSetDownBarrierWarnDistance;

        float barrierValue = mBarrierDistanceStrategy.getBarrierValue(barrierDistance);
        float perceptionValue = mBarrierDistanceStrategy.getBarrierValue(perceptionDistance);

        mPaint.setColor(mAvoidanceIndicatorColor);
        int strokeWidth = mAvoidanceIndicatorStrokeWidth;
        float halfStrokeWidth = mAvoidanceIndicatorStrokeWidth / 2f;
        mPaint.setStrokeWidth(strokeWidth);
        mPaint.setStyle(Paint.Style.STROKE);
        float outOfPerceptionDistanceRange = barrierValue - perceptionValue;
        int restArea = barrierIndicatorHeight;
        if (outOfPerceptionDistanceRange > 0) {
            float outOfPerceptionArea = outOfPerceptionDistanceRange * ratio;
            if (outOfPerceptionArea > restArea) {
                outOfPerceptionArea = restArea;
            }
            if (isShowFramework()) {
                float top;
                float bottom;
                if (orientation == UPWARD) {
                    top = -(int) outOfPerceptionArea + halfStrokeWidth;
                    bottom = 0;
                } else {
                    top = 0;
                    bottom = (int) outOfPerceptionArea - halfStrokeWidth;
                }
                canvas.drawRect(-mAvoidanceIndicatorWidth + halfStrokeWidth, top, 0 - halfStrokeWidth, bottom, mPaint);
            }

            int dy = (int) outOfPerceptionArea * orientation;
            canvasOffsetY[0] += dy;
            canvas.translate(0, dy);
            restArea -= (int) outOfPerceptionArea;
        }
        return restArea;
    }

    private int drawSettingPerceptionIndicator(Canvas canvas, int orientation, int restArea, float ratio, float[] canvasOffsetY) {
        float barrierDistance = orientation == UPWARD ? getUpwardBarrierDistance() : getDownVisionDistance();
        float perceptionDistance = orientation == UPWARD ? mUserSetUpBarrierWarnDistance : mUserSetDownBarrierWarnDistance;
        float barrierAvoidanceDistance = orientation == UPWARD ? mUserSetUpBarrierAvoidanceDistance : mUserSetDownBarrierAvoidanceDistance;

        float barrierValue = mBarrierDistanceStrategy.getBarrierValue(barrierDistance);
        float perceptionValue = mBarrierDistanceStrategy.getBarrierValue(perceptionDistance);
        float barrierAvoidanceValue = mBarrierDistanceStrategy.getBarrierValue(barrierAvoidanceDistance);

        mPaint.setColor(mAvoidanceIndicatorColor);
        mPaint.setStyle(Paint.Style.FILL);
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
            int dy = (int) upPerceptionArea * orientation;
            canvasOffsetY[0] += dy;
            canvas.translate(0, dy);
        }
        return restArea;
    }

    //TODO  这里是绘制感知条高度的地方
    private int drawBarrierIndicator(Canvas canvas, int orientation, int restArea, float ratio, float[] canvasOffsetY) {
        float barrierDistance = orientation == UPWARD ? getUpwardBarrierDistance() : getDownVisionDistance();
        float barrierAvoidanceDistance = orientation == UPWARD ? mUserSetUpBarrierAvoidanceDistance : mUserSetDownBarrierAvoidanceDistance;

        float barrierValue = mBarrierDistanceStrategy.getBarrierValue(barrierDistance);
        float barrierAvoidanceValue = mBarrierDistanceStrategy.getBarrierValue(barrierAvoidanceDistance);


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
            int dy = (int) upBarrierArea * orientation;
            canvasOffsetY[0] += dy;
            canvas.translate(0, dy);
        }
        return restArea;
    }

    private void drawBarrier(Canvas canvas, int orientation, float[] canvasOffsetY) {
        float canvasOffset = canvasOffsetY[0];
        Bitmap barrier;
        if (orientation == UPWARD) {
            barrier = mBarrierUp;
        } else {
            barrier = mBarrierDown;
        }
        if (barrier == null) {
            return;
        }
        // 绘制上视障碍物指示条顶部横线
        float offsetX = isShowFramework() ? -mAvoidanceIndicatorWidth : -mAvoidanceIndicatorWidth / 2f - barrier.getWidth() / 2f;
        float offsetY = orientation == UPWARD ? -barrier.getHeight() : 0;
        canvas.drawBitmap(barrier, offsetX, offsetY, mPaint);

        if (isShowFramework()) {
            drawPfdBarrierText(canvas, orientation, canvasOffset, barrier, offsetX);
        }
    }

    private void drawPfdBarrierText(Canvas canvas, int orientation, float canvasOffset, Bitmap barrier, float offsetX) {
        float strokeWidth = mStrokeConfig.getStrokeThinWidth();
        mPaint.setStrokeWidth(strokeWidth);
        float textOffsetY = (FontUtils.getDigitalTextDrawHeight(mPaint) + mBarrierTextMargin) * orientation;
        float baseline;
        if (orientation == UPWARD) {
            float top = Math.min(textOffsetY - canvasOffset, -barrier.getHeight());
            baseline = FontUtils.getDigitalBaselineFromTop(mPaint, top) - mBarrierTextMargin;
        } else {
            float bottom = Math.max(textOffsetY - canvasOffset, barrier.getHeight());
            baseline = FontUtils.getDigitalBaselineFromBottom(mPaint, bottom) + mBarrierTextMargin;
        }
        drawBarrierText(canvas, orientation, baseline, offsetX - mBarrierTextMargin);
    }

    @Override
    protected String getCurrentValueDisplayFormat(boolean shorthand) {
        return shorthand ? "%04.0f" : "%05.1f";
    }

    @Override
    protected String getAttributeUnit() {
        return UnitUtils.getUintStrByLength(UnitUtils.isMetricUnits() ? UnitUtils.UnitType.METRIC : UnitUtils.UnitType.IMPERIAL);
    }

    @Override
    protected float getDisplayValue(float value) {
        return UnitUtils.getValueFromMetricByLength(value, UnitUtils.isMetricUnits() ? UnitUtils.UnitType.METRIC : UnitUtils.UnitType.IMPERIAL);
    }

    @Override
    protected int getIndicatorLineLength() {
        return mReturnToHomeIndicatorWidth;
    }

    private float getUpwardBarrierDistance() {
        if (mUpwardVisualEnable && mUpwardRadarEnable) {
            return Math.min(mUpwardRadarDistance, getUpVisionDistance());
        } else if (mUpwardRadarEnable) {
            return mUpwardRadarDistance;
        } else {
            return getUpVisionDistance();
        }
    }


    private float getUpVisionDistance() {
        return mPerceptionObstacleData.getUpwardObstacleDistance() / 1000f;
    }

    private float getDownVisionDistance() {
        return mPerceptionObstacleData.getDownwardObstacleDistance() / 1000f;
    }

    /**
     * 距离转避障条高度的策略以及避障条在 pfd 中起飞/降落时下方避障在 0 点
     */
    interface IBarrierDistanceStrategy {
        /**
         * 根据距离获取避障条高度比例
         */
        float getBarrierValue(float distance);
    }

    private static class HsiBarrierDistanceStrategy implements IBarrierDistanceStrategy {
        @Override
        public float getBarrierValue(float distance) {
            if (distance <= 0) {
                return 0;
            } else if (distance <= 2) {
                return distance / 2;
            } else if (distance <= 5) {
                return (distance + 1) / 3;
            } else {
                return (float) (0.3045f + 1.0769f * Math.log(distance));
            }
        }
    }

    private static class PfdBarrierDistanceStrategy implements IBarrierDistanceStrategy {
        @Override
        public float getBarrierValue(float distance) {
            Log.d(TAG, "PfdBarrierDistanceStrategy getBarrierValue=" + distance);

            return distance;
        }
    }
}
