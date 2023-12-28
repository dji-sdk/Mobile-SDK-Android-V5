package dji.v5.ux.core.ui.setting.fragment;

import dji.v5.utils.common.ContextUtil;
import dji.v5.utils.common.StringUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.ui.setting.ui.MenuFragment;

/**
 * Description :
 *
 * @author: Byte.Cai
 * date : 2023/9/20
 * <p>
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
public class RcCalibrationFragment extends MenuFragment {
    @Override
    protected int getLayoutId() {
        return R.layout.uxsdk_fragment_rc_calibration;
    }

    @Override
    protected String getPreferencesTitle() {
        return StringUtils.getResStr(ContextUtil.getContext(), R.string.uxsdk_setting_ui_rc_calibration);

    }
}
