package dji.v5.ux.core.base.charts.model;

import java.util.Arrays;

public class PointValue {
    private float x;
    private float y;
    private float originX;
    private float originY;
    private float diffX;
    private float diffY;
    private char[] label;

    public PointValue() {
        this.set(0.0F, 0.0F);
    }

    public PointValue(float x, float y) {
        this.set(x, y);
    }

    public PointValue(PointValue pointValue) {
        this.set(pointValue.x, pointValue.y);
        this.label = pointValue.label;
    }

    public void update(float scale) {
        this.x = this.originX + this.diffX * scale;
        this.y = this.originY + this.diffY * scale;
    }

    public void finish() {
        this.set(this.originX + this.diffX, this.originY + this.diffY);
    }

    public PointValue set(float x, float y) {
        this.x = x;
        this.y = y;
        this.originX = x;
        this.originY = y;
        this.diffX = 0.0F;
        this.diffY = 0.0F;
        return this;
    }

    public PointValue setTarget(float targetX, float targetY) {
        this.set(this.x, this.y);
        this.diffX = targetX - this.originX;
        this.diffY = targetY - this.originY;
        return this;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    /** @deprecated */
    @Deprecated
    public char[] getLabel() {
        return this.label;
    }

    public PointValue setLabel(String label) {
        this.label = label.toCharArray();
        return this;
    }

    public char[] getLabelAsChars() {
        return this.label;
    }

    /** @deprecated */
    @Deprecated
    public PointValue setLabel(char[] label) {
        this.label = label;
        return this;
    }

    public String toString() {
        return "PointValue [x=" + this.x + ", y=" + this.y + "]";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            PointValue that = (PointValue)o;
            if (Float.compare(that.diffX, this.diffX) != 0) {
                return false;
            } else if (Float.compare(that.diffY, this.diffY) != 0) {
                return false;
            } else if (Float.compare(that.originX, this.originX) != 0) {
                return false;
            } else if (Float.compare(that.originY, this.originY) != 0) {
                return false;
            } else if (Float.compare(that.x, this.x) != 0) {
                return false;
            } else if (Float.compare(that.y, this.y) != 0) {
                return false;
            } else {
                return Arrays.equals(this.label, that.label);
            }
        } else {
            return false;
        }
    }

    public int hashCode() {
        int result = this.x != 0.0F ? Float.floatToIntBits(this.x) : 0;
        result = 31 * result + (this.y != 0.0F ? Float.floatToIntBits(this.y) : 0);
        result = 31 * result + (this.originX != 0.0F ? Float.floatToIntBits(this.originX) : 0);
        result = 31 * result + (this.originY != 0.0F ? Float.floatToIntBits(this.originY) : 0);
        result = 31 * result + (this.diffX != 0.0F ? Float.floatToIntBits(this.diffX) : 0);
        result = 31 * result + (this.diffY != 0.0F ? Float.floatToIntBits(this.diffY) : 0);
        result = 31 * result + (this.label != null ? Arrays.hashCode(this.label) : 0);
        return result;
    }
}
