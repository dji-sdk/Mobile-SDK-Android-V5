package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import dji.sampleV5.aircraft.R
import kotlinx.android.synthetic.main.frag_mop_center_page.*

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2023/2/22
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class MOPCenterFragment : DJIFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_mop_center_page,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bt_open_mop_download_page.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_open_down_page)
        }

        bt_open_mop_interface_page.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_open_mop_interface_page)

        }

    }
}