package dji.v5.ux.core.base.charts.listener;

import dji.v5.ux.core.base.charts.model.SubcolumnValue;

public class DummyColumnChartOnValueSelectListener implements ColumnChartOnValueSelectListener {
    public DummyColumnChartOnValueSelectListener() {
        //do nothing
    }

    public void onValueSelected(int columnIndex, int subcolumnIndex, SubcolumnValue value) {
        //do nothing
    }

    public void onValueDeselected() {
        //do nothing
    }
}
