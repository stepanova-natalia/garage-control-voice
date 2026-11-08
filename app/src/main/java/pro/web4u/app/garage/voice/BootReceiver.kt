package pro.web4u.app.garage.voice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import pro.web4u.app.garage.voice.core.auth.TokenStorage
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import pro.web4u.app.garage.voice.core.AppLogger

class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootReceiverEntryPoint {
        fun tokenStorage(): TokenStorage
    }

    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                BootReceiverEntryPoint::class.java
            )
            if(entryPoint.tokenStorage().hasTokens()){
                // Android 15+ forbids starting a microphone service from BOOT_COMPLETED
                showStartNotification(context)
            }
        }
    }
    private fun showStartNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // the system drops it silently, the log is the only trace why nothing showed up after boot
            AppLogger.w("BootReceiver", "Нет разрешения на уведомления, запуск после загрузки не предложен")
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Запуск помощника", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Джарвис не запущен")
            .setContentText("Нажмите, чтобы запустить голосового помощника")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "voice_start_channel"
        private const val NOTIFICATION_ID = 2
    }
}