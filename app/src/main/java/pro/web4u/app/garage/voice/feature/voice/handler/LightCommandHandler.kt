package pro.web4u.app.garage.voice.feature.voice.handler


import pro.web4u.app.garage.voice.core.AppLogger
import pro.web4u.app.garage.voice.feature.garage.GarageApi
import pro.web4u.app.garage.voice.feature.garage.LightCommandRequest
import pro.web4u.app.garage.voice.feature.garage.domain.LightStatus
import pro.web4u.app.garage.voice.feature.voice.tts.TextSpeaker
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException
import java.io.IOException

@Singleton
class LightCommandHandler @Inject constructor(
    private val garageApi: GarageApi,
    private val tts: TextSpeaker
) : CommandHandler {

    override fun canHandle(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("свет") || t.contains("света")
    }

    override suspend fun handle(text: String) {
        val t = text.lowercase()
        when {
            t.contains("включи") || t.contains("включить") -> handleOn()
            t.contains("выключи") || t.contains("выключить") -> handleOff()
            t.contains("статус") -> handleStatus()
            else -> tts.speak("Команда не распознана")
        }
    }
    private suspend fun handleOn() {
        val status = getStatus()
        if (status == LightStatus.ON) {
            tts.speak("Свет уже включен")
        }else{
            if (sendCommand("on")) tts.speak("Включаю свет")
        }
    }

    private suspend fun handleOff() {
        val status = getStatus()
        if (status == LightStatus.OFF) {
            tts.speak("Свет уже выключен")
        }else{
            if (sendCommand("off")) tts.speak("Выключаю свет")
        }
    }

    private suspend fun handleStatus() {
        val status = getStatus()
        tts.speak(lightStatusText(status))
    }

    private suspend fun sendCommand(action: String): Boolean {
        try {
            garageApi.light(LightCommandRequest(action))
            return true
        } catch (e: HttpException) {
            AppLogger.e("LightCommandHandler", "Команда $action отклонена: ${e.code()}")
            val error = e.toCommandError()
            // on 409 the current light state tells why better than a generic phrase
            val phrase = if (error == CommandError.Rejected) lightStatusText(getStatus()) else error.toSpeech()
            tts.speak(phrase)
        } catch (e: IOException) {
            AppLogger.e("LightCommandHandler", "Команда $action не отправлена: ${e.message}")
            tts.speak(CommandError.ServerUnavailable.toSpeech())
        }
        return false
    }

    private suspend fun getStatus(): LightStatus {
        return try {
            LightStatus.fromRaw(garageApi.lightStatus().status)
        }catch (e: Exception) {
            AppLogger.e("LightCommandHandler", "Ошибка статуса: ${e.message}")
            LightStatus.UNKNOWN
        }
    }

    private fun lightStatusText(status: LightStatus) = when (status) {
        LightStatus.ON -> "Свет включен"
        LightStatus.OFF -> "Свет выключен"
        LightStatus.UNKNOWN -> "Статус света неизвестен"
    }
}