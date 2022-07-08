package dji.sampleV5.moduleaircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.viewModels
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.models.PerceptionVM
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sampleV5.modulecommon.util.Helper
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.perception.ObstacleAvoidanceType
import dji.v5.manager.aircraft.perception.PerceptionDirection
import dji.v5.manager.aircraft.perception.PerceptionInfo
import dji.v5.utils.common.LogUtils
import kotlinx.android.synthetic.main.frag_perception_page.*

/**
 * Description :感知模块Fragment
 *
 * @author: Byte.Cai
 *  date : 2022/6/7
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class PerceptionFragment : DJIFragment(), CompoundButton.OnCheckedChangeListener {
    private val TAG = LogUtils.getTag("PerceptionFragment")

    private val perceptionVM: PerceptionVM by viewModels()
    private val perceptionMsgBuilder: StringBuilder = StringBuilder()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_perception_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tb_obstacle_avoidance_master_switch.setOnCheckedChangeListener(this)
        tb_set_vision_positioning_enable_switch.setOnCheckedChangeListener(this)
        tb_set_precision_landing_enable_switch.setOnCheckedChangeListener(this)
        tv_obstacle_avoidance_up_switch.setOnCheckedChangeListener(this)
        tv_obstacle_avoidance_down_switch.setOnCheckedChangeListener(this)
        tv_obstacle_avoidance_horizontal_switch.setOnCheckedChangeListener(this)

        perceptionVM.addPerceptionInfoListener()
        observerPerceptionInfo()

        bt_set_obstacle_avoidance_type.setOnClickListener {
            val values = ObstacleAvoidanceType.values()
            initPopupNumberPicker(Helper.makeList(values)) {
                perceptionVM.setObstacleAvoidanceType(values[indexChosen[0]], buttonCompletionCallback)
            }
        }

        bt_set_obstacle_avoidance_warning_distance.setOnClickListener {
            val distance = doubleArrayOf(2.3, 6.7, 9.8, 10.0, 20.0, 30.0)
            val direction = PerceptionDirection.values()

            initPopupNumberPicker(Helper.makeList(distance)) {
                val selectDistance = distance[indexChosen[0]]
                initPopupNumberPicker(Helper.makeList(direction)) {
                    val selectDirection = direction[indexChosen[0]]
                    perceptionVM.setObstacleAvoidanceWarningDistance(selectDistance, selectDirection, buttonCompletionCallback)
                }
            }
        }

        bt_set_obstacle_avoidance_braking_distance.setOnClickListener {
            val distance = doubleArrayOf(2.0, 3.5, 5.6, 8.9, 10.0, 20.0)
            val direction = PerceptionDirection.values()

            initPopupNumberPicker(Helper.makeList(distance)) {
                val selectDistance = distance[indexChosen[0]]
                initPopupNumberPicker(Helper.makeList(direction)) {
                    val selectDirection = direction[indexChosen[0]]
                    perceptionVM.setObstacleAvoidanceBrakingDistance(selectDistance, selectDirection, buttonCompletionCallback)
                }

            }
        }

    }


    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        when (buttonView) {
            tb_obstacle_avoidance_master_switch -> {
                perceptionVM.setOverallObstacleAvoidanceEnabled(isChecked, object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        updateCurrentErrorMsg(isSuccess = true)

                    }

                    override fun onFailure(error: IDJIError) {
                        handleSwitchButtonError(error)
                        tb_obstacle_avoidance_master_switch.setOnCheckedChangeListener(null)
                        tb_obstacle_avoidance_master_switch.isChecked = !isChecked
                        tb_obstacle_avoidance_master_switch.setOnCheckedChangeListener(this@PerceptionFragment)

                    }

                })
            }
            tb_set_vision_positioning_enable_switch -> {
                perceptionVM.setVisionPositioningEnabled(isChecked, object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        updateCurrentErrorMsg(isSuccess = true)

                    }

                    override fun onFailure(error: IDJIError) {
                        handleSwitchButtonError(error)
                        tb_set_vision_positioning_enable_switch.setOnCheckedChangeListener(null)
                        tb_set_vision_positioning_enable_switch.isChecked = !isChecked
                        tb_set_vision_positioning_enable_switch.setOnCheckedChangeListener(this@PerceptionFragment)

                    }

                })
            }
            tb_set_precision_landing_enable_switch -> {
                perceptionVM.setPrecisionLandingEnabled(isChecked, object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        updateCurrentErrorMsg(isSuccess = true)

                    }

                    override fun onFailure(error: IDJIError) {
                        handleSwitchButtonError(error)
                        tb_set_precision_landing_enable_switch.setOnCheckedChangeListener(null)
                        tb_set_precision_landing_enable_switch.isChecked = !isChecked
                        tb_set_precision_landing_enable_switch.setOnCheckedChangeListener(this@PerceptionFragment)

                    }

                })
            }
            //避障子开关
            tv_obstacle_avoidance_up_switch -> {
                perceptionVM.setObstacleAvoidanceEnabled(isChecked, PerceptionDirection.UPWARD, object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        updateCurrentErrorMsg(isSuccess = true)

                    }

                    override fun onFailure(error: IDJIError) {
                        handleSwitchButtonError(error)
                        tv_obstacle_avoidance_up_switch.setOnCheckedChangeListener(null)
                        tv_obstacle_avoidance_up_switch.isChecked = !isChecked
                        tv_obstacle_avoidance_up_switch.setOnCheckedChangeListener(this@PerceptionFragment)

                    }

                })
            }

            tv_obstacle_avoidance_down_switch -> {
                perceptionVM.setObstacleAvoidanceEnabled(isChecked, PerceptionDirection.DOWNWARD, object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        updateCurrentErrorMsg(isSuccess = true)

                    }

                    override fun onFailure(error: IDJIError) {
                        handleSwitchButtonError(error)
                        tv_obstacle_avoidance_down_switch.setOnCheckedChangeListener(null)
                        tv_obstacle_avoidance_down_switch.isChecked = !isChecked
                        tv_obstacle_avoidance_down_switch.setOnCheckedChangeListener(this@PerceptionFragment)

                    }

                })
            }

            tv_obstacle_avoidance_horizontal_switch -> {
                perceptionVM.setObstacleAvoidanceEnabled(isChecked, PerceptionDirection.HORIZONTAL, object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        updateCurrentErrorMsg(isSuccess = true)

                    }

                    override fun onFailure(error: IDJIError) {
                        handleSwitchButtonError(error)
                        tv_obstacle_avoidance_horizontal_switch.setOnCheckedChangeListener(null)
                        tv_obstacle_avoidance_horizontal_switch.isChecked = !isChecked
                        tv_obstacle_avoidance_horizontal_switch.setOnCheckedChangeListener(this@PerceptionFragment)

                    }

                })
            }
        }
    }


    private fun handleSwitchButtonError(error: IDJIError) {
        val error = error.toString()
        showToast(error)
        updateCurrentErrorMsg(error)
    }

    private val buttonCompletionCallback = object : CommonCallbacks.CompletionCallback {
        override fun onSuccess() {
            ToastUtils.showToast("Successful operation")
            updateCurrentErrorMsg(isSuccess = true)
        }

        override fun onFailure(error: IDJIError) {
            val error = error.toString()
            showToast(error)
            updateCurrentErrorMsg(error)

        }

    }


    private fun updateCurrentErrorMsg(errorMsg: String? = null, isSuccess: Boolean = false) {
        tv_error_msg.text = if (isSuccess) "" else errorMsg
    }

    private fun showToast(toastMsg: String) {
        ToastUtils.showToast(toastMsg)
    }

    private fun observerPerceptionInfo() {
        perceptionVM.perceptionInfo.observe(viewLifecycleOwner, {
            updatePerceptionInfo(it)
            changeObstacleAvoidanceEnableSwitch(it)
            changeOtherEnableSwitch(it)
        })
    }

    private fun changeOtherEnableSwitch(perceptionInfo: PerceptionInfo) {
        perceptionInfo.apply {
            //避障总开关
            val checked1 = tb_obstacle_avoidance_master_switch.isChecked
            if (checked1 != isOverallObstacleAvoidanceEnabled) {
                tb_obstacle_avoidance_master_switch.setOnCheckedChangeListener(null)
                tb_obstacle_avoidance_master_switch.isChecked = isOverallObstacleAvoidanceEnabled
                tb_obstacle_avoidance_master_switch.setOnCheckedChangeListener(this@PerceptionFragment)
            }

            //视觉定位
            val checked2 = tb_set_vision_positioning_enable_switch.isChecked
            if (checked2 != isVisionPositioningEnabled) {
                tb_set_vision_positioning_enable_switch.setOnCheckedChangeListener(null)
                tb_set_vision_positioning_enable_switch.isChecked = isVisionPositioningEnabled
                tb_set_vision_positioning_enable_switch.setOnCheckedChangeListener(this@PerceptionFragment)

            }
            //精准降落
            val check3 = tb_set_precision_landing_enable_switch.isChecked
            if (check3 != isPrecisionLandingEnabled) {
                tb_set_precision_landing_enable_switch.setOnCheckedChangeListener(null)
                tb_set_precision_landing_enable_switch.isChecked = isPrecisionLandingEnabled
                tb_set_precision_landing_enable_switch.setOnCheckedChangeListener(this@PerceptionFragment)
            }
        }


    }

    private fun changeObstacleAvoidanceEnableSwitch(perceptionInfo: PerceptionInfo) {
        perceptionInfo.apply {
            //避障子开关
            val checked1 = tv_obstacle_avoidance_up_switch.isChecked
            if (checked1 != isUpwardObstacleAvoidanceEnabled) {
                tv_obstacle_avoidance_up_switch.setOnCheckedChangeListener(null)
                tv_obstacle_avoidance_up_switch.isChecked = isUpwardObstacleAvoidanceEnabled
                tv_obstacle_avoidance_up_switch.setOnCheckedChangeListener(this@PerceptionFragment)
            }

            val checked2 = tv_obstacle_avoidance_down_switch.isChecked
            if (checked2 != isDownwardObstacleAvoidanceEnabled) {
                tv_obstacle_avoidance_down_switch.setOnCheckedChangeListener(null)
                tv_obstacle_avoidance_down_switch.isChecked = isDownwardObstacleAvoidanceEnabled
                tv_obstacle_avoidance_down_switch.setOnCheckedChangeListener(this@PerceptionFragment)
            }

            val checked3 = tv_obstacle_avoidance_horizontal_switch.isChecked
            if (checked3 != isHorizontalObstacleAvoidanceEnabled) {
                tv_obstacle_avoidance_horizontal_switch.setOnCheckedChangeListener(null)
                tv_obstacle_avoidance_horizontal_switch.isChecked = isHorizontalObstacleAvoidanceEnabled
                tv_obstacle_avoidance_horizontal_switch.setOnCheckedChangeListener(this@PerceptionFragment)
            }
        }

    }

    private fun updatePerceptionInfo(perceptionInfo: PerceptionInfo) {
        perceptionMsgBuilder.apply {
            perceptionInfo.apply {
                setLength(0)
                append("isOverallObstacleAvoidanceEnabled:").append(isOverallObstacleAvoidanceEnabled).append("\n")
                append("ObstacleAvoidanceEnabled:").append(
                    " upward:$isUpwardObstacleAvoidanceEnabled,down:$isDownwardObstacleAvoidanceEnabled,horizontal:$isHorizontalObstacleAvoidanceEnabled"
                ).append("\n")
                append("isVisionPositioningEnabled:$isVisionPositioningEnabled").append("\n")
                append("isPrecisionLandingEnabled:$isPrecisionLandingEnabled").append("\n")
                append("ObstacleAvoidanceType:$obstacleAvoidanceType").append("\n")
                append("ObstacleAvoidanceBrakingDistance:").append(
                    " upward:$upwardObstacleAvoidanceBrakingDistance,down:$downwardObstacleAvoidanceBrakingDistance,horizontal:$horizontalObstacleAvoidanceBrakingDistance"
                ).append("\n")
                append("ObstacleAvoidanceWarningDistance:").append(" upward:$upwardObstacleAvoidanceWarningDistance,down:$downwardObstacleAvoidanceWarningDistance,horizontal:$horizontalObstacleAvoidanceWarningDistance")
                    .append("\n")
            }


        }
        activity?.runOnUiThread {
            tv_perception_info.text = perceptionMsgBuilder.toString()
        }
    }
}