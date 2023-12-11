package dji.v5.ux.core.widget.battery

import dji.sdk.keyvalue.key.BatteryKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.key.ProductKey
import dji.sdk.keyvalue.value.battery.BatteryConnectionStateMsg
import dji.sdk.keyvalue.value.battery.BatteryException
import dji.sdk.keyvalue.value.battery.BatteryOverviewValue
import dji.sdk.keyvalue.value.battery.IndustryBatteryType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.common.Date
import dji.sdk.keyvalue.value.product.ProductType
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.Flowable

class BatteryInfoWidgetModel(
    djiSdkModel: DJISDKModel, uxKeyManager: ObservableInMemoryKeyedStore
) : WidgetModel(djiSdkModel, uxKeyManager) {

    private var batteryIndex = 0
    private val batteryExceptionProcessor = DataProcessor.create(BatteryException())
    private val batteryTemperatureProcessor = DataProcessor.create(0.0)
    private val batteryVoltageProcessor = DataProcessor.create(0)
    private val batteryChargeRemainingProcessor = DataProcessor.create(0)
    private val batteryConnectionProcessor = DataProcessor.create(false)
    private val batteryIsCommunicationExceptionProcessor = DataProcessor.create(BatteryConnectionStateMsg())
    private val batteryNumberOfDischargesProcessor = DataProcessor.create(0)
    private val batteryIndustryBatteryTypeProcessor = DataProcessor.create(IndustryBatteryType.UNKNOWN)
    private val batteryBatteryHighVoltageStorageSecProcessor = DataProcessor.create(0L)
    private val batterySerialNumberProcessor = DataProcessor.create("")
    private val batteryManufacturedDateProcessor = DataProcessor.create(Date())
    private val batteryCellVoltagesProcessor = DataProcessor.create<List<Int>>(ArrayList())
    private val batteryOverviewProcessor = DataProcessor.create<List<BatteryOverviewValue>>(ArrayList())
    private val productTypeProcessor = DataProcessor.create(ProductType.UNKNOWN)

    val batteryException: Flowable<BatteryException>
        get() = batteryExceptionProcessor.toFlowableOnUI()
    val batteryTemperature: Flowable<Double>
        get() = batteryTemperatureProcessor.toFlowableOnUI()
    val batteryVoltage: Flowable<Int>
        get() = batteryVoltageProcessor.toFlowableOnUI()
    val batteryChargeRemaining: Flowable<Int>
        get() = batteryChargeRemainingProcessor.toFlowableOnUI()
    val batteryConnection: Flowable<Boolean>
        get() = batteryConnectionProcessor.toFlowableOnUI()
    val batteryIsCommunicationException: Flowable<BatteryConnectionStateMsg>
        get() = batteryIsCommunicationExceptionProcessor.toFlowableOnUI()
    val batteryNumberOfDischarges: Flowable<Int>
        get() = batteryNumberOfDischargesProcessor.toFlowableOnUI()
    val batteryIndustryBatteryType: Flowable<IndustryBatteryType>
        get() = batteryIndustryBatteryTypeProcessor.toFlowableOnUI()
    val batteryBatteryHighVoltageStorageSec: Flowable<Long>
        get() = batteryBatteryHighVoltageStorageSecProcessor.toFlowableOnUI()
    val batterySerialNumber: Flowable<String>
        get() = batterySerialNumberProcessor.toFlowableOnUI()
    val batteryManufacturedDate: Flowable<Date>
        get() = batteryManufacturedDateProcessor.toFlowableOnUI()
    val batteryCellVoltages: Flowable<List<Int>>
        get() = batteryCellVoltagesProcessor.toFlowableOnUI()
    val batteryOverview: Flowable<List<BatteryOverviewValue>>
        get() = batteryOverviewProcessor.toFlowableOnUI()
    val productType: Flowable<ProductType>
        get() = productTypeProcessor.toFlowableOnUI()

    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyBatteryException, batteryIndex), batteryExceptionProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyBatteryTemperature, batteryIndex), batteryTemperatureProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyVoltage, batteryIndex), batteryVoltageProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyChargeRemainingInPercent, batteryIndex), batteryChargeRemainingProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyConnection, batteryIndex), batteryConnectionProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyIsCommunicationException, batteryIndex), batteryIsCommunicationExceptionProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyNumberOfDischarges, batteryIndex), batteryNumberOfDischargesProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyIndustryBatteryType, batteryIndex), batteryIndustryBatteryTypeProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyBatteryHighVoltageStorageTime, batteryIndex), batteryBatteryHighVoltageStorageSecProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeySerialNumber, batteryIndex), batterySerialNumberProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyBatteryManufacturedDate, batteryIndex), batteryManufacturedDateProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyCellVoltages, batteryIndex), batteryCellVoltagesProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                BatteryKey.KeyBatteryOverviews, ComponentIndexType.AGGREGATION), batteryOverviewProcessor)
        bindDataProcessor(
            KeyTools.createKey(
                ProductKey.KeyProductType), productTypeProcessor)
    }

    override fun inCleanup() {
        // do noting
    }
    fun setBatteryIndex(batteryIndex: Int) {
        if (this.batteryIndex != batteryIndex) {
            this.batteryIndex = batteryIndex
            reset()
            restart()
        }
    }

    fun reset() {
        batteryExceptionProcessor.onNext(BatteryException())
        batteryTemperatureProcessor.onNext(0.0)
        batteryVoltageProcessor.onNext(0)
        batteryChargeRemainingProcessor.onNext(0)
        batteryConnectionProcessor.onNext(false)
        batteryIsCommunicationExceptionProcessor.onNext(BatteryConnectionStateMsg())
        batteryNumberOfDischargesProcessor.onNext(0)
        batteryIndustryBatteryTypeProcessor.onNext(IndustryBatteryType.UNKNOWN)
        batteryBatteryHighVoltageStorageSecProcessor.onNext(0L)
        batterySerialNumberProcessor.onNext("")
        batteryManufacturedDateProcessor.onNext(Date())
        batteryCellVoltagesProcessor.onNext(ArrayList())
        batteryOverviewProcessor.onNext(ArrayList())
        restart()
    }

    public override fun restart() {
        super.restart()
    }

    fun getBatteryIndex(): Int {
        return batteryIndex
    }
}