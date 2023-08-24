package dji.v5.ux.core.base.charts.model;

import java.util.Arrays;

import dji.v5.ux.core.base.charts.util.ChartUtils;

public class SliceValue {
    /** @deprecated */
    @Deprecated
    private int sliceSpacing = 2;
    private float value;
    private float originValue;
    private float diff;
    private int color;
    private int darkenColor;
    private char[] label;

    public SliceValue() {
        this.color = ChartUtils.DEFAULT_COLOR;
        this.darkenColor = ChartUtils.DEFAULT_DARKEN_COLOR;
        this.setValue(0.0F);
    }

    public SliceValue(float value) {
        this.color = ChartUtils.DEFAULT_COLOR;
        this.darkenColor = ChartUtils.DEFAULT_DARKEN_COLOR;
        this.setValue(value);
    }

    public SliceValue(float value, int color) {
        this.color = ChartUtils.DEFAULT_COLOR;
        this.darkenColor = ChartUtils.DEFAULT_DARKEN_COLOR;
        this.setValue(value);
        this.setColor(color);
    }

    public SliceValue(float value, int color, int sliceSpacing) {
        this.color = ChartUtils.DEFAULT_COLOR;
        this.darkenColor = ChartUtils.DEFAULT_DARKEN_COLOR;
        this.setValue(value);
        this.setColor(color);
        this.sliceSpacing = sliceSpacing;
    }

    public SliceValue(SliceValue sliceValue) {
        this.color = ChartUtils.DEFAULT_COLOR;
        this.darkenColor = ChartUtils.DEFAULT_DARKEN_COLOR;
        this.setValue(sliceValue.value);
        this.setColor(sliceValue.color);
        this.sliceSpacing = sliceValue.sliceSpacing;
        this.label = sliceValue.label;
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

    public SliceValue setValue(float value) {
        this.value = value;
        this.originValue = value;
        this.diff = 0.0F;
        return this;
    }

    public SliceValue setTarget(float target) {
        this.setValue(this.value);
        this.diff = target - this.originValue;
        return this;
    }

    public int getColor() {
        return this.color;
    }

    public SliceValue setColor(int color) {
        this.color = color;
        this.darkenColor = ChartUtils.darkenColor(color);
        return this;
    }

    public int getDarkenColor() {
        return this.darkenColor;
    }

    /** @deprecated */
    @Deprecated
    public int getSliceSpacing() {
        return this.sliceSpacing;
    }

    /** @deprecated */
    @Deprecated
    public SliceValue setSliceSpacing(int sliceSpacing) {
        this.sliceSpacing = sliceSpacing;
        return this;
    }

    /** @deprecated */
    @Deprecated
    public char[] getLabel() {
        return this.label;
    }

    /** @deprecated */
    @Deprecated
    public SliceValue setLabel(char[] label) {
        this.label = label;
        return this;
    }

    public SliceValue setLabel(String label) {
        this.label = label.toCharArray();
        return this;
    }

    public char[] getLabelAsChars() {
        return this.label;
    }

    public String toString() {
        return "SliceValue [value=" + this.value + "]";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            SliceValue that = (SliceValue)o;
            if (this.color != that.color) {
                return false;
            } else if (this.darkenColor != that.darkenColor) {
                return false;
            } else if (Float.compare(that.diff, this.diff) != 0) {
                return false;
            } else if (Float.compare(that.originValue, this.originValue) != 0) {
                return false;
            } else if (this.sliceSpacing != that.sliceSpacing) {
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
        result = 31 * result + this.sliceSpacing;
        result = 31 * result + (this.label != null ? Arrays.hashCode(this.label) : 0);
        return result;
    }
}

