package com.example.echoai.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.echoai.MainActivity
import com.example.echoai.R
import com.example.echoai.domain.RecordingController
import com.example.echoai.domain.RecordingStatus
import com.example.echoai.workers.FinalizeSessionWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class RecordingForegroundService : Service(), AudioManager.OnAudioFocusChangeListener {

    @Inject
    lateinit var recordingController: RecordingController

    @Inject
    lateinit var audioFocusHandler: AudioFocusHandler

    @Inject
    lateinit var telephonyHandler: TelephonyHandler

    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var statusJob: Job? = null
    private var notificationTextJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observeStatus()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (audioFocusHandler.requestAudioFocus(this)) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                        telephonyHandler.register { state -> onCallStateChanged(state) }
                    }
                    recordingController.start()
                }
            }
            ACTION_PAUSE -> recordingController.pause(PauseReason.USER)
            ACTION_RESUME -> serviceScope.launch { recordingController.resume() }
            ACTION_STOP -> recordingController.stop()
        }
        return START_STICKY
    }

    private fun observeStatus() {
        statusJob = serviceScope.launch {
            recordingController.recordingStatus.collectLatest { status ->
                when (status) {
                    is RecordingStatus.Recording -> {
                        observeElapsedTime() // Let observeElapsedTime manage startForeground and updates
                    }
                    is RecordingStatus.Paused -> {
                        notificationTextJob?.cancel()
                        val text = "Paused - ${status.reason.name.replace('_', ' ')}"
                        notificationManager.notify(NOTIFICATION_ID, createNotification(text, "EchoAI", status))
                    }
                    is RecordingStatus.Stopped -> {
                        stopSelf()
                    }
                    is RecordingStatus.Error -> {
                        notificationManager.notify(NOTIFICATION_ID, createNotification("Error: ${status.message}", "Error: EchoAI", status))
                        // Do not stop self on error, let the user manually stop
                    }
                    is RecordingStatus.Warning -> {
                        // Let observeElapsedTime manage startForeground and updates, ensure it's running
                        observeElapsedTime() 
                    }
                }
            }
        }
    }
    
    private fun observeElapsedTime() {
        notificationTextJob?.cancel()
        notificationTextJob = serviceScope.launch {
            recordingController.elapsedMillis.collectLatest { millis ->
                val timeFormatted = String.format("%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(millis),
                    TimeUnit.MILLISECONDS.toMinutes(millis) % 60,
                    TimeUnit.MILLISECONDS.toSeconds(millis) % 60
                )
                
                val currentStatus = recordingController.recordingStatus.value
                val notificationText = if (currentStatus is RecordingStatus.Warning) {
                    "${currentStatus.message} ($timeFormatted)"
                } else {
                    "Recording: $timeFormatted"
                }

                val notificationTitle = if (currentStatus is RecordingStatus.Warning) {
                    "Warning: EchoAI"
                } else {
                    "EchoAI"
                }

                // Always use startForeground for updates to ensure it stays a foreground service
                startForeground(NOTIFICATION_ID, createNotification(notificationText, notificationTitle, currentStatus))
            }
        }
    }

    override fun onDestroy() {
        statusJob?.cancel()
        notificationTextJob?.cancel()
        audioFocusHandler.abandonAudioFocus()
        telephonyHandler.unregister()
        if (recordingController.recordingStatus.value is RecordingStatus.Recording || recordingController.recordingStatus.value is RecordingStatus.Warning) {
            val workRequest = OneTimeWorkRequestBuilder<FinalizeSessionWorker>().build()
            WorkManager.getInstance(this).enqueue(workRequest)
        }
        super.onDestroy()
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> recordingController.pause(PauseReason.AUDIO_FOCUS)
            AudioManager.AUDIOFOCUS_GAIN -> serviceScope.launch { recordingController.resume() }
        }
    }

    private fun onCallStateChanged(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING, TelephonyManager.CALL_STATE_OFFHOOK -> recordingController.pause(PauseReason.PHONE_CALL)
            TelephonyManager.CALL_STATE_IDLE -> serviceScope.launch { recordingController.resume() }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Recording Service", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String, title: String, currentStatus: RecordingStatus): Notification {
        val contentIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, RecordingForegroundService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)

        when (currentStatus) {
            is RecordingStatus.Recording, is RecordingStatus.Warning -> {
                val pauseIntent = Intent(this, RecordingForegroundService::class.java).apply { action = ACTION_PAUSE }
                val pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                builder.addAction(R.drawable.ic_launcher_foreground, "Pause", pausePendingIntent)
            }
            is RecordingStatus.Paused -> {
                val resumeIntent = Intent(this, RecordingForegroundService::class.java).apply { action = ACTION_RESUME }
                val resumePendingIntent = PendingIntent.getService(this, 0, resumeIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                builder.addAction(R.drawable.ic_launcher_foreground, "Resume", resumePendingIntent)
            }
            else -> {}
        }

        return builder.build()
    }

    companion object {
        const val ACTION_START = "com.example.echoai.service.START"
        const val ACTION_PAUSE = "com.example.echoai.service.PAUSE"
        const val ACTION_RESUME = "com.example.echoai.service.RESUME"
        const val ACTION_STOP = "com.example.echoai.service.STOP"
        private const val CHANNEL_ID = "RecordingServiceChannel"
        private const val NOTIFICATION_ID = 1
    }
}
