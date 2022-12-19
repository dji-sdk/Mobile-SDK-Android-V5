package dji.v5.ux.core.widget.setting

import dji.sdk.keyvalue.key.*
import dji.sdk.keyvalue.value.product.ProductType
import dji.sdk.keyvalue.value.remotecontroller.RCMode
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.payload.PayloadCenter
import dji.v5.manager.aircraft.payload.listener.PayloadBasicInfoListener
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2022/11/17
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class SettingPanelWidgetModel(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore,
) : WidgetModel(djiSdkModel, keyedStore) {

    val productTypeProcessor = DataProcessor.create(ProductType.UNKNOWN)
    val flightControllerConnectProcessor = DataProcessor.create(false)
    val rcModeProcessor = DataProcessor.create(RCMode.UNKNOWN)
    val payloadConnectedProcessor = DataProcessor.create(false)
    val payloadBasicInfoListener = PayloadBasicInfoListener {
        payloadConnectedProcessor.onNext(it.isConnected)
    }

    override fun inSetup() {
        bindDataProcessor(KeyTools.createKey(ProductKey.KeyProductType), productTypeProcessor)
        bindDataProcessor(KeyTools.createKey(FlightControllerKey.KeyConnection), flightControllerConnectProcessor)
        bindDataProcessor(KeyTools.createKey(RemoteControllerKey.KeyRcMachineMode), rcModeProcessor)
        PayloadCenter.getInstance().payloadManager.forEach {
            it.value.addPayloadBasicInfoListener { payloadBasicInfoListener }
        }
    }

    override fun inCleanup() {
        KeyManager.getInstance().cancelListen(this)
    }
}
