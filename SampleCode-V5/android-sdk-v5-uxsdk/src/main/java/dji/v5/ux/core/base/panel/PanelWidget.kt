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
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.*
import androidx.annotation.IntRange
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.use
import dji.v5.utils.common.DisplayUtil
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.processors.PublishProcessor
import dji.v5.ux.R
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.util.ViewIDGenerator
import dji.v5.ux.core.util.ViewUtil

/**
 * Base class for all PanelWidgets.
 * [S] is the type of objects the panel holds.
 * [T] is the type of Widget State Update, @see[getWidgetStateUpdate].
 * A PanelWidget is a collection of widgets that can be displayed in different ways:
 * - [BarPanelWidget]
 * - [ListPanelWidget]
 * The PanelWidget can be configured with a [PanelWidgetConfiguration], which allows the user
 * to set the [PanelWidgetType] and set up a toolbar if needed.
 * It also provides helpers for the children to know the bounds of the parent:
 * - [PanelWidget.getParentTopId]
 * - [PanelWidget.getParentBottomId]
 * - [PanelWidget.getParentStartId]
 * - [PanelWidget.getParentEndId]
 *
 * Widgets in the BarPanelWidget can be customized in terms of how they are placed, e.g., using ratio,
 * wrapping content. See [PanelItem] for configuration of individual widgets.
 *
 * @param panelWidgetConfiguration * The default [PanelWidgetConfiguration].
 */
