package pro.web4u.app.garage.voice.feature.voice.handler

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import pro.web4u.app.garage.voice.core.AppLogger
import pro.web4u.app.garage.voice.feature.garage.GarageApi
import pro.web4u.app.garage.voice.feature.garage.GarageWebSocketClient
import pro.web4u.app.garage.voice.feature.garage.GateCommandRequest
import pro.web4u.app.garage.voice.feature.garage.domain.GateStatus
import pro.web4u.app.garage.voice.feature.voice.tts.TextSpeaker
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

@Singleton
class GateCommandHandler @Inject constructor(
    private val garageApi: GarageApi,
    private val tts: TextSpeaker,
    private val webSocketClient: GarageWebSocketClient
) : CommandHandler {
    private var cachedStatus: GateStatus = GateStatus.UNKNOWN

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
        tts.speak("Команда получена, выполняю")
        when (getStatus()) {
            GateStatus.OPEN -> tts.speak("Ворота уже открыты")
            GateStatus.MOVE -> waitForResult()
            else -> if (sendCommand("open")) {
                waitForMoving()
                waitForResult()
            }
        }
    }

    private suspend fun handleClose() {
        tts.speak("Команда получена, выполняю")
        when (getStatus()) {
            GateStatus.CLOSE -> tts.speak("Ворота уже закрыты")
            GateStatus.MOVE -> waitForResult()
            else -> if (sendCommand("close")) {
                waitForMoving()
                waitForResult()
            }
        }
    }

    private suspend fun handleStop() {
        tts.speak("Команда получена, выполняю")
        val status = getStatus()
        if (status == GateStatus.MOVE) {
            if (sendCommand("stop")) tts.speak("Команда остановки отправлена")
        } else {
            tts.speak(gateStatusText(status))
        }
    }

    private suspend fun handleContinue() {
        tts.speak("Команда получена, выполняю")
        val status = getStatus()
        if (status == GateStatus.STOP) {
            if (sendCommand("continue")) tts.speak("Продолжаю движение")
        } else {
            tts.speak(gateStatusText(status))
        }
    }

    private suspend fun handleStatus() {
        tts.speak(gateStatusText(getStatus()))
    }

    private suspend fun sendCommand(action: String): Boolean {
        try {
            garageApi.gate(GateCommandRequest(action))
            return true
        } catch (e: HttpException) {
            AppLogger.e("GateCommandHandler", "Команда $action отклонена: ${e.code()}")
            val error = e.toCommandError()
            // on 409 the current gate state tells why better than a generic phrase
            val phrase = if (error == CommandError.Rejected) gateStatusText(getStatus()) else error.toSpeech()
            tts.speak(phrase)
        } catch (e: IOException) {
            AppLogger.e("GateCommandHandler", "Команда $action не отправлена: ${e.message}")
            tts.speak(CommandError.ServerUnavailable.toSpeech())
        }
        return false
    }

    private suspend fun waitForMoving() {
        withTimeoutOrNull(MOVE_START_TIMEOUT) {
            webSocketClient.gateStatus.first { GateStatus.fromRaw(it) == GateStatus.MOVE }
        }
    }

    private suspend fun waitForResult() {
        withTimeoutOrNull(TRAVEL_TIMEOUT) {
            webSocketClient.gateStatus.first {
                val status = GateStatus.fromRaw(it)
                status == GateStatus.OPEN || status == GateStatus.CLOSE
            }
        }
    }

    private suspend fun getStatus(): GateStatus {
        return try {
            GateStatus.fromRaw(garageApi.gateStatus().status).also { cachedStatus = it }
        } catch (e: Exception) {
            AppLogger.e("GateCommandHandler", "Ошибка статуса: ${e.message}")
            cachedStatus
        }
    }

    private fun gateStatusText(status: GateStatus) = when (status) {
        GateStatus.OPEN -> "Ворота открыты"
        GateStatus.CLOSE -> "Ворота закрыты"
        GateStatus.MOVE -> "Ворота в движении"
        GateStatus.STOP -> "Ворота остановлены"
        GateStatus.UNKNOWN -> "Статус ворот неизвестен"
    }

    companion object {
        private val MOVE_START_TIMEOUT = 10.seconds
        private val TRAVEL_TIMEOUT = 60.seconds
    }
}