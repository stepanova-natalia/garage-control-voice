package pro.web4u.app.garage.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import pro.web4u.app.garage.voice.service.VoiceService
import pro.web4u.app.garage.voice.ui.SettingsScreen
import pro.web4u.app.garage.voice.ui.theme.GarageVoiceTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if(isGranted) startVoiceService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkAndRequestPermission()

        setContent {
            GarageVoiceTheme {
                var showSettings by remember { mutableStateOf(false) }
                if(showSettings){
                    SettingsScreen(
                        context = this,
                        onBack = { showSettings = false }
                    )
                } else {
                    MainScreen(onSettingsClick = { showSettings = true })
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
        startForegroundService(Intent(this, VoiceService::class.java))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onSettingsClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Голосовой помощник") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp)
        ) {
            Text("Джарвис слушает...", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Доступные команды:", style = MaterialTheme.typography.titleMedium)
            Text("• Джарвис, открой ворота")
            Text("• Джарвис, закрой ворота")
            Text("• Джарвис, включи свет")
            Text("• Джарвис, выключи свет")
            Text("• Джарвис, статус")
        }
    }
}