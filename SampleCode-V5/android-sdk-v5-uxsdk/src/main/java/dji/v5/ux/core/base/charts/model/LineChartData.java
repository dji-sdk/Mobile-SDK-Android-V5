package dji.v5.ux.core.base.charts.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LineChartData extends AbstractChartData {
    private List<Line> lines = new ArrayList<>();
    private float baseValue = 0.0F;

    public LineChartData() {
    }

    public LineChartData(List<Line> lines) {
        this.setLines(lines);
    }

    public LineChartData(LineChartData data) {
        super(data);
        this.baseValue = data.baseValue;
        Iterator<Line> var2 = data.lines.iterator();

        while(var2.hasNext()) {
            Line line = (Line)var2.next();
            this.lines.add(new Line(line));
        }

    }

    public static LineChartData generateDummyData() {
        LineChartData data = new LineChartData();
        List<PointValue> values = new ArrayList<>(4);
        values.add(new PointValue(0.0F, 2.0F));
        values.add(new PointValue(1.0F, 4.0F));
        values.add(new PointValue(2.0F, 3.0F));
        values.add(new PointValue(3.0F, 4.0F));
        Line line = new Line(values);
        List<Line> lines = new ArrayList<>(1);
        lines.add(line);
        data.setLines(lines);
        return data;
    }

    public void update(float scale) {
        Iterator<Line> var2 = this.lines.iterator();

        while(var2.hasNext()) {
            Line line = (Line)var2.next();
            line.update(scale);
        }

    }

    public void finish() {
        Iterator<Line> var1 = this.lines.iterator();

        while(var1.hasNext()) {
            Line line = (Line)var1.next();
            line.finish();
        }

    }

    public List<Line> getLines() {
        return this.lines;
    }

    public LineChartData setLines(List<Line> lines) {
        if (null == lines) {
            this.lines = new ArrayList<>();
        } else {
            this.lines = lines;
        }

        return this;
    }

    public float getBaseValue() {
        return this.baseValue;
    }

    public LineChartData setBaseValue(float baseValue) {
        this.baseValue = baseValue;
        return this;
    }
}

