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

package dji.v5.ux.core.panel.telemetry

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.use
import dji.v5.ux.R
import dji.v5.ux.core.base.panel.FreeFormPanelWidget
import dji.v5.ux.core.base.panel.PanelWidgetConfiguration
import dji.v5.ux.core.base.panel.PanelWidgetType
import dji.v5.ux.core.extension.getDimension
import dji.v5.ux.core.extension.getIntegerAndUse
import dji.v5.ux.core.extension.getResourceIdAndUse
import dji.v5.ux.core.widget.altitude.AGLAltitudeWidget
import dji.v5.ux.core.widget.altitude.AMSLAltitudeWidget
import dji.v5.ux.core.widget.distancehome.DistanceHomeWidget
import dji.v5.ux.core.widget.distancerc.DistanceRCWidget
import dji.v5.ux.core.widget.horizontalvelocity.HorizontalVelocityWidget
import dji.v5.ux.core.widget.location.LocationWidget
import dji.v5.ux.core.widget.verticalvelocity.VerticalVelocityWidget
import dji.v5.ux.core.widget.vps.VPSWidget

typealias WidgetID = String

/**
 * Compound widget that aggregates important physical state information
 * about the aircraft into a dashboard.
 * <p>
 * It includes the [AMSLAltitudeWidget], [AGLAltitudeWidget],
 * [DistanceHomeWidget], [DistanceRCWidget], [HorizontalVelocityWidget],
 * [VerticalVelocityWidget], [VPSWidget] and the [LocationWidget].
 * <p>
 * This widget can be customized to exclude any of these widgets
 * as required. A widget excluded will not be created.
 * Individual widgets can be accessed using the paneID of each widget
 * and the function [TelemetryPanelWidget.viewInPane].
 *
 */
