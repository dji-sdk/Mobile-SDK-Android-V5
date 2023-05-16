package dji.sampleV5.aircraft.telemetry


import android.util.Log
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.lang.reflect.Type


class TuskService{
    private val retrofit = RetrofitHelper.buildService(TuskApi::class.java)
    suspend fun getActions() : Response<TuskAircraftState> {
        retrofit.getActions().isSuccessful
        (
            object : Callback<TuskAircraftState> {
                override fun onResponse(
                    call: Call<TuskAircraftState>,
                    response: Response<TuskAircraftState>
                ) {
                    if (response.isSuccessful) {
                        Log.d("TuskService", "Get Actions Success")
                    } else {
                        Log.d("TuskService", "Get Actions Failure")
                    }
                }

                override fun onFailure(call: Call<TuskAircraftState>, t: Throwable) {
                    Log.d("TuskService", "Get Actions Failure")
                }
            }
        )
        return retrofit.getActions()
    }

    suspend fun postState(state: TuskAircraftState){
        retrofit.postState(state).enqueue(
            object : Callback<Unit?> {
                override fun onResponse(
                    call: Call<Unit?>,
                    response: Response<Unit?>
                ) {
                    if (response.isSuccessful) {
                        Log.d("TuskService", "Post State Success")
                    } else {
                        Log.d("TuskService", "Post State Failure")
                    }
                }

                override fun onFailure(call: Call<Unit?>, t: Throwable) {
                    Log.d("TuskService", "Post State Failure")
                }
            }
        )
    }

    suspend fun postStatus(status: TuskAircraftStatus) {
        retrofit.postStatus(status).enqueue(
            object : Callback<Unit?> {
                override fun onResponse(
                    call: Call<Unit?>,
                    response: Response<Unit?>
                ) {
                    if (response.isSuccessful) {
                        Log.d("TuskService", "Post Status Success")
                    } else {
                        Log.d("TuskService", "Post Status Failure")
                    }
                }
                override fun onFailure(call: Call<Unit?>, t: Throwable) {
                    Log.d("TuskService", "Post Status Failure")
                }
            }
        )
    }
}

object RetrofitHelper {
    // https://stackoverflow.com/questions/32750215/retrofit-2-0-how-to-return-null-string-if-response-body-is-empty
    val nullOnEmptyConverterFactory = object : Converter.Factory() {
        fun converterFactory() = this
        override fun responseBodyConverter(type: Type, annotations: Array<out Annotation>, retrofit: Retrofit) = object : Converter<ResponseBody, Any?> {
            val nextResponseBodyConverter = retrofit.nextResponseBodyConverter<Any?>(converterFactory(), type, annotations)
            override fun convert(value: ResponseBody) = if (value.contentLength() != 0L) nextResponseBodyConverter.convert(value) else null
        }
    }
    //    val baseUrl = "https://quotable.io/"
    //val baseUrl = "https://128.138.65.189:5000/"
    private const val baseUrl = "https://5d70e186-a5cb-4866-b48c-46c70be24563.mock.pstmn.io"

    private val client = OkHttpClient.Builder().build()
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(nullOnEmptyConverterFactory)
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()
    fun<T> buildService(service: Class<T>): T{
        return retrofit.create(service)
    }
}

interface TuskApi {
    // Initialize retrofit to connect to JS server
    // Use hardcoded static IP as host, we'll create a LAN network using hotspot
    // Assume the endpoint we want to have
    @GET("/actions")
    suspend fun getActions() : Response<TuskAircraftState>

    @Headers("Content-Type: application/json")
    @POST("/aircraft_state")
    suspend fun postState(@Body tuskAircraftState: TuskAircraftState) : Call<Unit?>

    @Headers("Content-Type: application/json")
    @POST("/aircraft_status")
    suspend fun postStatus(@Body tuskAircraftStatus: TuskAircraftStatus) : Call<Unit?>

    @Headers("Content-Type: application/json")
    @POST("/controller_status")
    suspend fun postControlStatus(@Body tuskControllerStatus: TuskControllerStatus) : Call<TuskControllerStatus>
}

// Classes for storing all data.
// Aircraft state is anything related to the physical aircraft's position, velocity, and attitude
data class TuskAircraftState(
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("altitude") val altitude: Double?,
    @SerializedName("roll") val roll: Double?,
    @SerializedName("pitch") val pitch: Double?,
    @SerializedName("yaw") val yaw: Double?,
    @SerializedName("velocityX") val velocityX: Double?,
    @SerializedName("velocityY") val velocityY: Double?,
    @SerializedName("velocityZ") val velocityZ: Double?,
    @SerializedName("windSpeed") val windSpeed: Int?,
    @SerializedName("windDirection") val windDirection: String?,
    @SerializedName("isFlying") val isFlying: Boolean?,
)

// Aircraft Status is anything related to the aircraft's hardware and connectivity
data class TuskAircraftStatus(
    @SerializedName("connected") val connected: Boolean?,
    @SerializedName("battery") val battery: Int?,
    @SerializedName("gps") val gps: Int?,
    @SerializedName("signalQuality") val signalQuality: Int?,
    @SerializedName("goHomeState") val goHomeState: String?,
//    https://developer.dji.com/api-reference-v5/android-api/Components/IKeyManager/DJIValue.html#value_flightcontroller_enum_gohomestate_inline
    @SerializedName("flightMode") val flightMode: String?,
//    https://developer.dji.com/api-reference-v5/android-api/Components/IKeyManager/DJIValue.html#value_flightcontroller_enum_flightmode_inline
    @SerializedName("motorsOn") val motorsOn: Boolean?,
    @SerializedName("homeLocationLat") val homeLocationLat: Double?,
    @SerializedName("homeLocationLong") val homeLocationLong: Double?,
    @SerializedName("gimbalAngle") val gimbalAngle: Double?,
)

data class TuskControllerStatus(
    @SerializedName("battery") val battery: Int?,
    @SerializedName("pauseButton") val pauseButton: Boolean?,
    @SerializedName("homeButton") val homeButton: Boolean?,
    @SerializedName("leftStickX") val leftStickX: Int?,
    @SerializedName("leftStickY") val leftStickY: Int?,
    @SerializedName("rightStickX") val rightStickX: Int?,
    @SerializedName("rightStickY") val rightStickY: Int?,
)