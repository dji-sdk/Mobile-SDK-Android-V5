package dji.v5.ux.core.base.charts.listener;

import dji.v5.ux.core.base.charts.model.BubbleValue;

public interface BubbleChartOnValueSelectListener extends OnValueDeselectListener {
    void onValueSelected(int var1, BubbleValue var2);
}
