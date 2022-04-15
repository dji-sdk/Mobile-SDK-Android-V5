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

import androidx.annotation.ColorInt
import dji.v5.ux.core.ui.CenterPointView.CenterPointType
import dji.v5.ux.core.ui.GridLineView.GridLineType
import dji.v5.ux.core.util.SettingDefinitions.ControlMode
import dji.v5.ux.core.util.UnitConversionUtil.TemperatureUnitType
import dji.v5.ux.core.util.UnitConversionUtil.UnitType


/**
 * Interface to be implemented for functions included under
 * global preferences. These settings will persist across app restarts.
 */
interface GlobalPreferencesInterface {
    /**
     * Set up the listeners for the global preferences interface
     */
    fun setUpListener()

    /**
     * Clean up the listeners for the global preferences interface
     */
    fun cleanup()

    //region global Settings interface
    /**
     * [UnitType] value saved.
     */
    var unitType: UnitType

    /**
     * [TemperatureUnitType] value saved.
     */
    var temperatureUnitType: TemperatureUnitType

    /**
     * Boolean value indicating if AFC is enabled if saved.
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getAFCEnabled")
    @set:JvmName("setAFCEnabled")
    var afcEnabled: Boolean

    /**
     * Boolean value indicating if the AirSense terms should never be shown.
     */
    var isAirSenseTermsNeverShown: Boolean

    /**
     * [GridLineType] for the grid line overlay.
     */
    var gridLineType: GridLineType

    /**
     * Center Point Type from [CenterPointType]
     */
    var centerPointType: CenterPointType

    /**
     * Center point color int
     */
    @get:ColorInt
    @setparam:ColorInt
    var centerPointColor: Int

    /**
     * Control mode from [ControlMode]
     */
    var controlMode: ControlMode

    /**
     * Boolean value indicating if the Unit Mode dialog should never be shown.
     */
    var isUnitModeDialogNeverShown: Boolean
    //endregion
}

