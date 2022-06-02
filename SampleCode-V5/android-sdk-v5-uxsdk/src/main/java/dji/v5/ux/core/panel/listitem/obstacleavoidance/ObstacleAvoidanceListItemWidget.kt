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

package dji.v5.ux.core.panel.listitem.obstacleavoidance

import android.content.Context
import android.util.AttributeSet
import dji.sdk.keyvalue.value.flightcontroller.ObstacleActionType
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.WidgetSizeDescription
import dji.v5.ux.core.base.panel.listitem.ListItemRadioButtonWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.getString
import dji.v5.ux.core.panel.listitem.rcstickmode.RCStickModeListItemWidget.ModelState

open class ObstacleAvoidanceListItemWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ListItemRadioButtonWidget<ModelState>(
    context,
    attrs,
    defStyleAttr,
    R.style.UXSDKObstacleAvoidanceListItemWidget
) {

    //region Fields
    private val widgetModel by lazy {
        ObstacleAvoidanceListItemWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }

    private var obstacleActionCloseIndex: Int = INVALID_OPTION_INDEX
    private var obstacleActionStopIndex: Int = INVALID_OPTION_INDEX
    private var obstacleActionAvoidIndex: Int = INVALID_OPTION_INDEX
    //endregion

    //region Constructor
    init {
        obstacleActionCloseIndex = addOptionToGroup(getString(R.string.uxsdk_obstacle_action_type_close))
        obstacleActionStopIndex = addOptionToGroup(getString(R.string.uxsdk_obstacle_action_type_stop))
        obstacleActionAvoidIndex = addOptionToGroup(getString(R.string.uxsdk_obstacle_action_type_avoid))
    }
    //endregion

    //region Lifecycle
    override fun reactToModelChanges() {
        addReaction(widgetModel.obstacleActionTypeProcessor.toFlowable()
            .observeOn(SchedulerProvider.ui())
            .subscribe { updateUI(it) })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    override fun onOptionTapped(optionIndex: Int, optionLabel: String) {
        val type = when (optionIndex) {
            obstacleActionCloseIndex -> {
                ObstacleActionType.CLOSE
            }
            obstacleActionStopIndex -> {
                ObstacleActionType.STOP
            }
            obstacleActionAvoidIndex -> {
                ObstacleActionType.APAS
            }
            else -> return
        }
        addDisposable(
            widgetModel.setObstacleActionType(type)
                .observeOn(SchedulerProvider.ui())
                .subscribe()
        )
    }

    private fun updateUI(type: ObstacleActionType) {
        when (type) {
            ObstacleActionType.UNKNOWN -> {
                isEnabled = false
            }
            ObstacleActionType.CLOSE -> {
                isEnabled = true
                setSelected(obstacleActionCloseIndex)
            }
            ObstacleActionType.STOP -> {
                isEnabled = true
                setSelected(obstacleActionStopIndex)
            }
            ObstacleActionType.APAS -> {
                isEnabled = true
                setSelected(obstacleActionAvoidIndex)
            }
        }
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    override val widgetSizeDescription: WidgetSizeDescription =
        WidgetSizeDescription(
            WidgetSizeDescription.SizeType.OTHER,
            widthDimension = WidgetSizeDescription.Dimension.EXPAND,
            heightDimension = WidgetSizeDescription.Dimension.WRAP
        )

    sealed class ModelState
}