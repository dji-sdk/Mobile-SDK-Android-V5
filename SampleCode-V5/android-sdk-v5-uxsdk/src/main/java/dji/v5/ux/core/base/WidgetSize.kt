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

package dji.v5.ux.core.base


/**
 * Describe how the widget should be sized.
 * @param sizeType Type of size for the widget
 * @param widthDimension The type of dimension of a widget's width
 * @param heightDimension The type of dimension of a widget's width
 */
data class WidgetSizeDescription(
        val sizeType: SizeType,
        val widthDimension: Dimension = Dimension.EXPAND,
        val heightDimension: Dimension = Dimension.EXPAND
) {
    /**
     * The size type, whether is a ratio or any other type.
     */
    enum class SizeType {
        /**
         * The size of widget can be dynamic but should follow a ratio
         */
        RATIO,

        /**
         * The size of the widget is anything other than a ratio
         */
        OTHER
    }

    /**
     * The dimension of the widget, whether it should expand or wrap.
     */
    enum class Dimension {
        /**
         * Indicates that the dimension can be expanded.
         * Similar to use 0dp in ConstraintLayout, or match parent in other layouts.
         */
        EXPAND,

        /**
         * Indicates that the widget should be wrapped, that is, the widget decides its own size.
         */
        WRAP
    }
}