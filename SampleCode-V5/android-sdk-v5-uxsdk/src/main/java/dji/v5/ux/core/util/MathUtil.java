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

public final class MathUtil {

    private static final int ROTATION_VECTOR_LENGTH_MAX = 4;
    private static final int ROTATION_MATRIX_SMALL = 9;
    private static final int ROTATION_MATRIX_BIG = 16;

    private MathUtil() {
        //Util class
    }

    /**
     * Customized helper function to convert a rotation vector to a rotation matrix.
     * Given a rotation vector (presumably from a ROTATION_VECTOR sensor), this function returns a
     * 9 or 16 element rotation matrix in the array rotationMatrix. The rotationMatrix array must have
     * a length of 9 or 16. If rotationMatrix.length == 9, the following matrix is returned:
     * /  R[ 0]   R[ 1]   R[ 2]   \
     * |  R[ 3]   R[ 4]   R[ 5]   |
     * \  R[ 6]   R[ 7]   R[ 8]   /
     * <p>
     * If rotationMatrix.length == 16, the following matrix is returned:
     * /  R[ 0]   R[ 1]   R[ 2]   0  \
     * |  R[ 4]   R[ 5]   R[ 6]   0  |
     * |  R[ 8]   R[ 9]   R[10]   0  |
     * \  0       0       0       1  /
     */
    public static void getRotationMatrixFromVector(float[] rotationMatrix, float[] rotationVector) {

        float q0;
        float q1 = rotationVector[0];
        float q2 = rotationVector[1];
        float q3 = rotationVector[2];

        if (rotationVector.length == ROTATION_VECTOR_LENGTH_MAX) {
            q0 = rotationVector[3];
        } else {
            q0 = 1 - q1 * q1 - q2 * q2 - q3 * q3;
            q0 = (q0 > 0) ? (float) Math.sqrt(q0) : 0;
        }

        float sqQ1 = 2 * q1 * q1;
        float sqQ2 = 2 * q2 * q2;
        float sqQ3 = 2 * q3 * q3;
        float q1Q2 = 2 * q1 * q2;
        float q3Q0 = 2 * q3 * q0;
        float q1Q3 = 2 * q1 * q3;
        float q2Q0 = 2 * q2 * q0;
        float q2Q3 = 2 * q2 * q3;
        float q1Q0 = 2 * q1 * q0;

        if (rotationMatrix.length == ROTATION_MATRIX_SMALL) {
            rotationMatrix[0] = 1 - sqQ2 - sqQ3;
            rotationMatrix[1] = q1Q2 - q3Q0;
            rotationMatrix[2] = q1Q3 + q2Q0;

            rotationMatrix[3] = q1Q2 + q3Q0;
            rotationMatrix[4] = 1 - sqQ1 - sqQ3;
            rotationMatrix[5] = q2Q3 - q1Q0;

            rotationMatrix[6] = q1Q3 - q2Q0;
            rotationMatrix[7] = q2Q3 + q1Q0;
            rotationMatrix[8] = 1 - sqQ1 - sqQ2;
        } else if (rotationMatrix.length == ROTATION_MATRIX_BIG) {
            rotationMatrix[0] = 1 - sqQ2 - sqQ3;
            rotationMatrix[1] = q1Q2 - q3Q0;
            rotationMatrix[2] = q1Q3 + q2Q0;
            rotationMatrix[3] = 0.0f;

            rotationMatrix[4] = q1Q2 + q3Q0;
            rotationMatrix[5] = 1 - sqQ1 - sqQ3;
            rotationMatrix[6] = q2Q3 - q1Q0;
            rotationMatrix[7] = 0.0f;

            rotationMatrix[8] = q1Q3 - q2Q0;
            rotationMatrix[9] = q2Q3 + q1Q0;
            rotationMatrix[10] = 1 - sqQ1 - sqQ2;
            rotationMatrix[11] = 0.0f;

            rotationMatrix[12] = rotationMatrix[13] = rotationMatrix[14] = 0.0f;
            rotationMatrix[15] = 1.0f;
        }
    }

    /**
     * Normalize the given value with the given current range to a value in the new
     * range.
     *
     * @param oldValue Given value in current range
     * @param oldMin   Current range minimum value
     * @param oldMax   Current range maximum value
     * @param newMin   New range minimum value
     * @param newMax   New range maximum value
     * @return Converted integer value of oldValue in the new range
     */
    public static int normalize(int oldValue, int oldMin, int oldMax, int newMin, int newMax) {
        int oldRange = (oldMax - oldMin);
        int newRange = (newMax - newMin);
        if (oldRange == 0) oldRange = 1;
        return (((oldValue - oldMin) * newRange) / oldRange) + newMin;
    }

    /**
     * Check if the string is valid integer
     *
     * @param number String representing the number
     * @return boolean true - string is number
     */
    public static boolean isInteger(String number) {
        try {
            Integer.parseInt(number);
            return true;

        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
