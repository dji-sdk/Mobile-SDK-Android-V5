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
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.common.video.channel.VideoChannelType;
import dji.v5.common.video.stream.PhysicalDevicePosition;
import dji.v5.common.video.stream.StreamSource;
import dji.v5.manager.datacenter.MediaDataCenter;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;
import dji.v5.ux.cameracore.widget.autoexposurelock.AutoExposureLockWidget;
import dji.v5.ux.cameracore.widget.cameracontrols.CameraControlsWidget;
import dji.v5.ux.cameracore.widget.cameracontrols.exposuresettings.ExposureSettingsPanel;
import dji.v5.ux.cameracore.widget.cameracontrols.lenscontrol.LensControlWidget;
import dji.v5.ux.cameracore.widget.focusexposureswitch.FocusExposureSwitchWidget;
import dji.v5.ux.cameracore.widget.focusmode.FocusModeWidget;
import dji.v5.ux.cameracore.widget.fpvinteraction.FPVInteractionWidget;
import dji.v5.ux.core.extension.ViewExtensions;
import dji.v5.ux.core.panel.systemstatus.SystemStatusListPanelWidget;
import dji.v5.ux.core.panel.topbar.TopBarPanelWidget;
import dji.v5.ux.core.util.CameraUtil;
import dji.v5.ux.core.util.CommonUtils;
import dji.v5.ux.core.util.DataProcessor;
import dji.v5.ux.core.widget.fpv.FPVWidget;
import dji.v5.ux.core.widget.hsi.PrimaryFlightDisplayWidget;
import dji.v5.ux.core.widget.simulator.SimulatorIndicatorWidget;
import dji.v5.ux.core.widget.systemstatus.SystemStatusWidget;
import dji.v5.ux.training.simulatorcontrol.SimulatorControlWidget;
import dji.v5.ux.visualcamera.aperture.CameraConfigApertureWidget;
import dji.v5.ux.visualcamera.ev.CameraConfigEVWidget;
import dji.v5.ux.visualcamera.iso.CameraConfigISOAndEIWidget;
import dji.v5.ux.visualcamera.shutter.CameraConfigShutterWidget;
import dji.v5.ux.visualcamera.ssd.CameraConfigSSDWidget;
import dji.v5.ux.visualcamera.storage.CameraConfigStorageWidget;
import dji.v5.ux.visualcamera.wb.CameraConfigWBWidget;
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
    protected ConstraintLayout parentView;
    protected SystemStatusListPanelWidget systemStatusListPanelWidget;
    protected SimulatorControlWidget simulatorControlWidget;
    protected CameraConfigISOAndEIWidget cameraConfigISOAndEIWidget;
    protected LensControlWidget lensControlWidget;
    protected CameraConfigShutterWidget cameraConfigShutterWidget;
    protected CameraConfigApertureWidget cameraConfigApertureWidget;
    protected CameraConfigEVWidget cameraConfigEVWidget;
    protected CameraConfigWBWidget cameraConfigWBWidget;
    protected CameraConfigStorageWidget cameraConfigStorageWidget;
    protected CameraConfigSSDWidget cameraConfigSSDWidget;
    protected AutoExposureLockWidget autoExposureLockWidget;
    protected FocusModeWidget focusModeWidget;
    protected FocusExposureSwitchWidget focusExposureSwitchWidget;
    protected CameraControlsWidget cameraControlsWidget;
    protected ExposureSettingsPanel exposureSettingsPanel;
    protected PrimaryFlightDisplayWidget pfvFlightDisplayWidget;

    private int widgetHeight;
    private int widgetWidth;
    private int widgetMargin;
    private int deviceWidth;
    private int deviceHeight;
    private CompositeDisposable compositeDisposable;
    private final DataProcessor<Boolean> cameraSourceProcessor = DataProcessor.create(false);
    //endregion

    //region Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uxsdk_activity_default_layout);

        widgetHeight = (int) getResources().getDimension(R.dimen.uxsdk_mini_map_height);
        widgetWidth = (int) getResources().getDimension(R.dimen.uxsdk_mini_map_width);
        widgetMargin = (int) getResources().getDimension(R.dimen.uxsdk_mini_map_margin);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        deviceHeight = displayMetrics.heightPixels;
        deviceWidth = displayMetrics.widthPixels;

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

        primaryFpvWidget = findViewById(R.id.widget_primary_fpv);
        fpvInteractionWidget = findViewById(R.id.widget_fpv_interaction);
        secondaryFPVWidget = findViewById(R.id.widget_secondary_fpv);
        parentView = findViewById(R.id.root_view);
        systemStatusListPanelWidget = findViewById(R.id.widget_panel_system_status_list);
        simulatorControlWidget = findViewById(R.id.widget_simulator_control);
        cameraConfigISOAndEIWidget = findViewById(R.id.widget_camera_config_iso_and_ei);
        lensControlWidget = findViewById(R.id.widget_lens_control);
        cameraConfigShutterWidget = findViewById(R.id.widget_camera_config_shutter);
        cameraConfigApertureWidget = findViewById(R.id.widget_camera_config_aperture);
        cameraConfigEVWidget = findViewById(R.id.widget_camera_config_ev);
        cameraConfigWBWidget = findViewById(R.id.widget_camera_config_wb);
        cameraConfigStorageWidget = findViewById(R.id.widget_camera_config_storage);
        cameraConfigSSDWidget = findViewById(R.id.widget_camera_config_ssd);
        autoExposureLockWidget = findViewById(R.id.widget_auto_exposure_lock);
        focusModeWidget = findViewById(R.id.widget_focus_mode);
        focusExposureSwitchWidget = findViewById(R.id.widget_focus_exposure_switch);
        exposureSettingsPanel = findViewById(R.id.panel_camera_controls_exposure_settings);
        pfvFlightDisplayWidget = findViewById(R.id.widget_fpv_flight_display_widget);
        cameraControlsWidget = findViewById(R.id.widget_camera_controls);
        cameraControlsWidget.getExposureSettingsIndicatorWidget().setStateChangeResourceId(R.id.panel_camera_controls_exposure_settings);

        initClickListener();
        MediaDataCenter.getInstance().getVideoStreamManager().addStreamSourcesListener(sources -> runOnUiThread(() -> updateFPVWidgetSource(sources)));
        primaryFpvWidget.setOnFPVStreamSourceListener(this::onCameraSourceUpdated);
    }

    private void initClickListener() {
        secondaryFPVWidget.setOnClickListener(v -> {
            swapVideoSource();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MediaDataCenter.getInstance().getVideoStreamManager().clearAllStreamSourcesListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                .observeOn(AndroidSchedulers.mainThread())
                .sample(300, TimeUnit.MILLISECONDS)
                .subscribe(result -> {
                })
        );
    }

    @Override
    protected void onPause() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
            compositeDisposable = null;
        }
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
        if (streamSources == null) {
            return;
        }

        //没有数据
        if (streamSources.size() == 0) {
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

    private void onCameraSourceUpdated(PhysicalDevicePosition devicePosition, CameraLensType lensType) {
        LogUtils.i(TAG, "onCameraSourceUpdated", devicePosition, lensType);
        ComponentIndexType cameraIndex = CameraUtil.getCameraIndex(devicePosition);
        cameraConfigISOAndEIWidget.updateCameraSource(cameraIndex, lensType);
        fpvInteractionWidget.updateCameraSource(cameraIndex, lensType);
        fpvInteractionWidget.updateGimbalIndex(CommonUtils.getGimbalIndex(devicePosition));
        lensControlWidget.updateCameraSource(cameraIndex, lensType);
        cameraConfigShutterWidget.updateCameraSource(cameraIndex, lensType);
        cameraConfigEVWidget.updateCameraSource(cameraIndex, lensType);
        cameraConfigWBWidget.updateCameraSource(cameraIndex, lensType);
        cameraConfigStorageWidget.updateCameraSource(cameraIndex, lensType);
        cameraConfigSSDWidget.updateCameraSource(cameraIndex, lensType);
        autoExposureLockWidget.updateCameraSource(cameraIndex, lensType);
        focusModeWidget.updateCameraSource(cameraIndex, lensType);
        focusExposureSwitchWidget.updateCameraSource(cameraIndex, lensType);
        cameraControlsWidget.updateCameraSource(cameraIndex, lensType);
        exposureSettingsPanel.updateCameraSource(cameraIndex, lensType);
        //飞行控件只在fpv图传下显示
        pfvFlightDisplayWidget.setVisibility(devicePosition == PhysicalDevicePosition.NOSE ? View.VISIBLE : View.GONE);
    }

    /**
     * Helper method to resize the FPV and Map Widgets.
     *
     * @param viewToEnlarge The view that needs to be enlarged to full screen.
     * @param viewToShrink  The view that needs to be shrunk to a thumbnail.
     */
    private void resizeViews(View viewToEnlarge, View viewToShrink) {
        //enlarge first widget
        ResizeAnimation enlargeAnimation = new ResizeAnimation(viewToEnlarge, widgetWidth, widgetHeight, deviceWidth, deviceHeight, 0);
        viewToEnlarge.startAnimation(enlargeAnimation);

        //shrink second widget
        ResizeAnimation shrinkAnimation = new ResizeAnimation(viewToShrink, deviceWidth, deviceHeight, widgetWidth, widgetHeight, widgetMargin);
        viewToShrink.startAnimation(shrinkAnimation);
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
        StreamSource newPrimaryStreamSource = primaryFpvWidget.getStreamSource();
        fpvInteractionWidget.setInteractionEnabled(false);
        if (newPrimaryStreamSource != null) {
            fpvInteractionWidget.setInteractionEnabled(newPrimaryStreamSource.getPhysicalDevicePosition() != PhysicalDevicePosition.NOSE);
        }
    }

    /**
     * Animation to change the size of a view.
     */
    private static class ResizeAnimation extends Animation {

        private static final int DURATION = 300;

        private View view;
        private int toHeight;
        private int fromHeight;
        private int toWidth;
        private int fromWidth;
        private int margin;

        private ResizeAnimation(View v, int fromWidth, int fromHeight, int toWidth, int toHeight, int margin) {
            this.toHeight = toHeight;
            this.toWidth = toWidth;
            this.fromHeight = fromHeight;
            this.fromWidth = fromWidth;
            view = v;
            this.margin = margin;
            setDuration(DURATION);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float height = (toHeight - fromHeight) * interpolatedTime + fromHeight;
            float width = (toWidth - fromWidth) * interpolatedTime + fromWidth;
            ConstraintLayout.LayoutParams p = (ConstraintLayout.LayoutParams) view.getLayoutParams();
            p.height = (int) height;
            p.width = (int) width;
            p.rightMargin = margin;
            p.bottomMargin = margin;
            view.requestLayout();
        }
    }
    //endregion
}
