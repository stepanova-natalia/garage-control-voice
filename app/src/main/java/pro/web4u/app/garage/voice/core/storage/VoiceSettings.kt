package pro.web4u.app.garage.voice.core.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class VoiceSettings @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEY_ASSISTANT_NAME = "assistant_name"
        const val DEFAULT_NAME = "джарвис"
    }

    private val prefs = context.getSharedPreferences("voice_ settings", Context.MODE_PRIVATE)

    fun getAssistantName(): String {
        return prefs.getString(KEY_ASSISTANT_NAME, DEFAULT_NAME) ?: DEFAULT_NAME
    }

    fun saveAssistantName(name: String) {
        prefs.edit().putString(KEY_ASSISTANT_NAME, name.lowercase()).apply()
    }
}