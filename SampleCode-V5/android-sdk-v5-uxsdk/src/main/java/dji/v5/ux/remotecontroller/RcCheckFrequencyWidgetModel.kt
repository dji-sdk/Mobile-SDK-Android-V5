package dji.v5.ux.remotecontroller

import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.key.RemoteControllerKey
import dji.sdk.keyvalue.value.remotecontroller.PairingState
import dji.v5.common.utils.RxUtil
import dji.v5.manager.KeyManager
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.Completable

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2023/8/16
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class RcCheckFrequencyWidgetModel(
    djiSdkModel: DJISDKModel,
    uxKeyManager: ObservableInMemoryKeyedStore,
) : WidgetModel(djiSdkModel, uxKeyManager) {
    val isMotorOnProcessor = DataProcessor.create(false)
    val connectionProcessor = DataProcessor.create(false)
    val pairingStateProcessor = DataProcessor.create(PairingState.UNKNOWN)


    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyAreMotorsOn), isMotorOnProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyConnection), connectionProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                RemoteControllerKey.KeyPairingStatus), pairingStateProcessor)

    }

    override fun inCleanup() {
        KeyManager.getInstance().cancelListen(this)

    }

    fun startPairing(): Completable {
        return RxUtil.performActionWithOutResult(
            KeyTools.createKey(
                RemoteControllerKey.KeyRequestPairing))
    }

    fun stopPairing(): Completable {
        return RxUtil.performActionWithOutResult(
            KeyTools.createKey(
                RemoteControllerKey.KeyStopPairing))
    }

}