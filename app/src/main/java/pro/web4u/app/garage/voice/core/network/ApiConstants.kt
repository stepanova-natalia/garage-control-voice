package pro.web4u.app.garage.voice.core.network

// version lives only here: when the backend moved to /v1, paths broke in every Api interface
object ApiConstants {
    private const val API_VERSION = "v1"

    const val AUTH_LOGIN = "$API_VERSION/auth/login"
    const val AUTH_REFRESH = "$API_VERSION/auth/refresh"
    const val GATE_WS = "$API_VERSION/garage/ws"
}