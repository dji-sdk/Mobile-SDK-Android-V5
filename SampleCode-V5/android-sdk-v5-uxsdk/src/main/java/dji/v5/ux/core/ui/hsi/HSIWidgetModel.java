package dji.v5.ux.core.ui.hsi;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.GimbalKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.ProductKey;
import dji.sdk.keyvalue.value.common.Attitude;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.common.LocationCoordinate2D;
import dji.sdk.keyvalue.value.common.LocationCoordinate3D;
import dji.sdk.keyvalue.value.common.Velocity3D;
import dji.sdk.keyvalue.value.flightcontroller.FCFlightMode;
import dji.sdk.keyvalue.value.product.ProductType;
import dji.v5.common.utils.RxUtil;
import dji.v5.manager.aircraft.perception.PerceptionManager;
import dji.v5.manager.aircraft.perception.data.ObstacleData;
import dji.v5.manager.aircraft.perception.data.PerceptionInfo;
import dji.v5.manager.aircraft.perception.listener.ObstacleDataListener;
import dji.v5.manager.aircraft.perception.listener.PerceptionInformationListener;
import dji.v5.manager.aircraft.perception.radar.RadarInformation;
import dji.v5.manager.aircraft.perception.radar.RadarInformationListener;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import dji.v5.ux.core.util.MobileGPSLocationUtil;
import io.reactivex.rxjava3.core.Flowable;

public class HSIWidgetModel extends WidgetModel implements LocationListener {
    private final DataProcessor<LocationCoordinate3D> aircraftLocationDataProcessor = DataProcessor.create(new LocationCoordinate3D(Double.NaN,
            Double.NaN, Double.NaN));
    private final DataProcessor<LocationCoordinate2D> homeLocationDataProcessor = DataProcessor.create(new LocationCoordinate2D(Double.NaN,
            Double.NaN));
    private final DataProcessor<FCFlightMode> flightModeProcessor = DataProcessor.create(FCFlightMode.UNKNOWN);
    private final DataProcessor<Boolean> multipleFlightModeEnabledProcessor = DataProcessor.create(false);
    private final DataProcessor<Velocity3D> velocityProcessor = DataProcessor.create(new Velocity3D());
    private final DataProcessor<Attitude> aircraftAttitudeProcessor = DataProcessor.create(new Attitude());
    private final DataProcessor<ProductType> productTypeDataProcessor = DataProcessor.create(ProductType.UNKNOWN);
    private final List<DataProcessor<Boolean>> gimbalConnectionProcessorList = new ArrayList<>();
    private final DataProcessor<Boolean> gimbalConnection0Processor = DataProcessor.create(false);
    private final DataProcessor<Boolean> gimbalConnection1Processor = DataProcessor.create(false);
    private final DataProcessor<Boolean> gimbalConnection2Processor = DataProcessor.create(false);
    private final List<DataProcessor<Double>> gimbalYawInDegreesProcessorList = new ArrayList<>();
    private final DataProcessor<Double> gimbalYawInDegrees0Processor = DataProcessor.create(0.0);
    private final DataProcessor<Double> gimbalYawInDegrees1Processor = DataProcessor.create(0.0);
    private final DataProcessor<Double> gimbalYawInDegrees2Processor = DataProcessor.create(0.0);

    /**
     * 新增
     */
    // 180 / PI = 57.295779513082321
    private static final Double RAD_TO_DEG = 57.295779513082321;

    private final DataProcessor<RadarInformation> radarInformationDataProcessor = DataProcessor.create(new RadarInformation());
    private final DataProcessor<PerceptionInfo> perceptionInformationDataProcessor = DataProcessor.create(new PerceptionInfo());


    private final DataProcessor<ObstacleData> radarObstacleDataProcessor = DataProcessor.create(new ObstacleData());
    private final DataProcessor<ObstacleData> perceptionObstacleDataProcessor = DataProcessor.create(new ObstacleData());

    private RadarInformationListener radarInformationListener = radarInformation -> radarInformationDataProcessor.onNext(radarInformation);
    private PerceptionInformationListener perceptionInformationListener = perceptionInfo -> perceptionInformationDataProcessor.onNext(perceptionInfo);


    private ObstacleDataListener radarObstacleDataListener = data -> radarObstacleDataProcessor.onNext(data);
    private ObstacleDataListener perceptionObstacleDataListener = data -> perceptionObstacleDataProcessor.onNext(data);
    private final DataProcessor<Location> locationDataProcessor = DataProcessor.create(new Location("HSIWidgetModel"));

