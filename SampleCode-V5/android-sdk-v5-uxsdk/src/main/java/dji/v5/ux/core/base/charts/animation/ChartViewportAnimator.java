package dji.v5.ux.core.base.charts.animation;

import dji.v5.ux.core.base.charts.model.Viewport;

public interface ChartViewportAnimator {
    int FAST_ANIMATION_DURATION = 300;

    void startAnimation(Viewport var1, Viewport var2);

    void startAnimation(Viewport var1, Viewport var2, long var3);

    void cancelAnimation();

    boolean isAnimationStarted();

    void setChartAnimationListener(ChartAnimationListener var1);
}

