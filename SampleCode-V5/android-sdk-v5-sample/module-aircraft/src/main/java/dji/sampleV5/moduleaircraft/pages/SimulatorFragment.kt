package dji.sampleV5.moduleaircraft.pages

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.models.SimulatorVM
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.simulator.*
import dji.sampleV5.modulecommon.util.ToastUtils
import kotlinx.android.synthetic.main.frag_simulator_page.*

/**
 * @author feel.feng
 * @time 2022/01/26 11:22 上午
 * @description:
 */
class SimulatorFragment : DJIFragment() {
    private val simulatorVM: SimulatorVM by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_simulator_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListener()
    }

    private fun initListener() {
        btn_enable_simulator.setOnClickListener {
            simulator_lng_et
            val coordinate2D = LocationCoordinate2D(simulator_lat_et.text.toString().toDouble(), simulator_lng_et.text.toString().toDouble())
            val data = InitializationSettings.createInstance(coordinate2D, simulator_gps_num_et.text.toString().toInt())
            simulatorVM.enableSimulator(data, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast( "start Success")
                    mainHandler.post {
                        simulator_state_info_tv?.setTextColor(Color.BLACK) }
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast( "start Failed" + error.description())

                }

            })
        }


        btn_disable_simulator.setOnClickListener {
            simulatorVM.disableSimulator(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showToast( "disable Success")
                    mainHandler.post { simulator_state_info_tv?.setTextColor(Color.RED) }
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showToast( "close Failed" + error.description())
                }
            })
        }

        simulatorVM.simulatorStateSb.observe(viewLifecycleOwner) {
            simulator_state_info_tv?.apply {
                text = it
                setTextColor(if (simulatorVM.isSimulatorOn()) Color.BLACK else Color.RED)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
    }
}