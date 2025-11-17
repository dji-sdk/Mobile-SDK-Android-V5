package dji.sampleV5.aircraft.models

import android.view.Surface
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dji.sampleV5.aircraft.util.ToastUtils
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.camera.CameraMode
import dji.sdk.keyvalue.value.airlink.ChannelPriority
import dji.sdk.keyvalue.value.camera.CameraType
import dji.sdk.keyvalue.value.camera.CameraVideoStreamSourceType
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.flightassistant.VisionAssistDirection
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.et.create
import dji.v5.et.listen
import dji.v5.et.set
import dji.v5.manager.KeyManager
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.interfaces.ICameraStreamManager
import dji.v5.manager.interfaces.ICameraStreamManager.FrameFormat
import dji.v5.manager.interfaces.ICameraStreamManager.ScaleType
import dji.v5.utils.common.DJIExecutor
import dji.v5.utils.common.DateUtils
import dji.v5.utils.common.LogPath
import dji.v5.utils.common.LogUtils
import dji.v5.utils.common.StringUtils
import dji.v5.ux.R
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

const val TAG = "CameraStreamDetailFragmentVM"

class CameraStreamDetailVM : DJIViewModel() {

    private val _availableLensListData = MutableLiveData<List<CameraVideoStreamSourceType>>(ArrayList())
    private val _currentLensData = MutableLiveData(CameraVideoStreamSourceType.DEFAULT_CAMERA)
    private val _cameraName = MutableLiveData("Unknown")
    private val _isVisionAssistEnabled = MutableLiveData(false)
    private val _visionAssistViewDirection = MutableLiveData(VisionAssistDirection.UNKNOWN)
    private val _visionAssistViewDirectionRange = MutableLiveData<List<VisionAssistDirection>>(ArrayList())
    val cameraStreamEnableMap = MutableLiveData(emptyMap<ComponentIndexType, Boolean>())

    private var cameraIndex = ComponentIndexType.UNKNOWN
    private var cameraType = ""
    private var isMotorOn = false
    private val visionAssistStatusListener = object :
        ICameraStreamManager.VisionAssistStatusListener {
        override fun onVisionAssistEnabled(isEnable: Boolean) {
            _isVisionAssistEnabled.postValue(isEnable)
        }

        override fun onVisionAssistViewDirectionUpdated(mode: VisionAssistDirection) {
            _visionAssistViewDirection.postValue(mode)
        }

        override fun onVisionAssistViewDirectionRangeUpdated(modes: MutableList<VisionAssistDirection>) {
            _visionAssistViewDirectionRange.postValue(modes)
        }
    }

    private val availableCameraUpdatedListener = object :
        ICameraStreamManager.AvailableCameraUpdatedListener {
        override fun onAvailableCameraUpdated(availableCameraList: MutableList<ComponentIndexType>) {
            //do nothing
        }

        override fun onCameraStreamEnableUpdate(map: MutableMap<ComponentIndexType, Boolean>) {
            cameraStreamEnableMap.postValue(map)
        }
    }

    private var streamFile: File? = null
    private var streamFileOutputStream: FileOutputStream? = null

