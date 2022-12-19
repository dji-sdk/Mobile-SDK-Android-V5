/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.v5.ux.sample.showcase.defaultlayout;

import android.os.Bundle;
import android.view.View;
import android.view.ViewStub;

import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.common.video.channel.VideoChannelState;
import dji.v5.common.video.channel.VideoChannelType;
import dji.v5.common.video.interfaces.IVideoChannel;
import dji.v5.common.video.interfaces.VideoChannelStateChangeListener;
import dji.v5.common.video.stream.PhysicalDevicePosition;
import dji.v5.common.video.stream.StreamSource;
import dji.v5.manager.datacenter.MediaDataCenter;
import dji.v5.utils.common.JsonUtil;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;
import dji.v5.ux.cameracore.widget.autoexposurelock.AutoExposureLockWidget;
import dji.v5.ux.cameracore.widget.cameracontrols.CameraControlsWidget;
import dji.v5.ux.cameracore.widget.cameracontrols.exposuresettings.ExposureSettingsPanel;
import dji.v5.ux.cameracore.widget.cameracontrols.lenscontrol.LensControlWidget;
import dji.v5.ux.cameracore.widget.focusexposureswitch.FocusExposureSwitchWidget;
import dji.v5.ux.cameracore.widget.focusmode.FocusModeWidget;
import dji.v5.ux.cameracore.widget.fpvinteraction.FPVInteractionWidget;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.ux.core.extension.ViewExtensions;
import dji.v5.ux.core.panel.systemstatus.SystemStatusListPanelWidget;
import dji.v5.ux.core.panel.topbar.TopBarPanelWidget;
import dji.v5.ux.core.widget.setting.SettingPanelWidget;
import dji.v5.ux.core.util.CameraUtil;
import dji.v5.ux.core.util.CommonUtils;
import dji.v5.ux.core.util.DataProcessor;
import dji.v5.ux.core.widget.fpv.FPVWidget;
import dji.v5.ux.core.widget.hsi.HorizontalSituationIndicatorWidget;
import dji.v5.ux.core.widget.hsi.PrimaryFlightDisplayWidget;
import dji.v5.ux.core.widget.setting.SettingWidget;
import dji.v5.ux.core.widget.simulator.SimulatorIndicatorWidget;
import dji.v5.ux.core.widget.systemstatus.SystemStatusWidget;
import dji.v5.ux.map.MapWidget;
import dji.v5.ux.mapkit.core.maps.DJIUiSettings;
import dji.v5.ux.training.simulatorcontrol.SimulatorControlWidget;
import dji.v5.ux.visualcamera.CameraNDVIPanelWidget;
import dji.v5.ux.visualcamera.CameraVisiblePanelWidget;
import dji.v5.ux.visualcamera.zoom.FocalZoomWidget;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * Displays a sample layout of widgets similar to that of the various DJI apps.
 */
public class DefaultLayoutActivity extends AppCompatActivity {

    //region Fields
    private final String TAG = LogUtils.getTag(this);

    protected FPVWidget primaryFpvWidget;
    protected FPVInteractionWidget fpvInteractionWidget;
    protected FPVWidget secondaryFPVWidget;
    protected SystemStatusListPanelWidget systemStatusListPanelWidget;
    protected SimulatorControlWidget simulatorControlWidget;
    protected LensControlWidget lensControlWidget;
    protected AutoExposureLockWidget autoExposureLockWidget;
    protected FocusModeWidget focusModeWidget;
    protected FocusExposureSwitchWidget focusExposureSwitchWidget;
    protected CameraControlsWidget cameraControlsWidget;
    protected HorizontalSituationIndicatorWidget horizontalSituationIndicatorWidget;
    protected ExposureSettingsPanel exposureSettingsPanel;
    protected PrimaryFlightDisplayWidget pfvFlightDisplayWidget;
    protected CameraNDVIPanelWidget ndviCameraPanel;
    protected CameraVisiblePanelWidget visualCameraPanel;
    protected FocalZoomWidget focalZoomWidget;
    protected SettingWidget settingWidget;
    protected MapWidget mapWidget;
    private SettingPanelWidget mSettingPanelWidget;
    private DrawerLayout mDrawerLayout;


    private CompositeDisposable compositeDisposable;
    private final DataProcessor<CameraSource> cameraSourceProcessor = DataProcessor.create(new CameraSource(PhysicalDevicePosition.UNKNOWN,
            CameraLensType.UNKNOWN));
    private VideoChannelStateChangeListener primaryChannelStateListener = null;
    private VideoChannelStateChangeListener secondaryChannelStateListener = null;
    //endregion

