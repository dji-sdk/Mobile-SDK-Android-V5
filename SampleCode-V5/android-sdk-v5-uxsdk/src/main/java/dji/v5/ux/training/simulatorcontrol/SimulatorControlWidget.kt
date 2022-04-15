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

package dji.v5.ux.training.simulatorcontrol

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.text.InputFilter
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.*
import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.Group
import androidx.core.content.res.use
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.sdk.keyvalue.value.flightcontroller.SimulatorInitializationSettings
import dji.sdk.keyvalue.value.flightcontroller.SimulatorState
import dji.v5.utils.common.LogUtils
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.processors.PublishProcessor
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.UXSDKError
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.communication.OnStateChangeCallback
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.ui.SeekBarView
import dji.v5.ux.core.util.DisplayUtil
import dji.v5.ux.core.util.EditTextNumberInputFilter
import dji.v5.ux.core.util.RxUtil
import dji.v5.ux.R
import dji.v5.ux.training.util.SimulatorPresetUtils
import dji.v5.ux.training.simulatorcontrol.SimulatorControlWidget.ModelState
import dji.v5.ux.training.simulatorcontrol.SimulatorControlWidget.ModelState.*
import dji.v5.ux.training.simulatorcontrol.SimulatorControlWidget.UIState.*
import dji.v5.ux.training.simulatorcontrol.preset.OnLoadPresetListener
import dji.v5.ux.training.simulatorcontrol.preset.PresetListDialog
import dji.v5.ux.training.simulatorcontrol.preset.SavePresetDialog
import dji.v5.ux.training.simulatorcontrol.preset.SimulatorPresetData
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Simulator Control Widget
 *
 * Widget can be used for quick simulation of the aircraft flight without flying it.
 * Aircraft should be connected to run the simulation.
 * User can enter the location coordinates, satellite count and
 * data frequency. The user has the option to save presets to reuse simulation
 * configuration.
 */
