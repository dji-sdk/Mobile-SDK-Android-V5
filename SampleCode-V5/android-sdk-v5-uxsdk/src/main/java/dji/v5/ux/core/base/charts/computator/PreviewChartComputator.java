package dji.v5.ux.core.base.charts.computator;

import dji.v5.ux.core.base.charts.model.Viewport;

public class PreviewChartComputator extends ChartComputator {
    public PreviewChartComputator() {
        //do nothing
    }

    @Override
    public float computeRawX(float valueX) {
        float pixelOffset = (valueX - this.maxViewport.left) * (this.contentRectMinusAllMargins.width() / this.maxViewport.width());
        return this.contentRectMinusAllMargins.left + pixelOffset;
    }

    @Override
    public float computeRawY(float valueY) {
        float pixelOffset = (valueY - this.maxViewport.bottom) * (this.contentRectMinusAllMargins.height() / this.maxViewport.height());
        return this.contentRectMinusAllMargins.bottom - pixelOffset;
    }

    @Override
    public Viewport getVisibleViewport() {
        return this.maxViewport;
    }

    @Override
    public void setVisibleViewport(Viewport visibleViewport) {
        this.setMaxViewport(visibleViewport);
    }

    @Override
    public void constrainViewport(float left, float top, float right, float bottom) {
        super.constrainViewport(left, top, right, bottom);
        this.viewportChangeListener.onViewportChanged(this.currentViewport);
    }
}
