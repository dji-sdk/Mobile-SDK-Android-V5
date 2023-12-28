package dji.sampleV5.aircraft.keyvalue

import android.content.Context
import android.view.View


import dji.sampleV5.aircraft.util.Util
import dji.v5.utils.common.LogUtils
import java.lang.Exception
import java.lang.StringBuilder
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
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
                } else if (isEnumList(field)) {
                    val type = field.genericType
                    if (type is ParameterizedType) {
                        val subObject: Class<out Enum<*>> =
                            type.actualTypeArguments[0] as Class<Enum<*>>
                        dataMap[clazz.canonicalName] = buildParamsSubItemListWithEnum(subObject)
                    }
                } else {
                    dataMap.clear()
                    break
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG,e.message)
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
                    KeyItemActionListener<List<String>?> {
                    override fun actionChange(t: List<String>?) {
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
            LogUtils.e(TAG,e.message)
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
     fun updatedSelectedInfo(data: List<EnumItem>, names: List<String?>) {
        for (item in data) {
            item.setSelected(false)
            for (name in names) {
                if (item.getName().equals(name)) {
                    item.setSelected(true)
                }
            }
        }
    }

    /**
     * 简单参数列表，逻辑处理
     */
    fun <E : Enum<*>?> showSimpleSubItemList(
        context: Context?,
        simpleItemList: List<EnumItem>,
        clazz: Class<E>,
        callBack: KeyItemActionListener<List<String>?>
    ) {
        val StrList = getSubItemNameList(simpleItemList)
        if (StrList.size == 0) {
            return
        }
        val selectedIndex = getSelectedIndex(simpleItemList)
        if (clazz.isEnum) {
            KeyValueDialogUtil.showSingleChoiceDialog(
                context,
                StrList,
                selectedIndex,
                object : KeyItemActionListener<List<String>?> {
                    override fun actionChange(values: List<String>?) {
                        updatedSelectedInfo(simpleItemList, values!!)
                        callBack.actionChange(values)
                    }
                })
        } else {
            KeyValueDialogUtil.showMultiChoiceDialog(
                context,
                StrList,
            ) { values ->
                updatedSelectedInfo(simpleItemList, values!!)
                callBack.actionChange(values)
            }
        }
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
                field.isAccessible = true
                val clazz = field.type
                if (setFieldPro(field , obj)){
                    continue
                }

                val subObj = clazz.newInstance()
                field[obj] = subObj
                initClassData(subObj)

            }
        } catch (e: Exception) {
            LogUtils.e(TAG, e.message)
        }
    }

     fun setFieldPro(field:Field , obj: Any?):Boolean{
         val clazz = field.type
         if (clazz.isEnum) {
             field[obj] = clazz.enumConstants!![0]
             return true;
         }

         if (Util.isWrapClass(clazz)) {
             if (clazz == Boolean::class.javaObjectType) {
                 field[obj] = false
             }
             //如果需要 可在else 中可给Integer Double 等设置初始值
             return true
         }
         if (clazz == MutableList::class.java) {
             field[obj] = ArrayList<Any>()// todo
             return true
         } else if (clazz == String::class.java) {
             field[obj] = ""
             return true
         }
         return false
     }

    private fun isEnumList(field: Field): Boolean {
        if (field.type == MutableList::class.java) {
            val type = field.genericType
            if (type is ParameterizedType) {
                val subType = type.actualTypeArguments[0]
                val clazz = subType as Class<*>
                if (clazz.isEnum) {
                    return true
                }
            }
        }
        return false
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
                    } else if (isEnumList(field)) {
                        setEnumListProperty(field , obj , subItemMap)
                    }
                }
            }
        } catch (e: Exception) {
           LogUtils.e(TAG , e.message)
        }
    }

    fun setEnumListProperty(field: Field , obj: Any? , subItemMap: Map<String?, List<EnumItem>>) {
        val type = field.genericType
        val clazz = field.type
        if (type is ParameterizedType) {
            val subObject = type.actualTypeArguments[0] as Class<Enum<*>>
            val itemList = subItemMap[clazz.canonicalName]
            val list: MutableList<Any> = ArrayList()
            val values: List<String> = getSelectedValues(itemList!!)
            for (value in values) {
                val test: Any = getEnumData(subObject, value)!!
                list.add(test)
            }
            field[obj] = list
        }
    }
    fun getSelectedValues(data: List<EnumItem>): List<String> {
        var value: String = ""
        val values: MutableList<String> = ArrayList()
        for (i in data.indices) {
            if (data[i].isSelected()) {
                value = data[i].getName()!!
                values.add(value)
                continue
            }
        }
        return values
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