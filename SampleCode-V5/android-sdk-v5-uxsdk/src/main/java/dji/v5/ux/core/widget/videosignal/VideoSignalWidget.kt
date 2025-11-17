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

package dji.v5.ux.core.widget.videosignal

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Pair
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.*
import androidx.annotation.IntRange
import androidx.core.content.res.use
import dji.sdk.keyvalue.value.airlink.FrequencyBand
import dji.v5.utils.common.DisplayUtil
import dji.v5.utils.common.SDRLinkHelper
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.widget.videosignal.VideoSignalWidget.ModelState.*
import dji.v5.ux.core.util.UxErrorHandle

/**
 * This widget shows the strength of the video signal between the
 * aircraft and the app through the RC.
 */
open class VideoSignalWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<VideoSignalWidget.ModelState>(context, attrs, defStyleAttr) {

    //region Fields
    private val videoIconImageView: ImageView = findViewById(R.id.imageview_video_icon)
    private val videoSignalImageView: ImageView = findViewById(R.id.imageview_video_signal)
    private val frequencyBandTextView: TextView = findViewById(R.id.textview_frequency_band)
    private val widgetModel: VideoSignalWidgetModel by lazy {
        VideoSignalWidgetModel(
            DJISDKModel.getInstance(),
            ObservableInMemoryKeyedStore.getInstance()
        )
    }

