package dji.v5.ux.flight.flightparam;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.Arrays;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.RemoteControllerKey;
import dji.sdk.keyvalue.value.flightcontroller.ControlChannelMapping;
import dji.sdk.keyvalue.value.remotecontroller.RemoteControllerType;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.utils.common.AndUtil;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.base.SwitcherCell;
import dji.v5.ux.core.base.TabSelectCell;
import dji.v5.ux.core.util.ViewUtil;

/*
 * Copyright (c) 2017, DJI All Rights Reserved.
 */

/**
 * <p>Created by luca on 2017/6/13.</p>
 */

public class FpaView extends LinearLayout implements SwitcherCell.OnCheckedChangedListener, TabSelectCell.OnTabChangeListener {
    private static final String  TAG = "FpaView";
    private TextView mDescView;
    private SwitcherCell mSwitcherCell;
    private TabSelectCell mFlycModeTabCell;
    private View mDividerTv;

    private int[] modeDrawable = new int[]{R.drawable.uxsdk_setting_ui_flyc_smart_controller_rc_mode_aps,R.drawable.uxsdk_setting_ui_flyc_smart_controller_rc_mode};

    public FpaView(Context context) {
        this(context, null);
    }

    public FpaView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FpaView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.uxsdk_setting_menu_flyc_mode, this, true);
        setOrientation(VERTICAL);
        modeDrawable[0] = getRes(R.drawable.uxsdk_setting_ui_flyc_m3e_rc_mode_ans, R.drawable.uxsdk_setting_ui_flyc_c_plus_rc_mode_aps, R.drawable.uxsdk_setting_ui_flyc_smart_controller_rc_mode_aps);
        modeDrawable[1] = getRes(R.drawable.uxsdk_setting_ui_flyc_m3e_rc_mode_tns, R.drawable.uxsdk_setting_ui_flyc_rc_plus_rc_mode, R.drawable.uxsdk_setting_ui_flyc_smart_controller_rc_mode);

        mDescView = findViewById(R.id.setting_ui_flyc_mode);
        mSwitcherCell = findViewById(R.id.setting_ui_flyc_mode_switch);
        mFlycModeTabCell = findViewById(R.id.setting_ui_flyc_mode_tab);
        mDividerTv = findViewById(R.id.setting_ui_flyc_mode_divider);
    }

//    public void setPresenter(FlycMenuContract.Presenter presenter) {
//
//    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mFlycModeTabCell.addTabs(0, Arrays.asList(getResources().getStringArray(getRes(R.array.uxsdk_flight_mode_rc510, R.array.uxsdk_flight_mode_smart_controller, R.array.uxsdk_flight_mode))));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyMultipleFlightModeEnabled), this, (oldValue, open) -> {
            LogUtils.d( TAG, "MultipleFlightModeEnabled = " + open);
            mSwitcherCell.setOnCheckedChangedListener(null);
            mSwitcherCell.setChecked(Boolean.TRUE.equals(open));
            mSwitcherCell.setOnCheckedChangedListener(FpaView.this);
            updateView(Boolean.TRUE.equals(open));
        });

    }

    @Override
    protected void onDetachedFromWindow() {
        KeyManager.getInstance().cancelListen(this);
        super.onDetachedFromWindow();
    }

    @Override
    public void onCheckedChanged(SwitcherCell cell, final boolean isChecked) {

        KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyMultipleFlightModeEnabled), isChecked, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                //关闭允许切换飞行模式开关时，将飞行档位切换至三角架模式 T/P/S
                if (!isChecked && mFlycModeTabCell.getCurrentTab() == 1) {
                    setTripodFlycMode();
                }
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                mSwitcherCell.setOnCheckedChangedListener(null);
                mSwitcherCell.setChecked(!isChecked);
                mSwitcherCell.setOnCheckedChangedListener(FpaView.this);
            }
        });

    }

    @Override
    public void onTabChanged(TabSelectCell cell, int oldIndex, int newIndex) {
        if(oldIndex == newIndex){
            return;
        }
        if (newIndex == 1) {
            //姿态模式 A/P/S
            showAttiFlycModeDialog(oldIndex);
        } else {
            //切换至三角架模式 T/P/S
            setTripodFlycMode();
        }
    }

    /**
     * 切换至 T/P/S 飞行档位
     */
    private void setTripodFlycMode() {

        KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyCustomFunctionMode), ControlChannelMapping.TRIPOD, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                updateFlycModeDescView(0);
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                setTabCellCurrentTab(1);
                ViewUtil.showToast(getContext() ,AndUtil.getResString(R.string.uxsdk_setting_menu_setting_fail));
            }
        });

    }

    /**
     * 根据不同机型显示不同布局与文案
     *
     * @param open 切换飞行模式开关，false则不显示具体内容，true则显示具体详情
     */
    private void updateView(boolean open) {
        //UI控件显示与隐藏
        setVisibility(View.VISIBLE);
        int visible = open ? VISIBLE : GONE;
        mSwitcherCell.setBottomDividerEnable(!open);
        mDescView.setVisibility(visible);
        mDividerTv.setVisibility(visible);
        mFlycModeTabCell.setVisibility(GONE);
        updateFlycModeValue();
    }

    /**
     * 通过飞行模式配置表获取当前飞行模式
     */
    private void updateFlycModeValue() {

        KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyCustomFunctionMode), new CommonCallbacks.CompletionCallbackWithParam<ControlChannelMapping>() {
            @Override
            public void onSuccess(ControlChannelMapping config) {
                updateFlycModeView(config);
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                //add log
            }
        });

    }

    private void updateFlycModeView(ControlChannelMapping channelMapping) {
        //从飞机获取的飞行模式配置表，刷新页面内容
        int currentTab = channelMapping == ControlChannelMapping.ATTITUDE_NORMAL ? 1 : 0;
        setTabCellCurrentTab(currentTab);
        mFlycModeTabCell.setVisibility(mSwitcherCell.isChecked() ? VISIBLE : GONE);
        //根据T/P/S或A/P/S显示不同的描述文案
        updateFlycModeDescView(currentTab);
    }

    /**
     * 切换为A/P/S时弹出dialog提示用户同意免责协议方可切换
     *
     * @param oldIndex
     */
    private void showAttiFlycModeDialog(int oldIndex) {
        LogUtils.d(TAG , "curIndex " + oldIndex);
        KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyCustomFunctionMode), ControlChannelMapping.ATTITUDE_NORMAL, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                ViewUtil.showToast(getContext() , R.string.uxsdk_setting_menu_desc_omni_perception_downwards , Toast.LENGTH_SHORT);
                updateFlycModeDescView(1);
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                setTabCellCurrentTab(0);
                ViewUtil.showToast(getContext() , AndUtil.getResString(R.string.uxsdk_setting_menu_setting_fail));
            }
        });
        //需用户同意方可设置为 A/P/S 模式
