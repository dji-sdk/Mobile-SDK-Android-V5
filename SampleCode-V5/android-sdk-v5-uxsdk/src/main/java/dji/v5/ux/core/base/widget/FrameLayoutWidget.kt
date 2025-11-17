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

package dji.v5.ux.core.base.widget

import android.content.Context
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import dji.v5.utils.common.LogUtils
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.processors.PublishProcessor
import dji.v5.ux.core.base.WidgetSizeDescription
import java.lang.RuntimeException
import java.lang.StringBuilder

/**
 * This is a base class for widgets requiring FrameLayout.
 * T is the type of Widget State Update, @see[getWidgetStateUpdate].
 */
abstract class FrameLayoutWidget<T : Any> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    protected val tag = LogUtils.getTag(this)

    //region Fields
    private var reactionDisposables: CompositeDisposable? = null
    private var compositeDisposable: CompositeDisposable? = null

    /**
     * Publish state data updates
     */
    protected val widgetStateDataProcessor: PublishProcessor<T> = PublishProcessor.create()

    //endregion

    //region Constructor
    init {
        initView(context, attrs, defStyleAttr)
    }
    //endregion

    //region Lifecycle
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isInEditMode) {
            return
        }
        reactionDisposables = CompositeDisposable()
        compositeDisposable = CompositeDisposable()
        reactToModelChanges()
    }

    override fun onDetachedFromWindow() {
        unregisterReactions()
        disposeAll()
        super.onDetachedFromWindow()
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
    protected abstract fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int)

    /**
     * Call addReaction here to bind to the model.
     */
    protected abstract fun reactToModelChanges()

    /**
     * Add a disposable which is automatically disposed with the view's lifecycle.
     *
     * @param disposable the disposable to add
     */
    protected fun addDisposable(disposable: Disposable) {
        compositeDisposable?.add(disposable)
    }
    //endregion

    //region Customization
    /**
     * Ideal dimension ratio in the format width:height.
     *
     * @return dimension ratio string.
     */
    open fun getIdealDimensionRatioString(): String? = null

    /**
     * Ideal widget size.
     * By default the widget size is a ratio
     */
    open val widgetSizeDescription: WidgetSizeDescription =
        WidgetSizeDescription(WidgetSizeDescription.SizeType.RATIO)
    //endregion

    //region Reactions
    /**
     * Add a reaction which is automatically disposed with the view's lifecycle.
     *
     * @param reaction the reaction to add.
     */
    protected fun addReaction(reaction: Disposable) {
        checkNotNull(reactionDisposables) { "Called this method only from reactToModelChanges." }
        reactionDisposables?.add(reaction)
    }

    private fun unregisterReactions() {
        reactionDisposables?.dispose()
        reactionDisposables = null

    }

    private fun disposeAll() {
        compositeDisposable?.dispose()
        compositeDisposable = null
    }
    //endregion

    /**
     * Get the update for the widget state
     *
     * @return update with widget state
     */
    open fun getWidgetStateUpdate(): Flowable<T> = widgetStateDataProcessor.onBackpressureBuffer()
    //endregion

    companion object {
        private const val TAG: String = "FrameLayoutWidget"
        const val INVALID_RESOURCE: Int = -1
        const val INVALID_COLOR: Int = 0
    }

    /**
     * 接管处理下可能存在的异步invalidate
     * https://cloud.tencent.com/developer/article/1846821
     */
    override fun invalidate() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            super.invalidate()
        } else {
            recordInvalidateCallStack(this)
        }
    }

    open fun recordInvalidateCallStack(view: View) {
        view.postInvalidate()
        val stringBuilder = StringBuilder()
        for (stackTraceElement in Thread.currentThread().stackTrace) {
            stringBuilder.append(stackTraceElement)
            stringBuilder.append("\n")
        }
        LogUtils.e(TAG, " async call invalidate \n$stringBuilder")
    }
}