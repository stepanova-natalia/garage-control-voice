package pro.web4u.app.garage.voice.feature.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pro.web4u.app.garage.voice.core.AppLogger
import pro.web4u.app.garage.voice.feature.garage.GarageWebSocketClient
import pro.web4u.app.garage.voice.feature.voice.handler.CommandRegistry
import pro.web4u.app.garage.voice.feature.voice.handler.GateCommandHandler
import pro.web4u.app.garage.voice.feature.voice.handler.LightCommandHandler
import pro.web4u.app.garage.voice.feature.voice.stt.VoskRecognizer
import pro.web4u.app.garage.voice.feature.voice.tts.TtsHelper
import javax.inject.Inject
import  java.io.File
import java.io.FileOutputStream
import androidx.core.content.edit

@AndroidEntryPoint
class VoiceService : Service() {

    @Inject lateinit var ttsHelper: TtsHelper
    @Inject lateinit var voskRecognizer: VoskRecognizer
    @Inject lateinit var voiceSession: VoiceSession
    @Inject lateinit var webSocketClient: GarageWebSocketClient
    @Inject lateinit var gateCommandHandler: GateCommandHandler
    @Inject lateinit var lightCommandHandler: LightCommandHandler
    @Inject lateinit var commandRegistry: CommandRegistry

    companion object {
        private const val CHANNEL_ID = "voice_service_channel"
        private const val NOTIFICATION_ID = 1
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupRegistry() {
        commandRegistry.register(gateCommandHandler)
        commandRegistry.register(lightCommandHandler)
    }

    private fun startService() {
        scope.launch {
            while (ttsHelper.isReady() == false) {
                delay(100)
            }
            initVosk()
            startWebSocket()
        }
    }

    override fun onCreate() {
        AppLogger.d("VoiceService", "onCreate вызван")
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        setupRegistry()
        startService()
    }

    private suspend fun initVosk() {
        try {
            val modelDir = applicationContext.filesDir.absolutePath + "/model-ru"
            copyModelFromAssets(modelDir)
            voskRecognizer.init(
                modelDir,
                """["окей", "джарвис", "открой", "закрой", "останови", "остановить", "продолжи", "продолжить", "ворота", "ворот", "движение", "включи", "выключи", "свет", "света", "статус", "[unk]"]"""
            )
            if(isFirstStart()) {
                ttsHelper.speak("Привет! Я Джарвис. Готов к работе.")
                delay(3000)
            }
            voiceSession.setRecognizer(voskRecognizer)
            scope.launch {
                voiceSession.start(scope)
            }
            AppLogger.d("VoiceService", "Vosk модель загружена")
        } catch (e: Exception) {
            AppLogger.e("VoiceService", "Ошибка загрузки модели: ${e.message}")
            ttsHelper.speak("Ошибка загрузка модели, перезапустите приложение")
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
        voiceSession.stop()
        ttsHelper.shutdown()
        voskRecognizer.close()
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
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    private fun startWebSocket() {
        scope.launch {
            var lastStatus: String? = null

            webSocketClient.gateStatus.collect { status ->
                AppLogger.d("VoiceService", "lastStatus=$lastStatus новый=$status")
                if(lastStatus != null && lastStatus != status){
                    val text = when (status) {
                        "open" -> "Ворота открыты"
                        "close" -> "Ворота закрыты"
                        "moving" -> "Ворота в движении"
                        "stop" -> "Ворота остановились"
                        else -> null
                    }
                    text?.let { ttsHelper.speak(it) }
                }
                lastStatus = status
            }
        }
        scope.launch {
            webSocketClient.isConnected
                .collect { connected ->
                    if(!connected) ttsHelper.speak("Нет связи с сервером")
                }
        }
    }

    private fun isFirstStart(): Boolean {
        val prefs = getSharedPreferences("voice_service", MODE_PRIVATE)
        val first = prefs.getBoolean("first_start", true)
        if(first) prefs.edit { putBoolean("first_start", false) }
        return first
    }
}