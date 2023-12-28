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
public class FlycMenuFragment extends MenuFragment {
    @Override
    protected String getPreferencesTitle() {
        return StringUtils.getResStr(ContextUtil.getContext(), R.string.uxsdk_setting_menu_title_flyc);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.uxsdk_setting_menu_aircraft_layout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextCell textCell = view.findViewById(R.id.setting_flyc_sensors_state);
        textCell.setOnClickListener(view1 -> {
            SensorsMenuFragment fragment = new SensorsMenuFragment();
            addFragment(getFragmentManager(), fragment, true);
        });

    }
}
