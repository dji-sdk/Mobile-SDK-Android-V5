package dji.v5.ux.core.ui.hsi;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import dji.sdk.keyvalue.value.flightcontroller.FCFlightMode;
import dji.sdk.keyvalue.value.product.ProductType;
import dji.v5.manager.aircraft.perception.data.ObstacleAvoidanceType;
import dji.v5.manager.aircraft.perception.data.PerceptionInfo;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.ui.hsi.config.IOmniAbility;
import dji.v5.ux.core.ui.hsi.dashboard.FpvStrokeConfig;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class HSIPerceptionLayer implements HSIContract.HSILayer {

    private static final String TAG = HSIPerceptionLayer.class.getSimpleName();

    private static final String PERCEPTION_DISABLED = "NA";

    private static final String PERCEPTION_TOF_ONLY = "TOF";
    private static final String PERCEPTION_USER_DISABLE = "OFF";

    /**
     * 雷达路径操作超时
     */
    private static final int RADAR_PATH_OP_TIMEOUT = 500;

    private static final int DEFAULT_PERCEPTION_BLIND_AREA_COUNT = 4;

    private static final int DEFAULT_PERCEPTION_AREA_COUNT = 4;

    private static final int DEFAULT_RADAR_START_ANGLE_OFFSET = -15;

    public static final int WARN_COLOR_ALPHA_CENTER = 0x14;
    public static final int WARN_COLOR_ALPHA_OUTER = 0x40;

    /**
     * 分多少个区块
     */
    private static final int PERCEPTION_GROUP_SIZE = 5;

    @NonNull
    private final Path mPath = new Path();

    @NonNull
    private final RectF mRect = new RectF();

    private final int[] mPerceptionLevelColor = new int[3];

    private final int[] mRadarLevelColor = new int[3];
    private final FpvStrokeConfig mStrokeConfig;

    private List<Integer> mRadarHorizontalDistances = new ArrayList<>();


    @NonNull
    private final GradientDrawable mPerceptionAreaDrawable;
    @NonNull
    private final GradientDrawable mTofDrawable;
    private final GradientDrawable mNaDrawable;


    private final int mPerceptionDisabledTextSize;

    private final int mMaxPerceptionStrokeWidth;
    private final int mRadarMaxPerceptionStrokeWidth;


    private HSIContract.HSIContainer mHSIContainer;

    private float mHorizontalBarrierAvoidanceDistance;

    private float mHorizontalPerceptionDistance;

    private int mCompassSize;

    private boolean mShowVisualPerceptionInfo;

    private boolean mShowRadarPerceptionInfo;

    private boolean mPerceptionNotWorkMode = false;

    private boolean isHidePerceptionBg = false;

    private FCFlightMode mFlightMode = FCFlightMode.UNKNOWN;

    private boolean mIsMultiModeOpen = false;

    @NonNull
    private final List<Shape> mShapeList = new ArrayList<>();

    @NonNull
    private final List<Shape> mRadarShapeList = new ArrayList<>();

    @Nullable
    private CompositeDisposable mDisposable;

    IOmniAbility mOmniAbility;

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

    /**
     * 机型不支持 TOF，如：M3E系列
     */
    private boolean mSupportTof = true;
    private final static PathPool mPathPool = new PathPool(128);
    private final static PathPool mRadarPathPool = new PathPool(128);
    private Shader mTofShader;
    private Shader mNaShader;
    private final int mWarnColor;
    private final int mDangerColor;
    @NonNull
    private HSIWidgetModel widgetModel;


    public HSIPerceptionLayer(@NonNull Context context, @Nullable AttributeSet attrs, HSIContract.HSIContainer container,
                              HSIWidgetModel widgetModel) {
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
                context.getResources().getDimensionPixelSize(R.dimen.uxsdk_2_dp));

        mRadarLevelColor[2] = typedArray.getColor(R.styleable.HSIView_uxsdk_hsi_radar_avoidance_setting_area_color,
                context.getResources().getColor(R.color.uxsdk_pfd_hsi_radar_barrier_color));
        mRadarLevelColor[1] = typedArray.getColor(R.styleable.HSIView_uxsdk_hsi_radar_perception_setting_area_color,
                context.getResources().getColor(R.color.uxsdk_pfd_hsi_radar_avoidance_color));
        mRadarLevelColor[0] = typedArray.getColor(R.styleable.HSIView_uxsdk_hsi_radar_max_perception_area_color,
                context.getResources().getColor(R.color.uxsdk_pfd_hsi_radar_main_color));
        mRadarMaxPerceptionStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.HSIView_uxsdk_hsi_radar_max_perception_stroke_width,
                context.getResources().getDimensionPixelSize(R.dimen.uxsdk_4_dp));
        typedArray.recycle();

        mPerceptionDisabledTextSize = context.getResources().getDimensionPixelSize(R.dimen.uxsdk_6_dp);

        mPerceptionAreaDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{
                        Color.parseColor("#29FFFFFF"),
                        Color.parseColor("#0CFFFFFF")
                });
        mPerceptionAreaDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        mPerceptionAreaDrawable.setShape(GradientDrawable.RECTANGLE);

        mWarnColor = context.getResources().getColor(R.color.uxsdk_pfd_avoidance_color);
        mDangerColor = context.getResources().getColor(R.color.uxsdk_pfd_barrier_color);
        mTofDrawable = new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{
                        ColorUtils.setAlphaComponent(mWarnColor, WARN_COLOR_ALPHA_OUTER),
                        ColorUtils.setAlphaComponent(mWarnColor, WARN_COLOR_ALPHA_CENTER)
                }
        );
        mTofDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        mTofDrawable.setShape(GradientDrawable.RECTANGLE);

        mNaDrawable = new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{
                        ColorUtils.setAlphaComponent(mDangerColor, WARN_COLOR_ALPHA_OUTER),
                        ColorUtils.setAlphaComponent(mDangerColor, WARN_COLOR_ALPHA_CENTER)
                }
        );
        mNaDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        mNaDrawable.setShape(GradientDrawable.RECTANGLE);

        mStrokeConfig = new FpvStrokeConfig(context);

        mOmniAbility = IOmniAbility.Companion.getCurrent();

    }

    @Override
    public void onStart() {
        mDisposable = new CompositeDisposable();

        mDisposable.add(widgetModel.getProductTypeDataProcessor().toFlowable()
                .subscribe(productType -> {
                    boolean isM3Serirs = productType == ProductType.DJI_MAVIC_3_ENTERPRISE_SERIES;
                    isHidePerceptionBg = isM3Serirs;
                    mSupportTof = !isM3Serirs;
                }));

        mDisposable.add(widgetModel.getPerceptionInformationDataProcessor().toFlowable().subscribe(new Consumer<PerceptionInfo>() {
            @Override
            public void accept(PerceptionInfo perceptionInfo) throws Throwable {
                mShowVisualPerceptionInfo =
                        perceptionInfo.isHorizontalObstacleAvoidanceEnabled() || perceptionInfo.getObstacleAvoidanceType() == ObstacleAvoidanceType.BYPASS;
                mHorizontalBarrierAvoidanceDistance = (float) perceptionInfo.getHorizontalObstacleAvoidanceBrakingDistance();
                mHorizontalPerceptionDistance = (float) perceptionInfo.getHorizontalObstacleAvoidanceWarningDistance();

                mVisionPerceptionEnableState[0] = perceptionInfo.getForwardObstacleAvoidanceWorking();
                mVisionPerceptionEnableState[1] = perceptionInfo.getRightSideObstacleAvoidanceWorking();
                mVisionPerceptionEnableState[2] = perceptionInfo.getBackwardObstacleAvoidanceWorking();
                mVisionPerceptionEnableState[3] = perceptionInfo.getLeftSideObstacleAvoidanceWorking();
            }
        }));

        mDisposable.add(widgetModel.getRadarInformationDataProcessor().toFlowable().subscribe(information -> {
            mShowRadarPerceptionInfo = information.isHorizontalObstacleAvoidanceEnabled();
        }));
        mDisposable.add(widgetModel.getPerceptionObstacleDataProcessor().toFlowable()
                .throttleLast(200, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread()) //shape 计算要在mainthread，不然有可能会导致native内存使用错误
                .observeOn(AndroidSchedulers.mainThread())
                .map(data -> updatePerceptionDrawShape(getOptimizationDataIfNeed(data.getHorizontalObstacleDistance())))
                .subscribe(list -> {
                    mShapeList.clear();
                    mShapeList.addAll(list);
                    if (mHSIContainer != null) {
                        mHSIContainer.updateWidget();
                    }
                }));

        mDisposable.add(widgetModel.getRadarObstacleDataProcessor().toFlowable().subscribe(data -> mRadarHorizontalDistances =
                data.getHorizontalObstacleDistance()));


        mDisposable.add(Flowable.combineLatest(
                widgetModel.getFlightModeProcessor().toFlowable(),
                widgetModel.getMultipleFlightModeEnabledProcessor().toFlowable(),
                (fcFlightMode, isMultiModeOpen) -> {
                    mFlightMode = fcFlightMode;
                    mIsMultiModeOpen = isMultiModeOpen;
                    mPerceptionNotWorkMode = mIsMultiModeOpen && (mFlightMode == FCFlightMode.GPS_SPORT || mFlightMode == FCFlightMode.ATTI);
                    Arrays.fill(mToFPerceptionEnableState, !mPerceptionNotWorkMode);
                    return true;
                }).subscribe()
        );

        mDisposable.add(getRadarDisposable());
    }


    /**
     * 针对数据进行分组，每组使用其中最小值
     * 数据分组可避免显示非常细的障碍图
     */
    @NonNull
    private List<Integer> getOptimizationDataIfNeed(List<Integer> data) {
        int size = data.size();
        if (size == 0) {
            return data;
        }
        int sectorLength = PERCEPTION_GROUP_SIZE;
        int sectorCount = size / sectorLength;
        float singleDataAngle = 360f / size;
        for (int i = 0; i < sectorCount; i++) {
            int srcPos = sectorLength * i;
            if (posInBlind(srcPos, sectorLength, singleDataAngle)) {
                continue;
            }
            int result = minInList(data, srcPos, sectorLength);
            fillList(data, srcPos, sectorLength, result);
        }
        return data;
    }

    /**
     * 判断数据起止位置是否落在盲区
     */
    private boolean posInBlind(int srcPos, int sectorLength, float singleDataAngle) {
        float startAngle = srcPos * singleDataAngle;
        float sweepAngle = sectorLength * singleDataAngle;
        return posInBlind(startAngle) || posInBlind(startAngle + sweepAngle);
    }

    /**
     * 数据是否落在盲区
     */
    private boolean posInBlind(float angle) {
        return Math.abs(angle % 90 - 45) < mOmniAbility.getPerceptionBlindAreaAngle() / 2f;
    }

    /**
     * @param data   数据集
     * @param offset 数据开始的索引
     * @param size   需要计算的数据量
     * @return
     * @throws IndexOutOfBoundsException offset 必须小于 date 大小
     */
    private int minInList(List<Integer> data, int offset, int size) {
        if (offset >= data.size() || offset < 0) {
            throw new IndexOutOfBoundsException();
        }
        int last = Math.min(offset + size, data.size());
        int result = data.get(last - 1);
        for (int i = last - 2; i >= offset; i--) {
            result = Math.min(result, data.get(i));
        }
        return result;
    }

    /**
     * @param data   数据集
     * @param offset 数据开始的索引
     * @param size   需要填充的大小
     * @return
     * @throws IndexOutOfBoundsException offset 必须小于 date 大小
     */
    private void fillList(List<Integer> data, int offset, int size, int value) {
        if (offset >= data.size() || offset < 0) {
            throw new IndexOutOfBoundsException();
        }
        int last = Math.min(offset + size, data.size());
        for (int i = last - 1; i >= offset; i--) {
            data.set(i, value);
        }
    }

    @Override
    public void onStop() {
        if (mDisposable != null) {
            mDisposable.dispose();
        }

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
                    if (mHSIContainer != null) {
                        mHSIContainer.updateWidget();
                    }
                });
    }

    @Override
    public void draw(Canvas canvas, Paint paint, int compassSize) {
        mCompassSize = compassSize;
        canvas.save();
        canvas.translate(0, (float) compassSize / 2);
        drawRadarBarrier(canvas, paint, compassSize);
        drawBarrier(canvas, paint, compassSize);
        drawPerception(canvas, paint, compassSize);
        canvas.restore();
    }

    boolean isUserDisable() {
        return !mShowVisualPerceptionInfo && !mShowRadarPerceptionInfo;
    }

    /**
     * 绘制 HSI 中障碍检测范围背景
     *
     * @param compassSize 罗盘半径
     */
    private void drawPerception(Canvas canvas, Paint paint, int compassSize) {
        if (mHSIContainer == null) {
            return;
        }
        fixAlpha(paint);
        canvas.save();

        int perceptionAngleTotal = 360 - DEFAULT_PERCEPTION_BLIND_AREA_COUNT * mOmniAbility.getPerceptionBlindAreaAngle();
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
            boolean visionEnable = mVisionPerceptionEnableState[i];
            if (!isHidePerceptionBg) {
                drawPerceptionBg(canvas, paint, i, visionEnable, radius, perceptionAngleEach);
            }
            if (isUserDisable() || mPerceptionNotWorkMode || (!visionEnable && mShowVisualPerceptionInfo)) {
                boolean isTof = mToFPerceptionEnableState[i];
                // 绘制文字
                paint.setTextSize(mPerceptionDisabledTextSize);
                paint.setTextAlign(Paint.Align.CENTER);
                String text = getPerceptionStatusText(isTof);
                paint.getTextBounds(text, 0, text.length(), HSIView.RECT);
                Paint.FontMetrics fontMetrics = paint.getFontMetrics();
                float baselineOffsetY = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom;
                canvas.save();
                canvas.translate(0, -(radius + mHSIContainer.getAircraftSize()) / 2);
                canvas.rotate(-90f * i);
                int textColor = getPerceptionTextColor(isTof);
                drawTextWithStroke(canvas, text, baselineOffsetY, mStrokeConfig.getStrokeBoldWidth(), mStrokeConfig.getStrokeDeepColor(), textColor
                        , paint);
                canvas.restore();
            }
            canvas.restore();
            canvas.rotate(perceptionAngleEach + mOmniAbility.getPerceptionBlindAreaAngle() * 1f);
        }

        canvas.restore();
    }

    private void drawPerceptionBg(Canvas canvas, Paint paint, int index, boolean visionEnable, float radius, int perceptionAngleEach) {
        if (isUserDisable() || (!mPerceptionNotWorkMode && (visionEnable || !mShowVisualPerceptionInfo))) {
            canvas.clipPath(mPath);
            mPerceptionAreaDrawable.setBounds((int) -radius, (int) -radius, (int) radius, 0);
            mPerceptionAreaDrawable.draw(canvas);
        } else {
            // 用户关闭开关
            boolean isTof = mToFPerceptionEnableState[index];
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(getPerceptionDisabledShader(radius, isTof));
            canvas.drawArc(mRect, 270 - (float) perceptionAngleEach / 2, perceptionAngleEach, true, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(null);
        }
    }

    /**
     * 画笔设置透明度颜色之后，画笔透明度需要修复
     */
    private void fixAlpha(Paint paint) {
        paint.setAlpha(255);
    }

    private int getPerceptionTextColor(boolean isTof) {
        if (!isUserDisable() && isTof && mSupportTof) {
            return mWarnColor;
        } else {
            return mDangerColor;
        }
    }

    @NonNull
    private String getPerceptionStatusText(boolean isTof) {
        if (isUserDisable()) {
            return PERCEPTION_USER_DISABLE;
        } else if (isTof && mSupportTof) {
            return PERCEPTION_TOF_ONLY;
        } else {
            return PERCEPTION_DISABLED;
        }
    }

    protected void drawTextWithStroke(Canvas canvas, String text, float baseline, float strokeWidth, int strokeColor, int textColor, Paint paint) {
        paint.setStrokeWidth(strokeWidth);
        paint.setColor(strokeColor);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawText(text, 0, baseline, paint);

        paint.setColor(textColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawText(text, 0, baseline, paint);
    }

    public Shader getPerceptionDisabledShader(float radius, boolean isTof) {
        Shader result;
        if (isTof && mSupportTof) {
            if (mTofShader == null) {
                mTofShader = createLinearShader(radius, mWarnColor);
            }
            result = mTofShader;
        } else {
            if (mNaShader == null) {
                mNaShader = createLinearShader(radius, mDangerColor);
            }
            result = mNaShader;
        }
        return result;
    }

    @NonNull
    private LinearGradient createLinearShader(float radius, int warnColor) {
        int centerColor = ColorUtils.setAlphaComponent(warnColor, WARN_COLOR_ALPHA_CENTER);
        int outerColor = ColorUtils.setAlphaComponent(warnColor, WARN_COLOR_ALPHA_OUTER);
        return new LinearGradient(0, 0, 0, -radius, centerColor, outerColor, Shader.TileMode.CLAMP);
    }

    private void drawBarrier(Canvas canvas, Paint paint, int compassSize) {
        if (mHSIContainer == null) {
            return;
        }

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
            int fromAngle = shape.mFromAngle;
            boolean skip = mPerceptionNotWorkMode;
            if (!skip) {
                skip = calcSkip(perceptionAngleEach, fromAngle);
            }
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
                    canvas.drawArc(-arcRadius, -arcRadius, arcRadius, arcRadius, 270f,
                            ((ArcShape) shape).mToAngle * 1f - shape.mFromAngle * 1f, false, paint);
                    paint.setStrokeWidth(lastStrokeWidth);
                }
            }
            canvas.restore();
        }
    }

    private boolean calcSkip(int perceptionAngleEach, int fromAngle) {
        boolean skip = false;
        if (fromAngle > perceptionAngleEach * 7 / 2 || fromAngle < perceptionAngleEach / 2) {
            skip = !mVisionPerceptionEnableState[0] && !mToFPerceptionEnableState[0];
        } else if (fromAngle > perceptionAngleEach / 2 && fromAngle < perceptionAngleEach * 3 / 2) {
            skip = !mVisionPerceptionEnableState[1] && !mToFPerceptionEnableState[1];
        } else if (fromAngle > perceptionAngleEach * 3 / 2 && fromAngle < perceptionAngleEach * 5 / 2) {
            skip = !mVisionPerceptionEnableState[2] && !mToFPerceptionEnableState[2];
        } else if (fromAngle > perceptionAngleEach * 5 / 2 && fromAngle < perceptionAngleEach * 7 / 2) {
            skip = !mVisionPerceptionEnableState[3] && !mToFPerceptionEnableState[3];
        }
        return skip;
    }

    private void drawRadarBarrier(Canvas canvas, Paint paint, int compassSize) {
        if (mHSIContainer == null) {
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
                    canvas.rotate(shape.mFromAngle + DEFAULT_RADAR_START_ANGLE_OFFSET * 1f);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(shape.mColor);
                    canvas.drawPath(((PathShape) shape).mPath, paint);
                    mRadarPathPool.recycle(((PathShape) shape).mPath);
                } else if (shape instanceof ArcShape) {
                    canvas.rotate(shape.mFromAngle + DEFAULT_RADAR_START_ANGLE_OFFSET * 1f);
                    paint.setColor(shape.mColor);
                    paint.setStyle(Paint.Style.STROKE);
                    float lastStrokeWidth = paint.getStrokeWidth();
                    paint.setStrokeWidth(mRadarMaxPerceptionStrokeWidth);
                    float arcRadius = radius - (float) mRadarMaxPerceptionStrokeWidth / 2;
                    canvas.drawArc(-arcRadius, -arcRadius, arcRadius, arcRadius, 270f,
                            ((ArcShape) shape).mToAngle - shape.mFromAngle * 1f, false, paint);
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
        int perceptionAngleTotal = 360 - DEFAULT_PERCEPTION_BLIND_AREA_COUNT * mOmniAbility.getPerceptionBlindAreaAngle();
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
        LogUtils.d(TAG,
                "updateRadarDrawShape " + horizontalBarrierDistance.get(0) + ". Current thread is " + currentThread.getName() + " " + currentThread.getId());
        long preTime = System.currentTimeMillis();
        List<Shape> result = updateDrawShape2(horizontalBarrierDistance, 0, mRadarLevelColor);
        LogUtils.d(TAG, "updateRadarDrawShape take " + (System.currentTimeMillis() - preTime));
        return result;
    }

    private List<Shape> updateDrawShape2(List<Integer> horizontalBarrierDistance, int startOffset, int[] levelColor) {

        List<Shape> shapeList = new ArrayList<>();
        if (mHSIContainer == null) {
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
                if (distanceInMeter <= mOmniAbility.getHorizontalDetectionCapability()) {
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
            if ((distanceInMeter >= visibleDistanceInHsi || i == horizontalBarrierDistance.size() - 1)
                    && !path1.isEmpty() && lastShape != null) {
                path1.close();
                path2 = mRadarPathPool.acquire();
                path2.reset();
                path2.addArc(-radius, -radius, radius, radius, 270, barrierRotation);
                path2.lineTo(0, 0);
                path2.close();
                LogUtils.d(TAG, "updateRadar2 op 1 size=" + shapeList.size() + " " + minDistanceInMeter);
                //todo：必须重构path的布尔操作算法，怀疑是雷达传递数据有问题，导致方法卡死
                path2.op(path1, Path.Op.DIFFERENCE);
                LogUtils.d(TAG, "updateRadar2 op 2");

                int areaColor;
                if (minDistanceInMeter > mHorizontalPerceptionDistance) {
                    areaColor = levelColor[0];//
                } else if (minDistanceInMeter > mHorizontalBarrierAvoidanceDistance + 2) {
                    // feature HYAPP-10551 避障变红由【刹停距离】改为【刹停距离+2m】
                    areaColor = levelColor[1];
                } else {
                    areaColor = levelColor[2];
                }
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

    private List<Shape> updateDrawShape(List<Integer> horizontalBarrierDistance, int startOffset, int[] levelColor) {
        List<Shape> shapeList = new ArrayList<>();

        if (mHSIContainer == null) {
            return shapeList;
        }


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
                if (distanceInMeter <= mOmniAbility.getHorizontalDetectionCapability()) {
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
            if ((distanceInMeter >= visibleDistanceInHsi || i == horizontalBarrierDistance.size() - 1)
                    && !path1.isEmpty() && lastShape instanceof PathShape) {
                path1.close();
                path2 = mPathPool.acquire();
                path2.reset();
                path2.addArc(-radius, -radius, radius, radius, 270, barrierRotation);
                path2.lineTo(0, 0);
                path2.close();
                //                LogUtils.d(TAG, "updateRadar op 1 size=" + shapeList.size() + " " + minDistanceInMeter);
                //                sendRadarMsg();
                path2.op(path1, Path.Op.DIFFERENCE);
                //                LogUtils.d(TAG, "updateRadar op 2");
                //                removeRadarMsg();
                int areaColor;
                if (minDistanceInMeter > mHorizontalPerceptionDistance) {
                    areaColor = levelColor[0];//
                } else if (minDistanceInMeter > mHorizontalBarrierAvoidanceDistance + 2) {
                    // feature HYAPP-10551 避障变红由【刹停距离】改为【刹停距离+2m】
                    areaColor = levelColor[1];
                } else {
                    areaColor = levelColor[2];
                }
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
