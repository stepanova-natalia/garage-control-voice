package pro.web4u.app.garage.voice.feature.voice.handler

import retrofit2.HttpException
import java.net.HttpURLConnection

sealed class CommandError {
    data object Rejected : CommandError()
    data object RateLimited : CommandError()
    data object Unauthorized : CommandError()
    data object ServerUnavailable : CommandError()
    data class ServerError(val code: Int) : CommandError()
}

private const val HTTP_TOO_MANY_REQUESTS = 429

fun HttpException.toCommandError(): CommandError = when (code()) {
    HttpURLConnection.HTTP_UNAUTHORIZED -> CommandError.Unauthorized
    HttpURLConnection.HTTP_CONFLICT -> CommandError.Rejected
    HTTP_TOO_MANY_REQUESTS -> CommandError.RateLimited
    else -> CommandError.ServerError(code())
}

fun CommandError.toSpeech(): String = when (this) {
    CommandError.Rejected -> "Команда сейчас недоступна"
    CommandError.RateLimited -> "Слишком много команд, подождите"
    CommandError.Unauthorized -> "Нужно войти заново"
    CommandError.ServerUnavailable -> "Нет связи с сервером"
    is CommandError.ServerError -> "Ошибка сервера"
}