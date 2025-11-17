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

@file:JvmName("ViewExtensions")

package dji.v5.ux.core.extension

import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.SparseLongArray
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import dji.v5.ux.R
import dji.v5.ux.core.util.UnitConversionUtil.UnitType


/**
 * Get the [String] for the given [stringRes].
 */
fun View.getString(@StringRes stringRes: Int, vararg value: Any): String = context.resources.getString(stringRes, *value)

/**
 * Get the [Drawable] for the given [drawableRes].
 */
fun View.getDrawable(@DrawableRes drawableRes: Int): Drawable = context.resources.getDrawable(drawableRes)

/**
 * The the color int for the given [colorRes].
 */
@ColorInt
fun View.getColor(@ColorRes colorRes: Int): Int = context.resources.getColor(colorRes)

/**
 * The the px size for the given [dimenRes].
 */
@Px
fun View.getDimension(@DimenRes dimenRes: Int): Float = context.resources.getDimension(dimenRes)

/**
 * Set the view [View.VISIBLE].
 */
fun View.show() {
    visibility = View.VISIBLE
}

/**
 * Set the view [View.GONE].
 */
fun View.hide() {
    visibility = View.GONE
}

/**
 * Toggle the view between [View.GONE] and [View.VISIBLE]
 */
fun View.toggleVisibility() {
    if (visibility == View.VISIBLE) hide()
    else show()
}

/**
 * Show a short length toast with the given [messageResId].
 */
fun View.showShortToast(@StringRes messageResId: Int) {
    Toast.makeText(
        context,
        messageResId,
        Toast.LENGTH_SHORT
    )
        .show()
}

/**
 * Show a long length toast with the given [messageResId].
 */
fun View.showLongToast(@StringRes messageResId: Int) {
    Toast.makeText(
        context,
        messageResId,
        Toast.LENGTH_LONG
    )
        .show()
}

/**
 * Show a short length toast with the given [String].
 */
fun View.showShortToast(message: String?) {
    Toast.makeText(
        context,
        message,
        Toast.LENGTH_SHORT
    )
        .show()
}

/**
 * Show a long length toast with the given [String].
 */
fun View.showLongToast(message: String?) {
    Toast.makeText(
        context,
        message,
        Toast.LENGTH_LONG
    )
        .show()
}

/**
 * The [TextView]'s text color int.
 */
var TextView.textColor: Int
    @ColorInt
    get() = this.currentTextColor
    set(@ColorInt value) {
        this.setTextColor(value)
    }

/**
 * The [TextView]'s text color state list.
 */
var TextView.textColorStateList: ColorStateList?
    get() = this.textColors
    set(value) {
        this.setTextColor(value)
    }

/**
 * The [ImageView]'s drawable.
 */
var ImageView.imageDrawable: Drawable?
    get() = this.drawable
    set(value) {
        this.setImageDrawable(value)
    }

/**
 * Show an alert dialog.
 * @param dialogTheme The style of the dialog
 * @param isCancellable Dismiss the dialog when touch outside its bounds
 * @param title Dialog title text
 * @param icon  Dialog title icon
 * @param message Dialog message
 * @param dismissButton Dismiss button text
 * @param dialogClickListener
 */
fun View.showAlertDialog(
    @StyleRes dialogTheme: Int = androidx.appcompat.R.style.Theme_AppCompat_Dialog,
    title: String? = getString(R.string.uxsdk_alert),
    icon: Drawable? = null,
    message: String? = null,
    dismissButton: String? = getString(R.string.uxsdk_app_ok),
    dialogClickListener: DialogInterface.OnClickListener? = null,
    dialogDismissListener: DialogInterface.OnDismissListener? = null,
) {
    val builder = AlertDialog.Builder(context, dialogTheme)
    builder.setTitle(title)
    builder.setIcon(icon)
    builder.setCancelable(true)
    builder.setMessage(message)
    builder.setPositiveButton(dismissButton, dialogClickListener)
    builder.setOnDismissListener(dialogDismissListener)
    val dialog = builder.create()
    dialog.show()
}

/**
 * Show an alert dialog.
 * @param dialogTheme The style of the dialog
 * @param isCancellable Dismiss the dialog when touch outside its bounds
 * @param title Dialog title text
 * @param icon  Dialog title icon
 * @param message Dialog message
 * @param positiveButton Positive button text
 * @param negativeButton Negative button text
 * @param dialogClickListener
 */
