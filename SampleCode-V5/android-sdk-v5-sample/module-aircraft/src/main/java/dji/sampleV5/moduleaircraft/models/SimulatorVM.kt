package dji.sampleV5.moduleaircraft.models

import dji.sampleV5.modulecommon.models.DJIViewModel
import dji.v5.common.callback.CommonCallbacks
import dji.v5.manager.aircraft.simulator.SimulatorManager
import dji.v5.manager.aircraft.simulator.InitializationSettings
import dji.v5.manager.aircraft.simulator.SimulatorStatusListener

/**
 * @author feel.feng
 * @time 2022/01/26 10:56 上午
 * @description:
 */
class SimulatorVM : DJIViewModel() {

    fun enableSimulator(initializationSettings: InitializationSettings, callback: CommonCallbacks.CompletionCallback){
        SimulatorManager.getInstance().enableSimulator(initializationSettings , callback);
    }

    fun disableSimulator(callback: CommonCallbacks.CompletionCallback) {
        SimulatorManager.getInstance().disableSimulator(callback);
    }

    fun addSimulatorListener(listener: SimulatorStatusListener) {
        SimulatorManager.getInstance().addSimulatorStateListener(listener);
    }

    fun removeSimulatorListener(listener: SimulatorStatusListener) {
        SimulatorManager.getInstance().removeSimulatorStateListener(listener)
    }

    fun  removeAllSimulatorListener() {
        SimulatorManager.getInstance().clearAllSimulatorStateListener()
    }

    fun  isSimulatorOn():Boolean{
        return SimulatorManager.getInstance().isSimulatorEnabled;
    }
}