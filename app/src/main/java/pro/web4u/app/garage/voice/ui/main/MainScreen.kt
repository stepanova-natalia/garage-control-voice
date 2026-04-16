package pro.web4u.app.garage.voice.ui.main

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.graphics.Color
import pro.web4u.app.garage.voice.feature.voice.VoiceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    assistantName: String,
    voiceState: VoiceState,
    onSettingsClick: () -> Unit
) {
    val displayName = assistantName.replaceFirstChar { it.uppercase() }
    val wakePhrase = "Окей $displayName"

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val pulseScaleState = infiniteTransition.animateFloat(
        initialValue = when (voiceState) {
            VoiceState.IDLE -> 0.95f
            VoiceState.LISTENING -> 0.8f
            VoiceState.EXECUTING -> 0.9f
        },
        targetValue = when (voiceState) {
            VoiceState.IDLE -> 1.05f
            VoiceState.LISTENING -> 1.3f
            VoiceState.EXECUTING -> 1.2f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (voiceState) {
                    VoiceState.IDLE -> 2000
                    VoiceState.LISTENING -> 600
                    VoiceState.EXECUTING -> 400
                }
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val iconColor = when (voiceState) {
        VoiceState.IDLE -> MaterialTheme.colorScheme.primary
        VoiceState.LISTENING -> Color(0xFF4CAF50)
        VoiceState.EXECUTING -> MaterialTheme.colorScheme.secondary
    }

    val statusText = when (voiceState) {
        VoiceState.IDLE -> "Скажите «$wakePhrase»"
        VoiceState.LISTENING -> "Слушаю, говорите команду..."
        VoiceState.EXECUTING -> "Выполняю..."
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {  Text(assistantName.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            //Indicator
            Box(
               contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp * pulseScaleState.value)
                        .background(
                            iconColor.copy(alpha = 0.1f),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            iconColor.copy(alpha = 0.15f),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(iconColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Слушаю вас",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(48.dp))
            //Command gate
            CommandCard(
                title = "🚪 Ворота",
                wakePhrase = wakePhrase,
                commands = listOf(
                    "открой ворота",
                    "закрой ворота",
                    "останови ворота",
                    "продолжи движение",
                    "статус ворот"
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            CommandCard(
                title = "💡 Свет",
                wakePhrase = wakePhrase,
                commands = listOf(
                    "включи свет",
                    "выключи свет",
                    "статус света"
                )
            )
        }
    }
}

@Composable
fun CommandCard(
    title: String,
    wakePhrase: String,
    commands: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            commands.forEach { command ->
                Text(
                    "$wakePhrase, $command",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}