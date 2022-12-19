package dji.v5.ux.core.ui.setting.ui;


import dji.v5.ux.core.ui.setting.fragment.BatteryMenuFragment;
import dji.v5.ux.core.ui.setting.fragment.CommonMenuFragment;
import dji.v5.ux.core.ui.setting.fragment.FlycMenuFragment;
import dji.v5.ux.core.ui.setting.fragment.GimbalMenuFragment;
import dji.v5.ux.core.ui.setting.fragment.HDMenuFragment;
import dji.v5.ux.core.ui.setting.fragment.OmniPerceptionMenuFragment;
import dji.v5.ux.core.ui.setting.fragment.PayloadFragment;
import dji.v5.ux.core.ui.setting.fragment.RCMenuFragment;
import dji.v5.ux.core.ui.setting.fragment.RtkMenuFragment;

/**
 * Description : 生成不同设置Fragment的工厂
 *
 * @author: Byte.Cai
 * date : 2022/11/17
 * <p>
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
public class MenuFragmentFactory {

    public static final String FRAGMENT_TAG_AIRCRAFT = "FlycMenuFragment";
    public static final String FRAGMENT_TAG_GIMBAL = "GimbalMenuFragment";
    public static final String FRAGMENT_TAG_RC = "RCMenuFragment";
    public static final String FRAGMENT_TAG_PERCEPTION = "PerceptionMenuFragment";
    public static final String FRAGMENT_TAG_HD = "HDMenuFragment";
    public static final String FRAGMENT_TAG_BATTERY = "BatteryMenuFragment";
    public static final String FRAGMENT_TAG_COMMON = "CommonMenuFragment";
    public static final String FRAGMENT_TAG_PAYLOAD = "PayloadFragment";
    public static final String FRAGMENT_TAG_SEARCHLIGHT_ACCESSORY = "SearchlightSettingFragment";
    public static final String FRAGMENT_TAG_RTK = "RtkMenuFragment";

    public static MenuFragment getMenuFragment(String tag) {
        if (FRAGMENT_TAG_AIRCRAFT.equals(tag)) {
            return new FlycMenuFragment();
        } else if (FRAGMENT_TAG_GIMBAL.equals(tag)) {
            return new GimbalMenuFragment();
        } else if (FRAGMENT_TAG_RC.equals(tag)) {
            return new RCMenuFragment();
        } else if (FRAGMENT_TAG_PERCEPTION.equals(tag)) {
            return new OmniPerceptionMenuFragment();
        } else if (FRAGMENT_TAG_HD.equals(tag)) {
            return new HDMenuFragment();
        } else if (FRAGMENT_TAG_BATTERY.equals(tag)) {
            return new BatteryMenuFragment();
        } else if (FRAGMENT_TAG_COMMON.equals(tag)) {
            return new CommonMenuFragment();
        } else if (FRAGMENT_TAG_PAYLOAD.equals(tag)) {
            return new PayloadFragment();
        } else if (FRAGMENT_TAG_RTK.equals(tag)) {
            return new RtkMenuFragment();
        } else {
            throw new IllegalArgumentException(tag + " Not support now.");
        }
    }

    private MenuFragmentFactory() {

    }
}
