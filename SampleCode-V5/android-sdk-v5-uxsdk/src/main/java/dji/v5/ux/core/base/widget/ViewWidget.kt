package dji.v5.ux.core.base.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import dji.v5.utils.common.LogUtils
import dji.v5.ux.core.base.WidgetSizeDescription
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

/**
 * 集成于view的widget，需要自习实现绘制
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
abstract class ViewWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    protected val logTag = LogUtils.getTag(this)
    //region Fields
    private var reactionDisposables: CompositeDisposable? = null
    private var compositeDisposable: CompositeDisposable? = null

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
    abstract fun getIdealDimensionRatioString(): String?

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
}