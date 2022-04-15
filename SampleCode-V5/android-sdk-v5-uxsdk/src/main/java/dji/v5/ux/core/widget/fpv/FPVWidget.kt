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
package dji.v5.ux.core.widget.fpv

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.SurfaceTexture
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.FloatRange
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.res.use
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.video.channel.VideoChannelType
import dji.v5.common.video.decoder.DecoderOutputMode
import dji.v5.common.video.decoder.DecoderState
import dji.v5.common.video.decoder.VideoDecoder
import dji.v5.common.video.interfaces.IVideoDecoder
import dji.v5.common.video.interfaces.StreamDataListener
import dji.v5.common.video.stream.PhysicalDeviceType
import dji.v5.common.video.stream.StreamSource
import dji.v5.utils.common.JsonUtil
import dji.v5.utils.common.LogUtils
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.module.FlatCameraModule
import dji.v5.ux.core.ui.CenterPointView
import dji.v5.ux.core.ui.GridLineView
import dji.v5.ux.core.util.DisplayUtil
import dji.v5.ux.core.util.RxUtil
import dji.v5.ux.core.widget.fpv.FPVWidget.ModelState
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.functions.Consumer

private const val TAG = "FPVWidget"
private const val ADJUST_ASPECT_RATIO_DELAY = 300
private const val ORIGINAL_SCALE = 1f
private const val PORTRAIT_ROTATION_ANGLE = 270
private const val LANDSCAPE_ROTATION_ANGLE = 0

/**
 * This widget shows the video feed from the camera.
 */
