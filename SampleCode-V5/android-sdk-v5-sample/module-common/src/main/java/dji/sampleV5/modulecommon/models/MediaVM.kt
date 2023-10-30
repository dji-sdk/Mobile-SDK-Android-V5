package dji.sampleV5.modulecommon.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.modulecommon.data.DJIToastResult
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.key.KeyTools.createKey

import dji.sdk.keyvalue.value.camera.CameraMode
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.error.RxError
import dji.v5.common.utils.CallbackUtils
import dji.v5.common.utils.RxUtil
import dji.v5.manager.KeyManager
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.datacenter.media.*
import dji.v5.utils.common.LogUtils
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.sdk.keyvalue.value.camera.CameraStorageLocation

/**
 * @author feel.feng
 * @time 2022/04/20 2:19 下午
 * @description: 媒体回放下载数据
 */
class MediaVM : DJIViewModel() {
    var mediaFileListData = MutableLiveData<MediaFileListData>()
    var fileListState = MutableLiveData<MediaFileListState>()
    var isPlayBack = MutableLiveData<Boolean?>()
    fun init() {
        addMediaFileListStateListener()
        mediaFileListData.value = MediaDataCenter.getInstance().mediaManager.mediaFileListData
        MediaDataCenter.getInstance().mediaManager.addMediaFileListStateListener { mediaFileListState ->
            if (mediaFileListState == MediaFileListState.UP_TO_DATE) {
                val data = MediaDataCenter.getInstance().mediaManager.mediaFileListData;
                mediaFileListData.postValue(data)
            }
        }

    }

    fun destroy() {
        KeyManager.getInstance().cancelListen(this);
        removeAllFileListStateListener()

        MediaDataCenter.getInstance().mediaManager.release()
    }

    fun pullMediaFileListFromCamera(mediaFileIndex: Int, count: Int) {
        var currentTime = System.currentTimeMillis()
        MediaDataCenter.getInstance().mediaManager.pullMediaFileListFromCamera(PullMediaFileListParam.Builder().mediaFileIndex(mediaFileIndex).count(count).build(), object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                ToastUtils.showToast("Spend time:${(System.currentTimeMillis() - currentTime) / 1000}s")
                LogUtils.i(logTag, "fetch success")
            }

            override fun onFailure(error: IDJIError) {
                LogUtils.e(logTag, "fetch failed$error")
            }
        })
    }

    private fun addMediaFileListStateListener() {
        MediaDataCenter.getInstance().mediaManager.addMediaFileListStateListener(object :
            MediaFileListStateListener {
            override fun onUpdate(mediaFileListState: MediaFileListState) {
                fileListState.postValue(mediaFileListState)
            }

        })
    }

    private fun removeAllFileListStateListener() {
        MediaDataCenter.getInstance().mediaManager.removeAllMediaFileListStateListener()
    }

    fun getMediaFileList(): List<MediaFile> {
        return mediaFileListData.value?.data!!
    }

    fun setMediaFileXMPCustomInfo(info: String) {
        MediaDataCenter.getInstance().mediaManager.setMediaFileXMPCustomInfo(info, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success())
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun getMediaFileXMPCustomInfo() {
        MediaDataCenter.getInstance().mediaManager.getMediaFileXMPCustomInfo(object :
            CommonCallbacks.CompletionCallbackWithParam<String> {
            override fun onSuccess(s: String) {
                toastResult?.postValue(DJIToastResult.success(s))
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed(error.toString()))
            }
        })
    }

    fun setComponentIndex(index: ComponentIndexType) {
        KeyManager.getInstance().cancelListen(this)
        KeyManager.getInstance().listen(KeyTools.createKey(CameraKey.KeyIsPlayingBack, index), this) { _, newValue ->
            isPlayBack.postValue(newValue)
        }
        val mediaSource = MediaFileListDataSource.Builder().setIndexType(index).build()
        MediaDataCenter.getInstance().mediaManager.setMediaFileDataSource(mediaSource)
    }

    fun setStorage(location : CameraStorageLocation){
        val mediaSource = MediaFileListDataSource.Builder().setLocation(location).build()
        MediaDataCenter.getInstance().mediaManager.setMediaFileDataSource(mediaSource)
    }

    fun enable() {
        MediaDataCenter.getInstance().mediaManager.enable(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                LogUtils.e(logTag, "enable playback success")
            }

            override fun onFailure(error: IDJIError) {
                LogUtils.e(logTag, "error is ${error.description()}")
            }
        })
    }

    fun disable() {
        MediaDataCenter.getInstance().mediaManager.disable(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                LogUtils.e(logTag, "exit playback success")
            }

            override fun onFailure(error: IDJIError) {
                LogUtils.e(logTag, "error is ${error.description()}")
            }
        })
    }

    fun takePhoto(callback: CommonCallbacks.CompletionCallback) {
        RxUtil.setValue(createKey<CameraMode>(CameraKey.KeyCameraMode), CameraMode.PHOTO_NORMAL)
            .andThen(RxUtil.performActionWithOutResult(createKey(CameraKey.KeyStartShootPhoto)))
            .subscribe({ CallbackUtils.onSuccess(callback) }
            ) { throwable: Throwable ->
                CallbackUtils.onFailure(
                    callback,
                    (throwable as RxError).djiError
                )
            }
    }
}