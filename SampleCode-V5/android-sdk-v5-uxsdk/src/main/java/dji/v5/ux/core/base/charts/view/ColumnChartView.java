package dji.v5.ux.core.base.charts.view;

import android.content.Context;
import android.util.AttributeSet;

import dji.v5.ux.core.base.charts.listener.ColumnChartOnValueSelectListener;
import dji.v5.ux.core.base.charts.listener.DummyColumnChartOnValueSelectListener;
import dji.v5.ux.core.base.charts.model.Column;
import dji.v5.ux.core.base.charts.model.ColumnChartData;
import dji.v5.ux.core.base.charts.model.SelectedValue;
import dji.v5.ux.core.base.charts.model.SubcolumnValue;
import dji.v5.ux.core.base.charts.provider.ColumnChartDataProvider;
import dji.v5.ux.core.base.charts.renderer.ColumnChartRenderer;

public class ColumnChartView extends AbstractChartView implements ColumnChartDataProvider {
    private ColumnChartData data;
    private ColumnChartOnValueSelectListener onValueTouchListener;

    public ColumnChartView(Context context) {
        this(context, (AttributeSet)null, 0);
    }

    public ColumnChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColumnChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.onValueTouchListener = new DummyColumnChartOnValueSelectListener();
        this.setChartRenderer(new ColumnChartRenderer(context, this, this));
        this.setColumnChartData(ColumnChartData.generateDummyData());
    }

    public ColumnChartData getColumnChartData() {
        return this.data;
    }

    public void setColumnChartData(ColumnChartData data) {
        if (null == data) {
            this.data = ColumnChartData.generateDummyData();
        } else {
            this.data = data;
        }

        super.onChartDataChange();
    }

    public ColumnChartData getChartData() {
        return this.data;
    }

    public void callTouchListener() {
        SelectedValue selectedValue = this.chartRenderer.getSelectedValue();
        if (selectedValue.isSet()) {
            SubcolumnValue value = (SubcolumnValue)((Column)this.data.getColumns().get(selectedValue.getFirstIndex())).getValues().get(selectedValue.getSecondIndex());
            this.onValueTouchListener.onValueSelected(selectedValue.getFirstIndex(), selectedValue.getSecondIndex(), value);
        } else {
            this.onValueTouchListener.onValueDeselected();
        }

    }

    public ColumnChartOnValueSelectListener getOnValueTouchListener() {
        return this.onValueTouchListener;
    }

    public void setOnValueTouchListener(ColumnChartOnValueSelectListener touchListener) {
        if (null != touchListener) {
            this.onValueTouchListener = touchListener;
        }

    }
}
