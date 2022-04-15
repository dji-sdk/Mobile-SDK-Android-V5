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

package dji.v5.ux.core.widget.vps

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.WidgetSizeDescription
import dji.v5.ux.core.base.widget.BaseTelemetryWidget
import dji.v5.ux.core.communication.GlobalPreferencesManager
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.widget.vps.VPSWidget.ModelState
import dji.v5.ux.core.widget.vps.VPSWidgetModel.VPSState
import java.text.DecimalFormat

private const val MIN_VPS_HEIGHT = 1.2f

/**
 * Shows the status of the vision positioning system
 * as well as the height of the aircraft as received from the
 * vision positioning system if available.
 */
open class VPSWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        widgetTheme: Int = 0
) : BaseTelemetryWidget<ModelState>(
        context,
        attrs,
        defStyleAttr,
        WidgetType.TEXT_IMAGE_RIGHT,
        widgetTheme,
        R.style.UXSDKVPSWidget
) {

    //region Fields
    override val metricDecimalFormat: DecimalFormat = DecimalFormat("###0.0")

    override val imperialDecimalFormat: DecimalFormat = DecimalFormat("###0")

    /**
     * Icon for VPS enabled
     */
    var vpsEnabledIcon: Drawable = getDrawable(R.drawable.uxsdk_ic_vps_enabled)

    /**
     * Icon for VPS disabled
     */
    var vpsDisabledIcon: Drawable = getDrawable(R.drawable.uxsdk_ic_vps_disabled)

    private val widgetModel: VPSWidgetModel by lazy {
        VPSWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                GlobalPreferencesManager.getInstance())
    }
    //endregion

    //region Constructor
    init {
        initThemeAttributes(context, widgetTheme)
        initAttributes(context, attrs)
        setValueTextViewMinWidthByText("88.8")
    }
    //endregion

    //region Lifecycle
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

    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ModelState.ProductConnected(it)) })
        addReaction(widgetModel.vpsState
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateUI(it) })
    }
    //endregion

    //region Reaction to model
    private fun updateUI(vpsState: VPSState) {
        widgetStateDataProcessor.onNext(ModelState.VPSStateUpdated(vpsState))
        when (vpsState) {
            VPSState.ProductDisconnected,
            VPSState.Disabled -> updateToDisconnectedState()
            is VPSState.Enabled -> updateVPSState(vpsState)
        }
    }

    private fun updateVPSState(vpsState: VPSState.Enabled) {
        widgetIcon = vpsEnabledIcon
        unitString = getDistanceString(vpsState.unitType)
        valueString = getDecimalFormat(vpsState.unitType).format(vpsState.height)
        valueTextColor = if (vpsState.height > MIN_VPS_HEIGHT.toDistance(vpsState.unitType)) {
            normalValueColor
        } else {
            errorValueColor
        }
    }

    private fun updateToDisconnectedState() {
        unitString = null
        widgetIcon = vpsDisabledIcon
        valueString = getString(R.string.uxsdk_string_default_value)
        valueTextColor = normalValueColor
    }
    //endregion

    //region Customization
    override fun getIdealDimensionRatioString(): String? = null


    override val widgetSizeDescription: WidgetSizeDescription =
            WidgetSizeDescription(WidgetSizeDescription.SizeType.OTHER,
                    widthDimension = WidgetSizeDescription.Dimension.EXPAND,
                    heightDimension = WidgetSizeDescription.Dimension.WRAP)

    /**
     * Set the icon for VPS enabled
     *
     * @param resourceId Integer ID of the icon
     */
    fun setVPSEnabledIcon(@DrawableRes resourceId: Int) {
        vpsEnabledIcon = getDrawable(resourceId)
    }

    /**
     * Set the icon for VPS disabled
     *
     * @param resourceId Integer ID of the icon
     */
    fun setVPSDisabledIcon(@DrawableRes resourceId: Int) {
        vpsDisabledIcon = getDrawable(resourceId)
    }

    @SuppressLint("Recycle")
    private fun initThemeAttributes(context: Context, widgetTheme: Int) {
        val vpsAttributeArray: IntArray = R.styleable.VPSWidget
        context.obtainStyledAttributes(widgetTheme, vpsAttributeArray).use {
            initAttributesByTypedArray(it)
        }
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.VPSWidget, 0, defaultStyle).use {
            initAttributesByTypedArray(it)
        }
    }

    private fun initAttributesByTypedArray(typedArray: TypedArray) {
        typedArray.getDrawableAndUse(R.styleable.VPSWidget_uxsdk_vpsEnabledIcon) {
            vpsEnabledIcon = it
        }
        typedArray.getDrawableAndUse(R.styleable.VPSWidget_uxsdk_vpsDisabledIcon) {
            vpsDisabledIcon = it
        }
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
     * Class defines widget state updates
     */
    sealed class ModelState {

        /**
         * Product connection update
         */
        data class ProductConnected(val boolean: Boolean) : ModelState()

        /**
         * VPS model state
         */
        data class VPSStateUpdated(val vpsState: VPSState) : ModelState()
    }
    //endregion

}