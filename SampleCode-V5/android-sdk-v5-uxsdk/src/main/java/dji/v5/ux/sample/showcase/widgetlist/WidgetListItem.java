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

package dji.v5.ux.sample.showcase.widgetlist;

import androidx.annotation.StringRes;

/**
 * An item within the widget list.
 */
public class WidgetListItem {

    //region Fields
    @StringRes
    private final int titleId;
    private final WidgetViewHolder<?>[] widgetViewHolders;
    //endregion

    /**
     * Create a new {@link WidgetListItem} with the given title and the given widgetViewHolders.
     *
     * @param titleId A resource ID of the title to display within the widget list.
     * @param widgetViewHolders An array of {@link WidgetViewHolder} objects to display.
     */
    public WidgetListItem(@StringRes int titleId, WidgetViewHolder<?>... widgetViewHolders) {
        this.titleId = titleId;
        this.widgetViewHolders = widgetViewHolders;
    }

    /**
     * Get the resource ID of the title to display within the widget list.
     *
     * @return A resource ID of the title to display within the widget list.
     */
    @StringRes
    public int getTitleId() {
        return titleId;
    }

    /**
     * Get the array of {@link WidgetViewHolder} objects to display.
     *
     * @return An array of {@link WidgetViewHolder} objects to display
     */
    public WidgetViewHolder<?>[] getWidgetViewHolders() {
        return widgetViewHolders;
    }
}
