package dji.sampleV5.aircraft.models

import androidx.lifecycle.MutableLiveData
import dji.sampleV5.aircraft.data.DJIToastResult
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.value.common.DoubleRect
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.sdk.keyvalue.value.flightcontroller.FlyToMode
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.et.create
import dji.v5.et.listen
import dji.v5.manager.KeyManager
import dji.v5.manager.intelligent.AutoSensingInfo
import dji.v5.manager.intelligent.AutoSensingInfoListener
import dji.v5.manager.intelligent.AutoSensingTarget
import dji.v5.manager.intelligent.IMissionCapabilityListener
import dji.v5.manager.intelligent.IMissionInfoListener
import dji.v5.manager.intelligent.IntelligentFlightInfo
import dji.v5.manager.intelligent.IntelligentFlightInfoListener
import dji.v5.manager.intelligent.IntelligentFlightManager
import dji.v5.manager.intelligent.IntelligentModel
import dji.v5.manager.intelligent.TargetType
import dji.v5.manager.intelligent.flyto.FlyToInfo
import dji.v5.manager.intelligent.flyto.FlyToParam
import dji.v5.manager.intelligent.flyto.FlyToTarget
import dji.v5.manager.intelligent.poi.POICapability
import dji.v5.manager.intelligent.poi.POIInfo
import dji.v5.manager.intelligent.poi.POIParam
import dji.v5.manager.intelligent.poi.POITarget
import dji.v5.manager.intelligent.smarttrack.SmartTrackInfo
import dji.v5.manager.intelligent.smarttrack.SmartTrackTarget
import dji.v5.manager.intelligent.spotlight.SpotLightInfo
import dji.v5.manager.intelligent.spotlight.SpotLightTarget

class IntelligentFlightVM : DJIViewModel() {

    val aircraftHeight = MutableLiveData<Double>()
    val aircraftLocation = MutableLiveData<LocationCoordinate2D>()

    val intelligentFlightInfo = MutableLiveData<IntelligentFlightInfo>()
    val autoSensingInfo = MutableLiveData<AutoSensingInfo>()
    val intelligentModels = MutableLiveData<List<IntelligentModel>>()
    val runningModelIndex = MutableLiveData<Int>()

    val poiInfo = MutableLiveData<POIInfo>()
    val poiTarget = MutableLiveData<POITarget>()

    val flyToInfo = MutableLiveData<FlyToInfo>()
    val flyToTarget = MutableLiveData<FlyToTarget>()

    val spotLightInfo = MutableLiveData<SpotLightInfo>()
    val spotLightTarget = MutableLiveData<SpotLightTarget>()

    val smartTrackInfo = MutableLiveData<SmartTrackInfo>()
    val smartTrackTarget = MutableLiveData<SmartTrackTarget>()

    private val intelligentFlightInfoListener: IntelligentFlightInfoListener = object :
        IntelligentFlightInfoListener {
        override fun onIntelligentFlightInfoUpdate(info: IntelligentFlightInfo) {
            intelligentFlightInfo.postValue(info)
        }

        override fun onIntelligentFlightErrorUpdate(error: IDJIError) {
        }
    }

    private val autoSensingInfoListener: AutoSensingInfoListener = object :
        AutoSensingInfoListener {
        override fun onAutoSensingInfoUpdate(info: AutoSensingInfo) {
            autoSensingInfo.postValue(info)
        }

        override fun onTrackingTargetUpdate(target: AutoSensingTarget) {
//            super.onTrackingTargetUpdate(target)
        }

        override fun onIntelligentModelUpdate(models: MutableList<IntelligentModel>) {
            intelligentModels.postValue(models)
        }

        override fun onRunningIntelligentModelUpdate(modelId: Int) {
            runningModelIndex.postValue(modelId)
        }
    }

    private val poiInfoListener: IMissionInfoListener<POIInfo, POITarget> = object :
        IMissionInfoListener<POIInfo, POITarget> {
        override fun onMissionInfoUpdate(info: POIInfo) {
            poiInfo.postValue(info)
        }

        override fun onMissionTargetUpdate(target: POITarget) {
            poiTarget.postValue(target)
        }
    }

