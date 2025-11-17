package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.viewModels
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.databinding.FragAppSilentlyUpgradePageBinding
import dji.sampleV5.aircraft.databinding.FragPerceptionPageBinding
import dji.sampleV5.aircraft.models.PerceptionVM
import dji.sampleV5.aircraft.util.Helper
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.perception.data.ObstacleAvoidanceType
import dji.v5.manager.aircraft.perception.data.PerceptionDirection
import dji.v5.manager.aircraft.perception.data.PerceptionInfo
import dji.v5.manager.aircraft.perception.data.ObstacleData
import dji.v5.manager.aircraft.perception.radar.RadarInformation
import dji.sampleV5.aircraft.util.ToastUtils
import dji.v5.ux.core.extension.hide
import dji.v5.ux.core.extension.show

/**
 * Description :感知模块Fragment
 *
 * @author: Byte.Cai
 *  date : 2022/6/7
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class PerceptionFragment : DJIFragment(), CompoundButton.OnCheckedChangeListener {

    private var binding: FragPerceptionPageBinding? = null
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
        binding = FragPerceptionPageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.tbSetVisionPositioningEnableSwitch?.setOnCheckedChangeListener(this)
        binding?.tvRadarObstacleAvoidanceUpSwitch?.setOnCheckedChangeListener(this)
        binding?.tvRadarObstacleAvoidanceHorizontalSwitch?.setOnCheckedChangeListener(this)
        binding?.tbSetPrecisionLandingEnableSwitch?.setOnCheckedChangeListener(this)
        binding?.tvObstacleAvoidanceUpSwitch?.setOnCheckedChangeListener(this)
        binding?.tvObstacleAvoidanceDownSwitch?.setOnCheckedChangeListener(this)
        binding?.tvObstacleAvoidanceHorizontalSwitch?.setOnCheckedChangeListener(this)

        perceptionVM.addPerceptionInfoListener()
        observerPerceptionInfo()

        binding?.btSetObstacleAvoidanceType?.setOnClickListener {
            val values = ObstacleAvoidanceType.values()
            initPopupNumberPicker(Helper.makeList(values)) {
                perceptionVM.setObstacleAvoidanceType(values[indexChosen[0]], buttonCompletionCallback)
            }
        }

        binding?.btSetObstacleAvoidanceWarningDistance?.setOnClickListener {
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

        binding?.btSetObstacleAvoidanceBrakingDistance?.setOnClickListener {
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
            binding?.tbSetVisionPositioningEnableSwitch -> {
                perceptionVM.setVisionPositioningEnabled(isChecked, object :
                    CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        updateCurrentErrorMsg(isSuccess = true)

                    }

                    override fun onFailure(error: IDJIError) {
                        handleSwitchButtonError(error)
                        binding?.tbSetVisionPositioningEnableSwitch?.setOnCheckedChangeListener(null)
                        binding?.tbSetVisionPositioningEnableSwitch?.isChecked = !isChecked
                        binding?.tbSetVisionPositioningEnableSwitch?.setOnCheckedChangeListener(this@PerceptionFragment)

                    }

                })
            }

            binding?.tbSetPrecisionLandingEnableSwitch -> {
                perceptionVM.setPrecisionLandingEnabled(isChecked, object :
                    CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        updateCurrentErrorMsg(isSuccess = true)
                    }

                    override fun onFailure(error: IDJIError) {
                        handleSwitchButtonError(error)
                        binding?.tbSetPrecisionLandingEnableSwitch?.setOnCheckedChangeListener(null)
                        binding?.tbSetPrecisionLandingEnableSwitch?.isChecked = !isChecked
                        binding?.tbSetPrecisionLandingEnableSwitch?.setOnCheckedChangeListener(this@PerceptionFragment)
                    }
                })
            }

            //避障子开关
            binding?.tvObstacleAvoidanceUpSwitch -> {
                perceptionVM.setObstacleAvoidanceEnabled(isChecked, PerceptionDirection.UPWARD, object :
                    CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        updateCurrentErrorMsg(isSuccess = true)

                    }

                    override fun onFailure(error: IDJIError) {
                        handleSwitchButtonError(error)
                        binding?.tvObstacleAvoidanceUpSwitch?.setOnCheckedChangeListener(null)
                        binding?.tvObstacleAvoidanceUpSwitch?.isChecked = !isChecked
                        binding?.tvObstacleAvoidanceUpSwitch?.setOnCheckedChangeListener(this@PerceptionFragment)

                    }

                })
            }

            binding?.tvObstacleAvoidanceDownSwitch -> {
                perceptionVM.setObstacleAvoidanceEnabled(isChecked, PerceptionDirection.DOWNWARD, object :
                    CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        updateCurrentErrorMsg(isSuccess = true)

                    }

                    override fun onFailure(error: IDJIError) {
                        handleSwitchButtonError(error)
                        binding?.tvObstacleAvoidanceDownSwitch?.setOnCheckedChangeListener(null)
                        binding?.tvObstacleAvoidanceDownSwitch?.isChecked = !isChecked
                        binding?.tvObstacleAvoidanceDownSwitch?.setOnCheckedChangeListener(this@PerceptionFragment)
                    }
                })
            }

            binding?.tvObstacleAvoidanceHorizontalSwitch -> {
                perceptionVM.setObstacleAvoidanceEnabled(isChecked, PerceptionDirection.HORIZONTAL, object :
                    CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        updateCurrentErrorMsg(isSuccess = true)
                    }

                    override fun onFailure(error: IDJIError) {
                        handleSwitchButtonError(error)
                        binding?.tvObstacleAvoidanceHorizontalSwitch?.setOnCheckedChangeListener(null)
                        binding?.tvObstacleAvoidanceHorizontalSwitch?.isChecked = !isChecked
                        binding?.tvObstacleAvoidanceHorizontalSwitch?.setOnCheckedChangeListener(this@PerceptionFragment)
                    }
                })
            }

            //雷达避障子开关
            binding?.tvRadarObstacleAvoidanceUpSwitch -> {
                perceptionVM.setRadarObstacleAvoidanceEnabled(isChecked, PerceptionDirection.UPWARD, object :
                    CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        updateCurrentErrorMsg(isSuccess = true)

                    }

                    override fun onFailure(error: IDJIError) {
                        handleSwitchButtonError(error)
                        binding?.tvRadarObstacleAvoidanceUpSwitch?.setOnCheckedChangeListener(null)
                        binding?.tvRadarObstacleAvoidanceUpSwitch?.isChecked = !isChecked
                        binding?.tvRadarObstacleAvoidanceUpSwitch?.setOnCheckedChangeListener(this@PerceptionFragment)
                    }
                })
            }

            binding?.tvRadarObstacleAvoidanceHorizontalSwitch -> {
                perceptionVM.setRadarObstacleAvoidanceEnabled(isChecked, PerceptionDirection.HORIZONTAL, object :
                    CommonCallbacks.CompletionCallback {
                    override fun onSuccess() {
                        updateCurrentErrorMsg(isSuccess = true)

                    }

                    override fun onFailure(error: IDJIError) {
                        handleSwitchButtonError(error)
                        binding?.tvRadarObstacleAvoidanceHorizontalSwitch?.setOnCheckedChangeListener(null)
                        binding?.tvRadarObstacleAvoidanceHorizontalSwitch?.isChecked = !isChecked
                        binding?.tvRadarObstacleAvoidanceHorizontalSwitch?.setOnCheckedChangeListener(this@PerceptionFragment)
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
            binding?.tvErrorMsg?.text = if (isSuccess) "" else errorMsg
        }
    }

    private fun showToast(toastMsg: String) {
        ToastUtils.showToast(toastMsg)
    }

    private fun observerPerceptionInfo() {
        perceptionVM.perceptionInfo.observe(viewLifecycleOwner) {
            perceptionInfo = it
            updatePerceptionInfo()
            changeObstacleAvoidanceEnableSwitch()
            changeOtherEnableSwitch(it)
        }
        perceptionVM.obstacleData.observe(viewLifecycleOwner) {
            obstacleData = it
            updatePerceptionInfo()
        }
        perceptionVM.obstacleDataForRadar.observe(viewLifecycleOwner) {
            radarObstacleData = it
            updatePerceptionInfo()
        }

        perceptionVM.radarInformation.observe(viewLifecycleOwner) {
            radarInformation = it
            changeObstacleAvoidanceEnableSwitch()
            updatePerceptionInfo()
        }
        perceptionVM.radarConnect.observe(viewLifecycleOwner) {
            isRadarConnected = it
            if (it) {
                binding?.rlRadarObstacleAvoidanceSwitch?.show()
            } else {
                binding?.rlRadarObstacleAvoidanceSwitch?.hide()
            }
        }
    }

    private fun changeOtherEnableSwitch(perceptionInfo: PerceptionInfo) {
        perceptionInfo.apply {
            //视觉定位
            val checked2 = binding?.tbSetVisionPositioningEnableSwitch?.isChecked
            if (checked2 != isVisionPositioningEnabled) {
                binding?.tbSetVisionPositioningEnableSwitch?.setOnCheckedChangeListener(null)
                binding?.tbSetVisionPositioningEnableSwitch?.isChecked = isVisionPositioningEnabled
                binding?.tbSetVisionPositioningEnableSwitch?.setOnCheckedChangeListener(this@PerceptionFragment)
            }
            //精准降落
            val check3 = binding?.tbSetPrecisionLandingEnableSwitch?.isChecked
            if (check3 != isPrecisionLandingEnabled) {
                binding?.tbSetPrecisionLandingEnableSwitch?.setOnCheckedChangeListener(null)
                binding?.tbSetPrecisionLandingEnableSwitch?.isChecked = isPrecisionLandingEnabled
                binding?.tbSetPrecisionLandingEnableSwitch?.setOnCheckedChangeListener(this@PerceptionFragment)
            }
        }
    }

    private fun changeObstacleAvoidanceEnableSwitch() {
        perceptionInfo?.apply {
            //避障子开关
            val checked1 = binding?.tvObstacleAvoidanceUpSwitch?.isChecked
            if (checked1 != isUpwardObstacleAvoidanceEnabled) {
                binding?.tvObstacleAvoidanceUpSwitch?.setOnCheckedChangeListener(null)
                binding?.tvObstacleAvoidanceUpSwitch?.isChecked = isUpwardObstacleAvoidanceEnabled
                binding?.tvObstacleAvoidanceUpSwitch?.setOnCheckedChangeListener(this@PerceptionFragment)
            }

            val checked2 = binding?.tvObstacleAvoidanceDownSwitch?.isChecked
            if (checked2 != isDownwardObstacleAvoidanceEnabled) {
                binding?.tvObstacleAvoidanceDownSwitch?.setOnCheckedChangeListener(null)
                binding?.tvObstacleAvoidanceDownSwitch?.isChecked = isDownwardObstacleAvoidanceEnabled
                binding?.tvObstacleAvoidanceDownSwitch?.setOnCheckedChangeListener(this@PerceptionFragment)
            }

            val checked3 = binding?.tvObstacleAvoidanceHorizontalSwitch?.isChecked
            if (checked3 != isHorizontalObstacleAvoidanceEnabled) {
                binding?.tvObstacleAvoidanceHorizontalSwitch?.setOnCheckedChangeListener(null)
                binding?.tvObstacleAvoidanceHorizontalSwitch?.isChecked = isHorizontalObstacleAvoidanceEnabled
                binding?.tvObstacleAvoidanceHorizontalSwitch?.setOnCheckedChangeListener(this@PerceptionFragment)
            }
        }
        radarInformation?.apply {
            val checked1 = binding?.tvRadarObstacleAvoidanceUpSwitch?.isChecked
            if (checked1 != isUpwardObstacleAvoidanceEnabled) {
                binding?.tvRadarObstacleAvoidanceUpSwitch?.setOnCheckedChangeListener(null)
                binding?.tvRadarObstacleAvoidanceUpSwitch?.isChecked = isUpwardObstacleAvoidanceEnabled
                binding?.tvRadarObstacleAvoidanceUpSwitch?.setOnCheckedChangeListener(this@PerceptionFragment)
            }

            val checked2 = binding?.tvRadarObstacleAvoidanceHorizontalSwitch?.isChecked
            if (checked2 != isHorizontalObstacleAvoidanceEnabled) {
                binding?.tvRadarObstacleAvoidanceHorizontalSwitch?.setOnCheckedChangeListener(null)
                binding?.tvRadarObstacleAvoidanceHorizontalSwitch?.isChecked = isHorizontalObstacleAvoidanceEnabled
                binding?.tvRadarObstacleAvoidanceHorizontalSwitch?.setOnCheckedChangeListener(this@PerceptionFragment)
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
                append("isUpwardObstacleAvoidanceWorking:$isUpwardObstacleAvoidanceEnabled").append("\n")
                append("isLeftSideObstacleAvoidanceWorking:$leftSideObstacleAvoidanceWorking").append("\n")
                append("isRightSideObstacleAvoidanceWorking:$rightSideObstacleAvoidanceWorking").append("\n")
                append("isBackwardObstacleAvoidanceWorking:$backwardObstacleAvoidanceWorking").append("\n")
                append("isForwardObstacleAvoidanceWorking:$forwardObstacleAvoidanceWorking").append("\n")
                append("isDownwardObstacleAvoidanceWorking:$downwardObstacleAvoidanceWorking").append("\n")
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
            binding?.tvPerceptionInfo?.text = result
        }
    }
}