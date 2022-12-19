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

package dji.v5.ux.core.panel.topbar

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.use
import dji.v5.utils.common.LogUtils
import dji.v5.ux.R
import dji.v5.ux.core.base.WidgetSizeDescription
import dji.v5.ux.core.base.panel.BarPanelWidget
import dji.v5.ux.core.base.panel.PanelItem
import dji.v5.ux.core.base.panel.PanelWidgetConfiguration
import dji.v5.ux.core.extension.getDimension
import dji.v5.ux.core.extension.getIntegerAndUse
import dji.v5.ux.core.widget.airsense.AirSenseWidget
import dji.v5.ux.core.widget.battery.BatteryWidget
import dji.v5.ux.core.widget.connection.ConnectionWidget
import dji.v5.ux.core.widget.flightmode.FlightModeWidget
import dji.v5.ux.core.widget.gpssignal.GpsSignalWidget
import dji.v5.ux.core.widget.remotecontrollersignal.RemoteControllerSignalWidget
import dji.v5.ux.core.widget.simulator.SimulatorIndicatorWidget
import dji.v5.ux.core.widget.systemstatus.SystemStatusWidget
import dji.v5.ux.core.widget.videosignal.VideoSignalWidget
import dji.v5.ux.core.widget.perception.PerceptionStateWidget
import dji.v5.ux.warning.DeviceHealthAndStatusWidget
import dji.v5.ux.core.widget.setting.SettingWidget
import java.util.*

/**
 * Container for the top bar widgets. This [BarPanelWidget] is divided into two parts.
 * The left list contains:
 * - [SystemStatusWidget]
 * The right list contains
 * - [FlightModeWidget]
 * - [SimulatorIndicatorWidget]
 * - [AirSenseWidget]
 * - [GPSSignalWidget]
 * - [PerceptionStateWidget]
 * - [RemoteControllerSignalWidget]
 * - [VideoSignalWidget]
 * - [BatteryWidget]
 * - [ConnectionWidget]
 *
 * * Customization:
 * Use the attribute "excludeItem" to permanently remove items from the list. This will prevent a
 * certain item from being created and shown throughout the lifecycle of the bar panel widget. Here are
 * all the flags: system_status, flight_mode, simulator_indicator, air_sense, gps_signal,
 * vision, rc_signal, video_signal, battery, connection.
 *
 * Note that multiple flags can be used simultaneously by logically OR'ing
 * them. For example, to hide flight_mode and vision, it can be done by the
 * following two steps.
 * Define custom xmlns in its layout file:
 * xmlns:app="http://schemas.android.com/apk/res-auto"
 * Then, add following attribute to the [TopBarPanelWidget]:
 * app:excludeItem="flight_mode|vision".
 *
 * This panel widget also passes attributes to each of the child widgets created. See each
 * individual's widget documentation for more customization options.
 */
