package com.echoai.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.echoai.app.ui.theme.Dimens

private const val DEMO_API_KEY = "sk-echoai-demo-4f9a1c2b"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(Dimens.spacing_l),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacing_l)
        ) {
            ProviderCard()
            LiveEndpointCard()
        }
    }
}

@Composable
private fun ProviderCard() {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(Dimens.elevation_card)) {
        Column(modifier = Modifier.padding(Dimens.spacing_l), verticalArrangement = Arrangement.spacedBy(Dimens.spacing_s)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.spacing_s)) {
                Text("Transcription Provider", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                AssistChip(onClick = {}, label = { Text("Demo Mode") })
            }
            Text(
                "Transcripts and summaries in this build are generated on-device by a mock engine. No audio ever leaves the phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            Text("API Key", style = MaterialTheme.typography.labelLarge)
            Text(maskKey(DEMO_API_KEY), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun LiveEndpointCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(Dimens.elevation_card)
    ) {
        Column(modifier = Modifier.padding(Dimens.spacing_l), verticalArrangement = Arrangement.spacedBy(Dimens.spacing_s)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.spacing_s)) {
                Text("Live Endpoint", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                AssistChip(onClick = {}, enabled = false, label = { Text("Not configured") })
            }
            Text(
                "Set TRANSCRIBE_URL and TRANSCRIBE_KEY in local.properties to point this app at a real transcription API. " +
                    "Without them, the app always falls back to the mock pipeline above.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun maskKey(key: String): String {
    if (key.length <= 8) return key
    return key.take(key.length - 8) + "••••" + key.takeLast(4)
}
