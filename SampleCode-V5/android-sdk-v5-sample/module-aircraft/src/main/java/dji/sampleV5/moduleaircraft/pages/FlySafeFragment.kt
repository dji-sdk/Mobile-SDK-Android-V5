package dji.sampleV5.moduleaircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.models.FlySafeVM
import dji.sampleV5.modulecommon.BuildConfig
import dji.sampleV5.modulecommon.keyvalue.KeyValueDialogUtil
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sampleV5.modulecommon.util.Helper
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.v5.manager.aircraft.flysafe.info.FlySafeLicenseType
import dji.v5.manager.aircraft.flysafe.info.FlyZoneInformation
import dji.v5.manager.aircraft.flysafe.info.FlyZoneLicenseInfo
import dji.v5.manager.areacode.AreaCode
import dji.v5.manager.areacode.AreaCodeManager
import dji.v5.utils.common.DateUtils
import dji.v5.utils.common.JsonUtil
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.v5.ux.map.MapWidget
import dji.v5.ux.mapkit.core.maps.DJIMap
import kotlinx.android.synthetic.main.frag_fly_safe_page.*
import kotlinx.android.synthetic.main.frag_fly_safe_page.map_widget
import java.util.*

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/8/12
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class FlySafeFragment : DJIFragment() {
    companion object {
        private const val EVENT_TYPE = "Event type :"
        private const val LIMIT_HEIGHT = "Limit height :"
        private const val DESCRIPTION = "Description :"
        private const val COUNT_DOWN = "Count Down :"
    }

    private val flySafeVm: FlySafeVM by viewModels()
    private val notificationBuilder = StringBuilder()
    private var notificationCount = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_fly_safe_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flySafeVm.initListener()
        flySafeVm.flySafeWarningInformation.observe(viewLifecycleOwner) {
            showFlySafeNotification { builder ->
                builder.append(EVENT_TYPE).append(it.event).append("\n")
                builder.append(LIMIT_HEIGHT).append(it.heightLimit).append("\n")
                builder.append(DESCRIPTION).append(it.description).append("\n")
            }
        }
        flySafeVm.flySafeSeriousWarningInformation.observe(viewLifecycleOwner) {
            showFlySafeNotification { builder ->
                builder.append(EVENT_TYPE).append(it.event).append("\n")
                builder.append(LIMIT_HEIGHT).append(it.heightLimit).append("\n")
                builder.append(COUNT_DOWN).append(it.countdown).append("\n")
                builder.append(DESCRIPTION).append(it.description).append("\n")
            }
        }
        flySafeVm.flySafeReturnToHomeInformation.observe(viewLifecycleOwner) {
            showFlySafeNotification { builder ->
                builder.append(EVENT_TYPE).append(it.event).append("\n")
                builder.append(DESCRIPTION).append(it.description).append("\n")
            }
        }
        flySafeVm.flySafeTipInformation.observe(viewLifecycleOwner) {
            showFlySafeNotification { builder ->
                builder.append(EVENT_TYPE).append(it.event).append("\n")
                builder.append(LIMIT_HEIGHT).append(it.heightLimit).append("\n")
                builder.append(DESCRIPTION).append(it.description).append("\n")
            }
        }
        flySafeVm.flyZoneInformation.observe(viewLifecycleOwner) {
            showFlyZoneInformation(it)
        }
        flySafeVm.serverFlyZoneLicenseInfo.observe(viewLifecycleOwner) { info ->
            server_fly_zone_license_info.text = getFlyZoneLicenseInfoStr(info) {
                it.append("------------ Server Fly Zone License Info Start------------\n")
            }
        }
        flySafeVm.aircraftFlyZoneLicenseInfo.observe(viewLifecycleOwner) { info ->
            aircraft_fly_zone_license_info.text = getFlyZoneLicenseInfoStr(info) {
                it.append("------------ Aircraft Fly Zone License Info Start------------\n")
            }

        }
        flySafeVm.toastResult?.observe(viewLifecycleOwner) { result ->
            result?.msg?.let {
                lte_toast.text = it
            }
        }
        initBtnClickListener()
        createMapView(savedInstanceState)
        flySafeVm.pullFlyZoneLicensesFromAircraft()
    }

    private fun createMapView(savedInstanceState: Bundle?) {
        val onMapReadyListener = MapWidget.OnMapReadyListener { map ->
            map.setMapType(DJIMap.MapType.NORMAL)
        }
        val useAmap = AreaCodeManager.getInstance().areaCode.areaCodeEnum == AreaCode.CHINA
        if (useAmap) {
            map_widget.initAMap(onMapReadyListener)
        } else {
            map_widget.initMapLibreMap(BuildConfig.MAPLIBRE_TOKEN, onMapReadyListener)
        }
        map_widget.onCreate(savedInstanceState) //需要再init后调用否则Amap无法显示
    }

    override fun onPause() {
        super.onPause()
        map_widget.onPause()
    }

    override fun onResume() {
        super.onResume()
        map_widget.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        map_widget.onDestroy()
    }

    private fun initBtnClickListener() {
        btn_hide_or_show_all_fly_zone_info.setOnClickListener {
            if (lte_text.isVisible) {
                lte_text.visibility = View.INVISIBLE
                lte_fly_zone_text.visibility = View.INVISIBLE
            } else {
                lte_text.visibility = View.VISIBLE
                lte_fly_zone_text.visibility = View.VISIBLE
            }
        }
        btn_get_fly_zones_in_surrounding_area.setOnClickListener {
            val location = flySafeVm.getAircraftLocation()
            KeyValueDialogUtil.showInputDialog(
                activity, "(Latitude,Longitude)",
                location.latitude.toString() + "," + location.longitude.toString(), "", false
            ) {
                it?.split(",")?.apply {
                    if (this.size != 2 && this[0].toDoubleOrNull() == null && this[1].toDoubleOrNull() == null) {
                        ToastUtils.showToast("Value Parse Error")
                        return@showInputDialog
                    }
                    flySafeVm.getFlyZonesInSurroundingArea(LocationCoordinate2D(this[0].toDoubleOrNull(), this[1].toDoubleOrNull()))
                }
            }
        }
        btn_download_fly_zone_licenses_from_server.setOnClickListener {
            flySafeVm.downloadFlyZoneLicensesFromServer()
        }
        btn_push_fly_zone_licenses_to_aircraft.setOnClickListener {
            flySafeVm.pushFlyZoneLicensesToAircraft()
        }
        btn_pull_fly_zone_licenses_from_aircraft.setOnClickListener {
            flySafeVm.pullFlyZoneLicensesFromAircraft()
        }
        btn_delete_fly_zone_licenses_from_aircraft.setOnClickListener {
            flySafeVm.deleteFlyZoneLicensesFromAircraft()
        }
        btn_set_fly_zone_licenses_enabled.setOnClickListener {
            val info = flySafeVm.aircraftFlyZoneLicenseInfo.value
            if (info == null || info.isEmpty()) {
                ToastUtils.showToast("Please download licenses and push to aircraft first.")
                return@setOnClickListener
            }
            info.let { licenses ->
                val enables = arrayListOf(true, false)
                val licenseIDs = arrayListOf<Int>().apply {
                    licenses.forEach {
                        this.add(it.licenseId)
                    }
                }
                initPopupNumberPicker(Helper.makeList(licenseIDs), Helper.makeList(enables)) {
                    flySafeVm.setFlyZoneLicensesEnabled(licenses[indexChosen[0]], enables[indexChosen[1]])
                    resetIndex()
                }
                return@let
            }
        }
        btn_unlock_authorization_fly_zone.setOnClickListener {
            KeyValueDialogUtil.showInputDialog(
                activity, "(FlyZoneID)",
                "1", "", false
            ) {
                it?.apply {
                    if (this.toIntOrNull() == null) {
                        ToastUtils.showToast("Value Parse Error")
                        return@showInputDialog
                    }
                    flySafeVm.unlockAuthorizationFlyZone(this.toInt())
                }
            }
        }
        btn_unlock_all_enhanced_warning_fly_zone.setOnClickListener {
            flySafeVm.unlockAllEnhancedWarningFlyZone()
        }
    }

    private fun showFlySafeNotification(infoExtend: (builder: StringBuilder) -> Unit) {
        if (notificationCount % 4 == 0) {
            notificationCount = 0
            notificationBuilder.delete(0, notificationBuilder.length)
        }
        notificationCount++
        notificationBuilder.append("<-----").append("Notification count :").append(notificationCount).append("----->\n")
        notificationBuilder.append("Time : ").append(Date(System.currentTimeMillis()).toString()).append("\n")
        infoExtend(notificationBuilder)
        fly_safe_notification_msg.text = notificationBuilder
    }

    private fun getFlyZoneLicenseInfoStr(licenseInfo: MutableList<FlyZoneLicenseInfo>, infoPre: (builder: StringBuilder) -> Unit): StringBuilder {
        val sb = StringBuilder()
        infoPre(sb)
        for (info in licenseInfo) {
            sb.append("license id: " + info.licenseId)
            sb.append("\n")
            sb.append("license type: " + info.licenseType)
            sb.append("\n")
            sb.append("sn: " + info.sn)
            sb.append("\n")
            sb.append("description: " + info.description)
            sb.append("\n")
            sb.append("isEnabled: " + info.isEnabled)
            sb.append("\n")
            sb.append("isValid: " + info.isValid)
            sb.append("\n")
            when (info.licenseType) {
                FlySafeLicenseType.GEO_UNLOCK -> {
                    sb.append("flyZoneIDs: " + JsonUtil.toJson(info.flyZoneIDs))
                    sb.append("\n")
                }
                FlySafeLicenseType.CIRCLE_UNLOCK_AREA -> {
                    sb.append("cylinderLatitude: " + info.cylinderLatitude)
                    sb.append("\n")
                    sb.append("cylinderLongitude: " + info.cylinderLongitude)
                    sb.append("\n")
                    sb.append("cylinderRadius: " + info.cylinderRadius)
                    sb.append("\n")
                    sb.append("cylinderHeight: " + info.cylinderHeight)
                    sb.append("\n")
                }
                FlySafeLicenseType.COUNTRY_UNLOCK -> {
                    sb.append("countryId: " + info.countryId)
                    sb.append("\n")
                }
                FlySafeLicenseType.PARAMETER_CONFIGURATION -> {
                    sb.append("limitedHeight: " + info.limitedHeight)
                    sb.append("\n")
                }
                FlySafeLicenseType.PENTAGON_UNLOCK_AREA -> {
                    sb.append("polygonPoints: " + JsonUtil.toJson(info.polygonPoints))
                    sb.append("\n")
                }
                else -> {
                    // do nothing
                }
            }
            sb.append("start time: " + DateUtils.getDateStringFromLong(info.startTimeStamp, "yyyy-MM-dd HH:mm:ss"))
            sb.append("\n")
            sb.append("end time: " + DateUtils.getDateStringFromLong(info.endTimeStamp, "yyyy-MM-dd HH:mm:ss"))
            sb.append("\n")
            sb.append("----------------------------\n")
        }
        return sb
    }

    private fun showFlyZoneInformation(flyZoneInformation: List<FlyZoneInformation>) {
        val sb = StringBuffer()
        sb.append("\n")
        sb.append("-- Fly Zone Information --\n")
        for (info in flyZoneInformation) {
            sb.append("FlyZoneId: ").append(info.flyZoneID).append("\n")
            sb.append("Name: ").append(info.name).append("\n")
            sb.append("FlyZoneType: ").append(info.flyZoneType.name).append("\n")
            sb.append("FLyZoneShape: ").append(info.shape).append("\n")
            sb.append("FLyCategory: ").append(info.category).append("\n")
            sb.append("Latitude: ").append(info.circleCenter.latitude).append("\n")
            sb.append("Longitude: ").append(info.circleCenter.longitude).append("\n")
            sb.append("------------------------\n")
            sb.append("\n")
        }
        fly_zone_info.text = sb
    }
}