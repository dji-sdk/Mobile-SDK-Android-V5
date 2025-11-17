package dji.v5.ux.accessory

import dji.rtk.CoordinateSystem
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.key.ProductKey
import dji.sdk.keyvalue.utils.ProductUtil
import dji.sdk.keyvalue.value.product.ProductType
import dji.sdk.keyvalue.value.rtkbasestation.RTKReferenceStationSource
import dji.v5.common.utils.RxUtil
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.rtk.RTKSystemStateListener
import dji.v5.manager.areacode.AreaCode
import dji.v5.manager.areacode.AreaCodeChangeListener
import dji.v5.manager.areacode.AreaCodeManager
import dji.v5.manager.interfaces.IAreaCodeManager
import dji.v5.manager.interfaces.INetworkRTKManager
import dji.v5.manager.interfaces.IRTKCenter
import dji.v5.utils.common.LogUtils
import dji.v5.ux.core.base.DJISDKModel
import dji.v5.ux.core.base.WidgetModel
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore
import dji.v5.ux.core.util.DataProcessor
import io.reactivex.rxjava3.core.Flowable

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2022/8/15
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */

class RTKTypeSwitchWidgetModel(
    djiSdkModel: DJISDKModel,
    uxKeyManager: ObservableInMemoryKeyedStore,
    val areaCodeManager: IAreaCodeManager,
    val rtkCenter: IRTKCenter,
) : WidgetModel(djiSdkModel, uxKeyManager) {

    companion object {
        private const val TAG = "RTKTypeSwitchWidgetModel"
        private const val CUSTOM_RTK_SETTING_CACHE = "customRTKSettingCache"
    }

    private val supportReferenceStationListProcessor: DataProcessor<List<RTKReferenceStationSource>> = DataProcessor.create(ArrayList())
    private val areaCodeProcessor: DataProcessor<String> = DataProcessor.create(AreaCode.UNKNOWN.code)
    private val productTypeProcessor: DataProcessor<ProductType> = DataProcessor.create(ProductType.UNKNOWN)
    private val rtkSourceProcessor: DataProcessor<RTKReferenceStationSource> = DataProcessor.create(RTKReferenceStationSource.UNKNOWN)
    private val coordinateSystemListProcessor: DataProcessor<List<CoordinateSystem>> = DataProcessor.create(arrayListOf())
    private val isMotorOnProcessor: DataProcessor<Boolean> = DataProcessor.create(false)
    private var qxRTKManager: INetworkRTKManager? = null
    private var customNetworkRTKManager: INetworkRTKManager? = null
    private var currentRtkSource: RTKReferenceStationSource = RTKReferenceStationSource.UNKNOWN
    private val areaCodeChangeListener = AreaCodeChangeListener { _, result ->
        areaCodeProcessor.onNext(result.areaCode)
        updateSupportReferenceStationList()
    }


    private val rtkSystemStateListener = RTKSystemStateListener {
        val rtkSource = it.rtkReferenceStationSource
        var coordinateSystemList: List<CoordinateSystem> = arrayListOf()
        when (rtkSource) {
            RTKReferenceStationSource.QX_NETWORK_SERVICE,
            RTKReferenceStationSource.NTRIP_NETWORK_SERVICE,
            -> {
                coordinateSystemList = arrayListOf(CoordinateSystem.WGS84, CoordinateSystem.CGCS2000)
            }

            else -> {
                //Do Nothing
            }
        }
        if (currentRtkSource != rtkSource) {
            currentRtkSource = rtkSource
            rtkSourceProcessor.onNext(rtkSource)
            coordinateSystemListProcessor.onNext(coordinateSystemList)

        }
    }


    val isMotorsOn: Flowable<Boolean>
        get() = isMotorOnProcessor.toFlowable()

    val rtkSource: Flowable<RTKReferenceStationSource>
        get() = rtkSourceProcessor.toFlowable()

    val coordinateSystemList: Flowable<List<CoordinateSystem>>
        get() = coordinateSystemListProcessor.toFlowable()

    val supportReferenceStationList: Flowable<List<RTKReferenceStationSource>>
        get() = supportReferenceStationListProcessor.toFlowable()

    init {
        qxRTKManager = rtkCenter.qxrtkManager
        customNetworkRTKManager = rtkCenter.customRTKManager
    }

    override fun inSetup() {
        bindDataProcessor(
            KeyTools.createKey(
                FlightControllerKey.KeyAreMotorsOn
            ), isMotorOnProcessor
        )
        addDisposable(RxUtil.addListener(
            KeyTools.createKey(
                ProductKey.KeyProductType
            ), this
        ).subscribe {
            productTypeProcessor.onNext(it)
            updateSupportReferenceStationList()
        })
        areaCodeManager.addAreaCodeChangeListener(areaCodeChangeListener)
        rtkCenter.addRTKSystemStateListener(rtkSystemStateListener)

    }

    override fun inCleanup() {
        areaCodeManager.removeAreaCodeChangeListener(areaCodeChangeListener)
        rtkCenter.removeRTKSystemStateListener(rtkSystemStateListener)
        KeyManager.getInstance().cancelListen(this)
    }

    private fun updateSupportReferenceStationList() {
        if (ProductType.DJI_MAVIC_3_ENTERPRISE_SERIES == productTypeProcessor.value) {
            when (areaCodeProcessor.value) {
                AreaCode.CHINA.code -> {
                    supportReferenceStationListProcessor.onNext(getSupportReferenceStationSource(true))
                }

                else -> {
                    supportReferenceStationListProcessor.onNext(getSupportReferenceStationSource(false))
                }
            }
        } else {
            supportReferenceStationListProcessor.onNext(getSupportReferenceStationSource(isInChina()))
        }
    }


    /**
     * 获取支持的差分数据源
     */
    private fun getSupportReferenceStationSource(supportNetworkRTK: Boolean): List<RTKReferenceStationSource> {
        return if (ProductUtil.isM3EProduct() || ProductUtil.isM4EProduct() || ProductUtil.isM4DProduct()) {
            getMavicSupportReferenceStationSource(supportNetworkRTK)
        } else {
            getDefaultSupportReferenceStationSource(supportNetworkRTK)
        }
    }


    private fun getMavicSupportReferenceStationSource(supportNetworkRTK: Boolean): MutableList<RTKReferenceStationSource> {
        return if (supportNetworkRTK) {
            mutableListOf(
                RTKReferenceStationSource.NONE,
                RTKReferenceStationSource.BASE_STATION,
                RTKReferenceStationSource.NTRIP_NETWORK_SERVICE,
                RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE
            )
        } else {
            mutableListOf(
                RTKReferenceStationSource.NONE,
                RTKReferenceStationSource.BASE_STATION,
                RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE
            )
        }
    }

    private fun getDefaultSupportReferenceStationSource(supportNetworkRTK: Boolean): MutableList<RTKReferenceStationSource> {
        LogUtils.i(TAG, "supportNetworkRTK=$supportNetworkRTK")
        return if (supportNetworkRTK) {
            mutableListOf(
                RTKReferenceStationSource.NONE,
                RTKReferenceStationSource.BASE_STATION,
                RTKReferenceStationSource.QX_NETWORK_SERVICE,
                RTKReferenceStationSource.NTRIP_NETWORK_SERVICE,
                RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE
            )
        } else {
            mutableListOf(
                RTKReferenceStationSource.NONE,
                RTKReferenceStationSource.BASE_STATION,
                RTKReferenceStationSource.CUSTOM_NETWORK_SERVICE
            )
        }
    }


    /**
     * 是否在中国
     */
    private fun isInChina(): Boolean {
        val countryCode = AreaCodeManager.getInstance().areaCode.areaCode
        return AreaCode.CHINA.code.equals(countryCode)
    }

}