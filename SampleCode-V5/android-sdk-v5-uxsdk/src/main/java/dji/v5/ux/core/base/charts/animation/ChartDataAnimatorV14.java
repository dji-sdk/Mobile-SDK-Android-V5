package dji.v5.ux.core.base.charts.animation;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;

import dji.v5.ux.core.base.charts.view.Chart;

@SuppressLint({"NewApi"})
public class ChartDataAnimatorV14 implements ChartDataAnimator, Animator.AnimatorListener, ValueAnimator.AnimatorUpdateListener {
    private final Chart chart;
    private ValueAnimator animator;
    private ChartAnimationListener animationListener = new DummyChartAnimationListener();

    public ChartDataAnimatorV14(Chart chart) {
        this.chart = chart;
        this.animator = ValueAnimator.ofFloat(0.0F, 1.0F);
        this.animator.addListener(this);
        this.animator.addUpdateListener(this);
    }

    public void startAnimation(long duration) {
        if (duration >= 0L) {
            this.animator.setDuration(duration);
        } else {
            this.animator.setDuration(500L);
        }

        this.animator.start();
    }

    public void cancelAnimation() {
        this.animator.cancel();
    }

    public void onAnimationUpdate(ValueAnimator animation) {
        this.chart.animationDataUpdate(animation.getAnimatedFraction());
    }

    public void onAnimationCancel(Animator animation) {
        //do nothing
    }

    public void onAnimationEnd(Animator animation) {
        this.chart.animationDataFinished();
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

