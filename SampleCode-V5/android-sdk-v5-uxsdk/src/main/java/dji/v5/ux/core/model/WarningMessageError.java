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

package dji.v5.ux.core.model;

/**
 * {@link WarningMessage} errors
 */
public enum WarningMessageError {

    /**
     * Vision/Obstacle avoidance sensor message
     */
    VISION_AVOID(1004),

    /**
     * There are aircraft in vicinity
     */
    OTHER_AIRCRAFT_NEARBY(1022),

    /**
     *  Error due to customer use
     */
    CUSTOMER_USE_ERROR(10000),

    /**
     *  Unknown error
     */
    UNKNOWN(0);

    private int value;

    WarningMessageError(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    private boolean _equals(int b) {
        return value == b;
    }

    private static WarningMessageError[] values;

    public static WarningMessageError[] getValues() {
        if (values == null) {
            values = values();
        }
        return values;
    }

    public static WarningMessageError find(int b) {
        WarningMessageError result = UNKNOWN;
        for (int i = 0; i < getValues().length; i++) {
            if (getValues()[i]._equals(b)) {
                result = getValues()[i];
                break;
            }
        }
        return result;
    }
}
