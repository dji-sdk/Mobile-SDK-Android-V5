package dji.v5.ux.core.base.charts.view;

import dji.v5.ux.core.base.charts.animation.ChartAnimationListener;
import dji.v5.ux.core.base.charts.computator.ChartComputator;
import dji.v5.ux.core.base.charts.gesture.ChartTouchHandler;
import dji.v5.ux.core.base.charts.gesture.ContainerScrollType;
import dji.v5.ux.core.base.charts.gesture.ZoomType;
import dji.v5.ux.core.base.charts.listener.ViewportChangeListener;
import dji.v5.ux.core.base.charts.model.ChartData;
import dji.v5.ux.core.base.charts.model.SelectedValue;
import dji.v5.ux.core.base.charts.model.Viewport;
import dji.v5.ux.core.base.charts.renderer.AxesRenderer;
import dji.v5.ux.core.base.charts.renderer.ChartRenderer;

public interface Chart {
    ChartData getChartData();

    ChartRenderer getChartRenderer();

    void setChartRenderer(ChartRenderer var1);

    AxesRenderer getAxesRenderer();

    ChartComputator getChartComputator();

    ChartTouchHandler getTouchHandler();

    void animationDataUpdate(float var1);

    void animationDataFinished();

    void startDataAnimation();

    void startDataAnimation(long var1);

    void cancelDataAnimation();

    boolean isViewportCalculationEnabled();

    void setViewportCalculationEnabled(boolean var1);

    void setDataAnimationListener(ChartAnimationListener var1);

    void setViewportAnimationListener(ChartAnimationListener var1);

    void setViewportChangeListener(ViewportChangeListener var1);

    void callTouchListener();

    boolean isInteractive();

    void setInteractive(boolean var1);

    boolean isZoomEnabled();

    void setZoomEnabled(boolean var1);

    boolean isScrollEnabled();

    void setScrollEnabled(boolean var1);

    void moveTo(float var1, float var2);

    void moveToWithAnimation(float var1, float var2);

    ZoomType getZoomType();

    void setZoomType(ZoomType var1);

    float getMaxZoom();

    void setMaxZoom(float var1);

    float getZoomLevel();

    void setZoomLevel(float var1, float var2, float var3);

    void setZoomLevelWithAnimation(float var1, float var2, float var3);

    boolean isValueTouchEnabled();

    void setValueTouchEnabled(boolean var1);

    Viewport getMaximumViewport();

    void setMaximumViewport(Viewport var1);

    Viewport getCurrentViewport();

    void setCurrentViewport(Viewport var1);

    void setCurrentViewportWithAnimation(Viewport var1);

    void setCurrentViewportWithAnimation(Viewport var1, long var2);

    void resetViewports();

    boolean isValueSelectionEnabled();

    void setValueSelectionEnabled(boolean var1);

    void selectValue(SelectedValue var1);

    SelectedValue getSelectedValue();

    boolean isContainerScrollEnabled();

    void setContainerScrollEnabled(boolean var1, ContainerScrollType var2);
}
