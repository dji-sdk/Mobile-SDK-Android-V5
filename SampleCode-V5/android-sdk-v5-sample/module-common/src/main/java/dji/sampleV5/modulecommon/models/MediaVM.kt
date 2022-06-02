package dji.sampleV5.modulecommon.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.datacenter.media.*
import dji.v5.utils.common.LogUtils

/**
 * @author feel.feng
 * @time 2022/04/20 2:19 下午
 * @description: 媒体回放下载数据
 */
class MediaVM : DJIViewModel() {
    val TAG: String = LogUtils.getTag(this)
    var mediaFileListData = MutableLiveData<MediaFileListData>()
    var fileListState = MutableLiveData<MediaFileListState>()
    fun init(){
        addMediaFileListStateListener()
        mediaFileListData.value = MediaManager.getInstance().mediaFileListData
        MediaManager.getInstance().addMediaFileListStateListener { mediaFileListState ->
            if (mediaFileListState == MediaFileListState.UP_TO_DATE) {
                val data = MediaManager.getInstance().mediaFileListData;
                mediaFileListData.postValue(data)
            }
        }
        pullMediaFileListFromCamera()

    }

    fun destroy(){
        removeAllFileListStateListener()
        MediaManager.getInstance().release()
    }

    fun pullMediaFileListFromCamera( ){
        var currentTime  = System.currentTimeMillis()
        MediaManager.getInstance().pullMediaFileListFromCamera(PullMediaFileListParam.Builder().build(), object :CommonCallbacks.CompletionCallback{
            override fun onSuccess() {
                ToastUtils.showToast("Spend time:${(System.currentTimeMillis() - currentTime)/1000}s")
                LogUtils.i(TAG, "fetch success")
            }
            override fun onFailure(error: IDJIError) {
                LogUtils.e(TAG, "fetch failed$error")
            }
        })
    }

    private fun addMediaFileListStateListener(){
        MediaManager.getInstance().addMediaFileListStateListener(object :MediaFileListStateListener {
            override fun onUpdate(mediaFileListState: MediaFileListState) {
                fileListState.postValue(mediaFileListState)
            }

        })
    }

    private fun removeAllFileListStateListener(){
        MediaManager.getInstance().removeAllMediaFileListStateListener()
    }
    fun getMediaFileList(): List<MediaFile> {
       return mediaFileListData.value?.data!!

    }
}