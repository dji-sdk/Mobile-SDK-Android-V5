package dji.sampleV5.moduleaircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import dji.sampleV5.modulecommon.pages.DJIFragment
import dji.sampleV5.modulecommon.util.ToastUtils
import dji.sampleV5.moduleaircraft.R
import dji.sampleV5.moduleaircraft.models.FlightRecordVM
import kotlinx.android.synthetic.main.frag_flight_record_page.*

/**
 * ClassName : dji.sampleV5.moduleaircraft.pages.FlightRecordFragment
 * Description : FlightRecordFragment
 * Author : daniel.chen
 * CreateDate : 2021/7/15 11:13 上午
 * Copyright : ©2021 DJI All Rights Reserved.
 */
class FlightRecordFragment :DJIFragment(){
    private val flightRecordVM:FlightRecordVM by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_flight_record_page,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBtnListsner()
    }

    fun initBtnListsner() {
        btn_get_flight_record_path.setOnClickListener {
            ToastUtils.showToast(context, flightRecordVM.getFlightLogPath())
        }

        btn_get_flight_compressed_log_path.setOnClickListener {
            ToastUtils.showToast(context, flightRecordVM.getFlyClogPath())
        }
    }
}