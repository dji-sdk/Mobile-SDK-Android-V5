package dji.sampleV5.aircraft.telemetry
import android.util.Log
import com.google.gson.Gson
import dji.sampleV5.modulecommon.util.ITuskServiceCallback
import okhttp3.*
import okio.ByteString
import org.json.JSONObject

class TuskServiceWebsocket{
    private val client: OkHttpClient = OkHttpClient()
    private lateinit var webSocket: WebSocket
    private val gson: Gson = Gson()
// Set of Variables that system is expected to receive from server
    var maxVelocity: Double = 6.0 // m/s
    var waypointList =  listOf<Coordinate>()
    var nextWaypoint = Coordinate(0.0, 0.0, 0.0)
    var isGatherAction = false
    var isAlertAction = false
    var isStayAction = false
    var nextWaypointID = 0
    var plannerAction = "idle"
    var dwellTime = 0

    // Establish WebSocket connection
    private val defaultIP: String = "ws://192.168.0.102:8084"
    private var currentIP: String = defaultIP
    private var connectionStatus: Boolean = false

    fun setConnectionStatus (status: Boolean) {
        connectionStatus = status
    }

    fun getConnectionStatus(): Boolean {
        return connectionStatus
    }
    fun getCurrentIP(): String {
        return currentIP
    }
    fun setCurrentIP(ip: String) {
        currentIP = ip
    }
    fun connectWebSocket() {
//        val request = Request.Builder().url("ws://192.168.20.169:8084").build()
        val request = Request.Builder().url(currentIP).build()
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
            Log.v("TuskServiceAction", "Received WebSocket message: $message")
            val jsonObject = JSONObject(message)
            val action = jsonObject.optString("action")
            val args = jsonObject.opt("args")
            when (action) {
                "AllAircraftStatus" -> handleGetAllAircraftStatus(args) //Unused
                "NewControllerStatus" -> handleNewControllerStatus(args) //Unused
                "FollowWaypoints" -> handleWaypointSet(args as JSONObject?)
                "FlightWaypoint" -> handleNewWaypoint(args as JSONObject?)
                "FlightStatus" -> handleFlightStatusUpdate(args as JSONObject?)
                else -> Log.d("TuskService", "Unknown action: $action")
            }
        } catch (e: Exception) {
            Log.e("TuskService", "Failed to parse WebSocket message: ${e.message}")
        }
    }
    // Send WebSocket message
    private fun sendWebSocketMessage(action: String, args: Any) {
//        Log.v("TuskService", "Sending Websocket Message $action $args")
        val data = "{\"action\":\"$action\",\"args\":$args}"
        webSocket.send(data)
    }

    // Post aircraft state
    fun postState(state: TuskAircraftState) {
        sendWebSocketMessage("NewAircraftState", gson.toJson(state))
    }

    // Post aircraft status
    fun postStatus(status: TuskAircraftStatus) {
        sendWebSocketMessage("NewAircraftStatus", gson.toJson(status))
    }

    // Post Autonomy Status
    fun postAutonomyStatus(status: Event) {
        sendWebSocketMessage("NewFlightStatus", gson.toJson(status))
    }

    fun postStreamURL(url: StreamInfo) {
        sendWebSocketMessage("SetStream", gson.toJson(url))
    }

    // Post controller status
    fun postControlStatus(status: TuskControllerStatus) {
        sendWebSocketMessage("NewControllerStatus", gson.toJson(status))
    }

    // Reponse Handler:
    private fun handleNewControllerStatus(args: Any?) {
        // Handle the action here
        Log.d("TuskService", "Handling NewControllerStatus action")
    }

    private fun handleGetAllAircraftStatus(args: Any?) {
        // Handle the action here
        Log.d("TuskService", "Handling GetAllAircraftStatus action")
    }

    private fun handleWaypointSet(args: Any?) {
        // Handle action "FollowWaypoints" with the waypoint list
        try {
            if (args is JSONObject) {
                val flightPathArray = args.optJSONArray("flightPath")
                if (flightPathArray != null) {
                    for (i in 0 until flightPathArray.length()) {
                        val waypointArray = flightPathArray.optJSONArray(i)
                        if (waypointArray != null && waypointArray.length() >= 2) {
                            val lat = waypointArray.optDouble(0)
                            val long = waypointArray.optDouble(1)
                            val alt = waypointArray.optDouble(2)
                            Log.d("WaypointService", "Waypoint $i - Latitude: $lat, Longitude: $long,  Altitude: $alt")
                            // Add the waypoint to the waypoint list
                            waypointList += Coordinate(lat, long, 50.0)
                        }

                    }
                }
            } else {
                Log.d("TuskService", "Invalid args format for FollowWaypoints action")
            }
        } catch (e: Exception) {
            Log.e("TuskService", "Failed to handle FollowWaypoints action: ${e.message}")
        }
    }

    private fun handleNewWaypoint(args: Any?) {
        // Handle action "flightWaypoint" with the next waypoint
        try {
            if (args is JSONObject) {
                val lat = args.getDouble("latitude")
                val long = args.getDouble("longitude")
                val alt = args.getDouble("altitude")
                maxVelocity = args.getDouble("speed")
                maxVelocity *= (10.0 / 36.0)  // Convert from km/h to m/s
                nextWaypointID = args.getInt("waypointID")
                plannerAction = args.getString("plannerAction")
                dwellTime = args.getInt("dwellTime")

                Log.d(
                    "WaypointService",
                    "Next Waypoint - Latitude: $lat, Longitude: $long,  Altitude: $alt, " +
                            "Speed: $maxVelocity, WaypointID: $nextWaypointID, " +
                            "PlannerAction: $plannerAction, DwellTime: $dwellTime"
                )
                nextWaypoint = Coordinate(lat, long, alt)
            }
        } catch (e: Exception) {
            Log.e("TuskService", "Failed to handle FlightWaypoint action: ${e.message}")
        }
    }

    private fun handleFlightStatusUpdate(args: Any?){
        // Handle action "FlightStatus" with decision making event
        try {
            if (args is JSONObject) {
                val event = args.optJSONArray("event")
                val flag = event?.get(0)
                if (flag == "gather-info"){
                    isGatherAction = true
                } else if (flag == "alert-operator"){
                    isAlertAction = true
                } else {
                    Log.d("TuskService", "Invalid event format for FlightStatus action")
                }
            }
        } catch (e: Exception) {
            Log.e("TuskService", "Failed to handle FlightStatus action: ${e.message}")
        }
    }

    fun getActions() {
        TODO("Not yet implemented")
    }

    fun postControllerStatus(status: TuskControllerStatus) {

    }

//    override fun callReconnectWebsocket() {
//        connectWebSocket()
//    }
}