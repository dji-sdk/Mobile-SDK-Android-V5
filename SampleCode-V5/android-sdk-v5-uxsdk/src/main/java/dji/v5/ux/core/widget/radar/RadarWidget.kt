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
import android.util.AttributeSet
import androidx.annotation.*
import dji.v5.utils.common.LogUtils
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.GlobalPreferenceKeys
import dji.v5.ux.core.communication.GlobalPreferencesInterface
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.widget.radar.RadarWidget.ModelState
import dji.v5.ux.core.widget.radar.RadarWidget.ModelState.*

private const val TAG = "RadarWidget"
private const val ASPECT_RATIO = 17f / 8

/**
 * Shows the current state of the radar sensors. The forward and backward sensors each have four
 * sectors represented by [ObstacleDetectionSector], and the left and right sensors each have
 * a single sector. Each sector displays an icon according to its
 * [ObstacleDetectionSectorWarning] level.
 *
 * Each radar section has its own distance indicator which uses the unit set in the UNIT_MODE
 * global preferences [GlobalPreferencesInterface.unitType] and the
 * [GlobalPreferenceKeys.UNIT_TYPE] UX Key and defaults to m if
 * nothing is set.
 *
 * Additionally, an icon appears in the center when the upwards sensor detects an obstacle.
 *
 * This widget has a fixed aspect ratio of 17:8.
 */
