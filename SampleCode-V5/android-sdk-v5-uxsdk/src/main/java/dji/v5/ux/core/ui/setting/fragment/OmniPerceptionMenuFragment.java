package dji.v5.ux.core.ui.setting.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.utils.ProductUtil;
import dji.v5.manager.KeyManager;
import dji.v5.utils.common.ContextUtil;
import dji.v5.utils.common.StringUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.ui.setting.ui.MenuFragment;
import dji.v5.ux.obstacle.PrecisionLandingWidget;
import dji.v5.ux.obstacle.VisionPositionWidget;

/**
 * Description :
 *
 * @author: Byte.Cai
 * date : 2022/11/21
 * <p>
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
public class OmniPerceptionMenuFragment extends MenuFragment {
    private PrecisionLandingWidget precisionLandingWidget;
    private boolean isDownwardVisionSystemOpen;

    @Override
    protected String getPreferencesTitle() {
        return StringUtils.getResStr(ContextUtil.getContext(), R.string.uxsdk_setting_menu_title_perception);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.uxsdk_fragemnt_setting_menu_omni_perception_layout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        VisionPositionWidget visionPositionWidget = (VisionPositionWidget) view.findViewById(R.id.omni_vision_position_widget);
        precisionLandingWidget = (PrecisionLandingWidget) view.findViewById(R.id.omni_vision_precision_landing_widget);
        visionPositionWidget.setSwitchStateListener(check -> {
            isDownwardVisionSystemOpen = check;
            updatePrecisionWidgetVisible();
        });

        KeyManager.getInstance().listen(KeyTools.createKey(FlightControllerKey.KeyConnection), this, (oldValue, newValue) -> updatePrecisionWidgetVisible());


    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        KeyManager.getInstance().cancelListen(this);
    }

    private void updatePrecisionWidgetVisible() {
        if (precisionLandingWidget == null) {
            return;
        }
        if (isDownwardVisionSystemOpen && !ProductUtil.isM3EProduct() && !ProductUtil.isM4EProduct() && !ProductUtil.isM4DProduct()) {
            precisionLandingWidget.setVisibility(View.VISIBLE);
        } else {
            precisionLandingWidget.setVisibility(View.GONE);
        }
    }
}
