package dji.v5.ux.core.base.charts.model;

import java.util.Arrays;

import dji.v5.ux.core.base.charts.util.ChartUtils;

public class SubcolumnValue {
    private float value;
    private float originValue;
    private float diff;
    private int color;
    private int darkenColor;
    private char[] label;

    public SubcolumnValue() {
        this.color = ChartUtils.DEFAULT_COLOR;
        this.darkenColor = ChartUtils.DEFAULT_DARKEN_COLOR;
        this.setValue(0.0F);
    }

    public SubcolumnValue(float value) {
        this.color = ChartUtils.DEFAULT_COLOR;
        this.darkenColor = ChartUtils.DEFAULT_DARKEN_COLOR;
        this.setValue(value);
    }

    public SubcolumnValue(float value, int color) {
        this.color = ChartUtils.DEFAULT_COLOR;
        this.darkenColor = ChartUtils.DEFAULT_DARKEN_COLOR;
        this.setValue(value);
        this.setColor(color);
    }

    public SubcolumnValue(SubcolumnValue columnValue) {
        this.color = ChartUtils.DEFAULT_COLOR;
        this.darkenColor = ChartUtils.DEFAULT_DARKEN_COLOR;
        this.setValue(columnValue.value);
        this.setColor(columnValue.color);
        this.label = columnValue.label;
    }

    public void update(float scale) {
        this.value = this.originValue + this.diff * scale;
    }

    public void finish() {
        this.setValue(this.originValue + this.diff);
    }

    public float getValue() {
        return this.value;
    }

    public SubcolumnValue setValue(float value) {
        this.value = value;
        this.originValue = value;
        this.diff = 0.0F;
        return this;
    }

    public SubcolumnValue setTarget(float target) {
        this.setValue(this.value);
        this.diff = target - this.originValue;
        return this;
    }

    public int getColor() {
        return this.color;
    }

    public SubcolumnValue setColor(int color) {
        this.color = color;
        this.darkenColor = ChartUtils.darkenColor(color);
        return this;
    }

    public int getDarkenColor() {
        return this.darkenColor;
    }

    /** @deprecated */
    @Deprecated
    public char[] getLabel() {
        return this.label;
    }

    public SubcolumnValue setLabel(String label) {
        this.label = label.toCharArray();
        return this;
    }

    public char[] getLabelAsChars() {
        return this.label;
    }

    /** @deprecated */
    @Deprecated
    public SubcolumnValue setLabel(char[] label) {
        this.label = label;
        return this;
    }

    public String toString() {
        return "ColumnValue [value=" + this.value + "]";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            SubcolumnValue that = (SubcolumnValue)o;
            if (this.color != that.color) {
                return false;
            } else if (this.darkenColor != that.darkenColor) {
                return false;
            } else if (Float.compare(that.diff, this.diff) != 0) {
                return false;
            } else if (Float.compare(that.originValue, this.originValue) != 0) {
                return false;
            } else if (Float.compare(that.value, this.value) != 0) {
                return false;
            } else {
                return Arrays.equals(this.label, that.label);
            }
        } else {
            return false;
        }
    }

    public int hashCode() {
        int result = this.value != 0.0F ? Float.floatToIntBits(this.value) : 0;
        result = 31 * result + (this.originValue != 0.0F ? Float.floatToIntBits(this.originValue) : 0);
        result = 31 * result + (this.diff != 0.0F ? Float.floatToIntBits(this.diff) : 0);
        result = 31 * result + this.color;
        result = 31 * result + this.darkenColor;
        result = 31 * result + (this.label != null ? Arrays.hashCode(this.label) : 0);
        return result;
    }
}

