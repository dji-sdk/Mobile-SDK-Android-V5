package dji.v5.ux.core.base.charts.listener;

import dji.v5.ux.core.base.charts.model.PointValue;

public interface LineChartOnValueSelectListener extends OnValueDeselectListener {
    void onValueSelected(int var1, int var2, PointValue var3);
}
