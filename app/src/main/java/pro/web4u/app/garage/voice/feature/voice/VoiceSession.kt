package pro.web4u.app.garage.voice.feature.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pro.web4u.app.garage.voice.feature.voice.handler.CommandRegistry
import pro.web4u.app.garage.voice.feature.voice.stt.VoskRecognizer
import pro.web4u.app.garage.voice.feature.voice.tts.TtsHelper
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pro.web4u.app.garage.voice.core.AppLogger
import pro.web4u.app.garage.voice.core.storage.VoiceSettings
import javax.inject.Singleton

@Singleton
class VoiceSession @Inject constructor(
    private val registry: CommandRegistry,
    private val ttsHelper: TtsHelper,
    private val voiceSettings: VoiceSettings
) {
    private val _state = MutableStateFlow(VoiceState.IDLE)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private lateinit var recognizer: VoskRecognizer

    private val assistantName get() = voiceSettings.getAssistantName()
    private val wakePhrase = "окей $assistantName"
    private var isListeningForCommand = false
    private var timeoutJob: Job? = null

    fun setRecognizer(r: VoskRecognizer) {
        recognizer = r
    }

    suspend fun start(scope: CoroutineScope) {
        recognizer.startListening { text ->
            scope.launch {
                onTextRecognized(text, scope)
            }
        }
    }

    fun stop() {
        recognizer.stop()
        timeoutJob?.cancel()
    }

    private suspend fun onTextRecognized(text: String, scope: CoroutineScope) {
        if(ttsHelper.isSpeaking.value) return

        AppLogger.d("VoiceSession", "Текст: $text | Слушаю команду: $isListeningForCommand")

        val t = text.lowercase()

        if (t.contains(wakePhrase)) {
            val handled = registry.dispatch(text)
            if (handled) {
                timeoutJob?.cancel()
                isListeningForCommand = false
                _state.value = VoiceState.IDLE
            } else {
                isListeningForCommand = true
                _state.value = VoiceState.LISTENING
                startTimeout(scope)
            }
        } else if (isListeningForCommand) {
            val handled = registry.dispatch(text)
            if (handled) {
                timeoutJob?.cancel()
                isListeningForCommand = false
                _state.value = VoiceState.IDLE
            }
        }
    }

    private fun startTimeout(scope: CoroutineScope) {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(5000)
            if(isListeningForCommand) {
                isListeningForCommand = false
                _state.value = VoiceState.IDLE
                AppLogger.d("VoiceSession", "Таймаут - возврат в режим ожидания")
            }
        }
    }









}