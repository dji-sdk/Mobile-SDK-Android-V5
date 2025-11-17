package dji.v5.ux.core.base.charts.view;

import android.content.Context;
import android.util.AttributeSet;

import dji.v5.ux.core.base.charts.listener.ComboLineColumnChartOnValueSelectListener;
import dji.v5.ux.core.base.charts.listener.DummyCompoLineColumnChartOnValueSelectListener;
import dji.v5.ux.core.base.charts.model.ChartData;
import dji.v5.ux.core.base.charts.model.Column;
import dji.v5.ux.core.base.charts.model.ColumnChartData;
import dji.v5.ux.core.base.charts.model.ComboLineColumnChartData;
import dji.v5.ux.core.base.charts.model.Line;
import dji.v5.ux.core.base.charts.model.LineChartData;
import dji.v5.ux.core.base.charts.model.PointValue;
import dji.v5.ux.core.base.charts.model.SelectedValue;
import dji.v5.ux.core.base.charts.model.SubcolumnValue;
import dji.v5.ux.core.base.charts.provider.ColumnChartDataProvider;
import dji.v5.ux.core.base.charts.provider.ComboLineColumnChartDataProvider;
import dji.v5.ux.core.base.charts.provider.LineChartDataProvider;
import dji.v5.ux.core.base.charts.renderer.ColumnChartRenderer;
import dji.v5.ux.core.base.charts.renderer.ComboLineColumnChartRenderer;
import dji.v5.ux.core.base.charts.renderer.LineChartRenderer;

public class ComboLineColumnChartView extends AbstractChartView implements ComboLineColumnChartDataProvider {
    protected ComboLineColumnChartData data;
    protected ColumnChartDataProvider columnChartDataProvider;
    protected LineChartDataProvider lineChartDataProvider;
    protected ComboLineColumnChartOnValueSelectListener onValueTouchListener;

    public ComboLineColumnChartView(Context context) {
        this(context, (AttributeSet)null, 0);
    }

    public ComboLineColumnChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ComboLineColumnChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.columnChartDataProvider = new ComboColumnChartDataProvider();
        this.lineChartDataProvider = new ComboLineChartDataProvider();
        this.onValueTouchListener = new DummyCompoLineColumnChartOnValueSelectListener();
        this.setChartRenderer(new ComboLineColumnChartRenderer(context, this, this.columnChartDataProvider, this.lineChartDataProvider));
        this.setComboLineColumnChartData(ComboLineColumnChartData.generateDummyData());
    }

    public ComboLineColumnChartData getComboLineColumnChartData() {
        return this.data;
    }

    public void setComboLineColumnChartData(ComboLineColumnChartData data) {
        this.data = data;
        super.onChartDataChange();
    }

    public ChartData getChartData() {
        return this.data;
    }

    public void callTouchListener() {
        SelectedValue selectedValue = this.chartRenderer.getSelectedValue();
        if (selectedValue.isSet()) {
            if (SelectedValue.SelectedValueType.COLUMN.equals(selectedValue.getType())) {
                SubcolumnValue value = (SubcolumnValue)((Column)this.data.getColumnChartData().getColumns().get(selectedValue.getFirstIndex())).getValues().get(selectedValue.getSecondIndex());
                this.onValueTouchListener.onColumnValueSelected(selectedValue.getFirstIndex(), selectedValue.getSecondIndex(), value);
            } else {
                if (!SelectedValue.SelectedValueType.LINE.equals(selectedValue.getType())) {
                    throw new IllegalArgumentException("Invalid selected value type " + selectedValue.getType().name());
                }

                PointValue value = (PointValue)((Line)this.data.getLineChartData().getLines().get(selectedValue.getFirstIndex())).getValues().get(selectedValue.getSecondIndex());
                this.onValueTouchListener.onPointValueSelected(selectedValue.getFirstIndex(), selectedValue.getSecondIndex(), value);
            }
        } else {
            this.onValueTouchListener.onValueDeselected();
        }

    }

    public ComboLineColumnChartOnValueSelectListener getOnValueTouchListener() {
        return this.onValueTouchListener;
    }

    public void setOnValueTouchListener(ComboLineColumnChartOnValueSelectListener touchListener) {
        if (null != touchListener) {
            this.onValueTouchListener = touchListener;
        }

    }

    public void setColumnChartRenderer(Context context, ColumnChartRenderer columnChartRenderer) {
        this.setChartRenderer(new ComboLineColumnChartRenderer(context, this, columnChartRenderer, this.lineChartDataProvider));
    }

    public void setLineChartRenderer(Context context, LineChartRenderer lineChartRenderer) {
        this.setChartRenderer(new ComboLineColumnChartRenderer(context, this, this.columnChartDataProvider, lineChartRenderer));
    }

    private class ComboColumnChartDataProvider implements ColumnChartDataProvider {
        private ComboColumnChartDataProvider() {
        }

        public ColumnChartData getColumnChartData() {
            return ComboLineColumnChartView.this.data.getColumnChartData();
        }

        public void setColumnChartData(ColumnChartData data) {
            ComboLineColumnChartView.this.data.setColumnChartData(data);
        }
    }

    private class ComboLineChartDataProvider implements LineChartDataProvider {
        private ComboLineChartDataProvider() {
        }

        public LineChartData getLineChartData() {
            return ComboLineColumnChartView.this.data.getLineChartData();
        }

        public void setLineChartData(LineChartData data) {
            ComboLineColumnChartView.this.data.setLineChartData(data);
        }
    }
}
