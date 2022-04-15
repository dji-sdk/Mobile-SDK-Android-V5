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

import android.content.Context
import android.util.AttributeSet
import android.view.View
import dji.sdk.keyvalue.value.product.ProductType
import dji.v5.ux.core.base.panel.SmartListModel
import dji.v5.ux.core.base.panel.WidgetID
import dji.v5.ux.core.panel.listitem.aircraftbatterytemperature.AircraftBatteryTemperatureListItemWidget
import dji.v5.ux.core.panel.listitem.emmcstatus.EMMCStatusListItemWidget
import dji.v5.ux.core.panel.listitem.flightmode.FlightModeListItemWidget
import dji.v5.ux.core.panel.listitem.maxaltitude.MaxAltitudeListItemWidget
import dji.v5.ux.core.panel.listitem.maxflightdistance.MaxFlightDistanceListItemWidget
import dji.v5.ux.core.panel.listitem.novicemode.NoviceModeListItemWidget
import dji.v5.ux.core.panel.listitem.overview.OverviewListItemWidget
import dji.v5.ux.core.panel.listitem.rcbattery.RCBatteryListItemWidget
import dji.v5.ux.core.panel.listitem.rcstickmode.RCStickModeListItemWidget
import dji.v5.ux.core.panel.listitem.returntohomealtitude.ReturnToHomeAltitudeListItemWidget
import dji.v5.ux.core.panel.listitem.sdcardstatus.SDCardStatusListItemWidget
import dji.v5.ux.core.panel.listitem.ssdstatus.SSDStatusListItemWidget
import dji.v5.ux.core.panel.listitem.travelmode.TravelModeListItemWidget
import dji.v5.ux.core.panel.listitem.unittype.UnitModeListItemWidget
import dji.v5.ux.core.panel.systemstatus.SystemStatusSmartListModel.SystemStatusListItem.*

private const val TAG = "systemstatuslist"

/**
 * [SmartListModel] to handheld what items should be shown for the [SystemStatusListPanelWidget].
 */
