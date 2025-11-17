package dji.v5.ux.gimbal

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.gimbal.PostureFineTuneAxis
import dji.v5.utils.common.AndUtil
import dji.v5.utils.common.LogUtils
import dji.v5.ux.R
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.IGimbalIndex
import dji.v5.ux.core.base.SchedulerProvider.ui
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import java.util.Locale

/**
 * This widget is used to manually fine-tune the gimbal
 */
open class GimbalFineTuneWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<Any>(context, attrs, defStyleAttr), View.OnClickListener, IGimbalIndex {

    companion object {
        private val TAG = "GimbalFineTuneWidget"
        private val GIMBAL_PITCH_FINE_TUNE = PostureFineTuneAxis.PITCH_AXIS
        private val GIMBAL_ROLL_FINE_TUNE = PostureFineTuneAxis.ROLL_AXIS
        private val GIMBAL_YAW_FINE_TUNE = PostureFineTuneAxis.YAW_AXIS
    }

    private val widgetModel = GimbalFineTuneWidgetModel(
        DJISDKModel.getInstance(),
        ObservableInMemoryKeyedStore.getInstance()
    )

    private val pitchTv: TextView = findViewById(R.id.fpv_left_tv)
    private val rollTv: TextView = findViewById(R.id.fpv_middle_tv)
    private val yawTv: TextView = findViewById(R.id.fpv_right_tv)
    private val imgMinus: ImageView = findViewById(R.id.fpv_gimbal_finetune_minus_img)
    private val tvValue: TextView = findViewById(R.id.fpv_gimbal_finetune_value_tv)
    private val imgPlus: ImageView = findViewById(R.id.fpv_gimbal_finetune_plus_img)

    var rollDegree: Double = 0.0
    var pitchDegree: Double = 0.0
    var yawDegree: Double = 0.0
    var currentAxis: PostureFineTuneAxis = PostureFineTuneAxis.PITCH_AXIS

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        inflate(context, R.layout.uxsdk_gimbal_fine_tune, this)
        setBackgroundResource(R.drawable.uxsdk_background_black_rectangle)
        val padding = AndUtil.dip2px(getContext(), 16f)
        setPadding(padding, padding, padding, padding)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
            pitchTv.setOnClickListener(this)
            rollTv.setOnClickListener(this)
            yawTv.setOnClickListener(this)
            imgMinus.setOnClickListener(this)
            imgPlus.setOnClickListener(this)
            tvValue.setOnClickListener(this)
            updateViewForType(currentAxis)
        }

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
    }

    override fun reactToModelChanges() {
        //Roll微调角度
        addReaction(widgetModel.rollAdjustDegree().observeOn(ui()).subscribe { degree: Double ->
            rollDegree = degree
            updateViewForType(currentAxis)
        })

        //Pitch微调角度
        addReaction(widgetModel.pitchAdjustDegree().observeOn(ui()).subscribe { degree: Double ->
            pitchDegree = degree
            updateViewForType(currentAxis)
        })
        //Yaw微调角度
        addReaction(widgetModel.yawAdjustDegree().observeOn(ui()).subscribe { degree: Double ->
            yawDegree = degree
            updateViewForType(currentAxis)
        })
    }

    override fun onClick(v: View) {
        val id = v.id
        if (R.id.fpv_left_tv == id) {
            updateViewForType(GIMBAL_PITCH_FINE_TUNE)
        } else if (R.id.fpv_middle_tv == id) {
            updateViewForType(GIMBAL_ROLL_FINE_TUNE)
        } else if (R.id.fpv_right_tv == id) {
            updateViewForType(GIMBAL_YAW_FINE_TUNE)
        } else if (R.id.fpv_gimbal_finetune_minus_img == id) {
            widgetModel.fineTunePosture(currentAxis, -0.1).subscribe()
        } else if (R.id.fpv_gimbal_finetune_plus_img == id) {
            widgetModel.fineTunePosture(currentAxis, 0.1).subscribe()
        } else if (R.id.fpv_gimbal_finetune_value_tv == id) {
            widgetModel.fineTunePosture(currentAxis, 0.0).subscribe()
        }
    }

    private fun updateViewForType(type: PostureFineTuneAxis) {
        currentAxis = type
        pitchTv.setTextColor(ContextCompat.getColor(context, R.color.uxsdk_fpv_white_50))
        rollTv.setTextColor(ContextCompat.getColor(context, R.color.uxsdk_fpv_white_50))
        yawTv.setTextColor(ContextCompat.getColor(context, R.color.uxsdk_fpv_white_50))
        when (type) {
            GIMBAL_PITCH_FINE_TUNE -> {
                pitchTv.setTextColor(ContextCompat.getColor(context, R.color.uxsdk_setting_ui_btn_hover))
                imgMinus.setImageResource(R.drawable.fpv_pitch_down_normal)
                imgPlus.setImageResource(R.drawable.fpv_pitch_up_normal)
                tvValue.text = String.format(Locale.US, "%.1f", if (-pitchDegree == -0.0) 0.0 else -pitchDegree)
            }

            GIMBAL_ROLL_FINE_TUNE -> {
                rollTv.setTextColor(ContextCompat.getColor(context, R.color.uxsdk_setting_ui_btn_hover))
                imgMinus.setImageResource(R.drawable.fpv_roll_leftarrow_normal)
                imgPlus.setImageResource(R.drawable.fpv_roll_rightarrow_normal)
                tvValue.text = String.format(Locale.US, "%.1f", if (rollDegree == -0.0) 0.0 else rollDegree)
            }

            GIMBAL_YAW_FINE_TUNE -> {
                yawTv.setTextColor(ContextCompat.getColor(context, R.color.uxsdk_setting_ui_btn_hover))
                imgMinus.setImageResource(R.drawable.fpv_yaw_right)
                imgPlus.setImageResource(R.drawable.fpv_yaw_left)
                tvValue.text = String.format(Locale.US, "%.1f", if (-yawDegree == -0.0) 0.0 else -yawDegree)
            }

            else -> {
                LogUtils.e(TAG, "unknown type: $currentAxis")
            }
        }
    }

    override fun getGimbalIndex(): ComponentIndexType {
        return widgetModel.getGimbalIndex()
    }

    override fun updateGimbalIndex(gimbalIndex: ComponentIndexType) {
        widgetModel.updateGimbalIndex(gimbalIndex)
    }

}