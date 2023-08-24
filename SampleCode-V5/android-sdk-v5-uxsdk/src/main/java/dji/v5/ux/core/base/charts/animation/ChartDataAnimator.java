package dji.v5.ux.core.base.charts.animation;

public interface ChartDataAnimator {
    long DEFAULT_ANIMATION_DURATION = 500L;

    void startAnimation(long var1);

    void cancelAnimation();

    boolean isAnimationStarted();

    void setChartAnimationListener(ChartAnimationListener var1);
}
