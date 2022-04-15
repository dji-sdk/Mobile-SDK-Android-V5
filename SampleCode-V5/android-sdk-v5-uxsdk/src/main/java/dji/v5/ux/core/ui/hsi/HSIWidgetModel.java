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

    public DataProcessor<LocationCoordinate2D> aircraftLocationDataProcessor = DataProcessor.create(new LocationCoordinate2D(Double.NaN, Double.NaN));
    public DataProcessor<LocationCoordinate2D> homeLocationDataProcessor = DataProcessor.create(new LocationCoordinate2D(Double.NaN, Double.NaN));
    public DataProcessor<AirSenseSystemInformation> airSenseSystemInformationProcessor = DataProcessor.create(new AirSenseSystemInformation());
    public DataProcessor<AirSenseWarningLevel> airSenseWarningLevelProcessor = DataProcessor.create(AirSenseWarningLevel.UNKNOWN);
    public DataProcessor<AirSenseAirplaneState[]> airSenseAirplaneStatesProcessor = DataProcessor.create(new AirSenseAirplaneState[0]);
    public DataProcessor<PerceptionPushOmnidirectionalRadarStatus> perceptionTOFDistanceProcessor = DataProcessor.create(new PerceptionPushOmnidirectionalRadarStatus());
    public DataProcessor<PerceptionPushOmnidirectionalRadarStatus> perceptionFullDistanceProcessor = DataProcessor.create(new PerceptionPushOmnidirectionalRadarStatus());
    public DataProcessor<OmnidirectionalObstacleAvoidanceStatus> obstacleAvoidanceSensorStateProcessor = DataProcessor.create(new OmnidirectionalObstacleAvoidanceStatus());
    public DataProcessor<Boolean> omniHorizontalAvoidanceEnabledProcessor = DataProcessor.create(false);
    public DataProcessor<Double> omniHorizontalRadarDistanceProcessor = DataProcessor.create(0.0);
    public DataProcessor<Double> horizontalAvoidanceDistanceProcessor = DataProcessor.create(0.0);
    public DataProcessor<Boolean> radarConnectionProcessor = DataProcessor.create(false);
    public DataProcessor<Boolean> radarHorizontalObstacleAvoidanceEnabledProcessor = DataProcessor.create(false);
    public DataProcessor<PerceptionInformation> radarObstacleAvoidanceStateProcessor = DataProcessor.create(new PerceptionInformation());
    public DataProcessor<FCFlightMode> flightModeProcessor = DataProcessor.create(FCFlightMode.UNKNOWN);
    public DataProcessor<Boolean> multipleFlightModeEnabledProcessor = DataProcessor.create(false);
    public DataProcessor<Velocity3D> velocityProcessor = DataProcessor.create(new Velocity3D());
    public DataProcessor<Attitude> aircraftAttitudeProcessor = DataProcessor.create(new Attitude());
    public List<DataProcessor<Boolean>> gimbalConnectionProcessorList = new ArrayList<>();
    private DataProcessor<Boolean> gimbalConnection0Processor = DataProcessor.create(false);
    private DataProcessor<Boolean> gimbalConnection1Processor = DataProcessor.create(false);
    private DataProcessor<Boolean> gimbalConnection2Processor = DataProcessor.create(false);
    public List<DataProcessor<Attitude>> gimbalAttitudeInDegreesProcessorList = new ArrayList<>();
    private DataProcessor<Attitude> gimbalAttitudeInDegrees0Processor = DataProcessor.create(new Attitude());
    private DataProcessor<Attitude> gimbalAttitudeInDegrees1Processor = DataProcessor.create(new Attitude());
    private DataProcessor<Attitude> gimbalAttitudeInDegrees2Processor = DataProcessor.create(new Attitude());

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
}
