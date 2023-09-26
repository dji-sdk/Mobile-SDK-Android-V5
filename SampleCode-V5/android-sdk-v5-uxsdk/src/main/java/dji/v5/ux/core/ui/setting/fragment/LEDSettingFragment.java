package dji.v5.ux.core.ui.setting.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import dji.v5.utils.common.AndUtil;
import dji.v5.ux.R;
import dji.v5.ux.core.ui.setting.ui.MenuFragment;


public class LEDSettingFragment extends MenuFragment {

    @Override
    protected int getLayoutId() {
        return R.layout.uxsdk_setting_menu_common_devicename;
    }

    @Override
    protected String getPreferencesTitle() {
        return AndUtil.getResString(R.string.uxsdk_setting_menu_title_device_name);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return mFragmentRoot;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

}
