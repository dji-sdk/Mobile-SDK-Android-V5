package dji.v5.ux.core.ui.setting;


import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.List;

import dji.v5.utils.common.AndUtil;
import dji.v5.utils.common.ContextUtil;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.ui.setting.data.MenuBean;
import dji.v5.ux.core.ui.setting.taplayout.QTabView;
import dji.v5.ux.core.ui.setting.taplayout.TabAdapter;
import dji.v5.ux.core.ui.setting.ui.MenuFragmentFactory;
import dji.v5.ux.core.ui.setting.ui.SettingMenuFragment;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * Description :
 *
 * @author: Byte.Cai
 * date : 2022/11/21
 * <p>
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
public class SettingFragmentPagerAdapter extends FragmentPagerAdapter implements TabAdapter {

    private List<MenuBean> mAdapterMenus;
    private List<Fragment> mAdapterFragments;
    private FragmentManager mCurrentFragmentManager;

    public SettingFragmentPagerAdapter(FragmentManager fm, List<MenuBean> menus, List<Fragment> mFragments) {
        super(fm);
        this.mAdapterMenus = menus;
        this.mAdapterFragments = mFragments;
        this.mCurrentFragmentManager = fm;
    }

    @Override
    public Fragment getItem(int position) {
        return mAdapterFragments.get(position);
    }

    public int getSelectIndex(String str) {
        if (mAdapterFragments != null && mAdapterFragments.size() > 0 && !TextUtils.isEmpty(str)) {
            for (int i = 0; i < mAdapterFragments.size(); i++) {
                if (mAdapterFragments.get(i) instanceof SettingMenuFragment) {
                    SettingMenuFragment fragment = (SettingMenuFragment) mAdapterFragments.get(i);
                    if (str.equals(fragment.getFragmentFlag())) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    public String getSelectFlag(int index) {
        if (mAdapterFragments != null && index >= 0 && index < mAdapterFragments.size()) {
            SettingMenuFragment fragment = (SettingMenuFragment) mAdapterFragments.get(index);
            if (fragment != null) {
                return fragment.getFragmentFlag();
            }
        }
        return "";
    }

    public void removePayloadPanel() {
        removeMenuPanel(MenuFragmentFactory.FRAGMENT_TAG_PAYLOAD, R.drawable.uxsdk_ic_setting_psdk);
    }

    public void addPayloadPanel() {
        addMenuPanel(MenuFragmentFactory.FRAGMENT_TAG_GIMBAL,
                MenuFragmentFactory.FRAGMENT_TAG_PAYLOAD,
                R.drawable.uxsdk_ic_setting_psdk_active,
                R.drawable.uxsdk_ic_setting_psdk);
    }

    public void addSearchlightPayloadPanel() {
        addMenuPanel(MenuFragmentFactory.FRAGMENT_TAG_GIMBAL,
                MenuFragmentFactory.FRAGMENT_TAG_SEARCHLIGHT_ACCESSORY,
                R.drawable.uxsdk_ic_setting_psdk_active,
                R.drawable.uxsdk_ic_setting_psdk);
    }

    public void removeSearchlightPayloadPanel() {
        removeMenuPanel(MenuFragmentFactory.FRAGMENT_TAG_SEARCHLIGHT_ACCESSORY, R.drawable.uxsdk_ic_setting_psdk);
    }

    public void removeRTKPanel() {
        removeMenuPanel(MenuFragmentFactory.FRAGMENT_TAG_RTK, R.drawable.uxsdk_ic_setting_rtk);
    }

    public void addRTKPanel() {
        addMenuPanel(MenuFragmentFactory.FRAGMENT_TAG_GIMBAL, MenuFragmentFactory.FRAGMENT_TAG_RTK, R.drawable.uxsdk_ic_setting_rtk_active,
                R.drawable.uxsdk_ic_setting_rtk);
    }

    public void addMenuPanel(String prePage, String page, int selectedIcon, int normalIcon) {

        int payloadIndex = getSelectIndex(page);
        if (payloadIndex >= 0) {
            return;
        }
        int gimbalIndex = getSelectIndex(prePage);
        if (gimbalIndex >= 0) {
            payloadIndex = gimbalIndex + 1;
            mAdapterMenus.add(payloadIndex, new MenuBean(selectedIcon, normalIcon));
            SettingMenuFragment fragment = SettingMenuFragment.newInstance(page);
            mAdapterFragments.add(payloadIndex, fragment);
            notifyDataSetChanged();
        }
    }

    public void removeMenuPanel(String page, int normalIcon) {
        int payloadIndex = getSelectIndex(page);
        if (payloadIndex >= 0) {
            MenuBean menu = mAdapterMenus.get(payloadIndex);
            if (menu.getNormalIcon() == normalIcon) {
                mAdapterMenus.remove(payloadIndex);
                 mAdapterFragments.remove(payloadIndex);
                notifyDataSetChanged();
            }
        }
    }

    @Override
    public int getItemPosition(Object object) {
        // When call notifyDataSetChanged(), the view pager will remove all views and reload them all. As so the reload effect is obtained.
        return POSITION_NONE;
    }

    @Override
    public int getCount() {
        return mAdapterFragments.size();
    }

    @Override
    public int getBadge(int position) {
        return 0;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        String name = ((SettingMenuFragment) getItem(position)).getFragmentFlag();
        Fragment fragment = mCurrentFragmentManager.findFragmentByTag(name);
        if (fragment != null) {
            mCurrentFragmentManager.beginTransaction().attach(fragment).commitNowAllowingStateLoss();
        } else {
            fragment = getItem(position);
            mCurrentFragmentManager.beginTransaction()
                    .add(container.getId(), fragment, name)
                    .commitNowAllowingStateLoss();
        }
        mCurrentFragmentManager.executePendingTransactions();
        return fragment;
    }

    @Override
    public QTabView.TabIcon getIcon(int position) {
        // 修复 position 和 mAdapterMenus 可能不一致的稳定性问题
        MenuBean menu = mAdapterMenus.get(position >= mAdapterMenus.size() ? mAdapterMenus.size() - 1 : position);
        return new QTabView.TabIcon.Builder().setIcon(menu.getSelectIcon(), menu.getNormalIcon())
                .setIconGravity(Gravity.CENTER)
                .setIconSize(AndUtil.dip2px(ContextUtil.getContext(), 50),
                        AndUtil.dip2px(ContextUtil.getContext(), 50))
                .setBackground(R.drawable.uxsdk_selector_blue_oval_mask)
                .build();
    }

    @Override
    public QTabView.TabTitle getTitle(int position) {
        return null;
    }

    @Override
    public int getBackground(int position) {
        return R.color.uxsdk_fpv_popover_content_background_color;
    }

    public void destroy() {
        mCurrentFragmentManager = null;
        if (mAdapterFragments != null) {
            mAdapterFragments.clear();
        }
        if (mAdapterMenus != null) {
            mAdapterMenus.clear();
        }
    }
}
