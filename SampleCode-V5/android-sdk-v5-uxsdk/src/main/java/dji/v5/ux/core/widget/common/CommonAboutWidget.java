package dji.v5.ux.core.widget.common;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.sdk.keyvalue.utils.ProductUtil;
import dji.sdk.keyvalue.value.camera.CameraType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.utils.common.BytesUtil;
import dji.v5.utils.common.LogPath;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.TextCell;
import dji.v5.ux.core.base.widget.FrameLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.CameraUtil;
import dji.v5.ux.core.widget.battery.BatteryResourceUtil;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleSource;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Function;

public class CommonAboutWidget extends FrameLayoutWidget<Object> {

    public static final String NO_VALUE = "N/A";

    protected CommonAboutWidgetModel widgetModel =
            new CommonAboutWidgetModel(
                    DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance());

    protected TextCell fcVerCell;
    protected TextCell rcVerCell;
    protected LinearLayout gimbalVersionLy;
    protected LinearLayout cameraVersionLy;
    protected TextCell camera1SnCell;
    protected TextCell camera2SnCell;
    protected TextCell camera3SnCell;
    protected TextCell flycSerialCell;
    protected TextCell rcSerialCell;
    protected TextCell rtkSerialCell;
    protected LinearLayout batterVersionLayout;

    protected int textCellHeight;
    private int batteryNumber;

    protected final Map<ComponentIndexType, CameraType> cameraTypeMap = new HashMap<>();

    protected final HashMap<Integer, View> cameraViews = new HashMap<>();

    protected CompositeDisposable batteryDisposable;

    public CommonAboutWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CommonAboutWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CommonAboutWidget(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_setting_menu_about, this);

        fcVerCell = findViewById(R.id.common_menu_fc_version);
        rcVerCell = findViewById(R.id.common_menu_rc_version);
        gimbalVersionLy = findViewById(R.id.common_menu_gimbal_version_ly);
        cameraVersionLy = findViewById(R.id.common_menu_camera_version_ly);
        camera1SnCell = findViewById(R.id.common_menu_camera1_sn);
        camera2SnCell = findViewById(R.id.common_menu_camera2_sn);
        camera3SnCell = findViewById(R.id.common_menu_camera3_sn);
        flycSerialCell = findViewById(R.id.common_menu_flyc_serial);
        rcSerialCell = findViewById(R.id.common_menu_rc_serial);
        rtkSerialCell = findViewById(R.id.common_menu_rtk_serial);
        batterVersionLayout = findViewById(R.id.common_menu_battery_version_layout);

        textCellHeight = getResources().getDimensionPixelSize(R.dimen.uxsdk_58_dp);

