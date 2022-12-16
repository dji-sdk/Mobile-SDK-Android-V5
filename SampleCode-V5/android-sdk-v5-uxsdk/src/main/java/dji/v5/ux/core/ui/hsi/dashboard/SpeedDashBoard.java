package dji.v5.ux.core.ui.hsi.dashboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import dji.v5.ux.R;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.common.utils.UnitUtils;
import dji.v5.ux.core.widget.hsi.SpeedDisplayModel;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class SpeedDashBoard extends ScrollableAttributeDashBoard {

    private float mSpeedX;
    private float mSpeedY;
    private float mSpeed;
    private SpeedDisplayModel widgetModel;


    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    public SpeedDashBoard(Context context) {
        this(context, null);
    }

    public SpeedDashBoard(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpeedDashBoard(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mWaypointIcon = ContextCompat.getDrawable(getContext(), R.drawable.uxsdk_fpv_pfd_waypoint_left);
    }
    public void setModel(SpeedDisplayModel model) {
        widgetModel = model;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isInEditMode()) {
            return;
        }

        mCompositeDisposable.add(widgetModel.getVelocityProcessor().toFlowable().observeOn(SchedulerProvider.ui())
                .subscribe(speed -> {
                    mSpeedX = speed.getX().floatValue();
                    mSpeedY = speed.getY().floatValue();
                    mSpeed = updateSpeed();
                    setCurrentValue(mSpeed);

                }));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 绘制航点速度
        drawSpeedIndicator(canvas);
    }


    /**
     * 绘制速度标识和文字
     */
    private void drawSpeedIndicator(Canvas canvas) {
//        MissionManagerDelegate instance = MissionManagerDelegate.INSTANCE;
//        if (!instance.isRunningMission()) {
//            return;
//        }
//        MissionExecutePointInfo pointInfo = MissionManagerDelegate.INSTANCE.getExecutePointInfo();
//        if (pointInfo == null) {
//            return;
//        }
        Drawable icon = mWaypointIcon;
        float speed = 23;
        float current = getCurrentValue();
        float delta = speed - current;
        int frameworkHeight = getFrameworkHeight();
        float ratio = (float) frameworkHeight / mVisibleCalibrationUnitCount / mAttributeOffsetPerUnit;
        float y = delta * ratio;
        if (Math.abs(y) >= getFrameworkHeight() / 2f) {
            return;
        }
        canvas.save();

        // 绘制 icon
        int iconWidth = icon.getMinimumWidth();
        int iconHeight = icon.getMinimumHeight();
        int left = Math.round(getWidth() - mFrameworkPaddingStart + mWaypointIconPadding);
        int top = Math.round(-y - icon.getMinimumHeight() / 2f + getHeight() / 2f);
        int right = left + iconWidth;
        int bottom = top + iconHeight;
        icon.setBounds(left, top, right, bottom);
        icon.draw(canvas);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mCompositeDisposable.dispose();

    }

    private float updateSpeed() {
        double tmp = Math.pow(mSpeedX, 2) + Math.pow(mSpeedY, 2);
        if (tmp < 0) {
            tmp = 0;
        }
        return (float) Math.sqrt(tmp);
    }

    @Override
    protected String getCurrentValueDisplayFormat(boolean shorthand) {
        return "%04.1f";
    }

    @Override
    protected String getAttributeUnit() {
        return UnitUtils.getSpeedUnit();
    }

    @Override
    protected float getDisplayValue(float value) {
        return UnitUtils.transFormSpeedIntoDifferentUnit(value);
    }
}
