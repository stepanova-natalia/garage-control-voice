package pro.web4u.app.garage.voice.feature.voice.handler

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandRegistry @Inject constructor() {
    private val handlers = mutableListOf<CommandHandler>()

    fun register(handler: CommandHandler) {
        handlers.add(handler)
    }

    suspend fun dispatch(text: String): Boolean {
        val handler = handlers.firstOrNull { it.canHandle(text) }
        Log.d("CommandRegistry", "Текст: $text | Хендлер: ${handler?.javaClass?.simpleName}")
        return if (handler != null) {
            handler.handle(text)
            true
        }else{
            false
        }
    }
}