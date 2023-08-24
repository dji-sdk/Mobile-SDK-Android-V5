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

package dji.v5.ux.core.panel.systemstatus

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.use
import dji.v5.utils.common.LogUtils
import dji.v5.ux.R
import dji.v5.ux.core.base.panel.ListPanelWidget
import dji.v5.ux.core.base.panel.PanelWidgetConfiguration
import dji.v5.ux.core.base.panel.PanelWidgetType
import dji.v5.ux.core.base.panel.WidgetID
import dji.v5.ux.core.communication.OnStateChangeCallback
import dji.v5.ux.core.extension.getIntegerAndUse
import dji.v5.ux.core.extension.toggleVisibility
import dji.v5.ux.core.panel.listitem.aircraftbatterytemperature.AircraftBatteryTemperatureListItemWidget
import dji.v5.ux.core.panel.listitem.emmcstatus.EMMCStatusListItemWidget
import dji.v5.ux.core.panel.listitem.flightmode.FlightModeListItemWidget
import dji.v5.ux.core.panel.listitem.maxaltitude.MaxAltitudeListItemWidget
import dji.v5.ux.core.panel.listitem.maxflightdistance.MaxFlightDistanceListItemWidget
import dji.v5.ux.core.panel.listitem.overview.OverviewListItemWidget
import dji.v5.ux.core.panel.listitem.rcbattery.RCBatteryListItemWidget
import dji.v5.ux.core.panel.listitem.rcstickmode.RCStickModeListItemWidget
import dji.v5.ux.core.panel.listitem.returntohomealtitude.ReturnToHomeAltitudeListItemWidget
import dji.v5.ux.core.panel.listitem.sdcardstatus.SDCardStatusListItemWidget
import dji.v5.ux.core.panel.listitem.travelmode.TravelModeListItemWidget
import dji.v5.ux.core.widget.systemstatus.SystemStatusWidget

/**
 * To Allow the user to toggle hide and show this panel widget, use in conjunction
 * with [SystemStatusWidget].
 *
 * This panel widget shows the system status list that includes a list of items (like IMU, GPS, etc.).
 * The current version of this panel widget is a sample and more items are to come
 * in future releases.
 *
 * Customization:
 * Use the attribute "excludeItem" to permanently remove items from the list. This will prevent a
 * certain item from being created and shown throughout the lifecycle of the panel widget. Here are
 * all the flags: flight_mode, compass, vision_sensor, radio_quality, rc_stick_mode, rc_battery,
 * aircraft_battery_temperature, sd_card_status, emmc_status, max_altitude, max_flight_distance,
 * travel_mode.
 *
 * Note that multiple flags can be used simultaneously by logically OR'ing
 * them. For example, to hide sd card status and rc stick mode, it can be done by the
 * following two steps.
 * Define custom xmlns in its layout file:
 * xmlns:app="http://schemas.android.com/apk/res-auto"
 * Then, add following attribute to the SystemStatusListPanelWidget:
 * app:excludeItem="sd_card_status|rc_stick_mode".
 *
 * This panel widget also passes attributes to each of the child widgets created. See each
 * widget for individual customizations:
 * [OverviewListItemWidget],
 * [ReturnToHomeAltitudeListItemWidget],
 * [FlightModeListItemWidget],
 * [RCStickModeListItemWidget],
 * [RCBatteryListItemWidget],
 * [AircraftBatteryTemperatureListItemWidget],
 * [SDCardStatusListItemWidget],
 * [EMMCStatusListItemWidget],
 * [MaxAltitudeListItemWidget],
 * [MaxFlightDistanceListItemWidget],
 * [TravelModeListItemWidget].
 *
 * To customize the individual widgets, pass a theme in XML:
 * <code>android:theme="@style/UXSDKSystemStatusListTheme"</code
 */
class SystemStatusListPanelWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    configuration: PanelWidgetConfiguration = PanelWidgetConfiguration(
        context,
        PanelWidgetType.LIST,
        showTitleBar = true,
        panelTitle = context.getString(R.string.uxsdk_system_status_list_title),
        hasCloseButton = true
    )
) : ListPanelWidget<Any>(context, attrs, defStyleAttr, configuration) {

    //region Lifecycle
    override fun initPanelWidget(context: Context, attrs: AttributeSet?, defStyleAttr: Int, widgetConfiguration: PanelWidgetConfiguration?) {
        // Nothing to do
    }

    init {
        val excludedItemsValue = attrs?.let { initAttributes(context, it) }
        val excludedItemsSet = getExcludedItems(excludedItemsValue)

        smartListModel = SystemStatusSmartListModel(context, attrs, excludedItemsSet)
    }

    override fun onSmartListModelCreated() {
        // Nothing to do
    }


    override fun reactToModelChanges() {
        // Nothing to do
    }

    //region Customizations
    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet): Int {
        context.obtainStyledAttributes(attrs, R.styleable.SystemStatusListPanelWidget).use { typedArray ->
            typedArray.getIntegerAndUse(R.styleable.SystemStatusListPanelWidget_uxsdk_excludeItem) {
                return it
            }
        }
        return 0
    }
    //endregion

    //region Helpers
    private fun getExcludedItems(excludedItemsValue: Int?): Set<WidgetID>? {
        return if (excludedItemsValue != null) {
            SystemStatusSmartListModel.SystemStatusListItem.values
                .filter { it.isItemExcluded(excludedItemsValue) }
                .map { it.widgetID }
                .toSet()
        } else {
            null
        }
    }
    //endregion

}