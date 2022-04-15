package dji.sampleV5.modulecommon.data

import dji.sampleV5.modulecommon.R

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/5/7
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class CommonFragmentPageInfoFactory : IFragmentPageInfoFactory {

    override fun createPageInfo(): FragmentPageInfo {
        return FragmentPageInfo(R.navigation.nav_common).apply {
            items.add(FragmentPageInfoItem(R.id.key_value_page, R.string.item_key_value_title, R.string.item_key_value_description))
            items.add(FragmentPageInfoItem(R.id.multi_video_decoding_page, R.string.item_multi_video_decoding_title, R.string.item_multi_video_decoding_description))
            items.add(FragmentPageInfoItem(R.id.key_diagnostic_page, R.string.item_diagnostic_title, R.string.item_diagnostic_description))
            items.add(FragmentPageInfoItem(R.id.key_live_stream_page, R.string.item_live_stream_title, R.string.item_live_stream_description))
        }
    }
}