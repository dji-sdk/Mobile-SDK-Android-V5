package dji.v5.ux.core.ui.setting.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;


import io.reactivex.rxjava3.disposables.CompositeDisposable;

/*
 * Copyright (c) 2014, DJI All Rights Reserved.
 */

/**
 *
 * @author luca
 * @date 2016/12/13
 */

public abstract class AppFragment extends BaseFragment {

    protected CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    protected OnBackPressedHandler mOnBackPressedHandler;
    protected CommonLoadingDialog mLoadingDialog;

    protected Object mBusListener;
    protected boolean isRcConnected = false;
    protected boolean isDeviceConnected = false;

    //region Fragment Lifecycle
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if((getActivity() instanceof OnBackPressedHandler)){
            this.mOnBackPressedHandler = (OnBackPressedHandler) getActivity();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mOnBackPressedHandler != null) {
            //告诉FragmentActivity，当前Fragment在栈顶
            mOnBackPressedHandler.setSelectedFragment(this);
        }
    }

    /**
     * FragmentActivity捕捉到物理返回键点击事件后会首先询问Fragment是否消费该事件
     * @return 如果没有Fragment消息时FragmentActivity自己才会消费该事件
     */
    protected boolean onBackPressed() {
        return false;
    }
    //endregion

    //region Loading Dialog
    /**
     * 进度对话框是否正在显示
     * @return true 正在显示
     */
    public boolean isShowLoadingDialog() {
        return mLoadingDialog != null && mLoadingDialog.isShowing();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        initData();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private void initData() {
//        mBusListener = new Object(){
//            @Subscribe(thread = EventThread.MAIN_THREAD, tags = {@Tag(BusAction.TAG_RESTART_MANUAL_FLIGHT)})
//            @SuppressWarnings("unused")
//            public void onEventRestartFlight(Boolean restart) {
//                if (restart) {
//                    onRestartFlight();
//                }
//            }
//
//            @Subscribe(thread = EventThread.MAIN_THREAD, tags = { @Tag(DroneEvent.TAG)})
//            @SuppressWarnings("unused")
//            public void onEventDeviceDisconnected(DroneEvent event) {
//                if (event.getType() == DroneEvent.COMPONENT_CONNECTED && event.getComponentKey() == DroneEvent.ComponentKey.FlightController) {
//                    if (!event.isConnected()) {
//                        onDeviceDisconnected();
//                    } else {
//                        onDeviceConnected();
//                    }
//                    isDeviceConnected = event.isConnected();
//                }
//
//                if (!isRcConnected && Pilot2Repo.RemoteController().Connection().getValue(false)) {
//                    isRcConnected = true;
//                    onRcConnected();
//                } else if (isRcConnected && !Pilot2Repo.RemoteController().Connection().getValue(false)) {
//                    isRcConnected = false;
//                    onRcDisconnected();
//                }
//            }
//        };
//        RxBus.get().register(mBusListener);
    }

    @Nullable
    protected FragmentActivity getAppActivity() {
        return  getActivity();
    }
    @Override
    public void onDestroyView() {
        //调用clear可以保证disposed未false，使得Fragment恢复时能够再次调用add
        if (mCompositeDisposable != null) {
            mCompositeDisposable.clear();
        }
        super.onDestroyView();
    }

    protected boolean isFinishing() {
        return getActivity() == null || getActivity().isFinishing();
    }

    protected void showLoadingDialog() {
        if (mLoadingDialog == null) {
            mLoadingDialog = new CommonLoadingDialog(getAppActivity());
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

    /**
     * 重启飞控界面
     */
    protected void onRestartFlight() {

    }

    /**
     * 设备断开连接
     */
    protected void onDeviceDisconnected() {

    }

    protected void onDeviceConnected() {

    }

    /**
     * APP与遥控器建立连接
     */
    protected void onRcConnected() {

    }

    /**
     * APP与遥控器断开连接
     */
    protected void onRcDisconnected() {

    }
}
