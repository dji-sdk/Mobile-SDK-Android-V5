package dji.v5.ux.core.ui.setting.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import dji.v5.utils.common.AndUtil;
import dji.v5.ux.R;
import dji.v5.ux.core.base.CompassStatusWidget;
import dji.v5.ux.core.base.ImuStatusWidget;
import dji.v5.ux.core.base.TabGroupWidget;
import dji.v5.ux.core.ui.setting.ui.MenuFragment;


/**
 * <p>传感器状态页面</p>
 * <p>
 * <li>IMU状态</li>
 * <li>Compass状态</li>
 *
 */

public class SensorsMenuFragment extends MenuFragment {

    private static final String CURRENT_INDEX = "current_sensor_index";
    private static final int INDEX_IMU = 0;

    TabGroupWidget mSettingMenuSensorsTab;
    ImuStatusWidget mSettingMenuSensorsImu;
    CompassStatusWidget mSettingMenuSensorsCompass;

    @Override
    protected int getLayoutId() {
        return R.layout.uxsdk_setting_menu_sensors_layout;
    }

    @Override
    protected String getPreferencesTitle() {
        return AndUtil.getResString(R.string.uxsdk_setting_menu_title_sensors_state);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mSettingMenuSensorsTab = mFragmentRoot.findViewById(R.id.setting_menu_sensors_tab);
        mSettingMenuSensorsImu = mFragmentRoot.findViewById(R.id.setting_menu_sensors_imu);
        mSettingMenuSensorsCompass = mFragmentRoot.findViewById(R.id.setting_menu_sensors_compass);
        if (savedInstanceState != null) {
            mSettingMenuSensorsTab.setCheckedIndex(savedInstanceState.getInt(CURRENT_INDEX, 0));
        }
        initView();

        mSettingMenuSensorsTab.setOnTabChangeListener((oldIndex, newIndex) -> {
            if (newIndex == 0) {
                mSettingMenuSensorsImu.setVisibility(View.VISIBLE);
                mSettingMenuSensorsCompass.setVisibility(View.GONE);
            } else {
                mSettingMenuSensorsImu.setVisibility(View.GONE);
                mSettingMenuSensorsCompass.setVisibility(View.VISIBLE);
            }
        });

        return mFragmentRoot;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mSettingMenuSensorsTab != null) {
            outState.putInt(CURRENT_INDEX, mSettingMenuSensorsTab.getCheckedIndex());
        }
        super.onSaveInstanceState(outState);
    }



    private void initView() {
        final boolean visible = (View.VISIBLE == mSettingMenuSensorsTab.getVisibility());
        mSettingMenuSensorsTab.setVisibility(View.VISIBLE);
        if (!visible) {
            mSettingMenuSensorsImu.setVisibility(View.VISIBLE);
            mSettingMenuSensorsTab.setCheckedIndex(INDEX_IMU);
        }
    }
}
