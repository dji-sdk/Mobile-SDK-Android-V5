package dji.sampleV5.aircraft.data

import dji.sampleV5.aircraft.R
import dji.v5.common.register.SDKLinKMode

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/5/7
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class CommonFragmentPageInfoFactory : IFragmentPageInfoFactory {

    override fun createPageInfo(): FragmentPageItemList {
        return FragmentPageItemList(R.navigation.nav_common).apply {
            // 插件化布局文件暂未使用，后续如需启用取消注释即可
            // items.add(FragmentPageItem(R.id.normal_plugin_page, R.string.item_normal_title, R.string.item_normal_description, listOf(SDKLinKMode.REMOTE_PILOT)))
            items.add(FragmentPageItem(R.id.key_value_page, R.string.item_key_value_title, R.string.item_key_value_description,listOf(SDKLinKMode.LOCAL,SDKLinKMode.REMOTE_PILOT)))
            items.add(FragmentPageItem(R.id.key_multi_camera_stream_page, R.string.item_multi_camera_stream_title, R.string.item_multi_camera_stream_description, listOf(SDKLinKMode.LOCAL,SDKLinKMode.REMOTE_PILOT)))
            items.add(FragmentPageItem(R.id.key_data_protection_page, R.string.item_data_protection_title, R.string.item_data_protection_description,listOf(SDKLinKMode.LOCAL,SDKLinKMode.REMOTE_PILOT)))
            items.add(FragmentPageItem(R.id.key_diagnostic_page, R.string.item_diagnostic_title, R.string.item_diagnostic_description, listOf(SDKLinKMode.LOCAL)))
            items.add(FragmentPageItem(R.id.key_media_playback_page, R.string.item_media_playback_title, R.string.item_media_playback_description, listOf(SDKLinKMode.LOCAL,SDKLinKMode.REMOTE_PILOT)))
            items.add(FragmentPageItem(R.id.key_live_stream_page, R.string.item_live_stream_title, R.string.item_live_stream_description, listOf(SDKLinKMode.LOCAL,SDKLinKMode.REMOTE_PILOT)))
            items.add(FragmentPageItem(R.id.key_login_account_page, R.string.item_login_account_title, R.string.item_login_account_description,listOf(SDKLinKMode.LOCAL,SDKLinKMode.REMOTE_PILOT)))
            items.add(FragmentPageItem(R.id.key_log_info_page, R.string.item_log_info_title, R.string.item_log_info_description, listOf(SDKLinKMode.LOCAL,SDKLinKMode.REMOTE_PILOT)))
            items.add(FragmentPageItem(R.id.key_diagnostic_page, R.string.item_diagnostic_title, R.string.item_diagnostic_description,listOf(SDKLinKMode.LOCAL)))
            items.add(FragmentPageItem(R.id.key_app_silently_upgrade_page, R.string.item_app_silently_upgrade_title, R.string.item_app_silently_upgrade_description,listOf(SDKLinKMode.LOCAL,SDKLinKMode.REMOTE_PILOT)))
            // items.add(FragmentPageItem(R.id.ar_projection_page, R.string.item_ar_projection_title, R.string.item_ar_projection_description, listOf(SDKLinKMode.LOCAL,SDKLinKMode.REMOTE_PILOT)))
            // items.add(FragmentPageItem(R.id.cable_inspection_page, R.string.item_cable_inspection_title, R.string.item_cable_inspection_description, listOf(SDKLinKMode.REMOTE_PILOT)))
            // items.add(FragmentPageItem(R.id.remote_control_page, R.string.item_remote_control_title, R.string.item_remote_control_description, listOf(SDKLinKMode.LOCAL)))
        }
    }
}