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

package dji.v5.ux.sample.showcase.widgetlist;

import android.os.Bundle;
import android.view.ViewGroup;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import dji.v5.ux.R;
import dji.v5.ux.accessory.RTKEnabledWidget;
import dji.v5.ux.accessory.RTKSatelliteStatusWidget;
import dji.v5.ux.accessory.RTKWidget;
import dji.v5.ux.cameracore.widget.autoexposurelock.AutoExposureLockWidget;
import dji.v5.ux.cameracore.widget.cameracapture.CameraCaptureWidget;
import dji.v5.ux.cameracore.widget.cameracapture.recordvideo.RecordVideoWidget;
import dji.v5.ux.cameracore.widget.cameracapture.shootphoto.ShootPhotoWidget;
import dji.v5.ux.cameracore.widget.cameracontrols.CameraControlsWidget;
import dji.v5.ux.cameracore.widget.cameracontrols.camerasettingsindicator.CameraSettingsMenuIndicatorWidget;
import dji.v5.ux.cameracore.widget.cameracontrols.exposuresettings.ExposureModeSettingWidget;
import dji.v5.ux.cameracore.widget.cameracontrols.exposuresettings.ExposureSettingsPanel;
import dji.v5.ux.cameracore.widget.cameracontrols.exposuresettings.ISOAndEISettingWidget;
import dji.v5.ux.cameracore.widget.cameracontrols.exposuresettingsindicator.ExposureSettingsIndicatorWidget;
import dji.v5.ux.cameracore.widget.cameracontrols.photovideoswitch.PhotoVideoSwitchWidget;
import dji.v5.ux.cameracore.widget.focusexposureswitch.FocusExposureSwitchWidget;
import dji.v5.ux.cameracore.widget.focusmode.FocusModeWidget;
import dji.v5.ux.cameracore.widget.fpvinteraction.FPVInteractionWidget;
import dji.v5.ux.core.panel.systemstatus.SystemStatusListPanelWidget;
import dji.v5.ux.core.panel.telemetry.TelemetryPanelWidget;
import dji.v5.ux.core.panel.topbar.TopBarPanelWidget;
import dji.v5.ux.core.widget.airsense.AirSenseWidget;
import dji.v5.ux.core.widget.altitude.AGLAltitudeWidget;
import dji.v5.ux.core.widget.battery.BatteryWidget;
import dji.v5.ux.core.widget.compass.CompassWidget;
import dji.v5.ux.core.widget.connection.ConnectionWidget;
import dji.v5.ux.core.widget.distancehome.DistanceHomeWidget;
import dji.v5.ux.core.widget.distancerc.DistanceRCWidget;
import dji.v5.ux.core.widget.flightmode.FlightModeWidget;
import dji.v5.ux.core.widget.fpv.FPVWidget;
import dji.v5.ux.core.widget.gpssignal.GPSSignalWidget;
import dji.v5.ux.core.widget.horizontalvelocity.HorizontalVelocityWidget;
import dji.v5.ux.core.widget.hsi.AttitudeDisplayWidget;
import dji.v5.ux.core.widget.hsi.HorizontalSituationIndicatorWidget;
import dji.v5.ux.core.widget.hsi.PrimaryFlightDisplayWidget;
import dji.v5.ux.core.widget.hsi.SpeedDisplayWidget;
import dji.v5.ux.core.widget.remainingflighttime.RemainingFlightTimeWidget;
import dji.v5.ux.core.widget.remotecontrollersignal.RemoteControllerSignalWidget;
import dji.v5.ux.core.widget.simulator.SimulatorIndicatorWidget;
import dji.v5.ux.core.widget.systemstatus.SystemStatusWidget;
import dji.v5.ux.core.widget.useraccount.UserAccountLoginWidget;
import dji.v5.ux.core.widget.verticalvelocity.VerticalVelocityWidget;
import dji.v5.ux.core.widget.videosignal.VideoSignalWidget;
import dji.v5.ux.core.widget.vision.VisionWidget;
import dji.v5.ux.core.widget.vps.VPSWidget;
import dji.v5.ux.training.simulatorcontrol.SimulatorControlWidget;
import dji.v5.ux.visualcamera.aperture.CameraConfigApertureWidget;
import dji.v5.ux.visualcamera.ev.CameraConfigEVWidget;
import dji.v5.ux.visualcamera.iso.CameraConfigISOAndEIWidget;
import dji.v5.ux.visualcamera.shutter.CameraConfigShutterWidget;
import dji.v5.ux.visualcamera.ssd.CameraConfigSSDWidget;
import dji.v5.ux.visualcamera.storage.CameraConfigStorageWidget;
import dji.v5.ux.visualcamera.wb.CameraConfigWBWidget;

