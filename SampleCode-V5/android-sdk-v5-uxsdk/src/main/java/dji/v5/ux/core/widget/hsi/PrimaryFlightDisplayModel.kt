package dji.v5.ux.core.widget.hsi

import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.common.Attitude
import dji.sdk.keyvalue.value.common.Velocity3D
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
 * @date 2021/12/2
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class PrimaryFlightDisplayModel constructor(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {
    private val tag = LogUtils.getTag("PrimaryFlightDisplayModel")
    val velocityProcessor = DataProcessor.create(Velocity3D())
    val aircraftAttitudeProcessor: DataProcessor<Attitude> = DataProcessor.create(Attitude())

    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyAircraftVelocity), velocityProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyAircraftAttitude), aircraftAttitudeProcessor)
    }

    override fun inCleanup() {
//        LogUtils.d(tag,"TODO Method not implemented yet")
    }
}