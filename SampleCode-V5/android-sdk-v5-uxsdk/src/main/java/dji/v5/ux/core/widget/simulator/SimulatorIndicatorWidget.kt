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

package dji.v5.ux.core.widget.simulator

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Pair
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.functions.Consumer
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.widget.IconButtonWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.communication.OnStateChangeCallback
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.util.RxUtil
import dji.v5.ux.core.widget.simulator.SimulatorIndicatorWidget.ModelState
import dji.v5.ux.core.widget.simulator.SimulatorIndicatorWidget.ModelState.ProductConnected
import dji.v5.ux.core.widget.simulator.SimulatorIndicatorWidget.ModelState.SimulatorStateUpdated

/**
 * Simulator Indicator Widget will display the current state of the simulator
 *
 * Simulator Indicator Widget has two states
 * Active - Green icon indicates currently simulator is running on the device
 * Inactive - White icon indicates simulator is currently turned off on the device
 */
open class SimulatorIndicatorWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : IconButtonWidget<ModelState>(
    context,
    attrs,
    defStyleAttr
), View.OnClickListener {

    //region Fields
    private var stateChangeResourceId: Int = INVALID_RESOURCE
    private val widgetModel by lazy {
        SimulatorIndicatorWidgetModel(
            DJISDKModel.getInstance(),
            ObservableInMemoryKeyedStore.getInstance()
        )
    }

    /**
     * The drawable resource for the simulator active icon
     */
    var simulatorActiveIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_simulator_active)
        set(value) {
            field = value
            checkAndUpdateIcon()
        }

    /**
     * The drawable resource for the simulator inactive icon
     */
    var simulatorInactiveIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_simulator)
        set(value) {
            field = value
            checkAndUpdateIcon()
        }

    /**
     * Call back for when the widget is tapped.
     * This can be used to link the widget to SimulatorControlWidget
     */
    var stateChangeCallback: OnStateChangeCallback<Any>? = null

    //endregion

    //region Lifecycle
    init {
        attrs?.let { initAttributes(context, it) }
        connectedStateIconColor = getColor(R.color.uxsdk_white)
    }

    override fun reactToModelChanges() {
        addReaction(reactToSimulatorStateChange())
        addReaction(widgetModel.isSimulatorActive
            .observeOn(SchedulerProvider.ui())
            .subscribe { widgetStateDataProcessor.onNext(SimulatorStateUpdated(it)) })
        addReaction(widgetModel.productConnection
            .observeOn(SchedulerProvider.ui())
            .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
    }

    override fun getIdealDimensionRatioString(): String {
        return getString(R.string.uxsdk_widget_default_ratio)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
        initializeListener()
    }

    override fun onDetachedFromWindow() {
        destroyListener()
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    override fun onClick(view: View?) {
        super.onClick(view)
        stateChangeCallback?.onStateChange(null)
    }

    override fun checkAndUpdateIconColor() {
        if (!isInEditMode) {
            addDisposable(reactToSimulatorStateChange())
        }
    }

    //endregion

    //region private Methods


    private fun reactToSimulatorStateChange(): Disposable {
        return Flowable.combineLatest(widgetModel.productConnection, widgetModel.isSimulatorActive,
            { first: Boolean, second: Boolean -> Pair(first, second) })
            .observeOn(SchedulerProvider.ui())
            .subscribe(
                Consumer { values: Pair<Boolean, Boolean> -> updateUI(values.first, values.second) },
                RxUtil.logErrorConsumer(TAG, "react to Focus Mode Change: ")
            )
    }

    private fun updateUI(isConnected: Boolean, isActive: Boolean) {
        if (isConnected && isActive) {
            foregroundImageView.imageDrawable = simulatorActiveIcon
            foregroundImageView.clearColorFilter()
        } else if (isConnected) {
            foregroundImageView.imageDrawable = simulatorInactiveIcon
            foregroundImageView.setColorFilter(connectedStateIconColor, PorterDuff.Mode.SRC_IN)

        } else {
            foregroundImageView.imageDrawable = simulatorInactiveIcon
            foregroundImageView.setColorFilter(disconnectedStateIconColor, PorterDuff.Mode.SRC_IN)

        }
    }


    private fun checkAndUpdateIcon() {
        if (!isInEditMode) {
            addDisposable(reactToSimulatorStateChange())
        }
    }

    private fun initializeListener() {
        if (stateChangeResourceId != INVALID_RESOURCE && this.rootView != null) {
            val widgetView = this.rootView.findViewById<View>(stateChangeResourceId)
            if (widgetView is OnStateChangeCallback<*>?) {
                stateChangeCallback = widgetView as OnStateChangeCallback<Any>?
            }
        }
    }

    private fun destroyListener() {
        stateChangeCallback = null
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.SimulatorIndicatorWidget).use { typedArray ->
            typedArray.getResourceIdAndUse(R.styleable.SimulatorIndicatorWidget_uxsdk_onStateChange) {
                stateChangeResourceId = it
            }
            typedArray.getDrawableAndUse(R.styleable.SimulatorIndicatorWidget_uxsdk_simulatorActiveDrawable) {
                simulatorActiveIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.SimulatorIndicatorWidget_uxsdk_simulatorInactiveDrawable) {
                simulatorInactiveIcon = it
            }
        }
    }
    //endregion

    //region customizations

    /**
     * Set the resource ID for the simulator active icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setSimulatorActiveIcon(@DrawableRes resourceId: Int) {
        simulatorActiveIcon = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the simulator inactive icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setSimulatorInactiveIcon(@DrawableRes resourceId: Int) {
        simulatorInactiveIcon = getDrawable(resourceId)
    }

    //endregion

    //region Hooks

    /**
     * Get the [ModelState] updates
     */
    @SuppressWarnings
    override fun getWidgetStateUpdate(): Flowable<ModelState> {
        return super.getWidgetStateUpdate()
    }

    /**
     * Class defines the widget state updates
     */
    sealed class ModelState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : ModelState()

        /**
         * Simulator State update
         */
        data class SimulatorStateUpdated(val isActive: Boolean) : ModelState()
    }
    //endregion

    companion object {
        private const val TAG = "SimulatorIndWidget"
    }
}
