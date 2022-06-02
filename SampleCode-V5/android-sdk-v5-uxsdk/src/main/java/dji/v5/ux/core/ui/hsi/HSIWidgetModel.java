package dji.v5.ux.core.ui.hsi;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import dji.sdk.keyvalue.key.FlightAssistantKey;
import dji.sdk.keyvalue.key.FlightControllerKey;
import dji.sdk.keyvalue.key.GimbalKey;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.key.RadarKey;
import dji.sdk.keyvalue.value.common.Attitude;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.common.LocationCoordinate2D;
import dji.sdk.keyvalue.value.common.Velocity3D;
import dji.sdk.keyvalue.value.flightassistant.OmnidirectionalObstacleAvoidanceStatus;
import dji.sdk.keyvalue.value.flightassistant.PerceptionInformation;
import dji.sdk.keyvalue.value.flightassistant.PerceptionPushOmnidirectionalRadarStatus;
import dji.sdk.keyvalue.value.flightcontroller.AirSenseAirplaneState;
import dji.sdk.keyvalue.value.flightcontroller.AirSenseSystemInformation;
import dji.sdk.keyvalue.value.flightcontroller.AirSenseWarningLevel;
import dji.sdk.keyvalue.value.flightcontroller.FCFlightMode;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;

public class HSIWidgetModel extends WidgetModel {

    private final DataProcessor<LocationCoordinate2D> aircraftLocationDataProcessor = DataProcessor.create(new LocationCoordinate2D(Double.NaN, Double.NaN));
    private final DataProcessor<LocationCoordinate2D> homeLocationDataProcessor = DataProcessor.create(new LocationCoordinate2D(Double.NaN, Double.NaN));
    private final DataProcessor<AirSenseSystemInformation> airSenseSystemInformationProcessor = DataProcessor.create(new AirSenseSystemInformation());
    private final DataProcessor<AirSenseWarningLevel> airSenseWarningLevelProcessor = DataProcessor.create(AirSenseWarningLevel.UNKNOWN);
    private final DataProcessor<AirSenseAirplaneState[]> airSenseAirplaneStatesProcessor = DataProcessor.create(new AirSenseAirplaneState[0]);
    private final DataProcessor<PerceptionPushOmnidirectionalRadarStatus> perceptionTOFDistanceProcessor = DataProcessor.create(new PerceptionPushOmnidirectionalRadarStatus());
    private final DataProcessor<PerceptionPushOmnidirectionalRadarStatus> perceptionFullDistanceProcessor = DataProcessor.create(new PerceptionPushOmnidirectionalRadarStatus());
    private final DataProcessor<OmnidirectionalObstacleAvoidanceStatus> obstacleAvoidanceSensorStateProcessor = DataProcessor.create(new OmnidirectionalObstacleAvoidanceStatus());
    private final DataProcessor<Boolean> omniHorizontalAvoidanceEnabledProcessor = DataProcessor.create(false);
    private final DataProcessor<Double> omniHorizontalRadarDistanceProcessor = DataProcessor.create(0.0);
    private final DataProcessor<Double> horizontalAvoidanceDistanceProcessor = DataProcessor.create(0.0);
    private final DataProcessor<Boolean> radarConnectionProcessor = DataProcessor.create(false);
    private final DataProcessor<Boolean> radarHorizontalObstacleAvoidanceEnabledProcessor = DataProcessor.create(false);
    private final DataProcessor<PerceptionInformation> radarObstacleAvoidanceStateProcessor = DataProcessor.create(new PerceptionInformation());
    private final DataProcessor<FCFlightMode> flightModeProcessor = DataProcessor.create(FCFlightMode.UNKNOWN);
    private final DataProcessor<Boolean> multipleFlightModeEnabledProcessor = DataProcessor.create(false);
    private final DataProcessor<Velocity3D> velocityProcessor = DataProcessor.create(new Velocity3D());
    private final DataProcessor<Attitude> aircraftAttitudeProcessor = DataProcessor.create(new Attitude());
    private final List<DataProcessor<Boolean>> gimbalConnectionProcessorList = new ArrayList<>();
    private final DataProcessor<Boolean> gimbalConnection0Processor = DataProcessor.create(false);
    private final DataProcessor<Boolean> gimbalConnection1Processor = DataProcessor.create(false);
    private final DataProcessor<Boolean> gimbalConnection2Processor = DataProcessor.create(false);
    private final List<DataProcessor<Attitude>> gimbalAttitudeInDegreesProcessorList = new ArrayList<>();
    private final DataProcessor<Attitude> gimbalAttitudeInDegrees0Processor = DataProcessor.create(new Attitude());
    private final DataProcessor<Attitude> gimbalAttitudeInDegrees1Processor = DataProcessor.create(new Attitude());
    private final DataProcessor<Attitude> gimbalAttitudeInDegrees2Processor = DataProcessor.create(new Attitude());