    //region Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uxsdk_activity_default_layout);

        // Setup top bar state callbacks
        TopBarPanelWidget topBarPanel = findViewById(R.id.panel_top_bar);
        SystemStatusWidget systemStatusWidget = topBarPanel.getSystemStatusWidget();
        if (systemStatusWidget != null) {
            systemStatusWidget.setStateChangeCallback(findViewById(R.id.widget_panel_system_status_list));
        }

        SimulatorIndicatorWidget simulatorIndicatorWidget = topBarPanel.getSimulatorIndicatorWidget();
        if (simulatorIndicatorWidget != null) {
            simulatorIndicatorWidget.setStateChangeCallback(findViewById(R.id.widget_simulator_control));
        }
        mDrawerLayout = findViewById(R.id.root_view);
        settingWidget = topBarPanel.getSettingWidget();
        primaryFpvWidget = findViewById(R.id.widget_primary_fpv);
        fpvInteractionWidget = findViewById(R.id.widget_fpv_interaction);
        secondaryFPVWidget = findViewById(R.id.widget_secondary_fpv);
        systemStatusListPanelWidget = findViewById(R.id.widget_panel_system_status_list);
        simulatorControlWidget = findViewById(R.id.widget_simulator_control);
        lensControlWidget = findViewById(R.id.widget_lens_control);
        ndviCameraPanel = findViewById(R.id.panel_ndvi_camera);
        visualCameraPanel = findViewById(R.id.panel_visual_camera);
        autoExposureLockWidget = findViewById(R.id.widget_auto_exposure_lock);
        focusModeWidget = findViewById(R.id.widget_focus_mode);
        focusExposureSwitchWidget = findViewById(R.id.widget_focus_exposure_switch);
        exposureSettingsPanel = findViewById(R.id.panel_camera_controls_exposure_settings);
        pfvFlightDisplayWidget = findViewById(R.id.widget_fpv_flight_display_widget);
        focalZoomWidget = findViewById(R.id.widget_focal_zoom);
        cameraControlsWidget = findViewById(R.id.widget_camera_controls);
        horizontalSituationIndicatorWidget = findViewById(R.id.widget_horizontal_situation_indicator);

        mapWidget = findViewById(R.id.widget_map);
        cameraControlsWidget.getExposureSettingsIndicatorWidget().setStateChangeResourceId(R.id.panel_camera_controls_exposure_settings);

        initClickListener();
        MediaDataCenter.getInstance().getVideoStreamManager().addStreamSourcesListener(sources -> runOnUiThread(() -> updateFPVWidgetSource(sources)));
        primaryFpvWidget.setOnFPVStreamSourceListener((devicePosition, lensType) -> {
            LogUtils.i(TAG, devicePosition, lensType);
            cameraSourceProcessor.onNext(new CameraSource(devicePosition, lensType));
        });
        //小surfaceView放置在顶部，避免被大的遮挡
        secondaryFPVWidget.setSurfaceViewZOrderOnTop(true);
        secondaryFPVWidget.setSurfaceViewZOrderMediaOverlay(true);

