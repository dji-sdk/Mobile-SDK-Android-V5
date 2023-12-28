package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dji.sampleV5.aircraft.R

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/5/11
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
class WaypointFragment : DJIFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_waypoint_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}