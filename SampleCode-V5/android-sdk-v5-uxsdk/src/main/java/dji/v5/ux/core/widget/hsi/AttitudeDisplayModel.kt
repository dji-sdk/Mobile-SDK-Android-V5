package dji.v5.ux.core.widget.hsi

import dji.sdk.keyvalue.key.*
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.sdk.keyvalue.value.common.Velocity3D
import dji.sdk.keyvalue.value.rtkmobilestation.RTKTakeoffAltitudeInfo
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.perception.PerceptionManager
import dji.v5.manager.aircraft.perception.data.ObstacleData
import dji.v5.manager.aircraft.perception.data.PerceptionInfo
import dji.v5.manager.aircraft.perception.listener.ObstacleDataListener
import dji.v5.manager.aircraft.perception.listener.PerceptionInformationListener
import dji.v5.manager.aircraft.perception.radar.RadarInformation
import dji.v5.manager.aircraft.perception.radar.RadarInformationListener
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
    keyedStore: ObservableInMemoryKeyedStore,
) : WidgetModel(djiSdkModel, keyedStore) {
    private val perceptionManager = PerceptionManager.getInstance()
    val velocityProcessor = DataProcessor.create(Velocity3D())
    val altitudeProcessor = DataProcessor.create(0.0)
    val goHomeHeightProcessor: DataProcessor<Int> = DataProcessor.create(0)
    val limitMaxFlightHeightInMeterProcessor = DataProcessor.create(0)
    val rtkTakeoffAltitudeInfoProcessor = DataProcessor.create(RTKTakeoffAltitudeInfo())

    val aircraftLocationDataProcessor = DataProcessor.create(LocationCoordinate2D(Double.NaN, Double.NaN))
    val perceptionInfoProcessor = DataProcessor.create(PerceptionInfo())
    val radarInfoProcessor = DataProcessor.create(RadarInformation())
    val perceptionObstacleDataProcessor = DataProcessor.create(ObstacleData())
    val radarObstacleDataProcessor = DataProcessor.create(ObstacleData())

    private val perceptionInformationListener = PerceptionInformationListener {
        perceptionInfoProcessor.onNext(it)
    }

    private val perceptionObstacleDataListener = ObstacleDataListener {
        perceptionObstacleDataProcessor.onNext(it)
    }

    private val radarObstacleDataListener = ObstacleDataListener {
        radarObstacleDataProcessor.onNext(it)
    }
    private val radarInformationListener = RadarInformationListener {
        radarInfoProcessor.onNext(it)
    }



    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyAircraftVelocity), velocityProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyAltitude), altitudeProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyGoHomeHeight), goHomeHeightProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyHeightLimit), limitMaxFlightHeightInMeterProcessor)

        bindDataProcessor(
            KeyTools.createKey(
                RtkMobileStationKey.KeyRTKTakeoffAltitudeInfo), rtkTakeoffAltitudeInfoProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyAircraftLocation), aircraftLocationDataProcessor)


        perceptionManager.addPerceptionInformationListener(perceptionInformationListener)
        perceptionManager.addObstacleDataListener(perceptionObstacleDataListener)
        perceptionManager.radarManager.addRadarInformationListener(radarInformationListener)
        perceptionManager.radarManager.addObstacleDataListener(radarObstacleDataListener)

    }

    override fun inCleanup() {
        perceptionManager.removePerceptionInformationListener(perceptionInformationListener)
        perceptionManager.removeObstacleDataListener(perceptionObstacleDataListener)
        perceptionManager.radarManager.removeRadarInformationListener(radarInformationListener)
        perceptionManager.radarManager.removeObstacleDataListener(radarObstacleDataListener)
        KeyManager.getInstance().cancelListen(this)

    }
}