open class TelemetryPanelWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    configuration: PanelWidgetConfiguration = PanelWidgetConfiguration(
        context = context,
        panelWidgetType = PanelWidgetType.FREE_FORM),
    private var widgetTheme: Int = 0,
) : FreeFormPanelWidget<Any>(context, attrs, defStyleAttr, configuration) {

    //region fields

    private var excludedItemSet = emptySet<WidgetID>()

    /**
     * Pane ID of the [AMSLAltitudeWidget]
     */
    val amslAltitudeWidgetPaneID: Int

    /**
     * Pane ID of the [AGLAltitudeWidget]
     */
    val aglAltitudeWidgetPaneID: Int

    /**
     * Pane ID of the [HorizontalVelocityWidget]
     */
    val horizontalVelocityWidgetPaneID: Int

    /**
     * Pane ID of the [DistanceRCWidget]
     */
    val distanceRCWidgetPaneID: Int

    /**
     * Pane ID of the [DistanceHomeWidget]
     */
    val distanceHomeWidgetPaneID: Int

    /**
     * Pane ID of the [VerticalVelocityWidget]
     */
    val verticalVelocityWidgetPaneID: Int

    /**
     * Pane ID of the [VPSWidget]
     */
    val vpsWidgetPaneID: Int

    /**
     * Pane ID of the [LocationWidget]
     */
    val locationWidgetPaneID: Int

    //endregion

    //region constructor
    init {
        // Create 4 rows
        val rowList = splitPane(rootID, SplitType.VERTICAL, arrayOf(0.25f, 0.25f, 0.25f, 0.25f))
        // Create 3 columns for second row
        val columnList1 = splitPane(rowList[1], SplitType.HORIZONTAL, arrayOf(0.3f, 0.4f, 0.3f))
        // Create 3 columns for third row
        val columnList2 = splitPane(rowList[2], SplitType.HORIZONTAL, arrayOf(0.3f, 0.4f, 0.3f))

        //Assign the paneIds based on position in the panel
        amslAltitudeWidgetPaneID = rowList[0]
        aglAltitudeWidgetPaneID = columnList1[0]
        horizontalVelocityWidgetPaneID = columnList1[1]
        distanceRCWidgetPaneID = columnList1[2]
        distanceHomeWidgetPaneID = columnList2[0]
        verticalVelocityWidgetPaneID = columnList2[1]
        vpsWidgetPaneID = columnList2[2]
        locationWidgetPaneID = rowList[3]

        // Get the excluded items list.
        attrs?.let { initAttributes(context, it) }
        addWidgetsToPanel()
    }
    //endregion

    //region helpers
    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.TelemetryPanelWidget).use { typedArray ->
            typedArray.getIntegerAndUse(R.styleable.TelemetryPanelWidget_uxsdk_excludeTelemetryItem) {
                excludedItemSet = getExcludeListSet(it)
            }
            typedArray.getResourceIdAndUse(R.styleable.TelemetryPanelWidget_uxsdk_telemetry_widget_theme) {
                widgetTheme = it
            }
        }

    }

    private fun getExcludeListSet(excludeListValue: Int?): Set<WidgetID> {
        return if (excludeListValue != null) {
            TelemetryPanelItem.values
                .filter { it.isItemExcluded(excludeListValue) }
                .map { it.widgetID }
                .toSet()
        } else {
            emptySet()
        }
    }

    private fun addWidgetsToPanel() {
        val widgetMargin = getDimension(R.dimen.uxsdk_telemetry_column_margin).toInt()

        setPane(Pane(TelemetryPanelItem.AMSL_ALTITUDE, amslAltitudeWidgetPaneID, ViewAlignment.LEFT), createWidgetBlock = {
            AMSLAltitudeWidget(context, widgetTheme = widgetTheme)
        })

        setPane(
            Pane(TelemetryPanelItem.AGL_ALTITUDE, aglAltitudeWidgetPaneID, ViewAlignment.LEFT, rightMargin = widgetMargin),
            createWidgetBlock = {
                AGLAltitudeWidget(context, widgetTheme = widgetTheme)
            },
        )

        setPane(Pane(TelemetryPanelItem.HORIZONTAL_VELOCITY, horizontalVelocityWidgetPaneID, ViewAlignment.LEFT, leftMargin = widgetMargin,
            rightMargin = widgetMargin), createWidgetBlock = {
            HorizontalVelocityWidget(context, widgetTheme = widgetTheme)
        })

        setPane(Pane(TelemetryPanelItem.DISTANCE_RC, distanceRCWidgetPaneID, ViewAlignment.LEFT, leftMargin = widgetMargin),
            createWidgetBlock = {
                DistanceRCWidget(context, widgetTheme = widgetTheme)
            })

        setPane(Pane(TelemetryPanelItem.DISTANCE_HOME, distanceHomeWidgetPaneID, ViewAlignment.LEFT, rightMargin = widgetMargin),
            createWidgetBlock = {
                DistanceHomeWidget(context, widgetTheme = widgetTheme)
            })

        setPane(Pane(TelemetryPanelItem.VERTICAL_VELOCITY, verticalVelocityWidgetPaneID, ViewAlignment.LEFT, leftMargin = widgetMargin,
            rightMargin = widgetMargin), createWidgetBlock = {
            VerticalVelocityWidget(context, widgetTheme = widgetTheme)
        })

        setPane(Pane(TelemetryPanelItem.VPS, vpsWidgetPaneID, ViewAlignment.LEFT, leftMargin = widgetMargin),
            createWidgetBlock = {
                VPSWidget(context, widgetTheme = widgetTheme)
            })

        setPane(Pane(TelemetryPanelItem.LOCATION, locationWidgetPaneID, ViewAlignment.LEFT),
            createWidgetBlock = {
                LocationWidget(context, widgetTheme = widgetTheme)
            })
    }

    private inline fun <R : View> setPane(
        pane: Pane,
        createWidgetBlock: () -> R,
    ) {
        val paneID = pane.paneID
        if (excludedItemSet.contains(pane.panelItem.widgetID)) {
            setPaneVisibility(paneID, false)
        } else {
            val widget = createWidgetBlock()
            addView(paneID, widget, pane.position, pane.leftMargin, pane.topMargin, pane.rightMargin, pane.bottomMargin)
            setPaneVisibility(paneID, true)
        }
    }

    private data class Pane(
        val panelItem: TelemetryPanelItem,
        val paneID: Int,
        val position: ViewAlignment = ViewAlignment.CENTER,
        val leftMargin: Int = 0,
        val topMargin: Int = 0,
        val rightMargin: Int = 0,
        val bottomMargin: Int = 0,
    )
    //endregion

    //region lifecycle
    override fun reactToModelChanges() {
        // Empty method
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }
    //endregion


    /**
     * Default Widgets for the [TelemetryPanelWidget]
     * @property widgetID Identifier for the item
     * @property value Int value for excluding items
     */
    enum class TelemetryPanelItem(val widgetID: WidgetID, val value: Int) {

        /**
         * Maps to [AMSLAltitudeWidget].
         */
        AMSL_ALTITUDE("amsl_altitude", 1),

        /**
         * Maps to [AGL_ALTITUDE].
         */
        AGL_ALTITUDE("agl_altitude", 2),

        /**
         * Maps to [HorizontalVelocityWidget].
         */
        HORIZONTAL_VELOCITY("horizontal_velocity", 4),

        /**
         * Maps to [DistanceRCWidget].
         */
        DISTANCE_RC("distance_rc", 8),

        /**
         * Maps to [DistanceHomeWidget].
         */
        DISTANCE_HOME("distance_home", 16),

        /**
         * Maps to [VerticalVelocityWidget].
         */
        VERTICAL_VELOCITY("vertical_velocity", 32),

        /**
         * Maps to [VPSWidget].
         */
        VPS("vps", 64),

        /**
         * Maps to [LocationWidget].
         */
        LOCATION("location", 128);


        /**
         * Checks if the item is excluded given the flag [excludeItems].
         */
        fun isItemExcluded(excludeItems: Int): Boolean {
            return excludeItems and this.value == this.value
        }


        companion object {
            @JvmStatic
            val values = values()

            /**
             * Create a [TelemetryPanelItem] from a [WidgetID].
             */
            @JvmStatic
            fun from(widgetID: WidgetID): TelemetryPanelItem? =
                values.find { it.widgetID == widgetID }

            /**
             * Create a [TelemetryPanelItem] from an int value.
             */
            @JvmStatic
            fun from(value: Int): TelemetryPanelItem? =
                values.find { it.value == value }
        }
    }
}