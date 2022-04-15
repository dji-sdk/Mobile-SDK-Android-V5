/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.v5.ux.core.communication;

import java.util.Arrays;

import androidx.annotation.Nullable;
import dji.v5.utils.common.LogUtils;

/**
 * This is a wrapper class used by the UXKey system to
 * propagate data of `Object` type through to the users
 */
public class ModelValue {

    private static final String TAG = "ModelValue";
    private final Object data;

    public ModelValue(@Nullable Object data) {
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    @Override
    public int hashCode() {
        return data != null ? data.hashCode() : 0;
    }

    @Override
    public boolean equals(Object o) {
        //True if both o and data are null, false otherwise
        if (o == null && data == null) {
            return true;
        } else if (o != null && data != null) {
            if (data.getClass().isArray() && o.getClass().isArray()) {
                return arrayEquals(data, o);
            } else {
                if (o instanceof ModelValue) {
                    return o.equals(data);
                }
            }
        }
        return false;
    }

    private boolean arrayEquals(Object a, Object b) {
        boolean returnEqual = false;
        if (a.getClass() == int[].class) {
            returnEqual = Arrays.equals((int[]) a, (int[]) b);
        } else if (a.getClass() == boolean[].class) {
            returnEqual = Arrays.equals((boolean[]) a, (boolean[]) b);
        } else if (a.getClass() == byte[].class) {
            returnEqual = Arrays.equals((byte[]) a, (byte[]) b);
        } else if (a.getClass() == char[].class) {
            returnEqual = Arrays.equals((char[]) a, (char[]) b);
        } else if (a.getClass() == long[].class) {
            returnEqual = Arrays.equals((long[]) a, (long[]) b);
        } else if (a.getClass() == short[].class) {
            returnEqual = Arrays.equals((short[]) a, (short[]) b);
        } else if (a.getClass() == float[].class) {
            returnEqual = Arrays.equals((float[]) a, (float[]) b);
        } else if (a.getClass() == double[].class) {
            returnEqual = Arrays.equals((double[]) a, (double[]) b);
        } else {
            try {
                returnEqual = Arrays.equals((Object[]) a, (Object[]) b);
            } catch (Exception e) {
                LogUtils.e(TAG, e.getMessage());
            }
        }
        return returnEqual;
    }
}
