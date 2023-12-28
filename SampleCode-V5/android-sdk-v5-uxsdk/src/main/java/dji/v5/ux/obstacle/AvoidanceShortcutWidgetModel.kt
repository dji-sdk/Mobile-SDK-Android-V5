package dji.v5.ux.obstacle

import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.key.ProductKey
import dji.sdk.keyvalue.value.flightcontroller.FCFlightMode
import dji.sdk.keyvalue.value.product.ProductType
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.error.RxError
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.perception.PerceptionManager
import dji.v5.manager.aircraft.perception.data.ObstacleAvoidanceType
import dji.v5.manager.aircraft.perception.listener.PerceptionInformationListener
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.Completable

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2023/8/14
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class AvoidanceShortcutWidgetModel(
    djiSdkModel: DJISDKModel,
    uxKeyManager: ObservableInMemoryKeyedStore,
) : WidgetModel(djiSdkModel, uxKeyManager) {

     val productTypeProcessor: DataProcessor<ProductType> = DataProcessor.create(ProductType.UNKNOWN)
     val flightModeProcessor: DataProcessor<FCFlightMode> = DataProcessor.create(FCFlightMode.UNKNOWN)
     val obstacleAvoidanceTypeProcessor: DataProcessor<ObstacleAvoidanceType> = DataProcessor.create(ObstacleAvoidanceType.CLOSE)

    private val perceptionManager = PerceptionManager.getInstance()
    private val perceptionInformationListener = PerceptionInformationListener{
        obstacleAvoidanceTypeProcessor.onNext(it.obstacleAvoidanceType)
    }

    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                ProductKey.KeyProductType),productTypeProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyFCFlightMode),flightModeProcessor)
        perceptionManager.addPerceptionInformationListener(perceptionInformationListener)

    }

    override fun inCleanup() {
        perceptionManager.removePerceptionInformationListener(perceptionInformationListener)
        KeyManager.getInstance().cancelListen(this)
    }


    fun setObstacleActionType(type: ObstacleAvoidanceType): Completable {
        return Completable.create {
            perceptionManager.setObstacleAvoidanceType(type, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    it.onComplete()
                }

                override fun onFailure(error: IDJIError) {
                    it.onError(RxError(error))
                }
            })
        }

    }
}