    private val streamListener = ICameraStreamManager.ReceiveStreamListener { data, offset, length, info ->
        if (streamFile == null) {
            val fileName = "[${cameraIndex.name}]${DateUtils.getSystemTime()}.${info.mimeType.name.lowercase(Locale.ROOT)}"
            ToastUtils.showToast("begin to save,$fileName")
            streamFile = File(LogUtils.getLogPath(), fileName)
            streamFileOutputStream = FileOutputStream(streamFile)
            return@ReceiveStreamListener
        }
        DJIExecutor.getExecutor().execute {
            try {
                streamFileOutputStream?.write(data, offset, length)
            } catch (e: Exception) {
                //do nothing
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        setCameraIndex(ComponentIndexType.UNKNOWN)
        MediaDataCenter.getInstance().cameraStreamManager.removeAvailableCameraUpdatedListener(availableCameraUpdatedListener)
        MediaDataCenter.getInstance().cameraStreamManager.removeVisionAssistStatusListener(visionAssistStatusListener)
        KeyManager.getInstance().cancelListen(this)
        doStopDownloadStreamToLocal()
    }

    fun setCameraIndex(cameraIndex: ComponentIndexType) {
        KeyManager.getInstance().cancelListen(this)
        if (this.cameraIndex == cameraIndex) {
            return
        }
        this.cameraIndex = cameraIndex
        if (this.cameraIndex == ComponentIndexType.UNKNOWN) {
            return
        }
        listenCameraName()
        listenAvailableLens()
        listenCurrentLens()
        listenVisionAssistStatus()
        MediaDataCenter.getInstance().cameraStreamManager.addAvailableCameraUpdatedListener(availableCameraUpdatedListener)
    }

    private fun listenCameraName() {
        CameraKey.KeyCameraType.create(cameraIndex).listen(this) {
            cameraType = it?.name ?: CameraType.NOT_SUPPORTED.name
            updateCameraName()
        }

        FlightControllerKey.KeyAreMotorsOn.create().listen(this) {
            isMotorOn = it == true
            updateCameraName()
        }
    }

    private fun updateCameraName() {
        _cameraName.postValue("")
        if (cameraIndex == ComponentIndexType.UNKNOWN) {
            return
        }
        if (cameraIndex == ComponentIndexType.FPV) {
            _cameraName.postValue(ComponentIndexType.FPV.name)
            return
        }
        if (cameraIndex == ComponentIndexType.VISION_ASSIST) {
            var msg = ComponentIndexType.VISION_ASSIST.name
            if (!isMotorOn) {
                msg = "$msg(${StringUtils.getResStr(R.string.uxsdk_assistant_video_empty_text)})"
            }
            _cameraName.postValue(msg)
            return
        }
        _cameraName.postValue(cameraType)
    }

    private fun listenAvailableLens() {
        _availableLensListData.postValue(arrayListOf())
        if (cameraIndex == ComponentIndexType.UNKNOWN) {
            return
        }
        CameraKey.KeyCameraVideoStreamSourceRange.create(cameraIndex).listen(this) {
            val list: List<CameraVideoStreamSourceType> = it ?: arrayListOf()
            _availableLensListData.postValue(list)
        }
    }

    private fun listenCurrentLens() {
        _currentLensData.postValue(CameraVideoStreamSourceType.DEFAULT_CAMERA)
        if (cameraIndex == ComponentIndexType.UNKNOWN) {
            return
        }
        CameraKey.KeyCameraVideoStreamSource.create(cameraIndex).listen(this) {
            if (it != null) {
                _currentLensData.postValue(it)
            } else {
                _currentLensData.postValue(CameraVideoStreamSourceType.DEFAULT_CAMERA)
            }
        }
    }

    private fun listenVisionAssistStatus() {
        MediaDataCenter.getInstance().cameraStreamManager.addVisionAssistStatusListener(visionAssistStatusListener)
    }

    fun changeCameraLens(lensType: CameraVideoStreamSourceType) {
        CameraKey.KeyCameraVideoStreamSource.create(cameraIndex).set(lensType)
    }

    fun putCameraStreamSurface(
        surface: Surface, width: Int, height: Int, scaleType: ScaleType
    ) {
        MediaDataCenter.getInstance().cameraStreamManager.putCameraStreamSurface(cameraIndex, surface, width, height, scaleType)
    }

    fun removeCameraStreamSurface(surface: Surface) {
        MediaDataCenter.getInstance().cameraStreamManager.removeCameraStreamSurface(surface)
    }

    fun downloadYUVImageToLocal(format: FrameFormat, formatName: String) {
        MediaDataCenter.getInstance().cameraStreamManager.addFrameListener(
            cameraIndex,
            format,
            object : ICameraStreamManager.CameraFrameListener {
                override fun onFrame(frameData: ByteArray, offset: Int, length: Int, width: Int, height: Int, format: FrameFormat) {
                    try {
                        val dirs = File(LogUtils.getLogPath() + "STREAM_PIC")
                        if (!dirs.exists()) {
                            dirs.mkdirs()
                        }
                        val fileName = "[${cameraIndex.name}][$width x $height]${DateUtils.getSystemTime()}.${formatName}"
                        val file = File(dirs.absolutePath, fileName)
                        FileOutputStream(file).use { stream ->
                            stream.write(frameData, offset, length)
                            stream.flush()
                            stream.close()
                            ToastUtils.showToast("Save to : ${file.path}")
                        }
                        LogUtils.i(TAG, "Save to : ${file.path}")
                    } catch (e: Exception) {
                        ToastUtils.showToast("Save fail : $e")
                    }
                    // Because only one frame needs to be saved, you need to call removeOnFrameListener here
                    // If you need to read frame data for a long time, you can choose to actually call remove OnFrameListener according to your needs
                    MediaDataCenter.getInstance().cameraStreamManager.removeFrameListener(this)
                }
            })
    }

    fun enableVisionAssist(enable: Boolean) {
        MediaDataCenter.getInstance().cameraStreamManager.enableVisionAssist(enable, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                ToastUtils.showToast("enableVisionAssist onSuccess $enable")
            }

            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast("enableVisionAssist onFailure $enable,error:$error")
            }
        })
    }

    fun setVisionAssistViewDirection(direction: VisionAssistDirection) {
        MediaDataCenter.getInstance().cameraStreamManager.setVisionAssistViewDirection(direction, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                ToastUtils.showToast("set Direction onSuccess $direction")
            }

            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast("set Direction onFailure $direction,error:$error")
            }
        })
    }

    fun beginDownloadStreamToLocal() {
        if (streamFile != null) {
            ToastUtils.showToast("Pls stop first.")
            return
        }
        MediaDataCenter.getInstance().cameraStreamManager.addReceiveStreamListener(cameraIndex, streamListener)
    }

    fun stopDownloadStreamToLocal() {
        if (streamFile == null) {
            ToastUtils.showToast("Pls begin first.")
            return
        }
        ToastUtils.showToast("stop to save,${streamFile?.name}")
        doStopDownloadStreamToLocal()
    }

    fun setStreamEncoderBitrate(bitrate: Int) {
        MediaDataCenter.getInstance().cameraStreamManager.setStreamEncoderBitrate(cameraIndex, bitrate)
    }

    fun getStreamEncoderBitrate() = MediaDataCenter.getInstance().cameraStreamManager.getStreamEncoderBitrate(cameraIndex)

    fun changeCameraMode(mode: CameraMode) {
        CameraKey.KeyCameraMode.create().set(mode)
    }

    fun setStreamPriority(priority: ChannelPriority) = MediaDataCenter.getInstance().cameraStreamManager.setStreamPriority(cameraIndex, priority)

    fun getStreamPriority() = MediaDataCenter.getInstance().cameraStreamManager.getStreamPriority(cameraIndex)

    fun enableStream(enable: Boolean) = MediaDataCenter.getInstance().cameraStreamManager.enableStream(cameraIndex, enable)

    private fun doStopDownloadStreamToLocal() {
        MediaDataCenter.getInstance().cameraStreamManager.removeReceiveStreamListener(streamListener)
        try {
            streamFileOutputStream?.flush()
            streamFileOutputStream?.close()
            streamFileOutputStream = null
            streamFile = null
        } catch (e: Exception) {
            //do nothing
        }
    }

    val availableLensListData: LiveData<List<CameraVideoStreamSourceType>>
        get() = _availableLensListData

    val currentLensData: LiveData<CameraVideoStreamSourceType>
        get() = _currentLensData

    val cameraName: LiveData<String>
        get() = _cameraName

    val isVisionAssistEnabled: LiveData<Boolean>
        get() = _isVisionAssistEnabled

    val visionAssistViewDirection: LiveData<VisionAssistDirection>
        get() = _visionAssistViewDirection

    val visionAssistViewDirectionRange: LiveData<List<VisionAssistDirection>>
        get() = _visionAssistViewDirectionRange
}