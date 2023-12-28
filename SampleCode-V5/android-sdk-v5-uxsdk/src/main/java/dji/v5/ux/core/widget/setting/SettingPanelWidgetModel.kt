package dji.v5.ux.core.widget.setting

import dji.sdk.keyvalue.key.*
import dji.sdk.keyvalue.value.product.ProductType
import dji.sdk.keyvalue.value.remotecontroller.RCMode
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.payload.PayloadCenter
import dji.v5.manager.aircraft.payload.PayloadIndexType
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
    private val payloadConnectStatusMap = HashMap<PayloadIndexType, Boolean>()
    val productTypeProcessor = DataProcessor.create(ProductType.UNKNOWN)
    val flightControllerConnectProcessor = DataProcessor.create(false)
    val rcModeProcessor = DataProcessor.create(RCMode.UNKNOWN)
    val payloadConnectedStatusMapProcessor = DataProcessor.create(payloadConnectStatusMap)


    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                ProductKey.KeyProductType), productTypeProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyConnection), flightControllerConnectProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                RemoteControllerKey.KeyRcMachineMode), rcModeProcessor)

        PayloadCenter.getInstance().payloadManager.forEach {
            it.value.addPayloadBasicInfoListener { payloadBasicInfo ->
                run {
                    if (payloadBasicInfo.isConnected) {
                        payloadConnectStatusMap[it.key] = true
                    } else {
                        payloadConnectStatusMap.remove(it.key)
                    }
                    payloadConnectedStatusMapProcessor.onNext(payloadConnectStatusMap)

                }
            }
        }
    }

    override fun inCleanup() {
        KeyManager.getInstance().cancelListen(this)
    }

    fun getFlightControllerConnectStatus(): Boolean {
        return FlightControllerKey.KeyConnection.create().get(false)
    }

    fun getProduceType(): ProductType {
        return ProductKey.KeyProductType.create().get(ProductType.UNKNOWN)
    }
}
