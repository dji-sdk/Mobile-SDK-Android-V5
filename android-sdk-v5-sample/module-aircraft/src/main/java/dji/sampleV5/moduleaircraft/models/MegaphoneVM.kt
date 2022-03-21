package dji.sampleV5.moduleaircraft.models

import android.os.Handler
import android.os.HandlerThread
import androidx.lifecycle.MutableLiveData
import dji.sampleV5.modulecommon.models.DJIViewModel
import dji.sdk.keyvalue.key.DJIKey
import dji.sdk.keyvalue.key.PayloadKey
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.recorder.AudioRecordHandler
import dji.v5.common.recorder.EncodedDataCallback
import dji.v5.common.recorder.OpusEncoder
import dji.v5.common.recorder.SourceDataCallback
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.megaphone.*
import dji.v5.utils.common.ContextUtil
import dji.v5.utils.common.FileUtils
import dji.v5.utils.common.LogUtils
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Description : 喊话器
 * Author : daniel.chen
 * CreateDate : 2022/1/17 12:29 下午
 * Copyright : ©2021 DJI All Rights Reserved.
 */
class MegaphoneVM : DJIViewModel() {
    private val TAG = "MegaphoneVM"
    val curRealTimeUploadedState = MutableLiveData(UploadState.UNKNOWN)
    val curRealTimeSentBytes = MutableLiveData(0L)
    val curRealTimeTotalBytes = MutableLiveData(0L)
    var isPayloadConnect = MutableLiveData(false)

    private var audioRecorderHandler: AudioRecordHandler? = null
    private var opusEncoder: OpusEncoder? = null
    private var isRecording = false
    private var mDuration: Long = -1
    private var mStartTime: Long = -1
    private var savePath: File? = null
    private var saveRawPath: File? = null
    private var mAudioBos: BufferedOutputStream? = null
    private var mRawAudioBos: BufferedOutputStream? = null

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private val listener: RealTimeTransimissionStateListener =
        object : RealTimeTransimissionStateListener {
            override fun onProgress(sentBytes: Long, totalBytes: Long) {
                curRealTimeSentBytes.value = sentBytes
                curRealTimeTotalBytes.value = totalBytes
            }

            override fun onUploadedStatus(state: UploadState?) {
                curRealTimeUploadedState.value = state
            }
        }

    init {
        handlerThread = HandlerThread("IO_Handler_Thread")
        handlerThread!!.start()
        handler = Handler(handlerThread!!.looper)
        addListener()

        KeyManager.getInstance().listen(
            DJIKey.create(PayloadKey.KeyConnection, 0, 2),
            this,
            object : CommonCallbacks.KeyListener<Boolean> {
                override fun onValueChange(oldValue: Boolean?, newValue: Boolean?) {
                    isPayloadConnect.value = newValue ?: false
                }
            })
    }

    override fun onCleared() {
        removeAllListener()
        KeyManager.getInstance()
            .cancelListen(KeyTools.createKey(PayloadKey.KeyConnection, ComponentIndexType.UP))
    }

    fun addListener() {
        MegaphoneManager.getInstance().addRealTimeTransmissionStateListener(listener)
    }

    fun removeAllListener() {
        MegaphoneManager.getInstance().clearAllRealTimeTransimissionStateListener()
    }

    fun setVolume(volume: Int, callback: CommonCallbacks.CompletionCallback) {
        MegaphoneManager.getInstance().setVolume(volume, callback)
    }

    fun getVolume(callback: CommonCallbacks.CompletionCallbackWithParam<Int>) {
        MegaphoneManager.getInstance().getVolume(callback)
    }

    fun setPlayMode(playMode: PlayMode, callback: CommonCallbacks.CompletionCallback) {
        MegaphoneManager.getInstance().setPlayMode(playMode, callback)
    }

    fun getPlayMode(callback: CommonCallbacks.CompletionCallbackWithParam<PlayMode>) {
        MegaphoneManager.getInstance().getPlayMode(callback)
    }

    fun setWorkMode(workMode: WorkMode, callback: CommonCallbacks.CompletionCallback) {
        MegaphoneManager.getInstance().setWorkMode(workMode, callback)
    }

    fun getWorkMode(callback: CommonCallbacks.CompletionCallbackWithParam<WorkMode>) {
        MegaphoneManager.getInstance().getWorkMode(callback)
    }

    fun getStatus(callback: CommonCallbacks.CompletionCallbackWithParam<MegaphoneStatus>) {
        MegaphoneManager.getInstance().getStatus(callback)
    }

