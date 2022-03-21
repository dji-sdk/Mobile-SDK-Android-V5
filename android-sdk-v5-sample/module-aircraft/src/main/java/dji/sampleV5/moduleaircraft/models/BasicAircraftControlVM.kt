package dji.sampleV5.moduleaircraft.models

import dji.sampleV5.modulecommon.models.DJIViewModel
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.v5.common.callback.CommonCallbacks
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.manager.KeyManager

class BasicAircraftControlVM : DJIViewModel() {

    fun startTakeOff(callback: CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>) {
        KeyManager.getInstance()
            .performAction(KeyTools.createKey(FlightControllerKey.KeyStartTakeoff), null, callback)
    }

    fun startLanding(callback: CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>) {
        KeyManager.getInstance()
            .performAction(KeyTools.createKey(FlightControllerKey.KeyStartAutoLanding), null, callback)
    }
}