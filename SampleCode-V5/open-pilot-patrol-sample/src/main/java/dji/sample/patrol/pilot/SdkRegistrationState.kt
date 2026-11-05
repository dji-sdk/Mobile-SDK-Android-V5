package dji.sample.patrol.pilot

sealed class SdkRegistrationState {
    object Unregistered : SdkRegistrationState()
    object Registered : SdkRegistrationState()
    data class Failed(val message: String) : SdkRegistrationState()
}
