package dji.v5.ux.core.ui.setting.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;



/**
 * A simple {@link Fragment} subclass.
 */
public abstract class BaseFragment extends Fragment {

    protected final String TAG = getClass().getSimpleName();

    protected View mFragmentRoot;

    protected abstract int getLayoutId();

    protected BaseFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mFragmentRoot = inflater.inflate(getLayoutId(), container, false);
        return mFragmentRoot;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mFragmentRoot = null;
    }

    //添加fragment
    protected void addChildFragment(int containerViewId, BaseFragment fragment) {
        if (null != fragment) {
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.replace(containerViewId, fragment, fragment.getClass().getSimpleName());
            transaction.addToBackStack(fragment.getClass().getSimpleName());
            transaction.commitAllowingStateLoss();
        }
    }

    //移除fragment
    protected void removeChildFragment() {
        if (getChildFragmentManager().getBackStackEntryCount() > 1) {
            getChildFragmentManager().popBackStack();
        }
    }

    protected static void addFragment(FragmentManager fm, int containerViewId, BaseFragment fragment) {
        if (fm == null || fragment == null) {
            return;
        }
        FragmentTransaction transaction = fm.beginTransaction();
        // TODO Animation
        transaction.replace(containerViewId, fragment);
        transaction.addToBackStack(fragment.getClass().getSimpleName());
        transaction.commitAllowingStateLoss();
    }

    protected static void replaceFragment(FragmentManager fm, int containerViewId, BaseFragment fragment) {
        if (fm == null || fragment == null) {
            return;
        }
        FragmentTransaction transaction = fm.beginTransaction();
        // TODO Animation
        transaction.replace(containerViewId, fragment);
        transaction.commitAllowingStateLoss();
    }

    protected static void clearBackStack(FragmentManager fragmentManager) {
        if (fragmentManager.getBackStackEntryCount() > 0) {
            FragmentManager.BackStackEntry entry = fragmentManager.getBackStackEntryAt(0);
            fragmentManager.popBackStack(entry.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }
}
