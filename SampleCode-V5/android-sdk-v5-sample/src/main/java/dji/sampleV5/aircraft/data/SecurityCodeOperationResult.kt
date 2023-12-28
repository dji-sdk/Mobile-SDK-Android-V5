package dji.sampleV5.aircraft.data

import androidx.annotation.StringRes
import dji.sampleV5.aircraft.R
import dji.sdk.errorcode.DJIErrorCode
import dji.v5.utils.common.StringUtils

enum class SecurityCodeOperationResult(val resultCode: Int, @param:StringRes val desResId: Int) {
    NO_ERR(0, R.string.securitycode_operation_result_success),
    BUSY(DJIErrorCode.ACCESS_LOCKER_V1_BUSY.value(), R.string.securitycode_operation_result_busy),
    SET_FAILED(DJIErrorCode.ACCESS_LOCKER_V1_PW_SET_FAILED.value(), R.string.securitycode_operation_result_set_failed),
    VERIFY_FAILED(DJIErrorCode.ACCESS_LOCKER_V1_PW_VERIFY_FAILED.value(), R.string.securitycode_operation_result_verify_failed),
    NEW_PW_REPEAT(DJIErrorCode.ACCESS_LOCKER_V1_NEW_PW_REPEAT.value(), R.string.securitycode_operation_result_new_pw_repeat),
    RESET_FAILED(DJIErrorCode.ACCESS_LOCKER_V1_RESET_FAILED.value(), R.string.securitycode_operation_result_reset_failed),
    USERNAME_INVALID(DJIErrorCode.ACCESS_LOCKER_USER_NAME_FORMAT_INVALID.value(), R.string.securitycode_operation_result_user_name_format_invalid),
    CONTROL_NOT_SUPPORT(DJIErrorCode.ACCESS_LOCKER_V1_CONTROL_NOT_SUPPORT.value(), R.string.securitycode_operation_result_control_not_support),
    FEATURE_NOT_SUPPORT(DJIErrorCode.FEATURE_NOT_SUPPORTED.value(), R.string.securitycode_operation_result_feature_not_support),
    COMMAND_NOT_SUPPORT(DJIErrorCode.COMMAND_NOT_SUPPORT_NOW.value(), R.string.securitycode_operation_result_command_not_support),
    NOT_CURRENT_DEVICE(0xFFFF, R.string.securitycode_operation_result_not_current_device_result),
    UNKNOWN(DJIErrorCode.UNKNOWN.value(), R.string.securitycode_operation_result_unknown);

    val resultDes: String
        get() = StringUtils.getResStr(desResId)

    companion object {
        fun find(code: Int): SecurityCodeOperationResult {
            for (operationResult in values()) {
                if (operationResult.resultCode == code) {
                    return operationResult
                }
            }
            return UNKNOWN
        }
    }
}