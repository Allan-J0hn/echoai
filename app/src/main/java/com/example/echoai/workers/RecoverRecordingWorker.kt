package com.example.echoai.workers

import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.echoai.data.local.SessionStateRepository
import com.example.echoai.data.local.SessionStatus
import com.example.echoai.service.RecordingForegroundService
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
            appContext.startService(intent)
        }
        return Result.success()
    }
}
