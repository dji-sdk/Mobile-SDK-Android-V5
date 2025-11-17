package dji.v5.ux.core.base.charts.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.core.view.ViewCompat;
import dji.v5.ux.core.base.charts.computator.PreviewChartComputator;
import dji.v5.ux.core.base.charts.gesture.PreviewChartTouchHandler;
import dji.v5.ux.core.base.charts.model.ColumnChartData;
import dji.v5.ux.core.base.charts.renderer.PreviewColumnChartRenderer;

public class PreviewColumnChartView extends ColumnChartView {
    protected PreviewColumnChartRenderer previewChartRenderer;

    public PreviewColumnChartView(Context context) {
        this(context, (AttributeSet)null, 0);
    }

    public PreviewColumnChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreviewColumnChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.chartComputator = new PreviewChartComputator();
        this.previewChartRenderer = new PreviewColumnChartRenderer(context, this, this);
        this.touchHandler = new PreviewChartTouchHandler(context, this);
        this.setChartRenderer(this.previewChartRenderer);
        this.setColumnChartData(ColumnChartData.generateDummyData());
    }

    public int getPreviewColor() {
        return this.previewChartRenderer.getPreviewColor();
    }

    public void setPreviewColor(int color) {
        this.previewChartRenderer.setPreviewColor(color);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        int offset = this.computeHorizontalScrollOffset();
        int range = this.computeHorizontalScrollRange() - this.computeHorizontalScrollExtent();
        if (range == 0) {
            return false;
        } else if (direction < 0) {
            return offset > 0;
        } else {
            return offset < range - 1;
        }
    }
}
