package dji.sampleV5.aircraft.telemetry
import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okio.ByteString
import org.json.JSONObject

class TuskServiceWebsocket {
    private val client: OkHttpClient = OkHttpClient()
    private lateinit var webSocket: WebSocket
    private val gson: Gson = Gson()

    // Establish WebSocket connection
    fun connectWebSocket() {
        val request = Request.Builder().url("ws://192.168.0.100:8084").build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("TuskService", "WebSocket connection opened")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleWebSocketMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Do we need this?
                Log.d("TuskService", "Received binary message: " + bytes.hex())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("TuskService", "WebSocket closing: $code $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("TuskService", "WebSocket connection failure: ${t.message}")
            }
        })
    }

    // Handle received WebSocket message
    private fun handleWebSocketMessage(message: String) {
        try {
            val jsonObject = JSONObject(message)
            val action = jsonObject.optString("action")
            val args = jsonObject.opt("args")

            when (action) {
                "AllAircraftStatus" -> handleGetAllAircraftStatus(args)
                "NewControllerStatus" -> handleNewControllerStatus(args)
                else -> Log.d("TuskService", "Unknown action: $action")
            }
        } catch (e: Exception) {
            Log.e("TuskService", "Failed to parse WebSocket message: ${e.message}")
        }
    }

    // Send WebSocket message
    private fun sendWebSocketMessage(action: String, args: Any) {
        Log.v("TuskService", "Sending Websocket Message $action $args")
        val data = "{\"action\":\"$action\",\"args\":$args}"
        webSocket.send(data)
    }

    // Get all aircraft statuses
    suspend fun getAllAircraftStatus() {
        sendWebSocketMessage("GetAllAircraftStatus", "")
    }

    // Post aircraft state
    fun postState(state: TuskAircraftState) {
        sendWebSocketMessage("NewAircraftState", gson.toJson(state))
    }

    // Post aircraft status
    fun postStatus(status: TuskAircraftStatus) {
        sendWebSocketMessage("NewAircraftStatus", gson.toJson(status))
    }

    // Post controller status
    fun postControlStatus(status: TuskControllerStatus) {
        sendWebSocketMessage("NewControllerStatus", gson.toJson(status))
    }

    // Reponse Handler:
    private fun handleNewControllerStatus(args: Any) {
        // Handle the action here
        Log.d("TuskService", "Handling NewControllerStatus action")
    }

    private fun handleGetAllAircraftStatus(args: Any) {
        // Handle the action here
        Log.d("TuskService", "Handling GetAllAircraftStatus action")
    }

    fun getActions() {
        TODO("Not yet implemented")
    }

    fun postControllerStatus(status: TuskControllerStatus) {

    }
}