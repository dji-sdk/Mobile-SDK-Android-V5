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

package dji.v5.ux.core.widget.vision

import android.content.Context
import android.util.AttributeSet
import android.view.View
import dji.v5.ux.core.base.widget.FrameLayoutWidget
import dji.v5.ux.core.widget.vision.VisionWidget.ModelState
import dji.v5.ux.core.widget.vision.VisionWidget.ModelState.*

/**
 * Shows the current state of the vision system. There are two different vision systems that are
 * used by different aircraft. Older aircraft have three states that indicate whether the system is
 * enabled and working correctly. Newer aircraft such as the Mavic 2 and Mavic 2 Enterprise have an
 * omnidirectional vision system, which means they use the statuses that begin with "OMNI" to
 * indicate which directions are enabled and working correctly.
 */
open class VisionWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayoutWidget<ModelState>(
    context,
    attrs,
    defStyleAttr
), View.OnClickListener {
//    //region Fields
//    private val visionIconImageView: ImageView = findViewById(R.id.imageview_vision_icon)
//    private val visionMap: MutableMap<VisionWidgetModel.VisionSystemState, Drawable?> =
//            mutableMapOf(
//                    VisionWidgetModel.VisionSystemState.NORMAL to getDrawable(R.drawable.uxsdk_ic_topbar_visual_normal),
//                    VisionWidgetModel.VisionSystemState.CLOSED to getDrawable(R.drawable.uxsdk_ic_topbar_visual_closed),
//                    VisionWidgetModel.VisionSystemState.DISABLED to getDrawable(R.drawable.uxsdk_ic_topbar_visual_error),
//                    VisionWidgetModel.VisionSystemState.OMNI_ALL to getDrawable(R.drawable.uxsdk_ic_avoid_normal_all),
//                    VisionWidgetModel.VisionSystemState.OMNI_FRONT_BACK to getDrawable(R.drawable.uxsdk_ic_avoid_normal_front_back),
//                    VisionWidgetModel.VisionSystemState.OMNI_HORIZONTAL to getDrawable(R.drawable.uxsdk_ic_omni_perception_horizontal),
//                    VisionWidgetModel.VisionSystemState.OMNI_VERTICAL to getDrawable(R.drawable.uxsdk_ic_omni_perception_vertical),
//                    VisionWidgetModel.VisionSystemState.OMNI_DISABLED to getDrawable(R.drawable.uxsdk_ic_avoid_disable_all),
//                    VisionWidgetModel.VisionSystemState.OMNI_CLOSED to getDrawable(R.drawable.uxsdk_ic_avoid_disable_all))
//    private val uiUpdateStateProcessor: PublishProcessor<UIState> = PublishProcessor.create()
//
//    private val widgetModel by lazy {
//        VisionWidgetModel(DJISDKModel.getInstance(),
//                ObservableInMemoryKeyedStore.getInstance())
//    }
//
//    /**
//     * The color of the vision icon when the product is disconnected
//     */
//    @get:ColorInt
//    var disconnectedStateIconColor = getColor(R.color.uxsdk_gray_58)
//        set(@ColorInt value) {
//            field = value
//            checkAndUpdateIconColor()
//        }
//
//    /**
//     * Background drawable resource for the vision icon
//     */
//    var iconBackground: Drawable?
//        get() = visionIconImageView.background
//        set(value) {
//            visionIconImageView.background = value
//        }
//    //endregion
//
//    //region Constructor
//    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
//        View.inflate(context, R.layout.uxsdk_widget_vision, this)
//    }
//
//    init {
//        setOnClickListener(this)
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
//        super.onDetachedFromWindow()
//    }
//
//    override fun onClick(v: View?) {
//        uiUpdateStateProcessor.onNext(UIState.WidgetClicked)
//    }
//
//    override fun reactToModelChanges() {
//        addReaction(widgetModel.visionSystemState
//                .observeOn(SchedulerProvider.ui())
//                .subscribe { updateIcon(it) })
//        addReaction(widgetModel.isUserAvoidanceEnabled
//                .observeOn(SchedulerProvider.ui())
//                .subscribe { sendWarningMessage(it) })
//        addReaction(widgetModel.isVisionSupportedByProduct
//                .observeOn(SchedulerProvider.ui())
//                .subscribe { updateVisibility(it) })
//        addReaction(widgetModel.productConnection
//                .observeOn(SchedulerProvider.ui())
//                .subscribe { updateIconColor(it) })
//    }
//    //endregion
//
//    //region Reactions
//    private fun updateIcon(visionSystemState: VisionWidgetModel.VisionSystemState) {
//        visionIconImageView.setImageDrawable(visionMap[visionSystemState])
//        widgetStateDataProcessor.onNext(VisionSystemStateUpdated(visionSystemState))
//    }
//
//    private fun sendWarningMessage(isUserAvoidanceEnabled: Boolean) {
//        addDisposable(widgetModel.sendWarningMessage(getString(R.string.uxsdk_visual_radar_avoidance_disabled_message_post),
//                isUserAvoidanceEnabled)
//                .subscribe())
//        widgetStateDataProcessor.onNext(UserAvoidanceEnabledUpdated(isUserAvoidanceEnabled))
//    }
//
//    private fun updateVisibility(isVisionSupported: Boolean) {
//        visibility = if (isVisionSupported) View.VISIBLE else View.GONE
//        uiUpdateStateProcessor.onNext(VisibilityUpdated(isVisionSupported))
//    }
//
//    private fun updateIconColor(isConnected: Boolean) {
//        widgetStateDataProcessor.onNext(ProductConnected(isConnected))
//        if (isConnected) {
//            visionIconImageView.clearColorFilter()
//        } else {
//            visionIconImageView.setColorFilter(disconnectedStateIconColor, PorterDuff.Mode.SRC_IN)
//        }
//    }
//
//    private fun checkAndUpdateIcon() {
//        if (!isInEditMode) {
//            addDisposable(widgetModel.visionSystemState.firstOrError()
//                    .observeOn(SchedulerProvider.ui())
//                    .subscribe(Consumer { this.updateIcon(it) }, RxUtil.logErrorConsumer(TAG, "Update Icon ")))
//        }
//    }
//
//    private fun checkAndUpdateIconColor() {
//        if (!isInEditMode) {
//            addDisposable(widgetModel.productConnection.firstOrError()
//                    .observeOn(SchedulerProvider.ui())
//                    .subscribe(Consumer { this.updateIconColor(it) }, RxUtil.logErrorConsumer(TAG, "Update Icon Color ")))
//        }
//    }
//    //endregion
//
//    //region Customization
//    override fun getIdealDimensionRatioString(): String {
//        return getString(R.string.uxsdk_widget_default_ratio)
//    }
//
//    /**
//     * Sets the icon to the given image when the [VisionWidgetModel.VisionSystemState] is the
//     * given value.
//     *
//     * @param state     The state at which the icon will change to the given image.
//     * @param resourceId The id of the image the icon will change to.
//     */
//    fun setVisionIcon(state: VisionWidgetModel.VisionSystemState, @DrawableRes resourceId: Int) {
//        setVisionIcon(state, getDrawable(resourceId))
//    }
//
//    /**
//     * Sets the icon to the given image when the [VisionWidgetModel.VisionSystemState] is the
//     * given value.
//     *
//     * @param state   The state at which the icon will change to the given image.
//     * @param drawable The image the icon will change to.
//     */
//    fun setVisionIcon(state: VisionWidgetModel.VisionSystemState, drawable: Drawable?) {
//        visionMap[state] = drawable
//        checkAndUpdateIcon()
//    }
//
//    /**
//     * Gets the image that the icon will change to when the [VisionWidgetModel.VisionSystemState]
//     * is the given value.
//     *
//     * @param state The state at which the icon will change.
//     * @return The image the icon will change to for the given state.
//     */
//    fun getVisionIcon(state: VisionWidgetModel.VisionSystemState): Drawable? {
//        return visionMap[state]
//    }
//
//    /**
//     * Set the resource ID for the vision icon's background
//     *
//     * @param resourceId Integer ID of the background resource
//     */
//    fun setIconBackground(@DrawableRes resourceId: Int) {
//        iconBackground = getDrawable(resourceId)
//    }
//
//    //Initialize all customizable attributes
//    @SuppressLint("Recycle")
//    private fun initAttributes(context: Context, attrs: AttributeSet) {
//        context.obtainStyledAttributes(attrs, R.styleable.VisionWidget).use { typedArray ->
//            typedArray.getDrawableAndUse(R.styleable.VisionWidget_uxsdk_normalVisionIcon) {
//                setVisionIcon(VisionWidgetModel.VisionSystemState.NORMAL, it)
//            }
//            typedArray.getDrawableAndUse(R.styleable.VisionWidget_uxsdk_closedVisionIcon) {
//                setVisionIcon(VisionWidgetModel.VisionSystemState.CLOSED, it)
//            }
//            typedArray.getDrawableAndUse(R.styleable.VisionWidget_uxsdk_disabledVisionIcon) {
//                setVisionIcon(VisionWidgetModel.VisionSystemState.DISABLED, it)
//            }
//            typedArray.getDrawableAndUse(R.styleable.VisionWidget_uxsdk_omniAllVisionIcon) {
//                setVisionIcon(VisionWidgetModel.VisionSystemState.OMNI_ALL, it)
//            }
//            typedArray.getDrawableAndUse(R.styleable.VisionWidget_uxsdk_omniFrontBackVisionIcon) {
//                setVisionIcon(VisionWidgetModel.VisionSystemState.OMNI_FRONT_BACK, it)
//            }
//            typedArray.getDrawableAndUse(R.styleable.VisionWidget_uxsdk_omniClosedVisionIcon) {
//                setVisionIcon(VisionWidgetModel.VisionSystemState.OMNI_CLOSED, it)
//            }
//            typedArray.getDrawableAndUse(R.styleable.VisionWidget_uxsdk_omniDisabledVisionIcon) {
//                setVisionIcon(VisionWidgetModel.VisionSystemState.OMNI_DISABLED, it)
//            }
//            typedArray.getDrawableAndUse(R.styleable.VisionWidget_uxsdk_visionIconBackground) {
//                iconBackground = it
//            }
//            disconnectedStateIconColor = typedArray.getColor(R.styleable.VisionWidget_uxsdk_disconnectedStateIconColor,
//                    disconnectedStateIconColor)
//        }
//    }
//    //endregion
//
//    //region Hooks
//    /**
//     * Get the [UIState] updates
//     */
//    fun getUIStateUpdates(): Flowable<UIState> {
//        return uiUpdateStateProcessor.onBackpressureBuffer()
//    }
//
//    /**
//     * Get the [ModelState] updates
//     */
//    @SuppressWarnings
//    override fun getWidgetStateUpdate(): Flowable<ModelState> {
//        return super.getWidgetStateUpdate()
//    }

    /**
     * Widget UI update State
     */
    sealed class UIState {
        /**
         * Widget click update
         */
        object WidgetClicked : UIState()

        /**
         * Is vision supported by product update
         */
        data class VisibilityUpdated(val isVisible: Boolean) : UIState()
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
         * Vision system status update
         */
//        data class VisionSystemStateUpdated(val visionSystemState: VisionWidgetModel.VisionSystemState) :
//            ModelState()

        /**
         * Is user avoidance enabled update
         */
        data class UserAvoidanceEnabledUpdated(val isUserAvoidanceEnabled: Boolean) : ModelState()

    }
    //endregion

    companion object {
        private const val TAG = "VisionWidget"
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        //暂无实现
    }

    override fun reactToModelChanges() {
        //暂无实现
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    override fun onClick(v: View?) {
        //暂无实现
    }
}