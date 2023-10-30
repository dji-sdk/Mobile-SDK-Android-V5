package dji.v5.ux.core.base.charts.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dji.v5.ux.core.base.charts.formatter.ColumnChartValueFormatter;
import dji.v5.ux.core.base.charts.formatter.SimpleColumnChartValueFormatter;

public class Column {
    private boolean hasLabels = false;
    private boolean hasLabelsOnlyForSelected = false;
    private ColumnChartValueFormatter formatter = new SimpleColumnChartValueFormatter();
    private List<SubcolumnValue> values = new ArrayList<>();

    public Column() {
    }

    public Column(List<SubcolumnValue> values) {
        this.setValues(values);
    }

    public Column(Column column) {
        this.hasLabels = column.hasLabels;
        this.hasLabelsOnlyForSelected = column.hasLabelsOnlyForSelected;
        this.formatter = column.formatter;
        Iterator<SubcolumnValue> var2 = column.values.iterator();

        while(var2.hasNext()) {
            SubcolumnValue columnValue = (SubcolumnValue)var2.next();
            this.values.add(new SubcolumnValue(columnValue));
        }

    }

    public void update(float scale) {
        Iterator<SubcolumnValue> var2 = this.values.iterator();

        while(var2.hasNext()) {
            SubcolumnValue value = (SubcolumnValue)var2.next();
            value.update(scale);
        }

    }

    public void finish() {
        Iterator<SubcolumnValue> var1 = this.values.iterator();

        while(var1.hasNext()) {
            SubcolumnValue value = (SubcolumnValue)var1.next();
            value.finish();
        }

    }

    public List<SubcolumnValue> getValues() {
        return this.values;
    }

    public Column setValues(List<SubcolumnValue> values) {
        if (null == values) {
            this.values = new ArrayList<>();
        } else {
            this.values = values;
        }

        return this;
    }

    public boolean hasLabels() {
        return this.hasLabels;
    }

    public Column setHasLabels(boolean hasLabels) {
        this.hasLabels = hasLabels;
        if (hasLabels) {
            this.hasLabelsOnlyForSelected = false;
        }

        return this;
    }

    public boolean hasLabelsOnlyForSelected() {
        return this.hasLabelsOnlyForSelected;
    }

    public Column setHasLabelsOnlyForSelected(boolean hasLabelsOnlyForSelected) {
        this.hasLabelsOnlyForSelected = hasLabelsOnlyForSelected;
        if (hasLabelsOnlyForSelected) {
            this.hasLabels = false;
        }

        return this;
    }

    public ColumnChartValueFormatter getFormatter() {
        return this.formatter;
    }

    public Column setFormatter(ColumnChartValueFormatter formatter) {
        if (null != formatter) {
            this.formatter = formatter;
        }

        return this;
    }
}
