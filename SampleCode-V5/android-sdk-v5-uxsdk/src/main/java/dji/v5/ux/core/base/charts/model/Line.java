package dji.v5.ux.core.base.charts.model;

import android.graphics.PathEffect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dji.v5.ux.core.base.charts.formatter.LineChartValueFormatter;
import dji.v5.ux.core.base.charts.formatter.SimpleLineChartValueFormatter;
import dji.v5.ux.core.base.charts.util.ChartUtils;

public class Line {
    private int color;
    private int pointColor;
    private int darkenColor;
    private int areaTransparency;
    private int strokeWidth;
    private int pointRadius;
    private boolean hasPoints;
    private boolean hasLines;
    private boolean hasLabels;
    private boolean hasLabelsOnlyForSelected;
    private boolean isCubic;
    private boolean isSquare;
    private boolean isFilled;
    private ValueShape shape;
    private PathEffect pathEffect;
    private LineChartValueFormatter formatter;
    private List<PointValue> values;

    public Line() {
        this.color = ChartUtils.DEFAULT_COLOR;
        this.pointColor = 0;
        this.darkenColor = ChartUtils.DEFAULT_DARKEN_COLOR;
        this.areaTransparency = 64;
        this.strokeWidth = 3;
        this.pointRadius = 6;
        this.hasPoints = true;
        this.hasLines = true;
        this.hasLabels = false;
        this.hasLabelsOnlyForSelected = false;
        this.isCubic = false;
        this.isSquare = false;
        this.isFilled = false;
        this.shape = ValueShape.CIRCLE;
        this.formatter = new SimpleLineChartValueFormatter();
        this.values = new ArrayList<>();
    }

    public Line(List<PointValue> values) {
        this.color = ChartUtils.DEFAULT_COLOR;
        this.pointColor = 0;
        this.darkenColor = ChartUtils.DEFAULT_DARKEN_COLOR;
        this.areaTransparency = 64;
        this.strokeWidth = 3;
        this.pointRadius = 6;
        this.hasPoints = true;
        this.hasLines = true;
        this.hasLabels = false;
        this.hasLabelsOnlyForSelected = false;
        this.isCubic = false;
        this.isSquare = false;
        this.isFilled = false;
        this.shape = ValueShape.CIRCLE;
        this.formatter = new SimpleLineChartValueFormatter();
        this.values = new ArrayList<>();
        this.setValues(values);
    }

    public Line(Line line) {
        this.color = ChartUtils.DEFAULT_COLOR;
        this.pointColor = 0;
        this.darkenColor = ChartUtils.DEFAULT_DARKEN_COLOR;
        this.areaTransparency = 64;
        this.strokeWidth = 3;
        this.pointRadius = 6;
        this.hasPoints = true;
        this.hasLines = true;
        this.hasLabels = false;
        this.hasLabelsOnlyForSelected = false;
        this.isCubic = false;
        this.isSquare = false;
        this.isFilled = false;
        this.shape = ValueShape.CIRCLE;
        this.formatter = new SimpleLineChartValueFormatter();
        this.values = new ArrayList<>();
        this.color = line.color;
        this.pointColor = line.pointColor;
        this.darkenColor = line.darkenColor;
        this.areaTransparency = line.areaTransparency;
        this.strokeWidth = line.strokeWidth;
        this.pointRadius = line.pointRadius;
        this.hasPoints = line.hasPoints;
        this.hasLines = line.hasLines;
        this.hasLabels = line.hasLabels;
        this.hasLabelsOnlyForSelected = line.hasLabelsOnlyForSelected;
        this.isSquare = line.isSquare;
        this.isCubic = line.isCubic;
        this.isFilled = line.isFilled;
        this.shape = line.shape;
        this.pathEffect = line.pathEffect;
        this.formatter = line.formatter;
        Iterator<PointValue> var2 = line.values.iterator();

        while(var2.hasNext()) {
            PointValue pointValue = (PointValue)var2.next();
            this.values.add(new PointValue(pointValue));
        }

    }

