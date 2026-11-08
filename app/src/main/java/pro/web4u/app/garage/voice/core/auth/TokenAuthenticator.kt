package pro.web4u.app.garage.voice.core.auth

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import pro.web4u.app.garage.voice.core.network.ApiConstants
import pro.web4u.app.garage.voice.core.network.AuthApi
import pro.web4u.app.garage.voice.core.network.RefreshTokenRequest
import retrofit2.HttpException
import java.io.IOException
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val authApiProvider: Provider<AuthApi>
) : Authenticator {
    private val refreshMutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // 401 on login or refresh itself: refreshing again would wait on the mutex this call already holds
        if (response.request.isAuthRequest()) return null

        return runBlocking {
            refreshMutex.withLock {
                val refreshToken = tokenStorage.getRefreshToken() ?: return@withLock null
                val currentToken = tokenStorage.getAccessToken()
                val failedToken = response.request.header(AUTHORIZATION)?.removePrefix(BEARER_PREFIX)
                // a parallel request refreshed while this one waited, a second refresh would trip reuse detection
                if (currentToken != null && currentToken != failedToken) {
                    return@withLock response.request.withToken(currentToken)
                }
                try {
                    val tokens = authApiProvider.get().refresh(RefreshTokenRequest(refreshToken))
                    tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken)
                    response.request.withToken(tokens.accessToken)
                } catch (e: HttpException) {
                    if (e.code() == HttpURLConnection.HTTP_UNAUTHORIZED) tokenStorage.clearTokens()
                    null
                } catch (_: IOException) {
                    // no network is not a rejected session, keep the tokens for the next try
                    null
                }
            }
        }
    }

    private fun Request.isAuthRequest(): Boolean {
        val path = url.encodedPath
        return path.endsWith(ApiConstants.AUTH_LOGIN) || path.endsWith(ApiConstants.AUTH_REFRESH)
    }

    private fun Request.withToken(token: String): Request =
        newBuilder().header(AUTHORIZATION, "$BEARER_PREFIX$token").build()

    companion object {
        private const val AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }
}