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
import dji.v5.ux.core.widget.battery.BatterySettingWidget;

/**
 * Description :
 *
 * @author: Byte.Cai
 * date : 2022/11/21
 * <p>
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
public class BatteryMenuFragment extends MenuFragment {

    @Override
    protected String getPreferencesTitle() {
        return StringUtils.getResStr(ContextUtil.getContext(), R.string.uxsdk_setting_menu_title_battery);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.uxsdk_fragment_setting_menu_battery_layout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BatterySettingWidget batterySettingWidget = (BatterySettingWidget) view.findViewById(R.id.battery_setting_widget);
        batterySettingWidget.getBatteryGroupInfoWidget().setOnDetailOnClickListener(v -> {
            BatteryInfoDetailFragment fragment = new BatteryInfoDetailFragment();
            addFragment(getFragmentManager(), fragment, true);
        });
    }
}
