package com.example.echoai.ui.recording

import android.Manifest
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.echoai.domain.RecordingStatus
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RecordingScreen(
    onStopRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val recordAudioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    
    if (!recordAudioPermission.status.isGranted) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Microphone permission is required to record.",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { recordAudioPermission.launchPermissionRequest() }) {
                Text("Grant microphone permission")
            }
        }
    }
    return
    }

    val status by viewModel.recordingStatus.collectAsState()
    val elapsedMillis by viewModel.elapsedMillis.collectAsState()
    val formattedTime = String.format("%02d:%02d:%02d",
        TimeUnit.MILLISECONDS.toHours(elapsedMillis),
        TimeUnit.MILLISECONDS.toMinutes(elapsedMillis) % 60,
        TimeUnit.MILLISECONDS.toSeconds(elapsedMillis) % 60
    )

    val alpha by animateFloatAsState(
        targetValue = if (status is RecordingStatus.Recording) 1f else 0f,
        animationSpec = tween(durationMillis = 500)
    )

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(200.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f * alpha), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            val statusText = when (val s = status) {
                is RecordingStatus.Recording -> "Recording..."
                is RecordingStatus.Paused -> "Paused - ${s.reason.name.replace('_', ' ')}"
                else -> "Stopped"
            }
            Text(statusText, style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(64.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onStopRecording, modifier = Modifier.size(72.dp), shape = CircleShape) {
                    Icon(Icons.Filled.Stop, "Stop Recording", modifier = Modifier.size(36.dp))
                }

                AnimatedContent(targetState = status is RecordingStatus.Recording) { isRecording ->
                    if (isRecording) {
                        Button(onClick = onPauseRecording, modifier = Modifier.size(80.dp), shape = CircleShape) {
                            Icon(Icons.Filled.Pause, "Pause Recording", modifier = Modifier.size(48.dp))
                        }
                    } else {
                        Button(onClick = onResumeRecording, modifier = Modifier.size(80.dp), shape = CircleShape, enabled = status is RecordingStatus.Paused) {
                            Icon(Icons.Filled.PlayArrow, "Resume Recording", modifier = Modifier.size(48.dp))
                        }
                    }
                }
            }
        }
    }
}
