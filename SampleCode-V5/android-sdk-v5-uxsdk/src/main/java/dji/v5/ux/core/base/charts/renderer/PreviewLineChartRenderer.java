package dji.v5.ux.core.base.charts.renderer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import dji.v5.ux.core.base.charts.model.Viewport;
import dji.v5.ux.core.base.charts.provider.LineChartDataProvider;
import dji.v5.ux.core.base.charts.util.ChartUtils;
import dji.v5.ux.core.base.charts.view.Chart;

public class PreviewLineChartRenderer extends LineChartRenderer {
    private Paint previewPaint = new Paint();

    public PreviewLineChartRenderer(Context context, Chart chart, LineChartDataProvider dataProvider) {
        super(context, chart, dataProvider);
        this.previewPaint.setAntiAlias(true);
        this.previewPaint.setColor(-3355444);
        this.previewPaint.setStrokeWidth((float) ChartUtils.dp2px(this.density, 2));
    }

    @Override
    public void drawUnclipped(Canvas canvas) {
        super.drawUnclipped(canvas);
        Viewport currentViewport = this.computator.getCurrentViewport();
        float left = this.computator.computeRawX(currentViewport.left);
        float top = this.computator.computeRawY(currentViewport.top);
        float right = this.computator.computeRawX(currentViewport.right);
        float bottom = this.computator.computeRawY(currentViewport.bottom);
        this.previewPaint.setAlpha(64);
        this.previewPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(left, top, right, bottom, this.previewPaint);
        this.previewPaint.setStyle(Paint.Style.STROKE);
        this.previewPaint.setAlpha(255);
        canvas.drawRect(left, top, right, bottom, this.previewPaint);
    }

    public int getPreviewColor() {
        return this.previewPaint.getColor();
    }

    public void setPreviewColor(int color) {
        this.previewPaint.setColor(color);
    }
}

