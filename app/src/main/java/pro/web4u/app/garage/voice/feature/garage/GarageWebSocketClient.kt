package pro.web4u.app.garage.voice.feature.garage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import pro.web4u.app.garage.voice.core.storage.ServerSettings
import pro.web4u.app.garage.voice.core.auth.TokenStorage
import pro.web4u.app.garage.voice.core.network.ApiConstants
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class GarageWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @param:ApplicationContext private val context: Context,
    private val tokenStorage: TokenStorage
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _gateStatus = MutableSharedFlow<String>(replay = 0)
    val gateStatus: SharedFlow<String> = _gateStatus.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var webSocket: WebSocket? = null

    init {
        connect()
    }

    private fun connect(){
        val baseUrl = ServerSettings.getServerUrl(context).trimEnd('/')
        val url = baseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/" + ApiConstants.GATE_WS

        val request = Request.Builder().url(url).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener(){
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // the backend reads the token from the first frame and ignores the Authorization header
                val token = tokenStorage.getAccessToken()
                if (token != null) webSocket.send(token) else webSocket.close(NORMAL_CLOSURE, "no access token")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                _isConnected.value = true
                val status = JSONObject(text).getString("status")
                scope.launch { _gateStatus.emit(status) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _isConnected.value = false
                scope.launch {
                    delay(RECONNECT_DELAY)
                    connect()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                scope.launch {
                    delay(RECONNECT_DELAY)
                    connect()
                }
            }
        })
    }
    companion object {
        private const val NORMAL_CLOSURE = 1000
        private val RECONNECT_DELAY = 5.seconds
    }
}