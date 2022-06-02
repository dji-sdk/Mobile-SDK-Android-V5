package dji.sampleV5.modulecommon.models

import androidx.lifecycle.MutableLiveData
import dji.sdk.keyvalue.value.common.IntMsg
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.datacenter.media.MediaManager
import dji.v5.manager.datacenter.media.VideoPlayStatus
import dji.v5.utils.common.LogUtils

/**
 * @author feel.feng
 * @time 2022/05/16 8:07 下午
 * @description:
 */
class VideoPlayVM : DJIViewModel(){
    val TAG: String = LogUtils.getTag(this)
    var videoPlayStatus =  MutableLiveData<VideoPlayStatus>()

    fun addVideoPlayStateListener(){
        MediaManager.getInstance().addVideoPlayStateListener {
            videoPlayStatus.postValue(it)
        }
    }


    fun removeAllListener(){

        videoPlayStatus.postValue(VideoPlayStatus.Builder().build())
        MediaManager.getInstance().removeAllVideoPlayStateListener()
    }


    fun seek(position: Int) {
        val seekTo = IntMsg()
        seekTo.value = position
        MediaManager.getInstance().seekVideo(position  , object : CommonCallbacks.CompletionCallback{
            override fun onSuccess() {
                LogUtils.i(TAG, "seekVideo video success")
            }

            override fun onFailure(error: IDJIError) {
                LogUtils.e(TAG, "seekVideo video faild$error")
            }

        })
    }

     fun pause(){
        MediaManager.getInstance().pauseVideo(object :CommonCallbacks.CompletionCallback{
            override fun onSuccess() {
                LogUtils.i(TAG, "play success")
            }

            override fun onFailure(error: IDJIError) {
                LogUtils.e(TAG, "play video faild$error")
            }

        })
    }


     fun resume(){
        MediaManager.getInstance().resumeVideo(object :CommonCallbacks.CompletionCallback{
            override fun onSuccess() {
                LogUtils.i(TAG, "resume success")
            }

            override fun onFailure(error: IDJIError) {
                LogUtils.e(TAG, "resume video faild$error")
            }

        })
    }


     fun stop(){

        MediaManager.getInstance().stopVideo(object :CommonCallbacks.CompletionCallback{
            override fun onSuccess() {
                LogUtils.i(TAG, "stop success");
            }

            override fun onFailure(error: IDJIError) {
                LogUtils.e(TAG, "stop video faild$error")
            }

        })
    }

    fun showTime(time: Int): String? {
        var minute = time / 60
        val hour = minute / 60
        val second = time % 60
        minute %= 60
        return String.format("%02d:%02d:%02d", hour, minute, second)
    }
}