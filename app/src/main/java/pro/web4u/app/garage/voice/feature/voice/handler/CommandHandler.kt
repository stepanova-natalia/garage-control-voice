package pro.web4u.app.garage.voice.feature.voice.handler

interface CommandHandler {
    fun canHandle(text: String): Boolean
    suspend fun handle(text: String)
}