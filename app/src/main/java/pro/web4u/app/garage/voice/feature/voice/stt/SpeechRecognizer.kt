package pro.web4u.app.garage.voice.feature.voice.stt

interface SpeechRecognizer {
    suspend fun startListening(onResult: (String) -> Unit)
    fun stop()
}