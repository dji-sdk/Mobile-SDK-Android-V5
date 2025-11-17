package dji.v5.ux.flight.flightparam;

import android.location.Location;

import androidx.annotation.NonNull;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.ProductKey;
import dji.sdk.keyvalue.key.RemoteControllerKey;
import dji.sdk.keyvalue.value.common.LocationCoordinate2D;
import dji.sdk.keyvalue.value.product.ProductType;
import dji.sdk.keyvalue.value.remotecontroller.RCMode;
import dji.sdk.keyvalue.value.remotecontroller.RcGPSInfo;
import dji.v5.common.utils.GpsUtils;
import dji.v5.manager.KeyManager;
import dji.v5.utils.common.LocationUtil;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Completable;

import static dji.v5.ux.map.MapWidgetModel.INVALID_COORDINATE;

/**
 * @author feel.feng
 * @time 2023/08/11 11:20
 * @description:
 */
public class HomeSetWidgetModel extends WidgetModel {
    private final DataProcessor<ProductType> productTypeProcessor = DataProcessor.create(ProductType.UNKNOWN);

    // private final DataProcessor<Boolean> productConnectDataProcessor = DataProcessor.create(false);

    private final DataProcessor<RCMode> rcModeDataProcessor = DataProcessor.create(RCMode.UNKNOWN);
    private final DataProcessor<RcGPSInfo> rcGPSInfoDataProcessor = DataProcessor.create(new RcGPSInfo());

    public final DataProcessor<LocationCoordinate2D> homeLocationDataProcessor =
            DataProcessor.create(new LocationCoordinate2D(INVALID_COORDINATE, INVALID_COORDINATE));

    protected HomeSetWidgetModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @Override
    protected void inSetup() {
        bindDataProcessor(KeyTools.createKey(RemoteControllerKey.KeyRcMachineMode), rcModeDataProcessor);
        bindDataProcessor(KeyTools.createKey(ProductKey.KeyProductType), productTypeProcessor);
        //addDisposable(RxUtil.addListener(KeyTools.createKey(ProductKey.KeyConnection), this).subscribe(productConnectDataProcessor::onNext));

        bindDataProcessor(KeyTools.createKey(RemoteControllerKey.KeyRcGPSInfo), rcGPSInfoDataProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyHomeLocation), homeLocationDataProcessor);


    }

    @Override
    protected void inCleanup() {
        //do nothing
    }

    public RcGPSInfo getRcGPSInfo() {
        return rcGPSInfoDataProcessor.getValue();
    }

    boolean isSupportMultiRc() {
        ProductType curType = productTypeProcessor.getValue();
        return curType == ProductType.M30_SERIES || curType == ProductType.M350_RTK || curType == ProductType.M300_RTK || curType == ProductType.DJI_MATRICE_400;
    }

    boolean isCurrentRc(RCMode mode) {
        return rcModeDataProcessor.getValue() == mode;
    }

    public Completable setHomeLocationUseingAircraftLoc() {
        return djiSdkModel.performActionWithOutResult(KeyTools.createKey(FlightControllerKey.KeyHomeLocationUsingCurrentAircraftLocation));
    }

    public int checkRcGpsValid(final double latitude, final double longitude, final double accuracy) {
        if (!GpsUtils.checkLatitude(latitude)
                || !GpsUtils.checkLongitude(longitude)
                || !isFineAccuracy((float) accuracy, 60)) {
            return -1;
        }
        LocationCoordinate2D homeLocation = homeLocationDataProcessor.getValue();
        if (GpsUtils.isValid(homeLocation.getLatitude(), homeLocation.getLongitude())) {
            return (int) GpsUtils.distance(latitude, longitude, homeLocation.getLatitude(), homeLocation.getLongitude());
        } else {
            return -1;
        }
    }

    public Completable setHomeLocation(LocationCoordinate2D locationCoordinate2D) {
        return djiSdkModel.setValue(KeyTools.createKey(FlightControllerKey.KeyHomeLocation), locationCoordinate2D);
    }


    private boolean isFineAccuracy(float accuracy, float meter) {
        return 0.0F < accuracy && accuracy <= meter;
    }

    public boolean isChannelB() {
        return RCMode.CHANNEL_B == KeyManager.getInstance().getValue(KeyTools.createKey(RemoteControllerKey.KeyRcMachineMode), RCMode.UNKNOWN);
    }

    /**
     * 是否为A控，非双控机型返回false
     */
    public boolean isChannelA() {
        return RCMode.CHANNEL_A == KeyManager.getInstance().getValue(KeyTools.createKey(RemoteControllerKey.KeyRcMachineMode), RCMode.UNKNOWN);
    }

    public Location getOtherRcLocation() {
        return LocationUtil.getLastLocation();
    }
}
