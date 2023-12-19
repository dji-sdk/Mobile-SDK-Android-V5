
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.pages.DJIFragment
import dji.sampleV5.moduleaircraft.models.UASChinaVM
import dji.sdk.keyvalue.value.flightcontroller.RealNameRegistrationState
import dji.v5.utils.common.JsonUtil
import kotlinx.android.synthetic.main.frag_uas_china_page.*
import java.util.*

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2023/11/17
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class UASChinaFragment : DJIFragment() {
    private val TAG = "UASChinaFragment"

    private val uasChinaVM: UASChinaVM by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_uas_china_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uasChinaVM.addRealNameRegistrationStatusListener()

        uasChinaVM.uasRemoteIDStatus.observe(viewLifecycleOwner) {
            updateInfo()
        }
        uasChinaVM.uomRealNameFCStatus.observe(viewLifecycleOwner) {
            updateInfo()
        }

        bt_update_real_name_state.setOnClickListener {
            uasChinaVM.updateRealNameRegistrationStateFromUOM()
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        uasChinaVM.removeRealNameRegistrationStatusListener()
    }

    private fun updateInfo() {
        val builder = StringBuilder()
        builder.append("Uas Uom Real Name Status:")
            .append(uasChinaVM.uomRealNameFCStatus.value?.name +getRealNameStatusTip(uasChinaVM.uomRealNameFCStatus.value))
        builder.append("\n")
        mainHandler.post {
            tv_uas_china_real_name_status_tip.text = builder.toString()
        }
    }

    private fun getRealNameStatusTip(state: RealNameRegistrationState?): String {
        if (!isZh()) {
            return ""
        }
        return "-" +when (state) {
              RealNameRegistrationState.NOT_AUTH -> "未认证"
              RealNameRegistrationState.VAILD_AUTH -> "已认证"
              RealNameRegistrationState.CANCELLED -> "已注销"
              RealNameRegistrationState.NETWORK_ERROR -> "网络错误"
              RealNameRegistrationState.VERIFIED_AND_CANCLLED -> "飞机认证后注销"
              RealNameRegistrationState.UNSUPPORTED -> "飞机不支持实名认证功能"
              RealNameRegistrationState.NOT_ACTIVE_YET -> "飞机未激活"
              RealNameRegistrationState.DONT_NEED_CHECK_REALNAME -> "无需实名认证"
              RealNameRegistrationState.UNLOCKED -> "飞机已经解禁"
              RealNameRegistrationState.DONT_IN_CHINA_MAINLAND -> "不在中国大陆"
              RealNameRegistrationState.TMP_VALID_AUTH -> "临时有效"
              else -> "未知状态"
          }

    }

    fun isZh(): Boolean {
        val locale: Locale? = context?.resources?.configuration?.locale
        val language: String? = locale?.language
        return language?.endsWith("zh") ?: false
    }

}