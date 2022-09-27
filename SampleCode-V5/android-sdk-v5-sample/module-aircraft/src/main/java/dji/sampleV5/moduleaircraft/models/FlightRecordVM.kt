package dji.sampleV5.moduleaircraft.models

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import dji.sampleV5.modulecommon.models.DJIViewModel
import dji.v5.manager.aircraft.flightrecord.FlightLogManager

/**
 * Description : FLightReecordVM
 * Author : daniel.chen
 * CreateDate : 2021/7/15 10:51 上午
 * Copyright : ©2021 DJI All Rights Reserved.
 */
class FlightRecordVM : DJIViewModel() {
    fun getFlightLogPath(): String {
        return FlightLogManager.getInstance().flightRecordPath
    }

    fun getFlyClogPath(): String {
        return FlightLogManager.getInstance().flyClogPath
    }

    fun openFileChooser( path:String , context : Context?) {

        val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:$path")
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context?.startActivity(
            Intent.createChooser(intent, "Log Path"))
    }
}