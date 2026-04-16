package pro.web4u.app.garage.voice.feature.voice.handler


import pro.web4u.app.garage.voice.core.AppLogger
import pro.web4u.app.garage.voice.feature.garage.GarageApi
import pro.web4u.app.garage.voice.feature.garage.LightCommandRequest
import pro.web4u.app.garage.voice.feature.voice.tts.TextSpeaker
import javax.inject.Inject
import javax.inject.Singleton

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
        if(status == "on"){
            tts.speak("Свет уже включен")
        }else{
            garageApi.light(LightCommandRequest("on"))
            tts.speak("Включаю свет")
        }
    }

    private suspend fun handleOff() {
        val status = getStatus()
        if(status == "off") {
            tts.speak("Свет уже выключен")
        }else{
            garageApi.light(LightCommandRequest("off"))
            tts.speak("Выключаю свет")
        }
    }

    private suspend fun handleStatus() {
        val status = getStatus()
        tts.speak(lightStatusText(status))
    }

    private suspend fun getStatus(): String {
        return try {
            garageApi.lightStatus().status
        }catch (e: Exception) {
            AppLogger.e("LightCommandHandler", "Ошибка статуса: ${e.message}")
            "unknown"
        }
    }

    private fun lightStatusText(status: String) = when (status) {
        "on" -> "Свет включен"
        "off" -> "Свет выключен"
        else -> "Статус света не известен"
    }
}