package dji.v5.ux.core.base.charts.listener;

import dji.v5.ux.core.base.charts.model.SubcolumnValue;

public interface ColumnChartOnValueSelectListener extends OnValueDeselectListener {
    void onValueSelected(int var1, int var2, SubcolumnValue var3);
}