    public void update(float scale) {
        Iterator<PointValue> var2 = this.values.iterator();

        while(var2.hasNext()) {
            PointValue value = (PointValue)var2.next();
            value.update(scale);
        }

    }

    public void finish() {
        Iterator<PointValue> var1 = this.values.iterator();

        while(var1.hasNext()) {
            PointValue value = (PointValue)var1.next();
            value.finish();
        }

    }

    public List<PointValue> getValues() {
        return this.values;
    }

    public void setValues(List<PointValue> values) {
        if (null == values) {
            this.values = new ArrayList<>();
        } else {
            this.values = values;
        }

    }

    public int getColor() {
        return this.color;
    }

    public Line setColor(int color) {
        this.color = color;
        if (this.pointColor == 0) {
            this.darkenColor = ChartUtils.darkenColor(color);
        }

        return this;
    }

    public int getPointColor() {
        return this.pointColor == 0 ? this.color : this.pointColor;
    }

    public Line setPointColor(int pointColor) {
        this.pointColor = pointColor;
        if (pointColor == 0) {
            this.darkenColor = ChartUtils.darkenColor(this.color);
        } else {
            this.darkenColor = ChartUtils.darkenColor(pointColor);
        }

        return this;
    }

    public int getDarkenColor() {
        return this.darkenColor;
    }

    public int getAreaTransparency() {
        return this.areaTransparency;
    }

    public Line setAreaTransparency(int areaTransparency) {
        this.areaTransparency = areaTransparency;
        return this;
    }

    public int getStrokeWidth() {
        return this.strokeWidth;
    }

    public Line setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
        return this;
    }

    public boolean hasPoints() {
        return this.hasPoints;
    }

    public Line setHasPoints(boolean hasPoints) {
        this.hasPoints = hasPoints;
        return this;
    }

    public boolean hasLines() {
        return this.hasLines;
    }

    public Line setHasLines(boolean hasLines) {
        this.hasLines = hasLines;
        return this;
    }

    public boolean hasLabels() {
        return this.hasLabels;
    }

    public Line setHasLabels(boolean hasLabels) {
        this.hasLabels = hasLabels;
        if (hasLabels) {
            this.hasLabelsOnlyForSelected = false;
        }

        return this;
    }

    public boolean hasLabelsOnlyForSelected() {
        return this.hasLabelsOnlyForSelected;
    }

    public Line setHasLabelsOnlyForSelected(boolean hasLabelsOnlyForSelected) {
        this.hasLabelsOnlyForSelected = hasLabelsOnlyForSelected;
        if (hasLabelsOnlyForSelected) {
            this.hasLabels = false;
        }

        return this;
    }

    public int getPointRadius() {
        return this.pointRadius;
    }

    public Line setPointRadius(int pointRadius) {
        this.pointRadius = pointRadius;
        return this;
    }

    public boolean isCubic() {
        return this.isCubic;
    }

    public Line setCubic(boolean isCubic) {
        this.isCubic = isCubic;
        if (this.isSquare) {
            this.setSquare(false);
        }

        return this;
    }

    public boolean isSquare() {
        return this.isSquare;
    }

    public Line setSquare(boolean isSquare) {
        this.isSquare = isSquare;
        if (this.isCubic) {
            this.setCubic(false);
        }

        return this;
    }

    public boolean isFilled() {
        return this.isFilled;
    }

    public Line setFilled(boolean isFilled) {
        this.isFilled = isFilled;
        return this;
    }

    public ValueShape getShape() {
        return this.shape;
    }

    public Line setShape(ValueShape shape) {
        this.shape = shape;
        return this;
    }

    public PathEffect getPathEffect() {
        return this.pathEffect;
    }

    public void setPathEffect(PathEffect pathEffect) {
        this.pathEffect = pathEffect;
    }

    public LineChartValueFormatter getFormatter() {
        return this.formatter;
    }

    public Line setFormatter(LineChartValueFormatter formatter) {
        if (null != formatter) {
            this.formatter = formatter;
        }

        return this;
    }
}

