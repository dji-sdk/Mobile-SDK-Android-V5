package dji.sampleV5.aircraft.control

import android.util.Log
import dji.sampleV5.moduleaircraft.models.BasicAircraftControlVM
import dji.sampleV5.moduleaircraft.models.VirtualStickVM
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.sdk.keyvalue.value.flightcontroller.FlightCoordinateSystem
import dji.sdk.keyvalue.value.flightcontroller.RollPitchControlMode
import dji.sdk.keyvalue.value.flightcontroller.VerticalControlMode
import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam
import dji.sdk.keyvalue.value.flightcontroller.YawControlMode
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.utils.common.ToastUtils


/**
 * Class Description
 *
 *
 */

class VirtualStickControl{

    private val virtualStickVM = VirtualStickVM()
    private val basicAircraftControl = BasicAircraftControlVM()
    private val virtualStickFlightControlParam = VirtualStickFlightControlParam()
    val virtualStickState = VirtualStickVM.VirtualStickStateInfo().state
        //VirtualStickStateListener.isVirtualStickEnable()

    init {
        virtualStickVM.listenRCStick()
        virtualStickVM.enableVirtualStickAdvancedMode()
        Log.v("VirtualStickControl", "Virtual Stick Initialized")

    }

    fun enableVirtualStick(){
        virtualStickVM.enableVirtualStick(object : CommonCallbacks.CompletionCallback{
            override fun onSuccess() {
                ToastUtils.showToast("enableVirtualStick success.")
                Log.v("VirtualStickControl", "enableVirtualStick success.")
            }

            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast("enableVirtualStick error: $error")
                Log.v("VirtualStickControl", "enableVirtualStick error: $error")
            }
        })
    }

    fun enableVirtualStickAdvancedMode(){
        virtualStickVM.enableVirtualStickAdvancedMode()
    }

    fun startTakeOff(){
        basicAircraftControl.startTakeOff(object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
            override fun onSuccess(p0: EmptyMsg?) {
                ToastUtils.showToast("startTakeOff success.")
                Log.v("VirtualStickControl", "startTakeOff success.")
            }

            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast("startTakeOff Failure: $error")
                Log.v("VirtualStickControl", "startTakeOff error: $error")
            }
        })
    }

    fun startLanding(){
        basicAircraftControl.startLanding(object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
            override fun onSuccess(p0: EmptyMsg?) {
                ToastUtils.showToast("startLanding success.")
                Log.v("VirtualStickControl", "startLanding success.")
            }

            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast("startLanding error,$error")
                Log.v("VirtualStickControl", "startLanding error,$error")
            }
        })
    }

    fun sendAdvancedVirtualStickData(roll: Double, pitch: Double, yaw: Double,
                                     rollPitchControl: String, rollPitchCoordinate: String, verticalControlMode: String = "POSITION",
                                     verticalThrottle: Double, yawControl: String = "ANGLE"){
        /**
        Documentation available: https://developer.dji.com/doc/mobile-sdk-tutorial/en/basic-introduction/basic-concepts/flight-controller.html#virtual-sticks
        API Documentation: https://developer.dji.com/api-reference-v5/android-api/Components/IVirtualStickManager/Value_FlightController_Struct_VirtualStickFlightControlParam.html
        Roll Pitch Control Options: ANGLE, VELOCITY
        Roll Pitch Coordinate System Options: BODY, GROUND
        Vertical Control Mode Options: VELOCITY, POSITION
        Yaw Control Options: ANGLE, ANGULAR_VELOCITY

         **/
        virtualStickFlightControlParam.setRoll(roll)
        virtualStickFlightControlParam.setPitch(pitch)
        virtualStickFlightControlParam.setYaw(yaw)
        virtualStickFlightControlParam.setVerticalThrottle(verticalThrottle)

        virtualStickFlightControlParam.setVerticalControlMode(VerticalControlMode.valueOf(verticalControlMode))
        virtualStickFlightControlParam.setYawControlMode(YawControlMode.valueOf(yawControl))
        virtualStickFlightControlParam.setRollPitchControlMode(RollPitchControlMode.valueOf(rollPitchControl))
        virtualStickFlightControlParam.setRollPitchCoordinateSystem(FlightCoordinateSystem.valueOf(rollPitchCoordinate))

        virtualStickVM.sendVirtualStickAdvancedParam(virtualStickFlightControlParam)
    }

    fun sendVirtualStickVelocityGround(north: Double, east: Double, yaw: Double, alt: Double){
        // Simplified implementation with coordinate frame information taken into account
        sendAdvancedVirtualStickData(north, east, yaw, "VELOCITY", "GROUND","POSITION", alt, "ANGLE")
    }

    fun sendVirtualStickVelocityBody(forwardVel: Double, roll: Double, yaw: Double, alt: Double){
        // Simplified implementation with coordinate frame information taken into account
        sendAdvancedVirtualStickData(forwardVel, roll, yaw, "VELOCITY", "BODY","POSITION", alt, "ANGLE")
    }
    fun changeRightPosition(hor:Int,vert:Int){
        enableVirtualStick()
        virtualStickVM.setRightPosition(hor,vert)
        endVirtualStick()
    }

    fun changeLeftPosition(hor:Int, vert:Int){
        enableVirtualStick()
        virtualStickVM.setLeftPosition(hor,vert)
        endVirtualStick()
    }

    fun endVirtualStick(){
        virtualStickVM.disableVirtualStick(object : CommonCallbacks.CompletionCallback{
            override fun onSuccess() {
                ToastUtils.showToast("disableVirtualStick success.")
                Log.v("VirtualStickControl", "disableVirtualStick success.")
            }

            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast("disableVirtualStick error: $error")
                Log.v("VirtualStickControl", "disableVirtualStick error: $error")
            }
        }
        )
//        virtualStickVM.disableVirtualStickAdvancedMode()
    }

    fun sendYawAlt(yaw: Double, alt: Double){
        sendAdvancedVirtualStickData(0.0, 0.0, yaw, "ANGLE", "GROUND", "POSITION", alt, "ANGLE")
    }

    fun setAlt(alt: Double){
        sendAdvancedVirtualStickData(0.0, 0.0, 0.0, "ANGLE", "GROUND", "POSITION", alt, "ANGULAR_VELOCITY")
    }

    fun sendYaw(yaw: Double){
        sendAdvancedVirtualStickData(0.0, 0.0, yaw, "ANGLE", "GROUND", "VELOCITY", 0.0, "ANGLE")
    }

    fun sendForwardVel(vel: Double){
        sendAdvancedVirtualStickData(vel, 0.0, 0.0, "VELOCITY", "BODY", "VELOCITY", 0.0, "ANGULAR_VELOCITY")
    }

}