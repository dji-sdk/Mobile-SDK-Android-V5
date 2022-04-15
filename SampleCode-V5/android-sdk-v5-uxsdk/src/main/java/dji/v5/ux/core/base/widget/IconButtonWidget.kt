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

package dji.v5.ux.core.base.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.processors.PublishProcessor
import dji.v5.ux.R
import dji.v5.ux.core.extension.*

/**
 * Abstract class that represents a widget with a single Image View.
 * The class provides functionality and customizations for widgets to reuse
 */
abstract class IconButtonWidget<T> @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayoutWidget<T>(context, attrs, defStyleAttr), View.OnClickListener {

    //region Fields

    protected val foregroundImageView: ImageView = findViewById(R.id.image_view_button)
    protected val uiUpdateStateProcessor: PublishProcessor<UIState> = PublishProcessor.create()

    /**
     * The color of the icon when the product is connected
     */
    @get:ColorInt
    var connectedStateIconColor: Int = getColor(R.color.uxsdk_transparent)
        set(@ColorInt value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * The color of the icon when the product is disconnected
     */
    @get:ColorInt
    var disconnectedStateIconColor: Int = getColor(R.color.uxsdk_gray_58)
        set(@ColorInt value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * Get current background of foregroundImageView
     */
    var iconBackground: Drawable?
        get() = foregroundImageView.background
        set(value) {
            foregroundImageView.background = value
        }

    //endregion
    @CallSuper
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_icon_button_widget, this)
        setOnClickListener(this)
    }

    init {
        attrs?.let { initAttributes(context, it) }
    }

    protected abstract fun checkAndUpdateIconColor()

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.IconButtonWidget).use { typedArray ->
            typedArray.getDrawableAndUse(R.styleable.IconButtonWidget_uxsdk_iconBackground) {
                iconBackground = it
            }
            typedArray.getColorAndUse(R.styleable.IconButtonWidget_uxsdk_connectedStateIconColor) {
                connectedStateIconColor = it
            }
            typedArray.getColorAndUse(R.styleable.IconButtonWidget_uxsdk_disconnectedStateIconColor) {
                disconnectedStateIconColor = it
            }
        }
    }

    @CallSuper
    override fun onClick(view: View?) {
        uiUpdateStateProcessor.onNext(UIState.WidgetClicked)
    }

    override fun getIdealDimensionRatioString(): String {
        return getString(R.string.uxsdk_widget_default_ratio)
    }

    /**
     * Set background to foregroundImageView
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setIconBackground(@DrawableRes resourceId: Int) {
        iconBackground = getDrawable(resourceId)
    }

    /**
     * Get the [UIState] updates
     */
    fun getUIStateUpdates(): Flowable<UIState> {
        return uiUpdateStateProcessor.onBackpressureBuffer()
    }

    /**
     * Widget UI update State
     */
    sealed class UIState {
        /**
         * Widget click update
         */
        object WidgetClicked : UIState()

        /**
         * Dialog shown update
         */
        data class DialogDisplayed(val info: Any?) : UIState()

        /**
         * Dialog action confirm
         */
        data class DialogActionConfirmed(val info: Any?) : UIState()

        /**
         * Dialog action dismiss
         */
        data class DialogDismissed(val info: Any?) : UIState()

        /**
         * Dialog action dismiss
         */
        data class DialogActionCancelled(val info: Any?) : UIState()

        /**
         * Dialog checkbox interaction
         */
        data class DialogCheckboxCheckChanged(val info: Any?) : UIState()
    }
}
