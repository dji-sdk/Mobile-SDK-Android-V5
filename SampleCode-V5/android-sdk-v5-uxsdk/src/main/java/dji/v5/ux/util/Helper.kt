package dji.v5.ux.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.ContextCompat.startActivity
import dji.open.pilot.base.manager.file.FileTransferDestination
import dji.open.pilot.client.manager.file.FileTransferManager
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.utils.common.ContextUtil
import dji.v5.utils.common.FileUtils
import dji.v5.utils.common.LogUtils
import dji.v5.ux.util.ToastUtils
import java.io.File


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
        } catch (e: Exception) {
            LogUtils.e(TAG, "startWeb ", e.message)
        }
    }

    fun openFileChooser(path: String, context: Context?) {
        val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:$path")
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context?.startActivity(
            Intent.createChooser(intent, "MSDK")
        )
    }

    fun pushFileToRemote(path: String, isShowToast: Boolean = true) {
        FileTransferManager.getInstance().pushFileToRemote(path, object :
            CommonCallbacks.CompletionCallbackWithProgress<Double> {
            override fun onProgressUpdate(progress: Double) {
                if (isShowToast) {
                    ToastUtils.showToast("$path:$progress")
                }
            }

            override fun onSuccess() {
                if (isShowToast) {
                    ToastUtils.showToast("$path:onSuccess")
                }
            }

            override fun onFailure(error: IDJIError) {
                if (isShowToast) {
                    ToastUtils.showToast("$path:$error")
                }
            }
        })
    }

    fun pushSingleIconToRemote(res: String, isShowToast: Boolean = true){
        val resFile = File(ContextUtil.getContext().getExternalFilesDir("/"), res)
        FileUtils.copyAssetsFileIfNeed(ContextUtil.getContext(), "plugin/$res", resFile)
        pushFileToRemote(resFile.path, isShowToast)
    }

    /**
     * 推送文件到远端指定目录（例如 [FileTransferDestination.APP_LOG]）。
     */
    fun pushFileToRemoteDest(
        path: String,
        dest: FileTransferDestination,
        isShowToast: Boolean = true,
    ) {
        FileTransferManager.getInstance().pushFileToRemoteDest(path, dest, object :
            CommonCallbacks.CompletionCallbackWithProgress<Double> {
            override fun onProgressUpdate(progress: Double) {
                LogUtils.i(TAG, "pushFileToRemoteDest[$dest] progress=$progress, path=$path")
                if (isShowToast) {
                    ToastUtils.showToast("$path -> ${dest.name}: $progress")
                }
            }

            override fun onSuccess() {
                LogUtils.i(TAG, "pushFileToRemoteDest[$dest] onSuccess, path=$path")
                if (isShowToast) {
                    ToastUtils.showToast("$path -> ${dest.name}: onSuccess")
                }
            }

            override fun onFailure(error: IDJIError) {
                LogUtils.e(TAG, "pushFileToRemoteDest[$dest] onFailure, path=$path, error=$error")
                if (isShowToast) {
                    ToastUtils.showToast("$path -> ${dest.name}: $error")
                }
            }
        })
    }
}