open class RadarWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayoutWidget<ModelState>(context, attrs, defStyleAttr) {
    private val tag = LogUtils.getTag("RadarWidget")
//    //region Fields
//    private var radarSections: Array<RadarSectionViewHolder?> = arrayOfNulls(4)
//    private var upwardObstacle: ImageView = findViewById(R.id.imageview_upward_obstacle)
//    private var soundDisposable: Disposable? = null
//
//    private val widgetModel: RadarWidgetModel by lazy {
//        RadarWidgetModel(DJISDKModel.getInstance(),
//                ObservableInMemoryKeyedStore.getInstance(),
//                GlobalPreferencesManager.getInstance())
//    }
//
//    /**
//     * The images for the forward radar section. The images will be overlapped to form the
//     * sections of the radar. An array of level-list [Drawable] objects with levels from 0-5 is
//     * recommended.
//     */
//    var forwardRadarImages: Array<Drawable?> = arrayOf(
//            getDrawable(R.drawable.uxsdk_ic_radar_forward_0),
//            getDrawable(R.drawable.uxsdk_ic_radar_forward_1),
//            getDrawable(R.drawable.uxsdk_ic_radar_forward_2),
//            getDrawable(R.drawable.uxsdk_ic_radar_forward_3))
//        set(value) {
//            field = value
//            radarSections[VisionSensorPosition.NOSE.value()]?.setImages(value)
//        }
//
//    /**
//     * The images for the backward radar section. The images will be overlapped to form the
//     * sections of the radar. An array of level-list [Drawable] objects with levels from 0-5 is
//     * recommended.
//     */
//    var backwardRadarImages: Array<Drawable?> = arrayOf(
//            getDrawable(R.drawable.uxsdk_ic_radar_backward_0),
//            getDrawable(R.drawable.uxsdk_ic_radar_backward_1),
//            getDrawable(R.drawable.uxsdk_ic_radar_backward_2),
//            getDrawable(R.drawable.uxsdk_ic_radar_backward_3))
//        set(value) {
//            field = value
//            radarSections[VisionSensorPosition.TAIL.value()]?.setImages(value)
//        }
//
//    /**
//     * The image for the left radar section. A level-list [Drawable] objects with levels from 0-1
//     * is recommended.
//     */
//    var leftRadarImage: Drawable? = getDrawable(R.drawable.uxsdk_ic_radar_left)
//        set(value) {
//            field = value
//            radarSections[VisionSensorPosition.LEFT.value()]?.setImages(arrayOf(value))
//        }
//
//    /**
//     * The image for the right radar section. A level-list [Drawable] objects with levels from 0-1
//     * is recommended.
//     */
//    var rightRadarImage: Drawable? = getDrawable(R.drawable.uxsdk_ic_radar_right)
//        set(value) {
//            field = value
//            radarSections[VisionSensorPosition.RIGHT.value()]?.setImages(arrayOf(value))
//        }
//
//    /**
//     * The text color for the distance text views
//     */
//    @get:ColorInt
//    @setparam:ColorInt
//    var distanceTextColor: Int
//        get() = radarSections[0]?.distanceTextColor ?: getColor(R.color.uxsdk_white)
//        set(color) {
//            radarSections.forEach { it?.distanceTextColor = color }
//        }
//
//    /**
//     * The text color state list of the distance text views
//     */
//    var distanceTextColors: ColorStateList?
//        get() = radarSections[0]?.distanceTextColors
//        set(colors) {
//            radarSections.forEach { it?.distanceTextColors = colors }
//        }
//
//    /**
//     * The background of the distance text views
//     */
//    var distanceTextBackground: Drawable?
//        get() = radarSections[0]?.distanceTextBackground
//        set(background) {
//            radarSections.forEach { it?.distanceTextBackground = background }
//        }
//
//    /**
//     * The text size of the distance text views
//     */
//    @get:Dimension
//    @setparam:Dimension
//    var distanceTextSize: Float
//        get() = radarSections[0]?.distanceTextSize ?: 13f
//        set(textSize) {
//            radarSections.forEach { it?.distanceTextSize = textSize }
//        }
//
//    /**
//     * The drawable resource for the arrow icons. The given icon should be pointed up, and each
//     * radar direction's icon will be rotated to point the corresponding direction.
//     */
//    var distanceArrowIcon: Drawable?
//        get() = radarSections[0]?.distanceArrowIcon
//        set(icon) {
//            radarSections.forEach { it?.distanceArrowIcon = icon }
//        }
//
//    /**
//     * The drawable resource for the arrow icon's background
//     */
//    var distanceArrowIconBackground: Drawable?
//        get() = radarSections[0]?.distanceArrowIconBackground
//        set(background) {
//            radarSections.forEach { it?.distanceArrowIconBackground = background }
//        }
//
//    /**
//     * The drawable resource for the upward obstacle icon
//     */
//    var upwardObstacleIcon: Drawable?
//        get() = upwardObstacle.drawable
//        set(icon) {
//            upwardObstacle.setImageDrawable(icon)
//        }
//
//    /**
//     * The drawable resource for the upward obstacle icon's background
//     */
//    var upwardObstacleIconBackground: Drawable?
//        get() = upwardObstacle.background
//        set(background) {
//            upwardObstacle.background = background
//        }
//    //endregion
//
//    //region Constructor
//    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
//        View.inflate(context, R.layout.uxsdk_widget_radar, this)
//    }
//
//    init {
//        val forwardIds = intArrayOf(R.id.imageview_radar_forward_0,
//                R.id.imageview_radar_forward_1,
//                R.id.imageview_radar_forward_2,
//                R.id.imageview_radar_forward_3)
//
//        val backwardIds = intArrayOf(R.id.imageview_radar_backward_0,
//                R.id.imageview_radar_backward_1,
//                R.id.imageview_radar_backward_2,
//                R.id.imageview_radar_backward_3)
//
//        if(!isInEditMode){
//            radarSections[VisionSensorPosition.NOSE.value()] =
//                MultiAngleRadarSectionViewHolder(forwardIds,
//                    R.id.textview_forward_distance,
//                    R.id.imageview_forward_arrow, this)
//
//            radarSections[VisionSensorPosition.TAIL.value()] =
//                MultiAngleRadarSectionViewHolder(backwardIds,
//                    R.id.textview_backward_distance,
//                    R.id.imageview_backward_arrow, this)
//
//            radarSections[VisionSensorPosition.LEFT.value()] =
//                SingleAngleRadarSectionViewHolder(R.id.imageview_radar_left,
//                    R.id.textview_left_distance,
//                    R.id.imageview_left_arrow, this)
//
//            radarSections[VisionSensorPosition.RIGHT.value()] =
//                SingleAngleRadarSectionViewHolder(R.id.imageview_radar_right,
//                    R.id.textview_right_distance,
//                    R.id.imageview_right_arrow, this)
//        }
//
//        attrs?.let { initAttributes(context, it) }
//    }
//    //endregion
//
//    //region Lifecycle
//    override fun onAttachedToWindow() {
//        super.onAttachedToWindow()
//        if (!isInEditMode) {
//            widgetModel.setup()
//        }
//    }
//
//    override fun onDetachedFromWindow() {
//        if (!isInEditMode) {
//            widgetModel.cleanup()
//        }
//        soundDisposable?.dispose()
//        soundDisposable = null
//        super.onDetachedFromWindow()
//    }
//
//    override fun reactToModelChanges() {
//        addReaction(reactToUpdateRadarSections())
//        addReaction(widgetModel.ascentLimitedByObstacle
//                .observeOn(SchedulerProvider.ui())
//                .subscribe { updateUpwardRadar(it) })
//        addReaction(widgetModel.obstacleAvoidanceLevel
//                .observeOn(SchedulerProvider.ui())
//                .subscribe { playAlertSound(it) })
//        addReaction(widgetModel.isRadarEnabled
//                .observeOn(SchedulerProvider.ui())
//                .subscribe { updateRadarEnabled(it) })
//        addReaction(widgetModel.productConnection
//                .observeOn(SchedulerProvider.ui())
//                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
//    }
//
//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
//        var width = MeasureSpec.getSize(widthMeasureSpec)
//        var height = MeasureSpec.getSize(heightMeasureSpec)
//        if (height * ASPECT_RATIO < width) {
//            width = (height * ASPECT_RATIO).toInt()
//        } else {
//            height = (width / ASPECT_RATIO).toInt()
//        }
//        setMeasuredDimension(View.resolveSize(width, widthMeasureSpec), View.resolveSize(height, heightMeasureSpec))
//    }
//    //endregion
//
//    //region Reactions
//    private fun reactToUpdateRadarSections(): Disposable {
//        return Flowable.combineLatest(widgetModel.visionDetectionState, widgetModel.unitType,
//                BiFunction { first: VisionDetectionState, second: UnitConversionUtil.UnitType -> Pair(first, second) })
//                .observeOn(SchedulerProvider.ui())
//                .subscribe(Consumer { values: Pair<VisionDetectionState, UnitConversionUtil.UnitType> -> updateRadarSections(values.first, values.second) },
//                        RxUtil.logErrorConsumer(TAG, "reactToUpdateRadarSections: "))
//    }
//
//    private fun updateRadarSections(state: VisionDetectionState, unitType: UnitConversionUtil.UnitType) {
//        val sectors = state.detectionSectors
//        if (state.position.value() < radarSections.size) {
//            val radarSection = radarSections[state.position.value()]
//            val unit: String = if (unitType == UnitConversionUtil.UnitType.IMPERIAL) {
//                getString(R.string.uxsdk_unit_feet)
//            } else {
//                getString(R.string.uxsdk_unit_meters)
//            }
//            radarSection?.setDistance(state.obstacleDistanceInMeters, unit)
//            radarSection?.setSectors(sectors, unit)
//            when (state.position) {
//                VisionSensorPosition.NOSE -> widgetStateDataProcessor.onNext(NoseStateUpdated(state))
//                VisionSensorPosition.TAIL -> widgetStateDataProcessor.onNext(TailStateUpdated(state))
//                VisionSensorPosition.LEFT -> widgetStateDataProcessor.onNext(LeftStateUpdated(state))
//                VisionSensorPosition.RIGHT -> widgetStateDataProcessor.onNext(RightStateUpdated(state))
//                else -> {
//                    // Do nothing
//                }
//            }
//        }
//    }
//
//    private fun updateUpwardRadar(isAscentLimitedByObstacle: Boolean) {
//        upwardObstacle.visibility = if (isAscentLimitedByObstacle) View.VISIBLE else View.GONE
//        widgetStateDataProcessor.onNext(AscentLimitedUpdated(isAscentLimitedByObstacle))
//    }
//
//    private fun updateRadarEnabled(isRadarEnabled: Boolean) {
//        if (!isRadarEnabled) {
//            radarSections.forEach {
//                it?.hide()
//            }
//        }
//        widgetStateDataProcessor.onNext(RadarEnabled(isRadarEnabled))
//    }
//
//    private fun playAlertSound(avoidanceLevel: ObstacleAvoidanceLevel) {
//        if (avoidanceLevel != ObstacleAvoidanceLevel.NONE) {
//            if (soundDisposable == null) {
//                soundDisposable = Observable.interval(1, java.util.concurrent.TimeUnit.SECONDS)
//                        .subscribeOn(SchedulerProvider.io())
//                        .observeOn(SchedulerProvider.ui())
//                        .subscribe { AudioUtil.playSound(context, getSoundId(avoidanceLevel), true) }
//            }
//        } else {
//            soundDisposable?.dispose()
//            soundDisposable = null
//        }
//    }
//
//    @RawRes
//    private fun getSoundId(avoidAlertLevel: ObstacleAvoidanceLevel): Int {
//        return when (avoidAlertLevel) {
//            ObstacleAvoidanceLevel.LEVEL_1 -> R.raw.uxsdk_radar_beep_1000
//            ObstacleAvoidanceLevel.LEVEL_2 -> R.raw.uxsdk_radar_beep_500
//            ObstacleAvoidanceLevel.LEVEL_3 -> R.raw.uxsdk_radar_beep_250
//            else -> 0
//        }
//    }
//    //endregion
//
//    //region Customization
//    override fun getIdealDimensionRatioString(): String {
//        return getString(R.string.uxsdk_widget_radar_ratio)
//    }
//
//    /**
//     * Set the warning level ranges for the specified product models.
//     *
//     * @param models      The product models for which these level ranges apply.
//     * @param levelRanges An array where each number represents the maximum distance in meters for
//     * the corresponding warning level. For example [70, 4, 2] would indicate
//     * that warning level LEVEL_1 has the range (4-70], warning level LEVEL_2
//     * has the range (2,4], and warning level LEVEL_3 has the range [0,2].
//     * A distance with a value above the largest number in the array will have
//     * the warning level INVALID.
//     */
//    fun setWarningLevelRanges(models: Array<ProductType>, levelRanges: FloatArray) {
//        widgetModel.setWarningLevelRanges(models, levelRanges)
//    }
//
//    /**
//     * Set the resource IDs for the forward radar section. The images will be overlapped to form the
//     * sections of the radar.
//     *
//     * @param resourceIds An array of level-list resource IDs with levels from 0-5.
//     */
//    fun setForwardRadarImages(resourceIds: IntArray) {
//        forwardRadarImages = resourceIds.map { resourceId: Int ->
//            getDrawable(resourceId)
//        }.toTypedArray()
//    }
//
//    /**
//     * Set the resource IDs for the backward radar section. The images will be overlapped to form the
//     * sections of the radar.
//     *
//     * @param resourceIds An array of level-list resource IDs with levels from 0-5.
//     */
//    fun setBackwardRadarImages(resourceIds: IntArray) {
//        backwardRadarImages = resourceIds.map { resourceId: Int ->
//            getDrawable(resourceId)
//        }.toTypedArray()
//    }
//
//    /**
//     * Set the resource ID for the left radar section.
//     *
//     * @param resourceId A level-list resource ID with levels from 0-1.
//     */
//    fun setLeftRadarImage(@DrawableRes resourceId: Int) {
//        leftRadarImage = getDrawable(resourceId)
//    }
//
//    /**
//     * Set the resource ID for the right radar section.
//     *
//     * @param resourceId A level-list resource ID with levels from 0-1.
//     */
//    fun setRightRadarImage(@DrawableRes resourceId: Int) {
//        leftRadarImage = getDrawable(resourceId)
//    }
//
//    /**
//     * Set text appearance of the distance text views
//     *
//     * @param textAppearanceResId Style resource for text appearance
//     */
//    fun setDistanceTextAppearance(@StyleRes textAppearanceResId: Int) {
//        radarSections.forEach { it?.setDistanceTextAppearance(context, textAppearanceResId) }
//    }
//
//    /**
//     * Set the resource ID for the background of the distance text views
//     *
//     * @param resourceId Integer ID of the text view's background resource
//     */
//    fun setDistanceTextBackground(@DrawableRes resourceId: Int) {
//        radarSections.forEach { it?.setDistanceTextBackground(resourceId) }
//    }
//
//    /**
//     * Set the resource ID for the arrow icons. The given icon should be pointed up, and each
//     * radar direction's icon will be rotated to point the corresponding direction.
//     *
//     * @param resourceId Integer ID of the drawable resource
//     */
//    fun setDistanceArrowIcon(@DrawableRes resourceId: Int) {
//        radarSections.forEach { it?.setDistanceArrowIcon(resourceId) }
//    }
//
//    /**
//     * Set the resource ID for the arrow icon's background
//     *
//     * @param resourceId Integer ID of the icon's background resource
//     */
//    fun setDistanceArrowIconBackground(@DrawableRes resourceId: Int) {
//        radarSections.forEach { it?.setDistanceArrowIconBackground(resourceId) }
//    }
//
//    /**
//     * Set the resource ID for the upward obstacle icon
//     *
//     * @param resourceId Integer ID of the drawable resource
//     */
//    fun setUpwardObstacleIcon(@DrawableRes resourceId: Int) {
//        upwardObstacleIcon = getDrawable(resourceId)
//    }
//
//    /**
//     * Set the resource ID for the upward obstacle icon's background
//     *
//     * @param resourceId Integer ID of the icon's background resource
//     */
//    fun setUpwardObstacleIconBackground(@DrawableRes resourceId: Int) {
//        upwardObstacle.setBackgroundResource(resourceId)
//    }
//
//    @SuppressLint("Recycle")
//    private fun initAttributes(context: Context, attrs: AttributeSet) {
//        context.obtainStyledAttributes(attrs, R.styleable.RadarWidget).use { typedArray ->
//            typedArray.getDrawableArrayAndUse(R.styleable.RadarWidget_uxsdk_forwardImages) {
//                forwardRadarImages = it
//            }
//            typedArray.getDrawableArrayAndUse(R.styleable.RadarWidget_uxsdk_backwardImages) {
//                backwardRadarImages = it
//            }
//            typedArray.getDrawableAndUse(R.styleable.RadarWidget_uxsdk_leftImage) {
//                leftRadarImage = it
//            }
//            typedArray.getDrawableAndUse(R.styleable.RadarWidget_uxsdk_rightImage) {
//                rightRadarImage = it
//            }
//            typedArray.getDrawableAndUse(R.styleable.RadarWidget_uxsdk_distanceArrowIcon) {
//                distanceArrowIcon = it
//            }
//            typedArray.getDrawableAndUse(R.styleable.RadarWidget_uxsdk_distanceArrowIconBackground) {
//                distanceArrowIconBackground = it
//            }
//            typedArray.getDrawableAndUse(R.styleable.RadarWidget_uxsdk_upwardObstacleIcon) {
//                upwardObstacleIcon = it
//            }
//            typedArray.getDrawableAndUse(R.styleable.RadarWidget_uxsdk_upwardObstacleIconBackground) {
//                upwardObstacleIconBackground = it
//            }
//            typedArray.getDrawableAndUse(R.styleable.RadarWidget_uxsdk_distanceTextBackground) {
//                distanceTextBackground = it
//            }
//            typedArray.getResourceIdAndUse(R.styleable.RadarWidget_uxsdk_distanceTextAppearance) {
//                setDistanceTextAppearance(it)
//            }
//            typedArray.getColorAndUse(R.styleable.RadarWidget_uxsdk_distanceTextColor) {
//                distanceTextColor = it
//            }
//            typedArray.getDimensionAndUse(R.styleable.RadarWidget_uxsdk_distanceTextSize) {
//                distanceTextSize = DisplayUtil.pxToSp(context, it)
//            }
//        }
//    }
//    //endregion
//
//    //region Hooks
//    /**
//     * Class defines the widget state updates
//     */
//    sealed class ModelState {
//        /**
//         * Product connection update
//         */
//        data class ProductConnected(val isConnected: Boolean) : ModelState()
//
//        /**
//         * Ascent limited update
//         */
//        data class AscentLimitedUpdated(val isAscentLimited: Boolean) : ModelState()
//
//        /**
//         * Radar Enabled
//         */
//        data class RadarEnabled(val isRadarEnabled: Boolean) : ModelState()
//
//        /**
//         * Nose state update
//         */
//        data class NoseStateUpdated(val noseState: VisionDetectionState) : ModelState()
//
//        /**
//         * Tail state update
//         */
//        data class TailStateUpdated(val tailState: VisionDetectionState) : ModelState()
//
//        /**
//         * Right state update
//         */
//        data class RightStateUpdated(val rightState: VisionDetectionState) : ModelState()
//
//        /**
//         * Left state update
//         */
//        data class LeftStateUpdated(val leftState: VisionDetectionState) : ModelState()
//
//    }
    //endregion

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
//        LogUtils.d(tag,"TODO Method not implemented yet")
    }

    override fun reactToModelChanges() {
//        LogUtils.d(tag,"TODO Method not implemented yet")
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    sealed class ModelState
}