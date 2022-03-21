package dji.sampleV5.modulecommon.keyvalue

import android.content.Context
import android.view.View


import dji.sampleV5.modulecommon.util.Util
import dji.v5.utils.common.LogUtils
import java.lang.Exception
import java.lang.StringBuilder
import java.util.*

/**
 * @author feel.feng
 * @time 2022/03/10 11:23 上午
 * @description:
 */
object KeyItemHelper {

    val TAG = LogUtils.getTag(this)
    val  LISTEN_RECORD_MAX_LENGTH = 2000
    val  FILED_CHANGE = "\$change"
    /**
     * 通过反射给字段赋值
     *
     * @param obj
     */
    fun initSubItemData(obj: Any): Map<String?, List<EnumItem>> {
        val dataMap: MutableMap<String?, List<EnumItem>> = HashMap()
        try {
            val fields = obj.javaClass.declaredFields
            for (field in fields) {
                if (field.name == FILED_CHANGE || field.name == "serialVersionUID") {
                    continue
                }
                val clazz = field.type
                if (clazz.isEnum) {
                    dataMap[clazz.canonicalName] =
                        buildParamsSubItemListWithEnum(field.type as Class<Enum<*>>)
                } else {
                    dataMap.clear()
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return dataMap
    }

    /**
     * 处理界面参数选择和设置
     *
     * @param anchor
     * @param dataMap
     */
    fun <P> processSubListLogic(
        anchor: View,
        param: P,
        dataMap: Map<String?, List<EnumItem>>,
        callBack: KeyItemActionListener<String?>
    ) {
        try {
            val nameList = Util.getMapKeyList(dataMap)
            val subItemList = Util.getMapValueList(dataMap)
            if (dataMap.size == 1) {
                //简单列表
                val list = subItemList[0]
                val clazz = Class.forName(nameList[0]!!) as Class<Enum<*>>
                showSimpleSubItemList(anchor.context, list, clazz, object :
                    KeyItemActionListener<String?> {
                    override fun actionChange(t: String?) {
                        updateClassData(param, dataMap)
                        callBack.actionChange(param.toString())
                    }
                })
            } else {
                //复合列表
                KeyValueDialogUtil.showListConfirmWindow(
                    anchor,
                    getSimpleNameList(nameList),
                    "select item for setting",
                    object :
                        KeyItemActionListener<String?> {
                        override fun actionChange(msg: String?) {
                            if ("confirm" == msg) {
                                updateClassData(param, dataMap)
                                callBack.actionChange(param.toString())
                            } else {
                                val clazz = getClassWithName(msg, nameList) as Class<Enum<*>>?
                                val list = dataMap[clazz!!.canonicalName]!!
                                showSimpleSubItemList(
                                    anchor.context,
                                    list,
                                    clazz
                                ) { updateClassData(param, dataMap) }
                            }
                        }
                    })
            }
        } catch (e: ClassNotFoundException) {
            LogUtils.e(TAG , e.message)
        }
    }

    /**
     * 类名列表获取：通过全名list获取简名list
     *
     * @param nameList
     * @return
     */
    fun getSimpleNameList(nameList: List<String?>): List<String> {
        val simpleNameList: MutableList<String> = ArrayList()
        for (str in nameList) {
            simpleNameList.add(str!!.substring(str!!.lastIndexOf(".") + 1))
        }
        return simpleNameList
    }

    /**
     * 通过简名获取类字节码
     *
     * @param simpleName
     * @param nameList
     * @return
     */
    fun getClassWithName(simpleName: String?, nameList: List<String?>): Class<*>? {
        var clazz: Class<*>? = null
        try {
            for (str in nameList) {
                if (str!!.endsWith(simpleName!!)) {
                    clazz = Class.forName(str!!)
                    break
                }
            }
        } catch (e: ClassNotFoundException) {
            LogUtils.e(TAG , e.message)
        }
        return clazz
    }

    /**
     * 构建参数条目的选项子列表
     *
     * @param <E>
     * @return
    </E> */
    fun <E : Enum<*>?> buildParamsSubItemListWithEnum(clazz: Class<E>): List<EnumItem> {
        val list: MutableList<EnumItem> = ArrayList()
        try {
            var item: EnumItem
            val objs: Array<out E>? = clazz.getEnumConstants()
            for (obj in objs!!) {
                item = EnumItem()
                item.setName(obj.toString())
                list.add(item)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /**
     * 获取属性子列表数据
     *
     * @param data
     * @return
     */
    fun getSubItemNameList(data: List<EnumItem>): List<String> {
        val result: MutableList<String> = ArrayList()
        for (item in data) {
            result.add(item.getName().toString())
        }
        return result
    }

    /**
     * 获取选中属性的顺序值
     *
     * @param data
     * @return
     */
    fun getSelectedIndex(data: List<EnumItem>): Int {
        var index = 0
        for (i in data.indices) {
            if (data[i].isSelected()) {
                index = i
                break
            }
        }
        return index
    }

    /**
     * 获取选中属性的value
     *
     * @param data
     * @return
     */
    fun getSelectedValue(data: List<EnumItem>): String {
        var value = ""
        for (i in data.indices) {
            if (data[i].isSelected()) {
                value = data[i].getName().toString()
                break
            }
        }
        return value
    }

    /**
     * 更新选择状态
     *
     * @param data
     * @param name
     */
    fun updatedSelectedInfo(data: List<EnumItem>, name: String) {
        for (item in data) {
            if (item.getName() == name) {
                item.setSelected(true)
            } else {
                item.setSelected(false)
            }
        }
    }

    /**
     * 简单参数列表，逻辑处理
     */
    fun <E : Enum<*>?> showSimpleSubItemList(
        context: Context?,
        simpleItemList: List<EnumItem>,
        clazz: Class<E>?,
        callBack: KeyItemActionListener<String?>
    ) {
        val StrList = getSubItemNameList(simpleItemList)
        if (StrList.size == 0) {
            return
        }
        val selectedIndex = getSelectedIndex(simpleItemList)
        KeyValueDialogUtil.showSingleChoiceDialog(
            context,
            StrList,
            selectedIndex,
            object :
                KeyItemActionListener<String?> {
                override fun actionChange(t: String?) {
                    if (t != null) {
                        updatedSelectedInfo(simpleItemList, t)
                    }
                    callBack.actionChange(t)
                }
            })
    }

    /**
     * 通过反射给字段赋值
     *
     * @param obj
     */
    fun initClassData(obj: Any?) {
        if (obj == null || Util.isBlank(obj.toString())) {
            return
        }
        try {
            val pFields = obj.javaClass.declaredFields
            for (field in pFields) {
                if (field.name == FILED_CHANGE || field.name == "serialVersionUID") {
                    continue
                }
                val clazz = field.type
                if (clazz.isEnum) {
                    field[obj] = clazz.enumConstants!![0]
                    return;
                }

                if (Util.isWrapClass(clazz)) {
                    if (clazz == Boolean::class.java) {
                        field[obj] = false
                    } else {
                        field[obj] = 0
                    }
                    return
                }
                if (clazz == MutableList::class.java) {
                    field[obj] = ArrayList<Any>()
                    return
                }

                val subObj = clazz.newInstance()
                field[obj] = subObj
                initClassData(subObj)

            }
        } catch (e: Exception) {
            LogUtils.e(TAG, e.message)
        }
    }

    /**
     * 通过反射给字段赋值
     *
     * @param obj
     */
    fun updateClassData(obj: Any?, subItemMap: Map<String?, List<EnumItem>>) {
        try {
            val pFields = obj?.javaClass?.declaredFields
            if (pFields != null) {
                for (field in pFields) {
                    if (field.name == FILED_CHANGE || field.name == "serialVersionUID") {
                        continue
                    }
                    field.isAccessible = true
                    val clazz = field.type
                    if (clazz.isEnum) {
                        val itemList = subItemMap[clazz.canonicalName]!!
                        field[obj] = getEnumData(clazz as Class<Enum<*>>, getSelectedValue(itemList))
                    }
                }
            }
        } catch (e: Exception) {
           LogUtils.e(TAG , e.message)
        }
    }

    /**
     * 追加携带日期的推送字符串
     *
     * @param targetStr
     * @param appendStr
     * @return
     */
    fun appendListenRecord(targetStr: String, appendStr: String?): String {
        if (Util.isBlank(appendStr)) {
            return targetStr
        }
        val sb = StringBuilder(targetStr)
        sb.append("\n")
        sb.append(Util.getDateStr(Date()) + ":")
        sb.append("\n")
        sb.append(appendStr)
        //长度限制
        var result = sb.toString()
        if (result.length > LISTEN_RECORD_MAX_LENGTH) {
            result = result.substring(result.length - LISTEN_RECORD_MAX_LENGTH)
        }
        val title = "push info:"
        if (!result.startsWith(title)) {
            result = """
                $title
                $result
                """.trimIndent()
        }
        return result
    }

    /**
     * 通过字节码和值来获取实例
     *
     * @param clazz
     * @param value
     * @param <E>
     * @return
    </E> */
    fun <E : Enum<*>?> getEnumData(clazz: Class<E>, value: String): E? {
        var data: E? = null
        try {
            val objs: Array<out E>? = clazz.getEnumConstants()
            for (obj in objs!!) {
                if (obj.toString() == value) {
                    data = obj as E
                    break
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG , e.message)
        }
        return data
    }
}