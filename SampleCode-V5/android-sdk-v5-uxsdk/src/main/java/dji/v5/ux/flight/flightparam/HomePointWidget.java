package dji.v5.ux.flight.flightparam;


import android.content.Context;
import android.location.Location;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.sdk.keyvalue.value.common.LocationCoordinate2D;
import dji.sdk.keyvalue.value.remotecontroller.RCMode;
import dji.sdk.keyvalue.value.remotecontroller.RcGPSInfo;
import dji.v5.common.utils.GpsUtils;
import dji.v5.utils.common.LocationUtil;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.ViewUtil;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.disposables.Disposable;

public class HomePointWidget extends ConstraintLayoutWidget<Object> {

    private HomeSetWidgetModel widgetModel = new HomeSetWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());
    private ImageView rcAHomePointIv;
    private ImageView rcBHomePointIv;
    public HomePointWidget(@NonNull Context context) {
        super(context);
    }

    public HomePointWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HomePointWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_setting_menu_aircraft_home_set_layout, this);
        findViewById(R.id.setting_menu_flyc_homepoint_now).setOnClickListener(view -> prepareToSetHomePoint(0));

        rcAHomePointIv = findViewById(R.id.setting_menu_flyc_homepoint_rc_a);
        rcAHomePointIv.setOnClickListener(view -> prepareToSetHomePoint(1));

        rcBHomePointIv = findViewById(R.id.setting_menu_flyc_homepoint_rc_b);
        rcBHomePointIv.setOnClickListener(view -> prepareToSetHomePoint(2));

    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getProductConnection().observeOn(SchedulerProvider.ui()).subscribe(connected -> {
            if (!connected) {
                updateHomePointViewWithSingleRc();
            }
        }));
    }



    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            widgetModel.setup();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
    }



    /**
     * 设置返航点
     * @param index 0:飞机位置 1:A控位置 2:B控位置
     */
    public void prepareToSetHomePoint(final int index) {
        if (index == 0) {
            // 设置飞机位置为返航点
            setAircraftHome();

        } else if (!widgetModel.isSupportMultiRc()) {
            //不支持双控，默认为只能设置当前遥控器为返航点。
            prepareToSetCurRcHomePoint();

        } else {
            //支持双控，需要判断
            RCMode mode = index == 1 ? RCMode.CHANNEL_A : RCMode.CHANNEL_B;
            if (widgetModel.isCurrentRc(mode)) {
                // 设置当前遥控器位置为返航点
                prepareToSetCurRcHomePoint();
            } else {
                // 设置另外一个遥控器位置为返航点
                setRcHome(widgetModel.getOtherRcLocation(), false);
            }
        }
    }

    private void setAircraftHome() {

        widgetModel.setHomeLocationUseingAircraftLoc().subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                // do something
            }

            @Override
            public void onComplete() {
                ViewUtil.showToast(getContext() , R.string.uxsdk_fpv_toast_homepoint_setting_drone , Toast.LENGTH_SHORT);
            }

            @Override
            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                ViewUtil.showToast(getContext() , R.string.uxsdk_fpv_toast_homepoint_setting_failed , Toast.LENGTH_SHORT);
            }
        });
    }


    private void prepareToSetCurRcHomePoint() {
        Location location = LocationUtil.getLastLocation();
        if (location == null) {
            RcGPSInfo gpsInfo = widgetModel.getRcGPSInfo();
            if (gpsInfo == null || !gpsInfo.getIsValid()) {
                ViewUtil.showToast(getContext() , R.string.uxsdk_fpv_toast_homepoint_gps_weak , Toast.LENGTH_SHORT);
                return;
            }
            location = GpsUtils.convertToLocation(gpsInfo);
        }
        final Location newLocation = location;


        addReaction(widgetModel.checkRcGpsValid(location.getLatitude(), location.getLongitude(), location.getAccuracy()).subscribe(distance -> {
            if (distance == -1) {
                ViewUtil.showToast(getContext() , R.string.uxsdk_fpv_toast_homepoint_gps_weak , Toast.LENGTH_SHORT);
            } else {
                //showSetHomePointDlg(true, distance, newLocation);
                setRcHome(newLocation , true);
            }
        }, throwable -> ViewUtil.showToast(getContext() , R.string.uxsdk_fpv_toast_homepoint_gps_weak , Toast.LENGTH_SHORT)) );


    }



    private void setRcHome(Location location, boolean isCurrentRc) {
        if (location == null) {
            return;
        }
        LogUtils.i("RcHome" , "isCurrentRc keep " + isCurrentRc);
        LocationCoordinate2D location2D = new LocationCoordinate2D(location.getLatitude(), location.getLongitude());
        if (!GpsUtils.isValid(location2D.getLatitude() , location2D.getLongitude())) {
            ViewUtil.showToast(getContext(),R.string.uxsdk_fpv_toast_homepoint_setting_failed , Toast.LENGTH_SHORT);
            return;
        }
        widgetModel.setHomeLocation(location2D).subscribe(new CompletableObserver() {
            @Override
            public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                //do nothing
            }

            @Override
            public void onComplete() {
                ViewUtil.showToast(getContext() , R.string.uxsdk_fpv_toast_homepoint_setting_current_rc, Toast.LENGTH_SHORT);

            }

            @Override
            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                ViewUtil.showToast(getContext(),R.string.uxsdk_fpv_toast_homepoint_setting_failed , Toast.LENGTH_SHORT);
            }
        });


    }

    private void selectHomePointView(ImageView view, @DrawableRes int iconRes) {
        if (view == null) {
            return;
        }
        view.setVisibility(View.VISIBLE);
        view.setImageResource(iconRes);
    }

    private void hideHomePointView(ImageView view) {
        if (view == null) {
            return;
        }
        view.setVisibility(View.GONE);
    }

    private void updateHomePointViewWithSingleRc() {
        if (widgetModel.isChannelA()) {
            selectHomePointView(rcAHomePointIv, R.drawable.uxsdk_ic_fpv_setting_rc_a);
            hideHomePointView(rcBHomePointIv);
        } else if (widgetModel.isChannelB()) {
            selectHomePointView(rcBHomePointIv, R.drawable.uxsdk_ic_fpv_setting_rc_b);
            hideHomePointView(rcAHomePointIv);
        }
    }

}