    private val poiCapabilityListener: IMissionCapabilityListener<POICapability> = IMissionCapabilityListener<POICapability> {
        //kk
    }

    private val flyToInfoListener: IMissionInfoListener<FlyToInfo, FlyToTarget> = object :
        IMissionInfoListener<FlyToInfo, FlyToTarget> {
        override fun onMissionInfoUpdate(info: FlyToInfo) {
            flyToInfo.postValue(info)
        }

        override fun onMissionTargetUpdate(target: FlyToTarget) {
            flyToTarget.postValue(target)
        }
    }

    private val spotLightInfoListener: IMissionInfoListener<SpotLightInfo, SpotLightTarget> = object :
        IMissionInfoListener<SpotLightInfo, SpotLightTarget> {
        override fun onMissionInfoUpdate(info: SpotLightInfo) {
            spotLightInfo.postValue(info)
        }

        override fun onMissionTargetUpdate(target: SpotLightTarget) {
            spotLightTarget.postValue(target)
        }
    }

    private val smartTrackListener: IMissionInfoListener<SmartTrackInfo, SmartTrackTarget> = object :
        IMissionInfoListener<SmartTrackInfo, SmartTrackTarget> {
        override fun onMissionInfoUpdate(info: SmartTrackInfo) {
            smartTrackInfo.postValue(info)
        }

        override fun onMissionTargetUpdate(target: SmartTrackTarget) {
            smartTrackTarget.postValue(target)
        }
    }

    fun initListener() {
        IntelligentFlightManager.getInstance().addIntelligentFlightInfoListener(intelligentFlightInfoListener)
        IntelligentFlightManager.getInstance().addAutoSensingInfoListener(autoSensingInfoListener)
        IntelligentFlightManager.getInstance().poiMissionManager.addMissionInfoListener(poiInfoListener)
        IntelligentFlightManager.getInstance().poiMissionManager.addMissionCapabilityListener(poiCapabilityListener)
        IntelligentFlightManager.getInstance().flyToMissionManager.addMissionInfoListener(flyToInfoListener)
        IntelligentFlightManager.getInstance().spotLightManager.addMissionInfoListener(spotLightInfoListener)
        IntelligentFlightManager.getInstance().smartTrackMissionManager.addMissionInfoListener(smartTrackListener)
        FlightControllerKey.KeyAltitude.create().listen(this) { height ->
            height?.let {
                aircraftHeight.postValue(it)
            }
        }
        FlightControllerKey.KeyAircraftLocation.create().listen(this) { location ->
            location?.let {
                aircraftLocation.postValue(it)
            }
        }
    }

    fun cleanListener() {
        IntelligentFlightManager.getInstance().removeIntelligentFlightInfoListener(intelligentFlightInfoListener)
        IntelligentFlightManager.getInstance().removeAutoSensingInfoListener(autoSensingInfoListener)
        IntelligentFlightManager.getInstance().poiMissionManager.removeMissionInfoListener(poiInfoListener)
        IntelligentFlightManager.getInstance().poiMissionManager.removeMissionCapabilityListener(poiCapabilityListener)
        IntelligentFlightManager.getInstance().flyToMissionManager.removeMissionInfoListener(flyToInfoListener)
        IntelligentFlightManager.getInstance().spotLightManager.removeMissionInfoListener(spotLightInfoListener)
        IntelligentFlightManager.getInstance().smartTrackMissionManager.removeMissionInfoListener(smartTrackListener)
        KeyManager.getInstance().cancelListen(this)
    }

