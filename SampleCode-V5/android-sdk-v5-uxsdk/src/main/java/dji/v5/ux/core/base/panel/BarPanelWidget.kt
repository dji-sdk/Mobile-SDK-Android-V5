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

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.res.use
import dji.v5.ux.R
import dji.v5.ux.core.base.WidgetSizeDescription
import dji.v5.ux.core.base.panel.BarPanelWidget.BarPanelWidgetOrientation
import dji.v5.ux.core.extension.getDimensionAndUse
import dji.v5.ux.core.extension.getFloatAndUse
import dji.v5.ux.core.extension.getIntAndUse
import dji.v5.ux.core.util.ViewIDGenerator

/**
 * Base class for [BarPanelWidget]s. A BarPanelWidget is a collection of simple widgets that
 * can be grouped together in two orientations:
 * [BarPanelWidgetOrientation.VERTICAL] or [BarPanelWidgetOrientation.HORIZONTAL].
 * This type of [PanelWidget] is primarily used for the application's top bar or similarly sized
 * widgets.
 * The BarPanelWidget is split into two lists: left and right (in vertical orientation left is
 * equivalent to top and right to bottom.)
 * BarPanelWidgets don't have title bars or close bars.
 * To initialize the BarPanelWidget, pass a list of [PanelItem] using [BarPanelWidget.addLeftWidgets]
 * and/or [BarPanelWidget.addRightWidgets].
 *
 * Note that child widgets are not created dynamically, instead they are created when the
 * BarPanelWidget is created.
 *
 * @property orientation The current BarPanelWidget orientation.
 */
