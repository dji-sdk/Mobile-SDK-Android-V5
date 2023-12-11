package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.models.UASUAVM
import kotlinx.android.synthetic.main.frag_uas_us_page.*

/**
 * Description :美国无人机远程识别示例
 *
 * @author: Byte.Cai
 *  date : 2022/8/2
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class UASUSFragment : DJIFragment() {
    private val uas: UASUAVM by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_uas_us_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uas.addRemoteIdStatusListener()

        uas.uasRemoteIDStatus.observe(viewLifecycleOwner) {
            tv_ua_rid_status.text = "RemoteIdStatus:${it}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        uas.clearRemoteIdStatusListener()
    }
}