    fun startPlay(callback: CommonCallbacks.CompletionCallback) {
        MegaphoneManager.getInstance().startPlay(callback)
    }

    fun stopPlay(callback: CommonCallbacks.CompletionCallback) {
        MegaphoneManager.getInstance().stopPlay(callback)
    }

    fun pushFileToMegaphone(
        fileInfo: FileInfo,
        callback: CommonCallbacks.CompletionCallbackWithProgress<Int>
    ) {
        MegaphoneManager.getInstance().startPushingFileToMegaphone(fileInfo, callback)
    }

    fun stopPushingFile(callback: CommonCallbacks.CompletionCallback) {
        MegaphoneManager.getInstance().cancelPushingFileToMegaphone(callback)
    }

    fun initRecorder() {
        audioRecorderHandler = AudioRecordHandler.getInstance()
        audioRecorderHandler?.init()

        opusEncoder = OpusEncoder()
        opusEncoder!!.config(audioRecorderHandler?.audioConfig)


        opusEncoder!!.setEncodedDataCallback(object : EncodedDataCallback {
            override fun onAudioEncodedCallback(
                data: ByteArray?,
                size: Int
            ) {
                //这里直接发送数据到csdk，同时缓存到本地文件(回调中新开线程写文件)
                handler!!.post {
                    mAudioBos!!.write(data)
                }

                MegaphoneManager.getInstance().sendRealTimeDataToMegaphone(
                    data,
                    data?.size!!,
                    object : CommonCallbacks.CompletionCallback {
                        override fun onSuccess() {
                            LogUtils.d(
                                TAG,
                                "send real time data to megaphone success ${data?.size!!}"
                            )
                        }

                        override fun onFailure(error: IDJIError) {
                            LogUtils.d(TAG, "send real time data to megaphone failed")
                        }
                    })
            }
        })

        audioRecorderHandler!!.setDataCallBack(object : SourceDataCallback {
            override fun onAudioSourceDataCallback(data: ByteArray?, index: Int) {
                handler!!.post {
                    mRawAudioBos!!.write(data)
                }

                opusEncoder!!.putData(data)
            }
        })
    }

    fun startRecord() {
        if (isRecording) {
            return
        }

        mAudioBos = BufferedOutputStream(FileOutputStream(getSavedPath()))
        mRawAudioBos = BufferedOutputStream(FileOutputStream(getRawSavedPath()))


        MegaphoneManager.getInstance()
            .startRealTimeTransmission(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    LogUtils.d(TAG, "start real time transmission success!")
                }

                override fun onFailure(error: IDJIError) {
                    LogUtils.d(TAG, "start real time transmission failed $error")
                }
            })
        mStartTime = System.currentTimeMillis()
        isRecording = true
        initRecorder()
        audioRecorderHandler!!.start()
        opusEncoder!!.start()
    }

    fun stopRecord() {
        if (!isRecording) {
            return
        }
        mDuration = System.currentTimeMillis() - mStartTime
        isRecording = false
        audioRecorderHandler!!.stop()
        audioRecorderHandler!!.release()
        opusEncoder!!.release()
        audioRecorderHandler = null
        opusEncoder = null

        mAudioBos!!.flush()
        mAudioBos!!.close()

        mRawAudioBos!!.flush()
        mRawAudioBos!!.close()

        MegaphoneManager.getInstance()
            .appendEOFToRealTimeData(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    LogUtils.d(TAG, "apped real time EOF to megaphone success")
                    MegaphoneManager.getInstance()
                        .startPlay(object : CommonCallbacks.CompletionCallback {
                            override fun onSuccess() {
                                LogUtils.d(TAG, "Start Play Success")
                            }

                            override fun onFailure(error: IDJIError) {
                                LogUtils.d(TAG, "Start Play Failed")
                            }
                        })
                }

                override fun onFailure(error: IDJIError) {
                    LogUtils.d(TAG, "apped real time EOF to megaphone failed: $error")
                }
            })
    }

    fun getSavedPath(): File? {
        val fileName = "AudioTest.opus"
        savePath = File(
            FileUtils.getMainDir(
                ContextUtil.getContext().getExternalFilesDir(""),
                "RecordFile"
            ), fileName
        )
        return savePath
    }

    fun getRawSavedPath(): File? {
        val fileName = "Raw" + "_AudioTest.pcm"
        saveRawPath = File(
            FileUtils.getMainDir(
                ContextUtil.getContext().getExternalFilesDir(""),
                "RecordFile"
            ), fileName
        )
        return saveRawPath
    }
}