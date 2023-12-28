package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.viewModels
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.models.PerceptionVM
import dji.sampleV5.aircraft.util.Helper
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.perception.data.ObstacleAvoidanceType
import dji.v5.manager.aircraft.perception.data.PerceptionDirection
import dji.v5.manager.aircraft.perception.data.PerceptionInfo
import dji.v5.manager.aircraft.perception.data.ObstacleData
import dji.v5.manager.aircraft.perception.radar.RadarInformation
import dji.v5.utils.common.LogUtils
import dji.sampleV5.aircraft.util.ToastUtils
import dji.v5.ux.core.extension.hide
import dji.v5.ux.core.extension.show
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

    //感知开关等信息
    private val perceptionInfoMsgBuilder: StringBuilder = StringBuilder()

    //雷达特有的开关等信息
    private val radarInfoMsgBuilder: StringBuilder = StringBuilder()

    //感知避障数据
    private val perceptionObstacleDataBuilder: StringBuilder = StringBuilder()

    //雷达避障数据
    private val radarObstacleDataBuilder: StringBuilder = StringBuilder()
    private var perceptionInfo: PerceptionInfo? = null
    private var radarInformation: RadarInformation? = null
    private var obstacleData: ObstacleData? = null
    private var radarObstacleData: ObstacleData? = null
    private var isRadarConnected = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_perception_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tb_set_vision_positioning_enable_switch.setOnCheckedChangeListener(this)
        tv_radar_obstacle_avoidance_up_switch.setOnCheckedChangeListener(this)
        tv_radar_obstacle_avoidance_horizontal_switch.setOnCheckedChangeListener(this)
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
            val distance = doubleArrayOf(1.8, 2.0, 3.5, 5.6, 8.9, 10.0, 20.0)
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

            //雷达避障子开关
            tv_radar_obstacle_avoidance_up_switch -> {
                perceptionVM.setRadarObstacleAvoidanceEnabled(isChecked, PerceptionDirection.UPWARD, object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        updateCurrentErrorMsg(isSuccess = true)

                    }

                    override fun onFailure(error: IDJIError) {
                        handleSwitchButtonError(error)
                        tv_radar_obstacle_avoidance_up_switch.setOnCheckedChangeListener(null)
                        tv_radar_obstacle_avoidance_up_switch.isChecked = !isChecked
                        tv_radar_obstacle_avoidance_up_switch.setOnCheckedChangeListener(this@PerceptionFragment)

                    }

                })
            }

            tv_radar_obstacle_avoidance_horizontal_switch -> {
                perceptionVM.setRadarObstacleAvoidanceEnabled(isChecked, PerceptionDirection.HORIZONTAL, object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        updateCurrentErrorMsg(isSuccess = true)

                    }

                    override fun onFailure(error: IDJIError) {
                        handleSwitchButtonError(error)
                        tv_radar_obstacle_avoidance_horizontal_switch.setOnCheckedChangeListener(null)
                        tv_radar_obstacle_avoidance_horizontal_switch.isChecked = !isChecked
                        tv_radar_obstacle_avoidance_horizontal_switch.setOnCheckedChangeListener(this@PerceptionFragment)

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
            updatePerceptionInfo()
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
        if (isFragmentShow()) {
            tv_error_msg.text = if (isSuccess) "" else errorMsg
        }
    }

    private fun showToast(toastMsg: String) {
        ToastUtils.showToast(toastMsg)
    }

    private fun observerPerceptionInfo() {
        perceptionVM.perceptionInfo.observe(viewLifecycleOwner, {
            perceptionInfo = it
            updatePerceptionInfo()
            changeObstacleAvoidanceEnableSwitch()
            changeOtherEnableSwitch(it)
        })
        perceptionVM.obstacleData.observe(viewLifecycleOwner, {
            obstacleData = it
            updatePerceptionInfo()
        })
        perceptionVM.obstacleDataForRadar.observe(viewLifecycleOwner, {
            radarObstacleData = it
            updatePerceptionInfo()
        })

        perceptionVM.radarInformation.observe(viewLifecycleOwner, {
            radarInformation = it
            changeObstacleAvoidanceEnableSwitch()
            updatePerceptionInfo()
        })
        perceptionVM.radarConnect.observe(viewLifecycleOwner, {
            isRadarConnected = it
            if (it) {
                rl_radar_obstacle_avoidance_switch.show()
            } else {
                rl_radar_obstacle_avoidance_switch.hide()
            }
        })


    }

    private fun changeOtherEnableSwitch(perceptionInfo: PerceptionInfo) {
        perceptionInfo.apply {
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

    private fun changeObstacleAvoidanceEnableSwitch() {
        perceptionInfo?.apply {
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
        radarInformation?.apply {
            val checked1 = tv_radar_obstacle_avoidance_up_switch.isChecked
            if (checked1 != isUpwardObstacleAvoidanceEnabled) {
                tv_radar_obstacle_avoidance_up_switch.setOnCheckedChangeListener(null)
                tv_radar_obstacle_avoidance_up_switch.isChecked = isUpwardObstacleAvoidanceEnabled
                tv_radar_obstacle_avoidance_up_switch.setOnCheckedChangeListener(this@PerceptionFragment)
            }

            val checked2 = tv_radar_obstacle_avoidance_horizontal_switch.isChecked
            if (checked2 != isHorizontalObstacleAvoidanceEnabled) {
                tv_radar_obstacle_avoidance_horizontal_switch.setOnCheckedChangeListener(null)
                tv_radar_obstacle_avoidance_horizontal_switch.isChecked = isHorizontalObstacleAvoidanceEnabled
                tv_radar_obstacle_avoidance_horizontal_switch.setOnCheckedChangeListener(this@PerceptionFragment)
            }
        }

    }

    private var result: String = ""
    private fun updatePerceptionInfo() {
        perceptionInfoMsgBuilder.apply {
            perceptionInfo?.apply {
                clear()
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

        perceptionObstacleDataBuilder.apply {
            obstacleData?.apply {
                clear()
                append("\n").append("ObstacleDataForPerception:").append("\n")
                append("horizontalObstacleDistance:$horizontalObstacleDistance").append("\n")
                append("upwardObstacleDistance:$upwardObstacleDistance").append("\n")
                append("downwardObstacleDistance:$downwardObstacleDistance").append("\n")
                append("horizontalAngleInterval:$horizontalAngleInterval").append("\n")
            }
        }
        result = perceptionInfoMsgBuilder.toString() + perceptionObstacleDataBuilder.toString()
        if (isRadarConnected) {
            radarObstacleDataBuilder.clear()
            radarObstacleDataBuilder.apply {
                radarObstacleData?.apply {
                    append("\n").append("ObstacleDataForRadar:").append("\n")
                    append("horizontalObstacleDistance:$horizontalObstacleDistance").append("\n")
                    append("upwardObstacleDistance:$upwardObstacleDistance").append("\n")
                    append("horizontalAngleInterval:$horizontalAngleInterval").append("\n")

                }
            }

            radarInfoMsgBuilder.clear()
            radarInfoMsgBuilder.apply {
                radarInformation?.apply {
                    append("\n").append("RadarInformation:").append("\n")
                    append("isHorizontalObstacleAvoidanceEnabled:$isHorizontalObstacleAvoidanceEnabled").append("\n")
                    append("isUpwardObstacleAvoidanceEnabled:$isUpwardObstacleAvoidanceEnabled").append("\n")
                }
            }
            result = result + radarInfoMsgBuilder.toString() + radarObstacleDataBuilder.toString()
        }

        activity?.runOnUiThread {
            tv_perception_info.text = result
        }
    }
}