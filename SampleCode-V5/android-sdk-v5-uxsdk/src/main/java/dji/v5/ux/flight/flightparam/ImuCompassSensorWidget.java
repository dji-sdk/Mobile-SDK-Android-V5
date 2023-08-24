package dji.v5.ux.flight.flightparam;


import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.v5.ux.R;
import dji.v5.ux.accessory.DescSpinnerCell;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;

public class ImuCompassSensorWidget extends ConstraintLayoutWidget<Object> {

    LostActionWidgetModel widgetModel = new LostActionWidgetModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance());
    DescSpinnerCell spinnerCell;
    public ImuCompassSensorWidget(@NonNull Context context) {
        super(context);
    }

    public ImuCompassSensorWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ImuCompassSensorWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_setting_menu_imu_campass_sensor_layout, this);

    }

    @Override
    protected void reactToModelChanges() {
        //do something
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


}
