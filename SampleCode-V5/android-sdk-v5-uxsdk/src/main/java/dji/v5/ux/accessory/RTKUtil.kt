package dji.v5.ux.accessory

import android.view.View
import dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource
import dji.sdk.keyvalue.value.rtkmobilestation.RTKPositioningSolution
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
    fun getRTKTypeName(view: View, rtkReferenceStationSource: RTKReferenceStationSource?): String {
        view.run {
            if (rtkReferenceStationSource == null) {
                return getString(R.string.uxsdk_rtk_type_unknown_rtk);
            }
            return when (rtkReferenceStationSource) {
                RTKReferenceStationSource.QX_NETWORK_SERVICE -> getString(R.string.uxsdk_rtk_type_nrtk)
                RTKReferenceStationSource.BASE_STATION -> getString(R.string.uxsdk_rtk_type_rtk_mobile_station)
                RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE -> getString(R.string.uxsdk_rtk_type_custom_rtk)
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
}