    public HSIWidgetModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @Override
    protected void inSetup() {
        // HSIMarkerLayer
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D), aircraftLocationDataProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyHomeLocation), homeLocationDataProcessor);

        /*kGimbalAttitude 获取的云台YAW 角度 和 飞机的YAW 不是一个坐标系的，需要加上 kImuCoordinateTran 的值来修正*/
        Flowable.combineLatest(RxUtil.addListener(KeyTools.createKey(GimbalKey.KeyGimbalAttitude, ComponentIndexType.LEFT_OR_MAIN), this),
                RxUtil.addListener(KeyTools.createKey(FlightControllerKey.KeyImuCoordinateTran), this),
                (attitude, aDouble) -> attitude.getYaw() + aDouble * RAD_TO_DEG).subscribe(gimbalYawInDegrees0Processor::onNext);

        Flowable.combineLatest(RxUtil.addListener(KeyTools.createKey(GimbalKey.KeyGimbalAttitude, ComponentIndexType.RIGHT), this),
                RxUtil.addListener(KeyTools.createKey(FlightControllerKey.KeyImuCoordinateTran), this),
                (attitude, aDouble) -> attitude.getYaw() + aDouble * RAD_TO_DEG).subscribe(gimbalYawInDegrees1Processor::onNext);

        Flowable.combineLatest(RxUtil.addListener(KeyTools.createKey(GimbalKey.KeyGimbalAttitude, ComponentIndexType.UP), this),
                RxUtil.addListener(KeyTools.createKey(FlightControllerKey.KeyImuCoordinateTran), this),
                (attitude, aDouble) -> attitude.getYaw() + aDouble * RAD_TO_DEG).subscribe(gimbalYawInDegrees2Processor::onNext);

        gimbalYawInDegreesProcessorList.add(gimbalYawInDegrees0Processor);
        gimbalYawInDegreesProcessorList.add(gimbalYawInDegrees1Processor);
        gimbalYawInDegreesProcessorList.add(gimbalYawInDegrees2Processor);
        MobileGPSLocationUtil.getInstance().addLocationListener(this);
        MobileGPSLocationUtil.getInstance().startUpdateLocation();

        //HSIPerceptionLayer
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyFCFlightMode), flightModeProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyMultipleFlightModeEnabled), multipleFlightModeEnabledProcessor);

        //HSIView
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyAircraftVelocity), velocityProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyAircraftAttitude), aircraftAttitudeProcessor);
        bindDataProcessor(KeyTools.createKey(ProductKey.KeyProductType), productTypeDataProcessor);

        bindDataProcessor(KeyTools.createKey(GimbalKey.KeyConnection, ComponentIndexType.LEFT_OR_MAIN), gimbalConnection0Processor);
        bindDataProcessor(KeyTools.createKey(GimbalKey.KeyConnection, ComponentIndexType.RIGHT), gimbalConnection1Processor);
        bindDataProcessor(KeyTools.createKey(GimbalKey.KeyConnection, ComponentIndexType.UP), gimbalConnection2Processor);
        gimbalConnectionProcessorList.add(gimbalConnection0Processor);
        gimbalConnectionProcessorList.add(gimbalConnection1Processor);
        gimbalConnectionProcessorList.add(gimbalConnection2Processor);

        PerceptionManager.getInstance().getRadarManager().addObstacleDataListener(radarObstacleDataListener);
        PerceptionManager.getInstance().getRadarManager().addRadarInformationListener(radarInformationListener);
        PerceptionManager.getInstance().addPerceptionInformationListener(perceptionInformationListener);
        PerceptionManager.getInstance().addObstacleDataListener(perceptionObstacleDataListener);
    }

    @Override
    protected void inCleanup() {
        gimbalConnectionProcessorList.clear();
        gimbalYawInDegreesProcessorList.clear();

        PerceptionManager.getInstance().getRadarManager().removeRadarInformationListener(radarInformationListener);
        PerceptionManager.getInstance().getRadarManager().removeObstacleDataListener(radarObstacleDataListener);
        PerceptionManager.getInstance().removePerceptionInformationListener(perceptionInformationListener);
        PerceptionManager.getInstance().removeObstacleDataListener(perceptionObstacleDataListener);
        MobileGPSLocationUtil.getInstance().removeLocationListener(this);
    }

    public DataProcessor<LocationCoordinate3D> getAircraftLocationDataProcessor() {
        return aircraftLocationDataProcessor;
    }

    public DataProcessor<LocationCoordinate2D> getHomeLocationDataProcessor() {
        return homeLocationDataProcessor;
    }


    public DataProcessor<FCFlightMode> getFlightModeProcessor() {
        return flightModeProcessor;
    }

    public DataProcessor<Boolean> getMultipleFlightModeEnabledProcessor() {
        return multipleFlightModeEnabledProcessor;
    }

    public DataProcessor<Velocity3D> getVelocityProcessor() {
        return velocityProcessor;
    }

    public DataProcessor<Attitude> getAircraftAttitudeProcessor() {
        return aircraftAttitudeProcessor;
    }

    public List<DataProcessor<Boolean>> getGimbalConnectionProcessorList() {
        return gimbalConnectionProcessorList;
    }


    public List<DataProcessor<Double>> getGimbalYawInDegreesProcessorList() {
        return gimbalYawInDegreesProcessorList;
    }


    public DataProcessor<RadarInformation> getRadarInformationDataProcessor() {
        return radarInformationDataProcessor;
    }

    public DataProcessor<ProductType> getProductTypeDataProcessor() {
        return productTypeDataProcessor;
    }

    public DataProcessor<PerceptionInfo> getPerceptionInformationDataProcessor() {
        return perceptionInformationDataProcessor;
    }

    public DataProcessor<Location> getLocationDataProcessor() {
        return locationDataProcessor;
    }

    @Override
    public void onLocationChanged(Location location) {
        locationDataProcessor.onNext(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Do nothing
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Do nothing
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Do nothing
    }

    public DataProcessor<ObstacleData> getRadarObstacleDataProcessor() {
        return radarObstacleDataProcessor;
    }

    public DataProcessor<ObstacleData> getPerceptionObstacleDataProcessor() {
        return perceptionObstacleDataProcessor;
    }
}


