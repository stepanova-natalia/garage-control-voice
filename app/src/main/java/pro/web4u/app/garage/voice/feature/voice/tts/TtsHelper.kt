package pro.web4u.app.garage.voice.feature.voice.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pro.web4u.app.garage.voice.core.AppLogger
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsHelper @Inject constructor(
    @ApplicationContext private val context: Context
): TextSpeaker {

    private var tts: TextToSpeech? = null
    private var isReady = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.forLanguageTag("ru")
                isReady = true
                AppLogger.d("TtsHelper", "TTS готов")
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            } else {
                AppLogger.e("TtsHelper", "TTS ошибка инициализации: $status")
            }
        }
    }

    override fun speak(text: String) {
        if(isReady) {
            _isSpeaking.value = true
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, text)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }

    fun isReady(): Boolean = isReady
}