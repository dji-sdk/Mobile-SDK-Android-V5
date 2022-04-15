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

package dji.v5.ux.core.base.panel

import android.view.View
import androidx.annotation.IntRange
import dji.v5.ux.core.base.WidgetSizeDescription
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.base.widget.FrameLayoutWidget

/**
 * Encapsulates a View with properties used to define how it is laid out.
 * @property view The child view to add into a panel.
 * @property itemMarginLeft Left margin pixel size, null by default
 * @property itemMarginTop Top margin pixel size, null by default
 * @property itemMarginRight Right margin pixel size, null by default
 * @property itemMarginBottom Bottom margin pixel size, null by default
 * @param desiredWidgetSizeDescription Widget size description, null by default
 * @param desiredRatioString Desired ratio for the view of the width to height, null by default
 */
class PanelItem(
        val view: View,
        @IntRange(from = 0) val itemMarginLeft: Int? = null,
        @IntRange(from = 0) val itemMarginTop: Int? = null,
        @IntRange(from = 0) val itemMarginRight: Int? = null,
        @IntRange(from = 0) val itemMarginBottom: Int? = null,
        desiredWidgetSizeDescription: WidgetSizeDescription? = null,
        desiredRatioString: String? = null) {

    /**
     * The default [WidgetSizeDescription].
     */
    val widgetSizeDescription: WidgetSizeDescription? = desiredWidgetSizeDescription
            ?: when (view) {
                is FrameLayoutWidget<*> -> view.widgetSizeDescription
                is ConstraintLayoutWidget<*> -> view.widgetSizeDescription
                else -> null
            }

    /**
     * The default ratio [String] of the width to height.
     */
    val ratioString: String? = desiredRatioString
            ?: when (view) {
                is ConstraintLayoutWidget<*> -> view.getIdealDimensionRatioString()
                is FrameLayoutWidget<*> -> view.getIdealDimensionRatioString()
                else -> null
            }
}
