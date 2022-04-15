package dji.sampleV5.moduleaircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.modulecommon.models.DJIViewModel
import dji.sdk.keyvalue.key.RemoteControllerKey
import dji.sdk.keyvalue.value.flightcontroller.FlightControlAuthority
import dji.sdk.keyvalue.value.flightcontroller.FlightControlAuthorityChangeReason
import dji.v5.common.callback.CommonCallbacks
import dji.v5.et.create
import dji.v5.et.listen
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager
import dji.v5.manager.aircraft.virtualstick.VirtualStickState
import dji.v5.manager.aircraft.virtualstick.VirtualStickStateListener

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/6/18
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class VirtualStickVM : DJIViewModel() {

    val currentSpeedLevel = MutableLiveData(0.0)
    var useRcStick = MutableLiveData(false)
    val currentVirtualStickStateInfo = MutableLiveData(VirtualStickStateInfo())

    // RC Stick Value
    private var RCStickLeftHorizontal = 0
    private var RCStickLeftVertical = 0
    private var RCStickRightHorizontal = 0
    private var RCStickRightVertical = 0

    init {
        currentSpeedLevel.value = VirtualStickManager.getInstance().speedLevel
        VirtualStickManager.getInstance().setVirtualStickStateListener(object :
            VirtualStickStateListener {
            override fun onVirtualStickStateUpdate(stickState: VirtualStickState) {
                currentVirtualStickStateInfo.postValue(currentVirtualStickStateInfo.value?.apply {
                    this.state = stickState
                })
            }

            override fun onChangeReasonUpdate(reason: FlightControlAuthorityChangeReason) {
                currentVirtualStickStateInfo.postValue(currentVirtualStickStateInfo.value?.apply {
                    this.reason = reason
                })
            }
        })
    }

    fun enableVirtualStick(callback: CommonCallbacks.CompletionCallback) {
        VirtualStickManager.getInstance().enableVirtualStick(callback)
    }

    fun disableVirtualStick(callback: CommonCallbacks.CompletionCallback) {
        VirtualStickManager.getInstance().disableVirtualStick(callback)
    }

    fun setSpeedLevel(speedLevel: Double) {
        VirtualStickManager.getInstance().speedLevel = speedLevel
        currentSpeedLevel.value = speedLevel
    }

    fun setLeftPosition(horizontal: Int, vertical: Int) {
        VirtualStickManager.getInstance().leftStick.horizontalPosition = horizontal
        VirtualStickManager.getInstance().leftStick.verticalPosition = vertical
    }

    fun setRightPosition(horizontal: Int, vertical: Int) {
        VirtualStickManager.getInstance().rightStick.horizontalPosition = horizontal
        VirtualStickManager.getInstance().rightStick.verticalPosition = vertical
    }

    fun listenRCStick() {
        RemoteControllerKey.KeyStickLeftHorizontal.create().listen(this) {
            RCStickLeftHorizontal = it ?: 0
            tryUpdateVirtualStickByRc()
        }
        RemoteControllerKey.KeyStickLeftVertical.create().listen(this) {
            RCStickRightHorizontal = it ?: 0
            tryUpdateVirtualStickByRc()
        }
        RemoteControllerKey.KeyStickRightHorizontal.create().listen(this) {
            RCStickRightHorizontal = it ?: 0
            tryUpdateVirtualStickByRc()
        }
        RemoteControllerKey.KeyStickRightVertical.create().listen(this) {
            RCStickRightVertical = it ?: 0
            tryUpdateVirtualStickByRc()
        }
    }

    private fun tryUpdateVirtualStickByRc() {
        if (useRcStick.value == true) {
            setLeftPosition(RCStickLeftHorizontal, RCStickLeftVertical)
            setRightPosition(RCStickRightHorizontal, RCStickRightVertical)
        }
    }

    override fun onCleared() {
        KeyManager.getInstance().cancelListen(this)
        VirtualStickManager.getInstance().clearAllVirtualStickStateListener()
    }

    data class VirtualStickStateInfo(
        var state: VirtualStickState = VirtualStickState(false, FlightControlAuthority.UNKNOWN),
        var reason: FlightControlAuthorityChangeReason = FlightControlAuthorityChangeReason.UNKNOWN
    )
}