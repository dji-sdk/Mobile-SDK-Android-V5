package dji.v5.ux.core.base.charts.renderer;

import android.content.Context;

import dji.v5.ux.core.base.charts.provider.ColumnChartDataProvider;
import dji.v5.ux.core.base.charts.provider.LineChartDataProvider;
import dji.v5.ux.core.base.charts.view.Chart;

public class ComboLineColumnChartRenderer extends ComboChartRenderer {
    private ColumnChartRenderer columnChartRenderer;
    private LineChartRenderer lineChartRenderer;

    public ComboLineColumnChartRenderer(Context context, Chart chart, ColumnChartDataProvider columnChartDataProvider, LineChartDataProvider lineChartDataProvider) {
        this(context, chart, new ColumnChartRenderer(context, chart, columnChartDataProvider), new LineChartRenderer(context, chart, lineChartDataProvider));
    }

    public ComboLineColumnChartRenderer(Context context, Chart chart, ColumnChartRenderer columnChartRenderer, LineChartDataProvider lineChartDataProvider) {
        this(context, chart, columnChartRenderer, new LineChartRenderer(context, chart, lineChartDataProvider));
    }

    public ComboLineColumnChartRenderer(Context context, Chart chart, ColumnChartDataProvider columnChartDataProvider, LineChartRenderer lineChartRenderer) {
        this(context, chart, new ColumnChartRenderer(context, chart, columnChartDataProvider), lineChartRenderer);
    }

    public ComboLineColumnChartRenderer(Context context, Chart chart, ColumnChartRenderer columnChartRenderer, LineChartRenderer lineChartRenderer) {
        super(context, chart);
        this.columnChartRenderer = columnChartRenderer;
        this.lineChartRenderer = lineChartRenderer;
        this.renderers.add(this.columnChartRenderer);
        this.renderers.add(this.lineChartRenderer);
    }
}
