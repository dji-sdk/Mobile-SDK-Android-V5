package dji.sampleV5.aircraft.keyvalue

/**
 * @author feel.feng
 * @time 2022/10/26 2:25 下午
 * @description:
 */
class KeySetCommand(
    private val productType: String,
    private val componentTypeName: String,
    private val componentIndex: Int
) : KeyOperatorCommand(productType, componentTypeName, componentIndex) {

    private val TAG_GET = "【SET】"
    private val TAG_ERROR = "SetErrorMsg"
    private val INTERVAL_TIME = 3000L

    //&& ("CameraMode" ==item.toString() || "RegionMeteringArea" == item.toString())
    override fun filter(item: KeyItem<*, *>): Boolean {
        return item.canSet()
    }

    override fun run(item: KeyItem<*, *>) {
        super.doKeyParam(item, KeyCheckType.SET)
    }

    override fun getTAG(): String {
        return TAG_GET
    }

    override fun getErrorTAG(): String {
        return TAG_ERROR
    }

    override fun getIntervalTime(): Long {
        return INTERVAL_TIME
    }


}