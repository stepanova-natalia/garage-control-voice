package pro.web4u.app.garage.voice.feature.voice

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import pro.web4u.app.garage.voice.feature.garage.GarageApi
import pro.web4u.app.garage.voice.feature.garage.GarageWebSocketClient
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

@AndroidEntryPoint
class VoiceService : Service() {

    @Inject lateinit var garageApi: GarageApi
    @Inject lateinit var webSocketClient: GarageWebSocketClient
    @Inject lateinit var commandExecutor: CommandExecutor

    companion object {
        private const val CHANNEL_ID = "voice_service_channel"
        private const val NOTIFICATION_ID = 1
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private var tts: TtsHelper? = null
    private var isSpeaking = false
    private var isListeningForCommand = false
    private var listeningtimeoutJob: kotlinx.coroutines.Job? = null

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        Log.d("VoiceService", "onCreate вызван")
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        tts = TtsHelper(this) { speaking -> isSpeaking = speaking }
        initVosk()
    }

    private fun initVosk() {
        scope.launch {
            try {
                while (tts?.isReady() == false) delay(100)

                val modelDir = applicationContext.filesDir.absolutePath + "/model-ru"
                copyModelFromAssets(modelDir)
                model = Model(modelDir)
                Log.d("VoiceService", "Model создан")

                val grammar = """["джарвис", "открой", "закрой", "останови", "остановить", "продолжи", "продолжить", "ворота", "ворот", "движение", "включи", "выключи", "свет", "света", "статус", "[unk]"]"""
                recognizer = Recognizer(model, 16000.0f, grammar)

                tts?.speak("Привет!Я Джарвис. Готов к работе.")
                delay(3000)

                startListening()
                startWebSocket()

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    tts?.speak("Ошибка микрофона, перезапустите приложение")
                    return@launch
                }
                Log.d("VoiceService", "Vosk модель загружена")
            } catch (e: Exception) {
                Log.e("VoiceService", "Ошибка загрузки модели: ${e.message}")
            }
        }
    }

    private fun copyModelFromAssets(destPath: String) {
        val dest = File(destPath)
        if (dest.exists()) return
        dest.mkdirs()
        copyAssetFolder("model-ru", destPath)
    }

    private fun copyAssetFolder(assetPath: String, destPath: String) {
        val assets = applicationContext.assets.list(assetPath) ?: return
        if (assets.isEmpty()) {
            copyAssetFile(assetPath, destPath)
            return
        }
        File(destPath).mkdirs()
        for (asset in assets) {
            copyAssetFolder("$assetPath/$asset", "$destPath/$asset")
        }
    }

    private fun copyAssetFile(assetPath: String, destPath: String) {
        applicationContext.assets.open(assetPath).use { input ->
            FileOutputStream(destPath).use { output ->
                input.copyTo(output)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        job.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        recognizer?.close()
        model?.close()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Голосовой помощник",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Голосовой помощник")
            .setContentText("Я Джарвис, жду команды...")
            .setSmallIcon(R.drawable.ic_btn_speak_now)
            .build()
    }

    private suspend fun startListening() {
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
            } catch (e: SecurityException) {
                Log.e("VoiceService", "SecurityException: ${e.message}")
                tts?.speak("Нет разрешения на микрофон")
                return@withContext
            } catch (e: Exception) {
                Log.e("VoiceService", "Ошибка создания AudioRecord: ${e.message}")
                tts?.speak("Ошибка микрофона")
                return@withContext
            }
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("VoiceService", "AudioRecord не инициализирован")
                tts?.speak("Ошибка микрофона, перезапустите приложение")
                return@withContext
            }
            audioRecord?.startRecording()
        }

        if(audioRecord?.state != AudioRecord.STATE_INITIALIZED) return

        scope.launch(Dispatchers.IO) {
            val buffer = ShortArray(bufferSize)
            while (true) {
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
                    if(!isSpeaking && recognizer?.acceptWaveForm(bytes, bytes.size) == true) {
                        val result = recognizer?.result
                        val json = JSONObject(result ?: "{}")
                        val text = json.optString("text")
                        if(text.isNotBlank()) {
                            Log.d("VoiceService", "Текст: $text | Слушаю команду: $isListeningForCommand")
                            if(!isListeningForCommand) {
                                if(text.lowercase().contains("джарвис")){
                                    isListeningForCommand = true
                                    tts?.speak("Слушаю")
                                    listeningtimeoutJob?.cancel()
                                    listeningtimeoutJob = scope.launch {
                                        delay(5000)
                                        if(isListeningForCommand) {
                                            isListeningForCommand = false
                                            Log.d("VoiceService", "Таймаут — возврат в режим ожидания")
                                        }
                                    }
                                }
                            } else {
                               val command = CommandParser.parse(text, "джарвис")
                               if (command != Command.UNKNOWN) {
                                   listeningtimeoutJob?.cancel()
                                   isListeningForCommand = false
                                   executeCommand(command)
                               }
                            }
                            val command = CommandParser.parse(text, "джарвис")
                            Log.d("VoiceService", "Текст: $text | Команда: $command")
                            if(command != Command.UNKNOWN) {
                                executeCommand(command)
                            }
                        }


                    }
                }
            }
        }
    }

    private fun executeCommand(command: Command) {
        scope.launch {
            commandExecutor.execute(command, tts!!)
        }
    }

    private fun startWebSocket() {
        scope.launch {
            while (true) {
                try {
                    webSocketClient.observeGateStatus()
                        .collect { status ->
                            val text = when (status) {
                                "open" -> "Ворота открыты"
                                "closed" -> "Ворота закрыты"
                                else -> null
                            }
                            text?.let { tts?.speak(it) }
                            Log.d("VoiceService", "Статус ворот: $status")
                        }
                } catch (e: Exception) {
                    Log.e("VoiceService", "WebSocket ошибка: ${e.message}")
                    delay(5000)
                }
            }
        }
    }
}