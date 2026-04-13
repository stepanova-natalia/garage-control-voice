package pro.web4u.app.garage.voice.core.storage

import android.content.Context
import java.net.URI

object ServerSettings {

    private const val PREFS_NAME = "server_settings"
    private const val KEY_SERVER_URL = "server_url"
    private const val DEFAULT_URL = "https://garage.home.arpa"

    fun getServerUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SERVER_URL, DEFAULT_URL) ?: DEFAULT_URL
    }

    fun saveServerUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SERVER_URL, url).apply()
    }


}