package dji.v5.ux.core.base.charts.model;

import java.util.Arrays;

public class AxisValue {
    private float value;
    private char[] label;

    public AxisValue(float value) {
        this.setValue(value);
    }

    /** @deprecated */
    @Deprecated
    public AxisValue(float value, char[] label) {
        this.value = value;
        this.label = label;
    }

    public AxisValue(AxisValue axisValue) {
        this.value = axisValue.value;
        this.label = axisValue.label;
    }

    public float getValue() {
        return this.value;
    }

    public AxisValue setValue(float value) {
        this.value = value;
        return this;
    }

    /** @deprecated */
    @Deprecated
    public char[] getLabel() {
        return this.label;
    }

    public AxisValue setLabel(String label) {
        this.label = label.toCharArray();
        return this;
    }

    public char[] getLabelAsChars() {
        return this.label;
    }

    /** @deprecated */
    @Deprecated
    public AxisValue setLabel(char[] label) {
        this.label = label;
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            AxisValue axisValue = (AxisValue)o;
            if (Float.compare(axisValue.value, this.value) != 0) {
                return false;
            } else {
                return Arrays.equals(this.label, axisValue.label);
            }
        } else {
            return false;
        }
    }

    public int hashCode() {
        int result = this.value != 0.0F ? Float.floatToIntBits(this.value) : 0;
        result = 31 * result + (this.label != null ? Arrays.hashCode(this.label) : 0);
        return result;
    }
}

