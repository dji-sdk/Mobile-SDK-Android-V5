package dji.v5.ux.core.widget.common;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import dji.sdk.keyvalue.key.BatteryKey;
import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.GimbalKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.PayloadKey;
import dji.sdk.keyvalue.key.ProductKey;
import dji.sdk.keyvalue.key.RemoteControllerKey;
import dji.sdk.keyvalue.key.RtkMobileStationKey;
import dji.sdk.keyvalue.value.battery.BatteryOverviewValue;
import dji.sdk.keyvalue.value.battery.IndustryBatteryType;
import dji.sdk.keyvalue.value.camera.CameraType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.payload.PayloadCameraType;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.CameraUtil;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

public class CommonAboutWidgetModel extends WidgetModel {

    public final DataProcessor<String> rcSerialNumberProcessor = DataProcessor.create("");
    public final DataProcessor<Boolean> rtkConnectionProcessor = DataProcessor.create(false);
    public final DataProcessor<List<BatteryOverviewValue>> batteryOverviewProcessor = DataProcessor.create(new ArrayList<>());
    public final DataProcessor<Boolean> rcConnectionProcessor = DataProcessor.create(false);
    public final DataProcessor<Boolean> fcConnectionProcessor = DataProcessor.create(false);
    public final DataProcessor<Boolean> gimbal1ConnectionProcessor = DataProcessor.create(false);
    public final DataProcessor<Boolean> gimbal2ConnectionProcessor = DataProcessor.create(false);
    public final DataProcessor<Boolean> gimbal3ConnectionProcessor = DataProcessor.create(false);
    public final DataProcessor<Boolean> camera1ConnectionProcessor = DataProcessor.create(false);
    public final DataProcessor<Boolean> camera2ConnectionProcessor = DataProcessor.create(false);
    public final DataProcessor<Boolean> camera3ConnectionProcessor = DataProcessor.create(false);
    public final DataProcessor<String> camera1SerialNumberProcessor = DataProcessor.create("");
    public final DataProcessor<String> camera2SerialNumberProcessor = DataProcessor.create("");
    public final DataProcessor<String> camera3SerialNumberProcessor = DataProcessor.create("");

    protected CommonAboutWidgetModel(
            @NonNull DJISDKModel djiSdkModel,
            @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createKey(RemoteControllerKey.KeyRcRK3399SirialNumber), rcSerialNumberProcessor);
        bindDataProcessor(KeyTools.createKey(RemoteControllerKey.KeyConnection), rcConnectionProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyConnection), fcConnectionProcessor);
        bindDataProcessor(KeyTools.createKey(RtkMobileStationKey.KeyIsRTKDongleConnect), rtkConnectionProcessor);
        bindDataProcessor(KeyTools.createKey(BatteryKey.KeyBatteryOverviews, ComponentIndexType.AGGREGATION), batteryOverviewProcessor);
        bindDataProcessor(KeyTools.createKey(GimbalKey.KeyConnection, ComponentIndexType.LEFT_OR_MAIN), gimbal1ConnectionProcessor);
        bindDataProcessor(KeyTools.createKey(GimbalKey.KeyConnection, ComponentIndexType.RIGHT), gimbal2ConnectionProcessor);
        bindDataProcessor(KeyTools.createKey(GimbalKey.KeyConnection, ComponentIndexType.UP), gimbal3ConnectionProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyConnection, ComponentIndexType.LEFT_OR_MAIN), camera1ConnectionProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyConnection, ComponentIndexType.RIGHT), camera2ConnectionProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeyConnection, ComponentIndexType.UP), camera3ConnectionProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeySerialNumber, ComponentIndexType.LEFT_OR_MAIN), camera1SerialNumberProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeySerialNumber, ComponentIndexType.RIGHT), camera2SerialNumberProcessor);
        bindDataProcessor(KeyTools.createKey(CameraKey.KeySerialNumber, ComponentIndexType.UP), camera3SerialNumberProcessor);
    }

    @Override
    protected void inCleanup() {
        // do noting
    }

    public Single<String> getRCVersion() {
        return djiSdkModel.getValue(KeyTools.createKey(RemoteControllerKey.KeyFirmwareVersion))
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable doForceUpdateCache() {
        return djiSdkModel.performActionWithOutResult(KeyTools.createKey(RemoteControllerKey.KeyForceUpdateCacheValue), "FirmwareVersion")
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<String> getGimbalVersion(ComponentIndexType cameraIndex) {
        return djiSdkModel.getValue(KeyTools.createKey(GimbalKey.KeyFirmwareVersion, cameraIndex))
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<String> getCameraVersion(ComponentIndexType cameraIndex) {
        return djiSdkModel.getValue(KeyTools.createKey(CameraKey.KeyFirmwareVersion, cameraIndex))
                .observeOn(AndroidSchedulers.mainThread());
    }


    public Single<CameraType> getCameraType(ComponentIndexType cameraIndex) {
        return djiSdkModel.getValue(KeyTools.createKey(CameraKey.KeyCameraType, cameraIndex))
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<String> getPayloadCameraVersion(ComponentIndexType cameraIndex) {
        return djiSdkModel.getValue(KeyTools.createKey(PayloadKey.KeyFirmwareVersion, cameraIndex))
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<String> getPayloadCameraName(ComponentIndexType cameraIndex) {
        return djiSdkModel.getValue(KeyTools.createKey(PayloadKey.KeyPayloadCameraType, cameraIndex))
                .flatMap(type -> {
                    if (type != PayloadCameraType.UNKNOWN) {
                        return Single.just(CameraUtil.getPayloadCameraName(type));
                    }
                    return djiSdkModel.getValue(KeyTools.createKey(PayloadKey.KeyPayloadProductName, cameraIndex));
                })
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<String> getCameraName(ComponentIndexType cameraIndex) {
        return getCameraType(cameraIndex).flatMap(cameraType -> {
            if (cameraType != CameraType.PAYLOAD) {
                return Single.just(CameraUtil.getCameraDisplayName(cameraType))
                        .observeOn(AndroidSchedulers.mainThread());
            } else {
                return getPayloadCameraName(cameraIndex);
            }
        });
    }

    public Single<String> getFCSerialNumber() {
        return djiSdkModel.getValue(KeyTools.createKey(FlightControllerKey.KeySerialNumber))
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<String> getRTKSerialNumber() {
        return djiSdkModel.getValue(KeyTools.createKey(RtkMobileStationKey.KeyRTKDongleSN))
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Flowable<Boolean> getBatteryConnection(int index, Object listenerHolder) {
        return djiSdkModel.addListener(KeyTools.createKey(BatteryKey.KeyConnection, index), listenerHolder)
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<String> getBatteryVersion(int index) {
        return djiSdkModel.getValue(KeyTools.createKey(BatteryKey.KeyFirmwareVersion, index))
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<IndustryBatteryType> getIndustryBatteryType(int index) {
        return djiSdkModel.getValue(KeyTools.createKey(BatteryKey.KeyIndustryBatteryType, index))
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<String> getProductVersion() {
        return djiSdkModel.getValue(KeyTools.createKey(ProductKey.KeyFirmwareVersion))
                .observeOn(AndroidSchedulers.mainThread());
    }
}
