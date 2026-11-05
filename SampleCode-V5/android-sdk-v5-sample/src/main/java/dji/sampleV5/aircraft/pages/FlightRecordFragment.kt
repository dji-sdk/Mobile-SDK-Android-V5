package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import dji.sampleV5.aircraft.databinding.FragFlightRecordPageBinding
import dji.sampleV5.aircraft.models.FlightRecordVM
import dji.v5.ux.util.Helper
import dji.v5.utils.common.DiskUtil

/**
 * ClassName : dji.sampleV5.aircraft.pages.FlightRecordFragment
 * Description : FlightRecordFragment
 * Author : daniel.chen
 * CreateDate : 2021/7/15 11:13 上午
 * Copyright : ©2021 DJI All Rights Reserved.
 */
class FlightRecordFragment : DJIFragment() {
    private val flightRecordVM: FlightRecordVM by activityViewModels()
    private var binding: FragFlightRecordPageBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragFlightRecordPageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initBtnListener()
    }

    private fun initView() {
        binding?.recordTv?.text = flightRecordVM.getFlightLogPath()
        binding?.clogPathTv?.text = flightRecordVM.getFlyClogPath()
    }

    private fun initBtnListener() {
        binding?.btnOpenFlightRecordPath?.setOnClickListener {
            val flightRecordPath = flightRecordVM.getFlightLogPath()
            if (!flightRecordPath.contains(DiskUtil.SDCARD_ROOT)) {
                return@setOnClickListener
            }
            val uriPath = flightRecordPath.substring(DiskUtil.SDCARD_ROOT.length + 1, flightRecordPath.length - 1).replace("/", "%2f")
            Helper.openFileChooser(uriPath, activity)

        }
        binding?.btnGetFlightCompressedLogPath?.setOnClickListener {
            val flyclogPath = flightRecordVM.getFlyClogPath()
            if (!flyclogPath.contains(DiskUtil.SDCARD_ROOT)) {
                return@setOnClickListener
            }
            val uriPath =
                flyclogPath.substring(DiskUtil.SDCARD_ROOT.length + 1, flyclogPath.length - 1)
                    .replace("/", "%2f")
            Helper.openFileChooser(uriPath, activity)
        }
    }
}