package dji.v5.ux.core.base.charts.view;

import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.core.view.ViewCompat;
import dji.v5.ux.core.base.charts.animation.PieChartRotationAnimator;
import dji.v5.ux.core.base.charts.animation.PieChartRotationAnimatorV14;
import dji.v5.ux.core.base.charts.gesture.PieChartTouchHandler;
import dji.v5.ux.core.base.charts.listener.DummyPieChartOnValueSelectListener;
import dji.v5.ux.core.base.charts.listener.PieChartOnValueSelectListener;
import dji.v5.ux.core.base.charts.model.ChartData;
import dji.v5.ux.core.base.charts.model.PieChartData;
import dji.v5.ux.core.base.charts.model.SelectedValue;
import dji.v5.ux.core.base.charts.model.SliceValue;
import dji.v5.ux.core.base.charts.provider.PieChartDataProvider;
import dji.v5.ux.core.base.charts.renderer.PieChartRenderer;

public class PieChartView extends AbstractChartView implements PieChartDataProvider {
    protected PieChartData data;
    protected PieChartOnValueSelectListener onValueTouchListener;
    protected PieChartRenderer pieChartRenderer;
    protected PieChartRotationAnimator rotationAnimator;

    public PieChartView(Context context) {
        this(context, (AttributeSet) null, 0);
    }

    public PieChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.onValueTouchListener = new DummyPieChartOnValueSelectListener();
        this.pieChartRenderer = new PieChartRenderer(context, this, this);
        this.touchHandler = new PieChartTouchHandler(context, this);
        this.setChartRenderer(this.pieChartRenderer);
        this.rotationAnimator = new PieChartRotationAnimatorV14(this);
        this.setPieChartData(PieChartData.generateDummyData());
    }

    public PieChartData getPieChartData() {
        return this.data;
    }

    public void setPieChartData(PieChartData data) {
        if (null == data) {
            this.data = PieChartData.generateDummyData();
        } else {
            this.data = data;
        }

        super.onChartDataChange();
    }

    public ChartData getChartData() {
        return this.data;
    }

    public void callTouchListener() {
        SelectedValue selectedValue = this.chartRenderer.getSelectedValue();
        if (selectedValue.isSet()) {
            SliceValue sliceValue = (SliceValue) this.data.getValues().get(selectedValue.getFirstIndex());
            this.onValueTouchListener.onValueSelected(selectedValue.getFirstIndex(), sliceValue);
        } else {
            this.onValueTouchListener.onValueDeselected();
        }

    }

    public PieChartOnValueSelectListener getOnValueTouchListener() {
        return this.onValueTouchListener;
    }

    public void setOnValueTouchListener(PieChartOnValueSelectListener touchListener) {
        if (null != touchListener) {
            this.onValueTouchListener = touchListener;
        }

    }

    public RectF getCircleOval() {
        return this.pieChartRenderer.getCircleOval();
    }

    public void setCircleOval(RectF orginCircleOval) {
        this.pieChartRenderer.setCircleOval(orginCircleOval);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public int getChartRotation() {
        return this.pieChartRenderer.getChartRotation();
    }

    public void setChartRotation(int rotation, boolean isAnimated) {
        if (isAnimated) {
            this.rotationAnimator.cancelAnimation();
            this.rotationAnimator.startAnimation((float) this.pieChartRenderer.getChartRotation(), (float) rotation);
        } else {
            this.pieChartRenderer.setChartRotation(rotation);
        }

        ViewCompat.postInvalidateOnAnimation(this);
    }

    public boolean isChartRotationEnabled() {
        return this.touchHandler instanceof PieChartTouchHandler ? ((PieChartTouchHandler) this.touchHandler).isRotationEnabled() : false;
    }

    public void setChartRotationEnabled(boolean isRotationEnabled) {
        if (this.touchHandler instanceof PieChartTouchHandler) {
            ((PieChartTouchHandler) this.touchHandler).setRotationEnabled(isRotationEnabled);
        }

    }

    public SliceValue getValueForAngle(int angle, SelectedValue selectedValue) {
        return this.pieChartRenderer.getValueForAngle(angle, selectedValue);
    }

    public float getCircleFillRatio() {
        return this.pieChartRenderer.getCircleFillRatio();
    }

    public void setCircleFillRatio(float fillRatio) {
        this.pieChartRenderer.setCircleFillRatio(fillRatio);
        ViewCompat.postInvalidateOnAnimation(this);
    }
}
