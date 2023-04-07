package dji.v5.ux.accessory

import android.text.TextUtils
import android.view.View
import dji.rtk.CoordinateSystem
import dji.sdk.keyvalue.value.rtkbasestation.RTKCustomNetworkSetting
import dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource
import dji.sdk.keyvalue.value.rtkmobilestation.RTKPositioningSolution
import dji.v5.manager.aircraft.rtk.RTKCenter
import dji.v5.utils.common.ContextUtil
import dji.v5.utils.common.DjiSharedPreferencesManager
import dji.v5.utils.common.JsonUtil
import dji.v5.utils.common.LogUtils
import dji.v5.ux.R
import dji.v5.ux.core.extension.getString

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2022/5/24
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */

object RTKUtil {
    private const val TAG = "RTKUtil"
    private const val USER_RTK_NETWORK_SERVICE_SETTINGS = "user_rtk_network_service_settings"
    private const val USER_RTK_REFERENCE_SOURCE = "user_rtk_reference_source"
    private const val USER_RTK_NETWORK_COORDINATE_SYSTEM_CMCC = "user_rtk_network_coordinate_system_cmcc"
    private const val USER_RTK_NETWORK_COORDINATE_SYSTEM_QX = "user_rtk_network_coordinate_system_qx"
    fun getRTKTypeName(view: View, rtkReferenceStationSource: RTKReferenceStationSource?): String {
        view.run {
            if (rtkReferenceStationSource == null) {
                return getString(R.string.uxsdk_rtk_type_unknown_rtk);
            }
            return when (rtkReferenceStationSource) {
                RTKReferenceStationSource.QX_NETWORK_SERVICE -> getString(R.string.uxsdk_rtk_type_nrtk)
                RTKReferenceStationSource.BASE_STATION -> getString(R.string.uxsdk_rtk_type_rtk_mobile_station)
                RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE -> getString(R.string.uxsdk_rtk_type_custom_rtk)
                RTKReferenceStationSource.NTRIP_NETWORK_SERVICE -> getString(R.string.uxsdk_rtk_type_cmcc_rtk)
                else -> getString(R.string.uxsdk_rtk_type_unknown_rtk)
            }
        }

    }

    fun getRTKStatusName(view: View, positioningSolution: RTKPositioningSolution?): String {
        view.run {
            if (positioningSolution == null) {
                return getString(R.string.uxsdk_rtk_solution_unknown)
            }
            return when (positioningSolution) {
                RTKPositioningSolution.NONE -> getString(R.string.uxsdk_rtk_solution_none)
                RTKPositioningSolution.SINGLE_POINT -> getString(R.string.uxsdk_rtk_solution_single)
                RTKPositioningSolution.FLOAT -> getString(R.string.uxsdk_rtk_solution_float)
                RTKPositioningSolution.FIXED_POINT -> getString(R.string.uxsdk_rtk_solution_fixed)
                RTKPositioningSolution.UNKNOWN -> getString(R.string.uxsdk_rtk_solution_unknown)
                else -> getString(R.string.uxsdk_rtk_solution_unknown)
            }
        }
    }

    fun getNetRTKCoordinateSystem(rtkReferenceStationSource: RTKReferenceStationSource): CoordinateSystem? {
        var coordinateSystem = ""
        when (rtkReferenceStationSource) {
            RTKReferenceStationSource.NTRIP_NETWORK_SERVICE -> {
                coordinateSystem = DjiSharedPreferencesManager.getString(ContextUtil.getContext(),
                    USER_RTK_NETWORK_COORDINATE_SYSTEM_CMCC, "")
            }
            RTKReferenceStationSource.QX_NETWORK_SERVICE -> {
                coordinateSystem = DjiSharedPreferencesManager.getString(ContextUtil.getContext(), USER_RTK_NETWORK_COORDINATE_SYSTEM_QX, "")
            }
            else -> {
                LogUtils.e(TAG, "getNetRTKCoordinateSystem error,unSupport rtkReferenceStationSource:$rtkReferenceStationSource")
            }
        }
        return if (TextUtils.isEmpty(coordinateSystem)) null else getCoordinateName(coordinateSystem)

    }

    fun saveRTKCoordinateSystem(rtkReferenceStationSource: RTKReferenceStationSource, coordinateSystem: CoordinateSystem) {
        when (rtkReferenceStationSource) {
            RTKReferenceStationSource.QX_NETWORK_SERVICE -> {
                DjiSharedPreferencesManager.putString(ContextUtil.getContext(), USER_RTK_NETWORK_COORDINATE_SYSTEM_QX, coordinateSystem.name)
            }

            RTKReferenceStationSource.NTRIP_NETWORK_SERVICE -> {
                DjiSharedPreferencesManager.putString(ContextUtil.getContext(), USER_RTK_NETWORK_COORDINATE_SYSTEM_CMCC, coordinateSystem.name)
            }
            else -> {
                LogUtils.e(TAG, "saveRTKCoordinateSystem error,unSupport rtkReferenceStationSource:$rtkReferenceStationSource")
            }
        }
    }


    fun saveRtkCustomNetworkSetting(settings: RTKCustomNetworkSetting) {
        DjiSharedPreferencesManager.putString(ContextUtil.getContext(), USER_RTK_NETWORK_SERVICE_SETTINGS, settings.toString())
    }

    fun getRtkCustomNetworkSetting(): RTKCustomNetworkSetting? {
        val localSetting = DjiSharedPreferencesManager.getString(ContextUtil.getContext(), USER_RTK_NETWORK_SERVICE_SETTINGS, "")
        return if (!TextUtils.isEmpty(localSetting)) {
            JsonUtil.toBean(localSetting, RTKCustomNetworkSetting::class.java)
        } else RTKCenter.getInstance().customRTKManager.customNetworkRTKSettings
            ?: RTKCustomNetworkSetting(
                "",
                0,
                "",
                "",
                ""
            )

    }

    private fun getCoordinateName(value: String): CoordinateSystem {
        return when (value) {
            CoordinateSystem.CGCS2000.name ->
                CoordinateSystem.CGCS2000
            CoordinateSystem.WGS84.name ->
                CoordinateSystem.WGS84
            else -> {
                //如果未设置过坐标系，则默认使用CGCS2000作为启动的坐标系
                CoordinateSystem.CGCS2000
            }
        }
    }
}