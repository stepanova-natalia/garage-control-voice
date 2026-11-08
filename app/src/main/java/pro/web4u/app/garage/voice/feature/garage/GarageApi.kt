package pro.web4u.app.garage.voice.feature.garage

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.UUID

data class GateCommandRequest(
    @SerializedName("action") val action: String,
    @SerializedName("commandId") val commandId: String = UUID.randomUUID().toString()
)

data class LightCommandRequest(
    @SerializedName("action") val action: String,
    @SerializedName("commandId") val commandId: String = UUID.randomUUID().toString()
)

data class GateStatusResponse(
    @SerializedName("status") val status: String
)

data class LightStatusResponse(
    @SerializedName("status") val status: String
)

interface GarageApi {

    @POST("/v1/garage/command")
    suspend fun gate(@Body request: GateCommandRequest): Unit

    @POST("/v1/garage/light")
    suspend fun light(@Body request: LightCommandRequest): Unit

    @GET("v1/garage/status")
    suspend fun gateStatus(): GateStatusResponse

    @GET("v1/garage/light/status")
    suspend fun lightStatus(): LightStatusResponse
}