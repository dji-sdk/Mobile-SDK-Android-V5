package dji.v5.ux.core.base.charts.model;

import java.util.Arrays;

import dji.v5.ux.core.base.charts.util.ChartUtils;

public class BubbleValue {
    private float x;
    private float y;
    private float z;
    private float originX;
    private float originY;
    private float originZ;
    private float diffX;
    private float diffY;
    private float diffZ;
    private int color;
    private int darkenColor;
    private ValueShape shape;
    private char[] label;

    public BubbleValue() {
        this.color = ChartUtils.DEFAULT_COLOR;
        this.darkenColor = ChartUtils.DEFAULT_DARKEN_COLOR;
        this.shape = ValueShape.CIRCLE;
        this.set(0.0F, 0.0F, 0.0F);
    }

    public BubbleValue(float x, float y, float z) {
        this.color = ChartUtils.DEFAULT_COLOR;
        this.darkenColor = ChartUtils.DEFAULT_DARKEN_COLOR;
        this.shape = ValueShape.CIRCLE;
        this.set(x, y, z);
    }

    public BubbleValue(float x, float y, float z, int color) {
        this.color = ChartUtils.DEFAULT_COLOR;
        this.darkenColor = ChartUtils.DEFAULT_DARKEN_COLOR;
        this.shape = ValueShape.CIRCLE;
        this.set(x, y, z);
        this.setColor(color);
    }

    public BubbleValue(BubbleValue bubbleValue) {
        this.color = ChartUtils.DEFAULT_COLOR;
        this.darkenColor = ChartUtils.DEFAULT_DARKEN_COLOR;
        this.shape = ValueShape.CIRCLE;
        this.set(bubbleValue.x, bubbleValue.y, bubbleValue.z);
        this.setColor(bubbleValue.color);
        this.label = bubbleValue.label;
    }

    public void update(float scale) {
        this.x = this.originX + this.diffX * scale;
        this.y = this.originY + this.diffY * scale;
        this.z = this.originZ + this.diffZ * scale;
    }

    public void finish() {
        this.set(this.originX + this.diffX, this.originY + this.diffY, this.originZ + this.diffZ);
    }

    public BubbleValue set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.originX = x;
        this.originY = y;
        this.originZ = z;
        this.diffX = 0.0F;
        this.diffY = 0.0F;
        this.diffZ = 0.0F;
        return this;
    }

    public BubbleValue setTarget(float targetX, float targetY, float targetZ) {
        this.set(this.x, this.y, this.z);
        this.diffX = targetX - this.originX;
        this.diffY = targetY - this.originY;
        this.diffZ = targetZ - this.originZ;
        return this;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getZ() {
        return this.z;
    }

    public int getColor() {
        return this.color;
    }

    public BubbleValue setColor(int color) {
        this.color = color;
        this.darkenColor = ChartUtils.darkenColor(color);
        return this;
    }

    public int getDarkenColor() {
        return this.darkenColor;
    }

    public ValueShape getShape() {
        return this.shape;
    }

    public BubbleValue setShape(ValueShape shape) {
        this.shape = shape;
        return this;
    }

    /** @deprecated */
    @Deprecated
    public char[] getLabel() {
        return this.label;
    }

    public BubbleValue setLabel(String label) {
        this.label = label.toCharArray();
        return this;
    }

    public char[] getLabelAsChars() {
        return this.label;
    }

    /** @deprecated */
    @Deprecated
    public BubbleValue setLabel(char[] label) {
        this.label = label;
        return this;
    }

    public String toString() {
        return "BubbleValue [x=" + this.x + ", y=" + this.y + ", z=" + this.z + "]";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            BubbleValue that = (BubbleValue)o;
            if (this.color != that.color) {
                return false;
            } else if (this.darkenColor != that.darkenColor) {
                return false;
            } else if (Float.compare(that.diffX, this.diffX) != 0) {
                return false;
            } else if (Float.compare(that.diffY, this.diffY) != 0) {
                return false;
            } else if (Float.compare(that.diffZ, this.diffZ) != 0) {
                return false;
            } else if (Float.compare(that.originX, this.originX) != 0) {
                return false;
            } else if (Float.compare(that.originY, this.originY) != 0) {
                return false;
            } else if (Float.compare(that.originZ, this.originZ) != 0) {
                return false;
            } else if (Float.compare(that.x, this.x) != 0) {
                return false;
            } else if (Float.compare(that.y, this.y) != 0) {
                return false;
            } else if (Float.compare(that.z, this.z) != 0) {
                return false;
            } else if (!Arrays.equals(this.label, that.label)) {
                return false;
            } else {
                return this.shape == that.shape;
            }
        } else {
            return false;
        }
    }

    public int hashCode() {
        int result = this.x != 0.0F ? Float.floatToIntBits(this.x) : 0;
        result = 31 * result + (this.y != 0.0F ? Float.floatToIntBits(this.y) : 0);
        result = 31 * result + (this.z != 0.0F ? Float.floatToIntBits(this.z) : 0);
        result = 31 * result + (this.originX != 0.0F ? Float.floatToIntBits(this.originX) : 0);
        result = 31 * result + (this.originY != 0.0F ? Float.floatToIntBits(this.originY) : 0);
        result = 31 * result + (this.originZ != 0.0F ? Float.floatToIntBits(this.originZ) : 0);
        result = 31 * result + (this.diffX != 0.0F ? Float.floatToIntBits(this.diffX) : 0);
        result = 31 * result + (this.diffY != 0.0F ? Float.floatToIntBits(this.diffY) : 0);
        result = 31 * result + (this.diffZ != 0.0F ? Float.floatToIntBits(this.diffZ) : 0);
        result = 31 * result + this.color;
        result = 31 * result + this.darkenColor;
        result = 31 * result + (this.shape != null ? this.shape.hashCode() : 0);
        result = 31 * result + (this.label != null ? Arrays.hashCode(this.label) : 0);
        return result;
    }
}

