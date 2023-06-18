package dji.sampleV5.aircraft.telemetry


import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.lang.reflect.Type


class TuskServiceRetrofit{
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
