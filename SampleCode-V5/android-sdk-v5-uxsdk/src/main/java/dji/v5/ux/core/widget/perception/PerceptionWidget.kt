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

package dji.v5.ux.core.widget.perception

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import dji.v5.manager.aircraft.perception.data.PerceptionInfo
import dji.v5.manager.aircraft.perception.PerceptionManager
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.widget.FrameLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.getDrawable
import dji.v5.ux.core.extension.getString

/**
 * Description :The widget used to show whether the visual obstacle avoidance system is enabled.
 *
 * @author: William.Wong
 *  date : 2022/9/21
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class PerceptionStateWidget @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayoutWidget<Boolean>(context, attrs, defStyleAttr) {

    private val perceptionIconImageView: ImageView = findViewById(R.id.imageview_vision_icon)

    private val widgetModel by lazy {
        PerceptionStateWidgetModel(
            DJISDKModel.getInstance(),
            ObservableInMemoryKeyedStore.getInstance(),
            PerceptionManager.getInstance()
        )
    }

    // Used to represent the state of perception switch
    private var perceptionSwitchState = PerceptionStateWidgetModel.PerceptionSwitchState.ALL_DISABLED

    // Used to represent the relationships between the perception switch and images
    private val perceptionSwitchStateMap: MutableMap<PerceptionStateWidgetModel.PerceptionSwitchState, Drawable> =
        mutableMapOf(
            PerceptionStateWidgetModel.PerceptionSwitchState.ALL_ENABLED to getDrawable(R.drawable.uxsdk_ic_avoid_normal_all),
            PerceptionStateWidgetModel.PerceptionSwitchState.ONLY_HORI_OFF to getDrawable(R.drawable.uxsdk_ic_avoid_only_hori_off),
            PerceptionStateWidgetModel.PerceptionSwitchState.ONLY_UP_OFF to getDrawable(R.drawable.uxsdk_ic_avoid_only_up_off),
            PerceptionStateWidgetModel.PerceptionSwitchState.ONLY_DOWN_OFF to getDrawable(R.drawable.uxsdk_ic_avoid_only_down_off),
            PerceptionStateWidgetModel.PerceptionSwitchState.ONLY_HORI_ON to getDrawable(R.drawable.uxsdk_ic_avoid_only_hori_on),
            PerceptionStateWidgetModel.PerceptionSwitchState.ONLY_UP_ON to getDrawable(R.drawable.uxsdk_ic_avoid_only_up_on),
            PerceptionStateWidgetModel.PerceptionSwitchState.ONLY_DOWN_ON to getDrawable(R.drawable.uxsdk_ic_avoid_only_down_on),
            PerceptionStateWidgetModel.PerceptionSwitchState.ALL_DISABLED to getDrawable(R.drawable.uxsdk_ic_avoid_disable_all)
        )

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_widget_vision, this)
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

    override fun getIdealDimensionRatioString(): String? {
        return getString(R.string.uxsdk_widget_default_ratio)
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.perceptionInfo
            .observeOn(SchedulerProvider.ui())
            .subscribe { handlePerceptionInfo(it) })
    }

    /**
     * This function will determine the current perception switch state and draw image.
     */
    private fun handlePerceptionInfo(perceptionInfo: PerceptionInfo) {
        // If the vision positioning is disabled, all obstacle avoidance system will stop working
        if (!perceptionInfo.isVisionPositioningEnabled) {
            perceptionSwitchState = PerceptionStateWidgetModel.PerceptionSwitchState.ALL_DISABLED
            perceptionIconImageView.setImageDrawable(perceptionSwitchStateMap[perceptionSwitchState])
            return
        }
        handleHorizontalPerceptionInfo(perceptionInfo)
        handleUpwardPerceptionInfo(perceptionInfo)
        handleDownwardPerceptionInfo(perceptionInfo)
        handleAllPerceptionInfo(perceptionInfo)
        perceptionIconImageView.setImageDrawable(perceptionSwitchStateMap[perceptionSwitchState])
    }

    /**
     * This function will determine the the current horizontal system state.
     */
    private fun handleHorizontalPerceptionInfo(perceptionInfo: PerceptionInfo) {
        if (isPerceptionEnabled(perceptionInfo.isHorizontalObstacleAvoidanceEnabled,
                !perceptionInfo.isUpwardObstacleAvoidanceEnabled,
                !perceptionInfo.isDownwardObstacleAvoidanceEnabled)) {
            perceptionSwitchState = PerceptionStateWidgetModel.PerceptionSwitchState.ONLY_HORI_ON
        } else if(!isPerceptionEnabled(perceptionInfo.isHorizontalObstacleAvoidanceEnabled,
                perceptionInfo.isUpwardObstacleAvoidanceEnabled,
                perceptionInfo.isDownwardObstacleAvoidanceEnabled)) {
            perceptionSwitchState = PerceptionStateWidgetModel.PerceptionSwitchState.ONLY_HORI_OFF
        }
    }

    /**
     * This function will determine the the current upward system state.
     */
    private fun handleUpwardPerceptionInfo(perceptionInfo: PerceptionInfo) {
        if (isPerceptionEnabled(!perceptionInfo.isHorizontalObstacleAvoidanceEnabled,
                perceptionInfo.isUpwardObstacleAvoidanceEnabled,
                !perceptionInfo.isDownwardObstacleAvoidanceEnabled)) {
            perceptionSwitchState = PerceptionStateWidgetModel.PerceptionSwitchState.ONLY_UP_ON
        } else if(isPerceptionEnabled(perceptionInfo.isHorizontalObstacleAvoidanceEnabled,
                !perceptionInfo.isUpwardObstacleAvoidanceEnabled,
                perceptionInfo.isDownwardObstacleAvoidanceEnabled)) {
            perceptionSwitchState = PerceptionStateWidgetModel.PerceptionSwitchState.ONLY_UP_OFF
        }
    }

    /**
     * This function will determine the the current downward system state.
     */
    private fun handleDownwardPerceptionInfo(perceptionInfo: PerceptionInfo) {
        if (isPerceptionEnabled(!perceptionInfo.isHorizontalObstacleAvoidanceEnabled,
                !perceptionInfo.isUpwardObstacleAvoidanceEnabled,
                perceptionInfo.isDownwardObstacleAvoidanceEnabled)) {
            perceptionSwitchState = PerceptionStateWidgetModel.PerceptionSwitchState.ONLY_DOWN_ON
        } else if(isPerceptionEnabled(perceptionInfo.isHorizontalObstacleAvoidanceEnabled,
                perceptionInfo.isUpwardObstacleAvoidanceEnabled,
                !perceptionInfo.isDownwardObstacleAvoidanceEnabled)) {
            perceptionSwitchState = PerceptionStateWidgetModel.PerceptionSwitchState.ONLY_DOWN_OFF
        }
    }

    /**
     * This function will determine if all the perception is enabled.
     */
    private fun handleAllPerceptionInfo(perceptionInfo: PerceptionInfo) {
        if (isPerceptionEnabled(perceptionInfo.isHorizontalObstacleAvoidanceEnabled,
                perceptionInfo.isUpwardObstacleAvoidanceEnabled,
                perceptionInfo.isDownwardObstacleAvoidanceEnabled)) {
            perceptionSwitchState = PerceptionStateWidgetModel.PerceptionSwitchState.ALL_ENABLED
        } else if(isPerceptionEnabled(!perceptionInfo.isHorizontalObstacleAvoidanceEnabled,
                !perceptionInfo.isUpwardObstacleAvoidanceEnabled,
                !perceptionInfo.isDownwardObstacleAvoidanceEnabled)) {
            perceptionSwitchState = PerceptionStateWidgetModel.PerceptionSwitchState.ALL_DISABLED
        }
    }

    /**
     * This function will combine each direction's perception switch state.
     */
    private fun isPerceptionEnabled(horizontalDirection: Boolean, upwardDirection: Boolean, downwardDirection: Boolean): Boolean {
        return horizontalDirection && upwardDirection && downwardDirection
    }
}
