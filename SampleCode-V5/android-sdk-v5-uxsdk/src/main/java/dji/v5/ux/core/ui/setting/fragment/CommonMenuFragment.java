package dji.v5.ux.core.ui.setting.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.v5.utils.common.ContextUtil;
import dji.v5.utils.common.StringUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.base.TextCell;
import dji.v5.ux.core.ui.setting.ui.MenuFragment;

/**
 * Description :
 *
 * @author: Byte.Cai
 * date : 2022/11/21
 * <p>
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
public class CommonMenuFragment extends MenuFragment {
    @Override
    protected String getPreferencesTitle() {
        return StringUtils.getResStr(ContextUtil.getContext(), R.string.uxsdk_setting_menu_title_common);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.uxsdk_setting_menu_common_layout;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.setting_common_about).setOnClickListener(view1 -> {
            CommonAboutFragment fragment = new CommonAboutFragment();
            addFragment(getFragmentManager(), fragment, true);
        });


        view.findViewById(R.id.setting_device_name).setOnClickListener(view12 -> {
            DeviceNameFragment fragment = new DeviceNameFragment();
            addFragment(getFragmentManager(), fragment, true);
        });
        view.findViewById(R.id.setting_led).setOnClickListener(view13 -> {
            LedMenuFragment fragment = new LedMenuFragment();
            addFragment(getFragmentManager(), fragment, true);
        });


    }
}
