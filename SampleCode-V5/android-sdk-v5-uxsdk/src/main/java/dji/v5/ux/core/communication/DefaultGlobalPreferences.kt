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

package dji.v5.ux.core.communication

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.content.edit
import dji.v5.ux.core.ui.CenterPointView
import dji.v5.ux.core.ui.GridLineView.GridLineType
import dji.v5.ux.core.util.SettingDefinitions
import dji.v5.ux.core.util.UnitConversionUtil


/**
 * Default implementation of the GlobalPreferencesInterface using SharedPreferences.
 * These settings will persist across app restarts.
 */
class DefaultGlobalPreferences(context: Context) : GlobalPreferencesInterface {

    private val sharedPreferences: SharedPreferences = getSharedPreferences(context)

    override fun setUpListener() { //Do nothing
    }

    override fun cleanup() { //Do nothing
    }

    override var unitType: UnitConversionUtil.UnitType
        get() = UnitConversionUtil.UnitType.find(sharedPreferences.getInt(PREF_GLOBAL_UNIT_TYPE,
                UnitConversionUtil.UnitType.METRIC.value()))
        set(unitType) = sharedPreferences.edit { putInt(PREF_GLOBAL_UNIT_TYPE, unitType.value()) }

    override var temperatureUnitType: UnitConversionUtil.TemperatureUnitType
        get() = UnitConversionUtil.TemperatureUnitType.find(sharedPreferences.getInt(PREF_TEMPERATURE_UNIT_TYPE,
                UnitConversionUtil.UnitType.METRIC.value()))
        set(temperatureUnit) = sharedPreferences.edit { putInt(PREF_TEMPERATURE_UNIT_TYPE, temperatureUnit.value()) }


    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getAFCEnabled")
    @set:JvmName("setAFCEnabled")
    override var afcEnabled: Boolean
        get() = sharedPreferences.getBoolean(PREF_IS_AFC_ENABLED, true)
        set(enabled) = sharedPreferences.edit { putBoolean(PREF_IS_AFC_ENABLED, enabled) }

    override var isAirSenseTermsNeverShown: Boolean
        get() = sharedPreferences.getBoolean(PREF_AIR_SENSE_TERMS_NEVER_SHOWN, false)
        set(neverShown) =
            sharedPreferences.edit { putBoolean(PREF_AIR_SENSE_TERMS_NEVER_SHOWN, neverShown) }

    override var gridLineType: GridLineType
        get() = GridLineType.find(sharedPreferences.getInt(PREF_GRID_LINE_TYPE,
                GridLineType.NONE.value))
        set(gridLineType) =
            sharedPreferences.edit { putInt(PREF_GRID_LINE_TYPE, gridLineType.value) }


    override var centerPointType: CenterPointView.CenterPointType
        get() = CenterPointView.CenterPointType.find(sharedPreferences.getInt(PREF_CENTER_POINT_TYPE,
                CenterPointView.CenterPointType.NONE.value))
        set(centerPointType) =
            sharedPreferences.edit { putInt(PREF_CENTER_POINT_TYPE, centerPointType.value) }

    @get:ColorInt
    @setparam:ColorInt
    override var centerPointColor: Int
        get() = sharedPreferences.getInt(PREF_CENTER_POINT_COLOR, Color.WHITE)
        set(centerPointColor) =
            sharedPreferences.edit { putInt(PREF_CENTER_POINT_COLOR, centerPointColor) }

    override var controlMode: SettingDefinitions.ControlMode
        get() = SettingDefinitions.ControlMode.find(sharedPreferences.getInt(PREF_CONTROL_MODE,
                SettingDefinitions.ControlMode.SPOT_METER.value()))
        set(controlMode) = sharedPreferences.edit { putInt(PREF_CONTROL_MODE, controlMode.value()) }

    override var isUnitModeDialogNeverShown: Boolean
        get() = sharedPreferences.getBoolean(PREF_UNIT_MODE_DIALOG_NEVER_SHOWN, false)
        set(neverShown) =
            sharedPreferences.edit { putBoolean(PREF_UNIT_MODE_DIALOG_NEVER_SHOWN, neverShown) }

    companion object {
        //region Constants
        private const val PREF_IS_AFC_ENABLED: String = "afcEnabled"
        private const val PREF_GLOBAL_UNIT_TYPE: String = "globalUnitType"
        private const val PREF_TEMPERATURE_UNIT_TYPE: String = "temperatureUnitType"
        private const val PREF_AIR_SENSE_TERMS_NEVER_SHOWN: String = "airSenseTerms"
        private const val PREF_GRID_LINE_TYPE: String = "gridLineType"
        private const val PREF_CENTER_POINT_TYPE: String = "centerPointType"
        private const val PREF_CENTER_POINT_COLOR: String = "centerPointColor"
        private const val PREF_CONTROL_MODE: String = "controlMode"
        private const val PREF_UNIT_MODE_DIALOG_NEVER_SHOWN: String = "unitMode"
        private fun getSharedPreferences(context: Context): SharedPreferences =
                context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

    }
}
