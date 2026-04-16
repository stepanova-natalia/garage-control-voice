package pro.web4u.app.garage.voice.feature.voice.stt

interface WakeWordDetector {
    suspend fun startDetecting(onDetected: () -> Unit)
    fun stop()
}