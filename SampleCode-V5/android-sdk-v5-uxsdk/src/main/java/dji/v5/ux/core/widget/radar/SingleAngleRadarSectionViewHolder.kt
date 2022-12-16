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

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView

import androidx.annotation.IdRes


/**
 * Represents a section of the obstacle detection radar that has a single segment.
 */
class SingleAngleRadarSectionViewHolder(
        @IdRes imageId: Int,
        @IdRes distanceId: Int,
        @IdRes arrowId: Int,
        parent: View
) : RadarSectionViewHolder() {

    override val distance: TextView = parent.findViewById(distanceId)
    override val arrow: ImageView = parent.findViewById(arrowId)
    private val radarImage: ImageView = parent.findViewById(imageId)

    init {
        hide()
    }

    override fun hide() {
        radarImage.visibility = View.GONE
        distance.visibility = View.GONE
        arrow.visibility = View.GONE
    }


    override fun setImages(images: Array<Drawable?>) {
        radarImage.setImageDrawable(images[0])
    }

//    override fun setSectors(sectors: Array<ObstacleDetectionSector>?, unitStr: String?) {
//        val warningLevel = sectors?.get(0)?.warningLevel
//        if (warningLevel == ObstacleDetectionSectorWarning.LEVEL_1
//                || warningLevel == ObstacleDetectionSectorWarning.LEVEL_2) {
//            show()
//            radarImage.setImageLevel(warningLevel.value())
//        } else {
//            hide()
//        }
//    }
}
