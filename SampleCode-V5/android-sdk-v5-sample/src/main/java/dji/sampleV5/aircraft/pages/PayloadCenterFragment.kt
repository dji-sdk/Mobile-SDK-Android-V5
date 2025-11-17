package dji.sampleV5.aircraft.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.databinding.FragPayloadCenterPageBinding
import dji.sampleV5.aircraft.util.Helper
import dji.v5.manager.aircraft.payload.PayloadIndexType

/**
 * Description :
 *
 * @author: Byte.Cai
 *  date : 2022/12/2
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class PayloadCenterFragment : DJIFragment() {

    private var binding: FragPayloadCenterPageBinding? = null

    companion object {
        const val KEY_PAYLOAD_INDEX_TYPE = "payload_index_type"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragPayloadCenterPageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.btOpenPayloadDataPage?.setOnClickListener {
            val values = PayloadIndexType.allValues
            initPopupNumberPicker(Helper.makeList(values)) {
                val bundle = Bundle()
                bundle.putInt(KEY_PAYLOAD_INDEX_TYPE, values[indexChosen[0]].value())
                Navigation.findNavController(it).navigate(R.id.action_open_payload_data_page, bundle)
            }
        }

        binding?.btOpenPayloadWidgetPage?.setOnClickListener {
            val values = PayloadIndexType.allValues
            initPopupNumberPicker(Helper.makeList(values)) {
                val bundle = Bundle()
                bundle.putInt(KEY_PAYLOAD_INDEX_TYPE, values[indexChosen[0]].value())
                Navigation.findNavController(it).navigate(R.id.action_open_payload_widget_page, bundle)

            }
        }

        binding?.btOpenIntelligentBoxPage?.setOnClickListener {
            val values = PayloadIndexType.allValues
            initPopupNumberPicker(Helper.makeList(values)) {
                val bundle = Bundle()
                bundle.putInt(KEY_PAYLOAD_INDEX_TYPE, values[indexChosen[0]].value())
                Navigation.findNavController(it).navigate(R.id.action_open_intelligent_box_page, bundle)

            }
        }
    }
}