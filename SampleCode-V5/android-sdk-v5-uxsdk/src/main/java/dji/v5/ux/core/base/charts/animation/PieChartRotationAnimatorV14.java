package dji.v5.ux.core.base.charts.animation;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;

import dji.v5.ux.core.base.charts.view.PieChartView;

@SuppressLint({"NewApi"})
public class PieChartRotationAnimatorV14 implements PieChartRotationAnimator, Animator.AnimatorListener, ValueAnimator.AnimatorUpdateListener {
    private final PieChartView chart;
    private ValueAnimator animator;
    private float startRotation;
    private float targetRotation;
    private ChartAnimationListener animationListener;

    public PieChartRotationAnimatorV14(PieChartView chart) {
        this(chart, 200L);
    }

    public PieChartRotationAnimatorV14(PieChartView chart, long duration) {
        this.startRotation = 0.0F;
        this.targetRotation = 0.0F;
        this.animationListener = new DummyChartAnimationListener();
        this.chart = chart;
        this.animator = ValueAnimator.ofFloat(0.0F, 1.0F);
        this.animator.setDuration(duration);
        this.animator.addListener(this);
        this.animator.addUpdateListener(this);
    }

    public void startAnimation(float startRotation, float targetRotation) {
        this.startRotation = (startRotation % 360.0F + 360.0F) % 360.0F;
        this.targetRotation = (targetRotation % 360.0F + 360.0F) % 360.0F;
        this.animator.start();
    }

    public void cancelAnimation() {
        this.animator.cancel();
    }

    public void onAnimationUpdate(ValueAnimator animation) {
        float scale = animation.getAnimatedFraction();
        float rotation = this.startRotation + (this.targetRotation - this.startRotation) * scale;
        rotation = (rotation % 360.0F + 360.0F) % 360.0F;
        this.chart.setChartRotation((int)rotation, false);
    }

    public void onAnimationCancel(Animator animation) {
        //do nothing
    }

    public void onAnimationEnd(Animator animation) {
        this.chart.setChartRotation((int)this.targetRotation, false);
        this.animationListener.onAnimationFinished();
    }

    public void onAnimationRepeat(Animator animation) {
        //do nothing
    }

    public void onAnimationStart(Animator animation) {
        this.animationListener.onAnimationStarted();
    }

    public boolean isAnimationStarted() {
        return this.animator.isStarted();
    }

    public void setChartAnimationListener(ChartAnimationListener animationListener) {
        if (null == animationListener) {
            this.animationListener = new DummyChartAnimationListener();
        } else {
            this.animationListener = animationListener;
        }

    }
}

