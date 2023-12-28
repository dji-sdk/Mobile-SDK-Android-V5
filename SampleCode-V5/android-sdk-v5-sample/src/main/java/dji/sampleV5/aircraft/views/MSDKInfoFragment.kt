package dji.sampleV5.aircraft.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.pages.DJIFragment
import kotlinx.android.synthetic.main.frag_main_title.msdk_info_text_main
import kotlinx.android.synthetic.main.frag_main_title.msdk_info_text_second
import kotlinx.android.synthetic.main.view_title_bar.return_btn
import kotlinx.android.synthetic.main.view_title_bar.title_text_view

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/5/11
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class MSDKInfoFragment : DJIFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.frag_main_title, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initMSDKInfo()
        initListener()
    }

    private fun initMSDKInfo() {
        msdkInfoVm.msdkInfo.observe(viewLifecycleOwner) {
            it?.let {
                val mainInfo = "MSDK Info:[Ver:${it.SDKVersion} BuildVer:${it.buildVer} Debug:${it.isDebug} ProductCategory:${it.packageProductCategory} LDMLicenseLoaded:${it.isLDMLicenseLoaded} ]"
                msdk_info_text_main.text = mainInfo
                val secondInfo = "Device:${it.productType} | Network:${it.networkInfo} | CountryCode:${it.countryCode} | FirmwareVer:${it.firmwareVer} | LDMEnabled:${it.isLDMEnabled}"
                msdk_info_text_second.text = secondInfo
            }
        }
        msdkInfoVm.refreshMSDKInfo()

        msdkInfoVm.mainTitle.observe(viewLifecycleOwner) {
            it?.let {
                title_text_view.text = it
            }
        }
    }

    private fun initListener() {
        return_btn.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
}