fun View.showConfirmationDialog(
    @StyleRes dialogTheme: Int = androidx.appcompat.R.style.Theme_AppCompat_Dialog,
    title: String? = getString(R.string.uxsdk_alert),
    icon: Drawable? = null,
    message: String? = null,
    dialogClickListener: DialogInterface.OnClickListener? = null,
    dialogDismissListener: DialogInterface.OnDismissListener? = null,
) {
    val builder = AlertDialog.Builder(context, dialogTheme)
    builder.setIcon(icon)
    builder.setTitle(title)
    builder.setCancelable(true)
    builder.setMessage(message)
    builder.setPositiveButton(getString(R.string.uxsdk_app_ok), dialogClickListener)
    builder.setNegativeButton(getString(R.string.uxsdk_app_cancel), dialogClickListener)
    builder.setOnDismissListener(dialogDismissListener)
    val dialog = builder.create()
    dialog.show()
}

/**
 * Get the unit string for velocity based on [UnitType]
 */
fun View.getVelocityString(unitType: UnitType): String {
    return if (unitType == UnitType.IMPERIAL) {
        getString(R.string.uxsdk_unit_mile_per_hr)
    } else {
        getString(R.string.uxsdk_unit_meter_per_second)
    }
}

/**
 * Get the unit string for distance based on [UnitType]
 */
fun View.getDistanceString(unitType: UnitType): String {
    return if (unitType == UnitType.IMPERIAL) {
        getString(R.string.uxsdk_unit_feet)
    } else {
        getString(R.string.uxsdk_unit_meters)
    }
}

/**
 * Set the border to a view.
 * The extension creates a layered background which can be used to set up a border to the view.
 *
 * @param backgroundColor - The color for the solid background.
 * @param borderColor - The color for the border for the view.
 * @param leftBorder - The size of the left border.
 * @param topBorder - The size of the top border.
 * @param rightBorder - The size of the right border.
 * @param bottomBorder - The size of the bottom border.
 *
 */
fun View.setBorder(
    @ColorInt backgroundColor: Int = getColor(R.color.uxsdk_transparent),
    @ColorInt borderColor: Int = getColor(R.color.uxsdk_transparent),
    leftBorder: Int = 0,
    topBorder: Int = 0,
    rightBorder: Int = 0,
    bottomBorder: Int = 0,
) {
    val borderColorDrawable = ColorDrawable(borderColor)
    val backgroundColorDrawable = ColorDrawable(backgroundColor)

    // Initialize a new array of drawable objects
    val drawables = arrayOf<Drawable>(borderColorDrawable, backgroundColorDrawable)

    // Initialize a new layer drawable instance from drawables array
    val layerDrawable = LayerDrawable(drawables)

    // Set padding for background color layer
    layerDrawable.setLayerInset(
        1,  // Index of the drawable to adjust [background color layer]
        leftBorder,  // Number of pixels to add to the left bound [left border]
        topBorder,  // Number of pixels to add to the top bound [top border]
        rightBorder,  // Number of pixels to add to the right bound [right border]
        bottomBorder // Number of pixels to add to the bottom bound [bottom border]
    )
    this.background = layerDrawable

}

/**
 * On click listener for recycler view.
 */
fun <T : RecyclerView.ViewHolder> T.listen(event: (position: Int) -> Unit): T {
    itemView.setOnClickListener {
        event.invoke(adapterPosition)
    }
    return this
}

const val TRANSITION_OFFSET = 80
private const val FAST_CLICK_DURATION = 300
private val sClickTimes: SparseLongArray = SparseLongArray()

/**
 * 判断是否点击过快
 * @param viewId
 * @param duration
 * @return
 */
fun Button.isFastClick(duration: Int): Boolean {
    val prevTime: Long = sClickTimes.get(this.id)
    val now = System.currentTimeMillis()
    val isFast = now - prevTime < duration
    if (!isFast) {
        sClickTimes.put(this.id, now)
    }
    return isFast
}

/**
 * 判断是否点击过快
 * @param viewId
 * @return
 */
fun Button.isFastClick(): Boolean {
    return isFastClick(FAST_CLICK_DURATION)
}
