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
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.contains
import dji.v5.ux.R
import dji.v5.ux.core.extension.getColor
import dji.v5.ux.core.extension.hide
import dji.v5.ux.core.extension.show
import dji.v5.ux.core.util.ViewIDGenerator
import kotlin.random.Random

/**
 * Base class for [FreeFormPanelWidget].
 * A [FreeFormPanelWidget] is a panel containing splittable panes.
 * Out of the box the panel contains only one pane.
 * Based on the requirement, a pane can be split into rows or columns with proportion of choice.
 * Each pane can hold one view or widget and can be split only once. If a pane containing a view or
 * a widget is split, the view/widget will be removed permanently before splitting the pane.
 * Use [FreeFormPanelWidget.splitPane] to split a pane.
 * A pane which has been split can be unified by merging its children. If any child of the pane to
 * be merged contains a view or a widget, the view/widget will be removed permanently.
 * Use [FreeFormPanelWidget.mergeChildren] to merge the children of a pane.
 * Use [FreeFormPanelWidget.mergeSiblings] to merge the siblings of a pane.
 *
 * The class provides helper methods to add, remove and get view/widget in a pane.
 *
 */
abstract class FreeFormPanelWidget<T> @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        configuration: PanelWidgetConfiguration
) : PanelWidget<PanelItem, T>(
        context,
        attrs,
        defStyleAttr,
        configuration) {

    //region Fields
    private val paneMap: HashMap<Int, Pane> = hashMapOf()

    private var isLabelAssistEnabled: Boolean = false

    private var isBackgroundAssistEnabled: Boolean = false

    /**
     * Color of the text for in debug labels.
     */
    var debugTextColor: Int = getColor(R.color.uxsdk_white)

    /**
     * Color of the background of debug labels.
     */
    var debugTextBackgroundColor = getColor(R.color.uxsdk_black)

    /**
     * ID of root pane.
     */
    val rootID: Int = ViewIDGenerator.generateViewId()

    /**
     * List of the currently visible pane IDs.
     */
    val listOfViewIds: List<Int>
        get() = paneMap.filter { !it.value.isSplit }.keys.toList()
    //endregion

    //region Lifecycle
    init {
        check(panelWidgetConfiguration.panelWidgetType == PanelWidgetType.FREE_FORM) {
            "PanelWidgetConfiguration.panelWidgetType should be PanelWidgetType.FREE_FORM"
        }
        val defaultView = View(context)
        addView(defaultView, childCount)
        defaultView.id = rootID
        defaultView.setBackgroundColor(getColor(R.color.uxsdk_yellow))
        val constraintSet = ConstraintSet()
        constraintSet.clone(this)
        constraintSet.constrainHeight(rootID, 0)
        constraintSet.constrainWidth(rootID, 0)
        constraintSet.constraintToParentStart(defaultView)
        constraintSet.constraintToParentEnd(defaultView)
        constraintSet.constraintToParentBottom(defaultView)
        constraintSet.constraintToParentTop(defaultView)
        constraintSet.applyTo(this)
        paneMap[rootID] = Pane(id = rootID, parentId = -1, background = defaultView)
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        // Empty method
    }

    override fun initPanelWidget(context: Context, attrs: AttributeSet?, defStyleAttr: Int, widgetConfiguration: PanelWidgetConfiguration?) {
        // No implementation
    }


    @Throws(UnsupportedOperationException::class)
    override fun getWidget(index: Int): PanelItem? =
            throw UnsupportedOperationException("Try findViewPane instead")

    @Throws(UnsupportedOperationException::class)
    override fun addWidgets(items: Array<PanelItem>) =
            throw UnsupportedOperationException(("This call is not supported."))

    @Throws(UnsupportedOperationException::class)
    override fun size(): Int =
            throw UnsupportedOperationException(("This call is not supported."))

    @Throws(UnsupportedOperationException::class)
    override fun addWidget(index: Int, item: PanelItem) =
            throw UnsupportedOperationException("Try addView instead")

    @Throws(UnsupportedOperationException::class)
    override fun removeWidget(index: Int): PanelItem? =
            throw UnsupportedOperationException("Try remove view instead")

    @Throws(UnsupportedOperationException::class)
    override fun updateUI() =
            throw UnsupportedOperationException(("This call is not supported."))

    override fun removeAllWidgets() {
        for ((_, pane) in paneMap) {
            removeView(pane.view)
            pane.view = null
        }
    }
    //endregion

    //region Panel Customizations
    /**
     * Split pane into multiple panes.
     *
     * @param paneId - the ID of the pane that needs to be split.
     * @param splitType - instance of [SplitType] representing the axis to use for the splits.
     * @param proportions - float array representing the proportions in which the pane should be.
     *                      split. The sum of the proportions should not exceed 1. If the sum is less
     *                      than 1 the balance will be added to the last pane.
     * @return List of integer IDs of the newly created panes.
     */
    fun splitPane(paneId: Int, splitType: SplitType, proportions: Array<Float>): List<Int> {
        // Pane has already been split or pane does not exist
        val parentPane = paneMap[paneId]
        if (parentPane == null || parentPane.isSplit) {
            return emptyList()
        }

        // Pane cannot be split into less than 2 parts
        if (proportions.size < 2) {
            return emptyList()
        }

        val sum = proportions.sum()

        // Sum of proportions cannot exceed 1 which is the total size
        if (sum > 1f) {
            return emptyList()
        }

        // Remove all the debug labels
        removeDebugViews()

        // If a view / widget exists in the current pane, remove it
        if (parentPane.view != null) {
            removeView(paneId)
        }

        val set = ConstraintSet()
        set.clone(this)

        val childrenViewList = arrayListOf<View>()
        val childrenIdList = IntArray(proportions.size)

        val weights = proportions.toFloatArray()

        // Loop over number of proportions and create child elements
        proportions.forEachIndexed { index, _ ->
            val childView = View(context)
            childView.id = ViewIDGenerator.generateViewId()
            childrenIdList[index] = childView.id
            childrenViewList.add(childView)
            addView(childView, childCount)
            val childPane = Pane(id = childView.id, parentId = parentPane.id, background = childView)
            paneMap[childView.id] = childPane
            if (splitType == SplitType.HORIZONTAL) {
                // If Split type is horizontal make height 100%
                set.constrainPercentHeight(childrenViewList[index].id, 1f)
                set.connect(childView.id, ConstraintSet.TOP, paneId, ConstraintSet.TOP)
                set.connect(childView.id, ConstraintSet.BOTTOM, paneId, ConstraintSet.BOTTOM)
            } else {
                // If Split type is vertical make width 100%
                set.constrainPercentWidth(childrenViewList[index].id, 1f)
                set.connect(childView.id, ConstraintSet.LEFT, paneId, ConstraintSet.LEFT)
                set.connect(childView.id, ConstraintSet.RIGHT, paneId, ConstraintSet.RIGHT)
            }
        }

        // Adjust the last pane proportion to compensate for the left over space.
        proportions[proportions.lastIndex] = 1.0f - sum + proportions[proportions.lastIndex]

        if (splitType == SplitType.HORIZONTAL) {
            set.createHorizontalChain(paneId,
                    ConstraintSet.LEFT,
                    paneId,
                    ConstraintSet.RIGHT,
                    childrenIdList,
                    weights,
                    ConstraintSet.CHAIN_SPREAD)
        } else {
            set.createVerticalChain(paneId,
                    ConstraintSet.TOP,
                    paneId,
                    ConstraintSet.BOTTOM,
                    childrenIdList,
                    weights,
                    ConstraintSet.CHAIN_SPREAD)
        }
        set.applyTo(this)

        // Assign the list of children to the pane
        parentPane.childrenIdList = childrenIdList.toList()
        parentPane.isSplit = true

        // Assign the list of children as siblings to each other
        childrenIdList.forEach { childId -> paneMap[childId]?.siblingIdList = childrenIdList.toList() }

        //Add debug labels if debugging is enabled
        addDebugViews(isLabelAssistEnabled, isBackgroundAssistEnabled)
        return childrenIdList.toList()

    }

    /**
     * The method addView installs a descendant of [View] into the designated pane,
     * removing the existing view already there.
     *
     * @param paneId - the ID of the pane to which view should be added.
     * @param widgetView - the instance of widget or view to be added to the pane.
     * @param position - instance of [ViewAlignment] representing the position of the view in the pane.
     * @param leftMargin - Optional left margin for the widget or view. Defaults to 0.
     * @param topMargin - Optional top margin for the widget or view. Defaults to 0.
     * @param rightMargin - Optional right margin for the widget or view. Defaults to 0.
     * @param bottomMargin - Optional bottom margin for the widget or view. Defaults to 0.
     */
    fun addView(paneId: Int, widgetView: View, position: ViewAlignment = ViewAlignment.CENTER,
                leftMargin: Int = 0, topMargin: Int = 0, rightMargin: Int = 0, bottomMargin: Int = 0) {
        val pane = paneMap[paneId] ?: return
        removeView(paneId)
        if (widgetView.id == -1) {
            widgetView.id = ViewIDGenerator.generateViewId()
        }
        addView(widgetView, childCount)
        setViewAlignment(paneId, widgetView, position, leftMargin, topMargin, rightMargin, bottomMargin)
        pane.view = widgetView
        pane.position = position
    }

    /**
     * Remove the view in a pane.
     *
     * @param - ID of the pane from which the view should be removed.
     */
    fun removeView(paneId: Int) {
        val pane = paneMap[paneId] ?: return
        val viewToRemove = pane.view ?: return
        val set = ConstraintSet()
        set.clone(this)
        set.clear(viewToRemove.id)
        set.applyTo(this)
        if (contains(viewToRemove)) {
            removeView(viewToRemove)
        }
        pane.view = null
    }

    /**
     * The method returns the integer ID of the pane in which the given view in
     * installed or -1.
     */
    fun findViewPane(view: View): Int {
        val key = paneMap.filter { it.value.view == view }.keys.firstOrNull()
        return key ?: -1
    }

    /**
     * The method returns the view installed in the given pane ID.
     */
    fun viewInPane(paneId: Int): View? {
        return paneMap[paneId]?.view
    }

    /**
     * Get the list of siblings of a pane.
     */
    fun getSiblingList(paneId: Int): List<Int> {
        val list = paneMap[paneId]?.siblingIdList
        return list ?: emptyList()
    }

    /**
     * Get the list of children of a pane.
     */
    fun getChildrenList(paneId: Int): List<Int> {
        val list = paneMap[paneId]?.childrenIdList
        return list ?: emptyList()
    }

    /**
     * Get the parent id of a pane.
     */
    fun getParentId(paneId: Int): Int {
        val parentId = paneMap[paneId]?.parentId
        return parentId ?: -1
    }

    /**
     * The method enablePaneDebug activates or deactivates some visual debugging tools
     * for the Freeform panel. These tools can help you quickly debug how your panes are
     * appearing and where, and how large they are.
     *
     *
     * @param enableLabelAssist - Turns debugging PaneIDs on or off in each pane.
     * @param enableBackgroundAssist - This optional boolean sets the background color of panes
     *                                 using a rotating list of colors to help visualize where
     *                                 each pane displays. Defaults to off.
     * @param textColor - This optional parameter sets the UIColor of the text label for each pane
     *                    created after the assist is turned on. Defaults to white.
     * @param textBackgroundColor - This optional parameter sets the UIColor to use as the
     *                              background color of the text label. Defaults to black.
     */
    fun enablePaneDebug(enableLabelAssist: Boolean, enableBackgroundAssist: Boolean = false,
                        @ColorInt textColor: Int = INVALID_COLOR, @ColorInt textBackgroundColor: Int = INVALID_COLOR) {
        if (textColor != INVALID_COLOR) {
            debugTextColor = textColor
        }
        if (textBackgroundColor != INVALID_COLOR) {
            debugTextBackgroundColor = textBackgroundColor
        }

        isLabelAssistEnabled = enableLabelAssist
        isBackgroundAssistEnabled = enableBackgroundAssist
        removeDebugViews()
        addDebugViews(isLabelAssistEnabled, isBackgroundAssistEnabled)
    }

    /**
     * Call method mergeChildren to merge all the children of a pane back to form the original pane.
     * This deletes all the children and re-exposes the parent pane.
     *
     * @param paneId - The paneID to be revealed after removing the child panes.
     */
    fun mergeChildren(paneId: Int) {
        removeDebugViews()
        mergePaneChildren(paneId)
        addDebugViews(isLabelAssistEnabled, isBackgroundAssistEnabled)
    }

    /**
     * Call method mergeSiblings to merge all the siblings of a pane back to form the parent pane.
     * This deletes all the siblings and re-exposes the parent pane.
     *
     * @param paneId - The paneID to be deleted along with its siblings.
     */
    fun mergeSiblings(paneId: Int) {
        val pane = paneMap[paneId] ?: return
        mergeChildren(pane.parentId)
    }

    /**
     * Get the [ViewAlignment] used by the pane to align its content.
     */
    fun getPanePositioning(paneId: Int): ViewAlignment? {
        return paneMap[paneId]?.position
    }

    /**
     * Set the [ViewAlignment] to a pane to align its content.
     *
     * @param paneId - the ID of the pane to which the position should be set.
     * @param position - instance of [ViewAlignment] representing the position of the view in the pane.
     * @param leftMargin - Optional left margin for the widget or view. Defaults to 0.
     * @param topMargin - Optional top margin for the widget or view. Defaults to 0.
     * @param rightMargin - Optional right margin for the widget or view. Defaults to 0.
     * @param bottomMargin
     */
    fun setPanePosition(paneId: Int, position: ViewAlignment,
                        leftMargin: Int = 0, topMargin: Int = 0,
                        rightMargin: Int = 0, bottomMargin: Int = 0) {
        val pane = paneMap[paneId] ?: return
        val widgetView = pane.view ?: return
        pane.position = position
        setViewAlignment(paneId, widgetView, position, leftMargin, topMargin, rightMargin, bottomMargin)
    }

    /**
     * Set the background color of the pane.
     */
    fun setPaneBackgroundColor(paneId: Int, @ColorInt color: Int) {
        val pane = paneMap[paneId] ?: return
        pane.backgroundColor = color
        pane.background.setBackgroundColor(color)
    }

    /**
     * Set the background color of the pane.
     */
    fun setPaneVisibility(paneId: Int, isVisible: Boolean) {
        val pane = paneMap[paneId] ?: return
        if (isVisible) {
            pane.background.show()
        } else {
            pane.background.hide()
        }
        val view = pane.view ?: return
        if (isVisible) {
            view.show()
        } else {
            view.hide()
        }
    }

    //endregion

    //region Helpers
    private fun setViewAlignment(paneId: Int, widgetView: View, position: ViewAlignment,
                                 leftMargin: Int, topMargin: Int, rightMargin: Int, bottomMargin: Int) {
        val set = ConstraintSet()
        set.clone(this)
        set.clear(widgetView.id)
        set.setMargin(widgetView.id, ConstraintSet.LEFT, leftMargin)
        set.setMargin(widgetView.id, ConstraintSet.TOP, topMargin)
        set.setMargin(widgetView.id, ConstraintSet.RIGHT, rightMargin)
        set.setMargin(widgetView.id, ConstraintSet.BOTTOM, bottomMargin)
        when (position) {
            ViewAlignment.CENTER -> {
                set.connect(widgetView.id, ConstraintSet.LEFT, paneId, ConstraintSet.LEFT)
                set.connect(widgetView.id, ConstraintSet.RIGHT, paneId, ConstraintSet.RIGHT)
                set.connect(widgetView.id, ConstraintSet.TOP, paneId, ConstraintSet.TOP)
                set.connect(widgetView.id, ConstraintSet.BOTTOM, paneId, ConstraintSet.BOTTOM)
            }
            ViewAlignment.TOP -> {
                set.constrainHeight(widgetView.id, ConstraintSet.WRAP_CONTENT)
                set.constrainWidth(widgetView.id, 0)
                set.connect(widgetView.id, ConstraintSet.LEFT, paneId, ConstraintSet.LEFT)
                set.connect(widgetView.id, ConstraintSet.RIGHT, paneId, ConstraintSet.RIGHT)
                set.connect(widgetView.id, ConstraintSet.TOP, paneId, ConstraintSet.TOP)
            }
            ViewAlignment.BOTTOM -> {
                set.constrainHeight(widgetView.id, ConstraintSet.WRAP_CONTENT)
                set.constrainWidth(widgetView.id, 0)
                set.connect(widgetView.id, ConstraintSet.LEFT, paneId, ConstraintSet.LEFT)
                set.connect(widgetView.id, ConstraintSet.RIGHT, paneId, ConstraintSet.RIGHT)
                set.connect(widgetView.id, ConstraintSet.BOTTOM, paneId, ConstraintSet.BOTTOM)
            }
            ViewAlignment.LEFT -> {
                set.constrainHeight(widgetView.id, 0)
                set.constrainWidth(widgetView.id, ConstraintSet.WRAP_CONTENT)
                set.connect(widgetView.id, ConstraintSet.LEFT, paneId, ConstraintSet.LEFT)
                set.connect(widgetView.id, ConstraintSet.TOP, paneId, ConstraintSet.TOP)
                set.connect(widgetView.id, ConstraintSet.BOTTOM, paneId, ConstraintSet.BOTTOM)
            }
            ViewAlignment.RIGHT -> {
                set.constrainHeight(widgetView.id, 0)
                set.constrainWidth(widgetView.id, ConstraintSet.WRAP_CONTENT)
                set.connect(widgetView.id, ConstraintSet.RIGHT, paneId, ConstraintSet.RIGHT)
                set.connect(widgetView.id, ConstraintSet.TOP, paneId, ConstraintSet.TOP)
                set.connect(widgetView.id, ConstraintSet.BOTTOM, paneId, ConstraintSet.BOTTOM)
            }
            ViewAlignment.LEFT_TOP -> {
                set.constrainHeight(widgetView.id, ConstraintSet.WRAP_CONTENT)
                set.constrainWidth(widgetView.id, ConstraintSet.WRAP_CONTENT)
                set.connect(widgetView.id, ConstraintSet.LEFT, paneId, ConstraintSet.LEFT)
                set.connect(widgetView.id, ConstraintSet.TOP, paneId, ConstraintSet.TOP)
            }
            ViewAlignment.LEFT_BOTTOM -> {
                set.constrainHeight(widgetView.id, ConstraintSet.WRAP_CONTENT)
                set.constrainWidth(widgetView.id, ConstraintSet.WRAP_CONTENT)
                set.connect(widgetView.id, ConstraintSet.LEFT, paneId, ConstraintSet.LEFT)
                set.connect(widgetView.id, ConstraintSet.BOTTOM, paneId, ConstraintSet.BOTTOM)
            }
            ViewAlignment.RIGHT_TOP -> {
                set.constrainHeight(widgetView.id, ConstraintSet.WRAP_CONTENT)
                set.constrainWidth(widgetView.id, ConstraintSet.WRAP_CONTENT)
                set.connect(widgetView.id, ConstraintSet.RIGHT, paneId, ConstraintSet.RIGHT)
                set.connect(widgetView.id, ConstraintSet.TOP, paneId, ConstraintSet.TOP)
            }
            ViewAlignment.RIGHT_BOTTOM -> {
                set.constrainHeight(widgetView.id, ConstraintSet.WRAP_CONTENT)
                set.constrainWidth(widgetView.id, ConstraintSet.WRAP_CONTENT)
                set.connect(widgetView.id, ConstraintSet.RIGHT, paneId, ConstraintSet.RIGHT)
                set.connect(widgetView.id, ConstraintSet.BOTTOM, paneId, ConstraintSet.BOTTOM)
            }
        }
        set.applyTo(this)
    }

    private fun mergePaneChildren(paneId: Int) {
        val pane = paneMap[paneId] ?: return
        if (!pane.isSplit) return

        for (childId in pane.childrenIdList) {
            // Call function recursively to delete all children
            mergePaneChildren(childId)
            val childPane = paneMap[childId] ?: continue
            val set = ConstraintSet()
            set.clone(this)
            // Remove widget / view from the pane
            val widgetView = childPane.view
            if (widgetView != null) {
                set.clear(widgetView.id)
                removeView(widgetView)
            }
            // Remove debug text view from the pane
            val debugTextView = childPane.debugTextView
            if (debugTextView != null) {
                set.clear(debugTextView.id)
                removeView(debugTextView)
                childPane.debugTextView = null
            }
            set.clear(childPane.id)
            // Remove Pane
            removeView(childPane.background)
            childPane.siblingIdList = emptyList()
            paneMap.remove(childPane.id)
            set.applyTo(this)
        }
        pane.childrenIdList = emptyList()
        pane.isSplit = false
    }

    private fun addDebugViews(isLabelAssist: Boolean = false,
                              isBackgroundAssist: Boolean = false) {
        val visiblePaneMap = paneMap.filter { !it.value.isSplit }
        for ((_, pane) in visiblePaneMap) {
            // If background assist is enabled apply random color to view background
            if (isBackgroundAssist) {
                val color: Int = Color.argb(255,
                        Random.nextInt(256),
                        Random.nextInt(256),
                        Random.nextInt(256))
                pane.background.setBackgroundColor(color)
            }
            // If label assist is enabled add debug label to text view
            if (isLabelAssist) {
                if (pane.debugTextView == null) {
                    val debugTextView = TextView(context)
                    debugTextView.id = ViewIDGenerator.generateViewId()
                    debugTextView.text = pane.id.toString()
                    debugTextView.setTextColor(debugTextColor)
                    debugTextView.setBackgroundColor(debugTextBackgroundColor)
                    addView(debugTextView, childCount)
                    val set = ConstraintSet()
                    set.clone(this)
                    set.connect(debugTextView.id, ConstraintSet.LEFT, pane.background.id, ConstraintSet.LEFT)
                    set.connect(debugTextView.id, ConstraintSet.TOP, pane.background.id, ConstraintSet.TOP)
                    set.applyTo(this)
                    pane.debugTextView = debugTextView
                }
            }
        }
    }


    private fun removeDebugViews() {
        val visiblePaneMap = paneMap.filter { !it.value.isSplit }
        for ((_, pane) in visiblePaneMap) {
            // If no color was applied prior to debug make background null
            if (pane.backgroundColor == INVALID_COLOR) {
                pane.background.background = null
            } else {
                pane.background.setBackgroundColor(pane.backgroundColor)
            }
            val debugTextView = pane.debugTextView
            // Remove debug text view if present
            if (debugTextView != null) {
                val set = ConstraintSet()
                set.clone(this)
                set.clear(debugTextView.id)
                set.applyTo(this)
                if (contains(debugTextView)) {
                    removeView(debugTextView)
                }
                pane.debugTextView = null
            }
        }
    }

    //endregion

    /**
     * View placement positions in a pane.
     */
    enum class ViewAlignment {
        /**
         * Center of the pane.
         */
        CENTER,

        /**
         * Top edge of the pane horizontally centered.
         */
        TOP,

        /**
         * Bottom edge of the pane horizontally centered.
         */
        BOTTOM,

        /**
         * Left edge of the pane vertically centered.
         */
        LEFT,

        /**
         * Right edge of the pane vertically centered.
         */
        RIGHT,

        /**
         * Top left corner of the pane.
         */
        LEFT_TOP,

        /**
         * Bottom left corner of the pane.
         */
        LEFT_BOTTOM,

        /**
         * Top right corner of the pane.
         */
        RIGHT_TOP,

        /**
         * Bottom right corner of the pane.
         */
        RIGHT_BOTTOM

    }

    /**
     * Pane class for caching pane data.
     *
     */
    private data class Pane(val id: Int,
                            val parentId: Int,
                            val background: View,
                            var backgroundColor: Int = INVALID_COLOR,
                            var isSplit: Boolean = false,
                            var view: View? = null,
                            var position: ViewAlignment? = null,
                            var childrenIdList: List<Int> = emptyList(),
                            var siblingIdList: List<Int> = emptyList(),
                            var debugTextView: TextView? = null)

    /**
     * Enum to define types of split.
     */
    enum class SplitType {
        /**
         * Pane will be split into columns
         */
        HORIZONTAL,

        /**
         * Pane will be split into rows
         */
        VERTICAL
    }

}