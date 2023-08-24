package dji.v5.ux.core.base.charts.renderer;

import android.content.Context;
import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dji.v5.ux.core.base.charts.model.Viewport;
import dji.v5.ux.core.base.charts.view.Chart;

public class ComboChartRenderer extends AbstractChartRenderer {
    protected List<ChartRenderer> renderers = new ArrayList<>();
    protected Viewport unionViewport = new Viewport();

    public ComboChartRenderer(Context context, Chart chart) {
        super(context, chart);
    }

    public void onChartSizeChanged() {
        Iterator<ChartRenderer> var1 = this.renderers.iterator();

        while(var1.hasNext()) {
            ChartRenderer renderer = (ChartRenderer)var1.next();
            renderer.onChartSizeChanged();
        }

    }

    @Override
    public void onChartDataChanged() {
        super.onChartDataChanged();
        Iterator<ChartRenderer> var1 = this.renderers.iterator();

        while(var1.hasNext()) {
            ChartRenderer renderer = (ChartRenderer)var1.next();
            renderer.onChartDataChanged();
        }

        this.onChartViewportChanged();
    }

    public void onChartViewportChanged() {
        if (this.isViewportCalculationEnabled) {
            int rendererIndex = 0;

            for(Iterator<ChartRenderer> var2 = this.renderers.iterator(); var2.hasNext(); ++rendererIndex) {
                ChartRenderer renderer = (ChartRenderer)var2.next();
                renderer.onChartViewportChanged();
                if (rendererIndex == 0) {
                    this.unionViewport.set(renderer.getMaximumViewport());
                } else {
                    this.unionViewport.union(renderer.getMaximumViewport());
                }
            }

            this.computator.setMaxViewport(this.unionViewport);
            this.computator.setCurrentViewport(this.unionViewport);
        }

    }

    public void draw(Canvas canvas) {
        Iterator<ChartRenderer> var2 = this.renderers.iterator();

        while(var2.hasNext()) {
            ChartRenderer renderer = (ChartRenderer)var2.next();
            renderer.draw(canvas);
        }

    }

    public void drawUnclipped(Canvas canvas) {
        Iterator<ChartRenderer> var2 = this.renderers.iterator();

        while(var2.hasNext()) {
            ChartRenderer renderer = (ChartRenderer)var2.next();
            renderer.drawUnclipped(canvas);
        }

    }

    public boolean checkTouch(float touchX, float touchY) {
        this.selectedValue.clear();

        int rendererIndex;
        ChartRenderer renderer;
        for(rendererIndex = this.renderers.size() - 1; rendererIndex >= 0; --rendererIndex) {
            renderer = (ChartRenderer)this.renderers.get(rendererIndex);
            if (renderer.checkTouch(touchX, touchY)) {
                this.selectedValue.set(renderer.getSelectedValue());
                break;
            }
        }

        --rendererIndex;

        while(rendererIndex >= 0) {
            renderer = (ChartRenderer)this.renderers.get(rendererIndex);
            renderer.clearTouch();
            --rendererIndex;
        }

        return this.isTouched();
    }

    @Override
    public void clearTouch() {
        Iterator<ChartRenderer> var1 = this.renderers.iterator();

        while(var1.hasNext()) {
            ChartRenderer renderer = (ChartRenderer)var1.next();
            renderer.clearTouch();
        }

        this.selectedValue.clear();
    }
}

