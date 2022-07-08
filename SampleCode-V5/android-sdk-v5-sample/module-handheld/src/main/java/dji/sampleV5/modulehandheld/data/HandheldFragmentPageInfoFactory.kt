package dji.sampleV5.modulehandheld.data

import dji.sampleV5.modulecommon.data.FragmentPageInfo
import dji.sampleV5.modulecommon.data.FragmentPageInfoItem
import dji.sampleV5.modulecommon.data.IFragmentPageInfoFactory
import dji.sampleV5.modulehandheld.R

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/5/7
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class HandheldFragmentPageInfoFactory : IFragmentPageInfoFactory {

    override fun createPageInfo(): FragmentPageInfo {
        return FragmentPageInfo(R.navigation.nav_handheld).apply {
            items.add(FragmentPageInfoItem(R.id.handle_page, R.string.item_handheld_title, R.string.item_handheld_description))
        }
    }
}