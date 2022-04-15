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

import androidx.annotation.IntRange

/**
 * Enum representing the type of panel widget created.
 * @param value Int value
 */
enum class PanelWidgetType(val value: Int) {
    /**
     * Vertical type for [BarPanelWidget]
     */
    BAR_VERTICAL(0),

    /**
     * Horizontal type for [BarPanelWidget]
     */
    BAR_HORIZONTAL(1),

    /**
     * Vertical list type for [ListPanelWidget]
     */
    LIST(2),

    /**
     *  A custom structure formed using [FreeFormPanelWidget]
     */
    FREE_FORM(3),

    /**
     *  A custom structure formed using [ToolbarPanelWidget]
     */
    TOOLBAR_LEFT(4),

    /**
     *  A custom structure formed using [ToolbarPanelWidget]
     */
    TOOLBAR_RIGHT(5),

    /**
     *  A custom structure formed using [ToolbarPanelWidget]
     */
    TOOLBAR_TOP(6);

    companion object {
        @JvmStatic
        val values = values()

        /**
         * Find a [PanelWidgetType] from an int value.
         */
        @JvmStatic
        fun find(@IntRange(from = 0, to = 3) index: Int): PanelWidgetType = values[index]
    }
}
