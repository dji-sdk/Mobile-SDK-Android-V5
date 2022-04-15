package dji.v5.ux.core.widget.hsi

import dji.sdk.keyvalue.key.FlightAssistantKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.RadarKey
import dji.sdk.keyvalue.key.RtkMobileStationKey
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.sdk.keyvalue.value.common.Velocity3D
import dji.sdk.keyvalue.value.flightassistant.PerceptionInformation
import dji.sdk.keyvalue.value.flightassistant.PerceptionPushOmnidirectionalRadarStatus
import dji.sdk.keyvalue.value.rtkmobilestation.RTKTakeoffAltitudeInfo
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.utils.common.LogUtils
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/11/26
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class AttitudeDisplayModel constructor(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {
    private val tag = LogUtils.getTag("AttitudeDisplayModel")
    val omniPerceptionRadarUpDistanceProcessor = DataProcessor.create(PerceptionPushOmnidirectionalRadarStatus())
    val omniPerceptionRadarDownDistanceProcessor = DataProcessor.create(PerceptionPushOmnidirectionalRadarStatus())
    val upwardsAvoidanceDistanceProcessor = DataProcessor.create(0.0)
    val downwardsAvoidanceDistanceProcessor = DataProcessor.create(0)
    val omniUpRadarDistanceProcessor = DataProcessor.create(0.0)
    val omniDownRadarDistanceProcessor = DataProcessor.create(0.0)
    val omniUpwardsObstacleAvoidanceEnabledProcessor = DataProcessor.create(false)
    val velocityProcessor = DataProcessor.create(Velocity3D())
    val altitudeProcessor = DataProcessor.create(0.0)
    val goHomeHeightProcessor: DataProcessor<Int> = DataProcessor.create(0)
    val limitMaxFlightHeightInMeterProcessor = DataProcessor.create(0)
    val landingProtectionEnabledProcessor = DataProcessor.create(false)
    val radarUpwardsObstacleAvoidanceEnabledProcessor = DataProcessor.create(false)
    val radarObstacleAvoidanceStateProcessor = DataProcessor.create(PerceptionInformation())
    val rtkTakeoffAltitudeInfoProcessor = DataProcessor.create(RTKTakeoffAltitudeInfo())
    val aircraftLocationDataProcessor = DataProcessor.create(LocationCoordinate2D(Double.NaN, Double.NaN))

    override fun inSetup() {
        bindDataProcessor(KeyTools.createKey(FlightAssistantKey.KeyOmniPerceptionRadarUpDistance), omniPerceptionRadarUpDistanceProcessor)
        bindDataProcessor(KeyTools.createKey(FlightAssistantKey.KeyOmniPerceptionRadarDownDistance), omniPerceptionRadarUpDistanceProcessor)
        bindDataProcessor(KeyTools.createKey(FlightAssistantKey.KeyUpwardsAvoidanceDistance), upwardsAvoidanceDistanceProcessor)
        bindDataProcessor(KeyTools.createKey(FlightAssistantKey.KeyDownwardsAvoidanceDistance), downwardsAvoidanceDistanceProcessor)
        bindDataProcessor(KeyTools.createKey(FlightAssistantKey.KeyOmniUpRadarDistance), omniUpRadarDistanceProcessor)
        bindDataProcessor(KeyTools.createKey(FlightAssistantKey.KeyOmniDownRadarDistance), omniDownRadarDistanceProcessor)
        bindDataProcessor(KeyTools.createKey(FlightAssistantKey.KeyOmniUpwardsObstacleAvoidanceEnabled), omniUpwardsObstacleAvoidanceEnabledProcessor)
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyAircraftVelocity), velocityProcessor)
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyAltitude), altitudeProcessor)
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyGoHomeHeight), goHomeHeightProcessor)
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyHeightLimit), limitMaxFlightHeightInMeterProcessor)
        bindDataProcessor(KeyTools.createKey(FlightAssistantKey.KeyLandingProtectionEnabled), landingProtectionEnabledProcessor)
        bindDataProcessor(KeyTools.createKey(FlightAssistantKey.KeyRadarUpwardsObstacleAvoidanceEnabled), radarUpwardsObstacleAvoidanceEnabledProcessor)
        bindDataProcessor(KeyTools.createKey(RadarKey.KeyRadarObstacleAvoidanceState), radarObstacleAvoidanceStateProcessor)
        bindDataProcessor(KeyTools.createKey(RtkMobileStationKey.KeyRTKTakeoffAltitudeInfo), rtkTakeoffAltitudeInfoProcessor)
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyAircraftLocation), aircraftLocationDataProcessor)
    }

    override fun inCleanup() {
//        LogUtils.d(tag,"TODO Method not implemented yet")
    }
}