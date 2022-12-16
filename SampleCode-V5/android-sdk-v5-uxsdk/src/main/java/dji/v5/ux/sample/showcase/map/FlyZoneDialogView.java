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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

import dji.v5.ux.R;

/**
 * A dialog view that allows the user to customize the fly zones of a MapWidget.
 */
public class FlyZoneDialogView extends ScrollView {

//    //region views
//    private CheckBox all;
//    private CheckBox auth;
//    private CheckBox warning;
//    private CheckBox enhancedWarning;
//    private CheckBox restricted;
//    private Button btnCustomUnlockColor;
//    private Button btnCustomUnlockSync;
//    //endregion
//
    //region Lifecycle
    public FlyZoneDialogView(Context context) {
        super(context);
        inflate(context, R.layout.uxsdk_dialog_fly_zone, this);
    }

    public FlyZoneDialogView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.uxsdk_dialog_fly_zone, this);
    }
//
//    /**
//     * Initialize the components of the dialog based on the current state of the MapWidget.
//     *
//     * @param mapWidget The MapWidget that this fly zone dialog controls.
//     */
//    public void init(MapWidget mapWidget) {
//        initCheckboxes(mapWidget);
//        initColors(mapWidget);
//    }
//    //endregion
//
//    /**
//     * Initialize the checkboxes that enable and disable each fly zone category.
//     *
//     * @param mapWidget The MapWidget that this fly zone dialog controls.
//     */
//    private void initCheckboxes(final MapWidget mapWidget) {
//        all = findViewById(R.id.all);
//        auth = findViewById(R.id.auth);
//        warning = findViewById(R.id.warning);
//        enhancedWarning = findViewById(R.id.enhanced_warning);
//        restricted = findViewById(R.id.restricted);
//        Switch switchCustomUnlock = findViewById(R.id.custom_unlock_switch);
//        switchCustomUnlock.setChecked(mapWidget.getFlyZoneHelper().isCustomUnlockZonesVisible());
//        CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
//                switch (compoundButton.getId()) {
//                    case R.id.all:
//                        auth.setChecked(isChecked);
//                        warning.setChecked(isChecked);
//                        enhancedWarning.setChecked(isChecked);
//                        restricted.setChecked(isChecked);
//                        break;
//                    case R.id.custom_unlock_switch:
//                        btnCustomUnlockColor.setEnabled(isChecked);
//                        btnCustomUnlockSync.setEnabled(isChecked);
//                        if (isChecked) {
//                            mapWidget.getFlyZoneHelper().setCustomUnlockZonesVisible(true);
//                        } else {
//                            mapWidget.getFlyZoneHelper().setCustomUnlockZonesVisible(false);
//                        }
//                        break;
//                    default:
//                        all.setOnCheckedChangeListener(null);
//                        all.setChecked(auth.isChecked()
//                                && warning.isChecked()
//                                && enhancedWarning.isChecked()
//                                && restricted.isChecked());
//                        all.setOnCheckedChangeListener(this);
//                        break;
//                }
//            }
//        };
//        auth.setChecked(mapWidget.getFlyZoneHelper().isFlyZoneVisible(FlyZoneCategory.AUTHORIZATION));
//        warning.setChecked(mapWidget.getFlyZoneHelper().isFlyZoneVisible(FlyZoneCategory.WARNING));
//        enhancedWarning.setChecked(mapWidget.getFlyZoneHelper().isFlyZoneVisible(FlyZoneCategory.ENHANCED_WARNING));
//        restricted.setChecked(mapWidget.getFlyZoneHelper().isFlyZoneVisible(FlyZoneCategory.RESTRICTED));
//        all.setChecked(auth.isChecked()
//                && warning.isChecked()
//                && enhancedWarning.isChecked()
//                && restricted.isChecked());
//
//        all.setOnCheckedChangeListener(listener);
//        auth.setOnCheckedChangeListener(listener);
//        warning.setOnCheckedChangeListener(listener);
//        enhancedWarning.setOnCheckedChangeListener(listener);
//        restricted.setOnCheckedChangeListener(listener);
//        switchCustomUnlock.setOnCheckedChangeListener(listener);
//    }
//
//    /**
//     * Initialize the color controls for the fly zones.
//     *
//     * @param mapWidget The MapWidget that this fly zone dialog controls.
//     */
//    private void initColors(final MapWidget mapWidget) {
//        Button authColor = findViewById(R.id.auth_color);
//        Button warningColor = findViewById(R.id.warning_color);
//        Button enhancedWarningColor = findViewById(R.id.enhanced_warning_color);
//        Button restrictedColor = findViewById(R.id.restricted_color);
//        Button maxHeightColor = findViewById(R.id.max_height_color);
//        Button selfUnlockColor = findViewById(R.id.self_unlock_color);
//        btnCustomUnlockColor = findViewById(R.id.custom_unlock_color);
//        btnCustomUnlockSync = findViewById(R.id.custom_unlock_sync);
//        btnCustomUnlockColor.setEnabled(mapWidget.getFlyZoneHelper().isCustomUnlockZonesVisible());
//        btnCustomUnlockSync.setEnabled(mapWidget.getFlyZoneHelper().isCustomUnlockZonesVisible());
//
//
//        final float STROKE_WIDTH = 15.0f;
//        authColor.setBackground(getBackground(mapWidget.getFlyZoneHelper().getFlyZoneColor(FlyZoneCategory.AUTHORIZATION),
//                mapWidget.getFlyZoneHelper().getFlyZoneAlpha(FlyZoneCategory.AUTHORIZATION),
//                STROKE_WIDTH));
//        warningColor.setBackground(getBackground(mapWidget.getFlyZoneHelper().getFlyZoneColor(FlyZoneCategory.WARNING),
//                mapWidget.getFlyZoneHelper().getFlyZoneAlpha(FlyZoneCategory.WARNING),
//                STROKE_WIDTH));
//        enhancedWarningColor.setBackground(getBackground(mapWidget.getFlyZoneHelper().getFlyZoneColor(FlyZoneCategory.ENHANCED_WARNING),
//                mapWidget.getFlyZoneHelper().getFlyZoneAlpha(FlyZoneCategory.ENHANCED_WARNING),
//                STROKE_WIDTH));
//        restrictedColor.setBackground(getBackground(mapWidget.getFlyZoneHelper().getFlyZoneColor(FlyZoneCategory.RESTRICTED),
//                mapWidget.getFlyZoneHelper().getFlyZoneAlpha(FlyZoneCategory.RESTRICTED),
//                STROKE_WIDTH));
//        maxHeightColor.setBackground(getBackground(mapWidget.getFlyZoneHelper().getMaximumHeightColor(),
//                mapWidget.getFlyZoneHelper().getMaximumHeightAlpha(), STROKE_WIDTH));
//        selfUnlockColor.setBackground(getBackground(mapWidget.getFlyZoneHelper().getSelfUnlockColor(),
//                mapWidget.getFlyZoneHelper().getSelfUnlockAlpha(), STROKE_WIDTH));
//
//        OnClickListener onClickListener = view -> {
//            Random rnd = new Random();
//            @ColorInt int randomColor = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
//
//            int alpha = 26;
//            float strokeWidth = mapWidget.getFlyZoneHelper().getFlyZoneBorderWidth();
//            switch (view.getId()) {
//                case R.id.auth_color:
//                    alpha = mapWidget.getFlyZoneHelper().getFlyZoneAlpha(FlyZoneCategory.AUTHORIZATION);
//                    mapWidget.getFlyZoneHelper().setFlyZoneColor(FlyZoneCategory.AUTHORIZATION, randomColor);
//                    break;
//                case R.id.warning_color:
//                    alpha = mapWidget.getFlyZoneHelper().getFlyZoneAlpha(FlyZoneCategory.WARNING);
//                    mapWidget.getFlyZoneHelper().setFlyZoneColor(FlyZoneCategory.WARNING, randomColor);
//                    break;
//                case R.id.enhanced_warning_color:
//                    alpha = mapWidget.getFlyZoneHelper().getFlyZoneAlpha(FlyZoneCategory.ENHANCED_WARNING);
//                    mapWidget.getFlyZoneHelper().setFlyZoneColor(FlyZoneCategory.ENHANCED_WARNING, randomColor);
//                    break;
//                case R.id.restricted_color:
//                    alpha = mapWidget.getFlyZoneHelper().getFlyZoneAlpha(FlyZoneCategory.RESTRICTED);
//                    mapWidget.getFlyZoneHelper().setFlyZoneColor(FlyZoneCategory.RESTRICTED, randomColor);
//                    break;
//                case R.id.max_height_color:
//                    alpha = mapWidget.getFlyZoneHelper().getMaximumHeightAlpha();
//                    mapWidget.getFlyZoneHelper().setMaximumHeightColor(randomColor);
//                    break;
//                case R.id.self_unlock_color:
//                    alpha = mapWidget.getFlyZoneHelper().getSelfUnlockAlpha();
//                    mapWidget.getFlyZoneHelper().setSelfUnlockColor(randomColor);
//                    break;
//                case R.id.custom_unlock_color:
//                    mapWidget.getFlyZoneHelper().setCustomUnlockFlyZoneColor(randomColor);
//                    randomColor = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
//                    mapWidget.getFlyZoneHelper().setCustomUnlockFlyZoneSentToAircraftColor(randomColor);
//                    randomColor = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
//                    mapWidget.getFlyZoneHelper().setCustomUnlockFlyZoneEnabledColor(randomColor);
//                    return;
//                case R.id.custom_unlock_sync:
//                    mapWidget.syncCustomUnlockZonesToAircraft();
//                    return;
//                default:
//                    return;
//            }
//            view.setBackground(getBackground(randomColor, alpha, strokeWidth));
//        };
//        authColor.setOnClickListener(onClickListener);
//        warningColor.setOnClickListener(onClickListener);
//        enhancedWarningColor.setOnClickListener(onClickListener);
//        restrictedColor.setOnClickListener(onClickListener);
//        maxHeightColor.setOnClickListener(onClickListener);
//        selfUnlockColor.setOnClickListener(onClickListener);
//        btnCustomUnlockColor.setOnClickListener(onClickListener);
//        btnCustomUnlockSync.setOnClickListener(onClickListener);
//    }
//
//    /**
//     * Get a oval shaped background with a border of the given color and a fill of the given color
//     * and alpha.
//     *
//     * @param color       The color of the border and fill of the oval.
//     * @param alpha       The alpha of the fill of the oval.
//     * @param strokeWidth The stroke width of the border.
//     * @return A GradientDrawable object.
//     */
//    private GradientDrawable getBackground(@ColorInt int color, int alpha, float strokeWidth) {
//        GradientDrawable drawable = new GradientDrawable();
//        drawable.setShape(GradientDrawable.OVAL);
//        drawable.setStroke((int) strokeWidth, color);
//        drawable.setColor(ColorUtils.setAlphaComponent(color, alpha));
//        return drawable;
//    }
//
//    /**
//     * Get whether the fly zones of the given category are enabled.
//     *
//     * @param category The category of fly zones.
//     * @return `true` if fly zones of the given category are enabled, `false` otherwise.
//     */
//    public boolean isFlyZoneEnabled(FlyZoneCategory category) {
//        switch (category) {
//            case AUTHORIZATION:
//                return auth.isChecked();
//            case WARNING:
//                return warning.isChecked();
//            case ENHANCED_WARNING:
//                return enhancedWarning.isChecked();
//            case RESTRICTED:
//                return restricted.isChecked();
//            default:
//                return false;
//        }
//    }
}
