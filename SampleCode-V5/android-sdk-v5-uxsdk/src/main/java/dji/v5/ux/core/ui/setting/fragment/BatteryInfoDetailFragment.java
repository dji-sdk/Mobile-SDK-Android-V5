package dji.v5.ux.core.ui.setting.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.v5.utils.common.ContextUtil;
import dji.v5.utils.common.StringUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.ui.setting.ui.MenuFragment;
import dji.v5.ux.core.widget.battery.BatteryGroupInfoWidget;
import dji.v5.ux.core.widget.battery.BatteryGroupWidget;

/**
 * Description :
 *
 * @author: Byte.Cai
 * date : 2023/9/21
 * <p>
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
public class BatteryInfoDetailFragment extends MenuFragment {
    @Override
    protected int getLayoutId() {
        return R.layout.uxsdk_fragment_battery_info_detail;
    }

    @Override
    protected String getPreferencesTitle() {
        return StringUtils.getResStr(ContextUtil.getContext(), R.string.uxsdk_setting_menu_title_battery);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BatteryGroupWidget batteryGroupWidget = (BatteryGroupWidget)view.findViewById(R.id.setting_menu_battery);
        batteryGroupWidget.setEnableBatteryCells(true);
        batteryGroupWidget.setEnableSerialNumber(true);

    }
}
