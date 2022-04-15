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
import androidx.annotation.IntRange
import dji.sdk.keyvalue.value.product.ProductType
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.core.base.SchedulerProvider
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.processors.PublishProcessor
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.panel.systemstatus.SmartListInternalModel
import dji.v5.ux.core.util.RxUtil

/**
 * The [String] to identify a widget.
 */
typealias WidgetID = String

/**
 * The [SmartListModel] handles the creation and order of child views for a [ListPanelWidget] by holding
 * a reference to ListPanelWidget's [ListPanelWidgetBaseModel].
 *
 * The [SmartListModel] controls when the child view is created and it may encapsulate a WidgetModel to
 * handheld business logic for when to show/hide items.
 *
 * When creating a [SmartListModel], widgets are registered with [registeredWidgetIDList]. This
 * list defines all the [WidgetID]s that are allowed in this panel.
 * A widget is said to become active, when its [WidgetID] is passed in a set with [buildAndInstallWidgets].
 * Thus, An active widget is a widget that is currently being shown and its object has been created.
 * Once a widget is active at least once, the [SmartListModel] will hold a reference until the [SmartListModel]
 * is destroyed.
 */
abstract class SmartListModel @JvmOverloads constructor(
        protected val context: Context,
        private val attrs: AttributeSet? = null,
        private val excludedItems: Set<WidgetID>? = null) {

    //region Properties
    /**
     * Defines what [WidgetID]s are allowed into this [SmartListModel].
     * This list also defines the order of all the items.
     * Override this to register more [WidgetID]s.
     */
    abstract val registeredWidgetIDList: List<WidgetID>

    /**
     * The default active widgets that will be created and shown.
     */
    abstract val defaultActiveWidgetSet: Set<WidgetID>

    /**
     * The size of the current active widgets.
     */
    val activeWidgetSize: Int
        get() = activeWidgetList.size

    /**
     * The size of all widgets registered in this [SmartListModel].
     */
    val totalWidgetSize: Int
        get() = registeredWidgetIDList.size

    /**
     * [Flowable] to observe when a widget has been created.
     * Emits a [Pair] of [WidgetID] and [View].
     */
    val widgetCreated: Flowable<Pair<WidgetID, View>>
        get() = widgetCreatedProcessor

    /**
     * Widget model to detect the connected product.
     */
    private var widgetModel : SmartListInternalModel? = null
    private var currentOrderList: MutableList<WidgetID> = mutableListOf()
    private val createdWidgetsMap: MutableMap<WidgetID, View> = mutableMapOf()
    private var activeWidgetList: List<View> = emptyList()
    private var activeWidgetSet: Set<WidgetID> = emptySet()
    private var listPanelWidgetBaseModel: ListPanelWidgetBaseModel? = null
    private var widgetCreatedProcessor: PublishProcessor<Pair<WidgetID, View>> = PublishProcessor.create()

    private var compositeDisposable: CompositeDisposable? = null
    //endregion

    //region Lifecycle
    /**
     * Set up the [SmartListModel] by initializing all the required resources.
     */
    fun setUp() {
        currentOrderList = registeredWidgetIDList.toMutableList()
        buildAndInstallWidgets(defaultActiveWidgetSet)
        compositeDisposable = CompositeDisposable()
        widgetModel = SmartListInternalModel(djiSdkModel = DJISDKModel.getInstance(), uxKeyManager = ObservableInMemoryKeyedStore.getInstance())
        widgetModel?.setup()
        addDisposable(widgetModel!!.productConnection.observeOn(SchedulerProvider.ui())
                .subscribe(Consumer { onProductConnectionChanged(it) },
                        RxUtil.logErrorConsumer("SmartListModel", "Error on Product changed. ")))
        addDisposable(widgetModel!!.aircraftModel.observeOn(SchedulerProvider.ui())
                .subscribe(Consumer { onAircraftModelChanged(it) },
                        RxUtil.logErrorConsumer("SmartListModel", "Error on Aircraft Model Changed. ")))

        inSetUp()

    }

    /**
     * Clean up the [SmartListModel] by destroying all the resources used.
     */
    fun cleanUp() {
        inCleanUp()
        widgetModel?.cleanup()
        disposeAll()
    }

    /**
     * Setup method for initialization that must be implemented
     */
    protected abstract fun inSetUp()

    /**
     * Cleanup method for post-usage destruction that must be implemented
     */
    protected abstract fun inCleanUp()

    /**
     * Setup the list based product connection
     */
    protected abstract fun onProductConnectionChanged(isConnected: Boolean)

    /**
     * Setup the list based on the connected aircraft
     */
    protected abstract fun onAircraftModelChanged(model: ProductType)
    //endregion

    //region Installing Widgets
    /**
     * Pass a new [Set] of active [WidgetID].
     * It will create widgets that have not been created and emit them to [ListPanelWidget].
     */
    protected fun buildAndInstallWidgets(newActiveWidgetIDs: Set<WidgetID>) {
        buildActiveWidgetList(newActiveWidgetIDs)
        updateHolder()
    }

    /**
     * Callback for when a [View] needs to be created for the given [WidgetID].
     */
    protected abstract fun createWidget(widgetID: WidgetID): View

    /**
     * Set a reference to a [ListPanelWidgetBaseModel].
     * Must be set to communicate changes to the [ListPanelWidget].
     */
    fun setListPanelWidgetHolder(listPanelWidgetBaseModel: ListPanelWidgetBaseModel) {
        this.listPanelWidgetBaseModel = listPanelWidgetBaseModel
    }

    /**
     * Get [View] at [index] from the active widget list.
     */
    fun getActiveWidget(@IntRange(from = 0) index: Int): View? = activeWidgetList.getOrNull(index)

    /**
     * Get [View] with [widgetID] from the active widget list.
     */
    fun getActiveWidget(widgetID: WidgetID): View? {
        if (activeWidgetSet.contains(widgetID)) return createdWidgetsMap[widgetID]
        return null
    }

    /**
     * Get the [WidgetID] at [index] from the current order list.
     * This is the order of all active and inactive widgets.
     */
    fun getWidgetID(@IntRange(from = 0) index: Int): WidgetID? = currentOrderList.getOrNull(index)

    /**
     * Get the [View] for the [WidgetID]. Only available if the widget has become active at least once.
     */
    fun getWidget(widgetID: WidgetID): View? = createdWidgetsMap[widgetID]

    /**
     * Ge the index for the [WidgetID] from the current order list.
     * This is the order of all active and inactive widgets.
     */
    fun getWidgetIndex(widgetID: WidgetID): Int {
        currentOrderList.forEachIndexed { index, currWidgetId ->
            if (currWidgetId == widgetID) return index
        }

        return -1
    }

    /**
     * Change the [index] for a [WidgetID], effectively changing its order in
     * the list of active and inactive widgets.
     */
    fun setIndex(@IntRange(from = 0) index: Int, widgetID: WidgetID) {
        if (index >= currentOrderList.size) return
        var currentIndex = -1
        currentOrderList.forEachIndexed { i, value ->
            if (value == widgetID) currentIndex = i
        }
        if (currentIndex != -1) {
            currentOrderList.removeAt(currentIndex)
            currentOrderList.add(index, widgetID)

            reorderActiveWidgets()
            updateHolder()
        }
    }

    /**
     * Change the order of a [WidgetID] from one index to another, effectively changing its order in
     * the list of active and inactive widgets.
     */
    fun setIndex(@IntRange(from = 0) fromIndex: Int, @IntRange(from = 0) toIndex: Int) {
        if (fromIndex >= currentOrderList.size || toIndex >= currentOrderList.size) return
        val widgetID = currentOrderList.removeAt(fromIndex)
        currentOrderList.add(toIndex, widgetID)

        reorderActiveWidgets()
        updateHolder()
    }

    /**
     * Remove items from the current active list.
     */
    protected fun updateListMinus(vararg itemToRemove: WidgetID) {
        val newOrder = activeWidgetSet.minus(itemToRemove.toSet())
        buildAndInstallWidgets(newOrder)
    }

    /**
     * Add items from the current active list.
     */
    protected fun updateListPlus(vararg itemToAdd: WidgetID) {
        val newOrder = activeWidgetSet.plus(itemToAdd.toSet())
        buildAndInstallWidgets(newOrder)
    }
    //endregion

    //region Helpers
    /**
     * Add a disposable which is automatically disposed with the [SmartListModel]'s lifecycle.
     *
     * @param disposable the disposable to add
     */
    protected fun addDisposable(disposable: Disposable) {
        compositeDisposable?.add(disposable)
    }

    private fun disposeAll() {
        compositeDisposable?.dispose()
        compositeDisposable = null
    }

    private fun updateHolder() {
        listPanelWidgetBaseModel?.addWidgets(activeWidgetList)
    }

    private fun WidgetID.isNotExcluded(): Boolean = excludedItems == null || !excludedItems.contains(this)

    private fun buildActiveWidgetList(newActiveWidgetIDs: Set<WidgetID>) {
        // Prevent widgetIDs that were not originally registered
        activeWidgetSet = newActiveWidgetIDs.intersect(registeredWidgetIDList)
        // Create views if they don't exist
        activeWidgetSet
                .filter { widgetID ->
                    widgetID.isNotExcluded() && !createdWidgetsMap.containsKey(widgetID)
                }
                .map { widgetID ->
                    val createdWidget = createWidget(widgetID)
                    widgetCreatedProcessor.onNext(widgetID to createdWidget)
                    createdWidgetsMap[widgetID] = createdWidget
                    createdWidget
                }


        // Create new list based on sorted items
        reorderActiveWidgets()
    }

    private fun reorderActiveWidgets() {
        activeWidgetList = currentOrderList
                .filter { widgetID ->
                    widgetID.isNotExcluded()
                            && activeWidgetSet.contains(widgetID)
                            && createdWidgetsMap.containsKey(widgetID)
                }.map { originalIndex -> createdWidgetsMap[originalIndex] as View }
    }
    //endregion

}