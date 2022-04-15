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

package dji.v5.ux.core.util;

import android.text.InputFilter;
import android.text.Spanned;

public class EditTextNumberInputFilter implements InputFilter{
    private int min, max;

    public EditTextNumberInputFilter(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public EditTextNumberInputFilter(String min, String max) {
        this.min = Integer.parseInt(min);
        this.max = Integer.parseInt(max);
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            String stringInput = dest.toString() + source.toString();
            double value;
            if (stringInput.length() == 1 && stringInput.charAt(0) == '-') {
                value = -1;
            } else {
                value = Double.parseDouble(dest.toString() + source.toString());
            }
            if (isInRange(min, max, value)) {
                return null;
            }
        } catch (NumberFormatException nfe) {
            //Do nothing as the number is not valid
        }
        return "";
    }

    private boolean isInRange(int a, int b, double c) {
        return b > a ? c >= a && c <= b : c >= b && c <= a;
    }

}
