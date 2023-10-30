package dji.v5.ux.core.base.charts.model;

public class ComboLineColumnChartData extends AbstractChartData {
    private ColumnChartData columnChartData;
    private LineChartData lineChartData;

    public ComboLineColumnChartData() {
        this.columnChartData = new ColumnChartData();
        this.lineChartData = new LineChartData();
    }

    public ComboLineColumnChartData(ColumnChartData columnChartData, LineChartData lineChartData) {
        this.setColumnChartData(columnChartData);
        this.setLineChartData(lineChartData);
    }

    public ComboLineColumnChartData(ComboLineColumnChartData data) {
        super(data);
        this.setColumnChartData(new ColumnChartData(data.getColumnChartData()));
        this.setLineChartData(new LineChartData(data.getLineChartData()));
    }

    public static ComboLineColumnChartData generateDummyData() {
        ComboLineColumnChartData data = new ComboLineColumnChartData();
        data.setColumnChartData(ColumnChartData.generateDummyData());
        data.setLineChartData(LineChartData.generateDummyData());
        return data;
    }

    public void update(float scale) {
        this.columnChartData.update(scale);
        this.lineChartData.update(scale);
    }

    public void finish() {
        this.columnChartData.finish();
        this.lineChartData.finish();
    }

    public ColumnChartData getColumnChartData() {
        return this.columnChartData;
    }

    public void setColumnChartData(ColumnChartData columnChartData) {
        if (null == columnChartData) {
            this.columnChartData = new ColumnChartData();
        } else {
            this.columnChartData = columnChartData;
        }

    }

    public LineChartData getLineChartData() {
        return this.lineChartData;
    }

    public void setLineChartData(LineChartData lineChartData) {
        if (null == lineChartData) {
            this.lineChartData = new LineChartData();
        } else {
            this.lineChartData = lineChartData;
        }

    }
}
