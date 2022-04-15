package dji.v5.ux.core.ui.hsi.dashboard;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.ux.core.util.UnitUtils;
import dji.v5.ux.core.widget.hsi.SpeedDisplayModel;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class SpeedDashBoard extends ScrollableAttributeDashBoard {

    private float mSpeedX;
    private float mSpeedY;
    private SpeedDisplayModel widgetModel;

    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    public SpeedDashBoard(Context context) {
        this(context, null);
    }

    public SpeedDashBoard(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpeedDashBoard(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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
                    updateSpeed();
                }));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mCompositeDisposable.dispose();
    }

    private void updateSpeed() {
        double tmp = Math.pow(mSpeedX, 2) + Math.pow(mSpeedY, 2);
        if (tmp < 0) {
            tmp = 0;
        }
        setCurrentValue((float) Math.sqrt(tmp));
    }

    @Override
    protected String getCurrentValueDisplayFormat() {
        return "%04.1f";
    }

    @Override
    protected String getAttributeUnit() {
        if (isInEditMode()){
            return "m/s";
        }
        return UnitUtils.getSpeedUnit();
    }

    @Override
    protected float getDisplayValue(float value) {
        if (isInEditMode()){
            return 0F;
        }
        return UnitUtils.transFormSpeedIntoDifferentUnit(value);
    }
}
