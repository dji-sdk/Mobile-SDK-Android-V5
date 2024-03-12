package dji.sampleV5.aircraft

import android.provider.Settings
import android.util.Log
import android.widget.Toast
import dji.sampleV5.aircraft.control.PachKeyManager
import dji.sampleV5.aircraft.defaultlayout.DefaultLayoutActivity
import dji.sampleV5.aircraft.telemetry.TuskServiceWebsocket
import dji.sampleV5.modulecommon.DJIMainActivity
import dji.sampleV5.modulecommon.SettingsActivity
import dji.v5.common.utils.GeoidManager
import dji.v5.ux.core.communication.DefaultGlobalPreferences
import dji.v5.ux.core.communication.GlobalPreferencesManager
import dji.v5.ux.core.util.UxSharedPreferencesUtil
import dji.v5.ux.sample.showcase.widgetlist.WidgetsActivity

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/2/14
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
class DJIAircraftMainActivity : DJIMainActivity() {
    override fun prepareUxActivity() {
        UxSharedPreferencesUtil.initialize(this)
        GlobalPreferencesManager.initialize(DefaultGlobalPreferences(this))
        GeoidManager.getInstance().init(this)

        enableDefaultLayout(DefaultLayoutActivity::class.java) // important
        enableWidgetList(WidgetsActivity::class.java)

        val TuskManager = PachKeyManager()
        TuskManager.runTesting()
//        prepareConfigurationTools()
//


    }

    override fun prepareTestingToolsActivity() {
        enableTestingTools(AircraftTestingToolsActivity::class.java)
    }

//    override fun loadSettingsActivity() {
//        enableSettings(SettingsActivity::class.java)
//    }

//    override fun loadWidgetActivity() {
//        enableWidgetList(WidgetsActivity::class.java)
//    }

    override fun callReconnectWebsocket() {
        Log.d("TuskService", "Callback called successfully")
        val ws = TuskServiceWebsocket()
        Toast.makeText(applicationContext, "Reconnecting WS with IP:\n ${ws.getCurrentIP()}", Toast.LENGTH_LONG).show()
        ws.connectWebSocket()
    }

    override fun callSetIP(ip: String) {
        Log.d("TuskService", "Callback callSetIP() called with value $ip")
        val ws = TuskServiceWebsocket()
        ws.setCurrentIP(ip)
    }

//    fun prepareConfigurationTools(){
//        enableLiveStreamShortcut(LiveStreamFragment::class.java)
//    }
}

