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

@file:JvmName("MathExtensions")

package dji.v5.ux.core.extension

import dji.v5.ux.core.util.UnitConversionUtil
import dji.v5.ux.core.util.UnitConversionUtil.UnitType

/**
 * Convert milliVolts to Volts
 */
fun Float.milliVoltsToVolts(): Float = this / 1000f

/**
 * Convert velocity to appropriate value by unit
 */
fun Float.toVelocity(unitType: UnitType): Float {
    return if (unitType == UnitType.IMPERIAL) {
        UnitConversionUtil.convertMetersPerSecToMilesPerHr(this)
    } else this
}

fun Double.toVelocity(unitType: UnitType): Double {
    return if (unitType == UnitType.IMPERIAL) {
        UnitConversionUtil.convertMetersPerSecToMilesPerHr(this)
    } else this
}

/**
 * Convert distance to appropriate value by unit
 */
fun Float.toDistance(unitType: UnitType): Float {
    return if (unitType == UnitType.IMPERIAL) {
        UnitConversionUtil.convertMetersToFeet(this)
    } else this
}

/**
 * Convert distance to appropriate value by unit
 */
fun Double.toDistance(unitType: UnitType): Double {
    return if (unitType == UnitType.IMPERIAL) {
        UnitConversionUtil.convertMetersToFeet(this)
    } else this
}
