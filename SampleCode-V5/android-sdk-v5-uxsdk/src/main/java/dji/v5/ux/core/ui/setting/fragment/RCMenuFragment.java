package dji.v5.ux.core.ui.setting.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.v5.manager.KeyManager;
import dji.v5.utils.common.ContextUtil;
import dji.v5.utils.common.StringUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.ui.setting.ui.MenuFragment;
import dji.v5.ux.core.util.ViewUtil;

/**
 * Description :
 *
 * @author: Byte.Cai
 * date : 2022/11/21
 * <p>
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
public class RCMenuFragment extends MenuFragment {
    @Override
    protected String getPreferencesTitle() {
        return StringUtils.getResStr(ContextUtil.getContext(), R.string.uxsdk_setting_menu_title_rc);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.uxsdk_fragment_setting_menu_remote_controller_layout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.setting_menu_rc_calibration).setOnClickListener(v -> {
            if (isAircraftConnected()) {
               String content = StringUtils.getResStr(R.string.uxsdk_setting_ui_rc_calibration_tip);
                ViewUtil.showToast(getContext(), content);
            }else {
                RcCalibrationFragment fragment = new RcCalibrationFragment();
                addFragment(getFragmentManager(), fragment, true);
            }
        });
    }

    private boolean isAircraftConnected() {
       return KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyConnection),false);
    }
}
