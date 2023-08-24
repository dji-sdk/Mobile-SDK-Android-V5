package dji.v5.ux.obstacle

import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.error.RxError
import dji.v5.manager.aircraft.perception.PerceptionManager
import dji.v5.manager.aircraft.perception.data.PerceptionInfo
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
 *  date : 2023/8/15
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class PrecisionLandingWidgetModel(
    djiSdkModel: DJISDKModel,
    uxKeyManager: ObservableInMemoryKeyedStore,
) : WidgetModel(djiSdkModel, uxKeyManager) {
    val perceptionInfoProcessor: DataProcessor<PerceptionInfo> = DataProcessor.create(PerceptionInfo())
    private val perceptionManager = PerceptionManager.getInstance()
    private val perceptionInformationListener = PerceptionInformationListener {
        perceptionInfoProcessor.onNext(it)

    }


    override fun inSetup() {
        perceptionManager.addPerceptionInformationListener(perceptionInformationListener)
    }

    override fun inCleanup() {
        perceptionManager.removePerceptionInformationListener(perceptionInformationListener)
    }


    fun setPrecisionLandingEnabled(enable: Boolean): Completable {
        return Completable.create {
            perceptionManager.setPrecisionLandingEnabled(enable, object : CommonCallbacks.CompletionCallback {
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