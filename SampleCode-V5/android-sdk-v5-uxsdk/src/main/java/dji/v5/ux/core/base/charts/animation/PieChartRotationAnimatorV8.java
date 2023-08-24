package dji.v5.ux.core.base.charts.animation;

import android.os.Handler;
import android.os.SystemClock;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import dji.v5.ux.core.base.charts.view.PieChartView;

public class PieChartRotationAnimatorV8 implements PieChartRotationAnimator {
    final PieChartView chart;
    final long duration;
    final Handler handler;
    final Interpolator interpolator;
    long start;
    boolean isAnimationStarted;
    private float startRotation;
    private float targetRotation;
    private ChartAnimationListener animationListener;
    private final Runnable runnable;

    public PieChartRotationAnimatorV8(PieChartView chart) {
        this(chart, 200L);
    }

    public PieChartRotationAnimatorV8(PieChartView chart, long duration) {
        this.interpolator = new AccelerateDecelerateInterpolator();
        this.isAnimationStarted = false;
        this.startRotation = 0.0F;
        this.targetRotation = 0.0F;
        this.animationListener = new DummyChartAnimationListener();
        this.runnable = new Runnable() {
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - PieChartRotationAnimatorV8.this.start;
                if (elapsed > PieChartRotationAnimatorV8.this.duration) {
                    PieChartRotationAnimatorV8.this.isAnimationStarted = false;
                    PieChartRotationAnimatorV8.this.handler.removeCallbacks(PieChartRotationAnimatorV8.this.runnable);
                    PieChartRotationAnimatorV8.this.chart.setChartRotation((int)PieChartRotationAnimatorV8.this.targetRotation, false);
                    PieChartRotationAnimatorV8.this.animationListener.onAnimationFinished();
                } else {
                    float scale = Math.min(PieChartRotationAnimatorV8.this.interpolator.getInterpolation((float)elapsed / (float)PieChartRotationAnimatorV8.this.duration), 1.0F);
                    float rotation = PieChartRotationAnimatorV8.this.startRotation + (PieChartRotationAnimatorV8.this.targetRotation - PieChartRotationAnimatorV8.this.startRotation) * scale;
                    rotation = (rotation % 360.0F + 360.0F) % 360.0F;
                    PieChartRotationAnimatorV8.this.chart.setChartRotation((int)rotation, false);
                    PieChartRotationAnimatorV8.this.handler.postDelayed(this, 16L);
                }
            }
        };
        this.chart = chart;
        this.duration = duration;
        this.handler = new Handler();
    }

    public void startAnimation(float startRotation, float targetRotation) {
        this.startRotation = (startRotation % 360.0F + 360.0F) % 360.0F;
        this.targetRotation = (targetRotation % 360.0F + 360.0F) % 360.0F;
        this.isAnimationStarted = true;
        this.animationListener.onAnimationStarted();
        this.start = SystemClock.uptimeMillis();
        this.handler.post(this.runnable);
    }

    public void cancelAnimation() {
        this.isAnimationStarted = false;
        this.handler.removeCallbacks(this.runnable);
        this.chart.setChartRotation((int)this.targetRotation, false);
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

