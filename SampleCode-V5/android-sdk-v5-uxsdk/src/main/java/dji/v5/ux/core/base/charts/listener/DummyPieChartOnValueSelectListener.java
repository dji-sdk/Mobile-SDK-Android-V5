package dji.v5.ux.core.base.charts.listener;

import dji.v5.ux.core.base.charts.model.SliceValue;

public class DummyPieChartOnValueSelectListener implements PieChartOnValueSelectListener {
    public DummyPieChartOnValueSelectListener() {
        //do nothing
    }

    public void onValueSelected(int arcIndex, SliceValue value) {
        //do nothing
    }

    public void onValueDeselected() {
        //do nothing
    }
}
