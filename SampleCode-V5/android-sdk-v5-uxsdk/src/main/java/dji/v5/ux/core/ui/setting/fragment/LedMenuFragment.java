package dji.v5.ux.core.ui.setting.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import dji.sdk.keyvalue.key.FlightAssistantKey;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.ProductKey;
import dji.sdk.keyvalue.value.flightassistant.AuxiliaryLightMode;
import dji.sdk.keyvalue.value.flightcontroller.LEDsSettings;
import dji.sdk.keyvalue.value.product.ProductType;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.utils.common.LogUtils;
import dji.v5.utils.common.StringUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.base.SwitcherCell;
import dji.v5.ux.core.base.TabSelectCell;
import dji.v5.ux.core.ui.setting.ui.MenuFragment;
import dji.v5.ux.core.util.ViewUtil;



/**
 * Created by Lahm.Long on 2019/12/23
 * desc :
 */
public class LedMenuFragment extends MenuFragment
        implements
        SwitcherCell.OnCheckedChangedListener,
        TabSelectCell.OnTabChangeListener {
//    @Inject
//    LedMenuContract.Presenter mPresenter;

    private  SwitcherCell mLEDBeaconCell;//夜航灯
    private SwitcherCell mLEDArmCell;//机臂灯
    private SwitcherCell mLEDStatusCell;//状态灯
    private SwitcherCell m430LEDsHideModeCell;//隐藏模式
    private LinearLayout m430LedSettingLayout;
    private SwitcherCell m430BeaconLedCell;//430夜航灯开关

    private TabSelectCell m430TopAuxiliaryTabSelectCell;//430上补光灯
    private TabSelectCell m430BottomAuxiliaryTabSelectCell;//430下补光灯

    private void initView(View view) {
        mLEDBeaconCell = view.findViewById(R.id.setting_menu_common_led_beacon);
        mLEDArmCell = view.findViewById(R.id.setting_menu_common_led_arm);
        mLEDStatusCell = view.findViewById(R.id.setting_menu_common_led_status);
        m430LEDsHideModeCell = view.findViewById(R.id.setting_menu_common_leds_hide_mode);
        m430LedSettingLayout = view.findViewById(R.id.setting_menu_common_led);
        m430BeaconLedCell = view.findViewById(R.id.setting_menu_common_beacon_led);
        m430TopAuxiliaryTabSelectCell =view.findViewById(R.id.setting_menu_common_top_auxiliary_light);
        m430BottomAuxiliaryTabSelectCell = view.findViewById(R.id.setting_menu_common_bottom_auxiliary_light);
    }

    @Override
    protected String getPreferencesTitle() {
        return StringUtils.getResStr(R.string.uxsdk_setting_menu_title_led);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.uxsdk_setting_menu_led_layout;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
//        DaggerLedMenuComponent.builder()
//                .appComponent(((AppActivity) getActivity()).getAppComponent())
//                .ledMenuModule(new LedMenuModule(this))
//                .build()
//                .inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return mFragmentRoot;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        init430LEDsHideModeView();
        initKey();
    }

    private void initKey() {
        KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyHasInternalBeaconLeds), new CommonCallbacks.CompletionCallbackWithParam<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                if (aBoolean) {
                    updateViewsByState();
                }
            }
            @Override
            public void onFailure(@NonNull IDJIError error) {
                //add log
            }
        });

        KeyManager.getInstance().listen(KeyTools.createKey(FlightAssistantKey.KeyBottomAuxiliaryLightMode), this, (oldValue, mode) -> {
            if (mode != null) {
                setCellTab(m430BottomAuxiliaryTabSelectCell, mode.value());
            }
        });


        KeyManager.getInstance().getValue(KeyTools.createKey(ProductKey.KeyProductType), new CommonCallbacks.CompletionCallbackWithParam<ProductType>() {
            @Override
            public void onSuccess(ProductType productType) {

                if (productType == ProductType.M30_SERIES || productType == ProductType.DJI_MAVIC_3_ENTERPRISE_SERIES) {
                    m430TopAuxiliaryTabSelectCell.setVisibility(View.GONE);
                    m430BeaconLedCell.setVisibility(View.VISIBLE);
                }else{
                    updateTopAuxiliaryTabSelectCell();

                }
            }
            @Override
            public void onFailure(@NonNull IDJIError error) {
                //add log
            }
        });


    }

    private void updateTopAuxiliaryTabSelectCell() {
        KeyManager.getInstance().listen(KeyTools.createKey(FlightAssistantKey.KeyTopAuxiliaryLightMode), this, (oldValue, mode) -> {
            if(mode != null) {
                setCellTab(m430TopAuxiliaryTabSelectCell, mode.value());
            }
        });
    }

    private void updateViewsByState() {
        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyLEDsSettings), this, (oldValue, leDsSettings) -> {
            if (leDsSettings != null) {
                setCellCheck(m430BeaconLedCell, leDsSettings.getNavigationLEDsOn());
                setCellCheck(mLEDStatusCell, leDsSettings.getStatusIndicatorLEDsOn());
                setCellCheck(mLEDArmCell, leDsSettings.getFrontLEDsOn());
            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        KeyManager.getInstance().cancelListen(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    //可设置隐藏模式及夜航灯，上下补光灯
    private void init430LEDsHideModeView() {
        mLEDBeaconCell.setVisibility(View.GONE);
        mLEDArmCell.setVisibility(View.GONE);
        mLEDStatusCell.setVisibility(View.GONE);
        m430LEDsHideModeCell.setVisibility(View.GONE);
        m430LEDsHideModeCell.setOnCheckedChangedListener(this);
        m430BeaconLedCell.setOnCheckedChangedListener(this);
        m430TopAuxiliaryTabSelectCell.setOnTabChangeListener(this);
        m430BottomAuxiliaryTabSelectCell.setOnTabChangeListener(this);
//        mCompositeDisposable.add(LEDDiscreetModeLiveData.getInstance().observable(getLifecycle()).subscribe(isHideMode -> {
//            if (m430LEDsHideModeCell != null && m430LEDsHideModeCell.isChecked() != isHideMode) {
//                setCellCheck(m430LEDsHideModeCell, isHideMode);
//            }
//
//            m430LedSettingLayout.setVisibility(isHideMode ? View.GONE : View.VISIBLE);
//        }));
        m430LedSettingLayout.setVisibility( View.VISIBLE);
    }

    @Override
    public void onCheckedChanged(SwitcherCell cell, boolean isChecked) {
        int id = cell.getId();//夜航灯
        if (id == R.id.setting_menu_common_leds_hide_mode) {//隐藏模式按钮
            if (isChecked) {
                openLEDsHide(true);
            } else {
                openLEDsHide(false);
            }
        } else if (id == R.id.setting_menu_common_beacon_led || id == R.id.setting_menu_common_led_beacon) {//非430夜航灯

            LEDsSettings leDsSettings = createLedsBuilder();
            leDsSettings.setNavigationLEDsOn(isChecked);
            KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyLEDsSettings), leDsSettings, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    //add tips
                }
                @Override
                public void onFailure(@NonNull IDJIError error) {
                    //add log
                }
            });
        } else if (id == R.id.setting_menu_common_led_arm) {//机臂灯

            LEDsSettings leDsSettings = createLedsBuilder();
            leDsSettings.setFrontLEDsOn(isChecked);
            leDsSettings.setRearLEDsOn(isChecked);
            leDsSettings.setStatusIndicatorLEDsOn(isChecked);
            KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyLEDsSettings), leDsSettings, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    if (!isChecked) {
                        ViewUtil.showToast(getContext(), R.string.uxsdk_app_operator_fail, Toast.LENGTH_SHORT);
                    }
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    //add log
                }
            });
        } else if (id == R.id.setting_menu_common_led_status) {//状态灯
            LEDsSettings leDsSettings = createLedsBuilder();
            leDsSettings.setStatusIndicatorLEDsOn(isChecked);
            KeyManager.getInstance().setValue(KeyTools.createKey(FlightControllerKey.KeyLEDsSettings), leDsSettings, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                    if (!isChecked) {
                        ViewUtil.showToast(getContext(), R.string.uxsdk_app_operator_fail, Toast.LENGTH_SHORT);
                    }
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    //add log
                }
            });
        }
    }

    private LEDsSettings createLedsBuilder() {
        //先获取现阶段的值
        LEDsSettings defaultSettings = new LEDsSettings();
        LEDsSettings leDsSettings = KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyLEDsSettings) ,defaultSettings);
        return new LEDsSettings(leDsSettings.getFrontLEDsOn(),leDsSettings.getStatusIndicatorLEDsOn(),leDsSettings.getRearLEDsOn(),leDsSettings.getNavigationLEDsOn());

    }

    private void openLEDsHide(boolean isHide) {
        LogUtils.i(TAG , "leds hide mode " + isHide);
//        mCompositeDisposable.add(LEDDiscreetModeLiveData.getInstance().discreetMode(isHide)
//                .subscribe(aBoolean -> {
//                    if (!aBoolean) {
//                        ViewUtil.showToast(getContext()  ,(R.string.uxsdk_app_operator_fail,Toast.LENGTH_SHORT);
//                    } else {
//                        m430LedSettingLayout.setVisibility(isHide ? View.GONE : View.VISIBLE);
//                        m430BeaconLedCell.setChecked(false);
//                    }
//                }));

    }


    @Override
    public void onTabChanged(TabSelectCell cell, int oldIndex, int newIndex) {
        if (oldIndex == newIndex) {
            return;
        }
        int id = cell.getId();
        if (id == R.id.setting_menu_common_top_auxiliary_light) {

            KeyManager.getInstance().setValue(KeyTools.createKey(FlightAssistantKey.KeyTopAuxiliaryLightMode), AuxiliaryLightMode.find(newIndex), new CommonCallbacks.CompletionCallback() {
                @Override
                public void onSuccess() {
                  //do nothing
                }

                @Override
                public void onFailure(@NonNull IDJIError error) {
                    ViewUtil.showToast(getContext(), R.string.uxsdk_app_operator_fail, Toast.LENGTH_SHORT);
                }
            });

        } else if (id == R.id.setting_menu_common_bottom_auxiliary_light) {
           AuxiliaryLightMode mode = AuxiliaryLightMode.find(newIndex);
            if (mode == AuxiliaryLightMode.OFF) {
                ViewUtil.showToast(getContext(),R.string.uxsdk_setting_menu_close_bottom_aux_tips , Toast.LENGTH_LONG);
            }
                KeyManager.getInstance().setValue(KeyTools.createKey(FlightAssistantKey.KeyBottomAuxiliaryLightMode), AuxiliaryLightMode.find(newIndex), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        //do nothing
                    }
                    @Override
                    public void onFailure(@NonNull IDJIError error) {
                        ViewUtil.showToast(getContext(), R.string.uxsdk_app_operator_fail, Toast.LENGTH_SHORT);
                    }
                });

        }
    }

    private void setCellCheck(SwitcherCell cell, boolean isCheck) {
        if (null != cell) {
            cell.setOnCheckedChangedListener(null);
            cell.setChecked(isCheck);
            cell.setOnCheckedChangedListener(this);
        }
    }

    private void setCellTab(TabSelectCell cell, int tab) {
        if (null != cell) {
            cell.setOnTabChangeListener(null);
            cell.setCurrentTab(tab);
            cell.setOnTabChangeListener(this);
        }
    }
}