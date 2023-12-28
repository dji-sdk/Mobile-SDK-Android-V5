package dji.v5.ux.warning

import dji.sdk.keyvalue.key.FlightControllerKey
import dji.v5.et.create
import dji.v5.manager.diagnostic.*
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor

class DeviceHealthAndStatusWidgetModel constructor(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore,
) : WidgetModel(djiSdkModel, keyedStore) {

    val deviceMessageProcessor: DataProcessor<ArrayList<DeviceMessage>> = DataProcessor.create(arrayListOf())
    val isConnectedProcessor: DataProcessor<Boolean> = DataProcessor.create(false)

    private val healthInfoChangeListener = DJIDeviceHealthInfoChangeListener {
        updateDeviceMessage()
    }

    private val deviceStatusChangeListener = DJIDeviceStatusChangeListener { _, _ ->
        updateDeviceMessage()
    }

    override fun inSetup() {
        DeviceHealthManager.getInstance().addDJIDeviceHealthInfoChangeListener(healthInfoChangeListener)
        DeviceStatusManager.getInstance().addDJIDeviceStatusChangeListener(deviceStatusChangeListener)
        bindDataProcessor(FlightControllerKey.KeyConnection.create(), isConnectedProcessor)
    }

    override fun inCleanup() {
        DeviceHealthManager.getInstance().removeDJIDeviceHealthInfoChangeListener(healthInfoChangeListener)
        DeviceStatusManager.getInstance().removeDJIDeviceStatusChangeListener(deviceStatusChangeListener)
    }

    private fun updateDeviceMessage() {

        val messages = ArrayList<DeviceMessage>()
        for (health: DJIDeviceHealthInfo in DeviceHealthManager.getInstance().currentDJIDeviceHealthInfos) {
            messages.add(DeviceMessage(health.title(), health.description(), health.warningLevel(), health.informationCode()))
        }
//        val status = DeviceStatusManager.getInstance().currentDJIDeviceStatus
//        messages.add(DeviceMessage(status.description(), status.description(), status.warningLevel(), status.statusCode()))
        if (messages == deviceMessageProcessor.value) {
            return
        }
        messages.sortByDescending { msg -> msg.warningLevel }

        deviceMessageProcessor.onNext(messages)
    }

    fun level3Count() = deviceMessageProcessor.value.count { it.warningLevel == WarningLevel.WARNING || it.warningLevel == WarningLevel.SERIOUS_WARNING }

    fun level2Count() = deviceMessageProcessor.value.count { it.warningLevel == WarningLevel.NOTICE || it.warningLevel == WarningLevel.CAUTION }

    data class DeviceMessage(val title: String, val description: String, val warningLevel: WarningLevel, val code: String) {

        override fun equals(other: Any?): Boolean {
            return if (other is DeviceMessage) {
                other.code == this.code
            } else false
        }

        override fun hashCode(): Int {
            var result = title.hashCode()
            result = 31 * result + description.hashCode()
            result = 31 * result + warningLevel.hashCode()
            result = 31 * result + code.hashCode()
            return result
        }

        fun validDescription(): String {
            description.ifEmpty {
                return code
            }
            return description
        }
    }
}