        mapWidget.initAMap(map -> {
            // map.setOnMapClickListener(latLng -> onViewClick(mapWidget));
            DJIUiSettings uiSetting = map.getUiSettings();
            if (uiSetting != null) {
                uiSetting.setZoomControlsEnabled(false);//hide zoom widget
            }
        });
        mapWidget.onCreate(savedInstanceState);
    }

    private void initClickListener() {
        secondaryFPVWidget.setOnClickListener(v -> swapVideoSource());
        initChannelStateListener();

        if (settingWidget != null) {
            settingWidget.setOnClickListener(v -> toggleRightDrawer());
        }
    }

    private void toggleRightDrawer() {
        if (mSettingPanelWidget == null) {
            ViewStub stub = findViewById(R.id.manual_right_nav_setting_stub);
            if (stub != null) {
                mSettingPanelWidget = (SettingPanelWidget) stub.inflate();
            } else {
                mSettingPanelWidget = findViewById(R.id.manual_right_nav_setting);
            }
        }
        mDrawerLayout.openDrawer(GravityCompat.END);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapWidget.onDestroy();
        MediaDataCenter.getInstance().getVideoStreamManager().clearAllStreamSourcesListeners();
        removeChannelStateListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapWidget.onResume();
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(systemStatusListPanelWidget.closeButtonPressed()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pressed -> {
                    if (pressed) {
                        ViewExtensions.hide(systemStatusListPanelWidget);
                    }
                }));
        compositeDisposable.add(simulatorControlWidget.getUIStateUpdates()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(simulatorControlWidgetState -> {
                    if (simulatorControlWidgetState instanceof SimulatorControlWidget.UIState.VisibilityUpdated) {
                        if (((SimulatorControlWidget.UIState.VisibilityUpdated) simulatorControlWidgetState).isVisible()) {
                            hideOtherPanels(simulatorControlWidget);
                        }
                    }
                }));
        compositeDisposable.add(cameraSourceProcessor.toFlowable()
                .observeOn(SchedulerProvider.computation())
                .throttleLast(500, TimeUnit.MILLISECONDS)
                .subscribe(result -> runOnUiThread(() -> onCameraSourceUpdated(result.devicePosition, result.lensType)))
        );
    }

    @Override
    protected void onPause() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
            compositeDisposable = null;
        }
        mapWidget.onPause();
        super.onPause();
    }
    //endregion

    private void hideOtherPanels(@Nullable View widget) {
        View[] panels = {
                simulatorControlWidget
        };

        for (View panel : panels) {
            if (widget != panel) {
                panel.setVisibility(View.GONE);
            }
        }
    }

    private void updateFPVWidgetSource(List<StreamSource> streamSources) {
        LogUtils.i(TAG, JsonUtil.toJson(streamSources));
        if (streamSources == null) {
            return;
        }

        //没有数据
        if (streamSources.isEmpty()) {
            secondaryFPVWidget.setVisibility(View.GONE);
            return;
        }

        //仅一路数据
        if (streamSources.size() == 1) {
            //这里仅仅做Widget的显示与否，source和channel的获取放到widget中
            secondaryFPVWidget.setVisibility(View.GONE);
            return;
        }
        secondaryFPVWidget.setVisibility(View.VISIBLE);
    }

    private void initChannelStateListener() {
        IVideoChannel primaryChannel =
                MediaDataCenter.getInstance().getVideoStreamManager().getAvailableVideoChannel(VideoChannelType.PRIMARY_STREAM_CHANNEL);
        IVideoChannel secondaryChannel =
                MediaDataCenter.getInstance().getVideoStreamManager().getAvailableVideoChannel(VideoChannelType.SECONDARY_STREAM_CHANNEL);
        if (primaryChannel != null) {
            primaryChannelStateListener = (from, to) -> {
                StreamSource primaryStreamSource = primaryChannel.getStreamSource();
                if (VideoChannelState.ON == to && primaryStreamSource != null) {
                    runOnUiThread(() -> primaryFpvWidget.updateVideoSource(primaryStreamSource, VideoChannelType.PRIMARY_STREAM_CHANNEL));
                }
            };
            primaryChannel.addVideoChannelStateChangeListener(primaryChannelStateListener);
        }
        if (secondaryChannel != null) {
            secondaryChannelStateListener = (from, to) -> {
                StreamSource secondaryStreamSource = secondaryChannel.getStreamSource();
                if (VideoChannelState.ON == to && secondaryStreamSource != null) {
                    runOnUiThread(() -> secondaryFPVWidget.updateVideoSource(secondaryStreamSource, VideoChannelType.SECONDARY_STREAM_CHANNEL));
                }
            };
            secondaryChannel.addVideoChannelStateChangeListener(secondaryChannelStateListener);
        }
    }

    private void removeChannelStateListener() {
        IVideoChannel primaryChannel =
                MediaDataCenter.getInstance().getVideoStreamManager().getAvailableVideoChannel(VideoChannelType.PRIMARY_STREAM_CHANNEL);
        IVideoChannel secondaryChannel =
                MediaDataCenter.getInstance().getVideoStreamManager().getAvailableVideoChannel(VideoChannelType.SECONDARY_STREAM_CHANNEL);
        if (primaryChannel != null) {
            primaryChannel.removeVideoChannelStateChangeListener(primaryChannelStateListener);
        }
        if (secondaryChannel != null) {
            secondaryChannel.removeVideoChannelStateChangeListener(secondaryChannelStateListener);
        }
    }

    private void onCameraSourceUpdated(PhysicalDevicePosition devicePosition, CameraLensType lensType) {
        LogUtils.i(TAG, "onCameraSourceUpdated", devicePosition, lensType);
        ComponentIndexType cameraIndex = CameraUtil.getCameraIndex(devicePosition);
        fpvInteractionWidget.updateCameraSource(cameraIndex, lensType);
        fpvInteractionWidget.updateGimbalIndex(CommonUtils.getGimbalIndex(devicePosition));
        lensControlWidget.updateCameraSource(cameraIndex, lensType);
        ndviCameraPanel.updateCameraSource(cameraIndex, lensType);
        visualCameraPanel.updateCameraSource(cameraIndex, lensType);
        autoExposureLockWidget.updateCameraSource(cameraIndex, lensType);
        focusModeWidget.updateCameraSource(cameraIndex, lensType);
        focusExposureSwitchWidget.updateCameraSource(cameraIndex, lensType);
        cameraControlsWidget.updateCameraSource(cameraIndex, lensType);
        exposureSettingsPanel.updateCameraSource(cameraIndex, lensType);
        focalZoomWidget.updateCameraSource(cameraIndex, lensType);
        horizontalSituationIndicatorWidget.updateCameraSource(cameraIndex, lensType);
        updateViewVisibility(devicePosition, lensType);
        updateInteractionEnabled();
    }

    private void updateViewVisibility(PhysicalDevicePosition devicePosition, CameraLensType lensType) {
        //只在fpv下显示
        pfvFlightDisplayWidget.setVisibility(devicePosition == PhysicalDevicePosition.NOSE ? View.VISIBLE : View.INVISIBLE);

        //fpv下不显示
        lensControlWidget.setVisibility(devicePosition == PhysicalDevicePosition.NOSE ? View.INVISIBLE : View.VISIBLE);
        ndviCameraPanel.setVisibility(devicePosition == PhysicalDevicePosition.NOSE ? View.INVISIBLE : View.VISIBLE);
        visualCameraPanel.setVisibility(devicePosition == PhysicalDevicePosition.NOSE ? View.INVISIBLE : View.VISIBLE);
        autoExposureLockWidget.setVisibility(devicePosition == PhysicalDevicePosition.NOSE ? View.INVISIBLE : View.VISIBLE);
        focusModeWidget.setVisibility(devicePosition == PhysicalDevicePosition.NOSE ? View.INVISIBLE : View.VISIBLE);
        focusExposureSwitchWidget.setVisibility(devicePosition == PhysicalDevicePosition.NOSE ? View.INVISIBLE : View.VISIBLE);
        cameraControlsWidget.setVisibility(devicePosition == PhysicalDevicePosition.NOSE ? View.INVISIBLE : View.VISIBLE);
        focalZoomWidget.setVisibility(devicePosition == PhysicalDevicePosition.NOSE ? View.INVISIBLE : View.VISIBLE);
        horizontalSituationIndicatorWidget.setSimpleModeEnable(devicePosition != PhysicalDevicePosition.NOSE);

        //有其他的显示逻辑，这里确保fpv下不显示
        if (devicePosition == PhysicalDevicePosition.NOSE) {
            exposureSettingsPanel.setVisibility(View.GONE);
        }

        //只在部分len下显示
        ndviCameraPanel.setVisibility(CameraUtil.isSupportForNDVI(lensType) ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Swap the video sources of the FPV and secondary FPV widgets.
     */
    private void swapVideoSource() {
        VideoChannelType primaryVideoChannel = primaryFpvWidget.getVideoChannelType();
        StreamSource primaryStreamSource = primaryFpvWidget.getStreamSource();
        VideoChannelType secondaryVideoChannel = secondaryFPVWidget.getVideoChannelType();
        StreamSource secondaryStreamSource = secondaryFPVWidget.getStreamSource();
        //两个source都存在的情况下才进行切换
        if (secondaryStreamSource != null && primaryStreamSource != null) {
            primaryFpvWidget.updateVideoSource(secondaryStreamSource, secondaryVideoChannel);
            secondaryFPVWidget.updateVideoSource(primaryStreamSource, primaryVideoChannel);
        }
    }

    private void updateInteractionEnabled() {
        StreamSource newPrimaryStreamSource = primaryFpvWidget.getStreamSource();
        fpvInteractionWidget.setInteractionEnabled(false);
        if (newPrimaryStreamSource != null) {
            fpvInteractionWidget.setInteractionEnabled(newPrimaryStreamSource.getPhysicalDevicePosition() != PhysicalDevicePosition.NOSE);
        }
    }

    private static class CameraSource {
        PhysicalDevicePosition devicePosition;
        CameraLensType lensType;

        public CameraSource(PhysicalDevicePosition devicePosition, CameraLensType lensType) {
            this.devicePosition = devicePosition;
            this.lensType = lensType;
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }
}
