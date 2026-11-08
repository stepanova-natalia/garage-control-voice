package pro.web4u.app.garage.voice.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String
)

data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class RefreshTokenResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String
)

interface AuthApi {
    @POST(ApiConstants.AUTH_LOGIN)
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST(ApiConstants.AUTH_REFRESH)
    suspend fun refresh(@Body request: RefreshTokenRequest): RefreshTokenResponse
}