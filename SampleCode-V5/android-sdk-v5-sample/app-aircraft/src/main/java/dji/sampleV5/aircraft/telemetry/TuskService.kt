package dji.sampleV5.aircraft.telemetry


import android.util.Log
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import dji.sdk.keyvalue.value.common.Attitude
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

class TuskService{
    private val retrofit = RetrofitHelper.buildService(TuskApi::class.java)
    suspend fun getActions() : Response<TuskTelemetry> {
        retrofit.getActions().isSuccessful
        (
            object : Callback<TuskTelemetry> {
                override fun onResponse(
                    call: Call<TuskTelemetry>,
                    response: Response<TuskTelemetry>
                ) {
                    if (response.isSuccessful) {
                        Log.d("TuskTelemetry", "Success")
                    } else {
                        Log.d("TuskTelemetry", "Failure")
                    }
                }

                override fun onFailure(call: Call<TuskTelemetry>, t: Throwable) {
                    Log.d("TuskTelemetry", "Failure")
                }
            }
        )
        return retrofit.getActions()
    }

    suspend fun postTelemetry(mockTelem: TuskTelemetry) {
        retrofit.postTelemetry(mockTelem).enqueue(
            object : Callback<TuskTelemetry> {
                override fun onResponse(
                    call: Call<TuskTelemetry>,
                    response: Response<TuskTelemetry>
                ) {
                    if (response.isSuccessful) {
                        Log.d("TuskTelemetry", "Success")
                    } else {
                        Log.d("TuskTelemetry", "Failure")
                    }
                }

                override fun onFailure(call: Call<TuskTelemetry>, t: Throwable) {
                    Log.d("TuskTelemetry", "Failure")
                }
            }
        )
    }
}

object RetrofitHelper {

    //    val baseUrl = "https://quotable.io/"
    //val baseUrl = "https://128.138.65.189:5000/"
    private const val baseUrl = "https://11062e86-db17-4d1d-a872-675279309958.mock.pstmn.io"

    private val client = OkHttpClient.Builder().build()
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
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
    suspend fun getActions() : Response<TuskTelemetry>

    @Headers("Content-Type: application/json")
    @POST("/telemetry_store")
    suspend fun postTelemetry(@Body tuskTelemetry: TuskTelemetry) : Call<TuskTelemetry>
}

// Data class for telemetry creates a structure that enables the storage of all relevant information
data class TuskTelemetry(
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("altitude") val altitude: Double?,
    @SerializedName("battery") val battery: Int?,
    @SerializedName("gps") val gps: Int?,
    @SerializedName("roll") val roll: Double?,
    @SerializedName("pitch") val pitch: Double?,
    @SerializedName("yaw") val yaw: Double?,
)