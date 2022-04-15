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
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.core.util.DataProcessor

// TODO:
// * Documentation UX-5195

/**
 * The base model for the [ListPanelWidget]
 */
class ListPanelWidgetBaseModel {

    //region Properties
    /**
     * [Flowable] for the current widget list
     */
    val widgetList: Flowable<List<View>>
        get() = widgetListProcessor.toFlowable()

    private val widgetListProcessor: DataProcessor<List<View>> = DataProcessor.create(emptyList())
    //endregion

    //region List Helpers
    /**
     * Add a new [List] of [View].
     */
    fun addWidgets(widgetList: List<View>) {
        widgetListProcessor.onNext(widgetList)
    }

    /**
     * Add a [View] to the current list of widgets.
     */
    fun addWidget(widget: View) {
        val newList = widgetListProcessor.value + listOf(widget)

        widgetListProcessor.onNext(newList)
    }

    /**
     * Add a [View] at [index] to the current list of widgets.
     */
    fun addWidget(@IntRange(from = 0) index: Int, widget: View) {
        val prevList = widgetListProcessor.value
        if (index >= prevList.size) return

        val newList = when (index) {
            0 -> listOf(widget) + prevList
            prevList.size -> prevList + listOf(widget)
            else ->
                prevList.subList(0, index) + listOf(widget) + prevList.subList(index, prevList.size)

        }
        widgetListProcessor.onNext(newList)
    }

    /**
     * Remove a [View] at [index] from the current list of widgets.
     */
    fun removeWidget(@IntRange(from = 0) index: Int): View? {
        val prevList = widgetListProcessor.value
        if (index >= prevList.size) return null
        val newList = prevList.minus(prevList[index])
        widgetListProcessor.onNext(newList)
        return prevList[index]
    }

    /**
     * Remove all [View]s.
     */
    fun removeAllWidgets() {
        widgetListProcessor.onNext(emptyList<View>())
    }

    /**
     * Get the [View] at index from the current list of widgets.
     */
    fun getWidget(@IntRange(from = 0) index: Int): View? = widgetListProcessor.value.getOrNull(index)

    /**
     * Total size of the current list of widgets.
     */
    fun size(): Int = widgetListProcessor.value.size
    //endregion
}