package dji.v5.ux.core.base.charts.model;

import android.graphics.Typeface;

import dji.v5.ux.core.base.charts.util.ChartUtils;

public abstract class AbstractChartData implements ChartData {
    protected Axis axisXBottom;
    protected Axis axisYLeft;
    protected Axis axisXTop;
    protected Axis axisYRight;
    protected int valueLabelTextColor = -1;
    protected int valueLabelTextSize = 12;
    protected Typeface valueLabelTypeface;
    protected boolean isValueLabelBackgroundEnabled = true;
    protected boolean isValueLabelBackgrountAuto = true;
    protected int valueLabelBackgroundColor;

    protected AbstractChartData() {
        this.valueLabelBackgroundColor = ChartUtils.darkenColor(ChartUtils.DEFAULT_DARKEN_COLOR);
    }

    protected AbstractChartData(AbstractChartData data) {
        this.valueLabelBackgroundColor = ChartUtils.darkenColor(ChartUtils.DEFAULT_DARKEN_COLOR);
        if (null != data.axisXBottom) {
            this.axisXBottom = new Axis(data.axisXBottom);
        }

        if (null != data.axisXTop) {
            this.axisXTop = new Axis(data.axisXTop);
        }

        if (null != data.axisYLeft) {
            this.axisYLeft = new Axis(data.axisYLeft);
        }

        if (null != data.axisYRight) {
            this.axisYRight = new Axis(data.axisYRight);
        }

        this.valueLabelTextColor = data.valueLabelTextColor;
        this.valueLabelTextSize = data.valueLabelTextSize;
        this.valueLabelTypeface = data.valueLabelTypeface;
    }

    public Axis getAxisXBottom() {
        return this.axisXBottom;
    }

    public void setAxisXBottom(Axis axisX) {
        this.axisXBottom = axisX;
    }

    public Axis getAxisYLeft() {
        return this.axisYLeft;
    }

    public void setAxisYLeft(Axis axisY) {
        this.axisYLeft = axisY;
    }

    public Axis getAxisXTop() {
        return this.axisXTop;
    }

    public void setAxisXTop(Axis axisX) {
        this.axisXTop = axisX;
    }

    public Axis getAxisYRight() {
        return this.axisYRight;
    }

    public void setAxisYRight(Axis axisY) {
        this.axisYRight = axisY;
    }

    public int getValueLabelTextColor() {
        return this.valueLabelTextColor;
    }

    public void setValueLabelsTextColor(int valueLabelTextColor) {
        this.valueLabelTextColor = valueLabelTextColor;
    }

    public int getValueLabelTextSize() {
        return this.valueLabelTextSize;
    }

    public void setValueLabelTextSize(int valueLabelTextSize) {
        this.valueLabelTextSize = valueLabelTextSize;
    }

    public Typeface getValueLabelTypeface() {
        return this.valueLabelTypeface;
    }

    public void setValueLabelTypeface(Typeface typeface) {
        this.valueLabelTypeface = typeface;
    }

    public boolean isValueLabelBackgroundEnabled() {
        return this.isValueLabelBackgroundEnabled;
    }

    public void setValueLabelBackgroundEnabled(boolean isValueLabelBackgroundEnabled) {
        this.isValueLabelBackgroundEnabled = isValueLabelBackgroundEnabled;
    }

    public boolean isValueLabelBackgroundAuto() {
        return this.isValueLabelBackgrountAuto;
    }

    public void setValueLabelBackgroundAuto(boolean isValueLabelBackgrountAuto) {
        this.isValueLabelBackgrountAuto = isValueLabelBackgrountAuto;
    }

    public int getValueLabelBackgroundColor() {
        return this.valueLabelBackgroundColor;
    }

    public void setValueLabelBackgroundColor(int valueLabelBackgroundColor) {
        this.valueLabelBackgroundColor = valueLabelBackgroundColor;
    }
}

