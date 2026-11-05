package dji.sampleV5.aircraft.pages

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import dji.sampleV5.aircraft.data.QuickTestConfig
import dji.sampleV5.aircraft.databinding.FragSimulatorPageBinding
import dji.sampleV5.aircraft.models.SimulatorVM
import dji.v5.ux.util.Helper
import dji.v5.ux.util.ToastUtils
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.simulator.InitializationSettings
import dji.v5.manager.areacode.AreaCodeManager
import dji.v5.utils.common.LogUtils

/**
 * @author feel.feng
 * @time 2022/01/26 11:22 上午
 * @description:
 */
class SimulatorFragment : DJIFragment() {
    private val simulatorVM: SimulatorVM by activityViewModels()
    private var binding: FragSimulatorPageBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragSimulatorPageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListener()
    }

    private fun initListener() {
        binding?.btnEnableSimulator?.setOnClickListener {
            enableSimulator()
        }

        binding?.btnDisableSimulator?.setOnClickListener {
            disableSimulator(null)
        }

        binding?.btnSetAreacode?.setOnClickListener {
            updateAreaCode()
        }

        simulatorVM.simulatorStateSb.observe(viewLifecycleOwner) {
            binding?.simulatorStateInfoTv?.apply {
                text = it
                setTextColor(if (simulatorVM.isSimulatorOn()) Color.BLACK else Color.RED)
            }
        }

        binding?.btnQuickSimulatorArea?.setOnClickListener {
            initPopupNumberPicker(Helper.makeList(simulatorVM.quickInfo)) {
                updateView(simulatorVM.quickInfo[indexChosen[0]])
                if (simulatorVM.isSimulatorOn()) {
                    disableSimulator(object : CommonCallbacks.CompletionCallback {
                        override fun onSuccess() {
                            enableSimulator()
                        }

                        override fun onFailure(error: IDJIError) {
                            enableSimulator()
                        }
                    })
                } else {
                    enableSimulator()
                }
                updateAreaCode()
                resetIndex()
            }
        }
    }

    private fun enableSimulator() {
        val coordinate2D = LocationCoordinate2D(binding?.simulatorLatEt?.text.toString().toDouble(), binding?.simulatorLngEt?.text.toString().toDouble())
        val data = InitializationSettings.createInstance(coordinate2D, binding?.simulatorGpsNumEt?.text.toString().toInt())
        simulatorVM.enableSimulator(data, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                ToastUtils.showToast("start Success")
                mainHandler.post {
                    binding?.simulatorStateInfoTv?.setTextColor(Color.BLACK)
                }
            }

            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast("start Failed" + error.description())
            }
        })
    }

    private fun disableSimulator(callbacks: CommonCallbacks.CompletionCallback?) {
        simulatorVM.disableSimulator(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                ToastUtils.showToast("disable Success")
                mainHandler.post { binding?.simulatorStateInfoTv?.setTextColor(Color.RED) }
                callbacks?.onSuccess()
            }

            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast("close Failed" + error.description())
                callbacks?.onFailure(error)
            }
        })
    }

    private fun updateAreaCode() {
        val areCode = binding?.areacodeEt?.text.toString()
        LogUtils.d(tag, "areCode:$areCode")
        val idjiError = AreaCodeManager.getInstance().updateAreaCode(areCode)
        if (idjiError == null) {
            ToastUtils.showToast("Success")
        } else {
            ToastUtils.showToast(idjiError.toString())
        }
    }

    private fun updateView(info: QuickTestConfig.SimulatorArea) {
        binding?.simulatorLatEt?.setText(info.location.latitude.toString())
        binding?.simulatorLngEt?.setText(info.location.longitude.toString())
        binding?.areacodeEt?.setText(info.areaCode.value())
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
    }
}