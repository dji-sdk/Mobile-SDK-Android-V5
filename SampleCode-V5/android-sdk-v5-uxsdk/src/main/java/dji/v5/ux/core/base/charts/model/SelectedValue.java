package dji.v5.ux.core.base.charts.model;

public class SelectedValue {
    private int firstIndex;
    private int secondIndex;
    private SelectedValueType type;

    public SelectedValue() {
        this.type = SelectedValue.SelectedValueType.NONE;
        this.clear();
    }

    public SelectedValue(int firstIndex, int secondIndex, SelectedValueType type) {
        this.type = SelectedValue.SelectedValueType.NONE;
        this.set(firstIndex, secondIndex, type);
    }

    public void set(int firstIndex, int secondIndex, SelectedValueType type) {
        this.firstIndex = firstIndex;
        this.secondIndex = secondIndex;
        if (null != type) {
            this.type = type;
        } else {
            this.type = SelectedValue.SelectedValueType.NONE;
        }

    }

    public void set(SelectedValue selectedValue) {
        this.firstIndex = selectedValue.firstIndex;
        this.secondIndex = selectedValue.secondIndex;
        this.type = selectedValue.type;
    }

    public void clear() {
        this.set(Integer.MIN_VALUE, Integer.MIN_VALUE, SelectedValue.SelectedValueType.NONE);
    }

    public boolean isSet() {
        return this.firstIndex >= 0 && this.secondIndex >= 0;
    }

    public int getFirstIndex() {
        return this.firstIndex;
    }

    public void setFirstIndex(int firstIndex) {
        this.firstIndex = firstIndex;
    }

    public int getSecondIndex() {
        return this.secondIndex;
    }

    public void setSecondIndex(int secondIndex) {
        this.secondIndex = secondIndex;
    }

    public SelectedValueType getType() {
        return this.type;
    }

    public void setType(SelectedValueType type) {
        this.type = type;
    }

    public int hashCode() {
        int prime = 1;
        int result = 1;
        result = 31 * result + this.firstIndex;
        result = 31 * result + this.secondIndex;
        result = 31 * result + (this.type == null ? 0 : this.type.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (this.getClass() != obj.getClass()) {
            return false;
        } else {
            SelectedValue other = (SelectedValue)obj;
            if (this.firstIndex != other.firstIndex) {
                return false;
            } else if (this.secondIndex != other.secondIndex) {
                return false;
            } else {
                return this.type == other.type;
            }
        }
    }

    public String toString() {
        return "SelectedValue [firstIndex=" + this.firstIndex + ", secondIndex=" + this.secondIndex + ", type=" + this.type + "]";
    }

    public static enum SelectedValueType {
        NONE,
        LINE,
        COLUMN;

        private SelectedValueType() {
        }
    }
}