open class SimulatorControlWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayoutWidget<ModelState>(
        context,
        attrs,
        defStyleAttr),
        View.OnClickListener, OnStateChangeCallback<Any?>, OnLoadPresetListener {

    //region Fields
    private val widgetModel by lazy {
        SimulatorControlWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }
    private val latitudeEditText: EditText = findViewById(R.id.edit_text_simulator_lat)
    private val longitudeEditText: EditText = findViewById(R.id.edit_text_simulator_lng)
    private val frequencySeekBar: SeekBarView = findViewById(R.id.seek_bar_frequency)
    private val satelliteCountSeekBar: SeekBarView = findViewById(R.id.seek_bar_satellite_count)
    private val simulatorTitleTextView: TextView = findViewById(R.id.textview_simulator_title)
    private val simulatorSwitch: Switch = findViewById(R.id.switch_simulator)
    private val latitudeTextView: TextView = findViewById(R.id.textview_simulator_latitude_value)
    private val longitudeTextView: TextView = findViewById(R.id.textview_simulator_longitude_value)
    private val satelliteTextView: TextView = findViewById(R.id.textview_simulator_satellite_value)
    private val worldXTextView: TextView = findViewById(R.id.textview_simulator_world_x_value)
    private val worldYTextView: TextView = findViewById(R.id.textview_simulator_world_y_value)
    private val worldZTextView: TextView = findViewById(R.id.textview_simulator_world_z_value)
    private val motorsStartedTextView: TextView = findViewById(R.id.textview_simulator_motors_value)
    private val aircraftFlyingTextView: TextView = findViewById(R.id.textview_simulator_aircraft_flying_value)
    private val pitchTextView: TextView = findViewById(R.id.textview_simulator_aircraft_pitch_value)
    private val yawTextView: TextView = findViewById(R.id.textview_simulator_aircraft_yaw_value)
    private val rollTextView: TextView = findViewById(R.id.textview_simulator_aircraft_roll_value)
    private val frequencyTextView: TextView = findViewById(R.id.textview_simulator_frequency_value)
    private val loadPresetTextView: TextView = findViewById(R.id.textview_load_preset)
    private val savePresetTextView: TextView = findViewById(R.id.textview_save_preset)
    private val latitudeLabelTextView: TextView = findViewById(R.id.textview_simulator_latitude_label)
    private val longitudeLabelTextView: TextView = findViewById(R.id.textview_simulator_longitude_label)
    private val satelliteLabelTextView: TextView = findViewById(R.id.textview_simulator_satellite_label)
    private val worldXLabelTextView: TextView = findViewById(R.id.textview_simulator_world_x_label)
    private val worldYLabelTextView: TextView = findViewById(R.id.textview_simulator_world_y_label)
    private val worldZLabelTextView: TextView = findViewById(R.id.textview_simulator_world_z_label)
    private val motorsStartedLabelTextView: TextView = findViewById(R.id.textview_simulator_motors_label)
    private val aircraftFlyingLabelTextView: TextView = findViewById(R.id.textview_simulator_aircraft_flying_label)
    private val pitchLabelTextView: TextView = findViewById(R.id.textview_simulator_pitch_label)
    private val yawLabelTextView: TextView = findViewById(R.id.textview_simulator_yaw_label)
    private val rollLabelTextView: TextView = findViewById(R.id.textview_simulator_roll_label)
    private val frequencyLabelTextView: TextView = findViewById(R.id.textview_simulator_frequency_label)
    private val windXLabelTextView: TextView = findViewById(R.id.textview_wind_speed_x_label)
    private val windYLabelTextView: TextView = findViewById(R.id.textview_wind_speed_y_label)
    private val windZLabelTextView: TextView = findViewById(R.id.textview_wind_speed_z_label)
    private val windSpeedXSeekBar: SeekBarView = findViewById(R.id.seek_bar_wind_speed_x)
    private val windSpeedYSeekBar: SeekBarView = findViewById(R.id.seek_bar_wind_speed_y)
    private val windSpeedZSeekBar: SeekBarView = findViewById(R.id.seek_bar_wind_speed_z)
    private val positionSectionHeaderTextView: TextView = findViewById(R.id.textview_location_section_header)
    private val windSectionHeaderTextView: TextView = findViewById(R.id.textview_wind_section_header)
    private val attitudeSectionHeaderTextView: TextView = findViewById(R.id.textview_attitude_section_header)
    private val aircraftSectionHeaderTextView: TextView = findViewById(R.id.textview_status_section_header)
    private val attitudeGroup: Group = findViewById(R.id.constraint_group_attitude)
    private val aircraftStatusGroup: Group = findViewById(R.id.constraint_group_aircraft_state)
    private val realWorldPositionGroup: Group = findViewById(R.id.constraint_group_wind)
    private val windSimulationGroup: Group = findViewById(R.id.constraint_group_real_world)
    private val buttonGroup: Group = findViewById(R.id.constraint_group_buttons)
    private lateinit var df: DecimalFormat
    private var shouldReactToCheckChange = false
    private val seekBarChangeListener = object : SeekBarView.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBarView: SeekBarView, progress: Int, isFromUI: Boolean) {
            when (seekBarView) {
                satelliteCountSeekBar -> {
                    satelliteCountSeekBar.text = satelliteCountSeekBar.progress.toString()
                }
                frequencySeekBar -> {
                    frequencySeekBar.text = max(MIN_FREQUENCY, frequencySeekBar.progress).toString()
                }
                windSpeedXSeekBar -> {
                    seekBarView.text = normalizeWindValue(seekBarView.progress).toString()
                    if (isFromUI) {
                        setWindSpeedUI(WIND_DIRECTION_X, seekBarView.progress > 0)
                    }

                }
                windSpeedYSeekBar -> {
                    seekBarView.text = normalizeWindValue(seekBarView.progress).toString()
                    if (isFromUI) {
                        setWindSpeedUI(WIND_DIRECTION_Y, seekBarView.progress > 0)
                    }
                }
                windSpeedZSeekBar -> {
                    seekBarView.text = normalizeWindValue(seekBarView.progress).toString()
                    if (isFromUI) {
                        setWindSpeedUI(WIND_DIRECTION_Z, seekBarView.progress > 0)
                    }
                }
            }
        }

        override fun onMinusClicked(seekBarView: SeekBarView) {
            //No implementation
        }

        override fun onPlusClicked(seekBarView: SeekBarView) {
            //No implementation
        }

        override fun onStartTrackingTouch(seekBarView: SeekBarView, progress: Int) {
            //No implementation
        }

        override fun onStopTrackingTouch(seekBarView: SeekBarView, progress: Int) {
            //No implementation
        }

    }

    private val uiUpdateStateProcessor: PublishProcessor<UIState> = PublishProcessor.create()

    /**
     * The drawable resource for the simulator active icon
     */
    var simulatorActiveIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_simulator_active)
        set(value) {
            field = value
            checkAndUpdateState()
        }

    /**
     * The drawable resource for the simulator inactive icon
     */
    var simulatorInactiveIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_simulator)
        set(value) {
            field = value
            checkAndUpdateState()
        }

    /**
     * Visibility of the world position section
     */
    var isWorldPositionSectionVisible: Boolean = true
        set(value) {
            field = value
            checkAndUpdateState()
        }

    /**
     * Visibility of the attitude section
     */
    var isAttitudeSectionVisible: Boolean = true
        set(value) {
            field = value
            checkAndUpdateState()
        }

    /**
     * Visibility of the aircraft status section
     */
    var isAircraftStatusSectionVisible: Boolean = true
        set(value) {
            field = value
            checkAndUpdateState()
        }

    /**
     * Visibility of the wind section
     */
    var isWindSectionVisible: Boolean = true
        set(value) {
            field = value
            checkAndUpdateState()
        }

    /**
     * Get current background of title text
     *
     * @return Drawable representing the background of title
     */
    var titleBackground: Drawable?
        get() = simulatorTitleTextView.background
        set(value) {
            simulatorTitleTextView.background = value
        }

    /**
     * Color state list of widget title
     */
    var titleTextColors: ColorStateList?
        get() = simulatorTitleTextView.textColorStateList
        set(value) {
            simulatorTitleTextView.textColorStateList = value
        }

    /**
     * Text color of widget title
     */
    var titleTextColor: Int
        get() = simulatorTitleTextView.textColor
        set(value) {
            simulatorTitleTextView.textColor = value
        }

    /**
     * Text size of the title
     */
    var titleTextSize: Float
        get() = simulatorTitleTextView.textSize
        set(textSize) {
            simulatorTitleTextView.textSize = textSize
        }

    /**
     * Color state list of the value fields
     */
    var valueTextColors: ColorStateList?
        get() = latitudeTextView.textColorStateList
        set(value) {
            latitudeTextView.textColorStateList = value
            longitudeTextView.textColorStateList = value
            satelliteTextView.textColorStateList = value
            worldXTextView.textColorStateList = value
            worldYTextView.textColorStateList = value
            worldZTextView.textColorStateList = value
            motorsStartedTextView.textColorStateList = value
            aircraftFlyingTextView.textColorStateList = value
            pitchTextView.textColorStateList = value
            yawTextView.textColorStateList = value
            rollTextView.textColorStateList = value
            frequencyTextView.textColorStateList = value
        }

    /**
     * Text color of the value fields
     */
    var valueTextColor: Int
        get() = latitudeTextView.textColor
        set(value) {
            latitudeTextView.textColor = value
            longitudeTextView.textColor = value
            satelliteTextView.textColor = value
            worldXTextView.textColor = value
            worldYTextView.textColor = value
            worldZTextView.textColor = value
            motorsStartedTextView.textColor = value
            aircraftFlyingTextView.textColor = value
            pitchTextView.textColor = value
            yawTextView.textColor = value
            rollTextView.textColor = value
            frequencyTextView.textColor = value
        }

    /**
     * Text size of the value fields
     */
    var valueTextSize: Float
        get() = latitudeTextView.textSize
        set(textSize) {
            latitudeTextView.textSize = textSize
            longitudeTextView.textSize = textSize
            satelliteTextView.textSize = textSize
            worldXTextView.textSize = textSize
            worldYTextView.textSize = textSize
            worldZTextView.textSize = textSize
            motorsStartedTextView.textSize = textSize
            aircraftFlyingTextView.textSize = textSize
            pitchTextView.textSize = textSize
            yawTextView.textSize = textSize
            rollTextView.textSize = textSize
            frequencyTextView.textSize = textSize
        }

    /**
     * Background of value text
     */
    var valueBackground: Drawable?
        get() = latitudeTextView.background
        set(value) {
            latitudeTextView.background = value
            longitudeTextView.background = value
            satelliteTextView.background = value
            worldXTextView.background = value
            worldYTextView.background = value
            worldZTextView.background = value
            motorsStartedTextView.background = value
            aircraftFlyingTextView.background = value
            pitchTextView.background = value
            yawTextView.background = value
            rollTextView.background = value
            frequencyTextView.background = value
        }

    /**
     * Text size of labels
     */
    var labelTextSize: Float
        get() = latitudeLabelTextView.textSize
        set(textSize) {
            latitudeLabelTextView.textSize = textSize
            longitudeLabelTextView.textSize = textSize
            satelliteLabelTextView.textSize = textSize
            worldXLabelTextView.textSize = textSize
            worldYLabelTextView.textSize = textSize
            worldZLabelTextView.textSize = textSize
            motorsStartedLabelTextView.textSize = textSize
            aircraftFlyingLabelTextView.textSize = textSize
            pitchLabelTextView.textSize = textSize
            yawLabelTextView.textSize = textSize
            rollLabelTextView.textSize = textSize
            frequencyLabelTextView.textSize = textSize
            windXLabelTextView.textSize = textSize
            windYLabelTextView.textSize = textSize
            windZLabelTextView.textSize = textSize
        }

    /**
     * Color state list of widget labels
     */
    var labelTextColors: ColorStateList?
        get() = latitudeLabelTextView.textColorStateList
        set(value) {
            latitudeLabelTextView.textColorStateList = value
            longitudeLabelTextView.textColorStateList = value
            satelliteLabelTextView.textColorStateList = value
            worldXLabelTextView.textColorStateList = value
            worldYLabelTextView.textColorStateList = value
            worldZLabelTextView.textColorStateList = value
            motorsStartedLabelTextView.textColorStateList = value
            aircraftFlyingLabelTextView.textColorStateList = value
            pitchLabelTextView.textColorStateList = value
            yawLabelTextView.textColorStateList = value
            rollLabelTextView.textColorStateList = value
            frequencyLabelTextView.textColorStateList = value
            windXLabelTextView.textColorStateList = value
            windYLabelTextView.textColorStateList = value
            windZLabelTextView.textColorStateList = value
        }

    /**
     * Label text color
     */
    var labelTextColor: Int
        get() = latitudeLabelTextView.textColor
        set(value) {
            latitudeLabelTextView.textColor = value
            longitudeLabelTextView.textColor = value
            satelliteLabelTextView.textColor = value
            worldXLabelTextView.textColor = value
            worldYLabelTextView.textColor = value
            worldZLabelTextView.textColor = value
            motorsStartedLabelTextView.textColor = value
            aircraftFlyingLabelTextView.textColor = value
            pitchLabelTextView.textColor = value
            yawLabelTextView.textColor = value
            rollLabelTextView.textColor = value
            frequencyLabelTextView.textColor = value
            windXLabelTextView.textColor = value
            windYLabelTextView.textColor = value
            windZLabelTextView.textColor = value
        }

    /**
     * Background of label text
     */
    var labelBackground: Drawable?
        get() = latitudeLabelTextView.background
        set(value) {
            latitudeLabelTextView.background = value
            longitudeLabelTextView.background = value
            satelliteLabelTextView.background = value
            worldXLabelTextView.background = value
            worldYLabelTextView.background = value
            worldZLabelTextView.background = value
            motorsStartedLabelTextView.background = value
            aircraftFlyingLabelTextView.background = value
            pitchLabelTextView.background = value
            yawLabelTextView.background = value
            rollLabelTextView.background = value
            frequencyLabelTextView.background = value
            windXLabelTextView.background = value
            windYLabelTextView.background = value
            windZLabelTextView.background = value
        }

    /**
     * Text size of the widget input text fields
     */
    var inputTextSize: Float
        get() = latitudeEditText.textSize
        set(textSize) {
            latitudeEditText.textSize = textSize
            longitudeEditText.textSize = textSize
        }

    /**
     * Color state list of input text fields
     */
    var inputTextColors: ColorStateList?
        get() = latitudeEditText.textColorStateList
        set(value) {
            latitudeEditText.textColorStateList = value
            longitudeEditText.textColorStateList = value
        }

    /**
     * Text color of input text fields
     */
    var inputTextColor: Int
        get() = latitudeEditText.textColor
        set(value) {
            latitudeEditText.textColor = value
            longitudeEditText.textColor = value
        }

    /**
     * Background of input text fields
     */
    var inputBackground: Drawable?
        get() = latitudeEditText.background
        set(value) {
            latitudeEditText.background = value
            longitudeEditText.background = value
        }

    /**
     * Set text size of the widget button
     */
    var buttonTextSize: Float
        get() = savePresetTextView.textSize
        set(textSize) {
            savePresetTextView.textSize = textSize
            loadPresetTextView.textSize = textSize
        }

    /**
     * ColorStateList for buttons text color
     */
    var buttonTextColors: ColorStateList?
        get() = savePresetTextView.textColorStateList
        set(value) {
            savePresetTextView.textColorStateList = value
            loadPresetTextView.textColorStateList = value
        }

    /**
     * Text color of buttons
     */
    var buttonTextColor: Int
        get() = savePresetTextView.textColor
        set(value) {
            savePresetTextView.textColor = value
            loadPresetTextView.textColor = value
        }

    /**
     * Background of buttons
     */
    var buttonBackground: Drawable?
        get() = savePresetTextView.background
        set(value) {
            savePresetTextView.background = value
            loadPresetTextView.background = value
        }

    /**
     * Text size of the widget header text fields
     */
    var headerTextSize: Float
        get() = positionSectionHeaderTextView.textSize
        set(textSize) {
            positionSectionHeaderTextView.textSize = textSize
            aircraftSectionHeaderTextView.textSize = textSize
            attitudeSectionHeaderTextView.textSize = textSize
            windSectionHeaderTextView.textSize = textSize
        }

    /**
     * Current text color state list of header fields
     */
    var headerTextColors: ColorStateList?
        get() = positionSectionHeaderTextView.textColorStateList
        set(value) {
            positionSectionHeaderTextView.textColorStateList = value
            aircraftSectionHeaderTextView.textColorStateList = value
            attitudeSectionHeaderTextView.textColorStateList = value
            windSectionHeaderTextView.textColorStateList = value
        }

    /**
     * Background for section headers
     */
    var headerBackground: Drawable?
        get() = positionSectionHeaderTextView.background
        set(value) {
            positionSectionHeaderTextView.background = value
            aircraftSectionHeaderTextView.background = value
            attitudeSectionHeaderTextView.background = value
            windSectionHeaderTextView.background = value
        }

    /**
     *  Text color of header fields
     */
    var headerTextColor: Int
        get() = positionSectionHeaderTextView.textColor
        set(value) {
            positionSectionHeaderTextView.textColor = value
            aircraftSectionHeaderTextView.textColor = value
            attitudeSectionHeaderTextView.textColor = value
            windSectionHeaderTextView.textColor = value
        }

    /**
     * Text color of the seek bar value
     */
    var seekBarTextColor: Int
        get() = frequencySeekBar.valueTextColor
        set(value) {
            frequencySeekBar.valueTextColor = value
            satelliteCountSeekBar.valueTextColor = value
            windSpeedXSeekBar.valueTextColor = value
            windSpeedYSeekBar.valueTextColor = value
            windSpeedZSeekBar.valueTextColor = value
        }

    /**
     * Drawable for the seek bar track
     */
    var seekBarTrackIconBackground: Drawable?
        get() = frequencySeekBar.trackIconBackground
        set(value) {
            frequencySeekBar.trackIconBackground = value
            satelliteCountSeekBar.trackIconBackground = value
            windSpeedXSeekBar.trackIconBackground = value
            windSpeedYSeekBar.trackIconBackground = value
            windSpeedZSeekBar.trackIconBackground = value
        }

    /**
     * Drawable for the seek bar thumb
     */
    var seekBarThumbIcon: Drawable?
        get() = frequencySeekBar.thumbIcon
        set(value) {
            frequencySeekBar.thumbIcon = value
            satelliteCountSeekBar.thumbIcon = value
            windSpeedXSeekBar.thumbIcon = value
            windSpeedYSeekBar.thumbIcon = value
            windSpeedZSeekBar.thumbIcon = value
        }

    //endregion

    //region Lifecycle
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_widget_simulator_control, this)
    }

    init {
        setBackgroundResource(R.drawable.uxsdk_background_black_rectangle)
        initViewElements()
        attrs?.let { initAttributes(context, it) }
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { onProductChanged(it) })
        addReaction(widgetModel.satelliteCount
                .observeOn(SchedulerProvider.ui())
                .subscribe { this.updateSatelliteCount(it) })
