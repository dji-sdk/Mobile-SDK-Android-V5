package dji.v5.ux.cameracore.widget.cameracontrols.exposuresettings

import android.content.Context
import android.util.AttributeSet
import android.view.View
import dji.sdk.keyvalue.value.camera.CameraExposureMode
import dji.sdk.keyvalue.value.camera.CameraExposureSettings
import dji.sdk.keyvalue.value.camera.CameraISO
import dji.sdk.keyvalue.value.camera.EIType
import dji.sdk.keyvalue.value.common.CameraLensType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.ICameraIndex
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.ui.HorizontalSeekBar
import dji.v5.ux.core.util.AudioUtil
import dji.v5.ux.core.util.CameraUtil
import dji.v5.ux.core.util.RxUtil
import io.reactivex.rxjava3.functions.Action
import kotlinx.android.synthetic.main.uxsdk_widget_iso_ei_setting.view.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/11/2
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class ISOAndEISettingWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<ISOAndEISettingWidget.ModelState>(context, attrs, defStyleAttr),
    View.OnClickListener, HorizontalSeekBar.OnSeekBarChangeListener, ICameraIndex {

    private val LOCKED_ISO_VALUE = "500"

    private var isISOAutoSelected = false
    private var isISOAutoSupported = false
    private var isISOSeekBarEnabled = false
    private val isISOLocked = false
    private var isSeekBarTracking = false
    private var uiCameraISO = 0

    //去掉auto
    private var uiIsoValueArray: Array<CameraISO?> = arrayOf()
    private var eiValueArray: IntArray = intArrayOf()

    private val widgetModel by lazy {
        ISOAndEISettingModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        View.inflate(context, R.layout.uxsdk_widget_iso_ei_setting, this)
    }

    override fun reactToModelChanges() {

        // ISO part
        addReaction(widgetModel.exposureModeProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            onExposureModeUpdated(it)
            updateISOEnableStatus()
        })
        addReaction(widgetModel.ISOProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            onISOUpdated(it)
        })
        addReaction(widgetModel.ISORangeProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            updateISORangeValue(it.toTypedArray())
            updateISOEnableStatus()
            updateISORangeUI()
        })
        addReaction(widgetModel.exposureSettingsProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            val exposureParameters = it as CameraExposureSettings
            uiCameraISO = exposureParameters.iso
            updateISORangeUI()
        })

        // EI part
        addReaction(widgetModel.eiValueProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {

        })
        addReaction(widgetModel.eiValueRangeProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {

        })
        addReaction(widgetModel.eiRecommendedValueProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {

        })

        // mode
        addReaction(widgetModel.exposureSensitivityModeProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            updateWidgetUI()
        })
        addReaction(widgetModel.cameraModeProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            updateWidgetUI()
        })
        addReaction(widgetModel.flatCameraModeProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            updateWidgetUI()
        })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Initialize ISO array
        val res = context.resources
        val valueArray = res.getIntArray(R.array.uxsdk_iso_values)
        uiIsoValueArray = arrayOfNulls(valueArray.size)

        if (!isInEditMode) {
            for (i in valueArray.indices) {
                uiIsoValueArray[i] = CameraISO.find(valueArray[i])
            }
            updateISORangeValue(uiIsoValueArray)
        }

        // ISO seekBar
        isISOSeekBarEnabled = false
        seekbar_iso.progress = 0
        seekbar_iso.enable(false)
        seekbar_iso.addOnSeekBarChangeListener(this)
        seekbar_iso.isBaselineVisibility = false
        seekbar_iso.setMinValueVisibility(true)
        seekbar_iso.setMaxValueVisibility(true)
        seekbar_iso.setMinusVisibility(false)
        seekbar_iso.setPlusVisibility(false)
        button_iso_auto.setOnClickListener(this)

        // EI seekBar
        seekbar_ei.addOnSeekBarChangeListener(this)
        seekbar_ei.visibility = GONE
        seekbar_ei.setMinValueVisibility(true)
        seekbar_ei.setMaxValueVisibility(true)
        seekbar_ei.setMinusVisibility(false)
        seekbar_ei.setPlusVisibility(false)

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

    override fun getCameraIndex() = widgetModel.getCameraIndex()

    override fun getLensType() = widgetModel.getLensType()

    override fun updateCameraSource(cameraIndex: ComponentIndexType, lensType: CameraLensType) = widgetModel.updateCameraSource(cameraIndex, lensType)

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    override fun onProgressChanged(view: HorizontalSeekBar, progress: Int, isFromUI: Boolean) {
        if (view == seekbar_iso) {
            if (isISOLocked) {
                seekbar_iso.text = LOCKED_ISO_VALUE
            } else {
                if (uiIsoValueArray.isNotEmpty()) {
                    uiCameraISO = CameraUtil.convertISOToInt(uiIsoValueArray[progress])
                    seekbar_iso.text = uiCameraISO.toString()
                }
            }
        } else {
            if (progress < eiValueArray.size) {
                seekbar_ei.text = eiValueArray[progress].toString()
            }
        }
    }

    override fun onStartTrackingTouch(view: HorizontalSeekBar, progress: Int) {
        isSeekBarTracking = true
    }

    override fun onStopTrackingTouch(view: HorizontalSeekBar, progress: Int) {
        isSeekBarTracking = false
        AudioUtil.playSoundInBackground(context, R.raw.uxsdk_camera_ev_center)
        if (view == seekbar_iso) {
            if (uiIsoValueArray.isNotEmpty()) {
                val newISO = uiIsoValueArray[progress]
                newISO?.let {
                    updateISOToCamera(it)
                }
            }
        } else {
            if (progress < eiValueArray.size) {
                updateEIToCamera(eiValueArray[progress])
            }
        }
    }

    override fun onPlusClicked(view: HorizontalSeekBar) {
        //暂未实现
    }

    override fun onMinusClicked(view: HorizontalSeekBar) {
        //暂未实现
    }

    override fun onClick(v: View?) {
        if (v == button_iso_auto) {
            isISOAutoSelected = !isISOAutoSelected
            setAutoISO(isISOAutoSelected)
        }
    }

    private fun updateWidgetUI() {
        if (widgetModel.isRecordVideoEIMode()) {
            textview_iso_title.setText(R.string.uxsdk_camera_ei)
            seekbar_iso_layout.visibility = GONE
            seekbar_ei.visibility = VISIBLE
        } else {
            textview_iso_title.setText(R.string.uxsdk_camera_exposure_iso_title)
            seekbar_iso_layout.visibility = VISIBLE
            seekbar_ei.visibility = GONE
        }
    }

    private fun onISOUpdated(iso: CameraISO) {
        if (iso == CameraISO.ISO_FIXED) {
            updateISOLocked()
        }
    }

    private fun onExposureModeUpdated(exposureMode: CameraExposureMode) {
        if (exposureMode != CameraExposureMode.MANUAL) {
            isISOAutoSelected = true
            setAutoISO(isISOAutoSelected)
        } else {
            isISOAutoSelected = false
        }

    }

    private fun updateISORangeValue(array: Array<CameraISO?>) {
        isISOAutoSupported = checkAutoISO(array)
        val newISOValues: Array<CameraISO?> = if (isISOAutoSupported) {
            arrayOfNulls(array.size - 1)
        } else {
            arrayOfNulls(array.size)
        }

        // remove the auto value
        var i = 0
        var j = 0
        while (i < array.size) {
            if (array[i] != CameraISO.ISO_AUTO) {
                newISOValues[j] = array[i]
                j++
            }
            i++
        }
        uiIsoValueArray = newISOValues
    }

    private fun updateISORangeUI() {
        // Workaround where ISO range updates to single value in AUTO mode
        if (uiIsoValueArray.isNotEmpty()) {
            val minCameraISO = CameraUtil.convertISOToInt(uiIsoValueArray[0])
            seekbar_iso.setMinValueText(minCameraISO.toString())
            val maxCameraISO = CameraUtil.convertISOToInt(uiIsoValueArray[uiIsoValueArray.size - 1])
            seekbar_iso.setMaxValueText(maxCameraISO.toString())
            seekbar_iso.max = uiIsoValueArray.size - 1
            isISOSeekBarEnabled = true
            updateISOValue(uiIsoValueArray, uiCameraISO)
            // Auto button has relationship with ISO range, so need update this button here.
            updateAutoISOButton()
        } else {
            isISOSeekBarEnabled = false
        }
    }

    private fun updateISOEnableStatus() {
        seekbar_iso.enable(!isISOAutoSelected && isISOSeekBarEnabled)
    }

    private fun checkAutoISO(array: Array<CameraISO?>): Boolean {
        for (iso in array) {
            if (iso == CameraISO.ISO_AUTO) {
                return true
            }
        }
        return false
    }

    private fun updateISOValue(array: Array<CameraISO?>, value: Int) {
        val progress: Int = getISOIndex(array, value)
        seekbar_iso.progress = progress
    }

    private fun updateAutoISOButton() {
        if (isISOAutoSupported && isISOSeekBarEnabled && !widgetModel.isRecordVideoEIMode() && CameraUtil.isAutoISOSupportedByProduct()) {
            button_iso_auto.visibility = VISIBLE
        } else {
            button_iso_auto.visibility = GONE
        }
    }

    private fun getISOIndex(array: Array<CameraISO?>, isoValue: Int): Int {
        var index = -1
        val iso = CameraUtil.convertIntToISO(isoValue)
        for (i in array.indices) {
            if (iso == array[i]) {
                index = i
                break
            }
        }
        return index
    }

    private fun setAutoISO(isAuto: Boolean) {
        var newISO: CameraISO? = null
        if (isAuto) {
            newISO = CameraISO.ISO_AUTO
        } else {
            if (seekbar_iso.progress < uiIsoValueArray.size) {
                newISO = uiIsoValueArray[seekbar_iso.progress]
            }
        }
        newISO?.let {
            updateISOToCamera(it)
        }
    }

    private fun updateISOToCamera(iso: CameraISO) {
        addDisposable(
            widgetModel.setISO(iso).observeOn(SchedulerProvider.ui()).subscribe(Action { }, RxUtil.errorConsumer({
                seekbar_iso.restorePreviousProgress()
            }, this.toString(), "updateISOToCamera: "))
        )
    }

    private fun updateEIToCamera(ei: Int) {
        addDisposable(
            widgetModel.setEI(EIType.find(ei)).observeOn(SchedulerProvider.ui()).subscribe(Action { }, RxUtil.errorConsumer({
                seekbar_iso.restorePreviousProgress()
            }, this.toString(), "updateEIToCamera: "))
        )
    }

    // By referring to DJIGo4 in both iOS and Android version
    // Showing the ISO_FIXED  as locked value 500
    private fun updateISOLocked() {
        button_iso_auto.visibility = GONE
        seekbar_iso.enable(false)
        seekbar_iso.progress = seekbar_iso.max / 2 - 1
    }

    sealed class ModelState
}