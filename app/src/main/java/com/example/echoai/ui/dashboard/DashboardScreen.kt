package com.example.echoai.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.echoai.data.local.Session
import com.example.echoai.data.local.SessionStatus
import com.example.echoai.data.local.SessionWithChunks
import com.example.echoai.ui.common.SessionStatusChip
import com.example.echoai.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onStartRecording: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onGenerateSummary: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sessions") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onStartRecording,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Start new recording")
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is DashboardUiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.message, color = MaterialTheme.colorScheme.error) }
                is DashboardUiState.Success -> {
                    if (state.sessions.isEmpty()) {
                        EmptyState(onStartRecording = onStartRecording)
                    } else {
                        SessionList(
                            sessions = state.sessions,
                            onSessionClick = onSessionClick,
                            onDelete = { id -> viewModel.deleteSession(id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onStartRecording: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Dimens.spacing_xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Mic,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(Dimens.spacing_l))
        Text(
            "No sessions yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Dimens.spacing_s))
        Text(
            "Tap the + button to start a new recording.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onStartRecording, modifier = Modifier.padding(top = Dimens.spacing_l)) {
            Text("Start recording")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionItem(
    session: SessionWithChunks,
    onClick: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    var showDelete by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(session.session.startTime))
    val durationInSeconds = session.session.durationSec.toLong()
    val hours = TimeUnit.SECONDS.toHours(durationInSeconds)
    val minutes = TimeUnit.SECONDS.toMinutes(durationInSeconds) % 60
    val seconds = durationInSeconds % 60
    val formattedDuration = if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick(session.session.id) },
                onLongClick = { showDelete = true }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevation_card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(Dimens.spacing_l)) {
            Text(formattedDate, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(Dimens.spacing_xs))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Duration: $formattedDuration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SessionStatusChip(status = session.session.status)
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete recording?") },
            text = { Text("This will permanently delete this session.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(session.session.id)
                    showDelete = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SessionList(
    sessions: List<SessionWithChunks>,
    onSessionClick: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(Dimens.spacing_l),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacing_m)
    ) {
        items(sessions, key = { it.session.id }) { s ->
            SessionItem(s, onClick = onSessionClick, onDelete = onDelete)
        }
    }
}
