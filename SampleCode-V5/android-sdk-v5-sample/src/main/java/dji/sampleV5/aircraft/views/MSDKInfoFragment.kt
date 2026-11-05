package dji.sampleV5.aircraft.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.pages.DJIFragment

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/5/11
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class MSDKInfoFragment : DJIFragment() {

    private lateinit var msdkInfoTextMain: TextView
    private lateinit var msdkInfoTextSecond: TextView
    private lateinit var titleTextView: TextView
    private lateinit var returnBtn: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.frag_main_title, container, false)
        initView(v)
        return v
    }

    fun initView(v: View) {
        msdkInfoTextMain = v.findViewById(R.id.msdk_info_text_main)
        msdkInfoTextSecond = v.findViewById(R.id.msdk_info_text_second)
        titleTextView = v.findViewById(R.id.title_text_view)
        returnBtn = v.findViewById(R.id.return_btn)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initMSDKInfo()
        initListener()
    }

    private fun initMSDKInfo() {
        msdkInfoVm.msdkInfo.observe(viewLifecycleOwner) {
            it?.let {
                val mainInfo = "MSDK Info:[Ver:${it.SDKVersion} ${it.pluginVersion} BuildVer:${it.buildVer} Debug:${it.isDebug} LinkMode:${it.sdkLinkMode} LDMLicenseLoaded:${it.isLDMLicenseLoaded} ]"
                msdkInfoTextMain.text = mainInfo
                val secondInfo = "Device:${it.productType} | Network:${it.networkInfo} | CountryCode:${it.countryCode} | FirmwareVer:${it.firmwareVer} | LDMEnabled:${it.isLDMEnabled}"
                msdkInfoTextSecond.text = secondInfo
            }
        }
        msdkInfoVm.refreshMSDKInfo()

        msdkInfoVm.mainTitle.observe(viewLifecycleOwner) {
            it?.let {
                titleTextView.text = it
            }
        }
    }

    private fun initListener() {
        returnBtn.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
}