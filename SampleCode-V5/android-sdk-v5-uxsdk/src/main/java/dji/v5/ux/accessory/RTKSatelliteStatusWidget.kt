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

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import dji.v5.ux.R
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget

private const val TAG = "RTKStatusWidget"

/**
 * This widget shows all the information related to RTK.  This includes coordinates and altitude
 * of the aircraft and base station, course angle, GLONASS, Beidou, Galileo, and GPS satellite
 * counts for both antennas and the base station, and overall state of the RTK system.
 */
open class RTKSatelliteStatusWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayoutWidget<RTKSatelliteStatusWidget.ModelState>(context, attrs, defStyleAttr) {
    //region Fields
    private val rtkStatusTitleTextView: TextView = findViewById(R.id.textview_rtk_status_title)
    private val rtkStatusTextView: TextView = findViewById(R.id.textview_rtk_status)
    private val tableBackgroundImageView: ImageView = findViewById(R.id.imageview_table_background)
    private val antenna1TitleTextView: TextView = findViewById(R.id.textview_ant1_title)
    private val antenna2TitleTextView: TextView = findViewById(R.id.textview_ant2_title)
    private val gpsTitleTextView: TextView = findViewById(R.id.textview_gps_title)
    private val gpsAntenna1TextView: TextView = findViewById(R.id.textview_gps_antenna_1)
    private val gpsAntenna2TextView: TextView = findViewById(R.id.textview_gps_antenna_2)
    private val gpsBaseStationTextView: TextView = findViewById(R.id.textview_gps_base_station)
    private val beiDouTitleTextView: TextView = findViewById(R.id.textview_beidou_title)
    private val beiDouAntenna1TextView: TextView = findViewById(R.id.textview_beidou_antenna_1)
    private val beiDouAntenna2TextView: TextView = findViewById(R.id.textview_beidou_antenna_2)
    private val beiDouBaseStationTextView: TextView = findViewById(R.id.textview_beidou_base_station)
    private val glonassTitleTextView: TextView = findViewById(R.id.textview_glonass_title)
    private val glonassAntenna1TextView: TextView = findViewById(R.id.textview_glonass_antenna_1)
    private val glonassAntenna2TextView: TextView = findViewById(R.id.textview_glonass_antenna_2)
    private val glonassBaseStationTextView: TextView = findViewById(R.id.textview_glonass_base_station)
    private val galileoTitleTextView: TextView = findViewById(R.id.textview_galileo_title)
    private val galileoAntenna1TextView: TextView = findViewById(R.id.textview_galileo_antenna_1)
    private val galileoAntenna2TextView: TextView = findViewById(R.id.textview_galileo_antenna_2)
    private val galileoBaseStationTextView: TextView = findViewById(R.id.textview_galileo_base_station)
    private val latitudeTitleTextView: TextView = findViewById(R.id.textview_latitude_title)
    private val longitudeTitleTextView: TextView = findViewById(R.id.textview_longitude_title)
    private val altitudeTitleTextView: TextView = findViewById(R.id.textview_altitude_title)
    private val aircraftCoordinatesTitleTextView: TextView = findViewById(R.id.textview_aircraft_coordinates_title)
    private val aircraftLatitudeTextView: TextView = findViewById(R.id.textview_aircraft_latitude)
    private val aircraftLongitudeTextView: TextView = findViewById(R.id.textview_aircraft_longitude)
    private val aircraftAltitudeTextView: TextView = findViewById(R.id.textview_aircraft_altitude)
    private val baseStationCoordinatesTitleTextView: TextView = findViewById(R.id.textview_base_station_coordinates_title)
    private val baseStationLatitudeTextView: TextView = findViewById(R.id.textview_base_station_latitude)
    private val baseStationLongitudeTextView: TextView = findViewById(R.id.textview_base_station_longitude)
    private val baseStationAltitudeTextView: TextView = findViewById(R.id.textview_base_station_altitude)
    private val courseAngleTitleTextView: TextView = findViewById(R.id.textview_course_angle_title)
    private val courseAngleTextView: TextView = findViewById(R.id.textview_course_angle_value)
    private val orientationTitleTextView: TextView = findViewById(R.id.textview_orientation_title)
    private val orientationTextView: TextView = findViewById(R.id.textview_orientation)
    private val orientationImageView: ImageView = findViewById(R.id.imageview_orientation)
    private val positioningTitleTextView: TextView = findViewById(R.id.textview_positioning_title)
    private val positioningTextView: TextView = findViewById(R.id.textview_positioning)
    private val standardDeviationTitleTextView: TextView = findViewById(R.id.textview_standard_deviation_title)
    private val standardDeviationTextView: TextView = findViewById(R.id.textview_standard_deviation)
    private val rtkAircraftSeparator: View = findViewById(R.id.rtk_aircraft_separator)
    private val rtkBaseStationSeparator: View = findViewById(R.id.rtk_base_station_separator)
    private val rtkOrientationPositioningSeparator: View = findViewById(R.id.rtk_orientation_positioning_separator)
    private val rtkLocationSeparator: View = findViewById(R.id.rtk_location_separator)
    private val rtkSatelliteCountSeparator: View = findViewById(R.id.rtk_satellite_count_separator)
//    private val connectionStateTextColorMap: MutableMap<RTKBaseStationState, Int> =
//            mutableMapOf(
//                    RTKBaseStationState.CONNECTED_IN_USE to getColor(R.color.uxsdk_rtk_status_connected_in_use),
//                    RTKBaseStationState.CONNECTED_NOT_IN_USE to getColor(R.color.uxsdk_rtk_status_connected_not_in_use),
//                    RTKBaseStationState.DISCONNECTED to getColor(R.color.uxsdk_rtk_status_disconnected)
//            )
//
//    private val widgetModel by lazy {
//        RTKSatelliteStatusWidgetModel(DJISDKModel.getInstance(),
//                ObservableInMemoryKeyedStore.getInstance(),
//                GlobalPreferencesManager.getInstance())
//    }
//
//    /**
//     * The text size of the RTK connection state title.
//     */
//    var rtkConnectionStatusTitleTextSize: Float
//        @JvmName("getRTKConnectionStatusTitleTextSize")
//        @Dimension
//        get() = rtkStatusTitleTextView.textSize
//        @JvmName("setRTKConnectionStatusTitleTextSize")
//        set(@Dimension textSize) {
//            rtkStatusTitleTextView.textSize = textSize
//        }
//
//    /**
//     * The text color of the RTK connection state title.
//     */
//    var rtkConnectionStatusTitleTextColor: Int
//        @JvmName("getRTKConnectionStatusTitleTextColor")
//        @ColorInt
//        get() = rtkStatusTitleTextView.currentTextColor
//        @JvmName("setRTKConnectionStatusTitleTextColor")
//        set(@ColorInt color) {
//            rtkStatusTitleTextView.textColor = color
//        }
//
//    /**
//     * The background of the RTK connection state title.
//     */
//    var rtkConnectionStatusTitleTextBackground: Drawable?
//        @JvmName("getRTKConnectionStatusTitleTextBackground")
//        get() = rtkStatusTitleTextView.background
//        @JvmName("setRTKConnectionStatusTitleTextBackground")
//        set(drawable) {
//            rtkStatusTitleTextView.background = drawable
//        }
//
//    /**
//     * The text size of the RTK connection state.
//     */
//    var rtkConnectionStatusTextSize: Float
//        @JvmName("getRTKConnectionStatusTextSize")
//        @Dimension
//        get() = rtkStatusTextView.textSize
//        @JvmName("setRTKConnectionStatusTextSize")
//        set(@Dimension textSize) {
//            rtkStatusTextView.textSize = textSize
//        }
//
//    /**
//     * The background of the RTK connection state.
//     */
//    var rtkConnectionStatusTextBackground: Drawable?
//        @JvmName("getRTKConnectionStatusTextBackground")
//        get() = rtkStatusTextView.background
//        @JvmName("setRTKConnectionStatusTextBackground")
//        set(drawable) {
//            rtkStatusTextView.background = drawable
//        }
//
//    /**
//     * The color for the orientation disabled image view
//     */
//    @get:ColorInt
//    var orientationDisabledColor: Int = getColor(R.color.uxsdk_gray_45)
//        set(@ColorInt value) {
//            field = value
//            checkAndUpdateOrientation()
//        }
//
//    /**
//     * The color of the orientation enabled image view
//     */
//    @get:ColorInt
//    var orientationEnabledColor: Int = getColor(R.color.uxsdk_blue)
//        set(@ColorInt value) {
//            field = value
//            checkAndUpdateOrientation()
//        }
//
//    /**
//     * The text color state list of the RTK labels text views
//     */
//    var rtkLabelsTextColors: ColorStateList?
//        @JvmName("getRTKLabelsTextColors")
//        get() = antenna1TitleTextView.textColors
//        @JvmName("setRTKLabelsTextColors")
//        set(colorStateList) {
//            antenna1TitleTextView.setTextColor(colorStateList)
//            antenna2TitleTextView.setTextColor(colorStateList)
//            gpsTitleTextView.setTextColor(colorStateList)
//            beiDouTitleTextView.setTextColor(colorStateList)
//            glonassTitleTextView.setTextColor(colorStateList)
//            galileoTitleTextView.setTextColor(colorStateList)
//            latitudeTitleTextView.setTextColor(colorStateList)
//            longitudeTitleTextView.setTextColor(colorStateList)
//            altitudeTitleTextView.setTextColor(colorStateList)
//            aircraftCoordinatesTitleTextView.setTextColor(colorStateList)
//            baseStationCoordinatesTitleTextView.setTextColor(colorStateList)
//            courseAngleTitleTextView.setTextColor(colorStateList)
//            orientationTitleTextView.setTextColor(colorStateList)
//            positioningTitleTextView.setTextColor(colorStateList)
//            standardDeviationTitleTextView.setTextColor(colorStateList)
//        }
//
//    /**
//     * The text color of the RTK labels text views
//     */
//    var rtkLabelsTextColor: Int
//        @JvmName("getRTKLabelsTextColor")
//        @ColorInt
//        get() = antenna1TitleTextView.currentTextColor
//        @JvmName("setRTKLabelsTextColor")
//        set(@ColorInt color) {
//            antenna1TitleTextView.textColor = color
//            antenna2TitleTextView.textColor = color
//            gpsTitleTextView.textColor = color
//            beiDouTitleTextView.textColor = color
//            glonassTitleTextView.textColor = color
//            galileoTitleTextView.textColor = color
//            latitudeTitleTextView.textColor = color
//            longitudeTitleTextView.textColor = color
//            altitudeTitleTextView.textColor = color
//            aircraftCoordinatesTitleTextView.textColor = color
//            baseStationCoordinatesTitleTextView.textColor = color
//            courseAngleTitleTextView.textColor = color
//            orientationTitleTextView.textColor = color
//            positioningTitleTextView.textColor = color
//            standardDeviationTitleTextView.textColor = color
//        }
//
//    /**
//     * The text size of the RTK labels text views
//     */
//    var rtkLabelsTextSize: Float
//        @JvmName("getRTKLabelsTextSize")
//        @Dimension
//        get() = antenna1TitleTextView.textSize
//        @JvmName("setRTKLabelsTextSize")
//        set(@Dimension textSize) {
//            antenna1TitleTextView.textSize = textSize
//            antenna2TitleTextView.textSize = textSize
//            gpsTitleTextView.textSize = textSize
//            beiDouTitleTextView.textSize = textSize
//            glonassTitleTextView.textSize = textSize
//            galileoTitleTextView.textSize = textSize
//            latitudeTitleTextView.textSize = textSize
//            longitudeTitleTextView.textSize = textSize
//            altitudeTitleTextView.textSize = textSize
//            aircraftCoordinatesTitleTextView.textSize = textSize
//            baseStationCoordinatesTitleTextView.textSize = textSize
//            courseAngleTitleTextView.textSize = textSize
//            orientationTitleTextView.textSize = textSize
//            positioningTitleTextView.textSize = textSize
//            standardDeviationTitleTextView.textSize = textSize
//        }
//
//    /**
//     * The background for the RTK labels text views
//     */
//    var rtkLabelsTextBackground: Drawable?
//        @JvmName("getRTKLabelsTextBackground")
//        get() = antenna1TitleTextView.background
//        @JvmName("setRTKLabelsTextBackground")
//        set(drawable) {
//            antenna1TitleTextView.background = drawable
//            antenna2TitleTextView.background = drawable
//            gpsTitleTextView.background = drawable
//            beiDouTitleTextView.background = drawable
//            glonassTitleTextView.background = drawable
//            galileoTitleTextView.background = drawable
//            latitudeTitleTextView.background = drawable
//            longitudeTitleTextView.background = drawable
//            altitudeTitleTextView.background = drawable
//            aircraftCoordinatesTitleTextView.background = drawable
//            baseStationCoordinatesTitleTextView.background = drawable
//            courseAngleTitleTextView.background = drawable
//            orientationTitleTextView.background = drawable
//            positioningTitleTextView.background = drawable
//            standardDeviationTitleTextView.background = drawable
//        }
//
//    /**
//     * The text color state list of the RTK values text views
//     */
//    var rtkValuesTextColors: ColorStateList?
//        @JvmName("getRTKValuesTextColors")
//        get() = gpsAntenna1TextView.textColors
//        @JvmName("setRTKValuesTextColors")
//        set(colorStateList) {
//            gpsAntenna1TextView.setTextColor(colorStateList)
//            gpsAntenna2TextView.setTextColor(colorStateList)
//            gpsBaseStationTextView.setTextColor(colorStateList)
//            beiDouAntenna1TextView.setTextColor(colorStateList)
//            beiDouAntenna2TextView.setTextColor(colorStateList)
//            beiDouBaseStationTextView.setTextColor(colorStateList)
//            glonassAntenna1TextView.setTextColor(colorStateList)
//            glonassAntenna2TextView.setTextColor(colorStateList)
//            glonassBaseStationTextView.setTextColor(colorStateList)
//            galileoAntenna1TextView.setTextColor(colorStateList)
//            galileoAntenna2TextView.setTextColor(colorStateList)
//            galileoBaseStationTextView.setTextColor(colorStateList)
//            aircraftLatitudeTextView.setTextColor(colorStateList)
//            aircraftLongitudeTextView.setTextColor(colorStateList)
//            aircraftAltitudeTextView.setTextColor(colorStateList)
//            baseStationLatitudeTextView.setTextColor(colorStateList)
//            baseStationLongitudeTextView.setTextColor(colorStateList)
//            baseStationAltitudeTextView.setTextColor(colorStateList)
//            courseAngleTextView.setTextColor(colorStateList)
//            orientationTextView.setTextColor(colorStateList)
//            positioningTextView.setTextColor(colorStateList)
//            standardDeviationTextView.setTextColor(colorStateList)
//        }
//
//    /**
//     * The text color of the RTK values text views
//     */
//    var rtkValuesTextColor: Int
//        @JvmName("getRTKValuesTextColor")
//        @ColorInt
//        get() = gpsAntenna1TextView.currentTextColor
//        @JvmName("setRTKValuesTextColor")
//        set(@ColorInt color) {
//            gpsAntenna1TextView.textColor = color
//            gpsAntenna2TextView.textColor = color
//            gpsBaseStationTextView.textColor = color
//            beiDouAntenna1TextView.textColor = color
//            beiDouAntenna2TextView.textColor = color
//            beiDouBaseStationTextView.textColor = color
//            glonassAntenna1TextView.textColor = color
//            glonassAntenna2TextView.textColor = color
//            glonassBaseStationTextView.textColor = color
//            galileoAntenna1TextView.textColor = color
//            galileoAntenna2TextView.textColor = color
//            galileoBaseStationTextView.textColor = color
//            aircraftLatitudeTextView.textColor = color
//            aircraftLongitudeTextView.textColor = color
//            aircraftAltitudeTextView.textColor = color
//            baseStationLatitudeTextView.textColor = color
//            baseStationLongitudeTextView.textColor = color
//            baseStationAltitudeTextView.textColor = color
//            courseAngleTextView.textColor = color
//            orientationTextView.textColor = color
//            positioningTextView.textColor = color
//            standardDeviationTextView.textColor = color
//        }
//
//    /**
//     * The text size of the RTK values text views
//     */
//    var rtkValuesTextSize: Float
//        @JvmName("getRTKValuesTextSize")
//        @Dimension
//        get() = gpsAntenna1TextView.textSize
//        @JvmName("setRTKValuesTextSize")
//        set(@Dimension textSize) {
//            gpsAntenna1TextView.textSize = textSize
//            gpsAntenna2TextView.textSize = textSize
//            gpsBaseStationTextView.textSize = textSize
//            beiDouAntenna1TextView.textSize = textSize
//            beiDouAntenna2TextView.textSize = textSize
//            beiDouBaseStationTextView.textSize = textSize
//            glonassAntenna1TextView.textSize = textSize
//            glonassAntenna2TextView.textSize = textSize
//            glonassBaseStationTextView.textSize = textSize
//            galileoAntenna1TextView.textSize = textSize
//            galileoAntenna2TextView.textSize = textSize
//            galileoBaseStationTextView.textSize = textSize
//            aircraftLatitudeTextView.textSize = textSize
//            aircraftLongitudeTextView.textSize = textSize
//            aircraftAltitudeTextView.textSize = textSize
//            baseStationLatitudeTextView.textSize = textSize
//            baseStationLongitudeTextView.textSize = textSize
//            baseStationAltitudeTextView.textSize = textSize
//            courseAngleTextView.textSize = textSize
//            orientationTextView.textSize = textSize
//            positioningTextView.textSize = textSize
//            standardDeviationTextView.textSize = textSize
//        }
//
//    /**
//     * The background for the RTK values text views
//     */
//    var rtkValuesTextBackground: Drawable?
//        @JvmName("getRTKValuesTextBackground")
//        get() = gpsAntenna1TextView.background
//        @JvmName("setRTKValuesTextBackground")
//        set(drawable) {
//            gpsAntenna1TextView.background = drawable
//            gpsAntenna2TextView.background = drawable
//            gpsBaseStationTextView.background = drawable
//            beiDouAntenna1TextView.background = drawable
//            beiDouAntenna2TextView.background = drawable
//            beiDouBaseStationTextView.background = drawable
//            glonassAntenna1TextView.background = drawable
//            glonassAntenna2TextView.background = drawable
//            glonassBaseStationTextView.background = drawable
//            galileoAntenna1TextView.background = drawable
//            galileoAntenna2TextView.background = drawable
//            galileoBaseStationTextView.background = drawable
//            aircraftLatitudeTextView.background = drawable
//            aircraftLongitudeTextView.background = drawable
//            aircraftAltitudeTextView.background = drawable
//            baseStationLatitudeTextView.background = drawable
//            baseStationLongitudeTextView.background = drawable
//            baseStationAltitudeTextView.background = drawable
//            courseAngleTextView.background = drawable
//            orientationTextView.background = drawable
//            positioningTextView.background = drawable
//            standardDeviationTextView.background = drawable
//        }
//
//    /**
//     * The drawable resource for the orientation icon
//     */
//    var orientationIcon: Drawable?
//        get() = orientationImageView.imageDrawable
//        set(icon) {
//            orientationImageView.imageDrawable = icon
//        }
//
//    /**
//     * The color for the separator line views
//     */
//    var rtkSeparatorsColor: Int
//        @JvmName("getRTKSeparatorsColor")
//        @ColorInt
//        get() = rtkAircraftSeparator.solidColor
//        @JvmName("setRTKSeparatorsColor")
//        set(@ColorInt color) {
//            rtkAircraftSeparator.setBackgroundColor(color)
//            rtkBaseStationSeparator.setBackgroundColor(color)
//            rtkOrientationPositioningSeparator.setBackgroundColor(color)
//            rtkLocationSeparator.setBackgroundColor(color)
//            rtkSatelliteCountSeparator.setBackgroundColor(color)
//        }
//
//    /**
//     * Shows or hides the BeiDou satellite information.
//     */
//    var isBeiDouSatelliteInfoVisible = true
//        set(isVisible) {
//            field = isVisible
//            if (isVisible) {
//                beiDouTitleTextView.visibility = View.VISIBLE
//                beiDouAntenna1TextView.visibility = View.VISIBLE
//                beiDouBaseStationTextView.visibility = View.VISIBLE
//            } else {
//                beiDouTitleTextView.visibility = View.GONE
//                beiDouAntenna1TextView.visibility = View.GONE
//                beiDouBaseStationTextView.visibility = View.GONE
//            }
//            checkAndUpdateModel()
//        }
//
//    /**
//     * Shows or hides the GLONASS satellite information.
//     */
//    var isGLONASSSatelliteInfoVisible = true
//        set(isVisible) {
//            field = isVisible
//            if (isVisible) {
//                glonassTitleTextView.visibility = View.VISIBLE
//                glonassAntenna1TextView.visibility = View.VISIBLE
//                glonassBaseStationTextView.visibility = View.VISIBLE
//            } else {
//                glonassTitleTextView.visibility = View.GONE
//                glonassAntenna1TextView.visibility = View.GONE
//                glonassBaseStationTextView.visibility = View.GONE
//            }
//            checkAndUpdateModel()
//        }
//
//    /**
//     * Shows or hides the Galileo satellite information.
//     */
//    var isGalileoSatelliteInfoVisible = true
//        set(isVisible) {
//            field = isVisible
//            if (isVisible) {
//                galileoTitleTextView.visibility = View.VISIBLE
//                galileoAntenna1TextView.visibility = View.VISIBLE
//                galileoBaseStationTextView.visibility = View.VISIBLE
//            } else {
//                galileoTitleTextView.visibility = View.GONE
//                galileoAntenna1TextView.visibility = View.GONE
//                galileoBaseStationTextView.visibility = View.GONE
//            }
//            checkAndUpdateModel()
//        }
//
//    /**
//     * The image displayed behind the satellite state table.
//     */
//    var tableBackground: Drawable?
//        get() = tableBackgroundImageView.imageDrawable
//        set(value) {
//            tableBackgroundImageView.imageDrawable = value
//        }
//    //endregion
//
//    //region Constructor
//    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
//        inflate(context, R.layout.uxsdk_widget_rtk_satellite_status, this)
//    }
//
//    init {
//        initItemValues()
//        attrs?.let { initAttributes(context, it) }
//    }
//    //endregion
//
//    //region Lifecycle
//    override fun onAttachedToWindow() {
//        super.onAttachedToWindow()
//        if (!isInEditMode) {
//            widgetModel.setup()
//        }
//    }
//
//    override fun onDetachedFromWindow() {
//        if (!isInEditMode) {
//            widgetModel.cleanup()
//        }
//        super.onDetachedFromWindow()
//    }
//
//    override fun reactToModelChanges() {
//        addReaction(widgetModel.isRTKConnected
//                .observeOn(SchedulerProvider.ui())
//                .subscribe { updateUIForIsRTKConnected(it) })
//        addReaction(widgetModel.rtkState
//                .observeOn(SchedulerProvider.ui())
//                .subscribe { updateRTKStateUI(it) })
//        addReaction(widgetModel.model
//                .observeOn(SchedulerProvider.ui())
//                .subscribe { updateModel(it) })
//        addReaction(widgetModel.rtkSignal
//                .observeOn(SchedulerProvider.ui())
//                .subscribe { updateBaseStationTitle(it) })
//        addReaction(widgetModel.rtkBaseStationState
//                .observeOn(SchedulerProvider.ui())
//                .subscribe { updateBaseStationStatus(it) })
//        addReaction(widgetModel.rtkNetworkServiceState
//                .observeOn(SchedulerProvider.ui())
//                .subscribe { updateNetworkServiceStatus(it) })
//        addReaction(widgetModel.standardDeviation
//                .observeOn(SchedulerProvider.ui())
//                .subscribe { updateStandardDeviation(it) })
//        addReaction(widgetModel.productConnection
//                .observeOn(SchedulerProvider.ui())
//                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
//    }
//
//    //endregion
//
//    //region Reactions to model
//    private fun updateRTKStateUI(rtkState: RTKState) {
//        if (rtkState.mobileStationReceiver1GPSInfo == null) {
//            return
//        }
//        gpsAntenna1TextView.text = rtkState.mobileStationReceiver1GPSInfo.satelliteCount.toString()
//        gpsAntenna2TextView.text = rtkState.mobileStationReceiver2GPSInfo.satelliteCount.toString()
//        gpsBaseStationTextView.text = rtkState.baseStationReceiverGPSInfo.satelliteCount.toString()
//        aircraftLatitudeTextView.text = resources.getString(R.string.uxsdk_rtk_panel_coordinate_value,
//                rtkState.fusionMobileStationLocation.latitude)
//        aircraftLongitudeTextView.text = resources.getString(R.string.uxsdk_rtk_panel_coordinate_value,
//                rtkState.fusionMobileStationLocation.longitude)
//        aircraftAltitudeTextView.text = resources.getString(R.string.uxsdk_rtk_panel_altitude_value,
//                rtkState.mobileStationAltitude)
//        baseStationLatitudeTextView.text = resources.getString(R.string.uxsdk_rtk_panel_coordinate_value,
//                rtkState.baseStationLocation.latitude)
//        baseStationLongitudeTextView.text = resources.getString(R.string.uxsdk_rtk_panel_coordinate_value,
//                rtkState.baseStationLocation.longitude)
//        baseStationAltitudeTextView.text = resources.getString(R.string.uxsdk_rtk_panel_altitude_value,
//                rtkState.baseStationAltitude)
//        courseAngleTextView.text = resources.getString(R.string.uxsdk_rtk_panel_course_angle_value,
//                rtkState.fusionHeading)
//        updateRTKError(rtkState.error)
//        updateOrientationStatus(rtkState.isHeadingValid, rtkState.headingSolution)
//        updatePositionStatus(rtkState.positioningSolution)
//        updateBeidouSatelliteDisplay(rtkState)
//        updateGLONASSSatelliteDisplay(rtkState)
//        updateGalileoSatelliteDisplay(rtkState)
//        widgetStateDataProcessor.onNext(RTKStateUpdated(rtkState))
//    }
//
//    private fun updateUIForIsRTKConnected(isRTKConnected: Boolean) {
//        if (!isRTKConnected) {
//            initItemValues()
//        }
//        widgetStateDataProcessor.onNext(RTKConnectionUpdated(isRTKConnected))
//    }
//
//    private fun updateModel(model: Model) {
//        orientationTitleTextView.visibility = if (!isPhantom4RTK(model)) View.VISIBLE else View.GONE
//        orientationImageView.visibility = if (model == Model.MATRICE_210_RTK) View.VISIBLE else View.GONE
//        orientationTextView.visibility = if (model != Model.MATRICE_210_RTK && !isPhantom4RTK(model)) View.VISIBLE else View.GONE
//
//        courseAngleTitleTextView.visibility = if (!isPhantom4RTK(model)) View.VISIBLE else View.GONE
//        courseAngleTextView.visibility = if (!isPhantom4RTK(model)) View.VISIBLE else View.GONE
//
//        standardDeviationTitleTextView.visibility = if (isPhantom4RTK(model)) View.VISIBLE else View.GONE
//        standardDeviationTextView.visibility = if (isPhantom4RTK(model)) View.VISIBLE else View.GONE
//
//        antenna1TitleTextView.visibility = if (!isPhantom4RTK(model)) View.VISIBLE else View.GONE
//        antenna2TitleTextView.visibility = if (!isPhantom4RTK(model)) View.VISIBLE else View.GONE
//        gpsAntenna2TextView.visibility = if (!isPhantom4RTK(model)) View.VISIBLE else View.GONE
//        beiDouAntenna2TextView.visibility = if (!isPhantom4RTK(model) && isBeiDouSatelliteInfoVisible) View.VISIBLE else View.GONE
//        glonassAntenna2TextView.visibility = if (!isPhantom4RTK(model) && isGLONASSSatelliteInfoVisible) View.VISIBLE else View.GONE
//        galileoAntenna2TextView.visibility = if (!isPhantom4RTK(model) && isGalileoSatelliteInfoVisible) View.VISIBLE else View.GONE
//        widgetStateDataProcessor.onNext(ModelUpdated(model))
//    }
//
//    private fun isPhantom4RTK(model: Model): Boolean {
//        return model == Model.PHANTOM_4_RTK || model == Model.P_4_MULTISPECTRAL
//    }
//
//    //endregion
//
//    //region Helper methods
//    private fun initItemValues() {
//        gpsAntenna1TextView.setText(R.string.uxsdk_string_default_value)
//        gpsAntenna2TextView.setText(R.string.uxsdk_string_default_value)
//        gpsBaseStationTextView.setText(R.string.uxsdk_string_default_value)
//        beiDouAntenna1TextView.setText(R.string.uxsdk_string_default_value)
//        beiDouAntenna2TextView.setText(R.string.uxsdk_string_default_value)
//        beiDouBaseStationTextView.setText(R.string.uxsdk_string_default_value)
//        glonassAntenna1TextView.setText(R.string.uxsdk_string_default_value)
//        glonassAntenna2TextView.setText(R.string.uxsdk_string_default_value)
//        glonassBaseStationTextView.setText(R.string.uxsdk_string_default_value)
//        galileoAntenna1TextView.setText(R.string.uxsdk_string_default_value)
//        galileoAntenna2TextView.setText(R.string.uxsdk_string_default_value)
//        galileoBaseStationTextView.setText(R.string.uxsdk_string_default_value)
//        aircraftLatitudeTextView.setText(R.string.uxsdk_string_default_value)
//        aircraftLongitudeTextView.setText(R.string.uxsdk_string_default_value)
//        aircraftAltitudeTextView.setText(R.string.uxsdk_string_default_value)
//        baseStationLatitudeTextView.setText(R.string.uxsdk_string_default_value)
//        baseStationLongitudeTextView.setText(R.string.uxsdk_string_default_value)
//        baseStationAltitudeTextView.setText(R.string.uxsdk_string_default_value)
//        courseAngleTextView.setText(R.string.uxsdk_string_default_value)
//        orientationTextView.setText(R.string.uxsdk_string_default_value)
//        orientationImageView.setColorFilter(orientationDisabledColor)
//        positioningTextView.setText(R.string.uxsdk_string_default_value)
//    }
//
//    private fun updateRTKError(error: DJIError?) {
//        if (error === DJIFlightControllerError.RTK_CONNECTION_BROKEN) {
//            gpsBaseStationTextView.setText(R.string.uxsdk_string_default_value)
//            beiDouBaseStationTextView.setText(R.string.uxsdk_string_default_value)
//            glonassBaseStationTextView.setText(R.string.uxsdk_string_default_value)
//            galileoBaseStationTextView.setText(R.string.uxsdk_string_default_value)
//            baseStationLatitudeTextView.setText(R.string.uxsdk_string_default_value)
//            baseStationLongitudeTextView.setText(R.string.uxsdk_string_default_value)
//            baseStationAltitudeTextView.setText(R.string.uxsdk_string_default_value)
//        }
//    }
//
//    private fun updateOrientationStatus(isRTKHeadingValid: Boolean, headingSolution: HeadingSolution?) {
//        if (!isRTKHeadingValid || headingSolution == null) {
//            orientationTextView.setText(R.string.uxsdk_string_default_value)
//            orientationImageView.setColorFilter(orientationDisabledColor)
//        } else {
//            orientationTextView.text = getRTKStatusName(headingSolution)
//            orientationImageView.setColorFilter(orientationEnabledColor)
//        }
//    }
//
//    private fun updatePositionStatus(positioningSolution: PositioningSolution) {
//        if (positioningSolution == PositioningSolution.NONE) {
//            aircraftLatitudeTextView.setText(R.string.uxsdk_string_default_value)
//            aircraftLongitudeTextView.setText(R.string.uxsdk_string_default_value)
//            aircraftAltitudeTextView.setText(R.string.uxsdk_string_default_value)
//            positioningTextView.setText(R.string.uxsdk_string_default_value)
//        } else {
//            positioningTextView.text = getRTKStatusName(positioningSolution)
//        }
//    }
//
//    private fun updateStandardDeviation(standardDeviation: StandardDeviation) {
//        val resourceString = if (standardDeviation.unitType == UnitConversionUtil.UnitType.IMPERIAL) {
//            R.string.uxsdk_value_feet
//        } else {
//            R.string.uxsdk_value_meters
//        }
//        val standardDeviationStr = (resources.getString(resourceString, String.format(Locale.US, "%s", standardDeviation.latitude)) + "\n"
//                + resources.getString(resourceString, String.format(Locale.US, "%s", standardDeviation.longitude)) + "\n"
//                + resources.getString(resourceString, String.format(Locale.US, "%s", standardDeviation.altitude)))
//        standardDeviationTextView.text = standardDeviationStr
//        widgetStateDataProcessor.onNext(StandardDeviationUpdated(standardDeviation))
//    }
//
//    private fun updateBeidouSatelliteDisplay(rtkState: RTKState) {
//        if (rtkState.mobileStationReceiver1BeiDouInfo.isConstellationSupported
//                || rtkState.mobileStationReceiver2BeiDouInfo.isConstellationSupported
//                || rtkState.baseStationReceiverBeiDouInfo.isConstellationSupported) {
//            beiDouAntenna1TextView.text = rtkState.mobileStationReceiver1BeiDouInfo
//                    .satelliteCount.toString()
//            beiDouAntenna2TextView.text = rtkState.mobileStationReceiver2BeiDouInfo
//                    .satelliteCount.toString()
//            beiDouBaseStationTextView.text = rtkState.baseStationReceiverBeiDouInfo
//                    .satelliteCount.toString()
//        } else {
//            beiDouAntenna1TextView.setText(R.string.uxsdk_string_default_value)
//            beiDouAntenna2TextView.setText(R.string.uxsdk_string_default_value)
//            beiDouBaseStationTextView.setText(R.string.uxsdk_string_default_value)
//        }
//    }
//
//    private fun updateGLONASSSatelliteDisplay(rtkState: RTKState) {
//        if (rtkState.mobileStationReceiver1GLONASSInfo.isConstellationSupported
//                || rtkState.mobileStationReceiver2GLONASSInfo.isConstellationSupported
//                || rtkState.baseStationReceiverGLONASSInfo.isConstellationSupported) {
//            glonassAntenna1TextView.text = rtkState.mobileStationReceiver1GLONASSInfo
//                    .satelliteCount.toString()
//            glonassAntenna2TextView.text = rtkState.mobileStationReceiver2GLONASSInfo
//                    .satelliteCount.toString()
//            glonassBaseStationTextView.text = rtkState.baseStationReceiverGLONASSInfo
//                    .satelliteCount.toString()
//        } else {
//            glonassAntenna1TextView.setText(R.string.uxsdk_string_default_value)
//            glonassAntenna2TextView.setText(R.string.uxsdk_string_default_value)
//            glonassBaseStationTextView.setText(R.string.uxsdk_string_default_value)
//        }
//    }
//
//    private fun updateGalileoSatelliteDisplay(rtkState: RTKState) {
//        if (rtkState.mobileStationReceiver1GalileoInfo.isConstellationSupported
//                || rtkState.mobileStationReceiver2GalileoInfo.isConstellationSupported
//                || rtkState.baseStationReceiverGalileoInfo.isConstellationSupported) {
//            galileoAntenna1TextView.text = rtkState.mobileStationReceiver1GalileoInfo
//                    .satelliteCount.toString()
//            galileoAntenna2TextView.text = rtkState.mobileStationReceiver2GalileoInfo
//                    .satelliteCount.toString()
//            galileoBaseStationTextView.text = rtkState.baseStationReceiverGalileoInfo
//                    .satelliteCount.toString()
//        } else {
//            galileoAntenna1TextView.setText(R.string.uxsdk_string_default_value)
//            galileoAntenna2TextView.setText(R.string.uxsdk_string_default_value)
//            galileoBaseStationTextView.setText(R.string.uxsdk_string_default_value)
//        }
//    }
//
//    private fun updateNetworkServiceStatus(networkServiceState: RTKNetworkServiceState) {
//        val rtkStatusStr: String
//        var rtkStatusColor: Int = getRTKConnectionStatusLabelTextColor(RTKBaseStationState.DISCONNECTED)
//        when (networkServiceState.state) {
//            NetworkServiceChannelState.TRANSMITTING ->
//                if (networkServiceState.isRTKBeingUsed) {
//                    rtkStatusColor = getRTKConnectionStatusLabelTextColor(RTKBaseStationState.CONNECTED_IN_USE)
//                    rtkStatusStr = getString(R.string.uxsdk_rtk_state_connect)
//                } else {
//                    rtkStatusColor = getRTKConnectionStatusLabelTextColor(RTKBaseStationState.CONNECTED_NOT_IN_USE)
//                    rtkStatusStr = getString(R.string.uxsdk_rtk_state_connect_not_healthy)
//                }
//            NetworkServiceChannelState.SERVICE_SUSPENSION -> rtkStatusStr = getString(R.string.uxsdk_rtk_state_pause)
//            NetworkServiceChannelState.ACCOUNT_EXPIRED -> rtkStatusStr = getString(R.string.uxsdk_rtk_state_account_expire)
//            NetworkServiceChannelState.NETWORK_NOT_REACHABLE -> rtkStatusStr = getString(R.string.uxsdk_rtk_state_network_err)
//            NetworkServiceChannelState.LOGIN_FAILURE -> rtkStatusStr = getString(R.string.uxsdk_rtk_state_auth_failed)
//            NetworkServiceChannelState.UNKNOWN -> rtkStatusStr =
//                    if (networkServiceState.isNetworkServiceOpen) {
//                        resources.getString(R.string.uxsdk_rtk_nrtk_state_inner_error, getRTKTypeName(networkServiceState.rtkSignal))
//                    } else {
//                        getString(R.string.uxsdk_rtk_nrtk_state_unknown)
//                    }
//            NetworkServiceChannelState.ACCOUNT_ERROR -> rtkStatusStr = getString(R.string.uxsdk_rtk_nrtk_account_error)
//            NetworkServiceChannelState.CONNECTING -> rtkStatusStr = getString(R.string.uxsdk_rtk_nrtk_connecting)
//            NetworkServiceChannelState.INVALID_REQUEST -> rtkStatusStr = getString(R.string.uxsdk_rtk_nrtk_invalid_request)
//            NetworkServiceChannelState.SERVER_NOT_REACHABLE -> rtkStatusStr = getString(R.string.uxsdk_rtk_nrtk_server_not_reachable)
//            else -> rtkStatusStr = getString(R.string.uxsdk_rtk_state_disconnect)
//        }
//        rtkStatusTextView.text = rtkStatusStr
//        rtkStatusTextView.setTextColor(rtkStatusColor)
//        widgetStateDataProcessor.onNext(RTKNetworkServiceStateUpdated(networkServiceState))
//    }
//
//    private fun updateBaseStationStatus(connectionState: RTKBaseStationState) {
//        when (connectionState) {
//            RTKBaseStationState.CONNECTED_IN_USE -> {
//                rtkStatusTextView.setText(R.string.uxsdk_rtk_state_connect)
//                rtkStatusTextView.setTextColor(getRTKConnectionStatusLabelTextColor(RTKBaseStationState.CONNECTED_IN_USE))
//            }
//            RTKBaseStationState.CONNECTED_NOT_IN_USE -> {
//                rtkStatusTextView.setText(R.string.uxsdk_rtk_state_connect_not_healthy)
//                rtkStatusTextView.setTextColor(getRTKConnectionStatusLabelTextColor(RTKBaseStationState.CONNECTED_NOT_IN_USE))
//            }
//            RTKBaseStationState.DISCONNECTED -> {
//                rtkStatusTextView.setText(R.string.uxsdk_rtk_state_disconnect)
//                rtkStatusTextView.textColor = getRTKConnectionStatusLabelTextColor(RTKBaseStationState.DISCONNECTED)
//            }
//        }
//        widgetStateDataProcessor.onNext(RTKBaseStationStateUpdated(connectionState))
//    }
//
//    private fun updateBaseStationTitle(rtkSignal: RTKSignal) {
//        val name = getRTKTypeName(rtkSignal)
//        baseStationCoordinatesTitleTextView.text = name
//        rtkStatusTitleTextView.text = resources.getString(R.string.uxsdk_rtk_status_desc, name)
//        widgetStateDataProcessor.onNext(RTKSignalUpdated(rtkSignal))
//    }
//
//    private fun getRTKTypeName(rtkSignal: RTKSignal): String {
//        return when (rtkSignal) {
//            RTKSignal.NETWORK_RTK -> getString(R.string.uxsdk_rtk_type_nrtk)
//            RTKSignal.D_RTK_2 -> getString(R.string.uxsdk_rtk_type_rtk_mobile_station)
//            RTKSignal.BASE_STATION -> getString(R.string.uxsdk_rtk_type_rtk_base_station)
//            RTKSignal.CUSTOM_NETWORK -> getString(R.string.uxsdk_rtk_type_custom_rtk)
//        }
//    }
//
//    private fun getRTKStatusName(headingSolution: HeadingSolution): String {
//        return when (headingSolution) {
//            HeadingSolution.NONE -> getString(R.string.uxsdk_rtk_solution_none)
//            HeadingSolution.SINGLE_POINT -> getString(R.string.uxsdk_rtk_solution_single)
//            HeadingSolution.FLOAT -> getString(R.string.uxsdk_rtk_solution_float)
//            HeadingSolution.FIXED_POINT -> getString(R.string.uxsdk_rtk_solution_fixed)
//            HeadingSolution.UNKNOWN -> getString(R.string.uxsdk_rtk_solution_unknown)
//            else -> getString(R.string.uxsdk_rtk_solution_unknown)
//        }
//    }
//
//    private fun getRTKStatusName(positioningSolution: PositioningSolution): String {
//        return when (positioningSolution) {
//            PositioningSolution.NONE -> getString(R.string.uxsdk_rtk_solution_none)
//            PositioningSolution.SINGLE_POINT -> getString(R.string.uxsdk_rtk_solution_single)
//            PositioningSolution.FLOAT -> getString(R.string.uxsdk_rtk_solution_float)
//            PositioningSolution.FIXED_POINT -> getString(R.string.uxsdk_rtk_solution_fixed)
//            PositioningSolution.UNKNOWN -> getString(R.string.uxsdk_rtk_solution_unknown)
//            else -> getString(R.string.uxsdk_rtk_solution_unknown)
//        }
//    }
//
//    private fun checkAndUpdateOrientation() {
//        if (!isInEditMode) {
//            addDisposable(widgetModel.rtkState
//                    .firstOrError()
//                    .observeOn(SchedulerProvider.ui())
//                    .subscribe(Consumer { updateOrientationStatus(it.isHeadingValid, it.headingSolution) },
//                            RxUtil.logErrorConsumer(TAG, "updateOrientation")))
//        }
//    }
//
//    private fun checkAndUpdateModel() {
//        if (!isInEditMode) {
//            addDisposable(widgetModel.model
//                    .firstOrError()
//                    .observeOn(SchedulerProvider.ui())
//                    .subscribe(Consumer { updateModel(it) },
//                            RxUtil.logErrorConsumer(TAG, "updateModel")))
//        }
//    }
//
//    //endregion
//
//    //region Customization
//    override fun getIdealDimensionRatioString(): String {
//        return getString(R.string.uxsdk_widget_rtk_satellite_status_ratio)
//    }
//
//    /**
//     * Set the text appearance of the RTK connection state title.
//     *
//     * @param textAppearance Style resource for text appearance
//     */
//    fun setRTKConnectionStatusTitleTextAppearance(@StyleRes textAppearance: Int) {
//        rtkStatusTitleTextView.setTextAppearance(context, textAppearance)
//    }
//
//    /**
//     * Set the text appearance of the RTK connection state.
//     *
//     * @param textAppearance Style resource for text appearance
//     */
//    fun setRTKConnectionStatusTextAppearance(@StyleRes textAppearance: Int) {
//        rtkStatusTextView.setTextAppearance(context, textAppearance)
//    }
//
//    /**
//     * Set the text color of the RTK connection state when the RTKBaseStationState is the given
//     * value.
//     *
//     * @param state The state for which to set the text color.
//     * @param color The color of the text
//     */
//    fun setRTKConnectionStatusLabelTextColor(state: RTKBaseStationState, @ColorInt color: Int) {
//        connectionStateTextColorMap[state] = color
//        widgetModel.updateRTKConnectionState()
//    }
//
//    /**
//     * Get the text color of the RTK connection state when the RTKBaseStationState is the given
//     * value.
//     *
//     * @param state The state for which to get the text color.
//     * @return The color of the text
//     */
//    @ColorInt
//    fun getRTKConnectionStatusLabelTextColor(state: RTKBaseStationState): Int {
//        return (connectionStateTextColorMap[state]?.let { it }
//                ?: getColor(R.color.uxsdk_rtk_status_disconnected))
//    }
//
//    /**
//     * Set text appearance of the RTK labels text views
//     *
//     * @param textAppearance Style resource for text appearance
//     */
//    fun setRTKLabelsTextAppearance(@StyleRes textAppearance: Int) {
//        antenna1TitleTextView.setTextAppearance(context, textAppearance)
//        antenna2TitleTextView.setTextAppearance(context, textAppearance)
//        gpsTitleTextView.setTextAppearance(context, textAppearance)
//        beiDouTitleTextView.setTextAppearance(context, textAppearance)
//        glonassTitleTextView.setTextAppearance(context, textAppearance)
//        galileoTitleTextView.setTextAppearance(context, textAppearance)
//        latitudeTitleTextView.setTextAppearance(context, textAppearance)
//        longitudeTitleTextView.setTextAppearance(context, textAppearance)
//        altitudeTitleTextView.setTextAppearance(context, textAppearance)
//        aircraftCoordinatesTitleTextView.setTextAppearance(context, textAppearance)
//        baseStationCoordinatesTitleTextView.setTextAppearance(context, textAppearance)
//        courseAngleTitleTextView.setTextAppearance(context, textAppearance)
//        orientationTitleTextView.setTextAppearance(context, textAppearance)
//        positioningTitleTextView.setTextAppearance(context, textAppearance)
//        standardDeviationTitleTextView.setTextAppearance(context, textAppearance)
//    }
//
//    /**
//     * Set text appearance of the RTK values text views
//     *
//     * @param textAppearance Style resource for text appearance
//     */
//    fun setRTKValuesTextAppearance(@StyleRes textAppearance: Int) {
//        gpsAntenna1TextView.setTextAppearance(context, textAppearance)
//        gpsAntenna2TextView.setTextAppearance(context, textAppearance)
//        gpsBaseStationTextView.setTextAppearance(context, textAppearance)
//        beiDouAntenna1TextView.setTextAppearance(context, textAppearance)
//        beiDouAntenna2TextView.setTextAppearance(context, textAppearance)
//        beiDouBaseStationTextView.setTextAppearance(context, textAppearance)
//        glonassAntenna1TextView.setTextAppearance(context, textAppearance)
//        glonassAntenna2TextView.setTextAppearance(context, textAppearance)
//        glonassBaseStationTextView.setTextAppearance(context, textAppearance)
//        galileoAntenna1TextView.setTextAppearance(context, textAppearance)
//        galileoAntenna2TextView.setTextAppearance(context, textAppearance)
//        galileoBaseStationTextView.setTextAppearance(context, textAppearance)
//        aircraftLatitudeTextView.setTextAppearance(context, textAppearance)
//        aircraftLongitudeTextView.setTextAppearance(context, textAppearance)
//        aircraftAltitudeTextView.setTextAppearance(context, textAppearance)
//        baseStationLatitudeTextView.setTextAppearance(context, textAppearance)
//        baseStationLongitudeTextView.setTextAppearance(context, textAppearance)
//        baseStationAltitudeTextView.setTextAppearance(context, textAppearance)
//        courseAngleTextView.setTextAppearance(context, textAppearance)
//        orientationTextView.setTextAppearance(context, textAppearance)
//        positioningTextView.setTextAppearance(context, textAppearance)
//        standardDeviationTextView.setTextAppearance(context, textAppearance)
//    }
//
//    /**
//     * Set the resource ID for the orientation icon.
//     *
//     * @param resourceId Integer ID of the drawable resource
//     */
//    fun setOrientationIcon(@DrawableRes resourceId: Int) {
//        orientationIcon = getDrawable(resourceId)
//    }
//
//    /**
//     * Set the image displayed behind the satellite state table.
//     *
//     * @param resourceId Integer ID of the drawable resource
//     */
//    fun setTableBackground(@DrawableRes resourceId: Int) {
//        tableBackground = getDrawable(resourceId)
//    }
//
//    //Initialize all customizable attributes
//    @SuppressLint("Recycle")
//    private fun initAttributes(context: Context, attrs: AttributeSet) {
//        context.obtainStyledAttributes(attrs, R.styleable.RTKSatelliteStatusWidget).use { typedArray ->
//            typedArray.getResourceIdAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkConnectionStatusTitleTextAppearance) {
//                setRTKConnectionStatusTitleTextAppearance(it)
//            }
//            typedArray.getDimensionAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkConnectionStatusTitleTextSize) {
//                rtkConnectionStatusTitleTextSize = DisplayUtil.pxToSp(context, it)
//            }
//            typedArray.getColorAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkConnectionStatusTitleTextColor) {
//                rtkConnectionStatusTitleTextColor = it
//            }
//            typedArray.getDrawableAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkConnectionStatusTitleBackgroundDrawable) {
//                rtkConnectionStatusTitleTextBackground = it
//            }
//            typedArray.getResourceIdAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkConnectionStatusTextAppearance) {
//                setRTKConnectionStatusTextAppearance(it)
//            }
//            typedArray.getDimensionAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkConnectionStatusTextSize) {
//                rtkConnectionStatusTextSize = DisplayUtil.pxToSp(context, it)
//            }
//            typedArray.getColorAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkConnectionStatusConnectedInUseTextColor) {
//                setRTKConnectionStatusLabelTextColor(RTKBaseStationState.CONNECTED_IN_USE, it)
//            }
//            typedArray.getColorAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkConnectionStatusConnectedNotInUseTextColor) {
//                setRTKConnectionStatusLabelTextColor(RTKBaseStationState.CONNECTED_NOT_IN_USE, it)
//            }
//            typedArray.getColorAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkConnectionStatusDisconnectedTextColor) {
//                setRTKConnectionStatusLabelTextColor(RTKBaseStationState.DISCONNECTED, it)
//            }
//            typedArray.getDrawableAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkConnectionStatusBackgroundDrawable) {
//                rtkConnectionStatusTextBackground = it
//            }
//            typedArray.getResourceIdAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkLabelsTextAppearance) {
//                setRTKLabelsTextAppearance(it)
//            }
//            typedArray.getDimensionAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkLabelsTextSize) {
//                rtkLabelsTextSize = DisplayUtil.pxToSp(context, it)
//            }
//            typedArray.getColorAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkLabelsTextColor) {
//                rtkLabelsTextColor = it
//            }
//            typedArray.getDrawableAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkLabelsBackgroundDrawable) {
//                rtkLabelsTextBackground = it
//            }
//            typedArray.getResourceIdAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkValuesTextAppearance) {
//                setRTKValuesTextAppearance(it)
//            }
//            typedArray.getDimensionAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkValuesTextSize) {
//                rtkValuesTextSize = DisplayUtil.pxToSp(context, it)
//            }
//            typedArray.getColorAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkValuesTextColor) {
//                rtkValuesTextColor = it
//            }
//            typedArray.getDrawableAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkValuesBackgroundDrawable) {
//                rtkValuesTextBackground = it
//            }
//            typedArray.getColorAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_orientationIconEnabledColor) {
//                orientationEnabledColor = it
//            }
//            typedArray.getColorAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_orientationIconDisabledColor) {
//                orientationDisabledColor = it
//            }
//            typedArray.getDrawableAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_orientationIcon) {
//                orientationIcon = it
//            }
//            typedArray.getColorAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkSeparatorsColor) {
//                rtkSeparatorsColor = it
//            }
//            typedArray.getDrawableAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_tableBackground) {
//                tableBackground = it
//            }
//            isBeiDouSatelliteInfoVisible = typedArray.getBoolean(R.styleable.RTKSatelliteStatusWidget_uxsdk_beiDouSatellitesVisibility, true)
//            isGLONASSSatelliteInfoVisible = typedArray.getBoolean(R.styleable.RTKSatelliteStatusWidget_uxsdk_glonassSatellitesVisibility, true)
//            isGalileoSatelliteInfoVisible = typedArray.getBoolean(R.styleable.RTKSatelliteStatusWidget_uxsdk_galileoSatellitesVisibility, true)
//        }
//    }
//    //endregion
//
//    //region Hooks
//
//    /**
//     * Get the [ModelState] updates
//     */
//    @SuppressWarnings
//    override fun getWidgetStateUpdate(): Flowable<ModelState> {
//        return super.getWidgetStateUpdate()
//    }
//
//    /**
//     * Class defines the widget state updates
//     */
//    sealed class ModelState {
//        /**
//         * Product connection update
//         */
//        data class ProductConnected(val isConnected: Boolean) : ModelState()
//
//        /**
//         * RTK connection update
//         */
//        data class RTKConnectionUpdated(val isConnected: Boolean) : ModelState()
//
//        /**
//         * RTK state update
//         */
//        data class RTKStateUpdated(val rtkState: RTKState) : ModelState()
//
//        /**
//         * Model update
//         */
//        data class ModelUpdated(val model: Model) : ModelState()
//
//        /**
//         * RTK signal update
//         */
//        data class RTKSignalUpdated(val source: RTKSignal) : ModelState()
//
//        /**
//         * Standard deviation update
//         */
//        data class StandardDeviationUpdated(val standardDeviation: StandardDeviation) : ModelState()
//
//        /**
//         * RTK base station state update
//         */
//        data class RTKBaseStationStateUpdated(val state: RTKBaseStationState) : ModelState()
//
//        /**
//         * RTK network service state update
//         */
//        data class RTKNetworkServiceStateUpdated(val state: RTKNetworkServiceState) : ModelState()
//    }
//    //endregion

    sealed class ModelState

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        //暂未实现
    }

    override fun reactToModelChanges() {
        //暂未实现
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }
}