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

package dji.v5.ux.core.communication;

import dji.v5.ux.core.util.SettingDefinitions;
import dji.v5.ux.core.util.UnitConversionUtil;

/**
 * Class containing UXKeys related to global preferences
 */
public final class GlobalPreferenceKeys extends UXKeys {
    @UXParamKey(type = UnitConversionUtil.UnitType.class, updateType = UpdateType.ON_CHANGE)
    public static final String UNIT_TYPE = "UnitType";

    @UXParamKey(type = Boolean.class, updateType = UpdateType.ON_CHANGE)
    public static final String AFC_ENABLED = "AFCEnabled";

//    @UXParamKey(type = ColorWaveformSettings.ColorWaveformDisplayState.class, updateType = UpdateType.ON_CHANGE)
//    public static final String COLOR_WAVEFORM_DISPLAY_STATE = "ColorWaveformDisplayState";

    @UXParamKey(type = Boolean.class, updateType = UpdateType.ON_CHANGE)
    public static final String COLOR_WAVEFORM_ENABLED = "ColorWaveformEnabled";

    @UXParamKey(type = SettingDefinitions.ControlMode.class, updateType = UpdateType.ON_CHANGE)
    public static final String CONTROL_MODE = "ControlMode";

    @UXParamKey(type = UnitConversionUtil.TemperatureUnitType.class, updateType = UpdateType.ON_CHANGE)
    public static final String TEMPERATURE_UNIT_TYPE = "TemperatureUnitType";

    @UXParamKey(type = Boolean.class, updateType = UpdateType.ON_EVENT)
    public static final String GIMBAL_ADJUST_CLICKED = "gimbalAdujustClicled";

    private GlobalPreferenceKeys() {
        super();
    }
}
