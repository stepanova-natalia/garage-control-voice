package pro.web4u.app.garage.voice.feature.garage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import pro.web4u.app.garage.voice.core.storage.ServerSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GarageWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context
) {
    fun observeGateStatus(): Flow<String> = callbackFlow {
        val baseUrl = ServerSettings.getServerUrl(context).trimEnd('/')
        val url = baseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/garage/ws"

        val request = Request.Builder().url(url).build()

        val webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val status = JSONObject(text).getString("status")
                trySend(status)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                close(t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                close()
            }
        })
        awaitClose {webSocket.close(1000, "cancelled")}
    }
}