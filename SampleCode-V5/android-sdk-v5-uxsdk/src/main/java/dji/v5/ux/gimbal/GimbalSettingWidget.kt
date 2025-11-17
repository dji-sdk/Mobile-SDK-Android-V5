package dji.v5.ux.gimbal

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.Toast
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.gimbal.GimbalCalibrationState
import dji.sdk.keyvalue.value.gimbal.GimbalCalibrationStatusInfo
import dji.v5.utils.common.LogUtils
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.IGimbalIndex
import dji.v5.ux.core.base.SchedulerProvider.ui
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.ui.setting.dialog.CommonLoadingDialog
import dji.v5.ux.core.util.ViewUtil


open class GimbalSettingWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<Any>(context, attrs, defStyleAttr), View.OnClickListener, IGimbalIndex {

    companion object {
        private const val TAG = "GimbalSettingWidget"
    }

    private val loadingDialog: CommonLoadingDialog = CommonLoadingDialog(context)

    private var areMotors: Boolean = false

    private val widgetModel = GimbalSettingWidgetModel(
        DJISDKModel.getInstance(),
        ObservableInMemoryKeyedStore.getInstance()
    )

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_gimbal_setting, this)

        val btnReset = findViewById<Button>(R.id.setting_menu_gimbal_reset_param)
        val btnCalibration = findViewById<Button>(R.id.setting_menu_gimbal_calibration)
        val setting_menu_gimbal_adjust = findViewById<Button>(R.id.setting_menu_gimbal_adjust)


        btnReset.setOnClickListener(this)
        btnCalibration.setOnClickListener(this)
        setting_menu_gimbal_adjust.setOnClickListener(this)

    }

    override fun reactToModelChanges() {
        addReaction(widgetModel.calibrationStatus().observeOn(ui()).subscribe { status: GimbalCalibrationStatusInfo ->
            if (status.status != GimbalCalibrationState.IN_PROGRESS) {
                if (loadingDialog.isShowing) {
                    if (status.status == GimbalCalibrationState.IDLE) {
                        ViewUtil.showToast(context, R.string.uxsdk_gimbal_cali_success, Toast.LENGTH_SHORT)
                    } else {
                        ViewUtil.showToast(context, R.string.uxsdk_gimbal_cali_fail, Toast.LENGTH_SHORT)
                    }
                }
                loadingDialog.dismiss()
                return@subscribe
            }
            if (!loadingDialog.isShowing) {
                loadingDialog.show()
            }
            loadingDialog.setLoadingText(resources.getString(R.string.uxsdk_gimbal_caling, status.progress))
        })

        addReaction(widgetModel.areMotorsOn().subscribe { areMotors ->
            this.areMotors = areMotors
        })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
    }

    override fun onClick(v: View) {
        if (v.id == R.id.setting_menu_gimbal_reset_param) {
            widgetModel.resetGimbal()
                .doOnComplete { ViewUtil.showToast(context, R.string.uxsdk_gimbal_gimbal_reset_params_success, Toast.LENGTH_SHORT) }
                .doOnError { throwable: Throwable ->
                    LogUtils.e(TAG, "resetGimbal fail $throwable")
                    ViewUtil.showToast(context, R.string.uxsdk_gimbal_gimbal_reset_params_fail, Toast.LENGTH_SHORT)
                }.subscribe()
        } else if (v.id == R.id.setting_menu_gimbal_calibration) {
            if (areMotors) {
                ViewUtil.showToast(context, R.string.uxsdk_setting_ui_gimbal_calibration_tip, Toast.LENGTH_SHORT)
                return
            }
            ViewUtil.showToast(context, R.string.uxsdk_setting_ui_gimbal_auto_calibration_tip, Toast.LENGTH_SHORT)
            widgetModel.calibrateGimbal().subscribe()
        }
        else if (v.id ==R.id.setting_menu_gimbal_adjust) {
            widgetModel.setGimbalClicked()
        }
    }

    override fun getGimbalIndex(): ComponentIndexType {
        return widgetModel.getGimbalIndex()
    }

    override fun updateGimbalIndex(gimbalIndex: ComponentIndexType) {
        widgetModel.updateGimbalIndex(gimbalIndex)
    }
}