    public HSIWidgetModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @Override
    protected void inSetup() {
        // HSIMarkerLayer
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyAircraftLocation), aircraftLocationDataProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyHomeLocation), homeLocationDataProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyAirSenseSystemInformation), airSenseSystemInformationProcessor, airSenseSystemInformation -> {
            airSenseWarningLevelProcessor.onNext(airSenseSystemInformation.getWarningLevel());
            airSenseAirplaneStatesProcessor.onNext(airSenseSystemInformation.getAirplaneStates().toArray(new AirSenseAirplaneState[0]));
        });

        //HSIPerceptionLayer
        bindDataProcessor(KeyTools.createKey(FlightAssistantKey.KeyOmniTofDirectionalRadarStatus), perceptionTOFDistanceProcessor);
        bindDataProcessor(KeyTools.createKey(FlightAssistantKey.KeyOmnidirectionalRadarStatus), perceptionFullDistanceProcessor);
        bindDataProcessor(KeyTools.createKey(FlightAssistantKey.KeyOmnidirectionalObstacleAvoidance), obstacleAvoidanceSensorStateProcessor);
        bindDataProcessor(KeyTools.createKey(FlightAssistantKey.KeyOmniHorizontalObstacleAvoidanceEnabled), omniHorizontalAvoidanceEnabledProcessor);
        bindDataProcessor(KeyTools.createKey(FlightAssistantKey.KeyOmniHorizontalRadarDistance), omniHorizontalRadarDistanceProcessor);
        bindDataProcessor(KeyTools.createKey(FlightAssistantKey.KeyHorizontalAvoidanceDistance), horizontalAvoidanceDistanceProcessor);
        bindDataProcessor(KeyTools.createKey(RadarKey.KeyConnection), radarConnectionProcessor);
        bindDataProcessor(KeyTools.createKey(FlightAssistantKey.KeyRadarHorizontalObstacleAvoidanceEnabled), radarHorizontalObstacleAvoidanceEnabledProcessor);
        bindDataProcessor(KeyTools.createKey(RadarKey.KeyRadarObstacleAvoidanceState), radarObstacleAvoidanceStateProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyFCFlightMode), flightModeProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyMultipleFlightModeEnabled), multipleFlightModeEnabledProcessor);

        //HSIView
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyAircraftVelocity), velocityProcessor);
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyAircraftAttitude), aircraftAttitudeProcessor);

        bindDataProcessor(KeyTools.createKey(GimbalKey.KeyConnection, ComponentIndexType.LEFT_OR_MAIN), gimbalConnection0Processor);
        bindDataProcessor(KeyTools.createKey(GimbalKey.KeyConnection, ComponentIndexType.RIGHT), gimbalConnection1Processor);
        bindDataProcessor(KeyTools.createKey(GimbalKey.KeyConnection, ComponentIndexType.UP), gimbalConnection2Processor);
        gimbalConnectionProcessorList.add(gimbalConnection0Processor);
        gimbalConnectionProcessorList.add(gimbalConnection1Processor);
        gimbalConnectionProcessorList.add(gimbalConnection2Processor);

        bindDataProcessor(KeyTools.createKey(GimbalKey.KeyGimbalAttitude, ComponentIndexType.LEFT_OR_MAIN), gimbalAttitudeInDegrees0Processor);
        bindDataProcessor(KeyTools.createKey(GimbalKey.KeyGimbalAttitude, ComponentIndexType.RIGHT), gimbalAttitudeInDegrees1Processor);
        bindDataProcessor(KeyTools.createKey(GimbalKey.KeyGimbalAttitude, ComponentIndexType.UP), gimbalAttitudeInDegrees2Processor);
        gimbalAttitudeInDegreesProcessorList.add(gimbalAttitudeInDegrees0Processor);
        gimbalAttitudeInDegreesProcessorList.add(gimbalAttitudeInDegrees1Processor);
        gimbalAttitudeInDegreesProcessorList.add(gimbalAttitudeInDegrees2Processor);
    }

    @Override
    protected void inCleanup() {
        gimbalConnectionProcessorList.clear();
        gimbalAttitudeInDegreesProcessorList.clear();
    }

    public DataProcessor<LocationCoordinate2D> getAircraftLocationDataProcessor() {
        return aircraftLocationDataProcessor;
    }

    public DataProcessor<LocationCoordinate2D> getHomeLocationDataProcessor() {
        return homeLocationDataProcessor;
    }

    public DataProcessor<AirSenseSystemInformation> getAirSenseSystemInformationProcessor() {
        return airSenseSystemInformationProcessor;
    }

    public DataProcessor<AirSenseWarningLevel> getAirSenseWarningLevelProcessor() {
        return airSenseWarningLevelProcessor;
    }

    public DataProcessor<AirSenseAirplaneState[]> getAirSenseAirplaneStatesProcessor() {
        return airSenseAirplaneStatesProcessor;
    }

    public DataProcessor<PerceptionPushOmnidirectionalRadarStatus> getPerceptionTOFDistanceProcessor() {
        return perceptionTOFDistanceProcessor;
    }

    public DataProcessor<PerceptionPushOmnidirectionalRadarStatus> getPerceptionFullDistanceProcessor() {
        return perceptionFullDistanceProcessor;
    }

    public DataProcessor<OmnidirectionalObstacleAvoidanceStatus> getObstacleAvoidanceSensorStateProcessor() {
        return obstacleAvoidanceSensorStateProcessor;
    }

    public DataProcessor<Boolean> getOmniHorizontalAvoidanceEnabledProcessor() {
        return omniHorizontalAvoidanceEnabledProcessor;
    }

    public DataProcessor<Double> getOmniHorizontalRadarDistanceProcessor() {
        return omniHorizontalRadarDistanceProcessor;
    }

    public DataProcessor<Double> getHorizontalAvoidanceDistanceProcessor() {
        return horizontalAvoidanceDistanceProcessor;
    }

    public DataProcessor<Boolean> getRadarConnectionProcessor() {
        return radarConnectionProcessor;
    }

    public DataProcessor<Boolean> getRadarHorizontalObstacleAvoidanceEnabledProcessor() {
        return radarHorizontalObstacleAvoidanceEnabledProcessor;
    }

    public DataProcessor<PerceptionInformation> getRadarObstacleAvoidanceStateProcessor() {
        return radarObstacleAvoidanceStateProcessor;
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

    public DataProcessor<Boolean> getGimbalConnection0Processor() {
        return gimbalConnection0Processor;
    }

    public DataProcessor<Boolean> getGimbalConnection1Processor() {
        return gimbalConnection1Processor;
    }

    public DataProcessor<Boolean> getGimbalConnection2Processor() {
        return gimbalConnection2Processor;
    }

    public List<DataProcessor<Attitude>> getGimbalAttitudeInDegreesProcessorList() {
        return gimbalAttitudeInDegreesProcessorList;
    }

    public DataProcessor<Attitude> getGimbalAttitudeInDegrees0Processor() {
        return gimbalAttitudeInDegrees0Processor;
    }

    public DataProcessor<Attitude> getGimbalAttitudeInDegrees1Processor() {
        return gimbalAttitudeInDegrees1Processor;
    }

    public DataProcessor<Attitude> getGimbalAttitudeInDegrees2Processor() {
        return gimbalAttitudeInDegrees2Processor;
    }
}
