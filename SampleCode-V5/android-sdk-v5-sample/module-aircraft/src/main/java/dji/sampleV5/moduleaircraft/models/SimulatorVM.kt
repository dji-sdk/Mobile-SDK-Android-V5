package dji.sampleV5.moduleaircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.modulecommon.models.DJIViewModel
import dji.v5.common.callback.CommonCallbacks
import dji.v5.manager.aircraft.simulator.SimulatorManager
import dji.v5.manager.aircraft.simulator.InitializationSettings
import dji.v5.manager.aircraft.simulator.SimulatorStatusListener
import kotlinx.android.synthetic.main.frag_simulator_page.*

/**
 * @author feel.feng
 * @time 2022/01/26 10:56 上午
 * @description:
 */
class SimulatorVM : DJIViewModel() {

    val simulatorStateSb = MutableLiveData(StringBuffer())

    private val simulatorStateListener = SimulatorStatusListener { state ->
        simulatorStateSb.value?.apply {
            setLength(0)
            append("Motor On : " + state.areMotorsOn())
            append("\n")
            append("In the Air : " + state.isFlying)
            append("\n")
            append("Roll : " + state.roll)
            append("\n")
            append("Pitch : " + state.pitch)
            append("\n")
            append("Yaw : " + state.yaw)
            append("\n")
            append("PositionX : " + state.positionX)
            append("\n")
            append("PositionY : " + state.positionY)
            append("\n")
            append("PositionZ : " + state.positionZ)
            append("\n")
            append("Latitude : " + state.location.latitude)
            append("\n")
            append("Longitude : " + state.location.longitude)
            append("\n")
        }
        simulatorStateSb.postValue(simulatorStateSb.value)
    }

    init {
        addSimulatorListener()
    }

    override fun onCleared() {
        removeSimulatorListener()
    }

    fun enableSimulator(initializationSettings: InitializationSettings, callback: CommonCallbacks.CompletionCallback) {
        SimulatorManager.getInstance().enableSimulator(initializationSettings, callback);
    }

    fun disableSimulator(callback: CommonCallbacks.CompletionCallback) {
        SimulatorManager.getInstance().disableSimulator(callback);
    }

    private fun addSimulatorListener() {
        SimulatorManager.getInstance().addSimulatorStateListener(simulatorStateListener);
    }

    private fun removeSimulatorListener() {
        SimulatorManager.getInstance().removeSimulatorStateListener(simulatorStateListener)
    }

    fun removeAllSimulatorListener() {
        SimulatorManager.getInstance().clearAllSimulatorStateListener()
    }

    fun isSimulatorOn(): Boolean {
        return SimulatorManager.getInstance().isSimulatorEnabled;
    }
}