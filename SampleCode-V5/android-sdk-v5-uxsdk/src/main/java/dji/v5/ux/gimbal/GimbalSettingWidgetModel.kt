package dji.v5.ux.gimbal

import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.GimbalKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.sdk.keyvalue.value.gimbal.GimbalCalibrationState
import dji.sdk.keyvalue.value.gimbal.GimbalCalibrationStatusInfo
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.IGimbalIndex
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.GlobalPreferenceKeys
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.communication.UXKey
import dji.v5.ux.core.communication.UXKeys
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable

open class GimbalSettingWidgetModel constructor(
    djiSdkModel: DJISDKModel,
    uxKeyManager: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, uxKeyManager), IGimbalIndex {

    private var gimbalIndex = ComponentIndexType.LEFT_OR_MAIN

    private val calibrationStatusProcessor: DataProcessor<GimbalCalibrationStatusInfo> =
        DataProcessor.create(GimbalCalibrationStatusInfo(GimbalCalibrationState.IDLE, 0))

    val gimbalAdjust : UXKey  = UXKeys.create(GlobalPreferenceKeys.GIMBAL_ADJUST_CLICKED)

    private val areMotorsOnProcessor: DataProcessor<Boolean> = DataProcessor.create(false)

    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                GimbalKey.KeyGimbalCalibrationStatus, gimbalIndex), calibrationStatusProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyAreMotorsOn), areMotorsOnProcessor)
    }

    override fun inCleanup() {
        //Nothing to cleanup
    }

    fun resetGimbal(): Completable {
        return djiSdkModel.performActionWithOutResult(
            KeyTools.createKey(GimbalKey.KeyRestoreFactorySettings, gimbalIndex),
            EmptyMsg()
        )
    }

    fun calibrateGimbal(): Completable {
        return djiSdkModel.performActionWithOutResult(
            KeyTools.createKey(GimbalKey.KeyGimbalCalibrate, gimbalIndex),
            EmptyMsg()
        )
    }

    fun calibrationStatus(): Flowable<GimbalCalibrationStatusInfo> {
        return calibrationStatusProcessor.toFlowableOnUI()
    }

    fun areMotorsOn(): Flowable<Boolean> {
        return areMotorsOnProcessor.toFlowableOnUI()
    }


    fun setGimbalClicked(){
        ObservableInMemoryKeyedStore.getInstance().setValue(gimbalAdjust , true).subscribe()
    }

    override fun getGimbalIndex(): ComponentIndexType {
        return gimbalIndex
    }

    override fun updateGimbalIndex(gimbalIndex: ComponentIndexType) {
        if (this.gimbalIndex != gimbalIndex) {
            this.gimbalIndex = gimbalIndex
            restart()
        }
    }
}