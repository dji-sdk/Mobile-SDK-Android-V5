package dji.v5.ux.core.ui.setting.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;


import dji.v5.ux.R;


/*
 * Copyright (c) 2014, DJI All Rights Reserved.
 */

/**
 * <p>Created by luca on 2017/1/15.</p>
 */

public abstract class MenuFragment extends AppFragment {

    private Loader initLoader;
    protected abstract String getPreferencesTitle();

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
        // TODO
        // transaction.setCustomAnimations(R.animator.fragment_slide_right_enter, R.animator.fragment_slide_right_exit);
        transaction.replace(R.id.fragment_content, menuFragment);
        transaction.addToBackStack(menuFragment.getPreferencesTitle());
        transaction.commitAllowingStateLoss();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
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
        return view;
    }
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

    protected static void popBackFragmentStack(FragmentManager fm) {
        if (fm.getBackStackEntryCount() > 1) {
            fm.popBackStackImmediate();
        }
    }

//    protected void handleCheckChanged(final String tag, final boolean checked) {
//        Single.create(new SingleOnSubscribe<Boolean>() {
//            @Override
//            public void subscribe(SingleEmitter<Boolean> e) throws Exception {
//                PreferencesUtil.putBoolean(tag, checked);
//                new SimpleEvent<Boolean>(tag, checked).send();
//            }
//        }).subscribeOn(Schedulers.io()).subscribe();
//    }
//
//    protected void handleItemSelected(final String tag, final int position) {
//        Single.create(new SingleOnSubscribe<Boolean>() {
//            @Override
//            public void subscribe(SingleEmitter<Boolean> e) throws Exception {
//                PreferencesUtil.putInt(tag, position);
//                new SimpleEvent<Integer>(tag, position).send();
//            }
//        }).subscribeOn(Schedulers.io()).subscribe();
//    }
//
//    protected void handleValueChanged(final String tag, final int value) {
//        Single.create(new SingleOnSubscribe<Boolean>() {
//            @Override
//            public void subscribe(SingleEmitter<Boolean> e) throws Exception {
//                PreferencesUtil.putInt(tag, value);
//                new SimpleEvent<Integer>(tag, value).send();
//            }
//        }).subscribeOn(Schedulers.io()).subscribe();
//    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (initLoader != null) {
            initLoader.cancel();
            initLoader = null;
        }
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }
}
