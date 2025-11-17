package dji.sampleV5.aircraft.data

import dji.sampleV5.aircraft.R


/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/5/7
 *F
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class AircraftFragmentPageInfoFactory : IFragmentPageInfoFactory {

    override fun createPageInfo(): FragmentPageItemList {
        return FragmentPageItemList(R.navigation.nav_aircraft).apply {
            items.add(FragmentPageItem(R.id.virtual_stick_page, R.string.item_virtual_stick_title, R.string.item_virtual_description))
            items.add(FragmentPageItem(R.id.flight_record_page, R.string.item_flight_record_title, R.string.item_flight_record_description))
            items.add(FragmentPageItem(R.id.flight_upgrade_page, R.string.item_upgrade_title, R.string.item_upgrade_description))
            items.add(FragmentPageItem(R.id.flight_simulator_page, R.string.item_simulator_title, R.string.item_simulator_description))
            items.add(FragmentPageItem(R.id.psdk_center_page, R.string.item_psdk_title, R.string.item_psdk_description))
            items.add(FragmentPageItem(R.id.megaphone_page, R.string.item_megaphone_title, R.string.item_megaphone_description))
            items.add(FragmentPageItem(R.id.waypoint_v3_page, R.string.item_waypoint_title, R.string.item_waypoint_description))
            items.add(FragmentPageItem(R.id.waypoint_v3_page, R.string.item_waypoint_title, R.string.item_waypoint_description))
            items.add(FragmentPageItem(R.id.rtk_center_page, R.string.item_trk_center_title, R.string.item_trk_center_description))
            items.add(FragmentPageItem(R.id.perception_page, R.string.item_perception_title, R.string.item_perception_description))
            items.add(FragmentPageItem(R.id.uas_page, R.string.item_uas_title, R.string.item_uas_description))
            items.add(FragmentPageItem(R.id.lte_page, R.string.item_lte_title, R.string.item_lte_description))
            items.add(FragmentPageItem(R.id.fly_safe_page, R.string.item_fly_safe_title, R.string.item_fly_safe_description))
            items.add(FragmentPageItem(R.id.security_code_page, R.string.item_security_code_title, R.string.item_security_code_description))
            items.add(FragmentPageItem(R.id.mop_center_page, R.string.item_mop_title, R.string.item_mop_description))
            items.add(FragmentPageItem(R.id.look_at_page, R.string.item_look_at_title, R.string.item_look_at_description))
            items.add(FragmentPageItem(R.id.intelligent_flight_page, R.string.item_intelligent_flight_title, R.string.item_intelligent_flight__description))
        }
    }
}