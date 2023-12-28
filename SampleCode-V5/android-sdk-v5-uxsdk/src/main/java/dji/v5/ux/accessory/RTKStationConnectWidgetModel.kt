package dji.v5.ux.accessory

import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.rtkbasestation.RTKStationConnetState
import dji.sdk.keyvalue.value.rtkbasestation.RTKStationInfo
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.common.error.RxError
import dji.v5.manager.aircraft.rtk.station.ConnectedRTKStationInfo
import dji.v5.manager.aircraft.rtk.station.ConnectedRTKStationInfoListener
import dji.v5.manager.aircraft.rtk.station.RTKStationConnectStatusListener
import dji.v5.manager.aircraft.rtk.station.SearchRTKStationListener
import dji.v5.manager.interfaces.IRTKCenter
import dji.v5.ux.accessory.data.DJIRTKBaseStationConnectInfo
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.SchedulerProvider
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.processors.BehaviorProcessor
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.ArrayList

/**
 * Description :Widget Model for the [RTKStationConnectWidget] used to define
 * the underlying logic and communication
 *
 * @author: Byte.Cai
 *  date : 2022/9/1
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class RTKStationConnectWidgetModel(
    djiSdkModel: DJISDKModel,
    keyedStore: ObservableInMemoryKeyedStore,
    val rtkCenter: IRTKCenter,
) : WidgetModel(djiSdkModel, keyedStore) {
    private val rtkStationManager = rtkCenter.rtkStationManager
    private val rtkStationConnectStateProcessor: DataProcessor<RTKStationConnetState> =
        DataProcessor.create(RTKStationConnetState.UNKNOWN)

    private val stationListProcessor: DataProcessor<ArrayList<DJIRTKBaseStationConnectInfo>> =
        DataProcessor.create(arrayListOf())

    private val connectedRTKStationInfoProcessor: BehaviorProcessor<ConnectedRTKStationInfo> =
        BehaviorProcessor.create()
    private val isMotorOnProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private val stationConnectStatusListener =
        RTKStationConnectStatusListener { rtkBaseStationConnectState ->
            rtkStationConnectStateProcessor.onNext(rtkBaseStationConnectState)
        }

    private val connectedRTKStationInfoListener =
        ConnectedRTKStationInfoListener {
            connectedRTKStationInfoProcessor.onNext(it)
        }
    private val searchStationListener =
        SearchRTKStationListener { newConnectInfoList ->
            val convertToDJIRTKBaseStationConnectInfo = convertToDJIRTKBaseStationConnectInfo(newConnectInfoList)
            stationListProcessor.onNext(convertToDJIRTKBaseStationConnectInfo)
        }

    val isMotorOn: Flowable<Boolean>
        get() = isMotorOnProcessor.toFlowable()

    val stationConnectStatus: Flowable<RTKStationConnetState>
        get() = rtkStationConnectStateProcessor.toFlowable()

    val connectedRTKStationInfo: Flowable<ConnectedRTKStationInfo>
        get() = connectedRTKStationInfoProcessor.observeOn(SchedulerProvider.computation()).onBackpressureLatest()

    val stationList: Flowable<ArrayList<DJIRTKBaseStationConnectInfo>>
        get() = stationListProcessor.toFlowable()


    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyAreMotorsOn), isMotorOnProcessor)
        rtkStationManager.addRTKStationConnectStatusListener(stationConnectStatusListener)
        rtkStationManager.addConnectedRTKStationInfoListener(connectedRTKStationInfoListener)
        rtkStationManager.addSearchRTKStationListener(searchStationListener)
    }

    override fun inCleanup() {
        rtkStationManager.removeRTKStationConnectStatusListener(stationConnectStatusListener)
        rtkStationManager.removeConnectedRTKStationInfoListener(connectedRTKStationInfoListener)
        rtkStationManager.removeSearchRTKStationListener(searchStationListener)
    }

    fun startConnectToRTKStation(stationId: Int): Single<Boolean> {
        return Single.create(SingleOnSubscribe<Boolean> {
            rtkStationManager.startConnectToRTKStation(stationId, object :
                CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    it.onSuccess(true)
                }

                override fun onFailure(error: IDJIError) {
                    it.onError(RxError(error))
                }

            })
        }).subscribeOn(Schedulers.computation())

    }

    fun startSearchStationRTK(): Single<Boolean> {
        return Single.create(SingleOnSubscribe<Boolean> {
            rtkStationManager.startSearchRTKStation(object :
                CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    it.onSuccess(true)
                }

                override fun onFailure(error: IDJIError) {
                    it.onError(RxError(error))
                }

            })
        }).subscribeOn(Schedulers.computation())
    }

    fun stopSearchStationRTK(): Single<Boolean> {
        return Single.create(SingleOnSubscribe<Boolean> {
            rtkStationManager.startSearchRTKStation(object :
                CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    it.onSuccess(true)
                }

                override fun onFailure(error: IDJIError) {
                    it.onError(RxError(error))
                }

            })
        }).subscribeOn(Schedulers.computation())
    }


    private fun convertToDJIRTKBaseStationConnectInfo(list: List<RTKStationInfo>?): ArrayList<DJIRTKBaseStationConnectInfo> {
        val djiRTKBaseStationConnectInfoList = ArrayList<DJIRTKBaseStationConnectInfo>()
        list?.let {
            for (i in list) {
                val djirtkBaseStationConnectInfo = i.toDJIRTKBaseStationConnectInfo()
                djiRTKBaseStationConnectInfoList.add(djirtkBaseStationConnectInfo)
            }
        }
        return djiRTKBaseStationConnectInfoList

    }

    private fun RTKStationInfo.toDJIRTKBaseStationConnectInfo(): DJIRTKBaseStationConnectInfo {
        val baseStationId = this.stationId
        val name = this.stationName
        val signalLevel = this.signalLevel
        return DJIRTKBaseStationConnectInfo(baseStationId, signalLevel, name)
    }
}