package dji.v5.ux.core.ui.setting.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import dji.v5.ux.R;

/**
 * Description : 所有设置界面创建的入口
 *
 * @author: Byte.Cai
 * date : 2022/11/18
 * <p>
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
public class SettingMenuFragment extends Fragment implements FragmentManager.OnBackStackChangedListener {
    public static final String ARG_PARAM = "fragment_tag";
    private static final String NEED_LAZY_INFLATE = "need_lazy_inflate";

    private TextView mTitleView;
    private ImageView mBackBtn;
    private ImageView mProgressBar;

    private String mFragmentTag;
    private String mFragmentFlag;
    private View mFragmentRoot;
    private FragmentManager fragmentManager;
    private Runnable mLazyInflateTask;

    private SettingMenuFragment() {
        // Required empty private constructor
    }

    public static SettingMenuFragment newInstance(String tag) {
        return newInstance(tag, true);
    }

    public static SettingMenuFragment newInstance(String tag, boolean needLazyInitView) {
        SettingMenuFragment fragment = new SettingMenuFragment();
        Bundle args = fragment.getArguments();
        if (args == null) {
            args = new Bundle();
        }
        args.putString(ARG_PARAM, tag);
        args.putBoolean(NEED_LAZY_INFLATE, needLazyInitView);
        fragment.setArguments(args);
        fragment.setFragmentFlag(tag);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentManager = getChildFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);
        mFragmentTag = getArguments().getString(ARG_PARAM, "");

        mLazyInflateTask = this::inflateFunctionFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mFragmentRoot = inflater.inflate(R.layout.uxsdk_setting_menu_fragment_layout, null);
        return mFragmentRoot;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTitleView = view.findViewById(R.id.setting_menu_header_title);
        mBackBtn = view.findViewById(R.id.setting_menu_header_back);
        mProgressBar = view.findViewById(R.id.setting_menu_progress_bar);

        if (!TextUtils.isEmpty(mFragmentTag)) {
            if (getArguments() == null) {
                return;
            }
            mProgressBar.setVisibility(View.VISIBLE);
            boolean needLazyInflate = getArguments().getBoolean(NEED_LAZY_INFLATE, true);
            if (needLazyInflate) {
                mFragmentRoot.post(mLazyInflateTask);
            } else {
                inflateFunctionFragment();
            }
        } else {
            updateTitle();
        }

        mBackBtn.setOnClickListener(v -> {
            if (fragmentManager.getBackStackEntryCount() > 1) {
                MenuFragment menuFragment = getLastMenuFragment();
                if (menuFragment != null && !menuFragment.onBackPressed()) {
                    popBackFragmentStack();
                }
            } else {
                popBackFragmentStack();
            }
        });
    }

    private void inflateFunctionFragment() {
        if (TextUtils.isEmpty(mFragmentTag) || mProgressBar == null) {
            return;
        }
        MenuFragment menuFragment = MenuFragmentFactory.getMenuFragment(mFragmentTag);
        MenuFragment.addFragment(fragmentManager, menuFragment);
        mProgressBar.setVisibility(View.GONE);
        mFragmentTag = null;
    }

    private void popBackFragmentStack() {
        if (fragmentManager.getBackStackEntryCount() > 1) {
            fragmentManager.popBackStackImmediate();
        }
    }

    @Override
    public void onBackStackChanged() {
        updateTitle();
    }

    private void updateTitle() {
        int entryCount = fragmentManager.getBackStackEntryCount();

        if (mBackBtn != null) {
            mBackBtn.setVisibility(entryCount > 1 ? View.VISIBLE : View.INVISIBLE);
        }
        if (entryCount > 0) {
            FragmentManager.BackStackEntry entry = fragmentManager.getBackStackEntryAt(entryCount - 1);
            if (mTitleView != null) {
                mTitleView.setText(entry.getName());
            }
        }
    }


    private MenuFragment getLastMenuFragment() {
        if (fragmentManager.getFragments().size() > 0) {
            Fragment fragment = fragmentManager.getFragments().get(fragmentManager.getFragments().size() - 1);
            if (fragment instanceof MenuFragment) {
                return (MenuFragment) fragment;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }


    @Override
    public void onDestroyView() {
        mProgressBar.setVisibility(View.GONE);
        mProgressBar = null;
        mBackBtn = null;
        mTitleView = null;
        if (mLazyInflateTask != null && mFragmentRoot != null) {
            mFragmentRoot.removeCallbacks(mLazyInflateTask);
        }
        mFragmentRoot = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        mLazyInflateTask = null;

        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public String getFragmentFlag() {
        return mFragmentFlag;
    }

    public void setFragmentFlag(String mFragmentFlag) {
        this.mFragmentFlag = mFragmentFlag;
    }
}
