package dji.v5.ux.core.widget.setting;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import dji.sdk.keyvalue.utils.ProductUtil;
import dji.sdk.keyvalue.value.product.ProductType;
import dji.sdk.keyvalue.value.remotecontroller.RCMode;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;
import dji.v5.ux.accessory.RTKStartServiceHelper;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.ui.setting.SettingFragmentPagerAdapter;
import dji.v5.ux.core.ui.setting.data.MenuBean;
import dji.v5.ux.core.ui.setting.taplayout.TabViewPager;
import dji.v5.ux.core.ui.setting.taplayout.VerticalTabLayout;
import dji.v5.ux.core.ui.setting.ui.MenuFragment;
import dji.v5.ux.core.ui.setting.ui.MenuFragmentFactory;
import dji.v5.ux.core.ui.setting.ui.SettingMenuFragment;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;


/**
 * Description : 设置页Panel
 *
 * @author: Byte.Cai
 * date : 2022/11/17
 * <p>
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
public class SettingPanelWidget extends ConstraintLayoutWidget<Boolean> {
    private final String tag = LogUtils.getTag(this);

    private ProductType mProductType;
    private ProductType mPrevProductType;
    private boolean mProductConnected;
    private boolean mSearchlightConnected;
    private RCMode mCurrentRcMode = RCMode.NORMAL;


    private FragmentManager fm;
    private TabViewPager viewPager;
    private SettingFragmentPagerAdapter mAdapter;
    private List<MenuBean> menus = new ArrayList<>();
    private List<Fragment> mFragments = new ArrayList<>();
    private VerticalTabLayout mTabLayout;
    private Disposable mRestartDispose;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    private SettingPanelWidgetModel widgetModel;

    public SettingPanelWidget(Context context) {
        this(context, null);
    }

    public SettingPanelWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingPanelWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeView();
        showWidgets();
    }

    public void setWidgetModel(SettingPanelWidgetModel settingPanelWidgetModel) {
        this.widgetModel = settingPanelWidgetModel;
        prepareData();

    }

    protected void initializeView() {
        LayoutInflater.from(getContext()).inflate(R.layout.uxsdk_panel_layout_setting, this, true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        mTabLayout = findViewById(R.id.setting_vertical_tabLayout);
        viewPager = findViewById(R.id.setting_viewpager);
        viewPager.setPagingEnabled(false);

    }


    protected void prepareData() {
        mCompositeDisposable.add(widgetModel.getRcModeProcessor().toFlowable().subscribe(rcMode -> {
            if (rcMode != mCurrentRcMode) {
                mCurrentRcMode = rcMode;
                updateData(rcMode);
            }
        }));


        mCompositeDisposable.add(widgetModel.getProductTypeProcessor().toFlowable().subscribe(productType -> mProductType = productType));

        mCompositeDisposable.add(widgetModel.getFlightControllerConnectProcessor().toFlowable().subscribe(connection -> {
            if (connection != mProductConnected) {
                mProductConnected = connection;
                if (mProductConnected && mPrevProductType != mProductType) {
                    delayRestartPanel();
                }
                mPrevProductType = mProductType;
            }
        }));

        addRTKRtkModuleAvailableListener();
        addPayloadConnectListener();


    }

    private void addRTKRtkModuleAvailableListener() {
        mCompositeDisposable.add(RTKStartServiceHelper.INSTANCE.getRtkModuleAvailable().subscribe(aBoolean -> {
            if (isSupportAdvRtk(aBoolean)) {
                mAdapter.addRTKPanel();
            } else {
                mAdapter.removeRTKPanel();
            }
        }));
    }

    private void addPayloadConnectListener() {
        mCompositeDisposable.add(widgetModel.getPayloadConnectedProcessor().toFlowable().distinctUntilChanged().subscribe(aBoolean -> {
            if (aBoolean) {
                mAdapter.addPayloadPanel();
            } else {
                mAdapter.removePayloadPanel();
            }
        }));

    }

    protected void showWidgets() {
        LogUtils.d(tag, "showWidgets");
        fm = ((FragmentActivity) getContext()).getSupportFragmentManager();
        if (isSlaverRcMode(mCurrentRcMode)) {
            createSlaverFragments();
        } else {
            createMasterFragments();
        }
        mAdapter = new SettingFragmentPagerAdapter(fm, menus, mFragments);
        viewPager.setAdapter(mAdapter);
        //        mAdapter.notifyDataSetChanged();
        mTabLayout.setupWithViewPager(viewPager);


        //设置展示的默认页面，B控则展示遥控页面
        if (isSlaverRcMode(mCurrentRcMode)) {
            setCurrentItem(MenuFragmentFactory.FRAGMENT_TAG_RC);
        } else {
            //A控展示用户上一次选中的页面
            if (!TextUtils.isEmpty(getCurrentItemFlag())) {
                setCurrentItem(getCurrentItemFlag());
            } else {
                //否则默认展示飞控设置页
                setCurrentItem(MenuFragmentFactory.FRAGMENT_TAG_AIRCRAFT);
            }
        }
        mAdapter.notifyDataSetChanged();

    }


    @Override
    protected void onDetachedFromWindow() {
        destroy();
        super.onDetachedFromWindow();
    }

    private void destroy() {
        LogUtils.e(tag, "destroy mFragments.clear()");
        mFragments.clear();
        if (mCompositeDisposable != null && !mCompositeDisposable.isDisposed()) {
            mCompositeDisposable.dispose();
            mCompositeDisposable = null;
        }
        if (mRestartDispose != null && !mRestartDispose.isDisposed()) {
            mRestartDispose.dispose();
        }
        if (viewPager != null) {
            viewPager.clearOnPageChangeListeners();
        }
        if (mTabLayout != null) {
            mTabLayout.setupWithViewPager(null);
            mTabLayout = null;
        }
        if (mAdapter != null) {
            mAdapter.destroy();
            mAdapter = null;
        }
        fm = null;
        mAdapter = null;


    }


    private void setMasterFragmentData() {
        LogUtils.d(tag, "setMasterFragmentData");
        createMasterFragments();
        //        mAdapter.notifyDataSetChanged();
        if (!TextUtils.isEmpty(getCurrentItemFlag())) {
            setCurrentItem(getCurrentItemFlag());
        } else {
            setCurrentItem(MenuFragmentFactory.FRAGMENT_TAG_AIRCRAFT);
        }
        mAdapter.notifyDataSetChanged();
    }

    private void createMasterFragments() {
        menus.clear();
        mFragments.clear();

        menus.add(new MenuBean(R.drawable.uxsdk_ic_setting_drone_active, R.drawable.uxsdk_ic_setting_drone));
        mFragments.add(SettingMenuFragment.newInstance(MenuFragmentFactory.FRAGMENT_TAG_AIRCRAFT));

        menus.add(new MenuBean(R.drawable.uxsdk_ic_setting_obstacl_avoidance_active, R.drawable.uxsdk_ic_setting_obstacl_avoidance));
        mFragments.add(SettingMenuFragment.newInstance(MenuFragmentFactory.FRAGMENT_TAG_PERCEPTION));

        menus.add(new MenuBean(R.drawable.uxsdk_ic_setting_rc_active, R.drawable.uxsdk_ic_setting_rc));

        menus.add(new MenuBean(R.drawable.uxsdk_ic_setting_hd_active, R.drawable.uxsdk_ic_setting_hd));

        menus.add(new MenuBean(R.drawable.uxsdk_ic_setting_plane_electricity_active, R.drawable.uxsdk_ic_setting_plane_electricity));

        menus.add(new MenuBean(R.drawable.uxsdk_ic_setting_camera_active, R.drawable.uxsdk_ic_setting_camera));

        mFragments.add(SettingMenuFragment.newInstance(MenuFragmentFactory.FRAGMENT_TAG_RC));

        mFragments.add(SettingMenuFragment.newInstance(MenuFragmentFactory.FRAGMENT_TAG_HD));

        mFragments.add(SettingMenuFragment.newInstance(MenuFragmentFactory.FRAGMENT_TAG_BATTERY));

        mFragments.add(SettingMenuFragment.newInstance(MenuFragmentFactory.FRAGMENT_TAG_GIMBAL));

        if (mSearchlightConnected) {
            menus.add(new MenuBean(R.drawable.uxsdk_ic_setting_psdk_active, R.drawable.uxsdk_ic_setting_psdk));
            mFragments.add(SettingMenuFragment.newInstance(MenuFragmentFactory.FRAGMENT_TAG_SEARCHLIGHT_ACCESSORY));
        }
        if (isSupportAdvRtk(false)) {
            menus.add(new MenuBean(R.drawable.uxsdk_ic_setting_rtk_active, R.drawable.uxsdk_ic_setting_rtk));
            mFragments.add(SettingMenuFragment.newInstance(MenuFragmentFactory.FRAGMENT_TAG_RTK));
        }

        menus.add(new MenuBean(R.drawable.uxsdk_ic_setting_more_active, R.drawable.uxsdk_ic_setting_more));
        mFragments.add(SettingMenuFragment.newInstance(MenuFragmentFactory.FRAGMENT_TAG_COMMON));
    }

    private void setSlaverFragmentData() {
        createSlaverFragments();
        //        mAdapter.notifyDataSetChanged();
        setCurrentItem(MenuFragmentFactory.FRAGMENT_TAG_RC);
        mAdapter.notifyDataSetChanged();
    }

    private void createSlaverFragments() {
        menus.clear();
        menus.add(new MenuBean(R.drawable.uxsdk_ic_setting_rc_active, R.drawable.uxsdk_ic_setting_rc));
        menus.add(new MenuBean(R.drawable.uxsdk_ic_setting_hd_active, R.drawable.uxsdk_ic_setting_hd));
        menus.add(new MenuBean(R.drawable.uxsdk_ic_setting_plane_electricity_active, R.drawable.uxsdk_ic_setting_plane_electricity));
        menus.add(new MenuBean(R.drawable.uxsdk_ic_setting_camera_active, R.drawable.uxsdk_ic_setting_camera));
        menus.add(new MenuBean(R.drawable.uxsdk_ic_setting_more_active, R.drawable.uxsdk_ic_setting_more));

        mFragments.clear();
        mFragments.add(SettingMenuFragment.newInstance(MenuFragmentFactory.FRAGMENT_TAG_RC));
        mFragments.add(SettingMenuFragment.newInstance(MenuFragmentFactory.FRAGMENT_TAG_HD));
        mFragments.add(SettingMenuFragment.newInstance(MenuFragmentFactory.FRAGMENT_TAG_BATTERY));
        mFragments.add(SettingMenuFragment.newInstance(MenuFragmentFactory.FRAGMENT_TAG_GIMBAL));
        mFragments.add(SettingMenuFragment.newInstance(MenuFragmentFactory.FRAGMENT_TAG_COMMON));
    }

    public void setCurrentItem(String flag) {
        if (mAdapter == null) {
            LogUtils.d(tag, "SettingPanel is not ready!");
            return;
        }
        int result = mAdapter.getSelectIndex(flag);
        if (result >= 0) {
            viewPager.setCurrentItem(result, true);
        }
    }

    public String getCurrentItemFlag() {
        int result = viewPager.getCurrentItem();
        return mAdapter.getSelectFlag(result);
    }


    private void delayRestartPanel() {
        if (mRestartDispose != null && !mRestartDispose.isDisposed()) {
            mRestartDispose.dispose();
        }
        mRestartDispose = Observable.just(true)
                .delay(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> {
                    menus.clear();
                    mFragments.clear();
                    mAdapter.notifyDataSetChanged();
                    if (isSlaverRcMode(mCurrentRcMode)) {
                        setSlaverFragmentData();
                    } else {
                        setMasterFragmentData();
                    }
                });
    }

    private void updateData(RCMode rcMode) {
        LogUtils.d(tag, "rcMode=" + rcMode);
        if (isSlaverRcMode(rcMode)) {
            setSlaverFragmentData();
        } else {
            setMasterFragmentData();
        }
    }

    private boolean isSlaverRcMode(RCMode mode) {

        if (mode != null) {
            return mode == RCMode.SLAVE || mode == RCMode.SLAVE_SUB;
        }
        return false;
    }


    //辅控不支持RTK
    private boolean isSupportAdvRtk(Boolean isRTKModuleAvailable) {
        return !isSlaverRcMode(mCurrentRcMode) && isRTKModuleAvailable || ProductUtil.isM300Product() || ProductUtil.isM30Product()||ProductUtil.isM3EProduct();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
    }


    /**
     * 返回事件，询问fragment是否要处理返回事件
     */
    public boolean onBackPressed() {
        boolean handled = false;
        if (mFragments != null) {
            for (Fragment fragment : mFragments) {
                if (fragment instanceof MenuFragment) {
                    handled = ((MenuFragment) fragment).onBackPressed();
                    if (handled) {
                        break;
                    }
                }
            }
        }

        return handled;
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        //do nothing
    }

    @Override
    protected void reactToModelChanges() {
        //do nothing

    }

    @Nullable
    @Override
    public String getIdealDimensionRatioString() {
        return null;
    }
}
