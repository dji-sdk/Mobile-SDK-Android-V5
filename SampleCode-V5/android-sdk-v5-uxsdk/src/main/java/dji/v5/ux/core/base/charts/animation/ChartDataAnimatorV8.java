package dji.v5.ux.core.base.charts.animation;

import android.os.Handler;
import android.os.SystemClock;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import dji.v5.ux.core.base.charts.view.Chart;

public class ChartDataAnimatorV8 implements ChartDataAnimator{
    final Chart chart;
    final Handler handler;
    final Interpolator interpolator = new AccelerateDecelerateInterpolator();
    long start;
    boolean isAnimationStarted = false;
    long duration;
    private final Runnable runnable = new Runnable() {
        public void run() {
            long elapsed = SystemClock.uptimeMillis() - ChartDataAnimatorV8.this.start;
            if (elapsed > ChartDataAnimatorV8.this.duration) {
                ChartDataAnimatorV8.this.isAnimationStarted = false;
                ChartDataAnimatorV8.this.handler.removeCallbacks(ChartDataAnimatorV8.this.runnable);
                ChartDataAnimatorV8.this.chart.animationDataFinished();
            } else {
                float scale = Math.min(ChartDataAnimatorV8.this.interpolator.getInterpolation((float)elapsed / (float)ChartDataAnimatorV8.this.duration), 1.0F);
                ChartDataAnimatorV8.this.chart.animationDataUpdate(scale);
                ChartDataAnimatorV8.this.handler.postDelayed(this, 16L);
            }
        }
    };
    private ChartAnimationListener animationListener = new DummyChartAnimationListener();

    public ChartDataAnimatorV8(Chart chart) {
        this.chart = chart;
        this.handler = new Handler();
    }

    public void startAnimation(long duration) {
        if (duration >= 0L) {
            this.duration = duration;
        } else {
            this.duration = 500L;
        }

        this.isAnimationStarted = true;
        this.animationListener.onAnimationStarted();
        this.start = SystemClock.uptimeMillis();
        this.handler.post(this.runnable);
    }

    public void cancelAnimation() {
        this.isAnimationStarted = false;
        this.handler.removeCallbacks(this.runnable);
        this.chart.animationDataFinished();
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
