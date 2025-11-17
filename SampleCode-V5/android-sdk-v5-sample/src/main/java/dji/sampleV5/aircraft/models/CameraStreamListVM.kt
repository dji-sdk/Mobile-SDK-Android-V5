package dji.sampleV5.aircraft.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.interfaces.ICameraStreamManager.AvailableCameraUpdatedListener

class CameraStreamListVM : DJIViewModel(), AvailableCameraUpdatedListener {

    private val _availableCameraListData = MutableLiveData<List<ComponentIndexType>>(ArrayList())

    init {
        MediaDataCenter.getInstance().cameraStreamManager.addAvailableCameraUpdatedListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        MediaDataCenter.getInstance().cameraStreamManager.removeAvailableCameraUpdatedListener(this)
    }

    val availableCameraListData: LiveData<List<ComponentIndexType>>
        get() = _availableCameraListData

    override fun onAvailableCameraUpdated(availableCameraList: List<ComponentIndexType>) {
        _availableCameraListData.postValue(availableCameraList)
    }

    override fun onCameraStreamEnableUpdate(cameraStreamEnableMap: MutableMap<ComponentIndexType, Boolean>) {
        //
    }
}