//        final PilotDialog attiFlycModeDialog = new PilotDialog.Builder(getContext())
//                .title(AndUtil.getResString(getRes(R.string.setting_menu_flyc_mode_atti_dialog_title_n_mode_rc511, R.string.setting_menu_flyc_mode_atti_dialog_title_n_mode, R.string.setting_menu_flyc_mode_atti_dialog_title)))
//                .content(R.string.setting_menu_flyc_mode_atti_dialog_content)
//                .negativeText(AndUtil.getResString(R.string.app_cancel))
//                .onNegative((dialog, which) -> {
//                    setTabCellCurrentTab(oldIndex);
//                }).positiveText(AndUtil.getResString(R.string.setting_menu_flyc_mode_atti_dialog_confirm))
//                .positiveEnable(false)
//                .onPositive((dialog, which) -> {
//                    //发送切换为 S/P/A 模式命令
//                    mCompositeDisposable.add(Pilot2Repo.FlightController().CustomFunctionMode().setValueAsync(ControlChannelMapping.ATTITUDE_NORMAL).subscribe(resultWrapper -> {
//                        if (resultWrapper.isSucceed()) {
//                            updateFlycModeDescView(1);
//                        } else {
//                            setTabCellCurrentTab(0);
//                            ToastBox.showToast(AndUtil.getResString(R.string.setting_menu_setting_fail));
//                        }
//                    }));
//                }).cancelable(false)
//                .checkBoxPrompt(AndUtil.getResString(R.string.setting_menu_flyc_mode_atti_dialog_notice), false, (dialog, buttonView, isChecked) -> {
//                    //仅同意免责声明后方可切换
//                    dialog.setPositiveEnable(isChecked);
//                })
//                .build();
//        attiFlycModeDialog.show();
    }

    private void setTabCellCurrentTab(int index) {
        mFlycModeTabCell.setOnTabChangeListener(null);
        mFlycModeTabCell.setCurrentTab(index);
        mFlycModeTabCell.setOnTabChangeListener(this);
    }

    /**
     * 根据A/P/S或T/P/S档位的不同展示不同的文案和图片
     *
     * @param index
     */
    private void updateFlycModeDescView(int index) {
        if (index == 1) {
            mDescView.setCompoundDrawablesWithIntrinsicBounds(modeDrawable[0], 0, 0, 0);
            mDescView.setText(getRes(R.string.uxsdk_setting_menu_flyc_mode_atti_n_mode_rc511, R.string.uxsdk_setting_menu_flyc_mode_atti_n_mode, R.string.uxsdk_setting_menu_flyc_mode_atti));
        } else {
            mDescView.setCompoundDrawablesWithIntrinsicBounds(modeDrawable[1], 0, 0, 0);
            mDescView.setText(getRes(R.string.uxsdk_setting_menu_flyc_mode_n_rc511, R.string.uxsdk_setting_menu_flyc_mode_n, R.string.uxsdk_setting_menu_flyc_mode_m300));
        }
    }

    public static <T> T getRes(@NonNull T rcPlusRes, @NonNull T smartControllerRes) {
        RemoteControllerType curType   = KeyManager.getInstance().getValue(KeyTools.createKey(RemoteControllerKey.KeyRemoteControllerType) , RemoteControllerType.UNKNOWN);
        switch (curType) {
            case DJI_RC_PRO:
            case DJI_RC_PLUS:
                return rcPlusRes;
            case M300_RTK_RC:
            default:
                return smartControllerRes;
        }
    }

    public static <T> T getRes(@NonNull T m3eRes, @NonNull T rcPlusRes, @NonNull T smartControllerRes) {
        RemoteControllerType curType   = KeyManager.getInstance().getValue(KeyTools.createKey(RemoteControllerKey.KeyRemoteControllerType) , RemoteControllerType.UNKNOWN);

        switch (curType) {
            case DJI_RC_PRO:
                return m3eRes;
            case DJI_RC_PLUS:
                return rcPlusRes;
            case M300_RTK_RC:
            default:
                return smartControllerRes;
        }
    }
}