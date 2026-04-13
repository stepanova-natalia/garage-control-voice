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
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import pro.web4u.app.garage.voice.feature.garage.GarageApi
import pro.web4u.app.garage.voice.feature.garage.GateCommandRequest
import pro.web4u.app.garage.voice.feature.garage.LightCommandRequest
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

@AndroidEntryPoint
class VoiceService : Service() {

    @Inject lateinit var garageApi: GarageApi

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
                tts?.speak("Привет!")

                val modelDir = applicationContext.filesDir.absolutePath + "/model-ru"
                copyModelFromAssets(modelDir)
                model = Model(modelDir)
                Log.d("VoiceService", "Model создан")

                val grammar = """["джарвис", "открой", "закрой", "ворота", "включи", "выключи", "свет", "статус", "[unk]"]"""
                recognizer = Recognizer(model, 16000.0f, grammar)
                while (tts?.isReady() == false) {
                    delay(100)
                }
                tts?.speak("Я Джарвис. Готов к работе.")
                delay(3000)
                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    startListening()
                }
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
        super.onDestroy()
        job.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        recognizer?.close()
        model?.close()
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
            .setSmallIcon(R.drawable.ic_btn_speak_now)
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
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("VoiceService", "AudioRecord не инициализирован")
            }
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
                when (command) {
                    Command.GATE_OPEN -> {
                        tts?.speak("Проверяю статус ворот")
                        val status = garageApi.gateStatus().status
                        when (status) {
                            "open" -> tts?.speak("Ворота уже открыты")
                            "moving" -> handleMoving()
                            else -> {
                                garageApi.gate(GateCommandRequest("open"))
                                tts?.speak("Открываю ворота")
                            }
                        }
                    }
                    Command.GATE_CLOSE -> {
                        tts?.speak("Проверяю статус ворот")
                        val status = garageApi.gateStatus().status
                        when (status){
                            "close" -> tts?.speak("Ворота уже закрыты")
                            "moving" -> handleMoving()
                            else -> {
                                garageApi.gate(GateCommandRequest("close"))
                                tts?.speak("Закрываю ворота")
                            }
                        }
                    }
                    Command.LIGHT_ON -> {
                        tts?.speak("Проверяю статус света")
                        val status = garageApi.lightStatus().status
                        if(status == "on") {
                            tts?.speak("Свет уже включен")
                        }else{
                            garageApi.light(LightCommandRequest("on"))
                            tts?.speak("Включаю свет")
                        }
                    }
                    Command.LIGHT_OFF -> {
                        tts?.speak("Проверяю статус света")
                        val status = garageApi.lightStatus().status
                        if(status == "off") {
                            tts?.speak("Свет уже выключен")
                        }else{
                            garageApi.light(LightCommandRequest("off"))
                            tts?.speak("Выключаю свет")
                        }
                    }
                    Command.STATUS -> {
                        tts?.speak("Запрашиваю статус")
                        val gateStatus = garageApi.gateStatus().status
                        val lightStatus = garageApi.lightStatus().status
                        val gateText = when (gateStatus){
                            "open" -> "ворота открыты"
                            "close" -> "ворота закрыты"
                            "moving" -> "ворота в движении"
                            else -> "статус ворот неизвестен"
                        }
                        val lightText = when (lightStatus){
                            "on" -> "свет включен"
                            "off" -> "свет выключен"
                            else -> "статус света неизвестен"
                        }
                        tts?.speak("$gateText, $lightText")
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

    private suspend fun handleMoving() {
        tts?.speak("Ворота в движении, подождите")
        repeat(3) { attempt ->
            delay(30_000)
            val status = try {
                garageApi.gateStatus().status
            }catch (e: Exception) {
                return
            }
            if(status != "moving") {
                val text = if (status == "open") "ворота открыты" else "ворота закрыты"
                tts?.speak("Ворота остановились, $text")
                return
            }
            if (attempt < 2) tts?.speak("Всё еще в движении, жду")
        }
        tts?.speak("Ворота долго в движении, проверьте вручную")
    }
}