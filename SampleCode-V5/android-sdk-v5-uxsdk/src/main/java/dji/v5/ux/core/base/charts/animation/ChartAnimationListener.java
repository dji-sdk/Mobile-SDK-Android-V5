package dji.v5.ux.core.base.charts.animation;

import java.util.EventListener;

public interface ChartAnimationListener extends EventListener {
    void onAnimationStarted();

    void onAnimationFinished();
}