    /**
     * The color of the video display icon when the product is connected
     */
    @get:ColorInt
    var connectedStateIconColor: Int = getColor(R.color.uxsdk_white)
        set(@ColorInt value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * The color of the video display icon when the product is disconnected
     */
    @get:ColorInt
    var disconnectedStateIconColor: Int = getColor(R.color.uxsdk_gray_58)
        set(@ColorInt value) {
            field = value
            checkAndUpdateIconColor()
        }

    /**
     * Icon for video
     */
    var videoIcon: Drawable?
        get() = videoIconImageView.imageDrawable
        set(value) {
            videoIconImageView.imageDrawable = value
        }

    /**
     * Background of the video icon
     */
    var videoIconBackground: Drawable?
        get() = videoIconImageView.background
        set(value) {
            videoIconImageView.background = value
        }

    /**
     * Icon for the video signal strength
     */
    var videoSignalIcon: Drawable?
        get() = videoSignalImageView.imageDrawable
        set(value) {
            videoSignalImageView.imageDrawable = value
        }

    /**
     * Background of the video signal strength icon
     */
    var videoSignalIconBackground: Drawable?
        get() = videoSignalImageView.background
        set(value) {
            videoSignalImageView.background = value
        }

    /**
     * ColorStateList for the frequency band text
     */
    var textColors: ColorStateList?
        get() = frequencyBandTextView.textColorStateList
        set(value) {
            frequencyBandTextView.textColorStateList = value
        }

    /**
     * Color for the frequency band text
     */
    var textColor: Int
        @ColorInt
        get() = frequencyBandTextView.textColor
        set(@ColorInt value) {
            frequencyBandTextView.textColor = value
        }

    /**
     * Size for the frequency band text
     */
    var textSize: Float
        @Dimension
        get() = frequencyBandTextView.textSize
        set(@Dimension textSize) {
            frequencyBandTextView.textSize = textSize
        }

    /**
     * Background for the frequency band text
     */
    var textBackground: Drawable?
        get() = frequencyBandTextView.background
        set(drawable) {
            frequencyBandTextView.background = drawable
        }
    //endregion

    //region Constructor
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_widget_video_signal, this)
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
            .subscribe { this.updateIconColor(it) })
        addReaction(widgetModel.videoSignalQuality
            .observeOn(SchedulerProvider.ui())
            .subscribe { this.updateVideoSignal(it) })
        addReaction(widgetModel.airlinkFrequencyBand
            .observeOn(SchedulerProvider.ui())
            .subscribe { this.updateWifiFrequencyBandText(it) })
        addReaction(reactToOcuSyncFrequencyStateChange())

    }
    //endregion

    //region Reactions to model
    private fun updateVideoSignal(@IntRange(from = 0, to = 100) videoSignalQuality: Int) {
        widgetStateDataProcessor.onNext(VideoSignalQualityUpdated(videoSignalQuality))
        videoSignalImageView.setImageLevel(videoSignalQuality)
    }

    private fun updateIconColor(isConnected: Boolean) {
        widgetStateDataProcessor.onNext(ProductConnected(isConnected))
        if (isConnected) {
            videoIconImageView.setColorFilter(connectedStateIconColor, PorterDuff.Mode.SRC_IN)
        } else {
            videoIconImageView.setColorFilter(disconnectedStateIconColor, PorterDuff.Mode.SRC_IN)
            frequencyBandTextView.text = ""
        }
    }
    //endregion

    //region helpers

    private fun checkAndUpdateIconColor() {
        if (!isInEditMode) {
            addDisposable(widgetModel.productConnection.firstOrError()
                .observeOn(SchedulerProvider.ui())
                .subscribe(Consumer { this.updateIconColor(it) }, UxErrorHandle.logErrorConsumer(TAG, "Update Icon Color "))
            )
        }
    }

    private fun updateWifiFrequencyBandText(frequencyBandType: FrequencyBand) {
        widgetStateDataProcessor.onNext(AirlinkFrequencyBandUpdated(frequencyBandType))
        if (frequencyBandType != FrequencyBand.UNKNOWN) {
            frequencyBandTextView.text = when (frequencyBandType) {
                FrequencyBand.BAND_2_DOT_4G -> FREQUENCY_BAND_2_DOT_4_GHZ
                FrequencyBand.BAND_1_DOT_4G -> FREQUENCY_BAND_2_DOT_4_GHZ
                FrequencyBand.BAND_MULTI -> FREQUENCY_BAND_5_GHZ
                else -> ""
            }
        }
    }

    private fun reactToOcuSyncFrequencyStateChange(): Disposable {
        return Flowable.combineLatest(
            widgetModel.airlinkFrequencyBand,
            widgetModel.ocuSyncFrequencyPointIndex,
            { ocuSyncFrequencyBand, signalQuality -> Pair.create(ocuSyncFrequencyBand, signalQuality) })
            .observeOn(SchedulerProvider.ui())
            .subscribe { values ->
                widgetStateDataProcessor.onNext(AirlinkFrequencyBandUpdated(values.first))
                if (values.first != FrequencyBand.UNKNOWN) {
                    frequencyBandTextView.text = when (values.first) {
                        FrequencyBand.BAND_2_DOT_4G -> FREQUENCY_BAND_2_DOT_4_GHZ
                        FrequencyBand.BAND_5_DOT_8G -> FREQUENCY_BAND_5_DOT_8_GHZ
                        FrequencyBand.BAND_MULTI -> getAutoFrequencyBandStr(values.second)
                        else -> ""
                    }
                }

            }
    }

    private fun getAutoFrequencyBandStr(ocuFrequencyPoint: Int): String {
        return if (ocuFrequencyPoint > SDRLinkHelper.ORIGINAL_NF_2DOT4G_START_FREQ && ocuFrequencyPoint < SDRLinkHelper.ORIGINAL_NF_5DOT8G_START_FREQ) {
            FREQUENCY_BAND_2_DOT_4_GHZ
        } else if (ocuFrequencyPoint > SDRLinkHelper.ORIGINAL_NF_5DOT8G_START_FREQ) {
            FREQUENCY_BAND_5_DOT_8_GHZ
        } else {
            ""
        }
    }

    //endregion

    //region Customization
    override fun getIdealDimensionRatioString() = null

    /**
     * Set the resource ID for the video display icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setVideoIcon(@DrawableRes resourceId: Int) {
        videoIcon = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the video display icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setVideoIconBackground(@DrawableRes resourceId: Int) {
        videoIconBackground = getDrawable(resourceId)
    }

    /**
     * Set the resource ID for the video signal icon
     *
     * @param resourceId Integer ID of the drawable resource
     */
    fun setVideoSignalIcon(@DrawableRes resourceId: Int) {
        videoSignalIcon = getDrawable(resourceId)
    }


    /**
     * Set the resource ID for the video signal icon's background
     *
     * @param resourceId Integer ID of the background resource
     */
    fun setVideoSignalIconBackground(@DrawableRes resourceId: Int) {
        videoSignalIconBackground = getDrawable(resourceId)
    }

    /**
     * Set text appearance of the frequency band text view
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setTextAppearance(@StyleRes textAppearance: Int) {
        frequencyBandTextView.setTextAppearance(context, textAppearance)
    }
    //endregion

    //region Customization Helpers
    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.VideoSignalWidget).use { typedArray ->
            typedArray.getDrawableAndUse(R.styleable.VideoSignalWidget_uxsdk_videoIcon) {
                videoIcon = it
            }

            typedArray.getDrawableAndUse(R.styleable.VideoSignalWidget_uxsdk_videoSignalIcon) {
                videoSignalIcon = it
            }

            typedArray.getResourceIdAndUse(R.styleable.VideoSignalWidget_uxsdk_frequencyBandTextAppearance) {
                setTextAppearance(it)
            }

            typedArray.getDimensionAndUse(R.styleable.VideoSignalWidget_uxsdk_frequencyBandTextSize) {
                textSize = DisplayUtil.pxToSp(context, it)
            }

            typedArray.getColorAndUse(R.styleable.VideoSignalWidget_uxsdk_frequencyBandTextColor) {
                textColor = it
            }

            typedArray.getDrawableAndUse(R.styleable.VideoSignalWidget_uxsdk_frequencyBandBackgroundDrawable) {
                textBackground = it
            }

            connectedStateIconColor = typedArray.getColor(
                R.styleable.VideoSignalWidget_uxsdk_connectedStateIconColor,
                connectedStateIconColor
            )
            disconnectedStateIconColor = typedArray.getColor(
                R.styleable.VideoSignalWidget_uxsdk_disconnectedStateIconColor,
                disconnectedStateIconColor
            )
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

        /**
         * Video signal quality / strength update
         */
        data class VideoSignalQualityUpdated(val signalQuality: Int) : ModelState()

        /**
         * WiFi frequency band update
         */
        data class AirlinkFrequencyBandUpdated(val frequencyBandType: FrequencyBand) :
            ModelState()
    }

    //endregion

    companion object {
        private const val TAG = "VideoSignalWidget"
        private const val FREQUENCY_BAND_2_DOT_4_GHZ = "2.4G"
        private const val FREQUENCY_BAND_5_GHZ = "5G"
        private const val FREQUENCY_BAND_5_DOT_7_GHZ = "5.7G"
        private const val FREQUENCY_BAND_5_DOT_8_GHZ = "5.8G"
    }
}
