package dji.sampleV5.aircraft.keyvalue


import dji.sdk.keyvalue.key.ComponentType
import dji.v5.manager.capability.CapabilityManager
import dji.v5.manager.capability.CapabilityParser
import dji.v5.utils.common.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableObserver
import io.reactivex.rxjava3.disposables.Disposable
import java.lang.Exception
import java.lang.StringBuilder
import dji.sampleV5.aircraft.util.ToastUtils

/**
 * @author feel.feng
 * @time 2022/09/13 5:43 下午
 * @description: 能力集key 测试,结果中只展示未通过的key
 * 结果存储在包目录/keycheck/result.txt
 */
object CapabilityKeyChecker {


    private val TAG = LogUtils.getTag(this)


    /**
     * 根据key名称从能力集获取测试用例json集合 每个对象可反序列化为key的参数对象
     */
    fun getKeyParamList(keyName: String): MutableList<String> {
        return CapabilityParser.getInstance().getValueParamList(keyName)
    }


    fun getKeyItem(keyName: String): KeyItem<*, *>? {
        val allList: MutableList<KeyItem<*, *>> = ArrayList()
        var item: KeyItem<*, *>? = null
        KeyItemDataUtil.getAllKeyList(allList)
        allList.forEach {
            if (it.toString() == keyName) {
                item = it;
            }
        }

        return item
    }

    data class ItemDecoder(
        var componetIndex: Int = 0, // LEFT_OR_MAIN
        var subComponetType: Int = 65534, // DEFAULT
        var subComponetIndex: Int = 0,
        var jsonString: String
    )

    /**
     *  获取枚举对应的Json list字符串
     *  eg:CameraMode 对象 obj cameramodeMsg  field[obj] CameraMode
     */
    fun getDJIValueBeanStr(item: KeyItem<*, *>): String {
        var tagBegin = "{\"valueParamList\": ["
        var tagEnd = "]}"
        try {
            val pFields = item.param?.javaClass?.declaredFields
            if (pFields != null) {
                for (field in pFields) {

                    field.isAccessible = true
                    val clazz = field.type
                    if (clazz.isEnum) {
                        val itemList =
                            (item.subItemMap as Map<String?, List<EnumItem>>)[clazz.canonicalName]!!
                        var jsonList = StringBuilder(tagBegin)
                        itemList.forEach {
                            field[item.param] = KeyItemHelper.getEnumData(
                                clazz as Class<Enum<*>>,
                                it.getName().toString()
                            )
                            var jsonString =
                                "\"" + item.param.toString().replace("\"", "\\\"") + "\""

                            var result = jsonString + ","
                            if (!result.contains("65535")) {//过滤unknown
                                jsonList.append(result)
                            }
                        }
                        jsonList.deleteAt(jsonList.lastIndex)
                        jsonList.append(tagEnd)
                        return jsonList.toString()

                    }
                }
            }
        } catch (e: Exception) {
            LogUtils.e(KeyItemHelper.TAG, e.message)
        }
        return ""
    }

    /**
     * 根据item生成 枚举类型的json文件
     */
    fun generateAllEnumList(productType: String) {
        val allList: MutableList<KeyItem<*, *>> = ArrayList()
        DJIExecutor.getExecutorFor(DJIExecutor.Purpose.IO).execute {
            KeyItemDataUtil.getAllKeyList(allList)
            allList
                .filter {
                    val keyName = "Key$it"
                    it.canSet()
                            && CapabilityManager.getInstance().isKeySupported(
                        productType,
                        "",
                        ComponentType.find(it.getKeyInfo().componentType),
                        keyName
                    )

                }
                .forEach() { item ->
                    val jsonStr = getDJIValueBeanStr(item)
                    if (jsonStr.isNotEmpty()) {
                        var filePath = DiskUtil.getExternalCacheDirPath(
                            ContextUtil.getContext(),
                            "keycheck/$item.json"
                        )
                        FileUtils.writeFile(filePath, jsonStr, false)
                    }
                }
        }
    }

    private fun checkOneType(
        productType: String,
        componentTypeName: String,
        keyCheckType: KeyCheckType,
        componentIndex: Int
    ): Completable {

        var keyOperatorCommand = when (keyCheckType) {
            KeyCheckType.SET -> KeySetCommand(productType, componentTypeName, componentIndex)
            KeyCheckType.ACTION -> KeyActionCommand(productType, componentTypeName, componentIndex)
            KeyCheckType.GET -> KeyGetCommand(productType, componentTypeName, componentIndex)
        }
        return keyOperatorCommand.execute()
    }

    fun check(
        productType: String,
        componentTypeName: String,
        componentIndex: Int
    ) {
        checkOneType(productType, componentTypeName, KeyCheckType.SET, componentIndex)
            .andThen(checkOneType(productType, componentTypeName, KeyCheckType.SET, componentIndex))
            .andThen(checkOneType(productType, componentTypeName, KeyCheckType.ACTION, componentIndex))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : CompletableObserver {
                override fun onSubscribe(d: Disposable) {
                    LogUtils.e(TAG, "begin check")
                    ToastUtils.showToast("begin check")
                }

                override fun onComplete() {
                    LogUtils.e(TAG, "-check finish-")
                }

                override fun onError(e: Throwable) {
                    LogUtils.e(TAG, "check error${e.message}")
                }

            })

    }
}