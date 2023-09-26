package dji.v5.ux.core.widget.common;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import dji.v5.utils.common.LogUtils;
import dji.v5.utils.common.StringUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.widget.FrameLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.ViewUtil;

public class CommonDeviceNameWidget extends FrameLayoutWidget<Object> {

    private static final String TAG = "DeviceRenameWidget";

    protected CommonDeviceNameWidgetModel widgetModel =
            new CommonDeviceNameWidgetModel(
                    DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance());


    protected TextView saveTv;
    protected EditText renameCell;

    public CommonDeviceNameWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CommonDeviceNameWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CommonDeviceNameWidget(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_setting_menu_common_device_rename, this);

        saveTv = findViewById(R.id.setting_menu_common_save_tv);
        renameCell = findViewById(R.id.setting_menu_common_rename_cell);

        saveTv.setOnClickListener(v -> handleSaveAction());

      //  setBackgroundResource(R.drawable.uxsdk_background_black_rectangle);
    }

    @Override
    protected void reactToModelChanges() {
        addDisposable(widgetModel.getAircraftName().subscribe(name -> renameCell.setText(name)));
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

    private void handleSaveAction() {
        String deviceName = renameCell.getText().toString();
        if (deviceName.length() == 0) {
            ViewUtil.showToast(getContext(), R.string.uxsdk_setting_common_device_name_input_tip, Toast.LENGTH_SHORT);
            return;
        }
        if (StringUtils.containsEmoji(deviceName)) {
            ViewUtil.showToast(getContext(), R.string.uxsdk_setting_common_device_name_illegal, Toast.LENGTH_SHORT);
            return;
        }

        addDisposable(widgetModel.setAircraftName(deviceName).subscribe(() -> ViewUtil.showToast(getContext(), R.string.uxsdk_setting_common_device_name_save_success, Toast.LENGTH_SHORT), throwable -> {
            LogUtils.e(TAG, "handleSaveAction fail: " + throwable);
            ViewUtil.showToast(getContext(), R.string.uxsdk_setting_common_device_name_save_fail, Toast.LENGTH_SHORT);
        }));
    }
}
