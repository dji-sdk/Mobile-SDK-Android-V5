package dji.v5.ux.core.base.charts.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ColumnChartData extends AbstractChartData {
    private float fillRatio = 0.75F;
    private float baseValue = 0.0F;
    private List<Column> columns = new ArrayList<>();
    private boolean isStacked = false;

    public ColumnChartData() {
    }

    public ColumnChartData(List<Column> columns) {
        this.setColumns(columns);
    }

    public ColumnChartData(ColumnChartData data) {
        super(data);
        this.isStacked = data.isStacked;
        this.fillRatio = data.fillRatio;
        Iterator<Column> var2 = data.columns.iterator();

        while(var2.hasNext()) {
            Column column = (Column)var2.next();
            this.columns.add(new Column(column));
        }

    }

    public static ColumnChartData generateDummyData() {
        ColumnChartData data = new ColumnChartData();
        List<Column> columns = new ArrayList<>(4);

        for(int i = 1; i <= 4; ++i) {
            List<SubcolumnValue> values = new ArrayList<>(4);
            values.add(new SubcolumnValue(i));
            Column column = new Column(values);
            columns.add(column);
        }

        data.setColumns(columns);
        return data;
    }

    public void update(float scale) {
        Iterator<Column> var2 = this.columns.iterator();

        while(var2.hasNext()) {
            Column column = (Column)var2.next();
            column.update(scale);
        }

    }

    public void finish() {
        Iterator<Column> var1 = this.columns.iterator();

        while(var1.hasNext()) {
            Column column = (Column)var1.next();
            column.finish();
        }

    }

    public List<Column> getColumns() {
        return this.columns;
    }

    public ColumnChartData setColumns(List<Column> columns) {
        if (null == columns) {
            this.columns = new ArrayList<>();
        } else {
            this.columns = columns;
        }

        return this;
    }

    public boolean isStacked() {
        return this.isStacked;
    }

    public ColumnChartData setStacked(boolean isStacked) {
        this.isStacked = isStacked;
        return this;
    }

    public float getFillRatio() {
        return this.fillRatio;
    }

    public ColumnChartData setFillRatio(float fillRatio) {
        if (fillRatio < 0.0F) {
            fillRatio = 0.0F;
        }

        if (fillRatio > 1.0F) {
            fillRatio = 1.0F;
        }

        this.fillRatio = fillRatio;
        return this;
    }

    public float getBaseValue() {
        return this.baseValue;
    }

    public ColumnChartData setBaseValue(float baseValue) {
        this.baseValue = baseValue;
        return this;
    }
}

