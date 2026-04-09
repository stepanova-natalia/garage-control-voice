package pro.web4u.app.garage.voice.service

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import pro.web4u.app.garage.voice.api.ApiClient
import pro.web4u.app.garage.voice.api.GateRequest
import pro.web4u.app.garage.voice.api.LightRequest
import pro.web4u.app.garage.voice.parser.Command
import pro.web4u.app.garage.voice.parser.CommandParser
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

class VoiceService : Service() {

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

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        tts = TtsHelper(this) { speaking -> isSpeaking = speaking}
        initVosk()
    }

    private fun initVosk() {
        scope.launch {
            try {
                val modelDir = applicationContext.filesDir.absolutePath+"/model-ru"
                copyModelFromAssets(modelDir)
                model = Model(modelDir)
                recognizer = Recognizer(model, 16000.0f)
                startListening()
                delay(1500)
                tts?.speak("Привет! Я Джарвис. Готов к работе.")
                Log.d("VoiceService", "Vosk модель загружена")
            } catch (e: Exception) {
                Log.e("VoiceService", "Ошибка загрузки модели: ${e.message}")
            }
        }
    }

    private fun copyModelFromAssets(destPath: String) {
        val dest = File(destPath)
        if(dest.exists()) return
        dest.mkdirs()
        copyAssetFolder("model-ru", destPath)
    }

    private fun copyAssetFolder(assetPath: String, destPath: String) {
        val assets = applicationContext.assets.list(assetPath) ?: return
        if(assets.isEmpty()){
            copyAssetFile(assetPath, destPath)
            return
        }
        File(destPath).mkdirs()
        for(asset in assets) {
            copyAssetFolder("$assetPath/$asset", "$destPath/$asset")
        }
    }

    private fun copyAssetFile(assetPath: String, destPath: String){
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
        super.onDestroy()
        audioRecord?.stop()
        audioRecord?.release()
        tts?.shutdown()
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
            .setContentText("Слушаю...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    private fun startListening() {
        val bufferSize = AudioRecord.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        }catch (e: SecurityException){
            Log.e("VoiceService", "Нет разрешения на микрофон: ${e.message}")
            return
        }

        audioRecord?.startRecording()

        scope.launch {
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
            try {
                val api = ApiClient.getApi(this@VoiceService)
                val id = UUID.randomUUID().toString()
                when (command) {
                    Command.GATE_OPEN -> {
                        api.gate(GateRequest("open", id))
                        tts?.speak("Открываю ворота")
                    }
                    Command.GATE_CLOSE -> {
                        api.gate(GateRequest("close", id))
                        tts?.speak("Закрываю ворота")
                    }
                    Command.LIGHT_ON -> {
                        api.light(LightRequest("on", id))
                        tts?.speak("Включаю свет")
                    }
                    Command.LIGHT_OFF -> {
                        api.light(LightRequest("off", id))
                        tts?.speak("Выключаю свет")
                    }
                    Command.STATUS -> {
                        tts?.speak("Запрашиваю статус")
                    }
                    Command.UNKNOWN -> {
                        tts?.speak("Команда не распознана")
                    }
                }
            }catch (e: Exception) {
                Log.e("VoiceService", "Ошибка команды: ${e.message}")
                tts?.speak("Ошибка соединения с сервером")
            }
        }
    }
}