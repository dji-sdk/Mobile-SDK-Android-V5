package dji.v5.ux.core.base.charts.animation;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;

import dji.v5.ux.core.base.charts.model.Viewport;
import dji.v5.ux.core.base.charts.view.Chart;

@SuppressLint({"NewApi"})
public class ChartViewportAnimatorV14 implements ChartViewportAnimator, Animator.AnimatorListener, ValueAnimator.AnimatorUpdateListener {
    private final Chart chart;
    private ValueAnimator animator;
    private Viewport startViewport = new Viewport();
    private Viewport targetViewport = new Viewport();
    private Viewport newViewport = new Viewport();
    private ChartAnimationListener animationListener = new DummyChartAnimationListener();

    public ChartViewportAnimatorV14(Chart chart) {
        this.chart = chart;
        this.animator = ValueAnimator.ofFloat(0.0F, 1.0F);
        this.animator.addListener(this);
        this.animator.addUpdateListener(this);
        this.animator.setDuration(300L);
    }

    public void startAnimation(Viewport startViewport, Viewport targetViewport) {
        this.startViewport.set(startViewport);
        this.targetViewport.set(targetViewport);
        this.animator.setDuration(300L);
        this.animator.start();
    }

    public void startAnimation(Viewport startViewport, Viewport targetViewport, long duration) {
        this.startViewport.set(startViewport);
        this.targetViewport.set(targetViewport);
        this.animator.setDuration(duration);
        this.animator.start();
    }

    public void cancelAnimation() {
        this.animator.cancel();
    }

    public void onAnimationUpdate(ValueAnimator animation) {
        float scale = animation.getAnimatedFraction();
        float diffLeft = (this.targetViewport.left - this.startViewport.left) * scale;
        float diffTop = (this.targetViewport.top - this.startViewport.top) * scale;
        float diffRight = (this.targetViewport.right - this.startViewport.right) * scale;
        float diffBottom = (this.targetViewport.bottom - this.startViewport.bottom) * scale;
        this.newViewport.set(this.startViewport.left + diffLeft, this.startViewport.top + diffTop, this.startViewport.right + diffRight, this.startViewport.bottom + diffBottom);
        this.chart.setCurrentViewport(this.newViewport);
    }

    public void onAnimationCancel(Animator animation) {
        //do nothing
    }

    public void onAnimationEnd(Animator animation) {
        this.chart.setCurrentViewport(this.targetViewport);
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
