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

package dji.v5.ux.training.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dji.v5.utils.common.ContextUtil
import dji.v5.ux.training.simulatorcontrol.preset.SimulatorPresetData

/**
 * Simulator Widget Preferences
 * This shared preference file is dedicated for storing Simulator Presets.
 * It is essential to keep this segregation for getting all Preset entries even when
 * the keys dynamically change
 * Method to
 * 1. save a preset for simulator
 * 2. load the preset list
 * 3. delete a preset from the list
 */
object SimulatorPresetUtils {
    private const val SIMULATOR_SHARED_PREFERENCES = "simulatorsharedpreferences"
    private const val SIMULATOR_FREQUENCY = "simulatorfrequency"
    private const val SIMULATOR_LAT = "simulatorLatitude"
    private const val SIMULATOR_LNG = "simulatorLongitude"
    private val sharedPreferences: SharedPreferences = ContextUtil.getContext()
            .getSharedPreferences(SIMULATOR_SHARED_PREFERENCES, Context.MODE_PRIVATE)

    /**
     * List representing saved presets in simulator control
     */
    val presetList: Map<String, *>
        get() {
            val resultList = sharedPreferences.all
            resultList.remove(SIMULATOR_FREQUENCY)
            resultList.remove(SIMULATOR_LAT)
            resultList.remove(SIMULATOR_LNG)
            return resultList
        }

    /**
     * Cached location latitude value for simulator start
     */
    var currentSimulatorStartLat: String
        get() = sharedPreferences.getString(SIMULATOR_LAT, "") ?: ""
        set(value) {
            sharedPreferences.edit { putString(SIMULATOR_LAT, value) }
        }

    /**
     * Cached location longitude value for simulator start
     */
    var currentSimulatorStartLng: String
        get() = sharedPreferences.getString(SIMULATOR_LNG, "") ?: ""
        set(value) {
            sharedPreferences.edit { putString(SIMULATOR_LNG, value) }
        }

    /**
     * Cached frequency value for simulator start
     */
    var currentSimulatorFrequency: Int
        get() = sharedPreferences.getInt(SIMULATOR_FREQUENCY, -1)
        set(value) {
            sharedPreferences.edit { putInt(SIMULATOR_FREQUENCY, value) }
        }

    /**
     * Save preset to be used later.
     *
     * @param key - String value to be used as key and display name.
     * @param lat - Double value representing latitude.
     * @param lng - Double value representing longitude.
     * @param satelliteCount - Integer value representing satellite count.
     * @param frequency - Integer value representing data frequency.
     */
    fun savePreset(key: String, lat: Double, lng: Double, satelliteCount: Int, frequency: Int) {
        sharedPreferences.edit { putString(key, "$lat $lng $satelliteCount $frequency") }
    }

    /**
     * Save preset to be used later.
     *
     * @param key - String value to be used as key and display name.
     * @param simulatorPresetData - instance of [SimulatorPresetData].
     */
    fun savePreset(key: String, simulatorPresetData: SimulatorPresetData) {
        savePreset(key, simulatorPresetData.latitude,
                simulatorPresetData.longitude,
                simulatorPresetData.satelliteCount,
                simulatorPresetData.updateFrequency)
    }

    /**
     * Delete a preset.
     *
     * @param key - String key of the preset to be deleted.
     */
    fun deletePreset(key: String?) {
        sharedPreferences.edit { remove(key) }
    }

    /**
     * Clear cached frequency value.
     */
    fun clearSimulatorFrequency() {
        deletePreset(SIMULATOR_FREQUENCY)
    }

    /**
     * Clear cached latitude value.
     */
    fun clearSimulatorStartLat() {
        deletePreset(SIMULATOR_LAT)
    }

    /**
     * Clear cached longitude value.
     */
    fun clearSimulatorStartLng() {
        deletePreset(SIMULATOR_LNG)
    }

}