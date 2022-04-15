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
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.annotation.AnimRes
import androidx.core.content.res.use
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.processors.BehaviorProcessor
import dji.v5.ux.R
import dji.v5.ux.core.extension.addListener
import dji.v5.ux.core.extension.getResourceIdAndUse
import java.util.*

/**
 * Check if the current view is the first or the root view in the [PanelNavigationView].
 */
fun PanelNavigator.isCurrentViewRoot(): Boolean = peek() == null

/**
 * Make an object navigable.
 */
interface Navigable {
    /**
     * Use to navigate to another panel.
     */
    var panelNavigator: PanelNavigator?
}

/**
 * Core functions to navigate in the [PanelNavigationView].
 */
interface PanelNavigator {
    /**
     * Push a [View] to the top.
     */
    fun push(view: View)

    /**
     * Pop the current view. Will not pop if the current view is the root.
     */
    fun pop()

    /**
     * Get the top level [View].
     */
    fun peek(): View?
}

/**
 * View to handheld navigation between panels.
 * If a panel item needs to navigate to another panel, make that view implement [Navigable].
 * The [PanelNavigationView] controls the stack of views being presented. It also provides
 * customization to change the transition animations.
 *
 * The PanelNavigationView can be initialized with root view. The root view can also be [push]
 * during Runtime.
 */
abstract class PanelNavigationView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        rootView: View? = null
) : FrameLayout(context, attrs, defStyleAttr), PanelNavigator {

    //region Properties
    /**
     * Enables or disables all transition animations.
     */
    var isAnimationEnabled: Boolean = true

    /**
     * Transition animation for when a view is added on top.
     * Set to null to remove animation.
     */
    var addViewToTopAnimation: Animation = loadAnimation(R.anim.uxsdk_slide_right_in)

    /**
     * Transition animation for when a view is removed from the top.
     * * Set to null to remove animation.
     */
    var removeViewFromTopAnimation: Animation = loadAnimation(R.anim.uxsdk_slide_right_out)

    /**
     * Transition animation for when a view is reappearing.
     * * Set to null to remove animation.
     */
    var showViewAnimation: Animation = loadAnimation(R.anim.uxsdk_fade_in)

    /**
     * Transition animation for when a view is being replaced.
     * * Set to null to remove animation.
     */
    var hideViewAnimation: Animation = loadAnimation(R.anim.uxsdk_fade_out)

    private val viewStack: Stack<View> = Stack()
    private var currentView: View? = null
    private val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    private val isRootViewVisible: BehaviorProcessor<Boolean> = BehaviorProcessor.createDefault(true)
    private val viewAdded: BehaviorProcessor<View> = BehaviorProcessor.create()
    //endregion

    //region Lifecycle
    init {
        attrs?.let {
            initAnimationAttributes(context, it)
            // Must initialize attributes before the first view is added.
            initAttributes(context, it)
        }
        if (rootView != null) {
            push(rootView)
        }
    }
    //endregion

    //region Navigation
    /**
     * Push a [View] to the top.
     */
    override fun push(view: View) {
        val previousView = currentView
        view.tag = view.id
        if (view is Navigable) {
            view.panelNavigator = this
        }

        if (isAnimationEnabled && previousView != null) {
            view.isEnabled = false
            previousView.isEnabled = false
            addViewToTopAnimation.addListener(onEnd = fun(_) {
                post {
                    view.isEnabled = true
                    previousView.isEnabled = true
                    removeView(previousView)
                }
            })
            view.startAnimation(addViewToTopAnimation)
            previousView.startAnimation(hideViewAnimation)
        }

        currentView = view
        previousView?.let {
            viewStack.push(it)
            if (!isAnimationEnabled) {
                removeView(it)
            }
        }

        addView(currentView, layoutParams)
        updateIsRootViewVisible()
        onViewPushed(view)
        viewAdded.onNext(view)
    }

    /**
     * Pop the current view. Will not pop if the current view is the root.
     */
    override fun pop() {
        if (!canRemoveView()) return

        val viewToAdd = viewStack.pop()
        val viewToRemove = currentView as View

        if (isAnimationEnabled) {
            viewToRemove.isEnabled = false
            viewToAdd.isEnabled = false
            removeViewFromTopAnimation.addListener(onEnd = fun(_) {
                post {
                    viewToRemove.isEnabled = true
                    viewToAdd.isEnabled = true
                    removeView(viewToRemove)
                }
            })

            viewToAdd.startAnimation(showViewAnimation)
            viewToRemove.startAnimation(removeViewFromTopAnimation)
        } else {
            removeView(viewToRemove)
        }

        currentView = viewToAdd
        addView(currentView, layoutParams)
        updateIsRootViewVisible()
        viewAdded.onNext(viewToAdd)
    }

    /**
     * Get the top level [View].
     */
    override fun peek(): View? {
        if (viewStack.isEmpty()) return null
        return viewStack.peek()
    }

    /**
     * Callback for when a [View] is pushed to the top.
     */
    protected abstract fun onViewPushed(view: View)

    /**
     * [Flowable] to observe when a [View] is added to the top.
     */
    fun viewAdded(): Flowable<View> = viewAdded
    //endregion

    //region Customizations
    /**
     * Set the [animRes] for when a view is added on top.
     */
    fun setAddViewToTopAnimation(@AnimRes animRes: Int) {
        addViewToTopAnimation = loadAnimation(animRes)
    }

    /**
     * Set the [animRes] for when a view is removed from the top.
     */
    fun setRemoveViewFromTopAnimation(@AnimRes animRes: Int) {
        removeViewFromTopAnimation = loadAnimation(animRes)
    }

    /**
     * Set the [animRes] for when a view is reappearing.
     */
    fun setShowViewAnimation(@AnimRes animRes: Int) {
        showViewAnimation = loadAnimation(animRes)
    }

    /**
     * Set the [animRes] for when a view is being replaced.
     */
    fun setHideViewAnimation(@AnimRes animRes: Int) {
        hideViewAnimation = loadAnimation(animRes)
    }

    //region Customization
    @SuppressLint("Recycle")
    private fun initAnimationAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.PanelNavigationView).use { typedArray ->
            isAnimationEnabled = typedArray.getBoolean(R.styleable.PanelNavigationView_uxsdk_animationEnabled, true)
            typedArray.getResourceIdAndUse(R.styleable.PanelNavigationView_uxsdk_animationShowView) {
                setShowViewAnimation(it)
            }
            typedArray.getResourceIdAndUse(R.styleable.PanelNavigationView_uxsdk_animationHideView) {
                setHideViewAnimation(it)
            }
            typedArray.getResourceIdAndUse(R.styleable.PanelNavigationView_uxsdk_animationAddViewToTop) {
                setAddViewToTopAnimation(it)
            }
            typedArray.getResourceIdAndUse(R.styleable.PanelNavigationView_uxsdk_animationRemoveViewFromTop) {
                setRemoveViewFromTopAnimation(it)
            }
        }
    }

    /**
     * Initialize child attributes here.
     */
    protected abstract fun initAttributes(context: Context, attrs: AttributeSet)
    //endregion

    //region Helpers
    private fun canRemoveView(): Boolean = viewStack.size > 0 && currentView != null

    private fun updateIsRootViewVisible() {
        val isRootViewVisible = !canRemoveView()
        val currentValue = this.isRootViewVisible.value
        if (currentValue != isRootViewVisible) {
            this.isRootViewVisible.onNext(isRootViewVisible)
        }
    }

    private fun loadAnimation(@AnimRes animRes: Int) = AnimationUtils.loadAnimation(context, animRes)
    //endregion

}