abstract class BarPanelWidget<T> @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        val orientation: BarPanelWidgetOrientation
) : PanelWidget<PanelItem, T>(
        context,
        attrs,
        defStyleAttr,
        PanelWidgetConfiguration(context, orientation.toPanelWidgetType())) {

    //region Customization Properties
    /**
     * Margin for the left side of the BarPanelWidget.
     */
    @IntRange(from = 0)
    var itemsMarginLeft: Int = 0
        set(value) {
            field = value
            updateUI()
        }

    /**
     * Margin for the top side of the BarPanelWidget.
     */
    @IntRange(from = 0)
    var itemsMarginTop: Int = 0
        set(value) {
            field = value
            updateUI()
        }

    /**
     * Margin for the right side of the BarPanelWidget.
     */
    @IntRange(from = 0)
    var itemsMarginRight: Int = 0
        set(value) {
            field = value
            updateUI()
        }

    /**
     * Margin for the bottom side of the BarPanelWidget.
     */
    @IntRange(from = 0)
    var itemsMarginBottom: Int = 0
        set(value) {
            field = value
            updateUI()
        }

    /**
     * The spacing between the [PanelItem]s inserted.
     */
    @IntRange(from = 0)
    var itemSpacing: Int = 0
        set(value) {
            field = value
            updateUI()
        }

    /**
     * The bias for the left section of the BarPanelWidget. 0 aligns items to the left/top.
     * Left items are aligned to the left (0) by default.
     */
    @FloatRange(from = 0.0, to = 1.0)
    var leftBias: Float = 0f
        set(value) {
            field = value
            setChainBias(leftPanelItems, value)
        }

    /**
     * The bias for the right section of the BarPanelWidget. 0 aligns items to the left/top.
     * Right items are aligned to the right (1) by default.
     */
    @FloatRange(from = 0.0, to = 1.0)
    var rightBias: Float = 1f
        set(value) {
            field = value
            setChainBias(rightPanelItems, value)
        }

    /**
     * The chain style for the left section of the BarPanelWidget.
     * [ConstraintSet.CHAIN_PACKED] is used by default.
     */
    @IntRange(from = ConstraintSet.CHAIN_SPREAD.toLong(), to = ConstraintSet.CHAIN_PACKED.toLong())
    var leftChainStyle: Int = ConstraintSet.CHAIN_PACKED
        set(value) {
            field = value
            setChainStyle(leftPanelItems, value)
        }

    /**
     * The chain style for the right section of the BarPanelWidget.
     * [ConstraintSet.CHAIN_PACKED] is used by default.
     */
    @IntRange(from = ConstraintSet.CHAIN_SPREAD.toLong(), to = ConstraintSet.CHAIN_PACKED.toLong())
    var rightChainStyle: Int = ConstraintSet.CHAIN_PACKED
        set(value) {
            field = value
            setChainStyle(leftPanelItems, value)
        }

    /**
     * The percentage between the left and the right.
     * 0.5 distributes both sides equally.
     */
    @FloatRange(from = 0.0, to = 1.0)
    var guidelinePercent: Float = 0.5f
        set(value) {
            field = value
            val constraintSet = ConstraintSet()
            constraintSet.clone(this)
            constraintSet.setGuidelinePercent(midGuideline.id, field)
            constraintSet.applyTo(this)
        }
    //endregion

    //region Private Properties
    private var leftPanelItems: MutableList<PanelItem> = arrayListOf()
    private var rightPanelItems: MutableList<PanelItem> = arrayListOf()
    private val midGuideline = Guideline(context)
    //endregion

    //region Constructor
    init {
        check(this.panelWidgetConfiguration.isBarPanelWidget()) {
            "PanelWidgetConfiguration.panelWidgetType should " +
                    "be PanelWidgetType.BAR_VERTICAL or PanelWidgetType.BAR_HORIZONTAL"
        }
        setUpContainers()
        attrs?.let { initAttributes(it) }
    }

    private fun setUpContainers() {
        val guidelineOrientation =
                if (orientation == BarPanelWidgetOrientation.HORIZONTAL) {
                    ConstraintSet.VERTICAL_GUIDELINE
                } else {
                    ConstraintSet.HORIZONTAL_GUIDELINE
                }
        midGuideline.id = ViewIDGenerator.generateViewId()
        addView(midGuideline)

        val constraintSet = ConstraintSet()
        constraintSet.clone(this)

        constraintSet.create(midGuideline.id, guidelineOrientation)
        constraintSet.setGuidelinePercent(midGuideline.id, guidelinePercent)

        constraintSet.applyTo(this)
    }

    @SuppressLint("Recycle")
    protected open fun initAttributes(attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.BarPanelWidget).use { typedArray ->
            typedArray.getDimensionAndUse(R.styleable.BarPanelWidget_uxsdk_itemsMarginLeft) {
                itemsMarginLeft = it.toInt()
            }
            typedArray.getDimensionAndUse(R.styleable.BarPanelWidget_uxsdk_itemsMarginTop) {
                itemsMarginTop = it.toInt()
            }
            typedArray.getDimensionAndUse(R.styleable.BarPanelWidget_uxsdk_itemsMarginRight) {
                itemsMarginRight = it.toInt()
            }
            typedArray.getDimensionAndUse(R.styleable.BarPanelWidget_uxsdk_itemsMarginBottom) {
                itemsMarginBottom = it.toInt()
            }
            typedArray.getDimensionAndUse(R.styleable.BarPanelWidget_uxsdk_itemsSpacing) {
                itemSpacing = it.toInt()
            }
            typedArray.getFloatAndUse(R.styleable.BarPanelWidget_uxsdk_guidelinePercent) {
                guidelinePercent = it
            }
            typedArray.getFloatAndUse(R.styleable.BarPanelWidget_uxsdk_leftBias) {
                leftBias = it
            }
            typedArray.getFloatAndUse(R.styleable.BarPanelWidget_uxsdk_rightBias) {
                rightBias = it
            }
            typedArray.getIntAndUse(R.styleable.BarPanelWidget_uxsdk_leftChainStyle) {
                leftChainStyle = it.toChainStyle()
            }
            typedArray.getIntAndUse(R.styleable.BarPanelWidget_uxsdk_rightChainStyle) {
                leftChainStyle = it.toChainStyle()
            }
        }
    }
    //endregion

    //region Lifecycle & Setup
    override fun updateUI() {
        if (leftPanelItems.isEmpty() && rightPanelItems.isEmpty()) return

        // Set the size constraints to each view
        val constraintSet = ConstraintSet()
        constraintSet.clone(this)
        leftPanelItems.forEachIndexed { index, _ ->
            connectPanelItem(constraintSet, leftPanelItems, index, isLeftSide = true)
        }
        rightPanelItems.forEachIndexed { index, _ ->
            connectPanelItem(constraintSet, rightPanelItems, index, isLeftSide = false)
        }

        // Set chain styles
        setChainStyle(constraintSet, leftPanelItems, leftChainStyle)
        setChainStyle(constraintSet, rightPanelItems, rightChainStyle)
        setChainBias(constraintSet, leftPanelItems, leftBias)
        setChainBias(constraintSet, rightPanelItems, rightBias)

        constraintSet.applyTo(this)
    }

    private fun connectPanelItem(constraintSet: ConstraintSet, panelItems: MutableList<PanelItem>, @IntRange(from = 0) index: Int, isLeftSide: Boolean) {
        if (index < panelItems.size) {
            val barPanelItem = panelItems[index]
            val ratioString = barPanelItem.ratioString ?: defaultRatioString
            val widgetSizeDescription = barPanelItem.widgetSizeDescription
                    ?: WidgetSizeDescription(WidgetSizeDescription.SizeType.RATIO,
                            widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                            heightDimension = WidgetSizeDescription.Dimension.EXPAND)
            val currentView = barPanelItem.view

            if (widgetSizeDescription.sizeType == WidgetSizeDescription.SizeType.OTHER
                    && !widgetSizeDescription.widthShouldWrap()
                    && !(isFirstItem(index) || isLastItem(panelItems, index))) {
                throw IllegalStateException("Should not add a fill view in the middle of the list")
            }

            // Set size constraints
            val width =
                    if (widgetSizeDescription.widthShouldWrap()) ViewGroup.LayoutParams.WRAP_CONTENT
                    else 0
            val height =
                    if (widgetSizeDescription.heightShouldWrap()) ViewGroup.LayoutParams.WRAP_CONTENT
                    else 0
            constraintSet.constrainWidth(currentView.id, width)
            constraintSet.constrainHeight(currentView.id, height)
            if (widgetSizeDescription.sizeType == WidgetSizeDescription.SizeType.RATIO) {
                constraintSet.setDimensionRatio(currentView.id, ratioString)
            }

            // Constraint top/start
            if (orientation == BarPanelWidgetOrientation.HORIZONTAL) {
                constraintSet.constraintToParentTop(currentView, barPanelItem.getDefaultItemMarginTop())
                constraintSet.constraintToParentBottom(currentView, barPanelItem.getDefaultItemMarginBottom())
            } else {
                constraintSet.constraintToParentStart(currentView, barPanelItem.getDefaultItemMarginLeft())
                constraintSet.constraintToParentEnd(currentView, barPanelItem.getDefaultItemMarginRight())
            }

            // Chain with prev and next view (or parent/guideline)
            connectToPreviousView(constraintSet, panelItems, index, isLeftSide)
            connectToNextView(constraintSet, panelItems, index, isLeftSide)
        }
    }

    private fun connectToPreviousView(constraintSet: ConstraintSet, panelItems: MutableList<PanelItem>, currentIndex: Int, isLeft: Boolean) {
        val currentPanelItem = panelItems[currentIndex]
        val panelItemSide =
                if (orientation == BarPanelWidgetOrientation.HORIZONTAL) ConstraintSet.START
                else ConstraintSet.TOP
        val endID =
                if (isFirstItem(currentIndex)) {
                    if (isLeft) ConstraintSet.PARENT_ID
                    else midGuideline.id
                } else {
                    panelItems[currentIndex - 1].view.id
                }
        val endSide =
                if (isFirstItem(currentIndex)) {
                    if (orientation == BarPanelWidgetOrientation.HORIZONTAL) ConstraintSet.START
                    else ConstraintSet.TOP
                } else {
                    if (orientation == BarPanelWidgetOrientation.HORIZONTAL) ConstraintSet.END
                    else ConstraintSet.BOTTOM
                }
        constraintSet.connect(
                currentPanelItem.view.id,
                panelItemSide,
                endID,
                endSide,
                getStartTopMargin(currentIndex))
    }

    private fun connectToNextView(constraintSet: ConstraintSet, panelItems: MutableList<PanelItem>, currentIndex: Int, isLeft: Boolean) {
        val currentPanelItem = panelItems[currentIndex]
        val panelItemSide =
                if (orientation == BarPanelWidgetOrientation.HORIZONTAL) ConstraintSet.END
                else ConstraintSet.BOTTOM
        val endID =
                if (isLastItem(panelItems, currentIndex)) {
                    if (isLeft) midGuideline.id
                    else ConstraintSet.PARENT_ID
                } else {
                    panelItems[currentIndex + 1].view.id
                }
        val endSide =
                if (isLastItem(panelItems, currentIndex)) {
                    if (orientation == BarPanelWidgetOrientation.HORIZONTAL) ConstraintSet.END
                    else ConstraintSet.BOTTOM
                } else {
                    if (orientation == BarPanelWidgetOrientation.HORIZONTAL) ConstraintSet.START
                    else ConstraintSet.TOP
                }
        constraintSet.connect(
                currentPanelItem.view.id,
                panelItemSide,
                endID,
                endSide,
                getEndBottomMargin(panelItems, currentIndex))
    }

    private fun setChainStyle(panelItems: MutableList<PanelItem>, chainStyle: Int) {
        val constraintSet = ConstraintSet()
        setChainStyle(constraintSet, panelItems, chainStyle)
        constraintSet.applyTo(this)
    }

    private fun setChainStyle(constraintSet: ConstraintSet, panelItems: MutableList<PanelItem>, chainStyle: Int) {
        if (panelItems.isNotEmpty()) {
            if (orientation == BarPanelWidgetOrientation.HORIZONTAL) {
                constraintSet.setHorizontalChainStyle(panelItems[0].view.id, chainStyle)
            } else {
                constraintSet.setVerticalChainStyle(panelItems[0].view.id, chainStyle)
            }
        }
    }

    private fun setChainBias(panelItems: MutableList<PanelItem>, bias: Float) {
        val constraintSet = ConstraintSet()
        setChainBias(constraintSet, panelItems, bias)
        constraintSet.applyTo(this)
    }

    private fun setChainBias(constraintSet: ConstraintSet, panelItems: MutableList<PanelItem>, bias: Float) {
        if (panelItems.isNotEmpty()) {
            if (orientation == BarPanelWidgetOrientation.HORIZONTAL) {
                constraintSet.setHorizontalBias(panelItems[0].view.id, bias)
            } else {
                constraintSet.setVerticalBias(panelItems[0].view.id, bias)
            }
        }
    }
    //endregion

    //region Populate BarPanelWidget
    @Throws(UnsupportedOperationException::class)
    override fun getWidget(@IntRange(from = 0) index: Int): PanelItem? =
            throw UnsupportedOperationException("Try getLeftPanelItem or getRightPanelItem instead")

    @Throws(UnsupportedOperationException::class)
    override fun addWidgets(panelItems: Array<PanelItem>) {
        throw UnsupportedOperationException("Try addLeftPanelItems or addRightPanelItems instead")
    }

    /**
     * Total number of [PanelItem]s inside this [BarPanelWidget]. Includes left and right lists.
     */
    override fun size(): Int = leftPanelItems.size + rightPanelItems.size

    @Throws(UnsupportedOperationException::class)
    override fun addWidget(@IntRange(from = 0) index: Int, item: PanelItem) {
        throw UnsupportedOperationException("Call either")
    }

    @Throws(UnsupportedOperationException::class)
    override fun removeWidget(@IntRange(from = 0) index: Int): PanelItem? =
            throw UnsupportedOperationException("Try removeLeftPanelItem or removeRightPanelItem instead")

    /**
     * Remove all [PanelItem]s from both left and right lists of this BarPanelWidget.
     */
    override fun removeAllWidgets() {
        removeAllLeftWidgets()
        removeAllRightWidgets()
    }

    /**
     * Get [PanelItem] from the left list of this BarPanelWidget.
     * [index] 0, is the first position of the left list.
     */
    fun getLeftWidget(@IntRange(from = 0) index: Int): PanelItem? = getLeftWidget(index, leftPanelItems)

    /**
     * Append an array of [PanelItem]s to the left list of this BarPanelWidget.
     * The items should be unique and not be present in this BarPanelWidget already.
     */
    fun addLeftWidgets(panelItems: Array<PanelItem>) {
        addPanelItems(fromPanelItems = panelItems, toPanelItems = leftPanelItems)
    }

    /**
     * Total number of [PanelItem]s inside the left list of this [BarPanelWidget].
     */
    fun leftWidgetsSize(): Int = leftPanelItems.size

    /**
     * Insert a [PanelItem] at the given index to the left list of this BarPanelWidget.
     * [index] 0, is the first position of the left list.
     */
    fun addLeftWidget(@IntRange(from = 0) index: Int, panelItem: PanelItem) {
        insertPanelItem(leftPanelItems, panelItem, index)
    }

    /**
     * Remove a [PanelItem] at the given index from the left list of this BarPanelWidget.
     * [index] 0, is the first position of the left list.
     */
    fun removeLeftWidget(@IntRange(from = 0) index: Int): PanelItem? = removePanelItem(leftPanelItems, index)

    /**
     * Remove all [PanelItem] from the left list of this BarPanelWidget.
     */
    fun removeAllLeftWidgets() {
        removeAllPanelItem(rightPanelItems)
    }

    /**
     * Get [PanelItem] from the right list of this BarPanelWidget.
     * [index] 0, is the first position of the right list.
     */
    fun getRightWidget(@IntRange(from = 0) index: Int): PanelItem? = getLeftWidget(index, rightPanelItems)

    /**
     * Appends an array of [PanelItem]s to the right list of this BarPanelWidget.
     * The items should be unique and not be present in this BarPanelWidget already.
     */
    fun addRightWidgets(panelItems: Array<PanelItem>) {
        addPanelItems(fromPanelItems = panelItems, toPanelItems = rightPanelItems)
    }

    /**
     * Total number of [PanelItem]s inside the right list of this [BarPanelWidget].
     */
    fun rightWidgetsSize(): Int = rightPanelItems.size

    /**
     * Insert a [PanelItem] at the given index to the right list of this BarPanelWidget.
     * [index] 0, is the first position of the right list.
     */
    fun addRightWidget(@IntRange(from = 0) index: Int, panelItem: PanelItem) {
        insertPanelItem(rightPanelItems, panelItem, index)
    }

    /**
     * Remove a [PanelItem] at the given index from the right list of this BarPanelWidget.
     * [index] 0, is the first position of the right list.
     */
    fun removeRightWidget(@IntRange(from = 0) index: Int): PanelItem? = removePanelItem(rightPanelItems, index)

    /**
     * Remove all [PanelItem] from the right list of this BarPanelWidget.
     */
    fun removeAllRightWidgets() {
        removeAllPanelItem(rightPanelItems)
    }

    private fun getLeftWidget(@IntRange(from = 0) index: Int, panelItems: MutableList<PanelItem>): PanelItem? =
            panelItems.getOrNull(index)

    private fun addPanelItems(fromPanelItems: Array<PanelItem>, toPanelItems: MutableList<PanelItem>) {
        toPanelItems.addAll(fromPanelItems)
        addViews(fromPanelItems)
        updateUI()
    }

    private fun insertPanelItem(panelItems: MutableList<PanelItem>, panelItem: PanelItem, @IntRange(from = 0) atIndex: Int) {
        panelItems.add(atIndex, panelItem)
        addView(panelItem.view)
        updateUI()
    }

    private fun removePanelItem(panelItems: MutableList<PanelItem>, @IntRange(from = 0) atIndex: Int): PanelItem? {
        if (atIndex >= panelItems.size) return null

        val removedPanelItem = panelItems.removeAt(atIndex)
        removeView(removedPanelItem.view)
        updateUI()

        return removedPanelItem
    }

    private fun removeAllPanelItem(panelItems: MutableList<PanelItem>) {
        panelItems.forEach {
            removeView(it.view)
        }
        panelItems.clear()
        updateUI()
    }
    //endregion

    //region Helpers
    /**
     * Checks if [index] is equivalent to the first position in a list.
     */
    protected fun isFirstItem(@IntRange(from = 0) index: Int): Boolean = index == 0

    /**
     * Checks if item at [index] is at the last item of the [list].
     */
    protected fun isLastItem(list: MutableList<PanelItem>, @IntRange(from = 0) index: Int): Boolean =
            index == list.size - 1

    /**
     * Add views from each [PanelItem] into the parent ConstraintLayout.
     */
    protected fun addViews(panelItems: Array<PanelItem>) {
        panelItems.forEach { addView(it.view) }
    }

    private fun PanelItem.getDefaultItemMarginLeft(): Int = this.itemMarginLeft ?: itemsMarginLeft
    private fun PanelItem.getDefaultItemMarginTop(): Int = this.itemMarginTop ?: itemsMarginTop
    private fun PanelItem.getDefaultItemMarginRight(): Int = this.itemMarginRight
            ?: itemsMarginRight

    private fun PanelItem.getDefaultItemMarginBottom(): Int = this.itemMarginBottom
            ?: itemsMarginBottom

    private fun WidgetSizeDescription.widthShouldWrap() =
            widthDimension == WidgetSizeDescription.Dimension.WRAP


    private fun WidgetSizeDescription.heightShouldWrap(): Boolean =
            heightDimension == WidgetSizeDescription.Dimension.WRAP

    private fun getStartTopMargin(index: Int): Int {
        val margin =
                if (orientation == BarPanelWidgetOrientation.HORIZONTAL) itemsMarginLeft
                else itemsMarginTop
        return if (isFirstItem(index)) margin
        else itemSpacing / 2
    }

    private fun getEndBottomMargin(panelItems: MutableList<PanelItem>, index: Int): Int {
        val margin =
                if (orientation == BarPanelWidgetOrientation.HORIZONTAL) itemsMarginRight
                else itemsMarginBottom
        return if (isLastItem(panelItems, index)) margin
        else itemSpacing / 2
    }

    @IntRange(from = ConstraintSet.CHAIN_SPREAD.toLong(), to = ConstraintSet.CHAIN_PACKED.toLong())
    private fun Int.toChainStyle(): Int {
        return when (this) {
            0 -> ConstraintSet.CHAIN_SPREAD
            1 -> ConstraintSet.CHAIN_SPREAD_INSIDE
            else -> ConstraintSet.CHAIN_PACKED
        }
    }
    //endregion

    /**
     * Indicates the orientation of the BarPanelWidget.
     */
    enum class BarPanelWidgetOrientation {
        /**
         * Items are aligned from top to bottom.
         */
        VERTICAL,

        /**
         * Items are aligned from left to right.
         */
        HORIZONTAL;

        /**
         * Convert [BarPanelWidgetOrientation] into a [PanelWidgetType]
         */
        fun toPanelWidgetType(): PanelWidgetType =
                if (this == HORIZONTAL) PanelWidgetType.BAR_HORIZONTAL
                else PanelWidgetType.BAR_VERTICAL
    }
}