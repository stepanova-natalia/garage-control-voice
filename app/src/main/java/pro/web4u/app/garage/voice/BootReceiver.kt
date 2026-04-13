package pro.web4u.app.garage.voice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import pro.web4u.app.garage.voice.core.auth.TokenStorage
import pro.web4u.app.garage.voice.feature.voice.VoiceService

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
                context.startForegroundService(Intent(context, VoiceService::class.java))
            }
        }
    }
}