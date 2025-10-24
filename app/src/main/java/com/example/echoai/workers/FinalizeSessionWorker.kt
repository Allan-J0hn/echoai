package com.example.echoai.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.echoai.data.local.SessionStateRepository
import com.example.echoai.data.local.SessionStatus
import com.example.echoai.domain.RecordingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class FinalizeSessionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val recordingRepository: RecordingRepository,
    private val sessionStateRepository: SessionStateRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val sessionState = sessionStateRepository.get()
        if (sessionState != null && sessionState.status == SessionStatus.RECORDING) {
            recordingRepository.finishSession(sessionState.sessionId)
            sessionStateRepository.clear()
        }
        return Result.success()
    }
}
