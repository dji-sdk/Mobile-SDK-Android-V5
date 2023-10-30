package dji.v5.ux.core.base.charts.provider;

import dji.v5.ux.core.base.charts.model.LineChartData;

public interface LineChartDataProvider {
    LineChartData getLineChartData();

    void setLineChartData(LineChartData var1);
}
