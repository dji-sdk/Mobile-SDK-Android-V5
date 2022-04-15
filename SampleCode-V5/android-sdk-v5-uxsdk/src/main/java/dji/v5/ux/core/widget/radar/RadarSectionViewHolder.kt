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
package dji.v5.ux.core.widget.radar

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import java.util.*

/**
 * Represents a section of the obstacle detection radar : forward, backward, left, or right.
 */
abstract class RadarSectionViewHolder {

    /**
     * The [TextView] representing the distance between the obstacle and the aircraft
     */
    abstract val distance: TextView

    /**
     * The [ImageView] representing the direction of the obstacle
     */
    abstract val arrow: ImageView

    /**
     * The text color of the distance text view
     */
    @get:ColorInt
    var distanceTextColor: Int
        get() = distance.currentTextColor
        set(color) {
            distance.setTextColor(color)
        }

    /**
     * The text color state list of the distance text view
     */
    var distanceTextColors: ColorStateList?
        get() = distance.textColors
        set(colorStateList) {
            distance.setTextColor(colorStateList)
        }

    /**
     * The background of the distance text view
     */
    var distanceTextBackground: Drawable?
        get() = distance.background
        set(icon) {
            distance.background = icon
        }

    /**
     * The text size of the distance text view
     */
    var distanceTextSize: Float
        get() = distance.textSize
        set(textSize) {
            distance.textSize = textSize
        }

    /**
     * The drawable resource for the arrow icon. The given icon should be pointed up, and each
     * radar direction's icon will be rotated to point the corresponding direction.
     */
    var distanceArrowIcon: Drawable?
        get() = arrow.drawable
        set(icon) {
            arrow.setImageDrawable(icon)
        }

    /**
     * The drawable resource for the arrow icon's background
     */
    var distanceArrowIconBackground: Drawable?
        get() = arrow.background
        set(icon) {
            arrow.background = icon
        }

    /**
     * Set the distance of the detected obstacle.
     *
     * @param distance The distance of the detected obstacle.
     * @param unitStr  A string representing the units for the distance measurement.
     */
    open fun setDistance(distance: Double, unitStr: String?) {
        val distanceValue = distance.toFloat()
        this.distance.text = String.format(Locale.getDefault(), "%.1f %s", distanceValue, unitStr)
    }

    /**
     * Set the sectors of a multi-angle radar section. For single-angle radar sections, this
     * method has no effect.
     *
     * @param sectors An array of [ObstacleDetectionSector] representing the sections of a
     * multi-angle radar.
     * @param unitStr A string representing the units for the distance measurement.
     */
//    abstract fun setSectors(sectors: Array<ObstacleDetectionSector>?, unitStr: String?)

    /**
     * Set the images for this radar section. For single-angle radar sections, only the first
     * image in the array is used. For multi-angle radar sections, the images will be
     * overlapped to form the sections of the radar.
     *
     * @param images An array of level-list [Drawable] objects with levels from 0-5 for multi-angle
     * radar sections or 0-1 for single-angle radar sections.
     */
    abstract fun setImages(images: Array<Drawable?>)

    /**
     * Hide this radar section. May be shown again if [setSectors] or [setDistance] is called
     * afterwards.
     */
    abstract fun hide()

    //region Customizations
    /**
     * Set text appearance of the distance text view
     *
     * @param context A context object
     * @param textAppearanceResId Style resource for text appearance
     */
    fun setDistanceTextAppearance(context: Context?, @StyleRes textAppearanceResId: Int) {
        distance.setTextAppearance(context, textAppearanceResId)
    }

    /**
     * Set the resource ID for the background of the distance text view
     *
     * @param resourceId Integer ID of the text view's background resource
     */
    fun setDistanceTextBackground(@DrawableRes resourceId: Int) {
        distance.setBackgroundResource(resourceId)
    }

    /**
     * Set the resource ID for the arrow icon. The given icon should be pointed up, and each
     * radar direction's icon will be rotated to point the corresponding direction.
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setDistanceArrowIcon(@DrawableRes resourceId: Int) {
        arrow.setImageResource(resourceId)
    }

    /**
     * Set the resource ID for the arrow icon's background
     *
     * @param resourceId Integer ID of the icon's background resource
     */
    fun setDistanceArrowIconBackground(@DrawableRes resourceId: Int) {
        arrow.setBackgroundResource(resourceId)
    }
    //endregion
}