open class TopBarPanelWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    barPanelWidgetOrientation: BarPanelWidgetOrientation = BarPanelWidgetOrientation.HORIZONTAL
) : BarPanelWidget<Any>(context, attrs, defStyleAttr, barPanelWidgetOrientation) {

    //region Widgets Properties

    //region Widgets Properties
    val deviceHealthAndStatusWidget: DeviceHealthAndStatusWidget?

    /**
     * Getter for [SystemStatusWidget]. Null when excluded from the bar panel.
     */
    val systemStatusWidget: SystemStatusWidget?

    /**
     * Getter for [FlightModeWidget]. Null when excluded from the bar panel.
     */
    val flightModeWidget: FlightModeWidget?

    /**
     * Getter for [SimulatorIndicatorWidget]. Null when excluded from the bar panel.
     */
    val simulatorIndicatorWidget: SimulatorIndicatorWidget?

    /**
     * Getter for [AirSenseWidget]. Null when excluded from the bar panel.
     */
    val airSenseWidget: AirSenseWidget?

    /**
     * Getter for [GPSSignalWidget]. Null when excluded from the bar panel.
     */
    val gpsSignalWidget: GpsSignalWidget?

    /**
     * Getter for [PerceptionStateWidget]. Null when excluded from the bar panel.
     */
    val visionWidget: PerceptionStateWidget?

    /**
     * Getter for [RemoteControllerSignalWidget]. Null when excluded from the bar panel.
     */
    val remoteControllerSignalWidget: RemoteControllerSignalWidget?

    /**
     * Getter for [VideoSignalWidget]. Null when excluded from the bar panel.
     */
    val videoSignalWidget: VideoSignalWidget?

    /**
     * Getter for [BatteryWidget]. Null when excluded from the bar panel.
     */
    val batteryWidget: BatteryWidget?

    /**
     * Getter for [SettingWidget]. Null when excluded from the bar panel.
     */
    val settingWidget: SettingWidget?

    /**
     * Getter for [ConnectionWidget]. Null when excluded from the bar panel.
     */
//    val connectionWidget: ConnectionWidget?
    //endregion

    //region Private properties
    private var excludedItemsValue = 0
    //endregion

    //region Lifecycle & Setup

    override fun initPanelWidget(context: Context, attrs: AttributeSet?, defStyleAttr: Int, widgetConfiguration: PanelWidgetConfiguration?) {
        // Nothing to do
    }

    init {
        val leftPanelItems = ArrayList<PanelItem>()
        if (!WidgetValue.SYSTEM_STATUS.isItemExcluded(excludedItemsValue)) {
            systemStatusWidget = SystemStatusWidget(context, attrs)
            leftPanelItems.add(PanelItem(systemStatusWidget, itemMarginTop = 0, itemMarginBottom = 0))
        } else {
            systemStatusWidget = null
        }
        addLeftWidgets(leftPanelItems.toTypedArray())

        val rightPanelItems = ArrayList<PanelItem>()
        if (!WidgetValue.DEVICE_HEALTH.isItemExcluded(excludedItemsValue)) {
            deviceHealthAndStatusWidget = DeviceHealthAndStatusWidget(context, attrs)
            rightPanelItems.add(PanelItem(deviceHealthAndStatusWidget))
        } else {
            deviceHealthAndStatusWidget = null
        }
        if (!WidgetValue.GPS_SIGNAL.isItemExcluded(excludedItemsValue)) {
            gpsSignalWidget = GpsSignalWidget(context, attrs)
            rightPanelItems.add(PanelItem(gpsSignalWidget))
        } else {
            gpsSignalWidget = null
        }
        if (!WidgetValue.FLIGHT_MODE.isItemExcluded(excludedItemsValue)) {
            flightModeWidget = FlightModeWidget(context, attrs)
            rightPanelItems.add(PanelItem(flightModeWidget))
        } else {
            flightModeWidget = null
        }
        if (!WidgetValue.SIMULATOR_INDICATOR.isItemExcluded(excludedItemsValue)) {
            simulatorIndicatorWidget = SimulatorIndicatorWidget(context, attrs)
            rightPanelItems.add(PanelItem(simulatorIndicatorWidget))
        } else {
            simulatorIndicatorWidget = null
        }
        if (!WidgetValue.AIR_SENSE.isItemExcluded(excludedItemsValue)) {
            airSenseWidget = AirSenseWidget(context, attrs)
            rightPanelItems.add(PanelItem(airSenseWidget))
        } else {
            airSenseWidget = null
        }
        if (!WidgetValue.VISION.isItemExcluded(excludedItemsValue)) {
            visionWidget = PerceptionStateWidget(context, attrs)
            rightPanelItems.add(PanelItem(visionWidget))
        } else {
            visionWidget = null
        }
        if (!WidgetValue.RC_SIGNAL.isItemExcluded(excludedItemsValue)) {
            remoteControllerSignalWidget = RemoteControllerSignalWidget(context, attrs)
            rightPanelItems.add(PanelItem(remoteControllerSignalWidget))
        } else {
            remoteControllerSignalWidget = null
        }
        if (!WidgetValue.VIDEO_SIGNAL.isItemExcluded(excludedItemsValue)) {
            videoSignalWidget = VideoSignalWidget(context, attrs)
            rightPanelItems.add(PanelItem(videoSignalWidget))
        } else {
            videoSignalWidget = null
        }
        if (!WidgetValue.BATTERY.isItemExcluded(excludedItemsValue)) {
            batteryWidget = BatteryWidget(context, attrs)
            rightPanelItems.add(PanelItem(batteryWidget))
        } else {
            batteryWidget = null
        }

        if (!WidgetValue.SETTING.isItemExcluded(excludedItemsValue)) {
            settingWidget = SettingWidget(context, attrs)
            rightPanelItems.add(PanelItem(settingWidget))
        } else {
            settingWidget = null
        }
        addRightWidgets(rightPanelItems.toTypedArray())
    }

    @SuppressLint("Recycle")
    override fun initAttributes(attrs: AttributeSet) {
        guidelinePercent = 0.25f
        itemsMarginTop = getDimension(R.dimen.uxsdk_bar_panel_margin).toInt()
        itemsMarginBottom = getDimension(R.dimen.uxsdk_bar_panel_margin).toInt()

        context.obtainStyledAttributes(attrs, R.styleable.TopBarPanelWidget).use { typedArray ->
            typedArray.getIntegerAndUse(R.styleable.TopBarPanelWidget_uxsdk_excludeTopBarItem) {
                excludedItemsValue = it
            }
        }

        super.initAttributes(attrs)
    }

    override fun reactToModelChanges() {
        // Nothing to do
    }
    //endregion

    //region Customizations
    override fun getIdealDimensionRatioString(): String? = null

    override val widgetSizeDescription: WidgetSizeDescription =
        WidgetSizeDescription(
            WidgetSizeDescription.SizeType.OTHER,
            widthDimension = WidgetSizeDescription.Dimension.EXPAND,
            heightDimension = WidgetSizeDescription.Dimension.EXPAND
        )
    //endregion

    private enum class WidgetValue(val value: Int) {
        SYSTEM_STATUS(1),
        DEVICE_HEALTH(2),
        FLIGHT_MODE(3),
        SIMULATOR_INDICATOR(4),
        AIR_SENSE(8),
        GPS_SIGNAL(16),
        VISION(32),
        RC_SIGNAL(64),
        VIDEO_SIGNAL(128),
        BATTERY(256),
        SETTING(512),
        CONNECTION(1024);

        fun isItemExcluded(excludeItems: Int): Boolean {
            return excludeItems and this.value == this.value
        }
    }
}