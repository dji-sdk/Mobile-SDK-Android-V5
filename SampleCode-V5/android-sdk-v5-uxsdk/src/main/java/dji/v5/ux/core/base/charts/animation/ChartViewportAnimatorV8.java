package dji.v5.ux.core.base.charts.animation;

import android.os.Handler;
import android.os.SystemClock;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import dji.v5.ux.core.base.charts.model.Viewport;
import dji.v5.ux.core.base.charts.view.Chart;

public class ChartViewportAnimatorV8 implements ChartViewportAnimator {
    final Chart chart;
    final Handler handler;
    final Interpolator interpolator = new AccelerateDecelerateInterpolator();
    long start;
    boolean isAnimationStarted = false;
    private Viewport startViewport = new Viewport();
    private Viewport targetViewport = new Viewport();
    private Viewport newViewport = new Viewport();
    private long duration;
    private ChartAnimationListener animationListener = new DummyChartAnimationListener();
    private final Runnable runnable = new Runnable() {
        public void run() {
            long elapsed = SystemClock.uptimeMillis() - ChartViewportAnimatorV8.this.start;
            if (elapsed > ChartViewportAnimatorV8.this.duration) {
                ChartViewportAnimatorV8.this.isAnimationStarted = false;
                ChartViewportAnimatorV8.this.handler.removeCallbacks(ChartViewportAnimatorV8.this.runnable);
                ChartViewportAnimatorV8.this.chart.setCurrentViewport(ChartViewportAnimatorV8.this.targetViewport);
                ChartViewportAnimatorV8.this.animationListener.onAnimationFinished();
            } else {
                float scale = Math.min(ChartViewportAnimatorV8.this.interpolator.getInterpolation((float)elapsed / (float)ChartViewportAnimatorV8.this.duration), 1.0F);
                float diffLeft = (ChartViewportAnimatorV8.this.targetViewport.left - ChartViewportAnimatorV8.this.startViewport.left) * scale;
                float diffTop = (ChartViewportAnimatorV8.this.targetViewport.top - ChartViewportAnimatorV8.this.startViewport.top) * scale;
                float diffRight = (ChartViewportAnimatorV8.this.targetViewport.right - ChartViewportAnimatorV8.this.startViewport.right) * scale;
                float diffBottom = (ChartViewportAnimatorV8.this.targetViewport.bottom - ChartViewportAnimatorV8.this.startViewport.bottom) * scale;
                ChartViewportAnimatorV8.this.newViewport.set(ChartViewportAnimatorV8.this.startViewport.left + diffLeft, ChartViewportAnimatorV8.this.startViewport.top + diffTop, ChartViewportAnimatorV8.this.startViewport.right + diffRight, ChartViewportAnimatorV8.this.startViewport.bottom + diffBottom);
                ChartViewportAnimatorV8.this.chart.setCurrentViewport(ChartViewportAnimatorV8.this.newViewport);
                ChartViewportAnimatorV8.this.handler.postDelayed(this, 16L);
            }
        }
    };

    public ChartViewportAnimatorV8(Chart chart) {
        this.chart = chart;
        this.duration = 300L;
        this.handler = new Handler();
    }

    public void startAnimation(Viewport startViewport, Viewport targetViewport) {
        this.startViewport.set(startViewport);
        this.targetViewport.set(targetViewport);
        this.duration = 300L;
        this.isAnimationStarted = true;
        this.animationListener.onAnimationStarted();
        this.start = SystemClock.uptimeMillis();
        this.handler.post(this.runnable);
    }

    public void startAnimation(Viewport startViewport, Viewport targetViewport, long duration) {
        this.startViewport.set(startViewport);
        this.targetViewport.set(targetViewport);
        this.duration = duration;
        this.isAnimationStarted = true;
        this.animationListener.onAnimationStarted();
        this.start = SystemClock.uptimeMillis();
        this.handler.post(this.runnable);
    }

    public void cancelAnimation() {
        this.isAnimationStarted = false;
        this.handler.removeCallbacks(this.runnable);
        this.chart.setCurrentViewport(this.targetViewport);
        this.animationListener.onAnimationFinished();
    }

    public boolean isAnimationStarted() {
        return this.isAnimationStarted;
    }

    public void setChartAnimationListener(ChartAnimationListener animationListener) {
        if (null == animationListener) {
            this.animationListener = new DummyChartAnimationListener();
        } else {
            this.animationListener = animationListener;
        }

    }
}

