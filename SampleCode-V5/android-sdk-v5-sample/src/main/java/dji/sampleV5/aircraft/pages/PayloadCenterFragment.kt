package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.util.Helper
import dji.v5.manager.aircraft.payload.PayloadIndexType
import kotlinx.android.synthetic.main.frag_payload_center_page.*

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2022/12/2
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class PayloadCenterFragment : DJIFragment() {
    companion object {
        const val KEY_PAYLOAD_INDEX_TYPE = "payload_index_type"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_payload_center_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bt_open_payload_data_page.setOnClickListener {
            val values = PayloadIndexType.values()
            initPopupNumberPicker(Helper.makeList(values)) {
                val bundle = Bundle()
                bundle.putInt(KEY_PAYLOAD_INDEX_TYPE, values[indexChosen[0]].value())
                Navigation.findNavController(it).navigate(R.id.action_open_payload_data_page, bundle)
            }
        }

        bt_open_payload_widget_page.setOnClickListener {
            val values = PayloadIndexType.values()
            initPopupNumberPicker(Helper.makeList(values)) {
                val bundle = Bundle()
                bundle.putInt(KEY_PAYLOAD_INDEX_TYPE, values[indexChosen[0]].value())
                Navigation.findNavController(it).navigate(R.id.action_open_payload_widget_page, bundle)

            }
        }


    }
}