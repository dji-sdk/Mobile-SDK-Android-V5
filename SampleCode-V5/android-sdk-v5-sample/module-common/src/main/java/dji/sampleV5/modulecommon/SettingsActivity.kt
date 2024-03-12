package dji.sampleV5.modulecommon

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dji.sampleV5.modulecommon.models.BaseMainActivityVm
import dji.sampleV5.modulecommon.models.MSDKInfoVm
import dji.sampleV5.modulecommon.util.ITuskServiceCallback
import dji.v5.utils.common.LogUtils
import kotlinx.android.synthetic.main.activity_main.reconnect_ws
import kotlinx.android.synthetic.main.activity_settings.default_layout_button
import kotlinx.android.synthetic.main.activity_settings.live_stream_shortcut
import kotlinx.android.synthetic.main.activity_settings.testing_tool_button
import kotlinx.android.synthetic.main.activity_settings.widget_list_button

class SettingsActivity : AppCompatActivity(), ITuskServiceCallback {
//    private val baseMainActivityVm: BaseMainActivityVm by viewModels()
//    protected val msdkInfoVm: MSDKInfoVm by viewModels()
//    private val handler: Handler = Handler(Looper.getMainLooper())
    private val baseMainActivityVm: BaseMainActivityVm by viewModels()
//    abstract fun prepareTestingToolsActivity()
    public override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("debug", "got here 1")
        super.onCreate(savedInstanceState)
        Log.d("debug", "got here 2")
        setContentView(R.layout.activity_settings)
        Log.d("debug", "got here 3")
        if(baseMainActivityVm.registerState.value == "val") {
            // change
        }
//        reconnect_ws.setOnClickListener {
//            Log.d("TuskService", "Button Pressed?")
//            callReconnectWebsocket()
//        }
//        prepareTestingToolsActivity()
    }

    companion object {
        val TAG = LogUtils.getTag("SettingsActivity")
    }


    fun <T> enableDefaultLayout(cl: Class<T>) {
        enableShowCaseButton(default_layout_button, cl)
    }

    fun <T> enableWidgetList(cl: Class<T>) {
        enableShowCaseButton(widget_list_button, cl)
    }

    fun <T> enableTestingTools(cl: Class<T>) {
        enableShowCaseButton(testing_tool_button, cl)
    }

    fun <T> enableLiveStreamShortcut(cl: Class<T>){
        enableShowCaseButton(live_stream_shortcut, cl)
    }

    private fun <T> enableShowCaseButton(view: View, cl: Class<T>) {
        view.isEnabled = true
        view.setOnClickListener {
            Intent(this, cl).also {
                startActivity(it)
            }
        }
    }

    override fun callReconnectWebsocket() {
        TODO("Not yet implemented")
    }

    override fun callSetIP(ip: String) {
        TODO("Not yet implemented")
    }
    public override fun onDestroy() {
        super.onDestroy()
    }
}