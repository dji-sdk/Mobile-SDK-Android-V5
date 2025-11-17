package dji.v5.ux.gimbal

import dji.sdk.keyvalue.key.GimbalKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.gimbal.FineTunePostureMsg
import dji.sdk.keyvalue.value.gimbal.PostureFineTuneAxis
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.IGimbalIndex
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable

open class GimbalFineTuneWidgetModel constructor(
    djiSdkModel: DJISDKModel,
    uxKeyManager: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, uxKeyManager), IGimbalIndex {

    private var gimbalIndex = ComponentIndexType.LEFT_OR_MAIN

    private val rollAdjustDegreeProcessor = DataProcessor.create(0.0)
    private val yawAdjustDegreeProcessor = DataProcessor.create(0.0)
    private val pitchAdjustDegreeProcessor = DataProcessor.create(0.0)

    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                GimbalKey.KeyFineTuneRollTotalDegree, gimbalIndex), rollAdjustDegreeProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                GimbalKey.KeyFineTuneYawTotalDegree, gimbalIndex), yawAdjustDegreeProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                GimbalKey.KeyFineTunePitchTotalDegree, gimbalIndex), pitchAdjustDegreeProcessor)
    }

    override fun inCleanup() {
        //Nothing to cleanup
    }

    fun fineTunePosture(axis: PostureFineTuneAxis, value: Double): Completable {
        return djiSdkModel.performActionWithOutResult(
            KeyTools.createKey(GimbalKey.KeyFineTunePosture, gimbalIndex),
            FineTunePostureMsg(axis, value)
        )
    }

    fun rollAdjustDegree(): Flowable<Double> {
        return rollAdjustDegreeProcessor.toFlowable()
    }

    fun yawAdjustDegree(): Flowable<Double> {
        return yawAdjustDegreeProcessor.toFlowable()
    }

    fun pitchAdjustDegree(): Flowable<Double> {
        return pitchAdjustDegreeProcessor.toFlowable()
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