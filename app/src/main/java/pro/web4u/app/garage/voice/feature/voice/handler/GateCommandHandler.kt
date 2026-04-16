package pro.web4u.app.garage.voice.feature.voice.handler


import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import pro.web4u.app.garage.voice.core.AppLogger
import pro.web4u.app.garage.voice.feature.garage.GarageApi
import pro.web4u.app.garage.voice.feature.garage.GarageWebSocketClient
import pro.web4u.app.garage.voice.feature.garage.GateCommandRequest
import pro.web4u.app.garage.voice.feature.voice.tts.TextSpeaker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GateCommandHandler @Inject constructor(
    private val garageApi: GarageApi,
    private val tts: TextSpeaker,
    private val webSocketClient: GarageWebSocketClient
) : CommandHandler {
    private var cachedStatus: String = "unknown"


    override fun canHandle(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("ворота") || t.contains("ворот") ||
                t.contains("останови") || t.contains("остановить") ||
                t.contains("продолжи") || t.contains("продолжить")
    }

    override suspend fun handle(text: String) {
        val t = text.lowercase()
        when {
            t.contains("открой") || t.contains("открыть") -> handleOpen()
            t.contains("закрой") || t.contains("закрыть") -> handleClose()
            t.contains("останови") || t.contains("остановить") -> handleStop()
            t.contains("продолжи") || t.contains("продолжить") -> handleContinue()
            t.contains("статус") -> handleStatus()
            else -> tts.speak("Команда не распознана")
        }
    }

    private suspend fun handleOpen() {
        val status = getStatus()
        when(status){
            "open" -> tts.speak("Ворота уже открыты")
            "moving" -> waitForResult()
            else -> {
                garageApi.gate(GateCommandRequest("open"))
                waitForMoving()
                waitForResult()
            }
        }
    }

    private suspend fun handleClose() {
        val status = getStatus()
        when(status) {
            "close" -> tts.speak("Ворота уже закрыты")
            "moving" -> waitForResult()
            else -> {
                garageApi.gate(GateCommandRequest("close"))
                waitForMoving()
                waitForResult()
            }
        }
    }

    private suspend fun handleStop() {
        val status = getStatus()
        if(status == "moving"){
            garageApi.gate(GateCommandRequest("open"))
            tts.speak("Команда остановки отправлена")
        }else{
            tts.speak(gateStatusText(status))
        }
    }

    private suspend fun handleContinue() {
        val status = getStatus()
        if(status == "moving") {
            garageApi.gate(GateCommandRequest("open"))
            tts.speak("Продолжаю движение")
        }else{
            tts.speak(gateStatusText(status))
        }
    }

    private suspend fun handleStatus() {
        val status = getStatus()
        tts.speak(gateStatusText(status))
    }

    private suspend fun waitForMoving(){
        withTimeoutOrNull(10_000) {
            webSocketClient.gateStatus
                .first { it == "moving" }
        }
        tts.speak("Ворота в движении")
    }

    private suspend fun waitForResult() {
        val result =withTimeoutOrNull(60_000) {
            webSocketClient.gateStatus
                .first { it == "open" || it == "close" }
        }
        when (result) {
            "open" -> tts.speak("Ворота открыты")
            "close" -> tts.speak("Ворота закрыты")
            null -> tts.speak("Ворота долго в движении, проверьте вручную")
        }
    }

    private suspend fun getStatus(): String {
        return try {
            val status = garageApi.gateStatus().status
            cachedStatus = status
            status
        } catch (e: Exception) {
            AppLogger.e("GateCommandHandler", "Ошибка статуса: ${e.message}")
            cachedStatus
        }
    }

    private fun gateStatusText(status: String) = when (status) {
        "open" -> "Ворота открыты"
        "close" -> "Ворота закрыты"
        "moving" -> "Ворота в движении"
        "stop" -> "Ворота остановлены"
        else -> "Статус ворот неизвестен"
    }
}