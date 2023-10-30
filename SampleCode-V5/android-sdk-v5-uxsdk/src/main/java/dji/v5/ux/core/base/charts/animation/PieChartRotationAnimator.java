package dji.v5.ux.core.base.charts.animation;

public interface PieChartRotationAnimator {
    int FAST_ANIMATION_DURATION = 200;

    void startAnimation(float var1, float var2);

    void cancelAnimation();

    boolean isAnimationStarted();

    void setChartAnimationListener(ChartAnimationListener var1);
}
