package pro.web4u.app.garage.voice.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pro.web4u.app.garage.voice.core.auth.TokenStorage
import pro.web4u.app.garage.voice.core.network.AuthApi
import pro.web4u.app.garage.voice.core.network.LoginRequest
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStorage: TokenStorage
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isLoggedIn = MutableStateFlow(tokenStorage.hasTokens())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = authApi.login(LoginRequest(username, password))
                tokenStorage.saveTokens(response.accessToken, response.refreshToken)
                _isLoggedIn.value = true
            }catch (e: Exception) {
                _error.value = "Ошибка входа: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}