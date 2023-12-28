package dji.sampleV5.aircraft.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat.startActivity
import dji.v5.utils.common.LogUtils


object Helper {

    val TAG = LogUtils.getTag("Helper")

    fun makeList(o: Array<*>): ArrayList<String> {
        val list = ArrayList<String>()
        val iterator = o.iterator()
        while (iterator.hasNext()) {
            list.add(iterator.next().toString())
        }
        return list
    }

    fun makeList(o: IntArray): ArrayList<String> {
        val list = ArrayList<String>()
        for (i in o.indices) {
            list.add(Integer.valueOf(o[i]).toString())
        }
        return list
    }

    fun makeList(o: DoubleArray): ArrayList<String> {
        val list = ArrayList<String>()
        for (i in o.indices) {
            list.add(java.lang.Double.valueOf(o[i]).toString())
        }
        return list
    }

    fun makeList(o: LongArray): ArrayList<String> {
        val list = ArrayList<String>()
        for (i in o.indices) {
            list.add(java.lang.Long.valueOf(o[i]).toString())
        }
        return list
    }

    fun makeList(o: List<*>): ArrayList<String> {
        val list = ArrayList<String>()
        val iterator = o.iterator()
        while (iterator.hasNext()) {
            list.add(iterator.next().toString())
        }
        return list
    }

    fun startBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addCategory(Intent.CATEGORY_BROWSABLE)   //有些机型上会出现浏览器拉不起来的情况，例如华为P20
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // 解决使用applicationContext调用startActivity出现的异常
            startActivity(context, intent, null)
        }catch (e :Exception){
            LogUtils.e(TAG,"startWeb ",e.message)
        }
    }
}