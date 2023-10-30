package dji.v5.ux.core.base.charts.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Viewport implements Parcelable {
    public float left;
    public float top;
    public float right;
    public float bottom;
    public static final Parcelable.Creator<Viewport> CREATOR = new Parcelable.Creator<Viewport>() {
        public Viewport createFromParcel(Parcel in) {
            Viewport v = new Viewport();
            v.readFromParcel(in);
            return v;
        }

        public Viewport[] newArray(int size) {
            return new Viewport[size];
        }
    };

    public Viewport() {
    }

    public Viewport(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public Viewport(Viewport v) {
        if (v == null) {
            this.left = this.top = this.right = this.bottom = 0.0F;
        } else {
            this.left = v.left;
            this.top = v.top;
            this.right = v.right;
            this.bottom = v.bottom;
        }

    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (this.getClass() != obj.getClass()) {
            return false;
        } else {
            Viewport other = (Viewport)obj;
            if (Float.floatToIntBits(this.bottom) != Float.floatToIntBits(other.bottom)) {
                return false;
            } else if (Float.floatToIntBits(this.left) != Float.floatToIntBits(other.left)) {
                return false;
            } else if (Float.floatToIntBits(this.right) != Float.floatToIntBits(other.right)) {
                return false;
            } else {
                return Float.floatToIntBits(this.top) == Float.floatToIntBits(other.top);
            }
        }
    }

    public final boolean isEmpty() {
        return this.left >= this.right || this.bottom >= this.top;
    }

    public void setEmpty() {
        this.left = this.right = this.top = this.bottom = 0.0F;
    }

    public final float width() {
        return this.right - this.left;
    }

    public final float height() {
        return this.top - this.bottom;
    }

    public final float centerX() {
        return (this.left + this.right) * 0.5F;
    }

    public final float centerY() {
        return (this.top + this.bottom) * 0.5F;
    }

    public void set(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public void set(Viewport src) {
        this.left = src.left;
        this.top = src.top;
        this.right = src.right;
        this.bottom = src.bottom;
    }

    public void offset(float dx, float dy) {
        this.left += dx;
        this.top += dy;
        this.right += dx;
        this.bottom += dy;
    }

    public void offsetTo(float newLeft, float newTop) {
        this.right += newLeft - this.left;
        this.bottom += newTop - this.top;
        this.left = newLeft;
        this.top = newTop;
    }

    public void inset(float dx, float dy) {
        this.left += dx;
        this.top -= dy;
        this.right -= dx;
        this.bottom += dy;
    }

    public boolean contains(float x, float y) {
        return this.left < this.right && this.bottom < this.top && x >= this.left && x < this.right && y >= this.bottom && y < this.top;
    }

    public boolean contains(float left, float top, float right, float bottom) {
        return this.left < this.right && this.bottom < this.top && this.left <= left && this.top >= top && this.right >= right && this.bottom <= bottom;
    }

    public boolean contains(Viewport v) {
        return this.left < this.right && this.bottom < this.top && this.left <= v.left && this.top >= v.top && this.right >= v.right && this.bottom <= v.bottom;
    }

    public void union(float left, float top, float right, float bottom) {
        if (left < right && bottom < top) {
            if (this.left < this.right && this.bottom < this.top) {
                if (this.left > left) {
                    this.left = left;
                }

                if (this.top < top) {
                    this.top = top;
                }

                if (this.right < right) {
                    this.right = right;
                }

                if (this.bottom > bottom) {
                    this.bottom = bottom;
                }
            } else {
                this.left = left;
                this.top = top;
                this.right = right;
                this.bottom = bottom;
            }
        }

    }

    public void union(Viewport v) {
        this.union(v.left, v.top, v.right, v.bottom);
    }

    public boolean intersect(float left, float top, float right, float bottom) {
        if (this.left < right && left < this.right && this.bottom < top && bottom < this.top) {
            if (this.left < left) {
                this.left = left;
            }

            if (this.top > top) {
                this.top = top;
            }

            if (this.right > right) {
                this.right = right;
            }

            if (this.bottom < bottom) {
                this.bottom = bottom;
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean intersect(Viewport v) {
        return this.intersect(v.left, v.top, v.right, v.bottom);
    }

    public String toString() {
        return "Viewport [left=" + this.left + ", top=" + this.top + ", right=" + this.right + ", bottom=" + this.bottom + "]";
    }

    public int hashCode() {
        int prime = 1;
        int result = 1;
        result = 31 * result + Float.floatToIntBits(this.bottom);
        result = 31 * result + Float.floatToIntBits(this.left);
        result = 31 * result + Float.floatToIntBits(this.right);
        result = 31 * result + Float.floatToIntBits(this.top);
        return result;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeFloat(this.left);
        out.writeFloat(this.top);
        out.writeFloat(this.right);
        out.writeFloat(this.bottom);
    }

    public void readFromParcel(Parcel in) {
        this.left = in.readFloat();
        this.top = in.readFloat();
        this.right = in.readFloat();
        this.bottom = in.readFloat();
    }
}

