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

package dji.v5.ux.flight.takeoff

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.content.res.use
import dji.v5.utils.common.DisplayUtil
import io.reactivex.rxjava3.core.Flowable
import dji.v5.ux.core.base.SchedulerProvider
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.UXSDKError
import dji.v5.ux.core.base.widget.IconButtonWidget
import dji.v5.ux.core.base.widget.IconButtonWidget.UIState.*
import dji.v5.ux.core.communication.GlobalPreferencesManager
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.extension.*
import dji.v5.ux.core.ui.SlidingDialog
import dji.v5.ux.core.util.RxUtil
import dji.v5.ux.core.util.UnitConversionUtil
import dji.v5.ux.R
import dji.v5.ux.flight.takeoff.TakeOffWidget.ModelState
import dji.v5.ux.flight.takeoff.TakeOffWidget.ModelState.*
import dji.v5.ux.flight.takeoff.TakeOffWidget.DialogType.*
import dji.v5.ux.flight.takeoff.TakeOffWidgetModel.TakeOffLandingState
import java.text.DecimalFormat

private const val TAG = "TakeOffWidget"

/**
 * A button that performs actions related to takeoff and landing. There are three possible states
 * for the widget: ready to take off, ready to land, and landing in progress. Clicking the
 * button in each of these states will open a dialog to confirm take off, landing, and landing
 * cancellation, respectively.
 *
 * Additionally, this widget will show a dialog if landing is in progress, but it is currently
 * unsafe to land. The dialog will prompt the user whether or not they want to cancel landing.
 */
open class TakeOffWidget @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : IconButtonWidget<ModelState>(context, attrs, defStyleAttr) {
    //region Fields
    private var slidingDialog: SlidingDialog? = null
    private val decimalFormat = DecimalFormat("#.#")
    private var dialogType: DialogType? = null

    private val widgetModel by lazy {
        TakeOffWidgetModel(
                DJISDKModel.getInstance(),
                ObservableInMemoryKeyedStore.getInstance(),
                GlobalPreferencesManager.getInstance())
    }

    /**
     * The alpha of the image when the widget is disabled or pressed
     */
    var disabledAlpha = 0.38f

    /**
     * The alpha of the image when the widget is enabled
     */
    var enabledAlpha = 1.0f

    /**
     * The theme of the dialogs
     */
    @get:StyleRes
    @setparam:StyleRes
    var dialogTheme = R.style.UXSDKTakeOffDialogTheme
        set(value) {
            field = value
            initDialog()
        }

