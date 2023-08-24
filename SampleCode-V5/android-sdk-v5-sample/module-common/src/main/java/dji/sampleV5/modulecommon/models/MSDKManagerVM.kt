package dji.sampleV5.modulecommon.models

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback

class MSDKManagerVM : ViewModel() {
    // The data is held in livedata mode, but you can also save the results of the sdk callbacks any way you like.
    val lvRegisterState = MutableLiveData<Pair<Boolean, IDJIError?>>()
    val lvProductConnectionState = MutableLiveData<Pair<Boolean, Int>>()
    val lvProductChanges = MutableLiveData<Int>()
    val lvInitProcess = MutableLiveData<Pair<DJISDKInitEvent, Int>>()
    val lvDBDownloadProgress = MutableLiveData<Pair<Long, Long>>()

    fun initMobileSDK(appContext: Context) {
        // Initialize and set the sdk callback, which is held internally by the sdk until destroy() is called
        SDKManager.getInstance().init(appContext, object : SDKManagerCallback {
            override fun onRegisterSuccess() {
                lvRegisterState.postValue(Pair(true, null))
            }

            override fun onRegisterFailure(error: IDJIError) {
                lvRegisterState.postValue(Pair(false, error))
            }

            override fun onProductDisconnect(productId: Int) {
                lvProductConnectionState.postValue(Pair(false, productId))
            }

            override fun onProductConnect(productId: Int) {
                lvProductConnectionState.postValue(Pair(true, productId))
            }

            override fun onProductChanged(productId: Int) {
                lvProductChanges.postValue(productId)
            }

            override fun onInitProcess(event: DJISDKInitEvent, totalProcess: Int) {
                lvInitProcess.postValue(Pair(event, totalProcess))

                // Don't forget to call the registerApp()
                if (event == DJISDKInitEvent.INITIALIZE_COMPLETE) {
                    SDKManager.getInstance().registerApp()
                }
            }

            override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                lvDBDownloadProgress.postValue(Pair(current, total))
            }
        })
    }

    fun destroyMobileSDK() {
        SDKManager.getInstance().destroy()
    }

}