    fun startAutoSensing() {
        IntelligentFlightManager.getInstance().startAutoSensing(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success("startAutoSensing"))
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed("startAutoSensing,$error"))
            }
        })
    }

    fun stopAutoSensing() {
        IntelligentFlightManager.getInstance().stopAutoSensing(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success("stopAutoSensing"))
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed("stopAutoSensing,$error"))
            }
        })
    }

    fun selectIntelligentModel(model: Int) {
        IntelligentFlightManager.getInstance().selectIntelligentModel(model, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success("selectIntelligentModel,$model"))
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed("selectIntelligentModel,$error"))
            }
        })
    }

    fun startPOIMission(target: POITarget) {
        IntelligentFlightManager.getInstance().poiMissionManager.startMission(target, null,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    toastResult?.postValue(DJIToastResult.success("startPOIMission"))
                }

                override fun onFailure(error: IDJIError) {
                    toastResult?.postValue(DJIToastResult.failed("startPOIMission,$error"))
                }
            })
    }

    fun stopPOIMission() {
        IntelligentFlightManager.getInstance().poiMissionManager.stopMission(
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    toastResult?.postValue(DJIToastResult.success("stopPOIMission"))
                }

                override fun onFailure(error: IDJIError) {
                    toastResult?.postValue(DJIToastResult.failed("stopPOIMission,$error"))
                }
            })
    }

    fun updatePOIMissionTarget(target: POITarget) {
        IntelligentFlightManager.getInstance().poiMissionManager.updateMissionTarget(target,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    toastResult?.postValue(DJIToastResult.success("updatePOIMissionTarget,$target"))
                }

                override fun onFailure(error: IDJIError) {
                    toastResult?.postValue(DJIToastResult.failed("updatePOIMissionTarget,$target,$error"))
                }
            })
    }

    fun updatePOICircleSpeed(speed: Double) {
        val param = POIParam()
        param.circleSpeed = speed
        IntelligentFlightManager.getInstance().poiMissionManager.updateMissionParam(param,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    toastResult?.postValue(DJIToastResult.success("updatePOICircleSpeed,$param"))
                }

                override fun onFailure(error: IDJIError) {
                    toastResult?.postValue(DJIToastResult.failed("updatePOICircleSpeed,$param,$error"))
                }
            })
    }

    fun lockCircularVelocity(lock: Boolean) {
        IntelligentFlightManager.getInstance().poiMissionManager.lockCircularVelocity(
            lock,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    toastResult?.postValue(DJIToastResult.success("lockCircularVelocity:$lock"))
                }

                override fun onFailure(error: IDJIError) {
                    toastResult?.postValue(DJIToastResult.failed("lockCircularVelocity:$lock,$error"))
                }
            })
    }

    fun lockGimbalPitch(lock: Boolean) {
        IntelligentFlightManager.getInstance().poiMissionManager.lockGimbalPitch(
            lock,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    toastResult?.postValue(DJIToastResult.success("lockGimbalPitch:$lock"))
                }

                override fun onFailure(error: IDJIError) {
                    toastResult?.postValue(DJIToastResult.failed("lockGimbalPitch:$lock,$error"))
                }
            })
    }

    fun startFlyTo(target: FlyToTarget) {
        IntelligentFlightManager.getInstance().flyToMissionManager.startMission(target, null,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    toastResult?.postValue(DJIToastResult.success("startFlyTo"))
                }

                override fun onFailure(error: IDJIError) {
                    toastResult?.postValue(DJIToastResult.failed("startFlyTo,$error"))
                }
            })
    }

    fun stopFlyTo() {
        IntelligentFlightManager.getInstance().flyToMissionManager.stopMission(
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    toastResult?.postValue(DJIToastResult.success("stopFlyTo"))
                }

                override fun onFailure(error: IDJIError) {
                    toastResult?.postValue(DJIToastResult.failed("stopFlyTo,$error"))
                }
            })
    }

    fun setFlyToMode(mode: FlyToMode) {
        val param = FlyToParam()
        param.flyToMode = mode
        IntelligentFlightManager.getInstance().flyToMissionManager.updateMissionParam(param,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    toastResult?.postValue(DJIToastResult.success("setFlyToMode:$mode"))
                }

                override fun onFailure(error: IDJIError) {
                    toastResult?.postValue(DJIToastResult.failed("setFlyToMode:$mode,$error"))
                }
            })
    }

    fun setFlyToHeight(height: Int) {
        val param = FlyToParam()
        param.height = height
        IntelligentFlightManager.getInstance().flyToMissionManager.updateMissionParam(param,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    toastResult?.postValue(DJIToastResult.success("setFlyToMode:$height"))
                }

                override fun onFailure(error: IDJIError) {
                    toastResult?.postValue(DJIToastResult.failed("setFlyToMode:$height,$error"))
                }
            })
    }

    fun enterSpotLightMode() {
        IntelligentFlightManager.getInstance().spotLightManager.enterSpotLightMode(
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    toastResult?.postValue(DJIToastResult.success("enterSpotLightMode"))
                }

                override fun onFailure(error: IDJIError) {
                    toastResult?.postValue(DJIToastResult.failed("enterSpotLightMode,$error"))
                }
            })
    }

    fun exitSpotLightMode() {
        IntelligentFlightManager.getInstance().spotLightManager.exitSpotLightMode(
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    toastResult?.postValue(DJIToastResult.success("exitSpotLightMode"))
                }

                override fun onFailure(error: IDJIError) {
                    toastResult?.postValue(DJIToastResult.failed("exitSpotLightMode,$error"))
                }
            })
    }

    fun startSpotlight() {
        IntelligentFlightManager.getInstance().spotLightManager.startMission(null, null,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    toastResult?.postValue(DJIToastResult.success("startSpotlight"))
                }

                override fun onFailure(error: IDJIError) {
                    toastResult?.postValue(DJIToastResult.failed("startSpotlight,$error"))
                }
            })
    }

    fun selectAutoTarget(index: Int) {
        val param = SpotLightTarget()
        param.targetIndex = index
        param.targetType = TargetType.INDEX
        IntelligentFlightManager.getInstance().spotLightManager.updateMissionTarget(param,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    toastResult?.postValue(DJIToastResult.success("selectAutoTarget:$index"))
                }

                override fun onFailure(error: IDJIError) {
                    toastResult?.postValue(DJIToastResult.failed("selectAutoTarget:$index,$error"))
                }
            })
    }

    fun selectManualTarget(bound: DoubleRect) {
        val param = SpotLightTarget()
        param.targetRect = bound
        param.targetType = TargetType.RECT
        IntelligentFlightManager.getInstance().spotLightManager.updateMissionTarget(param,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    toastResult?.postValue(DJIToastResult.success("selectManualTarget:$bound"))
                }

                override fun onFailure(error: IDJIError) {
                    toastResult?.postValue(DJIToastResult.failed("selectManualTarget:$bound,$error"))
                }
            })
    }

    fun confirmTarget() {
        IntelligentFlightManager.getInstance().spotLightManager.confirmTarget(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success("confirmTarget"))
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed("confirmTarget,$error"))
            }
        })
    }

    fun stopSpotlight() {
        IntelligentFlightManager.getInstance().spotLightManager.stopMission(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success("stopSpotlight"))
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed("stopSpotlight,$error"))
            }
        })
    }

    fun startSmartTrack() {
        IntelligentFlightManager.getInstance().smartTrackMissionManager.startMission(null, null, object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success("startSmartTrack"))
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed("startSmartTrack,$error"))
            }
        })
    }

    fun stopSmartTrack() {
        IntelligentFlightManager.getInstance().smartTrackMissionManager.stopMission(object :
            CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                toastResult?.postValue(DJIToastResult.success("stopSmartTrack"))
            }

            override fun onFailure(error: IDJIError) {
                toastResult?.postValue(DJIToastResult.failed("stopSmartTrack,$error"))
            }
        })
    }

    fun selectTrackingTarget(index: Int) {
        val param = SmartTrackTarget()
        param.index = index
        IntelligentFlightManager.getInstance().smartTrackMissionManager.updateMissionTarget(param,
            object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    toastResult?.postValue(DJIToastResult.success("selectTrackingTarget:$index"))
                }

                override fun onFailure(error: IDJIError) {
                    toastResult?.postValue(DJIToastResult.failed("selectTrackingTarget:$index,$error"))
                }
            })
    }
}