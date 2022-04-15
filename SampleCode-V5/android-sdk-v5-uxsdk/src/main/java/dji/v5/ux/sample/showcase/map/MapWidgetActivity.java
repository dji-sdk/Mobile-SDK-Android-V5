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

package dji.v5.ux.sample.showcase.map;

import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Displays a MapWidget and controls to customize the look of
 * each of the components.
 */
public class MapWidgetActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {
//    //region constants
//    private static final String TAG = "MapWidgetActivity";
//    /**
//     * The key for passing a map provider through the intent.
//     */
//    public static final String MAP_PROVIDER_KEY = "MapProvider";
//    //endregion
//
//    //region Fields
//    @BindView(R.id.map_widget)
//    protected MapWidget mapWidget;
//    @BindView(R.id.icon_spinner)
//    protected Spinner iconSpinner;
//    @BindView(R.id.line_spinner)
//    protected Spinner lineSpinner;
//    @BindView(R.id.line_width_picker)
//    protected SeekBar lineWidthPicker;
//    @BindView(R.id.line_color)
//    protected TextView lineColor;
//    @BindView(R.id.settings_scroll_view)
//    protected ScrollView scrollView;
//    @BindView(R.id.btn_settings)
//    protected ImageView btnPanel;
//
//    private ImageView selectedIcon;
//    private int lineWidthValue;
//    private MapOverlay mapOverlay;
//    private GroundOverlay groundOverlay;
//    private TileOverlay tileOverlay;
//    private SettingDefinitions.MapProvider mapProvider;
//    private Map hereMap;
//    private GoogleMap googleMap;
//    private AMap aMap;
//    private MapboxMap mapboxMap;
//    private boolean isPanelOpen = true;
//    private List<DJIMarker> markerList;
//    //endregion
//
//    //region Lifecycle
//    @Override
//    public void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_map_widget);
//        ((Spinner) findViewById(R.id.map_spinner)).setSelection(0, false); // before binding so the listener won't be called
//        ButterKnife.bind(this);
//        markerList = new ArrayList<>();
//        MapWidget.OnMapReadyListener onMapReadyListener = map -> {
//            map.setMapType(DJIMap.MapType.NORMAL);
//
//            //Add toasts when a marker is dragged
//            map.setOnMarkerDragListener(new DJIMap.OnMarkerDragListener() {
//                @Override
//                public void onMarkerDragStart(DJIMarker djiMarker) {
//                    if (markerList.contains(djiMarker)) {
//                        Toast.makeText(MapWidgetActivity.this,
//                                getString(R.string.marker_drag_started, markerList.indexOf(djiMarker)),
//                                Toast.LENGTH_SHORT).show();
//                    }
//                }
//
//                @Override
//                public void onMarkerDrag(DJIMarker djiMarker) {
//                    // do nothing
//                }
//
//                @Override
//                public void onMarkerDragEnd(DJIMarker djiMarker) {
//                    if (markerList.contains(djiMarker)) {
//                        Toast.makeText(MapWidgetActivity.this,
//                                getString(R.string.marker_drag_ended, markerList.indexOf(djiMarker)),
//                                Toast.LENGTH_SHORT).show();
//                    }
//                }
//            });
//            //Add toasts when a marker is clicked
//            mapWidget.setOnMarkerClickListener(djiMarker -> {
//                Toast.makeText(MapWidgetActivity.this, getString(R.string.marker_clicked, markerList.indexOf(djiMarker)),
//                        Toast.LENGTH_SHORT).show();
//                return true;
//            });
//            //Add a marker to the map when the map is clicked
//            map.setOnMapClickListener(djiLatLng -> {
//                DJIMarker marker = map.addMarker(new DJIMarkerOptions().position(djiLatLng).draggable(true));
//                markerList.add(marker);
//            });
//
//        };
//        Intent intent = getIntent();
//        mapProvider = SettingDefinitions.MapProvider.find(intent.getIntExtra(MAP_PROVIDER_KEY, 0));
//        switch (mapProvider) {
//            case HERE:
//                boolean success = setIsolatedDiskCacheRootPath(
//                        getExternalFilesDir(null) + File.separator + ".here-maps-cache");
//                if (success) {
//                    mapWidget.initHereMap(onMapReadyListener);
//                }
//                break;
//            case GOOGLE:
//                mapWidget.initGoogleMap(onMapReadyListener);
//                break;
//            case AMAP:
//                mapWidget.initAMap(onMapReadyListener);
//                break;
//            case MAPBOX:
//            default:
//                mapWidget.initMapboxMap(getResources().getString(R.string.dji_ux_sample_mapbox_token), onMapReadyListener);
//                break;
//        }
//        mapWidget.onCreate(savedInstanceState);
//
//        selectedIcon = findViewById(R.id.icon_1);
//        selectedIcon.setSelected(true);
//        mapWidget.getFlyZoneHelper().setFlyZoneVisible(FlyZoneCategory.WARNING, true);
//        mapWidget.getFlyZoneHelper().setFlyZoneVisible(FlyZoneCategory.ENHANCED_WARNING, true);
//        mapWidget.getFlyZoneHelper().setFlyZoneVisible(FlyZoneCategory.AUTHORIZATION, true);
//        mapWidget.getFlyZoneHelper().setFlyZoneVisible(FlyZoneCategory.RESTRICTED, true);
//
//        lineWidthPicker.setOnSeekBarChangeListener(this);
//    }
//    //endregion
//
//    @OnCheckedChanged(R.id.home_direction)
//    public void onHomeDirectionCheckedChanged(boolean isChecked) {
//        mapWidget.setDirectionToHomeEnabled(isChecked);
//    }
//
//    @OnCheckedChanged(R.id.lock_bounds)
//    public void onLockBoundsCheckedChanged(boolean isChecked) {
//        mapWidget.setAutoFrameMapEnabled(isChecked);
//    }
//
//    @OnCheckedChanged(R.id.flight_path)
//    public void onFlightPathCheckedChanged(boolean isChecked) {
//        mapWidget.setFlightPathEnabled(isChecked);
//    }
//
//    @OnCheckedChanged(R.id.home_point)
//    public void onHomePointCheckedChanged(boolean isChecked) {
//        mapWidget.setHomeMarkerEnabled(isChecked);
//    }
//
//    @OnCheckedChanged(R.id.gimbal_yaw)
//    public void onGimbalYawCheckedChanged(boolean isChecked) {
//        mapWidget.setGimbalAttitudeEnabled(isChecked);
//    }
//
//    @OnCheckedChanged(R.id.flyzone_unlock)
//    public void onFlyzoneUnlockCheckedChanged(boolean isChecked) {
//        mapWidget.getFlyZoneHelper().setTapToUnlockEnabled(isChecked);
//    }
//
//    @OnCheckedChanged(R.id.flyzone_legend)
//    public void onFlyzoneLegendCheckedChanged(boolean isChecked) {
//        mapWidget.setFlyZoneLegendEnabled(isChecked);
//    }
//
//    @OnCheckedChanged(R.id.login_state_indicator)
//    public void onLoginStateIndicatorCheckedChanged(boolean isChecked) {
//        mapWidget.getUserAccountLoginWidget().setVisibility(isChecked ? View.VISIBLE : View.GONE);
//    }
//
//    @OnCheckedChanged(R.id.map_center_aircraft)
//    public void onMapCenterAircraftCheckedChanged(boolean isChecked) {
//        if (isChecked) {
//            mapWidget.setMapCenterLock(MapWidget.MapCenterLock.AIRCRAFT);
//        }
//    }
//
//    @OnCheckedChanged(R.id.map_center_home)
//    public void onMapCenterHomeCheckedChanged(boolean isChecked) {
//        if (isChecked) {
//            mapWidget.setMapCenterLock(MapWidget.MapCenterLock.HOME);
//        }
//    }
//
//    @OnCheckedChanged(R.id.map_center_none)
//    public void onMapCenterNoneCheckedChanged(boolean isChecked) {
//        if (isChecked) {
//            mapWidget.setMapCenterLock(MapWidget.MapCenterLock.NONE);
//        }
//    }
//
//    @OnClick(R.id.clear_flight_path)
//    public void onClearFlightPathClick() {
//        mapWidget.clearFlightPath();
//    }
//
//    @OnClick(R.id.btn_map_provider_test)
//    public void onOverlayClick() {
//        addOverlay();
//    }
//
//    @OnClick(R.id.line_color)
//    public void onLineColorClick() {
//        setRandomLineColor();
//    }
//
//    @OnClick(R.id.btn_settings)
//    public void onSettingsClick() {
//        movePanel();
//    }
//
//    @OnClick(R.id.btn_fly_zone)
//    public void onFlyZoneButtonClick() {
//        showSelectFlyZoneDialog();
//    }
//
//    /**
//     * Replaces the icon selected in the icon spinner to the icon that was selected below it.
//     */
//    @OnClick(R.id.replace)
//    public void onReplaceClick() {
//        Drawable drawable = selectedIcon.getDrawable();
//        if ("Aircraft".equals(iconSpinner.getSelectedItem())) {
//            mapWidget.setAircraftMarkerIcon(drawable);
//        } else if ("Home".equals(iconSpinner.getSelectedItem())) {
//            mapWidget.setHomeMarkerIcon(drawable);
//        } else if ("Gimbal Yaw".equals(iconSpinner.getSelectedItem())) {
//            mapWidget.setGimbalMarkerIcon(drawable);
//        } else if ("Locked Zone".equals(iconSpinner.getSelectedItem())) {
//            mapWidget.getFlyZoneHelper().setSelfLockedMarkerIcon(drawable);
//        } else if ("Unlocked Zone".equals(iconSpinner.getSelectedItem())) {
//            mapWidget.getFlyZoneHelper().setSelfUnlockedMarkerIcon(drawable);
//        }
//    }
//
//    /**
//     * Selects an icon that the icon selected in the icon spinner will be set to when the replace
//     * button is clicked.
//     *
//     * @param view The selected icon.
//     */
//    @OnClick({R.id.icon_1, R.id.icon_2, R.id.icon_3, R.id.icon_4, R.id.icon_5})
//    public void onIconClick(View view) {
//        ImageView imageView = (ImageView) view;
//        imageView.setSelected(true);
//        selectedIcon.setSelected(false);
//        selectedIcon = imageView;
//    }
//
//    /**
//     * Expands and collapses the panel.
//     */
//    private void movePanel() {
//        int translationStart;
//        int translationEnd;
//        if (isPanelOpen) {
//            translationStart = 0;
//            translationEnd = -scrollView.getWidth();
//        } else {
//
//            scrollView.bringToFront();
//            translationStart = -scrollView.getWidth();
//            translationEnd = 0;
//        }
//        TranslateAnimation animate = new TranslateAnimation(
//                translationStart, translationEnd, 0, 0);
//        animate.setDuration(300);
//        animate.setFillAfter(true);
//        animate.setAnimationListener(new Animation.AnimationListener() {
//            @Override
//            public void onAnimationStart(Animation animation) {
//                // do nothing
//            }
//
//            @Override
//            public void onAnimationEnd(Animation animation) {
//                if (isPanelOpen) {
//                    mapWidget.bringToFront();
//
//                }
//                btnPanel.bringToFront();
//                isPanelOpen = !isPanelOpen;
//
//            }
//
//            @Override
//            public void onAnimationRepeat(Animation animation) {
//                // do nothing
//            }
//        });
//        scrollView.startAnimation(animate);
//
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        mapWidget.onResume();
//    }
//
//    @Override
//    protected void onPause() {
//        mapWidget.onPause();
//        super.onPause();
//    }
//
//    @Override
//    protected void onDestroy() {
//        mapWidget.onDestroy();
//        super.onDestroy();
//    }
//
//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//        mapWidget.onSaveInstanceState(outState);
//    }
//
//    @Override
//    public void onLowMemory() {
//        super.onLowMemory();
//        mapWidget.onLowMemory();
//    }
//
//    /**
//     * Creates an alert dialog to control the fly zones.
//     */
//    private void showSelectFlyZoneDialog() {
//        final FlyZoneDialogView flyZoneDialogView = new FlyZoneDialogView(this);
//        flyZoneDialogView.init(mapWidget);
//
//        DialogInterface.OnClickListener positiveClickListener = (dialog, which) -> {
//            mapWidget.getFlyZoneHelper().setFlyZoneVisible(FlyZoneCategory.AUTHORIZATION,
//                    flyZoneDialogView.isFlyZoneEnabled(FlyZoneCategory.AUTHORIZATION));
//            mapWidget.getFlyZoneHelper().setFlyZoneVisible(FlyZoneCategory.WARNING,
//                    flyZoneDialogView.isFlyZoneEnabled(FlyZoneCategory.WARNING));
//            mapWidget.getFlyZoneHelper().setFlyZoneVisible(FlyZoneCategory.ENHANCED_WARNING,
//                    flyZoneDialogView.isFlyZoneEnabled(FlyZoneCategory.ENHANCED_WARNING));
//            mapWidget.getFlyZoneHelper().setFlyZoneVisible(FlyZoneCategory.RESTRICTED,
//                    flyZoneDialogView.isFlyZoneEnabled(FlyZoneCategory.RESTRICTED));
//            dialog.dismiss();
//        };
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Fly Zones");
//        builder.setView(flyZoneDialogView);
//        builder.setPositiveButton("OK", positiveClickListener);
//        AlertDialog dialog = builder.create();
//        dialog.show();
//    }
//
//    @OnItemSelected(R.id.map_spinner)
//    public void onMapItemSelected(int position) {
//        if (mapWidget.getMap() != null) {
//            switch (position) {
//                case 0:
//                    mapWidget.getMap().setMapType(DJIMap.MapType.NORMAL);
//                    break;
//                case 1:
//                    mapWidget.getMap().setMapType(DJIMap.MapType.SATELLITE);
//                    break;
//                case 2:
//                default:
//                    mapWidget.getMap().setMapType(DJIMap.MapType.HYBRID);
//                    break;
//            }
//        } else {
//            Toast.makeText(getApplicationContext(), R.string.error_map_not_initialized, Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    @OnItemSelected(R.id.line_spinner)
//    public void onLineItemSelected(int position) {
//        int width = 5;
//        switch (position) {
//            case 0:
//                width = (int) mapWidget.getDirectionToHomeWidth();
//                lineColor.setVisibility(View.VISIBLE);
//                lineColor.setTextColor(mapWidget.getDirectionToHomeColor());
//                break;
//            case 1:
//                width = (int) mapWidget.getFlightPathWidth();
//                lineColor.setVisibility(View.VISIBLE);
//                lineColor.setTextColor(mapWidget.getFlightPathColor());
//                break;
//            case 2:
//            default:
//                width = (int) mapWidget.getFlyZoneHelper().getFlyZoneBorderWidth();
//                lineColor.setVisibility(View.GONE);
//                break;
//        }
//        lineWidthPicker.setProgress(width);
//    }
//
//    /**
//     * Shows several examples of overlays that can be added through each provider's API. By fetching
//     * the original map object using {@link DJIMap#getMap()}, any of the provider's unique APIs can
//     * be used.
//     */
//    private void addOverlay() {
//        if (mapWidget.getMap() == null) {
//            Toast.makeText(getApplicationContext(), R.string.error_map_not_initialized, Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        float testLat = 37.4419f;
//        float testLng = -122.1430f;
//        switch (mapProvider) {
//            case HERE:
//                if (mapOverlay == null) {
//                    hereMap = (Map) mapWidget.getMap().getMap();
//                    ImageView overlayView = new ImageView(MapWidgetActivity.this);
//                    overlayView.setImageDrawable(getResources().getDrawable(R.drawable.ic_drone));
//                    GeoCoordinate testLocation = new GeoCoordinate(testLat, testLng);
//                    mapOverlay = new MapOverlay(overlayView, testLocation);
//                    hereMap.addMapOverlay(mapOverlay);
//                } else {
//                    hereMap.removeMapOverlay(mapOverlay);
//                    mapOverlay = null;
//                }
//                break;
//            case GOOGLE:
//                if (groundOverlay == null) {
//                    googleMap = (GoogleMap) mapWidget.getMap().getMap();
//                    LatLng latLng1 = new LatLng(testLat, testLng);
//                    LatLng latLng2 = new LatLng(testLat + 0.25, testLng + 0.25);
//                    LatLng latLng3 = new LatLng(testLat - 0.25, testLng - 0.25);
//                    LatLngBounds bounds = new LatLngBounds(latLng1, latLng2).including(latLng3);
//                    BitmapDescriptor aircraftImage = BitmapDescriptorFactory.fromBitmap(ViewUtil.getBitmapFromVectorDrawable(
//                            getResources().getDrawable(R.drawable.uxsdk_ic_compass_aircraft)));
//                    GroundOverlayOptions groundOverlayOptions = new GroundOverlayOptions();
//                    groundOverlayOptions.image(aircraftImage)
//                            .positionFromBounds(bounds)
//                            .transparency(0.5f)
//                            .visible(true);
//                    groundOverlay = googleMap.addGroundOverlay(groundOverlayOptions);
//                } else {
//                    groundOverlay.remove();
//                    groundOverlay = null;
//                }
//                break;
//            case AMAP:
//                if (tileOverlay == null) {
//                    aMap = (AMap) mapWidget.getMap().getMap();
//                    com.amap.api.maps.model.LatLng[] latlngs = new com.amap.api.maps.model.LatLng[500];
//                    for (int i = 0; i < 500; i++) {
//                        double x_ = Math.random() * 0.5 - 0.25;
//                        double y_ = Math.random() * 0.5 - 0.25;
//                        latlngs[i] = new com.amap.api.maps.model.LatLng(testLat + x_, testLng + y_);
//                    }
//                    HeatmapTileProvider.Builder builder = new HeatmapTileProvider.Builder();
//                    builder.data(Arrays.asList(latlngs));
//                    HeatmapTileProvider heatmapTileProvider = builder.build();
//                    TileOverlayOptions tileOverlayOptions = new TileOverlayOptions();
//                    tileOverlayOptions.tileProvider(heatmapTileProvider).visible(true);
//                    tileOverlay = aMap.addTileOverlay(tileOverlayOptions);
//                } else {
//                    tileOverlay.remove();
//                    tileOverlay = null;
//                }
//                break;
//            case MAPBOX:
//            default:
//                Random rnd = new Random();
//                mapboxMap = (MapboxMap) mapWidget.getMap().getMap();
//                mapboxMap.getStyle(style -> style.getLayer("water")
//                        .setProperties(PropertyFactory.fillColor(Color.argb(255,
//                                rnd.nextInt(256),
//                                rnd.nextInt(256),
//                                rnd.nextInt(256)))));
//                break;
//        }
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        if (mapProvider == SettingDefinitions.MapProvider.HERE) {
//            startService(new Intent(getBaseContext(), MapService.class));
//        }
//    }
//
//    @Override
//    protected void onStop() {
//        if (mapProvider == SettingDefinitions.MapProvider.HERE) {
//            stopService(new Intent(getBaseContext(), MapService.class));
//        }
//        super.onStop();
//    }
//
//    @Override
//    public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
//        this.lineWidthValue = progressValue;
//    }
//
//    @Override
//    public void onStartTrackingTouch(SeekBar seekBar) {
//        // do nothing
//    }
//
//    @Override
//    public void onStopTrackingTouch(SeekBar seekBar) {
//        if ("Home Direction".equals(lineSpinner.getSelectedItem())) {
//            mapWidget.setDirectionToHomeWidth(lineWidthValue);
//        } else if ("Flight Path".equals(lineSpinner.getSelectedItem())) {
//            mapWidget.setFlightPathWidth(lineWidthValue);
//        } else if ("Fly Zone Border".equals(lineSpinner.getSelectedItem())) {
//            mapWidget.getFlyZoneHelper().setFlyZoneBorderWidth(lineWidthValue);
//        }
//    }
//
//    /**
//     * Sets the line indicated by the line spinner to a random color.
//     */
//    private void setRandomLineColor() {
//        Random rnd = new Random();
//        @ColorInt int randomColor = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
//
//        lineColor.setTextColor(randomColor);
//        if ("Home Direction".equals(lineSpinner.getSelectedItem())) {
//            mapWidget.setDirectionToHomeColor(randomColor);
//        } else if ("Flight Path".equals(lineSpinner.getSelectedItem())) {
//            mapWidget.setFlightPathColor(randomColor);
//        }
//    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//        LogUtils.d(TAG,MSG);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
//        LogUtils.d(TAG,MSG);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
//        LogUtils.d(TAG,MSG);
    }
}
