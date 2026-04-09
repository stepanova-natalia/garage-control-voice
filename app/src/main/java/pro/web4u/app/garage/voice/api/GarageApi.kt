package pro.web4u.app.garage.voice.api

import retrofit2.http.Body
import retrofit2.http.POST


data class GateRequest(val action: String, val commandId: String)
data class LightRequest(val action: String, val commandId: String)
data class ApiResponse(val status: String)


interface GarageApi {

    @POST("/garage/gate")
    suspend fun gate(@Body request: GateRequest): ApiResponse

    @POST("/garage/light")
    suspend fun light(@Body request: LightRequest): ApiResponse
}