abstract class PanelWidget<S, T> @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        protected val panelWidgetConfiguration: PanelWidgetConfiguration
) : ConstraintLayoutWidget<T>(context, attrs, defStyleAttr), Navigable {

    //region Public Properties
    /**
     * Background color for the title bar
     */
    @get:ColorInt
    var titleBarBackgroundColor: Int = 0
        set(@ColorInt value) {
            field = value
            titleBarContainer?.setBackgroundColor(value)
        }

    /**
     * The drawable for the back button icon
     */
    var backButtonIcon: Drawable?
        get() = backImageView?.imageDrawable
        set(value) {
            backImageView?.imageDrawable = value
        }

    /**
     * The color for the back button icon
     */
    @get:ColorInt
    var backButtonIconColor: Int? = null
        set(@ColorInt value) {
            field = value
            if (backImageView != null && value != null) {
                ViewUtil.tintImage(backImageView as ImageView, value)
            }
        }

    /**
     * The color state list for the back button icon
     */
    var backButtonIconColors: ColorStateList? = null
        set(value) {
            field = value
            if (backImageView != null && value != null) {
                ViewUtil.tintImage(backImageView as ImageView, value)
            }
        }

    /**
     * The drawable for the back button icon's background
     */
    var backButtonIconBackground: Drawable? = null
        set(value) {
            field = value
            backImageView?.background = value
        }

    /**
     * The drawable for the close button icon
     */
    var closeButtonIcon: Drawable? = null
        set(value) {
            field = value
            closeImageView?.imageDrawable = value
        }

    /**
     * The drawable for the close button icon's background
     */
    var closeButtonIconBackground: Drawable? = null
        set(value) {
            field = value
            closeImageView?.background = value
        }

    /**
     * The color for the close button icon
     */
    @get:ColorInt
    var closeButtonIconColor: Int? = null
        set(@ColorInt value) {
            field = value
            if (closeImageView != null && value != null) {
                ViewUtil.tintImage(closeImageView as ImageView, value)
            }
        }

    /**
     * The color state list for the close button icon
     */
    var closeButtonIconColors: ColorStateList? = null
        set(value) {
            field = value
            if (closeImageView != null && value != null) {
                ViewUtil.tintImage(closeImageView as ImageView, value)
            }
        }

    /**
     * Text for the title
     */
    var titleText: CharSequence?
        get() = titleTextView?.text
        set(text) {
            if (text != null) {
                titleTextView?.text = text
            }
        }

    /**
     * Text size for the title
     */
    var titleTextSize: Float?
        get() = titleTextView?.textSize
        set(textSize) {
            if (textSize != null) {
                titleTextView?.textSize = textSize
            }
        }

    /**
     * The color for the title
     */
    @get:ColorInt
    var titleTextColor: Int? = null
        set(@ColorInt value) {
            field = value
            if (value != null) {
                titleTextView?.textColor = value
            }
        }
    //endregion

    //region Private Properties
    /**
     * Use for navigating between panels,
     */
    override var panelNavigator: PanelNavigator? = null

    /**
     * The default ratio [String] for panel items.
     */
    protected val defaultRatioString: String = getString(R.string.uxsdk_widget_default_ratio)
    private val backButtonPressedProcessor: PublishProcessor<Boolean> = PublishProcessor.create()
    private val closeBoxPressedProcessor: PublishProcessor<Boolean> = PublishProcessor.create()

    private var parentTopId: Int = ConstraintSet.PARENT_ID
    private val parentBottomId: Int = ConstraintSet.PARENT_ID
    private val parentStartId: Int = ConstraintSet.PARENT_ID
    private val parentEndId: Int = ConstraintSet.PARENT_ID

    private var titleBarContainer: View? = null
    private var backImageView: ImageView? = null
    private var backBackgroundView: View? = null
    private var closeImageView: ImageView? = null
    private var closeBackgroundView: View? = null
    private var titleTextView: TextView? = null

    private val closeButtonBackgroundOnClickListener = OnClickListener {
        closeImageView?.performClick()
    }
    private val closeButtonOnClickListener = OnClickListener {
        closeBoxPressedProcessor.onNext(true)
    }
    //endregion

    //region Constructor
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        // Nothing to do
    }

    init {
        setUpTitleBar()
        initPanelWidget(context, attrs, defStyleAttr, panelWidgetConfiguration)
        attrs?.let { initAttributes(context, it) }
    }

    /**
     * Invoked during the initialization of the class.
     * Inflate should be done here. For Kotlin, load attributes, findViewById should be done in
     * the init block.
     *
     * @param context      Context
     * @param attrs        Attribute set
     * @param defStyleAttr Style attribute
     */
    protected abstract fun initPanelWidget(context: Context, attrs: AttributeSet?, defStyleAttr: Int, widgetConfiguration: PanelWidgetConfiguration?)
    //endregion

    //region Lifecycle & Setup
    protected abstract fun updateUI()

    private fun setUpTitleBar() {
        val currentBackground = background
        if (panelWidgetConfiguration.showTitleBar) {
            val titleBarView: View = inflate(context, R.layout.uxsdk_layout_title_bar, this)
            val titleBarContainer = titleBarView.findViewById<View>(R.id.view_title_bar_container)
            val containerLayoutParams = titleBarContainer?.layoutParams
            containerLayoutParams?.height = panelWidgetConfiguration.titleBarHeight.toInt()

            // Back button
            backBackgroundView = titleBarView.findViewById(R.id.view_back_background)
            backImageView = titleBarView.findViewById<View>(R.id.image_view_back) as ImageView?
            backBackgroundView?.setOnClickListener { backImageView?.performClick() }
            backImageView?.setOnClickListener {
                backButtonPressedProcessor.onNext(true)
                panelNavigator?.pop()
            }
            hideBackButton()

            // Close box
            closeBackgroundView = titleBarView.findViewById(R.id.view_close_background)
            closeImageView = titleBarView.findViewById<View>(R.id.image_view_close) as ImageView?
            setCloseButtonVisible(panelWidgetConfiguration.hasCloseButton)

            // Title
            titleTextView = titleBarView.findViewById(R.id.text_view_title)
            titleTextView?.text = panelWidgetConfiguration.panelTitle
            titleTextView?.visibility =
                    if (panelWidgetConfiguration.showTitleBar) View.VISIBLE
                    else View.GONE

            parentTopId = titleBarContainer.id
            this.titleBarContainer = titleBarContainer
        }
        background = currentBackground
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (panelNavigator?.isCurrentViewRoot() == false) {
            showBackButton()
        }
    }
    //endregion

    //region Events
    /**
     * [Flowable] to observe when the back button is pressed.
     */
    fun backButtonPressed(): Flowable<Boolean> = backButtonPressedProcessor

    /**
     * [Flowable] to observe when the close button is pressed.
     */
    fun closeButtonPressed(): Flowable<Boolean> = closeBoxPressedProcessor
    //endregion

    //region Populate Panel
    /**
     * Get the [S] at index from to the current list of widgets.
     */
    abstract fun getWidget(@IntRange(from = 0) index: Int): S?

    /**
     * Add a new [List] of [S].
     */
    abstract fun addWidgets(items: Array<S>)

    /**
     * Total size of [S] in the current list of widgets.
     */
    abstract fun size(): Int

    /**
     * Add a [S] at [index] to the current list of widgets.
     */
    abstract fun addWidget(@IntRange(from = 0) index: Int, item: S)

    /**
     * Remove a [S] at [index] to the current list of widgets.
     */
    abstract fun removeWidget(@IntRange(from = 0) index: Int): S?

    /**
     * Remove all [S]s.
     */
    abstract fun removeAllWidgets()
    //endregion

    //region Customization
    /**
     * Enable/disable close and back button
     */
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        setBackButtonEnabled(enabled)
        setCloseButtonEnabled(enabled)
    }

    /**
     * Set the resource ID for the title background color
     *
     * @param colorRes Integer ID of the  resource
     */
    fun setTitleBarBackgroundColorRes(@ColorRes colorRes: Int) {
        titleBarBackgroundColor = getColor(colorRes)
    }

    /**
     * Set the resource ID for the back button icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setBackButtonIcon(@DrawableRes resourceId: Int) {
        backButtonIcon = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the back button icon's background
     *
     * @param resourceId Integer ID of the icon's background resource
     */
    fun setBackButtonIconBackground(@DrawableRes resourceId: Int) {
        backButtonIconBackground = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the back button icon's color
     *
     * @param colorRes Integer ID of the icon's background resource
     */
    fun setBackButtonIconColorRes(@ColorRes colorRes: Int) {
        backButtonIconColor = getColor(colorRes)
    }

    /**
     * Set the resource ID for the close button icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setCloseButtonIcon(@DrawableRes resourceId: Int) {
        closeButtonIcon = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the back button icon's background
     *
     * @param resourceId Integer ID of the icon's background resource
     */
    fun setCloseButtonIconBackground(@DrawableRes resourceId: Int) {
        closeButtonIconBackground = getDrawable(resourceId)
    }

    /**
     * Change the visibility of the title bar's close button.
     */
    fun setCloseButtonVisible(visible: Boolean) {
        if (visible) {
            closeImageView?.visibility = View.VISIBLE
            closeImageView?.isFocusable = true
            closeImageView?.isClickable = true
            closeBackgroundView?.setOnClickListener(closeButtonBackgroundOnClickListener)
            closeImageView?.setOnClickListener(closeButtonOnClickListener)
        } else {
            closeImageView?.visibility = View.INVISIBLE
            closeImageView?.isFocusable = false
            closeImageView?.isClickable = false
            closeBackgroundView?.setOnClickListener(null)
            closeImageView?.setOnClickListener(null)
        }
    }

    /**
     * Set text appearance of the title text view
     *
     * @param textAppearanceResId Style resource for text appearance
     */
    fun setTitleTextAppearance(@StyleRes textAppearanceResId: Int) {
        titleTextView?.setTextAppearance(context, textAppearanceResId)
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.PanelWidget).use { typedArray ->
            extractAttributes(typedArray)
        }
    }

    /**
     * Override the style of the panel with the [styleRes].
     */
    @SuppressLint("Recycle")
    fun setStyle(@StyleRes styleRes: Int) {
        context.obtainStyledAttributes(styleRes, R.styleable.PanelWidget).use { typedArray ->
            extractAttributes(typedArray)
        }
    }

    private fun extractAttributes(typedArray: TypedArray) {
        typedArray.getColorAndUse(R.styleable.PanelWidget_uxsdk_titleBarBackgroundColor) {
            titleBarBackgroundColor = it
        }
        typedArray.getDrawableAndUse(R.styleable.PanelWidget_uxsdk_backButtonIcon) {
            backButtonIcon = it
        }
        typedArray.getDrawableAndUse(R.styleable.PanelWidget_uxsdk_backButtonIconBackground) {
            backButtonIconBackground = it
        }
        typedArray.getColorStateListAndUse(R.styleable.PanelWidget_uxsdk_backButtonIconColor) {
            backButtonIconColors = it
        }
        typedArray.getColorAndUse(R.styleable.PanelWidget_uxsdk_backButtonIconColor) {
            backButtonIconColor = it
        }
        typedArray.getDrawableAndUse(R.styleable.PanelWidget_uxsdk_closeButtonIcon) {
            closeButtonIcon = it
        }
        typedArray.getDrawableAndUse(R.styleable.PanelWidget_uxsdk_closeButtonIconBackground) {
            closeButtonIconBackground = it
        }
        typedArray.getColorStateListAndUse(R.styleable.PanelWidget_uxsdk_closeButtonIconColor) {
            closeButtonIconColors = it
        }
        typedArray.getColorAndUse(R.styleable.PanelWidget_uxsdk_closeButtonIconColor) {
            closeButtonIconColor = it
        }
        typedArray.getStringAndUse(R.styleable.PanelWidget_uxsdk_titleText) {
            titleText = it
        }
        typedArray.getResourceIdAndUse(R.styleable.PanelWidget_uxsdk_titleTextAppearance) {
            setTitleTextAppearance(it)
        }
        typedArray.getDimensionAndUse(R.styleable.PanelWidget_uxsdk_titleTextSize) {
            titleTextSize = DisplayUtil.pxToSp(context, it)
        }
        typedArray.getColorAndUse(R.styleable.PanelWidget_uxsdk_titleTextColor) {
            titleTextColor = it
        }
    }
    //endregion

    //region Helpers
    /**
     * Constrain the [view] to the top of the panel, with an optional [margin].
     */
    protected fun ConstraintSet.constraintToParentTop(view: View, margin: Int = 0) {
        if (parentTopId == ConstraintSet.PARENT_ID) {
            connect(view.id, ConstraintSet.TOP, parentTopId, ConstraintSet.TOP, margin)
        } else {
            connect(view.id, ConstraintSet.TOP, parentTopId, ConstraintSet.BOTTOM, margin)
        }
    }

    /**
     * Constrain the [view] to the bottom of the panel, with an optional [margin].
     */
    protected fun ConstraintSet.constraintToParentBottom(view: View, margin: Int = 0) {
        if (parentBottomId == ConstraintSet.PARENT_ID) {
            connect(view.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, margin)
        } else {
            connect(view.id, ConstraintSet.BOTTOM, parentBottomId, ConstraintSet.TOP, margin)
        }
    }

    /**
     * Constrain the [view] to the start of the panel, with an optional [margin].
     */
    protected fun ConstraintSet.constraintToParentStart(view: View, margin: Int = 0) {
        if (parentStartId == ConstraintSet.PARENT_ID) {
            connect(view.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, margin)
        } else {
            connect(view.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.END, margin)
        }
    }

    /**
     * Constrain the [view] to the end of the panel, with an optional [margin].
     */
    protected fun ConstraintSet.constraintToParentEnd(view: View, margin: Int = 0) {
        if (parentEndId == ConstraintSet.PARENT_ID) {
            connect(view.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, margin)
        } else {
            connect(view.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.START, margin)
        }
    }

    /**
     * Get the current parent top view id to use for constraints.
     */
    protected fun getParentTopId(): Int = parentTopId

    /**
     * Get the current parent bottom view id to use for constraints.
     */
    protected fun getParentBottomId(): Int = parentBottomId

    /**
     * Get the current parent start view id to use for constraints.
     */
    protected fun getParentStartId(): Int = parentStartId

    /**
     * Get the current parent end view id to use for constraints.
     */
    protected fun getParentEndId(): Int = parentEndId

    /**
     * Add view to the panel.
     */
    override fun addView(view: View) {
        view.id = ViewIDGenerator.generateViewId()
        if (view is Navigable) {
            view.panelNavigator = panelNavigator
        }
        super.addView(view)
    }

    private fun showBackButton() {
        backImageView?.visibility = View.VISIBLE
        setBackButtonClickable(true)
    }

    private fun hideBackButton() {
        backImageView?.visibility = View.INVISIBLE
        setBackButtonClickable(false)
    }

    private fun setBackButtonClickable(enabled: Boolean) {
        backImageView?.isClickable = enabled
        backImageView?.isFocusable = enabled
        backBackgroundView?.isClickable = enabled
        backBackgroundView?.isFocusable = enabled
    }

    private fun setBackButtonEnabled(enabled: Boolean) {
        backImageView?.isEnabled = enabled
        backBackgroundView?.isEnabled = enabled
    }

    private fun setCloseButtonEnabled(enabled: Boolean) {
        closeImageView?.isEnabled = enabled
        closeBackgroundView?.isEnabled = enabled
    }
    //endregion
}

