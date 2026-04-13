package pro.web4u.app.garage.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import pro.web4u.app.garage.voice.feature.voice.VoiceService
import pro.web4u.app.garage.voice.ui.login.LoginScreen
import pro.web4u.app.garage.voice.ui.login.LoginViewModel
import pro.web4u.app.garage.voice.ui.main.MainScreen
import pro.web4u.app.garage.voice.ui.settings.SettingsScreen
import pro.web4u.app.garage.voice.ui.theme.GarageVoiceTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val loginViewModel: LoginViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if(isGranted) startVoiceService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GarageVoiceTheme {
                val isLoggedIn by loginViewModel.isLoggedIn.collectAsStateWithLifecycle()
                val isLoading by loginViewModel.isLoading.collectAsStateWithLifecycle()
                val error by loginViewModel.error.collectAsStateWithLifecycle()
                var showSettings by remember { mutableStateOf(false) }
                when {
                    !isLoggedIn -> {
                        LoginScreen(
                            isLoading = isLoading,
                            error = error,
                            onLogin = { username, password ->
                                loginViewModel.login(username, password)
                            }
                        )
                    }
                    showSettings -> {
                        SettingsScreen(
                            context = this,
                            onBack = { showSettings = false }
                        )
                    }
                    else -> {
                        checkAndRequestPermission()
                        MainScreen(onSettingsClick = { showSettings = true })
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> startVoiceService()
            else -> requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceService() {
        android.util.Log.d("MainActivity", "Запускаем сервис")
        startForegroundService(Intent(this, VoiceService::class.java))
    }
}