package dji.v5.ux.core.base.charts.listener;

import dji.v5.ux.core.base.charts.model.PointValue;

public class DummyLineChartOnValueSelectListener implements LineChartOnValueSelectListener {
    public DummyLineChartOnValueSelectListener() {
        //do nothing
    }

    public void onValueSelected(int lineIndex, int pointIndex, PointValue value) {
        //do nothing
    }

    public void onValueDeselected() {
        //do nothing
    }
}
