package pro.web4u.app.garage.voice.core.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import pro.web4u.app.garage.voice.core.network.AuthApi
import pro.web4u.app.garage.voice.core.network.RefreshTokenRequest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStorage: TokenStorage
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val refreshToken = tokenStorage.getRefreshToken() ?: return null

        val newTokens = runBlocking {
            try {
                val authApi = Retrofit.Builder()
                    .baseUrl(response.request.url.toString().substringBefore("/auth"))
                    .client(OkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(AuthApi::class.java)
                authApi.refresh(RefreshTokenRequest(refreshToken))
            }catch (e: Exception) {
                null
            }
        } ?: run {
            tokenStorage.clearTokens()
            return null
        }

        tokenStorage.saveTokens(newTokens.accessToken, newTokens.refreshToken)

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${newTokens.accessToken}")
            .build()
    }
}