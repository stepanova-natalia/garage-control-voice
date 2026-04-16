package pro.web4u.app.garage.voice.core

import android.util.Log


object AppLogger {

    private const val TAG = "GarageVoice"
    private var isDebug = true

    fun d(tag: String, message: String) {
        if(isDebug) Log.d("$TAG/$tag", message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$TAG/$tag", message, throwable)
    }

    fun w(tag: String, message: String) {
        if (isDebug) Log.w("$TAG/$tag", message)
    }

    fun i(tag: String, message: String) {
        Log.i("$TAG/$tag", message)
    }

    fun setDebug(debug: Boolean) {
        isDebug = debug
    }
}