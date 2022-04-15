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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Px;

/**
 * Utility class for converting display units.
 */
public final class DisplayUtil {

    private DisplayUtil() {
        // Util class
    }

    /**
     * Converts a pixel value to a density-independent pixel value.
     *
     * @param context A {@link Context} instance.
     * @param pxValue The pixel value to convert.
     * @return A density-independent pixel value.
     */
    public static float pxToDip(@NonNull Context context, @Px float pxValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return pxValue / scale + 0.5f;
    }

    /**
     * Converts a density-independent pixel value to a pixel value.
     *
     * @param context  A {@link Context} instance.
     * @param dipValue The density-independent pixel value to convert.
     * @return A pixel value.
     */
    @Px
    public static float dipToPx(@NonNull Context context, float dipValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return dipValue * scale + 0.5f;
    }

    /**
     * Converts a pixel value to a scale-independent pixel value.
     *
     * @param context A {@link Context} instance.
     * @param pxValue The pixel value to convert.
     * @return A scale-independent pixel value.
     */
    public static float pxToSp(@NonNull Context context, @Px float pxValue) {
        float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return pxValue / fontScale + 0.5f;
    }

    /**
     * Converts a scale-independent pixel value to a pixel value.
     *
     * @param context A {@link Context} instance.
     * @param spValue The scale-independent pixel value to convert.
     * @return A pixel value.
     */
    @Px
    public static float spToPx(@NonNull Context context, float spValue) {
        float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return spValue * fontScale + 0.5f;
    }
}

