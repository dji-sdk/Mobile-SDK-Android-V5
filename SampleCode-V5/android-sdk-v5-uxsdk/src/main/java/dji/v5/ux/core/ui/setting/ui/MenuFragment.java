package dji.v5.ux.core.ui.setting.ui;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import dji.v5.ux.R;
import dji.v5.ux.core.ui.setting.dialog.CommonLoadingDialog;


/**
 * Description : 设置界面所有Fragment的基类Fragment，封装了一些基础能力
 *
 * @author: Byte.Cai
 * date : 2022/11/18
 * <p>
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
public abstract class MenuFragment extends Fragment {

    private Loader initLoader;
    protected View mFragmentRoot;
    protected CommonLoadingDialog mLoadingDialog;


    protected abstract String getPreferencesTitle();



    //region fragment operation
    protected static void addFragment(FragmentManager fm, MenuFragment menuFragment) {
        addFragment(fm, menuFragment, false);
    }

    protected static void addFragment(FragmentManager fm, MenuFragment menuFragment, boolean isAnimated) {
        if (fm == null || menuFragment == null) {
            return;
        }
        FragmentTransaction transaction = fm.beginTransaction();
        if (isAnimated) {
            transaction.setCustomAnimations(R.anim.uxsdk_push_right_in, R.anim.uxsdk_fade_out, R.anim.uxsdk_fade_in, R.anim.uxsdk_push_right_out);
        }
        transaction.replace(R.id.fragment_content, menuFragment);
        transaction.addToBackStack(menuFragment.getPreferencesTitle());
        transaction.commitAllowingStateLoss();
    }

    //添加fragment
    protected void addChildFragment(int containerViewId, Fragment fragment) {
        if (null != fragment) {
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.replace(containerViewId, fragment, fragment.getClass().getSimpleName());
            transaction.addToBackStack(fragment.getClass().getSimpleName());
            transaction.commitAllowingStateLoss();
        }
    }


    protected static void clearBackStack(FragmentManager fragmentManager) {
        if (fragmentManager.getBackStackEntryCount() > 0) {
            FragmentManager.BackStackEntry entry = fragmentManager.getBackStackEntryAt(0);
            fragmentManager.popBackStack(entry.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    protected static void replaceFragment(FragmentManager fm, int containerViewId, Fragment fragment) {
        if (fm == null || fragment == null) {
            return;
        }
        FragmentTransaction transaction = fm.beginTransaction();
        // TODO Animation
        transaction.replace(containerViewId, fragment);
        transaction.commitAllowingStateLoss();
    }
    //endregion


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mFragmentRoot = inflater.inflate(getLayoutId(), container, false);
        initLoader = Loader.createLoader();
        initLoader.setListener(new Loader.LoaderListener() {
            @Override
            public void onCreateView() {
                onPrepareView();
            }

            @Override
            public void onCreateData() {
                onPrepareDataInBackground();
            }

            @Override
            public void onRefreshView() {
                onRefreshDataOnView();
            }
        }).start();
        return mFragmentRoot;
    }

    //region childFragment use
    /**
     * UI 控件初始化
     */
    @UiThread
    protected void onPrepareView() {

    }

    /**
     * 在工作线程中准备初始化页面时需要的数据，不包含UI控件
     */
    @WorkerThread
    protected void onPrepareDataInBackground() {

    }

    /**
     * UI 与 数据准备好之后，这里刷新一下页面
     */
    @UiThread
    protected void onRefreshDataOnView() {

    }

    /**
     * 子类实现具体布局
     */
    protected abstract int getLayoutId();
    //endregion





    //region dialog
    /**
     * 进度对话框是否正在显示
     * @return true 正在显示
     */
    public boolean isShowLoadingDialog() {
        return mLoadingDialog != null && mLoadingDialog.isShowing();
    }

    protected void showLoadingDialog() {
        if (mLoadingDialog == null) {
            mLoadingDialog = new CommonLoadingDialog(getActivity());
            mLoadingDialog.setCancelable(true);
            mLoadingDialog.setCanceledOnTouchOutside(true);
        }
        if (!mLoadingDialog.isShowing()) {
            mLoadingDialog.show();
        }
    }

    protected void hideLoadingDialog() {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
    }
    //endregion



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (initLoader != null) {
            initLoader.cancel();
            initLoader = null;
        }
        mFragmentRoot = null;
    }

    public boolean onBackPressed() {
        return false;
    }
}
