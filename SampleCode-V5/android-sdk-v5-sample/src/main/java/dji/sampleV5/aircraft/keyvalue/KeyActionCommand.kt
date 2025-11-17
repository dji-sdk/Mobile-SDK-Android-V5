package dji.sampleV5.aircraft.keyvalue

/**
 * @author feel.feng
 * @time 2022/10/26 2:25 下午
 * @description:
 */
class KeyActionCommand(
    private val productType: String,
    private val componentTypeName: String,
    private val componentIndex: Int
) : KeyOperatorCommand(productType, componentTypeName, componentIndex) {

    private val TAG_GET = "【ACTION】"
    private val TAG_ERROR = "ActionErrorMsg"

    override fun filter(item: KeyItem<*, *>): Boolean {
        return item.canAction()
    }

    override fun run(item: KeyItem<*, *>) {
        super.doKeyParam(item, KeyCheckType.ACTION)
    }

    override fun getTAG(): String {
        return TAG_GET
    }

    override fun getErrorTAG(): String {
        return TAG_ERROR
    }


}