//        addReaction(widgetModel.simulatorWindData
//                .debounce(500, TimeUnit.MILLISECONDS)
//                .observeOn(SchedulerProvider.ui())
//                .subscribe { this.updateWindValues(it) })
        addReaction(widgetModel.simulatorState
                .observeOn(SchedulerProvider.ui())
            .subscribe({ this.updateWidgetValues(it) }, { e -> LogUtils.e(logTag, e.message) }))
        addReaction(widgetModel.isSimulatorActive
                .observeOn(SchedulerProvider.ui())
                .subscribe { this.updateUI(it) })
    }

    private fun onProductChanged(it: Boolean) {
        widgetStateDataProcessor.onNext(ProductConnected(it))
        isEnabled = it
    }

    override fun getIdealDimensionRatioString(): String {
        return getString(R.string.uxsdk_widget_simulator_control_ratio)
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

    override fun onLoadPreset(simulatorPresetData: SimulatorPresetData?) {
        if (simulatorPresetData != null) {
            latitudeEditText.setText(simulatorPresetData.latitude.toString())
            longitudeEditText.setText(simulatorPresetData.longitude.toString())
            satelliteCountSeekBar.progress = simulatorPresetData.satelliteCount
            frequencySeekBar.progress = simulatorPresetData.updateFrequency
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.textview_load_preset -> {
                uiUpdateStateProcessor.onNext(LoadPresetClicked)
                showPresetListDialog()
            }
            R.id.textview_save_preset -> {
                uiUpdateStateProcessor.onNext(SavePresetClicked)
                showSavePresetDialog()
            }
        }
    }

    override fun onStateChange(state: Any?) {
        toggleVisibility()
    }

    //endregion

    //region private methods
    private fun toggleVisibility() {
        visibility = if (visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
        uiUpdateStateProcessor.onNext(VisibilityUpdated(visibility == View.VISIBLE))
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.SimulatorControlWidget).use { typedArray ->
            typedArray.getDrawableAndUse(R.styleable.SimulatorControlWidget_uxsdk_simulatorActiveDrawable) {
                simulatorActiveIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.SimulatorControlWidget_uxsdk_simulatorInactiveDrawable) {
                simulatorInactiveIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.SimulatorControlWidget_uxsdk_buttonBackground) {
                buttonBackground = it
            }
            typedArray.getColorStateListAndUse(R.styleable.SimulatorControlWidget_uxsdk_buttonTextColor) {
                buttonTextColors = it
            }
            typedArray.getColorAndUse(R.styleable.SimulatorControlWidget_uxsdk_buttonTextColor) {
                buttonTextColor = it
            }
            typedArray.getDimensionAndUse(R.styleable.SimulatorControlWidget_uxsdk_buttonTextSize) {
                buttonTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.SimulatorControlWidget_uxsdk_buttonTextAppearance) {
                setButtonTextAppearance(it)
            }
            typedArray.getDrawableAndUse(R.styleable.SimulatorControlWidget_uxsdk_labelsBackground) {
                labelBackground = it
            }
            typedArray.getColorStateListAndUse(R.styleable.SimulatorControlWidget_uxsdk_labelsTextColor) {
                labelTextColors = it
            }
            typedArray.getColorAndUse(R.styleable.SimulatorControlWidget_uxsdk_labelsTextColor) {
                labelTextColor = it
            }
            typedArray.getDimensionAndUse(R.styleable.SimulatorControlWidget_uxsdk_labelsTextSize) {
                labelTextSize = it
            }
            typedArray.getResourceIdAndUse(R.styleable.SimulatorControlWidget_uxsdk_labelsTextAppearance) {
                setLabelTextAppearance(it)
            }
            typedArray.getDrawableAndUse(R.styleable.SimulatorControlWidget_uxsdk_inputBackground) {
                inputBackground = it
            }
            typedArray.getColorStateListAndUse(R.styleable.SimulatorControlWidget_uxsdk_inputTextColor) {
                inputTextColors = it
            }
            typedArray.getColorAndUse(R.styleable.SimulatorControlWidget_uxsdk_inputTextColor) {
                inputTextColor = it
            }
            typedArray.getDimensionAndUse(R.styleable.SimulatorControlWidget_uxsdk_inputTextSize) {
                inputTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.SimulatorControlWidget_uxsdk_inputTextAppearance) {
                setInputTextAppearance(it)
            }
            typedArray.getDrawableAndUse(R.styleable.SimulatorControlWidget_uxsdk_valueBackground) {
                valueBackground = it
            }
            typedArray.getColorStateListAndUse(R.styleable.SimulatorControlWidget_uxsdk_valueTextColor) {
                valueTextColors = it
            }

            typedArray.getColorAndUse(R.styleable.SimulatorControlWidget_uxsdk_valueTextColor) {
                valueTextColor = it
            }
            typedArray.getDimensionAndUse(R.styleable.SimulatorControlWidget_uxsdk_valueTextSize) {
                valueTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.SimulatorControlWidget_uxsdk_valueTextAppearance) {
                setValueTextAppearance(it)
            }
            typedArray.getDrawableAndUse(R.styleable.SimulatorControlWidget_uxsdk_headerBackground) {
                headerBackground = it
            }
            typedArray.getColorStateListAndUse(R.styleable.SimulatorControlWidget_uxsdk_headerTextColor) {
                headerTextColors = it
            }
            typedArray.getColorAndUse(R.styleable.SimulatorControlWidget_uxsdk_headerTextColor) {
                headerTextColor = it
            }
            typedArray.getDimensionAndUse(R.styleable.SimulatorControlWidget_uxsdk_headerTextSize) {
                headerTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.SimulatorControlWidget_uxsdk_headerTextAppearance) {
                setHeaderTextAppearance(it)
            }
            typedArray.getDrawableAndUse(R.styleable.SimulatorControlWidget_uxsdk_widgetTitleBackground) {
                titleBackground = it
            }
            typedArray.getColorStateListAndUse(R.styleable.SimulatorControlWidget_uxsdk_widgetTitleTextColor) {
                titleTextColors = it
            }
            typedArray.getColorAndUse(R.styleable.SimulatorControlWidget_uxsdk_widgetTitleTextColor) {
                titleTextColor = it
            }
            typedArray.getDimensionAndUse(R.styleable.SimulatorControlWidget_uxsdk_widgetTitleTextSize) {
                titleTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getResourceIdAndUse(R.styleable.SimulatorControlWidget_uxsdk_widgetTitleTextAppearance) {
                setTitleTextAppearance(it)
            }
        }
    }

    private fun setWindSpeedUI(windDirection: Int, isPositive: Boolean) {
        uiUpdateStateProcessor.onNext(SimulatorWindChangeClicked(windDirection, isPositive))
//        addDisposable(widgetModel.setSimulatorWindData(SimulatorWindData.Builder()
//                .windSpeedX(normalizeWindValue(windSpeedXSeekBar.progress))
//                .windSpeedY(normalizeWindValue(windSpeedYSeekBar.progress))
//                .windSpeedZ(normalizeWindValue(windSpeedZSeekBar.progress))
//                .build())
//                .subscribeOn(SchedulerProvider.io())
//                .observeOn(SchedulerProvider.ui())
//                .subscribe({}) { error: Throwable ->
//                    if (error is UXSDKError) {
//                        checkAndUpdateWind()
//                    }
//                })
    }

    private fun checkAndUpdateWind() {
//        if (!isInEditMode) {
//            addDisposable(widgetModel.simulatorWindData
//                    .lastOrError()
//                    .observeOn(SchedulerProvider.ui())
//                    .subscribe(Consumer { simulatorWindData -> updateWindValues(simulatorWindData) },
//                            RxUtil.logErrorConsumer(TAG, "Update wind")))
//        }
    }

    private fun updateSatelliteCount(satelliteCount: Int) {
        satelliteTextView.text = satelliteCount.toString()
    }

    private fun updateUI(isActive: Boolean) {
        widgetStateDataProcessor.onNext(SimulatorActiveUpdated(isActive))
        shouldReactToCheckChange = false
        if (isActive) {
            updateWidgetToStartedState()
        } else {
            updateWidgetToStoppedState()
        }
        shouldReactToCheckChange = true
    }

    private fun checkAndUpdateState() {
        if (!isInEditMode) {
            addDisposable(widgetModel.isSimulatorActive.firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(Consumer { this.updateUI(it) },
                            RxUtil.logErrorConsumer(TAG, "Update Icon ")))
        }
    }

    private fun initViewElements() {
        val otherSymbols = DecimalFormatSymbols(Locale.getDefault())
        otherSymbols.decimalSeparator = '.'
        otherSymbols.groupingSeparator = ','
        df = DecimalFormat("#00.000000", otherSymbols)
        latitudeEditText.filters = arrayOf<InputFilter>(EditTextNumberInputFilter("-90", "90"))
        longitudeEditText.filters = arrayOf<InputFilter>(EditTextNumberInputFilter("-180", "180"))
        loadPresetTextView.setOnClickListener(this)
        savePresetTextView.setOnClickListener(this)
        simulatorSwitch.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean -> handleSwitchChange(isChecked) }
        valueTextColor = getColor(R.color.uxsdk_blue)
        titleTextColor = getColor(R.color.uxsdk_white)
        headerTextColor = getColor(R.color.uxsdk_white)
        inputTextColor = getColor(R.color.uxsdk_black)
        buttonTextColor = getColor(R.color.uxsdk_white)

        satelliteCountSeekBar.valueTextSize = DisplayUtil.pxToDip(context, 35f)
        satelliteCountSeekBar.max = 20
        satelliteCountSeekBar.addOnSeekBarChangeListener(seekBarChangeListener)
        satelliteCountSeekBar.enable(true)
        satelliteCountSeekBar.progress = 1

        frequencySeekBar.valueTextSize = DisplayUtil.pxToDip(context, 35f)
        frequencySeekBar.max = 150
        frequencySeekBar.addOnSeekBarChangeListener(seekBarChangeListener)
        frequencySeekBar.enable(true)
        frequencySeekBar.progress = DEFAULT_FREQUENCY

        windSpeedXSeekBar.max = WIND_SEEK_BAR_MAX
        windSpeedXSeekBar.addOnSeekBarChangeListener(seekBarChangeListener)
        windSpeedXSeekBar.enable(true)
        windSpeedXSeekBar.progress = WIND_SEEK_BAR_MAX / 2

        windSpeedYSeekBar.max = WIND_SEEK_BAR_MAX
        windSpeedYSeekBar.addOnSeekBarChangeListener(seekBarChangeListener)
        windSpeedYSeekBar.enable(true)
        windSpeedYSeekBar.progress = WIND_SEEK_BAR_MAX / 2

        windSpeedZSeekBar.max = WIND_SEEK_BAR_MAX
        windSpeedZSeekBar.addOnSeekBarChangeListener(seekBarChangeListener)
        windSpeedZSeekBar.enable(true)
        windSpeedZSeekBar.progress = WIND_SEEK_BAR_MAX / 2

    }

    private fun normalizeWindValue(progress: Int): Int {
        return progress - SIMULATION_MAX_WIND_SPEED
    }

    private fun deNormalizeWindValue(progress: Int): Int {
        return progress + SIMULATION_MAX_WIND_SPEED
    }

    private fun handleSwitchChange(isChecked: Boolean) {
        uiUpdateStateProcessor.onNext(SimulatorSwitchTap(isChecked))
        if (shouldReactToCheckChange) {
            if (isChecked) {
                startSimulator()
            } else {
                stopSimulator()
            }
        }
    }

    private fun stopSimulator() {
        addDisposable(widgetModel.stopSimulator().subscribe({}))
    }

    private fun startSimulator() {
        val locationCoordinate2D = simulatedLocation
        if (locationCoordinate2D != null) {
            setSimulatorStatus(true)

            SimulatorPresetUtils.currentSimulatorFrequency = frequencySeekBar.progress
            SimulatorPresetUtils.currentSimulatorStartLat = latitudeEditText.text.toString()
            SimulatorPresetUtils.currentSimulatorStartLng = longitudeEditText.text.toString()
            val initializationData = SimulatorInitializationSettings(locationCoordinate2D.latitude,locationCoordinate2D.longitude,
                    /**max(MIN_FREQUENCY, frequencySeekBar.progress),**/
                    satelliteCountSeekBar.progress)
            addDisposable(widgetModel.startSimulator(initializationData)
                    .subscribeOn(SchedulerProvider.io())
                    .observeOn(SchedulerProvider.ui())
                    .subscribe({}) { error: Throwable ->
                        if (error is UXSDKError) {
                            setSimulatorStatus(false)
                        }
                    })
        } else {
            setSimulatorStatus(false)
            Toast.makeText(context,
                    getString(R.string.uxsdk_simulator_input_val_error),
                    Toast.LENGTH_SHORT).show()
        }
    }

    private fun setSimulatorStatus(simulatorActive: Boolean) {
        shouldReactToCheckChange = false
        if (simulatorActive) {
            updateWidgetToStartedState()
        } else {
            SimulatorPresetUtils.clearSimulatorFrequency()
            SimulatorPresetUtils.clearSimulatorStartLat()
            SimulatorPresetUtils.clearSimulatorStartLng()
            updateWidgetToStoppedState()
        }
        shouldReactToCheckChange = true
    }

    private fun updateWidgetToStartedState() {
        simulatorSwitch.isChecked = true
        latitudeTextView.text = when {
            latitudeEditText.text.toString().isNotEmpty() -> {
                df.format(latitudeEditText.text.toString().toDouble())
            }
            SimulatorPresetUtils.currentSimulatorStartLat.isNotEmpty() -> {
                SimulatorPresetUtils.currentSimulatorStartLat
            }
            else -> {
                getString(R.string.uxsdk_simulator_null_string)
            }
        }
        longitudeTextView.text = when {
            longitudeEditText.text.toString().isNotEmpty() -> {
                df.format(longitudeEditText.text.toString().toDouble())
            }
            SimulatorPresetUtils.currentSimulatorStartLng.isNotEmpty() -> {
                SimulatorPresetUtils.currentSimulatorStartLng
            }
            else -> {
                getString(R.string.uxsdk_simulator_null_string)
            }
        }
        val simulatorFrequency = SimulatorPresetUtils.currentSimulatorFrequency
        frequencyTextView.text = if (simulatorFrequency > 0) {
            simulatorFrequency.toString()
        } else {
            getString(R.string.uxsdk_simulator_null_string)
        }
        latitudeEditText.visibility = View.INVISIBLE
        longitudeEditText.visibility = View.INVISIBLE
        satelliteCountSeekBar.visibility = View.INVISIBLE
        frequencySeekBar.visibility = View.INVISIBLE
        frequencyTextView.visibility = View.VISIBLE
        latitudeTextView.visibility = View.VISIBLE
        longitudeTextView.visibility = View.VISIBLE
        satelliteTextView.visibility = View.VISIBLE
        simulatorTitleTextView.setCompoundDrawablesWithIntrinsicBounds(simulatorActiveIcon, null, null, null)
        realWorldPositionGroup.visibility = if (isWorldPositionSectionVisible) View.VISIBLE else View.GONE
        windSimulationGroup.visibility = if (isWindSectionVisible) View.VISIBLE else View.GONE
        attitudeGroup.visibility = if (isAttitudeSectionVisible) View.VISIBLE else View.GONE
        aircraftStatusGroup.visibility = if (isAircraftStatusSectionVisible) View.VISIBLE else View.GONE
        buttonGroup.visibility = View.GONE
    }

    private fun updateWidgetToStoppedState() {
        simulatorSwitch.isChecked = false
        latitudeEditText.visibility = View.VISIBLE
        longitudeEditText.visibility = View.VISIBLE
        satelliteCountSeekBar.visibility = View.VISIBLE
        frequencySeekBar.visibility = View.VISIBLE
        frequencyTextView.visibility = View.INVISIBLE
        latitudeTextView.visibility = View.INVISIBLE
        longitudeTextView.visibility = View.INVISIBLE
        satelliteTextView.visibility = View.INVISIBLE
        worldXTextView.text = getString(R.string.uxsdk_simulator_null_string)
        worldYTextView.text = getString(R.string.uxsdk_simulator_null_string)
        worldZTextView.text = getString(R.string.uxsdk_simulator_null_string)
        pitchTextView.text = getString(R.string.uxsdk_simulator_null_string)
        yawTextView.text = getString(R.string.uxsdk_simulator_null_string)
        rollTextView.text = getString(R.string.uxsdk_simulator_null_string)
        motorsStartedTextView.text = getString(R.string.uxsdk_simulator_null_string)
        aircraftFlyingTextView.text = getString(R.string.uxsdk_simulator_null_string)
        windSpeedXSeekBar.progress = 20
        windSpeedYSeekBar.progress = 20
        windSpeedZSeekBar.progress = 20
        simulatorTitleTextView.setCompoundDrawablesWithIntrinsicBounds(simulatorInactiveIcon, null, null, null)
        realWorldPositionGroup.visibility = View.GONE
        windSimulationGroup.visibility = View.GONE
        attitudeGroup.visibility = View.GONE
        aircraftStatusGroup.visibility = View.GONE
        buttonGroup.visibility = View.VISIBLE
    }

    private fun updateWidgetValues(simulatorState: SimulatorState) {
        widgetStateDataProcessor.onNext(SimulatorStateUpdated(simulatorState))
        latitudeTextView.text = df.format(simulatorState.location.latitude)
        longitudeTextView.text = df.format(simulatorState.location.longitude)
        worldXTextView.text = df.format(simulatorState.positionX.toDouble())
        worldYTextView.text = df.format(simulatorState.positionY.toDouble())
        worldZTextView.text = df.format(simulatorState.positionZ.toDouble())
        pitchTextView.text = df.format(simulatorState.pitch.toDouble())
        yawTextView.text = df.format(simulatorState.yaw.toDouble())
        rollTextView.text = df.format(simulatorState.roll.toDouble())
        motorsStartedTextView.setText(if (simulatorState.areMotorsOn) R.string.uxsdk_app_yes else R.string.uxsdk_app_no)
        aircraftFlyingTextView.setText(if (simulatorState.isFlying) R.string.uxsdk_app_yes else R.string.uxsdk_app_no)
    }

//    private fun updateWindValues(simulatorWindData: SimulatorWindData) {
//        widgetStateDataProcessor.onNext(SimulatorWindDataUpdated(simulatorWindData))
//        if (simulatorWindData.windSpeedX.toString() != windSpeedXSeekBar.text) {
//            windSpeedXSeekBar.text = simulatorWindData.windSpeedX.toString()
//        }
//        if (deNormalizeWindValue(simulatorWindData.windSpeedX) != windSpeedXSeekBar.progress) {
//            windSpeedXSeekBar.progress = deNormalizeWindValue(simulatorWindData.windSpeedX)
//        }
//        if (simulatorWindData.windSpeedY.toString() != windSpeedYSeekBar.text) {
//            windSpeedYSeekBar.text = simulatorWindData.windSpeedY.toString()
//        }
//        if (deNormalizeWindValue(simulatorWindData.windSpeedY) != windSpeedYSeekBar.progress) {
//            windSpeedYSeekBar.progress = deNormalizeWindValue(simulatorWindData.windSpeedY)
//        }
//        if (simulatorWindData.windSpeedZ.toString() != windSpeedZSeekBar.text) {
//            windSpeedZSeekBar.text = simulatorWindData.windSpeedZ.toString()
//        }
//        if (deNormalizeWindValue(simulatorWindData.windSpeedZ) != windSpeedZSeekBar.progress) {
//            windSpeedZSeekBar.progress = deNormalizeWindValue(simulatorWindData.windSpeedZ)
//        }
//    }

    private val simulatedLocation: LocationCoordinate2D?
        get() {
            if (latitudeEditText.text.toString().isNotEmpty()
                    && longitudeEditText.text.toString().isNotEmpty()) {
                val latCoordinates = latitudeEditText.text.toString().toDouble()
                val lngCoordinates = longitudeEditText.text.toString().toDouble()
                if (!latCoordinates.isNaN() && !lngCoordinates.isNaN()) {
                    return LocationCoordinate2D(latCoordinates, lngCoordinates)
                }
            }
            return null
        }

    private fun showSavePresetDialog() {
        if (TextUtils.isEmpty(latitudeEditText.text.toString())
                || TextUtils.isEmpty(longitudeEditText.text.toString())) return
        val presetData = SimulatorPresetData(latitudeEditText.text.toString().toDouble(),
                longitudeEditText.text.toString().toDouble(),
                satelliteCountSeekBar.progress,
                max(MIN_FREQUENCY, frequencySeekBar.progress))
        SavePresetDialog(context, true, presetData).show()
    }

    private fun showPresetListDialog() {
        PresetListDialog(context, this, height).show()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        simulatorSwitch.isEnabled = enabled
    }
    //endregion

    //region customizations
    /**
     * Set background to title text
     *
     * @param resourceId of resource to be used as background of title
     */
    fun setTitleBackground(@DrawableRes resourceId: Int) {
        titleBackground = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the simulator active icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setSimulatorActiveIcon(@DrawableRes resourceId: Int) {
        simulatorActiveIcon = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the simulator inactive icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setSimulatorInactiveIcon(@DrawableRes resourceId: Int) {
        simulatorInactiveIcon = getDrawable(resourceId)
    }

    /**
     * Set text appearance of the widget title
     *
     * @param textAppearance resourceId for text appearance for title
     */
    fun setTitleTextAppearance(@StyleRes textAppearance: Int) {
        simulatorTitleTextView.setTextAppearance(context, textAppearance)
    }

    /**
     * Set text appearance of the value fields
     *
     * @param textAppearance resourceId for text appearance of value fields
     */
    fun setValueTextAppearance(@StyleRes textAppearance: Int) {
        latitudeTextView.setTextAppearance(context, textAppearance)
        longitudeTextView.setTextAppearance(context, textAppearance)
        satelliteTextView.setTextAppearance(context, textAppearance)
        worldXTextView.setTextAppearance(context, textAppearance)
        worldYTextView.setTextAppearance(context, textAppearance)
        worldZTextView.setTextAppearance(context, textAppearance)
        motorsStartedTextView.setTextAppearance(context, textAppearance)
        aircraftFlyingTextView.setTextAppearance(context, textAppearance)
        pitchTextView.setTextAppearance(context, textAppearance)
        yawTextView.setTextAppearance(context, textAppearance)
        rollTextView.setTextAppearance(context, textAppearance)
        frequencyTextView.setTextAppearance(context, textAppearance)
    }

    /**
     * Set background resource to value text
     *
     * @param resourceId to use as background for value fields
     */
    fun setValueBackground(@DrawableRes resourceId: Int) {
        valueBackground = getDrawable(resourceId)
    }

    /**
     * Set text appearance of the widget labels
     *
     * @param textAppearance resource id of text appearance for label text
     */
    fun setLabelTextAppearance(@StyleRes textAppearance: Int) {
        latitudeLabelTextView.setTextAppearance(context, textAppearance)
        longitudeLabelTextView.setTextAppearance(context, textAppearance)
        satelliteLabelTextView.setTextAppearance(context, textAppearance)
        worldXLabelTextView.setTextAppearance(context, textAppearance)
        worldYLabelTextView.setTextAppearance(context, textAppearance)
        worldZLabelTextView.setTextAppearance(context, textAppearance)
        motorsStartedLabelTextView.setTextAppearance(context, textAppearance)
        aircraftFlyingLabelTextView.setTextAppearance(context, textAppearance)
        pitchLabelTextView.setTextAppearance(context, textAppearance)
        yawLabelTextView.setTextAppearance(context, textAppearance)
        rollLabelTextView.setTextAppearance(context, textAppearance)
        frequencyLabelTextView.setTextAppearance(context, textAppearance)
        windXLabelTextView.setTextAppearance(context, textAppearance)
        windYLabelTextView.setTextAppearance(context, textAppearance)
        windZLabelTextView.setTextAppearance(context, textAppearance)
    }

    /**
     * Set background to label text
     *
     * @param resourceId to use as background for all labels
     */
    fun setLabelBackground(@DrawableRes resourceId: Int) {
        labelBackground = getDrawable(resourceId)
    }

    /**
     * Set text appearance of the widget input text fields
     *
     * @param textAppearance resourceId for text appearance of input text fields
     */
    fun setInputTextAppearance(@StyleRes textAppearance: Int) {
        latitudeEditText.setTextAppearance(context, textAppearance)
        longitudeEditText.setTextAppearance(context, textAppearance)
    }

    /**
     * Set background to input text fields
     *
     * @param resourceId to use as background to input fields
     */
    fun setInputBackground(@DrawableRes resourceId: Int) {
        inputBackground = getDrawable(resourceId)
    }

    /**
     * Set text appearance of the widget button
     *
     * @param textAppearance resourceId for text appearance for buttons
     */
    fun setButtonTextAppearance(@StyleRes textAppearance: Int) {
        savePresetTextView.setTextAppearance(context, textAppearance)
        loadPresetTextView.setTextAppearance(context, textAppearance)
    }

    /**
     * Set background resource to button
     *
     * @param resourceId to use as background for buttons
     */
    fun setButtonBackground(@DrawableRes resourceId: Int) {
        buttonBackground = getDrawable(resourceId)
    }

    /**
     * Set text appearance of the widget header text fields
     *
     * @param textAppearance resourceId for text appearance of header text fields
     */
    fun setHeaderTextAppearance(@StyleRes textAppearance: Int) {
        positionSectionHeaderTextView.setTextAppearance(context, textAppearance)
        aircraftSectionHeaderTextView.setTextAppearance(context, textAppearance)
        attitudeSectionHeaderTextView.setTextAppearance(context, textAppearance)
        windSectionHeaderTextView.setTextAppearance(context, textAppearance)
    }

    /**
     * Set background to header text fields
     *
     * @param resourceId to use as background to header fields
     */
    fun setHeaderBackground(@DrawableRes resourceId: Int) {
        headerBackground = getDrawable(resourceId)
    }
    //endregion

    //region Hooks
    /**
     * Get the [UIState] updates
     *
     */
    fun getUIStateUpdates(): Flowable<UIState> {
        return uiUpdateStateProcessor.onBackpressureBuffer()
    }

    /**
     * Get the [ModelState] updates
     */
    @SuppressWarnings
    override fun getWidgetStateUpdate(): Flowable<ModelState> {
        return super.getWidgetStateUpdate()
    }

    /**
     *
     * Class defines widget state updates
     */
    sealed class ModelState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : ModelState()

        /**
         * Simulator state update
         */
        data class SimulatorStateUpdated(val simulatorState: SimulatorState) : ModelState()

        /**
         * Simulator active/inactive update
         */
        data class SimulatorActiveUpdated(val isActive: Boolean) : ModelState()

//        /**
//         * Simulator wind data update
//         */
//        data class SimulatorWindDataUpdated(val windData: SimulatorWindData) : ModelState()
    }

    /**
     * Class defines the widget UI updates
     */
    sealed class UIState {

        /**
         * Update when widget visibility is toggled
         */
        data class VisibilityUpdated(val isVisible: Boolean) : UIState()

        /**
         * Update when load preset button is tapped
         */
        object LoadPresetClicked : UIState()

        /**
         * Update when save preset button is tapped
         */
        object SavePresetClicked : UIState()

        /**
         * Update when start/stop simulator switch is tapped
         */
        data class SimulatorSwitchTap(val isChecked: Boolean) : UIState()

        /**
         * Update when simulator wind variation button tapped
         * Wind direction
         * 0 - X
         * 1 - Y
         * 2 - Z
         */
        data class SimulatorWindChangeClicked(@IntRange(from = 0, to = 2) val windDirection: Int,
                                              val isPositive: Boolean) : UIState()

    }
    //endregion

    companion object {
        private const val TAG = "SimulatorCtlWidget"
        private const val WIND_DIRECTION_X = 0
        private const val WIND_DIRECTION_Y = 1
        private const val WIND_DIRECTION_Z = 2
        private const val MIN_FREQUENCY = 2
        private const val DEFAULT_FREQUENCY = 20
        private const val SIMULATION_MIN_WIND_SPEED = -20
        private const val SIMULATION_MAX_WIND_SPEED = 20
        private const val WIND_SEEK_BAR_MAX = 40
    }

}