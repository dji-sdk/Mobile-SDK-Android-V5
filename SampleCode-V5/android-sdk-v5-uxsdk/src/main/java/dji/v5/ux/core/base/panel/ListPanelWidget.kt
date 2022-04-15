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
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.util.ViewIDGenerator

/**
 * Base class for [ListPanelWidget]s. A [ListPanelWidget] is a vertical collection of widgets.
 * The child widgets of a [ListPanelWidget], unlike those of a [BarPanelWidget],
 * are created dynamically and the list can change based on changes at the MSDK level.
 *
 * This list is not meant to be used as an infinite list, but rather for a limited number of widgets.
 * For infinite lists, prefer to use a RecyclerView.
 *
 * Widgets in this list are placed with the following properties:
 * - Width: MATCH_PARENT
 * - Height: WRAP_CONTENT
 *
 * The ListPanelWidget may contain a default [SmartListModel], which injects views into this
 * ListPanelWidget. The [SmartListModel] can also be overwritten by setting [smartListModel], allowing
 * the user to change the behavior of the list.
 *
 * Customization:
 * The [ListPanelWidget] uses a ListView internally. To customize the dividers, user can use the
 * ListView's attributes:
 * android:divider="#FFCCFF"
 * android:dividerHeight="4dp"
 * User can also remove the dividers:
 * android:divider="@null"
 */
abstract class ListPanelWidget<T> @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        configuration: PanelWidgetConfiguration
) : PanelWidget<View, T>(context, attrs, defStyleAttr, configuration) {

    //region Properties
    /**
     * Optional [SmartListModel].
     * Setting a new instance refreshes the whole list.
     */
    var smartListModel: SmartListModel? = null
        set(value) {
            field = value
            field?.setListPanelWidgetHolder(listPanelWidgetBaseModel)
            if (ViewCompat.isAttachedToWindow(this)) {
                field?.setUp()
            }
            if (field != null) {
                onSmartListModelCreated()
            }
        }

    /**
     * Default [ListPanelWidgetBaseModel], can be overwritten.
     */
    protected open val listPanelWidgetBaseModel: ListPanelWidgetBaseModel = ListPanelWidgetBaseModel()
    private val adapter = Adapter()
    //endregion

    //region Constructor
    init {
        check(panelWidgetConfiguration.panelWidgetType == PanelWidgetType.LIST) {
            "PanelWidgetConfiguration.panelWidgetType should be PanelWidgetType.LIST"
        }

        setUpListView(attrs)
        // Set padding on the parent to 0, so only the listview can change padding
        setPadding(0, 0, 0, 0)
    }

    private fun setUpListView(attrs: AttributeSet?) {
        val listView = ListView(context, attrs)
        listView.id = ViewIDGenerator.generateViewId()
        listView.adapter = adapter
        addView(listView)
        listView.visibility = View.VISIBLE

        val constraintSet = ConstraintSet()
        constraintSet.clone(this)

        constraintSet.constrainWidth(listView.id, 0)
        constraintSet.constrainHeight(listView.id, 0)
        constraintSet.constraintToParentStart(listView)
        constraintSet.constraintToParentEnd(listView)
        constraintSet.constraintToParentBottom(listView)
        constraintSet.constraintToParentTop(listView)

        constraintSet.applyTo(this)
    }
    //endregion

    //region Lifecycle
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            smartListModel?.setUp()

            listPanelWidgetBaseModel.widgetList
                    .observeOn(SchedulerProvider.ui())
                    .subscribe { updateUI() }
        }
    }

    override fun onDetachedFromWindow() {
        smartListModel?.cleanUp()
        super.onDetachedFromWindow()
    }


    /**
     * Call to refresh the list.
     */
    override fun updateUI() {
        adapter.notifyDataSetChanged()
    }

    /**
     * Callback for when a new [SmartListModel] is created.
     */
    protected abstract fun onSmartListModelCreated()
    //endregion

    //region Populate Panel
    /**
     * Get the [View] at index from the current list of widgets.
     */
    override fun getWidget(index: Int): View? {
        smartListModel?.let { return it.getActiveWidget(index) }
        return listPanelWidgetBaseModel.getWidget(index)
    }

    /**
     * Add a new [List] of [View].
     */
    override fun addWidgets(items: Array<View>) {
        if (smartListModel == null) {
            listPanelWidgetBaseModel.addWidgets(items.toList())
        }
    }

    /**
     * Total size of [View] in the current list of widgets.
     */
    override fun size(): Int {
        smartListModel?.let { return it.activeWidgetSize }
        return listPanelWidgetBaseModel.size()
    }

    /**
     * Add a [View] at [index] to the current list of widgets.
     */
    override fun addWidget(index: Int, view: View) {
        if (smartListModel == null) {
            listPanelWidgetBaseModel.addWidget(index, view)
        }
    }

    /**
     * Remove a [View] at [index] from the current list of widgets.
     */
    override fun removeWidget(index: Int): View? {
        if (smartListModel == null) {
            return listPanelWidgetBaseModel.removeWidget(index)
        }
        return null
    }

    /**
     * Remove all [View]s.
     */
    override fun removeAllWidgets() {
        if (smartListModel == null) {
            listPanelWidgetBaseModel.removeAllWidgets()
        }
    }
    //endregion

    //region Customization
    override fun getIdealDimensionRatioString(): String? = null
    //endregion

    private inner class Adapter : BaseAdapter() {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = listPanelWidgetBaseModel.getWidget(position)
                    ?: throw IllegalAccessException("View not found at position $position")
            if (view is Navigable) {
                view.panelNavigator = this@ListPanelWidget.panelNavigator
            }
            view.setHasTransientState(true)
            return view
        }

        override fun getItem(position: Int): Any {
            return listPanelWidgetBaseModel.getWidget(position)
                    ?: throw IllegalAccessException("Item not found at position $position")
        }

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getCount(): Int = listPanelWidgetBaseModel.size()

    }
}