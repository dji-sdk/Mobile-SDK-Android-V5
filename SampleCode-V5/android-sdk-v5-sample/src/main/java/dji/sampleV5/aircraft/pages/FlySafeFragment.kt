package dji.sampleV5.aircraft.pages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dji.sampleV5.aircraft.databinding.FragFlySafePageBinding
import dji.sampleV5.aircraft.keyvalue.KeyValueDialogUtil
import dji.sampleV5.aircraft.models.FlySafeVM
import dji.sampleV5.aircraft.util.Helper
import dji.sampleV5.aircraft.util.ToastUtils
import dji.sdk.keyvalue.value.common.LocationCoordinate2D
import dji.v5.manager.aircraft.flysafe.FlySafeDatabaseComponent
import dji.v5.manager.aircraft.flysafe.FlySafeDatabaseUpgradeMode
import dji.v5.manager.aircraft.flysafe.info.FlySafeLicenseType
import dji.v5.manager.aircraft.flysafe.info.FlyZoneInformation
import dji.v5.manager.aircraft.flysafe.info.FlyZoneLicenseInfo
import dji.v5.utils.common.DateUtils
import dji.v5.utils.common.DocumentUtil
import dji.v5.utils.common.JsonUtil
import dji.v5.ux.accessory.DescSpinnerCell
import dji.v5.ux.map.MapWidget
import dji.v5.ux.mapkit.core.maps.DJIMap
import java.util.Date

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
        private const val CE_INFO = "CE INFO :"
    }

    private var binding: FragFlySafePageBinding? = null
    private val flySafeVm: FlySafeVM by viewModels()
    private val notificationBuilder = StringBuilder()
    private var notificationCount = 0

    private var lancher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result?.apply {
            val uri = data?.data
            val path = DocumentUtil.getPath(requireContext(), uri);
            flySafeVm.pushFlySafeDynamicDatabaseToAircraftAndApp(path)
            ToastUtils.showToast("User Database :$path")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragFlySafePageBinding.inflate(inflater, container, false)
        return binding?.root
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
                builder.append(CE_INFO).append(":\n")
                it.flyZoneInformation.forEach {
                    builder.append(it.lowerLimit).append("\n")
                    builder.append(it.upperLimit).append("\n")
                }
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
            binding?.serverFlyZoneLicenseInfo?.text = getFlyZoneLicenseInfoStr(info) {
                it.append("------------ Server Fly Zone License Info Start------------\n")
            }
        }
        flySafeVm.aircraftFlyZoneLicenseInfo.observe(viewLifecycleOwner) { info ->
            binding?.aircraftFlyZoneLicenseInfo?.text = getFlyZoneLicenseInfoStr(info) {
                it.append("------------ Aircraft Fly Zone License Info Start------------\n")
            }

        }
        flySafeVm.toastResult?.observe(viewLifecycleOwner) { result ->
            result?.msg?.let {
                binding?.lteToast?.text = it
            }
        }
        flySafeVm.importAndSyncState.observe(viewLifecycleOwner) {
            binding?.sysnAndImportInfo?.text = "Progress:${it.importAndSyncProgress} : ${it.error?.description()}"
        }

        flySafeVm.dataBaseInfo.observe(viewLifecycleOwner) {
            if (it.component == FlySafeDatabaseComponent.MSDK) {
                binding?.databaseInfoSdk?.text = "MSDK:Mode:${it.upgradeMode} ${it.dataBaseName} time:${it.dataBaseTime} size:${it.dataBaseSize} "
            } else {
                binding?.databaseInfoAircraft?.text = "Aircraft:Mode:${it.upgradeMode} time:${it.dataBaseTime}"
            }

        }

        flySafeVm.dataUpgradeState.observe(viewLifecycleOwner) {
            binding?.updateState?.text = "Upgrade State:${it.name}"
        }


        initBtnClickListener()
        createMapView(savedInstanceState)
        flySafeVm.pullFlyZoneLicensesFromAircraft()
    }

    private fun createMapView(savedInstanceState: Bundle?) {
        val onMapReadyListener = MapWidget.OnMapReadyListener { map ->
            map.setMapType(DJIMap.MapType.NORMAL)
        }

        binding?.mapWidget?.initMapLibreMap(requireContext(), onMapReadyListener)
        binding?.mapWidget?.onCreate(savedInstanceState) //需要再init后调用否则Amap无法显示
    }

    override fun onPause() {
        super.onPause()
        binding?.mapWidget?.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding?.mapWidget?.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.mapWidget?.onDestroy()
    }

    private fun initBtnClickListener() {
        binding?.btnHideOrShowAllFlyZoneInfo?.setOnClickListener {
            if (binding?.lteText?.isVisible == true) {
                binding?.lteText?.visibility = View.INVISIBLE
                binding?.lteFlyZoneText?.visibility = View.INVISIBLE
            } else {
                binding?.lteText?.visibility = View.VISIBLE
                binding?.lteFlyZoneText?.visibility = View.VISIBLE
            }
        }
        binding?.btnGetFlyZonesInSurroundingArea?.setOnClickListener {
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
        binding?.btnDownloadFlyZoneLicensesFromServer?.setOnClickListener {
            flySafeVm.downloadFlyZoneLicensesFromServer()
        }
        binding?.btnPushFlyZoneLicensesToAircraft?.setOnClickListener {
            flySafeVm.pushFlyZoneLicensesToAircraft()
        }
        binding?.btnPullFlyZoneLicensesFromAircraft?.setOnClickListener {
            flySafeVm.pullFlyZoneLicensesFromAircraft()
        }
        binding?.btnDeleteFlyZoneLicensesFromAircraft?.setOnClickListener {
            flySafeVm.deleteFlyZoneLicensesFromAircraft()
        }
        binding?.btnSetFlyZoneLicensesEnabled?.setOnClickListener {
            val info = flySafeVm.aircraftFlyZoneLicenseInfo.value
            if (info.isNullOrEmpty()) {
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
        binding?.btnUnlockAuthorizationFlyZone?.setOnClickListener {
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
        binding?.btnUnlockAllEnhancedWarningFlyZone?.setOnClickListener {
            flySafeVm.unlockAllEnhancedWarningFlyZone()
        }

        binding?.btnImport?.setOnClickListener {
            openFileFolder()
        }

        binding?.btnTestImportAvailable?.setOnClickListener {
            flySafeVm.pushAvailableTestFlySafeDynamicDatabaseToApp()
        }

        binding?.btnTestImportUnavailable?.setOnClickListener {
            flySafeVm.pushUnAvailableTestFlySafeDynamicDatabaseToApp()
        }

        binding?.btnSync?.setOnClickListener {
            flySafeVm.syncFlySafeMSDKDatabaseToAircraft()
        }

        binding?.importMode?.addOnItemSelectedListener(object :
            DescSpinnerCell.OnItemSelectedListener {
            override fun onItemSelected(position: Int) {
                flySafeVm.setFlySafeDynamicDatabaseUpgradeMode(FlySafeDatabaseUpgradeMode.find(position))
            }
        })
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
        binding?.flySafeNotificationMsg?.text = notificationBuilder
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

                FlySafeLicenseType.RID_UNLOCK -> {
                    sb.append("ridUnlockType: " + info.ridUnlockType)
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

    private fun showFlyZoneInformation(flyZoneInformation: MutableList<FlyZoneInformation>) {
//        flySafeVm.sortFlyZonesByDistanceFromAircraft(flyZoneInformation)
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
            sb.append("LowerLimit:").append(info.lowerLimit).append("\n")
            sb.append("UpperLimit:").append(info.upperLimit).append("\n")
            sb.append("------------------------\n")
            sb.append("\n")
        }
        binding?.flyZoneInfo?.text = sb
    }

    private fun openFileFolder() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        lancher.launch(intent)
    }
}