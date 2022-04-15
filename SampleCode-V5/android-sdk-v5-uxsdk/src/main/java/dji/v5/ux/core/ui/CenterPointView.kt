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
package dji.v5.ux.core.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import dji.v5.ux.R
import dji.v5.ux.core.communication.GlobalPreferencesManager
import dji.v5.ux.core.ui.CenterPointView.CenterPointType
import dji.v5.ux.core.util.ViewUtil

/**
 * Displays an icon based on the given [CenterPointType].
 */
class CenterPointView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    //region Fields
    /**
     * The color of the center point
     */
    @get:ColorInt
    var color: Int
        get() {
            return if (GlobalPreferencesManager.getInstance() != null) {
                GlobalPreferencesManager.getInstance().centerPointColor
            } else {
                Color.WHITE
            }
        }
        set(@ColorInt color) {
            if (this.color != color && GlobalPreferencesManager.getInstance() != null) {
                GlobalPreferencesManager.getInstance().centerPointColor = color
                ViewUtil.tintImage(this, color)
            }
        }

    /**
     * The type of center point
     */
    var type: CenterPointType
        get() {
            return if (GlobalPreferencesManager.getInstance() != null) {
                GlobalPreferencesManager.getInstance().centerPointType
            } else {
                CenterPointType.NONE
            }
        }
        set(type) {
            if (this.type != type && GlobalPreferencesManager.getInstance() != null) {
                GlobalPreferencesManager.getInstance().centerPointType = type
                initView()
            }
        }
    //endregion

    //region Constructor
    init {
        initView()
    }

    private fun initView() {
        if (type == CenterPointType.NONE) {
            visibility = View.GONE
        } else {
            visibility = View.VISIBLE
            setImageResource(type.drawableId)
            ViewUtil.tintImage(this, color)
        }
    }
    //endregion

    //region Classes
    /**
     * Represents the types of center points that can be set.
     *
     * @property value Identifier for the item
     * @property drawableId ID of the drawable resource
     */
    enum class CenterPointType(@get:JvmName("value") val value: Int,
                               @DrawableRes var drawableId: Int) {
        /**
         * The center point is hidden.
         */
        NONE(0, -1),

        /**
         * The center point is a standard shape.
         */
        STANDARD(1, R.drawable.uxsdk_ic_centerpoint_standard),

        /**
         * The center point is a cross shape.
         */
        CROSS(2, R.drawable.uxsdk_ic_centerpoint_cross),

        /**
         * The center point is a narrow cross shape.
         */
        NARROW_CROSS(3, R.drawable.uxsdk_ic_centerpoint_narrow_cross),

        /**
         * The center point is a frame shape.
         */
        FRAME(4, R.drawable.uxsdk_ic_centerpoint_frame),

        /**
         * The center point is a frame shape with a cross inside.
         */
        FRAME_AND_CROSS(5, R.drawable.uxsdk_ic_centerpoint_frame_and_cross),

        /**
         * The center point is a square shape.
         */
        SQUARE(6, R.drawable.uxsdk_ic_centerpoint_square),

        /**
         * The center point is a square shape with a cross inside.
         */
        SQUARE_AND_CROSS(7, R.drawable.uxsdk_ic_centerpoint_square_and_cross),

        /**
         * The center point is an unknown shape.
         */
        UNKNOWN(8, -1);

        companion object {
            @JvmStatic
            val values = values()

            @JvmStatic
            fun find(value: Int): CenterPointType {
                return values.find { it.value == value } ?: UNKNOWN
            }
        }

    }
    //endregion

    companion object {
        /**
         * Sets a new drawable to display when the center point is set to the given type.
         *
         * @param type       The type of center point
         * @param drawableId The drawable that will be displayed
         */
        @JvmStatic
        fun setImageForType(type: CenterPointType, @DrawableRes drawableId: Int) {
            type.drawableId = drawableId
        }
    }
}