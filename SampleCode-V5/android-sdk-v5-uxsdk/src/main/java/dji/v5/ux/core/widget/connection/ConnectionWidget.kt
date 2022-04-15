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

package dji.v5.ux.core.widget.connection

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.content.res.use
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.core.base.SchedulerProvider
import io.reactivex.rxjava3.functions.Consumer
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.widget.FrameLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.getDrawable
import dji.v5.ux.core.extension.getDrawableAndUse
import dji.v5.ux.core.extension.getString
import dji.v5.ux.core.extension.imageDrawable
import dji.v5.ux.core.widget.connection.ConnectionWidget.ModelState
import dji.v5.ux.core.widget.connection.ConnectionWidget.ModelState.ProductConnected
import dji.v5.ux.core.util.RxUtil

private const val TAG = "ConnectionWidget"

/**
 * This widget displays the connection status of the app with the product.
 */
open class ConnectionWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayoutWidget<ModelState>(context, attrs, defStyleAttr) {

    //region Fields
    /**
     * Background drawable resource for the connectivity icon
     */
    var connectivityIconBackground: Drawable?
        get() = connectivityImageView.background
        set(value) {
            connectivityImageView.background = value
        }

    /**
     * The icon when the product is connected
     */
    var connectedIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_connected)
        set(value) {
            field = value
            checkAndUpdateIcon()
        }

    /**
     * The icon when the product is disconnected
     */
    var disconnectedIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_disconnected)
        set(value) {
            field = value
            checkAndUpdateIcon()
        }

    private val widgetModel by lazy {
        ConnectionWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance())
    }
    private val connectivityImageView: ImageView = findViewById(R.id.image_view_connection_status)
    //endregion

    //region Constructor
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_widget_connection, this)
    }

    init {
        attrs?.let { initAttributes(context, it) }
    }
    //endregion

    //region Lifecycle
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::updateUI))
    }

    //endregion

    //region Reactions to model
    private fun updateUI(isConnected: Boolean) {
        widgetStateDataProcessor.onNext(ProductConnected(isConnected))
        connectivityImageView.imageDrawable = if (isConnected) {
            connectedIcon
        } else {
            disconnectedIcon
        }
    }
    //endregion

    //region private helpers
    private fun checkAndUpdateIcon() {
        if (!isInEditMode) {
            addDisposable(widgetModel.productConnection.lastOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(Consumer { updateUI(it) }, RxUtil.logErrorConsumer(TAG, "product connection")))
        }
    }
    //endregion

    //region Customizations
    override fun getIdealDimensionRatioString(): String {
        return getString(R.string.uxsdk_widget_default_ratio)
    }

    /**
     * Set the resource ID for the icon when the product is disconnected
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setDisconnectedIcon(@DrawableRes resourceId: Int) {
        disconnectedIcon = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the icon when the product is connected
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setConnectedIcon(@DrawableRes resourceId: Int) {
        connectedIcon = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the connectivity icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setConnectivityIconBackground(@DrawableRes resourceId: Int) {
        connectivityImageView.setBackgroundResource(resourceId)
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.ConnectionWidget).use { typedArray ->
            typedArray.getDrawableAndUse(R.styleable.ConnectionWidget_uxsdk_iconBackground) {
                connectivityIconBackground = it
            }
            typedArray.getDrawableAndUse(R.styleable.ConnectionWidget_uxsdk_connectedIcon) {
                connectedIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.ConnectionWidget_uxsdk_disconnectedIcon) {
                disconnectedIcon = it
            }
        }
    }
    //endregion

    //region Hooks

    /**
     * Get the [ModelState] updates
     */
    @SuppressWarnings
    override fun getWidgetStateUpdate(): Flowable<ModelState> {
        return super.getWidgetStateUpdate()
    }

    /**
     * Class defines the widget state updates
     */
    sealed class ModelState {
        /**
         * Product connection update
         */
        data class ProductConnected(val isConnected: Boolean) : ModelState()
    }
    //endregion
}