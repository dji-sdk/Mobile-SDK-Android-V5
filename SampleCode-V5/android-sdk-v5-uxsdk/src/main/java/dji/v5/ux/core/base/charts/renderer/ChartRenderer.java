package dji.v5.ux.core.base.charts.renderer;

import android.graphics.Canvas;

import dji.v5.ux.core.base.charts.model.SelectedValue;
import dji.v5.ux.core.base.charts.model.Viewport;

public interface ChartRenderer {
    void onChartSizeChanged();

    void onChartDataChanged();

    void onChartViewportChanged();

    void resetRenderer();

    void draw(Canvas var1);

    void drawUnclipped(Canvas var1);

    boolean checkTouch(float var1, float var2);

    boolean isTouched();

    void clearTouch();

    Viewport getMaximumViewport();

    void setMaximumViewport(Viewport var1);

    Viewport getCurrentViewport();

    void setCurrentViewport(Viewport var1);

    boolean isViewportCalculationEnabled();

    void setViewportCalculationEnabled(boolean var1);

    void selectValue(SelectedValue var1);

    SelectedValue getSelectedValue();
}

