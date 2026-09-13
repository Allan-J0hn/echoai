package com.echoai.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.echoai.app.domain.RecordingStatus
import com.echoai.app.service.RecordingForegroundService
import com.echoai.app.ui.dashboard.DashboardScreen
import com.echoai.app.ui.permissions.PermissionScreen
import com.echoai.app.ui.recording.RecordingScreen
import com.echoai.app.ui.recording.RecordingViewModel
import com.echoai.app.ui.settings.SettingsScreen
import com.echoai.app.ui.summary.SummaryScreen
import com.echoai.app.ui.theme.EchoaiTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EchoaiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val micPermissions = listOf(Manifest.permission.RECORD_AUDIO)
                    val micPermsState = rememberMultiplePermissionsState(micPermissions)
                    val micGranted = micPermsState.allPermissionsGranted

                    // Optional notifications (don’t gate UI on this)
                    val wantsNotifications = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    val notifPermsState = if (wantsNotifications)
                        rememberMultiplePermissionsState(listOf(Manifest.permission.POST_NOTIFICATIONS))
                    else null
                    LaunchedEffect(micGranted, notifPermsState?.allPermissionsGranted) {
                        if (micGranted && notifPermsState?.allPermissionsGranted == false) {
                            notifPermsState.launchMultiplePermissionRequest()
                        }
                    }

                    if (micGranted) {
                        EchoAiNavHost()
                    } else {
                        PermissionScreen(
                            // Only request RECORD_AUDIO here
                            onRequestPermission = { micPermsState.launchMultiplePermissionRequest() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EchoAiNavHost(recordingViewModel: RecordingViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val startDestination = remember {
        when (recordingViewModel.currentStatus) {
            is RecordingStatus.Recording, is RecordingStatus.Paused, is RecordingStatus.Warning -> "recording"
            else -> "dashboard"
        }
    }
    NavHost(navController = navController, startDestination = startDestination) {
        composable("dashboard") {
            DashboardScreen(
                onStartRecording = {
                    val intent = Intent(navController.context, RecordingForegroundService::class.java).apply {
                        action = RecordingForegroundService.ACTION_START
                    }
                    ContextCompat.startForegroundService(navController.context, intent)
                    navController.navigate("recording")
                },
                onSessionClick = { sessionId ->
                    navController.navigate("summary/$sessionId")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                }
            )
        }
        composable("recording") {
            RecordingScreen(
                onStopRecording = {
                    navController.context.startService(
                        Intent(navController.context, RecordingForegroundService::class.java).apply {
                            action = RecordingForegroundService.ACTION_STOP
                        }
                    )
                    navController.popBackStack()
                },
                onPauseRecording = {
                    navController.context.startService(
                        Intent(navController.context, RecordingForegroundService::class.java).apply {
                            action = RecordingForegroundService.ACTION_PAUSE
                        }
                    )
                },
                onResumeRecording = {
                    navController.context.startService(
                        Intent(navController.context, RecordingForegroundService::class.java).apply {
                            action = RecordingForegroundService.ACTION_RESUME
                        }
                    )
                }
            )
        }
        composable(
            "summary/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) {
            SummaryScreen()
        }
        composable("settings") {
            SettingsScreen()
        }
    }
}
