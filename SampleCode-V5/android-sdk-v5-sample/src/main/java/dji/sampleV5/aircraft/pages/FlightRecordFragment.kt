package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.models.FlightRecordVM
import dji.v5.utils.common.DiskUtil
import kotlinx.android.synthetic.main.frag_flight_record_page.*

/**
 * ClassName : dji.sampleV5.aircraft.pages.FlightRecordFragment
 * Description : FlightRecordFragment
 * Author : daniel.chen
 * CreateDate : 2021/7/15 11:13 上午
 * Copyright : ©2021 DJI All Rights Reserved.
 */
class FlightRecordFragment : DJIFragment() {
    private val flightRecordVM: FlightRecordVM by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_flight_record_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initBtnListsner()
    }

    private fun initView() {
        record_tv?.text = flightRecordVM.getFlightLogPath()
        clog_path_tv?.text = flightRecordVM.getFlyClogPath()
    }

    fun initBtnListsner() {
        btn_open_flight_record_path.setOnClickListener {
            var flightRecordPath = flightRecordVM.getFlightLogPath()
            if (!flightRecordPath.contains(DiskUtil.SDCARD_ROOT )){
                return@setOnClickListener
            }
            var  uriPath = flightRecordPath.substring(DiskUtil.SDCARD_ROOT.length + 1 , flightRecordPath.length - 1).replace("/" , "%2f")
            flightRecordVM.openFileChooser(uriPath , activity)

        }

        btn_get_flight_compressed_log_path.setOnClickListener {
            var flyclogPath = flightRecordVM.getFlyClogPath()
            if (!flyclogPath.contains(DiskUtil.SDCARD_ROOT )){
                return@setOnClickListener
            }
            var uriPath =
                flyclogPath.substring(DiskUtil.SDCARD_ROOT.length + 1, flyclogPath.length - 1)
                    .replace("/", "%2f")
            flightRecordVM.openFileChooser(uriPath, activity)
        }
    }
}