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
 * Represents a section of the obstacle detection radar that has multiple segments.
 */
class MultiAngleRadarSectionViewHolder(
        @IdRes imageIds: IntArray,
        @IdRes distanceId: Int,
        @IdRes arrowId: Int,
        parent: View
) : RadarSectionViewHolder() {

    override val distance: TextView = parent.findViewById(distanceId)
    override val arrow: ImageView = parent.findViewById(arrowId)
    private val sectorImages: Array<ImageView?> = arrayOfNulls(imageIds.size)

    init {
        imageIds.forEachIndexed { i, imageId ->
            sectorImages[i] = parent.findViewById(imageId)
        }
        hide()
    }

    override fun hide() {
        distance.visibility = View.GONE
        arrow.visibility = View.GONE
        sectorImages.forEach { it?.visibility = View.GONE }
    }


    override fun setImages(images: Array<Drawable?>) {
        sectorImages.forEachIndexed { index: Int, image: ImageView? ->
            if (index < images.size) {
                image?.setImageDrawable(images[index])
            }
        }
    }

//    override fun setSectors(sectors: Array<ObstacleDetectionSector>?, unitStr: String?) {
//        if (sectors != null && sectors.isNotEmpty()) {
//            var distanceValue: Float = Integer.MAX_VALUE.toFloat()
//            sectors.forEachIndexed { i, sector ->
//                if (sector.obstacleDistanceInMeters < distanceValue) {
//                    distanceValue = sector.obstacleDistanceInMeters
//                }
//                val level = sector.warningLevel.value()
//                if (level in 0..5) {
//                    sectorImages[i]?.setImageLevel(level)
//                }
//            }
//
//            val isInvalidOrZero = sectors.all { sector ->
//                sector.warningLevel == ObstacleDetectionSectorWarning.INVALID
//            } || sectors.all { sector ->
//                sector.warningLevel == ObstacleDetectionSectorWarning.LEVEL_1
//            }
//            if (isInvalidOrZero) {
//                hide()
//            } else {
//                show()
//                super.setDistance(distanceValue.toDouble(), unitStr)
//            }
//        } else {
//            hide()
//        }
//    }
}
