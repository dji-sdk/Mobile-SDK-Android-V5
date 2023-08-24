package dji.v5.ux.core.base.charts.listener;

import dji.v5.ux.core.base.charts.model.SliceValue;

public interface PieChartOnValueSelectListener extends OnValueDeselectListener {
    void onValueSelected(int var1, SliceValue var2);
}
