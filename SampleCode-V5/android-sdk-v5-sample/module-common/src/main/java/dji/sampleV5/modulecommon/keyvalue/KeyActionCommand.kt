package dji.sampleV5.modulecommon.keyvalue

import dji.v5.common.callback.CommonCallbacks

/**
 * @author feel.feng
 * @time 2022/10/26 2:25 下午
 * @description:
 */
class KeyActionCommand(private val productType: String,
                       private val componentTypeName: String) : KeyOperatorCommand(productType , componentTypeName) {

    private val TAG_GET = "【ACTION】"
    private val TAG_ERROR = "ActionErrorMsg"

    override fun filter(item: KeyItem<*, *>): Boolean {
        return item.canAction()
    }

    override fun run(item: KeyItem<*, *>) {
       super.doKeyParam(item,KeyCheckType.ACTION)
    }

    override fun getTAG(): String {
       return TAG_GET
    }

    override fun getErrorTAG(): String {
       return TAG_ERROR
    }


}