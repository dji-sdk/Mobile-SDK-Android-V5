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

package dji.v5.ux.accessory

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import dji.v5.utils.common.LogUtils
import dji.v5.ux.R
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.*

private const val TAG = "RTKWidget"

/**
 * This widget contains multiple widgets to control and get information related to RTK.
 */
open class RTKWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<RTKWidget.ModelState>(context, attrs, defStyleAttr) {
    //region Fields
    private val rtkDialogSeparator: View = findViewById(R.id.rtk_dialog_separator)

    private val widgetModel by lazy {
        RTKWidgetModel(
            DJISDKModel.getInstance(),
            ObservableInMemoryKeyedStore.getInstance()
        )
    }

    /**
     * Get the RTK Enabled Widget so it can be customized.
     */
    @get:JvmName("getRTKEnabledWidget")
    val rtkEnabledWidget: RTKEnabledWidget = findViewById(R.id.widget_rtk_enabled)

    /**
     * Get the RTK Satellite Status Widget so it can be customized.
     */
    @get:JvmName("getRTKSatelliteStatusWidget")
    val rtkSatelliteStatusWidget: RTKSatelliteStatusWidget = findViewById(R.id.widget_rtk_satellite_status)

    /**
     * Get the RTK Type Switch Widget so it can be customized.
     */
    @get:JvmName("getRTKTypeSwitchWidget")
    val rtkTypeSwitchWidget: RTKTypeSwitchWidget = findViewById(R.id.widget_rtk_type_switch)




    /**
     * The color for the separator line views
     */
    var rtkSeparatorsColor: Int
        @JvmName("getRTKSeparatorsColor")
        @ColorInt
        get() = rtkSatelliteStatusWidget.rtkSeparatorsColor
        @JvmName("setRTKSeparatorsColor")
        set(@ColorInt color) {
            rtkSatelliteStatusWidget.rtkSeparatorsColor = color
            rtkDialogSeparator.setBackgroundColor(color)
        }
    //endregion

    //region Constructor
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_widget_rtk, this)
    }

    init {
        LogUtils.i(TAG, "init")
        attrs?.let { initAttributes(context, it) }
    }
    //endregion

    //region Lifecycle
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        LogUtils.i(TAG, "onAttachedToWindow")

        if (!isInEditMode) {
            widgetModel.setup()
            LogUtils.i(TAG, "widgetModel.setup()")

        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    override fun reactToModelChanges() {
        //do nothing
    }




    //region Customization
    override fun getIdealDimensionRatioString(): String {
        return resources.getString(R.string.uxsdk_widget_rtk_ratio)
    }



    //Initialize all customizable attributes
    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.RTKWidget).use { typedArray ->
            typedArray.getColorAndUse(R.styleable.RTKWidget_uxsdk_rtkSeparatorsColor) {
                rtkSeparatorsColor = it
            }
        }
    }
    //endregion


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
         * RTK enabled update
         */
        data class RTKEnabledUpdated(val isRTKEnabled: Boolean) : ModelState()

    }
    //endregion

}