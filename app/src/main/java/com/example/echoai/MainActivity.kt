package com.example.echoai

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.echoai.service.RecordingForegroundService
import com.example.echoai.ui.dashboard.DashboardScreen
import com.example.echoai.ui.permissions.PermissionScreen
import com.example.echoai.ui.recording.RecordingScreen
import com.example.echoai.ui.summary.SummaryScreen
import com.example.echoai.ui.theme.EchoaiTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint

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

                    if (micGranted) {
                        EchoAiNavHost()
                        // Optionally, request notifications in-app somewhere else (snackbar/banner),
                        // but never block dashboard on it.
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
fun EchoAiNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                onStartRecording = {
                    val intent = Intent(navController.context, RecordingForegroundService::class.java).apply {
                        action = RecordingForegroundService.ACTION_START
                    }
                    navController.context.startService(intent)
                    navController.navigate("recording")
                },
                onSessionClick = { sessionId ->
                    navController.navigate("summary/$sessionId")
                },
                onGenerateSummary = { sessionId ->
                    navController.navigate("summary/$sessionId")
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
    }
}
