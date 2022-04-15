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

import android.content.Context
import androidx.annotation.DimenRes
import androidx.annotation.Px
import dji.v5.ux.R

/**
 * Configuration properties to initialize a panel.
 * @param context asd
 * @property panelWidgetType The type of panel widget
 * @property showTitleBar Boolean to hide or show the whole title bar
 * @property hasCloseButton Boolean to hide or show the close button
 * @property panelTitle Default panel title text
 * @param titleBarHeightDimensionResID [DimenRes] for the height of the title bar
 */
class PanelWidgetConfiguration(
        context: Context,
        var panelWidgetType: PanelWidgetType,
        var showTitleBar: Boolean = false,
        var hasCloseButton: Boolean = false,
        var panelTitle: String = "",
        @DimenRes titleBarHeightDimensionResID: Int = R.dimen.uxsdk_top_bar_default_height
) {
    /**
     * The title bar height in pixels.
     */
    @Px
    var titleBarHeight: Float = context.resources.getDimension(titleBarHeightDimensionResID)

    /**
     * Check if the [PanelWidget] is a [BarPanelWidget].
     */
    fun isBarPanelWidget(): Boolean =
            panelWidgetType == PanelWidgetType.BAR_HORIZONTAL
                    || panelWidgetType == PanelWidgetType.BAR_VERTICAL
}