open class SystemStatusSmartListModel @JvmOverloads constructor(
        context: Context,
        private val attrs: AttributeSet? = null,
        excludedItems: Set<WidgetID>? = null
) : SmartListModel(context, attrs, excludedItems) {

    //region Default Items
    /**
     * List of [WidgetID] of widgets that are allowed in this list.
     */
    override val registeredWidgetIDList: List<WidgetID> by lazy {
        listOf(OVERVIEW_STATUS.widgetID,
                RTH_ALTITUDE.widgetID,
                MAX_ALTITUDE.widgetID,
                MAX_FLIGHT_DISTANCE.widgetID,
                FLIGHT_MODE.widgetID,
                RC_STICK_MODE.widgetID,
                RC_BATTERY.widgetID,
                AIRCRAFT_BATTERY_TEMPERATURE.widgetID,
                SD_CARD_STATUS.widgetID,
                EMMC_STATUS.widgetID,
                SSD_STATUS.widgetID,
                TRAVEL_MODE.widgetID,
                NOVICE_MODE.widgetID,
                UNIT_MODE.widgetID)
    }

    /**
     * Default set of widgets that should be shown
     */
    override val defaultActiveWidgetSet: Set<WidgetID> by lazy {
        registeredWidgetIDList.toSet()
                .minus(EMMC_STATUS.widgetID)
                .minus(TRAVEL_MODE.widgetID)
                .minus(SSD_STATUS.widgetID)
    }
    //endregion

    //region Lifecycle
    override fun inSetUp() {
        //暂无实现
    }


    override fun inCleanUp() {
        //暂无实现
    }

    override fun onAircraftModelChanged(model: ProductType) {
        resetSystemStatusListToDefault()
        when (model) {
//            ProductType.INSPIRE_1,
//            ProductType.INSPIRE_1_PRO,
//            ProductType.INSPIRE_1_RAW -> onLandingGearUpdate(true)
//            ProductType.INSPIRE_2 -> {
//                onLandingGearUpdate(true)
//                onSSDSupported(true)
//            }
            ProductType.MAVIC_AIR,
//            ProductType.MAVIC_2_ZOOM,
//            ProductType.MAVIC_2_PRO,
            ProductType.MAVIC_2,
            ProductType.MAVIC_2_ENTERPRISE,
//            ProductType.MAVIC_2_ENTERPRISE_DUAL
            -> onInternalStorageSupported(true)
            else -> {
                // Do Nothing 
            }
        }
    }

    override fun onProductConnectionChanged(isConnected: Boolean) {
        if (!isConnected) {
            resetSystemStatusListToDefault()
        }
    }


    override fun createWidget(widgetID: WidgetID): View {
        return when (SystemStatusListItem.from(widgetID)) {
            OVERVIEW_STATUS -> OverviewListItemWidget(context, attrs)
            RTH_ALTITUDE -> ReturnToHomeAltitudeListItemWidget(context, attrs)
            FLIGHT_MODE -> FlightModeListItemWidget(context, attrs)
            RC_STICK_MODE -> RCStickModeListItemWidget(context, attrs)
            RC_BATTERY -> RCBatteryListItemWidget(context, attrs)
            AIRCRAFT_BATTERY_TEMPERATURE -> AircraftBatteryTemperatureListItemWidget(context, attrs)
            SD_CARD_STATUS -> SDCardStatusListItemWidget(context, attrs)
            EMMC_STATUS -> EMMCStatusListItemWidget(context, attrs)
            MAX_ALTITUDE -> MaxAltitudeListItemWidget(context, attrs)
            MAX_FLIGHT_DISTANCE -> MaxFlightDistanceListItemWidget(context, attrs)
            TRAVEL_MODE -> TravelModeListItemWidget(context, attrs)
            UNIT_MODE -> UnitModeListItemWidget(context, attrs)
            SSD_STATUS -> SSDStatusListItemWidget(context, attrs)
            NOVICE_MODE -> NoviceModeListItemWidget(context, attrs)
            null -> throw IllegalStateException("The WidgetID ($widgetID) is not recognized.")
        }
    }
    //endregion

    //region Helpers
    private fun resetSystemStatusListToDefault() {
        onLandingGearUpdate(false)
        onInternalStorageSupported(false)
        onSSDSupported(false)
    }

    private fun onLandingGearUpdate(movable: Boolean) {
        if (!movable) {
            updateListMinus(TRAVEL_MODE.widgetID)
        } else {
            updateListPlus(TRAVEL_MODE.widgetID)
        }
    }

    private fun onInternalStorageSupported(supported: Boolean) {
        if (!supported) {
            updateListMinus(EMMC_STATUS.widgetID)
        } else {
            updateListPlus(EMMC_STATUS.widgetID)
        }
    }

    private fun onSSDSupported(supported: Boolean) {
        if (!supported) {
            updateListMinus(SSD_STATUS.widgetID)
        } else {
            updateListPlus(SSD_STATUS.widgetID)
        }
    }
    //endregion

    /**
     * Default Widgets for the [SystemStatusListPanelWidget]
     * @property widgetID Identifier for the item
     * @property value Int value for excluding items
     */
    enum class SystemStatusListItem(val widgetID: WidgetID, val value: Int) {

        /**
         * Maps to [FlightModeListItemWidget].
         */
        FLIGHT_MODE("flight_mode", 1),

        /**
         * Maps to [RCStickModeListItemWidget].
         */
        RC_STICK_MODE("rc_stick_mode", 16),

        /**
         * Maps to [RCBatteryListItemWidget].
         */
        RC_BATTERY("rc_battery", 32),

        /**
         * Maps to [AircraftBatteryTemperatureListItemWidget].
         */
        AIRCRAFT_BATTERY_TEMPERATURE("aircraft_battery_temperature", 64),

        /**
         * Maps to [SDCardStatusListItemWidget].
         */
        SD_CARD_STATUS("sd_card_status", 128),

        /**
         * Maps to [EMMCStatusListItemWidget].
         */
        EMMC_STATUS("emmc_status", 256),

        /**
         * Maps to [MaxAltitudeListItemWidget].
         */
        MAX_ALTITUDE("max_altitude", 512),

        /**
         * Maps to [MaxFlightDistanceListItemWidget].
         */
        MAX_FLIGHT_DISTANCE("max_flight_distance", 1024),

        /**
         * Maps to [TravelModeListItemWidget].
         */
        TRAVEL_MODE("travel_mode", 2048),

        /**
         * Maps to [UnitModeListItemWidget].
         */
        UNIT_MODE("unit_mode", 4096),

        /**
         * Maps to [SSDStatusListItemWidget].
         */
        SSD_STATUS("ssd_status", 8192),

        /**
         * Maps to [NoviceModeListItemWidget].
         */
        NOVICE_MODE("novice_mode", 16384),

        /**
         * Maps to [OverviewListItemWidget].
         */
        OVERVIEW_STATUS("overview_status", 32768),

        /**
         * Maps to [ReturnToHomeAltitudeListItemWidget].
         */
        RTH_ALTITUDE("rth_altitude", 65536);


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
             * Create a [SystemStatusListItem] from a [WidgetID].
             */
            @JvmStatic
            fun from(widgetID: WidgetID): SystemStatusListItem? =
                    values.find { it.widgetID == widgetID }

            /**
             * Create a [SystemStatusListItem] from an int value.
             */
            @JvmStatic
            fun from(value: Int): SystemStatusListItem? =
                    values.find { it.value == value }
        }
    }
}