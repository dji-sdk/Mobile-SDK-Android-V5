package dji.v5.ux.core.base.charts.listener;

import dji.v5.ux.core.base.charts.model.PointValue;
import dji.v5.ux.core.base.charts.model.SubcolumnValue;

public class DummyCompoLineColumnChartOnValueSelectListener implements ComboLineColumnChartOnValueSelectListener {
    public DummyCompoLineColumnChartOnValueSelectListener() {
        //do nothing
    }

    public void onColumnValueSelected(int columnIndex, int subcolumnIndex, SubcolumnValue value) {
        //do nothing
    }

    public void onPointValueSelected(int lineIndex, int pointIndex, PointValue value) {
        //do nothing
    }

    public void onValueDeselected() {
        //do nothing
    }
}
