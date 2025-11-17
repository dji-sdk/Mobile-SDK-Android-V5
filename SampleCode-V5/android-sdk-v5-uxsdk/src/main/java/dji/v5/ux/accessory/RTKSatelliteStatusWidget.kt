package dji.v5.ux.accessory

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import dji.sdk.keyvalue.utils.ProductUtil
import dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource
import dji.sdk.keyvalue.value.rtkbasestation.RTKServiceState
import dji.sdk.keyvalue.value.rtkmobilestation.GNSSType
import dji.sdk.keyvalue.value.rtkmobilestation.RTKPositioningSolution
import dji.v5.manager.aircraft.rtk.RTKCenter
import dji.v5.manager.aircraft.rtk.RTKLocationInfo
import dji.v5.manager.aircraft.rtk.RTKSystemState
import dji.v5.utils.common.DisplayUtil
import dji.v5.utils.common.LogUtils
import dji.v5.ux.R

import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.GlobalPreferencesManager
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.util.UnitConversionUtil
import java.util.*

/**
 * Description :This widget shows all the information related to RTK.  This includes coordinates and altitude
 * of the aircraft and base station, course angle, GLONASS, Beidou, Galileo, and GPS satellite
 * counts for both antennas and the base station, and overall state of the RTK system.
 *
 * @author: Byte.Cai
 *  date : 2022/5/23
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
private const val TAG = "RTKSatelliteStatusWidget"

open class RTKSatelliteStatusWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayoutWidget<RTKSatelliteStatusWidget.ModelState>(context, attrs, defStyleAttr) {
    //region Fields
    private val rtkStatusTitleTextView: TextView = findViewById(R.id.textview_rtk_status_title)
    private val rtkStatusTextView: TextView = findViewById(R.id.textview_rtk_status)
    private val baseStationConnectImageView: ImageView = findViewById(R.id.imageview_connect_arrow)
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
    private val baseStationCoordinatesTitleTextView: TextView =
        findViewById(R.id.textview_base_station_coordinates_title)
    private val baseStationLatitudeTextView: TextView = findViewById(R.id.textview_base_station_latitude)
    private val baseStationLongitudeTextView: TextView = findViewById(R.id.textview_base_station_longitude)
    private val baseStationAltitudeTextView: TextView = findViewById(R.id.textview_base_station_altitude)
    private val courseAngleTitleTextView: TextView = findViewById(R.id.textview_course_angle_title)
    private val courseAngleTextView: TextView = findViewById(R.id.textview_course_angle_value)
    private val orientationTitleTextView: TextView = findViewById(R.id.textview_orientation_title)
    private val orientationTextView: TextView = findViewById(R.id.textview_orientation)
    private val positioningTitleTextView: TextView = findViewById(R.id.textview_positioning_title)
    private val positioningTextView: TextView = findViewById(R.id.textview_positioning)
    private val standardDeviationTitleTextView: TextView = findViewById(R.id.textview_standard_deviation_title)
    private val standardDeviationTextView: TextView = findViewById(R.id.textview_standard_deviation)
    private val rtkAircraftSeparator: View = findViewById(R.id.rtk_aircraft_separator)
    private val rtkBaseStationSeparator: View = findViewById(R.id.rtk_base_station_separator)
    private val rtkOrientationPositioningSeparator: View = findViewById(R.id.rtk_orientation_positioning_separator)
    private val rtkLocationSeparator: View = findViewById(R.id.rtk_location_separator)
    private val rtkSatelliteCountSeparator: View = findViewById(R.id.rtk_satellite_count_separator)
    private var positioningSolution :RTKPositioningSolution ?= null
    private val connectionStateTextColorMap: MutableMap<RTKSatelliteStatusWidgetModel.RTKBaseStationState, Int> =
        mutableMapOf(
            RTKSatelliteStatusWidgetModel.RTKBaseStationState.CONNECTED_IN_USE to getColor(R.color.uxsdk_rtk_status_connected_in_use),
            RTKSatelliteStatusWidgetModel.RTKBaseStationState.CONNECTED_NOT_IN_USE to getColor(R.color.uxsdk_rtk_status_connected_not_in_use),
            RTKSatelliteStatusWidgetModel.RTKBaseStationState.DISCONNECTED to getColor(R.color.uxsdk_rtk_status_disconnected)
        )

    private val widgetModel by lazy {
        RTKSatelliteStatusWidgetModel(
            DJISDKModel.getInstance(),
            ObservableInMemoryKeyedStore.getInstance(),
            GlobalPreferencesManager.getInstance(),
            RTKCenter.getInstance()
        )
    }


    /**
     * The text size of the RTK connection state title.
     */
    var rtkConnectionStatusTitleTextSize: Float
        @JvmName("getRTKConnectionStatusTitleTextSize")
        @Dimension
        get() = rtkStatusTitleTextView.textSize
        @JvmName("setRTKConnectionStatusTitleTextSize")
        set(@Dimension textSize) {
            rtkStatusTitleTextView.textSize = textSize
        }

    /**
     * The text color of the RTK connection state title.
     */
    var rtkConnectionStatusTitleTextColor: Int
        @JvmName("getRTKConnectionStatusTitleTextColor")
        @ColorInt
        get() = rtkStatusTitleTextView.currentTextColor
        @JvmName("setRTKConnectionStatusTitleTextColor")
        set(@ColorInt color) {
            rtkStatusTitleTextView.textColor = color
        }

    /**
     * The background of the RTK connection state title.
     */
    var rtkConnectionStatusTitleTextBackground: Drawable?
        @JvmName("getRTKConnectionStatusTitleTextBackground")
        get() = rtkStatusTitleTextView.background
        @JvmName("setRTKConnectionStatusTitleTextBackground")
        set(drawable) {
            rtkStatusTitleTextView.background = drawable
        }

    /**
     * The text size of the RTK connection state.
     */
    var rtkConnectionStatusTextSize: Float
        @JvmName("getRTKConnectionStatusTextSize")
        @Dimension
        get() = rtkStatusTextView.textSize
        @JvmName("setRTKConnectionStatusTextSize")
        set(@Dimension textSize) {
            rtkStatusTextView.textSize = textSize
        }

    /**
     * The background of the RTK connection state.
     */
    var rtkConnectionStatusTextBackground: Drawable?
        @JvmName("getRTKConnectionStatusTextBackground")
        get() = rtkStatusTextView.background
        @JvmName("setRTKConnectionStatusTextBackground")
        set(drawable) {
            rtkStatusTextView.background = drawable
        }

    /**
     * The text color state list of the RTK labels text views
     */
    var rtkLabelsTextColors: ColorStateList?
        @JvmName("getRTKLabelsTextColors")
        get() = antenna1TitleTextView.textColors
        @JvmName("setRTKLabelsTextColors")
        set(colorStateList) {
            antenna1TitleTextView.setTextColor(colorStateList)
            antenna2TitleTextView.setTextColor(colorStateList)
            gpsTitleTextView.setTextColor(colorStateList)
            beiDouTitleTextView.setTextColor(colorStateList)
            glonassTitleTextView.setTextColor(colorStateList)
            galileoTitleTextView.setTextColor(colorStateList)
            latitudeTitleTextView.setTextColor(colorStateList)
            longitudeTitleTextView.setTextColor(colorStateList)
            altitudeTitleTextView.setTextColor(colorStateList)
            aircraftCoordinatesTitleTextView.setTextColor(colorStateList)
            baseStationCoordinatesTitleTextView.setTextColor(colorStateList)
            courseAngleTitleTextView.setTextColor(colorStateList)
            orientationTitleTextView.setTextColor(colorStateList)
            positioningTitleTextView.setTextColor(colorStateList)
            standardDeviationTitleTextView.setTextColor(colorStateList)
        }

    /**
     * The text color of the RTK labels text views
     */
    var rtkLabelsTextColor: Int
        @JvmName("getRTKLabelsTextColor")
        @ColorInt
        get() = antenna1TitleTextView.currentTextColor
        @JvmName("setRTKLabelsTextColor")
        set(@ColorInt color) {
            antenna1TitleTextView.textColor = color
            antenna2TitleTextView.textColor = color
            gpsTitleTextView.textColor = color
            beiDouTitleTextView.textColor = color
            glonassTitleTextView.textColor = color
            galileoTitleTextView.textColor = color
            latitudeTitleTextView.textColor = color
            longitudeTitleTextView.textColor = color
            altitudeTitleTextView.textColor = color
            aircraftCoordinatesTitleTextView.textColor = color
            baseStationCoordinatesTitleTextView.textColor = color
            courseAngleTitleTextView.textColor = color
            orientationTitleTextView.textColor = color
            positioningTitleTextView.textColor = color
            standardDeviationTitleTextView.textColor = color
        }

    /**
     * The text size of the RTK labels text views
     */
    var rtkLabelsTextSize: Float
        @JvmName("getRTKLabelsTextSize")
        @Dimension
        get() = antenna1TitleTextView.textSize
        @JvmName("setRTKLabelsTextSize")
        set(@Dimension textSize) {
            antenna1TitleTextView.textSize = textSize
            antenna2TitleTextView.textSize = textSize
            gpsTitleTextView.textSize = textSize
            beiDouTitleTextView.textSize = textSize
            glonassTitleTextView.textSize = textSize
            galileoTitleTextView.textSize = textSize
            latitudeTitleTextView.textSize = textSize
            longitudeTitleTextView.textSize = textSize
            altitudeTitleTextView.textSize = textSize
            aircraftCoordinatesTitleTextView.textSize = textSize
            baseStationCoordinatesTitleTextView.textSize = textSize
            courseAngleTitleTextView.textSize = textSize
            orientationTitleTextView.textSize = textSize
            positioningTitleTextView.textSize = textSize
            standardDeviationTitleTextView.textSize = textSize
        }

    /**
     * The background for the RTK labels text views
     */
    var rtkLabelsTextBackground: Drawable?
        @JvmName("getRTKLabelsTextBackground")
        get() = antenna1TitleTextView.background
        @JvmName("setRTKLabelsTextBackground")
        set(drawable) {
            antenna1TitleTextView.background = drawable
            antenna2TitleTextView.background = drawable
            gpsTitleTextView.background = drawable
            beiDouTitleTextView.background = drawable
            glonassTitleTextView.background = drawable
            galileoTitleTextView.background = drawable
            latitudeTitleTextView.background = drawable
            longitudeTitleTextView.background = drawable
            altitudeTitleTextView.background = drawable
            aircraftCoordinatesTitleTextView.background = drawable
            baseStationCoordinatesTitleTextView.background = drawable
            courseAngleTitleTextView.background = drawable
            orientationTitleTextView.background = drawable
            positioningTitleTextView.background = drawable
            standardDeviationTitleTextView.background = drawable

        }

    /**
     * The text color state list of the RTK values text views
     */
    var rtkValuesTextColors: ColorStateList?
        @JvmName("getRTKValuesTextColors")
        get() = gpsAntenna1TextView.textColors
        @JvmName("setRTKValuesTextColors")
        set(colorStateList) {
            gpsAntenna1TextView.setTextColor(colorStateList)
            gpsAntenna2TextView.setTextColor(colorStateList)
            gpsBaseStationTextView.setTextColor(colorStateList)
            beiDouAntenna1TextView.setTextColor(colorStateList)
            beiDouAntenna2TextView.setTextColor(colorStateList)
            beiDouBaseStationTextView.setTextColor(colorStateList)
            glonassAntenna1TextView.setTextColor(colorStateList)
            glonassAntenna2TextView.setTextColor(colorStateList)
            glonassBaseStationTextView.setTextColor(colorStateList)
            galileoAntenna1TextView.setTextColor(colorStateList)
            galileoAntenna2TextView.setTextColor(colorStateList)
            galileoBaseStationTextView.setTextColor(colorStateList)
            aircraftLatitudeTextView.setTextColor(colorStateList)
            aircraftLongitudeTextView.setTextColor(colorStateList)
            aircraftAltitudeTextView.setTextColor(colorStateList)
            baseStationLatitudeTextView.setTextColor(colorStateList)
            baseStationLongitudeTextView.setTextColor(colorStateList)
            baseStationAltitudeTextView.setTextColor(colorStateList)
            courseAngleTextView.setTextColor(colorStateList)
            orientationTextView.setTextColor(colorStateList)
            positioningTextView.setTextColor(colorStateList)
            standardDeviationTextView.setTextColor(colorStateList)
        }

    /**
     * The text color of the RTK values text views
     */
    var rtkValuesTextColor: Int
        @JvmName("getRTKValuesTextColor")
        @ColorInt
        get() = gpsAntenna1TextView.currentTextColor
        @JvmName("setRTKValuesTextColor")
        set(@ColorInt color) {
            gpsAntenna1TextView.textColor = color
            gpsAntenna2TextView.textColor = color
            gpsBaseStationTextView.textColor = color
            beiDouAntenna1TextView.textColor = color
            beiDouAntenna2TextView.textColor = color
            beiDouBaseStationTextView.textColor = color
            glonassAntenna1TextView.textColor = color
            glonassAntenna2TextView.textColor = color
            glonassBaseStationTextView.textColor = color
            galileoAntenna1TextView.textColor = color
            galileoAntenna2TextView.textColor = color
            galileoBaseStationTextView.textColor = color
            aircraftLatitudeTextView.textColor = color
            aircraftLongitudeTextView.textColor = color
            aircraftAltitudeTextView.textColor = color
            baseStationLatitudeTextView.textColor = color
            baseStationLongitudeTextView.textColor = color
            baseStationAltitudeTextView.textColor = color
            courseAngleTextView.textColor = color
            orientationTextView.textColor = color
            positioningTextView.textColor = color
            standardDeviationTextView.textColor = color
        }

    /**
     * The text size of the RTK values text views
     */
    var rtkValuesTextSize: Float
        @JvmName("getRTKValuesTextSize")
        @Dimension
        get() = gpsAntenna1TextView.textSize
        @JvmName("setRTKValuesTextSize")
        set(@Dimension textSize) {
            gpsAntenna1TextView.textSize = textSize
            gpsAntenna2TextView.textSize = textSize
            gpsBaseStationTextView.textSize = textSize
            beiDouAntenna1TextView.textSize = textSize
            beiDouAntenna2TextView.textSize = textSize
            beiDouBaseStationTextView.textSize = textSize
            glonassAntenna1TextView.textSize = textSize
            glonassAntenna2TextView.textSize = textSize
            glonassBaseStationTextView.textSize = textSize
            galileoAntenna1TextView.textSize = textSize
            galileoAntenna2TextView.textSize = textSize
            galileoBaseStationTextView.textSize = textSize
            aircraftLatitudeTextView.textSize = textSize
            aircraftLongitudeTextView.textSize = textSize
            aircraftAltitudeTextView.textSize = textSize
            baseStationLatitudeTextView.textSize = textSize
            baseStationLongitudeTextView.textSize = textSize
            baseStationAltitudeTextView.textSize = textSize
            courseAngleTextView.textSize = textSize
            orientationTextView.textSize = textSize
            positioningTextView.textSize = textSize
            standardDeviationTextView.textSize = textSize
        }

    /**
     * The background for the RTK values text views
     */
    var rtkValuesTextBackground: Drawable?
        @JvmName("getRTKValuesTextBackground")
        get() = gpsAntenna1TextView.background
        @JvmName("setRTKValuesTextBackground")
        set(drawable) {
            gpsAntenna1TextView.background = drawable
            gpsAntenna2TextView.background = drawable
            gpsBaseStationTextView.background = drawable
            beiDouAntenna1TextView.background = drawable
            beiDouAntenna2TextView.background = drawable
            beiDouBaseStationTextView.background = drawable
            glonassAntenna1TextView.background = drawable
            glonassAntenna2TextView.background = drawable
            glonassBaseStationTextView.background = drawable
            galileoAntenna1TextView.background = drawable
            galileoAntenna2TextView.background = drawable
            galileoBaseStationTextView.background = drawable
            aircraftLatitudeTextView.background = drawable
            aircraftLongitudeTextView.background = drawable
            aircraftAltitudeTextView.background = drawable
            baseStationLatitudeTextView.background = drawable
            baseStationLongitudeTextView.background = drawable
            baseStationAltitudeTextView.background = drawable
            courseAngleTextView.background = drawable
            orientationTextView.background = drawable
            positioningTextView.background = drawable
            standardDeviationTextView.background = drawable
        }


    /**
     * The color for the separator line views
     */
    var rtkSeparatorsColor: Int
        @JvmName("getRTKSeparatorsColor")
        @ColorInt
        get() = rtkAircraftSeparator.solidColor
        @JvmName("setRTKSeparatorsColor")
        set(@ColorInt color) {
            rtkAircraftSeparator.setBackgroundColor(color)
            rtkBaseStationSeparator.setBackgroundColor(color)
            rtkOrientationPositioningSeparator.setBackgroundColor(color)
            rtkLocationSeparator.setBackgroundColor(color)
            rtkSatelliteCountSeparator.setBackgroundColor(color)
        }

    /**
     * Shows or hides the BeiDou satellite information.
     */
    var isBeiDouSatelliteInfoVisible = true
        set(isVisible) {
            field = isVisible
            if (isVisible) {
                beiDouTitleTextView.visibility = View.VISIBLE
                beiDouAntenna1TextView.visibility = View.VISIBLE
                beiDouBaseStationTextView.visibility = View.VISIBLE
            } else {
                beiDouTitleTextView.visibility = View.GONE
                beiDouAntenna1TextView.visibility = View.GONE
                beiDouBaseStationTextView.visibility = View.GONE
            }
        }

    /**
     * Shows or hides the GLONASS satellite information.
     */
    var isGLONASSSatelliteInfoVisible = true
        set(isVisible) {
            field = isVisible
            if (isVisible) {
                glonassTitleTextView.visibility = View.VISIBLE
                glonassAntenna1TextView.visibility = View.VISIBLE
                glonassBaseStationTextView.visibility = View.VISIBLE
            } else {
                glonassTitleTextView.visibility = View.GONE
                glonassAntenna1TextView.visibility = View.GONE
                glonassBaseStationTextView.visibility = View.GONE
            }
        }

    /**
     * Shows or hides the Galileo satellite information.
     */
    var isGalileoSatelliteInfoVisible = true
        set(isVisible) {
            field = isVisible
            if (isVisible) {
                galileoTitleTextView.visibility = View.VISIBLE
                galileoAntenna1TextView.visibility = View.VISIBLE
                galileoBaseStationTextView.visibility = View.VISIBLE
            } else {
                galileoTitleTextView.visibility = View.GONE
                galileoAntenna1TextView.visibility = View.GONE
                galileoBaseStationTextView.visibility = View.GONE
            }
        }

    /**
     * The image displayed behind the satellite state table.
     */
    var tableBackground: Drawable?
        get() = tableBackgroundImageView.imageDrawable
        set(value) {
            tableBackgroundImageView.imageDrawable = value
        }
    //end region

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_widget_rtk_satellite_status, this)
    }

    init {
        initItemValues()
        initListener()
        updateLabelView()
        attrs?.let { initAttributes(context, it) }
    }

    private fun updateLabelView() {
        if (ProductUtil.isM3EProduct() || ProductUtil.isM4EProduct() || ProductUtil.isM4DProduct()) {
            //orientation
            orientationTextView.visibility = GONE
            orientationTitleTextView.visibility = GONE
            //Antenna2
            beiDouAntenna2TextView.visibility = GONE
            galileoAntenna2TextView.visibility = GONE
            glonassAntenna2TextView.visibility = GONE
            gpsAntenna2TextView.visibility = GONE
            antenna2TitleTextView.visibility = GONE
            antenna1TitleTextView.visibility = GONE
            //courseAngle
            courseAngleTitleTextView.visibility = GONE
            courseAngleTextView.visibility = GONE


        } else {
            //orientation
            orientationTextView.visibility = VISIBLE
            orientationTitleTextView.visibility = VISIBLE
            //Antenna2
            beiDouAntenna2TextView.visibility = VISIBLE
            galileoAntenna2TextView.visibility = VISIBLE
            glonassAntenna2TextView.visibility = VISIBLE
            gpsAntenna2TextView.visibility = VISIBLE
            antenna2TitleTextView.visibility = VISIBLE
            antenna1TitleTextView.visibility = VISIBLE
            //courseAngle
            courseAngleTitleTextView.visibility = VISIBLE
            courseAngleTextView.visibility = VISIBLE
        }
    }

    private fun initListener() {
        baseStationConnectImageView.setOnClickListener {
            rtkStationListener?.showConnectView()
        }
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

    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection.observeOn(SchedulerProvider.ui()).subscribe { updateUIForIsRTKConnected(it) })
        addReaction(widgetModel.rtkLocationInfo.observeOn(SchedulerProvider.ui()).subscribe(this::updateRTKLocationInfo))

        addReaction(widgetModel.rtkSystemState.observeOn(SchedulerProvider.ui()).subscribe(this::updateRTKSystemState))
        addReaction(widgetModel.standardDeviation.observeOn(SchedulerProvider.ui()).subscribe { updateStandardDeviation(it) })
        addReaction(widgetModel.rtkBaseStationState.observeOn(SchedulerProvider.ui()).subscribe { updateBaseStationStatus(it) })
        addReaction(widgetModel.rtkNetworkServiceState.observeOn(SchedulerProvider.ui()).subscribe { updateNetworkServiceStatus(it) })

    }


    //region Helper methods
    private fun initItemValues() {
        gpsAntenna1TextView.setText(R.string.uxsdk_string_default_value)
        gpsAntenna2TextView.setText(R.string.uxsdk_string_default_value)
        gpsBaseStationTextView.setText(R.string.uxsdk_string_default_value)
        beiDouAntenna1TextView.setText(R.string.uxsdk_string_default_value)
        beiDouAntenna2TextView.setText(R.string.uxsdk_string_default_value)
        beiDouBaseStationTextView.setText(R.string.uxsdk_string_default_value)
        glonassAntenna1TextView.setText(R.string.uxsdk_string_default_value)
        glonassAntenna2TextView.setText(R.string.uxsdk_string_default_value)
        glonassBaseStationTextView.setText(R.string.uxsdk_string_default_value)
        galileoAntenna1TextView.setText(R.string.uxsdk_string_default_value)
        galileoAntenna2TextView.setText(R.string.uxsdk_string_default_value)
        galileoBaseStationTextView.setText(R.string.uxsdk_string_default_value)
        aircraftLatitudeTextView.setText(R.string.uxsdk_string_default_value)
        aircraftLongitudeTextView.setText(R.string.uxsdk_string_default_value)
        aircraftAltitudeTextView.setText(R.string.uxsdk_string_default_value)
        baseStationLatitudeTextView.setText(R.string.uxsdk_string_default_value)
        baseStationLongitudeTextView.setText(R.string.uxsdk_string_default_value)
        baseStationAltitudeTextView.setText(R.string.uxsdk_string_default_value)
        courseAngleTextView.setText(R.string.uxsdk_string_default_value)
        orientationTextView.setText(R.string.uxsdk_string_default_value)
        positioningTextView.setText(R.string.uxsdk_string_default_value)
    }

    //endregion


    //region Customization
    override fun getIdealDimensionRatioString(): String {
        return getString(R.string.uxsdk_widget_rtk_satellite_status_ratio)
    }

    /**
     * Set the text appearance of the RTK connection state title.
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setRTKConnectionStatusTitleTextAppearance(@StyleRes textAppearance: Int) {
        rtkStatusTitleTextView.setTextAppearance(context, textAppearance)
    }

    /**
     * Set the text appearance of the RTK connection state.
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setRTKConnectionStatusTextAppearance(@StyleRes textAppearance: Int) {
        rtkStatusTextView.setTextAppearance(context, textAppearance)
    }

    /**
     * Set the text color of the RTK connection state when the RTKBaseStationState is the given
     * value.
     *
     * @param state The state for which to set the text color.
     * @param color The color of the text
     */
    fun setRTKConnectionStatusLabelTextColor(
        state: RTKSatelliteStatusWidgetModel.RTKBaseStationState, @ColorInt color: Int,
    ) {
        connectionStateTextColorMap[state] = color
        widgetModel.updateRTKConnectionState()
    }

    /**
     * Get the text color of the RTK connection state when the RTKBaseStationState is the given
     * value.
     *
     * @param state The state for which to get the text color.
     * @return The color of the text
     */
    @ColorInt
    fun getRTKConnectionStatusLabelTextColor(state: RTKSatelliteStatusWidgetModel.RTKBaseStationState): Int {
        return (connectionStateTextColorMap[state]?.let { it }
            ?: getColor(R.color.uxsdk_rtk_status_disconnected))
    }

    /**
     * Set text appearance of the RTK labels text views
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setRTKLabelsTextAppearance(@StyleRes textAppearance: Int) {
        antenna1TitleTextView.setTextAppearance(context, textAppearance)
        antenna2TitleTextView.setTextAppearance(context, textAppearance)
        gpsTitleTextView.setTextAppearance(context, textAppearance)
        beiDouTitleTextView.setTextAppearance(context, textAppearance)
        glonassTitleTextView.setTextAppearance(context, textAppearance)
        galileoTitleTextView.setTextAppearance(context, textAppearance)
        latitudeTitleTextView.setTextAppearance(context, textAppearance)
        longitudeTitleTextView.setTextAppearance(context, textAppearance)
        altitudeTitleTextView.setTextAppearance(context, textAppearance)
        aircraftCoordinatesTitleTextView.setTextAppearance(context, textAppearance)
        baseStationCoordinatesTitleTextView.setTextAppearance(context, textAppearance)
        courseAngleTitleTextView.setTextAppearance(context, textAppearance)
        orientationTitleTextView.setTextAppearance(context, textAppearance)
        positioningTitleTextView.setTextAppearance(context, textAppearance)
        standardDeviationTitleTextView.setTextAppearance(context, textAppearance)
    }


    /**
     * Set text appearance of the RTK values text views
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setRTKValuesTextAppearance(@StyleRes textAppearance: Int) {
        gpsAntenna1TextView.setTextAppearance(context, textAppearance)
        gpsAntenna2TextView.setTextAppearance(context, textAppearance)
        gpsBaseStationTextView.setTextAppearance(context, textAppearance)
        beiDouAntenna1TextView.setTextAppearance(context, textAppearance)
        beiDouAntenna2TextView.setTextAppearance(context, textAppearance)
        beiDouBaseStationTextView.setTextAppearance(context, textAppearance)
        glonassAntenna1TextView.setTextAppearance(context, textAppearance)
        glonassAntenna2TextView.setTextAppearance(context, textAppearance)
        glonassBaseStationTextView.setTextAppearance(context, textAppearance)
        galileoAntenna1TextView.setTextAppearance(context, textAppearance)
        galileoAntenna2TextView.setTextAppearance(context, textAppearance)
        galileoBaseStationTextView.setTextAppearance(context, textAppearance)
        aircraftLatitudeTextView.setTextAppearance(context, textAppearance)
        aircraftLongitudeTextView.setTextAppearance(context, textAppearance)
        aircraftAltitudeTextView.setTextAppearance(context, textAppearance)
        baseStationLatitudeTextView.setTextAppearance(context, textAppearance)
        baseStationLongitudeTextView.setTextAppearance(context, textAppearance)
        baseStationAltitudeTextView.setTextAppearance(context, textAppearance)
        courseAngleTextView.setTextAppearance(context, textAppearance)
        orientationTextView.setTextAppearance(context, textAppearance)
        positioningTextView.setTextAppearance(context, textAppearance)
        standardDeviationTextView.setTextAppearance(context, textAppearance)
    }

    //Initialize all customizable attributes
    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.RTKSatelliteStatusWidget).use { typedArray ->
            typedArray.getResourceIdAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkConnectionStatusTitleTextAppearance) {
                setRTKConnectionStatusTitleTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkConnectionStatusTitleTextSize) {
                rtkConnectionStatusTitleTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkConnectionStatusTitleTextColor) {
                rtkConnectionStatusTitleTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkConnectionStatusTitleBackgroundDrawable) {
                rtkConnectionStatusTitleTextBackground = it
            }
            typedArray.getResourceIdAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkConnectionStatusTextAppearance) {
                setRTKConnectionStatusTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkConnectionStatusTextSize) {
                rtkConnectionStatusTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkConnectionStatusConnectedInUseTextColor) {
                setRTKConnectionStatusLabelTextColor(
                    RTKSatelliteStatusWidgetModel.RTKBaseStationState.CONNECTED_IN_USE,
                    it
                )
            }
            typedArray.getColorAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkConnectionStatusConnectedNotInUseTextColor) {
                setRTKConnectionStatusLabelTextColor(
                    RTKSatelliteStatusWidgetModel.RTKBaseStationState.CONNECTED_NOT_IN_USE,
                    it
                )
            }
            typedArray.getColorAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkConnectionStatusDisconnectedTextColor) {
                setRTKConnectionStatusLabelTextColor(RTKSatelliteStatusWidgetModel.RTKBaseStationState.DISCONNECTED, it)
            }
            typedArray.getDrawableAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkConnectionStatusBackgroundDrawable) {
                rtkConnectionStatusTextBackground = it
            }
            typedArray.getResourceIdAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkLabelsTextAppearance) {
                setRTKLabelsTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkLabelsTextSize) {
                rtkLabelsTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkLabelsTextColor) {
                rtkLabelsTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkLabelsBackgroundDrawable) {
                rtkLabelsTextBackground = it
            }
            typedArray.getResourceIdAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkValuesTextAppearance) {
                setRTKValuesTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkValuesTextSize) {
                rtkValuesTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkValuesTextColor) {
                rtkValuesTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkValuesBackgroundDrawable) {
                rtkValuesTextBackground = it
            }

            typedArray.getColorAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_rtkSeparatorsColor) {
                rtkSeparatorsColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.RTKSatelliteStatusWidget_uxsdk_tableBackground) {
                tableBackground = it
            }
            isBeiDouSatelliteInfoVisible =
                typedArray.getBoolean(R.styleable.RTKSatelliteStatusWidget_uxsdk_beiDouSatellitesVisibility, true)
            isGLONASSSatelliteInfoVisible =
                typedArray.getBoolean(R.styleable.RTKSatelliteStatusWidget_uxsdk_glonassSatellitesVisibility, true)
            isGalileoSatelliteInfoVisible =
                typedArray.getBoolean(R.styleable.RTKSatelliteStatusWidget_uxsdk_galileoSatellitesVisibility, true)
        }
    }


    private fun updateBaseStationStatus(connectionState: RTKSatelliteStatusWidgetModel.RTKBaseStationState) {
        when (connectionState) {
            RTKSatelliteStatusWidgetModel.RTKBaseStationState.CONNECTED_IN_USE -> {
                rtkStatusTextView.setText(R.string.uxsdk_rtk_state_connect)
                rtkStatusTextView.setTextColor(getRTKConnectionStatusLabelTextColor(RTKSatelliteStatusWidgetModel.RTKBaseStationState.CONNECTED_IN_USE))
            }
            RTKSatelliteStatusWidgetModel.RTKBaseStationState.CONNECTED_NOT_IN_USE -> {
                rtkStatusTextView.setText(R.string.uxsdk_rtk_state_connect_not_healthy)
                rtkStatusTextView.setTextColor(getRTKConnectionStatusLabelTextColor(RTKSatelliteStatusWidgetModel.RTKBaseStationState.CONNECTED_NOT_IN_USE))
            }
            RTKSatelliteStatusWidgetModel.RTKBaseStationState.DISCONNECTED -> {
                rtkStatusTextView.setText(R.string.uxsdk_rtk_state_disconnect)
                rtkStatusTextView.textColor =
                    getRTKConnectionStatusLabelTextColor(RTKSatelliteStatusWidgetModel.RTKBaseStationState.DISCONNECTED)
            }
        }

    }

    private fun updateNetworkServiceStatus(networkServiceState: RTKSatelliteStatusWidgetModel.RTKNetworkServiceState) {
        //去除RTKNetworkServiceState默认值带来的的影响
        LogUtils.i(TAG, "updateNetworkServiceStatus:$networkServiceState")
        if (networkServiceState.isNetworkServiceOpen == false) {
            return
        }
        val rtkStatusStr: String
        var rtkStatusColor: Int =
            getRTKConnectionStatusLabelTextColor(RTKSatelliteStatusWidgetModel.RTKBaseStationState.DISCONNECTED)

        when (networkServiceState.state) {
            RTKServiceState.RTCM_CONNECTED,
            RTKServiceState.RTCM_NORMAL,
            RTKServiceState.RTCM_USER_HAS_ACTIVATE,
            RTKServiceState.RTCM_USER_ACCOUNT_EXPIRES_SOON,
            RTKServiceState.RTCM_USE_DEFAULT_MOUNT_POINT,
            RTKServiceState.TRANSMITTING,
            ->
                if (networkServiceState.isRTKBeingUsed == true) {
                    rtkStatusColor =
                        getRTKConnectionStatusLabelTextColor(RTKSatelliteStatusWidgetModel.RTKBaseStationState.CONNECTED_IN_USE)
                    rtkStatusStr = getString(R.string.uxsdk_rtk_state_connect)
                } else {
                    rtkStatusColor =
                        getRTKConnectionStatusLabelTextColor(RTKSatelliteStatusWidgetModel.RTKBaseStationState.CONNECTED_NOT_IN_USE)
                    rtkStatusStr = getString(R.string.uxsdk_rtk_state_connect_not_healthy)
                }

            RTKServiceState.RTCM_AUTH_FAILED -> rtkStatusStr = getString(R.string.uxsdk_rtk_state_auth_failed)
            RTKServiceState.RTCM_USER_NOT_BOUNDED -> rtkStatusStr = getString(R.string.uxsdk_rtk_state_not_bind)
            RTKServiceState.SERVICE_SUSPENSION -> rtkStatusStr = getString(R.string.uxsdk_rtk_state_pause)
            RTKServiceState.RTCM_USER_NOT_ACTIVATED -> rtkStatusStr = getString(R.string.uxsdk_rtk_state_not_active)
            RTKServiceState.RTCM_ILLEGAL_UTC_TIME,
            RTKServiceState.ACCOUNT_EXPIRED,
            -> rtkStatusStr = getString(R.string.uxsdk_rtk_state_account_expire)
            RTKServiceState.NETWORK_NOT_REACHABLE -> rtkStatusStr = getString(R.string.uxsdk_rtk_state_network_err)
            RTKServiceState.LOGIN_FAILURE -> rtkStatusStr = getString(R.string.uxsdk_rtk_state_auth_failed)
            RTKServiceState.RTCM_SET_COORDINATE_FAILURE -> rtkStatusStr =
                getString(R.string.uxsdk_rtk_state_coordinate_fialed)
            RTKServiceState.RTCM_CONNECTING,
            RTKServiceState.READY,
            RTKServiceState.RTK_START_PROCESSING,
            -> rtkStatusStr = getString(R.string.uxsdk_rtk_state_connecting)
            RTKServiceState.ACCOUNT_ERROR -> rtkStatusStr = getString(R.string.uxsdk_rtk_nrtk_account_error)
            RTKServiceState.CONNECTING -> rtkStatusStr = getString(R.string.uxsdk_rtk_nrtk_connecting)
            RTKServiceState.INVALID_REQUEST -> rtkStatusStr = getString(R.string.uxsdk_rtk_nrtk_invalid_request)
            RTKServiceState.SERVER_NOT_REACHABLE -> rtkStatusStr =
                getString(R.string.uxsdk_rtk_nrtk_server_not_reachable)
            else -> rtkStatusStr = getString(R.string.uxsdk_rtk_state_disconnect)
        }
        rtkStatusTextView.text = rtkStatusStr
        rtkStatusTextView.setTextColor(rtkStatusColor)
    }

    private fun updateUIForIsRTKConnected(isRTKConnected: Boolean) {
        if (!isRTKConnected) {
            initItemValues()
        }
    }

    private fun updateRTKLocationInfo(rtkLocationInfo: RTKLocationInfo?) {
        //更新飞行器位置信息
        val aircraftLocation = rtkLocationInfo?.rtkLocation?.mobileStationLocation
        aircraftLocation?.run {
            aircraftLatitudeTextView.text = resources.getString(
                R.string.uxsdk_rtk_panel_coordinate_value,
                latitude
            )
            aircraftLongitudeTextView.text = resources.getString(
                R.string.uxsdk_rtk_panel_coordinate_value,
                longitude
            )
            aircraftAltitudeTextView.text = resources.getString(
                R.string.uxsdk_rtk_panel_altitude_value,
                altitude
            )
        }


        //更新基站位置信息
        val baseStationLocation = rtkLocationInfo?.rtkLocation?.baseStationLocation
        baseStationLocation?.run {
            baseStationLatitudeTextView.text = resources.getString(
                R.string.uxsdk_rtk_panel_coordinate_value,
                latitude
            )
            baseStationLongitudeTextView.text = resources.getString(
                R.string.uxsdk_rtk_panel_coordinate_value,
                longitude
            )
            baseStationAltitudeTextView.text = resources.getString(
                R.string.uxsdk_rtk_panel_altitude_value,
                altitude
            )
        }


        //更新航向角
        val rtkHeading = rtkLocationInfo?.rtkHeading
        var heading: Double? = rtkHeading?.heading
        heading?.let {
            heading += 90.0
            if (heading >= 360) {
                heading -= 360.0
            }
            courseAngleTextView.text = String.format(Locale.US, "%.1f", heading)
        }

        //更新定向信息
        if (rtkHeading?.directEnable == true) {
            orientationTextView.text = getString(R.string.uxsdk_rtk_solution_fixed)
        } else {
            orientationTextView.setText(R.string.uxsdk_string_default_value)
        }

        //更新定位信息
         positioningSolution = rtkLocationInfo?.rtkLocation?.positioningSolution
        if (positioningSolution == RTKPositioningSolution.NONE || positioningSolution == RTKPositioningSolution.UNKNOWN) {
            positioningTextView.setText(R.string.uxsdk_string_default_value)
        } else {
            positioningTextView.text = RTKUtil.getRTKStatusName(this, positioningSolution)
        }

        if (positioningSolution == RTKPositioningSolution.FIXED_POINT ) {
            rtkStatusTextView.setText(R.string.uxsdk_rtk_state_connect)
            rtkStatusTextView.setTextColor(getRTKConnectionStatusLabelTextColor(RTKSatelliteStatusWidgetModel.RTKBaseStationState.CONNECTED_IN_USE))
        }

    }

    //更新标准差
    private fun updateStandardDeviation(standardDeviation: RTKSatelliteStatusWidgetModel.StandardDeviation) {
        val resourceString = if (standardDeviation.unitType == UnitConversionUtil.UnitType.IMPERIAL) {
            R.string.uxsdk_value_feet
        } else {
            R.string.uxsdk_value_meters
        }
        val standardDeviationStr =
            (resources.getString(resourceString, String.format(Locale.US, "%s", standardDeviation.latitude)) + "\n"
                    + resources.getString(
                resourceString,
                String.format(Locale.US, "%s", standardDeviation.longitude)
            ) + "\n"
                    + resources.getString(resourceString, String.format(Locale.US, "%s", standardDeviation.altitude)))
        standardDeviationTextView.text = standardDeviationStr
    }

    private fun updateRTKSystemState(rtkSystemState: RTKSystemState?) {
        LogUtils.i(TAG, "rtkSystemState=$rtkSystemState")
        //更新天线1的卫星数
        val mobileStationReceiver1Info = rtkSystemState?.satelliteInfo?.mobileStationReceiver1Info
        mobileStationReceiver1Info?.let {
            for (receiverInfo in mobileStationReceiver1Info) {
                val type = receiverInfo.type
                val count = receiverInfo.count.toString()
                when (type) {
                    GNSSType.BEIDOU -> {
                        beiDouAntenna1TextView.text = count
                    }
                    GNSSType.GALILEO -> {
                        galileoAntenna1TextView.text = count
                    }
                    GNSSType.GLONASS -> {
                        glonassAntenna1TextView.text = count
                    }
                    GNSSType.GPS -> {
                        gpsAntenna1TextView.text = count
                    }
                    else -> {
                        //do something
                    }
                }
            }
        }

        //更新天线2的卫星数
        val mobileStationReceiver2Info = rtkSystemState?.satelliteInfo?.mobileStationReceiver2Info
        mobileStationReceiver2Info?.let {
            for (receiverInfo in mobileStationReceiver2Info) {
                val type = receiverInfo.type
                val count = receiverInfo.count.toString()
                when (type) {
                    GNSSType.BEIDOU -> {
                        beiDouAntenna2TextView.text = count
                    }
                    GNSSType.GALILEO -> {
                        galileoAntenna2TextView.text = count
                    }
                    GNSSType.GLONASS -> {
                        glonassAntenna2TextView.text = count
                    }
                    GNSSType.GPS -> {
                        gpsAntenna2TextView.text = count
                    }
                    else -> {
                        //do something
                    }
                }
            }
        }

        //更新基站的卫星数
        val baseStationReceiverInfo = rtkSystemState?.satelliteInfo?.baseStationReceiverInfo
        baseStationReceiverInfo?.let {
            for (receiverInfo in baseStationReceiverInfo) {
                val type = receiverInfo.type
                val count = receiverInfo.count.toString()
                when (type) {
                    GNSSType.BEIDOU -> {
                        beiDouBaseStationTextView.text = count
                    }
                    GNSSType.GALILEO -> {
                        galileoBaseStationTextView.text = count
                    }
                    GNSSType.GLONASS -> {
                        glonassBaseStationTextView.text = count
                    }
                    GNSSType.GPS -> {
                        gpsBaseStationTextView.text = count
                    }
                    else -> {
                        //do something
                    }
                }
            }
        }

        updateBaseStationUI(rtkSystemState?.rtkReferenceStationSource)
    }

    private fun updateBaseStationUI(stationSource: RTKReferenceStationSource?) {
        stationSource?.let {
            val name = RTKUtil.getRTKTypeName(this, stationSource)
            baseStationCoordinatesTitleTextView.text = name
            rtkStatusTitleTextView.text = resources.getString(R.string.uxsdk_rtk_status_desc, name)
        }
        if (stationSource == RTKReferenceStationSource.BASE_STATION) {
            baseStationConnectImageView.show()
        } else {
            baseStationConnectImageView.hide()
        }
    }


    /**
     * Class defines the widget state updates
     */
    sealed class ModelState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : ModelState()

    }

    interface RTKStationListener {
        fun showConnectView()
    }

    private var rtkStationListener: RTKStationListener? = null
    fun setRTKConnectListener(rtkStationListener: RTKStationListener) {
        this.rtkStationListener = rtkStationListener
    }
    //endregion
}