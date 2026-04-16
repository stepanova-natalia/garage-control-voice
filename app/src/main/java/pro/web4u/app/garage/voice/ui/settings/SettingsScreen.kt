package pro.web4u.app.garage.voice.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pro.web4u.app.garage.voice.core.storage.ServerSettings
import pro.web4u.app.garage.voice.core.storage.VoiceSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(context: Context, onBack: () -> Unit) {
    var url by remember { mutableStateOf(ServerSettings.getServerUrl(context)) }
    var assistantName by remember { mutableStateOf(VoiceSettings(context).getAssistantName()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp)
        ) {
            Text("Сервер", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL сервера") },
                singleLine = true,
                modifier = Modifier.Companion.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text("Голосовой помощник", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = assistantName,
                onValueChange = { assistantName = it },
                label = { Text("Имя помощника") },
                singleLine = true,
                modifier = Modifier.Companion.fillMaxWidth()
            )
            Button(
                onClick = {
                    ServerSettings.saveServerUrl(context, url.trim())
                    VoiceSettings(context).saveAssistantName(assistantName.trim())
                    onBack()
                },
                enabled = url.isNotBlank() && assistantName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }
        }
    }
}