    /**
     * Takeoff icon drawable
     */
    var takeOffActionIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_takeoff)
        set(drawable) {
            field = drawable
            checkAndUpdateTakeOffLandingState()
        }

    /**
     * Land action icon
     */
    var landActionIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_land)
        set(drawable) {
            field = drawable
            checkAndUpdateTakeOffLandingState()
        }

    /**
     * Cancel land action icon drawable
     */
    var cancelLandActionIcon: Drawable? = getDrawable(R.drawable.uxsdk_cancel_landing_selector)
        set(drawable) {
            field = drawable
            checkAndUpdateTakeOffLandingState()
        }

    /**
     * Takeoff dialog icon drawable
     */
    var takeOffDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_takeoff_yellow)

    /**
     * Landing dialog icon drawable
     */
    var landingDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_land_yellow)

    /**
     * Landing Confirmation dialog icon drawable
     */
    var landingConfirmationDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_land_yellow)

    /**
     * Unsafe To Land dialog icon drawable
     */
    var unsafeToLandDialogIcon: Drawable? = getDrawable(R.drawable.uxsdk_ic_land_yellow)

    /**
     * The text size of the title for all the dialogs shown by this widget
     */
    var dialogTitleTextSize: Float
        @Dimension
        get() = slidingDialog?.dialogTitleTextSize
                ?: DisplayUtil.pxToSp(context, getDimension(R.dimen.uxsdk_text_size_normal_medium))
        set(@Dimension textSize) {
            slidingDialog?.dialogTitleTextSize = textSize
        }

    /**
     * The color of the title for all the dialogs shown by this widget
     */
    @ColorInt
    var dialogTitleTextColor: Int = getColor(R.color.uxsdk_yellow)

    /**
     * The background of the title for all the dialogs shown by this widget
     */
    var dialogTitleBackground: Drawable?
        get() = slidingDialog?.dialogTitleBackground
        set(value) {
            slidingDialog?.dialogTitleBackground = value
        }

    /**
     * The text size of the message for all the dialogs shown by this widget
     */
    var dialogMessageTextSize: Float = DisplayUtil.pxToSp(context, getDimension(R.dimen.uxsdk_text_size_normal_medium))

    /**
     * The text size of the precision takeoff message for the takeoff dialog
     */
    var dialogPrecisionMessageTextSize: Float = DisplayUtil.pxToSp(context, getDimension(R.dimen.uxsdk_text_size_small))

    /**
     * The text color of the message for all the dialogs shown by this widget
     */
    var dialogMessageTextColor: Int
        @ColorInt
        get() = slidingDialog?.dialogMessageTextColor ?: getColor(R.color.uxsdk_white)
        set(@ColorInt color) {
            slidingDialog?.dialogMessageTextColor = color
        }

    /**
     * The background of the message for all the dialogs shown by this widget
     */
    var dialogMessageBackground: Drawable?
        get() = slidingDialog?.dialogMessageBackground
        set(value) {
            slidingDialog?.dialogMessageBackground = value
        }

    /**
     * The text size of the precision takeoff checkbox for the takeoff dialog
     */
    var dialogCheckBoxMessageTextSize: Float
        @Dimension
        get() = slidingDialog?.checkBoxMessageTextSize
                ?: DisplayUtil.pxToSp(context, getDimension(R.dimen.uxsdk_text_size_normal_medium))
        set(@Dimension textSize) {
            slidingDialog?.checkBoxMessageTextSize = textSize
        }

    /**
     * The text color of the precision takeoff checkbox for the takeoff dialog
     */
    var dialogCheckBoxMessageTextColor: Int
        @ColorInt
        get() = slidingDialog?.checkBoxMessageTextColor ?: getColor(R.color.uxsdk_white)
        set(@ColorInt color) {
            slidingDialog?.checkBoxMessageTextColor = color
        }

    /**
     * The background of the precision takeoff checkbox for the takeoff dialog
     */
    var dialogCheckBoxMessageBackground: Drawable?
        get() = slidingDialog?.checkBoxMessageBackground
        set(value) {
            slidingDialog?.checkBoxMessageBackground = value
        }

    /**
     * The text size of the cancel button for all the dialogs shown by this widget
     */
    var dialogCancelTextSize: Float
        @Dimension
        get() = slidingDialog?.cancelTextSize
                ?: DisplayUtil.pxToSp(context, getDimension(R.dimen.uxsdk_text_size_normal_medium))
        set(@Dimension textSize) {
            slidingDialog?.cancelTextSize = textSize
        }

    /**
     * The text color of the cancel button for all the dialogs shown by this widget
     */
    var dialogCancelTextColor: Int
        @ColorInt
        get() = slidingDialog?.cancelTextColor ?: getColor(R.color.uxsdk_white)
        set(@ColorInt color) {
            slidingDialog?.cancelTextColor = color
        }

    /**
     * The text colors of the cancel button for all the dialogs shown by this widget
     */
    var dialogCancelTextColors: ColorStateList?
        get() = slidingDialog?.cancelTextColors
                ?: ColorStateList.valueOf(getColor(R.color.uxsdk_white))
        set(colors) {
            slidingDialog?.cancelTextColors = colors
        }

    /**
     * The background of the cancel button for all the dialogs shown by this widget
     */
    var dialogCancelBackground: Drawable?
        get() = slidingDialog?.cancelBackground
        set(value) {
            slidingDialog?.cancelBackground = value
        }

    /**
     * The text size of the slider message for all the dialogs shown by this widget
     */
    var dialogSliderMessageTextSize: Float
        @Dimension
        get() = slidingDialog?.actionMessageTextSize
                ?: DisplayUtil.pxToSp(context, getDimension(R.dimen.uxsdk_text_size_normal))
        set(@Dimension textSize) {
            slidingDialog?.actionMessageTextSize = textSize
        }

    /**
     * The text color of the slider message for all the dialogs shown by this widget
     */
    var dialogSliderMessageTextColor: Int
        @ColorInt
        get() = slidingDialog?.actionMessageTextColor
                ?: getColor(R.color.uxsdk_slider_text)
        set(@ColorInt color) {
            slidingDialog?.actionMessageTextColor = color
        }

    /**
     * The background of the slider message for all the dialogs shown by this widget
     */
    var dialogSliderMessageBackground: Drawable?
        get() = slidingDialog?.actionMessageBackground
        set(value) {
            slidingDialog?.actionMessageBackground = value
        }

    /**
     * The icon to the right of the slider message for all the dialogs shown by this widget
     */
    var dialogSliderIcon: Drawable?
        get() = slidingDialog?.actionIcon
        set(icon) {
            slidingDialog?.actionIcon = icon
        }

    /**
     * The color of the slider thumb for all the dialogs shown by this widget
     */
    var dialogSliderThumbColor: Int
        @ColorInt
        get() = slidingDialog?.actionSliderThumbColor ?: getColor(R.color.uxsdk_white)
        set(@ColorInt color) {
            slidingDialog?.actionSliderThumbColor = color
        }

    /**
     * The color of the slider thumb when selected for all the dialogs shown by this widget
     */
    var dialogSliderThumbSelectedColor: Int
        @ColorInt
        get() = slidingDialog?.actionSliderThumbSelectedColor
                ?: getColor(R.color.uxsdk_slider_thumb_selected)
        set(@ColorInt color) {
            slidingDialog?.actionSliderThumbSelectedColor = color
        }

    /**
     * The fill color of the slider for all the dialogs shown by this widget
     */
    var dialogSliderFillColor: Int
        @ColorInt
        get() = slidingDialog?.actionSliderFillColor ?: getColor(R.color.uxsdk_slider_filled)
        set(@ColorInt color) {
            slidingDialog?.actionSliderFillColor = color
        }

    /**
     * The background of all the dialogs shown by this widget
     */
    var dialogBackground: Drawable?
        get() = slidingDialog?.background
                ?: getDrawable(R.drawable.uxsdk_background_black_rectangle)
        set(background) {
            slidingDialog?.background = background
        }

    //endregion

    //region Lifecycle
    init {
        setOnClickListener(this)
        initDialog()
        attrs?.let { initAttributes(context, it) }
    }

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
        addReaction(widgetModel.takeOffLandingState
                .observeOn(SchedulerProvider.ui())
                .subscribe { updateTakeOffStatus(it) })
        addReaction(widgetModel.productConnection
                .observeOn(SchedulerProvider.ui())
                .subscribe { widgetStateDataProcessor.onNext(ProductConnected(it)) })
    }

    override fun onClick(view: View?) {
        super.onClick(view)
        val current = foregroundImageView.drawable
        current?.let {
            when (it) {
                cancelLandActionIcon -> performCancelLandAction()
                landActionIcon -> showLandingDialog()
                else -> showTakeOffDialog()
            }
        }
    }

    //endregion

    //region private helpers
    private fun initDialog() {
        if (!isInEditMode) {
            slidingDialog = SlidingDialog(context, dialogTheme)
            slidingDialog?.setOnEventListener(object : SlidingDialog.OnEventListener {

                var checkBoxChecked: Boolean = false

                override fun onCancelClick(dialog: DialogInterface?) {
                    slidingDialog?.dismiss()
                    addDisposable(widgetModel.takeOffLandingState.firstOrError()
                            .observeOn(SchedulerProvider.ui())
                            .subscribe(Consumer { takeOffLandingState: TakeOffLandingState ->
                                if (takeOffLandingState == TakeOffLandingState.WAITING_FOR_LANDING_CONFIRMATION) {
                                    performCancelLandAction()
                                }
                                uiUpdateStateProcessor.onNext(DialogActionCancelled(dialogType))
                            }, RxUtil.logErrorConsumer(TAG, "Update takeoff Landing State")))
                }

                override fun onSlideChecked(dialog: DialogInterface?, checked: Boolean) {
                    if (checked) {
                        performOnSlideChecked(checkBoxChecked)
                    }
                }

                override fun onCheckBoxChecked(dialog: DialogInterface?, checked: Boolean) {
                    uiUpdateStateProcessor.onNext(DialogCheckboxCheckChanged(dialogType))
                    checkBoxChecked = checked
                    updateTakeOffDialogMessage()
                }
            })

            slidingDialog?.setOnDismissListener { uiUpdateStateProcessor.onNext(DialogDismissed(dialogType)) }

        }
    }

    private fun performOnSlideChecked(checkBoxChecked: Boolean) {
        val current = foregroundImageView.drawable
        if (current == landActionIcon) {
            addDisposable(performLandingAction())
        } else if (current == cancelLandActionIcon) {
            addDisposable(performLandingConfirmationAction())
        } else {
            if (checkBoxChecked) {
                addDisposable(performPrecisionTakeOffAction())
            } else {
                addDisposable(performTakeOffAction())
            }
        }
        slidingDialog?.dismiss()
        uiUpdateStateProcessor.onNext(DialogActionConfirmed(dialogType))
    }

    private fun performTakeOffAction(): Disposable {
        return widgetModel.performTakeOffAction()
                .subscribe({
                    widgetStateDataProcessor.onNext(TakeOffStartSucceeded)
                }) { error: Throwable ->
                    if (error is UXSDKError) {
                        widgetStateDataProcessor.onNext(TakeOffStartFailed(error))
                    }
                }
    }

    private fun performPrecisionTakeOffAction(): Disposable {
        return widgetModel.performPrecisionTakeOffAction()
                .subscribe({
                    widgetStateDataProcessor.onNext(PrecisionTakeOffStartSucceeded)
                }) { error: Throwable ->
                    if (error is UXSDKError) {
                        widgetStateDataProcessor.onNext(PrecisionTakeOffStartFailed(error))
                    }
                }
    }

    private fun performLandingAction(): Disposable {
        return widgetModel.performLandingAction()
                .subscribe({
                    widgetStateDataProcessor.onNext(LandingStartSucceeded)
                }) { error: Throwable ->
                    if (error is UXSDKError) {
                        widgetStateDataProcessor.onNext(LandingStartFailed(error))
                    }
                }
    }

    private fun performLandingConfirmationAction(): Disposable {
        return widgetModel.performLandingConfirmationAction()
                .subscribe({
                    widgetStateDataProcessor.onNext(LandingConfirmSucceeded)
                }) { error: Throwable ->
                    if (error is UXSDKError) {
                        widgetStateDataProcessor.onNext(LandingConfirmFailed(error))
                    }
                }
    }

    private fun performCancelLandAction(): Disposable {
        return widgetModel.performCancelLandingAction()
                .observeOn(SchedulerProvider.ui())
                .subscribe({
                    slidingDialog?.dismiss()
                    widgetStateDataProcessor.onNext(LandingCancelSucceeded)
                }) { error: Throwable ->
                    if (error is UXSDKError) {
                        widgetStateDataProcessor.onNext(LandingCancelFailed(error))
                    }
                }
    }

    private fun showDialog() {
        slidingDialog?.let {
            it.setDialogTitleTextColor(dialogTitleTextColor)
                    .setDialogMessageTextSize(
                            if (it.checkBoxChecked) {
                                dialogPrecisionMessageTextSize
                            } else {
                                dialogMessageTextSize
                            })
                    .show()
        }
    }

    private fun showTakeOffDialog() {
        slidingDialog?.setDialogTitleRes(R.string.uxsdk_take_off_header)
                ?.setActionMessageRes(R.string.uxsdk_take_off_action)
                ?.setDialogIcon(takeOffDialogIcon)
                ?.setCheckBoxMessageRes(R.string.uxsdk_precision_takeoff)
        showDialog()
        uiUpdateStateProcessor.onNext(DialogDisplayed(TakeOff))
        dialogType = TakeOff

        updateCheckBoxVisibility()
        updateTakeOffDialogMessage()
    }

    private fun updateCheckBoxVisibility() {
//        addDisposable(widgetModel.isPrecisionTakeoffSupported
//                .observeOn(SchedulerProvider.ui())
//                .subscribe(Consumer {
//                    slidingDialog?.setCheckBoxVisibility(it)
//                    if (!it) {
//                        slidingDialog?.setCheckBoxChecked(false)
//                    }
//                }, RxUtil.logErrorConsumer(TAG, "Update Precision Takeoff Check Box ")))
        slidingDialog?.setCheckBoxChecked(true)
    }

    private fun updateTakeOffDialogMessage() {
        if (slidingDialog?.checkBoxChecked == true) {
            val takeOffHeightString = getHeightString(widgetModel.precisionTakeOffHeight)
            slidingDialog?.setDialogMessage(resources.getString(R.string.uxsdk_precision_takeoff_message,
                            takeOffHeightString))
                    ?.setDialogMessageTextSize(dialogPrecisionMessageTextSize)
        } else {
            addDisposable(widgetModel.isInAttiMode.firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(Consumer { isInAttiMode: Boolean ->
                        val takeOffHeightString = getHeightString(widgetModel.takeOffHeight)
                        slidingDialog?.setDialogMessage(
                                        if (isInAttiMode) {
                                            resources.getString(R.string.uxsdk_take_off_atti_message, takeOffHeightString)
                                        } else {
                                            resources.getString(R.string.uxsdk_take_off_message, takeOffHeightString)
                                        })
                                ?.setDialogMessageTextSize(dialogMessageTextSize)
                    }, RxUtil.logErrorConsumer(TAG, "Update Takeoff Message ")))
        }
    }

    private fun getHeightString(height: TakeOffWidgetModel.Height): String {
        val resourceString =
                if (height.unitType == UnitConversionUtil.UnitType.IMPERIAL) {
                    R.string.uxsdk_value_feet
                } else {
                    R.string.uxsdk_value_meters
                }
        return resources.getString(resourceString, decimalFormat.format(height.height))
    }

    private fun showLandingDialog() {
        slidingDialog?.setDialogTitleRes(R.string.uxsdk_land_header)
                ?.setDialogMessageRes(R.string.uxsdk_land_message)
                ?.setDialogIcon(landingDialogIcon)
                ?.setActionMessageRes(R.string.uxsdk_land_action)
                ?.setCheckBoxVisibility(false)
                ?.setCheckBoxChecked(false)
        showDialog()
        uiUpdateStateProcessor.onNext(DialogDisplayed(Landing))
        dialogType = Landing
    }

    private fun showLandingConfirmationDialog() {
        slidingDialog?.setDialogTitleRes(R.string.uxsdk_land_confirmation_header)
                ?.setDialogIcon(landingConfirmationDialogIcon)
                ?.setActionMessageRes(R.string.uxsdk_land_action)
                ?.setCheckBoxVisibility(false)
                ?.setCheckBoxChecked(false)
        showDialog()
        uiUpdateStateProcessor.onNext(DialogDisplayed(LandingConfirmation))
        dialogType = LandingConfirmation

        updateLandingConfirmationDialogMessage()
    }

    private fun updateLandingConfirmationDialogMessage() {
        val landHeightString = getHeightString(widgetModel.landHeight)
        slidingDialog?.setDialogMessage(resources.getString(R.string.uxsdk_land_confirmation_message, landHeightString))
    }

    private fun showUnsafeToLandDialog() {
        slidingDialog?.setDialogTitleRes(R.string.uxsdk_unsafe_to_land_header)
                ?.setDialogMessageRes(R.string.uxsdk_unsafe_to_land_message)
                ?.setDialogIcon(unsafeToLandDialogIcon)
                ?.setActionMessageRes(R.string.uxsdk_unsafe_to_land_action)
                ?.setCheckBoxVisibility(false)
                ?.setCheckBoxChecked(false)
        showDialog()
        uiUpdateStateProcessor.onNext(DialogDisplayed(UnsafeToLand))
        dialogType = UnsafeToLand
    }

    private fun updateTakeOffStatus(takeOffLandingState: TakeOffLandingState) {
        widgetStateDataProcessor.onNext(TakeOffLandingStateUpdated(takeOffLandingState))
        foregroundImageView.setImageDrawable(
                when (takeOffLandingState) {
                    TakeOffLandingState.READY_TO_TAKE_OFF,
                    TakeOffLandingState.TAKE_OFF_DISABLED -> takeOffActionIcon
                    TakeOffLandingState.READY_TO_LAND,
                    TakeOffLandingState.LAND_DISABLED -> landActionIcon
                    TakeOffLandingState.AUTO_LANDING,
                    TakeOffLandingState.FORCED_AUTO_LANDING,
                    TakeOffLandingState.WAITING_FOR_LANDING_CONFIRMATION,
                    TakeOffLandingState.UNSAFE_TO_LAND -> cancelLandActionIcon
                    else -> null
                })
        isEnabled = !(takeOffLandingState == TakeOffLandingState.TAKE_OFF_DISABLED ||
                takeOffLandingState == TakeOffLandingState.LAND_DISABLED ||
                takeOffLandingState == TakeOffLandingState.FORCED_AUTO_LANDING)
        visibility =
                if (takeOffLandingState == TakeOffLandingState.RETURNING_TO_HOME ||
                        takeOffLandingState == TakeOffLandingState.DISCONNECTED) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
        updateImageAlpha()

        when (takeOffLandingState) {
            TakeOffLandingState.UNSAFE_TO_LAND -> showUnsafeToLandDialog()
            TakeOffLandingState.WAITING_FOR_LANDING_CONFIRMATION -> showLandingConfirmationDialog()
            TakeOffLandingState.READY_TO_TAKE_OFF,
            TakeOffLandingState.TAKE_OFF_DISABLED -> slidingDialog?.dismiss()
        }
    }

    private fun updateImageAlpha() {
        if (foregroundImageView.imageDrawable == cancelLandActionIcon) {
            foregroundImageView.isEnabled = isEnabled
            if (isPressed) {
                foregroundImageView.alpha = disabledAlpha
            } else {
                foregroundImageView.alpha = enabledAlpha
            }
            return
        }

        if ((isPressed || isFocused) || !isEnabled) {
            foregroundImageView.alpha = disabledAlpha
        } else {
            foregroundImageView.alpha = enabledAlpha
        }
    }

    private fun checkAndUpdateTakeOffLandingState() {
        if (!isInEditMode) {
            addDisposable(widgetModel.takeOffLandingState.firstOrError()
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(Consumer { this.updateTakeOffStatus(it) },
                            RxUtil.logErrorConsumer(TAG, "Update Take Off Landing State ")))
        }
    }

    override fun checkAndUpdateIconColor() {
        // do nothing
    }

    @SuppressLint("Recycle")
    private fun initAttributes(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.TakeOffWidget).use { typedArray ->
            typedArray.getFloatAndUse(R.styleable.TakeOffWidget_uxsdk_disabledAlpha) {
                disabledAlpha = it
            }
            typedArray.getFloatAndUse(R.styleable.TakeOffWidget_uxsdk_enabledAlpha) {
                enabledAlpha = it
            }
            typedArray.getResourceIdAndUse(R.styleable.TakeOffWidget_uxsdk_dialogTheme) {
                dialogTheme = it
            }
            typedArray.getDrawableAndUse(R.styleable.TakeOffWidget_uxsdk_takeOffActionDrawable) {
                takeOffActionIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.TakeOffWidget_uxsdk_landActionDrawable) {
                landActionIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.TakeOffWidget_uxsdk_cancelLandActionDrawable) {
                cancelLandActionIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.TakeOffWidget_uxsdk_takeOffDialogIcon) {
                takeOffDialogIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.TakeOffWidget_uxsdk_landingDialogIcon) {
                landingDialogIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.TakeOffWidget_uxsdk_landingConfirmationDialogIcon) {
                landingConfirmationDialogIcon = it
            }
            typedArray.getDrawableAndUse(R.styleable.TakeOffWidget_uxsdk_unsafeToLandDialogIcon) {
                unsafeToLandDialogIcon = it
            }
            typedArray.getResourceIdAndUse(R.styleable.TakeOffWidget_uxsdk_dialogTitleTextAppearance) {
                setDialogTitleTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.TakeOffWidget_uxsdk_dialogTitleTextSize) {
                dialogTitleTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.TakeOffWidget_uxsdk_dialogTitleTextColor) {
                dialogTitleTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.TakeOffWidget_uxsdk_dialogTitleBackground) {
                dialogTitleBackground = it
            }
            typedArray.getResourceIdAndUse(R.styleable.TakeOffWidget_uxsdk_dialogMessageTextAppearance) {
                setDialogMessageTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.TakeOffWidget_uxsdk_dialogMessageTextSize) {
                dialogMessageTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getDimensionAndUse(R.styleable.TakeOffWidget_uxsdk_dialogPrecisionMessageTextSize) {
                dialogPrecisionMessageTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.TakeOffWidget_uxsdk_dialogMessageTextColor) {
                dialogMessageTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.TakeOffWidget_uxsdk_dialogMessageBackground) {
                dialogMessageBackground = it
            }
            typedArray.getResourceIdAndUse(R.styleable.TakeOffWidget_uxsdk_dialogCheckBoxMessageTextAppearance) {
                setDialogCheckBoxMessageTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.TakeOffWidget_uxsdk_dialogCheckBoxMessageTextSize) {
                dialogCheckBoxMessageTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.TakeOffWidget_uxsdk_dialogCheckBoxMessageTextColor) {
                dialogCheckBoxMessageTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.TakeOffWidget_uxsdk_dialogCheckBoxMessageBackground) {
                dialogCheckBoxMessageBackground = it
            }
            typedArray.getResourceIdAndUse(R.styleable.TakeOffWidget_uxsdk_dialogCancelTextAppearance) {
                setDialogCancelTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.TakeOffWidget_uxsdk_dialogCancelTextSize) {
                dialogCancelTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.TakeOffWidget_uxsdk_dialogCancelTextColor) {
                dialogCancelTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.TakeOffWidget_uxsdk_dialogCancelBackground) {
                dialogCancelBackground = it
            }
            typedArray.getResourceIdAndUse(R.styleable.TakeOffWidget_uxsdk_dialogSliderMessageTextAppearance) {
                setDialogSliderMessageTextAppearance(it)
            }
            typedArray.getDimensionAndUse(R.styleable.TakeOffWidget_uxsdk_dialogSliderMessageTextSize) {
                dialogSliderMessageTextSize = DisplayUtil.pxToSp(context, it)
            }
            typedArray.getColorAndUse(R.styleable.TakeOffWidget_uxsdk_dialogSliderMessageTextColor) {
                dialogSliderMessageTextColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.TakeOffWidget_uxsdk_dialogSliderMessageBackground) {
                dialogSliderMessageBackground = it
            }
            typedArray.getDrawableAndUse(R.styleable.TakeOffWidget_uxsdk_dialogSliderIcon) {
                dialogSliderIcon = it
            }
            typedArray.getColorAndUse(R.styleable.TakeOffWidget_uxsdk_dialogSliderThumbColor) {
                dialogSliderThumbColor = it
            }
            typedArray.getColorAndUse(R.styleable.TakeOffWidget_uxsdk_dialogSliderThumbSelectedColor) {
                dialogSliderThumbSelectedColor = it
            }
            typedArray.getColorAndUse(R.styleable.TakeOffWidget_uxsdk_dialogSliderFillColor) {
                dialogSliderFillColor = it
            }
            typedArray.getDrawableAndUse(R.styleable.TakeOffWidget_uxsdk_dialogBackground) {
                dialogBackground = it
            }
        }
    }
    //endregion

    //region Customizations
    /**
     * Set takeoff icon drawable
     *
     * @param resourceId resource id of takeoff icon
     */
    fun setTakeOffActionIcon(@DrawableRes resourceId: Int) {
        takeOffActionIcon = getDrawable(resourceId)
    }

    /**
     * Set land action icon drawable
     *
     * @param resourceId resource id of land action icon
     */
    fun setLandActionIcon(@DrawableRes resourceId: Int) {
        landActionIcon = getDrawable(resourceId)
    }

    /**
     * Set cancel land action icon drawable
     *
     * @param resourceId resource id of cancel land action
     */
    fun setCancelLandActionIcon(@DrawableRes resourceId: Int) {
        cancelLandActionIcon = getDrawable(resourceId)
    }

    /**
     * Set takeoff dialog icon drawable
     *
     * @param resourceId resource id of takeoff dialog icon
     */
    fun setTakeOffDialogIcon(@DrawableRes resourceId: Int) {
        takeOffDialogIcon = getDrawable(resourceId)
    }

    /**
     * Set landing dialog icon drawable
     *
     * @param resourceId resource id of landing dialog icon
     */
    fun setLandingDialogIcon(@DrawableRes resourceId: Int) {
        landingDialogIcon = getDrawable(resourceId)
    }

    /**
     * Set landing confirmation dialog icon drawable
     *
     * @param resourceId resource id of landing confirmation dialog icon
     */
    fun setLandingConfirmationDialogIcon(@DrawableRes resourceId: Int) {
        landingConfirmationDialogIcon = getDrawable(resourceId)
    }

    /**
     * Set unsafe to land dialog icon drawable
     *
     * @param resourceId resource id of unsafe to land dialog icon
     */
    fun setUnsafeToLandDialogIcon(@DrawableRes resourceId: Int) {
        unsafeToLandDialogIcon = getDrawable(resourceId)
    }

    /**
     * Set the text appearance of the title for all the dialogs shown by this widget
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setDialogTitleTextAppearance(@StyleRes textAppearance: Int) {
        slidingDialog?.setDialogTitleTextAppearance(textAppearance)
    }

    /**
     * Set the text appearance of the message for all the dialogs shown by this widget
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setDialogMessageTextAppearance(@StyleRes textAppearance: Int) {
        slidingDialog?.setDialogMessageTextAppearance(textAppearance)
    }

    /**
     * Set the text appearance of the precision checkbox message for the takeoff dialog
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setDialogCheckBoxMessageTextAppearance(@StyleRes textAppearance: Int) {
        slidingDialog?.setCheckBoxMessageTextAppearance(textAppearance)
    }

    /**
     * Set the text appearance of the cancel button for all the dialogs shown by this widget
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setDialogCancelTextAppearance(@StyleRes textAppearance: Int) {
        slidingDialog?.setCancelTextAppearance(textAppearance)
    }

    /**
     * Set the text appearance of the slider message for all the dialogs shown by this widget
     *
     * @param textAppearance Style resource for text appearance
     */
    fun setDialogSliderMessageTextAppearance(@StyleRes textAppearance: Int) {
        slidingDialog?.setActionMessageTextAppearance(textAppearance)
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
         * Takeoff Landing State update
         */
        data class TakeOffLandingStateUpdated(val state: TakeOffLandingState) : ModelState()

        /**
         * Takeoff started successfully
         */
        object TakeOffStartSucceeded : ModelState()

        /**
         * Takeoff not started due to error
         */
        data class TakeOffStartFailed(val error: UXSDKError) : ModelState()

        /**
         * Precision Takeoff started successfully
         */
        object PrecisionTakeOffStartSucceeded : ModelState()

        /**
         * Precision Takeoff not started due to error
         */
        data class PrecisionTakeOffStartFailed(val error: UXSDKError) : ModelState()

        /**
         * Landing started successfully
         */
        object LandingStartSucceeded : ModelState()

        /**
         * Landing not started due to error
         */
        data class LandingStartFailed(val error: UXSDKError) : ModelState()

        /**
         * Landing confirmed successfully
         */
        object LandingConfirmSucceeded : ModelState()

        /**
         * Landing not confirmed due to error
         */
        data class LandingConfirmFailed(val error: UXSDKError) : ModelState()

        /**
         * Landing canceled successfully
         */
        object LandingCancelSucceeded : ModelState()

        /**
         * Landing not canceled due to error
         */
        data class LandingCancelFailed(val error: UXSDKError) : ModelState()
    }

    /**
     * The type of dialog shown
     */
    sealed class DialogType {

        /**
         * The takeoff dialog, which is shown when the widget is clicked and the aircraft is ready
         * to take off.
         */
        object TakeOff : DialogType()

        /**
         * The landing dialog, which is shown when the widget is clicked and the aircraft is ready
         * to land.
         */
        object Landing : DialogType()

        /**
         * The landing confirmation dialog, which is shown when the aircraft has paused
         * auto-landing and is waiting for confirmation before continuing.
         */
        object LandingConfirmation : DialogType()

        /**
         * The unsafe to land dialog, which is shown when the aircraft is auto-landing and has
         * determined it is unsafe to land.
         */
        object UnsafeToLand : DialogType()
    }

    //endregion
}