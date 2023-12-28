package dji.sampleV5.aircraft

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dji.sampleV5.aircraft.views.MSDKInfoFragment

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/12/16
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class AircraftMSDKInfoFragment : MSDKInfoFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_aircraft_main_title, container, false)
    }
}