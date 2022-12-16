package dji.v5.ux.cameracore.widget.cameracontrols

import dji.sdk.keyvalue.key.RemoteControllerKey
import dji.v5.et.create
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/12/13
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class RemoteControllerButtonDownModel constructor(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, keyedStore) {

    val isShutterButtonDownProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    val isRecordButtonDownProcessor: DataProcessor<Boolean> = DataProcessor.create(false)

    override fun inSetup() {
        bindDataProcessor(RemoteControllerKey.KeyShutterButtonDown.create(), isShutterButtonDownProcessor)
        bindDataProcessor(RemoteControllerKey.KeyRecordButtonDown.create(), isRecordButtonDownProcessor)
    }

    override fun inCleanup() {
        //暂未实现
    }
}