/**
 * Displays a list of widget names. Clicking on a widget name will show that widget in a separate
 * panel on large devices, or in a new activity on smaller devices.
 */
public class WidgetsActivity extends AppCompatActivity implements WidgetListFragment.OnWidgetItemSelectedListener {

    protected ArrayList<WidgetListItem> widgetListItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        populateList();
        setContentView(R.layout.uxsdk_activity_widgets);
        WidgetListFragment listFragment = new WidgetListFragment();
        listFragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, listFragment)
                .commit();
    }

    private void populateList() {
        widgetListItems = new ArrayList<>();
        widgetListItems.add(new WidgetListItem(R.string.air_sense_widget_title, new WidgetViewHolder(AirSenseWidget.class, 58, 50)));
        widgetListItems.add(new WidgetListItem(R.string.altitude_widget_title, new WidgetViewHolder(AGLAltitudeWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.auto_exposure_lock_widget_title, new WidgetViewHolder(AutoExposureLockWidget.class, 35, 35)));
        widgetListItems.add(new WidgetListItem(R.string.battery_widget_title, new WidgetViewHolder(BatteryWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.camera_capture_widget_title, new WidgetViewHolder(CameraCaptureWidget.class, 50, 50)));
        widgetListItems.add(new WidgetListItem(R.string.camera_config_aperture_widget_title, new WidgetViewHolder(CameraConfigApertureWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.camera_config_ev_widget_title, new WidgetViewHolder(CameraConfigEVWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.camera_config_iso_widget_title, new WidgetViewHolder(CameraConfigISOAndEIWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.camera_config_shutter_widget_title, new WidgetViewHolder(CameraConfigShutterWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.camera_config_ssd_widget_title, new WidgetViewHolder(CameraConfigSSDWidget.class, ViewGroup.LayoutParams.WRAP_CONTENT, 28)));
        widgetListItems.add(new WidgetListItem(R.string.camera_config_storage_widget_title, new WidgetViewHolder(CameraConfigStorageWidget.class, ViewGroup.LayoutParams.WRAP_CONTENT, 28)));
        widgetListItems.add(new WidgetListItem(R.string.camera_config_wb_widget_title, new WidgetViewHolder(CameraConfigWBWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.camera_controls_widget_title, new WidgetViewHolder(CameraControlsWidget.class, 50, 213)));
        widgetListItems.add(new WidgetListItem(R.string.camera_settings_menu_indicator_widget_title, new WidgetViewHolder(CameraSettingsMenuIndicatorWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.compass_widget_title, new WidgetViewHolder(CompassWidget.class, ViewGroup.LayoutParams.WRAP_CONTENT, 91)));
        widgetListItems.add(new WidgetListItem(R.string.connection_widget_title, new WidgetViewHolder(ConnectionWidget.class, ViewGroup.LayoutParams.WRAP_CONTENT, 50)));
        widgetListItems.add(new WidgetListItem(R.string.distance_home_widget_title, new WidgetViewHolder(DistanceHomeWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.distance_rc_widget_title, new WidgetViewHolder(DistanceRCWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.exposure_settings_indicator_widget_title, new WidgetViewHolder(ExposureSettingsIndicatorWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.flight_mode_widget_title, new WidgetViewHolder(FlightModeWidget.class, ViewGroup.LayoutParams.WRAP_CONTENT, 50)));
        widgetListItems.add(new WidgetListItem(R.string.focus_exposure_switch_widget_title, new WidgetViewHolder(FocusExposureSwitchWidget.class, 35, 35)));
        widgetListItems.add(new WidgetListItem(R.string.focus_mode_widget_title, new WidgetViewHolder(FocusModeWidget.class, 35, 35)));
        widgetListItems.add(new WidgetListItem(R.string.fpv_widget_title, new WidgetViewHolder(FPVWidget.class, 150, 100)));
        widgetListItems.add(new WidgetListItem(R.string.fpv_interaction_widget_title, new WidgetViewHolder(FPVInteractionWidget.class, 150, 100)));
        widgetListItems.add(new WidgetListItem(R.string.gps_signal_widget_title, new WidgetViewHolder(GPSSignalWidget.class, 95, 50)));
        widgetListItems.add(new WidgetListItem(R.string.horizontal_velocity_widget_title, new WidgetViewHolder(HorizontalVelocityWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.photo_video_switch_widget_title, new WidgetViewHolder(PhotoVideoSwitchWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.system_status_widget_title, new WidgetViewHolder(SystemStatusWidget.class, 238, 33)));
        widgetListItems.add(new WidgetListItem(R.string.record_video_widget_title, new WidgetViewHolder(RecordVideoWidget.class, 50, 50)));
        widgetListItems.add(new WidgetListItem(R.string.remaining_flight_time_widget_title, new WidgetViewHolder(RemainingFlightTimeWidget.class, ViewGroup.LayoutParams.MATCH_PARENT, 30)));
        widgetListItems.add(new WidgetListItem(R.string.remote_control_signal_widget_title, new WidgetViewHolder(RemoteControllerSignalWidget.class, 38, 22)));
        widgetListItems.add(new WidgetListItem(R.string.rtk_enabled_widget_title, new WidgetViewHolder(RTKEnabledWidget.class, ViewGroup.LayoutParams.MATCH_PARENT, 150)));
        widgetListItems.add(new WidgetListItem(R.string.rtk_satellite_status_widget_title, new WidgetViewHolder(RTKSatelliteStatusWidget.class, ViewGroup.LayoutParams.MATCH_PARENT, 350)));
        widgetListItems.add(new WidgetListItem(R.string.rtk_widget_title, new WidgetViewHolder(RTKWidget.class, ViewGroup.LayoutParams.MATCH_PARENT, 200)));
        widgetListItems.add(new WidgetListItem(R.string.shoot_photo_widget_title, new WidgetViewHolder(ShootPhotoWidget.class, 50, 50)));
        widgetListItems.add(new WidgetListItem(R.string.simulator_indicator_control_widgets_title, new WidgetViewHolder(SimulatorIndicatorWidget.class, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT), new WidgetViewHolder(SimulatorControlWidget.class, 300, ViewGroup.LayoutParams.MATCH_PARENT)));
        widgetListItems.add(new WidgetListItem(R.string.system_status_panel_title, new WidgetViewHolder(SystemStatusListPanelWidget.class, 550, 200)));
        widgetListItems.add(new WidgetListItem(R.string.telemetry_widget_title, new WidgetViewHolder(TelemetryPanelWidget.class, 350, 91)));
        widgetListItems.add(new WidgetListItem(R.string.top_bar_panel_title, new WidgetViewHolder(TopBarPanelWidget.class, ViewGroup.LayoutParams.MATCH_PARENT, 25)));
        widgetListItems.add(new WidgetListItem(R.string.user_account_login_widget_title, new WidgetViewHolder(UserAccountLoginWidget.class, 240, 60)));
        widgetListItems.add(new WidgetListItem(R.string.vertical_velocity_widget_title, new WidgetViewHolder(VerticalVelocityWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.vision_widget_title, new WidgetViewHolder(VisionWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.video_signal_widget_title, new WidgetViewHolder(VideoSignalWidget.class, 86, 50)));
        widgetListItems.add(new WidgetListItem(R.string.vps_widget_title, new WidgetViewHolder(VPSWidget.class)));
        widgetListItems.add(new WidgetListItem(R.string.exposure_setting_panel_title, new WidgetViewHolder(ExposureSettingsPanel.class, 211, 316)));
        widgetListItems.add(new WidgetListItem(R.string.exposure_mode_setting_widget_title, new WidgetViewHolder(ExposureModeSettingWidget.class, 160, 30)));
        widgetListItems.add(new WidgetListItem(R.string.iso_and_ei_setting_widget_title, new WidgetViewHolder(ISOAndEISettingWidget.class, 211, 60)));
        widgetListItems.add(new WidgetListItem(R.string.horizontal_situation_indicator_widget_title, new WidgetViewHolder(HorizontalSituationIndicatorWidget.class, 350, ViewGroup.LayoutParams.WRAP_CONTENT)));
        widgetListItems.add(new WidgetListItem(R.string.speed_display_widget_title, new WidgetViewHolder(SpeedDisplayWidget.class, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)));
        widgetListItems.add(new WidgetListItem(R.string.attitude_display_widget_title, new WidgetViewHolder(AttitudeDisplayWidget.class, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)));
        widgetListItems.add(new WidgetListItem(R.string.primary_flight_display_widget_title, new WidgetViewHolder(PrimaryFlightDisplayWidget.class, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)));
    }

    @Override
    public void onWidgetItemSelected(int position) {
        WidgetFragment newFragment = new WidgetFragment();
        Bundle args = new Bundle();
        args.putInt(WidgetFragment.ARG_POSITION, position);
        newFragment.setArguments(args);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