        //  setBackgroundResource(R.drawable.uxsdk_background_black_rectangle);
    }

    @Override
    protected void reactToModelChanges() {
        addDisposable(widgetModel.rcConnectionProcessor.toFlowableOnUI().subscribe(value -> updateRCVersion()));

        addDisposable(Observable.combineLatest(
                widgetModel.gimbal1ConnectionProcessor.toObservableOnUI(),
                widgetModel.gimbal2ConnectionProcessor.toObservableOnUI(),
                widgetModel.gimbal3ConnectionProcessor.toObservableOnUI(),
                CameraUtil::getConnectionCameraList
        ).subscribe(this::updateGimbalVersion));

        addDisposable(Observable.combineLatest(
                widgetModel.camera1ConnectionProcessor.toObservableOnUI(),
                widgetModel.camera2ConnectionProcessor.toObservableOnUI(),
                widgetModel.camera3ConnectionProcessor.toObservableOnUI(),
                CameraUtil::getConnectionCameraList
        ).subscribe(this::updateCameraView));

        addDisposable(widgetModel.fcConnectionProcessor.toFlowableOnUI().subscribe(value -> {
            if (Boolean.TRUE.equals(value)) {
                addDisposable(widgetModel.getFCSerialNumber().subscribe(this::updateFlycSerialNumber, throwable -> LogUtils.e(LogPath.COMMON, throwable.getMessage())));
                addDisposable(widgetModel.getProductVersion().subscribe(this::updateProductVersion, throwable -> LogUtils.e(LogPath.COMMON, throwable.getMessage())));
            } else {
                updateFlycSerialNumber(NO_VALUE);
                updateProductVersion(NO_VALUE);
            }
        }));

        addDisposable(widgetModel.rcSerialNumberProcessor.toFlowableOnUI().subscribe(this::updateRcSerialNumber));

        addDisposable(widgetModel.rtkConnectionProcessor.toFlowableOnUI().subscribe(value -> {
            if (Boolean.TRUE.equals(value)) {
                addDisposable(widgetModel.getRTKSerialNumber().subscribe(this::updateRTKSerialNumber));
            } else {
                updateRTKSerialNumber("");
            }
        }));

        addDisposable(widgetModel.batteryOverviewProcessor.toFlowableOnUI().subscribe(overviewValues -> {
            if (batteryNumber != overviewValues.size()
                    || batterVersionLayout.getChildCount() == 0) {
                batteryNumber = overviewValues.size();
                updateBatteryView();
            }
        }));

        if (ProductUtil.isM350Product() || ProductUtil.isM400Product() || ProductUtil.isM300Product()) {
            addDisposable(Observable.combineLatest(
                    widgetModel.camera1SerialNumberProcessor.toObservableOnUI(),
                    widgetModel.camera2SerialNumberProcessor.toObservableOnUI(),
                    widgetModel.camera3SerialNumberProcessor.toObservableOnUI(),
                    (camera1, camera2, camera3) -> {
                        updateCameraSerialNumber(camera1SnCell, camera1, ComponentIndexType.LEFT_OR_MAIN);
                        updateCameraSerialNumber(camera2SnCell, camera2, ComponentIndexType.RIGHT);
                        updateCameraSerialNumber(camera3SnCell, camera3, ComponentIndexType.UP);
                        return new Object();
                    }
            ).subscribe());
        }
    }

    private void updateRCVersion() {
        addDisposable(widgetModel.doForceUpdateCache().andThen(widgetModel.getRCVersion()).subscribe(
                (version, throwable) -> {
                    if (throwable == null) {
                        rcVerCell.setContent(version);
                    } else {
                        rcVerCell.setContent(NO_VALUE);
                    }
                }));
    }

    private void updateGimbalVersion(Collection<ComponentIndexType> cameraIndexList) {
        gimbalVersionLy.removeAllViews();
        for (ComponentIndexType cameraIndex : cameraIndexList) {
            addDisposable(widgetModel.getGimbalVersion(cameraIndex)
                    .delay(cameraIndex.value() * 100L, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((version, throwable) -> {
                        for (int i = 0; i < gimbalVersionLy.getChildCount(); i++) {
                            View v = gimbalVersionLy.getChildAt(i);
                            if (v.getTag() == cameraIndex) {
                                gimbalVersionLy.removeViewAt(i);
                            }
                        }
                        TextCell gimbalTc = new TextCell(getContext());
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, textCellHeight);
                        gimbalTc.setTag(cameraIndex);
                        gimbalVersionLy.addView(gimbalTc, layoutParams);
                        gimbalTc.setContent(version);
                        if (cameraIndexList.size() > 1) {
                            gimbalTc.setTitle(getResources().getString(R.string.uxsdk_setting_menu_common_gimbal_version, String.valueOf(cameraIndex.value() + 1)));
                        } else {
                            gimbalTc.setTitle(getResources().getString(R.string.uxsdk_setting_menu_common_gimbal_version, ""));
                        }
                    }));
        }
    }

    private void updateCameraView(Collection<ComponentIndexType> cameraIndexList) {
        cameraVersionLy.removeAllViews();
        for (ComponentIndexType cameraIndex : cameraIndexList) {
            addDisposable(widgetModel.getCameraType(cameraIndex)
                    .delay(cameraIndex.value() * 100L, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSuccess(cameraType -> cameraTypeMap.put(cameraIndex, cameraType))
                    .flatMap((Function<CameraType, SingleSource<String>>) cameraType -> {
                        cameraTypeMap.put(cameraIndex, cameraType);
                        if (cameraType == CameraType.PAYLOAD) {
                            return Single.zip(widgetModel.getPayloadCameraVersion(cameraIndex), widgetModel.getPayloadCameraName(cameraIndex),
                                    (version, cameraName) -> {
                                        updateCameraVersion(cameraIndex, version, cameraName);
                                        return version;
                                    });
                        } else {
                            return Single.zip(widgetModel.getCameraVersion(cameraIndex), Single.just(CameraUtil.getCameraDisplayName(cameraType)),
                                    (version, cameraName) -> {
                                        updateCameraVersion(cameraIndex, version, cameraName);
                                        return version;
                                    });
                        }
                    })
                    .subscribe());
        }
    }

    private void updateCameraVersion(ComponentIndexType cameraIndex, String version, String cameraName) {
        TextCell[] textCells;
        //XT2 需要显示两个相机的版本号
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, textCellHeight);
        if (CameraType.ZENMUSE_XT2 == cameraTypeMap.get(cameraIndex)) {
            if (cameraViews.get(cameraIndex.value()) != null && cameraViews.get(cameraIndex.value() + 1) != null) {
                //倒着清除
                cameraVersionLy.removeView(cameraViews.get(cameraIndex.value() + 1));
                cameraVersionLy.removeView(cameraViews.get(cameraIndex.value()));
            }
            textCells = new TextCell[]{new TextCell(getContext()), new TextCell(getContext())};
            cameraViews.put(cameraIndex.value(), textCells[0]);
            cameraViews.put(cameraIndex.value() + 1, textCells[1]);
            cameraVersionLy.addView(textCells[0], layoutParams);
            cameraVersionLy.addView(textCells[1], layoutParams);
        } else {
            if (cameraViews.get(cameraIndex.value()) != null) {
                cameraVersionLy.removeView(cameraViews.get(cameraIndex.value()));
            }
            textCells = new TextCell[]{new TextCell(getContext())};
            cameraViews.put(cameraIndex.value(), textCells[0]);
            cameraVersionLy.addView(textCells[0], layoutParams);
        }
        if (textCells.length == 1) {
            textCells[0].setContent(version);
            textCells[0].setTitle(getResources().getString(R.string.uxsdk_setting_menu_common_camera_version, cameraName));
        } else {
            //XT2 通过相机1、相机2区分不同相机的版本号
            textCells[0].setContent(version);
            textCells[0].setTitle(getResources().getString(R.string.uxsdk_setting_menu_common_camera_version, cameraName) + "1");
            textCells[1].setContent(getXT2Version(version));
            textCells[1].setTitle(getResources().getString(R.string.uxsdk_setting_menu_common_camera_version, cameraName) + "2");
        }
    }

    /**
     * 获取XT2另一个相机的版本号
     *
     * @param origin 相机版本号
     */
    private String getXT2Version(String origin) {
        int dotIndex = origin.lastIndexOf('.');
        String forthNum = origin.substring(dotIndex + 1);
        String remain = origin.substring(0, dotIndex);
        dotIndex = remain.lastIndexOf('.');
        String thirdNum = remain.substring(dotIndex + 1);
        remain = remain.substring(0, dotIndex);
        int realValue = Integer.parseInt(forthNum) + 100 * Integer.parseInt(thirdNum);
        byte[] bytes = BytesUtil.getBytes(realValue & 0xFFFF);
        return remain + "." + (BytesUtil.getInt(bytes[1]) >= 10 ? "" : "0") + BytesUtil.getInt(bytes[1])
                + "." + (BytesUtil.getInt(bytes[0]) > 10 ? "" : "0") + BytesUtil.getInt(bytes[0]);
    }

    private void updateCameraSerialNumber(TextCell cell, String sn, ComponentIndexType cameraIndex) {
        if (TextUtils.isEmpty(sn)) {
            cell.setVisibility(View.GONE);
            return;
        }
        addDisposable(widgetModel.getCameraName(cameraIndex)
                .subscribe(name -> {
                    cell.setVisibility(View.VISIBLE);
                    cell.setTitle(getResources().getString(R.string.uxsdk_setting_menu_common_camera_version, name)
                            + getResources().getString(R.string.uxsdk_setting_menu_common_serial));
                    cell.setContent(sn);
                }));
    }

    public void updateProductVersion(String version) {
        fcVerCell.setContent(version);
    }

    public void updateFlycSerialNumber(String serialNum) {
        flycSerialCell.setContent(serialNum);
    }

    public void updateRcSerialNumber(String serialNum) {
        rcSerialCell.setContent(serialNum);
    }

    private void updateRTKSerialNumber(String sn) {
        if (!TextUtils.isEmpty(sn) && (ProductUtil.isM3EProduct() || ProductUtil.isM4EProduct() || ProductUtil.isM4DProduct())) {
            rtkSerialCell.setContent(sn);
            rtkSerialCell.setVisibility(View.VISIBLE);
        } else {
            rtkSerialCell.setVisibility(View.GONE);
        }
    }

    private void updateBatteryView() {
        batterVersionLayout.removeAllViews();
        if (batteryDisposable != null && !batteryDisposable.isDisposed()) {
            batteryDisposable.dispose();
        }
        batteryDisposable = new CompositeDisposable();
        for (int i = 0; i < batteryNumber; i++) {
            TextCell textCell = new TextCell(getContext());
            batterVersionLayout.addView(textCell);
            onBatteryVersionGet(i, null);
            int finalI = i;
            batteryDisposable.add(widgetModel.getBatteryConnection(i, batteryDisposable).toObservable()
                    .flatMap((Function<Boolean, ObservableSource<String>>) value -> {
                        if (Boolean.TRUE.equals(value)) {
                            return widgetModel.getBatteryVersion(finalI).toObservable();
                        } else {
                            return Observable.just("");
                        }
                    })
                    .subscribe(version -> {
                        if (!TextUtils.isEmpty(version)) {
                            onBatteryVersionGet(finalI, version);
                        } else {
                            onBatteryVersionGet(finalI, null);
                        }
                    })
            );
        }
    }

    private void onBatteryVersionGet(int index, String versionStr) {
        TextCell textCell = (TextCell) batterVersionLayout.getChildAt(index);
        if (textCell != null) {
            if (!TextUtils.isEmpty(versionStr)) {
                textCell.setContent(versionStr);
                textCell.setVisibility(VISIBLE);
            } else {
                textCell.setVisibility(GONE);
            }
            final String batteryTitle = BatteryResourceUtil.INSTANCE.getBatteryTitle(index);
            textCell.setTitle(batteryTitle);
            if (ProductUtil.isM350Product() || ProductUtil.isM400Product() || ProductUtil.isM300Product()) {
                batteryDisposable.add(widgetModel.getIndustryBatteryType(index).subscribe(type -> {
                    if (type != null) {
                        textCell.setTitle(batteryTitle + " (" + BatteryResourceUtil.INSTANCE.productName(type) + ")");
                    }
                }));
            }
        }
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
        if (batteryDisposable != null) {
            batteryDisposable.dispose();
            batteryDisposable = null;
        }
    }
}
