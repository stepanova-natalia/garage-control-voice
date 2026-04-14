package pro.web4u.app.garage.voice.feature.voice

import android.util.Log
import kotlinx.coroutines.delay
import pro.web4u.app.garage.voice.feature.garage.GarageApi
import pro.web4u.app.garage.voice.feature.garage.GateCommandRequest
import pro.web4u.app.garage.voice.feature.garage.LightCommandRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandExecutor @Inject constructor(
    private val garageApi: GarageApi
) {

    suspend fun execute(command: Command, tts: TextSpeaker) {
        var lastException: Exception? = null
        repeat(2) { attempt ->
            try {
                executeInternal(command, tts)
                return
            } catch (e: Exception) {
                lastException = e
                Log.e("CommandExecutor", "Попытка ${attempt + 1} не удалась: ${e.message}")
                if (attempt == 0) delay(2000)
            }
        }
        Log.e("CommandExecutor", "Все попытки исчерпаны: ${lastException?.message}")
        tts.speak("Ошибка соединения с сервером")
    }

    suspend fun executeInternal(command: Command, tts: TextSpeaker) {
        when (command) {
            Command.GATE_OPEN -> {
                tts.speak("Проверяю статус ворот")
                val status = garageApi.gateStatus().status
                when (status) {
                    "open" -> tts.speak("Ворота уже открыты")
                    "moving" -> handleMoving(tts)
                    else -> {
                        garageApi.gate(GateCommandRequest("open"))
                        tts.speak("Открываю ворота")
                    }
                }
            }
            Command.GATE_CLOSE -> {
                tts.speak("Проверяю статус ворот")
                val status = garageApi.gateStatus().status
                when (status) {
                    "close" -> tts.speak("Ворота уже закрыты")
                    "moving" -> handleMoving(tts)
                    else -> {
                        garageApi.gate(GateCommandRequest("close"))
                        tts.speak("Закрываю ворота")
                    }
                }
            }
            Command.LIGHT_ON -> {
                tts.speak("Проверяю статус света")
                val status = garageApi.lightStatus().status
                if (status == "on") {
                    tts.speak("Свет уже включён")
                } else {
                    garageApi.light(LightCommandRequest("on"))
                    tts.speak("Включаю свет")
                }
            }
            Command.LIGHT_OFF -> {
                tts.speak("Проверяю статус света")
                val status = garageApi.lightStatus().status
                if (status == "off") {
                    tts.speak("Свет уже выключен")
                } else {
                    garageApi.light(LightCommandRequest("off"))
                    tts.speak("Выключаю свет")
                }
            }
            Command.GATE_STATUS -> {
                tts.speak("Запрашиваю статус ворот")
                val status = garageApi.gateStatus().status
                val text = when (status) {
                    "open" -> "Ворота открыты"
                    "close" -> "Ворота закрыты"
                    "moving" -> "Ворота в движении"
                    else -> "Статус ворот неизвестен"
                }
                tts.speak(text)
            }
            Command.LIGHT_STATUS -> {
                tts.speak("Запрашиваю статус света")
                val status = garageApi.lightStatus().status
                val text = when (status) {
                    "on" -> "Свет включён"
                    "off" -> "Свет выключен"
                    else -> "Статус света неизвестен"
                }
                tts.speak(text)
            }
            Command.GATE_STOP -> {
                tts.speak("Проверяю статус ворот")
                val status = garageApi.gateStatus().status
                if(status=="moving"){
                    garageApi.gate(GateCommandRequest("open"))
                    tts.speak("Команда остановки отправлена")
                }else{
                    val text = when (status) {
                        "open" -> "Ворота открыты"
                        "close" -> "Ворота закрыты"
                        "moving" -> "Ворота в движении"
                        else -> "Статус ворот неизвестен"
                    }
                    tts.speak(text)
                }
            }
            Command.GATE_CONTINUE -> {
                tts.speak("Проверяю статус ворот")
                val status = garageApi.gateStatus().status
                if(status == "moving"){
                    garageApi.gate(GateCommandRequest("open"))
                    tts.speak("Продолжаю движение")
                }else{
                    val text = when (status) {
                        "open" -> "Ворота открыты"
                        "close" -> "Ворота закрыты"
                        "moving" -> "Ворота в движении"
                        else -> "Статус ворот неизвестен"
                    }
                    tts.speak(text)
                }
            }
            Command.UNKNOWN -> tts.speak("Команда не распознана")
        }
    }

    private suspend fun handleMoving(tts: TextSpeaker) {
        tts.speak("Ворота в движении, подождите")
        var attempts = 0
        while (attempts < 3) {
            delay(30_000)
            val status = try {
                garageApi.gateStatus().status
            } catch (e: Exception) {
                return
            }
            if (status != "moving") {
                val text = if (status == "open") "ворота открыты" else "ворота закрыты"
                tts.speak("Ворота остановились, $text")
                return
            }
            if (attempts < 2) tts.speak("Всё ещё в движении, жду")
            attempts++
        }
        tts.speak("Ворота долго в движении, проверьте вручную")
    }
}