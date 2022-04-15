package dji.v5.ux.core.ui.hsi;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.v5.ux.R;
import dji.v5.ux.core.util.MatrixUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class AircraftAttitudeView extends View {

    private static final String TAG = "AircraftAttitudeView";

    private static final int VIEW_DRAW_FRAME_RATE = 60;

    private static final int DATA_RECEIVED_FRAME_RATE = 10;

    @NonNull
    private static final float[] EMPTY_FLOAT_ARRAYS = new float[0];

    /**
     * 机头"+"相对于父布局的百分比
     */
    private static final float AIRCRAFT_NOSE_PERCENTAGE_OF_PARENT = 1f / 6;

    /**
     * 速度矢量球相对于父布局的百分比
     */
    private static final float AIRCRAFT_HEADING_VIEW_PERCENTAGE_OF_PARENT = 3f / 25;

    private static final float[] C2I_MATRIX = new float[]{0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f};
    /**
     * 镜头焦距，以像素为单位
     */
    private static final float FPV_FOCUS_X = 1716.12f;
    /**
     * 镜头焦距，以像素为单位
     */
    private static final float FPV_FOCUS_Y = 1716.12f;

    /**
     * 视频原始宽度的1/2
     */
    private static final float VIDEO_CENTER_X = 2028f;

    /**
     * 视频原始高度的1/2
     */
    private static final float VIDEO_CENTER_Y = 1520f;

    @NonNull
    private final Paint mPaint;

    private int mVideoViewWidth;

    private int mVideoViewHeight;

    private int mActualWidth;

    private int mActualHeight;

    private final int mLineStrokeWidth;

    private float mYaw;
    private float mRoll;
    private float mPitch;

    private float mSpeedX;
    private float mSpeedY;
    private float mSpeedZ;

    @NonNull
    private final List<float[]> mParameters = new ArrayList<>();

    @NonNull
    private final PublishSubject<float[]> mAircraftAttitudePublisher = PublishSubject.create();

    @Nullable
    private ViewModel mViewModel;

    @Nullable
    private CompositeDisposable mDisposable;

    public AircraftAttitudeView(Context context) {
        this(context, null);
    }

    public AircraftAttitudeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AircraftAttitudeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AircraftAttitudeView);
        mActualWidth = typedArray.getDimensionPixelSize(R.styleable.AircraftAttitudeView_uxsdk_actual_width, -1);
        mActualHeight = typedArray.getDimensionPixelSize(R.styleable.AircraftAttitudeView_uxsdk_actual_height, -1);
        typedArray.recycle();

        mLineStrokeWidth = context.getResources().getDimensionPixelSize(R.dimen.uxsdk_1_dp);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setColor(getResources().getColor(R.color.uxsdk_pfd_main_color));
        mPaint.setStrokeWidth(mLineStrokeWidth);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mDisposable = new CompositeDisposable();
        // 屏幕刷新率是60Hz，协议推送频率是10Hz，对数据进行两帧线性内插，提高平滑度
        mDisposable.add(mAircraftAttitudePublisher
                .debounce(10, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.single())
                .map(floats -> new float[][]{
                        mParameters.isEmpty() ? null : mParameters.get(mParameters.size() - 1),
                        floats
                })
                .observeOn(Schedulers.computation())
                .map(params -> {
                    return handParameter(params);
                })
                .onErrorReturn(throwable -> {
                    return false;
                })
                .subscribe());

        mDisposable.add(Observable.interval(1000 / VIEW_DRAW_FRAME_RATE, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.single())
                .map(aLong -> {
                    return getParameter();
                })
                .distinctUntilChanged()
                .observeOn(Schedulers.computation())
                .map(floats -> {
                    ViewModel viewModel = new ViewModel();
                    if (floats.length > 0) {
                        updateHorizontalLine(viewModel, (int) floats[0], (int) floats[1], floats[2],
                                floats[3], floats[4]);
                        updateSpeedVectorMark(viewModel, (int) floats[0], (int) floats[1], floats[2],
                                floats[3], floats[4], floats[5], floats[6], floats[7]);
                    }
                    return viewModel;
                })
                .onErrorReturn(throwable -> {
                    return new ViewModel();
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(viewModel -> {
                    if (!viewModel.isValidate()) {
                        return;
                    }
                    mViewModel = viewModel;
                    invalidate();
                }));
    }

    private boolean handParameter(float[][] params){
        if (params[0] == null) {
            mParameters.add(params[1]);
        } else {
            float[] startElement = params[0];
            float[] floats = params[1];
            int interval = VIEW_DRAW_FRAME_RATE / DATA_RECEIVED_FRAME_RATE;
            //若pitch yaw roll前后角度相反，采用线性插值会导致速度矢量球有剧烈的变化。这里用等量的前参数+后参数进行插值可以解决此问题
            boolean isDrasticChange = floats[2] * startElement[2] < 0 || floats[3] * startElement[3] < 0 || floats[4] * startElement[4] < 0;
            float pitchOffset = (floats[2] - startElement[2]) / interval;
            float yawOffset = (floats[3] - startElement[3]) / interval;
            float rollOffset = (floats[4] - startElement[4]) / interval;
            float speedXOffset = (floats[5] - startElement[5]) / interval;
            float speedYOffset = (floats[6] - startElement[6]) / interval;
            float speedZOffset = (floats[7] - startElement[7]) / interval;
            for (int i = 1; i < interval; i++) {
                float[] offset = null;
                if (!isDrasticChange) {
                    offset = new float[]{floats[0], floats[1],
                            startElement[2] + pitchOffset * i, startElement[3] + yawOffset * i, startElement[4] + rollOffset * i, startElement[5] + speedXOffset * i,
                            startElement[6] + speedYOffset * i, startElement[7] + speedZOffset * i
                    };
                } else {
                    offset = i < interval / 2 ? startElement : floats;
                }
                mParameters.add(offset);
            }
            mParameters.add(floats);
        }
        return true;
    }

    private float[] getParameter(){
        if (mParameters.isEmpty()) {
            return EMPTY_FLOAT_ARRAYS;
        } else {
            if (mParameters.get(0) == null) {
                mParameters.remove(0);
                return EMPTY_FLOAT_ARRAYS;
            } else {
                if (mParameters.size() > 1) {
                    return mParameters.remove(0);
                } else {
                    return mParameters.get(0);
                }
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
        }
    }

    public void setYaw(float yaw) {
        mYaw = yaw;
        recalculateAndInvalidate();
    }

    public void setRoll(float roll) {
        mRoll = roll;
        recalculateAndInvalidate();
    }

    public void setPitch(float pitch) {
        mPitch = pitch;
        recalculateAndInvalidate();
    }

    public void setSpeedX(float speedX) {
        mSpeedX = speedX;
        recalculateAndInvalidate();
    }

    public void setSpeedY(float speedY) {
        mSpeedY = speedY;
        recalculateAndInvalidate();
    }

    public void setSpeedZ(float speedZ) {
        mSpeedZ = speedZ;
        recalculateAndInvalidate();
    }

    public void setVideoViewSize(int videoViewWidth, int videoViewHeight) {
        mVideoViewWidth = videoViewWidth;
        mVideoViewHeight = videoViewHeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mActualHeight == -1) {
            mActualHeight = getHeight();
        }
        if (mActualWidth == -1) {
            mActualWidth = getWidth();
        }
        // "+"的大小取两者的最小值
        int aircraftNoseSize = Math.min((int) (mActualWidth * AIRCRAFT_NOSE_PERCENTAGE_OF_PARENT),
                (int) (mActualHeight * AIRCRAFT_NOSE_PERCENTAGE_OF_PARENT));

        drawAircraftNose(canvas, aircraftNoseSize);
        if (mViewModel != null) {
            drawAircraftHorizon(canvas, aircraftNoseSize, mViewModel.aircraftHorizonRotate, mViewModel.aircraftHorizonOffsetY);
            drawAircraftHeading(canvas, mViewModel.aircraftHeadingOffsetX, mViewModel.aircraftHeadingOffsetY);
            return;
        }
        drawAircraftHorizon(canvas, aircraftNoseSize, 0, 0);
        drawAircraftHeading(canvas, 0, 0);
    }

    /**
     * 画出飞机机头，始终在图传正中央，用"+"表示
     *
     * @param aircraftNoseSize 机头"+"的大小
     */
    private void drawAircraftNose(Canvas canvas, int aircraftNoseSize) {
        // 画横线
        canvas.drawLine(
                (float) (getWidth() - aircraftNoseSize) / 2,
                (float) getHeight() / 2,
                (float) (getWidth() + aircraftNoseSize) / 2,
                (float) getHeight() / 2,
                mPaint
        );

        // 画竖线
        canvas.drawLine(
                (float) getWidth() / 2,
                (float) (getHeight() + aircraftNoseSize) / 2,
                (float) getWidth() / 2,
                (float) (getHeight() - aircraftNoseSize) / 2,
                mPaint
        );
    }

    /**
     * 画出速度矢量球，表示飞机即将飞去的位置，飞机往后飞的时候不显示
     * 要有一条线连接矢量球跟机头
     */
    private void drawAircraftHeading(Canvas canvas, float offsetX, float offsetY) {
        float middleHeight = (float) getHeight() / 2;
        float middleWidth = (float) getWidth() / 2;
        // 计算速度矢量球的大小
        float aircraftHeadingSize = Math.min(mActualWidth * AIRCRAFT_HEADING_VIEW_PERCENTAGE_OF_PARENT,
                mActualHeight * AIRCRAFT_HEADING_VIEW_PERCENTAGE_OF_PARENT);

        // 画连接机头与矢量球的线段
        canvas.save();
        canvas.translate(middleWidth, middleHeight);
        canvas.drawLine(0, 0, offsetX, offsetY, mPaint);
        canvas.translate(offsetX, offsetY);
        // 画矢量球
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(0, 0, aircraftHeadingSize / 2 / 2, mPaint);
        // 画矢量球左上右的线段
        canvas.drawLine(-aircraftHeadingSize / 2, 0, -aircraftHeadingSize / 2 / 2, 0, mPaint);
        canvas.drawLine(aircraftHeadingSize / 2 / 2, 0, aircraftHeadingSize / 2, 0, mPaint);
        canvas.drawLine(0, -aircraftHeadingSize / 2 / 2, 0, -aircraftHeadingSize / 2, mPaint);
        canvas.restore();
    }

    /**
     * 画出地平线，反应飞行器的姿态，与飞行器倾斜角度相反
     *
     * @param aircraftNoseSize 机头"+"的大小
     * @param rotate           倾斜角
     * @param offsetY          垂直方向上的偏移量
     */
    private void drawAircraftHorizon(Canvas canvas, int aircraftNoseSize, float rotate, float offsetY) {
        float middleHeight = (float) getHeight() / 2;
        float middleWidth = (float) getWidth() / 2;
        canvas.save();
        canvas.translate(middleWidth, middleHeight);
        canvas.rotate(rotate);
        canvas.translate(0, -offsetY);
        // 画左边的线段
        canvas.drawLine(-(float) mActualWidth / 2,
                0,
                (float) -aircraftNoseSize / 2,
                0,
                mPaint);
        // 画右边的线段
        canvas.drawLine((float) aircraftNoseSize / 2,
                0,
                (float) mActualWidth / 2,
                0,
                mPaint);
        canvas.restore();

    }

    /**
     * 计算地平线在VideoView中的位置，原始位置是视频中间
     */
    private static void updateHorizontalLine(ViewModel viewModel, int displayWidth, int displayHeight,
                                             float pitch, float yaw, float roll) {
        float[] i2gMat = MatrixUtils.createRotationMatrix(yaw, pitch, roll);
        float[] bl2gMat = MatrixUtils.createRotationMatrix(yaw, 0, 0);

        float[] g2blMat = MatrixUtils.transposeMatrix(bl2gMat);
        float[] i2blMat = MatrixUtils.productMatrix(g2blMat, i2gMat);

        float[] c2blMat = MatrixUtils.productMatrix(i2blMat, C2I_MATRIX);
        float[] bl2cMat = MatrixUtils.transposeMatrix(c2blMat);

        float[] kMat = MatrixUtils.createIntrinsicMatrix(FPV_FOCUS_X, FPV_FOCUS_Y, VIDEO_CENTER_X, VIDEO_CENTER_Y);

        float[] v1 = {bl2cMat[0] / bl2cMat[6], bl2cMat[3] / bl2cMat[6], 1.0f};
        float[] v2 = {(bl2cMat[0] + bl2cMat[1]) / (bl2cMat[6] + bl2cMat[7]), (bl2cMat[3] + bl2cMat[4]) / (bl2cMat[6] + bl2cMat[7]), 1.0f};
        v1 = MatrixUtils.rotateVector(v1, kMat);
        v2 = MatrixUtils.rotateVector(v2, kMat);

        float a = (v2[1] - v1[1]) / (v2[0] - v1[0]);
        float angrad = (float) Math.atan(a);
        float rotate = (float) Math.toDegrees(angrad);
        float c = v1[1] - a * v1[0];
        float offsetY = (float) displayHeight / 2 - (float) displayWidth / 2 * a - c * displayHeight / (VIDEO_CENTER_Y * 2);

        viewModel.aircraftHorizonRotate = rotate;
        viewModel.aircraftHorizonOffsetY = offsetY;
    }

    /**
     * 计算速度矢量球在VideoView中的位置，原始位置是视频中间
     */
    private static void updateSpeedVectorMark(ViewModel viewModel, int displayWidth, int displayHeight,
                                              float pitch, float yaw, float roll,
                                              float speedX, float speedY, float speedZ) {

        float[] i2gMat = MatrixUtils.createRotationMatrix(yaw, pitch, roll);
        float[] g2iMat = MatrixUtils.transposeMatrix(i2gMat);

        float[] speedVector = new float[]{speedX, speedY, speedZ};

        float[] vi = MatrixUtils.rotateVector(speedVector, g2iMat);

        if (vi[0] >= 0) {
            float[] c2gMat = MatrixUtils.productMatrix(i2gMat, C2I_MATRIX);
            float[] g2cMat = MatrixUtils.transposeMatrix(c2gMat);
            float[] kMat = MatrixUtils.createIntrinsicMatrix(FPV_FOCUS_X, FPV_FOCUS_Y, VIDEO_CENTER_X, VIDEO_CENTER_Y);
            float[] mMat = MatrixUtils.productMatrix(kMat, g2cMat);

            speedVector = MatrixUtils.rotateVector(speedVector, mMat);
            speedVector[0] = speedVector[0] / speedVector[2];
            speedVector[1] = speedVector[1] / speedVector[2];

            float vectorX = (speedVector[0] - VIDEO_CENTER_X) * displayWidth / (VIDEO_CENTER_X * 2);
            float vectorY = (speedVector[1] - VIDEO_CENTER_Y) * displayHeight / (VIDEO_CENTER_Y * 2);

            //检查边界值
            if (vectorX < -(float) displayWidth / 2 || vectorX > (float) displayWidth / 2) {
                float checkedX;
                checkedX = Math.max(vectorX, (float) -displayWidth / 2);
                checkedX = vectorX > (float) displayWidth / 2 ? (float) displayWidth / 2 : checkedX;

                float checkedY = checkedX / vectorX * vectorY;

                vectorX = checkedX;
                vectorY = checkedY;
            }

            if (vectorY < -(float) displayHeight / 2 || vectorY > (float) displayHeight / 2) {
                float checkedY;
                checkedY = Math.max(vectorY, (float) -displayHeight / 2);
                checkedY = vectorY > (float) displayHeight / 2 ? (float) displayHeight / 2 : checkedY;

                vectorX = vectorX / vectorY * checkedY;
                vectorY = checkedY;
            }

            viewModel.aircraftHeadingOffsetX = vectorX;
            viewModel.aircraftHeadingOffsetY = vectorY;

        }
    }

    private void recalculateAndInvalidate() {
        mAircraftAttitudePublisher.onNext(new float[]{
                mVideoViewWidth, mVideoViewHeight,
                mPitch, mYaw, mRoll,
                mSpeedX, mSpeedY, mSpeedZ
        });
    }

    private static final class ViewModel {
        float aircraftHorizonRotate = Float.NaN;
        float aircraftHorizonOffsetY = Float.NaN;
        float aircraftHeadingOffsetX = Float.NaN;
        float aircraftHeadingOffsetY = Float.NaN;

        boolean isValidate() {
            return !Float.isNaN(aircraftHorizonRotate)
                    || !Float.isNaN(aircraftHorizonOffsetY)
                    || !Float.isNaN(aircraftHeadingOffsetX)
                    || !Float.isNaN(aircraftHeadingOffsetY);
        }
    }
}
