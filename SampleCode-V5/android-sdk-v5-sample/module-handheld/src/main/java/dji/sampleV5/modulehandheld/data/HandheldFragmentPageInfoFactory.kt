package dji.sampleV5.modulehandheld.data

import dji.sampleV5.modulecommon.data.FragmentPageItemList
import dji.sampleV5.modulecommon.data.FragmentPageItem
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

    override fun createPageInfo(): FragmentPageItemList {
        return FragmentPageItemList(R.navigation.nav_handheld).apply {
            items.add(FragmentPageItem(R.id.handle_page, R.string.item_handheld_title, R.string.item_handheld_description))
        }
    }
}