open class FPVWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<ModelState>(context, attrs, defStyleAttr),
    SurfaceHolder.Callback {
    private var viewWidth = 0
    private var viewHeight = 0
    private var rotationAngle = 0
    private val fpvSurfaceView: SurfaceView = findViewById(R.id.surface_view_fpv)
    private val cameraNameTextView: TextView = findViewById(R.id.textview_camera_name)
    private val cameraSideTextView: TextView = findViewById(R.id.textview_camera_side)
    private val verticalOffset: Guideline = findViewById(R.id.vertical_offset)
    private val horizontalOffset: Guideline = findViewById(R.id.horizontal_offset)
    private var fpvStateChangeResourceId: Int = INVALID_RESOURCE
    private var videoDecoder: IVideoDecoder? = null
    private var holder: SurfaceHolder? = null

    private val widgetModel: FPVWidgetModel = FPVWidgetModel(
        DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance(), FlatCameraModule()
    )

    var videoChannelType: VideoChannelType = VideoChannelType.PRIMARY_STREAM_CHANNEL
        set(value) {
            field = value
            widgetModel.videoChannelType = value
        }

    /**
     * Whether the video feed source's camera name is visible on the video feed.
     */
    var isCameraSourceNameVisible = true
        set(value) {
            field = value
            checkAndUpdateCameraName()
        }

    /**
     * Whether the video feed source's camera side is visible on the video feed.
     * Only shown on aircraft that support multiple gimbals.
     */
    var isCameraSourceSideVisible = true
        set(value) {
            field = value
            checkAndUpdateCameraSide()
        }

    /**
     * Whether the grid lines are enabled.
     */
    var isGridLinesEnabled = true
        set(isGridLinesEnabled) {
            field = isGridLinesEnabled
            updateGridLineVisibility()
        }

    /**
     * Whether the center point is enabled.
     */
    var isCenterPointEnabled = true
        set(isCenterPointEnabled) {
            field = isCenterPointEnabled
            centerPointView.visibility = if (isCenterPointEnabled) View.VISIBLE else View.GONE
        }

    /**
     * The text color state list of the camera name text view
     */
    var cameraNameTextColors: ColorStateList?
        get() = cameraNameTextView.textColors
        set(colorStateList) {
            cameraNameTextView.setTextColor(colorStateList)
        }

    /**
     * The text color of the camera name text view
     */
    @get:ColorInt
    @setparam:ColorInt
    var cameraNameTextColor: Int
        get() = cameraNameTextView.currentTextColor
        set(color) {
            cameraNameTextView.setTextColor(color)
        }

    /**
     * The text size of the camera name text view
     */
    @get:Dimension
    @setparam:Dimension
    var cameraNameTextSize: Float
        get() = cameraNameTextView.textSize
        set(textSize) {
            cameraNameTextView.textSize = textSize
        }

    /**
     * The background for the camera name text view
     */
    var cameraNameTextBackground: Drawable?
        get() = cameraNameTextView.background
        set(drawable) {
            cameraNameTextView.background = drawable
        }

    /**
     * The text color state list of the camera name text view
     */
    var cameraSideTextColors: ColorStateList?
        get() = cameraSideTextView.textColors
        set(colorStateList) {
            cameraSideTextView.setTextColor(colorStateList)
        }

    /**
     * The text color of the camera side text view
     */
    @get:ColorInt
    @setparam:ColorInt
    var cameraSideTextColor: Int
        get() = cameraSideTextView.currentTextColor
        set(color) {
            cameraSideTextView.setTextColor(color)
        }

    /**
     * The text size of the camera side text view
     */
    @get:Dimension
    @setparam:Dimension
    var cameraSideTextSize: Float
        get() = cameraSideTextView.textSize
        set(textSize) {
            cameraSideTextView.textSize = textSize
        }

    /**
     * The background for the camera side text view
     */
    var cameraSideTextBackground: Drawable?
        get() = cameraSideTextView.background
        set(drawable) {
            cameraSideTextView.background = drawable
        }

    /**
     * The vertical alignment of the camera name and side text views
     */
    var cameraDetailsVerticalAlignment: Float
        @FloatRange(from = 0.0, to = 1.0)
        get() {
            val layoutParams: LayoutParams = verticalOffset.layoutParams as LayoutParams
            return layoutParams.guidePercent
        }
        set(@FloatRange(from = 0.0, to = 1.0) percent) {
            val layoutParams: LayoutParams = verticalOffset.layoutParams as LayoutParams
            layoutParams.guidePercent = percent
            verticalOffset.layoutParams = layoutParams
        }

    /**
     * The horizontal alignment of the camera name and side text views
     */
    var cameraDetailsHorizontalAlignment: Float
        @FloatRange(from = 0.0, to = 1.0)
        get() {
            val layoutParams: LayoutParams = horizontalOffset.layoutParams as LayoutParams
            return layoutParams.guidePercent
        }
        set(@FloatRange(from = 0.0, to = 1.0) percent) {
            val layoutParams: LayoutParams = horizontalOffset.layoutParams as LayoutParams
            layoutParams.guidePercent = percent
            horizontalOffset.layoutParams = layoutParams
        }

    /**
     * The [GridLineView] shown in this widget
     */
    val gridLineView: GridLineView = findViewById(R.id.view_grid_line)

    /**
     * The [CenterPointView] shown in this widget
     */
    val centerPointView: CenterPointView = findViewById(R.id.view_center_point)

    //endregion

    //region Constructor
    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_widget_fpv, this)
    }

    init {
        if (!isInEditMode) {
            fpvSurfaceView.holder.addCallback(this)
            rotationAngle = LANDSCAPE_ROTATION_ANGLE
        }
        attrs?.let { initAttributes(context, it) }
    }
    //endregion

    //region LifeCycle
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
        initializeListeners()
    }

    private fun initializeListeners() {
        //后面补上
    }

    override fun onDetachedFromWindow() {
        destroyListeners()
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        videoDecoder?.destory()
        videoDecoder = null
        super.onDetachedFromWindow()
    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.cameraNameProcessor.toFlowable()
            .observeOn(SchedulerProvider.ui())
            .subscribe { cameraName: String -> updateCameraName(cameraName) })
        addReaction(widgetModel.cameraSideProcessor.toFlowable()
            .observeOn(SchedulerProvider.ui())
            .subscribe { cameraSide: String -> updateCameraSide(cameraSide) })
        addReaction(widgetModel.hasVideoViewChanged
            .observeOn(SchedulerProvider.ui())
            .subscribe { delayCalculator() })
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        this.holder = holder
        if (videoDecoder == null) {
            videoDecoder = VideoDecoder(
                context,
                videoChannelType,
                DecoderOutputMode.SURFACE_MODE,
                holder
            )
        } else if (videoDecoder?.decoderStatus == DecoderState.PAUSED) {
            videoDecoder?.onResume()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        this.holder = holder
        if (videoDecoder == null) {
            videoDecoder = VideoDecoder(
                context,
                videoChannelType,
                DecoderOutputMode.SURFACE_MODE,
                holder
            )
        } else if (videoDecoder?.decoderStatus == DecoderState.PAUSED) {
            videoDecoder?.onResume()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        videoDecoder?.onPause()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (!isInEditMode) {
            setViewDimensions()
            delayCalculator()
        }
    }

    private fun destroyListeners() {
        //后面补上
    }

    //endregion
    //region Customization
    override fun getIdealDimensionRatioString(): String {
        return getString(R.string.uxsdk_widget_fpv_ratio)
    }

    fun updateVideoSource(source: StreamSource, channelType: VideoChannelType) {
        LogUtils.i(logTag, "updateVideoSource", JsonUtil.toJson(source), channelType)
        widgetModel.streamSource = source
        if (videoChannelType != channelType) {
            changeVideoDecoder(channelType)
        }
        videoChannelType = channelType
    }

    fun getStreamSource() = widgetModel.streamSource

    private fun changeVideoDecoder(channel: VideoChannelType) {
        LogUtils.i(logTag, "changeVideoDecoder", channel)
        videoDecoder?.videoChannelType = channel
        fpvSurfaceView.invalidate()
    }

    fun setOnFPVStreamSourceListener(listener: FPVStreamSourceListener) {
        widgetModel.streamSourceListener = listener
    }

    //endregion
    //region Helpers
    private fun setViewDimensions() {
        viewWidth = measuredWidth
        viewHeight = measuredHeight
    }

    /**
     * This method should not to be called until the size of `TextureView` is fixed.
     */
    public fun changeView(width: Int, height: Int, relativeWidth: Int, relativeHeight: Int) {
        val lp = fpvSurfaceView.layoutParams
        lp.width = width
        lp.height = height
        fpvSurfaceView.layoutParams = lp
        if (width > viewWidth) {
            fpvSurfaceView.scaleX = width.toFloat() / viewWidth
        } else {
            fpvSurfaceView.scaleX = ORIGINAL_SCALE
        }
        if (height > viewHeight) {
            fpvSurfaceView.scaleY = height.toFloat() / viewHeight
        } else {
            fpvSurfaceView.scaleY = ORIGINAL_SCALE
        }
        gridLineView.adjustDimensions(relativeWidth, relativeHeight)
    }

    private fun delayCalculator() {
        //后面补充
    }

    private fun updateCameraName(cameraName: String) {
        cameraNameTextView.text = cameraName
        if (cameraName.isNotEmpty() && isCameraSourceNameVisible) {
            cameraNameTextView.visibility = View.VISIBLE
        } else {
            cameraNameTextView.visibility = View.GONE
        }
    }

    private fun updateCameraSide(cameraSide: String) {
        cameraSideTextView.visibility = View.VISIBLE
        cameraSideTextView.text = cameraSide
    }

    private fun checkAndUpdateCameraName() {
        if (!isInEditMode) {
            addDisposable(
                widgetModel.cameraNameProcessor.toFlowable()
                    .firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(
                        Consumer { cameraName: String -> updateCameraName(cameraName) },
                        RxUtil.logErrorConsumer(TAG, "updateCameraName")
                    )
            )
        }
    }

    private fun checkAndUpdateCameraSide() {
        if (!isInEditMode) {
            addDisposable(
                widgetModel.cameraSideProcessor.toFlowable()
                    .firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(
                        Consumer { cameraSide: String -> updateCameraSide(cameraSide) },
                        RxUtil.logErrorConsumer(TAG, "updateCameraSide")
                    )
            )
        }
    }

    private fun updateGridLineVisibility() {
        gridLineView.visibility = if (isGridLinesEnabled
            && widgetModel.streamSource?.physicalDeviceType == PhysicalDeviceType.FPV) View.VISIBLE else View.GONE
    }
    //endregion

    //region Customization helpers
    /**
     * Set text appearance of the camera name text view
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setCameraNameTextAppearance(@StyleRes textAppearance: Int) {
        cameraNameTextView.setTextAppearance(context, textAppearance)
    }

    /**
     * Set text appearance of the camera side text view
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setCameraSideTextAppearance(@StyleRes textAppearance: Int) {
        cameraSideTextView.setTextAppearance(context, textAppearance)
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.FPVWidget).use { typedArray ->
            if (!isInEditMode) {
                typedArray.getIntegerAndUse(R.styleable.FPVWidget_uxsdk_videoChannelType) {
                    videoChannelType = (VideoChannelType.find(it))
                    widgetModel.updateStreamSource()
                }
                typedArray.getBooleanAndUse(R.styleable.FPVWidget_uxsdk_gridLinesEnabled, true) {
                    isGridLinesEnabled = it
                }
                typedArray.getBooleanAndUse(R.styleable.FPVWidget_uxsdk_centerPointEnabled, true) {
                    isCenterPointEnabled = it
                }
            }
            typedArray.getBooleanAndUse(R.styleable.FPVWidget_uxsdk_sourceCameraNameVisibility, true) {
                isCameraSourceNameVisible = it
            }
            typedArray.getBooleanAndUse(R.styleable.FPVWidget_uxsdk_sourceCameraSideVisibility, true) {
                isCameraSourceSideVisible = it
            }
            typedArray.getResourceIdAndUse(R.styleable.FPVWidget_uxsdk_cameraNameTextAppearance) {
                setCameraNameTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.FPVWidget_uxsdk_cameraNameTextSize) {
                cameraNameTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.FPVWidget_uxsdk_cameraNameTextColor) {
                cameraNameTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.FPVWidget_uxsdk_cameraNameBackgroundDrawable) {
                cameraNameTextBackground = it
            }
            typedArray.getResourceIdAndUse(R.styleable.FPVWidget_uxsdk_cameraSideTextAppearance) {
                setCameraSideTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.FPVWidget_uxsdk_cameraSideTextSize) {
                cameraSideTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.FPVWidget_uxsdk_cameraSideTextColor) {
                cameraSideTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.FPVWidget_uxsdk_cameraSideBackgroundDrawable) {
                cameraSideTextBackground = it
            }
            typedArray.getFloatAndUse(R.styleable.FPVWidget_uxsdk_cameraDetailsVerticalAlignment) {
                cameraDetailsVerticalAlignment = it
            }
            typedArray.getFloatAndUse(R.styleable.FPVWidget_uxsdk_cameraDetailsHorizontalAlignment) {
                cameraDetailsHorizontalAlignment = it
            }
            typedArray.getIntegerAndUse(R.styleable.FPVWidget_uxsdk_gridLineType) {
                gridLineView.type = GridLineView.GridLineType.find(it)
            }
            typedArray.getColorAndUse(R.styleable.FPVWidget_uxsdk_gridLineColor) {
                gridLineView.lineColor = it
            }
            typedArray.getFloatAndUse(R.styleable.FPVWidget_uxsdk_gridLineWidth) {
                gridLineView.lineWidth = it
            }
            typedArray.getIntegerAndUse(R.styleable.FPVWidget_uxsdk_gridLineNumber) {
                gridLineView.numberOfLines = it
            }
            typedArray.getIntegerAndUse(R.styleable.FPVWidget_uxsdk_centerPointType) {
                centerPointView.type = CenterPointView.CenterPointType.find(it)
            }
            typedArray.getColorAndUse(R.styleable.FPVWidget_uxsdk_centerPointColor) {
                centerPointView.color = it
            }
            typedArray.getResourceIdAndUse(R.styleable.FPVWidget_uxsdk_onStateChange) {
                fpvStateChangeResourceId = it
            }
        }
    }
    //endregion

    /**
     * The size of the video feed within this widget
     *
     * @property width The width of the video feed within this widget
     * @property height The height of the video feed within this widget
     */
    data class FPVSize(val width: Int, val height: Int)

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
    sealed class ModelState
}