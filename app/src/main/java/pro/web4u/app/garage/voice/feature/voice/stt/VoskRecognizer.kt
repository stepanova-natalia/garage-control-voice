package pro.web4u.app.garage.voice.feature.voice.stt

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import pro.web4u.app.garage.voice.core.AppLogger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject


class VoskRecognizer @Inject constructor() : SpeechRecognizer {
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private var isListening = false

    fun init(modelDir: String, grammar: String) {
        model = Model(modelDir)
        recognizer = Recognizer(model, 16000.0f, grammar)
        AppLogger.d("VoskRecognizer", "Модель загружена")
    }

    override suspend fun startListening(onResult: (String) -> Unit) {
        val bufferSize = AudioRecord.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        delay(500)

        withContext(Dispatchers.Main) {
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    16000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            }catch (e: SecurityException) {
                AppLogger.e("VoskRecognizer", "Нет разрешения на микрофон: ${e.message}")
                return@withContext
            }

            if(audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                AppLogger.e("VoskRecognizer", "AudioRecord не инициализирован")
                audioRecord = null
                return@withContext
            }

            audioRecord?.startRecording()
        }
        if(audioRecord?.state != AudioRecord.STATE_INITIALIZED) return

        isListening = true

        withContext(Dispatchers.IO) {
            val buffer = ShortArray(bufferSize)
            while (isListening) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: break
                if(read > 0) {
                    val bytes = ShortArray(read) { buffer[it] }
                        .let { shorts ->
                            val byteBuffer = ByteBuffer
                                .allocate(shorts.size*2)
                                .order(ByteOrder.LITTLE_ENDIAN)
                            shorts.forEach { byteBuffer.putShort(it) }
                            byteBuffer.array()
                        }
                    if(recognizer?.acceptWaveForm(bytes, bytes.size) == true) {
                        val result = recognizer?.result
                        val json = JSONObject(result ?: "{}")
                        val text = json.optString("text")
                        if(text.isNotBlank()) {
                            onResult(text)
                        }
                    }
                }
            }
        }
    }

    override fun stop() {
        isListening = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    fun close() {
        stop()
        recognizer?.close()
        model?.close()
    }


}