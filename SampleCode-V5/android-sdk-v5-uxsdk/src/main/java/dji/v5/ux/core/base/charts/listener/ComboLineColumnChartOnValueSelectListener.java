package dji.v5.ux.core.base.charts.listener;

import dji.v5.ux.core.base.charts.model.PointValue;
import dji.v5.ux.core.base.charts.model.SubcolumnValue;

public interface ComboLineColumnChartOnValueSelectListener extends OnValueDeselectListener {
    void onColumnValueSelected(int var1, int var2, SubcolumnValue var3);

    void onPointValueSelected(int var1, int var2, PointValue var3);
}
