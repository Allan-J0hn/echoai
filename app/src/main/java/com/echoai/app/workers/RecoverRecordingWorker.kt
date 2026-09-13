package com.echoai.app.workers

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.echoai.app.data.local.SessionStateRepository
import com.echoai.app.data.local.SessionStatus
import com.echoai.app.service.RecordingForegroundService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RecoverRecordingWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val sessionStateRepository: SessionStateRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val sessionState = sessionStateRepository.get()
        if (sessionState != null && sessionState.status == SessionStatus.RECORDING) {
            val intent = Intent(appContext, RecordingForegroundService::class.java).apply {
                action = RecordingForegroundService.ACTION_START
            }
            ContextCompat.startForegroundService(appContext, intent)
        }
        return Result.success()
    }
}
