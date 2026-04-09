package pro.web4u.app.garage.voice.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TtsHelper(context: Context, private val onSpeakingChanged: (Boolean) -> Unit) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if(status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("ru")
                isReady = true
                Log.d("TtsHelper", "TTS готов")
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onSpeakingChanged(true)
                    }
                    override fun onDone(utteranceId: String?) {
                        onSpeakingChanged(false)
                    }

                    override fun onError(utteranceId: String?) {
                        onSpeakingChanged(false)
                    }
                })
            } else {
                Log.e("TtsHelper", "TTS ошибка инициализации: $status")
            }
        }
    }

    fun speak(text: String) {
        if(isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }else {
            Log.w("TtsHelper", "TTS еще не готов, откладываем: $text")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}